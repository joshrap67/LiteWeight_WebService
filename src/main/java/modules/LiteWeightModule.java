package modules;

import aws.S3Access;
import aws.SnsAccess;
import dagger.Module;
import dagger.Provides;

import daos.Database;
import daos.UserDAO;
import daos.WorkoutDAO;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import helpers.Metrics;

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
    public S3Access provideS3Access() {
        return new S3Access();
    }

    @Provides
    @Singleton
    public SnsAccess provideSnsAccess() {
        return new SnsAccess();
    }

    @Provides
    public Metrics provideMetrics() {
        return this.metrics;
    }
}
