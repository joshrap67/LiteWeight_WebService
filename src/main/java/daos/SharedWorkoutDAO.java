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
import imports.Config;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import models.SharedWorkout;

public class SharedWorkoutDAO {

    public static final String SHARED_WORKOUTS_TABLE_NAME = "sharedWorkouts";
    public static final String SHARED_WORKOUTS_TABLE_PRIMARY_KEY = SharedWorkout.SHARED_WORKOUT_ID;
    protected final Table sharedWorkoutsTable;
    private final Database database;

    @Inject
    public SharedWorkoutDAO(final Database database) {
        AmazonDynamoDBClient client = (AmazonDynamoDBClient) AmazonDynamoDBClient.builder()
            .withRegion(Config.REGION)
            .withCredentials(new EnvironmentVariableCredentialsProvider())
            .build();
        final DynamoDB dynamoDb = new DynamoDB(client);

        this.database = database;
        this.sharedWorkoutsTable = dynamoDb.getTable(SHARED_WORKOUTS_TABLE_NAME);
    }

    public PutItemOutcome putSharedWorkout(final Item workout) {
        return this.sharedWorkoutsTable.putItem(workout);
    }

    private Item getSharedWorkoutItem(String currentWorkoutId) {
        return this.sharedWorkoutsTable.getItem(new PrimaryKey(SHARED_WORKOUTS_TABLE_PRIMARY_KEY, currentWorkoutId));
    }

    public SharedWorkout getSharedWorkout(String workoutId)
        throws NullPointerException, InvalidAttributeException, WorkoutNotFoundException {
        final Item workoutItem = Optional.ofNullable(this.getSharedWorkoutItem(workoutId))
            .orElseThrow(
                () -> new WorkoutNotFoundException(
                    String.format("Sent Workout with ID: %s not found", workoutId)));
        return new SharedWorkout(workoutItem);
    }

    public UpdateItemOutcome updateSharedWorkout(final String workoutId, final UpdateItemSpec updateItemSpec) {
        updateItemSpec.withPrimaryKey(SHARED_WORKOUTS_TABLE_PRIMARY_KEY, workoutId);
        return this.sharedWorkoutsTable.updateItem(updateItemSpec);
    }

    //transactions
    public TransactWriteItemsResult executeWriteTransaction(final List<TransactWriteItem> actions) {
        return this.database.executeWriteTransaction(actions);
    }

    public TransactGetItemsResult executeGetTransaction(final TransactGetItemsRequest transactGetItemsRequest) {
        return this.database.executeGetTransaction(transactGetItemsRequest);
    }

    //for cold start mitigation
    public List<TableDescription> describeSharedWorkoutsTable() {
        final ArrayList<TableDescription> descriptions = new ArrayList<>();
        descriptions.add(this.sharedWorkoutsTable.describe());
        return descriptions;
    }

    public static String getKeyIndex(final String tableName) throws Exception {
        if (tableName.equals(SHARED_WORKOUTS_TABLE_NAME)) {
            return SHARED_WORKOUTS_TABLE_PRIMARY_KEY;
        } else {
            throw new Exception("Invalid table name: " + tableName);
        }
    }
}
