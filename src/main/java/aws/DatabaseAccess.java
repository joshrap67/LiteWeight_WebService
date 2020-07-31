package aws;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
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
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsResult;
import exceptions.InvalidAttributeException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import models.User;
import models.Workout;

public class DatabaseAccess {

    public static final String USERS_TABLE_NAME = "users";
    public static final String WORKOUT_TABLE_NAME = "workouts";

    public static final String USERS_PRIMARY_KEY = User.USERNAME;
    public static final String WORKOUT_TABLE_KEY = Workout.WORKOUT_ID;

    protected final Table workoutTable;
    protected final Table usersTable;

    private final AmazonDynamoDBClient client;

    private final HashMap<String, Item> cache;

    public DatabaseAccess() {
        final Regions region = Regions.US_EAST_2;
        this.client = (AmazonDynamoDBClient) AmazonDynamoDBClient.builder()
            .withRegion(region)
            .withCredentials(new EnvironmentVariableCredentialsProvider())
            .build();
        final DynamoDB dynamoDb = new DynamoDB(this.client);

        this.workoutTable = dynamoDb.getTable(WORKOUT_TABLE_NAME);
        this.usersTable = dynamoDb.getTable(USERS_TABLE_NAME);
        this.cache = new HashMap<>();
    }

    // Workout table methods

    public PutItemOutcome putWorkout(final Item workout) {
        return this.workoutTable.putItem(workout);
    }

    // Users table methods
    public PutItemOutcome putUser(final Item user) {
        return this.usersTable.putItem(user);
    }

    public User getUserNoCache(final String username)
        throws InvalidAttributeException {
        final Item userItem = this.usersTable.getItem(new PrimaryKey(USERS_PRIMARY_KEY, username));
        this.cache.put(username, userItem);

        return new User(userItem);
    }

    public User getUser(final String username)
        throws NullPointerException, InvalidAttributeException {
        Item userItem;
        if (this.cache.containsKey(username)) {
            userItem = this.cache.get(username);
        } else {
            userItem = this.usersTable.getItem(new PrimaryKey(USERS_PRIMARY_KEY, username));
            this.cache.put(username, userItem);
        }

        return new User(userItem);
    }

    public Item getUserItem(final String username) throws NullPointerException {
        Item userItem;
        if (this.cache.containsKey(username)) {
            userItem = this.cache.get(username);
        } else {
            userItem = this.usersTable.getItem(new PrimaryKey(USERS_PRIMARY_KEY, username));
            this.cache.put(username, userItem);
        }

        return userItem;
    }

    public UpdateItemOutcome updateUser(final String username,
        final UpdateItemSpec updateItemSpec) {
        updateItemSpec.withPrimaryKey(USERS_PRIMARY_KEY, username);
        return this.usersTable.updateItem(updateItemSpec);
    }

    public UpdateItemOutcome updateUser(final UpdateItemTemplate updateItemData) throws Exception {
        return this.usersTable.updateItem(updateItemData.asUpdateItemSpec());
    }


    //for cold start mitigation
    public List<TableDescription> describeTables() {
        final ArrayList<TableDescription> descriptions = new ArrayList<>();
        descriptions.add(this.workoutTable.describe());
        descriptions.add(this.usersTable.describe());
        return descriptions;
    }

    //transactions
    public TransactWriteItemsResult executeWriteTransaction(final List<TransactWriteItem> actions) {
        final TransactWriteItemsRequest transactWriteItemsRequest = new TransactWriteItemsRequest()
            .withTransactItems(actions);
        return this.client.transactWriteItems(transactWriteItemsRequest);
    }

    public TransactGetItemsResult executeGetTransaction(
        final TransactGetItemsRequest transactGetItemsRequest) {
        return this.client.transactGetItems(transactGetItemsRequest);
    }

    public static String getKeyIndex(final String tableName) throws Exception {
        if (tableName.equals(DatabaseAccess.WORKOUT_TABLE_NAME)) {
            return WORKOUT_TABLE_KEY;
        } else if (tableName.equals(DatabaseAccess.USERS_TABLE_NAME)) {
            return USERS_PRIMARY_KEY;
        } else {
            throw new Exception("Invalid table name: " + tableName);
        }
    }
}
