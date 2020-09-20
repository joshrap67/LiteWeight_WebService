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
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import models.User;
import models.Workout;
import models.WorkoutUser;
import responses.UserWithWorkout;

public class CopyWorkoutManager {

    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;
    private final NewWorkoutManager newWorkoutManager;

    @Inject
    public CopyWorkoutManager(DatabaseAccess databaseAccess, Metrics metrics,
        NewWorkoutManager newWorkoutManager) {
        this.databaseAccess = databaseAccess;
        this.metrics = metrics;
        this.newWorkoutManager = newWorkoutManager;
    }

    /**
     * @param activeUser Username of new user to be inserted
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public ResultStatus<String> execute(final String activeUser, final String newWorkoutName,
        final Workout oldWorkout) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;

        try {
            final String oldWorkoutId = oldWorkout.getWorkoutId();

            ResultStatus<String> copyResult = newWorkoutManager
                .execute(activeUser, newWorkoutName, oldWorkout.getRoutine());

            if (copyResult.success) {
                final UserWithWorkout userWithWorkout = new UserWithWorkout(
                    JsonHelper.deserialize(copyResult.resultMessage));
                final Workout newWorkout = userWithWorkout.getWorkout();
                final WorkoutUser workoutMetaNew = userWithWorkout.getUser().getUserWorkouts()
                    .get(newWorkout.getWorkoutId());

                // update user object with new access time of the newly selected workout
                final UpdateItemData updateUserItemData = new UpdateItemData(activeUser,
                    DatabaseAccess.USERS_TABLE_NAME)
                    .withUpdateExpression(
                        "set " +
                            User.CURRENT_WORKOUT + " = :currentWorkout" + ", " +
                            User.WORKOUTS + ".#newWorkoutId= :newWorkoutMeta")
                    .withValueMap(
                        new ValueMap()
                            .withString(":currentWorkout", newWorkout.getWorkoutId())
                            .withMap(":newWorkoutMeta", workoutMetaNew.asMap()))
                    .withNameMap(new NameMap().with("#newWorkoutId", newWorkout.getWorkoutId()));

                // persist the current week/day/routine of the old workout
                final UpdateItemData updateOldWorkoutItemData = new UpdateItemData(oldWorkoutId,
                    DatabaseAccess.WORKOUT_TABLE_NAME)
                    .withUpdateExpression(
                        "set " +
                            Workout.CURRENT_DAY + " = :currentDay" + ", " +
                            Workout.CURRENT_WEEK + " = :currentWeek" + ", " +
                            "#routine" + " =:routineValue")
                    .withValueMap(
                        new ValueMap()
                            .withNumber(":currentDay", oldWorkout.getCurrentDay())
                            .withNumber(":currentWeek", oldWorkout.getCurrentWeek())
                            .withMap(":routineValue", oldWorkout.getRoutine().asMap()))
                    .withNameMap(new NameMap().with("#routine", Workout.ROUTINE));

                // want a transaction since more than one object is being updated at once
                final List<TransactWriteItem> actions = new ArrayList<>();
                actions.add(new TransactWriteItem().withUpdate(updateUserItemData.asUpdate()));
                actions
                    .add(new TransactWriteItem().withUpdate(updateOldWorkoutItemData.asUpdate()));

                this.databaseAccess.executeWriteTransaction(actions);
                resultStatus = ResultStatus
                    .successful(JsonHelper.serializeMap(userWithWorkout.asMap()));
            } else {
                this.metrics.log("Cannot copy workout." + copyResult.getResultMessage());
                resultStatus = ResultStatus.failureBadEntity("Workout could not be copied.");
            }

        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod);
        }

        this.metrics.commonClose(resultStatus.success);
        return resultStatus;
    }
}
