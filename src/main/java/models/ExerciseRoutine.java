package models;

import exceptions.InvalidAttributeException;
import interfaces.Model;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class ExerciseRoutine implements Model {

    public static final String SORT_INDEX = "sortIndex";
    public static final String COMPLETED = "completed";
    public static final String EXERCISE_ID = "exerciseId";
    public static final String WEIGHT = "weight";
    public static final String SETS = "sets";
    public static final String REPS = "reps";
    public static final String DETAILS = "details";

    private Integer sortIndex;
    private boolean completed;
    private String exerciseId;
    private double weight;
    private Integer sets;
    private Integer reps;
    private String details;

    public ExerciseRoutine(Map<String, Object> json) throws InvalidAttributeException {
        this.sortIndex = (int) json.get(SORT_INDEX);
        this.completed = (boolean) json.get(COMPLETED);
        this.exerciseId = (String) json.get(EXERCISE_ID);
        this.weight = (double) json.get(WEIGHT);
        this.sets = (int) json.get(SETS);
        this.reps = (int) json.get(REPS);
        this.details = (String) json.get(DETAILS);
    }


    @Override
    public Map<String, Object> asMap() {
        HashMap<String, Object> retVal = new HashMap<>();
        retVal.putIfAbsent(SORT_INDEX, this.sortIndex);
        retVal.putIfAbsent(COMPLETED, this.completed);
        retVal.putIfAbsent(EXERCISE_ID, this.exerciseId);
        retVal.putIfAbsent(WEIGHT, this.weight);
        retVal.putIfAbsent(SETS, this.sets);
        retVal.putIfAbsent(REPS, this.reps);
        retVal.putIfAbsent(DETAILS, this.details);
        return retVal;
    }
}
