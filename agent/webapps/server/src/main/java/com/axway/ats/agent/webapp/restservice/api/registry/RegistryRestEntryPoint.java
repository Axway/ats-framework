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
package com.axway.ats.agent.webapp.restservice.api.registry;

import java.io.InputStreamReader;
import java.util.NoSuchElementException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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

@Path( "registry")
@SwaggerClass( "registry")
public class RegistryRestEntryPoint {

    private static final Logger LOG  = Logger.getLogger(RegistryRestEntryPoint.class);
    private static final Gson   GSON = new Gson();

    @PUT
    @Path( "/")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "put",
            parametersDefinition = "Initialize registry operations resource details",
            summary = "Initialize registry operations resource",
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
                                       definition = "Successfully initialize registry operation resource details",
                                       description = "Successfully initialize registry operation resource",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The resource ID of the newly initialized resource",
                                               example = "123",
                                               name = "resourceId",
                                               type = "integer") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while initializing registry operation resource details",
                                       description = "Error while initializing registry operation resource",
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
            int resourceId = RegistryManager.initialize(sessionId);
            return Response.ok("{\"resourceId\":" + resourceId + "}").build();
        } catch (Exception e) {
            String message = "Unable to initialize registry operation resource from session with id '" + sessionId
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

    @PUT
    @Path( "path")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "put",
            parametersDefinition = "Create registry path details",
            summary = "Create registry path",
            url = "path")
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
                                                  description = "The root key. Any of the HKEY_* ones",
                                                  example = "HKEY_LOCAL_MACHINE",
                                                  name = "rootKey",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The name of the key path",
                                                  example = "some_key_path",
                                                  name = "keyPath",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully create registry path details",
                                       description = "Successfully create registry path",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "registry path successfully created",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while creating registry path details",
                                       description = "Error while creating registry path",
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
    public Response createPath( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String rootKey = null;
        String keyPath = null;
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
            // Uncomment this before push to git
            /*if (!OperatingSystemType.getCurrentOsType().equals(OperatingSystemType.WINDOWS)) {
                throw new UnsupportedOperationException("Registry operations are supported only on WINDOWS hosts");
            }*/
            rootKey = getJsonElement(jsonObject, "rootKey").getAsString();
            if (StringUtils.isNullOrEmpty(rootKey)) {
                throw new NoSuchElementException("rootKey is not provided with the request");
            }
            keyPath = getJsonElement(jsonObject, "keyPath").getAsString();
            if (StringUtils.isNullOrEmpty(keyPath)) {
                throw new NoSuchElementException("keyPath is not provided with the request");
            }
            RegistryManager.createPath(sessionId, resourceId, rootKey, keyPath);
            return Response.ok("{\"status_message\":\"registry path successfully created\"}").build();
        } catch (Exception e) {
            String message = "Unable to create registry path from session with id '" + sessionId
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

    @DELETE
    @Path( "key")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "delete",
            parametersDefinition = "",
            summary = "Delete registry key",
            url = "key")
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
                                                  description = "The root key. Any of the HKEY_* ones",
                                                  example = "HKEY_LOCAL_MACHINE",
                                                  name = "rootKey",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The key path",
                                                  example = "some_key_path",
                                                  name = "keyPath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The key name",
                                                  example = "some_key_name",
                                                  name = "keyName",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully delete registry key details",
                                       description = "Successfully delete registry key",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "registry key successfully deleted",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while deleting registry key details",
                                       description = "Error while deleting registry key",
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
    public Response deleteKey( @Context HttpServletRequest request,
                               @QueryParam( "sessionId") String sessionId,
                               @QueryParam( "resourceId") int resourceId,
                               @QueryParam( "rootKey") String rootKey,
                               @QueryParam( "keyPath") String keyPath,
                               @QueryParam( "keyName") String keyName ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            // Uncomment this before push to git
            /*if (!OperatingSystemType.getCurrentOsType().equals(OperatingSystemType.WINDOWS)) {
                throw new UnsupportedOperationException("Registry operations are supported only on WINDOWS hosts");
            }*/
            if (StringUtils.isNullOrEmpty(rootKey)) {
                throw new NoSuchElementException("rootKey is not provided with the request");
            }
            if (StringUtils.isNullOrEmpty(keyPath)) {
                throw new NoSuchElementException("keyPath is not provided with the request");
            }
            if (StringUtils.isNullOrEmpty(keyName)) {
                throw new NoSuchElementException("keyName is not provided with the request");
            }
            RegistryManager.deleteKey(sessionId, resourceId, rootKey, keyPath, keyName);
            return Response.ok("{\"status_message\":\"registry key successfully deleted\"}").build();
        } catch (Exception e) {
            String message = "Unable to delete registry key from session with id '" + sessionId
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
    @Path( "key")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Check if registry key is presented",
            url = "key")
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
                                                  description = "The root key. Any of the HKEY_* ones",
                                                  example = "HKEY_LOCAL_MACHINE",
                                                  name = "rootKey",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The key path",
                                                  example = "some_key_path",
                                                  name = "keyPath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The key name",
                                                  example = "some_key_name",
                                                  name = "keyName",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully check if registry key is presented details",
                                       description = "Successfully check if registry key is presented",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Whether the registry key is presented",
                                               example = "TRUE|FALSE",
                                               name = "action_result",
                                               type = "boolean") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while checking if registry key is presented details",
                                       description = "Error while checking if registry key is presented",
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
    public Response isKeyPresented( @Context HttpServletRequest request,
                                    @QueryParam( "sessionId") String sessionId,
                                    @QueryParam( "resourceId") int resourceId,
                                    @QueryParam( "rootKey") String rootKey,
                                    @QueryParam( "keyPath") String keyPath,
                                    @QueryParam( "keyName") String keyName ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            // Uncomment this before push to git
            /*if (!OperatingSystemType.getCurrentOsType().equals(OperatingSystemType.WINDOWS)) {
                throw new UnsupportedOperationException("Registry operations are supported only on WINDOWS hosts");
            }*/
            if (StringUtils.isNullOrEmpty(rootKey)) {
                throw new NoSuchElementException("rootKey is not provided with the request");
            }
            if (StringUtils.isNullOrEmpty(keyPath)) {
                throw new NoSuchElementException("keyPath is not provided with the request");
            }
            if (StringUtils.isNullOrEmpty(keyName)) {
                throw new NoSuchElementException("keyName is not provided with the request");
            }
            boolean isKeyPresented = RegistryManager.isKeyPresent(sessionId, resourceId, rootKey, keyPath, keyName);
            return Response.ok("{\"action_result\":\"" + GSON.toJson(isKeyPresented) + "\"}").build();
        } catch (Exception e) {
            String message = "Unable to check if registry key is presented from session with id '" + sessionId
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
    @Path( "key/binary")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get registry's key as binary",
            url = "key/binary")
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
                                                  description = "The root key. Any of the HKEY_* ones",
                                                  example = "HKEY_LOCAL_MACHINE",
                                                  name = "rootKey",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The key path",
                                                  example = "some_key_path",
                                                  name = "keyPath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The key name",
                                                  example = "some_key_name",
                                                  name = "keyName",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully get registry's key as binary details",
                                       description = "Successfully get registry's key as binary",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The binary value of the key",
                                               example = "{}",
                                               name = "action_result",
                                               type = "byte[]") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting registry's key as binary details",
                                       description = "Error while getting registry's key as binary",
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
    public Response getBinaryValue( @Context HttpServletRequest request,
                                    @QueryParam( "sessionId") String sessionId,
                                    @QueryParam( "resourceId") int resourceId,
                                    @QueryParam( "rootKey") String rootKey,
                                    @QueryParam( "keyPath") String keyPath,
                                    @QueryParam( "keyName") String keyName ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            // Uncomment this before push to git
            /*if (!OperatingSystemType.getCurrentOsType().equals(OperatingSystemType.WINDOWS)) {
                throw new UnsupportedOperationException("Registry operations are supported only on WINDOWS hosts");
            }*/
            if (StringUtils.isNullOrEmpty(rootKey)) {
                throw new NoSuchElementException("rootKey is not provided with the request");
            }
            if (StringUtils.isNullOrEmpty(keyPath)) {
                throw new NoSuchElementException("keyPath is not provided with the request");
            }
            if (StringUtils.isNullOrEmpty(keyName)) {
                throw new NoSuchElementException("keyName is not provided with the request");
            }
            byte[] value = RegistryManager.getBinaryValue(sessionId, resourceId, rootKey, keyPath, keyName);
            return Response.ok("{\"action_result\":\"" + GSON.toJson(value) + "\"}").build();
        } catch (Exception e) {
            String message = "Unable to get registry's key as binary from session with id '" + sessionId
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
    @Path( "key/int")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get registry's key as integer",
            url = "key/int")
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
                                                  description = "The root key. Any of the HKEY_* ones",
                                                  example = "HKEY_LOCAL_MACHINE",
                                                  name = "rootKey",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The key path",
                                                  example = "some_key_path",
                                                  name = "keyPath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The key name",
                                                  example = "some_key_name",
                                                  name = "keyName",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully get registry's key as integer details",
                                       description = "Successfully get registry's key as integer",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The integer value of the key",
                                               example = "some integer value",
                                               name = "action_result",
                                               type = "int") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting registry's key as integer details",
                                       description = "Error while getting registry's key as integer",
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
    public Response getIntValue( @Context HttpServletRequest request,
                                 @QueryParam( "sessionId") String sessionId,
                                 @QueryParam( "resourceId") int resourceId,
                                 @QueryParam( "rootKey") String rootKey,
                                 @QueryParam( "keyPath") String keyPath,
                                 @QueryParam( "keyName") String keyName ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            // Uncomment this before push to git
            /*if (!OperatingSystemType.getCurrentOsType().equals(OperatingSystemType.WINDOWS)) {
                throw new UnsupportedOperationException("Registry operations are supported only on WINDOWS hosts");
            }*/
            if (StringUtils.isNullOrEmpty(rootKey)) {
                throw new NoSuchElementException("rootKey is not provided with the request");
            }
            if (StringUtils.isNullOrEmpty(keyPath)) {
                throw new NoSuchElementException("keyPath is not provided with the request");
            }
            if (StringUtils.isNullOrEmpty(keyName)) {
                throw new NoSuchElementException("keyName is not provided with the request");
            }
            int value = RegistryManager.getIntValue(sessionId, resourceId, rootKey, keyPath, keyName);
            return Response.ok("{\"action_result\":\"" + GSON.toJson(value) + "\"}").build();
        } catch (Exception e) {
            String message = "Unable to get registry's key as integer from session with id '" + sessionId
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
    @Path( "key/long")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get registry's key as long",
            url = "key/long")
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
                                                  description = "The root key. Any of the HKEY_* ones",
                                                  example = "HKEY_LOCAL_MACHINE",
                                                  name = "rootKey",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The key path",
                                                  example = "some_key_path",
                                                  name = "keyPath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The key name",
                                                  example = "some_key_name",
                                                  name = "keyName",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully get registry's key as long details",
                                       description = "Successfully get registry's key as long",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The long value of the key",
                                               example = "some long value",
                                               name = "action_result",
                                               type = "long") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting registry's key as long details",
                                       description = "Error while getting registry's key as long",
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
    public Response getLongValue( @Context HttpServletRequest request,
                                  @QueryParam( "sessionId") String sessionId,
                                  @QueryParam( "resourceId") int resourceId,
                                  @QueryParam( "rootKey") String rootKey,
                                  @QueryParam( "keyPath") String keyPath,
                                  @QueryParam( "keyName") String keyName ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            // Uncomment this before push to git
            /*if (!OperatingSystemType.getCurrentOsType().equals(OperatingSystemType.WINDOWS)) {
                throw new UnsupportedOperationException("Registry operations are supported only on WINDOWS hosts");
            }*/
            if (StringUtils.isNullOrEmpty(rootKey)) {
                throw new NoSuchElementException("rootKey is not provided with the request");
            }
            if (StringUtils.isNullOrEmpty(keyPath)) {
                throw new NoSuchElementException("keyPath is not provided with the request");
            }
            if (StringUtils.isNullOrEmpty(keyName)) {
                throw new NoSuchElementException("keyName is not provided with the request");
            }
            long value = RegistryManager.getLongValue(sessionId, resourceId, rootKey, keyPath, keyName);
            return Response.ok("{\"action_result\":\"" + GSON.toJson(value) + "\"}").build();
        } catch (Exception e) {
            String message = "Unable to get registry's key as long from session with id '" + sessionId
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
    @Path( "key/string")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get registry's key as string",
            url = "key/string")
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
                                                  description = "The root key. Any of the HKEY_* ones",
                                                  example = "HKEY_LOCAL_MACHINE",
                                                  name = "rootKey",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The key path",
                                                  example = "some_key_path",
                                                  name = "keyPath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The key name",
                                                  example = "some_key_name",
                                                  name = "keyName",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully get registry's key as string details",
                                       description = "Successfully get registry's key as string",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The string value of the key",
                                               example = "some string value",
                                               name = "action_result",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting registry's key as string details",
                                       description = "Error while getting registry's key as string",
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
    public Response getStringValue( @Context HttpServletRequest request,
                                    @QueryParam( "sessionId") String sessionId,
                                    @QueryParam( "resourceId") int resourceId,
                                    @QueryParam( "rootKey") String rootKey,
                                    @QueryParam( "keyPath") String keyPath,
                                    @QueryParam( "keyName") String keyName ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            // Uncomment this before push to git
            /*if (!OperatingSystemType.getCurrentOsType().equals(OperatingSystemType.WINDOWS)) {
                throw new UnsupportedOperationException("Registry operations are supported only on WINDOWS hosts");
            }*/
            if (StringUtils.isNullOrEmpty(rootKey)) {
                throw new NoSuchElementException("rootKey is not provided with the request");
            }
            if (StringUtils.isNullOrEmpty(keyPath)) {
                throw new NoSuchElementException("keyPath is not provided with the request");
            }
            if (StringUtils.isNullOrEmpty(keyName)) {
                throw new NoSuchElementException("keyName is not provided with the request");
            }
            String value = RegistryManager.getStringValue(sessionId, resourceId, rootKey, keyPath, keyName);
            return Response.ok("{\"action_result\":\"" + GSON.toJson(value) + "\"}").build();
        } catch (Exception e) {
            String message = "Unable to get registry's key as String from session with id '" + sessionId
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

    @POST
    @Path( "key/binary")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Set new value to binary key details",
            summary = "Set new value to binary key",
            url = "key/binary")
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
                                                  description = "The root key. Any of the HKEY_* ones",
                                                  example = "HKEY_LOCAL_MACHINE",
                                                  name = "rootKey",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The key path",
                                                  example = "some_key_path",
                                                  name = "keyPath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The key name",
                                                  example = "some_key_name",
                                                  name = "keyName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The new binary value",
                                                  example = "{}",
                                                  name = "keyValue",
                                                  type = "byte[]")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully set new value to binary key details",
                                       description = "Successfully set new value to binary key",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfully set new binary value for registry key",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while setting new value to binary key details",
                                       description = "Error while setting new value to binary key",
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
    public Response setBinaryValue( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String rootKey = null;
        String keyPath = null;
        String keyName = null;
        byte[] keyValue = null;
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
            // Uncomment this before push to git
            /*if (!OperatingSystemType.getCurrentOsType().equals(OperatingSystemType.WINDOWS)) {
                throw new UnsupportedOperationException("Registry operations are supported only on WINDOWS hosts");
            }*/
            rootKey = getJsonElement(jsonObject, "rootKey").getAsString();
            if (StringUtils.isNullOrEmpty(rootKey)) {
                throw new NoSuchElementException("rootKey is not provided with the request");
            }
            keyPath = getJsonElement(jsonObject, "keyPath").getAsString();
            if (StringUtils.isNullOrEmpty(keyPath)) {
                throw new NoSuchElementException("keyPath is not provided with the request");
            }
            keyName = getJsonElement(jsonObject, "keyName").getAsString();
            if (StringUtils.isNullOrEmpty(keyName)) {
                throw new NoSuchElementException("keyName is not provided with the request");
            }
            String keyValueJSON = getJsonElement(jsonObject, "keyValue").toString();
            if (StringUtils.isNullOrEmpty(keyValueJSON)) {
                throw new NoSuchElementException("keyValue is not provided with the request");
            }
            keyValue = GSON.fromJson(keyValueJSON, byte[].class);
            RegistryManager.setBinaryValue(sessionId, resourceId, rootKey, keyPath, keyName, keyValue);
            return Response.ok("{\"status_message\":\"successfully set new binary value for registry key\"}").build();
        } catch (Exception e) {
            String message = "Unable to set new binary value for registry key from session with id '" + sessionId
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

    @POST
    @Path( "key/int")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Set new value to int key details",
            summary = "Set new value to int key",
            url = "key/int")
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
                                                  description = "The root key. Any of the HKEY_* ones",
                                                  example = "HKEY_LOCAL_MACHINE",
                                                  name = "rootKey",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The key path",
                                                  example = "some_key_path",
                                                  name = "keyPath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The key name",
                                                  example = "some_key_name",
                                                  name = "keyName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The new int value",
                                                  example = "123",
                                                  name = "keyValue",
                                                  type = "integer")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully set new value to int key details",
                                       description = "Successfully set new value to int key",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfully set new int value for registry key",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while setting new value to int key details",
                                       description = "Error while setting new value to int key",
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
    public Response setIntValue( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String rootKey = null;
        String keyPath = null;
        String keyName = null;
        int keyValue = -1;
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
            // Uncomment this before push to git
            /*if (!OperatingSystemType.getCurrentOsType().equals(OperatingSystemType.WINDOWS)) {
                throw new UnsupportedOperationException("Registry operations are supported only on WINDOWS hosts");
            }*/
            rootKey = getJsonElement(jsonObject, "rootKey").getAsString();
            if (StringUtils.isNullOrEmpty(rootKey)) {
                throw new NoSuchElementException("rootKey is not provided with the request");
            }
            keyPath = getJsonElement(jsonObject, "keyPath").getAsString();
            if (StringUtils.isNullOrEmpty(keyPath)) {
                throw new NoSuchElementException("keyPath is not provided with the request");
            }
            keyName = getJsonElement(jsonObject, "keyName").getAsString();
            if (StringUtils.isNullOrEmpty(keyName)) {
                throw new NoSuchElementException("keyName is not provided with the request");
            }
            keyValue = getJsonElement(jsonObject, "keyValue").getAsInt();
            RegistryManager.setIntValue(sessionId, resourceId, rootKey, keyPath, keyName, keyValue);
            return Response.ok("{\"status_message\":\"successfully set new int value for registry key\"}").build();
        } catch (Exception e) {
            String message = "Unable to set new int value for registry key from session with id '" + sessionId
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

    @POST
    @Path( "key/long")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Set new value to long key details",
            summary = "Set new value to long key",
            url = "key/long")
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
                                                  description = "The root key. Any of the HKEY_* ones",
                                                  example = "HKEY_LOCAL_MACHINE",
                                                  name = "rootKey",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The key path",
                                                  example = "some_key_path",
                                                  name = "keyPath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The key name",
                                                  example = "some_key_name",
                                                  name = "keyName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The new long value",
                                                  example = "123",
                                                  name = "keyValue",
                                                  type = "long")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully set new value to long key details",
                                       description = "Successfully set new value to long key",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfully set new long value for registry key",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while setting new value to long key details",
                                       description = "Error while setting new value to long key",
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
    public Response setLongValue( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String rootKey = null;
        String keyPath = null;
        String keyName = null;
        long keyValue = -1;
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
            // Uncomment this before push to git
            /*if (!OperatingSystemType.getCurrentOsType().equals(OperatingSystemType.WINDOWS)) {
                throw new UnsupportedOperationException("Registry operations are supported only on WINDOWS hosts");
            }*/
            rootKey = getJsonElement(jsonObject, "rootKey").getAsString();
            if (StringUtils.isNullOrEmpty(rootKey)) {
                throw new NoSuchElementException("rootKey is not provided with the request");
            }
            keyPath = getJsonElement(jsonObject, "keyPath").getAsString();
            if (StringUtils.isNullOrEmpty(keyPath)) {
                throw new NoSuchElementException("keyPath is not provided with the request");
            }
            keyName = getJsonElement(jsonObject, "keyName").getAsString();
            if (StringUtils.isNullOrEmpty(keyName)) {
                throw new NoSuchElementException("keyName is not provided with the request");
            }
            keyValue = getJsonElement(jsonObject, "keyValue").getAsLong();
            RegistryManager.setLongValue(sessionId, resourceId, rootKey, keyPath, keyName, keyValue);
            return Response.ok("{\"status_message\":\"successfully set new long value for registry key\"}").build();
        } catch (Exception e) {
            String message = "Unable to set new long value for registry key from session with id '" + sessionId
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

    @POST
    @Path( "key/string")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Set new value to String key details",
            summary = "Set new value to String key",
            url = "key/string")
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
                                                  description = "The root key. Any of the HKEY_* ones",
                                                  example = "HKEY_LOCAL_MACHINE",
                                                  name = "rootKey",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The key path",
                                                  example = "some_key_path",
                                                  name = "keyPath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The key name",
                                                  example = "some_key_name",
                                                  name = "keyName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The new String value",
                                                  example = "some text",
                                                  name = "keyValue",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully set new value to String key details",
                                       description = "Successfully set new value to String key",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfully set new String value for registry key",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while setting new value to String key details",
                                       description = "Error while setting new value to String key",
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
    public Response setStringValue( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String rootKey = null;
        String keyPath = null;
        String keyName = null;
        String keyValue = null;
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
            // Uncomment this before push to git
            /*if (!OperatingSystemType.getCurrentOsType().equals(OperatingSystemType.WINDOWS)) {
                throw new UnsupportedOperationException("Registry operations are supported only on WINDOWS hosts");
            }*/
            rootKey = getJsonElement(jsonObject, "rootKey").getAsString();
            if (StringUtils.isNullOrEmpty(rootKey)) {
                throw new NoSuchElementException("rootKey is not provided with the request");
            }
            keyPath = getJsonElement(jsonObject, "keyPath").getAsString();
            if (StringUtils.isNullOrEmpty(keyPath)) {
                throw new NoSuchElementException("keyPath is not provided with the request");
            }
            keyName = getJsonElement(jsonObject, "keyName").getAsString();
            if (StringUtils.isNullOrEmpty(keyName)) {
                throw new NoSuchElementException("keyName is not provided with the request");
            }
            keyValue = getJsonElement(jsonObject, "keyValue").getAsString();
            RegistryManager.setStringValue(sessionId, resourceId, rootKey, keyPath, keyName, keyValue);
            return Response.ok("{\"status_message\":\"successfully set new String value for registry key\"}").build();
        } catch (Exception e) {
            String message = "Unable to set new String value for registry key from session with id '" + sessionId
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
