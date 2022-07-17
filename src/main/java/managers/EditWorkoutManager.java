package managers;

import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import daos.UserDAO;
import daos.WorkoutDAO;
import exceptions.ManagerExecutionException;
import exceptions.UnauthorizedException;
import utils.Metrics;
import utils.UpdateItemTemplate;
import utils.Validator;
import utils.WorkoutUtils;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import models.User;
import models.Workout;
import responses.UserWithWorkout;

public class EditWorkoutManager {

    private final WorkoutDAO workoutDAO;
    private final UserDAO userDAO;
    private final Metrics metrics;

    @Inject
    public EditWorkoutManager(final WorkoutDAO workoutDAO, final UserDAO userDAO,
        final Metrics metrics) {
        this.workoutDAO = workoutDAO;
        this.userDAO = userDAO;
        this.metrics = metrics;
    }

    /**
     * Saves edited changes to an existing workout. The edited workout is saved in the database and the user's owned
     * exercises are updated to match whether they are apart of this workout now or not. The most frequent focus is also
     * updated in the workout if it now is different as a result of the edits.
     *
     * @param activeUser    the user that is editing this workout.
     * @param editedWorkout the modified workout as sent by the client.
     * @return UserWithWorkout that has the newly edited workout and exercise mapping.
     * @throws Exception thrown if there is any error.
     */
    public UserWithWorkout editWorkout(final String activeUser, final Workout editedWorkout) throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".editWorkout";
        this.metrics.commonSetup(classMethod);

        try {
            final User user = this.userDAO.getUser(activeUser);
            final Workout oldWorkout = this.workoutDAO.getWorkout(editedWorkout.getWorkoutId());
            if (!oldWorkout.getCreator().equals(user.getUsername())) {
                // prevents someone from trying to edit a workout that is not theirs.
                throw new UnauthorizedException("User does not have permissions to view this workout.");
            }

            final String workoutId = oldWorkout.getWorkoutId();
            final String errorMessage = Validator.validEditWorkoutInput(editedWorkout.getRoutine());

            if (!errorMessage.isEmpty()) {
                this.metrics.commonClose(false);
                throw new ManagerExecutionException(errorMessage);
            }

            // update all the exercises that are now a part of this workout
            WorkoutUtils.updateOwnedExercisesOnEdit(user, editedWorkout.getRoutine(),
                oldWorkout.getRoutine(), workoutId,
                oldWorkout.getWorkoutName());
            // update most frequent focus since exercises have changed
            editedWorkout.setMostFrequentFocus(
                WorkoutUtils.findMostFrequentFocus(user, editedWorkout.getRoutine()));

            // Need to determine if the current week/day is valid (frontend's responsibility is updating them)
            confirmValidCurrentDayAndWeek(editedWorkout);
            UpdateItemTemplate updateUserItemData = new UpdateItemTemplate(activeUser,
                UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("set " + User.EXERCISES + "= :exerciseMap")
                .withValueMap(new ValueMap().withMap(":exerciseMap", user.getOwnedExercisesMap()));

            UpdateItemTemplate updateWorkoutItemData = new UpdateItemTemplate(workoutId,
                WorkoutDAO.WORKOUT_TABLE_NAME)
                .withUpdateExpression("set " +
                    Workout.MOST_FREQUENT_FOCUS + " = :mostFrequentFocusVal, " +
                    Workout.CURRENT_WEEK + " =:currentWeekVal, " +
                    Workout.CURRENT_DAY + " =:currentDayVal, " +
                    "#routine = :routineMap")
                .withValueMap(new ValueMap()
                    .withString(":mostFrequentFocusVal", editedWorkout.getMostFrequentFocus())
                    .withNumber(":currentWeekVal", editedWorkout.getCurrentWeek())
                    .withNumber(":currentDayVal", editedWorkout.getCurrentDay())
                    .withMap(":routineMap", editedWorkout.getRoutine().asMap()))
                .withNameMap(new NameMap().with("#routine", Workout.ROUTINE));

            final List<TransactWriteItem> actions = new ArrayList<>();
            actions.add(new TransactWriteItem().withUpdate(updateUserItemData.asUpdate()));
            actions.add(new TransactWriteItem().withUpdate(updateWorkoutItemData.asUpdate()));
            this.workoutDAO.executeWriteTransaction(actions);

            this.metrics.commonClose(true);
            return new UserWithWorkout(user, editedWorkout);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }

    private void confirmValidCurrentDayAndWeek(final Workout editedWorkout) {
        // make sure that the current week according to the frontend is actually valid
        int currentDay = editedWorkout.getCurrentDay();
        int currentWeek = editedWorkout.getCurrentWeek();
        if (currentWeek >= 0 && currentWeek >= editedWorkout.getRoutine().getNumberOfWeeks()) {
            // frontend incorrectly set the current week, so just set it to 0
            editedWorkout.setCurrentWeek(0);
        }

        if (currentDay >= 0 && currentDay >= editedWorkout.getRoutine().getWeek(currentWeek)
            .getNumberOfDays()) {
            // frontend incorrectly set the current day, so just set it to 0
            editedWorkout.setCurrentDay(0);
        }
    }
}
