package managers;

import services.NotificationService;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import daos.UserDAO;
import exceptions.ManagerExecutionException;
import imports.Globals;
import utils.Metrics;
import utils.UpdateItemTemplate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import models.Friend;
import models.FriendRequest;
import models.NotificationData;
import models.User;
import responses.FriendRequestResponse;
import responses.FriendResponse;

public class SendFriendRequestManager {

    private final NotificationService notificationService;
    private final UserDAO userDAO;
    private final Metrics metrics;

    @Inject
    public SendFriendRequestManager(final NotificationService notificationService,
        final UserDAO userDAO,
        final Metrics metrics) {
        this.notificationService = notificationService;
        this.userDAO = userDAO;
        this.metrics = metrics;
    }

    /**
     * Sends a friend request to the desired user if all input is valid and the max number of friends for both have not
     * been reached.
     *
     * @param activeUser    user that is sending the friend request.
     * @param usernameToAdd user that the active user is adding.
     * @return FriendResponse sent back to the client containing the usernameToAdd's icon
     * @throws Exception if either user does not exist or if there is any input error.
     */
    public FriendResponse sendRequest(final String activeUser, final String usernameToAdd)
        throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".sendRequest";
        this.metrics.commonSetup(classMethod);

        try {
            final User activeUserObject = this.userDAO.getUser(activeUser);
            final User userToAdd = this.userDAO.getUser(usernameToAdd);

            String errorMessage = validConditions(activeUserObject, userToAdd);
            if (!errorMessage.isEmpty()) {
                this.metrics.commonClose(false);
                throw new ManagerExecutionException(errorMessage);
            }

            Friend friendToAdd = new Friend(userToAdd, false);
            FriendRequest friendRequest = new FriendRequest(activeUserObject, Instant.now().toString());

            // the active user needs to have this (unconfirmed) friend added to its friends list
            UpdateItemTemplate updateFriendData = new UpdateItemTemplate(usernameToAdd, UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("set " + User.FRIEND_REQUESTS + ".#username= :requestsVal")
                .withValueMap(new ValueMap().withMap(":requestsVal", friendRequest.asMap()))
                .withNameMap(new NameMap().with("#username", activeUser));
            // friend to add needs to have the friend request added to its friend request list
            UpdateItemTemplate updateActiveUserData = new UpdateItemTemplate(activeUser, UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("set " + User.FRIENDS + ".#username= :friendsVal")
                .withValueMap(new ValueMap().withMap(":friendsVal", friendToAdd.asMap()))
                .withNameMap(new NameMap().with("#username", usernameToAdd));

            final List<TransactWriteItem> actions = new ArrayList<>();
            actions.add(new TransactWriteItem().withUpdate(updateFriendData.asUpdate()));
            actions.add(new TransactWriteItem().withUpdate(updateActiveUserData.asUpdate()));

            this.userDAO.executeWriteTransaction(actions);
            // if this succeeds, go ahead and send a notification to the added user
            this.notificationService.sendMessage(userToAdd.getPushEndpointArn(),
                new NotificationData(NotificationService.friendRequestAction,
                    new FriendRequestResponse(friendRequest, activeUser).asMap()));

            this.metrics.commonClose(true);
            return new FriendResponse(friendToAdd, usernameToAdd);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }

    private String validConditions(final User activeUser, final User otherUser) {
        StringBuilder stringBuilder = new StringBuilder();
        String activeUserUsername = activeUser.getUsername();
        String otherUserUsername = otherUser.getUsername();
        if (activeUser.getFriends().size() >= Globals.MAX_NUMBER_FRIENDS) {
            stringBuilder.append("Max number of friends would be exceeded.\n");
        }
        if (otherUser.getUserPreferences().isPrivateAccount() || otherUser.getBlocked()
            .containsKey(activeUserUsername)) {
            stringBuilder.append("Unable to add ").append(otherUserUsername).append(".\n");
        }
        if (activeUser.getBlocked().containsKey(otherUserUsername)) {
            stringBuilder.append("You are currently blocking this user.\n");
        }
        if (otherUser.getFriendRequests().size() >= Globals.MAX_FRIEND_REQUESTS) {
            stringBuilder.append(otherUserUsername).append(" has too many pending requests.\n");
        }
        if (activeUser.getFriends().containsKey(otherUserUsername) && !activeUser.getFriends().get(otherUserUsername)
            .isConfirmed()) {
            stringBuilder.append("Request already sent.\n");
        }
        if (activeUser.getFriends().containsKey(otherUserUsername) && activeUser.getFriends().get(otherUserUsername)
            .isConfirmed()) {
            stringBuilder.append("You are already friends with ").append(otherUserUsername).append("\n");
        }
        if (otherUser.getFriendRequests().containsKey(activeUserUsername)) {
            stringBuilder.append("This user has already sent you a friend request.\n");
        }
        if (activeUserUsername.equals(otherUserUsername)) {
            stringBuilder.append("Cannot add yourself.\n");
        }
        return stringBuilder.toString().trim();
    }
}
