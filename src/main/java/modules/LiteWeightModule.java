package modules;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import aws.DatabaseAccess;
import helpers.Metrics;
import managers.CopyWorkoutManager;
import managers.DeleteExerciseManager;
import managers.DeleteWorkoutManager;
import managers.EditWorkoutManager;
import managers.GetUserDataManager;
import managers.GetUserWorkoutManager;
import managers.NewExerciseManager;
import managers.NewUserManager;
import managers.NewWorkoutManager;
import managers.RenameWorkoutManager;
import managers.ResetWorkoutStatisticsManager;
import managers.RestartWorkoutManager;
import managers.SwitchWorkoutManager;
import managers.SyncWorkoutManager;
import managers.UpdateExerciseManager;
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

    @Provides
    public RenameWorkoutManager provideRenameWorkoutManager(final DatabaseAccess databaseAccess) {
        return new RenameWorkoutManager(databaseAccess, this.metrics);
    }

    @Provides
    public DeleteWorkoutManager provideDeleteWorkoutManager(final DatabaseAccess databaseAccess) {
        return new DeleteWorkoutManager(databaseAccess, this.metrics);
    }

    @Provides
    public ResetWorkoutStatisticsManager provideResetWorkoutStatisticsManager(
        final DatabaseAccess databaseAccess) {
        return new ResetWorkoutStatisticsManager(databaseAccess, this.metrics);
    }

    @Provides
    public EditWorkoutManager provideEditWorkoutManager(
        final DatabaseAccess databaseAccess) {
        return new EditWorkoutManager(databaseAccess, this.metrics);
    }

    @Provides
    public UpdateExerciseManager provideUpdateExerciseManager(
        final DatabaseAccess databaseAccess) {
        return new UpdateExerciseManager(databaseAccess, this.metrics);
    }

    @Provides
    public NewExerciseManager provideNewExerciseManager(
        final DatabaseAccess databaseAccess) {
        return new NewExerciseManager(databaseAccess, this.metrics);
    }

    @Provides
    public SyncWorkoutManager provideSyncWorkoutManager(
        final DatabaseAccess databaseAccess) {
        return new SyncWorkoutManager(databaseAccess, this.metrics);
    }

    @Provides
    public RestartWorkoutManager provideRestartWorkoutManager(
        final DatabaseAccess databaseAccess) {
        return new RestartWorkoutManager(databaseAccess, this.metrics);
    }

    @Provides
    public DeleteExerciseManager provideDeleteExerciseManager(
        final DatabaseAccess databaseAccess) {
        return new DeleteExerciseManager(databaseAccess, this.metrics);
    }
}
