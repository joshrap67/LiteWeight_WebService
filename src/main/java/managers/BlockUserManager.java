package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import exceptions.ManagerExecutionException;
import helpers.Globals;
import helpers.Metrics;
import javax.inject.Inject;
import models.User;

public class BlockUserManager {

    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;
    private final RemoveFriendManager removeFriendManager;
    private final DeclineFriendRequestManager declineFriendRequestManager;
    private final CancelFriendRequestManager cancelFriendRequestManager;

    @Inject
    public BlockUserManager(final DatabaseAccess databaseAccess, final Metrics metrics,
        final RemoveFriendManager removeFriendManager,
        final DeclineFriendRequestManager declineFriendRequestManager,
        final CancelFriendRequestManager cancelFriendRequestManager) {
        this.databaseAccess = databaseAccess;
        this.metrics = metrics;
        this.removeFriendManager = removeFriendManager;
        this.declineFriendRequestManager = declineFriendRequestManager;
        this.cancelFriendRequestManager = cancelFriendRequestManager;
    }

    /**
     * This method gets the active user's data. If the active user's data does not exist, we assume
     * this is their first login and we enter a new user object in the db.
     *
     * @param activeUser The user that made the api request, trying to get data about themselves.
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public String execute(final String activeUser, final String userToBlock) throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        try {
            final User activeUserObject = this.databaseAccess.getUser(activeUser);
            final User userToBlockObject = this.databaseAccess.getUser(userToBlock);

            if (activeUserObject.getBlocked().size() >= Globals.MAX_BLOCKED) {
                this.metrics.commonClose(false);
                throw new ManagerExecutionException(
                    String.format("User %s has exceeded blocked limit", activeUser));
            }

            if (activeUserObject.getFriendRequests().containsKey(userToBlock)) {
                // to-be-blocked user sent the active user a friend request. decline the request
                declineFriendRequestManager.execute(activeUser, userToBlock);
            } else if (activeUserObject.getFriends().containsKey(userToBlock)
                && activeUserObject.getFriends().get(userToBlock).isConfirmed()) {
                // both the active user and to-be-blocked user are friends. Remove them both as friends
                removeFriendManager.execute(activeUser, userToBlock);
            } else if (activeUserObject.getFriends().containsKey(userToBlock)
                && !activeUserObject.getFriends().get(userToBlock).isConfirmed()) {
                // active user is sending a friend request to the to-be-blocked user. cancel the request
                cancelFriendRequestManager.execute(activeUser, userToBlock);
            }
            // note that any notifications are taken care of by the managers above, an exception would have been thrown if something went rong

            // go ahead and block the user
            UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withUpdateExpression(
                    "set " + User.BLOCKED + ".#username =:blockedUserIcon")
                .withValueMap(new ValueMap()
                    .withString(":blockedUserIcon", userToBlockObject.getIcon()))
                .withNameMap(new NameMap().with("#username", userToBlock));
            this.databaseAccess.updateUser(activeUser, updateItemSpec);

            // return the icon in case user doesn't already have the icon to display in the blocked list
            this.metrics.commonClose(true);
            return userToBlockObject.getIcon();
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
