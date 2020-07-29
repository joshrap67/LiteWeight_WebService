package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.Item;
import helpers.ErrorMessage;
import helpers.FileReader;
import helpers.JsonHelper;
import helpers.Metrics;
import helpers.ResultStatus;
import java.util.HashMap;
import javax.inject.Inject;
import models.User;
import responses.UserResponse;

public class NewUserManager {

  private final DatabaseAccess databaseAccess;
  private final Metrics metrics;

  @Inject
  public NewUserManager(final DatabaseAccess dbAccessManager, final Metrics metrics) {
    this.databaseAccess = dbAccessManager;
    this.metrics = metrics;
  }

  /**
   * @param username Username of new user to be inserted
   * @return Result status that will be sent to frontend with appropriate data or error messages.
   */
  public ResultStatus<String> execute(final String username) {
    final String classMethod = "GetUserDataManager.execute";
    this.metrics.commonSetup(classMethod);

    ResultStatus<String> resultStatus;

    try {
      UserResponse userResponse; // TODO not sure if going to return anything other than whole object
      Item user = new Item()
          .withString(User.USERNAME, username)
          .withNull(User.PREMIUM_TOKEN)
          .withNull(User.ICON)
          .withNull(User.CURRENT_WORKOUT)
          .withMap(User.WORKOUTS, new HashMap<>())
          .withNull(User.PUSH_ENDPOINT_ARN) // todo get this
          .withInt(User.WORKOUTS_SENT, 0)
          .withBoolean(User.PRIVATE_ACCOUNT, false)
          .withInt(User.NOTIFICATION_PREFERENCES, 0) // TODO use constant
          .withMap(User.FRIENDS, new HashMap<>())
          .withMap(User.FRIENDS_OF, new HashMap<>())
          .withMap(User.RECEIVED_WORKOUTS, new HashMap<>())
          .withMap(User.EXERCISES, FileReader.getDefaultExercises());

      this.databaseAccess.putUser(user);
      resultStatus = ResultStatus.successful(JsonHelper.convertObjectToJson(user.asMap()));
    } catch (Exception e) {
      this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
