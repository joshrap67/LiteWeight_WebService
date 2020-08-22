package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import exceptions.InvalidAttributeException;
import helpers.ErrorMessage;
import helpers.Metrics;
import helpers.ResultStatus;
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
    public DeleteExerciseManager(DatabaseAccess databaseAccess, Metrics metrics) {
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
            User user = this.databaseAccess.getUser(activeUser);

            if (user != null) {
                ExerciseUser exerciseUser = user.getUserExercises().get(exerciseId);
                List<String> workoutsToUpdate = new ArrayList<>(
                    exerciseUser.getWorkouts().keySet());
                updateWorkouts(exerciseId, workoutsToUpdate, user);
                user.getUserExercises().remove(exerciseId);

                final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                    .withUpdateExpression(
                        "set " +
                            User.EXERCISES + "= :" + User.EXERCISES)
                    .withValueMap(
                        new ValueMap()
                            .withMap(":" + User.EXERCISES, user.getUserExercisesMap()));
                this.databaseAccess.updateUser(user.getUsername(), updateItemSpec);

                resultStatus = ResultStatus.successful("Exercise deleted successfully");

            } else {
                this.metrics.log("Active user does not exist");
                resultStatus = ResultStatus.failureBadEntity("User does not exist.");
            }
        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod + ". " + e);
        }

        this.metrics.commonClose(resultStatus.responseCode);
        return resultStatus;
    }

    private void updateWorkouts(final String exerciseId, final List<String> workoutIds,
        final User user)
        throws InvalidAttributeException {
        final String classMethod = this.getClass().getSimpleName() + ".updateWorkouts";
        // because the number of workouts could go above 25 (max for transaction) just have to do a bunch of blind updates
        for (String workoutId : workoutIds) {
            Workout workout = this.databaseAccess.getWorkout(workoutId);
            WorkoutHelper.deleteExerciseFromRoutine(exerciseId, workout.getRoutine());
            String newMostFrequentFocus = WorkoutHelper
                .findMostFrequentFocus(user, workout.getRoutine());
            workout.setMostFrequentFocus(newMostFrequentFocus);

            UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withUpdateExpression("set "
                    + "#routine =:" + Workout.ROUTINE + ", " +
                    Workout.MOST_FREQUENT_FOCUS + "=:" + Workout.MOST_FREQUENT_FOCUS)
                .withNameMap(new NameMap().with("#routine", Workout.ROUTINE))
                .withValueMap(
                    new ValueMap()
                        .withMap(":" + Workout.ROUTINE, workout.getRoutine().asMap())
                        .withString(":" + Workout.MOST_FREQUENT_FOCUS, newMostFrequentFocus)
                );
            try {
                this.databaseAccess.updateWorkout(workoutId, updateItemSpec);
            } catch (Exception e) {
                this.metrics.log(new ErrorMessage<>(workoutId, classMethod, e));
            }
        }
    }
}
