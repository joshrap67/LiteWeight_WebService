package managers;

import aws.DatabaseAccess;
import aws.S3Access;
import helpers.ErrorMessage;
import helpers.Metrics;
import helpers.ResultStatus;
import javax.inject.Inject;
import models.User;

public class UpdateIconManager {

    public final DatabaseAccess databaseAccess;
    public final S3Access s3Access;
    public final Metrics metrics;

    @Inject
    public UpdateIconManager(final DatabaseAccess dbAccessManager, final Metrics metrics,
        final S3Access s3Access) {
        this.databaseAccess = dbAccessManager;
        this.s3Access = s3Access;
        this.metrics = metrics;
    }

    /**
     * @param activeUser Username of new user to be inserted
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public ResultStatus<String> execute(final String activeUser, byte[] imageData) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;

        try {
            User user = this.databaseAccess.getUser(activeUser);
            if (user != null) {
                // same filename is always used. Content is just overwritten
                String fileName = user.getIcon();
                boolean success = this.s3Access.updateImage(imageData, fileName, this.metrics);
                if (success) {
                    resultStatus = ResultStatus.successful("Picture updated successfully.");
                } else {
                    resultStatus = ResultStatus.failureBadEntity("Picture failed to update.");
                }
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
