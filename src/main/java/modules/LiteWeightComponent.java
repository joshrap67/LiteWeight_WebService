package modules;

import controllers.GetUserDataController;
import controllers.NewUserController;
import dagger.Component;
import javax.inject.Singleton;

@Singleton
@Component(modules = LiteWeightModule.class)
public interface LiteWeightComponent {

  void inject(GetUserDataController getUserDataController);

  void inject(NewUserController newUserController);
}