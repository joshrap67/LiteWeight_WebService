package aws;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.DeleteEndpointRequest;
import com.amazonaws.services.sns.model.DeleteEndpointResult;
import com.amazonaws.services.sns.model.EndpointDisabledException;
import com.amazonaws.services.sns.model.GetEndpointAttributesRequest;
import com.amazonaws.services.sns.model.GetPlatformApplicationAttributesRequest;
import com.amazonaws.services.sns.model.GetPlatformApplicationAttributesResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import helpers.Config;
import helpers.JsonHelper;
import java.security.InvalidParameterException;
import java.util.Map;
import models.NotificationData;

public class SnsAccess {

    public static final String friendRequestAction = "friendRequest";
    public static final String canceledFriendRequestAction = "canceledFriendRequest";
    public static final String acceptedFriendRequestAction = "acceptedFriendRequest";
    public static final String declinedFriendRequestAction = "declinedFriendRequest";
    public static final String removeFriendAction = "removeFriend";
    private final AmazonSNSClient client;

    public SnsAccess() {
        this.client = (AmazonSNSClient) AmazonSNSClient.builder()
            .withRegion(Config.REGION)
            .withCredentials(new EnvironmentVariableCredentialsProvider())
            .build();
    }

    public SnsAccess(final AmazonSNSClient amazonSnsClient) {
        this.client = amazonSnsClient;
    }

    /**
     * This method is used to create a new platform endpoint to be used for SNS.
     *
     * @param createPlatformEndpointRequest A request containing the details of the platform
     *                                      endpoint that is to be created.
     */
    public CreatePlatformEndpointResult registerPlatformEndpoint(
        final CreatePlatformEndpointRequest createPlatformEndpointRequest)
        throws AmazonServiceException {
        return client.createPlatformEndpoint(createPlatformEndpointRequest);
    }

    public Map<String, String> getEndpointAttributes(final String endpointArn) {
        return this.client.getEndpointAttributes(new GetEndpointAttributesRequest()
            .withEndpointArn(endpointArn)).getAttributes();
    }

    /**
     * This method is used to delete platform endpoints that are no longer being used.
     *
     * @param deleteEndpointRequest A request containing the details of the endpoint to be deleted.
     */
    public DeleteEndpointResult unregisterPlatformEndpoint(
        final DeleteEndpointRequest deleteEndpointRequest) throws InvalidParameterException {
        return this.client.deleteEndpoint(deleteEndpointRequest);
    }

    /**
     * This method is used to send a message to a front end device. Note that the frontend is
     * responsible for creating the push notification itself, this messages sent only contains the
     * data the frontend requires.
     *
     * @param arn              The arn of the target of this message.
     * @param notificationData This contains the action and payload information to be used by the
     *                         front end.
     */
    public PublishResult sendMessage(final String arn, final NotificationData notificationData)
        throws JsonProcessingException {
        if (arn == null) {
            return null;
        }
        Map<String, Object> notification = ImmutableMap.of(
            "data", ImmutableMap.of(
                "default", "default message",
                "metadata", notificationData.asMap()
            )
        );

        final String jsonNotification = JsonHelper
            .serializeMap(ImmutableMap.of("GCM", JsonHelper.serializeMap(notification)));

        final PublishRequest publishRequest = new PublishRequest()
            .withTargetArn(arn)
            .withMessage(jsonNotification);
        publishRequest.setMessageStructure("json");

        PublishResult publishResult;
        try {
            publishResult = this.client.publish(publishRequest);
        } catch (final EndpointDisabledException ede) {
            //this isn't an error on our end, read more about this exception here:
            //https://forums.aws.amazon.com/thread.jspa?threadID=174551
            publishResult = new PublishResult();
        }

        return publishResult;
    }

    /**
     * This method is used to fetch attributes when given a target arn.
     *
     * @param platformArn The arn of the platform that we want to fetch the attributes of.
     */
    public GetPlatformApplicationAttributesResult getPlatformAttributes(final String platformArn) {
        return this.client
            .getPlatformApplicationAttributes(
                new GetPlatformApplicationAttributesRequest()
                    .withPlatformApplicationArn(platformArn));
    }
}
