package controllers;

import exceptions.ManagerExecutionException;
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
import managers.RenameWorkoutManager;
import models.User;
import models.Workout;
import modules.Injector;

public class RenameWorkoutController implements ApiRequestController {

    @Inject
    public RenameWorkoutManager renameWorkoutManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> json,
        Metrics metrics) throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;
        final List<String> requiredKeys = Arrays
            .asList(RequestFields.ACTIVE_USER, Workout.WORKOUT_NAME, Workout.WORKOUT_ID);

        if (json.keySet().containsAll(requiredKeys)) {
            try {
                final String activeUser = (String) json.get(RequestFields.ACTIVE_USER);
                final String workoutName = (String) json.get(Workout.WORKOUT_NAME);
                final String workoutId = (String) json.get(Workout.WORKOUT_ID);

                Injector.getInjector(metrics).inject(this);
                final User result = this.renameWorkoutManager.renameWorkout(activeUser, workoutId, workoutName.trim());
                resultStatus = ResultStatus.successful(JsonUtils.serializeMap(result.asResponse()));
            } catch (ManagerExecutionException meu) {
                metrics.log("Input error: " + meu.getMessage());
                resultStatus = ResultStatus.failureBadRequest(meu.getMessage());
            } catch (UserNotFoundException | UnauthorizedException exception) {
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
