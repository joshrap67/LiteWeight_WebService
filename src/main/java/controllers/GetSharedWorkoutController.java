package controllers;

import com.google.common.collect.Maps;
import exceptions.ManagerExecutionException;
import exceptions.MissingApiRequestKeyException;
import exceptions.UserNotFoundException;
import exceptions.WorkoutNotFoundException;
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
import managers.GetSharedWorkoutManager;
import models.SharedWorkout;
import models.User;
import modules.Injector;

public class GetSharedWorkoutController implements ApiRequestController {

    @Inject
    public GetSharedWorkoutManager getSharedWorkoutManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> jsonMap,
        Metrics metrics) throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;
        final List<String> requiredKeys = Arrays
            .asList(RequestFields.ACTIVE_USER, SharedWorkout.SENT_WORKOUT_ID);

        if (jsonMap.keySet().containsAll(requiredKeys)) {
            try {
                Injector.getInjector(metrics).inject(this);

                final String username = (String) jsonMap.get(User.USERNAME);
                final String workoutId = (String) jsonMap.get(SharedWorkout.SENT_WORKOUT_ID);
                final SharedWorkout sharedWorkout = this.getSharedWorkoutManager
                    .getSharedWorkout(username, workoutId);

                resultStatus = ResultStatus
                    .successful(
                        JsonHelper.serializeMap(Maps.newHashMap(sharedWorkout.asResponse())));
            } catch (final MissingApiRequestKeyException e) {
                throw e;
            } catch (ManagerExecutionException meu) {
                metrics.log("Input error: " + meu.getMessage());
                resultStatus = ResultStatus.failureBadRequest(meu.getMessage());
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
