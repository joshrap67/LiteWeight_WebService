package managers;

import daos.SharedWorkoutDAO;
import services.NotificationService;
import daos.UserDAO;
import daos.WorkoutDAO;
import imports.Config;
import javax.inject.Inject;
import utils.Metrics;

public class WarmingManager {

    private final UserDAO userDAO;
    private final WorkoutDAO workoutDAO;
    private final SharedWorkoutDAO sharedWorkoutDAO;
    private final NotificationService notificationService;
    private final Metrics metrics;

    @Inject
    public WarmingManager(final UserDAO userDAO, final WorkoutDAO workoutDAO,
        final SharedWorkoutDAO sharedWorkoutDAO, final Metrics metrics,
        final NotificationService notificationService) {
        this.sharedWorkoutDAO = sharedWorkoutDAO;
        this.userDAO = userDAO;
        this.workoutDAO = workoutDAO;
        this.metrics = metrics;
        this.notificationService = notificationService;
    }

    /**
     * Warms all the endpoints that this API interacts with. This ensures that whenever a client engages this API, there
     * is already an instance of the service running in lambda.
     */
    public void warmEndpoints() {
        final String classMethod = this.getClass().getSimpleName() + ".warmEndpoints";
        this.metrics.commonSetup(classMethod);
        this.metrics.setPrintMetrics(false);

        try {
            this.userDAO.describeUserTable();
            this.sharedWorkoutDAO.describeSharedWorkoutsTable();
            this.workoutDAO.describeWorkoutTable();
            this.notificationService.getPlatformAttributes(Config.PUSH_SNS_PLATFORM_ARN);

            this.metrics.commonClose(true);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
