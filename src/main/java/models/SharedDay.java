package models;

import exceptions.InvalidAttributeException;
import interfaces.Model;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class SharedDay implements Iterable<SharedExercise>, Model {

    public static final String EXERCISES = "exercises";
    public static final String TAG = "tag";

    private List<SharedExercise> exercises;
    private String tag;

    public SharedDay() {
        this.exercises = new ArrayList<>();
    }

    public SharedDay(Map<String, Object> json) {
        this.exercises = new ArrayList<>();
        this.tag = (String) json.get(TAG);

        List<Object> exercisesJson = (List<Object>) json.get(EXERCISES);
        for (Object exerciseJson : exercisesJson) {
            SharedExercise routineExercise = new SharedExercise((Map<String, Object>) exerciseJson);
            this.exercises.add(routineExercise);
        }
    }

    public void put(int sortVal, SharedExercise sharedExercise) {
        this.exercises.set(sortVal, sharedExercise);
    }

    public void appendExercise(SharedExercise sharedExercise){
        this.exercises.add(sharedExercise);
    }

    @Override
    public Iterator<SharedExercise> iterator() {
        return this.exercises.iterator();
    }

    @Override
    public Map<String, Object> asMap() {
        HashMap<String, Object> retVal = new HashMap<>();
        List<Object> jsonExercises = new ArrayList<>();
        for (SharedExercise exercise : this) {
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
