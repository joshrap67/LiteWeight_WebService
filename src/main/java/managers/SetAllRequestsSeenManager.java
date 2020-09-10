package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import helpers.ErrorMessage;
import helpers.Metrics;
import helpers.ResultStatus;
import javax.inject.Inject;
import models.User;

public class SetAllRequestsSeenManager {

    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;

    @Inject
    public SetAllRequestsSeenManager(final DatabaseAccess databaseAccess, final Metrics metrics) {
        this.databaseAccess = databaseAccess;
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
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;

        try {
            User user = this.databaseAccess.getUser(activeUser);
            if (user != null) {
                // user that the active user is attempting to cancel is indeed a real user
                for (String username : user.getFriendRequests().keySet()) {
                    user.getFriendRequests().get(username).setSeen(true);
                }

                final UpdateItemSpec updateActiveUserData = new UpdateItemSpec()
                    .withUpdateExpression(
                        "set " + User.FRIEND_REQUESTS + "=:" + User.FRIEND_REQUESTS)
                    .withValueMap(new ValueMap()
                        .withMap(":" + User.FRIEND_REQUESTS, user.getFriendRequestsMap()));

                this.databaseAccess.updateUser(activeUser, updateActiveUserData);
                resultStatus = ResultStatus.successful("All requests set to seen.");
            } else {
                this.metrics.log(String.format("User %s does not exist", activeUser));
                resultStatus = ResultStatus
                    .failureBadEntity(String.format("Unable to add %s", activeUser));
            }
        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod);
        }

        this.metrics.commonClose(resultStatus.responseCode);
        return resultStatus;
    }
}
