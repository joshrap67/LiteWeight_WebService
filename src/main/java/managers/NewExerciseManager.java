package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import exceptions.InvalidAttributeException;
import exceptions.ManagerExecutionException;
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
    public ExerciseUserResponse execute(final String activeUser, final String exerciseName,
        final List<String> focuses) throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        try {
            final User user = this.databaseAccess.getUser(activeUser);

            List<String> focusList = new ArrayList<>(focuses);
            final String errorMessage = Validator.validNewExercise(user, exerciseName, focusList);

            if (!errorMessage.isEmpty()) {
                this.metrics.commonClose(false);
                throw new ManagerExecutionException(errorMessage);
            }

            // all input is valid so go ahead and make the new exercise
            ExerciseUser exerciseUser = new ExerciseUser(exerciseName,
                ExerciseUser.defaultVideoValue, focusList, false);
            String exerciseId = UUID.randomUUID().toString();

            final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withUpdateExpression("set " + User.EXERCISES + ".#exerciseId= :exerciseMap")
                .withNameMap(new NameMap().with("#exerciseId", exerciseId))
                .withValueMap(new ValueMap().withMap(":exerciseMap", exerciseUser.asMap()));
            this.databaseAccess.updateUser(user.getUsername(), updateItemSpec);

            this.metrics.commonClose(true);
            return new ExerciseUserResponse(exerciseId, exerciseUser);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }

}
