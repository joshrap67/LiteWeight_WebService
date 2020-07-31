package helpers;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class AttributeValueHelper {

    public static AttributeValue convertObjectToAttributeValue(final Object value) {
        AttributeValue attributeValue;

        if (value instanceof Map) {
            attributeValue = new AttributeValue()
                .withM(AttributeValueHelper.convertMapToAttributeValueMap((Map) value));
        } else if (value instanceof String) {
            attributeValue = new AttributeValue().withS((String) value);
        } else if (value instanceof Iterable) {
            attributeValue = new AttributeValue()
                .withL(AttributeValueHelper.convertIterableToAttributeValueList((Iterable) value));
        } else if (value instanceof Number) {
            attributeValue = new AttributeValue().withN(value.toString());
        } else if (value instanceof Boolean) {
            attributeValue = new AttributeValue().withBOOL((Boolean) value);
        } else {
            attributeValue = new AttributeValue().withNULL(true);
        }

        return attributeValue;
    }

    public static Map<String, AttributeValue> convertMapToAttributeValueMap(
        Map<String, Object> value) {
        return value.entrySet().stream().collect(
            Collectors.collectingAndThen(
                Collectors
                    .toMap((Map.Entry e) -> (String) e.getKey(),
                        (Map.Entry e) -> AttributeValueHelper
                            .convertObjectToAttributeValue(e.getValue())),
                HashMap::new
            ));
    }

    public static List<AttributeValue> convertIterableToAttributeValueList(Iterable value) {
        return (List<AttributeValue>) StreamSupport.stream(value.spliterator(), false)
            .map(o -> AttributeValueHelper.convertObjectToAttributeValue(o)).collect(
                Collectors.toList());
    }

    public static String convertStringToJson(String value) {
        return "\\\"" + value + "\\\"";
    }

    //endregion
}