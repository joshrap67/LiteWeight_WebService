package controllers;

import exceptions.MissingApiRequestKeyException;
import exceptions.UserNotFoundException;
import exceptions.WorkoutNotFoundException;
import helpers.ErrorMessage;
import helpers.JsonHelper;
import helpers.Metrics;
import helpers.Parser;
import helpers.RequestFields;
import helpers.ResultStatus;
import interfaces.ApiRequestController;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import managers.GetReceivedWorkoutsManager;
import models.ReceivedWorkoutMeta;
import models.User;
import modules.Injector;
import responses.ReceivedWorkouts;

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

                final String username = (String) jsonMap.get(User.USERNAME);
                final Integer batchNumber = Parser
                    .convertObjectToInteger(jsonMap.get(RequestFields.BATCH_NUMBER));
                final Map<String, ReceivedWorkoutMeta> receivedWorkouts = this.getReceivedWorkoutsManager
                    .getReceivedWorkouts(username, batchNumber);

                resultStatus = ResultStatus
                    .successful(JsonHelper
                        .serializeMap(new ReceivedWorkouts(receivedWorkouts).asResponse()));
            } catch (final MissingApiRequestKeyException e) {
                throw e;
            } catch (UserNotFoundException | WorkoutNotFoundException exception) {
                metrics.logWithBody(new ErrorMessage<>(classMethod, exception));
                resultStatus = ResultStatus.failureBadEntity(exception.getMessage());
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
