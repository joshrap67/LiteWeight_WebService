package managers;

import daos.SentWorkoutDAO;
import helpers.Metrics;
import javax.inject.Inject;
import models.SentWorkout;

public class GetSentWorkoutManager {

    private final SentWorkoutDAO sentWorkoutDAO;
    private final Metrics metrics;

    @Inject
    public GetSentWorkoutManager(final SentWorkoutDAO sentWorkoutDAO, final Metrics metrics) {
        this.sentWorkoutDAO = sentWorkoutDAO;
        this.metrics = metrics;
    }

    /**
     * This method is used when the user first successfully signs into the app. It provides the user
     * object to the user as well as the current workout if there is one.
     *
     * @param activeUser username of the user that made the api request, trying to get data about
     *                   themselves.
     * @return User object and workout object that of the current workout (null if no workouts
     * exist).
     */
    public SentWorkout getSentWorkout(final String activeUser, final String workoutId)
        throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".getSentWorkout";
        this.metrics.commonSetup(classMethod);

        try {
            // todo do validation to make sure user actually "owns" the workout??
            return this.sentWorkoutDAO.getSentWorkout(workoutId);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
