package models;

import com.amazonaws.services.dynamodbv2.document.Item;
import exceptions.InvalidAttributeException;
import interfaces.Model;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public class User implements Model {

  // Database keys
  public static final String USERNAME = "username";
  public static final String PREMIUM_TOKEN = "premiumToken";
  public static final String ICON = "icon";
  public static final String CURRENT_WORKOUT = "currentWorkout";
  public static final String WORKOUTS = "workouts";
  public static final String WORKOUTS_SENT = "workoutsSent";
  public static final String EXERCISES = "exercises";
  public static final String PRIVATE_ACCOUNT = "private_account";
  public static final String NOTIFICATION_PREFERENCES = "notificationPreferences";
  public static final String PUSH_ENDPOINT_ARN = "pushEndpointArn";

  private String username;
  private String icon;
  private String pushEndpointArn;
  private String premiumToken;
  private String currentWorkout;
  private int workoutsSent;
  private boolean privateAccount;
  private int notificationPreferences;

  @Setter(AccessLevel.NONE)
  private Map<String, WorkoutUser> userWorkouts;
  @Setter(AccessLevel.NONE)
  private Map<String, ExerciseUser> userExercises;


  public User(final Item userItem)
      throws InvalidAttributeException {
    this(userItem.asMap());
  }

  public User(Map<String, Object> json) throws InvalidAttributeException {
    this.setUsername((String) json.get(USERNAME));
    this.setIcon((String) json.get(ICON));
    this.setPushEndpointArn((String) json.get(PUSH_ENDPOINT_ARN));
    this.setPremiumToken((String) json.get(PREMIUM_TOKEN));
    this.setCurrentWorkout((String) json.get(CURRENT_WORKOUT));
    this.setWorkoutsSent((Integer) json.get(WORKOUTS_SENT));
    this.setPrivateAccount((Boolean) json.get(PRIVATE_ACCOUNT));
    this.setNotificationPreferences((Integer) json.get(NOTIFICATION_PREFERENCES));
    this.setUserWorkouts((Map<String, WorkoutUser>) json.get(WORKOUTS));
    this.setUserExercises((Map<String, ExerciseUser>) json.get(EXERCISES));
  }


  public void setUserExercises(Map<String, ExerciseUser> userExercises) {
    this.userExercises = userExercises;
  }

  public void setUserWorkouts(Map<String, WorkoutUser> json) {
    if (json == null) {
      this.userWorkouts = null;
    } else {
      this.userWorkouts = new HashMap<>();
      for (String id : json.keySet()) {
        this.userWorkouts.putIfAbsent(id, new WorkoutUser());
      }
    }
  }

  @Override
  public Map<String, Object> asMap() {
    return null;
  }
}
