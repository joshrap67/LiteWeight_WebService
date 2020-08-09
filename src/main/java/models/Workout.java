package models;

import com.amazonaws.services.dynamodbv2.document.Item;
import exceptions.InvalidAttributeException;
import helpers.Parser;
import interfaces.Model;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Workout implements Model {

    public static final String WORKOUT_ID = "workoutId";
    public static final String WORKOUT_NAME = "workoutName";
    public static final String CREATION_DATE = "creationDate";
    public static final String MOST_FREQUENT_FOCUS = "mostFrequentFocus";
    public static final String CREATOR = "creator";
    public static final String ROUTINE = "routine";
    public static final String CURRENT_DAY = "currentDay";
    public static final String CURRENT_WEEK = "currentWeek";

    private String workoutId;
    private String workoutName;
    private String creationDate;
    private String mostFrequentFocus;
    private String creator;
    private Routine routine;
    private Integer currentDay;
    private Integer currentWeek;


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
        this.routine = new Routine((Map<String, Object>) json.get(ROUTINE));
        this.currentDay = Parser.convertObjectToInteger(json.get(CURRENT_DAY));
        this.currentWeek = Parser.convertObjectToInteger(json.get(CURRENT_WEEK));
    }

    @Override
    public Map<String, Object> asMap() {
        HashMap<String, Object> retVal = new HashMap<>();
        retVal.putIfAbsent(WORKOUT_NAME, this.workoutName);
        retVal.putIfAbsent(WORKOUT_ID, this.workoutId);
        retVal.putIfAbsent(CREATION_DATE, this.creationDate);
        retVal.putIfAbsent(MOST_FREQUENT_FOCUS, this.mostFrequentFocus);
        retVal.putIfAbsent(CREATOR, this.creator);
        retVal.putIfAbsent(ROUTINE, this.routine.asMap());
        retVal.putIfAbsent(CURRENT_WEEK, this.currentWeek);
        retVal.putIfAbsent(CURRENT_DAY, this.currentDay);
        return retVal;
    }
}
