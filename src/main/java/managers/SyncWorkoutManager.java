package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import helpers.ErrorMessage;
import helpers.Metrics;
import helpers.ResultStatus;
import java.util.Map;
import javax.inject.Inject;
import models.Workout;

public class SyncWorkoutManager {

    public final DatabaseAccess databaseAccess;
    public final Metrics metrics;

    @Inject
    public SyncWorkoutManager(DatabaseAccess databaseAccess, Metrics metrics) {
        this.databaseAccess = databaseAccess;
        this.metrics = metrics;
    }

    /**
     * @param activeUser Username of new user to be inserted
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public ResultStatus<String> execute(final String activeUser,
        final Map<String, Object> workoutJson) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;

        try {
            Workout workout = new Workout(workoutJson);
            String workoutId = workout.getWorkoutId();
            // TODO should i do validation to make sure day/week is valid?

            // persist the current workout (routine and current day/week)
            final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withUpdateExpression(
                    "set " +
                        Workout.CURRENT_DAY + " =:" + Workout.CURRENT_DAY + ", " +
                        Workout.CURRENT_WEEK + " =:" + Workout.CURRENT_WEEK + ", " +
                        "#routine =:" + Workout.ROUTINE)
                .withNameMap(new NameMap().with("#routine", Workout.ROUTINE))
                .withValueMap(
                    new ValueMap()
                        .withNumber(":" + Workout.CURRENT_DAY, workout.getCurrentDay())
                        .withNumber(":" + Workout.CURRENT_WEEK, workout.getCurrentWeek())
                        .withMap(":" + Workout.ROUTINE, workout.getRoutine().asMap()));
            this.databaseAccess.updateWorkout(workoutId, updateItemSpec);
            resultStatus = ResultStatus
                .successful("Workout synced successfully"); // todo return workout??
        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod);
        }

        this.metrics.commonClose(resultStatus.responseCode);
        return resultStatus;
    }
}
