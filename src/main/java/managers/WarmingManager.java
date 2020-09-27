package managers;

import aws.SnsAccess;
import daos.UserDAO;
import daos.WorkoutDAO;
import helpers.Config;
import javax.inject.Inject;
import helpers.Metrics;

public class WarmingManager {

    private final UserDAO userDAO;
    private final WorkoutDAO workoutDAO;
    private final SnsAccess snsAccess;
    private final Metrics metrics;

    @Inject
    public WarmingManager(final UserDAO userDAO, final WorkoutDAO workoutDAO, final Metrics metrics,
        final SnsAccess snsAccess) {
        this.userDAO = userDAO;
        this.workoutDAO = workoutDAO;
        this.metrics = metrics;
        this.snsAccess = snsAccess;
    }

    /**
     * Warms all the endpoints that this API interacts with. This ensures that whenever a client
     * engages this API, there is already an instance of the service running in lambda.
     */
    public void warmEndpoints() {
        final String classMethod = this.getClass().getSimpleName() + ".warmEndpoints";
        this.metrics.commonSetup(classMethod);
        this.metrics.setPrintMetrics(false);

        try {
            this.userDAO.describeUserTable();
            this.workoutDAO.describeWorkoutTable();
            this.snsAccess.getPlatformAttributes(Config.PUSH_SNS_PLATFORM_ARN_DEV);

            this.metrics.commonClose(true);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
