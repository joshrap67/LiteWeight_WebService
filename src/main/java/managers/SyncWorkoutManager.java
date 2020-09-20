package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import helpers.ErrorMessage;
import helpers.Metrics;
import helpers.ResultStatus;
import javax.inject.Inject;
import models.Workout;

public class SyncWorkoutManager {

    public final DatabaseAccess databaseAccess;
    public final Metrics metrics;

    @Inject
    public SyncWorkoutManager(final DatabaseAccess databaseAccess, final Metrics metrics) {
        this.databaseAccess = databaseAccess;
        this.metrics = metrics;
    }

    /**
     * @param workout Workout that is to be synced.
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public boolean execute(final Workout workout) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        try {
            String workoutId = workout.getWorkoutId();
            rectifyCurrentDayAndWeek(workout);

            // persist the current workout (routine and current day/week)
            final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withUpdateExpression("set " +
                    Workout.CURRENT_DAY + " =:currentDayVal, " +
                    Workout.CURRENT_WEEK + " =:currentWeekVal, " +
                    "#routine =:routineMap")
                .withNameMap(new NameMap().with("#routine", Workout.ROUTINE))
                .withValueMap(new ValueMap()
                    .withNumber(":currentDayVal", workout.getCurrentDay())
                    .withNumber(":currentWeekVal", workout.getCurrentWeek())
                    .withMap(":routineMap", workout.getRoutine().asMap()));
            this.databaseAccess.updateWorkout(workoutId, updateItemSpec);

            this.metrics.commonClose(true);
            return true;
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }

    private void rectifyCurrentDayAndWeek(final Workout workout) {
        // make sure that the current week according to the frontend is actually valid
        int currentDay = workout.getCurrentDay();
        int currentWeek = workout.getCurrentWeek();
        if (currentWeek >= 0 && currentWeek >= workout.getRoutine().size()) {
            // frontend incorrectly set the current week, so just set it to 0
            workout.setCurrentWeek(0);
        }

        if (currentDay >= 0 && currentDay >= workout.getRoutine().getWeek(currentWeek)
            .size()) {
            // frontend incorrectly set the current day, so just set it to 0
            workout.setCurrentDay(0);
        }
    }
}
