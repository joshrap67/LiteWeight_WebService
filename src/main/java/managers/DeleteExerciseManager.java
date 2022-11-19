package managers;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import daos.UserDAO;
import daos.WorkoutDAO;
import utils.Metrics;
import utils.WorkoutUtils;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import models.OwnedExercise;
import models.Routine;
import models.User;
import models.Workout;

public class DeleteExerciseManager {

    public final UserDAO userDAO;
    public final Metrics metrics;
    public final WorkoutDAO workoutDAO;

    @Inject
    public DeleteExerciseManager(final UserDAO userDAO, final WorkoutDAO workoutDAO,
        final Metrics metrics) {
        this.userDAO = userDAO;
        this.workoutDAO = workoutDAO;
        this.metrics = metrics;
    }

    /**
     * This method deletes an exercise from a user's owned exercise mapping. It also removes this exercise from any
     * workout that contains it.
     *
     * @param exerciseId Id of the exercise that is to be deleted
     */
    public void deleteExercise(final String activeUser, final String exerciseId) throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".deleteExercise";
        this.metrics.commonSetup(classMethod);

        try {
            final User user = this.userDAO.getUser(activeUser);

            final OwnedExercise ownedExercise = user.getOwnedExercises().get(exerciseId);
            List<String> workoutsToUpdate = new ArrayList<>(ownedExercise.getWorkouts().keySet());
            updateWorkouts(exerciseId, workoutsToUpdate);
            user.getOwnedExercises().remove(exerciseId);

            UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withUpdateExpression("set " + User.EXERCISES + "= :exerciseMap")
                .withValueMap(new ValueMap().withMap(":exerciseMap", user.getOwnedExercisesMap()));
            this.userDAO.updateUser(user.getUsername(), updateItemSpec);
            this.metrics.commonClose(true);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }

    private void updateWorkouts(final String exerciseId, final List<String> workoutIds) throws Exception {
        // because the number of workouts could go above 25 (max for transaction) just have to do a bunch of blind updates
        for (String workoutId : workoutIds) {
            final Workout workout = this.workoutDAO.getWorkout(workoutId);
            Routine.deleteExerciseFromRoutine(exerciseId, workout.getRoutine());

            final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withUpdateExpression("set #routine =:routineMap")
                .withValueMap(new ValueMap().withMap(":routineMap", workout.getRoutine().asMap()))
                .withNameMap(new NameMap().with("#routine", Workout.ROUTINE));
            this.workoutDAO.updateWorkout(workoutId, updateItemSpec);
        }
    }
}
