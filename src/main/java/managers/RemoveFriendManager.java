package managers;

import services.NotificationService;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import daos.UserDAO;
import exceptions.ManagerExecutionException;
import utils.Metrics;
import utils.UpdateItemTemplate;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import models.NotificationData;
import models.User;

public class RemoveFriendManager {

    private final NotificationService notificationService;
    private final UserDAO userDAO;
    private final Metrics metrics;

    @Inject
    public RemoveFriendManager(final NotificationService notificationService, final UserDAO userDAO,
        final Metrics metrics) {
        this.notificationService = notificationService;
        this.userDAO = userDAO;
        this.metrics = metrics;
    }

    /**
     * Removes a friend from the active user and removed user's friends list. Upon success, a data notification is sent
     * to the removed user with a payload consisting of the active user's username.
     *
     * @param activeUser       user that is making the request.
     * @param usernameToRemove user that the active user is removing as a friend.
     * @throws Exception if either user is not found or if the two are no longer friends.
     */
    public void removeFriend(final String activeUser, final String usernameToRemove) throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".removeFriend";
        this.metrics.commonSetup(classMethod);

        try {
            final User activeUserObject = this.userDAO.getUser(activeUser);
            final User userToRemove = this.userDAO.getUser(usernameToRemove);
            if (!activeUserObject.getFriends().containsKey(usernameToRemove)) {
                this.metrics.commonClose(false);
                throw new ManagerExecutionException(String.format("User %s no longer has this friend.", activeUser));
            }

            // remove friend from active user
            UpdateItemTemplate activeUserData = new UpdateItemTemplate(activeUser, UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("remove " + User.FRIENDS + ".#username")
                .withNameMap(new NameMap().with("#username", usernameToRemove));

            // remove active user from friend's mapping
            UpdateItemTemplate updateFriendData = new UpdateItemTemplate(usernameToRemove, UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("remove " + User.FRIENDS + ".#username")
                .withNameMap(new NameMap().with("#username", activeUser));

            final List<TransactWriteItem> actions = new ArrayList<>();
            actions.add(new TransactWriteItem().withUpdate(activeUserData.asUpdate()));
            actions.add(new TransactWriteItem().withUpdate(updateFriendData.asUpdate()));

            this.userDAO.executeWriteTransaction(actions);
            // if this succeeds, go ahead and send a notification to the accepted user (only need to send username)
            this.notificationService.sendMessage(userToRemove.getPushEndpointArn(),
                new NotificationData(NotificationService.removedAsFriendAction,
                    Maps.newHashMap(ImmutableMap.<String, String>builder().put(User.USERNAME, activeUser)
                        .build())));
            this.metrics.commonClose(true);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
