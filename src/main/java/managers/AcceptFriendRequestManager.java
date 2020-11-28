package managers;

import services.NotificationService;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import daos.UserDAO;
import exceptions.ManagerExecutionException;
import imports.Globals;
import utils.Metrics;
import utils.UpdateItemData;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import models.Friend;
import models.NotificationData;
import models.User;

public class AcceptFriendRequestManager {

    private final NotificationService notificationService;
    private final UserDAO userDAO;
    private final Metrics metrics;

    @Inject
    public AcceptFriendRequestManager(final NotificationService notificationService, final UserDAO userDAO,
        final Metrics metrics) {
        this.notificationService = notificationService;
        this.userDAO = userDAO;
        this.metrics = metrics;
    }

    /**
     * Accepts a friend request and adds the accepted user to the friends list of the active user.
     * Upon success, a data notification is sent to the accepted user.
     *
     * @param activeUser       username of the user that is accepting the friend request.
     * @param usernameToAccept username of the user that the active user is accepting.
     * @throws Exception if there are any input errors or if either user does not exist.
     */
    public void acceptRequest(final String activeUser, final String usernameToAccept)
        throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".acceptRequest";
        this.metrics.commonSetup(classMethod);

        try {
            final User activeUserObject = this.userDAO.getUser(activeUser);
            final User userToAccept = this.userDAO.getUser(usernameToAccept);

            if (!activeUserObject.getFriendRequests().containsKey(usernameToAccept)) {
                this.metrics.commonClose(false);
                throw new ManagerExecutionException(
                    String.format("User %s no longer has this friend request.", usernameToAccept));
            }
            if (activeUserObject.getFriends().size() >= Globals.MAX_NUMBER_FRIENDS) {
                // sanity check to make sure that the request is still there
                this.metrics.commonClose(false);
                throw new ManagerExecutionException("Max number of friends reached.");
            }

            // remove request from active user and add the new friend
            Friend newFriend = new Friend(userToAccept, true);
            final UpdateItemData activeUserData = new UpdateItemData(
                activeUser, UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("set " + User.FRIENDS + ".#username = :friendVal " +
                    "remove " + User.FRIEND_REQUESTS + ".#username")
                .withNameMap(new NameMap().with("#username", usernameToAccept))
                .withValueMap(new ValueMap().withMap(":friendVal", newFriend.asMap()));

            // update the active user to be confirmed in the newly accepted friends mapping
            final UpdateItemData updateFriendData = new UpdateItemData(
                usernameToAccept, UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression(
                    "set " + User.FRIENDS + ".#username." + Friend.CONFIRMED + " = :confirmedVal")
                .withNameMap(new NameMap().with("#username", activeUser))
                .withValueMap(new ValueMap().withBoolean(":confirmedVal", true));

            // want a transaction since more than one object is being updated at once
            final List<TransactWriteItem> actions = new ArrayList<>();
            actions.add(new TransactWriteItem().withUpdate(activeUserData.asUpdate()));
            actions.add(new TransactWriteItem().withUpdate(updateFriendData.asUpdate()));
            this.userDAO.executeWriteTransaction(actions);

            // if this succeeds, go ahead and send a notification to the accepted user (only need to send username)
            this.notificationService.sendMessage(userToAccept.getPushEndpointArn(),
                new NotificationData(NotificationService.acceptedFriendRequestAction,
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
