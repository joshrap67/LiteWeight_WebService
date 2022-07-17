package managers;

import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import daos.UserDAO;
import daos.WorkoutDAO;
import exceptions.UnauthorizedException;
import utils.Metrics;
import utils.UpdateItemTemplate;
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
     * Deletes a workout from the workout map and then returns the next workout according to the id that was passed in.
     *
     * @param activeUser       user that is popping this workout.
     * @param deletedWorkoutId workout id of the workout that is to be deleted.
     * @return the user object updated with the next workout as well as all other updated fields. Note the workout will
     * be null if there are no more workouts after the pop.
     * @throws Exception if the workout/user is not found.
     */
    public UserWithWorkout deleteWorkoutThenFetch(final String activeUser, final String deletedWorkoutId,
        final String nextWorkoutId) throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".deleteWorkoutThenFetch";
        this.metrics.commonSetup(classMethod);

        try {
            final User user = this.userDAO.getUser(activeUser);
            final Workout oldWorkout = this.workoutDAO.getWorkout(deletedWorkoutId);
            if (!oldWorkout.getCreator().equals(user.getUsername())) {
                // prevents someone from trying to delete a workout that is not theirs.
                throw new UnauthorizedException("User does not have permissions to view this workout.");
            }

            // remove the workout everywhere in the user object
            for (String exerciseId : user.getOwnedExercises().keySet()) {
                user.getOwnedExercises().get(exerciseId).getWorkouts().remove(deletedWorkoutId);
            }
            user.getWorkoutMetas().remove(deletedWorkoutId);

            Workout nextWorkout = null; // if null then that signals no workouts left
            if (nextWorkoutId != null) {
                nextWorkout = this.workoutDAO.getWorkout(nextWorkoutId);
                if (!nextWorkout.getCreator().equals(user.getUsername())) {
                    // prevents someone from trying to switch to a workout that is not theirs.
                    throw new UnauthorizedException("User does not have permissions to view this workout.");
                }
            }
            user.setCurrentWorkout(nextWorkoutId);

            UpdateItemTemplate updateUserItemData = new UpdateItemTemplate(activeUser, UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("set " +
                    User.CURRENT_WORKOUT + " = :currentWorkoutVal, " +
                    User.WORKOUTS + "= :userWorkoutsMap, " +
                    User.EXERCISES + "= :exercisesMap")
                .withValueMap(new ValueMap()
                    .withString(":currentWorkoutVal", nextWorkoutId)
                    .withMap(":userWorkoutsMap", user.getWorkoutMetasMap())
                    .withMap(":exercisesMap", user.getOwnedExercisesMap()));

            UpdateItemTemplate updateWorkoutItemData = new UpdateItemTemplate(deletedWorkoutId,
                WorkoutDAO.WORKOUT_TABLE_NAME);

            final List<TransactWriteItem> actions = new ArrayList<>();
            actions.add(new TransactWriteItem().withUpdate(updateUserItemData.asUpdate()));
            actions.add(new TransactWriteItem().withDelete(updateWorkoutItemData.asDelete()));
            this.userDAO.executeWriteTransaction(actions);

            this.metrics.commonClose(true);
            return new UserWithWorkout(user, nextWorkout);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
