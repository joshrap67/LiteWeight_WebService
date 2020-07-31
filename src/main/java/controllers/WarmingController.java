package controllers;

import exceptions.MissingApiRequestKeyException;
import interfaces.ApiRequestController;
import java.util.Map;
import javax.inject.Inject;
import managers.WarmingManager;
import modules.Injector;
import helpers.ErrorMessage;
import helpers.Metrics;
import helpers.ResultStatus;

public class WarmingController implements ApiRequestController {

    @Inject
    public WarmingManager warmingManager;

    public ResultStatus processApiRequest(final Map<String, Object> jsonMap, final Metrics metrics)
        throws MissingApiRequestKeyException {
        final String classMethod = "WarmingController.processApiRequest";

        ResultStatus resultStatus;

        try {
            Injector.getInjector(metrics).inject(this);
            resultStatus = this.warmingManager.handle();
        } catch (final Exception e) {
            metrics.logWithBody(new ErrorMessage<Map>(classMethod, e));
            resultStatus = ResultStatus.failure("Exception in " + classMethod);
        }

        return resultStatus;
    }
}
