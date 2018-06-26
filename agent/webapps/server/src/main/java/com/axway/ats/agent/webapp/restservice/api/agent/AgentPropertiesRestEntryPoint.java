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
package com.axway.ats.agent.webapp.restservice.api.agent;

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

import com.axway.ats.agent.core.ActionHandler;
import com.axway.ats.agent.core.ComponentRepository;
import com.axway.ats.agent.webapp.restservice.api.ResourcesManager;
import com.axway.ats.agent.webapp.restservice.api.actions.ActionPojo;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerClass;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethod;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethodParameterDefinition;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethodParameterDefinitions;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethodResponse;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethodResponses;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.AtsVersion;
import com.axway.ats.core.threads.ThreadsPerCaller;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.appenders.PassiveDbAppender;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Path( "agent/properties")
@SwaggerClass( "agent/properties")
public class AgentPropertiesRestEntryPoint {

    private static final Logger LOG                           = Logger.getLogger(AgentPropertiesRestEntryPoint.class);
    private static final Gson   GSON                          = new Gson();

    public static final String  GET_CLASSPATH_OPERATION       = "getClassPath";
    public static final String  LOG_CLASSPATH_OPERATION       = "logClassPath";
    public static final String  GET_DUPLICATED_JARS_OPERATION = "getDuplicatedJars";
    public static final String  LOG_DUPLICATED_JARS_OPERATION = "logDuplicatedJars";
    public static final String  GET_AGENT_HOME_OPERATION      = "getAgentHome";
    public static final String  IS_COMPONENT_LOADED_OPERATION = "isComponentLoaded";
    public static final String  GET_ATS_VERSION_OPERATION     = "getAtsVersion";
    public static final String  GET_NUMBER_PENDING_LOG_EVENTS = "getNumberPendingLogEvents";

    @POST
    @Path( "")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Get or set Ats Agent property details",
            summary = "Get or set Ats Agent property",
            url = "")
    @SwaggerMethodParameterDefinitions( { @SwaggerMethodParameterDefinition(
            description = "The sessionID",
            example = "HOST_ID:localhost:8089;THREAD_ID:main",
            name = "sessionId",
            type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The operation's name for obtaining the property name",
                                                  example = "getClassPath|logClassPath|getDuplicatedJars|logDuplicatedJars|getAgentHome|isComponentLoaded|getAtsVersion|getNumberPendingLogEvents",
                                                  name = "operation",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Additional value for operation isComponentLoaded. Other operations do not have to add this value to the request.",
                                                  example = "For isComponentLoaded operation, the value will be the component's name.",
                                                  name = "value",
                                                  type = "string",
                                                  required = false) })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "",
                                       description = "",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Successfully get(set) ATS Agent property details",
                                               example = "Successfully get(set) ATS Agent property. The response's key and value depends on the operation in the request.",
                                               name = "classpath|status_message|duplicated_jars|agent_home|loaded|ats_version|number_of_pending_log_events",
                                               type = "string[]|string|string[]|string|boolean|string|integer") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while geting(setting) ATS Agent property details",
                                       description = "Error while geting(setting) ATS Agent property",
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
    public Response getProperty( @Context HttpServletRequest request ) {

        String sessionId = null;
        String operation = null;
        String value = null; // optional
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            operation = getJsonElement(jsonObject, "operation").getAsString();
            if (StringUtils.isNullOrEmpty(operation)) {
                throw new NoSuchElementException("operation is not provided with the request");
            }
            ActionPojo actionPojo = null;
            int resourceId = -1;
            switch (operation) {
                case GET_CLASSPATH_OPERATION:
                    // create action pojo
                    actionPojo = new ActionPojo(sessionId, -1, "auto-system-operations",
                                                "Internal System Operations get Class Path", new String[]{},
                                                new String[]{});
                    // initialize resource
                    resourceId = ResourcesManager.initializeResource(actionPojo);
                    actionPojo.setResourceId(resourceId);
                    // execute the operation
                    String[] classPath = (String[]) ResourcesManager.executeOverResource(actionPojo);
                    // deinitialize the resource
                    ResourcesManager.deinitializeResource(sessionId, resourceId);

                    return Response.ok("{\"classpath\":" + GSON.toJson(classPath) + "}").build();
                case LOG_CLASSPATH_OPERATION:
                    // create action pojo
                    actionPojo = new ActionPojo(sessionId, -1, "auto-system-operations",
                                                "Internal System Operations log Class Path", new String[]{},
                                                new String[]{});
                    // initialize resource
                    resourceId = ResourcesManager.initializeResource(actionPojo);
                    actionPojo.setResourceId(resourceId);
                    // execute the operation
                    ResourcesManager.executeOverResource(actionPojo);
                    // deinitialize the resource
                    ResourcesManager.deinitializeResource(sessionId, resourceId);

                    return Response.ok("{\"status_message\":\"classpath successfully logged\"}").build();
                case GET_DUPLICATED_JARS_OPERATION:
                    // create action pojo
                    actionPojo = new ActionPojo(sessionId, -1, "auto-system-operations",
                                                "Internal System Operations get Duplicated Jars", new String[]{},
                                                new String[]{});
                    // initialize resource
                    resourceId = ResourcesManager.initializeResource(actionPojo);
                    actionPojo.setResourceId(resourceId);
                    // execute the operation
                    String[] duplicatedJars = (String[]) ResourcesManager.executeOverResource(actionPojo);
                    // deinitialize the resource
                    ResourcesManager.deinitializeResource(sessionId, resourceId);

                    return Response.ok("{\"duplicated_jars\":" + GSON.toJson(duplicatedJars) + "}").build();
                case LOG_DUPLICATED_JARS_OPERATION:
                    // create action pojo
                    actionPojo = new ActionPojo(sessionId, -1, "auto-system-operations",
                                                "Internal System Operations log Duplicated Jars", new String[]{},
                                                new String[]{});
                    // initialize resource
                    resourceId = ResourcesManager.initializeResource(actionPojo);
                    actionPojo.setResourceId(resourceId);
                    // execute the operation
                    ResourcesManager.executeOverResource(actionPojo);
                    // deinitialize the resource
                    ResourcesManager.deinitializeResource(sessionId, resourceId);

                    return Response.ok("{\"status_message\":\"duplicated jars successfully logged\"}").build();
                case GET_AGENT_HOME_OPERATION:
                    return Response.ok("{\"agent_home\":" + System.getProperty(AtsSystemProperties.AGENT_HOME_FOLDER)
                                       + "}")
                                   .build();
                case IS_COMPONENT_LOADED_OPERATION:
                    value = getJsonElement(jsonObject, "value").getAsString(); // here the value acts as the component name
                    boolean loaded = ActionHandler.isComponentLoaded(ComponentRepository.DEFAULT_CALLER, value);
                    return Response.ok("{\"loaded\":" + loaded + "}").build();
                case GET_ATS_VERSION_OPERATION:
                    return Response.ok("{\"ats_version\":" + AtsVersion.getAtsVersion() + "}").build();
                case GET_NUMBER_PENDING_LOG_EVENTS:
                    int pendingLogEvents = -1;
                    PassiveDbAppender appender = PassiveDbAppender.getCurrentInstance();
                    if (appender != null) {
                        pendingLogEvents = appender.getNumberPendingLogEvents();
                    }
                    return Response.ok("{\"number_of_pending_log_events\":" + pendingLogEvents + "}").build();
                default:
                    throw new UnsupportedOperationException("Operation '" + operation + "' is not supported");
            }
        } catch (Exception e) {
            String message = "Unable to execute '" + operation + "' with argument {'" + value + "'}.";
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
