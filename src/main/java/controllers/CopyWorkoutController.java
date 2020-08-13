package controllers;

import exceptions.MissingApiRequestKeyException;
import helpers.ErrorMessage;
import helpers.Metrics;
import helpers.RequestFields;
import helpers.ResultStatus;
import interfaces.ApiRequestController;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import managers.CopyWorkoutManager;
import models.Workout;
import modules.Injector;

public class CopyWorkoutController implements ApiRequestController {

    @Inject
    CopyWorkoutManager copyWorkoutManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> jsonBody,
        Metrics metrics) throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;

        final List<String> requiredKeys = Arrays
            .asList(RequestFields.ACTIVE_USER, RequestFields.WORKOUT,
                Workout.WORKOUT_NAME);

        if (jsonBody.keySet().containsAll(requiredKeys)) {
            try {
                final String activeUser = (String) jsonBody.get(RequestFields.ACTIVE_USER);
                final String newWorkoutName = (String) jsonBody.get(Workout.WORKOUT_NAME);
                final Map<String, Object> oldWorkout = (Map<String, Object>) jsonBody
                    .get(RequestFields.WORKOUT);

                Injector.getInjector(metrics).inject(this);
                resultStatus = this.copyWorkoutManager
                    .execute(activeUser, newWorkoutName, oldWorkout);
            } catch (Exception e) {
                metrics.logWithBody(new ErrorMessage<>(classMethod, e));
                resultStatus = ResultStatus.failure("Exception in " + classMethod);
            }
        } else {
            throw new MissingApiRequestKeyException(requiredKeys);
        }

        return resultStatus;
    }
}
