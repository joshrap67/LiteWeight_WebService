package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import helpers.ErrorMessage;
import helpers.JsonHelper;
import helpers.Metrics;
import helpers.ResultStatus;
import helpers.UpdateItemData;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import models.User;
import models.Workout;
import models.WorkoutUser;
import responses.UserWithWorkout;

public class DeleteWorkoutManager {

    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;

    @Inject
    public DeleteWorkoutManager(DatabaseAccess databaseAccess, Metrics metrics) {
        this.databaseAccess = databaseAccess;
        this.metrics = metrics;
    }

    /**
     * @param workoutId TODO
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public ResultStatus<String> execute(final String activeUser, final String workoutId) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;

        try {
            User user = this.databaseAccess.getUser(activeUser);
            if (user != null) {
                // remove the workout everywhere in the user object
                for (String exerciseId : user.getUserExercises().keySet()) {
                    user.getUserExercises().get(exerciseId).getWorkouts().remove(workoutId);
                }
                user.getUserWorkouts().remove(workoutId);

                // need to find the next workout now that the user deleted the current
                List<WorkoutUser> workoutUsers = new ArrayList<>(user.getUserWorkouts().values());
                // for now user can only sort workouts by last date
                workoutUsers.sort((o1, o2) -> o2.getDateLast().compareTo(o1.getDateLast()));
                String nextWorkoutId = null; // workout that user is now on after deleting the current one
                if (!workoutUsers.isEmpty()) {
                    for (String workoutIdTarget : user.getUserWorkouts().keySet()) {
                        if (user.getUserWorkouts().get(workoutIdTarget).getWorkoutName()
                            .equals(workoutUsers.get(0).getWorkoutName())) {
                            nextWorkoutId = workoutIdTarget;
                            break;
                        }
                    }
                }
                user.setCurrentWorkout(nextWorkoutId);
                Workout nextWorkout = null; // if null then that signals no workouts left
                if (nextWorkoutId != null) {
                    nextWorkout = this.databaseAccess.getWorkout(nextWorkoutId);
                }

                final UpdateItemData updateUserItemData = new UpdateItemData(activeUser,
                    DatabaseAccess.USERS_TABLE_NAME)
                    .withUpdateExpression(
                        "set " +
                            User.CURRENT_WORKOUT + " = :" + User.CURRENT_WORKOUT + ", " +
                            User.WORKOUTS + "= :" + User.WORKOUTS + ", " +
                            User.EXERCISES + "= :" + User.EXERCISES)
                    .withValueMap(
                        new ValueMap()
                            .withString(":" + User.CURRENT_WORKOUT, nextWorkoutId)
                            .withMap(":" + User.WORKOUTS, user.getUserWorkoutsMap())
                            .withMap(":" + User.EXERCISES, user.getUserExercisesMap()));

                final UpdateItemData updateWorkoutItemData = new UpdateItemData(workoutId,
                    DatabaseAccess.WORKOUT_TABLE_NAME);

                // want a transaction since more than one object is being updated at once
                final List<TransactWriteItem> actions = new ArrayList<>();
                actions.add(new TransactWriteItem().withUpdate(updateUserItemData.asUpdate()));
                actions.add(new TransactWriteItem().withDelete(updateWorkoutItemData.asDelete()));

                this.databaseAccess.executeWriteTransaction(actions);

                resultStatus = ResultStatus
                    .successful(
                        JsonHelper
                            .serializeObject(new UserWithWorkout(user, nextWorkout).asMap()));
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
