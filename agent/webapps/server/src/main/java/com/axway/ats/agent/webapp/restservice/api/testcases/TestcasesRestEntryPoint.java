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
package com.axway.ats.agent.webapp.restservice.api.testcases;

import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.NoSuchElementException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.axway.ats.agent.core.MultiThreadedActionHandler;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerClass;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethod;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethodParameterDefinition;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethodParameterDefinitions;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethodResponse;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethodResponses;
import com.axway.ats.core.events.TestcaseStateEventsDispacher;
import com.axway.ats.core.threads.ThreadsPerCaller;
import com.axway.ats.core.utils.ClasspathUtils;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.AtsDbLogger;
import com.axway.ats.log.autodb.TestCaseState;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Path( "testcases")
@SwaggerClass( "testcases")
public class TestcasesRestEntryPoint {

    private static final Logger      LOG       = Logger.getLogger(TestcasesRestEntryPoint.class);
    private static final Gson        GSON      = new Gson();
    private static final AtsDbLogger log       = AtsDbLogger.getLogger("com.axway.ats.agent.webapp.restservice.api");

    private static int               lastRunId = -1;

    @PUT
    @Path( "")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "put",
            parametersDefinition = "Start testcase details",
            summary = "Start testcase",
            url = "")
    @SwaggerMethodParameterDefinitions( { @SwaggerMethodParameterDefinition(
            description = "The session ID",
            example = "HOST_ID:localhost:8089;THREAD_ID:main",
            name = "sessionId",
            type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The TestCaseState object",
                                                  example = "{\"lastExecutedTestcaseId\":-1,\"testcaseId\":762,\"runId\":138}",
                                                  name = "testCaseState",
                                                  type = "object") })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully start testcase details",
                                       description = "Successfully start testcase",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "testcase with id '<TESTCASE_ID>' from run with id '<RUN_ID>' successfully started",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while starting testcase details",
                                       description = "Error while starting testcase",
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
    public Response startTestcase( @Context HttpServletRequest request ) {

        TestCaseState testCaseState = null;
        String sessionId = null;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            String testCaseStateJson = getJsonElement(jsonObject, "testCaseState").toString();
            if (StringUtils.isNullOrEmpty(testCaseStateJson)) {
                throw new NoSuchElementException("testcaseState is not provided with the request");
            }
            testCaseState = GSON.fromJson(testCaseStateJson, TestCaseState.class);
            // cancel all action tasks, that are started on an agent by session with id sessionId
            MultiThreadedActionHandler.cancellAllQueuesFromAgent(sessionId);

            // get the current state on the remote machine
            TestCaseState currentState = log.getCurrentTestCaseState();
            boolean joinToNewTescase = true;
            if (currentState != null && currentState.isInitialized()) {
                /* This agent is already configured.
                 *
                 * Now check if the state is the same as the new one, this would mean we are trying to
                 * configure this agent for second time.
                 * This is normal as we get here when Test Executor or another agent calls this agent for first time.
                 *
                 * If the state is different, we hit an error which means this agent did not get On Test End event
                 * for the previous test case.
                 */
                if (!currentState.equals(testCaseState)) {

                    log.error("This test appears to be aborted by the user on the test executor side, but it kept running on the agent side."
                              + " Now we cancel any further logging from the agent.");
                    log.leaveTestCase(sessionId);
                } else {
                    joinToNewTescase = false;

                }
            }

            if (joinToNewTescase) {
                // connect to the new test case
                log.joinTestCase(testCaseState, sessionId);

                // take care of chained ATS agents(if there are any)
                TestcaseStateEventsDispacher.getInstance().onTestStart();
            }
            logClassPath(testCaseState);
            return Response.ok("{\"status_message\":\"testcase with id '" + testCaseState.getTestcaseId()
                               + "' from run with id '" + testCaseState.getRunId() + "' successfully started\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to start testcase with id '" + testCaseState.getTestcaseId()
                             + "' from run with id '" + testCaseState.getRunId() + "' received from session with id '"
                             + sessionId + "'";
            LOG.error(message, e);
            return Response.serverError()
                           .entity("{\"error\":" + GSON.toJson(e) + ", \"exceptionClass\":\"" + e.getClass().getName()
                                   + "\"}")
                           .build();
        } finally {
            ThreadsPerCaller.unregisterThread();
        }
    }

    @DELETE
    @Path( "")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "DELETE",
            parametersDefinition = "",
            summary = "Delete the last testcase, started by the current session",
            url = "")
    @SwaggerMethodParameterDefinitions( { @SwaggerMethodParameterDefinition(
            description = "The session ID",
            example = "HOST_ID:localhost:8089;RANDOM_TOKEN_IN:<SOME_UUID>;THREAD_ID:main",
            name = "sessionId",
            type = "string") })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully end testcase details",
                                       description = "Successfully end testcase",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "testcase with id '<TESTCASE_ID>' from run with id '<RUN_ID>' successfully ended",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while ending testcase details",
                                       description = "Error while ending testcase",
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
    public Response endTestcase( @Context HttpServletRequest request, @QueryParam( "sessionId") String sessionId ) {

        int currentRunId = -1;
        int currentTestcaseId = -1;
        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            sessionId = URLDecoder.decode(sessionId, "UTF-8");
            ThreadsPerCaller.registerThread(sessionId);
            TestCaseState currentState = log.getCurrentTestCaseState();
            currentRunId = currentState.getRunId();
            currentTestcaseId = currentState.getTestcaseId();
            /* If the agent is not configured, this means we are coming here for second time during same test.
             * Ignore this event.
             */
            if (currentState != null && currentState.isInitialized()) {
                log.leaveTestCase(sessionId);

                // take care of chained ATS agents(if there are any)
                TestcaseStateEventsDispacher.getInstance().onTestEnd();
            }
        } catch (Exception e) {
            String message = "Unable to end testcase started from session with id '"
                             + sessionId + "'";
            LOG.error(message, e);
            return Response.serverError()
                           .entity("{\"error\":" + GSON.toJson(e) + ", \"exceptionClass\":\"" + e.getClass().getName()
                                   + "\"}")
                           .build();
        } finally {
            ThreadsPerCaller.unregisterThread();
        }
        return Response.ok("{\"status_message\":\"testcase with id '" + currentRunId
                           + "' from run with id '" + currentTestcaseId + "' successfully ended\"}")
                       .build();
    }

    private void logClassPath( TestCaseState testCaseState ) {

        // this check is made so we log the classpath just once per run
        if (testCaseState.getRunId() != lastRunId) {

            lastRunId = testCaseState.getRunId();
            StringBuilder classpath = new StringBuilder();

            classpath.append("ATS Agent classpath on \"");
            classpath.append(HostUtils.getLocalHostIP());
            classpath.append("\" : \n");
            classpath.append(new ClasspathUtils().getClassPathDescription());

            log.info(classpath, true);
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
