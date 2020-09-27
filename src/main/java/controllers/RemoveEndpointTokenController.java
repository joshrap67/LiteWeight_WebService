package controllers;

import exceptions.MissingApiRequestKeyException;
import exceptions.UserNotFoundException;
import helpers.ErrorMessage;
import helpers.Metrics;
import helpers.RequestFields;
import helpers.ResultStatus;
import interfaces.ApiRequestController;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import managers.RemoveEndpointTokenManager;
import modules.Injector;

public class RemoveEndpointTokenController implements ApiRequestController {

    @Inject
    public RemoveEndpointTokenManager removeEndpointTokenManager;

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
                this.removeEndpointTokenManager.unregisterDevice(activeUser);
                resultStatus = ResultStatus.successful("Endpoint successfully unregistered.");
            } catch (UserNotFoundException unfe) {
                metrics.logWithBody(new ErrorMessage<>(classMethod, unfe));
                resultStatus = ResultStatus.failureBadEntity(unfe.getMessage());
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
