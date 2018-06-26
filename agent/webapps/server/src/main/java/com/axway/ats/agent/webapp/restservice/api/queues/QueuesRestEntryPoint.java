/*
 * Copyright 2017 Axway Software
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.axway.ats.agent.webapp.restservice.api.queues;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.axway.ats.agent.core.MultiThreadedActionHandler;
import com.axway.ats.agent.core.action.ActionRequest;
import com.axway.ats.agent.core.monitoring.queue.ActionExecutionStatistic;
import com.axway.ats.agent.core.monitoring.queue.QueueExecutionStatistics;
import com.axway.ats.agent.core.threading.data.config.LoaderDataConfig;
import com.axway.ats.agent.core.threading.patterns.ThreadingPattern;
import com.axway.ats.agent.webapp.restservice.api.actions.ActionPojo;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerClass;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethod;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethodParameterDefinition;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethodParameterDefinitions;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethodResponse;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethodResponses;
import com.axway.ats.core.threads.ThreadsPerCaller;
import com.axway.ats.core.utils.StringUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Path( "queues")
@SwaggerClass( "queues")
public class QueuesRestEntryPoint {

    private static final Logger LOG                          = Logger.getLogger(QueuesRestEntryPoint.class);
    private static final Gson   GSON                         = new Gson();

    private static final String START_QUEUE_OPERATION        = "start";
    private static final String RESUME_QUEUE_OPERATION       = "resume";
    private static final String CANCEL_ALL_QUEUES_OPERATION  = "cancelAll";
    private static final String CANCEL_QUEUE                 = "cancel";
    private static final String IS_QUEUE_RUNNING             = "isRunning";
    private static final String WAIT_UNTIL_QUEUE_IS_PAUSED   = "waitUntilPaused";
    private static final String WAIT_UNTIL_QUEUE_FINISH      = "waitUntilFinish";
    private static final String WAIT_UNTIL_ALL_QUEUES_FINISH = "waitUntilAllQueuesFinish";
    private static final String GET_ACTION_EXECUTION_RESULTS = "getActionExecutionResults";

    @PUT
    @Path( "schedule")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "put",
            parametersDefinition = "Schedule queue details",
            summary = "Schedule queue",
            url = "schedule")
    @SwaggerMethodParameterDefinitions( { @SwaggerMethodParameterDefinition(
            description = "The session ID",
            example = "HOST_ID:localhost:8089;RANDOM_TOKEN:3be2ae1c-6b72-40bb-bfbc-17d3d2e4182d;THREAD_ID",
            name = "sessionId",
            type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The queue name",
                                                  example = "FTP actions",
                                                  name = "queueName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The queue ID",
                                                  example = "1020",
                                                  name = "queueId",
                                                  type = "integer",
                                                  format = "integer32"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The actions which will be executed in the queue",
                                                  example = "[{\"resourceId\": 0,\"componentName\": \"testcmp\",\"methodName\": \"TestActions log message\",\"argumentsTypes\": [\"java.lang.String\"],\"argumentsValues\": [\"HELLO\"]}]",
                                                  name = "actions",
                                                  type = "object[]"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The threading pattern for the action execution",
                                                  example = "{\"duration\": 20,\"threadCount\": 5,\"blockUntilCompletion\": false,\"executionsPerTimeFrame\": 0,\"timeFrame\": 0,\"useSynchronizedIterations\": false,\"intervalBetweenIterations\": 1000,\"minIntervalBetweenIterations\": -1,\"maxIntervalBetweenIterations\": -1,\"queuePassRateInPercents\": 0.0}",
                                                  name = "threadingPattern",
                                                  type = "object"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The threading pattern's class name",
                                                  example = "com.axway.ats.agent.core.threading.patterns.FixedDurationAllAtOncePattern",
                                                  name = "threadingPatternClass",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The load data config",
                                                  example = "{\"parameterProviders\": []}",
                                                  name = "loaderDataConfig",
                                                  type = "object"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Whether the pattern iterations will be synchronized",
                                                  example = "true|false",
                                                  name = "useSynchronizedIterations",
                                                  type = "boolean") })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully schedule action queue details",
                                       description = "Successfully schedule action queue",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "queue '<QUEUE_ID>' successfully scheduled",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while scheduling action queue details",
                                       description = "Error while scheduling action queue",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non-transiend class fields for java.lang.Throwable ( detailMessage, cause, etc )\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.myproduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response scheduleQueue( @Context HttpServletRequest request ) {

        String sessionId = null;
        String queueName = null;
        int queueId = -1;
        ActionPojo[] actions = null;
        ThreadingPattern threadingPattern = null;
        LoaderDataConfig loaderDataConfig = null;
        boolean useSynchronizedIterations = false;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            queueName = getJsonElement(jsonObject, "queueName").getAsString();
            if (StringUtils.isNullOrEmpty(queueName)) {
                throw new NoSuchElementException("queueName is not provided with the request");
            }

            queueId = getJsonElement(jsonObject, "queueId").getAsInt();
            if (queueId < 0) {
                throw new IllegalArgumentException("queueId must be >= 0, but was " + queueId);
            }

            String threadingPatternJson = getJsonElement(jsonObject, "threadingPattern").toString();
            if (StringUtils.isNullOrEmpty(threadingPatternJson)) {
                throw new NoSuchElementException("threadingPattern is not provided with the request");
            }
            String threadingPatternClass = getJsonElement(jsonObject, "threadingPatternClass").getAsString();
            if (StringUtils.isNullOrEmpty(threadingPatternClass)) {
                throw new NoSuchElementException("threadingPatternClass is not provided with the request");
            }
            threadingPattern = (ThreadingPattern) GSON.fromJson(threadingPatternJson,
                                                                Class.forName(threadingPatternClass));
            String loaderDataConfigJson = getJsonElement(jsonObject, "loaderDataConfig").toString();
            if (StringUtils.isNullOrEmpty(loaderDataConfigJson)) {
                throw new NoSuchElementException("loaderDataConfig is not provided with the request");
            }
            loaderDataConfig = GSON.fromJson(loaderDataConfigJson, LoaderDataConfig.class);

            String useSynchronizedIterationsJson = getJsonElement(jsonObject,
                                                                  "useSynchronizedIterations").toString();
            if (StringUtils.isNullOrEmpty(useSynchronizedIterationsJson)) {
                throw new NoSuchElementException("useSynchronizedIterationsJson is not provided with the request");
            }
            useSynchronizedIterations = getJsonElement(jsonObject, "useSynchronizedIterations").getAsBoolean();

            String actionsJson = getJsonElement(jsonObject, "actions").toString();
            if (StringUtils.isNullOrEmpty(actionsJson)) {
                throw new NoSuchElementException("actions are not provided with the request");
            }
            actions = GSON.fromJson(actionsJson, ActionPojo[].class);
            List<ActionRequest> actionRequests = new ArrayList<ActionRequest>();
            for (ActionPojo pojo : actions) {
                ActionRequest actionRequest = new ActionRequest(pojo.getComponentName(), pojo.getMethodName(),
                                                                pojo.getActualArguments());
                actionRequests.add(actionRequest);
            }
            MultiThreadedActionHandler.getInstance(sessionId).scheduleActions(sessionId,
                                                                              queueName,
                                                                              queueId,
                                                                              actionRequests,
                                                                              threadingPattern,
                                                                              loaderDataConfig,
                                                                              useSynchronizedIterations);

            return Response.ok("{\"status_message\":\"queue '" + queueId + "' successfully scheduled\"}").build();
        } catch (Exception e) {
            String message = "Unable to schedule queue from session with id '" + sessionId + "'";
            LOG.error(message, e);
            return Response.serverError()
                           .entity("{\"error\":" + GSON.toJson(e) + ", \"exceptionClass\":\"" + e.getClass().getName()
                                   + "\"}")
                           .build();
        } finally {
            ThreadsPerCaller.unregisterThread();
        }
    }

    @POST
    @Path( "opts")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Queue operations details",
            summary = "Queue operations",
            url = "opts")
    @SwaggerMethodParameterDefinitions( { @SwaggerMethodParameterDefinition(
            description = "The session ID",
            example = "HOST_ID:localhost:8089;RANDOM_TOKEN:3be2ae1c-6b72-40bb-bfbc-17d3d2e4182d;THREAD_ID",
            name = "sessionId",
            type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The queue operation",
                                                  example = "start|resume|cancelAll|cancel|isRunning|waitUntilPaused|waitUntilFinish|waitUntilAllQueuesFinish|getActionExecutionResults",
                                                  name = "operation",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The queue name",
                                                  example = "FTP actions",
                                                  name = "queueName",
                                                  type = "string") })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully executed queue operation details",
                                       description = "Successfully executed queue operation",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Depending on the operation, the response's key and value are different. "
                                                             + "For example IS_QUEUE_RUNNING, returns {'running':TRUE|FALSE}, "
                                                             + "while GET_ACTION_EXECUTION_RESULTS returns an array of com.axway.ats.agent.core.monitoring.queue.ActionExecutionStatistic objects",
                                               example = "",
                                               name = "status_message|running|paused|queue_results",
                                               type = "string|boolean|object") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while executing queue operation details",
                                       description = "Error while executing queue operation",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non-transiend class fields for java.lang.Throwable ( detailMessage, cause, etc )\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.myproduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response executeQueueOperation( @Context HttpServletRequest request ) {

        String sessionId = null;
        String operation = null;
        String queueName = null;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            operation = getJsonElement(jsonObject, "operation").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("operation is not provided with the request");
            }
            if (!operation.equals(CANCEL_ALL_QUEUES_OPERATION) && !operation.equals(WAIT_UNTIL_ALL_QUEUES_FINISH)) {
                queueName = getJsonElement(jsonObject, "queueName").getAsString();
                if (StringUtils.isNullOrEmpty(sessionId)) {
                    throw new NoSuchElementException("queueName is not provided with the request");
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Execution of operation '" + operation + "' from session '" + sessionId + "'");
                // maybe add more details for the operation arguments (queueName, etc)
            }
            switch (operation) {
                case START_QUEUE_OPERATION:
                    // initialize the structure which will keep info about the execution results of this queue
                    QueueExecutionStatistics.getInstance().initActionExecutionResults(queueName);

                    MultiThreadedActionHandler.getInstance(sessionId).startQueue(queueName);
                    return Response.ok("{\"status_message\":\"queue '" + queueName + "' successfully started\"}")
                                   .build();
                case RESUME_QUEUE_OPERATION:
                    MultiThreadedActionHandler.getInstance(sessionId).resumeQueue(queueName);
                    return Response.ok("{\"status_message\":\"queue '" + queueName + "' successfully resumed\"}")
                                   .build();
                case CANCEL_ALL_QUEUES_OPERATION:
                    MultiThreadedActionHandler.getInstance(sessionId).cancelAllQueues();
                    return Response.ok("{\"status_message\":\"all queues successfully cancelled\"}").build();
                case CANCEL_QUEUE:
                    MultiThreadedActionHandler.getInstance(sessionId).cancelQueue(queueName);
                    return Response.ok("{\"status_message\":\"queue '" + queueName + "' successfully cancelled\"}")
                                   .build();
                case IS_QUEUE_RUNNING:
                    boolean running = MultiThreadedActionHandler.getInstance(sessionId).isQueueRunning(queueName);
                    return Response.ok("{\"running\":" + running + "}").build();
                case WAIT_UNTIL_QUEUE_IS_PAUSED:
                    boolean paused = MultiThreadedActionHandler.getInstance(sessionId)
                                                               .waitUntilQueueIsPaused(queueName);
                    return Response.ok("{\"paused\":" + paused + "}").build();
                case WAIT_UNTIL_QUEUE_FINISH:
                    MultiThreadedActionHandler.getInstance(sessionId)
                                              .waitUntilQueueFinish(queueName);
                    return Response.ok("{\"status_message\":\"queue '" + queueName + "' successfully finished\"}")
                                   .build();
                case WAIT_UNTIL_ALL_QUEUES_FINISH:
                    MultiThreadedActionHandler.getInstance(sessionId).waitUntilAllQueuesFinish();
                    return Response.ok("{\"status_message\":\"all queues successfully finished\"}").build();
                case GET_ACTION_EXECUTION_RESULTS:
                    List<ActionExecutionStatistic> results = QueueExecutionStatistics.getInstance()
                                                                                     .getActionExecutionResults(queueName);
                    // can results be null?
                    return Response.ok("{\"queue_results\":"
                                       + GSON.toJson(results.toArray(new ActionExecutionStatistic[results.size()]))
                                       + "}")
                                   .build();
                default:
                    throw new IllegalArgumentException("Queue operation '" + operation + "' is not supported");
            }
        } catch (Exception e) {
            String message = "Unable to execute operation '" + operation + "' from session with id '" + sessionId + "'";
            LOG.error(message, e);
            return Response.serverError()
                           .entity("{\"error\":" + GSON.toJson(e) + ", \"exceptionClass\":\"" + e.getClass().getName()
                                   + "\"}")
                           .build();
        } finally {
            ThreadsPerCaller.unregisterThread();
        }

    }

    private JsonElement getJsonElement( JsonObject object, String key ) {

        JsonElement element = object.get(key);
        if (element == null) {
            throw new NoSuchElementException("'" + key + "'" + " is not provided with the request");
        }
        return element;
    }

}
