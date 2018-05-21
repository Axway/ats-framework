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

import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.webapp.restservice.api.ResourcesManager;
import com.axway.ats.core.utils.StringUtils;
import com.google.gson.Gson;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Path( "actions")
@Api( value = "/actions")
public class ActionsRestEntryPoint {

    private static final Logger LOG  = Logger.getLogger(ActionsRestEntryPoint.class);
    private static final Gson   GSON = new Gson();

    @PUT
    @Path( "")
    @ApiOperation( value = "Initialize action class", notes = "Create instance of an action class")
    @ApiResponses( value = { @ApiResponse( code = 200, message = "Successful initialization of an action class", response = Response.class),
                             @ApiResponse( code = 500, message = "Internal server error", response = AgentException.class) })
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    public Response initialize( @ApiParam( value = "Action details", required = true) ActionPojo pojo ) {

        try {
            if (StringUtils.isNullOrEmpty(pojo.getSessionId())) {
                throw new IllegalArgumentException("Session ID is not provided with the request");
            }
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
        }

    }

    @POST
    @Path( "execute")
    @ApiOperation( value = "Execute action method", notes = "Execute action method")
    @ApiResponses( value = { @ApiResponse( code = 200, message = "Successful execution of an action method", response = Response.class),
                             @ApiResponse( code = 500, message = "Internal server error", response = AgentException.class) })
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)

    public Response execute( @ApiParam( value = "Action details", required = true) ActionPojo pojo ) {

        try {

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
        }

    }

    @DELETE
    @Path( "")
    @ApiOperation( value = "Deinitialize an action class", notes = "Deinitialize an action class")
    @ApiResponses( value = { @ApiResponse( code = 200, message = "Successful deinitialization of an action method", response = Response.class),
                             @ApiResponse( code = 500, message = "Internal server error", response = AgentException.class) })
    @Produces( MediaType.APPLICATION_JSON)
    public Response deinitialize( @Context HttpServletRequest request,
                                  @ApiParam( value = "Session ID", required = true) @QueryParam( "sessionId") String sessionId,
                                  @ApiParam( value = "Resource ID", required = true) @QueryParam( "resourceId") int resourceId ) {

        try {

            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new IllegalArgumentException("SessionID is not provided with the request");
            }

            if (resourceId < 0) {
                throw new IllegalArgumentException("ResourceID has invallid values (" + resourceId + ")");
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Deinitialization of action class with id '" + resourceId + "' for session with id '"
                          + sessionId + "'");
            }

            resourceId = ResourcesManager.deinitializeResource(sessionId, resourceId);
            String response = "{\"resourceId\":" + resourceId + ", \"status\":\"deleted\"}";
            return Response.ok(response).build();
        } catch (Exception e) {
            String message = "Unable to deinitialize action class with id'" + resourceId
                             + "' in session with id '" + sessionId + "'";
            LOG.error(message, e);
            return Response.serverError()
                           .entity("{\"error\":" + GSON.toJson(e) + ", \"exceptionClass\":\"" + e.getClass().getName()
                                   + "\"}")
                           .build();
        }

    }

}
