package controllers;

import exceptions.ManagerExecutionException;
import exceptions.MissingApiRequestKeyException;
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
import managers.EditWorkoutManager;
import models.Workout;
import modules.Injector;
import responses.UserWithWorkout;

public class EditWorkoutController implements ApiRequestController {

    @Inject
    EditWorkoutManager editWorkoutManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> json,
        Metrics metrics) throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;
        final List<String> requiredKeys = Arrays
            .asList(RequestFields.ACTIVE_USER, RequestFields.WORKOUT);

        if (json.keySet().containsAll(requiredKeys)) {
            try {
                final String activeUser = (String) json.get(RequestFields.ACTIVE_USER);
                final Workout workout = new Workout((Map<String, Object>) json
                    .get(RequestFields.WORKOUT));

                Injector.getInjector(metrics).inject(this);
                UserWithWorkout result = this.editWorkoutManager.editWorkout(activeUser, workout);
                resultStatus = ResultStatus
                    .successful(JsonUtils.serializeMap(result.asResponse()));
            } catch (ManagerExecutionException meu) {
                metrics.log("Input error: " + meu.getMessage());
                resultStatus = ResultStatus.failureBadRequest(meu.getMessage());
            } catch (WorkoutNotFoundException | UserNotFoundException exception) {
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
