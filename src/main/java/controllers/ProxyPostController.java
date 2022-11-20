package controllers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import utils.ErrorMessage;
import utils.JsonUtils;
import utils.Metrics;
import imports.RequestFields;
import imports.ResultStatus;
import utils.TokenUtils;
import utils.WarningMessage;
import interfaces.ApiRequestController;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class ProxyPostController implements
    RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    // all the different actions this web service has
    private static final Map<String, Class<? extends ApiRequestController>> ACTIONS_TO_CONTROLLERS = Maps
        .newHashMap(ImmutableMap.<String, Class<? extends ApiRequestController>>builder()
            .put("getUserData", GetUserDataController.class)
            .put("newUser", NewUserController.class)
            .put("newWorkout", NewWorkoutController.class)
            .put("warmingEndpoint", WarmingController.class)
            .put("getUserWorkout", GetUserWorkoutController.class)
            .put("switchWorkout", SwitchWorkoutController.class)
            .put("copyWorkout", CopyWorkoutController.class)
            .put("renameWorkout", RenameWorkoutController.class)
            .put("deleteWorkoutThenFetch", DeleteWorkoutThenFetchController.class)
            .put("resetWorkoutStatistics", ResetWorkoutStatisticsController.class)
            .put("editWorkout", EditWorkoutController.class)
            .put("updateExercise", UpdateExerciseController.class)
            .put("newExercise", NewExerciseController.class)
            .put("syncWorkout", SyncWorkoutController.class)
            .put("restartWorkout", RestartWorkoutController.class)
            .put("deleteExercise", DeleteExerciseController.class)
            .put("updateIcon", UpdateIconController.class)
            .put("updateEndpointId", RegisterEndpointTokenController.class)
            .put("removeEndpointId", RemoveEndpointTokenController.class)
            .put("sendFriendRequest", SendFriendRequestController.class)
            .put("cancelFriendRequest", CancelFriendRequestController.class)
            .put("setAllFriendRequestsSeen", SetAllFriendRequestsSeenController.class)
            .put("updateUserPreferences", UpdateUserPreferencesController.class)
            .put("acceptFriendRequest", AcceptFriendRequestController.class)
            .put("removeFriend", RemoveFriendController.class)
            .put("declineFriendRequest", DeclineFriendRequestController.class)
            .put("blockUser", BlockUserController.class)
            .put("unblockUser", UnblockUserController.class)
            .put("sendWorkout", SendWorkoutController.class)
            .put("getReceivedWorkouts", GetReceivedWorkoutsController.class)
            .put("getSharedWorkout", GetSharedWorkoutController.class)
            .put("setAllReceivedWorkoutsSeen", SetAllReceivedWorkoutsSeenController.class)
            .put("setReceivedWorkoutSeen", SetReceivedWorkoutSeenController.class)
            .put("acceptReceivedWorkout", AcceptReceivedWorkoutController.class)
            .put("declineReceivedWorkout", DeclineReceivedWorkoutController.class)
            .put("sendFeedback", SendFeedbackController.class)
            .build());

    private static final int minimumLiteWeightVersion = 10; // update this when there is a breaking change
    private static final int upgradeStatusCode = 426;

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        final Metrics metrics = new Metrics(context.getAwsRequestId(), context.getLogger());
        metrics.commonSetup(classMethod);
        String version = request.getHeaders().get(RequestFields.VERSION_NAME_HEADER);
        String versionCode = request.getHeaders().get(RequestFields.VERSION_CODE_HEADER);
        if (version != null && versionCode != null) {
            metrics.log("LiteWeight version code for request: " + version);
            try {
                int versionCodeInteger = Integer.parseInt(versionCode);
                if (versionCodeInteger < minimumLiteWeightVersion) {
                    metrics.commonClose(false);
                    metrics.logMetrics();
                    return new APIGatewayProxyResponseEvent()
                        .withBody("You must upgrade your version of LiteWeight to continue. ")
                        .withStatusCode(upgradeStatusCode);
                }
            } catch (Exception e) {
                metrics.log("Version code not in proper format. Continuing request...");
            }
        }

        ResultStatus<String> resultStatus;
        try {
            String action = request.getPath(); // in the form '/action'
            String[] splitAction = action.split("/"); // remove the prefixed slash

            if (splitAction.length == 2) {
                action = splitAction[1];

                if (ACTIONS_TO_CONTROLLERS.containsKey(action)) {
                    final Map<String, Object> jsonMap = JsonUtils.deserialize(request.getBody());
                    metrics.setRequestBody(jsonMap); // attach here for logging before handling action

                    if (!jsonMap.containsKey(RequestFields.ACTIVE_USER)) {
                        // get active user from id token passed to API and put it in the request payload
                        jsonMap.put(RequestFields.ACTIVE_USER, TokenUtils.getActiveUserFromRequest(request, context));

                        final Class<? extends ApiRequestController> controller = ACTIONS_TO_CONTROLLERS.get(action);
                        final Constructor c = controller.getConstructor();
                        final ApiRequestController apiRequestController = (ApiRequestController) c.newInstance();
                        resultStatus = apiRequestController.processApiRequest(jsonMap, metrics);
                    } else {
                        resultStatus = ResultStatus.failureBadRequest("Bad request body. Missing active user.");
                    }
                } else {
                    metrics.log(new WarningMessage(
                        new HashMap<String, Object>() {{
                            put("path", request.getPath());
                            put("body", request.getBody());
                        }}, classMethod, "Unknown action."));
                    resultStatus = ResultStatus.failureBadRequest("Unknown action.");
                }
            } else {
                metrics.log(new WarningMessage<>(
                    new HashMap<String, Object>() {{
                        put("path", request.getPath());
                        put("body", request.getBody());
                    }}, classMethod, "Bad request format."));
                resultStatus = ResultStatus.failureBadRequest("Bad request format.");
            }
        } catch (final Exception e) {
            metrics.log(new ErrorMessage(
                new HashMap<String, Object>() {{
                    put("path", request.getPath());
                    put("body", request.getBody());
                }}, classMethod, e));
            resultStatus = ResultStatus.failureBadRequest("Exception occurred." + request.getBody() + " " + e);
        }

        metrics.commonClose(resultStatus.success);
        metrics.logMetrics();

        return new APIGatewayProxyResponseEvent()
            .withBody(resultStatus.resultMessage)
            .withStatusCode(resultStatus.responseCode);
    }
}
