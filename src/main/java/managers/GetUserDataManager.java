package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.Item;
import javax.inject.Inject;
import helpers.ErrorMessage;
import helpers.JsonHelper;
import helpers.Metrics;
import helpers.ResultStatus;
import responses.UserResponse;

public class GetUserDataManager {

    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;

    @Inject
    public GetUserDataManager(final DatabaseAccess dbAccessManager, final Metrics metrics) {
        this.databaseAccess = dbAccessManager;
        this.metrics = metrics;
    }

    /**
     * This method gets the active user's data. If the active user's data does not exist, we assume
     * this is their first login and we enter a new user object in the db.
     *
     * @param activeUser The user that made the api request, trying to get data about themselves.
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public ResultStatus<String> execute(final String activeUser) {
        final String classMethod = "GetUserDataManager.execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;

        try {
            UserResponse userResponse;
            Item user = this.databaseAccess.getUserItem(activeUser);

            if (user != null) {
                userResponse = new UserResponse(user);
                resultStatus = ResultStatus
                    .successful(JsonHelper.convertObjectToJson(userResponse.asMap()));
            } else {
                resultStatus = ResultStatus.failure("User does not exist.");
            }
        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failure("Exception in " + classMethod);
        }

        this.metrics.commonClose(resultStatus.success);
        return resultStatus;
    }
}
