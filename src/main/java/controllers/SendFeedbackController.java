package controllers;

import exceptions.MissingApiRequestKeyException;
import utils.ErrorMessage;
import utils.Metrics;
import imports.RequestFields;
import imports.ResultStatus;
import interfaces.ApiRequestController;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import managers.SendFeedbackManager;
import modules.Injector;

public class SendFeedbackController implements ApiRequestController {

    @Inject
    public SendFeedbackManager sendFeedbackManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> json, Metrics metrics)
        throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;
        final List<String> requiredKeys = Arrays.asList(RequestFields.ACTIVE_USER, RequestFields.FEEDBACK_TIME,
            RequestFields.FEEDBACK);

        if (json.keySet().containsAll(requiredKeys)) {
            try {
                final String activeUser = (String) json.get(RequestFields.ACTIVE_USER);
                final String feedbackTime = (String) json.get(RequestFields.FEEDBACK_TIME);
                final String feedback = (String) json.get(RequestFields.FEEDBACK);

                Injector.getInjector(metrics).inject(this);
                this.sendFeedbackManager.sendFeedback(activeUser, feedbackTime, feedback);
                resultStatus = ResultStatus.successful("Feedback successfully sent.");
            } catch (Exception e) {
                metrics.logWithBody(new ErrorMessage<>(classMethod, e));
                resultStatus = ResultStatus.failureBadRequest("Unable to send feedback.");
            }
        } else {
            throw new MissingApiRequestKeyException(requiredKeys);
        }

        return resultStatus;
    }
}
