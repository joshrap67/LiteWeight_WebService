package managers;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import daos.UserDAO;
import exceptions.InvalidAttributeException;
import exceptions.UserNotFoundException;
import helpers.Metrics;
import javax.inject.Inject;
import models.User;
import models.WorkoutUser;

public class ResetWorkoutStatisticsManager {

    private final UserDAO userDAO;
    private final Metrics metrics;

    @Inject
    public ResetWorkoutStatisticsManager(final UserDAO userDAO,
        final Metrics metrics) {
        this.userDAO = userDAO;
        this.metrics = metrics;
    }

    /**
     * Resets the statistics of a given workout by updating the workout meta in the user's mapping.
     *
     * @param activeUser user that is resetting the workout statistics.
     * @param workoutId  id of the workout whose statistics are to be reset.
     * @return User user with updated workout meta mapping.
     * @throws InvalidAttributeException
     * @throws UserNotFoundException
     */
    public User resetStatistics(final String activeUser, final String workoutId)
        throws InvalidAttributeException, UserNotFoundException {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        try {
            final User user = this.userDAO.getUser(activeUser);

            final WorkoutUser workoutUser = user.getUserWorkouts().get(workoutId);
            workoutUser.setAverageExercisesCompleted(0.0);
            workoutUser.setTimesCompleted(0);
            workoutUser.setTotalExercisesSum(0);

            final UpdateItemSpec updateUserItemData = new UpdateItemSpec()
                .withUpdateExpression("set " + User.WORKOUTS + ".#workoutId= :workoutsMap")
                .withValueMap(new ValueMap().withMap(":workoutsMap", workoutUser.asMap()))
                .withNameMap(new NameMap().with("#workoutId", workoutId));
            this.userDAO.updateUser(activeUser, updateUserItemData);

            this.metrics.commonClose(true);
            return user;
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
