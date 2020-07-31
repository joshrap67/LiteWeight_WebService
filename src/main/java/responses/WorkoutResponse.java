package responses;

import com.amazonaws.services.dynamodbv2.document.Item;
import exceptions.InvalidAttributeException;
import interfaces.Model;
import java.util.Map;
import models.Workout;

public class WorkoutResponse extends Workout implements Model {

    public WorkoutResponse(Item workoutItem) throws InvalidAttributeException {
        super(workoutItem);
    }

    @Override
    public Map<String, Object> asMap() {
        return super.asMap();
    }
}
