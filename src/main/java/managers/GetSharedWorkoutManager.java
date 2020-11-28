package managers;

import daos.SharedWorkoutDAO;
import daos.UserDAO;
import exceptions.ManagerExecutionException;
import models.User;
import utils.Metrics;
import javax.inject.Inject;
import models.SharedWorkout;

public class GetSharedWorkoutManager {

    private final SharedWorkoutDAO sharedWorkoutDAO;
    private final Metrics metrics;
    private final UserDAO userDAO;


    @Inject
    public GetSharedWorkoutManager(final SharedWorkoutDAO sharedWorkoutDAO, final Metrics metrics,
        final UserDAO userDAO) {
        this.sharedWorkoutDAO = sharedWorkoutDAO;
        this.metrics = metrics;
        this.userDAO = userDAO;
    }

    /**
     * Returns a shared workout. Extra validation is done to ensure the user is the correct
     * recipient.
     *
     * @param activeUser user that is attempting to fetch this shared workout.
     * @param workoutId  id of the workout the user is attempting to fetch.
     * @return the shared workout for the given workoutId
     */
    public SharedWorkout getSharedWorkout(final String activeUser, final String workoutId)
        throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".getSharedWorkout";
        this.metrics.commonSetup(classMethod);

        try {
            User activeUserObject = this.userDAO.getUser(activeUser);
            if (!activeUserObject.getReceivedWorkouts().containsKey(workoutId)) {
                this.metrics.commonClose(false);
                throw new ManagerExecutionException("User is not a recipient for this workout.");
            }

            this.metrics.commonClose(true);
            return this.sharedWorkoutDAO.getSharedWorkout(workoutId);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
