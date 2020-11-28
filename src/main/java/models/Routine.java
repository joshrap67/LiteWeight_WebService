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
public class Routine implements Model, Iterable<Integer> {

    private Map<Integer, RoutineWeek> weeks;

    public Routine() {
        this.weeks = new HashMap<>();
    }

    public Routine(Routine toBeCloned) {
        // copy constructor
        this.weeks = new HashMap<>();
        for (Integer week : toBeCloned) {
            RoutineWeek routineWeek = new RoutineWeek();
            for (Integer day : toBeCloned.getWeek(week)) {
                routineWeek.put(day, toBeCloned.getDay(week, day).clone());
            }
            this.weeks.putIfAbsent(week, routineWeek);
        }
    }

    public Routine(Map<String, Object> json) throws InvalidAttributeException {
        if (json == null) {
            this.weeks = null;
        } else {
            this.weeks = new HashMap<>();
            for (String week : json.keySet()) {
                RoutineWeek routineWeek = new RoutineWeek((Map<String, Object>) json.get(week));
                this.weeks.put(Integer.parseInt(week), routineWeek);
            }
        }
    }

    public Routine(final SentRoutine routine, final Map<String, String> exerciseNameToId) {
        // this constructor is used to convert from a sent routine back to a normal workout routine
        this.weeks = new HashMap<>();
        for (Integer week : routine) {
            final RoutineWeek routineWeek = new RoutineWeek();
            for (Integer day : routine.getWeek(week)) {
                final RoutineDay routineDay = new RoutineDay();
                int sortVal = 0;
                for (SharedExercise sharedExercise : routine.getExerciseListForDay(week, day)) {
                    RoutineExercise routineExercise = new RoutineExercise(sharedExercise,
                        exerciseNameToId.get(sharedExercise.getExerciseName()));
                    routineDay.put(sortVal, routineExercise);
                    sortVal++;
                }
                routineWeek.put(day, routineDay);
            }
            this.putWeek(week, routineWeek);
        }
    }

    public List<RoutineExercise> getExerciseListForDay(int week, int day) {
        List<RoutineExercise> exerciseList = new ArrayList<>();
        for (Integer sortVal : this.getWeek(week).getDay(day)) {
            exerciseList.add(this.getDay(week, day).getExercise(sortVal));
        }
        return exerciseList;
    }

    public RoutineWeek getWeek(int week) {
        return this.weeks.get(week);
    }

    public RoutineDay getDay(int week, int day) {
        return this.weeks.get(week).getDay(day);
    }

    public void putWeek(int weekIndex, RoutineWeek week) {
        this.weeks.put(weekIndex, week);
    }

    public boolean removeExercise(int week, int day, String exerciseId) {
        return this.getDay(week, day).deleteExercise(exerciseId);
    }

    public static void deleteExerciseFromRoutine(final String exerciseId, final Routine routine) {
        // removes all instances of a given exercise in the routine
        for (Integer week : routine) {
            for (Integer day : routine.getWeek(week)) {
                routine.removeExercise(week, day, exerciseId);
            }
        }
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
