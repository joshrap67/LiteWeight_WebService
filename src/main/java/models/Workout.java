package models;

import com.amazonaws.services.dynamodbv2.document.Item;
import exceptions.InvalidAttributeException;
import interfaces.Model;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
public class Workout implements Model {

    public static final String WORKOUT_ID = "workoutId";
    public static final String WORKOUT_NAME = "workoutName";
    public static final String CREATION_DATE = "creationDate";
    public static final String MOST_FREQUENT_FOCUS = "mostFrequentFocus";
    public static final String CREATOR = "creator";
    public static final String ROUTINE = "routine";

    private String workoutId;
    private String workoutName;
    private String creationDate;
    private String mostFrequentFocus;
    private String creator;
    private Routine routine;

    public Workout(final Item userItem)
        throws InvalidAttributeException {
        this(userItem.asMap());
    }

    public Workout(Map<String, Object> json) throws InvalidAttributeException {
        this.workoutId = (String) json.get(WORKOUT_ID);
        this.workoutName = (String) json.get(WORKOUT_NAME);
        this.creationDate = (String) json.get(CREATION_DATE);
        this.mostFrequentFocus = (String) json.get(MOST_FREQUENT_FOCUS);
        this.creator = (String) json.get(CREATOR);
        this.routine = (Routine) json.get(ROUTINE);
//        this.routine = new Routine((Map<String, Object>) json.get(ROUTINE));
    }

//    public void setRoutine(Map<String, Object> json) throws InvalidAttributeException {
//        if (json == null) {
//            this.routine = null;
//        } else {
//            this.routine = new HashMap<>();
//            for (String week : json.keySet()) {
//                Map<String, Object> days = (Map<String, Object>) json.get(week);
//                for (String day : days.keySet()) {
//                    Map<Integer, ExerciseRoutine> specificDay = new HashMap<>();
//                    specificDay.putIfAbsent(Integer.parseInt(day), new ExerciseRoutine(
//                        (Map<String, Object>) days.get(day)));
//                    this.routine.putIfAbsent(Integer.parseInt(week), specificDay);
//                }
//            }
//        }
//    }

    @Override
    public Map<String, Object> asMap() {
        HashMap<String, Object> retVal = new HashMap<>();
        retVal.putIfAbsent(WORKOUT_NAME, this.workoutName);
        retVal.putIfAbsent(WORKOUT_ID, this.workoutId);
        retVal.putIfAbsent(CREATION_DATE, this.creationDate);
        retVal.putIfAbsent(MOST_FREQUENT_FOCUS, this.mostFrequentFocus);
        retVal.putIfAbsent(CREATOR, this.creator);
        retVal.putIfAbsent(ROUTINE, this.routine.asMap());
        return retVal;
    }

//    public Map<String, Map<String, Object>> getRoutineMap() {
//        if (this.routine == null) {
//            return null;
//        }
//        Map<String, Map<String, Object>> retVal = new HashMap<>();
//        for (Integer week : this.routine.keySet()) {
//            for (Integer day : this.routine.keySet()) {
//                Map<String, Object> specificDay = new HashMap<>();
//                specificDay.putIfAbsent(day.toString(), this.routine.get(week).get(day).asMap());
//                retVal.putIfAbsent(week.toString(), specificDay);
//            }
//        }
//        return retVal;
//    }
}
