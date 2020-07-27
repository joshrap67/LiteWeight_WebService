package models;

import interfaces.Model;
import java.util.Map;

public class WorkoutUser implements Model {

  public static final String WORKOUT_NAME = "workoutName";
  public static final String CURRENT_DAY = "currentDay";
  public static final String DATE_LAST = "dateLast";
  public static final String TIMES_COMPLETED = "timesCompleted";
  public static final String AVERAGE_EXERCISES_COMPLETED = "averageExercisesCompleted";
  public static final String TOTAL_EXERCISES_SUM = "totalExercisesSum";

  @Override
  public Map<String, Object> asMap() {
    return null;
  }
}
