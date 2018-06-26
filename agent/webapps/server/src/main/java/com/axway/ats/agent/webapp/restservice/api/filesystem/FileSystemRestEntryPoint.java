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
package com.axway.ats.agent.webapp.restservice.api.filesystem;

import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
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
import com.axway.ats.common.filesystem.FileMatchInfo;
import com.axway.ats.common.filesystem.FileTailInfo;
import com.axway.ats.core.threads.ThreadsPerCaller;
import com.axway.ats.core.utils.StringUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@SwaggerClass( "filesystem")
@Path( "filesystem")
public class FileSystemRestEntryPoint {

    private static final Logger LOG  = Logger.getLogger(FileSystemRestEntryPoint.class);
    private static final Gson   GSON = new Gson();

    @PUT
    @Path( "/")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "put",
            parametersDefinition = "Initialize file system details",
            summary = "Initialize file system",
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
                                       definition = "Successfully initialize file system resource details",
                                       description = "Successfully initialize file system resource",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The resource ID of the newly initialized resource",
                                               example = "123",
                                               name = "resourceId",
                                               type = "integer") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while initializing file system resource details",
                                       description = "Error while initializing file system resource",
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
    public Response initializeFileSystem( @Context HttpServletRequest request ) {

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
            int resourceId = FileSystemManager.initialize(sessionId);
            return Response.ok("{\"resourceId\":" + resourceId + "}").build();
        } catch (Exception e) {
            String message = "Unable to initialize file system resource from session with id '" + sessionId + "'";
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
    @Path( "file/append")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Append content to file details",
            summary = "Append content to file",
            url = "file/append")
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
                                                  description = "The file path",
                                                  example = "/home/atsuser/file.txt",
                                                  name = "filePath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The content, which will be appended to the end of the file",
                                                  example = "some content",
                                                  name = "contentToAdd",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully append content to file details",
                                       description = "Successfully append content to file",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "succcessfully append content to file",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while appending content to file details",
                                       description = "Error while appending content to file",
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
    public Response appendToFile( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String filePath = null;
        String contentToAdd = null;
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
            filePath = getJsonElement(jsonObject, "filePath").getAsString();
            if (StringUtils.isNullOrEmpty(filePath)) {
                throw new NoSuchElementException("filePath is not provided with the request");
            }
            contentToAdd = getJsonElement(jsonObject, "contentToAdd").getAsString();
            if (StringUtils.isNullOrEmpty(contentToAdd)) {
                throw new NoSuchElementException("contentToAdd is not provided with the request");
            }
            FileSystemManager.appendToFile(sessionId, resourceId, filePath, contentToAdd);
            return Response.ok("{\"status_message\":\"succcessfully append content to file\"}").build();
        } catch (Exception e) {
            String message = "Unable to append content to file using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file/permissions")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get file permissions",
            url = "file/permissions")
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
                                                  description = "The file name",
                                                  example = "/home/atsuser/file.txt",
                                                  name = "fileName",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully get file permissions details",
                                       description = "Successfully get file permissions",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The file permissions",
                                               example = "0777",
                                               name = "action_result",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting file permissions details",
                                       description = "Error while getting file permissions",
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
    public Response getFilePermissions( @Context HttpServletRequest request, @QueryParam(
            value = "sessionId") String sessionId,
                                        @QueryParam(
                                                value = "resourceId") int resourceId,
                                        @QueryParam(
                                                value = "fileName") String fileName ) {

        try {

            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            sessionId = URLDecoder.decode(sessionId, "UTF-8");
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            if (StringUtils.isNullOrEmpty(fileName)) {
                throw new NoSuchElementException("fileName is not provided with the request");
            }
            fileName = URLDecoder.decode(fileName, "UTF-8");
            String filePermissions = FileSystemManager.getFilePermissions(sessionId, resourceId, fileName);
            return Response.ok("{\"action_result\":" + GSON.toJson(filePermissions) + "}").build();
        } catch (Exception e) {
            String message = "Unable to get file permissions using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file/filegroup")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get file group",
            url = "file/filegroup")
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
                                                  description = "The file name",
                                                  example = "/home/atsuser/file.txt",
                                                  name = "fileName",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully get file group details",
                                       description = "Successfully get file group",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The file group",
                                               example = "some file group",
                                               name = "action_result",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting file group details",
                                       description = "Error while getting file group",
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
    public Response getFileGroup( @Context HttpServletRequest request, @QueryParam(
            value = "sessionId") String sessionId,
                                  @QueryParam(
                                          value = "resourceId") int resourceId,
                                  @QueryParam(
                                          value = "fileName") String fileName ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            sessionId = URLDecoder.decode(sessionId, "UTF-8");
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            if (StringUtils.isNullOrEmpty(fileName)) {
                throw new NoSuchElementException("fileName is not provided with the request");
            }
            fileName = URLDecoder.decode(fileName, "UTF-8");
            String fileGroup = FileSystemManager.getFileGroup(sessionId, resourceId, fileName);
            return Response.ok("{\"action_result\":" + GSON.toJson(fileGroup) + "}").build();
        } catch (Exception e) {
            String message = "Unable to get file group using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file/gid")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get file GID",
            url = "file/gid")
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
                                                  description = "The file name",
                                                  example = "/home/atsuser/file.txt",
                                                  name = "fileName",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully get file GID details",
                                       description = "Successfully get file GID",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The file GID",
                                               example = "some file GID",
                                               name = "action_result",
                                               type = "long") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting file GID details",
                                       description = "Error while getting file GID",
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
    public Response getFileGID( @Context HttpServletRequest request, @QueryParam(
            value = "sessionId") String sessionId,
                                @QueryParam(
                                        value = "resourceId") int resourceId,
                                @QueryParam(
                                        value = "fileName") String fileName ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            sessionId = URLDecoder.decode(sessionId, "UTF-8");
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            if (StringUtils.isNullOrEmpty(fileName)) {
                throw new NoSuchElementException("fileName is not provided with the request");
            }
            fileName = URLDecoder.decode(fileName, "UTF-8");
            long fileGroup = FileSystemManager.getFileGID(sessionId, resourceId, fileName);
            return Response.ok("{\"action_result\":" + GSON.toJson(fileGroup) + "}").build();
        } catch (Exception e) {
            String message = "Unable to get file GID using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file/owner")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get file owner",
            url = "file/owner")
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
                                                  description = "The file name",
                                                  example = "/home/atsuser/file.txt",
                                                  name = "fileName",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully get file owner details",
                                       description = "Successfully get file owner",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The file owner",
                                               example = "some file owner",
                                               name = "action_result",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting file owner details",
                                       description = "Error while getting file owner",
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
    public Response getFileOwner( @Context HttpServletRequest request, @QueryParam(
            value = "sessionId") String sessionId,
                                  @QueryParam(
                                          value = "resourceId") int resourceId,
                                  @QueryParam(
                                          value = "fileName") String fileName ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            sessionId = URLDecoder.decode(sessionId, "UTF-8");
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            if (StringUtils.isNullOrEmpty(fileName)) {
                throw new NoSuchElementException("fileName is not provided with the request");
            }
            fileName = URLDecoder.decode(fileName, "UTF-8");
            String fileOwner = FileSystemManager.getFileOwner(sessionId, resourceId, fileName);
            return Response.ok("{\"action_result\":" + GSON.toJson(fileOwner) + "}").build();
        } catch (Exception e) {
            String message = "Unable to get file owner using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file/uid")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get file UID",
            url = "file/uid")
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
                                                  description = "The file name",
                                                  example = "/home/atsuser/file.txt",
                                                  name = "fileName",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully get file UID details",
                                       description = "Successfully get file UID",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The file UID",
                                               example = "some file UID",
                                               name = "action_result",
                                               type = "long") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting file UID details",
                                       description = "Error while getting file UID",
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
    public Response getFileUID( @Context HttpServletRequest request, @QueryParam(
            value = "sessionId") String sessionId,
                                @QueryParam(
                                        value = "resourceId") int resourceId,
                                @QueryParam(
                                        value = "fileName") String fileName ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            sessionId = URLDecoder.decode(sessionId, "UTF-8");
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            if (StringUtils.isNullOrEmpty(fileName)) {
                throw new NoSuchElementException("fileName is not provided with the request");
            }
            fileName = URLDecoder.decode(fileName, "UTF-8");
            long fileUID = FileSystemManager.getFileUID(sessionId, resourceId, fileName);
            return Response.ok("{\"action_result\":" + GSON.toJson(fileUID) + "}").build();
        } catch (Exception e) {
            String message = "Unable to get file UID using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file/modificationTime")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get file modification time",
            url = "file/modificationTime")
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
                                                  description = "The file name",
                                                  example = "/home/atsuser/file.txt",
                                                  name = "fileName",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully get file modification time details",
                                       description = "Successfully get file modification time",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The file modification time in milliseconds",
                                               example = "1002120122",
                                               name = "action_result",
                                               type = "long") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting file modification time details",
                                       description = "Error while getting file modification time",
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
    public Response getFileModificationTime( @Context HttpServletRequest request, @QueryParam(
            value = "sessionId") String sessionId,
                                             @QueryParam(
                                                     value = "resourceId") int resourceId,
                                             @QueryParam(
                                                     value = "fileName") String fileName ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            sessionId = URLDecoder.decode(sessionId, "UTF-8");
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            if (StringUtils.isNullOrEmpty(fileName)) {
                throw new NoSuchElementException("fileName is not provided with the request");
            }
            fileName = URLDecoder.decode(fileName, "UTF-8");
            long modificationTime = FileSystemManager.getFileModificationTime(sessionId, resourceId, fileName);
            return Response.ok("{\"action_result\":" + GSON.toJson(modificationTime) + "}").build();
        } catch (Exception e) {
            String message = "Unable to get file modification time using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file/size")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get file size",
            url = "file/size")
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
                                                  description = "The file name",
                                                  example = "/home/atsuser/file.txt",
                                                  name = "fileName",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully get file size details",
                                       description = "Successfully get file size",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The file size in bytes",
                                               example = "1024",
                                               name = "action_result",
                                               type = "long") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting file size details",
                                       description = "Error while getting file size",
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
    public Response getFileSize( @Context HttpServletRequest request, @QueryParam(
            value = "sessionId") String sessionId,
                                 @QueryParam(
                                         value = "resourceId") int resourceId,
                                 @QueryParam(
                                         value = "fileName") String fileName ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            sessionId = URLDecoder.decode(sessionId, "UTF-8");
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            if (StringUtils.isNullOrEmpty(fileName)) {
                throw new NoSuchElementException("fileName is not provided with the request");
            }
            fileName = URLDecoder.decode(fileName, "UTF-8");
            long fileSize = FileSystemManager.getFileSize(sessionId, resourceId, fileName);
            return Response.ok("{\"action_result\":" + GSON.toJson(fileSize) + "}").build();
        } catch (Exception e) {
            String message = "Unable to get file size using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file/uniqueId")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get file unique ID",
            url = "file/uniqueId")
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
                                                  description = "The file name",
                                                  example = "/home/atsuser/file.txt",
                                                  name = "fileName",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully get file unique ID details",
                                       description = "Successfully get file unique ID",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The file unique ID",
                                               example = "1024",
                                               name = "action_result",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting file unique ID details",
                                       description = "Error while getting file unique ID",
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
    public Response getFileUniqueID( @Context HttpServletRequest request, @QueryParam(
            value = "sessionId") String sessionId,
                                     @QueryParam(
                                             value = "resourceId") int resourceId,
                                     @QueryParam(
                                             value = "fileName") String fileName ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            sessionId = URLDecoder.decode(sessionId, "UTF-8");
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            if (StringUtils.isNullOrEmpty(fileName)) {
                throw new NoSuchElementException("fileName is not provided with the request");
            }
            fileName = URLDecoder.decode(fileName, "UTF-8");
            String uniqueID = FileSystemManager.getFileUniqueID(sessionId, resourceId, fileName);
            return Response.ok("{\"action_result\":" + GSON.toJson(uniqueID) + "}").build();
        } catch (Exception e) {
            String message = "Unable to get file unique ID using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file/uid")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Set file U I D details",
            summary = "Set file U I D",
            url = "file/uid")
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
                                                  description = "The file name",
                                                  example = "/home/atsuser/file.txt",
                                                  name = "fileName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The file UID",
                                                  example = "1000",
                                                  name = "uid",
                                                  type = "long")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully set file U I D details",
                                       description = "Successfully set file U I D",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "file uid successfully set to <some_uid>",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while setting file U I D details",
                                       description = "Error while setting file U I D",
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
    public Response setFileUID( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String fileName = null;
        long uid = -1;
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
            fileName = getJsonElement(jsonObject, "fileName").getAsString();
            if (StringUtils.isNullOrEmpty(fileName)) {
                throw new NoSuchElementException("fileName is not provided with the request");
            }
            uid = getJsonElement(jsonObject, "uid").getAsLong();
            if (uid < 0) {
                throw new IllegalArgumentException("uid has invallid value '" + uid + "'");
            }
            FileSystemManager.setFileUID(sessionId, resourceId, fileName, uid);
            return Response.ok("{\"status_message\":" + "\"successfully set uid to '" + uid + "'" + "\"}").build();
        } catch (Exception e) {
            String message = "Unable to set file U I D using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file/gid")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Set file G I D details",
            summary = "Set file G I D",
            url = "file/gid")
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
                                                  description = "The file name",
                                                  example = "/home/atsuser/file.txt",
                                                  name = "fileName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The file GID",
                                                  example = "1000",
                                                  name = "uid",
                                                  type = "long")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully set file G I D details",
                                       description = "Successfully set file G I D",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "file uid successfully set to <some_gid>",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while setting file G I D details",
                                       description = "Error while setting file G I D",
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
    public Response setFileGID( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String fileName = null;
        long gid = -1;
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
            fileName = getJsonElement(jsonObject, "fileName").getAsString();
            if (StringUtils.isNullOrEmpty(fileName)) {
                throw new NoSuchElementException("fileName is not provided with the request");
            }
            gid = getJsonElement(jsonObject, "gid").getAsLong();
            if (gid < 0) {
                throw new IllegalArgumentException("gid has invallid value '" + gid + "'");
            }
            FileSystemManager.setFileGID(sessionId, resourceId, fileName, gid);
            return Response.ok("{\"status_message\":" + "\"successfully set gid to '" + gid + "'" + "\"}").build();
        } catch (Exception e) {
            String message = "Unable to set file U I D using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file/permissions")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Set file permissions details",
            summary = "Set file permissions",
            url = "file/permissions")
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
                                                  description = "The file name",
                                                  example = "/home/atsuser/file.txt",
                                                  name = "fileName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The file permissions",
                                                  example = "0777",
                                                  name = "permissions",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully set file permissions details",
                                       description = "Successfully set file permissions",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "file uid successfully set to <permissions>",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while setting file permissions details",
                                       description = "Error while setting file permissions",
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
    public Response setFilePermissions( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String fileName = null;
        String permissions = null;
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
            fileName = getJsonElement(jsonObject, "fileName").getAsString();
            if (StringUtils.isNullOrEmpty(fileName)) {
                throw new NoSuchElementException("fileName is not provided with the request");
            }
            permissions = getJsonElement(jsonObject, "permissions").getAsString();
            if (StringUtils.isNullOrEmpty(permissions)) {
                throw new IllegalArgumentException("permissions is not provided with the request");
            }
            FileSystemManager.setFilePermissions(sessionId, resourceId, fileName, permissions);
            return Response.ok("{\"status_message\":" + "\"successfully set permissions to '" + permissions + "'"
                               + "\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to set file permissions using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file/modificationTime")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Set file modification time details",
            summary = "Set file modification time",
            url = "file/modificationTime")
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
                                                  description = "The file name",
                                                  example = "/home/atsuser/file.txt",
                                                  name = "fileName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The file modification time",
                                                  example = "1021301023423",
                                                  name = "modificationTime",
                                                  type = "long")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully set file modification time details",
                                       description = "Successfully set file modification time",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "file modification time successfully set to <modification time>",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while setting file modification time details",
                                       description = "Error while setting file modification time",
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
    public Response setFileModificationTime( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String fileName = null;
        long modificationTime = -1;
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
            fileName = getJsonElement(jsonObject, "fileName").getAsString();
            if (StringUtils.isNullOrEmpty(fileName)) {
                throw new NoSuchElementException("fileName is not provided with the request");
            }
            modificationTime = getJsonElement(jsonObject, "modificationTime").getAsLong();
            if (modificationTime < 0) {
                throw new IllegalArgumentException("modificationTime has invallid value '" + modificationTime + "'");
            }
            FileSystemManager.setFileModicationTime(sessionId, resourceId, fileName, modificationTime);
            return Response.ok("{\"status_message\":" + "\"successfully set file modification time to '"
                               + modificationTime + "'" + "\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to set file modification time using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file/hidden")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Set whether file is hidden details",
            summary = "Set whether file is hidden",
            url = "file/hidden")
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
                                                  description = "The file name",
                                                  example = "/home/atsuser/file.txt",
                                                  name = "fileName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Flag for hidden file",
                                                  example = "TRUE|FALSE",
                                                  name = "hidden",
                                                  type = "boolean")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully set file to be hidden details",
                                       description = "Successfully set file to be hidden",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "file hidden attribute successfully set to <true_or_false>",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while setting file hidden attribute details",
                                       description = "Error while setting file hidden attribute",
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
    public Response setFileHiddenAttribute( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String fileName = null;
        boolean hidden = false;
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
            fileName = getJsonElement(jsonObject, "fileName").getAsString();
            if (StringUtils.isNullOrEmpty(fileName)) {
                throw new NoSuchElementException("fileName is not provided with the request");
            }
            hidden = getJsonElement(jsonObject, "hidden").getAsBoolean();
            FileSystemManager.setFileHiddenAttribute(sessionId, resourceId, fileName, hidden);
            return Response.ok("{\"status_message\":" + "\"successfully set file hidden attribute to '"
                               + hidden + "'" + "\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to set file hidden attribute using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file/exist")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Check whether file exists",
            url = "file/exist")
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
                                                  description = "The file name",
                                                  example = "/home/atsuser/file.txt",
                                                  name = "fileName",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully check does file exists details",
                                       description = "Successfully check does file exists",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Whether file exists",
                                               example = "TRUE|FALSE",
                                               name = "action_result",
                                               type = "boolean") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while checking does file exists details",
                                       description = "Error while checking does file exists",
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
    public Response doesFileExists( @Context HttpServletRequest request,
                                    @QueryParam(
                                            value = "sessionId") String sessionId,
                                    @QueryParam(
                                            value = "resourceId") int resourceId,
                                    @QueryParam(
                                            value = "fileName") String fileName ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            sessionId = URLDecoder.decode(sessionId, "UTF-8");
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            fileName = URLDecoder.decode(fileName, "UTF-8");
            if (StringUtils.isNullOrEmpty(fileName)) {
                throw new NoSuchElementException("fileName is not provided with the request");
            }
            boolean exists = FileSystemManager.doesFileExists(sessionId, resourceId, fileName);
            return Response.ok("{\"action_result\":" + GSON.toJson(exists) + "}").build();
        } catch (Exception e) {
            String message = "Unable to check whether file exists using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "put",
            parametersDefinition = "Create file details",
            summary = "Create file",
            url = "file")
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
                                                  description = "The file name",
                                                  example = "/home/atsuser/file.txt",
                                                  name = "fileName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Optional file content. In order for this field to be taken into consideration, isRandomContent must be false",
                                                  example = "some content",
                                                  name = "fileContent",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The file size in bytes. If fileContent is set, this field can be set to -1",
                                                  example = "1024",
                                                  name = "fileSize",
                                                  type = "long"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The file user ID ( UID )",
                                                  example = "1000",
                                                  name = "uid",
                                                  type = "long"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The file group ID ( GID )",
                                                  example = "1000",
                                                  name = "gid",
                                                  type = "long"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The type of the line ending (*NIX, WIN, MAC)",
                                                  example = "\\n",
                                                  name = "eol",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Should random file content by generated for the newly created file or not",
                                                  example = "TRUE|FALSE",
                                                  name = "isRandomContent",
                                                  type = "boolean"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Determine whether the file binary or not",
                                                  example = "TRUE|FALSE",
                                                  name = "isBinary",
                                                  type = "boolean"),
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully create file details",
                                       description = "Successfully create file",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "file successfully created",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while creating file details",
                                       description = "Error while creating file",
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
    public Response createFile( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String fileName = null;
        String fileContent = null;
        long fileSize = -1;
        long uid = -1;
        long gid = -1;
        String eol = null;
        boolean isRandomContent = false;
        boolean isBinary = false;
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
            fileName = getJsonElement(jsonObject, "fileName").getAsString();
            if (StringUtils.isNullOrEmpty(fileName)) {
                throw new NoSuchElementException("fileName is not provided with the request");
            }
            if (!getJsonElement(jsonObject, "fileContent").isJsonNull()) {
                fileContent = getJsonElement(jsonObject, "fileContent").getAsString();
            }
            fileSize = getJsonElement(jsonObject, "fileSize").getAsLong();
            uid = getJsonElement(jsonObject, "uid").getAsLong();
            gid = getJsonElement(jsonObject, "gid").getAsLong();
            if (!getJsonElement(jsonObject, "eol").isJsonNull()) {
                eol = getJsonElement(jsonObject, "eol").getAsString();
            }
            isRandomContent = getJsonElement(jsonObject, "isRandomContent").getAsBoolean();
            isBinary = getJsonElement(jsonObject, "isBinary").getAsBoolean();
            FileSystemManager.createFile(sessionId, resourceId, fileName, fileContent, fileSize, uid, gid, eol,
                                         isRandomContent, isBinary);
            return Response.ok("{\"status_message\":\"file successfully create\"}").build();
        } catch (Exception e) {
            String message = "Unable to create file using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "delete",
            parametersDefinition = "",
            summary = "Delete file",
            url = "file")
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
                                                  description = "The file name",
                                                  example = "/home/atsuser/file.txt",
                                                  name = "fileName",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully deleted file details",
                                       description = "Successfully deleted file",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "file successfully deleted",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while deleting file details",
                                       description = "Error while deleting file",
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
    public Response deleteFile( @Context HttpServletRequest request,
                                @QueryParam(
                                        value = "sessionId") String sessionId,
                                @QueryParam(
                                        value = "resourceId") int resourceId,
                                @QueryParam(
                                        value = "fileName") String fileName ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            sessionId = URLDecoder.decode(sessionId, "UTF-8");
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            if (StringUtils.isNullOrEmpty(fileName)) {
                throw new NoSuchElementException("fileName is not provided with the request");
            }
            fileName = URLDecoder.decode(fileName, "UTF-8");
            FileSystemManager.deleteFile(sessionId, resourceId, fileName);
            return Response.ok("{\"status_message\":\"file successfully deleted\"}").build();
        } catch (Exception e) {
            String message = "Unable to delete file using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Rename file details",
            summary = "Rename file",
            url = "file")
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
                                                  description = "The old file name",
                                                  example = "/home/atsuser/old_file.txt",
                                                  name = "oldFileName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The new file name",
                                                  example = "/home/atsuser/new_file.txt",
                                                  name = "newFileName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Whether to overwrite already existing file with name <newFileName>",
                                                  example = "TRUE|FALSE",
                                                  name = "overwrtite",
                                                  type = "booloean")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully renamed file details",
                                       description = "Successfully renamed file",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "file '<file_name> successfully renamed",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while renaming file details",
                                       description = "Error while renaming file",
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
    public Response renameFile( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String oldFileName = null;
        String newFileName = null;
        boolean overwrite = false;
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
            oldFileName = getJsonElement(jsonObject, "oldFileName").getAsString();
            if (StringUtils.isNullOrEmpty(oldFileName)) {
                throw new NoSuchElementException("oldFileName is not provided with the request");
            }
            newFileName = getJsonElement(jsonObject, "newFileName").getAsString();
            if (StringUtils.isNullOrEmpty(newFileName)) {
                throw new NoSuchElementException("newFileName is not provided with the request");
            }
            overwrite = getJsonElement(jsonObject, "overwrite").getAsBoolean();
            FileSystemManager.renameFile(sessionId, resourceId, oldFileName, newFileName, overwrite);
            return Response.ok("{\"status_message\":\"file successfully renamed\"}").build();
        } catch (Exception e) {
            String message = "Unable to rename file using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file/content/lastLines")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get last lines from file",
            url = "file/content/lastLines")
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
                                                  description = "The file name",
                                                  example = "/home/atsuser/file.txt",
                                                  name = "fileName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The number of lines to be read fom the end of the file",
                                                  example = "7",
                                                  name = "numberOfLines",
                                                  type = "integer"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The charset name of the file. ",
                                                  example = "See java.nio.charset.StandardCharsets "
                                                            + "class for more details on which charsets are supported.",
                                                  name = "charset",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully got last line from file details",
                                       description = "Successfully got last line from file",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Array of the last line",
                                               example = "[\"line_1\",\"line_2\"]",
                                               name = "action_result",
                                               type = "string[]") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting last lines from file details",
                                       description = "Error while getting last lines from file",
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
    public Response getLastLines( @Context HttpServletRequest request,
                                  @QueryParam(
                                          value = "sessionId") String sessionId,
                                  @QueryParam(
                                          value = "resourceId") int resourceId,
                                  @QueryParam(
                                          value = "fileName") String fileName,
                                  @QueryParam(
                                          value = "numberOfLines") int numberOfLines,
                                  @QueryParam(
                                          value = "charset") String charset ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            sessionId = URLDecoder.decode(sessionId, "UTF-8");
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            if (StringUtils.isNullOrEmpty(fileName)) {
                throw new NoSuchElementException("fileName is not provided with the request");
            }
            fileName = URLDecoder.decode(fileName, "UTF-8");
            if (StringUtils.isNullOrEmpty(charset)) {
                charset = StandardCharsets.ISO_8859_1.name();
            }
            String[] lastLines = FileSystemManager.getLastLines(sessionId, resourceId, fileName, numberOfLines,
                                                                charset);
            return Response.ok("{\"action_result\":" + GSON.toJson(lastLines, String[].class) + "}").build();
        } catch (Exception e) {
            String message = "Unable to get last lines from file using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file/content")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Read file",
            url = "file/content")
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
                                                  description = "The file name",
                                                  example = "/home/atsuser/file.txt",
                                                  name = "fileName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The file encoding",
                                                  example = "UTF-8",
                                                  name = "fileEncoding",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully read file details",
                                       description = "Successfully read file",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The file content",
                                               example = "some text",
                                               name = "action_result",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while reading file details",
                                       description = "Error while reading file",
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
    public Response readFile( @Context HttpServletRequest request,
                              @QueryParam(
                                      value = "sessionId") String sessionId,
                              @QueryParam(
                                      value = "resourceId") int resourceId,
                              @QueryParam(
                                      value = "fileName") String fileName,
                              @QueryParam(
                                      value = "fileEncoding") String fileEncoding ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            sessionId = URLDecoder.decode(sessionId, "UTF-8");
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            if (StringUtils.isNullOrEmpty(fileName)) {
                throw new NoSuchElementException("fileName is not provided with the request");
            }
            fileName = URLDecoder.decode(fileName, "UTF-8");

            String fileContent = FileSystemManager.readFile(sessionId, resourceId, fileName, fileEncoding);
            return Response.ok("{\"action_result\":" + GSON.toJson(fileContent, String.class) + "}").build();
        } catch (Exception e) {
            String message = "Unable to read file using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file/content/tail")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Read file from certain position",
            url = "file/content/tail")
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
                                                  description = "The file name",
                                                  example = "/home/atsuser/file.txt",
                                                  name = "fileName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The offset in the file from which the content will be read",
                                                  example = "100",
                                                  name = "fromBytePosition",
                                                  type = "long")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully execute tail over file content details",
                                       description = "Successfully execute tail over file content file",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "File tail info object",
                                               example = "{\"currentPosition\": 152300,\"isFileRotated\": false,\"newContent\": \"some new content\"}",
                                               name = "action_result",
                                               type = "object") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while reading file details",
                                       description = "Error while reading file",
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
    public Response readFileFromPosition( @Context HttpServletRequest request,
                                          @QueryParam(
                                                  value = "sessionId") String sessionId,
                                          @QueryParam(
                                                  value = "resourceId") int resourceId,
                                          @QueryParam(
                                                  value = "fileName") String fileName,
                                          @QueryParam(
                                                  value = "fromBytePosition") long fromBytePosition ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            sessionId = URLDecoder.decode(sessionId, "UTF-8");
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            if (StringUtils.isNullOrEmpty(fileName)) {
                throw new NoSuchElementException("fileName is not provided with the request");
            }
            fileName = URLDecoder.decode(fileName, "UTF-8");

            FileTailInfo fileContent = FileSystemManager.readFileFromPosition(sessionId, resourceId, fileName,
                                                                              fromBytePosition);
            return Response.ok("{\"action_result\":" + GSON.toJson(fileContent, FileTailInfo.class) + "}").build();
        } catch (Exception e) {
            String message = "Unable to read file from position '" + fromBytePosition
                             + "' using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file/md5sum")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get file MD5 sum details",
            url = "file/md5sum")
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
                                                  description = "The file name",
                                                  example = "/home/atsuser/file.txt",
                                                  name = "fileName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Whether different file ending will produce different md5 (BINARY) or not (ASCII)",
                                                  example = "ASCII|BINARY",
                                                  name = "md5SumMode",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully get file MD5 sum details",
                                       description = "Successfully get file MD5 sum",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The file's MD5 sum",
                                               example = "e8e28d025aaec86eaecae163f09c0e9d",
                                               name = "action_result",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error getting file MD5 sum details",
                                       description = "Error getting file MD5 sum",
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

    public Response computeMd5Sum( @Context HttpServletRequest request,
                                   @QueryParam(
                                           value = "sessionId") String sessionId,
                                   @QueryParam(
                                           value = "resourceId") int resourceId,
                                   @QueryParam(
                                           value = "fileName") String fileName,
                                   @QueryParam(
                                           value = "md5SumMode") String md5SumMode ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            if (StringUtils.isNullOrEmpty(fileName)) {
                throw new NoSuchElementException("fileName is not provided with the request");
            }
            if (StringUtils.isNullOrEmpty(md5SumMode)) {
                throw new NoSuchElementException("md5SumMode is not provided with the request");
            }
            String md5sum = FileSystemManager.computeMd5Sum(sessionId, resourceId, fileName, md5SumMode);
            return Response.ok("{\"action_result\":" + GSON.toJson(md5sum, String.class) + "}").build();
        } catch (Exception e) {
            String message = "Unable to get file MD5 sum using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file/content/replace")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Replace specific texts in file details",
            summary = "Replace specific texts in file",
            url = "file/content/replace")
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
                                                  description = "The file name",
                                                  example = "/home/atsuser/old_file.txt",
                                                  name = "fileName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The text that will be replaced in the file.",
                                                  example = "Guess I'll be replaced",
                                                  name = "searchString",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The replacement text",
                                                  example = "Guess I was replaced",
                                                  name = "newString",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Whether the searchString is REGEX or plain text",
                                                  example = "TRUE|FALSE",
                                                  name = "isRegex",
                                                  type = "boolean"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "You can replace more than one text by using this field, instead searchString and newString.",
                                                  example = "{\"oldText\":\"newText\"}",
                                                  name = "searchTokens",
                                                  type = "object")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully replace text in file details",
                                       description = "Successfully replace text in file",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "file content successfully replaced",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while renaming file details",
                                       description = "Error while renaming file",
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
    public Response findTextInFileAfterGivenPosition( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String fileName = null;
        String searchString = null;
        String newString = null;
        Map<String, String> searchTokens = null;
        boolean isRegex = false;
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
            fileName = getJsonElement(jsonObject, "fileName").getAsString();
            if (StringUtils.isNullOrEmpty(fileName)) {
                throw new NoSuchElementException("fileName is not provided with the request");
            }
            isRegex = getJsonElement(jsonObject, "isRegex").getAsBoolean();
            boolean searchStringNotProvided = false;
            boolean newStringNotProvided = false;
            try {
                searchString = getJsonElement(jsonObject, "searchString").getAsString();
            } catch (Exception e) {
                // since map can be used instead that field, to not throw exception yet
                searchStringNotProvided = true;
            }
            try {
                newString = getJsonElement(jsonObject, "newString").getAsString();
            } catch (Exception e) {
                // since map can be used instead that field, to not throw exception yet
                newStringNotProvided = true;
            }

            if (searchStringNotProvided && newStringNotProvided) {
                // check if map is provided
                String searchTokensJSON = getJsonElement(jsonObject, "searchTokens").toString();
                if (StringUtils.isNullOrEmpty(searchTokensJSON)) {
                    throw new NoSuchElementException("searchTokens is not provided with the request");
                }
                searchTokens = GSON.fromJson(searchTokensJSON, Map.class);
                FileSystemManager.replaceText(sessionId, resourceId, fileName, searchTokens, isRegex);
            } else {
                if (!searchStringNotProvided && !newStringNotProvided) {
                    // both are specified
                    // use them instead the searchTokens's map
                    FileSystemManager.replaceText(sessionId, resourceId, fileName, searchString, newString, isRegex);
                } else {
                    // one of them is provided, but the other is not
                    // we do not care if searchTokens map is provided, since one of those fields is provided, an error will be thrown
                    throw new IllegalArgumentException("Both 'searchString' and 'newString' fields must be provided.");
                }
            }
            return Response.ok("{\"status_message\":\"file content successfully replaced\"}").build();
        } catch (Exception e) {
            String message = "Unable to replace text in file using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file/content/find")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Find text in file after given position details",
            summary = "Find text in file after given position",
            url = "file/content/find")
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
                                                  description = "The file name",
                                                  example = "/home/atsuser/old_file.txt",
                                                  name = "fileName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Whether the provided search texts are REGEXes or plain text ones",
                                                  example = "TRUE|FALSE",
                                                  name = "isRegex",
                                                  type = "boolean"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The patterns that will be searched in the file content",
                                                  example = "[\"[a-z]\"]",
                                                  name = "searchTexts",
                                                  type = "string[]"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The initial offset in the file from which the search will begin",
                                                  example = "100",
                                                  name = "searchFromPosition",
                                                  type = "long"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The current position in the file",
                                                  example = "10",
                                                  name = "currentLineNumber",
                                                  type = "int")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully find text in file after given position details",
                                       description = "Successfully find text in file after given position",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "File match info object",
                                               example = "{" +
                                                         "    \"numberOfMatchedLines\": 1," +
                                                         "    \"matched\": true," +
                                                         "    \"lastReadByte\": 55," +
                                                         "    \"lastReadLineNumber\": 15," +
                                                         "    \"lineNumbers\": [3]," +
                                                         "    \"lines\": [\"testCaseState\"]," +
                                                         "    \"matchedPatterns\": [\"test\"]" +
                                                         "}",
                                               name = "action_result",
                                               type = "object") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while searching for text in file details",
                                       description = "Error while searching for text in file",
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
    public Response findTextAfterGivenPositionInFile( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String fileName = null;
        boolean isRegex = false;
        String[] searchTexts = null;
        long searchFromPosition = -1;
        int currentLineNumber = -1;
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
            fileName = getJsonElement(jsonObject, "fileName").getAsString();
            if (StringUtils.isNullOrEmpty(fileName)) {
                throw new NoSuchElementException("fileName is not provided with the request");
            }
            isRegex = getJsonElement(jsonObject, "isRegex").getAsBoolean();
            String searchTextsJSON = getJsonElement(jsonObject, "searchTexts").toString();
            if (StringUtils.isNullOrEmpty(searchTextsJSON)) {
                throw new NoSuchElementException("searchTexts is not provided with the request");
            }
            searchTexts = GSON.fromJson(searchTextsJSON, String[].class);
            searchFromPosition = getJsonElement(jsonObject, "searchFromPosition").getAsLong();
            if (searchFromPosition < 0) {
                throw new IllegalArgumentException("searchFromPosition has invallid value '" + resourceId + "'");
            }
            currentLineNumber = getJsonElement(jsonObject, "currentLineNumber").getAsInt();
            if (currentLineNumber < 0) {
                throw new IllegalArgumentException("currentLineNumber has invallid value '" + resourceId + "'");
            }
            FileMatchInfo fmi = FileSystemManager.findTextAfterGivenPositionInFile(sessionId, resourceId, fileName,
                                                                                   searchTexts, isRegex,
                                                                                   searchFromPosition,
                                                                                   currentLineNumber);
            return Response.ok("{\"action_result\":" + GSON.toJson(fmi, FileMatchInfo.class) + "}").build();
        } catch (Exception e) {
            String message = "Unable to find text after given position in file using filesystem resource with id '"
                             + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file/content/grep")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Execute grep operation on file",
            url = "file/content/grep")
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
                                                  description = "The file name",
                                                  example = "/home/atsuser/file.txt",
                                                  name = "fileName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The String to match",
                                                  example = "some string",
                                                  name = "searchPattern",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Whether to use DOS/WIN-like characters (* and ?) (true) or any valid REGEX expression (false)",
                                                  example = "TRUE|FALSE",
                                                  name = "isSimpleMode",
                                                  type = "boolean")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully grep file details",
                                       description = "Successfully grep file",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Array of found lines",
                                               example = "[\"line_1\",\"line_2\"]",
                                               name = "action_result",
                                               type = "string[]") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while executing grep operation on file details",
                                       description = "Error while executing grep operation on file details",
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

    public Response fileGrep( @Context HttpServletRequest request,
                              @QueryParam(
                                      value = "sessionId") String sessionId,
                              @QueryParam(
                                      value = "resourceId") int resourceId,
                              @QueryParam(
                                      value = "fileName") String fileName,
                              @QueryParam(
                                      value = "searchPattern") String searchPattern,
                              @QueryParam(
                                      value = "isSimpleMode") boolean isSimpleMode ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            if (StringUtils.isNullOrEmpty(fileName)) {
                throw new NoSuchElementException("fileName is not provided with the request");
            }
            if (StringUtils.isNullOrEmpty(searchPattern)) {
                throw new NoSuchElementException("searchPattern is not provided with the request");
            }
            String[] lines = FileSystemManager.fileGrep(sessionId, resourceId, fileName, searchPattern, isSimpleMode);
            return Response.ok("{\"action_result\":" + GSON.toJson(lines, String[].class) + "}").build();
        } catch (Exception e) {
            String message = "Unable to execute grep operation on file using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file/lock")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Lock file details",
            summary = "Lock file",
            url = "file/lock")
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
                                                  description = "The file name",
                                                  example = "/home/atsuser/old_file.txt",
                                                  name = "fileName",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully lock file details",
                                       description = "Successfully lock file",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "file successfully locked",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while locking file details",
                                       description = "Error while locking file",
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
    public Response lockFile( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String fileName = null;
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
            fileName = getJsonElement(jsonObject, "fileName").getAsString();
            if (StringUtils.isNullOrEmpty(fileName)) {
                throw new NoSuchElementException("fileName is not provided with the request");
            }
            FileSystemManager.lockFile(sessionId, resourceId, fileName);
            return Response.ok("{\"status_message\":\"file successfully locked\"}").build();
        } catch (Exception e) {
            String message = "Unable to lock file file using filesystem resource with id '"
                             + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file/unlock")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Unlock file details",
            summary = "Unlock file",
            url = "file/unlock")
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
                                                  description = "The file name",
                                                  example = "/home/atsuser/old_file.txt",
                                                  name = "fileName",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully unlock file details",
                                       description = "Successfully unlock file",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "file successfully unlocked",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while unlocking file details",
                                       description = "Error while unlocking file",
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
    public Response unlockFile( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String fileName = null;
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
            fileName = getJsonElement(jsonObject, "fileName").getAsString();
            if (StringUtils.isNullOrEmpty(fileName)) {
                throw new NoSuchElementException("fileName is not provided with the request");
            }
            FileSystemManager.unlockFile(sessionId, resourceId, fileName);
            return Response.ok("{\"status_message\":\"file successfully unlocked\"}").build();
        } catch (Exception e) {
            String message = "Unable to unlock file file using filesystem resource with id '"
                             + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file/unzip")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Unzip file details",
            summary = "Unzip file",
            url = "file/unzip")
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
                                                  description = "The zip archive name",
                                                  example = "/home/atsuser/file.zip",
                                                  name = "zipFilePath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The output directory name",
                                                  example = "/home/atsuser",
                                                  name = "outputDirPath",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully unzip file details",
                                       description = "Successfully unzip file",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "file successfully unzipped",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while unzipping file details",
                                       description = "Error while unzipping file",
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
    public Response unzipFile( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String outputDirPath = null;
        String zipFilePath = null;
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
            outputDirPath = getJsonElement(jsonObject, "outputDirPath").getAsString();
            if (StringUtils.isNullOrEmpty(outputDirPath)) {
                throw new NoSuchElementException("outputDirPath is not provided with the request");
            }
            zipFilePath = getJsonElement(jsonObject, "zipFilePath").getAsString();
            if (StringUtils.isNullOrEmpty(zipFilePath)) {
                throw new NoSuchElementException("zipFilePath is not provided with the request");
            }
            FileSystemManager.unzipFile(sessionId, resourceId, zipFilePath, outputDirPath);
            return Response.ok("{\"status_message\":\"file successfully unzipped\"}").build();
        } catch (Exception e) {
            String message = "Unable to unzip file file using filesystem resource with id '"
                             + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file/extract")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Extract file details",
            summary = "Extract file",
            url = "file/extract")
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
                                                  description = "The archive file name. It must be .zip, .tar or .tar.gz",
                                                  example = "/home/atsuser/file.tar.gz",
                                                  name = "archiveFilePath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The output directory name",
                                                  example = "/home/atsuser",
                                                  name = "outputDirPath",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully extract file details",
                                       description = "Successfully extract file",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "file successfully extracted",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while extracting file details",
                                       description = "Error while extracting file",
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
    public Response extractFile( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String archiveFilePath = null;
        String outputDirPath = null;
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
            outputDirPath = getJsonElement(jsonObject, "outputDirPath").getAsString();
            if (StringUtils.isNullOrEmpty(outputDirPath)) {
                throw new NoSuchElementException("outputDirPath is not provided with the request");
            }
            archiveFilePath = getJsonElement(jsonObject, "archiveFilePath").getAsString();
            if (StringUtils.isNullOrEmpty(archiveFilePath)) {
                throw new NoSuchElementException("archiveFilePath is not provided with the request");
            }
            FileSystemManager.extractFile(sessionId, resourceId, archiveFilePath, outputDirPath);
            return Response.ok("{\"status_message\":\"file successfully extracted\"}").build();
        } catch (Exception e) {
            String message = "Unable to extract file file using filesystem resource with id '"
                             + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "file/send")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Send file details",
            summary = "Send file",
            url = "file/send")
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
                                                  description = "The source file name",
                                                  example = "/home/atsuser/srcFile.txt",
                                                  name = "fromFileName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The destination file name",
                                                  example = "/home/vsuser/dstFile.txt",
                                                  name = "toFileName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The destination host address where the file will be sent",
                                                  example = "212.89.23.131",
                                                  name = "machineIP",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The port number which will be used for the transfer. This is obtained via call to transferSocket",
                                                  example = "25300",
                                                  name = "port",
                                                  type = "integer"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Whether to throw an error if the file size changed during copying",
                                                  example = "TRUE|FALSE",
                                                  name = "failOnError",
                                                  type = "boolean")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully send file details",
                                       description = "Successfully send file",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "file successfully sent",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while sending file details",
                                       description = "Error while sending file",
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
    public Response sendFileTo( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String fromFileName = null;
        String toFileName = null;
        String machineIP = null;
        int port = -1;
        boolean failOnError = true;
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
            fromFileName = getJsonElement(jsonObject, "fromFileName").getAsString();
            if (StringUtils.isNullOrEmpty(fromFileName)) {
                throw new NoSuchElementException("fromFileName is not provided with the request");
            }
            toFileName = getJsonElement(jsonObject, "toFileName").getAsString();
            if (StringUtils.isNullOrEmpty(toFileName)) {
                throw new NoSuchElementException("toFileName is not provided with the request");
            }
            machineIP = getJsonElement(jsonObject, "machineIP").getAsString();
            if (StringUtils.isNullOrEmpty(machineIP)) {
                throw new NoSuchElementException("machineIP is not provided with the request");
            }
            port = getJsonElement(jsonObject, "port").getAsInt();
            if (port < 0) {
                throw new IllegalArgumentException("port has invallid value '" + resourceId + "'");
            }
            failOnError = getJsonElement(jsonObject, "failOnError").getAsBoolean();
            FileSystemManager.sendFileTo(sessionId, resourceId, fromFileName, toFileName, machineIP, port, failOnError);
            return Response.ok("{\"status_message\":\"file successfully sent\"}").build();
        } catch (Exception e) {
            String message = "Unable to send file file using filesystem resource with id '"
                             + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "/file/copy")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "put",
            parametersDefinition = "Copy file locally on the agent details",
            summary = "Copy file locally on the agent. If you want to copy the file from one machine to another make call to /file/send",
            url = "/file/copy")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The source file name",
                                                  example = "/home/atsuser/srcFile.txt",
                                                  name = "fromFileName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The destination file name",
                                                  example = "/home/atsuser/dstFile.txt",
                                                  name = "toFileName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Whether to fail if file size changed during copying",
                                                  example = "TRUE|FALSE",
                                                  name = "failOnError",
                                                  type = "boolean") })

    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully copy file locally details",
                                       description = "Successfully copy file locally",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfully copy file locally",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while copying file locally details",
                                       description = "Error while copying file locally",
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
    public Response copyFileLocally( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String fromFileName = null;
        String toFileName = null;
        boolean failOnError = false;
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
            fromFileName = getJsonElement(jsonObject, "fromFileName").getAsString();
            if (StringUtils.isNullOrEmpty(fromFileName)) {
                throw new NoSuchElementException("fromFileName is not provided with the request");
            }
            toFileName = getJsonElement(jsonObject, "toFileName").getAsString();
            if (StringUtils.isNullOrEmpty(toFileName)) {
                throw new NoSuchElementException("toFileName is not provided with the request");
            }
            failOnError = getJsonElement(jsonObject, "failOnError").getAsBoolean();
            FileSystemManager.copyFileLocally(sessionId, resourceId, fromFileName, toFileName, failOnError);
            return Response.ok("{\"status_message\":\"successfully copy file locally\"}").build();
        } catch (Exception e) {
            String message = "Unable to copy file locally using filesystem resource with id '"
                             + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "constructPath")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Contruct destination file path details",
            summary = "Since the agent can be on both WIN and Linux and the request can come from WIN or Linux, "
                      + "this method constructs the destination file path",
            url = "constructPath")
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
                                                  description = "The file name",
                                                  example = "file.txt",
                                                  name = "srcFileName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The destination file path",
                                                  example = "/home/atsuser",
                                                  name = "dstFilePath",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully constructed destination file path details",
                                       description = "Successfully constructed destination file path",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The destination file path",
                                               example = "/home/atsuser/file.txt",
                                               name = "action_result",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while constructing destination file path details",
                                       description = "Error while constructing destination file path",
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
    public Response constructFilePath( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String srcFileName = null;
        String dstFilePath = null;
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
            srcFileName = getJsonElement(jsonObject, "srcFileName").getAsString();
            if (StringUtils.isNullOrEmpty(srcFileName)) {
                throw new NoSuchElementException("srcFileName is not provided with the request");
            }
            dstFilePath = getJsonElement(jsonObject, "dstFilePath").getAsString();
            if (StringUtils.isNullOrEmpty(dstFilePath)) {
                throw new NoSuchElementException("dstFilePath is not provided with the request");
            }
            String dstFileName = FileSystemManager.constructDestinationFilePath(sessionId, resourceId, srcFileName,
                                                                                dstFilePath);
            return Response.ok("{\"action_result\":" + GSON.toJson(dstFileName, String.class) + "}").build();
        } catch (Exception e) {
            String message = "Unable to construct destination file path on file using filesystem resource with id '"
                             + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "copyPortRange")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Set port range for copy operations details",
            summary = "Set port range for copy operations.You can set either both ports (start and end) or just one of them",
            url = "copyPortRange")
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
                                                  description = "The start port number.",
                                                  example = "23500",
                                                  name = "copyFileStartPort",
                                                  type = "integer"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The end port number",
                                                  example = "23510",
                                                  name = "copyFileEndPort",
                                                  type = "integer")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully set copy port range details",
                                       description = "Successfully set copy port range",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfully set copy port range",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while setting copy port range details",
                                       description = "Error while setting copy port range",
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
    public Response setCopyPortRange( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        int copyFileStartPort = -1;
        int copyFileEndPort = -1;
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
            copyFileStartPort = getJsonElement(jsonObject, "copyFileStartPort").getAsInt();
            if (copyFileStartPort < 0) {
                throw new IllegalArgumentException("copyFileStartPort has invallid value '" + resourceId + "'");
            }
            copyFileEndPort = getJsonElement(jsonObject, "copyFileEndPort").getAsInt();
            if (copyFileEndPort < 0) {
                throw new IllegalArgumentException("copyFileEndPort has invallid value '" + resourceId + "'");
            }
            if (copyFileEndPort < copyFileStartPort) {
                throw new IllegalArgumentException("copyFileEndPort must be greather than the copyFileStartPort");
            }
            FileSystemManager.setCopyPortRange(sessionId, resourceId, copyFileStartPort, copyFileEndPort);
            return Response.ok("{\"status_message\":\"successfully set copy port range\"}").build();
        } catch (Exception e) {
            String message = "Unable to set copy port range using filesystem resource with id '"
                             + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "transferSocket")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Open and get socket for transfer operations details",
            url = "transferSocket")
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
                                       definition = "Successfully open and get socket for transfer operations details",
                                       description = "Successfully open and get socket for transfer operations",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfully set copy port range",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while setting copy port range details",
                                       description = "Error while setting copy port range",
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
    public Response openTransferSocket( @Context HttpServletRequest request,
                                        @QueryParam(
                                                value = "sessionId") String sessionId,
                                        @QueryParam(
                                                value = "resourceId") int resourceId ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            int transferSocket = FileSystemManager.openTransferSocket(sessionId, resourceId);
            return Response.ok("{\"action_result\":" + GSON.toJson(transferSocket, int.class) + "}").build();
        } catch (Exception e) {
            String message = "Unable to open trasfer socket using filesystem resource with id '"
                             + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "waitTransferComepletion")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Wait for transfer to complete details",
            summary = "Wait for transfer to complete",
            url = "waitForFileTransferCompletion")
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
                                                  description = "The port for the transfer",
                                                  example = "25300",
                                                  name = "port",
                                                  type = "integer")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully wait for transfer to complete details",
                                       description = "Successfully wait for transfer to complete",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfully set copy port range",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while waiting for transfer to complete details",
                                       description = "Error while waiting for transfer to complete",
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
    public Response waitForFileTransferCompletion( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        int port = -1;
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
            port = getJsonElement(jsonObject, "port").getAsInt();
            if (port < 0) {
                throw new IllegalArgumentException("port has invallid value '" + resourceId + "'");
            }
            FileSystemManager.waitForTransferToComplete(sessionId, resourceId, port);
            return Response.ok("{\"status_message\":\"successfully wait for transfer to complete\"}").build();
        } catch (Exception e) {
            String message = "Unable to wait for transfer to complete using filesystem resource with id '"
                             + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "findFiles")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Find files details",
            summary = "Find files",
            url = "findFiles")
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
                                                  description = "The file search directory",
                                                  example = "/home/atsuser",
                                                  name = "location",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The search string",
                                                  example = "log_file",
                                                  name = "searchString",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Whether the search string is regex",
                                                  example = "TRUE|FALSE",
                                                  name = "isRegex",
                                                  type = "boolean"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Whether matching directories will be included along with the matching files",
                                                  example = "TRUE|FALSE",
                                                  name = "acceptDirectories",
                                                  type = "boolean"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Whether the search will traverse subdirectories as well",
                                                  example = "TRUE|FALSE",
                                                  name = "recursiveSearch",
                                                  type = "boolean")
    })
    @SwaggerMethodResponses( { @SwaggerMethodResponse(
            code = 200,
            definition = "Successfully find files details",
            description = "Successfully find files",
            parametersDefinitions = { @SwaggerMethodParameterDefinition(
                    description = "Array of the found files and directories names",
                    example = "[\"/home/atsuser/log_file1.txt\"]",
                    name = "action_result",
                    type = "string[]") }), @SwaggerMethodResponse(
                            code = 500,
                            definition = "Error while executing operation findFiles details",
                            description = "Error while executing operation findFiles",
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
    public Response findFiles( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String location = null;
        String searchString = null;
        boolean isRegex;
        boolean acceptDirectories;
        boolean recursiveSearch;
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
            location = getJsonElement(jsonObject, "location").getAsString();
            if (StringUtils.isNullOrEmpty(location)) {
                throw new NoSuchElementException("location is not provided with the request");
            }
            searchString = getJsonElement(jsonObject, "searchString").getAsString();
            if (StringUtils.isNullOrEmpty(searchString)) {
                throw new NoSuchElementException("searchString is not provided with the request");
            }
            isRegex = getJsonElement(jsonObject, "isRegex").getAsBoolean();
            acceptDirectories = getJsonElement(jsonObject, "acceptDirectories").getAsBoolean();
            recursiveSearch = getJsonElement(jsonObject, "recursiveSearch").getAsBoolean();
            String[] files = FileSystemManager.findFiles(sessionId, resourceId, location, searchString, isRegex,
                                                         acceptDirectories, recursiveSearch);
            return Response.ok("{\"action_result\":" + GSON.toJson(files, String[].class) + "}").build();
        } catch (Exception e) {
            String message = "Unable to find files using filesystem resource with id '"
                             + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "directory/exist")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Check whether directory exists",
            url = "directory/exist")
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
                                                  description = "The directory name",
                                                  example = "/home/atsuser/",
                                                  name = "dirName",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully check does directory exists details",
                                       description = "Successfully check does directory exists",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Whether directory exists",
                                               example = "TRUE|FALSE",
                                               name = "action_result",
                                               type = "boolean") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while checking does directory exists details",
                                       description = "Error while checking does directory exists",
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
    public Response doesDirectoryExists( @Context HttpServletRequest request,
                                         @QueryParam(
                                                 value = "sessionId") String sessionId,
                                         @QueryParam(
                                                 value = "resourceId") int resourceId,
                                         @QueryParam(
                                                 value = "dirName") String dirName ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            sessionId = URLDecoder.decode(sessionId, "UTF-8");
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            dirName = URLDecoder.decode(dirName, "UTF-8");
            if (StringUtils.isNullOrEmpty(dirName)) {
                throw new NoSuchElementException("dirName is not provided with the request");
            }
            boolean exists = FileSystemManager.doesDirectoryExists(sessionId, resourceId, dirName);
            return Response.ok("{\"action_result\":" + GSON.toJson(exists) + "}").build();
        } catch (Exception e) {
            String message = "Unable to check whether directory exists using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "directory")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "put",
            parametersDefinition = "Create directory details",
            summary = "Create directory",
            url = "directory")
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
                                                  description = "The directory name",
                                                  example = "/home/atsuser/file.txt",
                                                  name = "dirName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The directory user ID ( UID )",
                                                  example = "1000",
                                                  name = "uid",
                                                  type = "long"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The directory group ID ( GID )",
                                                  example = "1000",
                                                  name = "gid",
                                                  type = "long")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully create directory details",
                                       description = "Successfully create directory",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "directory successfully created",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while creating directory details",
                                       description = "Error while creating directory",
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
    public Response createDirectory( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String directoryName = null;
        long uid = -1;
        long gid = -1;
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
            directoryName = getJsonElement(jsonObject, "directoryName").getAsString();
            if (StringUtils.isNullOrEmpty(directoryName)) {
                throw new NoSuchElementException("directoryName is not provided with the request");
            }
            uid = getJsonElement(jsonObject, "uid").getAsLong();
            gid = getJsonElement(jsonObject, "gid").getAsLong();
            FileSystemManager.createDirectory(sessionId, resourceId, directoryName, uid, gid);
            return Response.ok("{\"status_message\":\"directory successfully create\"}").build();
        } catch (Exception e) {
            String message = "Unable to create directory using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "directory")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "delete",
            parametersDefinition = "",
            summary = "Delete directory",
            url = "directory")
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
                                                  description = "The directory name",
                                                  example = "/home/atsuser/file.txt",
                                                  name = "dirName",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully deleted directory details",
                                       description = "Successfully deleted directory",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "directory successfully deleted",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while deleting directory details",
                                       description = "Error while deleting directory",
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
    public Response deleteDirectory( @Context HttpServletRequest request,
                                     @QueryParam(
                                             value = "sessionId") String sessionId,
                                     @QueryParam(
                                             value = "resourceId") int resourceId,
                                     @QueryParam(
                                             value = "directoryName") String directoryName,
                                     @QueryParam(
                                             value = "deleteRecursively") boolean deleteRecursively ) {

        try {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            sessionId = URLDecoder.decode(sessionId, "UTF-8");
            ThreadsPerCaller.registerThread(sessionId);
            if (resourceId < 0) {
                throw new IllegalArgumentException("resourceId has invallid value '" + resourceId + "'");
            }
            if (StringUtils.isNullOrEmpty(directoryName)) {
                throw new NoSuchElementException("directoryName is not provided with the request");
            }
            directoryName = URLDecoder.decode(directoryName, "UTF-8");
            FileSystemManager.deleteDirectory(sessionId, resourceId, directoryName, deleteRecursively);
            return Response.ok("{\"status_message\":\"directory successfully deleted\"}").build();
        } catch (Exception e) {
            String message = "Unable to delete directory using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "directory/purgeContent")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Purge directory content details",
            summary = "Delete only the directory child files and directories",
            url = "directory/purgeContent")
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
                                                  description = "The directory name",
                                                  example = "/home/atsuser",
                                                  name = "directoryName",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully purge directory content details",
                                       description = "Successfully purge directory content",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "directory's content successfully purged",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while purging directory content details",
                                       description = "Error while purging directory content",
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
    public Response purgeDirectoryContent( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String directoryName = null;
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
            directoryName = getJsonElement(jsonObject, "directoryName").getAsString();
            if (StringUtils.isNullOrEmpty(directoryName)) {
                throw new NoSuchElementException("directoryName is not provided with the request");
            }
            FileSystemManager.purgeDirectoryContent(sessionId, resourceId, directoryName);
            return Response.ok("{\"status_message\":" + "\"directory content successfully purged\"}").build();
        } catch (Exception e) {
            String message = "Unable to purge directory content using filesystem resource with id '" + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "directory/send")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Send directory details",
            summary = "Send directory",
            url = "directory/send")
    @SwaggerMethodParameterDefinitions( { @SwaggerMethodParameterDefinition(
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
                                                  description = "The source directory name",
                                                  example = "/home/atsuser/srcDir",
                                                  name = "fromDirName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The destination directory name",
                                                  example = "/home/vsuser/dstDir",
                                                  name = "toDirName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The destination host address where the directory will be sent",
                                                  example = "212.89.23.131",
                                                  name = "machineIP",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The port number which will be used for the transfer. This is obtained via call to transferSocket",
                                                  example = "25300",
                                                  name = "port",
                                                  type = "integer"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Whether to throw an error if the directory size changed during copying",
                                                  example = "TRUE|FALSE",
                                                  name = "failOnError",
                                                  type = "boolean"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Whether to copy recursively or not",
                                                  example = "TRUE|FALSE",
                                                  name = "isRecursive",
                                                  type = "boolean") })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully send directory details",
                                       description = "Successfully send directory",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "directory successfully sent",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while sending directory details",
                                       description = "Error while sending directory",
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
    public Response sendDirectoryTo( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String fromDirName = null;
        String toDirName = null;
        String machineIP = null;
        int port = -1;
        boolean isRecursive = false;
        boolean failOnError = true;
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
            fromDirName = getJsonElement(jsonObject, "fromDirName").getAsString();
            if (StringUtils.isNullOrEmpty(fromDirName)) {
                throw new NoSuchElementException("fromDirName is not provided with the request");
            }
            toDirName = getJsonElement(jsonObject, "toDirName").getAsString();
            if (StringUtils.isNullOrEmpty(toDirName)) {
                throw new NoSuchElementException("toDirName is not provided with the request");
            }
            machineIP = getJsonElement(jsonObject, "machineIP").getAsString();
            if (StringUtils.isNullOrEmpty(machineIP)) {
                throw new NoSuchElementException("machineIP is not provided with the request");
            }
            port = getJsonElement(jsonObject, "port").getAsInt();
            if (port < 0) {
                throw new IllegalArgumentException("port has invallid value '" + resourceId + "'");
            }
            isRecursive = getJsonElement(jsonObject, "isRecursive").getAsBoolean();
            failOnError = getJsonElement(jsonObject, "failOnError").getAsBoolean();
            FileSystemManager.sendDirectoryTo(sessionId, resourceId, fromDirName, toDirName, machineIP, port,
                                              isRecursive, failOnError);
            return Response.ok("{\"status_message\":\"directory successfully sent\"}").build();
        } catch (Exception e) {
            String message = "Unable to send directory using filesystem resource with id '"
                             + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
    @Path( "/directory/copy")
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "put",
            parametersDefinition = "Copy directory locally on the agent details",
            summary = "Copy directory locally on the agent. If you want to copy the directory from one machine to another make call to /file/send",
            url = "/directory/copy")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The source directory name",
                                                  example = "/home/atsuser/srcDir",
                                                  name = "fromDirName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The destination directory name",
                                                  example = "/home/atsuser/dstDir",
                                                  name = "toDirName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Whether to fail if directory size changed during copying",
                                                  example = "TRUE|FALSE",
                                                  name = "failOnError",
                                                  type = "boolean"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Whether to copy directory recursively or not",
                                                  example = "TRUE|FALSE",
                                                  name = "isRecursive",
                                                  type = "boolean") })

    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully copy directory locally details",
                                       description = "Successfully copy directory locally",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "successfully copy directory locally",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while copying directory locally details",
                                       description = "Error while copying directory locally",
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
    public Response copyDirectoryLocally( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String fromDirName = null;
        String toDirName = null;
        boolean failOnError = false;
        boolean isRecursive = false;
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
            fromDirName = getJsonElement(jsonObject, "fromDirName").getAsString();
            if (StringUtils.isNullOrEmpty(fromDirName)) {
                throw new NoSuchElementException("fromDirName is not provided with the request");
            }
            toDirName = getJsonElement(jsonObject, "toDirName").getAsString();
            if (StringUtils.isNullOrEmpty(toDirName)) {
                throw new NoSuchElementException("toDirName is not provided with the request");
            }
            failOnError = getJsonElement(jsonObject, "failOnError").getAsBoolean();
            FileSystemManager.copyDirectoryLocally(sessionId, resourceId, fromDirName, toDirName, failOnError, isRecursive);
            return Response.ok("{\"status_message\":\"successfully copy directory locally\"}").build();
        } catch (Exception e) {
            String message = "Unable to copy directory locally using filesystem resource with id '"
                             + resourceId
                             + "' from session with id '"
                             + sessionId + "'";
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
