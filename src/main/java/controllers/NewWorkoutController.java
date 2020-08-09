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
import managers.NewWorkoutManager;
import models.Workout;
import modules.Injector;

public class NewWorkoutController implements ApiRequestController {

    @Inject
    public NewWorkoutManager newWorkoutManager;

    @Override
    public ResultStatus processApiRequest(Map<String, Object> json,
        Metrics metrics) throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;

        final List<String> requiredKeys = Arrays
            .asList(RequestFields.ACTIVE_USER, Workout.WORKOUT_NAME, Workout.ROUTINE);

        if (json.keySet().containsAll(requiredKeys)) {
            try {
                final String activeUser = (String) json.get(RequestFields.ACTIVE_USER);
                final String workoutName = (String) json.get(Workout.WORKOUT_NAME);
                final Map<String, Object> routine = (Map<String, Object>) json.get(Workout.ROUTINE);

                Injector.getInjector(metrics).inject(this);
                resultStatus = this.newWorkoutManager
                    .execute(workoutName, activeUser, routine);
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
