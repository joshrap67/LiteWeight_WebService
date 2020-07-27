package helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class JsonHelper {

  public static String convertObjectToJson(Map<String, Object> object)
      throws JsonProcessingException {
    String retVal;
    ObjectMapper mapper = new ObjectMapper();

    try {
      retVal = mapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      retVal = "Error";
    }
    return retVal;

  }

  public static Map<String, Object> parseInput(InputStream inputStream) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {
    });
  }

  public static Map<String, Object> parseInput(String jsonString) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {
    });
  }
}
