package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.Item;
import exceptions.InvalidAttributeException;
import exceptions.UserNotFoundException;
import java.util.Optional;
import javax.inject.Inject;
import helpers.Metrics;
import models.User;
import responses.UserResponse;

public class GetUserDataManager {

    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;
    private final NewUserManager newUserManager;

    @Inject
    public GetUserDataManager(final DatabaseAccess dbAccessManager, final Metrics metrics,
        final NewUserManager newUserManager) {
        this.databaseAccess = dbAccessManager;
        this.metrics = metrics;
        this.newUserManager = newUserManager;
    }

    /**
     * This method gets the data of another user.
     *
     * @param username The user that made the api request, trying to get data about themselves.
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public UserResponse getUserData(final String username)
        throws UserNotFoundException, InvalidAttributeException {
        final String classMethod = this.getClass().getSimpleName() + ".getUserData";
        this.metrics.commonSetup(classMethod);

        try {
            Item user = Optional.ofNullable(this.databaseAccess.getUserItem(username)).orElseThrow(
                () -> new UserNotFoundException(String.format("%s not found.", username)));
            return new UserResponse(user);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }

    /**
     * This method gets the active user's data. If the active user's data does not exist, we assume
     * this is their first login and we enter a new user object in the db.
     *
     * @param activeUser The user that made the api request, trying to get data about themselves.
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public UserResponse getActiveUserData(final String activeUser) throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".getActiveUserData";
        this.metrics.commonSetup(classMethod);

        try {
            UserResponse userResponse;
            Item user = this.databaseAccess.getUserItem(activeUser);
            if (user == null) {
                // user has not been added yet in the DB, so create an entry for them
                User userResult = this.newUserManager.execute(activeUser);
                userResponse = new UserResponse(userResult.asMap());
            } else {
                // user already exists in DB so just return their data
                userResponse = new UserResponse(user);
            }
            return userResponse;
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }

    }
}
