package managers;

import aws.DatabaseAccess;
import aws.S3Access;
import com.amazonaws.services.dynamodbv2.document.Item;
import helpers.ErrorMessage;
import helpers.FileReader;
import helpers.JsonHelper;
import helpers.Metrics;
import helpers.ResultStatus;
import java.util.HashMap;
import java.util.UUID;
import javax.inject.Inject;
import models.User;

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
    public ResultStatus<Item> execute(final String username) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<Item> resultStatus;

        try {
            // whenever a user is created, give them a unique UUID file path that will always get updated

            final UUID uuid = UUID.randomUUID();
            final String fileName = uuid.toString() + "." + S3Access.JPG_TYPE;
            s3Access.uploadImage(FileReader.getDefaultProfilePicture(), fileName, this.metrics);

            Item user = new Item()
                .withString(User.USERNAME, username)
                .withNull(User.PREMIUM_TOKEN)
                .withString(User.ICON, fileName)
                .withNull(User.CURRENT_WORKOUT)
                .withMap(User.WORKOUTS, new HashMap<>())
                .withNull(User.PUSH_ENDPOINT_ARN)
                .withInt(User.WORKOUTS_SENT, 0)
                .withBoolean(User.PRIVATE_ACCOUNT, false)
                .withBoolean(User.UPDATE_DEFAULT_WEIGHT_ON_RESTART, true)
                .withBoolean(User.UPDATE_DEFAULT_WEIGHT_ON_SAVE, true)
                .withInt(User.NOTIFICATION_PREFERENCES,
                    0) // TODO make this a front end responsibility?
                .withMap(User.FRIENDS, new HashMap<>())
                .withMap(User.FRIEND_REQUESTS, new HashMap<>())
                .withMap(User.RECEIVED_WORKOUTS, new HashMap<>())
                .withMap(User.EXERCISES, FileReader.getDefaultExercises());

            this.databaseAccess.putUser(user);
            resultStatus = ResultStatus.successful(JsonHelper.serializeMap(user.asMap()), user);
        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod);
        }

        this.metrics.commonClose(resultStatus.responseCode);
        return resultStatus;
    }
}
