package managers;

import aws.DatabaseAccess;
import aws.SnsAccess;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import helpers.ErrorMessage;
import helpers.Globals;
import helpers.JsonHelper;
import helpers.Metrics;
import helpers.ResultStatus;
import helpers.UpdateItemData;
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
            User activeUserObject = this.databaseAccess.getUser(activeUser);
            if (activeUserObject != null) {
                if (activeUserObject.getFriends().size() < Globals.MAX_NUMBER_FRIENDS) {
                    User userToAdd = this.databaseAccess.getUser(usernameToAdd);
                    if (userToAdd != null) {
                        // user that the active user is attempting to add is indeed a real user
                        if (!userToAdd.getUserPreferences().isPrivateAccount()) {
                            if (!userToAdd.getBlocked().containsKey(activeUser)) {
                                // LAST CHECK I SWEAR :)
                                if (userToAdd.getFriendRequests().size()
                                    < Globals.MAX_FRIEND_REQUESTS) {
                                    Friend friendToAdd = new Friend(userToAdd,
                                        false); // added to active user's friends mapping
                                    FriendRequest friendRequest = new FriendRequest(
                                        activeUserObject,
                                        Instant.now().toString());

                                    // both friend and added friend are ready to be updated in DB
                                    final UpdateItemData updateFriendData = new UpdateItemData(
                                        usernameToAdd, DatabaseAccess.USERS_TABLE_NAME)
                                        .withUpdateExpression(
                                            "set " +
                                                User.FRIEND_REQUESTS + ".#username= :"
                                                + User.FRIEND_REQUESTS)
                                        .withValueMap(
                                            new ValueMap()
                                                .withMap(":" + User.FRIEND_REQUESTS,
                                                    friendRequest.asMap()))
                                        .withNameMap(new NameMap()
                                            .with("#username", activeUser));
                                    final UpdateItemData updateActiveUserData = new UpdateItemData(
                                        activeUser, DatabaseAccess.USERS_TABLE_NAME)
                                        .withUpdateExpression(
                                            "set " +
                                                User.FRIENDS + ".#username= :"
                                                + User.FRIENDS)
                                        .withValueMap(
                                            new ValueMap()
                                                .withMap(":" + User.FRIENDS,
                                                    friendToAdd.asMap()))
                                        .withNameMap(new NameMap()
                                            .with("#username", usernameToAdd));

                                    // want a transaction since more than one object is being updated at once
                                    final List<TransactWriteItem> actions = new ArrayList<>();
                                    actions.add(new TransactWriteItem()
                                        .withUpdate(updateFriendData.asUpdate()));
                                    actions.add(new TransactWriteItem()
                                        .withUpdate(updateActiveUserData.asUpdate()));

                                    this.databaseAccess.executeWriteTransaction(actions);
                                    // if this succeeds, go ahead and send a notification to the added user
                                    this.snsAccess.sendMessage(userToAdd.getPushEndpointArn(),
                                        new NotificationData(SnsAccess.friendRequestAction,
                                            new FriendRequestResponse(friendRequest, activeUser)
                                                .asMap()));
                                    resultStatus = ResultStatus
                                        .successful(JsonHelper.serializeMap(
                                            new FriendResponse(friendToAdd, usernameToAdd)
                                                .asMap()));
                                } else {
                                    this.metrics.log(String
                                        .format("User %s has too many requests.", usernameToAdd));
                                    resultStatus = ResultStatus
                                        .failureBadEntity(
                                            String.format("User %s has too many requests.",
                                                usernameToAdd));
                                }
                            } else {
                                this.metrics.log(String
                                    .format("User %s has this account blocked.", usernameToAdd));
                                resultStatus = ResultStatus
                                    .failureBadEntity(
                                        String.format("Unable to add %s", usernameToAdd));
                            }
                        } else {
                            this.metrics.log(String.format("User %s is private.", usernameToAdd));
                            resultStatus = ResultStatus
                                .failureBadEntity(String.format("Unable to add %s", usernameToAdd));
                        }
                    } else {
                        this.metrics.log(String.format("User %s does not exist.", usernameToAdd));
                        resultStatus = ResultStatus
                            .failureBadEntity(String.format("Unable to add %s", usernameToAdd));
                    }
                } else {
                    this.metrics.log("Max number of friends would be exceeded.");
                    resultStatus = ResultStatus
                        .failureBadEntity("Max number of friends would be exceeded.");
                }
            } else {
                this.metrics.log(String.format("User %s does not exist.", activeUser));
                resultStatus = ResultStatus
                    .failureBadEntity(String.format("Unable to add %s.", activeUser));
            }
        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod);
        }

        this.metrics.commonClose(resultStatus.responseCode);
        return resultStatus;
    }
}
