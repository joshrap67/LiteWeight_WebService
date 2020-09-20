package controllers;

import com.google.common.collect.ImmutableList;
import exceptions.MissingApiRequestKeyException;
import helpers.ErrorMessage;
import helpers.Metrics;
import helpers.RequestFields;
import helpers.ResultStatus;
import interfaces.ApiRequestController;
import java.util.Map;
import javax.inject.Inject;
import managers.NewUserManager;
import models.User;
import modules.Injector;

public class NewUserController implements ApiRequestController {

    @Inject
    public NewUserManager newUserManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> json,
        Metrics metrics) throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;

        try {
            Injector.getInjector(metrics).inject(this);

            if (json.containsKey(User.USERNAME)) {
                final String username = (String) json.get(User.USERNAME);
                resultStatus = this.newUserManager.execute(username);
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
