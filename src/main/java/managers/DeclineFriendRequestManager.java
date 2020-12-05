package managers;

import services.NotificationService;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import daos.UserDAO;
import exceptions.ManagerExecutionException;
import utils.Metrics;
import utils.UpdateItemTemplate;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import models.NotificationData;
import models.User;

public class DeclineFriendRequestManager {

    private final NotificationService notificationService;
    private final UserDAO userDAO;
    private final Metrics metrics;

    @Inject
    public DeclineFriendRequestManager(final NotificationService notificationService,
        final UserDAO userDAO,
        final Metrics metrics) {
        this.notificationService = notificationService;
        this.userDAO = userDAO;
        this.metrics = metrics;
    }

    /**
     * Declines a friend request and sends a notification to the user who was declined.
     *
     * @param activeUser   user that is declining the request.
     * @param declinedUser user whose request has been declined.
     */
    public void declineRequest(final String activeUser, final String declinedUser)
        throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".declineRequest";
        this.metrics.commonSetup(classMethod);

        try {
            final User activeUserObject = this.userDAO.getUser(activeUser);
            final User declinedUserObject = this.userDAO.getUser(declinedUser);

            if (!activeUserObject.getFriendRequests().containsKey(declinedUser)) {
                // sanity check to make sure that the friend request is still there
                this.metrics.commonClose(false);
                throw new ManagerExecutionException(
                    String.format("Friend request for %s no longer present.", declinedUser));
            }

            // remove friend from active user
            UpdateItemTemplate activeUserData = new UpdateItemTemplate(
                activeUser, UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("remove " + User.FRIEND_REQUESTS + ".#username")
                .withNameMap(new NameMap().with("#username", declinedUser));

            // remove the (unconfirmed) active user from friend's mapping
            UpdateItemTemplate updateFriendData = new UpdateItemTemplate(
                declinedUser, UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("remove " + User.FRIENDS + ".#username")
                .withNameMap(new NameMap().with("#username", activeUser));

            final List<TransactWriteItem> actions = new ArrayList<>();
            actions.add(new TransactWriteItem().withUpdate(activeUserData.asUpdate()));
            actions.add(new TransactWriteItem().withUpdate(updateFriendData.asUpdate()));

            this.userDAO.executeWriteTransaction(actions);
            // if this succeeds, go ahead and send a notification to the declined user (only need to send username)
            this.notificationService.sendMessage(declinedUserObject.getPushEndpointArn(),
                new NotificationData(NotificationService.declinedFriendRequestAction,
                    Maps.newHashMap(
                        ImmutableMap.<String, String>builder().put(User.USERNAME, activeUser)
                            .build())));

            this.metrics.commonClose(true);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }

    }
}
