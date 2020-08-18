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
import java.util.Map;
import javax.inject.Inject;
import models.ExerciseUser;
import models.User;

public class UpdateExerciseManager {

    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;

    @Inject
    public UpdateExerciseManager(DatabaseAccess databaseAccess, Metrics metrics) {
        this.databaseAccess = databaseAccess;
        this.metrics = metrics;
    }

    /**
     * @param exerciseId TODO
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public ResultStatus<String> execute(final String activeUser, final String exerciseId, final
    Map<String, Object> exerciseUserMap) {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;

        try {
            User user = this.databaseAccess.getUser(activeUser);
            ExerciseUser exerciseUser = new ExerciseUser(exerciseUserMap);

            if (user != null) {
                List<String> exerciseNames = new ArrayList<>();
                for (String _exerciseId : user.getUserExercises().keySet()) {
                    exerciseNames.add(user.getUserExercises().get(_exerciseId).getExerciseName());
                }
                String exerciseError = Validator.validExerciseUser(exerciseUser, exerciseNames);
                if (exerciseError == null) {
                    // all input is valid so go ahead and just replace old exercise in db with updated one
                    final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                        .withUpdateExpression(
                            "set " +
                                User.EXERCISES + ".#exerciseId= :" + User.EXERCISES)
                        .withNameMap(new NameMap().with("#exerciseId", exerciseId))
                        .withValueMap(
                            new ValueMap()
                                .withMap(":" + User.EXERCISES, exerciseUser.asMap()));
                    this.databaseAccess.updateUser(user.getUsername(), updateItemSpec);

                    // make sure to update user that is returned to frontend
                    user.getUserExercises().put(exerciseId, exerciseUser);

                    resultStatus = ResultStatus
                        .successful(JsonHelper.serializeObject(user.asMap()));
                } else {
                    this.metrics.log("Input error on exercise" + exerciseError);
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
