package models;

import interfaces.Model;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class SharedRoutine implements Model, Iterable<SharedWeek> {

    public static final String WEEKS = "weeks";

    private List<SharedWeek> weeks;

    public SharedRoutine() {
        this.weeks = new ArrayList<>();
    }

    public SharedRoutine(final Routine routine, Map<String, OwnedExercise> ownedExerciseMap) {
        this.weeks = new ArrayList<>();
        for (RoutineWeek week : routine) {
            final SharedWeek sharedWeek = new SharedWeek();
            for (RoutineDay day : week) {
                final SharedDay sharedDay = new SharedDay();
                sharedDay.setTag(day.getTag());
                for (RoutineExercise exercise : day) {
                    final SharedExercise sharedExercise = new SharedExercise(exercise,
                        ownedExerciseMap.get(exercise.getExerciseId()).getExerciseName());
                    sharedDay.appendExercise(sharedExercise);
                }
                sharedWeek.appendDay(sharedDay);
            }
            this.appendWeek(sharedWeek);
        }
    }

    public SharedRoutine(Map<String, Object> json) {
        if (json == null) {
            this.weeks = new ArrayList<>();
        } else {
            this.weeks = new ArrayList<>();
            List<Object> jsonWeeks = (List<Object>) json.get(WEEKS);
            for (Object jsonKey : jsonWeeks) {
                SharedWeek sharedWeek = new SharedWeek((Map<String, Object>) jsonKey);
                this.weeks.add(sharedWeek);
            }
        }
    }

    private void appendWeek(SharedWeek sharedWeek) {
        this.weeks.add(sharedWeek);
    }

    @Override
    public Map<String, Object> asMap() {
        HashMap<String, Object> retVal = new HashMap<>();
        List<Object> jsonWeeks = new ArrayList<>();
        for (SharedWeek week : this) {
            jsonWeeks.add(week.asMap());
        }
        retVal.put(WEEKS, jsonWeeks);
        return retVal;
    }

    @Override
    public Iterator<SharedWeek> iterator() {
        return this.weeks.iterator();
    }

    @Override
    public Map<String, Object> asResponse() {
        return this.asMap();
    }
}
