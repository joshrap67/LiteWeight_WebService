package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import helpers.ErrorMessage;
import helpers.Metrics;
import helpers.ResultStatus;
import javax.inject.Inject;
import models.User;

public class UnblockUserManager {

    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;

    @Inject
    public UnblockUserManager(final DatabaseAccess databaseAccess, final Metrics metrics) {
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
    public ResultStatus<String> execute(final String activeUser, final String usernameToUnblock) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;

        try {
            User activeUserObject = this.databaseAccess.getUser(activeUser);
            if (activeUserObject != null && activeUserObject.getBlocked()
                .containsKey(usernameToUnblock)) {
                // unblock the user
                UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                    .withUpdateExpression(
                        "remove " + User.BLOCKED + ".#username")
                    .withNameMap(new NameMap().with("#username", usernameToUnblock));
                this.databaseAccess.updateUser(activeUser, updateItemSpec);
                resultStatus = ResultStatus
                    .successful("User successfully unblocked.");
            } else {
                this.metrics
                    .log(String.format("Cannot unblock user %s", usernameToUnblock));
                resultStatus = ResultStatus
                    .failureBadEntity(
                        String.format("Unable to unblock %s", usernameToUnblock));
            }

        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod);
        }

        this.metrics.commonClose(resultStatus.responseCode);
        return resultStatus;
    }
}
