package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import helpers.ErrorMessage;
import helpers.JsonHelper;
import helpers.Metrics;
import helpers.ResultStatus;
import helpers.UpdateItemData;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import models.User;
import models.Workout;
import models.WorkoutUser;
import responses.UserWithWorkout;

public class SwitchWorkoutManager {

    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;

    @Inject
    public SwitchWorkoutManager(DatabaseAccess databaseAccess, Metrics metrics) {
        this.databaseAccess = databaseAccess;
        this.metrics = metrics;
    }

    /**
     * @param activeUser Username of new user to be inserted
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public ResultStatus<String> execute(final String activeUser, final String newWorkoutId,
        final Map<String, Object> oldWorkoutJson) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;

        try {
            Workout oldWorkout = new Workout(oldWorkoutJson);
            String oldWorkoutId = oldWorkout.getWorkoutId();
            Workout newWorkout = this.databaseAccess.getWorkout(newWorkoutId);

            if (newWorkout != null) {
                User user = this.databaseAccess.getUser(activeUser);
                final String creationTimeNew = Instant.now().toString();
                WorkoutUser workoutMetaNew = user.getUserWorkouts().get(newWorkoutId);
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
                            .withString(":" + User.CURRENT_WORKOUT, newWorkoutId)
                            .withMap(":newWorkoutMeta", workoutMetaNew.asMap()))
                    .withNameMap(new NameMap()
                        .with("#newWorkoutId", newWorkoutId));

                // persist the current week/day/routine of the old workout
                final UpdateItemData updateOldWorkoutItemData = new UpdateItemData(oldWorkoutId,
                    DatabaseAccess.WORKOUT_TABLE_NAME)
                    .withUpdateExpression(
                        "set " +
                            Workout.CURRENT_DAY + " = :" + Workout.CURRENT_DAY + ", " +
                            Workout.CURRENT_WEEK + " = :" + Workout.CURRENT_WEEK + ", " +
                            "#routine =:" + Workout.ROUTINE)
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
                resultStatus = ResultStatus.successful(JsonHelper.convertObjectToJson(
                    new UserWithWorkout(user, newWorkout).asMap()));
            } else {
                this.metrics.log(String.format("Workout with id %s not in database", newWorkoutId));
                resultStatus = ResultStatus.failure("Workout could not be loaded.");
            }

        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failure("Exception in " + classMethod);
        }

        this.metrics.commonClose(resultStatus.success);
        return resultStatus;
    }
}
