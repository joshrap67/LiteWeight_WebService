package managers;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import daos.UserDAO;
import exceptions.InvalidAttributeException;
import exceptions.UserNotFoundException;
import utils.Metrics;
import javax.inject.Inject;
import models.SharedWorkoutMeta;
import models.User;

public class SetReceivedWorkoutSeenManager {

    private final UserDAO userDAO;
    private final Metrics metrics;

    @Inject
    public SetReceivedWorkoutSeenManager(final UserDAO userDAO, final Metrics metrics) {
        this.userDAO = userDAO;
        this.metrics = metrics;
    }

    /**
     * Sets a specific received workout to be seen.
     *
     * @param activeUser user whose requests are being set to seen.
     * @throws InvalidAttributeException if error with user item.
     * @throws UserNotFoundException     if active user is not found.
     */
    public void setReceivedWorkoutSeen(final String activeUser, final String workoutId)
        throws InvalidAttributeException, UserNotFoundException {
        final String classMethod = this.getClass().getSimpleName() + ".setReceivedWorkoutSeen";
        this.metrics.commonSetup(classMethod);

        try {
            final User user = this.userDAO.getUser(activeUser);
            SharedWorkoutMeta workoutMeta = user.getReceivedWorkouts().get(workoutId);
            if (workoutMeta.isSeen()) {
                this.metrics.commonClose(true);
                return;
            }
            workoutMeta.setSeen(true);

            UpdateItemSpec updateActiveUserData = new UpdateItemSpec()
                .withUpdateExpression("set " + User.RECEIVED_WORKOUTS + ".#workoutId=:receivedWorkoutVal")
                .withValueMap(new ValueMap().withMap(":receivedWorkoutVal", workoutMeta.asMap()))
                .withNameMap(new NameMap().with("#workoutId", workoutId));
            this.userDAO.updateUser(activeUser, updateActiveUserData);

            this.metrics.commonClose(true);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
