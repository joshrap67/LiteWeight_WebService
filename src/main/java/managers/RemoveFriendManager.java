package managers;

import aws.DatabaseAccess;
import aws.SnsAccess;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
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
    public ResultStatus<String> execute(final String activeUser, final String usernameToRemove) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;

        try {
            final User activeUserObject = Optional
                .ofNullable(this.databaseAccess.getUser(activeUser))
                .orElseThrow(
                    () -> new UserNotFoundException(String.format("%s not found", activeUser)));
            final User userToRemove = Optional
                .ofNullable(this.databaseAccess.getUser(usernameToRemove))
                .orElseThrow(() -> new UserNotFoundException(
                    String.format("%s not found", usernameToRemove)));

            if (activeUserObject.getFriends().containsKey(usernameToRemove)) {
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
                        Maps.newHashMap(ImmutableMap.<String, String>builder()
                            .put(User.USERNAME, activeUser)
                            .build())));
                resultStatus = ResultStatus.successful("Friend successfully removed.");
            } else {
                this.metrics
                    .log(String.format("User %s no longer has this friend.", usernameToRemove));
                resultStatus = ResultStatus
                    .failureBadEntity(String.format("Unable to remove %s", usernameToRemove));
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
