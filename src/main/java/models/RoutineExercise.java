package models;

import utils.Parser;
import interfaces.Model;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class RoutineExercise implements Model {

    public static final String COMPLETED = "completed";
    public static final String EXERCISE_ID = "exerciseId";
    public static final String WEIGHT = "weight";
    public static final String SETS = "sets";
    public static final String REPS = "reps";
    public static final String DETAILS = "details";

    private boolean completed;
    private String exerciseId;
    private Double weight;
    private Integer sets;
    private Integer reps;
    private String details;

    public RoutineExercise(Map<String, Object> json) {
        this.completed = (boolean) json.get(COMPLETED);
        this.exerciseId = (String) json.get(EXERCISE_ID);
        this.weight = Parser.convertObjectToDouble(json.get(WEIGHT));
        this.sets = Parser.convertObjectToInteger(json.get(SETS));
        this.reps = Parser.convertObjectToInteger(json.get(REPS));
        this.details = (String) json.get(DETAILS);
    }

    public RoutineExercise(final SharedExercise sharedExercise, final String exerciseId) {
        // this constructor is used when converting from an exercise from a sent workout back to a normal workout exercise
        this.completed = false;
        this.exerciseId = exerciseId;
        this.weight = sharedExercise.getWeight();
        this.sets = sharedExercise.getSets();
        this.reps = sharedExercise.getReps();
        this.details = sharedExercise.getDetails();
    }

    public RoutineExercise(RoutineExercise toBeCopied) {
        // Copy constructor used for deep copies
        this.completed = toBeCopied.completed;
        this.exerciseId = toBeCopied.exerciseId;
        this.weight = toBeCopied.weight;
        this.sets = toBeCopied.sets;
        this.reps = toBeCopied.reps;
        this.details = toBeCopied.details;
    }

    @Override
    public Map<String, Object> asMap() {
        HashMap<String, Object> retVal = new HashMap<>();
        retVal.putIfAbsent(COMPLETED, this.completed);
        retVal.putIfAbsent(EXERCISE_ID, this.exerciseId);
        retVal.putIfAbsent(WEIGHT, this.weight);
        retVal.putIfAbsent(SETS, this.sets);
        retVal.putIfAbsent(REPS, this.reps);
        retVal.putIfAbsent(DETAILS, this.details);
        return retVal;
    }

    @Override
    public Map<String, Object> asResponse() {
        return this.asMap();
    }
}
