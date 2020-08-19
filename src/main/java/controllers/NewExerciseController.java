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
import managers.NewExerciseManager;
import managers.UpdateExerciseManager;
import models.ExerciseUser;
import modules.Injector;

public class NewExerciseController implements ApiRequestController {

    @Inject
    public NewExerciseManager newExerciseManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> jsonBody,
        Metrics metrics) throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;

        final List<String> requiredKeys = Arrays
            .asList(RequestFields.ACTIVE_USER, ExerciseUser.EXERCISE_NAME,
                ExerciseUser.FOCUSES);

        if (jsonBody.keySet().containsAll(requiredKeys)) {
            try {
                final String activeUser = (String) jsonBody.get(RequestFields.ACTIVE_USER);
                final String exerciseName = (String) jsonBody.get(ExerciseUser.EXERCISE_NAME);
                final List<String> focuses = (List<String>) jsonBody.get(ExerciseUser.FOCUSES);

                Injector.getInjector(metrics).inject(this);
                resultStatus = this.newExerciseManager
                    .execute(activeUser, exerciseName, focuses);
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
