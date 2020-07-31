package modules;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import aws.DatabaseAccess;
import helpers.Metrics;
import managers.GetUserDataManager;
import managers.NewUserManager;
import managers.NewWorkoutManager;
import managers.WarmingManager;

@Module
@RequiredArgsConstructor
public class LiteWeightModule {

    private final Metrics metrics;

    @Provides
    @Singleton
    public DatabaseAccess provideDbAccessManager() {
        return new DatabaseAccess();
    }

    @Provides
    public GetUserDataManager provideGetUserManager(final DatabaseAccess databaseAccess) {
        return new GetUserDataManager(databaseAccess, this.metrics);
    }

    @Provides
    public NewUserManager provideNewUserManager(final DatabaseAccess databaseAccess) {
        return new NewUserManager(databaseAccess, this.metrics);
    }

    @Provides
    public NewWorkoutManager provideNewWorkoutManager(final DatabaseAccess databaseAccess) {
        return new NewWorkoutManager(databaseAccess, this.metrics);
    }

    @Provides
    public WarmingManager provideWarmingHandler(final DatabaseAccess dbAccessManager) {
        return new WarmingManager(dbAccessManager, this.metrics);
    }
}
