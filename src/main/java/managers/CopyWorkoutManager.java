package managers;

import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import daos.UserDAO;
import daos.WorkoutDAO;
import exceptions.ManagerExecutionException;
import java.time.Instant;
import java.util.UUID;
import models.Routine;
import models.RoutineDay;
import models.RoutineExercise;
import models.RoutineWeek;
import utils.AttributeValueUtils;
import utils.Metrics;
import utils.UpdateItemTemplate;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import models.User;
import models.Workout;
import models.WorkoutMeta;
import responses.UserWithWorkout;
import utils.Validator;
import utils.WorkoutUtils;

public class CopyWorkoutManager {

    private final UserDAO userDAO;
    private final Metrics metrics;

    @Inject
    public CopyWorkoutManager(final UserDAO userDAO, final Metrics metrics) {
        this.userDAO = userDAO;
        this.metrics = metrics;
    }

    /**
     * Copies an old workout as a new workout - assuming all input is valid. Also syncs the old workout before
     * performing the copy.
     *
     * @param activeUser     username doing the copying.
     * @param newWorkoutName workout name for the copy of the old workout.
     * @param oldWorkout     workout that is being copied.
     * @return user with workout object that contains all the changed fields, as well as the new copied workout set as
     * current.
     */
    public UserWithWorkout copyWorkout(final String activeUser, final String newWorkoutName, final Workout oldWorkout)
        throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".copyWorkout";
        this.metrics.commonSetup(classMethod);

        try {
            final User activeUserObject = this.userDAO.getUser(activeUser);
            final String oldWorkoutId = oldWorkout.getWorkoutId();
            Validator.ensureWorkoutOwnership(activeUser, oldWorkout);

            final String errorMessage = Validator.validNewWorkoutInput(newWorkoutName, activeUserObject,
                oldWorkout.getRoutine());

            if (!errorMessage.isEmpty()) {
                this.metrics.commonClose(false);
                throw new ManagerExecutionException(errorMessage);
            }

            // copy the workout as a new one. Not using manager due to transactions
            final String workoutId = UUID.randomUUID().toString();
            final String creationTime = Instant.now().toString();
            final Workout newWorkout = new Workout();
            // remove any progress of the workout that is being copied
            Routine newRoutine = new Routine(oldWorkout.getRoutine());
            for (RoutineWeek week : newRoutine) {
                for (RoutineDay day : week) {
                    for (RoutineExercise exercise : day) {
                        exercise.setCompleted(false);
                    }
                }
            }
            newWorkout.setCreationDate(creationTime);
            newWorkout.setCreator(activeUser);
            newWorkout.setWorkoutId(workoutId);
            newWorkout.setWorkoutName(newWorkoutName.trim());
            newWorkout.setRoutine(newRoutine);
            newWorkout.setCurrentDay(0);
            newWorkout.setCurrentWeek(0);

            final WorkoutMeta workoutMeta = new WorkoutMeta();
            workoutMeta.setWorkoutName(newWorkoutName.trim());
            workoutMeta.setAverageExercisesCompleted(0.0);
            workoutMeta.setDateLast(creationTime);
            workoutMeta.setTimesCompleted(0);
            workoutMeta.setTotalExercisesSum(0);
            // need to set it here so frontend gets updated user item back
            activeUserObject.putNewWorkoutMeta(workoutId, workoutMeta);
            activeUserObject.setCurrentWorkout(workoutId);

            // update all the exercises that are now a part of this newly copied workout
            WorkoutUtils.updateOwnedExercises(activeUserObject, oldWorkout.getRoutine(), workoutId, newWorkoutName);

            // update user object with this newly copied workout
            UpdateItemTemplate updateUserItemData = new UpdateItemTemplate(activeUser, UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("set " +
                    User.CURRENT_WORKOUT + " = :currentWorkoutVal, " +
                    User.WORKOUTS + ".#newWorkoutId= :newWorkoutMeta, " +
                    User.EXERCISES + "= :exercisesMap")
                .withValueMap(new ValueMap()
                    .withString(":currentWorkoutVal", newWorkout.getWorkoutId())
                    .withMap(":newWorkoutMeta", workoutMeta.asMap())
                    .withMap(":exercisesMap", activeUserObject.getOwnedExercisesMap()))
                .withNameMap(new NameMap().with("#newWorkoutId", newWorkout.getWorkoutId()));

            // persist the current week/day/routine of the old workout
            UpdateItemTemplate updateOldWorkoutItemData = new UpdateItemTemplate(oldWorkoutId,
                WorkoutDAO.WORKOUT_TABLE_NAME)
                .withUpdateExpression("set " +
                    Workout.CURRENT_DAY + " = :currentDay, " +
                    Workout.CURRENT_WEEK + " = :currentWeek, " +
                    "#routine" + " =:routineValue")
                .withValueMap(new ValueMap()
                    .withNumber(":currentDay", oldWorkout.getCurrentDay())
                    .withNumber(":currentWeek", oldWorkout.getCurrentWeek())
                    .withMap(":routineValue", oldWorkout.getRoutine().asMap()))
                .withNameMap(new NameMap().with("#routine", Workout.ROUTINE));

            final List<TransactWriteItem> actions = new ArrayList<>();
            actions.add(new TransactWriteItem().withUpdate(updateUserItemData.asUpdate()));
            actions.add(new TransactWriteItem().withUpdate(updateOldWorkoutItemData.asUpdate()));
            actions.add(new TransactWriteItem()
                .withPut(new Put().withTableName(WorkoutDAO.WORKOUT_TABLE_NAME)
                    .withItem(AttributeValueUtils.convertMapToAttributeValueMap(newWorkout.asMap()))));
            this.userDAO.executeWriteTransaction(actions);

            this.metrics.commonClose(true);
            return new UserWithWorkout(activeUserObject, newWorkout);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
