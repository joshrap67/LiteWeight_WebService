package utils;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Delete;
import com.amazonaws.services.dynamodbv2.model.Update;
import daos.Database;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class UpdateItemTemplate {

    private final String keyValue;
    private final String tableName;

    private String updateExpression;
    private ValueMap valueMap;
    private NameMap nameMap;

    public UpdateItemTemplate(final String keyValue, final String tableName) {
        this.keyValue = keyValue;
        this.tableName = tableName;
    }

    public UpdateItemTemplate withUpdateExpression(final String updateExpression) {
        this.updateExpression = updateExpression;
        return this;
    }

    public UpdateItemTemplate withValueMap(final ValueMap valueMap) {
        this.valueMap = valueMap;
        return this;
    }

    public UpdateItemTemplate withNameMap(final NameMap nameMap) {
        this.nameMap = nameMap;
        return this;
    }

    public UpdateItemSpec asUpdateItemSpec() throws Exception {
        final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withPrimaryKey(Database.getKeyIndex(this.tableName), this.keyValue)
            .withUpdateExpression(this.updateExpression);

        if (this.valueMap != null) {
            updateItemSpec.withValueMap(this.valueMap);
        }

        if (this.nameMap != null) {
            updateItemSpec.withNameMap(this.nameMap);
        }

        return updateItemSpec;
    }

    public Update asUpdate() throws Exception {
        final Update update = new Update().withUpdateExpression(this.updateExpression).withTableName(this.tableName)
            .withKey(this.getKeyMap());

        if (this.valueMap != null) {
            for (final String key : this.valueMap.keySet()) {
                update.addExpressionAttributeValuesEntry(key,
                    AttributeValueUtils.convertObjectToAttributeValue(valueMap.get(key)));
            }
        }

        if (this.nameMap != null) {
            for (final Map.Entry entry : this.nameMap.entrySet()) {
                update.addExpressionAttributeNamesEntry(entry.getKey().toString(), entry.getValue().toString());
            }
        }

        return update;
    }

    public Delete asDelete() throws Exception {
        return new Delete().withTableName(this.tableName).withKey(this.getKeyMap());
    }

    private Map<String, AttributeValue> getKeyMap() throws Exception {
        final String keyIndex = Database.getKeyIndex(this.tableName);
        return new HashMap<>() {{
            put(keyIndex, new AttributeValue().withS(UpdateItemTemplate.this.keyValue));
        }};
    }
}