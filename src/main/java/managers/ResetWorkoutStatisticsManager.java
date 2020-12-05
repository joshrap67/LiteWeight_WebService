package managers;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import daos.UserDAO;
import exceptions.InvalidAttributeException;
import exceptions.UserNotFoundException;
import utils.Metrics;
import javax.inject.Inject;
import models.User;
import models.WorkoutMeta;

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
     * @return updated workout meta with all statistics reset.
     */
    public WorkoutMeta resetStatistics(final String activeUser, final String workoutId)
        throws InvalidAttributeException, UserNotFoundException {
        final String classMethod = this.getClass().getSimpleName() + ".resetStatistics";
        this.metrics.commonSetup(classMethod);

        try {
            final User user = this.userDAO.getUser(activeUser);

            final WorkoutMeta workoutMeta = user.getWorkoutMetas().get(workoutId);
            workoutMeta.setAverageExercisesCompleted(0.0);
            workoutMeta.setTimesCompleted(0);
            workoutMeta.setTotalExercisesSum(0);

            UpdateItemSpec updateUserItemData = new UpdateItemSpec()
                .withUpdateExpression("set " + User.WORKOUTS + ".#workoutId= :workoutsMap")
                .withValueMap(new ValueMap().withMap(":workoutsMap", workoutMeta.asMap()))
                .withNameMap(new NameMap().with("#workoutId", workoutId));
            this.userDAO.updateUser(activeUser, updateUserItemData);

            this.metrics.commonClose(true);
            return workoutMeta;
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
