package controllers;

import com.google.common.collect.ImmutableList;
import exceptions.ManagerExecutionException;
import exceptions.MissingApiRequestKeyException;
import utils.JsonUtils;
import interfaces.ApiRequestController;
import java.util.Map;
import javax.inject.Inject;
import managers.GetUserDataManager;
import models.User;
import modules.Injector;
import utils.ErrorMessage;
import utils.Metrics;
import imports.RequestFields;
import imports.ResultStatus;

public class GetUserDataController implements ApiRequestController {

    @Inject
    public GetUserDataManager getUserDataManager;

    @Override
    public ResultStatus<String> processApiRequest(final Map<String, Object> jsonMap, final Metrics metrics)
        throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;
        try {
            Injector.getInjector(metrics).inject(this);

            if (jsonMap.containsKey(User.USERNAME)) {
                final String username = (String) jsonMap.get(User.USERNAME);
                final User result = this.getUserDataManager.getUserData(username);
                resultStatus = ResultStatus.successful(JsonUtils.serializeMap(result.asResponse()));
            } else if (jsonMap.containsKey(RequestFields.ACTIVE_USER)) {
                final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
                final User result = this.getUserDataManager.getActiveUserData(activeUser);
                resultStatus = ResultStatus.successful(JsonUtils.serializeMap(result.asResponse()));
            } else {
                throw new MissingApiRequestKeyException(ImmutableList.of(RequestFields.ACTIVE_USER));
            }
        } catch (final MissingApiRequestKeyException e) {
            throw e;
        } catch (final ManagerExecutionException meu) {
            metrics.log("Input error: " + meu.getMessage());
            resultStatus = ResultStatus.failureBadRequest(meu.getMessage());
        } catch (final Exception e) {
            metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failureBadRequest("Exception in " + classMethod);
        }

        return resultStatus;
    }
}