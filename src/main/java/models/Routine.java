package models;

import exceptions.InvalidAttributeException;
import interfaces.Model;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public class Routine implements Model {

    @Setter(AccessLevel.NONE)
    Map<Integer, Map<Integer, RoutineDayMap>> routine;

    public Routine(Map<String, Object> json) throws InvalidAttributeException {
        if (json == null) {
            this.routine = null;
        } else {
            this.routine = new HashMap<>();
            for (String week : json.keySet()) {
                Map<String, Object> days = (Map<String, Object>) json.get(week);
                Map<Integer, RoutineDayMap> specificDay = new HashMap<>();
                for (String day : days.keySet()) {

                    RoutineDayMap dayExerciseMap = new RoutineDayMap(
                        (Map<String, Object>) ((Map<String, Object>) json
                            .get(week)).get(day));

                    specificDay.putIfAbsent(Integer.parseInt(day), dayExerciseMap);
                }
                this.routine.putIfAbsent(Integer.parseInt(week), specificDay);
            }
        }
    }

    public void resetAllExercises() {
        // marks all exercises in the routine as not completed
        for (Integer week : this.getRoutine().keySet()) {
            for (Integer day : this.getRoutine().get(week).keySet()) {
                for (ExerciseRoutine exerciseRoutine : this.getExerciseListForDay(week, day)) {
                    exerciseRoutine.setCompleted(false);
                }
            }
        }
    }

    public List<ExerciseRoutine> getExerciseListForDay(int week, int day) {
        List<ExerciseRoutine> list = new ArrayList<>();
        for (Integer sortVal : this.routine.get(week).get(day).getExerciseRoutineMap().keySet()) {
            list.add(this.routine.get(week).get(day).getExerciseRoutineMap().get(sortVal));
        }
        return list;
    }

    public boolean removeExercise(int week, int day, String exerciseId) {
        return this.routine.get(week).get(day).deleteExercise(exerciseId);
    }

    public Map<Integer, RoutineDayMap> getWeek(int week) {
        return this.getRoutine().get(week);
    }

    public RoutineDayMap getDay(int week, int day) {
        return this.getRoutine().get(week).get(day);
    }

    public int size() {
        return this.routine.size();
    }

    @Override
    public Map<String, Object> asMap() {
        HashMap<String, Object> retVal = new HashMap<>();
        if (this.routine != null) {
            for (Integer week : this.routine.keySet()) {
                Map<String, Object> specificDay = new HashMap<>();
                for (Integer day : this.routine.get(week).keySet()) {
                    Map<String, Object> exercisesForDay = new HashMap<>();
                    for (Integer sortVal : this.routine.get(week).get(day).getExerciseRoutineMap()
                        .keySet()) {
                        exercisesForDay.putIfAbsent(sortVal.toString(),
                            this.routine.get(week).get(day).getExerciseRoutineMap().get(sortVal)
                                .asMap());
                    }
                    specificDay.putIfAbsent(day.toString(), exercisesForDay);
                }
                retVal.putIfAbsent(week.toString(), specificDay);
            }
        }
        return retVal;
    }
}
