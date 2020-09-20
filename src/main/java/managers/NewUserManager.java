package managers;

import aws.DatabaseAccess;
import aws.S3Access;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import helpers.ErrorMessage;
import helpers.FileReader;
import helpers.JsonHelper;
import helpers.Metrics;
import helpers.ResultStatus;
import java.util.HashMap;
import java.util.UUID;
import javax.inject.Inject;
import models.User;
import models.UserPreferences;

public class NewUserManager {

    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;
    private final S3Access s3Access;

    @Inject
    public NewUserManager(final DatabaseAccess dbAccessManager, final Metrics metrics,
        final S3Access s3Access) {
        this.databaseAccess = dbAccessManager;
        this.s3Access = s3Access;
        this.metrics = metrics;
    }

    /**
     * @param username Username of new user to be inserted
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public ResultStatus<String> execute(final String username) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;
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

            PutItemOutcome outcome = this.databaseAccess.putUser(user);
            resultStatus = ResultStatus
                .successful(JsonHelper.serializeMap(outcome.getItem().asMap()));
        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod);
        }

        this.metrics.commonClose(resultStatus.success);
        return resultStatus;
    }
}
