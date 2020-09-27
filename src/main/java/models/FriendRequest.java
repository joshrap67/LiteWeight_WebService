package models;

import interfaces.Model;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class FriendRequest implements Model {

    public static final String ICON = "icon";
    public static final String SEEN = "seen";
    public static final String REQUEST_TIME_STAMP = "timeStamp";

    private String icon;
    private boolean seen;
    private String requestTimeStamp;

    public FriendRequest(Map<String, Object> jsonMap) {
        this.setIcon((String) jsonMap.get(ICON));
        this.setSeen((Boolean) jsonMap.get(SEEN));
        this.setRequestTimeStamp((String) jsonMap.get(REQUEST_TIME_STAMP));
    }

    public FriendRequest(User user, String timestamp) {
        this.setIcon(user.getIcon());
        this.setRequestTimeStamp(timestamp);
        this.setSeen(false);
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> retVal = new HashMap<>();
        retVal.putIfAbsent(ICON, this.getIcon());
        retVal.putIfAbsent(SEEN, this.isSeen());
        retVal.putIfAbsent(REQUEST_TIME_STAMP, this.getRequestTimeStamp());
        return retVal;
    }

    @Override
    public Map<String, Object> asResponse() {
        return this.asMap();
    }
}
