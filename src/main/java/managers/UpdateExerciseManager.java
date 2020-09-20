package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.fasterxml.jackson.core.JsonProcessingException;
import exceptions.InvalidAttributeException;
import exceptions.ManagerExecutionException;
import exceptions.UserNotFoundException;
import helpers.Metrics;
import helpers.Validator;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import models.ExerciseUser;
import models.User;

public class UpdateExerciseManager {

    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;

    @Inject
    public UpdateExerciseManager(final DatabaseAccess databaseAccess, final Metrics metrics) {
        this.databaseAccess = databaseAccess;
        this.metrics = metrics;
    }

    /**
     * @param exerciseId TODO
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public User execute(final String activeUser, final String exerciseId,
        final ExerciseUser exerciseUser)
        throws UserNotFoundException, InvalidAttributeException, JsonProcessingException, ManagerExecutionException {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        try {
            final User user = this.databaseAccess.getUser(activeUser);

            List<String> exerciseNames = new ArrayList<>();
            for (String _exerciseId : user.getUserExercises().keySet()) {
                exerciseNames.add(user.getUserExercises().get(_exerciseId).getExerciseName());
            }
            final String oldExerciseName = user.getUserExercises().get(exerciseId)
                .getExerciseName();
            final String exerciseError = Validator
                .validExerciseUser(exerciseUser, exerciseNames, oldExerciseName);
            if (!exerciseError.isEmpty()) {
                this.metrics.commonClose(false);
                throw new ManagerExecutionException(exerciseError);
            }

            // all input is valid so go ahead and just replace old exercise in db with updated one
            final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withUpdateExpression("set " + User.EXERCISES + ".#exerciseId= :exerciseMap")
                .withValueMap(new ValueMap().withMap(":exerciseMap", exerciseUser.asMap()))
                .withNameMap(new NameMap().with("#exerciseId", exerciseId));
            this.databaseAccess.updateUser(user.getUsername(), updateItemSpec);

            // make sure to update user that is returned to frontend
            user.getUserExercises().put(exerciseId, exerciseUser);
            this.metrics.commonClose(true);
            return user;
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
