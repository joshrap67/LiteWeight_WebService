package managers;

import services.NotificationService;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import daos.UserDAO;
import utils.Metrics;
import utils.UpdateItemTemplate;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import models.NotificationData;
import models.User;

public class CancelFriendRequestManager {

    private final NotificationService notificationService;
    private final UserDAO userDAO;
    private final Metrics metrics;

    @Inject
    public CancelFriendRequestManager(final NotificationService notificationService,
        final UserDAO userDAO,
        final Metrics metrics) {
        this.notificationService = notificationService;
        this.userDAO = userDAO;
        this.metrics = metrics;
    }

    /**
     * Cancels a friend request and then sends a notification to the user who was canceled.
     *
     * @param activeUser       user that is canceling the request.
     * @param usernameToCancel user that is being canceled by active user.
     */
    public void cancelRequest(final String activeUser, final String usernameToCancel)
        throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".cancelRequest";
        this.metrics.commonSetup(classMethod);

        try {
            final User userToCancel = this.userDAO.getUser(usernameToCancel);

            // for canceled user, remove the friend request
            UpdateItemTemplate updateFriendData = new UpdateItemTemplate(
                usernameToCancel, UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("remove " + User.FRIEND_REQUESTS + ".#username")
                .withNameMap(new NameMap().with("#username", activeUser));
            // for active user, remove the (unconfirmed) user from their friends mapping
            UpdateItemTemplate updateActiveUserData = new UpdateItemTemplate(
                activeUser, UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("remove " + User.FRIENDS + ".#username")
                .withNameMap(new NameMap().with("#username", usernameToCancel));

            final List<TransactWriteItem> actions = new ArrayList<>();
            actions.add(new TransactWriteItem().withUpdate(updateFriendData.asUpdate()));
            actions.add(new TransactWriteItem().withUpdate(updateActiveUserData.asUpdate()));

            this.userDAO.executeWriteTransaction(actions);
            // if this succeeds, go ahead and send a notification to the canceled user (only need to send username)
            this.notificationService.sendMessage(userToCancel.getPushEndpointArn(),
                new NotificationData(NotificationService.canceledFriendRequestAction,
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
