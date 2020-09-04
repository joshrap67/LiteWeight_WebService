package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import helpers.AttributeValueHelper;
import helpers.ErrorMessage;
import helpers.JsonHelper;
import helpers.Metrics;
import helpers.ResultStatus;
import helpers.UpdateItemData;
import helpers.Validator;
import helpers.WorkoutHelper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import models.Routine;
import models.User;
import models.Workout;
import models.WorkoutUser;
import responses.UserWithWorkout;

public class CopyWorkoutManager {

    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;

    @Inject
    public CopyWorkoutManager(DatabaseAccess databaseAccess, Metrics metrics) {
        this.databaseAccess = databaseAccess;
        this.metrics = metrics;
    }

    /**
     * @param activeUser Username of new user to be inserted
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public ResultStatus<String> execute(final String activeUser, final String newWorkoutName,
        final Map<String, Object> oldWorkoutJson) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;

        try {
            Workout oldWorkout = new Workout(oldWorkoutJson);
            String oldWorkoutId = oldWorkout.getWorkoutId();
            User user = this.databaseAccess.getUser(activeUser);

            Workout newWorkout = copyWorkout(oldWorkout, newWorkoutName, user);

            if (newWorkout != null) {
                final String creationTimeNew = Instant.now().toString();
                WorkoutUser workoutMetaNew = user.getUserWorkouts().get(newWorkout.getWorkoutId());
                workoutMetaNew.setDateLast(creationTimeNew);

                // update user object with new access time of the newly selected workout
                final UpdateItemData updateUserItemData = new UpdateItemData(activeUser,
                    DatabaseAccess.USERS_TABLE_NAME)
                    .withUpdateExpression(
                        "set " +
                            User.CURRENT_WORKOUT + " = :" + User.CURRENT_WORKOUT + ", " +
                            User.WORKOUTS + ".#newWorkoutId= :newWorkoutMeta")
                    .withValueMap(
                        new ValueMap()
                            .withString(":" + User.CURRENT_WORKOUT, newWorkout.getWorkoutId())
                            .withMap(":newWorkoutMeta", workoutMetaNew.asMap()))
                    .withNameMap(new NameMap()
                        .with("#newWorkoutId", newWorkout.getWorkoutId()));

                // persist the current week/day/routine of the old workout
                final UpdateItemData updateOldWorkoutItemData = new UpdateItemData(oldWorkoutId,
                    DatabaseAccess.WORKOUT_TABLE_NAME)
                    .withUpdateExpression(
                        "set " +
                            Workout.CURRENT_DAY + " = :" + Workout.CURRENT_DAY + ", " +
                            Workout.CURRENT_WEEK + " = :" + Workout.CURRENT_WEEK + ", " +
                            "#routine" + " =:" + Workout.ROUTINE)
                    .withValueMap(
                        new ValueMap()
                            .withNumber(":" + Workout.CURRENT_DAY, oldWorkout.getCurrentDay())
                            .withNumber(":" + Workout.CURRENT_WEEK, oldWorkout.getCurrentWeek())
                            .withMap(":" + Workout.ROUTINE, oldWorkout.getRoutine().asMap()))
                    .withNameMap(new NameMap()
                        .with("#routine", Workout.ROUTINE));

                // want a transaction since more than one object is being updated at once
                final List<TransactWriteItem> actions = new ArrayList<>();
                actions.add(new TransactWriteItem().withUpdate(updateUserItemData.asUpdate()));
                actions
                    .add(new TransactWriteItem().withUpdate(updateOldWorkoutItemData.asUpdate()));

                this.databaseAccess.executeWriteTransaction(actions);
                resultStatus = ResultStatus.successful(JsonHelper.serializeMap(
                    new UserWithWorkout(user, newWorkout).asMap()));
            } else {
                this.metrics
                    .log("Cannot copy workout.");
                resultStatus = ResultStatus.failureBadEntity("Workout could not be copied.");
            }

        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod);
        }

        this.metrics.commonClose(resultStatus.responseCode);
        return resultStatus;
    }


    private Workout copyWorkout(Workout oldWorkout, String newWorkoutName, User user)
        throws Exception {
        Workout newWorkout = null;

        final String workoutId = UUID.randomUUID().toString();
        final String creationTime = Instant.now().toString();
        final Routine routine = new Routine(oldWorkout.getRoutine().asMap());
        routine.resetAllExercises(); // in case the passed in workout had already been worked on
        String errorMessage = Validator.validNewWorkoutInput(newWorkoutName, user, routine);

        if (errorMessage == null) {
            // no error, so go ahead and try and insert this new workout along with updating active user
            newWorkout = new Workout();
            newWorkout.setCreationDate(creationTime);
            newWorkout.setCreator(user.getUsername());
            newWorkout.setMostFrequentFocus(oldWorkout.getMostFrequentFocus());
            newWorkout.setWorkoutId(workoutId);
            newWorkout.setWorkoutName(newWorkoutName.trim());
            newWorkout.setRoutine(routine);
            newWorkout.setCurrentDay(0);
            newWorkout.setCurrentWeek(0);

            final WorkoutUser workoutUser = new WorkoutUser();
            workoutUser.setWorkoutName(newWorkoutName.trim());
            workoutUser.setAverageExercisesCompleted(0.0);
            workoutUser.setDateLast(creationTime);
            workoutUser.setTimesCompleted(0);
            workoutUser.setTotalExercisesSum(0);
            // need to set it here so frontend gets updated user item back
            user.setUserWorkouts(workoutId, workoutUser);

            // update all the exercises that are now apart of this workout
            WorkoutHelper.updateUserExercises(user, routine, workoutId, newWorkoutName);

            final UpdateItemData updateItemData = new UpdateItemData(user.getUsername(),
                DatabaseAccess.USERS_TABLE_NAME)
                .withUpdateExpression(
                    "set " +
                        User.CURRENT_WORKOUT + " = :" + User.CURRENT_WORKOUT + ", " +
                        User.WORKOUTS + ".#workoutId= :" + User.WORKOUTS + ", " +
                        User.EXERCISES + "= :" + User.EXERCISES)
                .withValueMap(
                    new ValueMap()
                        .withString(":" + User.CURRENT_WORKOUT, workoutId)
                        .withMap(":" + User.WORKOUTS, workoutUser.asMap())
                        .withMap(":" + User.EXERCISES, user.getUserExercisesMap()))
                .withNameMap(new NameMap()
                    .with("#workoutId", workoutId));

            // want a transaction since more than one object is being updated at once
            final List<TransactWriteItem> actions = new ArrayList<>();
            actions.add(new TransactWriteItem().withUpdate(updateItemData.asUpdate()));
            actions.add(new TransactWriteItem()
                .withPut(
                    new Put().withTableName(DatabaseAccess.WORKOUT_TABLE_NAME).withItem(
                        AttributeValueHelper
                            .convertMapToAttributeValueMap(newWorkout.asMap()))));

            this.databaseAccess.executeWriteTransaction(actions);
        }
        return newWorkout;
    }
}
