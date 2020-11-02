package managers;

import aws.S3Access;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import daos.UserDAO;
import helpers.FileReader;
import helpers.Metrics;
import java.util.HashMap;
import java.util.UUID;
import javax.inject.Inject;
import models.User;
import models.UserPreferences;

public class NewUserManager {

    private final UserDAO userDAO;
    private final Metrics metrics;
    private final S3Access s3Access;

    @Inject
    public NewUserManager(final UserDAO userDAO, final Metrics metrics, final S3Access s3Access) {
        this.userDAO = userDAO;
        this.s3Access = s3Access;
        this.metrics = metrics;
    }

    /**
     * Creates a new user item and puts it in the user table. Note this is where the user's random
     * url for their icon is generated.
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
            final String fileName = uuid.toString() + "." + S3Access.JPG_TYPE;
            s3Access.uploadImage(FileReader.getDefaultProfilePicture(), fileName, this.metrics);

            final UserPreferences userPreferences = new UserPreferences();
            userPreferences.setMetricUnits(false);
            userPreferences.setPrivateAccount(false);
            userPreferences.setUpdateDefaultWeightOnRestart(true);
            userPreferences.setUpdateDefaultWeightOnSave(true);

            final Item user = new Item()
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
            PutItemOutcome outcome = this.userDAO.putUser(user);

            this.metrics.commonClose(true);
            return new User(outcome.getItem().asMap());
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
