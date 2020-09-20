package controllers;

import exceptions.MissingApiRequestKeyException;
import exceptions.UserNotFoundException;
import exceptions.WorkoutNotFoundException;
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
import managers.PopWorkoutManager;
import models.Workout;
import modules.Injector;
import responses.UserWithWorkout;

public class PopWorkoutController implements ApiRequestController {

    @Inject
    public PopWorkoutManager popWorkoutManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> json,
        Metrics metrics) throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;

        final List<String> requiredKeys = Arrays
            .asList(RequestFields.ACTIVE_USER, Workout.WORKOUT_ID);

        if (json.keySet().containsAll(requiredKeys)) {
            try {
                final String activeUser = (String) json.get(RequestFields.ACTIVE_USER);
                final String workoutId = (String) json.get(Workout.WORKOUT_ID);

                Injector.getInjector(metrics).inject(this);
                final UserWithWorkout result = this.popWorkoutManager
                    .execute(activeUser, workoutId);
                resultStatus = ResultStatus.successful(JsonHelper.serializeMap(result.asMap()));
            } catch (UserNotFoundException | WorkoutNotFoundException exception) {
                metrics.logWithBody(new ErrorMessage<>(classMethod, exception));
                resultStatus = ResultStatus.failureBadEntity(exception.getMessage());
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
