package managers;

import aws.DatabaseAccess;
import javax.inject.Inject;
import helpers.ErrorMessage;
import helpers.Metrics;
import helpers.ResultStatus;

public class WarmingManager {

    private final DatabaseAccess dbAccessManager;
    //    private final S3AccessManager s3AccessManager;
    //    private final SnsAccessManager snsAccessManager;
    private final Metrics metrics;

    @Inject
    public WarmingManager(final DatabaseAccess dbAccessManager,
        final Metrics metrics) {
        this.dbAccessManager = dbAccessManager;
        this.metrics = metrics;
    }

    public ResultStatus handle() {
        final String classMethod = "WarmingManager.handle";
        this.metrics.commonSetup(classMethod);

        //squelch metrics on warming -> we only want metrics on user impacting cold starts
        this.metrics.setPrintMetrics(false);

        ResultStatus resultStatus;

        try {
            this.dbAccessManager.describeTables();
//            this.s3AccessManager.imageBucketExists();
//            this.snsAccessManager.getPlatformAttributes(Config.PUSH_SNS_PLATFORM_ARN);

            resultStatus = ResultStatus.successful("Endpoints warmed.");
        } catch (Exception e) {
            this.metrics.log(new ErrorMessage(classMethod, e));
            resultStatus = ResultStatus.failure("Exception in " + classMethod);
        }

        this.metrics.commonClose(resultStatus.success);
        return resultStatus;
    }
}
