package daos;

import utils.UpdateItemTemplate;
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
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsResult;
import exceptions.InvalidAttributeException;
import exceptions.UserNotFoundException;
import imports.Config;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import models.User;

public class UserDAO {

    public static final String USERS_TABLE_NAME = "users";
    public static final String USERS_PRIMARY_KEY = User.USERNAME;

    protected final Table usersTable;
    private final Database database;

    @Inject
    public UserDAO(final Database database) {
        AmazonDynamoDBClient client = (AmazonDynamoDBClient) AmazonDynamoDBClient.builder()
            .withRegion(Config.REGION)
            .withCredentials(new EnvironmentVariableCredentialsProvider())
            .build();
        final DynamoDB dynamoDb = new DynamoDB(client);

        this.database = database;
        this.usersTable = dynamoDb.getTable(USERS_TABLE_NAME);
    }

    public PutItemOutcome putUser(final Item user) {
        return this.usersTable.putItem(user);
    }

    public User getUser(final String username)
        throws NullPointerException, InvalidAttributeException, UserNotFoundException {
        final Item userItem = Optional.ofNullable(this.getUserItem(username))
            .orElseThrow(
                () -> new UserNotFoundException(String.format("User \"%s\" not found", username)));
        return new User(userItem);
    }

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

    public TransactWriteItemsResult executeWriteTransaction(final List<TransactWriteItem> actions) {
        return this.database.executeWriteTransaction(actions);
    }

    // for cold start mitigation
    public List<TableDescription> describeUserTable() {
        final ArrayList<TableDescription> descriptions = new ArrayList<>();
        descriptions.add(this.usersTable.describe());
        return descriptions;
    }

    public static String getKeyIndex(final String tableName) throws Exception {
        if (tableName.equals(USERS_TABLE_NAME)) {
            return USERS_PRIMARY_KEY;
        } else {
            throw new Exception("Invalid table name: " + tableName);
        }
    }
}
