package models;

import exceptions.InvalidAttributeException;
import interfaces.Model;
import java.util.HashMap;
import java.util.Map;
import lombok.ToString;

@ToString
public class RoutineDayMap implements Model {

    private Map<Integer, ExerciseRoutine> exerciseRoutineMap;

    RoutineDayMap(Map<String, Object> json) throws InvalidAttributeException {
        this.exerciseRoutineMap = new HashMap<>();
        for (String sortVal : json.keySet()) {
            this.exerciseRoutineMap.put(Integer.valueOf(sortVal),
                new ExerciseRoutine((Map<String, Object>) json.get(sortVal)));
        }
    }

    public Map<Integer, ExerciseRoutine> getExerciseRoutineMap() {
        return this.exerciseRoutineMap;
    }

    void deleteExercise(String exerciseId) {
        int index = -1;
        for (Integer sortVal : this.exerciseRoutineMap.keySet()) {
            if (this.exerciseRoutineMap.get(sortVal).getExerciseId().equals(exerciseId)) {
                index = sortVal;
            }
        }
        this.exerciseRoutineMap.remove(index);
        balanceMap();
    }

    private void balanceMap() {
        int i = 0;
        Map<Integer, ExerciseRoutine> temp = new HashMap<>();
        for (Integer sortVal : this.exerciseRoutineMap.keySet()) {
            temp.put(i, this.exerciseRoutineMap.get(sortVal));
            i++;
        }
        this.exerciseRoutineMap = temp;
    }

    @Override
    public Map<String, Object> asMap() {
        HashMap<String, Object> retVal = new HashMap<>();
        for (Integer sortVal : this.exerciseRoutineMap.keySet()) {
            retVal.put(sortVal.toString(), this.exerciseRoutineMap.get(sortVal).asMap());
        }
        return retVal;
    }
}
