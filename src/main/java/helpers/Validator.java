package helpers;

import models.Routine;
import models.User;

public class Validator {

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
        boolean repeat = false;
        for (String workoutId : activeUser.getUserWorkouts().keySet()) {
            if (activeUser.getUserWorkouts().get(workoutId).getWorkoutName()
                .equals(workoutName.trim())) {
                repeat = true;
                break;
            }
        }
        if (repeat) {
            error.append("Workout name already exists.\n");
        }

        if (routine.getRoutine().keySet().size() > Globals.MAX_WEEKS_ROUTINE) {
            error.append("Workout exceeds maximum amount of weeks allowed.");
        }

        for (Integer week : routine.getRoutine().keySet()) {
            int dayCount = routine.getRoutine().get(week).keySet().size();
            if (dayCount > Globals.MAX_DAYS_ROUTINE) {
                error.append("Week: ").append(week)
                    .append(" exceeds maximum amount of days in a week.");
            }
        }

        return ((error.length() == 0) ? null : error.toString().trim());
    }
}
