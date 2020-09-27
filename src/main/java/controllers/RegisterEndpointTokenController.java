package controllers;

import exceptions.ManagerExecutionException;
import exceptions.MissingApiRequestKeyException;
import exceptions.UserNotFoundException;
import helpers.ErrorMessage;
import helpers.Metrics;
import helpers.RequestFields;
import helpers.ResultStatus;
import interfaces.ApiRequestController;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import managers.RegisterEndpointTokenManager;
import models.User;
import modules.Injector;

public class RegisterEndpointTokenController implements ApiRequestController {

    @Inject
    public RegisterEndpointTokenManager registerEndpointTokenManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> json,
        Metrics metrics) throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;

        final List<String> requiredKeys = Arrays
            .asList(RequestFields.ACTIVE_USER, User.PUSH_ENDPOINT_ARN);

        if (json.keySet().containsAll(requiredKeys)) {
            try {
                final String activeUser = (String) json.get(RequestFields.ACTIVE_USER);
                final String token = (String) json.get(User.PUSH_ENDPOINT_ARN);

                Injector.getInjector(metrics).inject(this);
                this.registerEndpointTokenManager.registerDevice(activeUser, token);
                resultStatus = ResultStatus.successful("Endpoint registered successfully.");
            } catch (ManagerExecutionException meu) {
                metrics.log("Input error: " + meu.getMessage());
                resultStatus = ResultStatus.failureBadEntity(meu.getMessage());
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
