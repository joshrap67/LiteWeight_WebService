package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import exceptions.UserNotFoundException;
import helpers.ErrorMessage;
import helpers.JsonHelper;
import helpers.Metrics;
import helpers.ResultStatus;
import helpers.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import models.ExerciseUser;
import models.User;
import responses.ExerciseUserResponse;

public class NewExerciseManager {

    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;

    @Inject
    public NewExerciseManager(final DatabaseAccess databaseAccess, final Metrics metrics) {
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
            final User user = Optional.ofNullable(this.databaseAccess.getUser(activeUser))
                .orElseThrow(
                    () -> new UserNotFoundException(String.format("%s not found", activeUser)));

            List<String> focusList = new ArrayList<>(focuses);
            final String errorMessage = Validator.validNewExercise(user, exerciseName, focusList);

            if (errorMessage.isEmpty()) {
                // all input is valid so go ahead and make the new exercise
                ExerciseUser exerciseUser = new ExerciseUser(exerciseName,
                    ExerciseUser.defaultVideoValue, focusList, false);
                String exerciseId = UUID.randomUUID().toString();

                final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                    .withUpdateExpression("set " + User.EXERCISES + ".#exerciseId= :exerciseMap")
                    .withNameMap(new NameMap().with("#exerciseId", exerciseId))
                    .withValueMap(new ValueMap().withMap(":exerciseMap", exerciseUser.asMap()));
                this.databaseAccess.updateUser(user.getUsername(), updateItemSpec);

                resultStatus = ResultStatus
                    .successful(JsonHelper.serializeMap(
                        new ExerciseUserResponse(exerciseId, exerciseUser).asMap()));
            } else {
                this.metrics.log("Input error on creating new exercise:" + errorMessage);
                resultStatus = ResultStatus
                    .failureBadEntity("Input error on creating new exercise.");
            }
        } catch (UserNotFoundException unfe) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, unfe));
            resultStatus = ResultStatus.failureBadEntity(unfe.getMessage());
        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failureBadEntity("Exception in " + classMethod + ". " + e);
        }

        this.metrics.commonClose(resultStatus.success);
        return resultStatus;
    }

}
