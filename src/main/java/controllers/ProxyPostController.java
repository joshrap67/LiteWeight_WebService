package controllers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import helpers.ErrorMessage;
import helpers.JsonHelper;
import helpers.Metrics;
import helpers.RequestFields;
import helpers.ResultStatus;
import helpers.TokenHelper;
import helpers.WarningMessage;
import interfaces.ApiRequestController;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class ProxyPostController implements
    RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    // all the different actions this service has
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
            .put("deleteWorkout", DeleteWorkoutController.class)
            .put("resetWorkoutStatistics", ResetWorkoutStatisticsController.class)
            .put("editWorkout", EditWorkoutController.class)
            .put("updateExercise", UpdateExerciseController.class)
            .put("newExercise", NewExerciseController.class)
            .put("syncWorkout", SyncWorkoutController.class)
            .put("restartWorkout", RestartWorkoutController.class)
            .build());

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request,
        Context context) {
        final String classMethod = this.getClass().getSimpleName() + ".processApiRequest";

        final Metrics metrics = new Metrics(context.getAwsRequestId(), context.getLogger());
        metrics.commonSetup(classMethod);

        ResultStatus<String> resultStatus;

        try {
            String action = request.getPath(); // this should be of the form '/action'
            String[] splitAction = action.split("/"); // remove the prefixed slash

            if (splitAction.length == 2) {
                action = splitAction[1]; // the action is after the '/'

                if (ACTIONS_TO_CONTROLLERS.containsKey(action)) {
                    final Map<String, Object> jsonMap = JsonHelper.deserialize(request.getBody());
                    metrics
                        .setRequestBody(jsonMap); // attach here for logging before handling action

                    if (!jsonMap.containsKey(RequestFields.ACTIVE_USER)) {
                        // get active user from id token passed to API and put it in the request payload
                        jsonMap.put(RequestFields.ACTIVE_USER,
                            TokenHelper.getActiveUserFromRequest(request, context));

                        final Class<? extends ApiRequestController> manager = ACTIONS_TO_CONTROLLERS
                            .get(action);
                        final Constructor c = manager.getConstructor();
                        final ApiRequestController apiRequestManager = (ApiRequestController) c
                            .newInstance();
                        resultStatus = apiRequestManager.processApiRequest(jsonMap, metrics);
                    } else {
                        resultStatus = ResultStatus
                            .failureBadRequest("Bad request body. Missing active user.");
                    }
                } else {
                    metrics.log(new WarningMessage(
                        new HashMap<String, Object>() {{
                            put("path", request.getPath());
                            put("body", request.getBody());
                        }},
                        classMethod, "Unknown action."));
                    resultStatus = ResultStatus.failureBadRequest("Unknown action.");
                }
            } else {
                metrics.log(new WarningMessage<>(
                    new HashMap<String, Object>() {{
                        put("path", request.getPath());
                        put("body", request.getBody());
                    }},
                    classMethod, "Bad request format."));
                resultStatus = ResultStatus.failureBadRequest("Bad request format.");
            }
        } catch (final Exception e) {
            metrics.log(new ErrorMessage(
                new HashMap<String, Object>() {{
                    put("path", request.getPath());
                    put("body", request.getBody());
                }},
                classMethod, e));
            resultStatus = ResultStatus
                .failureBadRequest("Exception occurred." + request.getBody() + " " + e.toString());
        }

        metrics.commonClose(resultStatus.responseCode);
        metrics.logMetrics();

        return new APIGatewayProxyResponseEvent().withBody(resultStatus.resultMessage)
            .withStatusCode(resultStatus.responseCode);
    }
}
