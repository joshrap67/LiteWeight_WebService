package managers;

import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.google.common.collect.Sets;
import daos.SentWorkoutDAO;
import daos.UserDAO;
import daos.WorkoutDAO;
import exceptions.ManagerExecutionException;
import helpers.AttributeValueHelper;
import helpers.Globals;
import helpers.Metrics;
import helpers.UpdateItemData;
import helpers.WorkoutHelper;
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
import models.SentExercise;
import models.SentWorkout;
import models.User;
import models.Workout;
import models.WorkoutMeta;
import responses.AcceptWorkoutResponse;

public class AcceptReceivedWorkoutManager {

    private final UserDAO userDAO;
    private final SentWorkoutDAO sentWorkoutDAO;
    private final Metrics metrics;

    @Inject
    public AcceptReceivedWorkoutManager(final SentWorkoutDAO sentWorkoutDAO, final UserDAO userDAO,
        final Metrics metrics) {
        this.sentWorkoutDAO = sentWorkoutDAO;
        this.userDAO = userDAO;
        this.metrics = metrics;
    }

    /**
     * Accepts a friend request and adds the accepted user to the friends list of the active user.
     * Upon success, a data notification is sent to the accepted user.
     *
     * @param activeUser        username of the user that is accepting the friend request.
     * @param workoutIdToAccept id of the received workout that the active user is accepting.
     * @throws Exception if there are any input errors or if either user does not exist.
     */
    public AcceptWorkoutResponse acceptReceivedWorkout(final String activeUser,
        final String workoutIdToAccept, final String optionalName)
        throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".acceptReceivedWorkout";
        this.metrics.commonSetup(classMethod);

        try {
            final User activeUserObject = this.userDAO.getUser(activeUser);
            final SentWorkout workoutToAccept = this.sentWorkoutDAO
                .getSentWorkout(workoutIdToAccept);
            final SharedWorkoutMeta sharedWorkoutMeta = activeUserObject.getReceivedWorkouts()
                .get(workoutIdToAccept);

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

            // update all the exercises that are now apart of this workout
            WorkoutHelper.updateUserExercises(activeUserObject, routine, workoutId,
                workoutToAccept.getWorkoutName());

            UpdateItemData updateUserItemData = new UpdateItemData(activeUser,
                UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("set " +
                    User.CURRENT_WORKOUT + " = :currentWorkoutVal, " +
                    User.WORKOUTS + ".#workoutId= :workoutUserMap, " +
                    User.EXERCISES + "= :exercisesMap "
                    + "remove " + User.RECEIVED_WORKOUTS + ".#receivedWorkoutId")
                .withValueMap(new ValueMap()
                    .withString(":currentWorkoutVal",
                        updateCurrentWorkout ? workoutId : activeUserObject.getCurrentWorkout())
                    .withMap(":workoutUserMap", workoutMeta.asMap())
                    .withMap(":exercisesMap", activeUserObject.getUserExercisesMap()))
                .withNameMap(new NameMap()
                    .with("#receivedWorkoutId", workoutIdToAccept)
                    .with("#workoutId", workoutId));
            // since user is accepting the workout, delete the sent workout from the table - it's no longer needed
            UpdateItemData updateSentWorkoutData = new UpdateItemData(
                workoutToAccept.getSentWorkoutId(), SentWorkoutDAO.SENT_WORKOUT_TABLE_NAME);

            List<TransactWriteItem> actions = new ArrayList<>();
            actions.add(new TransactWriteItem().withUpdate(updateUserItemData.asUpdate()));
            actions.add(new TransactWriteItem().withDelete(updateSentWorkoutData.asDelete()));
            actions.add(new TransactWriteItem()
                .withPut(new Put().withTableName(WorkoutDAO.WORKOUT_TABLE_NAME).withItem(
                    AttributeValueHelper.convertMapToAttributeValueMap(newWorkout.asMap()))));
            this.userDAO.executeWriteTransaction(actions);

            this.metrics.commonClose(true);
            return new AcceptWorkoutResponse(workoutId, workoutMeta, newWorkout,
                activeUserObject.getOwnedExercises());
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }

    private String validInput(final User activeUserObject, final SentWorkout sentWorkout) {
        StringBuilder error = new StringBuilder();
        if (activeUserObject.getPremiumToken() == null
            && activeUserObject.getUserWorkouts().size() >= Globals.MAX_FREE_WORKOUTS) {
            error.append("Maximum free workouts would be exceeded.");
        }
        if (activeUserObject.getPremiumToken() != null
            && activeUserObject.getUserWorkouts().size() >= Globals.MAX_WORKOUTS) {
            error.append("Maximum workouts would be exceeded.");
        }
        Set<String> sentWorkoutExercises = new HashSet<>();
        for (Integer week : sentWorkout.getRoutine()) {
            for (Integer day : sentWorkout.getRoutine().getWeek(week)) {
                for (SentExercise sentExercise : sentWorkout.getRoutine()
                    .getExerciseListForDay(week, day)) {
                    sentWorkoutExercises.add(sentExercise.getExerciseName());
                }
            }
        }
        Set<String> ownedExercises = new HashSet<>();
        for (String exerciseId : activeUserObject.getOwnedExercises().keySet()) {
            ownedExercises
                .add(activeUserObject.getOwnedExercises().get(exerciseId).getExerciseName());
        }
        List<WorkoutMeta> workoutMetas = new ArrayList<>(
            activeUserObject.getUserWorkouts().values());
        for (WorkoutMeta workoutMeta : workoutMetas) {
            if (workoutMeta.getWorkoutName().equals(sentWorkout.getWorkoutName())) {
                error.append("Workout with this name already exists.");
            }
        }
        Set<String> totalExercises = Sets.union(sentWorkoutExercises, ownedExercises);
        if (activeUserObject.getPremiumToken() == null
            && totalExercises.size() > Globals.MAX_FREE_EXERCISES) {
            error.append(
                "Accepting this workout would put you above the amount of free exercises allowed.");
        }
        if (activeUserObject.getPremiumToken() != null
            && totalExercises.size() > Globals.MAX_PREMIUM_EXERCISES) {
            error.append(
                "Accepting this workout would put you above the amount of exercises allowed.");
        }

        return error.toString().trim();
    }

    private void addNewExercises(final SentWorkout sentWorkout, final User user) {
        Set<String> sentWorkoutExercises = new HashSet<>();
        for (Integer week : sentWorkout.getRoutine()) {
            for (Integer day : sentWorkout.getRoutine().getWeek(week)) {
                for (SentExercise sentExercise : sentWorkout.getRoutine()
                    .getExerciseListForDay(week, day)) {
                    sentWorkoutExercises.add(sentExercise.getExerciseName());
                }
            }
        }
        Set<String> ownedExercises = new HashSet<>();
        for (String exerciseId : user.getOwnedExercises().keySet()) {
            ownedExercises.add(user.getOwnedExercises().get(exerciseId).getExerciseName());
        }
        Set<String> newExercises = Sets.difference(sentWorkoutExercises, ownedExercises);
        for (String exercise : newExercises) {
            // for each of the exercises that the user doesn't own, make a new entry for them in the owned mapping
            OwnedExercise ownedExercise = new OwnedExercise(
                sentWorkout.getExercises().get(exercise), exercise);
            String exerciseId = UUID.randomUUID().toString();
            user.getOwnedExercises().putIfAbsent(exerciseId, ownedExercise);
        }
    }
}
