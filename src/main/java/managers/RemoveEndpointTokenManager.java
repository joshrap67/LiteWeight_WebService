package managers;

import aws.DatabaseAccess;
import aws.SnsAccess;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.DeleteEndpointRequest;
import com.amazonaws.services.sns.model.InvalidParameterException;
import helpers.Config;
import helpers.ErrorMessage;
import helpers.Metrics;
import helpers.ResultStatus;
import java.util.Map;
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
     * @param activeUser The user making the api request whos push endpoint is being registered.
     * @return Standard result status object giving insight on whether the request was successful.
     */
    public ResultStatus<String> execute(final String activeUser) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;

        try {
            final User user = this.databaseAccess.getUser(activeUser);

            if (user.getPushEndpointArn() == null) {
                final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                    .withUpdateExpression("remove " + User.PUSH_ENDPOINT_ARN);

                this.databaseAccess.updateUser(activeUser, updateItemSpec);

                //we've made it here without exception which means the user doesn't have record of the
                //endpoint anymore, now we try to actually delete the arn. If the following fails we're
                //still safe as there's no reference to the arn in the db anymore
                final DeleteEndpointRequest deleteEndpointRequest = new DeleteEndpointRequest()
                    .withEndpointArn(user.getPushEndpointArn());
                this.snsAccess.unregisterPlatformEndpoint(deleteEndpointRequest);

                resultStatus = ResultStatus.successful("endpoint unregistered");
            } else {
                resultStatus = ResultStatus.successful("no endpoint to unregister");
            }
        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<Map>(classMethod, e));
            resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod);
        }

        this.metrics.commonClose(resultStatus.responseCode);
        return resultStatus;
    }

    private void attemptToRegisterUserEndpoint(final String activeUser,
        final String deviceToken) throws InvalidParameterException {
        //first thing to do is register the device token with SNS
        final CreatePlatformEndpointRequest createPlatformEndpointRequest =
            new CreatePlatformEndpointRequest()
                .withPlatformApplicationArn(Config.PUSH_SNS_PLATFORM_ARN_DEV)
                .withToken(deviceToken)
                .withCustomUserData(activeUser);
        final CreatePlatformEndpointResult createPlatformEndpointResult = this.snsAccess
            .registerPlatformEndpoint(createPlatformEndpointRequest);

        //this creation will give us a new ARN for the sns endpoint associated with the device token
        final String userEndpointArn = createPlatformEndpointResult.getEndpointArn();

        //we need to register the ARN for the user's device on the user item
        final String updateExpression = "set " + User.PUSH_ENDPOINT_ARN + " = :userEndpointArn";
        final ValueMap valueMap = new ValueMap().withString(":userEndpointArn", userEndpointArn);
        final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withUpdateExpression(updateExpression)
            .withValueMap(valueMap);

        this.databaseAccess.updateUser(activeUser, updateItemSpec);
    }
}
