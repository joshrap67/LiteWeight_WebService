package controllers;

import exceptions.MissingApiRequestKeyException;
import exceptions.UnauthorizedException;
import java.util.Arrays;
import utils.ErrorMessage;
import utils.Metrics;
import imports.RequestFields;
import imports.ResultStatus;
import interfaces.ApiRequestController;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import managers.SyncWorkoutManager;
import models.Workout;
import modules.Injector;

public class SyncWorkoutController implements ApiRequestController {

    @Inject
    public SyncWorkoutManager syncWorkoutManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> jsonBody, Metrics metrics)
        throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;
        final List<String> requiredKeys = Arrays.asList(RequestFields.ACTIVE_USER, RequestFields.WORKOUT);

        if (jsonBody.keySet().containsAll(requiredKeys)) {
            try {
                final Map<String, Object> workoutMap = (Map<String, Object>) jsonBody.get(RequestFields.WORKOUT);
                final String activeUser = (String) jsonBody.get(RequestFields.ACTIVE_USER);
                final Workout workout = new Workout(workoutMap);

                Injector.getInjector(metrics).inject(this);
                this.syncWorkoutManager.syncWorkout(activeUser, workout);
                resultStatus = ResultStatus.successful("Workout synced successfully.");
            } catch (UnauthorizedException exception) {
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
