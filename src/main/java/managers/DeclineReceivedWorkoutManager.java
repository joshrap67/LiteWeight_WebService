package managers;

import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import daos.UserDAO;
import exceptions.ManagerExecutionException;
import helpers.Metrics;
import helpers.UpdateItemData;
import javax.inject.Inject;
import models.User;

public class DeclineReceivedWorkoutManager {

    private final UserDAO userDAO;
    private final Metrics metrics;

    @Inject
    public DeclineReceivedWorkoutManager(final UserDAO userDAO, final Metrics metrics) {
        this.userDAO = userDAO;
        this.metrics = metrics;
    }

    /**
     * This method gets the active user's data. If the active user's data does not exist, we assume
     * this is their first login and we enter a new user object in the db.
     *
     * @param activeUser The user that made the api request, trying to get data about themselves.
     */
    public void declineWorkout(final String activeUser, final String declinedWorkoutId)
        throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".declineWorkout";
        this.metrics.commonSetup(classMethod);

        try {
            final User activeUserObject = this.userDAO.getUser(activeUser);

            if (!activeUserObject.getReceivedWorkouts().containsKey(declinedWorkoutId)) {
                // sanity check to make sure that the friend request is still there
                this.metrics.commonClose(false);
                throw new ManagerExecutionException(
                    String.format("Received workout with id %s no longer present.",
                        declinedWorkoutId));
            }

            // remove workout from active user
            final UpdateItemData activeUserData = new UpdateItemData(
                activeUser, UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("remove " + User.RECEIVED_WORKOUTS + ".#workoutId")
                .withNameMap(new NameMap().with("#workoutId", declinedWorkoutId));

            this.userDAO.updateUser(activeUser, activeUserData.asUpdateItemSpec());

            this.metrics.commonClose(true);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
