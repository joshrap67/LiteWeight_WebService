package interfaces;

import java.util.Map;

public interface Model {

    Map<String, Object> asMap();

    Map<String, Object> asResponse();
}
