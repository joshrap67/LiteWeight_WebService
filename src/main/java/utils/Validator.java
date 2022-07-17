package utils;

import imports.Globals;
import java.util.ArrayList;
import java.util.List;
import models.OwnedExercise;
import models.Routine;
import models.User;

public class Validator {

    private static final int
        MAX_WEIGHT = 99999, MAX_SETS = 99, MAX_REPS = 999, MAX_DETAILS_LENGTH = 120, MAX_URL_LENGTH = 200, MAX_EXERCISE_NAME = 40;

    public static String validNewWorkoutInput(final String workoutName, final User activeUser,
        final Routine routine) {
        StringBuilder error = new StringBuilder();
        if (activeUser.getWorkoutMetas().size() >= Globals.MAX_FREE_WORKOUTS && activeUser.getPremiumToken() == null) {
            error.append("Max amount of free workouts reached.\n");
        }
        if (activeUser.getPremiumToken() != null
            && activeUser.getWorkoutMetas().size() >= Globals.MAX_WORKOUTS) {
            error.append("Maximum workouts exceeded.\n");
        }

        String workoutNameError = validWorkoutName(workoutName, activeUser);
        if (!workoutNameError.isEmpty()) {
            error.append(String.format("%s\n", workoutNameError));
        }

        if (routine.getNumberOfWeeks() > Globals.MAX_WEEKS_ROUTINE) {
            error.append("Workout exceeds maximum amount of weeks allowed.\n");
        }

        for (Integer week : routine) {
            int dayCount = routine.getWeek(week).getNumberOfDays();
            if (dayCount > Globals.MAX_DAYS_ROUTINE) {
                error.append("Week: ").append(week).append(" exceeds maximum amount of days in a week.\n");
            }
        }

        return error.toString().trim();
    }

    public static String validWorkoutName(final String workoutName, final User user) {
        StringBuilder error = new StringBuilder();
        if (workoutName.length() > Globals.MAX_WORKOUT_NAME_LENGTH) {
            error.append("Workout name is too long.\n");
        }

        boolean repeat = false;
        for (String workoutId : user.getWorkoutMetas().keySet()) {
            if (user.getWorkoutMetas().get(workoutId).getWorkoutName().equals(workoutName.trim())) {
                repeat = true;
                break;
            }
        }
        if (repeat) {
            error.append("Workout name already exists.\n");
        }
        return error.toString().trim();
    }

    public static String validEditWorkoutInput(Routine routine) {
        StringBuilder error = new StringBuilder();
        if (routine.getNumberOfWeeks() > Globals.MAX_WEEKS_ROUTINE) {
            error.append("Workout exceeds maximum amount of weeks allowed.\n");
        }

        boolean emptyDays = false;
        for (Integer week : routine) {
            int dayCount = routine.getWeek(week).getNumberOfDays();
            if (dayCount > Globals.MAX_DAYS_ROUTINE) {
                error.append("Week: ").append(week).append(" exceeds maximum amount of days in a week.\n");
            }
            for (Integer day : routine.getWeek(week)) {
                if (routine.getExerciseListForDay(week, day).isEmpty()) {
                    emptyDays = true;
                }
            }
        }
        if (emptyDays) {
            error.append("Workout has a day without any exercises.\n");
        }

        return error.toString().trim();
    }

    public static String validOwnedExercise(final OwnedExercise ownedExercise,
        List<String> exerciseNames, String oldExerciseName) {
        StringBuilder error = new StringBuilder();
        if (ownedExercise.getFocuses().isEmpty()) {
            error.append("Must have at least one focus.\n");
        }
        if (invalidExerciseName(ownedExercise.getExerciseName())) {
            error.append("Exercise name too long.\n");
        }
        if (invalidWeight(ownedExercise.getDefaultWeight())) {
            error.append("Weight exceeds max allowed.\n");
        }
        if (invalidSets(ownedExercise.getDefaultSets())) {
            error.append("Sets exceeds max allowed.\n");
        }
        if (invalidReps(ownedExercise.getDefaultReps())) {
            error.append("Reps exceeds max allowed.\n");
        }
        if (invalidDetails(ownedExercise.getDefaultDetails())) {
            error.append("Details length exceeds max allowed.\n");
        }
        if (invalidURl(ownedExercise.getVideoUrl())) {
            error.append("URL length exceeds max allowed.\n");
        }

        if (!ownedExercise.getExerciseName().equals(oldExerciseName) &&
            exerciseNames.contains(ownedExercise.getExerciseName())) {
            // make sure to compare old name since user might not have changed name and otherwise would always get error saying exercise already exists
            error.append("Exercise name already exists.\n");
        }

        return error.toString().trim();
    }

    public static String validNewExercise(User activeUser, String exerciseName,
        double weight, int sets, int reps, String details, String videoUrl, List<String> focusList) {
        StringBuilder error = new StringBuilder();
        List<String> exerciseNames = new ArrayList<>();
        for (String _exerciseId : activeUser.getOwnedExercises().keySet()) {
            exerciseNames.add(activeUser.getOwnedExercises().get(_exerciseId).getExerciseName());
        }
        if (focusList.isEmpty()) {
            error.append("Must have at least one focus.\n");
        }
        if (exerciseNames.contains(exerciseName)) {
            error.append("Exercise name already exists.\n");
        }
        if (invalidExerciseName(exerciseName)) {
            error.append("Exercise name too long.\n");
        }
        if (invalidWeight(weight)) {
            error.append("Weight exceeds max allowed.\n");
        }
        if (invalidSets(sets)) {
            error.append("Sets exceeds max allowed.\n");
        }
        if (invalidReps(reps)) {
            error.append("Reps exceeds max allowed.\n");
        }
        if (invalidDetails(details)) {
            error.append("Details length exceeds max allowed.\n");
        }
        if (invalidURl(videoUrl)) {
            error.append("URL length exceeds max allowed.\n");
        }

        if (activeUser.getPremiumToken() == null
            && activeUser.getOwnedExercises().size() >= Globals.MAX_FREE_EXERCISES) {
            error.append("Max exercise limit reached.\n");
        } else if (activeUser.getPremiumToken() != null
            && activeUser.getOwnedExercises().size() >= Globals.MAX_PREMIUM_EXERCISES) {
            error.append("Max exercise limit reached.\n");
        }

        return error.toString().trim();
    }

    private static boolean invalidWeight(double weight) {
        return !(weight <= MAX_WEIGHT);
    }

    private static boolean invalidSets(int sets) {
        return sets > MAX_SETS;
    }

    private static boolean invalidReps(int reps) {
        return reps > MAX_REPS;
    }

    private static boolean invalidDetails(String details) {
        return details == null || details.length() > MAX_DETAILS_LENGTH;
    }

    private static boolean invalidURl(String url) {
        return url == null || url.length() > MAX_URL_LENGTH;
    }

    private static boolean invalidExerciseName(String name) {
        return name == null || name.length() > MAX_EXERCISE_NAME;
    }
}
