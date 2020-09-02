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
import models.SmsMetadata;

public class SnsAccess {

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
     * This method is used to send a message to a user such that the notification will not pop up
     * for that user.
     *
     * @param arn      The arn of the target of this message.
     * @param metadata This contains the action and payload information to be used by the front
     *                 end.
     */
    //to allow the notification to get sent without popping up, just don't add the notification
    public PublishResult sendMutedMessage(final String arn, final SmsMetadata metadata)
        throws JsonProcessingException {
        Map<String, Object> notification = ImmutableMap.of(
            "data", ImmutableMap.of(
                "click_action", "FLUTTER_NOTIFICATION_CLICK",
                "default", "default message",
                "metadata", metadata.asMap()
            )
        );

        final String jsonNotification = JsonHelper
            .serializeObject(ImmutableMap.of("GCM", notification));

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
     * This method is used to send a message to a user such that they will see a notification.
     *
     * @param arn      The arn of the target of this message.
     * @param title    The title of the notification.
     * @param body     The body of the notification.
     * @param tag      The tag to be attached to the notification. The tag stops multiple messages
     *                 about the same subject from appearing on the user's device. For example, a
     *                 second message about a specific event will replace the first message.
     * @param metadata This contains the action and payload information to be used by the front
     *                 end.
     */
    public PublishResult sendMessage(final String arn, final String title, final String body,
        final String tag, final SmsMetadata metadata, final String channelId)
        throws JsonProcessingException {
        final Map<String, Object> notification = ImmutableMap.of(
            "notification", ImmutableMap.of(
                "title", title,
                "body", body,
                "tag", tag
            ),
            "android", ImmutableMap.of(
                "notification", ImmutableMap.of("channel_id", channelId)
            ),
            // delivered into the intent of the android activity
            "data", ImmutableMap.of(
                "click_action", "FLUTTER_NOTIFICATION_CLICK",
                "default", "default message",
                "metadata", metadata.asMap()
            )
        );

        final String jsonNotification = JsonHelper
            .serializeObject(ImmutableMap.of("GCM", notification));

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
