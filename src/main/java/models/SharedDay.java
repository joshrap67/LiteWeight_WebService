package models;

import exceptions.InvalidAttributeException;
import interfaces.Model;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import lombok.Data;

@Data
public class SharedDay implements Iterable<Integer>, Model {

    private Map<Integer, SharedExercise> exercises;

    public SharedDay() {
        this.exercises = new HashMap<>();
    }

    public SharedDay(Map<String, Object> exercisesForDay) throws InvalidAttributeException {
        this.exercises = new HashMap<>();
        for (String sortVal : exercisesForDay.keySet()) {
            SharedExercise routineExercise = new SharedExercise(
                (Map<String, Object>) exercisesForDay.get(sortVal));
            this.exercises.put(Integer.parseInt(sortVal), routineExercise);
        }
    }

    public int getNumberOfExercises() {
        return this.exercises.size();
    }

    public SharedExercise getExercise(int sortVal) {
        return this.exercises.get(sortVal);
    }

    public void put(int sortVal, SharedExercise sharedExercise) {
        this.exercises.put(sortVal, sharedExercise);
    }

    @Override
    public Iterator<Integer> iterator() {
        return this.exercises.keySet().iterator();
    }

    @Override
    public Map<String, Object> asMap() {
        HashMap<String, Object> retVal = new HashMap<>();
        for (Integer sortVal : this) {
            retVal.put(sortVal.toString(), this.getExercise(sortVal).asMap());
        }
        return retVal;
    }

    @Override
    public Map<String, Object> asResponse() {
        return this.asMap();
    }
}
