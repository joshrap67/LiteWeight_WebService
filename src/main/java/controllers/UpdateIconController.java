package controllers;

import exceptions.MissingApiRequestKeyException;
import helpers.ErrorMessage;
import helpers.JsonHelper;
import helpers.Metrics;
import helpers.RequestFields;
import helpers.ResultStatus;
import interfaces.ApiRequestController;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import managers.UpdateIconManager;
import models.User;
import modules.Injector;

public class UpdateIconController implements ApiRequestController {

    @Inject
    public UpdateIconManager updateIconManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> jsonBody,
        Metrics metrics) throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;

        final List<String> requiredKeys = Arrays
            .asList(RequestFields.ACTIVE_USER, User.ICON);

        if (jsonBody.keySet().containsAll(requiredKeys)) {
            try {
                final String activeUser = (String) jsonBody.get(RequestFields.ACTIVE_USER);
                final String imageData = (String) jsonBody.get(User.ICON);
                byte[] imageBytes = JsonHelper.deserializeByteList(imageData);
                Injector.getInjector(metrics).inject(this);
                resultStatus = this.updateIconManager
                    .execute(activeUser, imageBytes);
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
