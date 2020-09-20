package responses;

import com.amazonaws.services.dynamodbv2.document.Item;
import exceptions.InvalidAttributeException;
import interfaces.Model;
import java.util.Map;
import models.User;

public class UserResponse extends User implements Model {

    public UserResponse(Item user) throws InvalidAttributeException {
        super(user);
    }

    public UserResponse(Map<String, Object> json) throws InvalidAttributeException {
        super(json);
    }

    @Override
    public Map<String, Object> asMap() {
        // todo remove push arn, received workouts map
        return super.asMap();
    }
}
