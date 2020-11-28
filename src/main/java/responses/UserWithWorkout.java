package responses;

import helpers.RequestFields;
import interfaces.Model;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import models.User;
import models.Workout;

@Data
public class UserWithWorkout implements Model {

    private User user;
    private Workout workout;
    private boolean workoutPresent;

    public UserWithWorkout(User user, Workout workout) {
        this.user = user;
        this.workoutPresent = workout != null;
        this.workout = workout;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> retVal = new HashMap<>();
        retVal.putIfAbsent(RequestFields.USER, this.user.asMap());
        if (this.workoutPresent) {
            retVal.putIfAbsent(RequestFields.WORKOUT, this.workout.asMap());
        } else {
            // in case the user has no workout, just return an empty map
            retVal.putIfAbsent(RequestFields.WORKOUT, new HashMap<>());
        }
        retVal.putIfAbsent(RequestFields.WORKOUT_PRESENT, this.workoutPresent);
        return retVal;
    }

    @Override
    public Map<String, Object> asResponse() {
        Map<String, Object> retVal = new HashMap<>();
        retVal.putIfAbsent(RequestFields.USER, this.user.asResponse());
        if (this.workoutPresent) {
            retVal.putIfAbsent(RequestFields.WORKOUT, this.workout.asResponse());
        } else {
            // in case the user has no workout, just return an empty map
            retVal.putIfAbsent(RequestFields.WORKOUT, new HashMap<>());
        }
        retVal.putIfAbsent(RequestFields.WORKOUT_PRESENT, this.workoutPresent);
        return retVal;
    }
}
