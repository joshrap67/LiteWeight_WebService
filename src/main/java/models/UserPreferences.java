package models;

import exceptions.InvalidAttributeException;
import interfaces.Model;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserPreferences implements Model {

    public static final String PRIVATE_ACCOUNT = "private";
    public static final String UPDATE_DEFAULT_WEIGHT_ON_SAVE = "updateDefaultWeightOnSave";
    public static final String UPDATE_DEFAULT_WEIGHT_ON_RESTART = "updateDefaultWeightOnRestart";
    public static final String METRIC = "metric";

    private boolean privateAccount;
    private boolean updateDefaultWeightOnSave;
    private boolean updateDefaultWeightOnRestart;
    private boolean metricUnits;

    public UserPreferences(Map<String, Object> json) {
        this.setPrivateAccount((Boolean) json.get(PRIVATE_ACCOUNT));
        this.setUpdateDefaultWeightOnRestart((Boolean) json.get(UPDATE_DEFAULT_WEIGHT_ON_RESTART));
        this.setUpdateDefaultWeightOnSave((Boolean) json.get(UPDATE_DEFAULT_WEIGHT_ON_SAVE));
        this.setMetricUnits((Boolean) json.get(METRIC));
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> retVal = new HashMap<>();
        retVal.putIfAbsent(PRIVATE_ACCOUNT, this.privateAccount);
        retVal.putIfAbsent(METRIC, this.metricUnits);
        retVal.putIfAbsent(UPDATE_DEFAULT_WEIGHT_ON_SAVE, this.updateDefaultWeightOnSave);
        retVal.putIfAbsent(UPDATE_DEFAULT_WEIGHT_ON_RESTART, this.updateDefaultWeightOnRestart);
        return retVal;
    }

    @Override
    public Map<String, Object> asResponse() {
        return this.asMap();
    }
}
