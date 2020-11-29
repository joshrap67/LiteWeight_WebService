package managers;

import services.NotificationService;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import daos.SharedWorkoutDAO;
import daos.UserDAO;
import daos.WorkoutDAO;
import exceptions.ManagerExecutionException;
import imports.Globals;
import utils.Metrics;
import utils.UpdateItemData;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import models.NotificationData;
import models.SharedWorkoutMeta;
import models.SharedWorkout;
import models.User;
import models.Workout;

public class SendWorkoutManager {

    private final NotificationService notificationService;
    private final UserDAO userDAO;
    private final WorkoutDAO workoutDAO;
    private final Metrics metrics;

    @Inject
    public SendWorkoutManager(final NotificationService notificationService, final UserDAO userDAO,
        final WorkoutDAO workoutDAO,
        final Metrics metrics) {
        this.notificationService = notificationService;
        this.userDAO = userDAO;
        this.workoutDAO = workoutDAO;
        this.metrics = metrics;
    }

    /**
     * Sends a workout to a recipient. Note that these workouts are separate objects with exercises
     * that are indexed by name and not by id.
     * <p>
     * If a workout has already been sent by the active user with the same name, the old shared
     * workout is overwritten and updated.
     * <p>
     * A push notification is sent to the recipient upon successful creation of the sent workout.
     *
     * @param activeUser        user that is sending the friend request.
     * @param recipientUsername user that the active user is sending a workout to.
     * @param workoutId         workout id of the workout that is being sent to the recipient
     * @return id of the workout that was sent.
     * @throws Exception if either user does not exist or if there is any input error.
     */
    public String sendWorkout(final String activeUser, final String recipientUsername,
        final String workoutId)
        throws Exception {
        final String classMethod = this.getClass().getSimpleName() + ".sendWorkout";
        this.metrics.commonSetup(classMethod);

        try {
            final User activeUserObject = this.userDAO.getUser(activeUser);
            final User recipientUser = this.userDAO.getUser(recipientUsername);

            String errorMessage = validConditions(activeUserObject, recipientUser);
            if (!errorMessage.isEmpty()) {
                this.metrics.commonClose(false);
                throw new ManagerExecutionException(errorMessage);
            }

            final Workout originalWorkout = this.workoutDAO.getWorkout(workoutId);
            String sharedWorkoutId = null;

            for (String workoutIdMeta : recipientUser.getReceivedWorkouts().keySet()) {
                final SharedWorkoutMeta meta = recipientUser.getReceivedWorkouts()
                    .get(workoutIdMeta);
                if (meta.getWorkoutName().equals(originalWorkout.getWorkoutName()) && meta
                    .getSender().equals(activeUser)) {
                    // sender has already sent a workout with this name
                    sharedWorkoutId = meta.getWorkoutId();
                    break;
                }
            }
            if (sharedWorkoutId == null) {
                // this is the first time this workout has been sent by the active user with this workout name, so we need an id
                sharedWorkoutId = UUID.randomUUID().toString();
            }

            final SharedWorkoutMeta sharedWorkoutMeta = new SharedWorkoutMeta();
            sharedWorkoutMeta.setDateSent(Instant.now().toString());
            sharedWorkoutMeta.setWorkoutId(sharedWorkoutId);
            sharedWorkoutMeta.setSeen(false);
            sharedWorkoutMeta.setSender(activeUser);
            sharedWorkoutMeta.setMostFrequentFocus(originalWorkout.getMostFrequentFocus());
            sharedWorkoutMeta.setWorkoutName(originalWorkout.getWorkoutName());
            sharedWorkoutMeta.setTotalDays(originalWorkout.getRoutine().getTotalNumberOfDays());
            sharedWorkoutMeta.setIcon(activeUserObject.getIcon());

            final SharedWorkout workoutToSend = new SharedWorkout(originalWorkout, activeUserObject,
                sharedWorkoutId);
            final Map<String, AttributeValue> workoutToSendItemValues = workoutToSend
                .asItemAttributes();

            // if meta is already there with this workout name, we just overwrite it for recipient
            final UpdateItemData recipientItemData = new UpdateItemData(
                recipientUsername, UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("set "
                    + User.RECEIVED_WORKOUTS + ".#workoutId= :workoutMetaVal")
                .withValueMap(new ValueMap()
                    .withMap(":workoutMetaVal", sharedWorkoutMeta.asMap()))
                .withNameMap(new NameMap().with("#workoutId", sharedWorkoutId));
            // need to update the number of sent workouts for the active user
            final UpdateItemData activeUserItemData = new UpdateItemData(
                activeUser, UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("set " + User.WORKOUTS_SENT + "= :sentVal")
                .withValueMap(
                    new ValueMap().withNumber(":sentVal", activeUserObject.getWorkoutsSent() + 1));

            final List<TransactWriteItem> actions = new ArrayList<>();
            actions.add(new TransactWriteItem().withUpdate(recipientItemData.asUpdate()));
            actions.add(new TransactWriteItem().withUpdate(activeUserItemData.asUpdate()));
            actions.add(new TransactWriteItem().withPut(new Put()
                .withItem(workoutToSendItemValues)
                .withTableName(SharedWorkoutDAO.SHARED_WORKOUTS_TABLE_NAME)));

            this.userDAO.executeWriteTransaction(actions);
            // if this succeeds, go ahead and send a notification to the recipient with the workout meta
            this.notificationService.sendMessage(recipientUser.getPushEndpointArn(),
                new NotificationData(NotificationService.receivedWorkoutAction,
                    sharedWorkoutMeta.asResponse()));

            this.metrics.commonClose(true);
            return sharedWorkoutId;
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }

    private String validConditions(final User activeUser, final User otherUser) {
        StringBuilder stringBuilder = new StringBuilder();
        String activeUserUsername = activeUser.getUsername();
        String otherUserUsername = otherUser.getUsername();
        if (otherUser.getUserPreferences().isPrivateAccount() || otherUser.getBlocked()
            .containsKey(activeUserUsername)) {
            stringBuilder.append("Unable to send workout to: ").append(otherUserUsername)
                .append(".\n");
        }
        if (activeUser.getBlocked().containsKey(otherUserUsername)) {
            stringBuilder.append("You are currently blocking this user.\n");
        }
        if (otherUser.getReceivedWorkouts().size() >= Globals.MAX_RECEIVED_WORKOUTS) {
            stringBuilder.append(otherUserUsername).append(" has too many received workouts.\n")
                .append("\n");
        }
        if (activeUser.getWorkoutsSent() >= Globals.MAX_FREE_WORKOUTS_SENT) {
            stringBuilder.append(
                "You have reached the max number of workouts that you can send without premium.");
        }
        if (activeUserUsername.equals(otherUserUsername)) {
            stringBuilder.append("Cannot send workout to yourself.\n");
        }

        return stringBuilder.toString().trim();
    }
}
