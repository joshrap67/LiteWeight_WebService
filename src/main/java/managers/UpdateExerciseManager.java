package managers;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import daos.UserDAO;
import exceptions.InvalidAttributeException;
import exceptions.ManagerExecutionException;
import exceptions.UserNotFoundException;
import utils.Metrics;
import utils.Validator;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import models.OwnedExercise;
import models.User;

public class UpdateExerciseManager {

    private final UserDAO userDAO;
    private final Metrics metrics;

    @Inject
    public UpdateExerciseManager(final UserDAO userDAO, final Metrics metrics) {
        this.userDAO = userDAO;
        this.metrics = metrics;
    }

    /**
     * Updates an owned exercise of the user if the input is valid. Note that at this time it just overwrites the
     * previous exercise values.
     *
     * @param activeUser      username of the user that is updating the exercise.
     * @param exerciseId      id of the exercise that is being updated.
     * @param updatedExercise the exercise that is to be updated.
     * @return User the user object with this exercise now updated.
     * @throws UserNotFoundException     if the active user is not found.
     * @throws InvalidAttributeException if the user item is invalid.
     * @throws ManagerExecutionException if there is any input errors.
     */
    public User updateExercise(final String activeUser, final String exerciseId, final OwnedExercise updatedExercise)
        throws UserNotFoundException, InvalidAttributeException, ManagerExecutionException {
        final String classMethod = this.getClass().getSimpleName() + ".updateExercise";
        this.metrics.commonSetup(classMethod);

        try {
            final User user = this.userDAO.getUser(activeUser);

            List<String> exerciseNames = new ArrayList<>();
            for (String _exerciseId : user.getOwnedExercises().keySet()) {
                exerciseNames.add(user.getOwnedExercises().get(_exerciseId).getExerciseName());
            }
            final String oldExerciseName = user.getOwnedExercises().get(exerciseId).getExerciseName();
            final String exerciseError = Validator.validOwnedExercise(updatedExercise, exerciseNames, oldExerciseName);
            if (!exerciseError.isEmpty()) {
                this.metrics.commonClose(false);
                throw new ManagerExecutionException(exerciseError);
            }

            // all input is valid so go ahead and just replace old exercise in db with updated one
            UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withUpdateExpression("set " + User.EXERCISES + ".#exerciseId= :exerciseMap")
                .withValueMap(new ValueMap().withMap(":exerciseMap", updatedExercise.asMap()))
                .withNameMap(new NameMap().with("#exerciseId", exerciseId));
            this.userDAO.updateUser(user.getUsername(), updateItemSpec);

            // make sure to update user that is returned to frontend
            user.getOwnedExercises().put(exerciseId, updatedExercise);
            this.metrics.commonClose(true);
            return user;
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
