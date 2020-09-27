package managers;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import daos.UserDAO;
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

    private final UserDAO userDAO;
    private final Metrics metrics;

    @Inject
    public UpdateExerciseManager(final UserDAO userDAO, final Metrics metrics) {
        this.userDAO = userDAO;
        this.metrics = metrics;
    }

    /**
     * Updates an owned exercise of the user if the input is valid. Note that at this time it just
     * overwrites the previous exercise values.
     *
     * @param activeUser   username of the user that is updating the exercise.
     * @param exerciseId   id of the exercise that is being updated.
     * @param exerciseUser the exercise that is to be updated.
     * @return User the user object with this exercise now updated.
     * @throws UserNotFoundException     if the active user is not found.
     * @throws InvalidAttributeException if the user item is invalid.
     * @throws ManagerExecutionException if there is any input errors.
     */
    public User updateExercise(final String activeUser, final String exerciseId,
        final ExerciseUser exerciseUser)
        throws UserNotFoundException, InvalidAttributeException, ManagerExecutionException {
        final String classMethod = this.getClass().getSimpleName() + ".updateExercise";
        this.metrics.commonSetup(classMethod);

        try {
            final User user = this.userDAO.getUser(activeUser);

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
            this.userDAO.updateUser(user.getUsername(), updateItemSpec);

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
