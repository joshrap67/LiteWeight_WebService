package controllers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import exceptions.ManagerExecutionException;
import exceptions.MissingApiRequestKeyException;
import exceptions.UnauthorizedException;
import exceptions.UserNotFoundException;
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
import managers.SendWorkoutManager;
import models.SharedWorkout;
import models.User;
import models.Workout;
import modules.Injector;

public class SendWorkoutController implements ApiRequestController {

    @Inject
    public SendWorkoutManager sendWorkoutManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> json,
        Metrics metrics) throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;
        final List<String> requiredKeys = Arrays
            .asList(RequestFields.ACTIVE_USER, User.USERNAME, Workout.WORKOUT_ID);

        if (json.keySet().containsAll(requiredKeys)) {
            try {
                final String activeUser = (String) json.get(RequestFields.ACTIVE_USER);
                final String recipientUsername = (String) json.get(User.USERNAME);
                final String workoutId = (String) json.get(Workout.WORKOUT_ID);

                Injector.getInjector(metrics).inject(this);
                final String sharedWorkoutId = this.sendWorkoutManager.sendWorkout(activeUser, recipientUsername,
                    workoutId);
                resultStatus = ResultStatus.successful(JsonUtils.serializeMap(Maps.newHashMap(
                    ImmutableMap.<String, String>builder()
                        .put(SharedWorkout.SHARED_WORKOUT_ID, sharedWorkoutId)
                        .build())));
            } catch (ManagerExecutionException meu) {
                metrics.log("Input error: " + meu.getMessage());
                resultStatus = ResultStatus.failureBadRequest(meu.getMessage());
            } catch (UserNotFoundException | UnauthorizedException exception) {
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
