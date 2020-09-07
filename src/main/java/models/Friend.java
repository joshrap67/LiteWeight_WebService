package models;

import interfaces.Model;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class Friend implements Model {

    public static final String CONFIRMED = "confirmed";

    private String icon;
    private boolean confirmed;

    public Friend(Map<String, Object> json) {
        this.icon = (String) json.get(User.ICON);
        this.confirmed = (boolean) json.get(CONFIRMED);
    }

    public Friend(User user) {
        this.icon = user.getIcon();
        this.confirmed = false;
    }

    @Override
    public Map<String, Object> asMap() {
        HashMap<String, Object> retVal = new HashMap<>();
        retVal.putIfAbsent(User.ICON, this.icon);
        retVal.putIfAbsent(CONFIRMED, this.confirmed);
        return retVal;
    }
}
