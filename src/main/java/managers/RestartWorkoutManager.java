package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import exceptions.UserNotFoundException;
import helpers.ErrorMessage;
import helpers.JsonHelper;
import helpers.Metrics;
import helpers.ResultStatus;
import helpers.UpdateItemData;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import models.ExerciseRoutine;
import models.ExerciseUser;
import models.User;
import models.Workout;
import models.WorkoutUser;
import responses.UserWithWorkout;

public class RestartWorkoutManager {

    public final DatabaseAccess databaseAccess;
    public final Metrics metrics;

    @Inject
    public RestartWorkoutManager(final DatabaseAccess databaseAccess, final Metrics metrics) {
        this.databaseAccess = databaseAccess;
        this.metrics = metrics;
    }

    /**
     * @param activeUser Username of new user to be inserted
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public ResultStatus<String> execute(final String activeUser,
        final Workout workout) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;
        try {
            final User user = Optional.ofNullable(this.databaseAccess.getUser(activeUser))
                .orElseThrow(
                    () -> new UserNotFoundException(String.format("%s not found", activeUser)));

            final String workoutId = workout.getWorkoutId();
            final WorkoutUser workoutMeta = user.getUserWorkouts().get(workoutId);
            restartWorkout(workout, workoutMeta, user);

            workoutMeta.setTimesCompleted(workoutMeta.getTimesCompleted() + 1);
            workout.setCurrentDay(0);
            workout.setCurrentWeek(0);

            // update the newly restarted workout (routine and current day/week)
            final UpdateItemData updateWorkoutData = new UpdateItemData(workoutId,
                DatabaseAccess.WORKOUT_TABLE_NAME)
                .withUpdateExpression("set " +
                    Workout.CURRENT_DAY + " =:currentDay, " +
                    Workout.CURRENT_WEEK + " =:currentWeek, " +
                    "#routine =:routineMap")
                .withValueMap(new ValueMap()
                    .withNumber(":currentDay", workout.getCurrentDay())
                    .withNumber(":currentWeek", workout.getCurrentWeek())
                    .withMap(":routineMap", workout.getRoutine().asMap()))
                .withNameMap(new NameMap().with("#routine", Workout.ROUTINE));

            final UpdateItemData updateUserData = new UpdateItemData(activeUser,
                DatabaseAccess.USERS_TABLE_NAME)
                .withUpdateExpression("set " +
                    User.WORKOUTS + ".#workoutId= :userWorkoutsMap, " +
                    User.EXERCISES + " = :exercisesMap")
                .withValueMap(new ValueMap()
                    .withMap(":userWorkoutsMap", workoutMeta.asMap())
                    .withMap(":exercisesMap", user.getUserExercisesMap()))
                .withNameMap(new NameMap().with("#workoutId", workoutId));

            // want a transaction since more than one object is being updated at once
            final List<TransactWriteItem> actions = new ArrayList<>();
            actions.add(new TransactWriteItem().withUpdate(updateUserData.asUpdate()));
            actions.add(new TransactWriteItem().withUpdate(updateWorkoutData.asUpdate()));
            this.databaseAccess.executeWriteTransaction(actions);

            resultStatus = ResultStatus.successful(
                JsonHelper.serializeMap(new UserWithWorkout(user, workout).asMap()));
        } catch (UserNotFoundException unfe) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, unfe));
            resultStatus = ResultStatus.failureBadEntity(unfe.getMessage());
        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod);
        }

        this.metrics.commonClose(resultStatus.success);
        return resultStatus;
    }

    private void restartWorkout(final Workout workout, final WorkoutUser workoutMeta,
        final User user) {
        // reset each exercise to not completed and update average accordingly
        for (int week = 0; week < workout.getRoutine().size(); week++) {
            for (int day = 0; day < workout.getRoutine().getWeek(week).size(); day++) {
                for (ExerciseRoutine exerciseRoutine : workout.getRoutine()
                    .getExerciseListForDay(week, day)) {
                    if (exerciseRoutine.isCompleted()) {
                        // update new average since this exercise was indeed completed
                        workoutMeta.setAverageExercisesCompleted(
                            increaseAverage(workoutMeta.getAverageExercisesCompleted(),
                                workoutMeta.getTotalExercisesSum(), 1));
                        exerciseRoutine.setCompleted(false);

                        if (user.getUserPreferences().isUpdateDefaultWeightOnRestart()) {
                            // automatically update default weight with this weight if its higher than previous
                            String exerciseId = exerciseRoutine.getExerciseId();
                            ExerciseUser exerciseUser = user.getUserExercises().get(exerciseId);
                            if (exerciseRoutine.getWeight() > exerciseUser.getDefaultWeight()) {
                                exerciseUser.setDefaultWeight(exerciseRoutine.getWeight());
                            }
                        }
                    } else {
                        // didn't complete the exercise, still need to update new average with this 0 value
                        workoutMeta.setAverageExercisesCompleted(
                            increaseAverage(workoutMeta.getAverageExercisesCompleted(),
                                workoutMeta.getTotalExercisesSum(), 0));
                    }
                    workoutMeta.setTotalExercisesSum(workoutMeta.getTotalExercisesSum() + 1);
                }
            }
        }
    }

    private static double increaseAverage(double oldAverage, int count, double newValue) {
        return ((newValue + (oldAverage * count)) / (count + 1));
    }
}
