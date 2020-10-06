package modules;

import controllers.AcceptFriendRequestController;
import controllers.BlockUserController;
import controllers.CancelFriendRequestController;
import controllers.CopyWorkoutController;
import controllers.DeclineFriendRequestController;
import controllers.DeleteExerciseController;
import controllers.DeleteWorkoutThenFetchController;
import controllers.EditWorkoutController;
import controllers.GetUserDataController;
import controllers.GetUserWorkoutController;
import controllers.NewExerciseController;
import controllers.NewUserController;
import controllers.NewWorkoutController;
import controllers.RegisterEndpointTokenController;
import controllers.RemoveEndpointTokenController;
import controllers.RemoveFriendController;
import controllers.RenameWorkoutController;
import controllers.ResetWorkoutStatisticsController;
import controllers.RestartWorkoutController;
import controllers.SendFriendRequestController;
import controllers.SendWorkoutController;
import controllers.SetAllRequestsSeenController;
import controllers.SwitchWorkoutController;
import controllers.SyncWorkoutController;
import controllers.UnblockUserController;
import controllers.UpdateExerciseController;
import controllers.UpdateIconController;
import controllers.UpdateUserPreferencesController;
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

    void inject(DeleteWorkoutThenFetchController deleteWorkoutThenFetchController);

    void inject(ResetWorkoutStatisticsController resetWorkoutStatisticsController);

    void inject(EditWorkoutController editWorkoutController);

    void inject(UpdateExerciseController updateExerciseController);

    void inject(NewExerciseController newExerciseController);

    void inject(SyncWorkoutController syncWorkoutController);

    void inject(RestartWorkoutController restartWorkoutController);

    void inject(DeleteExerciseController deleteExerciseController);

    void inject(UpdateIconController updateIconController);

    void inject(RegisterEndpointTokenController registerEndpointTokenController);

    void inject(RemoveEndpointTokenController removeEndpointTokenController);

    void inject(SendFriendRequestController sendFriendRequestController);

    void inject(CancelFriendRequestController cancelFriendRequestController);

    void inject(SetAllRequestsSeenController setAllRequestsSeenController);

    void inject(UpdateUserPreferencesController updateUserPreferencesController);

    void inject(AcceptFriendRequestController acceptFriendRequestController);

    void inject(RemoveFriendController removeFriendController);

    void inject(DeclineFriendRequestController declineFriendRequestController);

    void inject(BlockUserController blockUserController);

    void inject(UnblockUserController unblockUserController);

    void inject(SendWorkoutController sendWorkoutController);
}