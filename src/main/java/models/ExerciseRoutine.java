package models;

import interfaces.Model;
import java.util.Map;

public class ExerciseRoutine implements Model {

  public static final String SORT_INDEX = "sortIndex";
  public static final String COMPLETED = "completed";
  public static final String EXERCISE_ID = "exerciseId";
  public static final String EXERCISE_NAME = "exerciseName";
  public static final String WEIGHT = "weight";
  public static final String SETS = "sets";
  public static final String REPS = "reps";
  public static final String DETAILS = "details";


  @Override
  public Map<String, Object> asMap() {
    return null;
  }
}
