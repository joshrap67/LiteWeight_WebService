package models;

import interfaces.Model;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class RoutineDay implements Iterable<RoutineExercise>, Model {

    public static final String EXERCISES = "exercises";
    public static final String TAG = "tag";

    private List<RoutineExercise> exercises;
    private String tag;

    public RoutineDay() {
        this.exercises = new ArrayList<>();
    }

    public RoutineDay(Map<String, Object> json) {
        this.exercises = new ArrayList<>();
        this.tag = (String) json.get(TAG);

        List<Object> jsonExercises = (List<Object>) json.get(EXERCISES);
        for (Object exercise : jsonExercises) {
            RoutineExercise routineExercise = new RoutineExercise((Map<String, Object>) exercise);
            this.exercises.add(routineExercise);
        }
    }

    public RoutineDay clone() {
        RoutineDay clonedDay = new RoutineDay();
        clonedDay.tag = this.tag;
        for (RoutineExercise routineExercise : this.exercises) {
            RoutineExercise specificExerciseCloned = new RoutineExercise(routineExercise);
            clonedDay.getExercises().add(specificExerciseCloned);
        }
        return clonedDay;
    }

    public void put(int exerciseIndex, RoutineExercise routineExercise) {
        this.exercises.set(exerciseIndex, routineExercise);
    }

    public void appendExercise(RoutineExercise exercise) {
        this.exercises.add(exercise);
    }

    public boolean deleteExercise(String exerciseId) {
        return this.exercises.removeIf(exercise -> exercise.getExerciseId().equals(exerciseId));
    }

    @Override
    public Iterator<RoutineExercise> iterator() {
        return this.exercises.iterator();
    }

    @Override
    public Map<String, Object> asMap() {
        HashMap<String, Object> retVal = new HashMap<>();
        List<Object> jsonExercises = new ArrayList<>();
        for (RoutineExercise exercise : this) {
            jsonExercises.add(exercise.asMap());
        }
        retVal.put(EXERCISES, jsonExercises);
        retVal.put(TAG, this.tag);
        return retVal;
    }

    @Override
    public Map<String, Object> asResponse() {
        return this.asMap();
    }
}
