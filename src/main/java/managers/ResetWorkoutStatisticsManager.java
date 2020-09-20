package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import exceptions.UserNotFoundException;
import helpers.ErrorMessage;
import helpers.JsonHelper;
import helpers.Metrics;
import helpers.ResultStatus;
import java.util.Optional;
import javax.inject.Inject;
import models.User;
import models.WorkoutUser;

public class ResetWorkoutStatisticsManager {

    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;

    @Inject
    public ResetWorkoutStatisticsManager(final DatabaseAccess databaseAccess,
        final Metrics metrics) {
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
            final User user = Optional.ofNullable(this.databaseAccess.getUser(activeUser))
                .orElseThrow(
                    () -> new UserNotFoundException(String.format("%s not found", activeUser)));

            final WorkoutUser workoutUser = user.getUserWorkouts().get(workoutId);
            workoutUser.setAverageExercisesCompleted(0.0);
            workoutUser.setTimesCompleted(0);
            workoutUser.setTotalExercisesSum(0);

            final UpdateItemSpec updateUserItemData = new UpdateItemSpec()
                .withUpdateExpression("set " + User.WORKOUTS + ".#workoutId= :workoutsMap")
                .withValueMap(new ValueMap().withMap(":workoutsMap", workoutUser.asMap()))
                .withNameMap(new NameMap().with("#workoutId", workoutId));
            this.databaseAccess.updateUser(activeUser, updateUserItemData);

            resultStatus = ResultStatus.successful(JsonHelper.serializeMap(user.asMap()));
        } catch (UserNotFoundException unfe) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, unfe));
            resultStatus = ResultStatus.failureBadEntity(unfe.getMessage());
        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod + ". " + e);
        }

        this.metrics.commonClose(resultStatus.success);
        return resultStatus;
    }
}
