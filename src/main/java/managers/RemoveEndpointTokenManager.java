package managers;

import aws.DatabaseAccess;
import aws.SnsAccess;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.sns.model.DeleteEndpointRequest;
import exceptions.InvalidAttributeException;
import exceptions.UserNotFoundException;
import helpers.Metrics;
import javax.inject.Inject;
import models.User;

public class RemoveEndpointTokenManager {

    private final DatabaseAccess databaseAccess;
    private final SnsAccess snsAccess;
    private final Metrics metrics;

    @Inject
    public RemoveEndpointTokenManager(final DatabaseAccess databaseAccess,
        final SnsAccess snsAccess, final Metrics metrics) {
        this.databaseAccess = databaseAccess;
        this.snsAccess = snsAccess;
        this.metrics = metrics;
    }

    /**
     * This function takes in a device token registered in google cloud messaging and creates a SNS
     * endpoint for this token and then registers the ARN of the SNS endpoint on the user item.
     *
     * @param activeUser The user making the api request whose push endpoint is being registered.
     * @return Standard result status object giving insight on whether the request was successful.
     */
    public boolean execute(final String activeUser)
        throws InvalidAttributeException, UserNotFoundException {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        try {
            final User user = this.databaseAccess.getUser(activeUser);

            if (user.getPushEndpointArn() != null) {
                final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                    .withUpdateExpression("set " + User.PUSH_ENDPOINT_ARN + " =:arn")
                    .withValueMap(new ValueMap().withNull(":arn"));
                this.databaseAccess.updateUser(activeUser, updateItemSpec);

                // arn is no longer in DB. Now try and delete the ARN from SNS
                final DeleteEndpointRequest deleteEndpointRequest = new DeleteEndpointRequest()
                    .withEndpointArn(user.getPushEndpointArn());
                this.snsAccess.unregisterPlatformEndpoint(deleteEndpointRequest);
            }
            this.metrics.commonClose(true);
            return true;
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
