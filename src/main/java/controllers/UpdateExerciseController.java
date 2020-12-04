package controllers;

import exceptions.ManagerExecutionException;
import exceptions.MissingApiRequestKeyException;
import exceptions.UserNotFoundException;
import utils.ErrorMessage;
import utils.JsonUtils;
import utils.Metrics;
import imports.RequestFields;
import imports.ResultStatus;
import interfaces.ApiRequestController;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import managers.UpdateExerciseManager;
import models.OwnedExercise;
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
            .asList(RequestFields.ACTIVE_USER, RequestFields.EXERCISE, RequestFields.EXERCISE_ID);

        if (jsonBody.keySet().containsAll(requiredKeys)) {
            try {
                final String activeUser = (String) jsonBody.get(RequestFields.ACTIVE_USER);
                final String exerciseId = (String) jsonBody.get(RequestFields.EXERCISE_ID);
                final Map<String, Object> exerciseUserMap = (Map<String, Object>) jsonBody
                    .get(RequestFields.EXERCISE);
                final OwnedExercise ownedExercise = new OwnedExercise(exerciseUserMap);

                Injector.getInjector(metrics).inject(this);
                final User result = this.updateExerciseManager
                    .updateExercise(activeUser, exerciseId, ownedExercise);
                resultStatus = ResultStatus
                    .successful(JsonUtils.serializeMap(result.asResponse()));
            } catch (ManagerExecutionException meu) {
                metrics.log("Input error: " + meu.getMessage());
                resultStatus = ResultStatus.failureBadRequest(meu.getMessage());
            } catch (UserNotFoundException unfe) {
                metrics.logWithBody(new ErrorMessage<>(classMethod, unfe));
                resultStatus = ResultStatus.failureBadRequest(unfe.getMessage());
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
