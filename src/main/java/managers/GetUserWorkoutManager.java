package managers;

import com.amazonaws.services.dynamodbv2.document.Item;
import daos.UserDAO;
import daos.WorkoutDAO;
import exceptions.UnauthorizedException;
import utils.Metrics;
import javax.inject.Inject;
import models.User;
import models.Workout;
import responses.UserWithWorkout;

public class GetUserWorkoutManager {

    private final UserDAO userDAO;
    private final WorkoutDAO workoutDAO;
    private final Metrics metrics;
    private final NewUserManager newUserManager;

    @Inject
    public GetUserWorkoutManager(final UserDAO userDAO, final WorkoutDAO workoutDAO,
        final Metrics metrics,
        final NewUserManager newUserManager) {
        this.workoutDAO = workoutDAO;
        this.userDAO = userDAO;
        this.metrics = metrics;
        this.newUserManager = newUserManager;
    }

    /**
     * This method is used when the user first successfully signs into the app. It provides the user object to the user
     * as well as the current workout if there is one.
     *
     * @param activeUser username of the user that made the api request, trying to get data about themselves.
     * @return User object and workout object that of the current workout (null if no workouts exist).
     */
    public UserWithWorkout getUserWithWorkout(final String activeUser) throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".getUserWithWorkout";
        this.metrics.commonSetup(classMethod);

        try {
            boolean userExists = this.userDAO.userExists(activeUser);
            UserWithWorkout userWithWorkout;

            if (userExists) {
                // user is indeed in DB, so fetch the current workout if it exists
                final User user = this.userDAO.getUser(activeUser);
                final String currentWorkoutId = user.getCurrentWorkout();

                if (currentWorkoutId == null) {
                    // user has no workouts
                    userWithWorkout = new UserWithWorkout(user, null);
                } else {
                    // user has a workout so try and fetch it from the DB
                    final Workout workout = this.workoutDAO.getWorkout(currentWorkoutId);
                    userWithWorkout = new UserWithWorkout(user, workout);
                }
            } else {
                // this will be reached if the user just created an account, or it somehow got deleted in DB, so put user in DB
                final User result = this.newUserManager.createNewUser(activeUser);
                userWithWorkout = new UserWithWorkout(result, null);
            }
            this.metrics.commonClose(true);
            return userWithWorkout;
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
