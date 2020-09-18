package managers;

import aws.DatabaseAccess;
import aws.SnsAccess;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.sns.model.DeleteEndpointRequest;
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
     * @param activeUser The user making the api request whose push endpoint is being registered.
     * @return Standard result status object giving insight on whether the request was successful.
     */
    public ResultStatus<String> execute(final String activeUser) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;

        try {
            final User user = this.databaseAccess.getUser(activeUser);

            if (user.getPushEndpointArn() != null) {
                final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                    .withUpdateExpression("set " + User.PUSH_ENDPOINT_ARN + " =:value")
                    .withValueMap(new ValueMap().withNull(":value"));

                this.databaseAccess.updateUser(activeUser, updateItemSpec);

                //we've made it here without exception which means the user doesn't have record of the
                //endpoint anymore, now we try to actually delete the arn. If the following fails we're
                //still safe as there's no reference to the arn in the db anymore
                final DeleteEndpointRequest deleteEndpointRequest = new DeleteEndpointRequest()
                    .withEndpointArn(user.getPushEndpointArn());
                this.snsAccess.unregisterPlatformEndpoint(deleteEndpointRequest);

                resultStatus = ResultStatus.successful("Endpoint unregistered.");
            } else {
                resultStatus = ResultStatus.successful("No endpoint to unregister.");
            }
        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod);
        }

        this.metrics.commonClose(resultStatus.responseCode);
        return resultStatus;
    }
}
