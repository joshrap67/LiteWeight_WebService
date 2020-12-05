package managers;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import daos.UserDAO;
import exceptions.ManagerExecutionException;
import imports.Globals;
import utils.Metrics;
import javax.inject.Inject;
import models.User;

public class BlockUserManager {

    private final UserDAO userDAO;
    private final Metrics metrics;
    private final RemoveFriendManager removeFriendManager;
    private final DeclineFriendRequestManager declineFriendRequestManager;
    private final CancelFriendRequestManager cancelFriendRequestManager;

    @Inject
    public BlockUserManager(final UserDAO userDAO, final Metrics metrics,
        final RemoveFriendManager removeFriendManager,
        final DeclineFriendRequestManager declineFriendRequestManager,
        final CancelFriendRequestManager cancelFriendRequestManager) {
        this.userDAO = userDAO;
        this.metrics = metrics;
        this.removeFriendManager = removeFriendManager;
        this.declineFriendRequestManager = declineFriendRequestManager;
        this.cancelFriendRequestManager = cancelFriendRequestManager;
    }

    /**
     * Attempts to block a given user if the active user has not reached the max amount of blocked
     * users. Appropriate actions are taken if the active user has a pending friend request for the
     * pending blocked user, or has sent a friend request to the blocked user, or is friends with
     * the blocked user. Appropriate data notifications are sent if any of the above mentioned
     * conditions exist.
     *
     * @param activeUser  username of the user who is blocking the user to block.
     * @param userToBlock username of the user who is about to be blocked by the active user.
     * @return the icon url of the user that is being blocked.
     * @throws Exception if either user is not found or if there are input errors.
     */
    public String blockUser(final String activeUser, final String userToBlock) throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".blockUser";
        this.metrics.commonSetup(classMethod);

        try {
            final User activeUserObject = this.userDAO.getUser(activeUser);
            final User userToBlockObject = this.userDAO.getUser(userToBlock);

            if (activeUserObject.getBlocked().size() >= Globals.MAX_BLOCKED) {
                this.metrics.commonClose(false);
                throw new ManagerExecutionException(
                    String.format("User %s has exceeded blocked limit.", activeUser));
            }

            if (activeUserObject.getFriendRequests().containsKey(userToBlock)) {
                // to-be-blocked user sent the active user a friend request. decline the request
                declineFriendRequestManager.declineRequest(activeUser, userToBlock);
            } else if (activeUserObject.getFriends().containsKey(userToBlock)
                && activeUserObject.getFriends().get(userToBlock).isConfirmed()) {
                // both the active user and to-be-blocked user are friends. Remove them both as friends
                removeFriendManager.removeFriend(activeUser, userToBlock);
            } else if (activeUserObject.getFriends().containsKey(userToBlock)
                && !activeUserObject.getFriends().get(userToBlock).isConfirmed()) {
                // active user is sending a friend request to the to-be-blocked user. cancel the request
                cancelFriendRequestManager.cancelRequest(activeUser, userToBlock);
            }
            // note that any notifications are taken care of by the managers above, an exception would have been thrown if something went wrong

            // go ahead and block the user
            UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withUpdateExpression(
                    "set " + User.BLOCKED + ".#username =:blockedUserIcon")
                .withValueMap(new ValueMap()
                    .withString(":blockedUserIcon", userToBlockObject.getIcon()))
                .withNameMap(new NameMap().with("#username", userToBlock));
            this.userDAO.updateUser(activeUser, updateItemSpec);

            // return the icon in case user doesn't already have the icon to display in the blocked list
            this.metrics.commonClose(true);
            return userToBlockObject.getIcon();
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
