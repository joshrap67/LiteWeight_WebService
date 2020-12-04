package controllers;

import exceptions.MissingApiRequestKeyException;
import interfaces.ApiRequestController;
import java.util.Map;
import javax.inject.Inject;
import managers.WarmingManager;
import modules.Injector;
import utils.ErrorMessage;
import utils.Metrics;
import imports.ResultStatus;

public class WarmingController implements ApiRequestController {

    @Inject
    public WarmingManager warmingManager;

    public ResultStatus<String> processApiRequest(final Map<String, Object> jsonMap,
        final Metrics metrics)
        throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;

        try {
            Injector.getInjector(metrics).inject(this);
            this.warmingManager.warmEndpoints();
            resultStatus = ResultStatus.successful("Endpoints successfully warmed.");
        } catch (final Exception e) {
            metrics.logWithBody(new ErrorMessage<Map>(classMethod, e));
            resultStatus = ResultStatus.failureBadRequest("Exception in " + classMethod);
        }

        return resultStatus;
    }
}
