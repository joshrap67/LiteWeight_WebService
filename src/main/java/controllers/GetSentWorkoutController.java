package controllers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import exceptions.MissingApiRequestKeyException;
import exceptions.UserNotFoundException;
import exceptions.WorkoutNotFoundException;
import helpers.ErrorMessage;
import helpers.JsonHelper;
import helpers.Metrics;
import helpers.RequestFields;
import helpers.ResultStatus;
import interfaces.ApiRequestController;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import managers.GetSentWorkoutManager;
import models.SentWorkout;
import models.User;
import modules.Injector;

public class GetSentWorkoutController implements ApiRequestController {

    @Inject
    public GetSentWorkoutManager getSentWorkoutManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> jsonMap,
        Metrics metrics) throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;
        final List<String> requiredKeys = Arrays
            .asList(RequestFields.ACTIVE_USER, SentWorkout.SENT_WORKOUT_ID);

        if (jsonMap.keySet().containsAll(requiredKeys)) {
            try {
                Injector.getInjector(metrics).inject(this);

                final String username = (String) jsonMap.get(User.USERNAME);
                final String workoutId = (String) jsonMap.get(SentWorkout.SENT_WORKOUT_ID);
                final SentWorkout sentWorkout = this.getSentWorkoutManager
                    .getSentWorkout(username, workoutId);

                resultStatus = ResultStatus
                    .successful(JsonHelper.serializeMap(Maps.newHashMap(
                        ImmutableMap.<String, Object>builder()
                            .put(RequestFields.SENT_WORKOUT, sentWorkout.asResponse())
                            .build())));
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
