package managers;

import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import daos.UserDAO;
import daos.WorkoutDAO;
import utils.Metrics;
import utils.UpdateItemTemplate;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import models.RoutineExercise;
import models.OwnedExercise;
import models.User;
import models.Workout;
import models.WorkoutMeta;
import responses.UserWithWorkout;

public class RestartWorkoutManager {

    public final UserDAO userDAO;
    public final Metrics metrics;

    @Inject
    public RestartWorkoutManager(final UserDAO userDAO, final Metrics metrics) {
        this.userDAO = userDAO;
        this.metrics = metrics;
    }

    /**
     * Resets all exercises in a workout to be not completed and sets the current day and current week to the first
     * day/week of the workout. Also updates the statistics of this workout given the workout before it was restarted.
     *
     * @param activeUser user that is restarting their workout.
     * @param workout    the workout that is to be restarted.
     * @return UserWithWorkout with the workout being reset and the updated statistics added to the user object.
     * @throws Exception if the user or workout does not exist.
     */
    public UserWithWorkout restartWorkout(final String activeUser, final Workout workout) throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        try {
            final User user = this.userDAO.getUser(activeUser);

            final String workoutId = workout.getWorkoutId();
            final WorkoutMeta workoutMeta = user.getWorkoutMetas().get(workoutId);
            restartWorkout(workout, workoutMeta, user);

            workoutMeta.setTimesCompleted(workoutMeta.getTimesCompleted() + 1);
            workout.setCurrentDay(0);
            workout.setCurrentWeek(0);

            // update the newly restarted workout (routine and current day/week)
            UpdateItemTemplate updateWorkoutData = new UpdateItemTemplate(workoutId, WorkoutDAO.WORKOUT_TABLE_NAME)
                .withUpdateExpression("set " +
                    Workout.CURRENT_DAY + " =:currentDay, " +
                    Workout.CURRENT_WEEK + " =:currentWeek, " +
                    "#routine =:routineMap")
                .withValueMap(new ValueMap()
                    .withNumber(":currentDay", workout.getCurrentDay())
                    .withNumber(":currentWeek", workout.getCurrentWeek())
                    .withMap(":routineMap", workout.getRoutine().asMap()))
                .withNameMap(new NameMap().with("#routine", Workout.ROUTINE));

            UpdateItemTemplate updateUserData = new UpdateItemTemplate(activeUser, UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("set " +
                    User.WORKOUTS + ".#workoutId= :userWorkoutsMap, " +
                    User.EXERCISES + " = :exercisesMap")
                .withValueMap(new ValueMap()
                    .withMap(":userWorkoutsMap", workoutMeta.asMap())
                    .withMap(":exercisesMap", user.getOwnedExercisesMap()))
                .withNameMap(new NameMap().with("#workoutId", workoutId));

            final List<TransactWriteItem> actions = new ArrayList<>();
            actions.add(new TransactWriteItem().withUpdate(updateUserData.asUpdate()));
            actions.add(new TransactWriteItem().withUpdate(updateWorkoutData.asUpdate()));
            this.userDAO.executeWriteTransaction(actions);

            this.metrics.commonClose(true);
            return new UserWithWorkout(user, workout);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }

    private void restartWorkout(final Workout workout, final WorkoutMeta workoutMeta, final User user) {
        // reset each exercise to not completed and update average accordingly
        for (Integer week : workout.getRoutine()) {
            for (Integer day : workout.getRoutine().getWeek(week)) {
                for (RoutineExercise routineExercise : workout.getRoutine().getExerciseListForDay(week, day)) {
                    if (routineExercise.isCompleted()) {
                        // update new average since this exercise was indeed completed
                        workoutMeta.setAverageExercisesCompleted(
                            increaseAverage(workoutMeta.getAverageExercisesCompleted(),
                                workoutMeta.getTotalExercisesSum(), 1));
                        routineExercise.setCompleted(false);

                        if (user.getUserPreferences().isUpdateDefaultWeightOnRestart()) {
                            // automatically update default weight with this weight if its higher than previous
                            String exerciseId = routineExercise.getExerciseId();
                            OwnedExercise ownedExercise = user.getOwnedExercises().get(exerciseId);
                            if (routineExercise.getWeight() > ownedExercise.getDefaultWeight()) {
                                ownedExercise.setDefaultWeight(routineExercise.getWeight());
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
