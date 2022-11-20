package utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import models.OwnedExercise;
import models.RoutineDay;
import models.RoutineExercise;
import models.Routine;
import models.RoutineWeek;
import models.User;

public class WorkoutUtils {

    public static void updateOwnedExercises(final User user, final Routine routine, final String workoutId,
        final String workoutName) {
        // updates the list of exercises on the user object to include this new workout in all contained exercises

        boolean updateDefaultWeight = user.getUserPreferences().isUpdateDefaultWeightOnSave();
        // get a list of all exercises by id
        Set<String> exercises = new HashSet<>();
        for (RoutineWeek week : routine) {
            for (RoutineDay day : week) {
                for (RoutineExercise routineExercise : day) {
                    String exerciseId = routineExercise.getExerciseId();
                    OwnedExercise ownedExercise = user.getOwnedExercises().get(exerciseId);
                    if (updateDefaultWeight && routineExercise.getWeight() > ownedExercise.getDefaultWeight()) {
                        // if user wants to update default weight on save and this exercise has a greater
                        // weight than the current default, then update the default
                        ownedExercise.setDefaultWeight(routineExercise.getWeight());
                    }
                    exercises.add(exerciseId);
                }
            }
        }

        for (String exerciseId : exercises) {
            user.getOwnedExercises().get(exerciseId).getWorkouts().putIfAbsent(workoutId, workoutName.trim());
        }
    }

    public static void updateOwnedExercisesOnEdit(final User user, final Routine newRoutine, final Routine oldRoutine,
        final String workoutId, final String workoutName) {
        // updates the list of exercises on the user object to include this new workout in all contained exercises

        boolean updateDefaultWeight = user.getUserPreferences().isUpdateDefaultWeightOnSave();
        // get a list of all new exercises (by id)
        Set<String> newExercises = new HashSet<>();
        for (RoutineWeek week : newRoutine) {
            for (RoutineDay day : week) {
                for (RoutineExercise routineExercise : day) {
                    String exerciseId = routineExercise.getExerciseId();
                    OwnedExercise ownedExercise = user.getOwnedExercises().get(exerciseId);
                    if (updateDefaultWeight && routineExercise.getWeight() > ownedExercise.getDefaultWeight()) {
                        // if user wants to update default weight on save and this exercise has a greater
                        // weight than the current default, then update the default
                        ownedExercise.setDefaultWeight(routineExercise.getWeight());
                    }
                    newExercises.add(exerciseId);
                }
            }
        }
        // get a set of all the old exercises (by id)
        Set<String> oldExercises = new HashSet<>();
        for (RoutineWeek week : oldRoutine) {
            for (RoutineDay day : week) {
                for (RoutineExercise routineExercise : day) {
                    oldExercises.add(routineExercise.getExerciseId());
                }
            }
        }
        // find the exercises that are no longer being used in this workout
        Set<String> deletedExercises = new HashSet<>(oldExercises);
        deletedExercises.removeAll(newExercises);

        for (String exerciseId : newExercises) {
            // exercise is now in this workout, so reflect that in the user object
            user.getOwnedExercises().get(exerciseId).getWorkouts().putIfAbsent(workoutId, workoutName.trim());
        }
        for (String exerciseId : deletedExercises) {
            // exercise is no longer in the workout, so remove that mapping from exercise on user object
            user.getOwnedExercises().get(exerciseId).getWorkouts().remove(workoutId);
        }
    }

    public static String findMostFrequentFocus(final User user, final Routine routine) {
        Map<String, Integer> focusCount = new HashMap<>();
        for (RoutineWeek week : routine) {
            for (RoutineDay day : week) {
                for (RoutineExercise routineExercise : day) {
                    String exerciseId = routineExercise.getExerciseId();
                    for (String focus : user.getOwnedExercises().get(exerciseId).getFocuses()) {
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
}
