package controllers;

import exceptions.ManagerExecutionException;
import exceptions.MissingApiRequestKeyException;
import exceptions.UserNotFoundException;
import utils.ErrorMessage;
import utils.JsonUtils;
import utils.Metrics;
import imports.RequestFields;
import imports.ResultStatus;
import interfaces.ApiRequestController;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import managers.SendFriendRequestManager;
import models.User;
import modules.Injector;
import responses.FriendResponse;

public class SendFriendRequestController implements ApiRequestController {

    @Inject
    public SendFriendRequestManager sendFriendRequestManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> json, Metrics metrics)
        throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;
        final List<String> requiredKeys = Arrays.asList(RequestFields.ACTIVE_USER, User.USERNAME);

        if (json.keySet().containsAll(requiredKeys)) {
            try {
                final String activeUser = (String) json.get(RequestFields.ACTIVE_USER);
                final String userToAdd = (String) json.get(User.USERNAME);

                Injector.getInjector(metrics).inject(this);
                final FriendResponse friendResponse = this.sendFriendRequestManager.sendRequest(activeUser, userToAdd);
                resultStatus = ResultStatus.successful(JsonUtils.serializeMap(friendResponse.asResponse()));
            } catch (ManagerExecutionException meu) {
                metrics.log("Input error: " + meu.getMessage());
                resultStatus = ResultStatus.failureBadRequest(meu.getMessage());
            } catch (UserNotFoundException unfe) {
                metrics.logWithBody(new ErrorMessage<>(classMethod, unfe));
                resultStatus = ResultStatus.failureBadRequest(unfe.getMessage());
            } catch (Exception e) {
                metrics.logWithBody(new ErrorMessage<>(classMethod, e));
                resultStatus = ResultStatus.failureBadRequest("Unable to send friend request.");
            }
        } else {
            throw new MissingApiRequestKeyException(requiredKeys);
        }

        return resultStatus;
    }
}
