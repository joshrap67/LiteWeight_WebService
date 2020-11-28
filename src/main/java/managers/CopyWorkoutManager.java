package managers;

import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import daos.UserDAO;
import daos.WorkoutDAO;
import utils.Metrics;
import utils.UpdateItemData;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import models.User;
import models.Workout;
import models.WorkoutMeta;
import responses.UserWithWorkout;

public class CopyWorkoutManager {

    private final UserDAO userDAO;
    private final Metrics metrics;
    private final NewWorkoutManager newWorkoutManager;

    @Inject
    public CopyWorkoutManager(final UserDAO userDAO, final Metrics metrics,
        final NewWorkoutManager newWorkoutManager) {
        this.userDAO = userDAO;
        this.metrics = metrics;
        this.newWorkoutManager = newWorkoutManager;
    }

    /**
     * @param activeUser Username of new user to be inserted
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public UserWithWorkout copyWorkout(final String activeUser, final String newWorkoutName,
        final Workout oldWorkout) throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".copyWorkout";
        this.metrics.commonSetup(classMethod);

        try {
            final String oldWorkoutId = oldWorkout.getWorkoutId();

            // todo this should be part of the transaction below to satisfy ACID?
            final UserWithWorkout userWithWorkout = newWorkoutManager
                .createNewWorkout(activeUser, newWorkoutName, oldWorkout.getRoutine());

            final Workout newWorkout = userWithWorkout.getWorkout();
            final WorkoutMeta workoutMetaNew = userWithWorkout.getUser().getUserWorkouts()
                .get(newWorkout.getWorkoutId());

            // update user object with new access time of the newly selected workout
            final UpdateItemData updateUserItemData = new UpdateItemData(activeUser,
                UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("set " +
                    User.CURRENT_WORKOUT + " = :currentWorkout, " +
                    User.WORKOUTS + ".#newWorkoutId= :newWorkoutMeta")
                .withValueMap(new ValueMap()
                    .withString(":currentWorkout", newWorkout.getWorkoutId())
                    .withMap(":newWorkoutMeta", workoutMetaNew.asMap()))
                .withNameMap(new NameMap().with("#newWorkoutId", newWorkout.getWorkoutId()));

            // persist the current week/day/routine of the old workout
            final UpdateItemData updateOldWorkoutItemData = new UpdateItemData(oldWorkoutId,
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
            this.userDAO.executeWriteTransaction(actions);

            this.metrics.commonClose(true);
            return userWithWorkout;
        } catch (Exception e) {
            this.metrics.commonClose(true);
            throw e;
        }
    }
}
