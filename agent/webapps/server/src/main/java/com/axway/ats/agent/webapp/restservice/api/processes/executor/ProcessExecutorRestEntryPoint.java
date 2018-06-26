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
package com.axway.ats.agent.webapp.restservice.api.processes.executor;

import java.io.InputStreamReader;
import java.util.Arrays;
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

@Path( "processes/executors")
@SwaggerClass( "processes/executors")
public class ProcessExecutorRestEntryPoint {

    private static final Logger LOG  = Logger.getLogger(ProcessExecutorRestEntryPoint.class);

    private static final Gson   GSON = new Gson();

    @PUT
    @Path( "")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "put",
            parametersDefinition = "Initialize process executor details",
            summary = "Initialize process executor",
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
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "shell command arguments",
                                                  example = "[\"-nr test\"]",
                                                  name = "commandArguments",
                                                  type = "string[]") })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successful initialization of process executor details",
                                       description = "Successful initialization of process executor",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The resource ID of the initialized process executor",
                                               example = "123",
                                               name = "resourceId",
                                               type = "integer") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while initializing process executor details",
                                       description = "Error while initializing process executor",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non transiend class fields for java.lang.Throwable ( detailMessage, cause, etc)\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.mypoduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response
            initializeProcessExecutor( @Context HttpServletRequest request ) {

        String sessionId = null;
        String command = null;
        String[] commandArguments = null;
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
                    commandArguments = new String[]{};
                }
            } else {
                command = getJsonElement(jsonObject, "command").getAsString();
                commandArguments = GSON.fromJson(getJsonElement(jsonObject, "commandArguments"), String[].class);
            }

            int resourceId = ProcessesExecutorsManager.initializeProcessExecutor(sessionId, command, commandArguments);
            String response = "{\"resourceId\":" + resourceId + "}";

            return Response.ok(response).build();
        } catch (Exception e) {
            String message = null;
            if (defaultInitialization) {
                message = "Unable to perform default initialization for process executor in session with id '"
                          + sessionId + "'";
            }
            message = "Unable to initialize process executor for command '" + command
                      + "' with command arguments '" + Arrays.asList(commandArguments).toString() + "' "
                      + " in session with id '" + sessionId + "'";
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
    @Path( "start")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Start process executor details",
            summary = "Start process executor",
            url = "start")
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
                                                  type = "integer"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The directory from which the process will be started",
                                                  example = "/home/atsuser/",
                                                  name = "workDirectory",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "File which will store the process's STDOUT stream",
                                                  example = "/home/atsuser/stdout.txt",
                                                  name = "standardOutputFile",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "File which will store the process's STDERR stream",
                                                  example = "/home/atsuser/stderr.txt",
                                                  name = "errorOutputFile",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Toggle STDOUT logging",
                                                  example = "true|false",
                                                  name = "logStandardOutput",
                                                  type = "boolean"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Toggle STDERR logging",
                                                  example = "true|false",
                                                  name = "logErrorOutput",
                                                  type = "boolean"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Whether or not to wait for process to finish before returning control to the main Java program",
                                                  example = "true|false",
                                                  name = "waitForCompletion",
                                                  type = "boolean") })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successful start of process executor details",
                                       description = "Successful start of process executor",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "process successfully started",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while starting process executor details",
                                       description = "Error while starting process executor",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non transiend class fields for java.lang.Throwable ( detailMessage, cause, etc)\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.mypoduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response startProcess( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String workDirectory = null;
        String standardOutputFile = null;
        String errorOutputFile = null;
        boolean logStandardOutput = false;
        boolean logErrorOutput = false;
        boolean waitForCompletion = false;
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
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            workDirectory = GSON.fromJson(jsonObject.get("workDirectory"), String.class);
            standardOutputFile = GSON.fromJson(jsonObject.get("standardOutputFile"), String.class);
            errorOutputFile = GSON.fromJson(jsonObject.get("errorOutputFile"), String.class);
            logStandardOutput = GSON.fromJson(jsonObject.get("logStandardOutput"), boolean.class);
            logErrorOutput = GSON.fromJson(jsonObject.get("logErrorOutput"), boolean.class);
            waitForCompletion = GSON.fromJson(jsonObject.get("waitForCompletion"), boolean.class);
            ProcessesExecutorsManager.startProcess(sessionId, resourceId, workDirectory, standardOutputFile,
                                                   errorOutputFile, logStandardOutput, logErrorOutput,
                                                   waitForCompletion);

            return Response.ok("{\"status_message\":\"process successfully started\"}").build();
        } catch (Exception e) {
            String message = "Unable to start process with resourceId '" + resourceId + "' from session with id '"
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
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "",
            summary = "Kill process executor",
            url = "kill")
    @SwaggerMethodParameterDefinitions( { @SwaggerMethodParameterDefinition(
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
                                       definition = "Successful kill of process executor details",
                                       description = "Successful kill of process executor",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "process successfully killed",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while killing process executor details",
                                       description = "Error while killing process executor",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non transiend class fields for java.lang.Throwable ( detailMessage, cause, etc)\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.mypoduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response killProcess( @Context HttpServletRequest request ) {

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
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            ProcessesExecutorsManager.killProcess(sessionId, resourceId);

            return Response.ok("{\"status_message\":\"process successfully killed\"}").build();
        } catch (Exception e) {
            String message = "Unable to kill process with resourceId '" + resourceId + "' from session with id '"
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
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "",
            summary = "Kill process (executor)",
            url = "kill/all")
    @SwaggerMethodParameterDefinitions( { @SwaggerMethodParameterDefinition(
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
                                       definition = "Successful kill of process and its children details",
                                       description = "Successful kill of process and its children",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "process and its children successfully killed",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while killing process and its children details",
                                       description = "Error while killing process and its children",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non transiend class fields for java.lang.Throwable ( detailMessage, cause, etc)\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.mypoduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response killAll( @Context HttpServletRequest request ) {

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
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            ProcessesExecutorsManager.killProcessWithChildren(sessionId, resourceId);

            return Response.ok("{\"status_message\":\"process and its children successfully killed\"}").build();
        } catch (Exception e) {
            String message = "Unable to kill process ( and its children ) with resourceId '" + resourceId
                             + "' from session with id '"
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
    @Path( "kill/external")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Kill external process details",
            summary = "Kill external process",
            url = "kill/external")
    @SwaggerMethodParameterDefinitions( { @SwaggerMethodParameterDefinition(
            description = "The session ID",
            example = "HOST_ID:localhost:8089;THREAD_ID:main",
            name = "sessionId",
            type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID",
                                                  example = "1",
                                                  name = "resourceId",
                                                  type = "integer"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "start command snippet",
                                                  example = "grep -nr test",
                                                  name = "startCommandSnippet",
                                                  type = "string") })

    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successful kill of external process details",
                                       description = "Successful kill of external process",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "the number of killed external processes",
                                               example = "3",
                                               name = "action_result",
                                               type = "integer") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while killing external process details",
                                       description = "Error while killing external process",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non transiend class fields for java.lang.Throwable ( detailMessage, cause, etc )\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.mypoduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response killExternalProcess( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String startCommandSnippet = null;
        int numOfKilledProcesses = -1;
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
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            startCommandSnippet = getJsonElement(jsonObject, "startCommandSnippet").getAsString();
            if (StringUtils.isNullOrEmpty(startCommandSnippet)) {
                throw new NoSuchElementException("startCommandSnippet is not provided with the request");
            }
            numOfKilledProcesses = ProcessesExecutorsManager.killExternalProcess(sessionId, resourceId,
                                                                                 startCommandSnippet);

            return Response.ok("{\"action_result\":" + GSON.toJson(numOfKilledProcesses, int.class) + "}").build();
        } catch (Exception e) {
            String message = "Unable to kill external process with start command snippet '" + startCommandSnippet
                             + "' using resource with id '" + resourceId + "' from session with id '"
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
    @Path( "exitCode")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get process's exit code",
            url = "exitCode")
    @SwaggerMethodParameterDefinitions( { @SwaggerMethodParameterDefinition(
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
                                       definition = "Successful get of process exit code details",
                                       description = "Successful get of process exit code",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "the process exit code",
                                               example = "0",
                                               name = "action_result",
                                               type = "integer") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting process exit code details",
                                       description = "Error while getting process exit code",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non transiend class fields for java.lang.Throwable ( detailMessage, cause, etc )\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.mypoduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response getProcessExitCode( @Context HttpServletRequest request, @QueryParam( "sessionId") String sessionId,
                                        @QueryParam( "resourceId") int resourceId ) {

        int exitCode = -1;
        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            exitCode = ProcessesExecutorsManager.getProcessExitCode(sessionId, resourceId);

            return Response.ok("{\"action_result\":" + GSON.toJson(exitCode, int.class) + "}").build();
        } catch (Exception e) {
            String message = "Unable to get exit code for process with resourceId '" + resourceId
                             + "' from session with id '" + sessionId + "'";
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
    @Path( "pid")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get process's ID",
            url = "pid")
    @SwaggerMethodParameterDefinitions( { @SwaggerMethodParameterDefinition(
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
                                       definition = "Successful get of process id details",
                                       description = "Successful get of process id",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "the process id",
                                               example = "0",
                                               name = "action_result",
                                               type = "integer") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting process id details",
                                       description = "Error while getting process id",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non-transiend class fields for java.lang.Throwable ( detailMessage, cause, etc )\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.mypoduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response getProcessId( @Context HttpServletRequest request, @QueryParam( "sessionId") String sessionId,
                                  @QueryParam( "resourceId") int resourceId ) {

        int exitCode = -1;
        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            exitCode = ProcessesExecutorsManager.getProcessId(sessionId, resourceId);

            return Response.ok("{\"action_result\":" + GSON.toJson(exitCode, int.class) + "}").build();
        } catch (Exception e) {
            String message = "Unable to get pid for process with resourceId '" + resourceId
                             + "' from session with id '" + sessionId + "'";
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
    @Path( "stdout")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get process's STDOUT content",
            url = "stdout")
    @SwaggerMethodParameterDefinitions( { @SwaggerMethodParameterDefinition(
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
                                       definition = "Successful get of process STDOUT details",
                                       description = "Successful get of process STDOUT",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "the process STDOUT",
                                               example = "some STDOUT messages",
                                               name = "action_result",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting process STDOUT details",
                                       description = "Error while getting process STDOUT",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non-transiend class fields for java.lang.Throwable ( detailMessage, cause, etc )\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.mypoduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response getStandardOutput( @Context HttpServletRequest request, @QueryParam( "sessionId") String sessionId,
                                       @QueryParam( "resourceId") int resourceId ) {

        String stdout = null;
        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            stdout = ProcessesExecutorsManager.getStandardOutput(sessionId, resourceId);

            return Response.ok("{\"action_result\":" + GSON.toJson(stdout, stdout.getClass()) + "}").build();
        } catch (Exception e) {
            String message = "Unable to get standard output for process with resourceId '" + resourceId
                             + "' from session with id '" + sessionId + "'";
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
    @Path( "stdout/current")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get current process's STDOUT content",
            url = "stdout/current")
    @SwaggerMethodParameterDefinitions( { @SwaggerMethodParameterDefinition(
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
                                       definition = "Successful get of process current STDOUT details",
                                       description = "Successful get of process current STDOUT",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "the process current STDOUT",
                                               example = "some STDOUT messages",
                                               name = "action_result",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting process current STDOUT details",
                                       description = "Error while getting process current STDOUT",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non-transiend class fields for java.lang.Throwable ( detailMessage, cause, etc )\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.mypoduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response getCurrentStandardOutput( @Context HttpServletRequest request,
                                              @QueryParam( "sessionId") String sessionId,
                                              @QueryParam( "resourceId") int resourceId ) {

        String stdout = null;
        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            stdout = ProcessesExecutorsManager.getCurrentStandardOutput(sessionId, resourceId);

            return Response.ok("{\"action_result\":" + GSON.toJson(stdout, stdout.getClass()) + "}").build();
        } catch (Exception e) {
            String message = "Unable to get current standard output for process with resourceId '" + resourceId
                             + "' from session with id '" + sessionId + "'";
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
    @Path( "stdout/fullyread")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get whether process's STDOUT is fully read",
            url = "stdout/fullyread")
    @SwaggerMethodParameterDefinitions( { @SwaggerMethodParameterDefinition(
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
                                       definition = "Successful get whether process STDOUT is fully read details",
                                       description = "Successful get whether process STDOUT is fully read",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "whether the process STDERR is fully read",
                                               example = "TRUE|FALSE",
                                               name = "action_result",
                                               type = "boolean") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting whether process STDOUT is fully read details",
                                       description = "Error while getting whether process STDOUT is fully read",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non-transiend class fields for java.lang.Throwable ( detailMessage, cause, etc )\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.mypoduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response isStandardOutputFullyRead( @Context HttpServletRequest request,
                                               @QueryParam( "sessionId") String sessionId,
                                               @QueryParam( "resourceId") int resourceId ) {

        boolean fullyRead = false;
        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            fullyRead = ProcessesExecutorsManager.isStandardOutputFullyRead(sessionId, resourceId);

            return Response.ok("{\"action_result\":" + GSON.toJson(fullyRead, boolean.class) + "}").build();
        } catch (Exception e) {
            String message = "Unable to get whether standard output is fully read for process with resourceId '"
                             + resourceId
                             + "' from session with id '" + sessionId + "'";
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
    @Path( "stderr")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get process's STDERR content",
            url = "stderr")
    @SwaggerMethodParameterDefinitions( { @SwaggerMethodParameterDefinition(
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
                                       definition = "Successful get of process STDERR details",
                                       description = "Successful get of process STDERR",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "the process STDERR",
                                               example = "some STDERR messages",
                                               name = "action_result",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting process STDERR details",
                                       description = "Error while getting process STDERR",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non-transiend class fields for java.lang.Throwable ( detailMessage, cause, etc )\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.mypoduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response getStandardErrorOutput( @Context HttpServletRequest request,
                                            @QueryParam( "sessionId") String sessionId,
                                            @QueryParam( "resourceId") int resourceId ) {

        String stdout = null;
        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            stdout = ProcessesExecutorsManager.getStandardErrorOutput(sessionId, resourceId);

            return Response.ok("{\"action_result\":" + GSON.toJson(stdout, stdout.getClass()) + "}").build();
        } catch (Exception e) {
            String message = "Unable to get standard error output for process with resourceId '" + resourceId
                             + "' from session with id '" + sessionId + "'";
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
    @Path( "stderr/current")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get current process's STDERR content",
            url = "stderr/current")
    @SwaggerMethodParameterDefinitions( { @SwaggerMethodParameterDefinition(
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
                                       definition = "Successful get of process current STDERR details",
                                       description = "Successful get of process current STDERR",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "the process current STDERR",
                                               example = "some STDERR messages",
                                               name = "action_result",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting process current STDERR details",
                                       description = "Error while getting process current STDERR",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non-transiend class fields for java.lang.Throwable ( detailMessage, cause, etc )\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.mypoduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response getCurrentStandardErrorOutput( @Context HttpServletRequest request,
                                                   @QueryParam( "sessionId") String sessionId,
                                                   @QueryParam( "resourceId") int resourceId ) {

        String stdout = null;
        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            stdout = ProcessesExecutorsManager.getCurrentStandardErrorOutput(sessionId, resourceId);

            return Response.ok("{\"action_result\":" + GSON.toJson(stdout, stdout.getClass()) + "}").build();
        } catch (Exception e) {
            String message = "Unable to get current standard error output for process with resourceId '" + resourceId
                             + "' from session with id '" + sessionId + "'";
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
    @Path( "stderr/fullyread")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get whether process's STDERR is fully read",
            url = "stderr/fullyread")
    @SwaggerMethodParameterDefinitions( { @SwaggerMethodParameterDefinition(
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
                                       definition = "Successful get whether process STDERR is fully read details",
                                       description = "Successful get whether process STDERR is fully read",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "whether the process STDERR is fully read",
                                               example = "TRUE|FALSE",
                                               name = "action_result",
                                               type = "boolean") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting  whether process STDERR is fully read details",
                                       description = "Error while getting  whether process STDERR is fully read",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non-transiend class fields for java.lang.Throwable ( detailMessage, cause, etc )\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.mypoduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response isStandardErrorOutputFullyRead( @Context HttpServletRequest request,
                                                    @QueryParam( "sessionId") String sessionId,
                                                    @QueryParam( "resourceId") int resourceId ) {

        boolean fullyRead = false;
        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            fullyRead = ProcessesExecutorsManager.isStandardErrorOutputFullyRead(sessionId, resourceId);

            return Response.ok("{\"action_result\":" + GSON.toJson(fullyRead, boolean.class) + "}").build();
        } catch (Exception e) {
            String message = "Unable to get whether Standard error output is fully read for process with resourceId '"
                             + resourceId
                             + "' from session with id '" + sessionId + "'";
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
    @Path( "envvars")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get process environment variable's value",
            url = "envvars")
    @SwaggerMethodParameterDefinitions( { @SwaggerMethodParameterDefinition(
            description = "The session ID",
            example = "HOST_ID:localhost:8089;THREAD_ID:main",
            name = "sessionId",
            type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID",
                                                  example = "1",
                                                  name = "resourceId",
                                                  type = "integer"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The variable name. Such variable must already exists",
                                                  example = "my_var",
                                                  name = "variableName",
                                                  type = "string") })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successful get process environment variable details",
                                       description = "Successful get process environment variable",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The process environment variable's value",
                                               example = "some_env_var_value",
                                               name = "action_result",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting process environment variable details",
                                       description = "Error while getting process environment variable",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non-transiend class fields for java.lang.Throwable ( detailMessage, cause, etc )\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.mypoduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response getEnvironmentVariable( @Context HttpServletRequest request,
                                            @QueryParam( "sessionId") String sessionId,
                                            @QueryParam( "resourceId") int resourceId,
                                            @QueryParam( "variableName") String variableName ) {

        String envVar = null;
        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            if (StringUtils.isNullOrEmpty(variableName)) {
                throw new IllegalArgumentException("variableName not provided with the request");
            }
            envVar = ProcessesExecutorsManager.getEnvironmentVariable(sessionId, resourceId, variableName);

            return Response.ok("{\"action_result\":" + GSON.toJson(envVar, String.class) + "}").build();
        } catch (Exception e) {
            String message = "Unable to get environment variable '" + variableName + "' using resourceId '"
                             + resourceId
                             + "' from session with id '" + sessionId + "'";
            LOG.error(message, e);
            return Response.serverError()
                           .entity("{\"error\":" + GSON.toJson(e) + ", \"exceptionClass\":\"" + e.getClass().getName()
                                   + "\"}")
                           .build();
        } finally {
            ThreadsPerCaller.unregisterThread();
        }

    }

    @PUT
    @Path( "envvars")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "put",
            parametersDefinition = "Set process environment variable details",
            summary = "Set process environment variable",
            url = "envvars")
    @SwaggerMethodParameterDefinitions( { @SwaggerMethodParameterDefinition(
            description = "The session ID",
            example = "HOST_ID:localhost:8089;THREAD_ID:main",
            name = "sessionId",
            type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID",
                                                  example = "1",
                                                  name = "resourceId",
                                                  type = "integer"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The variable name",
                                                  example = "my_var",
                                                  name = "variableName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The variable value",
                                                  example = "my_value",
                                                  name = "variableValue",
                                                  type = "string") })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successful create of new process environment variable details",
                                       description = "Successful create of new process environment variable details",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "value of env variable set to <SOME_VALUE>",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while creating new process environment variable details",
                                       description = "Error while creating new process environment variable details",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non-transiend class fields for java.lang.Throwable ( detailMessage, cause, etc )\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.mypoduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response setEnvironmentVariable( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String variableName = null;
        String variableValue = null;
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
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            variableName = getJsonElement(jsonObject, "variableName").getAsString();
            if (StringUtils.isNullOrEmpty(variableName)) {
                throw new IllegalArgumentException("variableName not provided with the request");
            }
            variableValue = getJsonElement(jsonObject, "variableValue").getAsString();
            ProcessesExecutorsManager.setEnvironmentVariable(sessionId, resourceId, variableName, variableValue);

            return Response.ok("{\"status_message\":\"value of environment variable '" + variableName
                               + "' successfully set to '" + variableValue
                               + "'\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to set environment variable '" + variableName + "' using resourceId '"
                             + resourceId
                             + "' from session with id '" + sessionId + "'";
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
    @Path( "envvars")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Append a value to an existing process environment variable details",
            summary = "Append a value to an existing process environment variable",
            url = "envvars")
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
                                                  type = "integer"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The variable name. Such variable must already exists",
                                                  example = "my_var",
                                                  name = "variableName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The value which will be append to the existing one",
                                                  example = "additional_value",
                                                  name = "variableValueToAppend",
                                                  type = "string") })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successful append to process environment variable's value details",
                                       description = "Successful append to process environment variable's value",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while appending process environment variable's value details",
                                       description = "Error while appending process environment variable's value",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non-transiend class fields for java.lang.Throwable ( detailMessage, cause, etc )\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.mypoduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response appendToEnvironmentVariable( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String variableName = null;
        String variableValue = null;
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
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            variableName = getJsonElement(jsonObject, "variableName").getAsString();
            if (StringUtils.isNullOrEmpty(variableName)) {
                throw new IllegalArgumentException("variableName not provided with the request");
            }
            variableValue = getJsonElement(jsonObject, "variableValueToAppend").getAsString();
            ProcessesExecutorsManager.appendToEnvironmentVariable(sessionId, resourceId, variableName, variableValue);
        } catch (Exception e) {
            String message = "Unable to append to environment variable '" + variableName + "' using resourceId '"
                             + resourceId
                             + "' from session with id '" + sessionId + "'";
            LOG.error(message, e);
            return Response.serverError()
                           .entity("{\"error\":" + GSON.toJson(e) + ", \"exceptionClass\":\"" + e.getClass().getName()
                                   + "\"}")
                           .build();
        } finally {
            ThreadsPerCaller.unregisterThread();
        }

        return Response.ok("{\"status_message\":\"value '" + variableValue
                           + "' successfully appended to environment variable '"
                           + variableName + "'\"}")
                       .build();

    }

    private JsonElement getJsonElement( JsonObject object, String key ) {

        JsonElement element = object.get(key);
        if (element == null) {
            throw new NoSuchElementException("'" + key + "'" + " is not provided with the request");
        }
        return element;
    }

}
