package managers;

import services.NotificationService;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.sns.model.DeleteEndpointRequest;
import daos.UserDAO;
import exceptions.InvalidAttributeException;
import exceptions.UserNotFoundException;
import utils.Metrics;
import javax.inject.Inject;
import models.User;

public class RemoveEndpointTokenManager {

    private final UserDAO userDAO;
    private final NotificationService notificationService;
    private final Metrics metrics;

    @Inject
    public RemoveEndpointTokenManager(final UserDAO userDAO, final NotificationService notificationService,
        final Metrics metrics) {
        this.userDAO = userDAO;
        this.notificationService = notificationService;
        this.metrics = metrics;
    }

    /**
     * Sets the user's push token to null in the database and unregisters it from SNS. At this time only one device can
     * be registered per user.
     *
     * @param activeUser username of the user that is unregistering this device.
     * @throws InvalidAttributeException if the user is not properly formatted in the database.
     * @throws UserNotFoundException     if te user is not found in the user table.
     */
    public void unregisterDevice(final String activeUser)
        throws InvalidAttributeException, UserNotFoundException {
        final String classMethod = this.getClass().getSimpleName() + ".unregisterDevice";
        this.metrics.commonSetup(classMethod);

        try {
            final User user = this.userDAO.getUser(activeUser);

            if (user.getPushEndpointArn() != null) {
                UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                    .withUpdateExpression("set " + User.PUSH_ENDPOINT_ARN + " =:arn")
                    .withValueMap(new ValueMap().withNull(":arn"));
                this.userDAO.updateUser(activeUser, updateItemSpec);

                // ARN is no longer in DB. Now try and delete the ARN from SNS
                final DeleteEndpointRequest deleteEndpointRequest = new DeleteEndpointRequest().withEndpointArn(
                    user.getPushEndpointArn());
                this.notificationService.unregisterPlatformEndpoint(deleteEndpointRequest);
            }
            this.metrics.commonClose(true);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
