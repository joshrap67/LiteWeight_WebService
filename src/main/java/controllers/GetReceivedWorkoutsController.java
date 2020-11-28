package controllers;

import exceptions.MissingApiRequestKeyException;
import exceptions.UserNotFoundException;
import exceptions.WorkoutNotFoundException;
import utils.ErrorMessage;
import utils.JsonHelper;
import utils.Metrics;
import utils.Parser;
import imports.RequestFields;
import imports.ResultStatus;
import interfaces.ApiRequestController;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import managers.GetReceivedWorkoutsManager;
import models.SharedWorkoutMeta;
import modules.Injector;

public class GetReceivedWorkoutsController implements ApiRequestController {

    @Inject
    public GetReceivedWorkoutsManager getReceivedWorkoutsManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> jsonMap,
        Metrics metrics) throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;
        final List<String> requiredKeys = Arrays
            .asList(RequestFields.ACTIVE_USER, RequestFields.BATCH_NUMBER);

        if (jsonMap.keySet().containsAll(requiredKeys)) {
            try {
                Injector.getInjector(metrics).inject(this);

                final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
                final Integer batchNumber = Parser
                    .convertObjectToInteger(jsonMap.get(RequestFields.BATCH_NUMBER));
                final Map<String, SharedWorkoutMeta> receivedWorkouts = this.getReceivedWorkoutsManager
                    .getReceivedWorkouts(activeUser, batchNumber);

                Map<String, Object> retMap = new HashMap<>();
                for (String workoutId : receivedWorkouts.keySet()) {
                    retMap.putIfAbsent(workoutId, receivedWorkouts.get(workoutId).asResponse());
                }

                resultStatus = ResultStatus.successful(JsonHelper.serializeMap(retMap));
            } catch (final MissingApiRequestKeyException e) {
                throw e;
            } catch (UserNotFoundException | WorkoutNotFoundException exception) {
                metrics.logWithBody(new ErrorMessage<>(classMethod, exception));
                resultStatus = ResultStatus.failureBadRequest(exception.getMessage());
            } catch (final Exception e) {
                metrics.logWithBody(new ErrorMessage<>(classMethod, e));
                resultStatus = ResultStatus.failureBadRequest("Exception in " + classMethod);
            }
        } else {
            throw new MissingApiRequestKeyException(requiredKeys);
        }

        return resultStatus;
    }
}
