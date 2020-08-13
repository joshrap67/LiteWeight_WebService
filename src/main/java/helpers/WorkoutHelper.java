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

    public static void updateUserExercises(User user, Routine routine, String workoutId,
        String workoutName) {
        // updates the list of exercises on the user object to include this new workout in all contained exercises

        // get a list of all exercises (by id, not name of course)
        Set<String> exercises = new HashSet<>();
        for (Integer week : routine.getRoutine().keySet()) {
            for (Integer day : routine.getRoutine().get(week).keySet()) {
                List<ExerciseRoutine> exerciseListForDay = routine
                    .getExerciseListForDay(week, day);
                for (ExerciseRoutine exerciseRoutine : exerciseListForDay) {
                    String exerciseId = exerciseRoutine.getExerciseId();
                    exercises.add(exerciseId);
                }
            }
        }

        for (String exerciseId : exercises) {
            user.getUserExercises().get(exerciseId).getWorkouts()
                .putIfAbsent(workoutId, workoutName.trim());
        }
    }

    public static String findMostFrequentFocus(User user,
        Routine routine) {

        Map<String, Integer> focusCount = new HashMap<>();
        for (Integer week : routine.getRoutine().keySet()) {
            for (Integer day : routine.getRoutine().get(week).keySet()) {
                List<ExerciseRoutine> exerciseListForDay = routine
                    .getExerciseListForDay(week, day);
                for (ExerciseRoutine exerciseRoutine : exerciseListForDay) {
                    String exerciseId = exerciseRoutine.getExerciseId();
                    for (String focus : user.getUserExercises().get(exerciseId).getFocuses()
                        .keySet()) {
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
