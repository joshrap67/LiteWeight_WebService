package utils;

import exceptions.UnauthorizedException;
import imports.Globals;
import java.util.ArrayList;
import java.util.List;
import models.OwnedExercise;
import models.Routine;
import models.RoutineDay;
import models.RoutineWeek;
import models.User;
import models.Workout;

public class Validator {

    // todo unit test

    private static final int
        MAX_WEIGHT = 99999,
        MAX_SETS = 99,
        MAX_REPS = 999,
        MAX_DETAILS_LENGTH = 120,
        MAX_URL_LENGTH = 200,
        MAX_EXERCISE_NAME = 40,
        MAX_DAY_TAG_LENGTH = 25;

    public static String validNewWorkoutInput(final String workoutName, final User activeUser,
        final Routine routine) {
        StringBuilder error = new StringBuilder();
        if (activeUser.getWorkoutMetas().size() >= Globals.MAX_FREE_WORKOUTS && activeUser.getPremiumToken() == null) {
            error.append("Max amount of free workouts reached.\n");
        }
        if (activeUser.getPremiumToken() != null && activeUser.getWorkoutMetas().size() >= Globals.MAX_WORKOUTS) {
            error.append("Maximum workouts exceeded.\n");
        }

        String workoutNameError = validWorkoutName(workoutName, activeUser);
        if (!workoutNameError.isEmpty()) {
            error.append(String.format("%s\n", workoutNameError));
        }

        if (routineExceedsMaxWeeks(routine)) {
            error.append("Workout exceeds maximum amount of weeks allowed.\n");
        }
        String routineDayErrors = routineDayErrors(routine);
        if (!routineDayErrors.isEmpty()) {
            error.append(routineDayErrors).append("\n");
        }

        return error.toString().trim();
    }

    public static void ensureWorkoutOwnership(final String activeUser, final Workout workout)
        throws UnauthorizedException {
        if (!workout.getCreator().equals(activeUser)) {
            throw new UnauthorizedException("User does not have permissions to modify workout.");
        }
    }

    public static String validWorkoutName(final String workoutName, final User user) {
        StringBuilder error = new StringBuilder();
        if (workoutName.isEmpty()) {
            error.append("Workout name cannot be empty.\n");
        }

        if (workoutName.length() > Globals.MAX_WORKOUT_NAME_LENGTH) {
            error.append("Workout name is too long.\n");
        }

        boolean repeat = false;
        for (String workoutId : user.getWorkoutMetas().keySet()) {
            if (user.getWorkoutMetas().get(workoutId).getWorkoutName().equals(workoutName)) {
                repeat = true;
                break;
            }
        }
        if (repeat) {
            error.append("Workout name already exists.\n");
        }
        return error.toString().trim();
    }

    public static String validEditWorkoutInput(final Routine routine) {
        StringBuilder error = new StringBuilder();
        if (routineExceedsMaxWeeks(routine)) {
            error.append("Workout exceeds maximum amount of weeks allowed.\n");
        }

        String routineDayErrors = routineDayErrors(routine);
        if (!routineDayErrors.isEmpty()) {
            error.append(routineDayErrors).append("\n");
        }

        return error.toString().trim();
    }

    public static String validOwnedExercise(final OwnedExercise ownedExercise,
        final List<String> exerciseNames, final String oldExerciseName) {
        StringBuilder error = new StringBuilder();
        if (ownedExercise.getFocuses().isEmpty()) {
            error.append("Must have at least one focus.\n");
        }
        if (invalidExerciseName(ownedExercise.getExerciseName())) {
            error.append("Exercise name invalid.\n");
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

    public static String validNewExercise(final User user, final String exerciseName,
        final double weight, final int sets, final int reps, final String details, final String videoUrl,
        final List<String> focusList) {
        StringBuilder error = new StringBuilder();
        List<String> exerciseNames = new ArrayList<>();
        for (String _exerciseId : user.getOwnedExercises().keySet()) {
            exerciseNames.add(user.getOwnedExercises().get(_exerciseId).getExerciseName());
        }
        if (focusList.isEmpty()) {
            error.append("Must have at least one focus.\n");
        }
        if (exerciseNames.contains(exerciseName)) {
            error.append("Exercise name already exists.\n");
        }
        if (invalidExerciseName(exerciseName)) {
            error.append("Exercise name invalid.\n");
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

        if (user.getPremiumToken() == null && user.getOwnedExercises().size() >= Globals.MAX_FREE_EXERCISES) {
            error.append("Max exercise limit reached.\n");
        } else if (user.getPremiumToken() != null
            && user.getOwnedExercises().size() >= Globals.MAX_PREMIUM_EXERCISES) {
            error.append("Max exercise limit reached.\n");
        }

        return error.toString().trim();
    }

    private static boolean routineExceedsMaxWeeks(final Routine routine) {
        return routine.getNumberOfWeeks() > Globals.MAX_WEEKS_ROUTINE;
    }

    private static String routineDayErrors(final Routine routine) {
        StringBuilder error = new StringBuilder();
        int weekIndex = 0;
        for (RoutineWeek week : routine) {
            weekIndex++;
            int dayCount = week.getNumberOfDays();
            if (dayCount > Globals.MAX_DAYS_ROUTINE) {
                error.append("Week: ").append(weekIndex).append(" exceeds maximum amount of days in a week.\n");
            }
            int dayIndex = 0;
            for (RoutineDay day : week) {
                dayIndex++;
                if (day.getTag() != null && day.getTag().length() > MAX_DAY_TAG_LENGTH) {
                    error.append("Day tag for Week ")
                        .append(weekIndex).append(" Day ").append(dayIndex)
                        .append(" exceeds maximum length.\n");
                }
            }
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
        return name == null || name.isEmpty() || name.length() > MAX_EXERCISE_NAME;
    }
}
