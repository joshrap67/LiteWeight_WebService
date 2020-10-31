package responses;

import helpers.RequestFields;
import interfaces.Model;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import models.OwnedExercise;
import models.Workout;
import models.WorkoutMeta;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcceptWorkoutResponse implements Model {

    private String workoutId;
    private WorkoutMeta workoutMeta;
    private Workout workout;
    private Map<String, OwnedExercise> exercises;

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> retVal = new HashMap<>();
        retVal.putIfAbsent(Workout.WORKOUT_ID, this.workoutId);
        retVal.putIfAbsent(RequestFields.WORKOUT_META, this.workoutMeta.asResponse());
        retVal.putIfAbsent(RequestFields.WORKOUT, this.workout.asResponse());
        Map<String, Object> exerciseMap = new HashMap<>();
        for (String exerciseId : exercises.keySet()) {
            exerciseMap.putIfAbsent(exerciseId, exercises.get(exerciseId).asResponse());
        }
        retVal.putIfAbsent(RequestFields.EXERCISES, exerciseMap);
        return retVal;
    }

    @Override
    public Map<String, Object> asResponse() {
        return this.asMap();
    }
}
