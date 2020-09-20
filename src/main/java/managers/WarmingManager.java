package managers;

import aws.DatabaseAccess;
import aws.SnsAccess;
import helpers.Config;
import javax.inject.Inject;
import helpers.Metrics;

public class WarmingManager {

    private final DatabaseAccess dbAccessManager;
    private final SnsAccess snsAccess;
    private final Metrics metrics;

    @Inject
    public WarmingManager(final DatabaseAccess dbAccessManager, final Metrics metrics,
        final SnsAccess snsAccess) {
        this.dbAccessManager = dbAccessManager;
        this.metrics = metrics;
        this.snsAccess = snsAccess;
    }

    public void execute() {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);
        this.metrics.setPrintMetrics(false);

        try {
            this.dbAccessManager.describeTables();
            this.snsAccess.getPlatformAttributes(Config.PUSH_SNS_PLATFORM_ARN_DEV);

            this.metrics.commonClose(true);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
