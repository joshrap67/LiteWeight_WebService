package models;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

import com.amazonaws.services.dynamodbv2.document.Item;
import exceptions.InvalidAttributeException;
import utils.Parser;
import interfaces.Model;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import managers.GetReceivedWorkoutsManager;

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
    public static final String NEW_WORKOUTS = "newWorkouts";
    // not database keys, but keys for responses to front end
    public static final String UNSEEN_RECEIVED_WORKOUTS = "unseenReceivedWorkouts";
    public static final String TOTAL_RECEIVED_WORKOUTS = "totalReceivedWorkouts";

    private String username;
    private String icon;
    private String pushEndpointArn;
    private String premiumToken;
    private String currentWorkout;
    private Integer workoutsSent;
    private Integer newWorkouts;
    private UserPreferences userPreferences;

    @Setter(AccessLevel.NONE)
    private Map<String, String> blocked;
    @Setter(AccessLevel.NONE)
    private Map<String, WorkoutMeta> workoutMetas;
    @Setter(AccessLevel.NONE)
    private Map<String, OwnedExercise> ownedExercises;
    @Setter(AccessLevel.NONE)
    private Map<String, Friend> friends;
    @Setter(AccessLevel.NONE)
    private Map<String, FriendRequest> friendRequests;
    @Setter(AccessLevel.NONE)
    private Map<String, SharedWorkoutMeta> receivedWorkouts;


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
        this.setUserPreferences(new UserPreferences((Map<String, Object>) json.get(USER_PREFERENCES)));
        this.setWorkoutMetas((Map<String, Object>) json.get(WORKOUTS));
        this.setOwnedExercises((Map<String, Object>) json.get(EXERCISES));
        this.setFriends((Map<String, Object>) json.get(FRIENDS));
        this.setFriendRequests((Map<String, Object>) json.get(FRIEND_REQUESTS));
        this.setReceivedWorkouts((Map<String, Object>) json.get(RECEIVED_WORKOUTS));
        this.setBlocked((Map<String, Object>) json.get(BLOCKED));
    }

    // Setters
    public void setOwnedExercises(Map<String, Object> json) {
        if (json == null) {
            this.ownedExercises = null;
        } else {
            this.ownedExercises = new HashMap<>();
            for (String exerciseId : json.keySet()) {
                this.ownedExercises.putIfAbsent(exerciseId,
                    new OwnedExercise((Map<String, Object>) json.get(exerciseId)));
            }
        }
    }

    private void setBlocked(Map<String, Object> json) {
        if (json == null) {
            this.blocked = null;
        } else {
            this.blocked = new HashMap<>();
            for (String username : json.keySet()) {
                this.blocked.put(username, (String) json.get(username));
            }
        }
    }

    public void setFriends(Map<String, Object> json) {
        if (json == null) {
            this.friends = null;
        } else {
            this.friends = new HashMap<>();
            for (String username : json.keySet()) {
                this.friends.putIfAbsent(username, new Friend((Map<String, Object>) json.get(username)));
            }
        }
    }

    public void setFriendRequests(Map<String, Object> json) {
        if (json == null) {
            this.friendRequests = null;
        } else {
            this.friendRequests = new HashMap<>();
            for (String username : json.keySet()) {
                this.friendRequests.putIfAbsent(username, new FriendRequest((Map<String, Object>) json.get(username)));
            }
        }
    }

    public void putNewWorkoutMeta(String workoutId, WorkoutMeta workoutMeta) {
        this.workoutMetas.putIfAbsent(workoutId, workoutMeta);
    }

    public void setReceivedWorkouts(Map<String, Object> json) {
        if (json == null) {
            this.receivedWorkouts = null;
        } else {
            this.receivedWorkouts = new HashMap<>();
            for (String workoutId : json.keySet()) {
                this.receivedWorkouts.putIfAbsent(workoutId,
                    new SharedWorkoutMeta((Map<String, Object>) json.get(workoutId), workoutId));
            }
        }
    }

    public void setWorkoutMetas(Map<String, Object> json) {
        if (json == null) {
            this.workoutMetas = null;
        } else {
            this.workoutMetas = new HashMap<>();
            for (String workoutId : json.keySet()) {
                this.workoutMetas.putIfAbsent(workoutId, new WorkoutMeta((Map<String, Object>) json.get(workoutId)));
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
        retVal.putIfAbsent(WORKOUTS, this.getWorkoutMetasMap());
        retVal.putIfAbsent(EXERCISES, this.getOwnedExercisesMap());
        retVal.putIfAbsent(FRIENDS, this.getFriendsMap());
        retVal.putIfAbsent(RECEIVED_WORKOUTS, this.getReceivedWorkoutMetaMap());
        retVal.putIfAbsent(USER_PREFERENCES, this.userPreferences.asMap());
        retVal.putIfAbsent(FRIEND_REQUESTS, this.getFriendRequestsMap());
        return retVal;
    }

    @Override
    public Map<String, Object> asResponse() {
        Map<String, Object> map = this.asMap();
        map.remove(PUSH_ENDPOINT_ARN);
        map.remove(RECEIVED_WORKOUTS);
        map.putIfAbsent(RECEIVED_WORKOUTS, getReceivedWorkoutsResponse()); // is a batch
        map.putIfAbsent(UNSEEN_RECEIVED_WORKOUTS, getUnseenWorkoutsCount());
        map.putIfAbsent(TOTAL_RECEIVED_WORKOUTS, this.receivedWorkouts.size());
        return map;
    }

    private Map<String, Object> getReceivedWorkoutsResponse() {
        // give the user their first batch of received workouts. Any other ones will have to be added via API call
        Map<String, SharedWorkoutMeta> firstBatch = GetReceivedWorkoutsManager.getBatchOfWorkouts(
            this.getReceivedWorkouts(), 0);
        Map<String, Object> retMap = new HashMap<>();
        for (String workoutId : firstBatch.keySet()) {
            retMap.putIfAbsent(workoutId, receivedWorkouts.get(workoutId).asResponse());
        }
        return retMap;
    }

    private int getUnseenWorkoutsCount() {
        int retVal = 0;
        for (String workoutId : this.receivedWorkouts.keySet()) {
            if (!receivedWorkouts.get(workoutId).isSeen()) {
                retVal++;
            }
        }
        return retVal;
    }

    public Map<String, Map<String, Object>> getWorkoutMetasMap() {
        if (this.workoutMetas == null) {
            return null;
        }

        return this.workoutMetas.entrySet().stream().collect(
            collectingAndThen(
                toMap(Entry::getKey, (Map.Entry<String, WorkoutMeta> e) -> e.getValue().asMap()),
                HashMap::new));
    }

    public Map<String, Map<String, Object>> getReceivedWorkoutMetaMap() {
        if (this.receivedWorkouts == null) {
            return null;
        }

        return this.receivedWorkouts.entrySet().stream().collect(
            collectingAndThen(
                toMap(Entry::getKey, (Map.Entry<String, SharedWorkoutMeta> e) -> e.getValue().asMap()),
                HashMap::new));
    }

    public Map<String, Map<String, Object>> getOwnedExercisesMap() {
        if (this.ownedExercises == null) {
            return null;
        }

        return this.ownedExercises.entrySet().stream().collect(
            collectingAndThen(
                toMap(Entry::getKey, (Map.Entry<String, OwnedExercise> e) -> e.getValue().asMap()),
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
}
