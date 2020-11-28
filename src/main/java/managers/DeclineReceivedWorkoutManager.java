package managers;

import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import daos.SharedWorkoutDAO;
import daos.UserDAO;
import exceptions.ManagerExecutionException;
import utils.Metrics;
import utils.UpdateItemData;
import java.util.ArrayList;
import java.util.List;
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

            // remove workout from sent workout table
            UpdateItemData updateSentWorkoutData = new UpdateItemData(
                declinedWorkoutId, SharedWorkoutDAO.SENT_WORKOUT_TABLE_NAME);

            // remove workout from active user
            UpdateItemData activeUserData = new UpdateItemData(
                activeUser, UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("remove " + User.RECEIVED_WORKOUTS + ".#workoutId")
                .withNameMap(new NameMap().with("#workoutId", declinedWorkoutId));

            List<TransactWriteItem> actions = new ArrayList<>();
            actions.add(new TransactWriteItem().withUpdate(activeUserData.asUpdate()));
            actions.add(new TransactWriteItem().withDelete(updateSentWorkoutData.asDelete()));
            this.userDAO.executeWriteTransaction(actions);

            this.metrics.commonClose(true);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
