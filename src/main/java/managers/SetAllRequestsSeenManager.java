package managers;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import daos.UserDAO;
import exceptions.InvalidAttributeException;
import exceptions.UserNotFoundException;
import utils.Metrics;
import javax.inject.Inject;
import models.User;

public class SetAllRequestsSeenManager {

    private final UserDAO userDAO;
    private final Metrics metrics;

    @Inject
    public SetAllRequestsSeenManager(final UserDAO userDAO, final Metrics metrics) {
        this.userDAO = userDAO;
        this.metrics = metrics;
    }

    /**
     * Loops through all the friend requests of a user and sets them to seen.
     *
     * @param activeUser user whose requests are being set to seen.
     * @throws InvalidAttributeException if error with user item.
     * @throws UserNotFoundException     if active user is not found.
     */
    public void setAllRequestsSeen(final String activeUser)
        throws InvalidAttributeException, UserNotFoundException {
        final String classMethod = this.getClass().getSimpleName() + ".setAllRequestsSeen";
        this.metrics.commonSetup(classMethod);

        try {
            final User user = this.userDAO.getUser(activeUser);

            for (String username : user.getFriendRequests().keySet()) {
                user.getFriendRequests().get(username).setSeen(true);
            }

            final UpdateItemSpec updateActiveUserData = new UpdateItemSpec()
                .withUpdateExpression("set " + User.FRIEND_REQUESTS + "=:friendRequestsVal")
                .withValueMap(
                    new ValueMap().withMap(":friendRequestsVal", user.getFriendRequestsMap()));
            this.userDAO.updateUser(activeUser, updateActiveUserData);

            this.metrics.commonClose(false);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
