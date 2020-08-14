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
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import models.User;
import models.Workout;
import models.WorkoutUser;

public class RenameWorkoutManager {

    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;

    @Inject
    public RenameWorkoutManager(DatabaseAccess databaseAccess, Metrics metrics) {
        this.databaseAccess = databaseAccess;
        this.metrics = metrics;
    }

    /**
     * @param workoutId TODO
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public ResultStatus<String> execute(final String activeUser, final String workoutId,
        final String newWorkoutName) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;

        try {
            User user = this.databaseAccess.getUser(activeUser);
            if (user != null) {
                String errorMessage = Validator.validWorkoutName(newWorkoutName, user);

                if (errorMessage == null) {
                    // no error, so go ahead and try and rename the workout
                    Workout workout = this.databaseAccess.getWorkout(workoutId);
                    workout.setWorkoutName(newWorkoutName);
                    // update all the exercises that are apart of this newly renamed workout
                    updateUserExercises(user, workoutId, newWorkoutName);
                    WorkoutUser workoutUser = user.getUserWorkouts().get(workoutId);
                    workoutUser.setWorkoutName(newWorkoutName);

                    final UpdateItemData updateUserItemData = new UpdateItemData(activeUser,
                        DatabaseAccess.USERS_TABLE_NAME)
                        .withUpdateExpression(
                            "set " +
                                User.WORKOUTS + ".#workoutId= :" + User.WORKOUTS + ", " +
                                User.EXERCISES + "= :" + User.EXERCISES)
                        .withValueMap(
                            new ValueMap()
                                .withMap(":" + User.WORKOUTS, workoutUser.asMap())
                                .withMap(":" + User.EXERCISES, user.getUserExercisesMap()))
                        .withNameMap(new NameMap()
                            .with("#workoutId", workoutId));

                    final UpdateItemData updateWorkoutItemData = new UpdateItemData(workoutId,
                        DatabaseAccess.WORKOUT_TABLE_NAME)
                        .withUpdateExpression(
                            "set " +
                                Workout.WORKOUT_NAME + "= :" + Workout.WORKOUT_NAME)
                        .withValueMap(
                            new ValueMap()
                                .withString(":" + Workout.WORKOUT_NAME, newWorkoutName));
                    // want a transaction since more than one object is being updated at once
                    final List<TransactWriteItem> actions = new ArrayList<>();
                    actions.add(new TransactWriteItem().withUpdate(updateUserItemData.asUpdate()));
                    actions
                        .add(new TransactWriteItem().withUpdate(updateWorkoutItemData.asUpdate()));

                    this.databaseAccess.executeWriteTransaction(actions);

                    resultStatus = ResultStatus
                        .successful(JsonHelper.convertObjectToJson(user.asMap()));
                } else {
                    this.metrics.log("Input error: " + errorMessage);
                    resultStatus = ResultStatus.failure(errorMessage);
                }

            } else {
                this.metrics.log("Active user does not exist");
                resultStatus = ResultStatus.failure("User does not exist.");
            }
        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failure("Exception in " + classMethod + ". " + e);
        }

        this.metrics.commonClose(resultStatus.success);
        return resultStatus;
    }

    private static void updateUserExercises(User user, String workoutId,
        String newWorkoutName) {
        // loops through all user exercises and updates the old workout name with the newly renamed one
        for (String exerciseId : user.getUserExercises().keySet()) {
            if (user.getUserExercises().get(exerciseId).getWorkouts().containsKey(workoutId)) {
                // old workout name found, replace it
                user.getUserExercises().get(exerciseId).getWorkouts()
                    .put(workoutId, newWorkoutName);
            }
        }
    }
}
