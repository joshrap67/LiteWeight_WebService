package models;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import exceptions.InvalidAttributeException;
import helpers.AttributeValueHelper;
import interfaces.Model;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SentWorkout implements Model {

    public static final String SENT_WORKOUT_ID = "sentWorkoutId";
    public static final String WORKOUT_NAME = "workoutName";
    public static final String CREATOR = "creator";
    public static final String ROUTINE = "routine";

    private String sentWorkoutId;
    private String workoutName;
    private String creator;
    private SentRoutine routine;

    public SentWorkout(final Item userItem)
        throws InvalidAttributeException {
        this(userItem.asMap());
    }

    public SentWorkout(Map<String, Object> json) throws InvalidAttributeException {
        this.sentWorkoutId = (String) json.get(SENT_WORKOUT_ID);
        this.workoutName = (String) json.get(WORKOUT_NAME);
        this.creator = (String) json.get(CREATOR);
        this.routine = new SentRoutine((Map<String, Object>) json.get(ROUTINE));
    }

    public SentWorkout(final Workout workout, final User user, final String sentWorkoutId) {
        this.sentWorkoutId = sentWorkoutId;
        this.workoutName = workout.getWorkoutName();
        this.creator = workout.getCreator();
        this.routine = convertWorkoutRoutineToSentRoutine(workout.getRoutine(), user);
    }

    private static SentRoutine convertWorkoutRoutineToSentRoutine(final Routine routine,
        final User user) {
        final SentRoutine sentRoutine = new SentRoutine();
        for (Integer week : routine) {
            final SentWeek sentWeek = new SentWeek();
            for (Integer day : routine.getWeek(week)) {
                final SentDay sentDay = new SentDay();
                int sortVal = 0;
                for (RoutineExercise exercise : routine.getExerciseListForDay(week, day)) {
                    final SentExercise sentExercise = new SentExercise(exercise,
                        user.getOwnedExercises().get(exercise.getExerciseId()).getExerciseName());
                    sentDay.put(sortVal, sentExercise);
                    sortVal++;
                }
                sentWeek.put(day, sentDay);
            }
            sentRoutine.putWeek(week, sentWeek);
        }
        return sentRoutine;
    }

    public Map<String, AttributeValue> asItemAttributes() {
        final Map<String, AttributeValue> workoutToSendItemValues = new HashMap<>();
        workoutToSendItemValues.putIfAbsent(WORKOUT_NAME, new AttributeValue(this.workoutName));
        workoutToSendItemValues.putIfAbsent(SENT_WORKOUT_ID, new AttributeValue(this.sentWorkoutId));
        workoutToSendItemValues.putIfAbsent(CREATOR, new AttributeValue(this.creator));
        workoutToSendItemValues.putIfAbsent(ROUTINE, new AttributeValue()
            .withM(AttributeValueHelper.convertMapToAttributeValueMap(this.routine.asMap())));
        return workoutToSendItemValues;
    }

    @Override
    public Map<String, Object> asMap() {
        HashMap<String, Object> retVal = new HashMap<>();
        retVal.putIfAbsent(WORKOUT_NAME, this.workoutName);
        retVal.putIfAbsent(SENT_WORKOUT_ID, this.sentWorkoutId);
        retVal.putIfAbsent(CREATOR, this.creator);
        retVal.putIfAbsent(ROUTINE, this.routine.asMap());
        return retVal;
    }

    @Override
    public Map<String, Object> asResponse() {
        return this.asMap();
    }
}
