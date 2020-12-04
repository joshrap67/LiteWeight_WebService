package modules;

import services.StorageService;
import services.NotificationService;
import dagger.Module;
import dagger.Provides;

import daos.Database;
import daos.UserDAO;
import daos.WorkoutDAO;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import utils.Metrics;

@Module
@RequiredArgsConstructor
public class LiteWeightModule {

    private final Metrics metrics;

    @Provides
    @Singleton
    public UserDAO provideUserDAO(final Database database) {
        return new UserDAO(database);
    }

    @Provides
    @Singleton
    public WorkoutDAO provideWorkoutDAO(final Database database) {
        return new WorkoutDAO(database);
    }

    @Provides
    @Singleton
    public StorageService provideS3Access() {
        return new StorageService();
    }

    @Provides
    @Singleton
    public NotificationService provideSnsAccess() {
        return new NotificationService();
    }

    @Provides
    public Metrics provideMetrics() {
        return this.metrics;
    }
}
