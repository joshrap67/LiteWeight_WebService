package responses;

import interfaces.Model;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import models.Friend;
import models.User;

@Data
public class FriendResponse implements Model {

    private String icon;
    private String username;
    private boolean confirmed;

    public FriendResponse(Friend friend, String username) {
        this.icon = friend.getIcon();
        this.confirmed = friend.isConfirmed();
        this.username = username;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> retVal = new HashMap<>();
        retVal.putIfAbsent(Friend.CONFIRMED, this.isConfirmed());
        retVal.putIfAbsent(User.USERNAME, this.getUsername());
        retVal.putIfAbsent(User.ICON, this.getIcon());
        return retVal;
    }

    @Override
    public Map<String, Object> asResponse() {
        return this.asMap();
    }
}
