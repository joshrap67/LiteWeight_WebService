package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import helpers.ErrorMessage;
import helpers.JsonHelper;
import helpers.Metrics;
import helpers.ResultStatus;
import helpers.UpdateItemData;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import models.User;
import models.Workout;
import models.WorkoutUser;
import responses.UserWithWorkout;

public class ResetWorkoutStatisticsManager {

    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;

    @Inject
    public ResetWorkoutStatisticsManager(DatabaseAccess databaseAccess, Metrics metrics) {
        this.databaseAccess = databaseAccess;
        this.metrics = metrics;
    }

    /**
     * @param workoutId TODO
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public ResultStatus<String> execute(final String activeUser, final String workoutId) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;

        try {
            User user = this.databaseAccess.getUser(activeUser);
            if (user != null) {
                WorkoutUser workoutUser = user.getUserWorkouts().get(workoutId);
                workoutUser.setAverageExercisesCompleted(0.0);
                workoutUser.setTimesCompleted(0);
                workoutUser.setTotalExercisesSum(0);

                final UpdateItemData updateUserItemData = new UpdateItemData(activeUser,
                    DatabaseAccess.USERS_TABLE_NAME)
                    .withUpdateExpression(
                        "set " +
                            User.WORKOUTS + ".#workoutId= :" + User.WORKOUTS)
                    .withValueMap(
                        new ValueMap()
                            .withMap(":" + User.WORKOUTS, workoutUser.asMap()))
                    .withNameMap(new NameMap().with("#workoutId", workoutId));

                // TODO if I do advanced statistics this is where I'd reset it

                // want a transaction since more than one object is being updated at once
                final List<TransactWriteItem> actions = new ArrayList<>();
                actions.add(new TransactWriteItem().withUpdate(updateUserItemData.asUpdate()));

                this.databaseAccess.executeWriteTransaction(actions);

                resultStatus = ResultStatus
                    .successful(
                        JsonHelper
                            .convertObjectToJson(user.asMap()));
            } else {
                this.metrics.log("Active user does not exist");
                resultStatus = ResultStatus.failure("User does not exist.");
            }
        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failure("Exception in " + classMethod + ". " + e);
        }

        this.metrics.commonClose(resultStatus.success);
        return resultStatus;
    }
}
