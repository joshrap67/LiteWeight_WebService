package models;

import helpers.Parser;
import interfaces.Model;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WorkoutUser implements Model {

    public static final String WORKOUT_NAME = "workoutName";
    public static final String DATE_LAST = "dateLast";
    public static final String TIMES_COMPLETED = "timesCompleted";
    public static final String AVERAGE_EXERCISES_COMPLETED = "averageExercisesCompleted";
    public static final String TOTAL_EXERCISES_SUM = "totalExercisesSum";

    private String workoutName;

    private String dateLast;
    private Integer timesCompleted;
    private Double averageExercisesCompleted;
    private Integer totalExercisesSum;

    public WorkoutUser(Map<String, Object> json) {
        this.workoutName = (String) json.get(WORKOUT_NAME);

        this.dateLast = (String) json.get(DATE_LAST);
        this.timesCompleted = Parser.convertObjectToInteger(json.get(TIMES_COMPLETED));
        this.averageExercisesCompleted = Parser
            .convertObjectToDouble(json.get(AVERAGE_EXERCISES_COMPLETED));
        this.totalExercisesSum = Parser.convertObjectToInteger(json.get(TOTAL_EXERCISES_SUM));
    }

    @Override
    public Map<String, Object> asMap() {
        HashMap<String, Object> retVal = new HashMap<>();
        retVal.putIfAbsent(WORKOUT_NAME, this.workoutName);
        retVal.putIfAbsent(DATE_LAST, this.dateLast);
        retVal.putIfAbsent(TIMES_COMPLETED, timesCompleted);
        retVal.putIfAbsent(AVERAGE_EXERCISES_COMPLETED, averageExercisesCompleted);
        retVal.putIfAbsent(TOTAL_EXERCISES_SUM, totalExercisesSum);
        return retVal;
    }

    @Override
    public Map<String, Object> asResponse() {
        return this.asMap();
    }
}
