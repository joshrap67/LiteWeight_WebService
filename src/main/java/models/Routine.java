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
public class Routine implements Model, Iterable<RoutineWeek> {

    public static final String WEEKS = "weeks";

    private List<RoutineWeek> weeks;

    public Routine() {
        this.weeks = new ArrayList<>();
    }

    public Routine(Routine toBeCloned) {
        // copy constructor
        this.weeks = new ArrayList<>();
        for (RoutineWeek week : toBeCloned) {
            RoutineWeek routineWeek = new RoutineWeek(); // todo add clone method
            for (RoutineDay day : week) {
                routineWeek.appendDay(day.clone());
            }
            this.weeks.add(routineWeek);
        }
    }

    public Routine(Map<String, Object> json) throws InvalidAttributeException {
        if (json == null) {
            this.weeks = new ArrayList<>();
        } else {
            this.weeks = new ArrayList<>();
            List<Object> jsonWeeks = (List<Object>) json.get(WEEKS);
            for (Object week : jsonWeeks) {
                RoutineWeek routineWeek = new RoutineWeek((Map<String, Object>) week);
                this.weeks.add(routineWeek);
            }
        }
    }

    public Routine(final SharedRoutine routine, final Map<String, String> exerciseNameToId) {
        // this constructor is used to convert from a shared routine back to a normal workout routine
        this.weeks = new ArrayList<>();
        for (SharedWeek week : routine) {
            final RoutineWeek routineWeek = new RoutineWeek();
            for (SharedDay day : week) {
                final RoutineDay routineDay = new RoutineDay();
                routineDay.setTag(day.getTag());
                for (SharedExercise sharedExercise : day) {
                    RoutineExercise routineExercise = new RoutineExercise(sharedExercise,
                        exerciseNameToId.get(sharedExercise.getExerciseName()));
                    routineDay.appendExercise(routineExercise);
                }
                routineWeek.appendDay(routineDay);
            }
            this.appendWeek(routineWeek);
        }
    }

    public RoutineWeek getWeek(int week) {
        return this.weeks.get(week);
    }

    public void appendWeek(RoutineWeek week) {
        this.weeks.add(week);
    }

    public static void deleteExerciseFromRoutine(final String exerciseId, final Routine routine) {
        // removes all instances of a given exercise in the routine
        for (RoutineWeek week : routine) {
            for (RoutineDay day : week) {
                day.deleteExercise(exerciseId);
            }
        }
    }

    public int getNumberOfWeeks() {
        return this.weeks.size();
    }

    public int getTotalNumberOfDays() {
        int days = 0;
        for (RoutineWeek week : this) {
            days += week.getNumberOfDays();
        }
        return days;
    }

    @Override
    public Map<String, Object> asMap() {
        HashMap<String, Object> retVal = new HashMap<>();
        List<Object> jsonWeeks = new ArrayList<>();
        for (RoutineWeek week : this) {
            jsonWeeks.add(week.asMap());
        }
        retVal.put(WEEKS, jsonWeeks);
        return retVal;
    }

    @Override
    public Iterator<RoutineWeek> iterator() {
        return this.weeks.iterator();
    }

    @Override
    public Map<String, Object> asResponse() {
        return this.asMap();
    }
}
