package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import exceptions.InvalidAttributeException;
import exceptions.UserNotFoundException;
import helpers.Metrics;
import javax.inject.Inject;
import models.User;
import models.WorkoutUser;

public class ResetWorkoutStatisticsManager {

    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;

    @Inject
    public ResetWorkoutStatisticsManager(final DatabaseAccess databaseAccess,
        final Metrics metrics) {
        this.databaseAccess = databaseAccess;
        this.metrics = metrics;
    }

    /**
     * @param workoutId TODO
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public User execute(final String activeUser, final String workoutId)
        throws InvalidAttributeException, UserNotFoundException {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        try {
            final User user = this.databaseAccess.getUser(activeUser);

            final WorkoutUser workoutUser = user.getUserWorkouts().get(workoutId);
            workoutUser.setAverageExercisesCompleted(0.0);
            workoutUser.setTimesCompleted(0);
            workoutUser.setTotalExercisesSum(0);

            final UpdateItemSpec updateUserItemData = new UpdateItemSpec()
                .withUpdateExpression("set " + User.WORKOUTS + ".#workoutId= :workoutsMap")
                .withValueMap(new ValueMap().withMap(":workoutsMap", workoutUser.asMap()))
                .withNameMap(new NameMap().with("#workoutId", workoutId));
            this.databaseAccess.updateUser(activeUser, updateUserItemData);

            this.metrics.commonClose(true);
            return user;
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
