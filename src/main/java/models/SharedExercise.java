package models;

import utils.Parser;
import interfaces.Model;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class SharedExercise implements Model {

    public static final String EXERCISE_NAME = "exerciseName";
    public static final String WEIGHT = "weight";
    public static final String SETS = "sets";
    public static final String REPS = "reps";
    public static final String DETAILS = "details";

    private String exerciseName;
    private Double weight;
    private Integer sets;
    private Integer reps;
    private String details;

    public SharedExercise(final Map<String, Object> json) {
        this.exerciseName = (String) json.get(EXERCISE_NAME);
        this.weight = Parser.convertObjectToDouble(json.get(WEIGHT));
        this.sets = Parser.convertObjectToInteger(json.get(SETS));
        this.reps = Parser.convertObjectToInteger(json.get(REPS));
        this.details = (String) json.get(DETAILS);
    }

    public SharedExercise(final RoutineExercise routineExercise, final String exerciseName) {
        this.exerciseName = exerciseName;
        this.weight = routineExercise.getWeight();
        this.sets = routineExercise.getSets();
        this.reps = routineExercise.getReps();
        this.details = routineExercise.getDetails();
    }

    @Override
    public Map<String, Object> asMap() {
        HashMap<String, Object> retVal = new HashMap<>();
        retVal.putIfAbsent(WEIGHT, this.weight);
        retVal.putIfAbsent(EXERCISE_NAME, this.exerciseName);
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
