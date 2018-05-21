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
package com.axway.ats.agent.webapp.restservice.api.processes;

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

import com.axway.ats.core.utils.StringUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Path( "processes/executors")
public class ProcessExecutorRestEntryPoint {

    private static final Logger LOG  = Logger.getLogger(ProcessExecutorRestEntryPoint.class);

    private static final Gson   GSON = new Gson();

    @PUT
    @Path( "")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
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
        }
    }

    @POST
    @Path( "start")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
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
        } catch (Exception e) {
            String message = "Unable to start process with resourceId '" + resourceId + "' from session with id '"
                             + sessionId + "'";
            LOG.error(message, e);
            return Response.serverError()
                           .entity("{\"error\":" + GSON.toJson(e) + ", \"exceptionClass\":\"" + e.getClass().getName()
                                   + "\"}")
                           .build();
        }
        return Response.ok("{\"status_message\":\"process successfully started\"}").build();

    }

    @POST
    @Path( "kill")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
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
            resourceId = getJsonElement(jsonObject, "resourceId").getAsInt();
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            ProcessesExecutorsManager.killProcess(sessionId, resourceId);
        } catch (Exception e) {
            String message = "Unable to kill process with resourceId '" + resourceId + "' from session with id '"
                             + sessionId + "'";
            LOG.error(message, e);
            return Response.serverError()
                           .entity("{\"error\":" + GSON.toJson(e) + ", \"exceptionClass\":\"" + e.getClass().getName()
                                   + "\"}")
                           .build();
        }
        return Response.ok("{\"status_message\":\"process successfully killed\"}").build();

    }

    @POST
    @Path( "kill/all")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
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
            resourceId = getJsonElement(jsonObject, "resourceId").getAsInt();
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            ProcessesExecutorsManager.killProcessWithChildren(sessionId, resourceId);
        } catch (Exception e) {
            String message = "Unable to kill process ( and its children ) with resourceId '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
            LOG.error(message, e);
            return Response.serverError()
                           .entity("{\"error\":" + GSON.toJson(e) + ", \"exceptionClass\":\"" + e.getClass().getName()
                                   + "\"}")
                           .build();
        }
        return Response.ok("{\"status_message\":\"process and its children successfully killed\"}").build();

    }

    @POST
    @Path( "kill/external")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
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
        } catch (Exception e) {
            String message = "Unable to kill external process with start command snippet '" + startCommandSnippet
                             + "' using resource with id '" + resourceId + "' from session with id '"
                             + sessionId + "'";
            LOG.error(message, e);
            return Response.serverError()
                           .entity("{\"error\":" + GSON.toJson(e) + ", \"exceptionClass\":\"" + e.getClass().getName()
                                   + "\"}")
                           .build();
        }
        return Response.ok("{\"action_result\":" + GSON.toJson(numOfKilledProcesses, int.class) + "}").build();

    }

    @GET
    @Path( "exitCode")
    @Produces( MediaType.APPLICATION_JSON)
    public Response getProcessExitCode( @Context HttpServletRequest request, @QueryParam( "sessionId") String sessionId,
                                        @QueryParam( "resourceId") int resourceId ) {

        int exitCode = -1;
        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            exitCode = ProcessesExecutorsManager.getProcessExitCode(sessionId, resourceId);
        } catch (Exception e) {
            String message = "Unable to get exit code for process with resourceId '" + resourceId
                             + "' from session with id '" + sessionId + "'";
            LOG.error(message, e);
            return Response.serverError()
                           .entity("{\"error\":" + GSON.toJson(e) + ", \"exceptionClass\":\"" + e.getClass().getName()
                                   + "\"}")
                           .build();
        }

        return Response.ok("{\"action_result\":" + GSON.toJson(exitCode, int.class) + "}").build();

    }

    @GET
    @Path( "pid")
    @Produces( MediaType.APPLICATION_JSON)
    public Response getProcessId( @Context HttpServletRequest request, @QueryParam( "sessionId") String sessionId,
                                  @QueryParam( "resourceId") int resourceId ) {

        int exitCode = -1;
        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            exitCode = ProcessesExecutorsManager.getProcessId(sessionId, resourceId);
        } catch (Exception e) {
            String message = "Unable to get pid for process with resourceId '" + resourceId
                             + "' from session with id '" + sessionId + "'";
            LOG.error(message, e);
            return Response.serverError()
                           .entity("{\"error\":" + GSON.toJson(e) + ", \"exceptionClass\":\"" + e.getClass().getName()
                                   + "\"}")
                           .build();
        }

        return Response.ok("{\"action_result\":" + GSON.toJson(exitCode, int.class) + "}").build();

    }

    @GET
    @Path( "stdout")
    @Produces( MediaType.APPLICATION_JSON)
    public Response getStandardOutput( @Context HttpServletRequest request, @QueryParam( "sessionId") String sessionId,
                                       @QueryParam( "resourceId") int resourceId ) {

        String stdout = null;
        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            stdout = ProcessesExecutorsManager.getStandardOutput(sessionId, resourceId);
        } catch (Exception e) {
            String message = "Unable to get standard output for process with resourceId '" + resourceId
                             + "' from session with id '" + sessionId + "'";
            LOG.error(message, e);
            return Response.serverError()
                           .entity("{\"error\":" + GSON.toJson(e) + ", \"exceptionClass\":\"" + e.getClass().getName()
                                   + "\"}")
                           .build();
        }

        return Response.ok("{\"action_result\":" + GSON.toJson(stdout, stdout.getClass()) + "}").build();

    }

    @GET
    @Path( "stdout/current")
    @Produces( MediaType.APPLICATION_JSON)
    public Response getCurrentStandardOutput( @Context HttpServletRequest request,
                                              @QueryParam( "sessionId") String sessionId,
                                              @QueryParam( "resourceId") int resourceId ) {

        String stdout = null;
        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            stdout = ProcessesExecutorsManager.getCurrentStandardOutput(sessionId, resourceId);
        } catch (Exception e) {
            String message = "Unable to get current standard output for process with resourceId '" + resourceId
                             + "' from session with id '" + sessionId + "'";
            LOG.error(message, e);
            return Response.serverError()
                           .entity("{\"error\":" + GSON.toJson(e) + ", \"exceptionClass\":\"" + e.getClass().getName()
                                   + "\"}")
                           .build();
        }

        return Response.ok("{\"action_result\":" + GSON.toJson(stdout, stdout.getClass()) + "}").build();

    }

    @GET
    @Path( "stdout/fullyread")
    @Produces( MediaType.APPLICATION_JSON)
    public Response isStandardOutputFullyRead( @Context HttpServletRequest request,
                                               @QueryParam( "sessionId") String sessionId,
                                               @QueryParam( "resourceId") int resourceId ) {

        boolean fullyRead = false;
        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            fullyRead = ProcessesExecutorsManager.isStandardOutputFullyRead(sessionId, resourceId);
        } catch (Exception e) {
            String message = "Unable to get whether standard output is fully read for process with resourceId '"
                             + resourceId
                             + "' from session with id '" + sessionId + "'";
            LOG.error(message, e);
            return Response.serverError()
                           .entity("{\"error\":" + GSON.toJson(e) + ", \"exceptionClass\":\"" + e.getClass().getName()
                                   + "\"}")
                           .build();
        }

        return Response.ok("{\"action_result\":" + GSON.toJson(fullyRead, boolean.class) + "}").build();

    }

    @GET
    @Path( "stderr")
    @Produces( MediaType.APPLICATION_JSON)
    public Response getStandardErrorOutput( @Context HttpServletRequest request,
                                            @QueryParam( "sessionId") String sessionId,
                                            @QueryParam( "resourceId") int resourceId ) {

        String stdout = null;
        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            stdout = ProcessesExecutorsManager.getStandardErrorOutput(sessionId, resourceId);
        } catch (Exception e) {
            String message = "Unable to get standard error output for process with resourceId '" + resourceId
                             + "' from session with id '" + sessionId + "'";
            LOG.error(message, e);
            return Response.serverError()
                           .entity("{\"error\":" + GSON.toJson(e) + ", \"exceptionClass\":\"" + e.getClass().getName()
                                   + "\"}")
                           .build();
        }

        return Response.ok("{\"action_result\":" + GSON.toJson(stdout, stdout.getClass()) + "}").build();

    }

    @GET
    @Path( "stderr/current")
    @Produces( MediaType.APPLICATION_JSON)
    public Response getCurrentStandardErrorOutput( @Context HttpServletRequest request,
                                                   @QueryParam( "sessionId") String sessionId,
                                                   @QueryParam( "resourceId") int resourceId ) {

        String stdout = null;
        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            stdout = ProcessesExecutorsManager.getCurrentStandardErrorOutput(sessionId, resourceId);
        } catch (Exception e) {
            String message = "Unable to get current standard error output for process with resourceId '" + resourceId
                             + "' from session with id '" + sessionId + "'";
            LOG.error(message, e);
            return Response.serverError()
                           .entity("{\"error\":" + GSON.toJson(e) + ", \"exceptionClass\":\"" + e.getClass().getName()
                                   + "\"}")
                           .build();
        }

        return Response.ok("{\"action_result\":" + GSON.toJson(stdout, stdout.getClass()) + "}").build();

    }

    @GET
    @Path( "stderr/fullyread")
    @Produces( MediaType.APPLICATION_JSON)
    public Response isStandardErrorOutputFullyRead( @Context HttpServletRequest request,
                                                    @QueryParam( "sessionId") String sessionId,
                                                    @QueryParam( "resourceId") int resourceId ) {

        boolean fullyRead = false;
        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            fullyRead = ProcessesExecutorsManager.isStandardErrorOutputFullyRead(sessionId, resourceId);
        } catch (Exception e) {
            String message = "Unable to get whether Standard error output is fully read for process with resourceId '"
                             + resourceId
                             + "' from session with id '" + sessionId + "'";
            LOG.error(message, e);
            return Response.serverError()
                           .entity("{\"error\":" + GSON.toJson(e) + ", \"exceptionClass\":\"" + e.getClass().getName()
                                   + "\"}")
                           .build();
        }

        return Response.ok("{\"action_result\":" + GSON.toJson(fullyRead, boolean.class) + "}").build();

    }

    @GET
    @Path( "envvars")
    @Produces( MediaType.APPLICATION_JSON)
    public Response getEnvironmentVariable( @Context HttpServletRequest request,
                                            @QueryParam( "sessionId") String sessionId,
                                            @QueryParam( "resourceId") int resourceId,
                                            @QueryParam( "variableName") String variableName ) {

        String envVar = null;
        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            if (StringUtils.isNullOrEmpty(variableName)) {
                throw new IllegalArgumentException("variableName not provided with the request");
            }
            envVar = ProcessesExecutorsManager.getEnvironmentVariable(sessionId, resourceId, variableName);
        } catch (Exception e) {
            String message = "Unable to get environment variable '" + variableName + "' using resourceId '"
                             + resourceId
                             + "' from session with id '" + sessionId + "'";
            LOG.error(message, e);
            return Response.serverError()
                           .entity("{\"error\":" + GSON.toJson(e) + ", \"exceptionClass\":\"" + e.getClass().getName()
                                   + "\"}")
                           .build();
        }

        return Response.ok("{\"action_result\":" + GSON.toJson(envVar, String.class) + "}").build();

    }

    @PUT
    @Path( "envvars")
    @Produces( MediaType.APPLICATION_JSON)
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
        } catch (Exception e) {
            String message = "Unable to set environment variable '" + variableName + "' using resourceId '"
                             + resourceId
                             + "' from session with id '" + sessionId + "'";
            LOG.error(message, e);
            return Response.serverError()
                           .entity("{\"error\":" + GSON.toJson(e) + ", \"exceptionClass\":\"" + e.getClass().getName()
                                   + "\"}")
                           .build();
        }

        return Response.ok("{\"status_message\":\"value of environment variable '" + variableName
                           + "' successfully set to '" + variableValue
                           + "'\"}")
                       .build();

    }

    @POST
    @Path( "envvars")
    @Produces( MediaType.APPLICATION_JSON)
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
        }

        return Response.ok("{\"status_message\":\"value '" + variableValue
                           + "' successfully appended to environment variable '"
                           + variableName + "'\"}")
                       .build();

    }

    private JsonElement getJsonElement( JsonObject object, String key ) {

        JsonElement element = object.get(key);
        if (element == null) {
            throw new NoSuchElementException(key + " is not provided with the request");
        }
        return element;
    }

}
