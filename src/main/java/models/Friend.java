package models;

import interfaces.Model;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class Friend implements Model {
  private String icon;

  public Friend(Map<String, Object> json){
    this.icon = (String) json.get(User.ICON);
  }

  @Override
  public Map<String, Object> asMap() {
    HashMap<String, Object> retVal = new HashMap<>();
    retVal.putIfAbsent(User.ICON, this.icon);
    return retVal;
  }
}
