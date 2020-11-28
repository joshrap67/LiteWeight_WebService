package managers;

import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import daos.UserDAO;
import daos.WorkoutDAO;
import exceptions.ManagerExecutionException;
import helpers.Metrics;
import helpers.UpdateItemData;
import helpers.Validator;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import models.User;
import models.Workout;
import models.WorkoutMeta;

public class RenameWorkoutManager {

    private final UserDAO userDAO;
    private final WorkoutDAO workoutDAO;
    private final Metrics metrics;

    @Inject
    public RenameWorkoutManager(final UserDAO userDAO, final WorkoutDAO workoutDAO,
        final Metrics metrics) {
        this.workoutDAO = workoutDAO;
        this.userDAO = userDAO;
        this.metrics = metrics;
    }

    /**
     * Renames a given workout and loops through all owned exercises to update their workout mapping
     * to have this new name,
     *
     * @param activeUser     user that is renaming the workout.
     * @param workoutId      id of the workout that is to be renamed.
     * @param newWorkoutName new name of the workout.
     * @return User the updated user with the updated workout meta and exercise mapping.
     * @throws Exception if there is input error.
     */
    public User renameWorkout(final String activeUser, final String workoutId,
        final String newWorkoutName) throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".renameWorkout";
        this.metrics.commonSetup(classMethod);

        try {
            final User user = this.userDAO.getUser(activeUser);

            final String errorMessage = Validator.validWorkoutName(newWorkoutName, user);
            if (!errorMessage.isEmpty()) {
                this.metrics.commonClose(false);
                throw new ManagerExecutionException(errorMessage);
            }

            // no error, so go ahead and try and rename the workout
            final Workout workout = this.workoutDAO.getWorkout(workoutId);
            workout.setWorkoutName(newWorkoutName);
            // update all the exercises that are apart of this newly renamed workout
            updateUserExercises(user, workoutId, newWorkoutName);
            WorkoutMeta workoutMeta = user.getUserWorkouts().get(workoutId);
            workoutMeta.setWorkoutName(newWorkoutName);

            final UpdateItemData updateUserItemData = new UpdateItemData(activeUser,
                UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("set " +
                    User.WORKOUTS + ".#workoutId = :workoutMap, " +
                    User.EXERCISES + "= :exercisesMap")
                .withValueMap(new ValueMap()
                    .withMap(":workoutMap", workoutMeta.asMap())
                    .withMap(":exercisesMap", user.getUserExercisesMap()))
                .withNameMap(new NameMap().with("#workoutId", workoutId));

            final UpdateItemData updateWorkoutItemData = new UpdateItemData(workoutId,
                WorkoutDAO.WORKOUT_TABLE_NAME)
                .withUpdateExpression("set " + Workout.WORKOUT_NAME + "= :workoutNameVal")
                .withValueMap(new ValueMap().withString(":workoutNameVal", newWorkoutName));
            // want a transaction since more than one object is being updated at once
            final List<TransactWriteItem> actions = new ArrayList<>();
            actions.add(new TransactWriteItem().withUpdate(updateUserItemData.asUpdate()));
            actions.add(new TransactWriteItem().withUpdate(updateWorkoutItemData.asUpdate()));
            this.userDAO.executeWriteTransaction(actions);

            this.metrics.commonClose(true);
            return user;
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }

    private static void updateUserExercises(final User user, final String workoutId,
        final String newWorkoutName) {
        // loops through all user exercises and updates the old workout name with the newly renamed one
        for (String exerciseId : user.getOwnedExercises().keySet()) {
            if (user.getOwnedExercises().get(exerciseId).getWorkouts().containsKey(workoutId)) {
                // old workout name found, replace it
                user.getOwnedExercises().get(exerciseId).getWorkouts()
                    .put(workoutId, newWorkoutName);
            }
        }
    }
}
