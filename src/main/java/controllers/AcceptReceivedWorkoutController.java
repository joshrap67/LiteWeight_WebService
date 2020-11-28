package controllers;

import exceptions.ManagerExecutionException;
import exceptions.MissingApiRequestKeyException;
import exceptions.UserNotFoundException;
import utils.ErrorMessage;
import utils.JsonHelper;
import utils.Metrics;
import imports.RequestFields;
import imports.ResultStatus;
import interfaces.ApiRequestController;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import managers.AcceptReceivedWorkoutManager;
import models.SharedWorkout;
import models.Workout;
import modules.Injector;
import responses.AcceptWorkoutResponse;

public class AcceptReceivedWorkoutController implements ApiRequestController {

    @Inject
    public AcceptReceivedWorkoutManager acceptReceivedWorkoutManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> json,
        Metrics metrics) throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;
        final List<String> requiredKeys = Arrays
            .asList(RequestFields.ACTIVE_USER, SharedWorkout.SENT_WORKOUT_ID);

        if (json.keySet().containsAll(requiredKeys)) {
            try {
                final String activeUser = (String) json.get(RequestFields.ACTIVE_USER);
                final String workoutId = (String) json.get(SharedWorkout.SENT_WORKOUT_ID);
                String optionalName = null;
                if (json.containsKey(Workout.WORKOUT_NAME)) {
                    optionalName = (String) json.get(Workout.WORKOUT_NAME);
                }

                Injector.getInjector(metrics).inject(this);
                AcceptWorkoutResponse result = this.acceptReceivedWorkoutManager
                    .acceptReceivedWorkout(activeUser, workoutId, optionalName);
                resultStatus = ResultStatus
                    .successful(JsonHelper.serializeMap(result.asResponse()));
            } catch (ManagerExecutionException meu) {
                metrics.log("Input error: " + meu.getMessage());
                resultStatus = ResultStatus.failureBadRequest(meu.getMessage());
            } catch (UserNotFoundException unfe) {
                metrics.logWithBody(new ErrorMessage<>(classMethod, unfe));
                resultStatus = ResultStatus.failureBadRequest(unfe.getMessage());
            } catch (Exception e) {
                metrics.logWithBody(new ErrorMessage<>(classMethod, e));
                resultStatus = ResultStatus.failureBadRequest("Unable to accept workout.");
            }
        } else {
            throw new MissingApiRequestKeyException(requiredKeys);
        }

        return resultStatus;
    }
}
