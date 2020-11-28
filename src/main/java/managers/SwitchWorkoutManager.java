package managers;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import daos.UserDAO;
import daos.WorkoutDAO;
import utils.Metrics;
import java.time.Instant;
import javax.inject.Inject;
import models.User;
import models.Workout;
import models.WorkoutMeta;
import responses.UserWithWorkout;

public class SwitchWorkoutManager {

    private final UserDAO userDAO;
    private final WorkoutDAO workoutDAO;
    private final Metrics metrics;
    private final SyncWorkoutManager syncWorkoutManager;

    @Inject
    public SwitchWorkoutManager(final UserDAO userDAO, final WorkoutDAO workoutDAO,
        final Metrics metrics, final SyncWorkoutManager syncWorkoutManager) {
        this.userDAO = userDAO;
        this.workoutDAO = workoutDAO;
        this.metrics = metrics;
        this.syncWorkoutManager = syncWorkoutManager;
    }

    /**
     * Switches to the workout that the user passes in. The user's current workout (oldWorkout) is
     * synced before switching to the new workout.
     *
     * @param activeUser   user that is attempting to switch workouts.
     * @param newWorkoutId id of the workout to be switched to.
     * @param oldWorkout   the old workout the user is switching from.
     * @return UserWithWorkout has the newly switched workout and the user object updated with the
     * new current workout.
     * @throws Exception if user/workout does not exist.
     */
    public UserWithWorkout switchWorkout(final String activeUser, final String newWorkoutId,
        final Workout oldWorkout) throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".switchWorkout";
        this.metrics.commonSetup(classMethod);

        try {
            final User user = this.userDAO.getUser(activeUser);
            final Workout newWorkout = this.workoutDAO.getWorkout(newWorkoutId);

            user.setCurrentWorkout(newWorkoutId);
            final String timeNow = Instant.now().toString();
            final WorkoutMeta workoutMetaNew = user.getUserWorkouts().get(newWorkoutId);
            workoutMetaNew.setDateLast(timeNow);

            // update user object with new access time of the newly selected workout
            final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withUpdateExpression("set " +
                    User.CURRENT_WORKOUT + " = :currentWorkoutVal, " +
                    User.WORKOUTS + ".#newWorkoutId= :newWorkoutMeta")
                .withValueMap(new ValueMap()
                    .withString(":currentWorkoutVal", newWorkoutId)
                    .withMap(":newWorkoutMeta", workoutMetaNew.asMap()))
                .withNameMap(new NameMap().with("#newWorkoutId", newWorkoutId));

            // persist the current week/day/routine of the old workout
            this.syncWorkoutManager.syncWorkout(oldWorkout);
            this.userDAO.updateUser(activeUser, updateItemSpec);

            this.metrics.commonClose(true);
            return new UserWithWorkout(user, newWorkout);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
