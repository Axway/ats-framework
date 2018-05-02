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
package com.axway.ats.agent.webapp.restservice.api;

import java.util.Arrays;

import javax.servlet.ServletContext;
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

import com.axway.ats.agent.webapp.restservice.api.model.ActionPojo;
import com.axway.ats.agent.webapp.restservice.api.model.AgentPropertiesPojo;
import com.axway.ats.agent.webapp.restservice.model.pojo.ErrorPojo;
import com.axway.ats.core.utils.StringUtils;
import com.google.gson.Gson;

/**
 * Implementation of all public REST methods used to communicate
 * with the ATS Agent.
 */

@Path( "v1")
public class AgentRestServerImpl {

    @Context
    private ServletContext      servletContext;

    private static final Logger LOG = Logger.getLogger(AgentRestServerImpl.class);

    @POST
    @Path( "agent/properties")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    public Response agentProperties( @Context HttpServletRequest request, AgentPropertiesPojo pojo ) {

        String operation = pojo.getOperation();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Execution of agentProperties with the following arguments:{\n\toperation:" + pojo.getOperation()
                      + ",\n\tvalue:" + pojo.getValue() + "\n}");
        }

        try {
            if (StringUtils.isNullOrEmpty(operation)) {
                throw new UnsupportedOperationException("Provided operation is null or empty");
            }

            return AgentPropertiesHandler.executeOperation(operation, pojo.getValue());
        } catch (Exception e) {
            LOG.error(e);
            return Response.serverError().entity(new ErrorPojo(e)).build();
        }
    }

    @PUT
    @Path( "actions")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    public Response initializeAction( @Context HttpServletRequest request, ActionPojo pojo ) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Execution of initializeAction with the following arguments:{\n\tactionMethodName:"
                      + pojo.getActionMethodName()
                      + ",\n\tcomponentName:" + pojo.getComponentName() + "\n\targumentsTypes:"
                      + Arrays.toString(pojo.getArgumentsTypes()) + "\n\targumentsValues: "
                      + Arrays.toString(pojo.getArgumentsTypes()) + "\n}");
        }

        try {
            int actionId = ActionsManager.initializeAction(pojo);
            String response = "{\"actionId\":" + actionId + "}";
            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.error("Unable to initialize action '" + pojo.getActionMethodName() + "'", e);
            return Response.serverError().entity("{\"error\":" + new Gson().toJson(e) + "}").build();
        }

    }

    @DELETE
    @Path( "actions")
    @Produces( MediaType.APPLICATION_JSON)
    public Response deinitializeAction( @Context HttpServletRequest request, @QueryParam( "actionId") int actionId ) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Execution of deinitializeAction with the following arguments:{\n\tactionId:" + actionId + "\n}");
        }

        try {
            ActionsManager.deinitializeAction(actionId);
            String response = "{\"actionId\":" + actionId + ", \"status\":\"deleted\"}";
            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.error("Unable to deinitialize action with id '" + actionId + "'", e);
            return Response.serverError().entity("{\"error\":" + new Gson().toJson(e) + "}").build();
        }

    }

    @POST
    @Path( "actions/execute")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    public Response executeAction( @Context HttpServletRequest request, ActionPojo pojo ) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Execution of executeAction with the following arguments:{\n\tactionMethodName:"
                      + pojo.getActionMethodName()
                      + ",\n\tcomponentName:" + pojo.getComponentName() + "\n\targumentsTypes:"
                      + Arrays.toString(pojo.getArgumentsTypes()) + "\n\targumentsValues: "
                      + Arrays.toString(pojo.getArgumentsTypes()) + "\n}");
        }

        try {
            Object result = ActionsManager.executeAction(pojo);
            String response = "{\"result\":" + new Gson().toJson(result) + "}";
            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.error("Unable to execute action method '" + pojo.getActionMethodName() + "' on action class with id '"
                      + pojo.getActionId() + "'", e);
            return Response.serverError().entity("{\"error\":" + new Gson().toJson(e) + "}").build();
        }

    }

    

}
