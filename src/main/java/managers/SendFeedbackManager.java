package managers;

import services.NotificationService;
import imports.Config;
import utils.Metrics;
import javax.inject.Inject;

public class SendFeedbackManager {

    private final NotificationService notificationService;
    private final Metrics metrics;

    @Inject
    public SendFeedbackManager(final NotificationService notificationService, final Metrics metrics) {
        this.notificationService = notificationService;
        this.metrics = metrics;
    }

    /**
     * Sends an email to the developer with the feedback that the active user sent. This can also at
     * this time include reports of any kind.
     *
     * @param activeUser   user that is sending the friend feedback.
     * @param feedbackTime time that the feedback was submitted.
     * @param feedback     the actual feedback the user is reporting.
     */
    public void sendFeedback(final String activeUser, final String feedbackTime,
        final String feedback) {
        final String classMethod = this.getClass().getSimpleName() + ".sendFeedback";
        this.metrics.commonSetup(classMethod);

        String formattedMessage = getFormattedFeedback(activeUser, feedbackTime, feedback);
        this.metrics.commonClose(true);
        this.notificationService
            .sendEmail(Config.PUSH_EMAIL_PLATFORM_ARN, "New Feedback", formattedMessage);
    }

    public static String getFormattedFeedback(String senderUsername, String feedbackTime,
        String feedback) {
        return String
            .format("User: %s\nTime: %s\nFeedback: %s", senderUsername, feedbackTime, feedback);
    }
}
