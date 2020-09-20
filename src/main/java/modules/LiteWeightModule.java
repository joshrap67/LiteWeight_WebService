package modules;

import aws.S3Access;
import aws.SnsAccess;
import dagger.Module;
import dagger.Provides;

import daos.UserDAO;
import daos.WorkoutDAO;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import aws.DatabaseAccess;
import helpers.Metrics;

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
    @Singleton
    public UserDAO provideUserDAO() {
        return new UserDAO();
    }

    @Provides
    @Singleton
    public WorkoutDAO provideWorkoutDAO() {
        return new WorkoutDAO();
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
