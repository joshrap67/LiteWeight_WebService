package daos;

import aws.DatabaseAccess;
import aws.UpdateItemTemplate;
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
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsResult;
import exceptions.InvalidAttributeException;
import helpers.Config;
import java.util.ArrayList;
import java.util.List;
import models.User;

public class UserDAO {

    public static final String USERS_TABLE_NAME = "users";
    public static final String USERS_PRIMARY_KEY = User.USERNAME;
    protected final Table usersTable;
    private final AmazonDynamoDBClient client;

    public UserDAO() {
        this.client = (AmazonDynamoDBClient) AmazonDynamoDBClient.builder()
            .withRegion(Config.REGION)
            .withCredentials(new EnvironmentVariableCredentialsProvider())
            .build();
        final DynamoDB dynamoDb = new DynamoDB(this.client);

        this.usersTable = dynamoDb.getTable(USERS_TABLE_NAME);
    }

    // Users table methods
    public PutItemOutcome putUser(final Item user) {
        return this.usersTable.putItem(user);
    }

//    public User getUser(final String username)
//        throws NullPointerException, InvalidAttributeException {
//        return new User(this.usersTable.getItem(new PrimaryKey(USERS_PRIMARY_KEY, username)));
//    }

    public Item getUserItem(final String username) throws NullPointerException {
        return this.usersTable.getItem(new PrimaryKey(USERS_PRIMARY_KEY, username));
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
        if (tableName.equals(DatabaseAccess.USERS_TABLE_NAME)) {
            return USERS_PRIMARY_KEY;
        } else {
            throw new Exception("Invalid table name: " + tableName);
        }
    }
}
