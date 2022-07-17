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
public class SharedRoutine implements Model, Iterable<Integer> {

    private Map<Integer, SharedWeek> weeks;

    public SharedRoutine() {
        this.weeks = new HashMap<>();
    }

    public SharedRoutine(final Routine routine, Map<String, OwnedExercise> ownedExerciseMap) {
        this.weeks = new HashMap<>();
        for (Integer week : routine) {
            final SharedWeek sharedWeek = new SharedWeek();
            for (Integer day : routine.getWeek(week)) {
                final SharedDay sharedDay = new SharedDay();
                int sortVal = 0;
                for (RoutineExercise exercise : routine.getExerciseListForDay(week, day)) {
                    final SharedExercise sharedExercise = new SharedExercise(exercise,
                        ownedExerciseMap.get(exercise.getExerciseId()).getExerciseName());
                    sharedDay.put(sortVal, sharedExercise);
                    sortVal++;
                }
                sharedWeek.put(day, sharedDay);
            }
            this.putWeek(week, sharedWeek);
        }
    }

    public SharedRoutine(Map<String, Object> json) throws InvalidAttributeException {
        if (json == null) {
            this.weeks = null;
        } else {
            this.weeks = new HashMap<>();
            for (String week : json.keySet()) {
                SharedWeek sharedWeek = new SharedWeek((Map<String, Object>) json.get(week));
                this.weeks.put(Integer.parseInt(week), sharedWeek);
            }
        }
    }

    public List<SharedExercise> getExerciseListForDay(int week, int day) {
        List<SharedExercise> exerciseList = new ArrayList<>();
        for (Integer sortVal : this.getWeek(week).getDay(day)) {
            exerciseList.add(this.getDay(week, day).getExercise(sortVal));
        }
        return exerciseList;
    }

    public SharedWeek getWeek(int week) {
        return this.weeks.get(week);
    }

    public SharedDay getDay(int week, int day) {
        return this.weeks.get(week).getDay(day);
    }

    public void putWeek(int weekIndex, SharedWeek week) {
        this.weeks.put(weekIndex, week);
    }

    @Override
    public Map<String, Object> asMap() {
        HashMap<String, Object> retVal = new HashMap<>();
        for (Integer week : this.weeks.keySet()) {
            retVal.put(week.toString(), this.getWeek(week).asMap());
        }

        return retVal;
    }

    @Override
    public Iterator<Integer> iterator() {
        return this.weeks.keySet().iterator();
    }

    @Override
    public Map<String, Object> asResponse() {
        return this.asMap();
    }
}
