package controllers;

import exceptions.ManagerExecutionException;
import exceptions.MissingApiRequestKeyException;
import exceptions.UserNotFoundException;
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
import managers.NewWorkoutManager;
import models.Routine;
import models.Workout;
import modules.Injector;
import responses.UserWithWorkout;

public class NewWorkoutController implements ApiRequestController {

    @Inject
    public NewWorkoutManager newWorkoutManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> json,
        Metrics metrics) throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;

        final List<String> requiredKeys = Arrays
            .asList(RequestFields.ACTIVE_USER, Workout.WORKOUT_NAME, Workout.ROUTINE);

        if (json.keySet().containsAll(requiredKeys)) {
            try {
                final String activeUser = (String) json.get(RequestFields.ACTIVE_USER);
                final String workoutName = (String) json.get(Workout.WORKOUT_NAME);
                final Map<String, Object> routineMap = (Map<String, Object>) json
                    .get(Workout.ROUTINE);
                final Routine routine = new Routine(routineMap);

                Injector.getInjector(metrics).inject(this);
                final UserWithWorkout userWithWorkout = this.newWorkoutManager
                    .createNewWorkout(activeUser, workoutName, routine);
                resultStatus = ResultStatus
                    .successful(JsonHelper.serializeMap(userWithWorkout.asMap()));
            } catch (ManagerExecutionException meu) {
                metrics.log("Input error: " + meu.getMessage());
                resultStatus = ResultStatus.failureBadEntity(meu.getMessage());
            } catch (UserNotFoundException unfe) {
                metrics.logWithBody(new ErrorMessage<>(classMethod, unfe));
                resultStatus = ResultStatus.failureBadEntity(unfe.getMessage());
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
