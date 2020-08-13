package modules;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import aws.DatabaseAccess;
import helpers.Metrics;
import managers.CopyWorkoutManager;
import managers.GetUserDataManager;
import managers.GetUserWorkoutManager;
import managers.NewUserManager;
import managers.NewWorkoutManager;
import managers.SwitchWorkoutManager;
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
    public WarmingManager provideWarmingHandler(final DatabaseAccess databaseAccess) {
        return new WarmingManager(databaseAccess, this.metrics);
    }

    @Provides
    public GetUserWorkoutManager provideGetUserWorkoutManager(final DatabaseAccess databaseAccess) {
        return new GetUserWorkoutManager(databaseAccess, this.metrics);
    }

    @Provides
    public SwitchWorkoutManager provideSwitchWorkoutManager(final DatabaseAccess databaseAccess) {
        return new SwitchWorkoutManager(databaseAccess, this.metrics);
    }

    @Provides
    public CopyWorkoutManager provideCopyWorkoutManager(final DatabaseAccess databaseAccess) {
        return new CopyWorkoutManager(databaseAccess, this.metrics);
    }
}
