package responses;

import helpers.RequestFields;
import interfaces.Model;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import models.ExerciseUser;

@Data
public class ExerciseUserResponse implements Model {

    private String exerciseId;
    private ExerciseUser exerciseUser;

    public ExerciseUserResponse(String exerciseId, ExerciseUser exerciseUser) {
        this.exerciseId = exerciseId;
        this.exerciseUser = exerciseUser;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> retVal = new HashMap<>();
        retVal.putIfAbsent(RequestFields.EXERCISE_ID, exerciseId);
        retVal.putIfAbsent(RequestFields.EXERCISE, exerciseUser);
        return retVal;
    }

    @Override
    public Map<String, Object> asResponse() {
        return this.asMap();
    }


}
