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
public class SentRoutine implements Model, Iterable<Integer> {

    private Map<Integer, SentWeek> weeks;

    public SentRoutine() {
        this.weeks = new HashMap<>();
    }

    public SentRoutine(final Routine routine, Map<String, OwnedExercise> ownedExerciseMap) {
        this.weeks = new HashMap<>();
        for (Integer week : routine) {
            final SentWeek sentWeek = new SentWeek();
            for (Integer day : routine.getWeek(week)) {
                final SentDay sentDay = new SentDay();
                int sortVal = 0;
                for (RoutineExercise exercise : routine.getExerciseListForDay(week, day)) {
                    final SentExercise sentExercise = new SentExercise(exercise,
                        ownedExerciseMap.get(exercise.getExerciseId()).getExerciseName());
                    sentDay.put(sortVal, sentExercise);
                    sortVal++;
                }
                sentWeek.put(day, sentDay);
            }
            this.putWeek(week, sentWeek);
        }
    }

    public SentRoutine(Map<String, Object> json) throws InvalidAttributeException {
        if (json == null) {
            this.weeks = null;
        } else {
            this.weeks = new HashMap<>();
            for (String week : json.keySet()) {
                SentWeek sentWeek = new SentWeek((Map<String, Object>) json.get(week));
                this.weeks.put(Integer.parseInt(week), sentWeek);
            }
        }
    }

    public List<SentExercise> getExerciseListForDay(int week, int day) {
        List<SentExercise> exerciseList = new ArrayList<>();
        for (Integer sortVal : this.getWeek(week).getDay(day)) {
            exerciseList.add(this.getDay(week, day).getExercise(sortVal));
        }
        return exerciseList;
    }

    public SentWeek getWeek(int week) {
        return this.weeks.get(week);
    }

    public SentDay getDay(int week, int day) {
        return this.weeks.get(week).getDay(day);
    }

    public void putWeek(int weekIndex, SentWeek week) {
        this.weeks.put(weekIndex, week);
    }

    public int getNumberOfWeeks() {
        return this.weeks.size();
    }

    public int getTotalNumberOfDays() {
        int days = 0;
        for (Integer week : this.weeks.keySet()) {
            days += this.weeks.get(week).getNumberOfDays();
        }
        return days;
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
