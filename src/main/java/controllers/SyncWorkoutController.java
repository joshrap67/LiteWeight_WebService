package controllers;

import exceptions.MissingApiRequestKeyException;
import utils.ErrorMessage;
import utils.Metrics;
import imports.RequestFields;
import imports.ResultStatus;
import interfaces.ApiRequestController;
import java.util.Collections;
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
    public ResultStatus<String> processApiRequest(Map<String, Object> jsonBody,
        Metrics metrics) throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;
        final List<String> requiredKeys = Collections.singletonList(RequestFields.WORKOUT);

        if (jsonBody.keySet().containsAll(requiredKeys)) {
            try {
                final Map<String, Object> workoutMap = (Map<String, Object>) jsonBody
                    .get(RequestFields.WORKOUT);
                final Workout workout = new Workout(workoutMap);

                Injector.getInjector(metrics).inject(this);
                this.syncWorkoutManager.syncWorkout(workout);
                resultStatus = ResultStatus.successful("Workout synced successfully.");
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
