package managers;

import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import daos.UserDAO;
import daos.WorkoutDAO;
import utils.Metrics;
import utils.UpdateItemData;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import models.User;
import models.Workout;
import responses.UserWithWorkout;

public class DeleteWorkoutThenFetchWorkoutManager {

    private final UserDAO userDAO;
    private final WorkoutDAO workoutDAO;
    private final Metrics metrics;

    @Inject
    public DeleteWorkoutThenFetchWorkoutManager(final UserDAO userDAO, final WorkoutDAO workoutDAO,
        final Metrics metrics) {
        this.userDAO = userDAO;
        this.workoutDAO = workoutDAO;
        this.metrics = metrics;
    }

    /**
     * Pops a workout from the workout map and then returns the next workout according to how the
     * client is sorting it (for now it is the most recently accessed).
     *
     * @param activeUser       user that is popping this workout.
     * @param deletedWorkoutId workout id of the workout that is to be popped.
     * @return UserWithWorkout with the user object updated with the next workout on the workout
     * list stack as well as the updated user object. Note the workout will be null if there are no
     * more workouts after the pop.
     * @throws Exception if the workout/user is not found.
     */
    public UserWithWorkout deleteWorkoutThenFetch(final String activeUser,
        final String deletedWorkoutId, final String nextWorkoutId)
        throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".deleteWorkoutThenFetch";
        this.metrics.commonSetup(classMethod);

        try {
            final User user = this.userDAO.getUser(activeUser);

            // remove the workout everywhere in the user object
            for (String exerciseId : user.getOwnedExercises().keySet()) {
                user.getOwnedExercises().get(exerciseId).getWorkouts().remove(deletedWorkoutId);
            }
            user.getUserWorkouts().remove(deletedWorkoutId);

            Workout nextWorkout = null; // if null then that signals no workouts left
            if (nextWorkoutId != null) {
                nextWorkout = this.workoutDAO.getWorkout(nextWorkoutId);
            }
            user.setCurrentWorkout(nextWorkoutId);

            final UpdateItemData updateUserItemData = new UpdateItemData(activeUser,
                UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("set " +
                    User.CURRENT_WORKOUT + " = :currentWorkoutVal, " +
                    User.WORKOUTS + "= :userWorkoutsMap, " +
                    User.EXERCISES + "= :exercisesMap")
                .withValueMap(new ValueMap()
                    .withString(":currentWorkoutVal", nextWorkoutId)
                    .withMap(":userWorkoutsMap", user.getUserWorkoutsMap())
                    .withMap(":exercisesMap", user.getUserExercisesMap()));

            final UpdateItemData updateWorkoutItemData = new UpdateItemData(deletedWorkoutId,
                WorkoutDAO.WORKOUT_TABLE_NAME);

            // want a transaction since more than one object is being updated at once
            final List<TransactWriteItem> actions = new ArrayList<>();
            actions.add(new TransactWriteItem().withUpdate(updateUserItemData.asUpdate()));
            actions.add(new TransactWriteItem().withDelete(updateWorkoutItemData.asDelete()));
            this.userDAO.executeWriteTransaction(actions);

            return new UserWithWorkout(user, nextWorkout);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
