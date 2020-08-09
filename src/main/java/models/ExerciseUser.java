package models;

import helpers.Parser;
import interfaces.Model;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public class ExerciseUser implements Model {

    public static final String EXERCISE_NAME = "exerciseName";
    public static final String FOCUSES = "focuses";
    public static final String DEFAULT_EXERCISE = "defaultExercise";
    public static final String DEFAULT_WEIGHT = "defaultWeight";
    public static final String DEFAULT_SETS = "defaultSets";
    public static final String DEFAULT_REPS = "defaultReps";
    public static final String DEFAULT_DETAILS = "defaultDetails";
    public static final String VIDEO_URL = "videoUrl";

    private String exerciseName;
    private boolean defaultExercise;
    private Double defaultWeight; // stored in lbs
    private Integer defaultSets;
    private Integer defaultReps;
    private String defaultNote;
    private String videoUrl;
    @Setter(AccessLevel.NONE)
    private Map<String, Boolean> focuses;
    @Setter(AccessLevel.NONE)
    private Map<String, String> workouts; // id to workout name that this exercise is apart of


    public ExerciseUser(Map<String, Object> json) {
        this.exerciseName = (String) json.get(EXERCISE_NAME);
        this.defaultExercise = (boolean) json.get(DEFAULT_EXERCISE);
        this.defaultWeight = Parser.convertObjectToDouble(json.get(DEFAULT_WEIGHT));
        this.defaultSets = Parser.convertObjectToInteger(json.get(DEFAULT_SETS));
        this.defaultReps = Parser.convertObjectToInteger(json.get(DEFAULT_REPS));
        this.defaultNote = (String) json.get(DEFAULT_DETAILS);
        this.videoUrl = (String) json.get(VIDEO_URL);
        this.setWorkouts((Map<String, Object>) json.get(User.WORKOUTS));
        this.setFocuses((Map<String, Object>) json.get(FOCUSES));
    }

    public ExerciseUser(String exerciseName, String videUrl, Map<String, Boolean> focuses) {
        // constructor that is called when user is created for the first time with default exercises
        this.exerciseName = exerciseName;
        this.videoUrl = videUrl;
        this.focuses = focuses;
        this.defaultExercise = true;
        this.defaultWeight = 0.0;
        this.defaultReps = 12; // TODO use const
        this.defaultSets = 3;
        this.defaultNote = "";
        this.workouts = new HashMap<>();
    }

    public void setFocuses(Map<String, Object> json) {
        if (json == null) {
            this.focuses = null;
        } else {
            this.focuses = new HashMap<>();
            for (String focusName : json.keySet()) {
                this.focuses.putIfAbsent(focusName, true);
            }
        }
    }

    public void setWorkouts(Map<String, Object> json) {
        if (json == null) {
            this.workouts = null;
        } else {
            this.workouts = new HashMap<>();
            for (String workoutId : json.keySet()) {
                this.workouts.putIfAbsent(workoutId, (String) json.get(Workout.WORKOUT_NAME));
            }
        }
    }

    @Override
    public Map<String, Object> asMap() {
        HashMap<String, Object> retVal = new HashMap<>();
        retVal.putIfAbsent(EXERCISE_NAME, this.exerciseName);
        retVal.putIfAbsent(DEFAULT_EXERCISE, this.defaultExercise);
        retVal.putIfAbsent(DEFAULT_WEIGHT, this.defaultWeight);
        retVal.putIfAbsent(DEFAULT_REPS, this.defaultReps);
        retVal.putIfAbsent(DEFAULT_SETS, this.defaultSets);
        retVal.putIfAbsent(DEFAULT_DETAILS, this.defaultNote);
        retVal.putIfAbsent(VIDEO_URL, this.videoUrl);
        retVal.putIfAbsent(FOCUSES, this.focuses);
        retVal.putIfAbsent(User.WORKOUTS, this.workouts);
        return retVal;
    }
}
