package managers;

import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import daos.UserDAO;
import daos.WorkoutDAO;
import exceptions.ManagerExecutionException;
import utils.AttributeValueUtils;
import utils.Metrics;
import utils.UpdateItemTemplate;
import utils.Validator;
import utils.WorkoutUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import models.Routine;
import models.User;
import models.Workout;
import models.WorkoutMeta;
import responses.UserWithWorkout;

public class NewWorkoutManager {

    private final WorkoutDAO workoutDAO;
    private final UserDAO userDAO;
    private final Metrics metrics;

    @Inject
    public NewWorkoutManager(final WorkoutDAO workoutDAO, final UserDAO userDAO,
        final Metrics metrics) {
        this.userDAO = userDAO;
        this.workoutDAO = workoutDAO;
        this.metrics = metrics;
    }

    /**
     * Creates a new workout if all input is valid and if the user has not already reached the max number of workouts
     * allowed. Updates all exercises that are apart of the newly created workout to have this workout listed in their
     * workout maps.
     *
     * @param activeUser  user that is creating this new workout.
     * @param workoutName name of the workout that is to be created.
     * @param routine     the routine of the workout to be created.
     * @return UserWithWorkout the newly created workout and updated user object.
     * @throws Exception thrown if there exists input validation.
     */
    public UserWithWorkout createNewWorkout(final String activeUser, final String workoutName, final Routine routine)
        throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".createNewWorkout";
        this.metrics.commonSetup(classMethod);

        try {
            final User user = this.userDAO.getUser(activeUser);

            final String workoutId = UUID.randomUUID().toString();
            final String errorMessage = Validator.validNewWorkoutInput(workoutName, user, routine);

            if (!errorMessage.isEmpty()) {
                this.metrics.commonClose(false);
                throw new ManagerExecutionException(errorMessage);
            }
            final String creationTime = Instant.now().toString();

            // no error, so go ahead and try and insert this new workout along with updating active user
            final Workout newWorkout = new Workout();
            newWorkout.setCreationDate(creationTime);
            newWorkout.setCreator(activeUser);
            newWorkout.setWorkoutId(workoutId);
            newWorkout.setWorkoutName(workoutName);
            newWorkout.setRoutine(routine);
            newWorkout.setCurrentDay(0);
            newWorkout.setCurrentWeek(0);

            final WorkoutMeta workoutMeta = new WorkoutMeta();
            workoutMeta.setWorkoutName(workoutName);
            workoutMeta.setAverageExercisesCompleted(0.0);
            workoutMeta.setDateLast(creationTime);
            workoutMeta.setTimesCompleted(0);
            workoutMeta.setTotalExercisesSum(0);
            // need to set it here so frontend gets updated user item back
            user.putNewWorkoutMeta(workoutId, workoutMeta);
            user.setCurrentWorkout(workoutId);
            // update all the exercises that are now a part of this workout
            WorkoutUtils.updateOwnedExercises(user, routine, workoutId, workoutName);

            UpdateItemTemplate updateItemData = new UpdateItemTemplate(activeUser, UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("set " +
                    User.CURRENT_WORKOUT + " = :currentWorkoutVal, " +
                    User.WORKOUTS + ".#workoutId= :workoutUserMap, " +
                    User.EXERCISES + "= :exercisesMap")
                .withValueMap(new ValueMap()
                    .withString(":currentWorkoutVal", workoutId)
                    .withMap(":workoutUserMap", workoutMeta.asMap())
                    .withMap(":exercisesMap", user.getOwnedExercisesMap()))
                .withNameMap(new NameMap().with("#workoutId", workoutId));

            final List<TransactWriteItem> actions = new ArrayList<>();
            actions.add(new TransactWriteItem().withUpdate(updateItemData.asUpdate()));
            actions.add(new TransactWriteItem()
                .withPut(new Put().withTableName(WorkoutDAO.WORKOUT_TABLE_NAME).withItem(
                    AttributeValueUtils.convertMapToAttributeValueMap(newWorkout.asMap()))));
            this.workoutDAO.executeWriteTransaction(actions);

            this.metrics.commonClose(true);
            return new UserWithWorkout(user, newWorkout);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
