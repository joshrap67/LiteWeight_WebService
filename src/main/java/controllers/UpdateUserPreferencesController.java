package controllers;

import exceptions.MissingApiRequestKeyException;
import helpers.ErrorMessage;
import helpers.Metrics;
import helpers.RequestFields;
import helpers.ResultStatus;
import interfaces.ApiRequestController;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import managers.UpdateUserPreferencesManager;
import models.User;
import models.UserPreferences;
import modules.Injector;

public class UpdateUserPreferencesController implements ApiRequestController {

    @Inject
    public UpdateUserPreferencesManager updateUserPreferencesManager;

    @Override
    public ResultStatus<String> processApiRequest(Map<String, Object> jsonBody,
        Metrics metrics) throws MissingApiRequestKeyException {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        ResultStatus<String> resultStatus;

        final List<String> requiredKeys = Arrays
            .asList(RequestFields.ACTIVE_USER, User.USER_PREFERENCES);

        if (jsonBody.keySet().containsAll(requiredKeys)) {
            try {
                final String activeUser = (String) jsonBody.get(RequestFields.ACTIVE_USER);
                final UserPreferences userPreferences = new UserPreferences(
                    (Map<String, Object>) jsonBody.get(User.USER_PREFERENCES));
                Injector.getInjector(metrics).inject(this);
                this.updateUserPreferencesManager.updateUserPreferences(activeUser, userPreferences);
                resultStatus = ResultStatus.successful("User prefs updated successfully.");
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
