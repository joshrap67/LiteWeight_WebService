package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import helpers.Metrics;
import helpers.UpdateItemData;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
    public UserWithWorkout execute(final String activeUser, final String newWorkoutId,
        final Workout oldWorkout) throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        try {
            String oldWorkoutId = oldWorkout.getWorkoutId();
            final User user = this.databaseAccess.getUser(activeUser);
            final Workout newWorkout = this.databaseAccess.getWorkout(newWorkoutId);

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

            this.metrics.commonClose(true);
            return new UserWithWorkout(user, newWorkout);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
