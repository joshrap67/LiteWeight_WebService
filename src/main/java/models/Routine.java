package models;

import exceptions.InvalidAttributeException;
import interfaces.Model;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public class Routine implements Model {

    @Setter(AccessLevel.NONE)
    Map<Integer, Map<Integer, ExerciseRoutine>> routine;

    public Routine(Map<String, Object> json) throws InvalidAttributeException {
        if (json == null) {
            this.routine = null;
        } else {
            this.routine = new HashMap<>();
            for (String week : json.keySet()) {
                Map<String, Object> days = (Map<String, Object>) json.get(week);
                for (String day : days.keySet()) {
                    Map<Integer, ExerciseRoutine> specificDay = new HashMap<>();
                    specificDay.putIfAbsent(Integer.parseInt(day), new ExerciseRoutine(
                        (Map<String, Object>) days.get(day)));
                    this.routine.putIfAbsent(Integer.parseInt(week), specificDay);
                }
            }
        }
    }

    @Override
    public Map<String, Object> asMap() {
        HashMap<String, Object> retVal = new HashMap<>();
        if (this.routine != null) {
            for (Integer week : this.routine.keySet()) {
                for (Integer day : this.routine.keySet()) {
                    Map<String, Object> specificDay = new HashMap<>();
                    specificDay
                        .putIfAbsent(day.toString(), this.routine.get(week).get(day).asMap());
                    retVal.putIfAbsent(week.toString(), specificDay);
                }
            }
        }
        return retVal;
    }
}
