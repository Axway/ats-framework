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
package com.axway.ats.agent.webapp.restservice.api.actions;

import java.util.Arrays;
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

import com.axway.ats.agent.webapp.restservice.api.ResourcesManager;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerClass;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethod;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethodParameterDefinition;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethodParameterDefinitions;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethodResponse;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethodResponses;
import com.axway.ats.core.threads.ThreadsPerCaller;
import com.axway.ats.core.utils.StringUtils;
import com.google.gson.Gson;

@Path( "actions")
@SwaggerClass( "actions")
public class ActionsRestEntryPoint {

    private static final Logger LOG  = Logger.getLogger(ActionsRestEntryPoint.class);
    private static final Gson   GSON = new Gson();

    @PUT
    @Path( "")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "put",
            parametersDefinition = "Initialize custom action class details",
            summary = "Initialize custom action class",
            url = "")
    @SwaggerMethodParameterDefinitions( { @SwaggerMethodParameterDefinition(
            description = "The session ID",
            example = "HOST_ID:localhost:8089;THREAD_ID:main",
            name = "sessionId",
            type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The action method name. Note that this is not the actual Java method name, but instead the value of the @Action annotation name attribute",
                                                  example = "FTP actions connect",
                                                  name = "methodName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The component name",
                                                  example = "ftpactions",
                                                  name = "componentName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The action arguments' types/classes",
                                                  example = "[\"java.lang.String\",\"int.class\",\"boolean\",\"com.ftpactions.model.CustomClass\"]",
                                                  name = "argumentsTypes",
                                                  type = "string[]"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The action arguments' values",
                                                  example = "[\"a string\",\"1\",\"true\",\"{custom_class_field_name:some_value}\"]",
                                                  name = "argumentsValues",
                                                  type = "string[]") })
    @SwaggerMethodResponses( { @SwaggerMethodResponse(
            code = 200,
            definition = "Successfull initialization of Action details",
            description = "Successfull initialization of Action",
            parametersDefinitions = { @SwaggerMethodParameterDefinition(
                    description = "The resource ID for the initialized action class",
                    example = "123",
                    name = "resourceId",
                    type = "integer") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while initializing Action details",
                                       description = "Error while initializing Action",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "the actual java exception",
                                               example = "\"See the non transiend class fields for java.lang.Throwable ( detailMessage, cause, etc)\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "the java exception class name",
                                                                         example = "java.lang.Exception",
                                                                         name = "exceptionClass",
                                                                         type = "string") }) })
    public Response initialize( @Context HttpServletRequest request, ActionPojo pojo ) {

        try {
            if (StringUtils.isNullOrEmpty(pojo.getSessionId())) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(pojo.getSessionId());
            if (LOG.isDebugEnabled()) {
                LOG.debug("Initializing of action class for method '" + pojo.getMethodName()
                          + "' in session with id '" + pojo.getSessionId() + "'");
            }
            int resourceId = ResourcesManager.initializeResource(pojo);
            String response = "{\"resourceId\":" + resourceId + "}";
            return Response.ok(response).build();
        } catch (Exception e) {
            String message = "Unable to initialize action class for method '" + pojo.getMethodName()
                             + "' in session with id '" + pojo.getResourceId() + "'";
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
    @Path( "execute")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Execute custom action details",
            summary = "Execute custom action",
            url = "execute")
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
                                                  description = "The action method name. Note that this is not the actual Java method name, but instead the value of the Action annotation's name attribute",
                                                  example = "FTP actions connect",
                                                  name = "methodName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The component name",
                                                  example = "ftpactions",
                                                  name = "componentName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The action argument's class names",
                                                  example = "[\"java.lang.String\", \"int.class\", \"boolean\", \"com.ftpactions.model.CustomClass\"]",
                                                  name = "argumentsTypes",
                                                  type = "string[]"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The action arguments values",
                                                  example = "[\"a string\",\"1\",\"true\",\"{some_field_from_custom_class:some_value}\"]",
                                                  name = "argumentsValues",
                                                  type = "string[]") })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successful execution of action method details",
                                       description = "Successful execution of action method",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The action result",
                                               example = "some_string",
                                               name = "action_result",
                                               type = "any") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while executing action method details",
                                       description = "Error while executing action method",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "THe actuon Java exception object",
                                               example = "\"See the non transiend class fields for java.lang.Throwable ( detailMessage, cause, etc)\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.custom.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response execute( @Context HttpServletRequest request, ActionPojo pojo ) {

        try {
            if (StringUtils.isNullOrEmpty(pojo.getSessionId())) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            if (pojo.getResourceId() < 0) {
                throw new IllegalArgumentException("resourceId must be >=0, but was" + pojo.getResourceId());
            }
            ThreadsPerCaller.registerThread(pojo.getSessionId());
            if (LOG.isDebugEnabled()) {
                String message = "Executing action '" + pojo.getMethodName() + "' from component '"
                                 + pojo.getComponentName() + "' using the following arguments '"
                                 + Arrays.toString(pojo.getArgumentsValues()) + "'. Action class's resource ID is '"
                                 + pojo.getResourceId() + "'and session ID is '" + pojo.getSessionId()
                                 + "'";
                LOG.debug(message);
            }

            Object result = ResourcesManager.executeOverResource(pojo);
            String response = "{\"action_result\":" + new Gson().toJson(result) + "}";
            return Response.ok(response).build();

        } catch (Exception e) {
            String message = "Unable to execute action '" + pojo.getMethodName() + "' from component '"
                             + pojo.getComponentName() + "' using the following arguments '"
                             + Arrays.toString(pojo.getArgumentsValues()) + "'. Action class's resourceId was '"
                             + pojo.getResourceId() + "'and sessionId  was '" + pojo.getSessionId()
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
    @Path( "")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "delete",
            parametersDefinition = "",
            summary = "Deinitialize custom action class",
            url = "")
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
                                       definition = "Successfull deinitialization of action class details",
                                       description = "Successfull deinitialization of action class",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The resource ID of the deinitialized action class",
                                               example = "123",
                                               name = "resourceId",
                                               type = "integer"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "Status message",
                                                                         example = "Action class with resource id '10' successfully deleted",
                                                                         name = "status_message",
                                                                         type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while deinitializing action class details",
                                       description = "Error while deinitializing action class",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The actuon Java exception object",
                                               example = "\"See the non transiend class fields for java.lang.Throwable ( detailMessage, cause, etc)\"",
                                               name = "error",
                                               type = "object"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The java exception class name",
                                                                         example = "com.mypoduct.exception.NoEntryException",
                                                                         name = "exceptionClass",
                                                                         type = "string") })
    })
    public Response deinitialize( @Context HttpServletRequest request,
                                  @QueryParam( "sessionId") String sessionId,
                                  @QueryParam( "resourceId") int resourceId ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId must be >= 0, but was" + resourceId);
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Deinitialization of action class with id '" + resourceId + "' for session with id '"
                          + sessionId + "'");
            }

            resourceId = ResourcesManager.deinitializeResource(sessionId, resourceId);
            String response = "{\"status_message\":\"Action class with resource id '" + resourceId
                              + "' successfully deleted\"}";
            return Response.ok(response).build();
        } catch (Exception e) {
            String message = "Unable to deinitialize action class with id'" + resourceId
                             + "' in session with id '" + sessionId + "'";
            LOG.error(message, e);
            return Response.serverError()
                           .entity("{\"error\":" + GSON.toJson(e) + ", \"exceptionClass\":\"" + e.getClass().getName()
                                   + "\"}")
                           .build();
        } finally {
            ThreadsPerCaller.unregisterThread();
        }
    }

}
