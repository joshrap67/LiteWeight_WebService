package models;

import utils.Parser;
import interfaces.Model;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public class OwnedExercise implements Model {

    public static final String EXERCISE_NAME = "exerciseName";
    public static final String FOCUSES = "focuses";
    public static final String DEFAULT_WEIGHT = "defaultWeight";
    public static final String DEFAULT_SETS = "defaultSets";
    public static final String DEFAULT_REPS = "defaultReps";
    public static final String DEFAULT_DETAILS = "defaultDetails";
    public static final String VIDEO_URL = "videoUrl";

    public static final int defaultSetsValue = 3;
    public static final int defaultRepsValue = 15;
    public static final double defaultWeightValue = 0.0;
    public static final String defaultDetailsValue = "";
    public static final String defaultVideoValue = "";

    private String exerciseName;
    private Double defaultWeight; // stored in lbs
    private Integer defaultSets;
    private Integer defaultReps;
    private String defaultDetails;
    private String videoUrl;
    private List<String> focuses;
    @Setter(AccessLevel.NONE)
    private Map<String, String> workouts; // id to workout name that this exercise is apart of


    public OwnedExercise(Map<String, Object> json) {
        this.exerciseName = (String) json.get(EXERCISE_NAME);
        this.defaultWeight = Parser.convertObjectToDouble(json.get(DEFAULT_WEIGHT));
        this.defaultSets = Parser.convertObjectToInteger(json.get(DEFAULT_SETS));
        this.defaultReps = Parser.convertObjectToInteger(json.get(DEFAULT_REPS));
        this.defaultDetails = (String) json.get(DEFAULT_DETAILS);
        this.videoUrl = (String) json.get(VIDEO_URL);
        this.setWorkouts((Map<String, Object>) json.get(User.WORKOUTS));
        this.focuses = (List<String>) json.get(FOCUSES);
    }

    public OwnedExercise(String exerciseName, String videUrl, List<String> focuses) {
        this.exerciseName = exerciseName;
        this.videoUrl = videUrl;
        this.focuses = focuses;
        this.defaultWeight = defaultWeightValue;
        this.defaultReps = defaultRepsValue;
        this.defaultSets = defaultSetsValue;
        this.defaultDetails = defaultDetailsValue;
        this.workouts = new HashMap<>();
    }

    public OwnedExercise(String exerciseName, double weight, int sets, int reps, String details,
        String videUrl, List<String> focuses) {
        this.exerciseName = exerciseName;
        this.videoUrl = videUrl;
        this.focuses = focuses;
        this.defaultWeight = weight;
        this.defaultReps = reps;
        this.defaultSets = sets;
        this.defaultDetails = details;
        this.workouts = new HashMap<>();
    }

    public OwnedExercise(final SharedWorkoutExercise sharedWorkoutExercise, final String name) {
        // used to convert to an owned exercise from a sharedWorkoutExercise
        this.exerciseName = name;
        this.focuses = sharedWorkoutExercise.getFocuses();
        this.videoUrl = sharedWorkoutExercise.getVideoUrl();
        this.defaultWeight = defaultWeightValue;
        this.defaultReps = defaultRepsValue;
        this.defaultSets = defaultSetsValue;
        this.defaultDetails = defaultDetailsValue;
        this.workouts = new HashMap<>();
    }

    public void setWorkouts(Map<String, Object> json) {
        if (json == null) {
            this.workouts = null;
        } else {
            this.workouts = new HashMap<>();
            for (String workoutId : json.keySet()) {
                this.workouts.putIfAbsent(workoutId, (String) json.get(workoutId));
            }
        }
    }

    @Override
    public Map<String, Object> asMap() {
        HashMap<String, Object> retVal = new HashMap<>();
        retVal.putIfAbsent(EXERCISE_NAME, this.exerciseName);
        retVal.putIfAbsent(DEFAULT_WEIGHT, this.defaultWeight);
        retVal.putIfAbsent(DEFAULT_REPS, this.defaultReps);
        retVal.putIfAbsent(DEFAULT_SETS, this.defaultSets);
        retVal.putIfAbsent(DEFAULT_DETAILS, this.defaultDetails);
        retVal.putIfAbsent(VIDEO_URL, this.videoUrl);
        retVal.putIfAbsent(FOCUSES, this.focuses);
        retVal.putIfAbsent(User.WORKOUTS, this.workouts);
        return retVal;
    }

    @Override
    public Map<String, Object> asResponse() {
        return this.asMap();
    }
}
