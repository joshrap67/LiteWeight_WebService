package models;

import exceptions.InvalidAttributeException;
import interfaces.Model;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import lombok.Data;

@Data
public class RoutineDay implements Iterable<Integer>, Model {

    private Map<Integer, RoutineExercise> exercises;

    public RoutineDay() {
        this.exercises = new HashMap<>();
    }

    public RoutineDay(Map<String, Object> exercisesForDay) throws InvalidAttributeException {
        this.exercises = new HashMap<>();
        for (String sortVal : exercisesForDay.keySet()) {
            RoutineExercise routineExercise = new RoutineExercise(
                (Map<String, Object>) exercisesForDay.get(sortVal));
            this.exercises.put(Integer.parseInt(sortVal), routineExercise);
        }
    }

    public RoutineDay clone() {
        RoutineDay retVal = new RoutineDay();
        for (Integer sortVal : this.exercises.keySet()) {
            RoutineExercise specificExerciseCloned = new RoutineExercise(
                this.exercises.get(sortVal));
            retVal.getExercises().putIfAbsent(sortVal, specificExerciseCloned);
        }
        return retVal;
    }

    public void put(int sortVal, RoutineExercise routineExercise) {
        this.exercises.put(sortVal, routineExercise);
    }

    public int getNumberOfExercises() {
        return this.exercises.size();
    }

    public RoutineExercise getExercise(int sortVal) {
        return this.exercises.get(sortVal);
    }

    public boolean deleteExercise(String exerciseId) {
        boolean deleted = false;

        int index = -1;
        for (Integer sortVal : this.exercises.keySet()) {
            if (this.exercises.get(sortVal).getExerciseId().equals(exerciseId)) {
                index = sortVal;
            }
        }
        if (index != -1) {
            this.exercises.remove(index);
            balanceMap();
            deleted = true;
        }
        return deleted;
    }

    private void balanceMap() {
        int i = 0;
        Map<Integer, RoutineExercise> temp = new HashMap<>();
        for (Integer sortVal : this.exercises.keySet()) {
            temp.put(i, this.exercises.get(sortVal));
            i++;
        }
        this.exercises = temp;
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
