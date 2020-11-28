package controllers;

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
        final List<String> requiredKeys = Arrays.asList(RequestFields.ACTIVE_USER, User.ICON);

        if (jsonBody.keySet().containsAll(requiredKeys)) {
            try {
                final String activeUser = (String) jsonBody.get(RequestFields.ACTIVE_USER);
                final String imageData = (String) jsonBody.get(User.ICON);
                byte[] imageBytes = JsonUtils.deserializeByteList(imageData);

                Injector.getInjector(metrics).inject(this);
                boolean success = this.updateIconManager
                    .updateIcon(activeUser, imageBytes);
                if (success) {
                    resultStatus = ResultStatus.successful("Picture updated successfully.");
                } else {
                    resultStatus = ResultStatus.failureBadRequest("Picture failed to update.");
                }
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
