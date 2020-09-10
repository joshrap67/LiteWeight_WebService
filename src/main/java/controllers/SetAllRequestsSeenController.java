package controllers;

import exceptions.MissingApiRequestKeyException;
import helpers.ErrorMessage;
import helpers.Metrics;
import helpers.RequestFields;
import helpers.ResultStatus;
import interfaces.ApiRequestController;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import managers.SetAllRequestsSeenManager;
import modules.Injector;

public class SetAllRequestsSeenController implements ApiRequestController {

    @Inject
    public SetAllRequestsSeenManager setAllRequestsSeenManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> json,
        Metrics metrics) throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;

        final List<String> requiredKeys = Collections.singletonList(RequestFields.ACTIVE_USER);

        if (json.keySet().containsAll(requiredKeys)) {
            try {
                final String activeUser = (String) json.get(RequestFields.ACTIVE_USER);

                Injector.getInjector(metrics).inject(this);
                resultStatus = this.setAllRequestsSeenManager.execute(activeUser);
            } catch (Exception e) {
                metrics.logWithBody(new ErrorMessage<>(classMethod, e));
                resultStatus = ResultStatus.failureBadRequest("Exception in " + classMethod);
            }
        } else {
            throw new MissingApiRequestKeyException(requiredKeys);
        }

        return resultStatus;
    }
}
