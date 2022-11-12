package managers;

import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.google.common.collect.Sets;
import daos.SharedWorkoutDAO;
import daos.UserDAO;
import daos.WorkoutDAO;
import exceptions.ManagerExecutionException;
import models.SharedDay;
import models.SharedWeek;
import utils.AttributeValueUtils;
import imports.Globals;
import utils.Metrics;
import utils.UpdateItemTemplate;
import utils.WorkoutUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;
import models.OwnedExercise;
import models.SharedWorkoutMeta;
import models.Routine;
import models.SharedExercise;
import models.SharedWorkout;
import models.User;
import models.Workout;
import models.WorkoutMeta;
import responses.AcceptWorkoutResponse;

public class AcceptReceivedWorkoutManager {

    private final UserDAO userDAO;
    private final SharedWorkoutDAO sharedWorkoutDAO;
    private final Metrics metrics;

    @Inject
    public AcceptReceivedWorkoutManager(final SharedWorkoutDAO sharedWorkoutDAO,
        final UserDAO userDAO,
        final Metrics metrics) {
        this.sharedWorkoutDAO = sharedWorkoutDAO;
        this.userDAO = userDAO;
        this.metrics = metrics;
    }

    /**
     * Accepts a received workout and adds any exercises that the user doesn't already own to their owned exercises
     * mapping.
     * <p>
     * Accepting a workout is the same as creating a new one, so the same action is used here.
     *
     * @param activeUser        username of the user that is accepting the workout.
     * @param workoutIdToAccept id of the received workout that the active user is accepting.
     * @param optionalName      if present, the user is changing the name before accepting it.
     * @throws Exception if there are any input errors or if user does not exist.
     */
    public AcceptWorkoutResponse acceptReceivedWorkout(final String activeUser, final String workoutIdToAccept,
        final String optionalName)
        throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".acceptReceivedWorkout";
        this.metrics.commonSetup(classMethod);

        try {
            final User activeUserObject = this.userDAO.getUser(activeUser);
            final SharedWorkout workoutToAccept = this.sharedWorkoutDAO.getSharedWorkout(workoutIdToAccept);
            final SharedWorkoutMeta sharedWorkoutMeta = activeUserObject.getReceivedWorkouts().get(workoutIdToAccept);

            if (optionalName != null) {
                workoutToAccept.setWorkoutName(optionalName);
            }

            String errorMessage = validInput(activeUserObject, workoutToAccept);
            if (!errorMessage.isEmpty()) {
                this.metrics.commonClose(false);
                throw new ManagerExecutionException(errorMessage);
            }

            // add any exercises that the user does not already own
            addNewExercises(workoutToAccept, activeUserObject);

            // user has no workouts yet, so make this newly accepted one the current workout
            boolean updateCurrentWorkout = activeUserObject.getCurrentWorkout() == null;

            // create the workout as a new one. Not a separate method due to transactions
            final String workoutId = UUID.randomUUID().toString();
            final String creationTime = Instant.now().toString();
            final Workout newWorkout = new Workout();
            Map<String, String> exerciseNameToId = new HashMap<>();
            for (String exerciseId : activeUserObject.getOwnedExercises().keySet()) {
                exerciseNameToId.putIfAbsent(
                    activeUserObject.getOwnedExercises().get(exerciseId).getExerciseName(),
                    exerciseId);
            }
            final Routine routine = new Routine(workoutToAccept.getRoutine(), exerciseNameToId);
            newWorkout.setCreationDate(creationTime);
            newWorkout.setCreator(activeUser);
            newWorkout.setMostFrequentFocus(sharedWorkoutMeta.getMostFrequentFocus());
            newWorkout.setWorkoutId(workoutId);
            newWorkout.setWorkoutName(workoutToAccept.getWorkoutName());
            newWorkout.setRoutine(routine);
            newWorkout.setCurrentDay(0);
            newWorkout.setCurrentWeek(0);

            final WorkoutMeta workoutMeta = new WorkoutMeta();
            workoutMeta.setWorkoutName(workoutToAccept.getWorkoutName().trim());
            workoutMeta.setAverageExercisesCompleted(0.0);
            workoutMeta.setDateLast(creationTime);
            workoutMeta.setTimesCompleted(0);
            workoutMeta.setTotalExercisesSum(0);

            // update all the exercises that are now a part of this workout
            WorkoutUtils.updateOwnedExercises(activeUserObject, routine, workoutId, workoutToAccept.getWorkoutName());

            UpdateItemTemplate updateUserItemData = new UpdateItemTemplate(activeUser, UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("set " +
                    User.CURRENT_WORKOUT + " = :currentWorkoutVal, " +
                    User.WORKOUTS + ".#workoutId= :workoutUserMap, " +
                    User.EXERCISES + "= :exercisesMap "
                    + "remove " + User.RECEIVED_WORKOUTS + ".#receivedWorkoutId")
                .withValueMap(new ValueMap()
                    .withString(":currentWorkoutVal",
                        updateCurrentWorkout ? workoutId : activeUserObject.getCurrentWorkout())
                    .withMap(":workoutUserMap", workoutMeta.asMap())
                    .withMap(":exercisesMap", activeUserObject.getOwnedExercisesMap()))
                .withNameMap(new NameMap()
                    .with("#receivedWorkoutId", workoutIdToAccept)
                    .with("#workoutId", workoutId));
            // since user is accepting the workout, delete the shared workout from the table - it's no longer needed
            UpdateItemTemplate updateSharedWorkoutData = new UpdateItemTemplate(
                workoutToAccept.getSharedWorkoutId(), SharedWorkoutDAO.SHARED_WORKOUTS_TABLE_NAME);

            List<TransactWriteItem> actions = new ArrayList<>();
            actions.add(new TransactWriteItem().withUpdate(updateUserItemData.asUpdate()));
            actions.add(new TransactWriteItem().withDelete(updateSharedWorkoutData.asDelete()));
            actions.add(new TransactWriteItem()
                .withPut(new Put().withTableName(WorkoutDAO.WORKOUT_TABLE_NAME).withItem(
                    AttributeValueUtils.convertMapToAttributeValueMap(newWorkout.asMap()))));
            this.userDAO.executeWriteTransaction(actions);

            this.metrics.commonClose(true);
            return new AcceptWorkoutResponse(workoutId, workoutMeta, newWorkout,
                activeUserObject.getOwnedExercises());
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }

    private String validInput(final User activeUserObject, final SharedWorkout sharedWorkout) {
        StringBuilder error = new StringBuilder();
        if (activeUserObject.getPremiumToken() == null
            && activeUserObject.getWorkoutMetas().size() >= Globals.MAX_FREE_WORKOUTS) {
            error.append("Maximum workouts would be exceeded.");
        }
        if (activeUserObject.getPremiumToken() != null
            && activeUserObject.getWorkoutMetas().size() >= Globals.MAX_WORKOUTS) {
            error.append("Maximum workouts would be exceeded.");
        }
        Set<String> sharedWorkoutExercises = new HashSet<>();
        for (SharedWeek week : sharedWorkout.getRoutine()) {
            for (SharedDay day : week) {
                for (SharedExercise sharedExercise : day) {
                    sharedWorkoutExercises.add(sharedExercise.getExerciseName());
                }
            }
        }

        List<WorkoutMeta> workoutMetas = new ArrayList<>(activeUserObject.getWorkoutMetas().values());
        for (WorkoutMeta workoutMeta : workoutMetas) {
            if (workoutMeta.getWorkoutName().equals(sharedWorkout.getWorkoutName())) {
                error.append("Workout with this name already exists.");
            }
        }

        Set<String> ownedExercises = new HashSet<>();
        for (String exerciseId : activeUserObject.getOwnedExercises().keySet()) {
            ownedExercises.add(activeUserObject.getOwnedExercises().get(exerciseId).getExerciseName());
        }
        Set<String> totalExercises = Sets.union(sharedWorkoutExercises, ownedExercises);
        if (activeUserObject.getPremiumToken() == null && totalExercises.size() > Globals.MAX_FREE_EXERCISES) {
            error.append("Accepting this workout would put you above the amount of exercises allowed.");
        }
        if (activeUserObject.getPremiumToken() != null && totalExercises.size() > Globals.MAX_PREMIUM_EXERCISES) {
            error.append("Accepting this workout would put you above the amount of exercises allowed.");
        }

        return error.toString().trim();
    }

    private void addNewExercises(final SharedWorkout sharedWorkout, final User user) {
        Set<String> sharedWorkoutExercises = new HashSet<>();
        for (SharedWeek week : sharedWorkout.getRoutine()) {
            for (SharedDay day : week) {
                for (SharedExercise sharedExercise : day) {
                    sharedWorkoutExercises.add(sharedExercise.getExerciseName());
                }
            }
        }
        Set<String> ownedExercises = new HashSet<>();
        for (String exerciseId : user.getOwnedExercises().keySet()) {
            ownedExercises.add(user.getOwnedExercises().get(exerciseId).getExerciseName());
        }
        Set<String> newExercises = Sets.difference(sharedWorkoutExercises, ownedExercises);
        for (String exerciseName : newExercises) {
            // for each of the exercises that the user doesn't own, make a new entry for them in the owned mapping
            OwnedExercise ownedExercise = new OwnedExercise(sharedWorkout.getExercises().get(exerciseName),
                exerciseName);
            String exerciseId = UUID.randomUUID().toString();
            user.getOwnedExercises().putIfAbsent(exerciseId, ownedExercise);
        }
    }
}
