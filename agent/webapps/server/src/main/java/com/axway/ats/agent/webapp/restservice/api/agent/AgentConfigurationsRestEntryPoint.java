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
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.axway.ats.agent.core.MainComponentLoader;
import com.axway.ats.agent.core.configuration.AgentConfigurator;
import com.axway.ats.agent.core.configuration.ConfigurationManager;
import com.axway.ats.agent.core.configuration.Configurator;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerClass;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethod;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethodParameterDefinition;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethodParameterDefinitions;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethodResponse;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethodResponses;
import com.axway.ats.core.AtsVersion;
import com.axway.ats.core.threads.ThreadsPerCaller;
import com.axway.ats.core.utils.StringUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Path( "agent/configurations")
@SwaggerClass( "agent/configurations")
public class AgentConfigurationsRestEntryPoint {

    private static final Logger LOG  = Logger.getLogger(AgentConfigurationsRestEntryPoint.class);
    private static final Gson   GSON = new Gson();

    @PUT
    @Path( "/")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "put",
            parametersDefinition = "Put configuration to the agent details",
            summary = "Put configuration to the agent",
            url = "/")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "some session ID",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The configurators and its actual Java class names in a map ( className => JSON serialized configurator object )",
                                                  example = "{\"com.axway.ats.agent.core.configuration.RemoteLoggingConfigurator\":{}}",
                                                  name = "configurators",
                                                  type = "object[]")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully apply configurators to agent details",
                                       description = "Successfully apply configurators to agent. This method will return status message and the ATS agent version",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfully put configuration to agent",
                                               name = "status_message",
                                               type = "string"),
                                                                 @SwaggerMethodParameterDefinition(
                                                                         description = "The ATS agent version",
                                                                         example = "4.1.0",
                                                                         name = "ats_version",
                                                                         type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while applying configurators to agent details",
                                       description = "Error while applying configurators to agent",
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
    public Response pushConfiguration( @Context HttpServletRequest request ) {

        String sessionId = null;
        List<Configurator> configurators = null;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            String configuratorsJson = getJsonElement(jsonObject, "configurators").toString();
            if (StringUtils.isNullOrEmpty(configuratorsJson)) {
                throw new NoSuchElementException("configurators are not provided with the request");
            }
            configurators = getConfiguratorsFromJson(jsonObject);
            // Check if AgentConfigurator is set. In such case we will need to reload the
            // Agent components as this configuration defines the way Agent components are loaded.
            boolean needToReloadComponents = false;
            for (Configurator configurator : configurators) {
                if (configurator instanceof AgentConfigurator) {
                    needToReloadComponents = true;
                    break;
                }
            }
            if (needToReloadComponents) {
                // the already loaded Agent components are first unloaded
                MainComponentLoader.getInstance().destroy();

                // the initialization procedure will implicitly apply the new configurations
                // and then will load up again the Agent components
                MainComponentLoader.getInstance().initialize(configurators);
            } else {
                // just apply the configurations
                ConfigurationManager.getInstance().apply(configurators);
            }
            return Response.ok("{\"status_message\":\"successfully put configuration to agent\", \"ats_version\":\""
                               + AtsVersion.getAtsVersion() + "\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to put new configuration(s)";
            LOG.error(message, e);
            return Response.serverError()
                           .entity("{\"error\":" + GSON.toJson(e) + ", \"exceptionClass\":\"" + e.getClass().getName()
                                   + "\"}")
                           .build();
        } finally {
            ThreadsPerCaller.unregisterThread();
        }

    }

    private List<Configurator> getConfiguratorsFromJson( JsonObject jsonObject ) throws ClassNotFoundException {

        List<Configurator> configurators = new ArrayList<>();
        JsonObject configuratorsJsonObject = getJsonElement(jsonObject, "configurators").getAsJsonObject();
        for (Entry<String, JsonElement> entry : configuratorsJsonObject.entrySet()) {
            Class<?> configuratorClass = Class.forName(entry.getKey());
            Configurator configurator = (Configurator) GSON.fromJson(entry.getValue().toString(), configuratorClass);
            configurators.add(configurator);
        }

        return configurators;
    }

    private JsonElement getJsonElement( JsonObject object, String key ) {

        JsonElement element = object.get(key);
        if (element == null) {
            throw new NoSuchElementException("'" + key + "'" + " is not provided with the request");
        }
        return element;
    }

}
