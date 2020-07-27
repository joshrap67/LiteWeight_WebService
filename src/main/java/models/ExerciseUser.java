package models;

import interfaces.Model;
import java.util.Map;

public class ExerciseUser implements Model {

  public static final String EXERCISE_NAME = "exerciseName";
  public static final String FOCUSES = "focuses";
  public static final String DEFAULT_EXERCISE = "defaultExercise";
  public static final String DEFAULT_WEIGHT = "defaultWeight";
  public static final String DEFAULT_SET = "defaultSet";
  public static final String DEFAULT_REPS = "defaultReps";
  public static final String DEFAULT_NOTE = "defaultNote";
  public static final String VIDEO_URL = "videoUrl";

  @Override
  public Map<String, Object> asMap() {
    return null;
  }
}
