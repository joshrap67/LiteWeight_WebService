package responses;

import helpers.RequestFields;
import interfaces.Model;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import models.OwnedExercise;

@Data
public class ExerciseUserResponse implements Model {

    private String exerciseId;
    private OwnedExercise ownedExercise;

    public ExerciseUserResponse(String exerciseId, OwnedExercise ownedExercise) {
        this.exerciseId = exerciseId;
        this.ownedExercise = ownedExercise;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> retVal = new HashMap<>();
        retVal.putIfAbsent(RequestFields.EXERCISE_ID, exerciseId);
        retVal.putIfAbsent(RequestFields.EXERCISE, ownedExercise);
        return retVal;
    }

    @Override
    public Map<String, Object> asResponse() {
        return this.asMap();
    }


}
