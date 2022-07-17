package managers;

import services.StorageService;
import com.amazonaws.services.dynamodbv2.document.Item;
import daos.UserDAO;
import utils.FileReader;
import utils.Metrics;
import java.util.HashMap;
import java.util.UUID;
import javax.inject.Inject;
import models.User;
import models.UserPreferences;

public class NewUserManager {

    private final UserDAO userDAO;
    private final Metrics metrics;
    private final StorageService storageService;

    @Inject
    public NewUserManager(final UserDAO userDAO, final Metrics metrics,
        final StorageService storageService) {
        this.userDAO = userDAO;
        this.storageService = storageService;
        this.metrics = metrics;
    }

    /**
     * Creates a new user item and puts it in the user table. Note this is where the user's random url for their icon is
     * generated.
     *
     * @param username username of the user that is to be created.
     * @return User the newly created user.
     * @throws Exception if there is any error putting the user in the user table.
     */
    public User createNewUser(final String username) throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".createNewUser";
        this.metrics.commonSetup(classMethod);

        try {
            // whenever a user is created, give them a unique UUID file path that will always get updated
            final UUID uuid = UUID.randomUUID();
            final String fileName = String.format("%s.%s", uuid, StorageService.JPG_TYPE);
            // TODO this should really not happen, make a private bucket for this image and download it from there to get the jar smaller
            storageService.uploadImage(FileReader.getDefaultProfilePicture(), fileName, this.metrics);

            final UserPreferences userPreferences = new UserPreferences();
            userPreferences.setMetricUnits(false);
            userPreferences.setPrivateAccount(false);
            userPreferences.setUpdateDefaultWeightOnRestart(true);
            userPreferences.setUpdateDefaultWeightOnSave(true);

            Item user = new Item()
                .withString(User.USERNAME, username)
                .withNull(User.PREMIUM_TOKEN)
                .withString(User.ICON, fileName)
                .withNull(User.CURRENT_WORKOUT)
                .withMap(User.WORKOUTS, new HashMap<>())
                .withNull(User.PUSH_ENDPOINT_ARN)
                .withInt(User.WORKOUTS_SENT, 0)
                .withMap(User.USER_PREFERENCES, userPreferences.asMap())
                .withMap(User.FRIENDS, new HashMap<>())
                .withMap(User.BLOCKED, new HashMap<>())
                .withMap(User.FRIEND_REQUESTS, new HashMap<>())
                .withMap(User.RECEIVED_WORKOUTS, new HashMap<>())
                .withMap(User.EXERCISES, FileReader.getDefaultExercises());
            this.userDAO.putUser(user);

            this.metrics.commonClose(true);
            return new User(user);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
