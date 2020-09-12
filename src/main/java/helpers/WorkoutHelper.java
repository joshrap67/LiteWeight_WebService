package helpers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import models.ExerciseRoutine;
import models.Routine;
import models.User;

public class WorkoutHelper {

    public static void updateUserExercises(final User user, final Routine routine,
        final String workoutId,
        final String workoutName) {
        // updates the list of exercises on the user object to include this new workout in all contained exercises

        boolean updateDefaultWeight = user.getUserPreferences().isUpdateDefaultWeightOnSave();
        // get a list of all exercises (by id, not name of course)
        Set<String> exercises = new HashSet<>();
        for (int week = 0; week < routine.size(); week++) {
            for (int day = 0; day < routine.getWeek(week).size(); day++) {
                List<ExerciseRoutine> exerciseListForDay = routine
                    .getExerciseListForDay(week, day);
                for (ExerciseRoutine exerciseRoutine : exerciseListForDay) {
                    String exerciseId = exerciseRoutine.getExerciseId();
                    if (updateDefaultWeight && exerciseRoutine.getWeight() > user.getUserExercises()
                        .get(exerciseId).getDefaultWeight()) {
                        // if user wants to update default weight on save and this exercise has a greater
                        // weight than the current default, then update the default
                        user.getUserExercises().get(exerciseId)
                            .setDefaultWeight(exerciseRoutine.getWeight());
                    }
                    exercises.add(exerciseId);

                }
            }
        }

        for (String exerciseId : exercises) {
            user.getUserExercises().get(exerciseId).getWorkouts()
                .putIfAbsent(workoutId, workoutName.trim());
        }
    }

    public static void updateUserExercisesOnEdit(final User user, final Routine newRoutine,
        final Routine oldRoutine,
        final String workoutId,
        final String workoutName) {
        // updates the list of exercises on the user object to include this new workout in all contained exercises

        boolean updateDefaultWeight = user.getUserPreferences().isUpdateDefaultWeightOnSave();
        // get a list of all new exercises (by id)
        Set<String> newExercises = new HashSet<>();
        for (int week = 0; week < newRoutine.size(); week++) {
            for (int day = 0; day < newRoutine.getWeek(week).size(); day++) {
                List<ExerciseRoutine> exerciseListForDay = newRoutine
                    .getExerciseListForDay(week, day);
                for (ExerciseRoutine exerciseRoutine : exerciseListForDay) {
                    String exerciseId = exerciseRoutine.getExerciseId();
                    if (updateDefaultWeight && exerciseRoutine.getWeight() > user.getUserExercises()
                        .get(exerciseId).getDefaultWeight()) {
                        // if user wants to update default weight on save and this exercise has a greater
                        // weight than the current default, then update the default
                        user.getUserExercises().get(exerciseId)
                            .setDefaultWeight(exerciseRoutine.getWeight());
                    }
                    newExercises.add(exerciseId);
                }
            }
        }
        // get a set of all the old exercises (by id)
        Set<String> oldExercises = new HashSet<>();
        for (int week = 0; week < oldRoutine.size(); week++) {
            for (int day = 0; day < oldRoutine.getWeek(week).size(); day++) {
                List<ExerciseRoutine> exerciseListForDay = oldRoutine
                    .getExerciseListForDay(week, day);
                for (ExerciseRoutine exerciseRoutine : exerciseListForDay) {
                    String exerciseId = exerciseRoutine.getExerciseId();
                    oldExercises.add(exerciseId);
                }
            }
        }
        // find the exercises that are no longer being used in this workout
        Set<String> deletedExercises = new HashSet<>(oldExercises);
        deletedExercises.removeAll(newExercises);

        for (String exerciseId : newExercises) {
            // exercise is now in this workout, so reflect that in the user object
            user.getUserExercises().get(exerciseId).getWorkouts()
                .putIfAbsent(workoutId, workoutName.trim());
        }
        for (String exerciseId : deletedExercises) {
            // exercise is no longer in the workout, so remove that mapping from exercise on user object
            user.getUserExercises().get(exerciseId).getWorkouts().remove(workoutId);
        }
    }

    public static String findMostFrequentFocus(final User user,
        final Routine routine) {

        Map<String, Integer> focusCount = new HashMap<>();
        for (int week = 0; week < routine.size(); week++) {
            for (int day = 0; day < routine.getWeek(week).size(); day++) {
                List<ExerciseRoutine> exerciseListForDay = routine
                    .getExerciseListForDay(week, day);
                for (ExerciseRoutine exerciseRoutine : exerciseListForDay) {
                    String exerciseId = exerciseRoutine.getExerciseId();
                    for (String focus : user.getUserExercises().get(exerciseId).getFocuses()) {
                        focusCount.merge(focus, 1, Integer::sum);
                    }
                }
            }
        }

        StringJoiner retVal = new StringJoiner(FileReader.FOCUS_DELIM, "", "");
        int max = 0;
        for (String focus : focusCount.keySet()) {
            int count = focusCount.get(focus);
            if (count > max) {
                max = count;
            }
        }
        for (String focus : focusCount.keySet()) {
            int count = focusCount.get(focus);
            if (count == max) {
                retVal.add(focus);
            }
        }
        return retVal.toString();
    }

    public static void deleteExerciseFromRoutine(final String exerciseId,
        final Routine routine) {
        for (int week = 0; week < routine.size(); week++) {
            for (int day = 0; day < routine.getWeek(week).size(); day++) {
                routine.removeExercise(week, day, exerciseId);
            }
        }
    }
}
