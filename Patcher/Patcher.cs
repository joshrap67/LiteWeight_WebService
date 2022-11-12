using Amazon;
using Amazon.DynamoDBv2;
using Amazon.DynamoDBv2.DataModel;
using Amazon.Runtime.CredentialManagement;

namespace Patcher;

public abstract class Patcher
{
    public abstract Task Patch();

    protected readonly IAmazonDynamoDB DynamoDBClient;
    protected readonly DynamoDBContext Context;

    public Patcher()
    {
        var chain = new CredentialProfileStoreChain();
        chain.TryGetAWSCredentials("josh", out var credentials);

        DynamoDBClient = new AmazonDynamoDBClient(credentials, RegionEndpoint.USEast1);
        Context = new DynamoDBContext(DynamoDBClient, new DynamoDBContextConfig { IsEmptyStringValueEnabled = true });
    }
}
