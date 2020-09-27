package controllers;

import exceptions.ManagerExecutionException;
import exceptions.MissingApiRequestKeyException;
import exceptions.UserNotFoundException;
import helpers.ErrorMessage;
import helpers.JsonHelper;
import helpers.Metrics;
import helpers.RequestFields;
import helpers.ResultStatus;
import interfaces.ApiRequestController;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import managers.UpdateExerciseManager;
import models.ExerciseUser;
import models.User;
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
                final Map<String, Object> exerciseUserMap = (Map<String, Object>) jsonBody
                    .get(RequestFields.EXERCISE);
                final ExerciseUser exerciseUser = new ExerciseUser(exerciseUserMap);

                Injector.getInjector(metrics).inject(this);
                final User result = this.updateExerciseManager
                    .updateExercise(activeUser, exerciseId, exerciseUser);
                resultStatus = ResultStatus.successful(JsonHelper.serializeMap(result.asMap()));
            } catch (ManagerExecutionException meu) {
                metrics.log("Input error: " + meu.getMessage());
                resultStatus = ResultStatus.failureBadEntity(meu.getMessage());
            } catch (UserNotFoundException unfe) {
                metrics.logWithBody(new ErrorMessage<>(classMethod, unfe));
                resultStatus = ResultStatus.failureBadEntity(unfe.getMessage());
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
