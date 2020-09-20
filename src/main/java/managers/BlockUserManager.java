package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.google.common.collect.ImmutableMap;
import exceptions.UserNotFoundException;
import helpers.ErrorMessage;
import helpers.Globals;
import helpers.JsonHelper;
import helpers.Metrics;
import helpers.ResultStatus;
import java.util.Optional;
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
    public ResultStatus<String> execute(final String activeUser, final String userToBlock) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;
        try {
            final User activeUserObject = Optional
                .ofNullable(this.databaseAccess.getUser(activeUser))
                .orElseThrow(
                    () -> new UserNotFoundException(String.format("%s not found", activeUser)));
            final User userToBlockObject = Optional
                .ofNullable(this.databaseAccess.getUser(userToBlock))
                .orElseThrow(() -> new UserNotFoundException(
                    String.format("%s not found", userToBlock)));

            if (activeUserObject.getBlocked().size() < Globals.MAX_BLOCKED) {
                ResultStatus<String> updateStatus;
                if (activeUserObject.getFriendRequests().containsKey(userToBlock)) {
                    // to-be-blocked user sent the active user a friend request. decline the request
                    updateStatus = declineFriendRequestManager
                        .execute(activeUser, userToBlock);
                } else if (activeUserObject.getFriends().containsKey(userToBlock)
                    && activeUserObject.getFriends().get(userToBlock).isConfirmed()) {
                    // both the active user and to-be-blocked user are friends. Remove them both as friends
                    updateStatus = removeFriendManager.execute(activeUser, userToBlock);
                } else if (activeUserObject.getFriends().containsKey(userToBlock)
                    && !activeUserObject.getFriends().get(userToBlock).isConfirmed()) {
                    // active user is sending a friend request to the to-be-blocked user. cancel the request
                    updateStatus = cancelFriendRequestManager
                        .execute(activeUser, userToBlock);
                } else {
                    // no relation of these two users
                    updateStatus = ResultStatus.successful("Proceed");
                }
                // note that any notifications are taken care of by the managers above
                if (updateStatus.success) {
                    // go ahead and block the user
                    UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                        .withUpdateExpression(
                            "set " + User.BLOCKED + ".#username =:blockedUserIcon")
                        .withValueMap(new ValueMap()
                            .withString(":blockedUserIcon", userToBlockObject.getIcon()))
                        .withNameMap(new NameMap().with("#username", userToBlock));
                    this.databaseAccess.updateUser(activeUser, updateItemSpec);
                    // return the icon in case user doesn't already have the icon to display in the blocked list
                    resultStatus = ResultStatus.successful(JsonHelper
                        .serializeMap(ImmutableMap.of(User.ICON, userToBlockObject.getIcon())));
                } else {
                    // something went wrong
                    this.metrics.log(updateStatus.resultMessage);
                    resultStatus = updateStatus;
                }
            } else {
                this.metrics.log(String.format("User %s has exceeded blocked limit", activeUser));
                resultStatus = ResultStatus.failureBadEntity("User has too many users blocked.");
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
}
