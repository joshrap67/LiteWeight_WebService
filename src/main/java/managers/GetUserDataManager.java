package managers;

import com.amazonaws.services.dynamodbv2.document.Item;
import daos.UserDAO;
import exceptions.InvalidAttributeException;
import exceptions.UserNotFoundException;
import java.util.Optional;
import javax.inject.Inject;
import utils.Metrics;
import models.User;

public class GetUserDataManager {

    private final UserDAO userDAO;
    private final Metrics metrics;
    private final NewUserManager newUserManager;

    @Inject
    public GetUserDataManager(final UserDAO userDAO, final Metrics metrics,
        final NewUserManager newUserManager) {
        this.userDAO = userDAO;
        this.metrics = metrics;
        this.newUserManager = newUserManager;
    }

    /**
     * Returns the data of a given user from the user table.
     *
     * @param username username of user that the client is requesting data for.
     * @return user object of the given username.
     */
    public User getUserData(final String username)
        throws UserNotFoundException, InvalidAttributeException {
        final String classMethod = this.getClass().getSimpleName() + ".getUserData";
        this.metrics.commonSetup(classMethod);

        try {
            Item user = Optional.ofNullable(this.userDAO.getUserItem(username)).orElseThrow(
                () -> new UserNotFoundException(String.format("%s not found.", username)));
            this.metrics.commonClose(true);
            return new User(user);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }

    /**
     * Returns the active user's data from the user table. If the active user's data does not exist,
     * it is assumed this is their first login and they are created into the user table.
     *
     * @param activeUser The user that made the api request, trying to get data about themselves.
     * @return UserResponse of the user's active data (certain fields omitted for client
     * consumption)
     */
    public User getActiveUserData(final String activeUser) throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".getActiveUserData";
        this.metrics.commonSetup(classMethod);

        try {
            User user;
            Item userItem = this.userDAO.getUserItem(activeUser);
            if (userItem == null) {
                // user has not been added yet in the DB, so create an entry for them
                user = this.newUserManager.createNewUser(activeUser);
            } else {
                // user already exists in DB so just return their data
                user = new User(userItem);
            }
            this.metrics.commonClose(true);
            return user;
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }

    }
}
