package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import helpers.AttributeValueHelper;
import helpers.ErrorMessage;
import helpers.JsonHelper;
import helpers.Metrics;
import helpers.ResultStatus;
import helpers.UpdateItemData;
import helpers.Validator;
import helpers.WorkoutHelper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import models.Routine;
import models.User;
import models.Workout;
import models.WorkoutUser;
import responses.UserWithWorkout;

public class NewWorkoutManager {

    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;

    @Inject
    public NewWorkoutManager(DatabaseAccess databaseAccess, Metrics metrics) {
        this.databaseAccess = databaseAccess;
        this.metrics = metrics;
    }

    /**
     * @param workoutName TODO
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public ResultStatus<String> execute(final String workoutName, final String activeUser,
        final Map<String, Object> routineMap) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;

        try {
            User user = this.databaseAccess.getUser(activeUser);
            if (user != null) {
                final String workoutId = UUID.randomUUID().toString();
                final String creationTime = Instant.now().toString();
                final Routine routine = new Routine(routineMap);
                String errorMessage = Validator.validNewWorkoutInput(workoutName, user, routine);

                if (errorMessage == null) {
                    // no error, so go ahead and try and insert this new workout along with updating active user
                    final Workout newWorkout = new Workout();
                    newWorkout.setCreationDate(creationTime);
                    newWorkout.setCreator(activeUser);
                    newWorkout
                        .setMostFrequentFocus(WorkoutHelper.findMostFrequentFocus(user, routine));
                    newWorkout.setWorkoutId(workoutId);
                    newWorkout.setWorkoutName(workoutName.trim());
                    newWorkout.setRoutine(routine);
                    newWorkout.setCurrentDay(0);
                    newWorkout.setCurrentWeek(0);

                    final WorkoutUser workoutUser = new WorkoutUser();
                    workoutUser.setWorkoutName(workoutName.trim());
                    workoutUser.setAverageExercisesCompleted(0.0);
                    workoutUser.setDateLast(creationTime);
                    workoutUser.setTimesCompleted(0);
                    workoutUser.setTotalExercisesSum(0);
                    // need to set it here so frontend gets updated user item back
                    user.setUserWorkouts(workoutId, workoutUser);

                    // update all the exercises that are now apart of this workout
                    WorkoutHelper.updateUserExercises(user, routine, workoutId, workoutName);

                    final UpdateItemData updateItemData = new UpdateItemData(activeUser,
                        DatabaseAccess.USERS_TABLE_NAME)
                        .withUpdateExpression(
                            "set " +
                                User.CURRENT_WORKOUT + " = :" + User.CURRENT_WORKOUT + ", " +
                                User.WORKOUTS + ".#workoutId= :" + User.WORKOUTS + ", " +
                                User.EXERCISES + "= :" + User.EXERCISES)
                        .withValueMap(
                            new ValueMap()
                                .withString(":" + User.CURRENT_WORKOUT, workoutId)
                                .withMap(":" + User.WORKOUTS, workoutUser.asMap())
                                .withMap(":" + User.EXERCISES, user.getUserExercisesMap()))
                        .withNameMap(new NameMap()
                            .with("#workoutId", workoutId));

                    // want a transaction since more than one object is being updated at once
                    final List<TransactWriteItem> actions = new ArrayList<>();
                    actions.add(new TransactWriteItem().withUpdate(updateItemData.asUpdate()));
                    actions.add(new TransactWriteItem()
                        .withPut(
                            new Put().withTableName(DatabaseAccess.WORKOUT_TABLE_NAME).withItem(
                                AttributeValueHelper
                                    .convertMapToAttributeValueMap(newWorkout.asMap()))));

                    this.databaseAccess.executeWriteTransaction(actions);

                    resultStatus = ResultStatus
                        .successful(
                            JsonHelper.serializeMap(
                                new UserWithWorkout(user, newWorkout).asMap()));
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
}
