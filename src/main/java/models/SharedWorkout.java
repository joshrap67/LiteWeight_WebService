package models;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import exceptions.InvalidAttributeException;
import utils.AttributeValueUtils;
import interfaces.Model;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SharedWorkout implements Model {

    public static final String SHARED_WORKOUT_ID = "sharedWorkoutId";
    public static final String WORKOUT_NAME = "workoutName";
    public static final String CREATOR = "creator";
    public static final String ROUTINE = "routine";
    public static final String EXERCISES = "exercises";

    private String sharedWorkoutId;
    private String workoutName;
    private String creator;
    private SharedRoutine routine;
    private Map<String, SharedWorkoutExercise> exercises;

    public SharedWorkout(final Item userItem)
        throws InvalidAttributeException {
        this(userItem.asMap());
    }

    public SharedWorkout(Map<String, Object> json) throws InvalidAttributeException {
        this.sharedWorkoutId = (String) json.get(SHARED_WORKOUT_ID);
        this.workoutName = (String) json.get(WORKOUT_NAME);
        this.creator = (String) json.get(CREATOR);
        this.setExercises((Map<String, Object>) json.get(EXERCISES));
        this.routine = new SharedRoutine((Map<String, Object>) json.get(ROUTINE));
    }

    public SharedWorkout(final Workout workout, final User user, final String sharedWorkoutId) {
        this.sharedWorkoutId = sharedWorkoutId;
        this.workoutName = workout.getWorkoutName();
        this.creator = workout.getCreator();
        this.routine = new SharedRoutine(workout.getRoutine(), user.getOwnedExercises());
        // preserve the focuses and video url of the exercises
        this.exercises = new HashMap<>();
        Set<String> exerciseNames = new HashSet<>();
        Map<String, String> exerciseNameToId = new HashMap<>();
        for (RoutineWeek week : workout.getRoutine()) {
            for (RoutineDay day : week) {
                for (RoutineExercise exercise : day) {
                    String exerciseName = user.getOwnedExercises().get(exercise.getExerciseId()).getExerciseName();
                    exerciseNames.add(exerciseName);
                    exerciseNameToId.putIfAbsent(exerciseName, exercise.getExerciseId());
                }
            }
        }
        for (String exerciseName : exerciseNames) {
            this.exercises.putIfAbsent(exerciseName,
                new SharedWorkoutExercise(user.getOwnedExercises().get(exerciseNameToId.get(exerciseName))));
        }
    }

    public Map<String, AttributeValue> asItemAttributes() {
        final Map<String, AttributeValue> workoutToSendItemValues = new HashMap<>();
        workoutToSendItemValues.putIfAbsent(WORKOUT_NAME, new AttributeValue(this.workoutName));
        workoutToSendItemValues.putIfAbsent(SHARED_WORKOUT_ID, new AttributeValue(this.sharedWorkoutId));
        workoutToSendItemValues.putIfAbsent(CREATOR, new AttributeValue(this.creator));
        workoutToSendItemValues.putIfAbsent(ROUTINE,
            new AttributeValue().withM(AttributeValueUtils.convertMapToAttributeValueMap(this.routine.asMap())));
        workoutToSendItemValues.putIfAbsent(EXERCISES,
            new AttributeValue().withM(AttributeValueUtils.convertMapToAttributeValueMap(getExercisesMap())));
        return workoutToSendItemValues;
    }

    public void setExercises(Map<String, Object> json) {
        if (json == null) {
            this.exercises = null;
        } else {
            this.exercises = new HashMap<>();
            for (String exerciseName : json.keySet()) {
                this.exercises.putIfAbsent(exerciseName,
                    new SharedWorkoutExercise((Map<String, Object>) json.get(exerciseName)));
            }
        }
    }

    @Override
    public Map<String, Object> asMap() {
        HashMap<String, Object> retVal = new HashMap<>();
        retVal.putIfAbsent(WORKOUT_NAME, this.workoutName);
        retVal.putIfAbsent(SHARED_WORKOUT_ID, this.sharedWorkoutId);
        retVal.putIfAbsent(CREATOR, this.creator);
        retVal.putIfAbsent(ROUTINE, this.routine.asMap());
        retVal.putIfAbsent(EXERCISES, this.getExercisesMap());
        return retVal;
    }

    public Map<String, Object> getExercisesMap() {
        if (this.exercises == null) {
            return null;
        }

        return this.exercises.entrySet().stream().collect(
            collectingAndThen(
                toMap(Entry::getKey, (Map.Entry<String, SharedWorkoutExercise> e) -> e.getValue().asMap()),
                HashMap::new));
    }

    @Override
    public Map<String, Object> asResponse() {
        return this.asMap();
    }
}
