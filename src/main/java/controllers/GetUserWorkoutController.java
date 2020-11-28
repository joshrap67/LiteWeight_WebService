package controllers;

import com.google.common.collect.ImmutableList;
import exceptions.MissingApiRequestKeyException;
import exceptions.UserNotFoundException;
import exceptions.WorkoutNotFoundException;
import utils.ErrorMessage;
import utils.JsonHelper;
import utils.Metrics;
import imports.RequestFields;
import imports.ResultStatus;
import interfaces.ApiRequestController;
import java.util.Map;
import javax.inject.Inject;
import managers.GetUserWorkoutManager;
import models.User;
import modules.Injector;
import responses.UserWithWorkout;

public class GetUserWorkoutController implements ApiRequestController {

    @Inject
    public GetUserWorkoutManager getUserWorkoutManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> jsonMap,
        Metrics metrics) throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;
        try {
            Injector.getInjector(metrics).inject(this);

            if (jsonMap.containsKey(User.USERNAME)) {
                final String username = (String) jsonMap.get(User.USERNAME);
                final UserWithWorkout userWithWorkout = this.getUserWorkoutManager
                    .getUserWithWorkout(username);
                resultStatus = ResultStatus
                    .successful(JsonHelper.serializeMap(userWithWorkout.asResponse()));
            } else if (jsonMap.containsKey(RequestFields.ACTIVE_USER)) {
                final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
                final UserWithWorkout userWithWorkout = this.getUserWorkoutManager
                    .getUserWithWorkout(activeUser);
                resultStatus = ResultStatus
                    .successful(JsonHelper.serializeMap(userWithWorkout.asResponse()));
            } else {
                throw new MissingApiRequestKeyException(
                    ImmutableList.of(RequestFields.ACTIVE_USER));
            }
        } catch (final MissingApiRequestKeyException e) {
            throw e;
        } catch (UserNotFoundException | WorkoutNotFoundException exception) {
            metrics.logWithBody(new ErrorMessage<>(classMethod, exception));
            resultStatus = ResultStatus.failureBadRequest(exception.getMessage());
        } catch (final Exception e) {
            metrics.logWithBody(new ErrorMessage<Map>(classMethod, e));
            resultStatus = ResultStatus.failureBadRequest("Exception in " + classMethod);
        }

        return resultStatus;
    }
}
