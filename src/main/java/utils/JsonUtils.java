package utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;

public class JsonUtils {

    public static String serializeMap(Map<String, Object> map) {
        String retVal;
        ObjectMapper mapper = new ObjectMapper();

        try {
            retVal = mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            retVal = "Error";
        }
        return retVal;
    }

    public static Map<String, Object> deserialize(String jsonString) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonString, new TypeReference<>() {});
    }

    public static byte[] deserializeByteList(String jsonString) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonString, byte[].class);
    }
}
