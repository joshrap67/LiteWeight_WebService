package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.Item;
import exceptions.UserNotFoundException;
import java.util.Optional;
import javax.inject.Inject;
import helpers.ErrorMessage;
import helpers.JsonHelper;
import helpers.Metrics;
import helpers.ResultStatus;
import responses.UserResponse;

public class GetUserDataManager {

    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;
    private final NewUserManager newUserManager;

    @Inject
    public GetUserDataManager(final DatabaseAccess dbAccessManager, final Metrics metrics,
        final NewUserManager newUserManager) {
        this.databaseAccess = dbAccessManager;
        this.metrics = metrics;
        this.newUserManager = newUserManager;
    }

    /**
     * This method gets the data of another user.
     *
     * @param username The user that made the api request, trying to get data about themselves.
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public ResultStatus<String> getUserData(final String username) {
        final String classMethod = this.getClass().getSimpleName() + ".getUserData";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;
        try {
            UserResponse userResponse;
            Item user = Optional.ofNullable(this.databaseAccess.getUserItem(username))
                .orElseThrow(
                    () -> new UserNotFoundException(String.format("%s not found.", username)));
            userResponse = new UserResponse(user);
            resultStatus = ResultStatus.successful(JsonHelper.serializeMap(userResponse.asMap()));
        } catch (UserNotFoundException unfe) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, unfe));
            resultStatus = ResultStatus.failureBadEntity(unfe.getMessage());
        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod);
        }

        this.metrics.commonClose(resultStatus.success);
        return resultStatus;
    }

    /**
     * This method gets the active user's data. If the active user's data does not exist, we assume
     * this is their first login and we enter a new user object in the db.
     *
     * @param activeUser The user that made the api request, trying to get data about themselves.
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public ResultStatus<String> getActiveUserData(final String activeUser) {
        final String classMethod = this.getClass().getSimpleName() + ".getActiveUserData";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;
        try {
            UserResponse userResponse;
            Item user = this.databaseAccess.getUserItem(activeUser);
            if (user == null) {
                // user has not been added yet in the DB, so add them
                ResultStatus<String> userResultStatus = this.newUserManager.execute(activeUser);
                if (userResultStatus.responseCode == ResultStatus.SUCCESS_CODE) {
                    userResponse = new UserResponse(
                        JsonHelper.deserialize(userResultStatus.resultMessage));
                    resultStatus = ResultStatus
                        .successful(JsonHelper.serializeMap(userResponse.asMap()));
                } else {
                    this.metrics.log("Error creating new user.");
                    resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod);
                }
            } else {
                // user already exists in DB so just return its data
                userResponse = new UserResponse(user);
                resultStatus = ResultStatus
                    .successful(JsonHelper.serializeMap(userResponse.asMap()));
            }
        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod);
        }

        this.metrics.commonClose(resultStatus.success);
        return resultStatus;
    }
}
