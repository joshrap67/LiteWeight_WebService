package managers;

import aws.SnsAccess;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import daos.SentWorkoutDAO;
import daos.UserDAO;
import daos.WorkoutDAO;
import exceptions.ManagerExecutionException;
import helpers.Globals;
import helpers.Metrics;
import helpers.UpdateItemData;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import models.NotificationData;
import models.ReceivedWorkoutMeta;
import models.SentWorkout;
import models.User;
import models.Workout;

public class SendWorkoutManager {

    private final SnsAccess snsAccess;
    private final UserDAO userDAO;
    private final WorkoutDAO workoutDAO;
    private final Metrics metrics;

    @Inject
    public SendWorkoutManager(final SnsAccess snsAccess, final UserDAO userDAO,
        final WorkoutDAO workoutDAO,
        final Metrics metrics) {
        this.snsAccess = snsAccess;
        this.userDAO = userDAO;
        this.workoutDAO = workoutDAO;
        this.metrics = metrics;
    }

    /**
     * Sends a workout to a recipient. Note that these workouts are separate objects with exercises
     * that are indexed by name and not by id.
     * <p>
     * If a workout has already been sent by the active user with the same name, the old sent
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
            String sentWorkoutId = null;
            int unseenCount = recipientUser.getUnseenReceivedWorkouts();

            for (String workoutIdMeta : recipientUser.getReceivedWorkouts().keySet()) {
                final ReceivedWorkoutMeta meta = recipientUser.getReceivedWorkouts()
                    .get(workoutIdMeta);
                if (meta.getWorkoutName().equals(originalWorkout.getWorkoutName()) && meta
                    .getSender().equals(activeUser)) {
                    // sender has already sent a workout with this name
                    sentWorkoutId = meta.getWorkoutId();
                    // workout with this name has already been sent, so we don't want a new notification if it is already unseen
                    if (meta.isSeen()) {
                        // workout with this name has already been sent and seen, make it unseen since workout updated
                        unseenCount++;
                    }
                    break;
                }
            }
            if (sentWorkoutId == null) {
                // this is the first time this workout has been sent by the active user with this workout name, so we need an id
                sentWorkoutId = UUID.randomUUID().toString();
                unseenCount++;
            }

            final ReceivedWorkoutMeta receivedWorkoutMeta = new ReceivedWorkoutMeta();
            receivedWorkoutMeta.setDateSent(Instant.now().toString());
            receivedWorkoutMeta.setSeen(false);
            receivedWorkoutMeta.setSender(activeUser);
            receivedWorkoutMeta.setMostFrequentFocus(originalWorkout.getMostFrequentFocus());
            receivedWorkoutMeta.setWorkoutName(originalWorkout.getWorkoutName());
            receivedWorkoutMeta.setTotalDays(originalWorkout.getRoutine().getTotalNumberOfDays());

            final SentWorkout workoutToSend = new SentWorkout(originalWorkout, activeUserObject,
                sentWorkoutId);
            final Map<String, AttributeValue> workoutToSendItemValues = workoutToSend
                .asItemAttributes();

            // if meta is already there with this workout name, we just overwrite it for recipient
            final UpdateItemData recipientItemData = new UpdateItemData(
                recipientUsername, UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("set "
                    + User.RECEIVED_WORKOUTS + ".#workoutId= :workoutMetaVal, "
                    + User.UNSEEN_RECEIVED_WORKOUTS + "= :unseenVal")
                .withValueMap(new ValueMap()
                    .withMap(":workoutMetaVal", receivedWorkoutMeta.asMap())
                    .withNumber(":unseenVal", unseenCount))
                .withNameMap(new NameMap().with("#workoutId", sentWorkoutId));
            // need to update the number of sent workouts for the active user
            final UpdateItemData activeUserItemData = new UpdateItemData(
                activeUser, UserDAO.USERS_TABLE_NAME)
                .withUpdateExpression("set " + User.WORKOUTS_SENT + "= :sentVal")
                .withValueMap(
                    new ValueMap().withNumber(":sentVal", activeUserObject.getWorkoutsSent() + 1));

            // want a transaction since more than one object is being updated at once
            final List<TransactWriteItem> actions = new ArrayList<>();
            actions.add(new TransactWriteItem().withUpdate(recipientItemData.asUpdate()));
            actions.add(new TransactWriteItem().withUpdate(activeUserItemData.asUpdate()));
            actions.add(new TransactWriteItem().withPut(new Put()
                .withItem(workoutToSendItemValues)
                .withTableName(SentWorkoutDAO.SENT_WORKOUT_TABLE_NAME)));

            this.userDAO.executeWriteTransaction(actions);
            // if this succeeds, go ahead and send a notification to the recipient with the workout meta
            this.snsAccess.sendMessage(recipientUser.getPushEndpointArn(),
                new NotificationData(SnsAccess.receivedWorkoutAction,
                    receivedWorkoutMeta.asResponse()));
            return sentWorkoutId;
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

        // todo have a timeout for number of workouts sendable in a given amount of time?
        return stringBuilder.toString().trim();
    }
}
