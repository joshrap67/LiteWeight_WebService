package managers;

import aws.DatabaseAccess;
import aws.SnsAccess;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import exceptions.ManagerExecutionException;
import exceptions.UserNotFoundException;
import helpers.ErrorMessage;
import helpers.Metrics;
import helpers.ResultStatus;
import helpers.UpdateItemData;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import models.NotificationData;
import models.User;

public class RemoveFriendManager {

    private final SnsAccess snsAccess;
    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;

    @Inject
    public RemoveFriendManager(final SnsAccess snsAccess,
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
    public boolean execute(final String activeUser, final String usernameToRemove)
        throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        try {
            final User activeUserObject = this.databaseAccess.getUser(activeUser);
            final User userToRemove = this.databaseAccess.getUser(usernameToRemove);
            if (!activeUserObject.getFriends().containsKey(usernameToRemove)) {
                this.metrics.commonClose(false);
                throw new ManagerExecutionException(
                    String.format("User %s no longer has this friend.", usernameToRemove));
            }

            // remove friend from active user
            final UpdateItemData activeUserData = new UpdateItemData(
                activeUser, DatabaseAccess.USERS_TABLE_NAME)
                .withUpdateExpression("remove " + User.FRIENDS + ".#username")
                .withNameMap(new NameMap().with("#username", usernameToRemove));

            // remove active user from friend's mapping
            final UpdateItemData updateFriendData = new UpdateItemData(
                usernameToRemove, DatabaseAccess.USERS_TABLE_NAME)
                .withUpdateExpression("remove " + User.FRIENDS + ".#username")
                .withNameMap(new NameMap().with("#username", activeUser));

            // want a transaction since more than one object is being updated at once
            final List<TransactWriteItem> actions = new ArrayList<>();
            actions.add(new TransactWriteItem().withUpdate(activeUserData.asUpdate()));
            actions.add(new TransactWriteItem().withUpdate(updateFriendData.asUpdate()));

            this.databaseAccess.executeWriteTransaction(actions);
            // if this succeeds, go ahead and send a notification to the accepted user (only need to send username)
            this.snsAccess.sendMessage(userToRemove.getPushEndpointArn(),
                new NotificationData(SnsAccess.removeFriendAction,
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
