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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import models.User;

public class RegisterEndpointTokenManager {

    private static final String USER_DATA_KEY = "CustomUserData";

    private final DatabaseAccess databaseAccess;
    private final SnsAccess snsAccess;
    private final RemoveEndpointTokenManager removeEndpointTokenManager;
    private final Metrics metrics;

    @Inject
    public RegisterEndpointTokenManager(final DatabaseAccess databaseAccess,
        final SnsAccess snsAccess,
        final RemoveEndpointTokenManager removeEndpointTokenManager, final Metrics metrics) {
        this.databaseAccess = databaseAccess;
        this.snsAccess = snsAccess;
        this.removeEndpointTokenManager = removeEndpointTokenManager;
        this.metrics = metrics;
    }

    /**
     * This function takes in a device token registered in google cloud messaging and creates a SNS
     * endpoint for this token and then registers the ARN of the SNS endpoint on the user item.
     *
     * @param activeUser  The user making the api request whos push endpoint is being registered.
     * @param deviceToken This is the GCM token for the user's device that is used to register the
     *                    endpoint.
     * @return Standard result status object giving insight on whether the request was successful.
     */
    public ResultStatus<String> execute(final String activeUser, final String deviceToken) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;
        try {
            this.attemptToRegisterUserEndpoint(activeUser, deviceToken);
            resultStatus = ResultStatus.successful("User push arn set successfully.");
        } catch (final InvalidParameterException ipe) {
            //The error handling here is obtained from aws doc: https://docs.aws.amazon.com/sns/latest/dg/mobile-platform-endpoint.html#mobile-platform-endpoint-sdk-examples
            final String message = ipe.getErrorMessage();
            final Pattern p = Pattern
                .compile(".*Endpoint (arn:aws:sns[^ ]+) already exists with the same [Tt]oken.*");
            final Matcher m = p.matcher(message);
            if (m.matches()) {
                // The platform endpoint already exists for this token

                // We have to get the current user associated with the arn and unsubscribe them then we have to subscribe the new user
                final String endpointArn = m.group(1);

                final Map<String, String> endpointAttributes = this.snsAccess
                    .getEndpointAttributes(endpointArn);

                final String oldUsername = endpointAttributes.get(USER_DATA_KEY);

                if (this.removeEndpointTokenManager.execute(oldUsername).success) {
                    // by the chance the user that owned this had their mapping removed but the endpoint still existed, we do this for sanity
                    this.snsAccess
                        .unregisterPlatformEndpoint(
                            new DeleteEndpointRequest().withEndpointArn(endpointArn));

                    this.attemptToRegisterUserEndpoint(activeUser, deviceToken);
                    resultStatus = ResultStatus.successful("User push arn set successfully.");
                } else {
                    this.metrics.logWithBody(new ErrorMessage<>(classMethod,
                        "Unable to unregister endpoint from other user."));
                    resultStatus = ResultStatus
                        .failureBadEntity("Unable to unregister endpoint from other user.");
                }
            } else {
                this.metrics.logWithBody(new ErrorMessage<>(classMethod, ipe));
                resultStatus = ResultStatus
                    .failureBadEntity("InvalidParameterException in " + classMethod);
            }
        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod);
        }

        this.metrics.commonClose(resultStatus.success);
        return resultStatus;
    }

    private void attemptToRegisterUserEndpoint(final String activeUser,
        final String deviceToken) throws InvalidParameterException {
        // first thing to do is register the device token with SNS
        final CreatePlatformEndpointRequest createPlatformEndpointRequest =
            new CreatePlatformEndpointRequest()
                .withPlatformApplicationArn(Config.PUSH_SNS_PLATFORM_ARN_DEV)
                .withToken(deviceToken)
                .withCustomUserData(activeUser);
        final CreatePlatformEndpointResult createPlatformEndpointResult = this.snsAccess
            .registerPlatformEndpoint(createPlatformEndpointRequest);

        // this creation will give a new ARN for the sns endpoint associated with the device token
        final String userEndpointArn = createPlatformEndpointResult.getEndpointArn();

        // need to register the ARN for the user's device on the user item
        final String updateExpression = "set " + User.PUSH_ENDPOINT_ARN + " = :userEndpointArn";
        final ValueMap valueMap = new ValueMap().withString(":userEndpointArn", userEndpointArn);
        final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withUpdateExpression(updateExpression)
            .withValueMap(valueMap);

        this.databaseAccess.updateUser(activeUser, updateItemSpec);
    }
}
