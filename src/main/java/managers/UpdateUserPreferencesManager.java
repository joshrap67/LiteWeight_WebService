package managers;

import aws.DatabaseAccess;
import aws.S3Access;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import helpers.ErrorMessage;
import helpers.Metrics;
import helpers.ResultStatus;
import javax.inject.Inject;
import models.User;
import models.UserPreferences;

public class UpdateUserPreferencesManager {

    public final DatabaseAccess databaseAccess;
    public final S3Access s3Access;
    public final Metrics metrics;

    @Inject
    public UpdateUserPreferencesManager(final DatabaseAccess dbAccessManager, final Metrics metrics,
        final S3Access s3Access) {
        this.databaseAccess = dbAccessManager;
        this.s3Access = s3Access;
        this.metrics = metrics;
    }

    /**
     * @param activeUser Username of new user to be inserted
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public ResultStatus<String> execute(final String activeUser, UserPreferences userPreferences) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;

        try {
            User user = this.databaseAccess.getUser(activeUser);
            if (user != null && userPreferences != null) {
                // right now just overwrite values in DB with these new ones
                UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                    .withUpdateExpression(
                        "set " + User.USER_PREFERENCES + " =:" + User.USER_PREFERENCES)
                    .withValueMap(new ValueMap()
                        .withMap(":" + User.USER_PREFERENCES, userPreferences.asMap()));
                this.databaseAccess.updateUser(activeUser, updateItemSpec);
                resultStatus = ResultStatus.successful("Preferences updated.");

            } else {
                this.metrics.log("Error does not exist.");
                resultStatus = ResultStatus.failureBadEntity("User does not exist.");
            }

        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod);
        }

        this.metrics.commonClose(resultStatus.responseCode);
        return resultStatus;
    }
}
