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
package com.axway.ats.agent.webapp.restservice.api.machine;

import java.io.InputStreamReader;
import java.util.NoSuchElementException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
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

@Path( "machine/description")
@SwaggerClass( "machine/description")
public class MachineDescriptionRestEntryPoint {

    private static final Logger LOG  = Logger.getLogger(MachineDescriptionRestEntryPoint.class);
    private static final Gson   GSON = new Gson();

    @PUT
    @Path( "/")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "put",
            parametersDefinition = "Initialize machine description operations resource details",
            summary = "Initialize machine description operations resource",
            url = "/")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "some session ID",
                                                  name = "sessionId",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully initialize machine description operations resource details",
                                       description = "Successfully initialize machine description operations resource",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The resource ID of the newly initialized resource",
                                               example = "123",
                                               name = "resourceId",
                                               type = "integer") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while initializing machine description operations resource details",
                                       description = "Error while initializing machine description operations resource",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non-transient class fields for java.lang.Throwable ( detailMessage, cause, etc )\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.myproduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response initialize( @Context HttpServletRequest request ) {

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
            // Uncomment this before push to git
            /*if (!OperatingSystemType.getCurrentOsType().equals(OperatingSystemType.WINDOWS)) {
                throw new UnsupportedOperationException("Registry operations are supported only on WINDOWS hosts");
            }*/
            int resourceId = MachineDescriptionManager.initialize(sessionId);
            return Response.ok("{\"resourceId\":" + resourceId + "}").build();
        } catch (Exception e) {
            String message = "Unable to initialize machine description operations resource from session with id '"
                             + sessionId
                             + "'";
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
    @Path( "/")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "Get machine description details",
            summary = "Get machine description",
            url = "/")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "some session ID",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The resource ID",
                                                  example = "some resource ID",
                                                  name = "resourceId",
                                                  type = "integer")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully get machine description details",
                                       description = "Successfully get machine description",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The machine description",
                                               example = "TODO",
                                               name = "action_result",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting machine description details",
                                       description = "Error while getting machine description",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action Java exception object",
                                               example = "\"See the non-transient class fields for java.lang.Throwable ( detailMessage, cause, etc )\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.myproduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response getDescription( @Context HttpServletRequest request,
                                    @QueryParam( "sessionId") String sessionId,
                                    @QueryParam( "resourceId") int resourceId ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            String description = MachineDescriptionManager.getDescription(sessionId, resourceId);
            return Response.ok("{\"action_result\":" + GSON.toJson(description) + "}").build();
        } catch (Exception e) {
            String message = "Unable to get machine description from resource with id '" + resourceId
                             + "' and session with id '" + sessionId
                             + "'";
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
