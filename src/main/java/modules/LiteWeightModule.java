package modules;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import aws.DatabaseAccess;
import helpers.Metrics;
import managers.GetUserDataManager;

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
}
