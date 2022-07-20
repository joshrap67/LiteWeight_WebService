package controllers;

import exceptions.MissingApiRequestKeyException;
import exceptions.UnauthorizedException;
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
import managers.RestartWorkoutManager;
import models.Workout;
import modules.Injector;
import responses.UserWithWorkout;

public class RestartWorkoutController implements ApiRequestController {

    @Inject
    public RestartWorkoutManager restartWorkoutManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> jsonBody, Metrics metrics)
        throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;
        final List<String> requiredKeys = Arrays.asList(RequestFields.ACTIVE_USER, RequestFields.WORKOUT);

        if (jsonBody.keySet().containsAll(requiredKeys)) {
            try {
                final String activeUser = (String) jsonBody.get(RequestFields.ACTIVE_USER);
                final Map<String, Object> workoutMap = (Map<String, Object>) jsonBody.get(RequestFields.WORKOUT);
                final Workout workout = new Workout(workoutMap);

                Injector.getInjector(metrics).inject(this);
                final UserWithWorkout result = this.restartWorkoutManager.restartWorkout(activeUser, workout);
                resultStatus = ResultStatus.successful(JsonUtils.serializeMap(result.asResponse()));
            } catch (UserNotFoundException | UnauthorizedException e) {
                metrics.logWithBody(new ErrorMessage<>(classMethod, e));
                resultStatus = ResultStatus.failureBadRequest(e.getMessage());
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
