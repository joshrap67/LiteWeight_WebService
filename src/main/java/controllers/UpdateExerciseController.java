package controllers;

import exceptions.MissingApiRequestKeyException;
import helpers.ErrorMessage;
import helpers.Metrics;
import helpers.RequestFields;
import helpers.ResultStatus;
import interfaces.ApiRequestController;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import managers.UpdateExerciseManager;
import modules.Injector;

public class UpdateExerciseController implements ApiRequestController {

    @Inject
    public UpdateExerciseManager updateExerciseManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> jsonBody,
        Metrics metrics) throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;

        final List<String> requiredKeys = Arrays
            .asList(RequestFields.ACTIVE_USER, RequestFields.EXERCISE,
                RequestFields.EXERCISE_ID);

        if (jsonBody.keySet().containsAll(requiredKeys)) {
            try {
                final String activeUser = (String) jsonBody.get(RequestFields.ACTIVE_USER);
                final String exerciseId = (String) jsonBody.get(RequestFields.EXERCISE_ID);
                final Map<String, Object> exerciseUser = (Map<String, Object>) jsonBody
                    .get(RequestFields.EXERCISE);

                Injector.getInjector(metrics).inject(this);
                resultStatus = this.updateExerciseManager
                    .execute(activeUser, exerciseId, exerciseUser);
            } catch (Exception e) {
                metrics.logWithBody(new ErrorMessage<>(classMethod, e));
                resultStatus = ResultStatus.failureBadRequest("Exception in " + classMethod);
            }
        } else {
            throw new MissingApiRequestKeyException(requiredKeys);
        }
        return resultStatus;
    }
}
