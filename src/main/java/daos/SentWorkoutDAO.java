package daos;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.TransactGetItemsRequest;
import com.amazonaws.services.dynamodbv2.model.TransactGetItemsResult;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsResult;
import exceptions.InvalidAttributeException;
import exceptions.WorkoutNotFoundException;
import helpers.Config;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import models.Workout;

public class SentWorkoutDAO {

    public static final String SENT_WORKOUT_TABLE_NAME = "sentWorkouts";

    public static final String SENT_WORKOUT_TABLE_PRIMARY_KEY = Workout.WORKOUT_ID;

    protected final Table sentWorkoutTable;
    private final Database database;

    @Inject
    public SentWorkoutDAO(final Database database) {
        AmazonDynamoDBClient client = (AmazonDynamoDBClient) AmazonDynamoDBClient.builder()
            .withRegion(Config.REGION)
            .withCredentials(new EnvironmentVariableCredentialsProvider())
            .build();
        final DynamoDB dynamoDb = new DynamoDB(client);

        this.database = database;
        this.sentWorkoutTable = dynamoDb.getTable(SENT_WORKOUT_TABLE_NAME);
    }

    public PutItemOutcome putWorkout(final Item workout) {
        return this.sentWorkoutTable.putItem(workout);
    }

    public Item getWorkoutItem(String currentWorkoutId) {
        return this.sentWorkoutTable
            .getItem(new PrimaryKey(SENT_WORKOUT_TABLE_PRIMARY_KEY, currentWorkoutId));
    }

    public Workout getWorkout(String currentWorkoutId)
        throws NullPointerException, InvalidAttributeException, WorkoutNotFoundException {
        final Item workoutItem = Optional.ofNullable(this.getWorkoutItem(currentWorkoutId))
            .orElseThrow(
                () -> new WorkoutNotFoundException(
                    String.format("Workout with ID: %s not found", currentWorkoutId)));
        return new Workout(workoutItem);
    }

    public UpdateItemOutcome updateWorkout(final String workoutId,
        final UpdateItemSpec updateItemSpec) {
        updateItemSpec.withPrimaryKey(SENT_WORKOUT_TABLE_PRIMARY_KEY, workoutId);
        return this.sentWorkoutTable.updateItem(updateItemSpec);
    }

    //transactions
    public TransactWriteItemsResult executeWriteTransaction(final List<TransactWriteItem> actions) {
        return this.database.executeWriteTransaction(actions);
    }

    public TransactGetItemsResult executeGetTransaction(
        final TransactGetItemsRequest transactGetItemsRequest) {
        return this.database.executeGetTransaction(transactGetItemsRequest);
    }

    //for cold start mitigation
    public List<TableDescription> describeSentWorkoutTable() {
        final ArrayList<TableDescription> descriptions = new ArrayList<>();
        descriptions.add(this.sentWorkoutTable.describe());
        return descriptions;
    }

    public static String getKeyIndex(final String tableName) throws Exception {
        if (tableName.equals(SENT_WORKOUT_TABLE_NAME)) {
            return SENT_WORKOUT_TABLE_PRIMARY_KEY;
        } else {
            throw new Exception("Invalid table name: " + tableName);
        }
    }
}
