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
package com.axway.ats.agent.webapp.restservice.api.system.input;

import java.io.InputStreamReader;
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

@SwaggerClass( "system/input")
@Path( "system/input")
public class SystemInputRestEntryPoint {

    private static final Logger LOG  = Logger.getLogger(SystemInputRestEntryPoint.class);
    private static final Gson   GSON = new Gson();

    @PUT
    @Path( "/")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "put",
            parametersDefinition = "Initialize system input resource details",
            summary = "Initialize system input resource",
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
                                       definition = "Successfully initialize system input resource details",
                                       description = "Successfully initialize system input resource",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The resource ID of the newly initialized resource",
                                               example = "123",
                                               name = "resourceId",
                                               type = "integer") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while initializing system input resource details",
                                       description = "Error while initializing system input resource",
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
    public Response initializeSystemInput( @Context HttpServletRequest request ) {

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
            int resourceId = SystemInputManager.initialize(sessionId);
            return Response.ok("{\"resourceId\":" + resourceId + "}").build();
        } catch (Exception e) {
            String message = "Unable to initialize system input resource from session with id '" + sessionId + "'";
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
    @Path( "mouse/click")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Click at location details",
            summary = "Click at location",
            url = "mouse/click")
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
                                                  description = "The location for the click on the X-axis",
                                                  example = "500",
                                                  name = "x",
                                                  type = "integer"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The location for the click on the Y-axis",
                                                  example = "1280",
                                                  name = "y",
                                                  type = "integer")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully execute 'click at' system input operation details",
                                       description = "Successfully execute 'click at' system input operation",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "operation 'click at' successfully executed",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while executing system input operation 'click at' details",
                                       description = "Error while executing system input operation 'click at'",
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
    public Response clickAt( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        int x = -1;
        int y = -1;
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
            x = getJsonElement(jsonObject, "x").getAsInt();
            y = getJsonElement(jsonObject, "y").getAsInt();
            SystemInputManager.clickAt(sessionId, resourceId, x, y);
            return Response.ok("{\"status_message\":\"operation 'click at' successfully executed\"}").build();
        } catch (Exception e) {
            String message = "Unable to execute system input operation 'click at' from session with id '" + sessionId
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
    @Path( "keyboard/type")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Keyboard type operation details",
            summary = "Keyboard type operation",
            url = "keyboard/type")
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
                                                  description = "The text which will be typed. "
                                                                + "This field is optional, but either this or keyCodes must be presented in the request",
                                                  example = "some text",
                                                  name = "text",
                                                  type = "string",
                                                  required = false),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "See java.awt.event.KeyEvent constants for available values."
                                                                + "This field is optional, but either this or text must be presented in the request",
                                                  example = "[38,40]",
                                                  name = "keyCodes",
                                                  type = "integer[]")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully execute 'type' system input operation details",
                                       description = "Successfully execute 'type' system input operation",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "operation 'type' successfully executed",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while executing system input operation 'type' details",
                                       description = "Error while executing system input operation 'type'",
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
    public Response type( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String text = null;
        int[] keyCodes = null;
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
            try {
                text = getJsonElement(jsonObject, "text").getAsString();
                if (StringUtils.isNullOrEmpty(sessionId)) {
                    throw new NoSuchElementException("sessionId is not provided with the request");
                }
            } catch (Exception e) {
                // text is optional
            }
            try {
                String keyCodesJSON = getJsonElement(jsonObject, "keyCodes").toString();
                if (StringUtils.isNullOrEmpty(keyCodesJSON)) {
                    throw new NoSuchElementException("keyCodes is not provided with the request");
                }
                keyCodes = GSON.fromJson(keyCodesJSON, int[].class);
            } catch (Exception e) {
                // keyCodes is optional
            }

            if (text == null) { // Note that empty string for text is a valid value
                if (keyCodes == null) {
                    throw new IllegalArgumentException("Atleast one of 'text' and 'keyCodes' fields must be presented in the request");
                } else {
                    SystemInputManager.type(sessionId, resourceId, keyCodes);
                }
            } else {
                if (keyCodes == null) {
                    SystemInputManager.type(sessionId, resourceId, text);
                } else {
                    SystemInputManager.type(sessionId, resourceId, text, keyCodes);
                }
            }
            return Response.ok("{\"status_message\":\"operation 'type' successfully executed\"}").build();
        } catch (Exception e) {
            String message = "Unable to execute system input operation 'type' from session with id '" + sessionId
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
    @Path( "keyboard/key/press")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Press keyboard key details",
            summary = "Press keyboard key",
            url = "keyboard/key/press")
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
                                                  description = "See java.awt.event.KeyEvent constants for available values.",
                                                  example = "38",
                                                  name = "keyCode",
                                                  type = "integer")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully execute 'keyPress' system input operation details",
                                       description = "Successfully execute 'keyPress' system input operation",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "operation 'keyPress' successfully executed",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while executing system input operation 'keyPress' details",
                                       description = "Error while executing system input operation 'keyPress'",
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
    public Response keyPress( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        int keyCode = -1;
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
            keyCode = getJsonElement(jsonObject, "keyCode").getAsInt();
            if (keyCode < 0) {
                throw new IllegalArgumentException("keyCode has invallid value '" + resourceId + "'");
            }
            SystemInputManager.keyPress(sessionId, resourceId, keyCode);
            return Response.ok("{\"status_message\":\"operation 'keyPress' successfully executed\"}").build();
        } catch (Exception e) {
            String message = "Unable to execute system input operation 'keyPress' from session with id '" + sessionId
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
    @Path( "keyboard/key/release")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Release keyboard key details",
            summary = "Release keyboard key",
            url = "keyboard/key/release")
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
                                                  description = "See java.awt.event.KeyEvent constants for available values.",
                                                  example = "40",
                                                  name = "keyCode",
                                                  type = "integer")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully execute 'keyRelease' system input operation details",
                                       description = "Successfully execute 'keyRelease' system input operation",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "operation 'keyRelease' successfully executed",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while executing system input operation 'keyRelease' details",
                                       description = "Error while executing system input operation 'keyRelease'",
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
    public Response keyRelease( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        int keyCode = -1;
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
            keyCode = getJsonElement(jsonObject, "keyCode").getAsInt();
            if (keyCode < 0) {
                throw new IllegalArgumentException("keyCode has invallid value '" + resourceId + "'");
            }
            SystemInputManager.keyRelease(sessionId, resourceId, keyCode);
            return Response.ok("{\"status_message\":\"operation 'keyRelease' successfully executed\"}").build();
        } catch (Exception e) {
            String message = "Unable to execute system input operation 'keyRelease' from session with id '" + sessionId
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
    @Path( "keyboard/key/alt/with/functional/4/press")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Press Alt and F4 keys details",
            summary = "Press Alt and F4 keys",
            url = "keyboard/key/alt/with/functional/4/press")
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
                                       definition = "Successfully execute 'pressAltF4' system input operation details",
                                       description = "Successfully execute 'pressAltF4' system input operation",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "operation 'pressAltF4' successfully executed",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while executing system input operation 'pressAltF4' details",
                                       description = "Error while executing system input operation 'pressAltF4'",
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
    public Response pressAltF4( @Context HttpServletRequest request ) {

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
            SystemInputManager.pressAltF4(sessionId, resourceId);
            return Response.ok("{\"status_message\":\"operation 'pressAltF4' successfully executed\"}").build();
        } catch (Exception e) {
            String message = "Unable to execute system input operation 'pressAltF4' from session with id '" + sessionId
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
    @Path( "keyboard/key/escape/press")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Press Escape key details",
            summary = "Press Escape key",
            url = "keyboard/key/escape/press")
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
                                       definition = "Successfully execute 'pressEsc' system input operation details",
                                       description = "Successfully execute 'pressEsc' system input operation",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "operation 'pressEsc' successfully executed",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while executing system input operation 'pressEsc' details",
                                       description = "Error while executing system input operation 'pressEsc'",
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
    public Response pressEsc( @Context HttpServletRequest request ) {

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
            SystemInputManager.pressEsc(sessionId, resourceId);
            return Response.ok("{\"status_message\":\"operation 'pressEsc' successfully executed\"}").build();
        } catch (Exception e) {
            String message = "Unable to execute system input operation 'pressEsc' from session with id '" + sessionId
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
    @Path( "keyboard/key/space/press")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Press Space key details",
            summary = "Press Space key",
            url = "keyboard/key/space/press")
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
                                       definition = "Successfully execute 'pressSpace' system input operation details",
                                       description = "Successfully execute 'pressSpace' system input operation",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "operation 'pressSpace' successfully executed",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while executing system input operation 'pressSpace' details",
                                       description = "Error while executing system input operation 'pressSpace'",
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
    public Response pressSpace( @Context HttpServletRequest request ) {

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
            SystemInputManager.pressSpace(sessionId, resourceId);
            return Response.ok("{\"status_message\":\"operation 'pressSpace' successfully executed\"}").build();
        } catch (Exception e) {
            String message = "Unable to execute system input operation 'pressSpace' from session with id '" + sessionId
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
    @Path( "keyboard/key/enter/press")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Press Enter key details",
            summary = "Press Enter key",
            url = "keyboard/key/enter/press")
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
                                       definition = "Successfully execute 'pressEnter' system input operation details",
                                       description = "Successfully execute 'pressEnter' system input operation",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "operation 'pressEnter' successfully executed",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while executing system input operation 'pressEnter' details",
                                       description = "Error while executing system input operation 'pressEnter'",
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
    public Response pressEnter( @Context HttpServletRequest request ) {

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
            SystemInputManager.pressEnter(sessionId, resourceId);
            return Response.ok("{\"status_message\":\"operation 'pressEnter' successfully executed\"}").build();
        } catch (Exception e) {
            String message = "Unable to execute system input operation 'pressEnter' from session with id '" + sessionId
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
    @Path( "keyboard/key/tab/press")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Press Tab key details",
            summary = "Press Tab key",
            url = "keyboard/key/tab/press")
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
                                       definition = "Successfully execute 'pressTab' system input operation details",
                                       description = "Successfully execute 'pressTab' system input operation",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "operation 'pressTab' successfully executed",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while executing system input operation 'pressTab' details",
                                       description = "Error while executing system input operation 'pressTab'",
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
    public Response pressTab( @Context HttpServletRequest request ) {

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
            SystemInputManager.pressTab(sessionId, resourceId);
            return Response.ok("{\"status_message\":\"operation 'pressTab' successfully executed\"}").build();
        } catch (Exception e) {
            String message = "Unable to execute system input operation 'pressTab' from session with id '" + sessionId
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
