package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import exceptions.UserNotFoundException;
import exceptions.WorkoutNotFoundException;
import helpers.ErrorMessage;
import helpers.JsonHelper;
import helpers.Metrics;
import helpers.ResultStatus;
import helpers.UpdateItemData;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import models.User;
import models.Workout;
import models.WorkoutUser;
import responses.UserWithWorkout;

public class SwitchWorkoutManager {

    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;

    @Inject
    public SwitchWorkoutManager(final DatabaseAccess databaseAccess, final Metrics metrics) {
        this.databaseAccess = databaseAccess;
        this.metrics = metrics;
    }

    /**
     * @param activeUser Username of new user to be inserted
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public ResultStatus<String> execute(final String activeUser, final String newWorkoutId,
        final Workout oldWorkout) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;
        try {
            String oldWorkoutId = oldWorkout.getWorkoutId();
            final User user = Optional.ofNullable(this.databaseAccess.getUser(activeUser))
                .orElseThrow(
                    () -> new UserNotFoundException(String.format("%s not found", activeUser)));
            final Workout newWorkout = Optional
                .ofNullable(this.databaseAccess.getWorkout(newWorkoutId))
                .orElseThrow(
                    () -> new WorkoutNotFoundException(
                        String.format("Workout with ID %s not found", newWorkoutId)));

            final String creationTimeNew = Instant.now().toString();
            final WorkoutUser workoutMetaNew = user.getUserWorkouts().get(newWorkoutId);
            workoutMetaNew.setDateLast(creationTimeNew);

            // update user object with new access time of the newly selected workout
            final UpdateItemData updateUserItemData = new UpdateItemData(activeUser,
                DatabaseAccess.USERS_TABLE_NAME)
                .withUpdateExpression("set " +
                    User.CURRENT_WORKOUT + " = :currentWorkoutVal, " +
                    User.WORKOUTS + ".#newWorkoutId= :newWorkoutMeta")
                .withValueMap(new ValueMap()
                    .withString(":currentWorkoutVal", newWorkoutId)
                    .withMap(":newWorkoutMeta", workoutMetaNew.asMap()))
                .withNameMap(new NameMap().with("#newWorkoutId", newWorkoutId));

            // persist the current week/day/routine of the old workout
            final UpdateItemData updateOldWorkoutItemData = new UpdateItemData(oldWorkoutId,
                DatabaseAccess.WORKOUT_TABLE_NAME)
                .withUpdateExpression("set " +
                    Workout.CURRENT_DAY + " = :currentDayVal, " +
                    Workout.CURRENT_WEEK + " = :currentWeekVal, " +
                    "#routine =:routineVal")
                .withValueMap(new ValueMap()
                    .withNumber(":currentDayVal", oldWorkout.getCurrentDay())
                    .withNumber(":currentWeekVal", oldWorkout.getCurrentWeek())
                    .withMap(":routineVal", oldWorkout.getRoutine().asMap()))
                .withNameMap(new NameMap().with("#routine", Workout.ROUTINE));

            // want a transaction since more than one object is being updated at once
            final List<TransactWriteItem> actions = new ArrayList<>();
            actions.add(new TransactWriteItem().withUpdate(updateUserItemData.asUpdate()));
            actions.add(new TransactWriteItem().withUpdate(updateOldWorkoutItemData.asUpdate()));
            this.databaseAccess.executeWriteTransaction(actions);

            resultStatus = ResultStatus.successful(JsonHelper.serializeMap(
                new UserWithWorkout(user, newWorkout).asMap()));
        } catch (WorkoutNotFoundException | UserNotFoundException exception) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, exception));
            resultStatus = ResultStatus.failureBadEntity(exception.getMessage());
        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod);
        }

        this.metrics.commonClose(resultStatus.success);
        return resultStatus;
    }
}
