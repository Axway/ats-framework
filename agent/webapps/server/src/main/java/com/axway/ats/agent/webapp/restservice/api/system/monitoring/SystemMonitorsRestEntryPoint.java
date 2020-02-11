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
package com.axway.ats.agent.webapp.restservice.api.system.monitoring;

import java.io.InputStreamReader;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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

@SwaggerClass( "system/monitoring")
@Path( "system/monitoring")
public class SystemMonitorsRestEntryPoint {

    private static final Logger LOG  = Logger.getLogger(SystemMonitorsRestEntryPoint.class);
    private static final Gson   GSON = new Gson();

    @PUT
    @Path( "/")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod( httpOperation = "put", parametersDefinition = "Initialize system monitor resource details", summary = "Initialize system monitor resource", url = "/")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition( description = "The caller ID", example = "HOST_ID:localhost:8089;WORKDIR:C/users/atsuser/SOME_PROJECT_PATH;THREAD_ID:1;THREAD_NAME:main", name = "callerId", type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse( code = 200, definition = "Successfully initialize system monitor resource details", description = "Successfully initialize system monitor resource", parametersDefinitions = { @SwaggerMethodParameterDefinition( description = "The resource ID of the newly initialized resource", example = "123", name = "resourceId", type = "long") }),
                               @SwaggerMethodResponse( code = 500, definition = "Error while initializing system monitor resource details", description = "Error while initializing system monitor resource", parametersDefinitions = { @SwaggerMethodParameterDefinition( description = "The action Java exception object", example = "\"See the non-transient class fields for java.lang.Throwable ( detailMessage, cause, etc )\"", name = "error", type = "object"),
                                                                                                                                                                                                                                        @SwaggerMethodParameterDefinition( description = "The java exception class name", example = "com.myproduct.exception.NoEntryException", name = "exceptionClass", type = "string") })
    })
    public Response initialize( @Context HttpServletRequest request ) {

        String callerId = null;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(), "UTF-8"))
                                                    .getAsJsonObject();
            callerId = getJsonElement(jsonObject, "callerId").getAsString();
            if (StringUtils.isNullOrEmpty(callerId)) {
                throw new NoSuchElementException("callerId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(callerId);
            long resourceId = SystemMonitorsManager.initialize();
            return Response.ok("{\"resourceId\":" + resourceId + "}").build();
        } catch (Exception e) {
            String message = "Unable to initialize system monitor resource from caller with id '" + callerId + "'";
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
    @Path( "/")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod( httpOperation = "delete", parametersDefinition = "", summary = "Deinitialize system monitor resource", url = "/")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition( description = "The caller ID", example = "HOST_ID:localhost:8089;THREAD_ID:main", name = "callerId", type = "string"),
                                          @SwaggerMethodParameterDefinition( description = "The resource ID", example = "1", name = "resourceId", type = "long") })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse( code = 200, definition = "Successfull deinitialization of system monitor details", description = "Successfull deinitialization of system monitor", parametersDefinitions = {
                                                                                                                                                                                                                                    @SwaggerMethodParameterDefinition( description = "Status message", example = "System monitor with resource id '10' successfully deleted", name = "status_message", type = "string") }),
                               @SwaggerMethodResponse( code = 500, definition = "Error while deinitializing system monitor details", description = "Error while deinitializing system monitor", parametersDefinitions = { @SwaggerMethodParameterDefinition( description = "The action Java exception object", example = "\"See the non-transient class fields for java.lang.Throwable ( detailMessage, cause, etc )\"", name = "error", type = "object"),
                                                                                                                                                                                                                          @SwaggerMethodParameterDefinition( description = "The java exception class name", example = "com.mypoduct.exception.NoEntryException", name = "exceptionClass", type = "string") })
    })
    public Response deinitialize( @Context HttpServletRequest request,
                                  @QueryParam( "callerId") String callerId,
                                  @QueryParam( "resourceId") long resourceId ) {

        try {
            if (StringUtils.isNullOrEmpty(callerId)) {
                throw new NoSuchElementException("callerId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(callerId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId must be >= 0, but was" + resourceId);
            }

            resourceId = SystemMonitorsManager.deinitialize(resourceId);
            String response = "{\"status_message\":\"System monitor with resource id '" + resourceId
                              + "' successfully deleted\"}";
            return Response.ok(response).build();
        } catch (Exception e) {
            String message = "Unable to deinitialize system monitor with resource id'" + resourceId
                             + "' in caller with id '" + callerId + "'";
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
    @Path( "initializeMonitoringContext")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod( httpOperation = "post", parametersDefinition = "Initialize monitoring context details", summary = "Initialize monitoring context", url = "initializeMonitoringContext")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition( description = "The caller ID", example = "HOST_ID:localhost:8089;WORKDIR:C/users/atsuser/SOME_PROJECT_PATH;THREAD_ID:1;THREAD_NAME:main", name = "callerId", type = "string"),
                                          @SwaggerMethodParameterDefinition( description = "The resource ID", example = "1", name = "resourceId", type = "long")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse( code = 200, definition = "Successfull initialize monitoring context details", description = "Successfull initialize monitoring context", parametersDefinitions = {
                                                                                                                                                                                                                          @SwaggerMethodParameterDefinition( description = "Status message", example = "monitoring context for system monitor with resource id '10' successfully initialized", name = "status_message", type = "string") }),
                               @SwaggerMethodResponse( code = 500, definition = "Error while initializing monitoring context details", description = "Error while initializing monitoring context", parametersDefinitions = { @SwaggerMethodParameterDefinition( description = "The action Java exception object", example = "\"See the non-transient class fields for java.lang.Throwable ( detailMessage, cause, etc )\"", name = "error", type = "object"),
                                                                                                                                                                                                                              @SwaggerMethodParameterDefinition( description = "The java exception class name", example = "com.mypoduct.exception.NoEntryException", name = "exceptionClass", type = "string") })
    })
    public Response initializeMonitoringContext( @Context HttpServletRequest request ) {

        String callerId = null;
        long resourceId = -1;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            callerId = getJsonElement(jsonObject, "callerId").getAsString();
            if (StringUtils.isNullOrEmpty(callerId)) {
                throw new NoSuchElementException("callerId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(callerId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsLong();
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invalid value '" + resourceId + "'");
            }
            SystemMonitorsManager.initializeMonitoringContext(resourceId,
                                                              request.getLocalAddr() + ":" + request.getLocalPort());
            String response = "{\"status_message\":\"monitoring context successfully initialized\"}";
            return Response.ok(response).build();
        } catch (Exception e) {
            String message = "Unable to initialize monitoring context for system monitor with resource id '"
                             + resourceId
                             + "' in caller with id '" + callerId + "'";
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
    @Path( "schedule/monitoring/system")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod( httpOperation = "post", parametersDefinition = "Schedule system monitoring details", summary = "Schedule system monitoring", url = "schedule/monitoring/system")
    @SwaggerMethodParameterDefinitions( { /*TODO*/ })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse( code = 200, definition = "Successfull schedule system monitoring details", description = "Successfull schedule system monitoring", parametersDefinitions = {
                                                                                                                                                                                                                    @SwaggerMethodParameterDefinition( description = "Status message", example = "successfully scheduled system monitoring", name = "status_message", type = "string") }),
                               @SwaggerMethodResponse( code = 500, definition = "Error while scheduling system monitoring details", description = "Error while scheduling system monitoring", parametersDefinitions = { @SwaggerMethodParameterDefinition( description = "The action Java exception object", example = "\"See the non-transient class fields for java.lang.Throwable ( detailMessage, cause, etc )\"", name = "error", type = "object"),
                                                                                                                                                                                                                        @SwaggerMethodParameterDefinition( description = "The java exception class name", example = "com.mypoduct.exception.NoEntryException", name = "exceptionClass", type = "string") })
    })
    public Response scheduleSystemMonitoring( @Context HttpServletRequest request ) {

        String callerId = null;
        long resourceId = -1;
        String[] systemReadingTypes = null;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            callerId = getJsonElement(jsonObject, "callerId").getAsString();
            if (StringUtils.isNullOrEmpty(callerId)) {
                throw new NoSuchElementException("callerId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(callerId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsLong();
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invalid value '" + resourceId + "'");
            }
            systemReadingTypes = GSON.fromJson(getJsonElement(jsonObject, "systemReadingTypes"), String[].class);
            SystemMonitorsManager.scheduleSystemMonitoring(resourceId, systemReadingTypes,
                                                           request.getLocalAddr() + ":" + request.getLocalPort());
            String response = "{\"status_message\":\"successfully scheduled system monitoring\"}";
            return Response.ok(response).build();
        } catch (Exception e) {
            String message = "Unable to schedule system monitoring for system monitor with resource id'" + resourceId
                             + "' in caller with id '" + callerId + "'";
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
    @Path( "schedule/monitoring")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod( httpOperation = "post", parametersDefinition = "Schedule monitoring details", summary = "Schedule monitoring", url = "schedule/monitoring")
    @SwaggerMethodParameterDefinitions( { /*TODO*/ })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse( code = 200, definition = "Successfull schedule monitoring details", description = "Successfull schedule monitoring", parametersDefinitions = {
                                                                                                                                                                                                      @SwaggerMethodParameterDefinition( description = "Status message", example = "successfully scheduled monitoring", name = "status_message", type = "string") }),
                               @SwaggerMethodResponse( code = 500, definition = "Error while scheduling monitoring details", description = "Error while scheduling monitoring", parametersDefinitions = { @SwaggerMethodParameterDefinition( description = "The action Java exception object", example = "\"See the non-transient class fields for java.lang.Throwable ( detailMessage, cause, etc )\"", name = "error", type = "object"),
                                                                                                                                                                                                          @SwaggerMethodParameterDefinition( description = "The java exception class name", example = "com.mypoduct.exception.NoEntryException", name = "exceptionClass", type = "string") })
    })
    public Response scheduleMonitoring( @Context HttpServletRequest request ) {

        String callerId = null;
        long resourceId = -1;
        String readingType = null;
        Map<String, String> readingParameters = null;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            callerId = getJsonElement(jsonObject, "callerId").getAsString();
            if (StringUtils.isNullOrEmpty(callerId)) {
                throw new NoSuchElementException("callerId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(callerId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsLong();
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invalid value '" + resourceId + "'");
            }
            readingType = getJsonElement(jsonObject, "readingType").getAsString();
            if (StringUtils.isNullOrEmpty(readingType)) {
                throw new NoSuchElementException("readingType is not provided with the request");
            }
            readingParameters = GSON.fromJson(getJsonElement(jsonObject, "readingParameters"), Map.class);
            SystemMonitorsManager.scheduleMonitoring(resourceId, readingType, readingParameters,
                                                     request.getLocalAddr() + ":" + request.getLocalPort());
            String response = "{\"status_message\":\"successfully scheduled monitoring\"}";
            return Response.ok(response).build();
        } catch (Exception e) {
            String message = "Unable to schedule monitoring for system monitor with resource id'" + resourceId
                             + "' in caller with id '" + callerId + "'";
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
    @Path( "schedule/monitoring/process")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod( httpOperation = "post", parametersDefinition = "Schedule process monitoring details", summary = "Schedule process monitoring", url = "schedule/monitoring/process")
    @SwaggerMethodParameterDefinitions( { /*TODO*/ })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse( code = 200, definition = "Successfull schedule process monitoring details", description = "Successfull schedule process monitoring", parametersDefinitions = {
                                                                                                                                                                                                                      @SwaggerMethodParameterDefinition( description = "Status message", example = "successfully scheduled process monitoring", name = "status_message", type = "string") }),
                               @SwaggerMethodResponse( code = 500, definition = "Error while scheduling process monitoring details", description = "Error while scheduling process monitoring", parametersDefinitions = { @SwaggerMethodParameterDefinition( description = "The action Java exception object", example = "\"See the non-transient class fields for java.lang.Throwable ( detailMessage, cause, etc )\"", name = "error", type = "object"),
                                                                                                                                                                                                                          @SwaggerMethodParameterDefinition( description = "The java exception class name", example = "com.mypoduct.exception.NoEntryException", name = "exceptionClass", type = "string") })
    })
    public Response scheduleProcessMonitoring( @Context HttpServletRequest request ) {

        String callerId = null;
        long resourceId = -1;
        String parentProcess = null;
        String processPattern = null;
        String processAlias = null;
        String processUsername = null;
        String[] processReadingTypes = null;
        boolean hasUsername = false;
        boolean isChildProcessMonitoring = false;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            callerId = getJsonElement(jsonObject, "callerId").getAsString();
            if (StringUtils.isNullOrEmpty(callerId)) {
                throw new NoSuchElementException("callerId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(callerId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsLong();
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invalid value '" + resourceId + "'");
            }
            processPattern = getJsonElement(jsonObject, "processPattern").getAsString();
            if (StringUtils.isNullOrEmpty(processPattern)) {
                throw new NoSuchElementException("processPattern is not provided with the request");
            }
            processAlias = getJsonElement(jsonObject, "processAlias").getAsString();
            if (StringUtils.isNullOrEmpty(processAlias)) {
                throw new NoSuchElementException("processAlias is not provided with the request");
            }
            processReadingTypes = GSON.fromJson(getJsonElement(jsonObject, "processReadingTypes"), String[].class);
            try {
                processUsername = getJsonElement(jsonObject, "processUsername").getAsString();
                if (StringUtils.isNullOrEmpty(processUsername)) {
                    throw new NoSuchElementException("processUsername is not provided with the request");
                }
                hasUsername = true;
            } catch (Exception e) {
                // the parameter processUsername is optional
                // so do not throw an exception if it is not presented in the request
                hasUsername = false;
            }
            try {
                parentProcess = getJsonElement(jsonObject, "parentProcess").getAsString();
                if (StringUtils.isNullOrEmpty(parentProcess)) {
                    throw new NoSuchElementException("parentProcess is not provided with the request");
                }
                isChildProcessMonitoring = true;
            } catch (Exception e) {
                // the parameter processUsername is optional
                // so do not throw an exception if it is not presented in the request
                isChildProcessMonitoring = false;
            }

            if (hasUsername) {
                if (isChildProcessMonitoring) {
                    SystemMonitorsManager.scheduleChildProcessMonitoring(resourceId, parentProcess,
                                                                         processPattern, processAlias, processUsername,
                                                                         processReadingTypes,
                                                                         request.getLocalAddr() + ":"
                                                                                              + request.getLocalPort());
                } else {
                    SystemMonitorsManager.scheduleProcessMonitoring(resourceId, processPattern, processAlias,
                                                                    processUsername,
                                                                    processReadingTypes,
                                                                    request.getLocalAddr() + ":"
                                                                                         + request.getLocalPort());
                }
            } else {
                if (isChildProcessMonitoring) {
                    SystemMonitorsManager.scheduleChildProcessMonitoring(resourceId, parentProcess,
                                                                         processPattern, processAlias,
                                                                         processReadingTypes,
                                                                         request.getLocalAddr() + ":"
                                                                                              + request.getLocalPort());
                } else {
                    SystemMonitorsManager.scheduleProcessMonitoring(resourceId, processPattern, processAlias,
                                                                    processReadingTypes,
                                                                    request.getLocalAddr() + ":"
                                                                                         + request.getLocalPort());
                }
            }

            String response = "{\"status_message\":\"successfully scheduled process monitoring\"}";
            return Response.ok(response).build();
        } catch (Exception e) {
            String message = "Unable to schedule process monitoring for system monitor with resource id'" + resourceId
                             + "' in caller with id '" + callerId + "'";
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
    @Path( "schedule/monitoring/jvm")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod( httpOperation = "post", parametersDefinition = "Schedule JVM monitoring details", summary = "Schedule JVM monitoring", url = "schedule/monitoring/jvm")
    @SwaggerMethodParameterDefinitions( { /*TODO*/ })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse( code = 200, definition = "Successfull schedule JVM monitoring details", description = "Successfull schedule JVM monitoring", parametersDefinitions = {
                                                                                                                                                                                                              @SwaggerMethodParameterDefinition( description = "Status message", example = "successfully scheduled JVM monitoring", name = "status_message", type = "string") }),
                               @SwaggerMethodResponse( code = 500, definition = "Error while scheduling JVM monitoring details", description = "Error while scheduling JVM monitoring", parametersDefinitions = { @SwaggerMethodParameterDefinition( description = "The action Java exception object", example = "\"See the non-transient class fields for java.lang.Throwable ( detailMessage, cause, etc )\"", name = "error", type = "object"),
                                                                                                                                                                                                                  @SwaggerMethodParameterDefinition( description = "The java exception class name", example = "com.mypoduct.exception.NoEntryException", name = "exceptionClass", type = "string") })
    })
    public Response scheduleJvmMonitoring( @Context HttpServletRequest request ) {

        String callerId = null;
        long resourceId = -1;
        String jvmPort = null;
        String alias = null;
        String[] jvmReadingTypes = null;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            callerId = getJsonElement(jsonObject, "callerId").getAsString();
            if (StringUtils.isNullOrEmpty(callerId)) {
                throw new NoSuchElementException("callerId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(callerId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsLong();
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invalid value '" + resourceId + "'");
            }
            jvmPort = getJsonElement(jsonObject, "jvmPort").getAsString();
            if (StringUtils.isNullOrEmpty(jvmPort)) {
                throw new NoSuchElementException("jvmPort is not provided with the request");
            }
            jvmReadingTypes = GSON.fromJson(getJsonElement(jsonObject, "jvmReadingTypes"), String[].class);
            try {
                alias = getJsonElement(jsonObject, "alias").getAsString();
                if (StringUtils.isNullOrEmpty(jvmPort)) {
                    throw new NoSuchElementException("alias is not provided with the request");
                }
                SystemMonitorsManager.scheduleJvmMonitoring(resourceId, jvmPort, alias, jvmReadingTypes,
                                                            request.getLocalAddr() + ":" + request.getLocalPort());
            } catch (Exception e) {
                // optional parameter
                // do not throw an exception
                SystemMonitorsManager.scheduleJvmMonitoring(resourceId, jvmPort, "", jvmReadingTypes,
                                                            request.getLocalAddr() + ":" + request.getLocalPort());
            }
            String response = "{\"status_message\":\"successfully scheduled JVM monitoring\"}";
            return Response.ok(response).build();
        } catch (Exception e) {
            String message = "Unable to schedule JVM monitoring for system monitor with resource id'" + resourceId
                             + "' in caller with id '" + callerId + "'";
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
    @Path( "schedule/monitoring/jvm/custom")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod( httpOperation = "post", parametersDefinition = "Schedule custom JVM monitoring details", summary = "Schedule custom JVM monitoring", url = "schedule/monitoring/jvm/custom")
    @SwaggerMethodParameterDefinitions( { /*TODO*/ })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse( code = 200, definition = "Successfull schedule custom JVM monitoring details", description = "Successfull schedule custom JVM monitoring", parametersDefinitions = {
                                                                                                                                                                                                                            @SwaggerMethodParameterDefinition( description = "Status message", example = "successfully scheduled custom JVM monitoring", name = "status_message", type = "string") }),
                               @SwaggerMethodResponse( code = 500, definition = "Error while scheduling custom JVM monitoring details", description = "Error while scheduling custom JVM monitoring", parametersDefinitions = { @SwaggerMethodParameterDefinition( description = "The action Java exception object", example = "\"See the non-transient class fields for java.lang.Throwable ( detailMessage, cause, etc )\"", name = "error", type = "object"),
                                                                                                                                                                                                                                @SwaggerMethodParameterDefinition( description = "The java exception class name", example = "com.mypoduct.exception.NoEntryException", name = "exceptionClass", type = "string") })
    })
    public Response scheduleCustomJvmMonitoring( @Context HttpServletRequest request ) {

        String callerId = null;
        long resourceId = -1;
        String jmxPort = null;
        String alias = null;
        String mbeanName = null;
        String unit = null;
        String[] mbeanAttributes = null;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            callerId = getJsonElement(jsonObject, "callerId").getAsString();
            if (StringUtils.isNullOrEmpty(callerId)) {
                throw new NoSuchElementException("callerId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(callerId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsLong();
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invalid value '" + resourceId + "'");
            }
            // TODO
            jmxPort = getJsonElement(jsonObject, "jmxPort").getAsString();
            if (StringUtils.isNullOrEmpty(jmxPort)) {
                throw new NoSuchElementException("jmxPort is not provided with the request");
            }
            alias = getJsonElement(jsonObject, "alias").getAsString();
            if (StringUtils.isNullOrEmpty(alias)) {
                throw new NoSuchElementException("alias is not provided with the request");
            }
            mbeanName = getJsonElement(jsonObject, "mbeanName").getAsString();
            if (StringUtils.isNullOrEmpty(mbeanName)) {
                throw new NoSuchElementException("mbeanName is not provided with the request");
            }
            unit = getJsonElement(jsonObject, "unit").getAsString();
            if (StringUtils.isNullOrEmpty(unit)) {
                throw new NoSuchElementException("unit is not provided with the request");
            }
            mbeanAttributes = GSON.fromJson(getJsonElement(jsonObject, "mbeanAttributes"), String[].class);
            SystemMonitorsManager.scheduleCustomJvmMonitoring(resourceId, jmxPort, alias, mbeanName, unit,
                                                              mbeanAttributes,
                                                              request.getLocalAddr() + ":" + request.getLocalPort());
            String response = "{\"status_message\":\"successfully scheduled custom JVM monitoring\"}";
            return Response.ok(response).build();
        } catch (Exception e) {
            String message = "Unable to schedule custom JVM monitoring for system monitor with resource id'"
                             + resourceId
                             + "' in caller with id '" + callerId + "'";
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
    @Path( "schedule/userActivity")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod( httpOperation = "post", parametersDefinition = "Schedule user activity details", summary = "Schedule user activity", url = "schedule/userActivity")
    @SwaggerMethodParameterDefinitions( { /*TODO*/ })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse( code = 200, definition = "Successfull schedule user activity details", description = "Successfull schedule user activity", parametersDefinitions = {
                                                                                                                                                                                                            @SwaggerMethodParameterDefinition( description = "Status message", example = "successfully scheduled user activity monitoring", name = "status_message", type = "string") }),
                               @SwaggerMethodResponse( code = 500, definition = "Error while scheduling user activity monitoring details", description = "Error while scheduling user activity monitoring", parametersDefinitions = { @SwaggerMethodParameterDefinition( description = "The action Java exception object", example = "\"See the non-transient class fields for java.lang.Throwable ( detailMessage, cause, etc )\"", name = "error", type = "object"),
                                                                                                                                                                                                                                      @SwaggerMethodParameterDefinition( description = "The java exception class name", example = "com.mypoduct.exception.NoEntryException", name = "exceptionClass", type = "string") })
    })
    public Response scheduleUserActivity( @Context HttpServletRequest request ) {

        String callerId = null;
        long resourceId = -1;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            callerId = getJsonElement(jsonObject, "callerId").getAsString();
            if (StringUtils.isNullOrEmpty(callerId)) {
                throw new NoSuchElementException("callerId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(callerId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsLong();
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invalid value '" + resourceId + "'");
            }
            SystemMonitorsManager.scheduleUserActivity(resourceId,
                                                       request.getLocalAddr() + ":" + request.getLocalPort());
            String response = "{\"status_message\":\"successfully scheduled user activity monitoring\"}";
            return Response.ok(response).build();
        } catch (Exception e) {
            String message = "Unable to schedule monitoring for system monitor with resource id'" + resourceId
                             + "' in caller with id '" + callerId + "'";
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
    @SwaggerMethod( httpOperation = "post", parametersDefinition = "Start monitoring details", summary = "Start monitoring", url = "start")
    @SwaggerMethodParameterDefinitions( { /*TODO*/ })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse( code = 200, definition = "Successfull start monitoring details", description = "Successfull start monitoring", parametersDefinitions = {
                                                                                                                                                                                                @SwaggerMethodParameterDefinition( description = "Status message", example = "monitoring successfully started", name = "status_message", type = "string") }),
                               @SwaggerMethodResponse( code = 500, definition = "Error while starting monitoring details", description = "Error while starting monitoring", parametersDefinitions = { @SwaggerMethodParameterDefinition( description = "The action Java exception object", example = "\"See the non-transient class fields for java.lang.Throwable ( detailMessage, cause, etc )\"", name = "error", type = "object"),
                                                                                                                                                                                                      @SwaggerMethodParameterDefinition( description = "The java exception class name", example = "com.mypoduct.exception.NoEntryException", name = "exceptionClass", type = "string") })
    })
    public Response startMonitoring( @Context HttpServletRequest request ) {

        String callerId = null;
        long resourceId = -1;
        int pollingInterval = -1;
        long startTimestamp = -1;
        long maximumRunningTime = -1;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            callerId = getJsonElement(jsonObject, "callerId").getAsString();
            if (StringUtils.isNullOrEmpty(callerId)) {
                throw new NoSuchElementException("callerId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(callerId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsLong();
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invalid value '" + resourceId + "'");
            }
            pollingInterval = getJsonElement(jsonObject, "pollingInterval").getAsInt();
            if (pollingInterval < 0) {
                throw new IllegalArgumentException("pollingInterval has invalid value '" + resourceId + "'");
            }
            startTimestamp = getJsonElement(jsonObject, "startTimestamp").getAsLong();
            if (startTimestamp < 0) {
                throw new IllegalArgumentException("startTimestamp has invalid value '" + resourceId + "'");
            }
            try {
                maximumRunningTime = getJsonElement(jsonObject, "maximumRunningTime").getAsLong();
            } catch (Exception e) {
                // request does not provide maximumRunningTime value, use the default one
                maximumRunningTime = RestSystemMonitor.DEFAULT_MAXIMUM_RUNNING_TIME;
            }
            maximumRunningTime *= 1000; // convert to seconds
            SystemMonitorsManager.startMonitoring(resourceId, pollingInterval, startTimestamp, maximumRunningTime,
                                                  request.getLocalAddr() + ":" + request.getLocalPort());
            String response = "{\"status_message\":\"monitoring successfully started\"}";
            return Response.ok(response).build();
        } catch (Exception e) {
            String message = "Unable to start monitoring for system monitor with resource id '" + resourceId
                             + "' in caller with id '" + callerId + "'";
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
    @Path( "stop")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod( httpOperation = "post", parametersDefinition = "Stop monitoring details", summary = "Stop monitoring", url = "stop")
    @SwaggerMethodParameterDefinitions( { /*TODO*/ })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse( code = 200, definition = "Successfull stop monitoring details", description = "Successfull stop monitoring", parametersDefinitions = {
                                                                                                                                                                                              @SwaggerMethodParameterDefinition( description = "Status message", example = "monitoring successfully stopped", name = "status_message", type = "string") }),
                               @SwaggerMethodResponse( code = 500, definition = "Error while stopped monitoring details", description = "Error while stopped monitoring", parametersDefinitions = { @SwaggerMethodParameterDefinition( description = "The action Java exception object", example = "\"See the non-transient class fields for java.lang.Throwable ( detailMessage, cause, etc )\"", name = "error", type = "object"),
                                                                                                                                                                                                    @SwaggerMethodParameterDefinition( description = "The java exception class name", example = "com.mypoduct.exception.NoEntryException", name = "exceptionClass", type = "string") })
    })
    public Response stopMonitoring( @Context HttpServletRequest request ) {

        String callerId = null;
        long resourceId = -1;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            callerId = getJsonElement(jsonObject, "callerId").getAsString();
            if (StringUtils.isNullOrEmpty(callerId)) {
                throw new NoSuchElementException("callerId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(callerId);
            resourceId = getJsonElement(jsonObject, "resourceId").getAsLong();
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invalid value '" + resourceId + "'");
            }
            SystemMonitorsManager.stopMonitoring(resourceId,
                                                 request.getLocalAddr() + ":" + request.getLocalPort());
            String response = "{\"status_message\":\"monitoring successfully stopped\"}";
            return Response.ok(response).build();
        } catch (Exception e) {
            String message = "Unable to stop monitoring for system monitor with resource id '" + resourceId
                             + "' in caller with id '" + callerId + "'";
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
