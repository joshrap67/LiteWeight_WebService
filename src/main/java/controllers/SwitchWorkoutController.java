package controllers;

import exceptions.MissingApiRequestKeyException;
import exceptions.UnauthorizedException;
import exceptions.UserNotFoundException;
import exceptions.WorkoutNotFoundException;
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
import managers.SwitchWorkoutManager;
import models.Workout;
import modules.Injector;
import responses.UserWithWorkout;

public class SwitchWorkoutController implements ApiRequestController {

    @Inject
    public SwitchWorkoutManager switchWorkoutManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> jsonBody,
        Metrics metrics) throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;
        final List<String> requiredKeys = Arrays
            .asList(RequestFields.ACTIVE_USER, Workout.WORKOUT_ID, RequestFields.WORKOUT);

        if (jsonBody.keySet().containsAll(requiredKeys)) {
            try {
                final String activeUser = (String) jsonBody.get(RequestFields.ACTIVE_USER);
                final String newWorkoutId = (String) jsonBody.get(Workout.WORKOUT_ID);
                final Map<String, Object> oldWorkoutMap = (Map<String, Object>) jsonBody.get(RequestFields.WORKOUT);
                final Workout oldWorkout = new Workout(oldWorkoutMap);

                Injector.getInjector(metrics).inject(this);
                final UserWithWorkout result = this.switchWorkoutManager.switchWorkout(activeUser, newWorkoutId,
                    oldWorkout);
                resultStatus = ResultStatus.successful(JsonUtils.serializeMap(result.asResponse()));
            } catch (WorkoutNotFoundException | UnauthorizedException | UserNotFoundException exception) {
                metrics.logWithBody(new ErrorMessage<>(classMethod, exception));
                resultStatus = ResultStatus.failureBadRequest(exception.getMessage());
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
