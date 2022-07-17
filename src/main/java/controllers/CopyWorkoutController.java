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
import managers.CopyWorkoutManager;
import models.Workout;
import modules.Injector;
import responses.UserWithWorkout;

public class CopyWorkoutController implements ApiRequestController {

    @Inject
    CopyWorkoutManager copyWorkoutManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> jsonBody, Metrics metrics)
        throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;
        final List<String> requiredKeys = Arrays.asList(RequestFields.ACTIVE_USER, RequestFields.WORKOUT,
            Workout.WORKOUT_NAME);

        if (jsonBody.keySet().containsAll(requiredKeys)) {
            try {
                final String user = (String) jsonBody.get(RequestFields.ACTIVE_USER);
                final String newWorkoutName = (String) jsonBody.get(Workout.WORKOUT_NAME);
                final Map<String, Object> oldWorkoutMap = (Map<String, Object>) jsonBody.get(RequestFields.WORKOUT);
                final Workout oldWorkout = new Workout(oldWorkoutMap);

                Injector.getInjector(metrics).inject(this);
                final UserWithWorkout result = this.copyWorkoutManager.copyWorkout(user, newWorkoutName, oldWorkout);
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
