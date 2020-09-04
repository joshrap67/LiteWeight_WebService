package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import helpers.ErrorMessage;
import helpers.JsonHelper;
import helpers.Metrics;
import helpers.ResultStatus;
import helpers.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import models.ExerciseUser;
import models.User;
import responses.ExerciseUserResponse;

public class NewExerciseManager {

    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;

    @Inject
    public NewExerciseManager(DatabaseAccess databaseAccess, Metrics metrics) {
        this.databaseAccess = databaseAccess;
        this.metrics = metrics;
    }

    /**
     * @param exerciseName TODO
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public ResultStatus<String> execute(final String activeUser, final String exerciseName,
        final List<String> focuses) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;

        try {
            User user = this.databaseAccess.getUser(activeUser);

            if (user != null) {
                List<String> focusList = new ArrayList<>(focuses);
                String errorMessage = Validator.validNewExercise(user, exerciseName);

                if (!focusList.isEmpty() && errorMessage == null) {
                    // all input is valid so go ahead and make the new exercise
                    ExerciseUser exerciseUser = new ExerciseUser(exerciseName,
                        ExerciseUser.defaultVideoValue, focusList, false);
                    String exerciseId = UUID.randomUUID().toString();

                    final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                        .withUpdateExpression(
                            "set " +
                                User.EXERCISES + ".#exerciseId= :" + User.EXERCISES)
                        .withNameMap(new NameMap().with("#exerciseId", exerciseId))
                        .withValueMap(
                            new ValueMap()
                                .withMap(":" + User.EXERCISES, exerciseUser.asMap()));
                    this.databaseAccess.updateUser(user.getUsername(), updateItemSpec);

                    resultStatus = ResultStatus
                        .successful(JsonHelper.serializeMap(
                            new ExerciseUserResponse(exerciseId, exerciseUser).asMap()));
                } else {
                    this.metrics.log("Input error on exercise" + errorMessage);
                    resultStatus = ResultStatus.failureBadEntity("Input error on exercise.");
                }
            } else {
                this.metrics.log("Active user does not exist");
                resultStatus = ResultStatus.failureBadEntity("User does not exist.");
            }
        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod + ". " + e);
        }

        this.metrics.commonClose(resultStatus.responseCode);
        return resultStatus;
    }

}
