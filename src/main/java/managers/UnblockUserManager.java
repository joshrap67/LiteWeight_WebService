package managers;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import daos.UserDAO;
import exceptions.InvalidAttributeException;
import exceptions.ManagerExecutionException;
import exceptions.UserNotFoundException;
import utils.Metrics;
import javax.inject.Inject;
import models.User;

public class UnblockUserManager {

    private final UserDAO userDAO;
    private final Metrics metrics;

    @Inject
    public UnblockUserManager(final UserDAO userDAO, final Metrics metrics) {
        this.userDAO = userDAO;
        this.metrics = metrics;
    }

    /**
     * Unblocks a specified user.
     *
     * @param activeUser    username of te user that is doing the unblocking.
     * @param userToUnblock username of the user that the active user is attempting to unblock.
     * @throws InvalidAttributeException if error in user item.
     * @throws UserNotFoundException     if either user is not found.
     * @throws ManagerExecutionException if the user is not actually blocking the user to unblock
     */
    public void unblockUser(final String activeUser, final String userToUnblock)
        throws InvalidAttributeException, UserNotFoundException, ManagerExecutionException {
        final String classMethod = this.getClass().getSimpleName() + ".unblockUser";
        this.metrics.commonSetup(classMethod);

        try {
            final User activeUserObject = this.userDAO.getUser(activeUser);

            if (!activeUserObject.getBlocked().containsKey(userToUnblock)) {
                this.metrics.commonClose(false);
                throw new ManagerExecutionException(
                    String.format("Unable to unblock %s", userToUnblock));
            }

            // unblock the user
            UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withUpdateExpression("remove " + User.BLOCKED + ".#username")
                .withNameMap(new NameMap().with("#username", userToUnblock));
            this.userDAO.updateUser(activeUser, updateItemSpec);

            this.metrics.commonClose(true);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
