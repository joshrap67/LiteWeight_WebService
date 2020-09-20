package managers;

import aws.DatabaseAccess;
import aws.S3Access;
import exceptions.InvalidAttributeException;
import exceptions.UserNotFoundException;
import helpers.Metrics;
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
    public boolean execute(final String activeUser, final byte[] imageData)
        throws InvalidAttributeException, UserNotFoundException {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        try {
            final User user = this.databaseAccess.getUser(activeUser);
            // same filename is always used. Content is just overwritten
            String fileName = user.getIcon();
            boolean success = this.s3Access.uploadImage(imageData, fileName, this.metrics);

            this.metrics.commonClose(success);
            return success;
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
