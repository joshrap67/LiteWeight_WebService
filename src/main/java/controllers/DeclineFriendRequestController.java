package controllers;

import exceptions.ManagerExecutionException;
import exceptions.MissingApiRequestKeyException;
import exceptions.UserNotFoundException;
import utils.ErrorMessage;
import utils.Metrics;
import imports.RequestFields;
import imports.ResultStatus;
import interfaces.ApiRequestController;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import managers.DeclineFriendRequestManager;
import models.User;
import modules.Injector;

public class DeclineFriendRequestController implements ApiRequestController {

    @Inject
    public DeclineFriendRequestManager declineFriendRequestManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> json, Metrics metrics)
        throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;
        final List<String> requiredKeys = Arrays.asList(RequestFields.ACTIVE_USER, User.USERNAME);

        if (json.keySet().containsAll(requiredKeys)) {
            try {
                final String activeUser = (String) json.get(RequestFields.ACTIVE_USER);
                final String declinedUser = (String) json.get(User.USERNAME);

                Injector.getInjector(metrics).inject(this);
                this.declineFriendRequestManager.declineRequest(activeUser, declinedUser);
                resultStatus = ResultStatus.successful("Request successfully declined.");
            } catch (ManagerExecutionException meu) {
                metrics.log("Input error: " + meu.getMessage());
                resultStatus = ResultStatus.failureBadRequest(meu.getMessage());
            } catch (UserNotFoundException unfe) {
                metrics.logWithBody(new ErrorMessage<>(classMethod, unfe));
                resultStatus = ResultStatus.failureBadRequest(unfe.getMessage());
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
