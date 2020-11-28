package controllers;

import exceptions.MissingApiRequestKeyException;
import exceptions.UserNotFoundException;
import models.WorkoutMeta;
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
import managers.ResetWorkoutStatisticsManager;
import models.Workout;
import modules.Injector;

public class ResetWorkoutStatisticsController implements ApiRequestController {

    @Inject
    public ResetWorkoutStatisticsManager resetWorkoutStatisticsManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> json,
        Metrics metrics) throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;
        final List<String> requiredKeys = Arrays
            .asList(RequestFields.ACTIVE_USER, Workout.WORKOUT_ID);

        if (json.keySet().containsAll(requiredKeys)) {
            try {
                final String activeUser = (String) json.get(RequestFields.ACTIVE_USER);
                final String workoutId = (String) json.get(Workout.WORKOUT_ID);

                Injector.getInjector(metrics).inject(this);
                final WorkoutMeta result = this.resetWorkoutStatisticsManager
                    .resetStatistics(activeUser, workoutId);
                resultStatus = ResultStatus
                    .successful(JsonUtils.serializeMap(result.asResponse()));
            } catch (UserNotFoundException unfe) {
                metrics.logWithBody(new ErrorMessage<>(classMethod, unfe));
                resultStatus = ResultStatus.failureBadRequest(unfe.getMessage());
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
