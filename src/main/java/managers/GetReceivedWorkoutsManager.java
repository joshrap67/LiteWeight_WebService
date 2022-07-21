package managers;

import daos.UserDAO;
import utils.Metrics;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import models.SharedWorkoutMeta;
import models.User;
import org.joda.time.DateTime;

public class GetReceivedWorkoutsManager {

    private final UserDAO userDAO;
    private final Metrics metrics;
    public static final int WORKOUT_BATCH_SIZE = 25;

    @Inject
    public GetReceivedWorkoutsManager(final UserDAO userDAO, final Metrics metrics) {
        this.userDAO = userDAO;
        this.metrics = metrics;
    }

    /**
     * Gets a batch of received workout metas to the active user based on the current batch number after all workouts
     * are sorted by date sent. E.g. if there are 10 received workouts with a batch size of 2 and the user passes in
     * batch number 3, this method will return a map of workout metas indexed 6 and 7.
     *
     * @param activeUser  username of the user that made the api request, trying to get data about themselves.
     * @param batchNumber batch number that the user is currently on.
     * @return Map of the received workout metas that fall into this batch
     */
    public Map<String, SharedWorkoutMeta> getReceivedWorkouts(final String activeUser, final int batchNumber)
        throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".getReceivedWorkouts";
        this.metrics.commonSetup(classMethod);

        try {
            final User activeUserObject = this.userDAO.getUser(activeUser);
            final Map<String, SharedWorkoutMeta> receivedWorkouts = activeUserObject.getReceivedWorkouts();

            this.metrics.commonClose(true);
            return getBatchOfWorkouts(receivedWorkouts, batchNumber);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }

    public static Map<String, SharedWorkoutMeta> getBatchOfWorkouts(
        final Map<String, SharedWorkoutMeta> receivedWorkouts, final Integer batchNumber) {
        int newestReceivedWorkoutIndex = batchNumber * WORKOUT_BATCH_SIZE;
        int oldestReceivedWorkoutIndex = ((batchNumber + 1) * WORKOUT_BATCH_SIZE) - 1;

        ArrayList<String> workoutIds = new ArrayList<>(receivedWorkouts.keySet());
        Map<String, SharedWorkoutMeta> workoutsBatch = new LinkedHashMap<>();

        // get all the received workouts from the first workout to the oldest workout in the batch
        if (workoutIds.size() > newestReceivedWorkoutIndex) {
            if (workoutIds.size() <= oldestReceivedWorkoutIndex) {
                // if on the last batch and remainder doesn't fit into the entire batch, last index is the size
                oldestReceivedWorkoutIndex = workoutIds.size() - 1;
            }

            // sort the workouts based on sent time of the workout
            workoutIds.sort((o1, o2) -> {
                DateTime dateTime = DateTime.parse(receivedWorkouts.get(o1).getDateSent());
                DateTime dateTimeOther = DateTime.parse(receivedWorkouts.get(o2).getDateSent());
                return dateTimeOther.compareTo(dateTime);
            });

            for (int i = newestReceivedWorkoutIndex; i <= oldestReceivedWorkoutIndex; i++) {
                String workoutId = workoutIds.get(i);
                workoutsBatch.put(workoutId, receivedWorkouts.get(workoutId));
            }
        } // else no workouts in this index range

        return workoutsBatch;
    }
}
