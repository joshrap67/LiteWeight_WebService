package modules;

import controllers.CopyWorkoutController;
import controllers.DeleteExerciseController;
import controllers.DeleteWorkoutController;
import controllers.EditWorkoutController;
import controllers.GetUserDataController;
import controllers.GetUserWorkoutController;
import controllers.NewExerciseController;
import controllers.NewUserController;
import controllers.NewWorkoutController;
import controllers.RenameWorkoutController;
import controllers.ResetWorkoutStatisticsController;
import controllers.RestartWorkoutController;
import controllers.SwitchWorkoutController;
import controllers.SyncWorkoutController;
import controllers.UpdateExerciseController;
import controllers.WarmingController;
import dagger.Component;
import javax.inject.Singleton;

@Singleton
@Component(modules = LiteWeightModule.class)
public interface LiteWeightComponent {

    void inject(GetUserDataController getUserDataController);

    void inject(NewUserController newUserController);

    void inject(NewWorkoutController newWorkoutController);

    void inject(WarmingController warmingController);

    void inject(GetUserWorkoutController getUserWorkoutController);

    void inject(SwitchWorkoutController switchWorkoutController);

    void inject(CopyWorkoutController copyWorkoutController);

    void inject(RenameWorkoutController renameWorkoutController);

    void inject(DeleteWorkoutController deleteWorkoutController);

    void inject(ResetWorkoutStatisticsController resetWorkoutStatisticsController);

    void inject(EditWorkoutController editWorkoutController);

    void inject(UpdateExerciseController updateExerciseController);

    void inject(NewExerciseController newExerciseController);

    void inject(SyncWorkoutController syncWorkoutController);

    void inject(RestartWorkoutController restartWorkoutController);

    void inject(DeleteExerciseController deleteExerciseController);
}