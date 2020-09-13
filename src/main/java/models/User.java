package models;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

import com.amazonaws.services.dynamodbv2.document.Item;
import exceptions.InvalidAttributeException;
import helpers.Parser;
import interfaces.Model;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
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
    public static final String PUSH_ENDPOINT_ARN = "pushEndpointArn";
    public static final String FRIENDS = "friends";
    public static final String FRIEND_REQUESTS = "friendRequests";
    public static final String RECEIVED_WORKOUTS = "receivedWorkouts";
    public static final String USER_PREFERENCES = "preferences";
    public static final String BLOCKED = "blocked";

    private String username;
    private String icon;
    private String pushEndpointArn; // todo don't return to frontend
    private String premiumToken;
    private String currentWorkout;
    private Integer workoutsSent;
    private UserPreferences userPreferences;

    @Setter(AccessLevel.NONE)
    private Map<String, String> blocked;
    @Setter(AccessLevel.NONE)
    private Map<String, WorkoutUser> userWorkouts;
    @Setter(AccessLevel.NONE)
    private Map<String, ExerciseUser> userExercises;
    @Setter(AccessLevel.NONE)
    private Map<String, Friend> friends;
    @Setter(AccessLevel.NONE)
    private Map<String, FriendRequest> friendRequests;
    @Setter(AccessLevel.NONE)
    private Map<String, String> receivedWorkouts;


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
        this.setWorkoutsSent(Parser.convertObjectToInteger(json.get(WORKOUTS_SENT)));
        this.setUserPreferences(new UserPreferences(
            (Map<String, Object>) json.get(USER_PREFERENCES)));
        this.setUserWorkouts((Map<String, Object>) json.get(WORKOUTS));
        this.setUserExercises((Map<String, Object>) json.get(EXERCISES));
        this.setFriends((Map<String, Object>) json.get(FRIENDS));
        this.setFriendRequests((Map<String, Object>) json.get(FRIEND_REQUESTS));
        this.setReceivedWorkouts((Map<String, Object>) json.get(RECEIVED_WORKOUTS));
        this.setBlocked((Map<String, Object>) json.get(BLOCKED));
    }

    // Setters
    public void setUserExercises(Map<String, Object> json) {
        if (json == null) {
            this.userExercises = null;
        } else {
            this.userExercises = new HashMap<>();
            for (String exerciseId : json.keySet()) {
                this.userExercises
                    .putIfAbsent(exerciseId,
                        new ExerciseUser((Map<String, Object>) json.get(exerciseId)));
            }
        }
    }

    private void setBlocked(Map<String, Object> json) {
        if (json == null) {
            this.blocked = null;
        } else {
            this.blocked = new HashMap<>();
            for (String username : json.keySet()) {
                this.blocked.put(username, (String) json.get(ICON));
            }
        }
    }

    public void setFriends(Map<String, Object> json) {
        if (json == null) {
            this.friends = null;
        } else {
            this.friends = new HashMap<>();
            for (String username : json.keySet()) {
                this.friends
                    .putIfAbsent(username, new Friend((Map<String, Object>) json.get(username)));
            }
        }
    }

    public void setFriendRequests(Map<String, Object> json) {
        if (json == null) {
            this.friendRequests = null;
        } else {
            this.friendRequests = new HashMap<>();
            for (String username : json.keySet()) {
                this.friendRequests.putIfAbsent(username, new FriendRequest(
                    (Map<String, Object>) json.get(username)));
            }
        }
    }

    public void setReceivedWorkouts(Map<String, Object> json) {
        if (json == null) {
            this.receivedWorkouts = null;
        } else {
            this.receivedWorkouts = new HashMap<>();
            for (String workoutId : json.keySet()) {
                this.receivedWorkouts.putIfAbsent(workoutId, (String) json.get(workoutId));
            }
        }
    }

    public void setUserWorkouts(Map<String, Object> json) {
        if (json == null) {
            this.userWorkouts = null;
        } else {
            this.userWorkouts = new HashMap<>();
            for (String workoutId : json.keySet()) {
                this.userWorkouts.putIfAbsent(workoutId, new WorkoutUser(
                    (Map<String, Object>) json.get(workoutId)));
            }
        }
    }

    public Map<String, Map<String, Object>> getFriendRequestsMap() {
        if (this.friendRequests == null) {
            return null;
        }

        Map<String, Map<String, Object>> retVal = new HashMap<>();
        for (String username : this.friendRequests.keySet()) {
            retVal.putIfAbsent(username, this.friendRequests.get(username).asMap());
        }
        return retVal;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> retVal = new HashMap<>();
        retVal.putIfAbsent(USERNAME, this.username);
        retVal.putIfAbsent(ICON, this.icon);
        retVal.putIfAbsent(PUSH_ENDPOINT_ARN, this.pushEndpointArn);
        retVal.putIfAbsent(PREMIUM_TOKEN, this.premiumToken);
        retVal.putIfAbsent(CURRENT_WORKOUT, this.currentWorkout);
        retVal.putIfAbsent(WORKOUTS_SENT, this.workoutsSent);
        retVal.putIfAbsent(BLOCKED, this.blocked);
        retVal.putIfAbsent(WORKOUTS, this.getUserWorkoutsMap());
        retVal.putIfAbsent(EXERCISES, this.getUserExercisesMap());
        retVal.putIfAbsent(FRIENDS, this.getFriendsMap());
        retVal.putIfAbsent(USER_PREFERENCES, this.userPreferences.asMap());
        retVal.putIfAbsent(FRIEND_REQUESTS, this.getFriendRequestsMap());
        retVal.putIfAbsent(RECEIVED_WORKOUTS, this.receivedWorkouts);
        return retVal;
    }

    public Map<String, Map<String, Object>> getUserWorkoutsMap() {
        if (this.userWorkouts == null) {
            return null;
        }

        return this.userWorkouts.entrySet().stream().collect(
            collectingAndThen(
                toMap(Entry::getKey, (Map.Entry<String, WorkoutUser> e) -> e.getValue().asMap()),
                HashMap::new));
    }

    public Map<String, Map<String, Object>> getUserExercisesMap() {
        if (this.userExercises == null) {
            return null;
        }

        return this.userExercises.entrySet().stream().collect(
            collectingAndThen(
                toMap(Entry::getKey, (Map.Entry<String, ExerciseUser> e) -> e.getValue().asMap()),
                HashMap::new));
    }

    public Map<String, Map<String, Object>> getFriendsMap() {
        if (this.friends == null) {
            return null;
        }

        return this.friends.entrySet().stream().collect(
            collectingAndThen(
                toMap(Entry::getKey, (Map.Entry<String, Friend> e) -> e.getValue().asMap()),
                HashMap::new));
    }

    public void setUserWorkouts(String workoutId, WorkoutUser workoutUser) {
        this.userWorkouts.putIfAbsent(workoutId, workoutUser);
    }
}
