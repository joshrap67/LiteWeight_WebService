package daos;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.TransactGetItemsRequest;
import com.amazonaws.services.dynamodbv2.model.TransactGetItemsResult;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsResult;
import imports.Config;
import java.util.List;
import javax.inject.Inject;

public class Database {

    private final AmazonDynamoDBClient client;

    @Inject
    public Database() {
        this.client = (AmazonDynamoDBClient) AmazonDynamoDBClient.builder()
            .withRegion(Config.REGION)
            .withCredentials(new EnvironmentVariableCredentialsProvider())
            .build();
    }

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
        switch (tableName) {
            case WorkoutDAO.WORKOUT_TABLE_NAME:
                return WorkoutDAO.WORKOUT_TABLE_PRIMARY_KEY;
            case UserDAO.USERS_TABLE_NAME:
                return UserDAO.USERS_PRIMARY_KEY;
            case SharedWorkoutDAO.SENT_WORKOUT_TABLE_NAME:
                return SharedWorkoutDAO.SENT_WORKOUT_TABLE_PRIMARY_KEY;
            default:
                throw new Exception("Invalid table name: " + tableName);
        }
    }
}
