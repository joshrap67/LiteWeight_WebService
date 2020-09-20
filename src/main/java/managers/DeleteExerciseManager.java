package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import exceptions.InvalidAttributeException;
import exceptions.UserNotFoundException;
import exceptions.WorkoutNotFoundException;
import helpers.ErrorMessage;
import helpers.Metrics;
import helpers.ResultStatus;
import helpers.WorkoutHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    public ResultStatus<String> execute(final String activeUser, final String exerciseId) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;
        try {
            final User user = Optional.ofNullable(this.databaseAccess.getUser(activeUser))
                .orElseThrow(
                    () -> new UserNotFoundException(String.format("%s not found", activeUser)));

            final ExerciseUser exerciseUser = user.getUserExercises().get(exerciseId);
            List<String> workoutsToUpdate = new ArrayList<>(exerciseUser.getWorkouts().keySet());
            updateWorkouts(exerciseId, workoutsToUpdate, user);
            user.getUserExercises().remove(exerciseId);

            final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withUpdateExpression("set " + User.EXERCISES + "= :exerciseMap")
                .withValueMap(new ValueMap().withMap(":exerciseMap", user.getUserExercisesMap()));
            this.databaseAccess.updateUser(user.getUsername(), updateItemSpec);

            resultStatus = ResultStatus.successful("Exercise deleted successfully.");
        } catch (WorkoutNotFoundException | UserNotFoundException exception) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, exception));
            resultStatus = ResultStatus.failureBadEntity(exception.getMessage());
        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod + ". " + e);
        }

        this.metrics.commonClose(resultStatus.success);
        return resultStatus;
    }

    private void updateWorkouts(final String exerciseId, final List<String> workoutIds,
        final User user)
        throws InvalidAttributeException, WorkoutNotFoundException {
        final String classMethod = this.getClass().getSimpleName() + ".updateWorkouts";
        // because the number of workouts could go above 25 (max for transaction) just have to do a bunch of blind updates
        for (String workoutId : workoutIds) {
            final Workout workout = Optional
                .ofNullable(this.databaseAccess.getWorkout(workoutId))
                .orElseThrow(
                    () -> new WorkoutNotFoundException(
                        String.format("Workout with ID %s not found", workoutId)));
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
            try {
                this.databaseAccess.updateWorkout(workoutId, updateItemSpec);
            } catch (Exception e) {
                this.metrics.log(new ErrorMessage<>(workoutId, classMethod, e));
            }
        }
    }
}
