package managers;

import aws.SnsAccess;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import daos.UserDAO;
import helpers.Metrics;
import helpers.UpdateItemData;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import models.NotificationData;
import models.User;

public class CancelFriendRequestManager {

    private final SnsAccess snsAccess;
    private final UserDAO userDAO;
    private final Metrics metrics;

    @Inject
    public CancelFriendRequestManager(final SnsAccess snsAccess, final UserDAO userDAO,
        final Metrics metrics) {
        this.snsAccess = snsAccess;
        this.userDAO = userDAO;
        this.metrics = metrics;
    }

    /**
     * This method gets the active user's data. If the active user's data does not exist, we assume
     * this is their first login and we enter a new user object in the db.
     *
     * @param activeUser The user that made the api request, trying to get data about themselves.
     */
    public void cancelRequest(final String activeUser, final String usernameToCancel)
        throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".cancelRequest";
        this.metrics.commonSetup(classMethod);

        try {
            final User userToCancel = this.userDAO.getUser(usernameToCancel);

            // for canceled user, remove the friend request
            final UpdateItemData updateFriendData = new UpdateItemData(
                usernameToCancel, UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("remove " + User.FRIEND_REQUESTS + ".#username")
                .withNameMap(new NameMap().with("#username", activeUser));
            // for active user, remove the (unconfirmed) user from their friends mapping
            final UpdateItemData updateActiveUserData = new UpdateItemData(
                activeUser, UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("remove " + User.FRIENDS + ".#username")
                .withNameMap(new NameMap().with("#username", usernameToCancel));

            // want a transaction since more than one object is being updated at once
            final List<TransactWriteItem> actions = new ArrayList<>();
            actions.add(new TransactWriteItem().withUpdate(updateFriendData.asUpdate()));
            actions.add(new TransactWriteItem().withUpdate(updateActiveUserData.asUpdate()));

            this.userDAO.executeWriteTransaction(actions);
            // if this succeeds, go ahead and send a notification to the canceled user (only need to send username)
            this.snsAccess.sendMessage(userToCancel.getPushEndpointArn(),
                new NotificationData(SnsAccess.canceledFriendRequestAction,
                    Maps.newHashMap(
                        ImmutableMap.<String, String>builder().put(User.USERNAME, activeUser)
                            .build())));

            this.metrics.commonClose(true);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
