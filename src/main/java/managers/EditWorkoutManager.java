package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import helpers.ErrorMessage;
import helpers.JsonHelper;
import helpers.Metrics;
import helpers.ResultStatus;
import helpers.UpdateItemData;
import helpers.Validator;
import helpers.WorkoutHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import models.User;
import models.Workout;
import responses.UserWithWorkout;

public class EditWorkoutManager {

    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;

    @Inject
    public EditWorkoutManager(DatabaseAccess databaseAccess, Metrics metrics) {
        this.databaseAccess = databaseAccess;
        this.metrics = metrics;
    }

    /**
     * @param activeUser TODO
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public ResultStatus<String> execute(final String activeUser,
        final Map<String, Object> editedWorkoutMap) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;

        try {
            User user = this.databaseAccess.getUser(activeUser);
            if (user != null) {
                Workout editedWorkout = new Workout(editedWorkoutMap);
                final Workout originalWorkout = this.databaseAccess
                    .getWorkout(editedWorkout.getWorkoutId());
                final String workoutId = originalWorkout.getWorkoutId();
                String errorMessage = Validator.validEditWorkoutInput(editedWorkout.getRoutine());

                if (errorMessage == null) {
                    // update all the exercises that are now apart of this workout
                    WorkoutHelper
                        .updateUserExercisesOnEdit(user, editedWorkout.getRoutine(),
                            originalWorkout.getRoutine(), workoutId,
                            originalWorkout.getWorkoutName());
                    // update most frequent focus since exercises have changed
                    editedWorkout
                        .setMostFrequentFocus(
                            WorkoutHelper.findMostFrequentFocus(user, editedWorkout.getRoutine()));

                    // Need to determine if the current week/day is valid (frontend's responsibility is updating them)
                    checkCurrentDay(editedWorkout);
                    final UpdateItemData updateUserItemData = new UpdateItemData(activeUser,
                        DatabaseAccess.USERS_TABLE_NAME)
                        .withUpdateExpression(
                            "set " + User.EXERCISES + "= :" + User.EXERCISES)
                        .withValueMap(
                            new ValueMap()
                                .withMap(":" + User.EXERCISES, user.getUserExercisesMap()));

                    final UpdateItemData updateWorkoutItemData = new UpdateItemData(workoutId,
                        DatabaseAccess.WORKOUT_TABLE_NAME)
                        .withUpdateExpression(
                            "set " +
                                Workout.MOST_FREQUENT_FOCUS + " = :" + Workout.MOST_FREQUENT_FOCUS
                                + ", " +
                                Workout.CURRENT_WEEK + " =:" + Workout.CURRENT_WEEK + ", " +
                                Workout.CURRENT_DAY + " =:" + Workout.CURRENT_DAY + ", " +
                                "#routine = :" + Workout.ROUTINE)
                        .withValueMap(
                            new ValueMap()
                                .withString(":" + Workout.MOST_FREQUENT_FOCUS,
                                    editedWorkout.getMostFrequentFocus())
                                .withNumber(":" + Workout.CURRENT_WEEK,
                                    editedWorkout.getCurrentWeek())
                                .withNumber(":" + Workout.CURRENT_DAY,
                                    editedWorkout.getCurrentDay())
                                .withMap(":" + Workout.ROUTINE,
                                    editedWorkout.getRoutine().asMap()))
                        .withNameMap(new NameMap()
                            .with("#routine", Workout.ROUTINE));

                    // want a transaction since more than one object is being updated at once
                    final List<TransactWriteItem> actions = new ArrayList<>();
                    actions.add(new TransactWriteItem().withUpdate(updateUserItemData.asUpdate()));
                    actions
                        .add(new TransactWriteItem().withUpdate(updateWorkoutItemData.asUpdate()));

                    this.databaseAccess.executeWriteTransaction(actions);

                    resultStatus = ResultStatus
                        .successful(
                            JsonHelper.serializeMap(
                                new UserWithWorkout(user, editedWorkout).asMap()));
                } else {
                    this.metrics.log("Input error: " + errorMessage);
                    resultStatus = ResultStatus.failureBadEntity(errorMessage);
                }

            } else {
                this.metrics.log("Active user does not exist");
                resultStatus = ResultStatus.failureBadEntity("User does not exist.");
            }
        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod + ". " + e);
        }

        this.metrics.commonClose(resultStatus.responseCode);
        return resultStatus;
    }

    private void checkCurrentDay(final Workout editedWorkout) {
        int currentDay = editedWorkout.getCurrentDay();
        int currentWeek = editedWorkout.getCurrentWeek();
        // make sure that the current week according to the frontend is actually valid
        if (currentWeek >= 0 && currentWeek >= editedWorkout.getRoutine().size()) {
            // frontend incorrectly set the current week, so just set it to 0
            editedWorkout.setCurrentWeek(0);
        }

        if (currentDay >= 0 && currentDay >= editedWorkout.getRoutine().getWeek(currentWeek)
            .size()) {
            // frontend incorrectly set the current day, so just set it to 0
            editedWorkout.setCurrentDay(0);
        }
    }
}
