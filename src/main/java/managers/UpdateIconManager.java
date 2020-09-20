package managers;

import aws.DatabaseAccess;
import aws.S3Access;
import exceptions.UserNotFoundException;
import helpers.ErrorMessage;
import helpers.Metrics;
import helpers.ResultStatus;
import java.util.Optional;
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
    public ResultStatus<String> execute(final String activeUser, final byte[] imageData) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;

        try {
            final User user = Optional.ofNullable(this.databaseAccess.getUser(activeUser))
                .orElseThrow(
                    () -> new UserNotFoundException(String.format("%s not found", activeUser)));
            // same filename is always used. Content is just overwritten
            String fileName = user.getIcon();
            boolean success = this.s3Access.uploadImage(imageData, fileName, this.metrics);
            if (success) {
                resultStatus = ResultStatus.successful("Picture updated successfully.");
            } else {
                resultStatus = ResultStatus.failureBadEntity("Picture failed to update.");
            }
        } catch (UserNotFoundException unfe) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, unfe));
            resultStatus = ResultStatus.failureBadEntity(unfe.getMessage());
        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod);
        }

        this.metrics.commonClose(resultStatus.success);
        return resultStatus;
    }
}
