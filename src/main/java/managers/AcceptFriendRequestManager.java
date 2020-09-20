package managers;

import aws.DatabaseAccess;
import aws.SnsAccess;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import exceptions.ManagerExecutionException;
import helpers.Globals;
import helpers.Metrics;
import helpers.UpdateItemData;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import models.Friend;
import models.NotificationData;
import models.User;

public class AcceptFriendRequestManager {

    private final SnsAccess snsAccess;
    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;

    @Inject
    public AcceptFriendRequestManager(final SnsAccess snsAccess,
        final DatabaseAccess databaseAccess,
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
    public boolean execute(final String activeUser, final String usernameToAccept)
        throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        try {
            final User activeUserObject = this.databaseAccess.getUser(activeUser);
            final User userToAccept = this.databaseAccess.getUser(usernameToAccept);

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
                activeUser, DatabaseAccess.USERS_TABLE_NAME)
                .withUpdateExpression("set " + User.FRIENDS + ".#username = :friendVal, " +
                    "remove " + User.FRIEND_REQUESTS + ".#username")
                .withNameMap(new NameMap().with("#username", usernameToAccept))
                .withValueMap(new ValueMap().withMap(":friendVal", newFriend.asMap()));

            // update the active user to be confirmed in the newly accepted friends mapping
            final UpdateItemData updateFriendData = new UpdateItemData(
                usernameToAccept, DatabaseAccess.USERS_TABLE_NAME)
                .withUpdateExpression(
                    "set " + User.FRIENDS + ".#username." + Friend.CONFIRMED + " = :confirmedVal")
                .withNameMap(new NameMap().with("#username", activeUser))
                .withValueMap(new ValueMap().withBoolean(":confirmedVal", true));

            // want a transaction since more than one object is being updated at once
            final List<TransactWriteItem> actions = new ArrayList<>();
            actions.add(new TransactWriteItem().withUpdate(activeUserData.asUpdate()));
            actions.add(new TransactWriteItem().withUpdate(updateFriendData.asUpdate()));
            this.databaseAccess.executeWriteTransaction(actions);

            // if this succeeds, go ahead and send a notification to the accepted user (only need to send username)
            this.snsAccess.sendMessage(userToAccept.getPushEndpointArn(),
                new NotificationData(SnsAccess.acceptedFriendRequestAction,
                    Maps.newHashMap(
                        ImmutableMap.<String, String>builder().put(User.USERNAME, activeUser)
                            .build())));
            this.metrics.commonClose(true);
            return true;
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
