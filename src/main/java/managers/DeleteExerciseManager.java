package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import helpers.Metrics;
import helpers.WorkoutHelper;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import models.ExerciseUser;
import models.User;
import models.Workout;

public class DeleteExerciseManager {

    public final DatabaseAccess databaseAccess;
    public final Metrics metrics;

    @Inject
    public DeleteExerciseManager(final DatabaseAccess databaseAccess, final Metrics metrics) {
        this.databaseAccess = databaseAccess;
        this.metrics = metrics;
    }

    /**
     * @param exerciseId TODO
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public boolean execute(final String activeUser, final String exerciseId) throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        try {
            final User user = this.databaseAccess.getUser(activeUser);

            final ExerciseUser exerciseUser = user.getUserExercises().get(exerciseId);
            List<String> workoutsToUpdate = new ArrayList<>(exerciseUser.getWorkouts().keySet());
            updateWorkouts(exerciseId, workoutsToUpdate, user);
            user.getUserExercises().remove(exerciseId);

            final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withUpdateExpression("set " + User.EXERCISES + "= :exerciseMap")
                .withValueMap(new ValueMap().withMap(":exerciseMap", user.getUserExercisesMap()));
            this.databaseAccess.updateUser(user.getUsername(), updateItemSpec);
            this.metrics.commonClose(true);
            return true;
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }

    }

    private void updateWorkouts(final String exerciseId, final List<String> workoutIds,
        final User user) throws Exception {
        // because the number of workouts could go above 25 (max for transaction) just have to do a bunch of blind updates
        for (String workoutId : workoutIds) {
            final Workout workout = this.databaseAccess.getWorkout(workoutId);

            WorkoutHelper.deleteExerciseFromRoutine(exerciseId, workout.getRoutine());
            final String newMostFrequentFocus = WorkoutHelper
                .findMostFrequentFocus(user, workout.getRoutine());
            workout.setMostFrequentFocus(newMostFrequentFocus);

            final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withUpdateExpression("set #routine =:routineMap, " +
                    Workout.MOST_FREQUENT_FOCUS + "=:mostFrequentFocusVal")
                .withValueMap(new ValueMap()
                    .withMap(":routineMap", workout.getRoutine().asMap())
                    .withString(":mostFrequentFocusVal", newMostFrequentFocus))
                .withNameMap(new NameMap().with("#routine", Workout.ROUTINE));
            this.databaseAccess.updateWorkout(workoutId, updateItemSpec);
        }
    }
}
