package helpers;

import java.util.ArrayList;
import java.util.List;
import models.OwnedExercise;
import models.Routine;
import models.User;

public class Validator {

    private static final int
        MAX_WEIGHT = 99999, MAX_SETS = 99, MAX_REPS = 999, MAX_DETAILS_LENGTH = 120,
        MAX_URL_LENGTH = 200, MAX_EXERCISE_NAME = 40,
        MAX_FREE_CUSTOM_EXERCISES = 15, MAX_PREMIUM_CUSTOM_EXERCISES = 30;

    public static String validNewWorkoutInput(final String workoutName, final User activeUser,
        final Routine routine) {

        StringBuilder error = new StringBuilder();
        if (activeUser.getUserWorkouts().size() > Globals.MAX_FREE_WORKOUTS
            && activeUser.getPremiumToken() == null) {
            // TODO need to actually verify that token is good?
            error.append("Max amount of free workouts reached.\n");
        } // TODO still need a max non free workouts
        if (workoutName.length() > Globals.MAX_WORKOUT_NAME_LENGTH) {
            error.append("Workout name is too long.\n");
        }
        String workoutNameError = validWorkoutName(workoutName, activeUser);
        if (workoutNameError != null) {
            error.append(String.format("%s\n", workoutNameError));
        }

        if (routine.getNumberOfWeeks() > Globals.MAX_WEEKS_ROUTINE) {
            error.append("Workout exceeds maximum amount of weeks allowed.");
        }

        for (Integer week : routine) {
            int dayCount = routine.getWeek(week).getNumberOfDays();
            if (dayCount > Globals.MAX_DAYS_ROUTINE) {
                error.append("Week: ").append(week)
                    .append(" exceeds maximum amount of days in a week.");
            }
        }

        return error.toString().trim();
    }

    public static String validEditWorkoutInput(Routine routine) {
        StringBuilder error = new StringBuilder();
        if (routine.getNumberOfWeeks() > Globals.MAX_WEEKS_ROUTINE) {
            error.append("Workout exceeds maximum amount of weeks allowed.");
        }

        boolean emptyDays = false;
        for (Integer week : routine) {
            int dayCount = routine.getWeek(week).getNumberOfDays();
            if (dayCount > Globals.MAX_DAYS_ROUTINE) {
                error.append("Week: ").append(week)
                    .append(" exceeds maximum amount of days in a week.");
            }
            for (Integer day : routine.getWeek(week)) {
                if (routine.getExerciseListForDay(week, day).isEmpty()) {
                    emptyDays = true;
                }
            }
        }
        if (emptyDays) {
            error.append("Workout has a day without any exercises.");
        }

        return error.toString().trim();
    }

    public static String validWorkoutName(final String workoutName, final User user) {
        String error = null;
        boolean repeat = false;
        for (String workoutId : user.getUserWorkouts().keySet()) {
            if (user.getUserWorkouts().get(workoutId).getWorkoutName()
                .equals(workoutName.trim())) {
                repeat = true;
                break;
            }
        }
        if (repeat) {
            error = "Workout name already exists.";
        }
        return error;
    }

    public static String validExerciseUser(final OwnedExercise ownedExercise,
        List<String> exerciseNames, String oldExerciseName) {
        StringBuilder error = new StringBuilder();
        if (ownedExercise.getDefaultWeight() > MAX_WEIGHT) {
            error.append("Weight exceeds max allowed.");
        }
        if (ownedExercise.getDefaultSets() > MAX_SETS) {
            error.append("Sets exceeds max allowed.");
        }
        if (ownedExercise.getDefaultReps() > MAX_REPS) {
            error.append("Sets exceeds max allowed.");
        }
        if (ownedExercise.getDefaultDetails().length() > MAX_DETAILS_LENGTH) {
            error.append("Details length exceeds max allowed.");
        }
        if (ownedExercise.getVideoUrl().length() > MAX_URL_LENGTH) {
            error.append("URL length exceeds max allowed.");
        }
        if (!ownedExercise.isDefaultExercise() &&
            !ownedExercise.getExerciseName().equals(oldExerciseName) &&
            exerciseNames.contains(ownedExercise.getExerciseName())) {
            // make sure to compare old name since user might not have changed name and otherwise would always get error saying exercise already exists
            error.append("Exercise name already exists.");
        }
        if (!ownedExercise.isDefaultExercise()
            && ownedExercise.getExerciseName().length() > MAX_EXERCISE_NAME) {
            error.append("Exercise name too long.");
        }

        return error.toString().trim();
    }

    public static String validNewExercise(User activeUser, String exerciseName,
        List<String> focusList) {
        StringBuilder error = new StringBuilder();
        List<String> exerciseNames = new ArrayList<>();
        int customCount = 0;
        for (String _exerciseId : activeUser.getOwnedExercises().keySet()) {
            if (!activeUser.getOwnedExercises().get(_exerciseId).isDefaultExercise()) {
                customCount++;
            }
            exerciseNames.add(activeUser.getOwnedExercises().get(_exerciseId).getExerciseName());
        }
        if (focusList.isEmpty()) {
            error.append("Must have at least one focus.\n");
        }
        if (exerciseNames.contains(exerciseName)) {
            error.append("Exercise name already exists.\n");
        }
        if (exerciseName.length() > MAX_EXERCISE_NAME) {
            error.append("Exercise length is too long.\n");
        }

        if (activeUser.getPremiumToken() == null && customCount >= MAX_FREE_CUSTOM_EXERCISES) {
            error.append("Max free custom exercise limit reached.\n");
        } else if (activeUser.getPremiumToken() != null
            && customCount >= MAX_PREMIUM_CUSTOM_EXERCISES) {
            error.append("Max custom exercise limit reached.\n");
        }

        return error.toString().trim();

    }
}
