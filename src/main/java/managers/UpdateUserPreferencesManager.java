package managers;

import services.StorageService;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import daos.UserDAO;
import utils.Metrics;
import javax.inject.Inject;
import models.User;
import models.UserPreferences;

public class UpdateUserPreferencesManager {

    public final UserDAO userDAO;
    public final StorageService storageService;
    public final Metrics metrics;

    @Inject
    public UpdateUserPreferencesManager(final UserDAO userDAO, final Metrics metrics,
        final StorageService storageService) {
        this.userDAO = userDAO;
        this.storageService = storageService;
        this.metrics = metrics;
    }

    /**
     * Updates the preferences of the given user. Note that at this time it just overwrites the
     * values and there are no validation checks (since right now all values are either true or
     * false).
     *
     * @param activeUser username of the user that is updating their preferences.
     * @param userPrefs  the preferences to be updated.
     */
    public void updateUserPreferences(final String activeUser, final UserPreferences userPrefs) {
        final String classMethod = this.getClass().getSimpleName() + ".updateUserPreferences";
        this.metrics.commonSetup(classMethod);

        try {
            // right now just overwrite values in DB with these new ones
            UpdateItemSpec updateItemSpec = new UpdateItemSpec().withUpdateExpression(
                "set " + User.USER_PREFERENCES + " =:userPrefsVal")
                .withValueMap(new ValueMap().withMap(":userPrefsVal", userPrefs.asMap()));
            this.userDAO.updateUser(activeUser, updateItemSpec);

            this.metrics.commonClose(true);
        } catch (Exception e) {
            this.metrics.commonClose(false);
            throw e;
        }
    }
}
