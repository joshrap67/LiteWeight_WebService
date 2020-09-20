package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.Item;
import helpers.Metrics;
import javax.inject.Inject;
import models.User;
import models.Workout;
import responses.UserWithWorkout;

public class GetUserWorkoutManager {

    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;
    private final NewUserManager newUserManager;

    @Inject
    public GetUserWorkoutManager(final DatabaseAccess databaseAccess, final Metrics metrics,
        final NewUserManager newUserManager) {
        this.databaseAccess = databaseAccess;
        this.metrics = metrics;
        this.newUserManager = newUserManager;
    }

    /**
     * This method is used when the user first successfully signs into the app. It provides the user
     * object to the user as well as the current workout if there is one.
     *
     * @param activeUser The user that made the api request, trying to get data about themselves.
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public UserWithWorkout execute(final String activeUser) throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".execute";
        this.metrics.commonSetup(classMethod);

        try {
            Item userItem = this.databaseAccess.getUserItem(activeUser);
            UserWithWorkout userWithWorkout;

            if (userItem != null) {
                // user is indeed in DB, so fetch the current workout if it exists
                final User user = new User(userItem);
                final String currentWorkoutId = user.getCurrentWorkout();

                if (currentWorkoutId == null) {
                    // user has no workouts
                    userWithWorkout = new UserWithWorkout(user, null);
                } else {
                    // user has a workout so try and fetch it from the DB
                    final Workout workout = new Workout(
                        this.databaseAccess.getWorkoutItem(currentWorkoutId));
                    userWithWorkout = new UserWithWorkout(user, workout);
                }
            } else {
                // this will be reached if the user just created an account or it somehow got deleted in DB, so put user in DB
                final User result = this.newUserManager.execute(activeUser);
                userWithWorkout = new UserWithWorkout(result, null);
            }
            return userWithWorkout;
        }
        catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
