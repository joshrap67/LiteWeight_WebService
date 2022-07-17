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
import managers.NewExerciseManager;
import models.OwnedExercise;
import modules.Injector;
import responses.OwnedExerciseResponse;

public class NewExerciseController implements ApiRequestController {

    @Inject
    public NewExerciseManager newExerciseManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> jsonBody,
        Metrics metrics) throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;
        final List<String> requiredKeys = Arrays
            .asList(
                RequestFields.ACTIVE_USER,
                OwnedExercise.EXERCISE_NAME,
                OwnedExercise.FOCUSES,
                OwnedExercise.DEFAULT_WEIGHT,
                OwnedExercise.DEFAULT_SETS,
                OwnedExercise.DEFAULT_REPS,
                OwnedExercise.DEFAULT_DETAILS,
                OwnedExercise.VIDEO_URL
            );

        if (jsonBody.keySet().containsAll(requiredKeys)) {
            try {
                // todo trim all inputs
                final String activeUser = ((String) jsonBody.get(RequestFields.ACTIVE_USER)).trim();
                final String exerciseName = ((String) jsonBody.get(OwnedExercise.EXERCISE_NAME)).trim();
                final double defaultWeight = (double) jsonBody.get(OwnedExercise.DEFAULT_WEIGHT);
                final int defaultSets = (int) jsonBody.get(OwnedExercise.DEFAULT_SETS);
                final int defaultReps = (int) jsonBody.get(OwnedExercise.DEFAULT_REPS);
                final String defaultDetails = ((String) jsonBody.get(OwnedExercise.DEFAULT_DETAILS)).trim();
                final String videoURL = ((String) jsonBody.get(OwnedExercise.VIDEO_URL)).trim();
                final List<String> focuses = (List<String>) jsonBody.get(OwnedExercise.FOCUSES);

                Injector.getInjector(metrics).inject(this);
                final OwnedExerciseResponse result = this.newExerciseManager
                    .newExercise(activeUser, exerciseName, defaultWeight, defaultSets, defaultReps,
                        defaultDetails, videoURL, focuses);
                resultStatus = ResultStatus.successful(JsonUtils.serializeMap(result.asResponse()));
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
