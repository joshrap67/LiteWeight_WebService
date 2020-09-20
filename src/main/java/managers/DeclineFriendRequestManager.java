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

public class DeclineFriendRequestManager {

    private final SnsAccess snsAccess;
    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;

    @Inject
    public DeclineFriendRequestManager(final SnsAccess snsAccess,
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
    public ResultStatus<String> execute(final String activeUser, final String declinedUser) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;

        try {
            final User activeUserObject = Optional
                .ofNullable(this.databaseAccess.getUser(activeUser))
                .orElseThrow(
                    () -> new UserNotFoundException(String.format("%s not found", activeUser)));
            final User declinedUserObject = Optional
                .ofNullable(this.databaseAccess.getUser(declinedUser))
                .orElseThrow(() -> new UserNotFoundException(
                    String.format("%s not found", declinedUser)));

            // sanity check to make sure that the friend request is still there
            if (activeUserObject.getFriendRequests().containsKey(declinedUser)) {

                // remove friend from active user
                final UpdateItemData activeUserData = new UpdateItemData(
                    activeUser, DatabaseAccess.USERS_TABLE_NAME)
                    .withUpdateExpression("remove " + User.FRIEND_REQUESTS + ".#username")
                    .withNameMap(new NameMap().with("#username", declinedUser));

                // remove the (unconfirmed) active user from friend's mapping
                final UpdateItemData updateFriendData = new UpdateItemData(
                    declinedUser, DatabaseAccess.USERS_TABLE_NAME)
                    .withUpdateExpression("remove " + User.FRIENDS + ".#username")
                    .withNameMap(new NameMap().with("#username", activeUser));

                // want a transaction since more than one object is being updated at once
                final List<TransactWriteItem> actions = new ArrayList<>();
                actions.add(new TransactWriteItem().withUpdate(activeUserData.asUpdate()));
                actions.add(new TransactWriteItem().withUpdate(updateFriendData.asUpdate()));

                this.databaseAccess.executeWriteTransaction(actions);
                // if this succeeds, go ahead and send a notification to the declined user (only need to send username)
                this.snsAccess.sendMessage(declinedUserObject.getPushEndpointArn(),
                    new NotificationData(SnsAccess.declinedFriendRequestAction,
                        Maps.newHashMap(ImmutableMap.<String, String>builder()
                            .put(User.USERNAME, activeUser)
                            .build())));
                resultStatus = ResultStatus.successful("Friend request successfully declined.");
            } else {
                this.metrics
                    .log(String.format("User %s no longer has friend request.", declinedUser));
                resultStatus = ResultStatus
                    .failureBadEntity(String.format("Unable to decline %s", declinedUser));
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
