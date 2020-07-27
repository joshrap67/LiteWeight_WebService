package responses;

import com.amazonaws.services.dynamodbv2.document.Item;
import exceptions.InvalidAttributeException;
import models.User;

public class UserResponse extends User {

  public UserResponse(Item user) throws InvalidAttributeException {
    super(user);
  }
}
