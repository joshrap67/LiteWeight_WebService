package controllers;

import com.google.common.collect.ImmutableList;
import exceptions.MissingApiRequestKeyException;
import interfaces.ApiRequestController;
import java.util.Map;
import javax.inject.Inject;
import managers.GetUserDataManager;
import models.User;
import modules.Injector;
import helpers.ErrorMessage;
import helpers.Metrics;
import helpers.RequestFields;
import helpers.ResultStatus;

public class GetUserDataController implements ApiRequestController {

    @Inject
    public GetUserDataManager getUserDataManager;

    @Override
    public ResultStatus<String> processApiRequest(final Map<String, Object> jsonMap,
        final Metrics metrics)
        throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;

        try {
            Injector.getInjector(metrics).inject(this);

            if (jsonMap.containsKey(User.USERNAME)) {
                final String username = (String) jsonMap.get(User.USERNAME);
                resultStatus = this.getUserDataManager.getUserData(username);
            } else if (jsonMap.containsKey(RequestFields.ACTIVE_USER)) {
                final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
                resultStatus = this.getUserDataManager.getActiveUserData(activeUser);
            } else {
                throw new MissingApiRequestKeyException(
                    ImmutableList.of(RequestFields.ACTIVE_USER));
            }
        } catch (final MissingApiRequestKeyException e) {
            throw e;
        } catch (final Exception e) {
            metrics.logWithBody(new ErrorMessage<Map>(classMethod, e));
            resultStatus = ResultStatus.failureBadRequest("Exception in " + classMethod);
        }

        return resultStatus;
    }
}