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
import managers.NewExerciseManager;
import models.OwnedExercise;
import modules.Injector;
import responses.ExerciseUserResponse;

public class NewExerciseController implements ApiRequestController {

    @Inject
    public NewExerciseManager newExerciseManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> jsonBody,
        Metrics metrics) throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;

        final List<String> requiredKeys = Arrays
            .asList(RequestFields.ACTIVE_USER, OwnedExercise.EXERCISE_NAME,
                OwnedExercise.FOCUSES);

        if (jsonBody.keySet().containsAll(requiredKeys)) {
            try {
                final String activeUser = (String) jsonBody.get(RequestFields.ACTIVE_USER);
                final String exerciseName = (String) jsonBody.get(OwnedExercise.EXERCISE_NAME);
                final List<String> focuses = (List<String>) jsonBody.get(OwnedExercise.FOCUSES);

                Injector.getInjector(metrics).inject(this);
                final ExerciseUserResponse result = this.newExerciseManager
                    .newExercise(activeUser, exerciseName, focuses);
                resultStatus = ResultStatus.successful(JsonHelper.serializeMap(result.asResponse()));
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
