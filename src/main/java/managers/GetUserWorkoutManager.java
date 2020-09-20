package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.Item;
import helpers.ErrorMessage;
import helpers.JsonHelper;
import helpers.Metrics;
import helpers.ResultStatus;
import javax.inject.Inject;
import models.User;
import models.Workout;
import responses.UserWithWorkout;

public class GetUserWorkoutManager {

    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;
    private final NewUserManager newUserManager;

    @Inject
    public GetUserWorkoutManager(final DatabaseAccess databaseAccess, final Metrics metrics,
        final NewUserManager newUserManager) {
        this.databaseAccess = databaseAccess;
        this.metrics = metrics;
        this.newUserManager = newUserManager;
    }

    /**
     * This method is used when the user first successfully signs into the app. It provides the user
     * object to the user as well as the current workout if there is one.
     *
     * @param activeUser The user that made the api request, trying to get data about themselves.
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public ResultStatus<String> execute(final String activeUser) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;
        try {
            Item userItem = this.databaseAccess.getUserItem(activeUser);
            if (userItem != null) {
                final User user = new User(userItem);
                String currentWorkoutId = user.getCurrentWorkout();
                UserWithWorkout userWithWorkout;

                if (currentWorkoutId == null) {
                    // user has no workouts
                    userWithWorkout = new UserWithWorkout(user, null);
                } else {
                    // user has a workout so try and fetch it from the DB
                    final Workout workout = new Workout(
                        this.databaseAccess.getWorkoutItem(currentWorkoutId));
                    userWithWorkout = new UserWithWorkout(user, workout);
                }

                resultStatus = ResultStatus
                    .successful(JsonHelper.serializeMap(userWithWorkout.asMap()));
            } else {
                // this will be reached if the user just created an account or it somehow got deleted in DB, so put user in DB
                final ResultStatus<String> result = this.newUserManager.execute(activeUser);
                if (result.responseCode == ResultStatus.SUCCESS_CODE) {
                    UserWithWorkout userWithWorkout = new UserWithWorkout(
                        new User(JsonHelper.deserialize(result.resultMessage)), null);
                    resultStatus = ResultStatus
                        .successful(JsonHelper.serializeMap(userWithWorkout.asMap()));
                } else {
                    this.metrics.log(result.resultMessage);
                    resultStatus = ResultStatus.failureBadEntity(result.resultMessage);
                }
            }
        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod);
        }

        this.metrics.commonClose(resultStatus.success);
        return resultStatus;
    }
}
