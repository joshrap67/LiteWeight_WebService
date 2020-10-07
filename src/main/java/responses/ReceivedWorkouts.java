package responses;

import helpers.RequestFields;
import interfaces.Model;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import models.ReceivedWorkoutMeta;

@Data
public class ReceivedWorkouts implements Model {

    private Map<String, ReceivedWorkoutMeta> receivedWorkouts;

    public ReceivedWorkouts(Map<String, ReceivedWorkoutMeta> receivedWorkouts) {
        this.receivedWorkouts = receivedWorkouts;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> retVal = new HashMap<>();
        for (String workoutId : this.receivedWorkouts.keySet()) {
            retVal.putIfAbsent(workoutId, this.receivedWorkouts.get(workoutId).asMap());
        }
        return retVal;
    }

    @Override
    public Map<String, Object> asResponse() {
        Map<String, Object> retVal = new HashMap<>();
        retVal.putIfAbsent(RequestFields.RECEIVED_WORKOUTS, this.asMap());
        return retVal;
    }
}
