package managers;

import aws.S3Access;
import daos.UserDAO;
import exceptions.InvalidAttributeException;
import exceptions.UserNotFoundException;
import helpers.Metrics;
import javax.inject.Inject;
import models.User;

public class UpdateIconManager {

    public final UserDAO userDAO;
    public final S3Access s3Access;
    public final Metrics metrics;

    @Inject
    public UpdateIconManager(final UserDAO userDAO, final Metrics metrics,
        final S3Access s3Access) {
        this.userDAO = userDAO;
        this.s3Access = s3Access;
        this.metrics = metrics;
    }

    /**
     * Updates the user's icon. Note that it just replaces the old icon using the same image url.
     *
     * @param activeUser username of the user who is updating their icon.
     * @param imageData  byte stream of the image that is to replace the current icon.
     * @return whether the icon was successfully uploaded or not.
     * @throws InvalidAttributeException if user item is formatted incorrectly.
     * @throws UserNotFoundException     if the active user is not found.
     */
    public boolean updateIcon(final String activeUser, final byte[] imageData)
        throws InvalidAttributeException, UserNotFoundException {
        final String classMethod = this.getClass().getSimpleName() + ".updateIcon";
        this.metrics.commonSetup(classMethod);

        try {
            final User user = this.userDAO.getUser(activeUser);
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
