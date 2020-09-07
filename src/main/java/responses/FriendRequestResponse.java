package responses;

import interfaces.Model;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import models.FriendRequest;
import models.User;

@Data
public class FriendRequestResponse implements Model {

    private String icon;
    private String username;
    private boolean seen;
    private String requestTimeStamp;

    public FriendRequestResponse(FriendRequest friendRequest, String username) {
        this.setIcon(friendRequest.getIcon());
        this.setUsername(username);
        this.setSeen(friendRequest.isSeen());
        this.setRequestTimeStamp(friendRequest.getRequestTimeStamp());
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> retVal = new HashMap<>();
        retVal.putIfAbsent(User.ICON, this.getIcon());
        retVal.putIfAbsent(FriendRequest.SEEN, this.isSeen());
        retVal.putIfAbsent(User.USERNAME, this.getUsername());
        retVal.putIfAbsent(FriendRequest.REQUEST_TIME_STAMP, this.getRequestTimeStamp());
        return retVal;
    }
}
