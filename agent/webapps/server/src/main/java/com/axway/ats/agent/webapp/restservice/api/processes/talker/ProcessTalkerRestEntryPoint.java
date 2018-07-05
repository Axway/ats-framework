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
package com.axway.ats.agent.webapp.restservice.api.processes.talker;

import java.io.InputStreamReader;
import java.util.NoSuchElementException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

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

@Path( "processes/talkers")
@SwaggerClass( "processes/talkers")
public class ProcessTalkerRestEntryPoint {

    private static final Logger LOG  = Logger.getLogger(ProcessTalkerRestEntryPoint.class);

    private static final Gson   GSON = new Gson();

    @PUT
    @Path( "")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "put",
            parametersDefinition = "Initialize process talker details",
            summary = "Initialize process talker",
            url = "")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "HOST_ID:localhost:8089;THREAD_ID:main",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "shell command or bat/bash script",
                                                  example = "grep",
                                                  name = "command",
                                                  type = "string") })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successful initialization of process talker details",
                                       description = "Successful initialization of process talker",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The resource ID of the initialized process talker",
                                               example = "123",
                                               name = "resourceId",
                                               type = "integer") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while initializing process talker details",
                                       description = "Error while initializing process talker",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non transiend class fields for java.lang.Throwable ( detailMessage, cause, etc)\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.myproduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response
            initializeProcessTalker( @Context HttpServletRequest request ) {

        String sessionId = null;
        String command = null;
        boolean defaultInitialization = false;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            if (jsonObject.has("defaultInitialization")) {
                defaultInitialization = jsonObject.get("defaultInitialization").getAsBoolean();
                if (defaultInitialization) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("We will perform Default initialization for Process Executor action");
                    }
                    command = "";
                }
            } else {
                command = getJsonElement(jsonObject, "command").getAsString();
            }

            int resourceId = ProcessesTalkersManager.initializeProcessTalker(sessionId, command);
            String response = "{\"resourceId\":" + resourceId + "}";
            return Response.ok(response).build();
        } catch (Exception e) {
            String message = null;
            if (defaultInitialization) {
                message = "Unable to perform default initialization for process talker in session with id '"
                          + sessionId + "'";
            }
            message = "Unable to initialize process talker for command '" + command + "' in session with id '"
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

    @POST
    @Path( "timeout")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "put",
            parametersDefinition = "Set default operation timeout details",
            summary = "Set default operation timeout",
            url = "timeout")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "HOST_ID:localhost:8089;THREAD_ID:main",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The timeout in seconds",
                                                  example = "1000",
                                                  name = "timeout",
                                                  type = "integer") })

    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully set default operation timeout details",
                                       description = "Successfully set default operation timeout",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "some message",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while setting default operation timeout details",
                                       description = "Error while setting default operation timeout",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non-transiend class fields for java.lang.Throwable ( detailMessage, cause, etc)\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.myproduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response
            setDefaultOperationTimeout( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        int timeout = -1;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsInt();
            if (resourceId < 0) {
                throw new NoSuchElementException("resourceId must be >= 0, but was " + resourceId);
            }
            timeout = getJsonElement(jsonObject, "defaultTimeoutSeconds").getAsInt();
            ProcessesTalkersManager.setDefaultOperationTimeout(sessionId, resourceId, timeout);
            return Response.ok("{\"status_message\":\"default operation timeout set to " + timeout + " ms\"}").build();
        } catch (Exception e) {
            String message = "Unable to set detault operation timeout to '" + timeout + "' for process talker '"
                             + resourceId
                             + "' in session with id '"
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

    @POST
    @Path( "command")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "put",
            parametersDefinition = "Set command details",
            summary = "Set command",
            url = "command")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "HOST_ID:localhost:8089;THREAD_ID:main",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The command",
                                                  example = "test.sh",
                                                  name = "command",
                                                  type = "string") })

    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successful set of process talker command details",
                                       description = "Successful set of process talker command",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "command set to <some_command>",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "",
                                       description = "",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non-transiend class fields for java.lang.Throwable ( detailMessage, cause, etc)\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.myproduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response
            setCommand( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String command = null;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsInt();
            if (resourceId < 0) {
                throw new NoSuchElementException("resourceId must be >= 0, but was " + resourceId);
            }
            command = getJsonElement(jsonObject, "command").getAsString();
            if (StringUtils.isNullOrEmpty(command)) {
                throw new NoSuchElementException("command is not provided with the request");
            }
            ProcessesTalkersManager.setCommand(sessionId, resourceId, command);
            return Response.ok("{\"status_message\":\"command set to '" + command + "'\"}").build();
        } catch (Exception e) {
            String message = "Unable to set command to '" + command + "' for process talker '" + resourceId
                             + "' in session with id '"
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

    @GET
    @Path( "content/pendingToMatch")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get pending to match content",
            url = "content/pendingToMatch")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "HOST_ID:localhost:8089;THREAD_ID:main",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID",
                                                  example = "1",
                                                  name = "command",
                                                  type = "integer") })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully get pending to match content details",
                                       description = "Successfully get pending to match content",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The pending to match content",
                                               example = "some pending content",
                                               name = "action_result",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting pending to match content details",
                                       description = "Error while getting pending to match content",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non-transiend class fields for java.lang.Throwable ( detailMessage, cause, etc)\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.myproduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response
            getPendingToMatchContent( @Context HttpServletRequest request,
                                      @QueryParam(
                                              value = "sessionId") String sessionId,
                                      @QueryParam(
                                              value = "resourceId") int resourceId ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new NoSuchElementException("resourceId must be >= 0, but was " + resourceId);
            }
            String content = ProcessesTalkersManager.getPendingToMatchContent(sessionId, resourceId);
            return Response.ok("{\"action_result\":" + GSON.toJson(content, content.getClass()) + "}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to get pending to match content for process talker '" + resourceId
                             + "' in session with id '"
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

    @GET
    @Path( "content/stdout/current")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get current STDOUT",
            url = "content/stdout/current")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "HOST_ID:localhost:8089;THREAD_ID:main",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID",
                                                  example = "1",
                                                  name = "command",
                                                  type = "integer") })

    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully get current STDOUT details",
                                       description = "Successfully get current STDOUT",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The current STDOUT",
                                               example = "some STDOUT messages",
                                               name = "action_result",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting current STDOUT details",
                                       description = "Error while getting current STDOUT",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non-transiend class fields for java.lang.Throwable ( detailMessage, cause, etc)\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.myproduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response getCurrentStdout( @Context HttpServletRequest request,
                                      @QueryParam(
                                              value = "sessionId") String sessionId,
                                      @QueryParam(
                                              value = "resourceId") int resourceId ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new NoSuchElementException("resourceId must be >= 0, but was " + resourceId);
            }
            String stdout = ProcessesTalkersManager.getCurrentStdout(sessionId, resourceId);
            return Response.ok("{\"action_result\":" + GSON.toJson(stdout, stdout.getClass()) + "}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to get curent stdout for process talker '" + resourceId
                             + "' in session with id '"
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

    @POST
    @Path( "content/stderr/expect")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Expect STDERR details",
            summary = "Expect STDERR",
            url = "content/stderr/expect")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "some-session-id ( must be unique for each session )",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID. This resource ID points to an existing process talker",
                                                  example = "123",
                                                  name = "resourceId",
                                                  type = "integer"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The pattern to expect in STDERR",
                                                  example = "Some string",
                                                  name = "pattern",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The timeout ( in seconds ) to wait for the pattern to be found",
                                                  example = "10",
                                                  name = "timeoutSeconds",
                                                  type = "integer")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully expect STDERR details",
                                       description = "Successfully expect STDERR",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfull operation 'expect' over STDERR",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while executing expect STDERR operation details",
                                       description = "Error while executing expect STDERR operation",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non-transiend class fields for java.lang.Throwable ( detailMessage, cause, etc)\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.myproduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response expectStderr( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String pattern = null;
        int timeoutSeconds = -1;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsInt();
            if (resourceId < 0) {
                throw new NoSuchElementException("resourceId must be >= 0, but was " + resourceId);
            }
            pattern = getJsonElement(jsonObject, "pattern").getAsString();
            if (StringUtils.isNullOrEmpty(pattern)) {
                throw new NoSuchElementException("pattern is not provided with the request");
            }
            timeoutSeconds = getJsonElement(jsonObject, "timeoutSeconds").getAsInt();
            ProcessesTalkersManager.expectErr(sessionId, resourceId, pattern, timeoutSeconds);
            return Response.ok("{\"status_message\":\"successfull operation 'expect' over STDERR\"}").build();
        } catch (Exception e) {
            String message = "Unable to execute 'expect' operation over STDERR for process talker '" + resourceId
                             + "' in session with id '"
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

    @POST
    @Path( "content/stderr/expect/byRegex")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Expect STDERR by REGEX details",
            summary = "Expect STDERR by REGEX",
            url = "content/stderr/expect/byRegex")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "some session ID",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID",
                                                  example = "123",
                                                  name = "resourceId",
                                                  type = "integer"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The REGEX pattern",
                                                  example = "[a-z].*",
                                                  name = "pattern",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The timeout ( in seconds ) to wait for the pattern to be found",
                                                  example = "3",
                                                  name = "timeoutSeconds",
                                                  type = "integer")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully expect STDERR by REGEX details",
                                       description = "Successfully expect STDERR by REGEX",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfull operation 'expect by regex' over STDERR",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while executing expect STDERR by REGEX operation details",
                                       description = "Error while executing expect STDERR by REGEX operation",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non-transiend class fields for java.lang.Throwable ( detailMessage, cause, etc)\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.myproduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response expectStderrByRegex( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String pattern = null;
        int timeoutSeconds = -1;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsInt();
            if (resourceId < 0) {
                throw new NoSuchElementException("resourceId must be >= 0, but was " + resourceId);
            }
            pattern = getJsonElement(jsonObject, "pattern").getAsString();
            if (StringUtils.isNullOrEmpty(pattern)) {
                throw new NoSuchElementException("pattern is not provided with the request");
            }
            timeoutSeconds = getJsonElement(jsonObject, "timeoutSeconds").getAsInt();
            ProcessesTalkersManager.expectErrByRegex(sessionId, resourceId, pattern, timeoutSeconds);
            return Response.ok("{\"status_message\":\"successfull operation 'expect by regex' over STDERR\"}").build();
        } catch (Exception e) {
            String message = "Unable to execute 'expect by regex' operation over STDERR for process talker '"
                             + resourceId
                             + "' in session with id '"
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

    @POST
    @Path( "content/stderr/expect/all")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Expect All STDERR details",
            summary = "Expect All STDERR",
            url = "content/stderr/expect/all")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "<HOST_NAME>_<UUID_VALUE>_<THREAD_NAME>",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID",
                                                  example = "123",
                                                  name = "resourceId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Array of the patterns ( strings ) to be expected in STDERR",
                                                  example = "[\"some_string\",\"other_string\"]",
                                                  name = "patterns",
                                                  type = "string[]"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The timeout ( in seconds ) to wait for the patterns to be found",
                                                  example = "3",
                                                  name = "timeoutSeconds",
                                                  type = "integer")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully execute operation expect All STDERR details",
                                       description = "Successfully execute operation expect All STDERR",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfull operation 'expect all' over STDERR",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while executing operation expect All STDERR details",
                                       description = "Error while executing operation expect All STDERR",
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
    public Response expectAllStderr( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String[] patterns = null;
        int timeoutSeconds = -1;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsInt();
            if (resourceId < 0) {
                throw new NoSuchElementException("resourceId must be >= 0, but was " + resourceId);
            }
            patterns = GSON.fromJson(getJsonElement(jsonObject, "patterns"), String[].class);
            timeoutSeconds = getJsonElement(jsonObject, "timeoutSeconds").getAsInt();
            ProcessesTalkersManager.expectErrAll(sessionId, resourceId, patterns, timeoutSeconds);
            return Response.ok("{\"status_message\":\"successfull operation 'expect all' over STDERR\"}").build();
        } catch (Exception e) {
            String message = "Unable to execute 'expect all' operation over STDERR for process talker '" + resourceId
                             + "' in session with id '"
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

    @POST
    @Path( "content/stderr/expect/all/byRegex")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Expect All STDERR details",
            summary = "Expect All STDERR",
            url = "content/stderr/expect/all/byRegex")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "<HOST_NAME>_<UUID_VALUE>_<THREAD_NAME>",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID",
                                                  example = "123",
                                                  name = "resourceId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Array of the REGEX patterns to be expected in STDERR",
                                                  example = "[\"[a-z].*\",\"[0-9].:\"]",
                                                  name = "patterns",
                                                  type = "string[]"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The timeout ( in seconds ) to wait for the patterns to be found",
                                                  example = "3",
                                                  name = "timeoutSeconds",
                                                  type = "integer")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully execute operation expect All STDERR by REGEX details",
                                       description = "Successfully execute operation expect All STDERR by REGEX",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfull operation 'expect all by regex' over STDERR",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while executing operation expect All STDERR by REGEX details",
                                       description = "Error while executing operation expect All by REGEX STDERR",
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
    public Response expectAllStderrByRegex( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String[] regexPatterns = null;
        int timeoutSeconds = -1;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsInt();
            if (resourceId < 0) {
                throw new NoSuchElementException("resourceId must be >= 0, but was " + resourceId);
            }
            regexPatterns = GSON.fromJson(getJsonElement(jsonObject, "regexPatterns"), String[].class);
            timeoutSeconds = getJsonElement(jsonObject, "timeoutSeconds").getAsInt();
            ProcessesTalkersManager.expectErrAllByRegex(sessionId, resourceId, regexPatterns, timeoutSeconds);
            return Response.ok("{\"status_message\":\"successfull operation 'expect all by regex' over STDERR\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to execute 'expect all by regex' operation over STDERR for process talker '"
                             + resourceId
                             + "' in session with id '"
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

    @POST
    @Path( "content/stderr/expect/any")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Expect Any STDERR details",
            summary = "Expect Any STDERR",
            url = "content/stderr/expect/any")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "<HOST_NAME>_<UUID_VALUE>_<THREAD_NAME>",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID",
                                                  example = "123",
                                                  name = "resourceId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Array of patterns, one or more of which to be expected in STDERR",
                                                  example = "[\"some_string\",\"other_string\"]",
                                                  name = "patterns",
                                                  type = "string[]"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The timeout ( in seconds ) to wait for the patterns to be found",
                                                  example = "3",
                                                  name = "timeoutSeconds",
                                                  type = "integer")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully execute operation expect Any STDERR details",
                                       description = "Successfully execute operation expect Any STDERR",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfull operation 'expect any' over STDERR",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while executing operation expect Any STDERR details",
                                       description = "Error while executing operation expect Any STDERR",
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
    public Response expectAnyStderr( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String[] patterns = null;
        int timeoutSeconds = -1;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsInt();
            if (resourceId < 0) {
                throw new NoSuchElementException("resourceId must be >= 0, but was " + resourceId);
            }
            patterns = GSON.fromJson(getJsonElement(jsonObject, "patterns"), String[].class);
            timeoutSeconds = getJsonElement(jsonObject, "timeoutSeconds").getAsInt();
            int result = ProcessesTalkersManager.expectErrAny(sessionId, resourceId, patterns, timeoutSeconds);
            return Response.ok("{\"action_result\":" + result + "}").build();
        } catch (Exception e) {
            String message = "Unable to execute 'expect any' operation over STDERR for process talker '" + resourceId
                             + "' in session with id '"
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

    @POST
    @Path( "content/stderr/expect/any/byRegex")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Expect Any by Regex STDERR details",
            summary = "Expect Any by Regex STDERR",
            url = "content/stderr/expect/any/byRegex")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "<HOST_NAME>_<UUID_VALUE>_<THREAD_NAME>",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID",
                                                  example = "123",
                                                  name = "resourceId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Array of REGEX patterns, one or more of which to be expected in STDERR",
                                                  example = "[\"some_string\",\"other_string\"]",
                                                  name = "patterns",
                                                  type = "string[]"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The timeout ( in seconds ) to wait for the patterns to be found",
                                                  example = "3",
                                                  name = "timeoutSeconds",
                                                  type = "integer")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully execute operation expect Any by REGEX STDERR details",
                                       description = "Successfully execute operation expect Any by REGEX STDERR",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfull operation 'expect any by regex' over STDERR",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while executing operation expect Any by REGEX STDERR details",
                                       description = "Error while executing operation expect Any by REGEX STDERR",
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
    public Response expectAnyStderrByRegex( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String[] patterns = null;
        int timeoutSeconds = -1;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsInt();
            if (resourceId < 0) {
                throw new NoSuchElementException("resourceId must be >= 0, but was " + resourceId);
            }
            patterns = GSON.fromJson(getJsonElement(jsonObject, "regexPatterns"), String[].class);
            timeoutSeconds = getJsonElement(jsonObject, "timeoutSeconds").getAsInt();
            int result = ProcessesTalkersManager.expectErrAnyByRegex(sessionId, resourceId, patterns, timeoutSeconds);
            return Response.ok("{\"action_result\":" + result + "}").build();
        } catch (Exception e) {
            String message = "Unable to execute 'expect any by regex' operation over STDERR for process talker '"
                             + resourceId
                             + "' in session with id '"
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

    @GET
    @Path( "content/stderr/current")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get current STDERR",
            url = "content/stderr/current")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "HOST_ID:localhost:8089;THREAD_ID:main",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID",
                                                  example = "1",
                                                  name = "resourceId",
                                                  type = "integer") })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully get current STDERR details",
                                       description = "Successfully get current STDERR ",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The session ID",
                                               example = "some-session-id",
                                               name = "sessionId",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting current STDERR details",
                                       description = "Error while getting current STDERR",
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
    public Response getCurrentStderr( @Context HttpServletRequest request,
                                      @QueryParam(
                                              value = "sessionId") String sessionId,
                                      @QueryParam(
                                              value = "resourceId") int resourceId ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new NoSuchElementException("resourceId must be >= 0, but was " + resourceId);
            }
            String stdout = ProcessesTalkersManager.getCurrentStderr(sessionId, resourceId);
            return Response.ok("{\"action_result\":" + GSON.toJson(stdout, stdout.getClass()) + "}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to get curent stderr for process talker '" + resourceId
                             + "' in session with id '"
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

    @POST
    @Path( "content/stdout/expect")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Expect STDOUT details",
            summary = "Expect STDOUT",
            url = "content/stdout/expect")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "some-session-id ( must be unique for each session )",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID. This resource ID points to an existing process talker",
                                                  example = "123",
                                                  name = "resourceId",
                                                  type = "integer"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The pattern to expect in STDOUT",
                                                  example = "Some string",
                                                  name = "pattern",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The timeout ( in seconds ) to wait for the pattern to be found",
                                                  example = "10",
                                                  name = "timeoutSeconds",
                                                  type = "integer")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully expect STDOUT details",
                                       description = "Successfully expect STDOUT",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfull operation 'expect' over STDOUT",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while executing expect STDOUT operation details",
                                       description = "Error while executing expect STDOUT operation",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non-transiend class fields for java.lang.Throwable ( detailMessage, cause, etc)\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.myproduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response expectStdout( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String pattern = null;
        int timeoutSeconds = -1;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsInt();
            if (resourceId < 0) {
                throw new NoSuchElementException("resourceId must be >= 0, but was " + resourceId);
            }
            pattern = getJsonElement(jsonObject, "pattern").getAsString();
            if (StringUtils.isNullOrEmpty(pattern)) {
                throw new NoSuchElementException("pattern is not provided with the request");
            }
            try {
                timeoutSeconds = getJsonElement(jsonObject, "timeoutSeconds").getAsInt();
                ProcessesTalkersManager.expect(sessionId, resourceId, pattern, timeoutSeconds);
            } catch (NoSuchElementException e) {
                // no timeoutSeconds provided with the request. The default one will be used
                ProcessesTalkersManager.expect(sessionId, resourceId, pattern);
            }
            return Response.ok("{\"status_message\":\"successfull operation 'expect' over STDOUT\"}").build();
        } catch (Exception e) {
            String message = "Unable to execute 'expect' operation over STDOUT for process talker '" + resourceId
                             + "' in session with id '"
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

    @POST
    @Path( "content/stdout/expect/byRegex")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Expect STDOUT by REGEX details",
            summary = "Expect STDOUT by REGEX STDOUT",
            url = "content/stdout/expect/byRegex")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "some session ID",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID",
                                                  example = "123",
                                                  name = "resourceId",
                                                  type = "integer"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The REGEX pattern",
                                                  example = "[a-z].*",
                                                  name = "pattern",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The timeout ( in seconds ) to wait for the pattern to be found",
                                                  example = "3",
                                                  name = "timeoutSeconds",
                                                  type = "integer")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully expect STDOUT by REGEX details",
                                       description = "Successfully expect STDOUT by REGEX",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfull operation 'expect by regex' over STDOUT",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while executing expect STDOUT by REGEX operation details",
                                       description = "Error while executing expect STDOUT by REGEX operation",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non-transiend class fields for java.lang.Throwable ( detailMessage, cause, etc)\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.myproduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response expectStdoutByRegex( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String pattern = null;
        int timeoutSeconds = -1;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsInt();
            if (resourceId < 0) {
                throw new NoSuchElementException("resourceId must be >= 0, but was " + resourceId);
            }
            pattern = getJsonElement(jsonObject, "pattern").getAsString();
            if (StringUtils.isNullOrEmpty(pattern)) {
                throw new NoSuchElementException("pattern is not provided with the request");
            }
            try {
                timeoutSeconds = getJsonElement(jsonObject, "timeoutSeconds").getAsInt();
                ProcessesTalkersManager.expectByRegex(sessionId, resourceId, pattern, timeoutSeconds);
            } catch (NoSuchElementException e) {
                // no timeoutSeconds provided with the request. The default one will be used
                ProcessesTalkersManager.expectByRegex(sessionId, resourceId, pattern);
            }
            return Response.ok("{\"status_message\":\"successfull operation 'expect by regex' over STDOUT\"}").build();
        } catch (Exception e) {
            String message = "Unable to execute 'expect by regex' operation over STDOUT for process talker '"
                             + resourceId
                             + "' in session with id '"
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

    @POST
    @Path( "content/stdout/expect/all")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Expect All STDOUT details",
            summary = "Expect All STDOUT",
            url = "content/stdout/expect/all")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "<HOST_NAME>_<UUID_VALUE>_<THREAD_NAME>",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID",
                                                  example = "123",
                                                  name = "resourceId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Array of the patterns ( strings ) to be expected in STDERR",
                                                  example = "[\"some_string\",\"other_string\"]",
                                                  name = "patterns",
                                                  type = "string[]"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The timeout ( in seconds ) to wait for the patterns to be found",
                                                  example = "3",
                                                  name = "timeoutSeconds",
                                                  type = "integer")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully execute operation expect All STDOUT details",
                                       description = "Successfully execute operation expect All STDOUT",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfull operation 'expect all' over STDOUT",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while executing operation expect All STDOUT details",
                                       description = "Error while executing operation expect All STDOUT",
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
    public Response expectAllStdout( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String[] patterns = null;
        int timeoutSeconds = -1;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsInt();
            if (resourceId < 0) {
                throw new NoSuchElementException("resourceId must be >= 0, but was " + resourceId);
            }
            patterns = GSON.fromJson(getJsonElement(jsonObject, "patterns"), String[].class);
            try {
                timeoutSeconds = getJsonElement(jsonObject, "timeoutSeconds").getAsInt();
                ProcessesTalkersManager.expectAll(sessionId, resourceId, patterns, timeoutSeconds);
            } catch (NoSuchElementException e) {
                // no timeoutSeconds provided with the request. The default one will be used
                ProcessesTalkersManager.expectAll(sessionId, resourceId, patterns);
            }
            return Response.ok("{\"status_message\":\"successfull operation 'expect all' over STDOUT\"}").build();
        } catch (Exception e) {
            String message = "Unable to execute 'expect all' operation over STDOUT for process talker '" + resourceId
                             + "' in session with id '"
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

    @POST
    @Path( "content/stdout/expect/all/byRegex")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Expect All STDOUT details",
            summary = "Expect All STDOUT",
            url = "content/stdout/expect/all/byRegex")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "<HOST_NAME>_<UUID_VALUE>_<THREAD_NAME>",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID",
                                                  example = "123",
                                                  name = "resourceId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Array of the REGEX patterns to be expected in STDOUT",
                                                  example = "[\"[a-z].*\",\"[0-9].:\"]",
                                                  name = "patterns",
                                                  type = "string[]"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The timeout ( in seconds ) to wait for the patterns to be found",
                                                  example = "3",
                                                  name = "timeoutSeconds",
                                                  type = "integer")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully execute operation expect All STDOUT by REGEX details",
                                       description = "Successfully execute operation expect All STDOUT by REGEX",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfull operation 'expect all by regex' over STDOUT",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while executing operation expect All STDERR by REGEX details",
                                       description = "Error while executing operation expect All by REGEX STDOUT",
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
    public Response expectAllStdoutByRegex( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String[] regexPatterns = null;
        int timeoutSeconds = -1;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsInt();
            if (resourceId < 0) {
                throw new NoSuchElementException("resourceId must be >= 0, but was " + resourceId);
            }
            regexPatterns = GSON.fromJson(getJsonElement(jsonObject, "regexPatterns"), String[].class);
            try {
                timeoutSeconds = getJsonElement(jsonObject, "timeoutSeconds").getAsInt();
                ProcessesTalkersManager.expectAllByRegex(sessionId, resourceId, regexPatterns, timeoutSeconds);
            } catch (NoSuchElementException e) {
                // no timeoutSeconds provided with the request. The default one will be used
                ProcessesTalkersManager.expectAllByRegex(sessionId, resourceId, regexPatterns);
            }
            return Response.ok("{\"status_message\":\"successfull operation 'expect all by regex' over STDOUT\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to execute 'expect all by regex' operation over STDOUT for process talker '"
                             + resourceId
                             + "' in session with id '"
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

    @POST
    @Path( "content/stdout/expect/any")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Expect Any STDOUT details",
            summary = "Expect Any STDOUT",
            url = "content/stdout/expect/any")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "<HOST_NAME>_<UUID_VALUE>_<THREAD_NAME>",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID",
                                                  example = "123",
                                                  name = "resourceId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Array of patterns, one or more of which to be expected in STDERR",
                                                  example = "[\"some_string\",\"other_string\"]",
                                                  name = "patterns",
                                                  type = "string[]"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The timeout ( in seconds ) to wait for the patterns to be found",
                                                  example = "3",
                                                  name = "timeoutSeconds",
                                                  type = "integer")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully execute operation expect Any STDOUT details",
                                       description = "Successfully execute operation expect Any STDOUT",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfull operation 'expect any' over STDOUT",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while executing operation expect Any STDOUT details",
                                       description = "Error while executing operation expect Any STDOUT",
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
    public Response expectAnyStdout( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String[] patterns = null;
        int timeoutSeconds = -1;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsInt();
            if (resourceId < 0) {
                throw new NoSuchElementException("resourceId must be >= 0, but was " + resourceId);
            }
            patterns = GSON.fromJson(getJsonElement(jsonObject, "patterns"), String[].class);
            int result = -1;
            try {
                timeoutSeconds = getJsonElement(jsonObject, "timeoutSeconds").getAsInt();
                result = ProcessesTalkersManager.expectAny(sessionId, resourceId, patterns, timeoutSeconds);
            } catch (NoSuchElementException e) {
                // no timeoutSeconds provided with the request. The default one will be used
                result = ProcessesTalkersManager.expectAny(sessionId, resourceId, patterns);
            }
            return Response.ok("{\"action_result\":" + result + "}").build();
        } catch (Exception e) {
            String message = "Unable to execute 'expect any' operation over STDOUT for process talker '" + resourceId
                             + "' in session with id '"
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

    @POST
    @Path( "content/stdout/expect/any/byRegex")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Expect Any by Regex STDOUT details",
            summary = "Expect Any by Regex STDOUT",
            url = "content/stdout/expect/any/byRegex")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "<HOST_NAME>_<UUID_VALUE>_<THREAD_NAME>",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID",
                                                  example = "123",
                                                  name = "resourceId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Array of REGEX patterns, one or more of which to be expected in STDOUT",
                                                  example = "[\"some_string\",\"other_string\"]",
                                                  name = "patterns",
                                                  type = "string[]"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The timeout ( in seconds ) to wait for the patterns to be found",
                                                  example = "3",
                                                  name = "timeoutSeconds",
                                                  type = "integer")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully execute operation expect Any by REGEX STDOUT details",
                                       description = "Successfully execute operation expect Any by REGEX STDOUT",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfull operation 'expect any by regex' over STDOUT",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while executing operation expect Any by REGEX STDOUT details",
                                       description = "Error while executing operation expect Any by REGEX STDOUT",
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

    public Response expectAnyStdoutByRegex( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String[] patterns = null;
        int timeoutSeconds = -1;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsInt();
            if (resourceId < 0) {
                throw new NoSuchElementException("resourceId must be >= 0, but was " + resourceId);
            }
            patterns = GSON.fromJson(getJsonElement(jsonObject, "regexPatterns"), String[].class);
            int result = -1;
            try {
                timeoutSeconds = getJsonElement(jsonObject, "timeoutSeconds").getAsInt();
                result = ProcessesTalkersManager.expectAnyByRegex(sessionId, resourceId, patterns, timeoutSeconds);
            } catch (NoSuchElementException e) {
                // no timeoutSeconds provided with the request. The default one will be used
                result = ProcessesTalkersManager.expectAnyByRegex(sessionId, resourceId, patterns);
            }
            return Response.ok("{\"action_result\":" + result + "}").build();
        } catch (Exception e) {
            String message = "Unable to execute 'expect any by regex' operation over STDOUT for process talker '"
                             + resourceId
                             + "' in session with id '"
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

    @POST
    @Path( "expect/closed")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Expect Close details",
            summary = "Expect Close",
            url = "closed/expect")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "some session ID",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID",
                                                  example = "123",
                                                  name = "resourceId",
                                                  type = "integer"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The timeout ( in seconds ) to wait for the process talker to close",
                                                  example = "10",
                                                  name = "timeOutSeconds",
                                                  type = "integer")
    })
    public Response expectClose( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        int timeoutSeconds = -1;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsInt();
            if (resourceId < 0) {
                throw new NoSuchElementException("resourceId must be >= 0, but was " + resourceId);
            }
            try {
                timeoutSeconds = getJsonElement(jsonObject, "timeOutSeconds").getAsInt();
                ProcessesTalkersManager.expectClose(sessionId, resourceId, timeoutSeconds);
            } catch (NoSuchElementException e) {
                // no timeoutSeconds provided with the request. The default one will be used
                ProcessesTalkersManager.expectClose(sessionId, resourceId);
            }
            return Response.ok("{\"status_message\":\"successfull operation 'expect close'\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to execute 'expect close' operation for process talker '"
                             + resourceId
                             + "' in session with id '"
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

    @GET
    @Path( "closed")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Is process talker closed",
            url = "closed")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "some session ID",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID",
                                                  example = "123",
                                                  name = "resourceId",
                                                  type = "integer")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully check whether process talker is closed details",
                                       description = "Successfully check whether process talker is closed",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The session ID",
                                               example = "some session ID",
                                               name = "sessionId",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while checking whether process talker is closed details",
                                       description = "",
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
    public Response isClosed( @Context HttpServletRequest request,
                              @QueryParam(
                                      value = "sessionId") String sessionId,
                              @QueryParam(
                                      value = "resourceId") int resourceId ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new NoSuchElementException("resourceId must be >= 0, but was " + resourceId);
            }
            boolean closed = ProcessesTalkersManager.isClosed(sessionId, resourceId);
            return Response.ok("{\"action_result\":" + closed + "}").build();
        } catch (Exception e) {
            String message = "Unable to execute 'isClosed' operation for process talker '"
                             + resourceId
                             + "' in session with id '"
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

    @GET
    @Path( "exitValue")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get exit value",
            url = "exitValue")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "some session ID",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID",
                                                  example = "123",
                                                  name = "resourceId",
                                                  type = "integer")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Sucessfully get process talker exit value (code) details",
                                       description = "Sucessfully get process talker exit value (code)",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The exit code",
                                               example = "0",
                                               name = "action_result",
                                               type = "integer") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting process talker exit value (code) details",
                                       description = "Error while getting process talker exit value (code)",
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
    public Response getExitValue( @Context HttpServletRequest request,
                                  @QueryParam(
                                          value = "sessionId") String sessionId,
                                  @QueryParam(
                                          value = "resourceId") int resourceId ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new NoSuchElementException("resourceId must be >= 0, but was " + resourceId);
            }
            int exitValue = ProcessesTalkersManager.getExitValue(sessionId, resourceId);
            return Response.ok("{\"action_result\":" + exitValue + "}").build();
        } catch (Exception e) {
            String message = "Unable to execute 'getExitValue' operation for process talker '"
                             + resourceId
                             + "' in session with id '"
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

    @POST
    @Path( "kill")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Kill the process details",
            summary = "Kill the process",
            url = "kill")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "some session ID",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID",
                                                  example = "123",
                                                  name = "resourceId",
                                                  type = "integer")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully kill process talker details",
                                       description = "Successfully kill process talker",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfull operation 'kill'",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while killing process talker details",
                                       description = "Error while killing process talker",
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
    public Response kill( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsInt();
            if (resourceId < 0) {
                throw new NoSuchElementException("resourceId must be >= 0, but was " + resourceId);
            }
            ProcessesTalkersManager.kill(sessionId, resourceId);
            return Response.ok("{\"status_message\":\"successfull operation 'kill'\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to execute 'killExternalProcess' operation for process talker '"
                             + resourceId
                             + "' in session with id '"
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

    @POST
    @Path( "kill/all")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Kill the process and its children details",
            summary = "Kill the process and its children",
            url = "kill/all")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "some session ID",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID",
                                                  example = "123",
                                                  name = "resourceId",
                                                  type = "integer")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully kill process talker and its children details",
                                       description = "Successfully kill process talker nd its children",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfull operation 'kill process and its children'",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while killing process talker and its children details",
                                       description = "Error while killing process talker and its children",
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
    public Response killWithChildren( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsInt();
            if (resourceId < 0) {
                throw new NoSuchElementException("resourceId must be >= 0, but was " + resourceId);
            }
            ProcessesTalkersManager.killWithChildren(sessionId, resourceId);
            return Response.ok("{\"status_message\":\"successfull operation 'kill process and its children'\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to execute 'kill process and its children' operation for process talker '"
                             + resourceId
                             + "' in session with id '"
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

    @POST
    @Path( "send")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Send some text to process details",
            summary = "Send some text to process",
            url = "send")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "some session ID",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID",
                                                  example = "123",
                                                  name = "resourceId",
                                                  type = "integer"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The text (string) to send to the process talker",
                                                  example = "Some text",
                                                  name = "text",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully send text to process talker details",
                                       description = "Successfully send text to process talker",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfull operation 'send'",
                                               name = "action_result",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while sending text to process talker details",
                                       description = "Error while sending text to process talker",
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
    public Response send( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String text = null;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsInt();
            if (resourceId < 0) {
                throw new NoSuchElementException("resourceId must be >= 0, but was " + resourceId);
            }
            text = getJsonElement(jsonObject, "text").getAsString();
            ProcessesTalkersManager.send(sessionId, resourceId, text);
            return Response.ok("{\"status_message\":\"successfull operation 'send'\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to execute 'send' operation for process talker '"
                             + resourceId
                             + "' in session with id '"
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

    @POST
    @Path( "send/enter")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Send ENTER key to process details",
            summary = "Send ENTER key to process",
            url = "send/enter")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "some session ID",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID",
                                                  example = "123",
                                                  name = "resourceId",
                                                  type = "integer")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully send ENTER key to process talker details",
                                       description = "Successfully send ENTER key to process talker",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfull operation 'send ENTER key'",
                                               name = "action_result",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while sending ENTER key to process talker details",
                                       description = "Error while sending ENTER key to process talker",
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
    public Response sendEnter( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsInt();
            if (resourceId < 0) {
                throw new NoSuchElementException("resourceId must be >= 0, but was " + resourceId);
            }
            ProcessesTalkersManager.sendEnter(sessionId, resourceId);
            return Response.ok("{\"status_message\":\"successfull operation 'send ENTER key'\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to execute 'send ENTER key' operation for process talker '"
                             + resourceId
                             + "' in session with id '"
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

    @POST
    @Path( "send/enter/loop")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Send ENTER key in Loop to process details",
            summary = "Send ENTER key in Loop to process",
            url = "send/enter/loop")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "some session ID",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID",
                                                  example = "123",
                                                  name = "resourceId",
                                                  type = "integer"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The pattern that will appear one or more times and after which an ENTER key must be send to the process talker",
                                                  example = "Press any key to continue",
                                                  name = "intermediatePattern",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The final pattern that must appear after the last sending of the Enter key to the process talker",
                                                  example = "Y/N?",
                                                  name = "finalPattern",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The maximum number of times to send the intermediate pattern to the process talker",
                                                  example = "10",
                                                  name = "maxLoopTimes",
                                                  type = "integer")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully send ENTER key in loop to process talker details",
                                       description = "Successfully send ENTER key in loop to process talker",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfull operation 'send ENTER key in Loop'",
                                               name = "action_result",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while sending ENTER key in loop to process talker details",
                                       description = "Error while sending ENTER key in loop to process talker",
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
    public Response sendEnterInLopp( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String intermediatePattern = null;
        String finalPattern = null;
        int maxLoopTimes = -1;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsInt();
            if (resourceId < 0) {
                throw new NoSuchElementException("resourceId must be >= 0, but was " + resourceId);
            }
            intermediatePattern = getJsonElement(jsonObject, "intermediatePattern").getAsString();
            if (StringUtils.isNullOrEmpty(intermediatePattern)) {
                throw new NoSuchElementException("intermediatePattern is not provided with the request");
            }
            finalPattern = getJsonElement(jsonObject, "finalPattern").getAsString();
            if (StringUtils.isNullOrEmpty(finalPattern)) {
                throw new NoSuchElementException("finalPattern is not provided with the request");
            }
            maxLoopTimes = getJsonElement(jsonObject, "maxLoopTimes").getAsInt();
            ProcessesTalkersManager.sendEnterInLoop(sessionId, resourceId, intermediatePattern, finalPattern,
                                                    maxLoopTimes);
            return Response.ok("{\"status_message\":\"successfull operation 'send ENTER key in Loop'\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to execute 'send ENTER key in Loop' operation for process talker '"
                             + resourceId
                             + "' in session with id '"
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

    private JsonElement getJsonElement( JsonObject object, String key ) {

        JsonElement element = object.get(key);
        if (element == null) {
            throw new NoSuchElementException("'" + key + "'" + " is not provided with the request");
        }
        return element;
    }

}
