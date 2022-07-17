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
import managers.DeleteWorkoutThenFetchWorkoutManager;
import models.Workout;
import modules.Injector;
import responses.UserWithWorkout;

public class DeleteWorkoutThenFetchController implements ApiRequestController {

    @Inject
    public DeleteWorkoutThenFetchWorkoutManager deleteWorkoutThenFetchWorkoutManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> json,
        Metrics metrics) throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;
        final List<String> requiredKeys = Arrays
            .asList(RequestFields.ACTIVE_USER, Workout.WORKOUT_ID, RequestFields.NEXT_WORKOUT_ID);

        if (json.keySet().containsAll(requiredKeys)) {
            try {
                final String activeUser = (String) json.get(RequestFields.ACTIVE_USER);
                final String deletedWorkoutId = (String) json.get(Workout.WORKOUT_ID);
                final String nextWorkoutId = (String) json.get(RequestFields.NEXT_WORKOUT_ID);

                Injector.getInjector(metrics).inject(this);
                final UserWithWorkout result = this.deleteWorkoutThenFetchWorkoutManager.deleteWorkoutThenFetch(
                    activeUser, deletedWorkoutId, nextWorkoutId);
                resultStatus = ResultStatus.successful(JsonUtils.serializeMap(result.asResponse()));
            } catch (UserNotFoundException | UnauthorizedException | WorkoutNotFoundException exception) {
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
