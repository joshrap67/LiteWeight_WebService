package managers;

import aws.DatabaseAccess;
import aws.S3Access;
import javax.inject.Inject;
import helpers.ErrorMessage;
import helpers.Metrics;
import helpers.ResultStatus;

public class WarmingManager {

    private final DatabaseAccess dbAccessManager;
    private final S3Access s3Access;
    //    private final SnsAccessManager snsAccessManager;
    private final Metrics metrics;

    @Inject
    public WarmingManager(final DatabaseAccess dbAccessManager,
        final Metrics metrics, final S3Access s3Access) {
        this.dbAccessManager = dbAccessManager;
        this.s3Access = s3Access;
        this.metrics = metrics;
    }

    public ResultStatus<String> execute() {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        this.metrics.setPrintMetrics(false);

        ResultStatus<String> resultStatus;

        try {
            this.dbAccessManager.describeTables();
            this.s3Access.imageBucketExists();
//            this.snsAccessManager.getPlatformAttributes(Config.PUSH_SNS_PLATFORM_ARN);

            resultStatus = ResultStatus.successful("Endpoints warmed.");
        } catch (Exception e) {
            this.metrics.log(new ErrorMessage(classMethod, e));
            resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod);
        }

        this.metrics.commonClose(resultStatus.responseCode);
        return resultStatus;
    }
}
