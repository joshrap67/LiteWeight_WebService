package managers;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import daos.WorkoutDAO;
import helpers.Metrics;
import javax.inject.Inject;
import models.Workout;

public class SyncWorkoutManager {

    public final WorkoutDAO workoutDAO;
    public final Metrics metrics;

    @Inject
    public SyncWorkoutManager(final WorkoutDAO workoutDAO, final Metrics metrics) {
        this.workoutDAO = workoutDAO;
        this.metrics = metrics;
    }

    /**
     * Syncs a workout in the workout table by updating the current week/day and routine from the
     * values found in the passed in workout.
     *
     * @param workout workout that is to be synced.
     */
    public void syncWorkout(final Workout workout) {
        final String classMethod = this.getClass().getSimpleName() + ".syncWorkout";
        this.metrics.commonSetup(classMethod);

        try {
            String workoutId = workout.getWorkoutId();
            confirmValidCurrentDayAndWeek(workout);

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
            this.workoutDAO.updateWorkout(workoutId, updateItemSpec);

            this.metrics.commonClose(true);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }

    private void confirmValidCurrentDayAndWeek(final Workout workout) {
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
