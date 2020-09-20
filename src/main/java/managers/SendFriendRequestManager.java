package managers;

import aws.DatabaseAccess;
import aws.SnsAccess;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import exceptions.UserNotFoundException;
import helpers.ErrorMessage;
import helpers.Globals;
import helpers.JsonHelper;
import helpers.Metrics;
import helpers.ResultStatus;
import helpers.UpdateItemData;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import models.Friend;
import models.FriendRequest;
import models.NotificationData;
import models.User;
import responses.FriendRequestResponse;
import responses.FriendResponse;

public class SendFriendRequestManager {

    private final SnsAccess snsAccess;
    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;

    @Inject
    public SendFriendRequestManager(final SnsAccess snsAccess, final DatabaseAccess databaseAccess,
        final Metrics metrics) {
        this.snsAccess = snsAccess;
        this.databaseAccess = databaseAccess;
        this.metrics = metrics;
    }

    /**
     * This method gets the active user's data. If the active user's data does not exist, we assume
     * this is their first login and we enter a new user object in the db.
     *
     * @param activeUser The user that made the api request, trying to get data about themselves.
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public ResultStatus<String> execute(final String activeUser, final String usernameToAdd) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;
        try {
            final User activeUserObject = Optional
                .ofNullable(this.databaseAccess.getUser(activeUser))
                .orElseThrow(
                    () -> new UserNotFoundException(String.format("%s not found", activeUser)));
            final User userToAdd = Optional.ofNullable(this.databaseAccess.getUser(usernameToAdd))
                .orElseThrow(
                    () -> new UserNotFoundException(String.format("%s not found", usernameToAdd)));

            String errorMessage = validConditions(activeUserObject, userToAdd);
            if (errorMessage.isEmpty()) {
                Friend friendToAdd = new Friend(userToAdd, false);
                FriendRequest friendRequest = new FriendRequest(activeUserObject,
                    Instant.now().toString());

                // the active user needs to have this (unconfirmed) friend added to its friends list
                final UpdateItemData updateFriendData = new UpdateItemData(
                    usernameToAdd, DatabaseAccess.USERS_TABLE_NAME)
                    .withUpdateExpression(
                        "set " + User.FRIEND_REQUESTS + ".#username= :" + User.FRIEND_REQUESTS)
                    .withValueMap(
                        new ValueMap().withMap(":" + User.FRIEND_REQUESTS, friendRequest.asMap()))
                    .withNameMap(new NameMap().with("#username", activeUser));
                // friend to add needs to have the friend request added to its friend request list
                final UpdateItemData updateActiveUserData = new UpdateItemData(
                    activeUser, DatabaseAccess.USERS_TABLE_NAME)
                    .withUpdateExpression(
                        "set " + User.FRIENDS + ".#username= :" + User.FRIENDS)
                    .withValueMap(new ValueMap().withMap(":" + User.FRIENDS, friendToAdd.asMap()))
                    .withNameMap(new NameMap().with("#username", usernameToAdd));

                // want a transaction since more than one object is being updated at once
                final List<TransactWriteItem> actions = new ArrayList<>();
                actions.add(new TransactWriteItem().withUpdate(updateFriendData.asUpdate()));
                actions.add(new TransactWriteItem().withUpdate(updateActiveUserData.asUpdate()));

                this.databaseAccess.executeWriteTransaction(actions);
                // if this succeeds, go ahead and send a notification to the added user
                this.snsAccess.sendMessage(userToAdd.getPushEndpointArn(),
                    new NotificationData(SnsAccess.friendRequestAction,
                        new FriendRequestResponse(friendRequest, activeUser).asMap()));
                resultStatus = ResultStatus.successful(JsonHelper
                    .serializeMap(new FriendResponse(friendToAdd, usernameToAdd).asMap()));
            } else {
                this.metrics.log(errorMessage);
                resultStatus = ResultStatus.failureBadEntity(errorMessage);
            }
        } catch (UserNotFoundException unfe) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, unfe));
            resultStatus = ResultStatus.failureBadEntity(unfe.getMessage());
        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod);
        }

        this.metrics.commonClose(resultStatus.success);
        return resultStatus;
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
            stringBuilder.append("Unable to add").append(otherUserUsername).append(".\n");
        }
        if (activeUser.getBlocked().containsKey(otherUserUsername)) {
            stringBuilder.append("You are currently blocking this user.\n");
        }
        if (otherUser.getFriendRequests().size() >= Globals.MAX_FRIEND_REQUESTS) {
            stringBuilder.append(otherUserUsername).append(" has too many pending requests.\n")
                .append("\n");
        }
        if (activeUser.getFriends().containsKey(otherUserUsername)
            && !activeUser.getFriends().get(otherUserUsername).isConfirmed()) {
            stringBuilder.append("Request already sent.\n");
        }
        if (activeUser.getFriends().containsKey(otherUserUsername)
            && activeUser.getFriends().get(otherUserUsername).isConfirmed()) {
            stringBuilder.append("You are already friends with ").append(otherUserUsername)
                .append("\n");
        }
        if (otherUser.getFriendRequests().containsKey(activeUserUsername)) {
            stringBuilder.append("This user has already sent you a friend request.");
        }
        return stringBuilder.toString().trim();
    }
}
