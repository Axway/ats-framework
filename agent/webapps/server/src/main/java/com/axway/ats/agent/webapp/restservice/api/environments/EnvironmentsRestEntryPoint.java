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
package com.axway.ats.agent.webapp.restservice.api.environments;

import java.io.InputStreamReader;
import java.util.NoSuchElementException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.axway.ats.agent.core.EnvironmentHandler;
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

@Path( "environments")
@SwaggerClass( "environments")
public class EnvironmentsRestEntryPoint {

    private static final Logger LOG  = Logger.getLogger(EnvironmentsRestEntryPoint.class);
    private static final Gson   GSON = new Gson();

    @POST
    @Path( "restore")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Restore environment details",
            summary = "Restore environment",
            url = "restore")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "some session ID",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The component name",
                                                  example = "some component name",
                                                  name = "componentName",
                                                  type = "string",
                                                  required = false),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The environment name",
                                                  example = "some environment name",
                                                  name = "environmentName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The folder path",
                                                  example = "some folder path",
                                                  name = "folderPath",
                                                  type = "string",
                                                  required = false)
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully restore environment details",
                                       description = "Successfully restore environment",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "environment 'environmentName' successfully restored",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while restoring environment details",
                                       description = "Error while restoring environment",
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
    public Response restoreEnvironment( @Context HttpServletRequest request ) {

        String sessionId = null;
        String componentName = null;
        String environmentName = null;
        String folderPath = null;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            try {
                componentName = getJsonElement(jsonObject, "componentName").getAsString();
            } catch (Exception e) {
                // do not throw exception since component name may be null
                componentName = null;
            }
            environmentName = getJsonElement(jsonObject, "environmentName").getAsString();
            if (StringUtils.isNullOrEmpty(environmentName)) {
                throw new NoSuchElementException("environmentName is not provided with the request");
            }
            try {
                folderPath = getJsonElement(jsonObject, "folderPath").getAsString();
            } catch (Exception e) {
                // do not throw exception since folder path may be null
                folderPath = null;
            }
            if (componentName == null) {
                // clean for all components
                EnvironmentHandler.getInstance().restoreAll(environmentName);
            } else {
                EnvironmentHandler.getInstance().restore(componentName, environmentName, folderPath);
            }
            return Response.ok("{\"status_message\":\"environment '" + environmentName + "' successfully restored\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to restore environment with arguments {'" + componentName + "', '"
                             + environmentName
                             + "', '" + folderPath + "'}";
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
    @Path( "backup")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Backup environment details",
            summary = "Backup environment",
            url = "backup")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "some session ID",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The component name",
                                                  example = "some component name",
                                                  name = "componentName",
                                                  type = "string",
                                                  required = false),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The environment name",
                                                  example = "some environment name",
                                                  name = "environmentName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The folder path",
                                                  example = "some folder path",
                                                  name = "folderPath",
                                                  type = "string",
                                                  required = false)
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully backed up environment details",
                                       description = "Successfully backed up environment",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "environment 'environmentName' successfully restored",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while backing up environment details",
                                       description = "Error while backing up environment",
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
    public Response backupEnvironment( @Context HttpServletRequest request ) {

        String sessionId = null;
        String componentName = null;
        String environmentName = null;
        String folderPath = null;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            try {
                componentName = getJsonElement(jsonObject, "componentName").getAsString();
            } catch (Exception e) {
                // do not throw exception since component name may be null
                componentName = null;
            }
            environmentName = getJsonElement(jsonObject, "environmentName").getAsString();
            if (StringUtils.isNullOrEmpty(environmentName)) {
                throw new NoSuchElementException("environmentName is not provided with the request");
            }
            try {
                folderPath = getJsonElement(jsonObject, "folderPath").getAsString();
            } catch (Exception e) {
                // do not throw exception since folder path may be null
                folderPath = null;
            }
            if (componentName == null) {
                // backup for all components
                EnvironmentHandler.getInstance().backupAll(environmentName);
            } else {
                EnvironmentHandler.getInstance().backup(componentName, environmentName, folderPath);
            }
            return Response.ok("{\"status_message\":\"environment '" + environmentName + "' successfully backed up\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to backing up environment with arguments {'" + componentName + "', '"
                             + environmentName
                             + "', '" + folderPath + "'}";
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
