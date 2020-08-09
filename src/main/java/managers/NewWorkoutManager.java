package managers;

import aws.DatabaseAccess;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import helpers.AttributeValueHelper;
import helpers.ErrorMessage;
import helpers.FileReader;
import helpers.Globals;
import helpers.JsonHelper;
import helpers.Metrics;
import helpers.ResultStatus;
import helpers.UpdateItemData;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import javax.inject.Inject;
import models.ExerciseRoutine;
import models.Routine;
import models.User;
import models.Workout;
import models.WorkoutUser;
import responses.UserWithWorkout;

public class NewWorkoutManager {

    private final DatabaseAccess databaseAccess;
    private final Metrics metrics;

    @Inject
    public NewWorkoutManager(DatabaseAccess databaseAccess, Metrics metrics) {
        this.databaseAccess = databaseAccess;
        this.metrics = metrics;
    }

    /**
     * @param workoutName TODO
     * @return Result status that will be sent to frontend with appropriate data or error messages.
     */
    public ResultStatus<String> execute(final String workoutName, final String activeUser,
        final Map<String, Object> routineMap) {
        final String classMethod = "NewWorkoutManager.execute";
        this.metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;

        try {
            User user = this.databaseAccess.getUser(activeUser);
            if (user != null) {
                final String workoutId = UUID.randomUUID().toString();
                final String creationTime = Instant.now().toString();
                final Routine routine = new Routine(routineMap);
                String errorMessage = validNewWorkoutInput(workoutName, user, routine);

                if (errorMessage == null) {
                    // no error, so go ahead and try and insert this new workout along with updating active user
                    final Workout newWorkout = new Workout();
                    newWorkout.setCreationDate(creationTime);
                    newWorkout.setCreator(activeUser);
                    newWorkout.setMostFrequentFocus(findMostFrequentFocus(user, routine));
                    newWorkout.setWorkoutId(workoutId);
                    newWorkout.setWorkoutName(workoutName.trim());
                    newWorkout.setRoutine(routine);
                    newWorkout.setCurrentDay(0);
                    newWorkout.setCurrentWeek(0);

                    final WorkoutUser workoutUser = new WorkoutUser();
                    workoutUser.setWorkoutName(workoutName.trim());
                    workoutUser.setAverageExercisesCompleted(0.0);
                    workoutUser.setDateLast(creationTime);
                    workoutUser.setTimesCompleted(0);
                    workoutUser.setTotalExercisesSum(0);

                    // update all the exercises that are now apart of this workout
                    updateUserExercises(user, routine, workoutId, workoutName);

                    final UpdateItemData updateItemData = new UpdateItemData(activeUser,
                        DatabaseAccess.USERS_TABLE_NAME)
                        .withUpdateExpression(
                            "set " +
                                User.CURRENT_WORKOUT + " = :" + User.CURRENT_WORKOUT + ", " +
                                User.WORKOUTS + ".#workoutId= :" + User.WORKOUTS + ", " +
                                User.EXERCISES + "= :" + User.EXERCISES)
                        .withValueMap(
                            new ValueMap()
                                .withString(":" + User.CURRENT_WORKOUT, workoutId)
                                .withMap(":" + User.WORKOUTS, workoutUser.asMap())
                                .withMap(":" + User.EXERCISES, user.getUserExercisesMap()))
                        .withNameMap(new NameMap()
                            .with("#workoutId", workoutId));

                    // want a transaction since more than one object is being updated at once
                    final List<TransactWriteItem> actions = new ArrayList<>();
                    actions.add(new TransactWriteItem().withUpdate(updateItemData.asUpdate()));
                    actions.add(new TransactWriteItem()
                        .withPut(
                            new Put().withTableName(DatabaseAccess.WORKOUT_TABLE_NAME).withItem(
                                AttributeValueHelper
                                    .convertMapToAttributeValueMap(newWorkout.asMap()))));

                    this.databaseAccess.executeWriteTransaction(actions);

                    resultStatus = ResultStatus
                        .successful(
                            JsonHelper.convertObjectToJson(
                                new UserWithWorkout(user, newWorkout).asMap()));
                } else {
                    this.metrics.log("Input error: " + errorMessage);
                    resultStatus = ResultStatus.failure(errorMessage);
                }

            } else {
                this.metrics.log("Active user does not exist");
                resultStatus = ResultStatus.failure("User does not exist.");
            }
        } catch (Exception e) {
            this.metrics.logWithBody(new ErrorMessage<>(classMethod, e));
            resultStatus = ResultStatus.failure("Exception in " + classMethod + ". " + e);
        }

        this.metrics.commonClose(resultStatus.success);
        return resultStatus;
    }

    private void updateUserExercises(User user, Routine routine, String workoutId,
        String workoutName) {
        // updates the list of exercises on the user object to include this new workout in all contained exercises
        // get a list of all exercises (by id, not name of course)
        Set<String> exercises = new HashSet<>();
        for (Integer week : routine.getRoutine().keySet()) {
            for (Integer day : routine.getRoutine().get(week).keySet()) {
                List<ExerciseRoutine> exerciseListForDay = routine
                    .getExerciseListForDay(week, day);
                for (ExerciseRoutine exerciseRoutine : exerciseListForDay) {
                    String exerciseId = exerciseRoutine.getExerciseId();
                    exercises.add(exerciseId);
                }
            }
        }

        for (String exerciseId : exercises) {
            user.getUserExercises().get(exerciseId).getWorkouts()
                .putIfAbsent(workoutId, workoutName.trim());
        }
    }

    private String validNewWorkoutInput(final String workoutName, final User activeUser,
        final Routine routine) {

        StringBuilder error = new StringBuilder();
        if (activeUser.getUserWorkouts().size() > Globals.MAX_FREE_WORKOUTS
            && activeUser.getPremiumToken() != null) {
            // TODO need to actually verify that token is good?
            error.append("Max amount of free workouts reached.\n");
        }
        if (workoutName.length() > Globals.MAX_WORKOUT_NAME_LENGTH) {
            error.append("Workout name is too long.\n");
        }
        boolean repeat = false;
        for (String workoutId : activeUser.getUserWorkouts().keySet()) {
            if (activeUser.getUserWorkouts().get(workoutId).getWorkoutName()
                .equals(workoutName.trim())) {
                repeat = true;
                break;
            }
        }
        if (repeat) {
            error.append("Workout name already exists.\n");
        }

        if (routine.getRoutine().keySet().size() > Globals.MAX_WEEKS_ROUTINE) {
            error.append("Workout exceeds maximum amount of weeks allowed.");
        }

        for (Integer week : routine.getRoutine().keySet()) {
            int dayCount = routine.getRoutine().get(week).keySet().size();
            if (dayCount > Globals.MAX_DAYS_ROUTINE) {
                error.append("Week: ").append(week)
                    .append(" exceeds maximum amount of days in a week.");
            }
        }

        return ((error.length() == 0) ? null : error.toString().trim());
    }

    public static String findMostFrequentFocus(User user,
        Routine routine) {

        Map<String, Integer> focusCount = new HashMap<>();
        for (Integer week : routine.getRoutine().keySet()) {
            for (Integer day : routine.getRoutine().get(week).keySet()) {
                List<ExerciseRoutine> exerciseListForDay = routine
                    .getExerciseListForDay(week, day);
                for (ExerciseRoutine exerciseRoutine : exerciseListForDay) {
                    String exerciseId = exerciseRoutine.getExerciseId();
                    for (String focus : user.getUserExercises().get(exerciseId).getFocuses()
                        .keySet()) {
                        focusCount.merge(focus, 1, Integer::sum);
                    }
                }
            }
        }

        StringJoiner retVal = new StringJoiner(FileReader.FOCUS_DELIM, "", "");
        int max = 0;
        for (String focus : focusCount.keySet()) {
            int count = focusCount.get(focus);
            if (count > max) {
                max = count;
            }
        }
        for (String focus : focusCount.keySet()) {
            int count = focusCount.get(focus);
            if (count == max) {
                retVal.add(focus);
            }
        }
        return retVal.toString();
    }
}
