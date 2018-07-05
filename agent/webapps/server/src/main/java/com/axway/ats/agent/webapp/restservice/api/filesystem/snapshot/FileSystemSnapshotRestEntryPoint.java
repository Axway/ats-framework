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
package com.axway.ats.agent.webapp.restservice.api.filesystem.snapshot;

import java.io.InputStreamReader;
import java.util.NoSuchElementException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
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
import com.axway.ats.core.filesystem.snapshot.LocalFileSystemSnapshot;
import com.axway.ats.core.filesystem.snapshot.SnapshotConfiguration;
import com.axway.ats.core.threads.ThreadsPerCaller;
import com.axway.ats.core.utils.StringUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@SwaggerClass( "filesystem/snapshot")
@Path( "filesystem/snapshot")
public class FileSystemSnapshotRestEntryPoint {

    private static final Logger LOG  = Logger.getLogger(FileSystemSnapshotRestEntryPoint.class);
    private static final Gson   GSON = new Gson();

    @PUT
    @Path( "/")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "put",
            parametersDefinition = "Initialize file system snapshot details",
            summary = "Initialize file snapshot system",
            url = "/")
    @SwaggerMethodParameterDefinitions( {
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The session ID",
                                                  example = "some session ID",
                                                  name = "sessionId",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The snapshot name",
                                                  example = "some name",
                                                  name = "name",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The snapshot configuration",
                                                  example = "{" + 
                                                          "    \"checkModificationTime\": true," + 
                                                          "    \"checkSize\": true," + 
                                                          "    \"checkMD5\": true," + 
                                                          "    \"checkPermissions\": true," + 
                                                          "    \"supportHidden\": true," + 
                                                          "    \"checkPropertiesFilesContent\": true," + 
                                                          "    \"propertiesFileExtensions\": [\".properties\"]," + 
                                                          "    \"checkXmlFilesContent\": true," + 
                                                          "    \"xmlFileExtensions\": [\".xml\"]," + 
                                                          "    \"checkIniFilesContent\": true," + 
                                                          "    \"iniFilesStartComment\": \"#\"," + 
                                                          "    \"iniFilesStartSection\": \"[\"," + 
                                                          "    \"iniFilesDelimiter\": \"=\"," + 
                                                          "    \"iniFileExtensions\": [\".ini\"]," + 
                                                          "    \"checkTextFilesContent\": true," + 
                                                          "    \"textFileExtensions\": [\".txt\"]" + 
                                                          "}",
                                                  name = "configuration",
                                                  type = "object")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully initialize file system snapshot resource details",
                                       description = "Successfully initialize file system snapshot resource",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The resource ID of the newly initialized resource",
                                               example = "123",
                                               name = "resourceId",
                                               type = "integer") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while initializing file system snapshot resource details",
                                       description = "Error while initializing file system snapshot resource",
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
    public Response initializeFileSystem( @Context HttpServletRequest request ) {

        String sessionId = null;
        String name = null;
        SnapshotConfiguration configuration = null;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(request.getInputStream(),
                                                                                 "UTF-8"))
                                                    .getAsJsonObject();
            sessionId = getJsonElement(jsonObject, "sessionId").getAsString();
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NoSuchElementException("sessionId is not provided with the request");
            }
            ThreadsPerCaller.registerThread(sessionId);
            name = getJsonElement(jsonObject, "name").getAsString();
            if (StringUtils.isNullOrEmpty(name)) {
                throw new NoSuchElementException("name is not provided with the request");
            }
            String snapshotConfigurationJSON = getJsonElement(jsonObject, "configuration").toString();
            if (StringUtils.isNullOrEmpty(snapshotConfigurationJSON)) {
                throw new NoSuchElementException("configuration is not provided with the request");
            }
            configuration = GSON.fromJson(snapshotConfigurationJSON, SnapshotConfiguration.class);
            int resourceId = FileSystemSnapshotManager.initFileSystemSnapshot(sessionId, name, configuration);
            return Response.ok("{\"resourceId\":" + resourceId + "}").build();
        } catch (Exception e) {
            String message = "Unable to initialize file system snapshot resource from session with id '" + sessionId
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
    @Path( "directory/add")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "put",
            parametersDefinition = "Add directory to file system snapshot details",
            summary = "Add directory to file system snapshot",
            url = "directory/add")
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
                                                  description = "The directory alias",
                                                  example = "root_dir",
                                                  name = "directoryAlias",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The directory path",
                                                  example = "/home/atsuser/",
                                                  name = "directoryPath",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully add directory to file system snapshot details",
                                       description = "Successfully add directory to file system snapshot",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "directory successfully added to file system snapshot",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while adding directory to file system snapshot details",
                                       description = "Error while adding directory to file system snapshot",
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
    public Response addDirectory( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String directoryAlias = null;
        String directoryPath = null;
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
            directoryAlias = getJsonElement(jsonObject, "directoryAlias").getAsString();
            if (StringUtils.isNullOrEmpty(directoryAlias)) {
                throw new NoSuchElementException("directoryAlias is not provided with the request");
            }
            directoryPath = getJsonElement(jsonObject, "directoryPath").getAsString();
            if (StringUtils.isNullOrEmpty(directoryPath)) {
                throw new NoSuchElementException("directoryPath is not provided with the request");
            }
            FileSystemSnapshotManager.addDirectory(sessionId, resourceId, directoryAlias, directoryPath);
            return Response.ok("{\"status_message\":" + "\"directory successfully added to file system snapshot\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to add directory to file system snapshot using resource with id '" + resourceId
                             + "' from session with id '" + sessionId
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
    @Path( "take")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Take file system snapshot details",
            summary = "Take file system snapshot",
            url = "take")
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
                                       definition = "Successfully took file system snapshot details",
                                       description = "Successfully took file system snapshot",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "file system snapshot successfully taken",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while taking file system snapshot details",
                                       description = "Error while taking file system snapshot",
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
    public Response takeSnapshot( @Context HttpServletRequest request ) {

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
            FileSystemSnapshotManager.takeSnapshot(sessionId, resourceId);
            return Response.ok("{\"status_message\":" + "\"file system snapshot successfully taken\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to take file system snapshot using resource with id '" + resourceId
                             + "' from session with id '" + sessionId
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
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "Get file system snapshot details",
            summary = "Get file system snapshot",
            url = "/")
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
                                       definition = "Successfully get file system snapshot details",
                                       description = "Successfully get file system snapshot",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The file system snapshot",
                                               example = "{\n" + 
                                                       "        \"name\": \"fss1\",\n" + 
                                                       "        \"snapshotTimestamp\": 1529911269622,\n" + 
                                                       "        \"dirSnapshots\": {\n" + 
                                                       "            \"dir1\": {\n" + 
                                                       "                \"path\": \"/home/atsuser/snapshots/\",\n" + 
                                                       "                \"subdirSnapshots\": {},\n" + 
                                                       "                \"fileSnapshots\": {\n" + 
                                                       "                    \"file.xml\": {\n" + 
                                                       "                        \"matchers\": [],\n" + 
                                                       "                        \"path\": \"/home/atsuser/snapshots/file.xml\",\n" + 
                                                       "                        \"size\": -1,\n" + 
                                                       "                        \"timeModified\": 1529664801000,\n" + 
                                                       "                        \"permissions\": \"0664\",\n" + 
                                                       "                        \"configuration\": {\n" + 
                                                       "                            \"checkModificationTime\": true,\n" + 
                                                       "                            \"checkSize\": false,\n" + 
                                                       "                            \"checkMD5\": false,\n" + 
                                                       "                            \"checkPermissions\": true,\n" + 
                                                       "                            \"supportHidden\": true,\n" + 
                                                       "                            \"checkPropertiesFilesContent\": true,\n" + 
                                                       "                            \"propertiesFileExtensions\": [\".properties\"],\n" + 
                                                       "                            \"checkXmlFilesContent\": true,\n" + 
                                                       "                            \"xmlFileExtensions\": [\".xml\"],\n" + 
                                                       "                            \"checkIniFilesContent\": true,\n" + 
                                                       "                            \"iniFilesStartComment\": \"#\",\n" + 
                                                       "                            \"iniFilesStartSection\": \"[\",\n" + 
                                                       "                            \"iniFilesDelimiter\": \"\\u003d\",\n" + 
                                                       "                            \"iniFileExtensions\": [\".ini\"],\n" + 
                                                       "                            \"checkTextFilesContent\": true,\n" + 
                                                       "                            \"textFileExtensions\": [\".txt\"]\n" + 
                                                       "                        }\n" + 
                                                       "                    },\n" + 
                                                       "                    \"file.ini\": {\n" + 
                                                       "                        \"matchers\": [],\n" + 
                                                       "                        \"path\": \"/home/atsuser/snapshots/file.ini\",\n" + 
                                                       "                        \"size\": -1,\n" + 
                                                       "                        \"timeModified\": 1529665246000,\n" + 
                                                       "                        \"permissions\": \"0664\",\n" + 
                                                       "                        \"configuration\": {\n" + 
                                                       "                            \"checkModificationTime\": true,\n" + 
                                                       "                            \"checkSize\": false,\n" + 
                                                       "                            \"checkMD5\": false,\n" + 
                                                       "                            \"checkPermissions\": true,\n" + 
                                                       "                            \"supportHidden\": true,\n" + 
                                                       "                            \"checkPropertiesFilesContent\": true,\n" + 
                                                       "                            \"propertiesFileExtensions\": [\".properties\"],\n" + 
                                                       "                            \"checkXmlFilesContent\": true,\n" + 
                                                       "                            \"xmlFileExtensions\": [\".xml\"],\n" + 
                                                       "                            \"checkIniFilesContent\": true,\n" + 
                                                       "                            \"iniFilesStartComment\": \"#\",\n" + 
                                                       "                            \"iniFilesStartSection\": \"[\",\n" + 
                                                       "                            \"iniFilesDelimiter\": \"\\u003d\",\n" + 
                                                       "                            \"iniFileExtensions\": [\".ini\"],\n" + 
                                                       "                            \"checkTextFilesContent\": true,\n" + 
                                                       "                            \"textFileExtensions\": [\".txt\"]\n" + 
                                                       "                        }\n" + 
                                                       "                    },\n" + 
                                                       "                    \"file.property\": {\n" + 
                                                       "                        \"path\": \"/home/atsuser/snapshots/file.property\",\n" + 
                                                       "                        \"size\": 319,\n" + 
                                                       "                        \"timeModified\": 1529657430000,\n" + 
                                                       "                        \"md5\": \"762a27b6dd51a4e09facdd2f32ed3abb\",\n" + 
                                                       "                        \"permissions\": \"0664\",\n" + 
                                                       "                        \"configuration\": {\n" + 
                                                       "                            \"checkModificationTime\": true,\n" + 
                                                       "                            \"checkSize\": true,\n" + 
                                                       "                            \"checkMD5\": true,\n" + 
                                                       "                            \"checkPermissions\": true,\n" + 
                                                       "                            \"supportHidden\": true,\n" + 
                                                       "                            \"checkPropertiesFilesContent\": true,\n" + 
                                                       "                            \"propertiesFileExtensions\": [\".properties\"],\n" + 
                                                       "                            \"checkXmlFilesContent\": true,\n" + 
                                                       "                            \"xmlFileExtensions\": [\".xml\"],\n" + 
                                                       "                            \"checkIniFilesContent\": true,\n" + 
                                                       "                            \"iniFilesStartComment\": \"#\",\n" + 
                                                       "                            \"iniFilesStartSection\": \"[\",\n" + 
                                                       "                            \"iniFilesDelimiter\": \"\\u003d\",\n" + 
                                                       "                            \"iniFileExtensions\": [\".ini\"],\n" + 
                                                       "                            \"checkTextFilesContent\": true,\n" + 
                                                       "                            \"textFileExtensions\": [\".txt\"]\n" + 
                                                       "                        }\n" + 
                                                       "                    }\n" + 
                                                       "                },\n" + 
                                                       "                \"matchersContainer\": {\n" + 
                                                       "                    \"fileAttributesMap\": {},\n" + 
                                                       "                    \"propertyMatchersMap\": {},\n" + 
                                                       "                    \"xmlMatchersMap\": {},\n" + 
                                                       "                    \"iniMatchersMap\": {},\n" + 
                                                       "                    \"textMatchersMap\": {}\n" + 
                                                       "                },\n" + 
                                                       "                \"skippedSubDirs\": []\n" + 
                                                       "            }\n" + 
                                                       "        },\n" + 
                                                       "        \"configuration\": {\n" + 
                                                       "            \"checkModificationTime\": true,\n" + 
                                                       "            \"checkSize\": true,\n" + 
                                                       "            \"checkMD5\": true,\n" + 
                                                       "            \"checkPermissions\": true,\n" + 
                                                       "            \"supportHidden\": true,\n" + 
                                                       "            \"checkPropertiesFilesContent\": true,\n" + 
                                                       "            \"propertiesFileExtensions\": [\".properties\"],\n" + 
                                                       "            \"checkXmlFilesContent\": true,\n" + 
                                                       "            \"xmlFileExtensions\": [\".xml\"],\n" + 
                                                       "            \"checkIniFilesContent\": true,\n" + 
                                                       "            \"iniFilesStartComment\": \"#\",\n" + 
                                                       "            \"iniFilesStartSection\": \"[\",\n" + 
                                                       "            \"iniFilesDelimiter\": \"\\u003d\",\n" + 
                                                       "            \"iniFileExtensions\": [\".ini\"],\n" + 
                                                       "            \"checkTextFilesContent\": true,\n" + 
                                                       "            \"textFileExtensions\": [\".txt\"]\n" + 
                                                       "        }\n" + 
                                                       "}\n" + 
                                                       "",
                                               name = "action_result",
                                               type = "object") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting file system snapshot details",
                                       description = "Error while getting file system snapshot",
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
    public Response getFileSystemSnapshot( @Context HttpServletRequest request,
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
            LocalFileSystemSnapshot snapshot = FileSystemSnapshotManager.getFileSystemSnapshot(sessionId, resourceId);
            return Response.ok("{\"action_result\":" + GSON.toJson(snapshot) + "}").build();
        } catch (Exception e) {
            String message = "Unable to get file system snapshot using resource with id '" + resourceId
                             + "' from session with id '" + sessionId
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
    @Path( "toFile")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Save file system snapshot to file details",
            summary = "Save file system snapshot to file",
            url = "toFile")
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
                                                  description = "The file where the snapshot will be saved in",
                                                  example = "/home/atsuser/fss_1.xml",
                                                  name = "backupFile",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully save file system snapshot to file details",
                                       description = "Successfully save file system snapshot to file",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "file system snapshot successfully saved to file",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while saving file system snapshot to file details",
                                       description = "Error while saving file system snapshot to file",
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
    public Response toFile( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String backupFile = null;
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
            backupFile = getJsonElement(jsonObject, "backupFile").getAsString();
            if (StringUtils.isNullOrEmpty(backupFile)) {
                throw new NoSuchElementException("backupFile is not provided with the request");
            }
            FileSystemSnapshotManager.toFile(sessionId, resourceId, backupFile);
            return Response.ok("{\"status_message\":" + "\"file system snapshot successfully saved to file\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to save file system snapshot to file using resource with id '" + resourceId
                             + "' from session with id '" + sessionId
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
    @Path( "loadFromFile")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Load file system snapshot from file details",
            summary = "Load file system snapshot from file",
            url = "loadFromFile")
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
                                                  description = "The file from which the snapshot will be load",
                                                  example = "/home/atsuser/fss_1.xml",
                                                  name = "sourceFile",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully load file system snapshot from file details",
                                       description = "Successfully load file system snapshot from file",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "file system snapshot successfully loaded from file",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while loading file system snapshot from file details",
                                       description = "Error while loading file system snapshot from file",
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
    public Response loadFromFile( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String sourceFile = null;
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
            sourceFile = getJsonElement(jsonObject, "sourceFile").getAsString();
            if (StringUtils.isNullOrEmpty(sourceFile)) {
                throw new NoSuchElementException("sourceFile is not provided with the request");
            }
            FileSystemSnapshotManager.loadFromFile(sessionId, resourceId, sourceFile);
            return Response.ok("{\"status_message\":" + "\"file system snapshot successfully loaded from file\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to load file system snapshot from file using resource with id '" + resourceId
                             + "' from session with id '" + sessionId
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
    @Path( "/")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Replace the file system snapshot details",
            summary = "Replace the file system snapshot",
            url = "/")
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
                                                  description = "The new snapshot object that will replace the existing one",
                                                  example = "{\n" + 
                                                          "        \"name\": \"fss1\",\n" + 
                                                          "        \"snapshotTimestamp\": 1529911269622,\n" + 
                                                          "        \"dirSnapshots\": {\n" + 
                                                          "            \"dir1\": {\n" + 
                                                          "                \"path\": \"/home/atsuser/snapshots/\",\n" + 
                                                          "                \"subdirSnapshots\": {},\n" + 
                                                          "                \"fileSnapshots\": {\n" + 
                                                          "                    \"file.xml\": {\n" + 
                                                          "                        \"matchers\": [],\n" + 
                                                          "                        \"path\": \"/home/atsuser/snapshots/file.xml\",\n" + 
                                                          "                        \"size\": -1,\n" + 
                                                          "                        \"timeModified\": 1529664801000,\n" + 
                                                          "                        \"permissions\": \"0664\",\n" + 
                                                          "                        \"configuration\": {\n" + 
                                                          "                            \"checkModificationTime\": true,\n" + 
                                                          "                            \"checkSize\": false,\n" + 
                                                          "                            \"checkMD5\": false,\n" + 
                                                          "                            \"checkPermissions\": true,\n" + 
                                                          "                            \"supportHidden\": true,\n" + 
                                                          "                            \"checkPropertiesFilesContent\": true,\n" + 
                                                          "                            \"propertiesFileExtensions\": [\".properties\"],\n" + 
                                                          "                            \"checkXmlFilesContent\": true,\n" + 
                                                          "                            \"xmlFileExtensions\": [\".xml\"],\n" + 
                                                          "                            \"checkIniFilesContent\": true,\n" + 
                                                          "                            \"iniFilesStartComment\": \"#\",\n" + 
                                                          "                            \"iniFilesStartSection\": \"[\",\n" + 
                                                          "                            \"iniFilesDelimiter\": \"\\u003d\",\n" + 
                                                          "                            \"iniFileExtensions\": [\".ini\"],\n" + 
                                                          "                            \"checkTextFilesContent\": true,\n" + 
                                                          "                            \"textFileExtensions\": [\".txt\"]\n" + 
                                                          "                        }\n" + 
                                                          "                    },\n" + 
                                                          "                    \"file.ini\": {\n" + 
                                                          "                        \"matchers\": [],\n" + 
                                                          "                        \"path\": \"/home/atsuser/snapshots/file.ini\",\n" + 
                                                          "                        \"size\": -1,\n" + 
                                                          "                        \"timeModified\": 1529665246000,\n" + 
                                                          "                        \"permissions\": \"0664\",\n" + 
                                                          "                        \"configuration\": {\n" + 
                                                          "                            \"checkModificationTime\": true,\n" + 
                                                          "                            \"checkSize\": false,\n" + 
                                                          "                            \"checkMD5\": false,\n" + 
                                                          "                            \"checkPermissions\": true,\n" + 
                                                          "                            \"supportHidden\": true,\n" + 
                                                          "                            \"checkPropertiesFilesContent\": true,\n" + 
                                                          "                            \"propertiesFileExtensions\": [\".properties\"],\n" + 
                                                          "                            \"checkXmlFilesContent\": true,\n" + 
                                                          "                            \"xmlFileExtensions\": [\".xml\"],\n" + 
                                                          "                            \"checkIniFilesContent\": true,\n" + 
                                                          "                            \"iniFilesStartComment\": \"#\",\n" + 
                                                          "                            \"iniFilesStartSection\": \"[\",\n" + 
                                                          "                            \"iniFilesDelimiter\": \"\\u003d\",\n" + 
                                                          "                            \"iniFileExtensions\": [\".ini\"],\n" + 
                                                          "                            \"checkTextFilesContent\": true,\n" + 
                                                          "                            \"textFileExtensions\": [\".txt\"]\n" + 
                                                          "                        }\n" + 
                                                          "                    },\n" + 
                                                          "                    \"file.property\": {\n" + 
                                                          "                        \"path\": \"/home/atsuser/snapshots/file.property\",\n" + 
                                                          "                        \"size\": 319,\n" + 
                                                          "                        \"timeModified\": 1529657430000,\n" + 
                                                          "                        \"md5\": \"762a27b6dd51a4e09facdd2f32ed3abb\",\n" + 
                                                          "                        \"permissions\": \"0664\",\n" + 
                                                          "                        \"configuration\": {\n" + 
                                                          "                            \"checkModificationTime\": true,\n" + 
                                                          "                            \"checkSize\": true,\n" + 
                                                          "                            \"checkMD5\": true,\n" + 
                                                          "                            \"checkPermissions\": true,\n" + 
                                                          "                            \"supportHidden\": true,\n" + 
                                                          "                            \"checkPropertiesFilesContent\": true,\n" + 
                                                          "                            \"propertiesFileExtensions\": [\".properties\"],\n" + 
                                                          "                            \"checkXmlFilesContent\": true,\n" + 
                                                          "                            \"xmlFileExtensions\": [\".xml\"],\n" + 
                                                          "                            \"checkIniFilesContent\": true,\n" + 
                                                          "                            \"iniFilesStartComment\": \"#\",\n" + 
                                                          "                            \"iniFilesStartSection\": \"[\",\n" + 
                                                          "                            \"iniFilesDelimiter\": \"\\u003d\",\n" + 
                                                          "                            \"iniFileExtensions\": [\".ini\"],\n" + 
                                                          "                            \"checkTextFilesContent\": true,\n" + 
                                                          "                            \"textFileExtensions\": [\".txt\"]\n" + 
                                                          "                        }\n" + 
                                                          "                    }\n" + 
                                                          "                },\n" + 
                                                          "                \"matchersContainer\": {\n" + 
                                                          "                    \"fileAttributesMap\": {},\n" + 
                                                          "                    \"propertyMatchersMap\": {},\n" + 
                                                          "                    \"xmlMatchersMap\": {},\n" + 
                                                          "                    \"iniMatchersMap\": {},\n" + 
                                                          "                    \"textMatchersMap\": {}\n" + 
                                                          "                },\n" + 
                                                          "                \"skippedSubDirs\": []\n" + 
                                                          "            }\n" + 
                                                          "        },\n" + 
                                                          "        \"configuration\": {\n" + 
                                                          "            \"checkModificationTime\": true,\n" + 
                                                          "            \"checkSize\": true,\n" + 
                                                          "            \"checkMD5\": true,\n" + 
                                                          "            \"checkPermissions\": true,\n" + 
                                                          "            \"supportHidden\": true,\n" + 
                                                          "            \"checkPropertiesFilesContent\": true,\n" + 
                                                          "            \"propertiesFileExtensions\": [\".properties\"],\n" + 
                                                          "            \"checkXmlFilesContent\": true,\n" + 
                                                          "            \"xmlFileExtensions\": [\".xml\"],\n" + 
                                                          "            \"checkIniFilesContent\": true,\n" + 
                                                          "            \"iniFilesStartComment\": \"#\",\n" + 
                                                          "            \"iniFilesStartSection\": \"[\",\n" + 
                                                          "            \"iniFilesDelimiter\": \"\\u003d\",\n" + 
                                                          "            \"iniFileExtensions\": [\".ini\"],\n" + 
                                                          "            \"checkTextFilesContent\": true,\n" + 
                                                          "            \"textFileExtensions\": [\".txt\"]\n" + 
                                                          "        }\n" + 
                                                          "}\n" + 
                                                          "",
                                                  name = "newSnapshot",
                                                  type = "object")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully push new file system snapshot details",
                                       description = "Successfully push new file system snapshot",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "new file system snapshot successfully pushed",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while pushing new file system snapshot details",
                                       description = "Error while pushing new file system snapshot",
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
    public Response pushFileSystemSnapshot( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        LocalFileSystemSnapshot newSnapshot = null;
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
            String newSnapshotJSON = getJsonElement(jsonObject, "newSnapshot").toString();
            if (StringUtils.isNullOrEmpty(newSnapshotJSON)) {
                throw new NoSuchElementException("newSnapshot is not provided with the request");
            }
            newSnapshot = GSON.fromJson(newSnapshotJSON, LocalFileSystemSnapshot.class);
            FileSystemSnapshotManager.pushFileSystemSnapshot(sessionId, resourceId, newSnapshot);
            return Response.ok("{\"status_message\":" + "\"new file system snapshot successfully pushed\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to push new file system snapshot using resource with id '" + resourceId
                             + "' from session with id '" + sessionId
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
    @Path( "new")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "put",
            parametersDefinition = "Create new file system snapshot from existing one details",
            summary = "Create new file system snapshot from existing one",
            url = "new")
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
                                                  description = "The new snapshot name",
                                                  example = "new_fss",
                                                  name = "newSnapshotName",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The source snapshot object that will be used to create new one",
                                                  example = "{\n" + 
                                                          "        \"name\": \"fss1\",\n" + 
                                                          "        \"snapshotTimestamp\": 1529911269622,\n" + 
                                                          "        \"dirSnapshots\": {\n" + 
                                                          "            \"dir1\": {\n" + 
                                                          "                \"path\": \"/home/atsuser/snapshots/\",\n" + 
                                                          "                \"subdirSnapshots\": {},\n" + 
                                                          "                \"fileSnapshots\": {\n" + 
                                                          "                    \"file.xml\": {\n" + 
                                                          "                        \"matchers\": [],\n" + 
                                                          "                        \"path\": \"/home/atsuser/snapshots/file.xml\",\n" + 
                                                          "                        \"size\": -1,\n" + 
                                                          "                        \"timeModified\": 1529664801000,\n" + 
                                                          "                        \"permissions\": \"0664\",\n" + 
                                                          "                        \"configuration\": {\n" + 
                                                          "                            \"checkModificationTime\": true,\n" + 
                                                          "                            \"checkSize\": false,\n" + 
                                                          "                            \"checkMD5\": false,\n" + 
                                                          "                            \"checkPermissions\": true,\n" + 
                                                          "                            \"supportHidden\": true,\n" + 
                                                          "                            \"checkPropertiesFilesContent\": true,\n" + 
                                                          "                            \"propertiesFileExtensions\": [\".properties\"],\n" + 
                                                          "                            \"checkXmlFilesContent\": true,\n" + 
                                                          "                            \"xmlFileExtensions\": [\".xml\"],\n" + 
                                                          "                            \"checkIniFilesContent\": true,\n" + 
                                                          "                            \"iniFilesStartComment\": \"#\",\n" + 
                                                          "                            \"iniFilesStartSection\": \"[\",\n" + 
                                                          "                            \"iniFilesDelimiter\": \"\\u003d\",\n" + 
                                                          "                            \"iniFileExtensions\": [\".ini\"],\n" + 
                                                          "                            \"checkTextFilesContent\": true,\n" + 
                                                          "                            \"textFileExtensions\": [\".txt\"]\n" + 
                                                          "                        }\n" + 
                                                          "                    },\n" + 
                                                          "                    \"file.ini\": {\n" + 
                                                          "                        \"matchers\": [],\n" + 
                                                          "                        \"path\": \"/home/atsuser/snapshots/file.ini\",\n" + 
                                                          "                        \"size\": -1,\n" + 
                                                          "                        \"timeModified\": 1529665246000,\n" + 
                                                          "                        \"permissions\": \"0664\",\n" + 
                                                          "                        \"configuration\": {\n" + 
                                                          "                            \"checkModificationTime\": true,\n" + 
                                                          "                            \"checkSize\": false,\n" + 
                                                          "                            \"checkMD5\": false,\n" + 
                                                          "                            \"checkPermissions\": true,\n" + 
                                                          "                            \"supportHidden\": true,\n" + 
                                                          "                            \"checkPropertiesFilesContent\": true,\n" + 
                                                          "                            \"propertiesFileExtensions\": [\".properties\"],\n" + 
                                                          "                            \"checkXmlFilesContent\": true,\n" + 
                                                          "                            \"xmlFileExtensions\": [\".xml\"],\n" + 
                                                          "                            \"checkIniFilesContent\": true,\n" + 
                                                          "                            \"iniFilesStartComment\": \"#\",\n" + 
                                                          "                            \"iniFilesStartSection\": \"[\",\n" + 
                                                          "                            \"iniFilesDelimiter\": \"\\u003d\",\n" + 
                                                          "                            \"iniFileExtensions\": [\".ini\"],\n" + 
                                                          "                            \"checkTextFilesContent\": true,\n" + 
                                                          "                            \"textFileExtensions\": [\".txt\"]\n" + 
                                                          "                        }\n" + 
                                                          "                    },\n" + 
                                                          "                    \"file.property\": {\n" + 
                                                          "                        \"path\": \"/home/atsuser/snapshots/file.property\",\n" + 
                                                          "                        \"size\": 319,\n" + 
                                                          "                        \"timeModified\": 1529657430000,\n" + 
                                                          "                        \"md5\": \"762a27b6dd51a4e09facdd2f32ed3abb\",\n" + 
                                                          "                        \"permissions\": \"0664\",\n" + 
                                                          "                        \"configuration\": {\n" + 
                                                          "                            \"checkModificationTime\": true,\n" + 
                                                          "                            \"checkSize\": true,\n" + 
                                                          "                            \"checkMD5\": true,\n" + 
                                                          "                            \"checkPermissions\": true,\n" + 
                                                          "                            \"supportHidden\": true,\n" + 
                                                          "                            \"checkPropertiesFilesContent\": true,\n" + 
                                                          "                            \"propertiesFileExtensions\": [\".properties\"],\n" + 
                                                          "                            \"checkXmlFilesContent\": true,\n" + 
                                                          "                            \"xmlFileExtensions\": [\".xml\"],\n" + 
                                                          "                            \"checkIniFilesContent\": true,\n" + 
                                                          "                            \"iniFilesStartComment\": \"#\",\n" + 
                                                          "                            \"iniFilesStartSection\": \"[\",\n" + 
                                                          "                            \"iniFilesDelimiter\": \"\\u003d\",\n" + 
                                                          "                            \"iniFileExtensions\": [\".ini\"],\n" + 
                                                          "                            \"checkTextFilesContent\": true,\n" + 
                                                          "                            \"textFileExtensions\": [\".txt\"]\n" + 
                                                          "                        }\n" + 
                                                          "                    }\n" + 
                                                          "                },\n" + 
                                                          "                \"matchersContainer\": {\n" + 
                                                          "                    \"fileAttributesMap\": {},\n" + 
                                                          "                    \"propertyMatchersMap\": {},\n" + 
                                                          "                    \"xmlMatchersMap\": {},\n" + 
                                                          "                    \"iniMatchersMap\": {},\n" + 
                                                          "                    \"textMatchersMap\": {}\n" + 
                                                          "                },\n" + 
                                                          "                \"skippedSubDirs\": []\n" + 
                                                          "            }\n" + 
                                                          "        },\n" + 
                                                          "        \"configuration\": {\n" + 
                                                          "            \"checkModificationTime\": true,\n" + 
                                                          "            \"checkSize\": true,\n" + 
                                                          "            \"checkMD5\": true,\n" + 
                                                          "            \"checkPermissions\": true,\n" + 
                                                          "            \"supportHidden\": true,\n" + 
                                                          "            \"checkPropertiesFilesContent\": true,\n" + 
                                                          "            \"propertiesFileExtensions\": [\".properties\"],\n" + 
                                                          "            \"checkXmlFilesContent\": true,\n" + 
                                                          "            \"xmlFileExtensions\": [\".xml\"],\n" + 
                                                          "            \"checkIniFilesContent\": true,\n" + 
                                                          "            \"iniFilesStartComment\": \"#\",\n" + 
                                                          "            \"iniFilesStartSection\": \"[\",\n" + 
                                                          "            \"iniFilesDelimiter\": \"\\u003d\",\n" + 
                                                          "            \"iniFileExtensions\": [\".ini\"],\n" + 
                                                          "            \"checkTextFilesContent\": true,\n" + 
                                                          "            \"textFileExtensions\": [\".txt\"]\n" + 
                                                          "        }\n" + 
                                                          "}\n" + 
                                                          "",
                                                  name = "srcFileSystemSnapshot",
                                                  type = "object")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully create new file system snapshot from existing one details",
                                       description = "Successfully create new file system snapshot from existing one",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "new file system snapshot successfully created from existig one",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while creating new file system snapshot from existing one details",
                                       description = "Error while creating new file system snapshot from existing one",
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
    public Response newFileSystemSnapshot( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        LocalFileSystemSnapshot srcFileSystemSnapshot = null;
        String newSnapshotName = null;
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
            String srcFileSystemSnapshotJSON = getJsonElement(jsonObject, "srcFileSystemSnapshot").toString();
            if (StringUtils.isNullOrEmpty(srcFileSystemSnapshotJSON)) {
                throw new NoSuchElementException("srcFileSystemSnapshot is not provided with the request");
            }
            srcFileSystemSnapshot = GSON.fromJson(srcFileSystemSnapshotJSON, LocalFileSystemSnapshot.class);
            newSnapshotName = getJsonElement(jsonObject, "newSnapshotName").getAsString();
            if (StringUtils.isNullOrEmpty(newSnapshotName)) {
                throw new NoSuchElementException("newSnapshotName is not provided with the request");
            }
            FileSystemSnapshotManager.newFileSystemSnapshot(sessionId, resourceId, srcFileSystemSnapshot,
                                                            newSnapshotName);
            return Response.ok("{\"status_message\":"
                               + "\"new file system snapshot successfully created from existig one\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to create new file system snapshot from existing one using resource with id '"
                             + resourceId
                             + "' from session with id '" + sessionId
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
    @Path( "name")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Set new name for file system snapshot details",
            summary = "Set new name for file system snapshot",
            url = "name")
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
                                                  description = "The new snapshot name",
                                                  example = "some name",
                                                  name = "name",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully set new name for file system snapshot details",
                                       description = "Successfully set new name for file system snapshot",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "file system snapshot name successfully changed",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while setting new file system snapshot name details",
                                       description = "Error while setting new file system snapshot name",
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
    public Response setName( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String name = null;
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
            name = getJsonElement(jsonObject, "name").getAsString();
            if (StringUtils.isNullOrEmpty(name)) {
                throw new NoSuchElementException("name is not provided with the request");
            }
            FileSystemSnapshotManager.setName(sessionId, resourceId, name);
            return Response.ok("{\"status_message\":" + "\"file system snapshot name successfully changed\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to set new name for file system snapshot using resource with id '" + resourceId
                             + "' from session with id '" + sessionId
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
    @Path( "file/check")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Check file from file system snapshot details",
            summary = "Check file from file system snapshot",
            url = "file/check")
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
                                                  description = "The file's parent directory alias",
                                                  example = "root_dir",
                                                  name = "rootDirectoryAlias",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The relative file path. "
                                                                + "For example if the parent dir is /home/atsuser and "
                                                                + "the file's path is /home/atsuser/subdir/file.txt, "
                                                                + "the relative path is subdir/file.txt",
                                                  example = "subdir/file.txt",
                                                  name = "relativeFilePath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Which file rules to check. See FileSystemSnapshot.CHECK_* constants for more info"
                                                                + "Check size (64), "
                                                                + "Check modification time (128), "
                                                                + "Check MD5 (256), "
                                                                + "Check permissions (512)",
                                                  example = "[64,128,256,512]",
                                                  name = "checkRules",
                                                  type = "int[]")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully check file from file system snapshot details",
                                       description = "Successfully check file from file system snapshot",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "file from file system snapshot name successfully checked",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while checking file from file system snapshot name details",
                                       description = "Error while checking file from file system snapshot name",
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
    public Response checkFile( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String rootDirectoryAlias = null;
        String relativeFilePath = null;
        int[] checkRules = null;
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
            rootDirectoryAlias = getJsonElement(jsonObject, "rootDirectoryAlias").getAsString();
            if (StringUtils.isNullOrEmpty(rootDirectoryAlias)) {
                throw new NoSuchElementException("rootDirectoryAlias is not provided with the request");
            }
            relativeFilePath = getJsonElement(jsonObject, "relativeFilePath").getAsString();
            if (StringUtils.isNullOrEmpty(relativeFilePath)) {
                throw new NoSuchElementException("relativeFilePath is not provided with the request");
            }
            String checkRulesJSON = getJsonElement(jsonObject, "checkRules").toString();
            if (StringUtils.isNullOrEmpty(checkRulesJSON)) {
                throw new NoSuchElementException("checkRules is not provided with the request");
            }
            checkRules = GSON.fromJson(checkRulesJSON, int[].class);
            FileSystemSnapshotManager.checkFile(sessionId, resourceId, rootDirectoryAlias, relativeFilePath,
                                                checkRules);
            return Response.ok("{\"status_message\":" + "\"file from file system snapshot name successfully checked\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to check file from file system snapshot using resource with id '" + resourceId
                             + "' from session with id '" + sessionId
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
    @Path( "directory/skip")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Skip directory in file system snapshot details",
            summary = "Skip directory in file system snapshot",
            url = "directory/skip")
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
                                                  description = "The directory relative path",
                                                  example = "subdir2",
                                                  name = "relativeDirectoryPath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The root directory alias",
                                                  example = "some root dir",
                                                  name = "rootDirectoryAlias",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully skip directory in file system snapshot details",
                                       description = "Successfully skip directory in file system snapshot",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "directory successfully skipped",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while skipping directory in file system snapshot details",
                                       description = "Error while skipping directory in file system snapshot",
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
    public Response skipDirectory( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String rootDirectoryAlias = null;
        String relativeDirectoryPath = null;
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
            rootDirectoryAlias = getJsonElement(jsonObject, "rootDirectoryAlias").getAsString();
            if (StringUtils.isNullOrEmpty(rootDirectoryAlias)) {
                throw new NoSuchElementException("rootDirectoryAlias is not provided with the request");
            }
            relativeDirectoryPath = getJsonElement(jsonObject, "relativeDirectoryPath").getAsString();
            if (StringUtils.isNullOrEmpty(relativeDirectoryPath)) {
                throw new NoSuchElementException("relativeDirectoryPath is not provided with the request");
            }
            FileSystemSnapshotManager.skipDirectory(sessionId, resourceId, rootDirectoryAlias, relativeDirectoryPath);
            return Response.ok("{\"status_message\":" + "\"directory successfully skipped\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to skip directory in file system snapshot using resource with id '" + resourceId
                             + "' from session with id '" + sessionId
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
    @Path( "directory/skip/byRegex")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Skip directory by regex in file system snapshot details",
            summary = "Skip directory by regex in file system snapshot",
            url = "directory/skip/byRegex")
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
                                                  description = "The directory relative path",
                                                  example = "[a-z].*[0-9]",
                                                  name = "relativeDirectoryPath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The root directory alias",
                                                  example = "some root dir",
                                                  name = "rootDirectoryAlias",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully skip directory by regex in file system snapshot details",
                                       description = "Successfully skip directory by regex in file system snapshot",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "directory by regex successfully skipped",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while skipping directory by regex in file system snapshot details",
                                       description = "Error while skipping directory by regex in file system snapshot",
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
    public Response skipDirectoryByRegex( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String rootDirectoryAlias = null;
        String relativeDirectoryPath = null;
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
            rootDirectoryAlias = getJsonElement(jsonObject, "rootDirectoryAlias").getAsString();
            if (StringUtils.isNullOrEmpty(rootDirectoryAlias)) {
                throw new NoSuchElementException("rootDirectoryAlias is not provided with the request");
            }
            relativeDirectoryPath = getJsonElement(jsonObject, "relativeDirectoryPath").getAsString();
            if (StringUtils.isNullOrEmpty(relativeDirectoryPath)) {
                throw new NoSuchElementException("relativeDirectoryPath is not provided with the request");
            }
            FileSystemSnapshotManager.skipDirectoryByRegex(sessionId, resourceId, rootDirectoryAlias,
                                                           relativeDirectoryPath);
            return Response.ok("{\"status_message\":" + "\"directory by regex successfully skipped\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to skip directory by regex in file system snapshot using resource with id '"
                             + resourceId
                             + "' from session with id '" + sessionId
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
    @Path( "file/skip")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Skip file in file system snapshot details",
            summary = "Skip file in file system snapshot",
            url = "file/skip")
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
                                                  description = "The file relative path",
                                                  example = "/home/atsuser/file.txt",
                                                  name = "relativeFilePath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The root directory alias",
                                                  example = "some root dir",
                                                  name = "rootDirectoryAlias",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Which file rules to skip. See FileSystemSnapshot.SKIP_* constants for more info"
                                                                + "Skip size (4), "
                                                                + "Skip modification time (8), "
                                                                + "Skip MD5 (16), "
                                                                + "Skip permissions (32)",
                                                  example = "[4,8,16,32]",
                                                  name = "skipRules",
                                                  type = "int[]")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully skip file in file system snapshot details",
                                       description = "Successfully skip file in file system snapshot",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "file successfully skipped",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while skipping file in file system snapshot details",
                                       description = "Error while skipping file in file system snapshot",
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
    public Response skipFile( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String rootDirectoryAlias = null;
        String relativeFilePath = null;
        int[] skipRules = null;
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
            rootDirectoryAlias = getJsonElement(jsonObject, "rootDirectoryAlias").getAsString();
            if (StringUtils.isNullOrEmpty(rootDirectoryAlias)) {
                throw new NoSuchElementException("rootDirectoryAlias is not provided with the request");
            }
            relativeFilePath = getJsonElement(jsonObject, "relativeFilePath").getAsString();
            if (StringUtils.isNullOrEmpty(relativeFilePath)) {
                throw new NoSuchElementException("relativeFilePath is not provided with the request");
            }
            String skipRulesJSON = getJsonElement(jsonObject, "skipRules").toString();
            if (StringUtils.isNullOrEmpty(skipRulesJSON)) {
                throw new NoSuchElementException("skipRules is not provided with the request");
            }
            skipRules = GSON.fromJson(skipRulesJSON, int[].class);
            FileSystemSnapshotManager.skipFile(sessionId, resourceId, rootDirectoryAlias, relativeFilePath, skipRules);
            return Response.ok("{\"status_message\":" + "\"file successfully skipped\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to skip file in file system snapshot using resource with id '" + resourceId
                             + "' from session with id '" + sessionId
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
    @Path( "file/skip/byRegex")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Skip file by regex in file system snapshot details",
            summary = "Skip file by regex in file system snapshot",
            url = "file/skip/byRegex")
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
                                                  description = "The file relative path",
                                                  example = "[a-z].*",
                                                  name = "relativeFilePath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The root directory alias",
                                                  example = "some root dir",
                                                  name = "rootDirectoryAlias",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Which file rules to skip. See FileSystemSnapshot.SKIP_* constants for more info"
                                                                + "Skip size (4), "
                                                                + "Skip modification time (8), "
                                                                + "Skip MD5 (16), "
                                                                + "Skip permissions (32)",
                                                  example = "[4,8,16,32]",
                                                  name = "skipRules",
                                                  type = "int[]")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully skip file by regex in file system snapshot details",
                                       description = "Successfully skip file by regex in file system snapshot",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "file by regex successfully skipped",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while skipping file by regex in file system snapshot details",
                                       description = "Error while skipping file by regex in file system snapshot",
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
    public Response skipFileByRegex( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String rootDirectoryAlias = null;
        String relativeFilePath = null;
        int[] skipRules = null;
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
            rootDirectoryAlias = getJsonElement(jsonObject, "rootDirectoryAlias").getAsString();
            if (StringUtils.isNullOrEmpty(rootDirectoryAlias)) {
                throw new NoSuchElementException("rootDirectoryAlias is not provided with the request");
            }
            relativeFilePath = getJsonElement(jsonObject, "relativeFilePath").getAsString();
            if (StringUtils.isNullOrEmpty(relativeFilePath)) {
                throw new NoSuchElementException("relativeFilePath is not provided with the request");
            }
            String skipRulesJSON = getJsonElement(jsonObject, "skipRules").toString();
            if (StringUtils.isNullOrEmpty(skipRulesJSON)) {
                throw new NoSuchElementException("skipRules is not provided with the request");
            }
            skipRules = GSON.fromJson(skipRulesJSON, int[].class);
            FileSystemSnapshotManager.skipFileByRegex(sessionId, resourceId, rootDirectoryAlias,
                                                      relativeFilePath, skipRules);
            return Response.ok("{\"status_message\":" + "\"file by regex successfully skipped\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to skip file by regex in file system snapshot using resource with id '"
                             + resourceId
                             + "' from session with id '" + sessionId
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
    @Path( "file/skip/line")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Skip line from file in file system snapshot details",
            summary = "Skip line from file in file system snapshot",
            url = "file/skip/line")
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
                                                  description = "The alias of the root directory",
                                                  example = "some root dir alias",
                                                  name = "rootDirectoryAlias",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Path to this file relative to the directory with provided alias",
                                                  example = "some_file.sh",
                                                  name = "relativeFilePath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The line to skip. "
                                                                + "Depending on the matchType, this can be either plain text or a regex",
                                                  example = "some text|[a-z].:[0-9]",
                                                  name = "line",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The matching type. "
                                                                + "A line can be skipped by being equal to some text, "
                                                                + "containing some text or being found by some regex",
                                                  example = "TEXT|CONTAINS_TEXT|REGEX",
                                                  name = "matchType",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully skip line from file in file system snapshot details",
                                       description = "Successfully skip line from file in file system snapshot",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "line from file successfully skipped",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while skipping line from file in file system snapshot details",
                                       description = "Error while skipping line from file in file system snapshot details",
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
    public Response skipTextLine( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String rootDirectoryAlias = null;
        String relativeFilePath = null;
        String line = null;
        String matchType = null;
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
            rootDirectoryAlias = getJsonElement(jsonObject, "rootDirectoryAlias").getAsString();
            if (StringUtils.isNullOrEmpty(rootDirectoryAlias)) {
                throw new NoSuchElementException("rootDirectoryAlias is not provided with the request");
            }
            relativeFilePath = getJsonElement(jsonObject, "relativeFilePath").getAsString();
            if (StringUtils.isNullOrEmpty(relativeFilePath)) {
                throw new NoSuchElementException("relativeFilePath is not provided with the request");
            }
            line = getJsonElement(jsonObject, "line").getAsString();
            if (StringUtils.isNullOrEmpty(line)) {
                throw new NoSuchElementException("line is not provided with the request");
            }
            matchType = getJsonElement(jsonObject, "matchType").getAsString();
            if (StringUtils.isNullOrEmpty(matchType)) {
                throw new NoSuchElementException("matchType is not provided with the request");
            }
            FileSystemSnapshotManager.skipTextLine(sessionId, resourceId, rootDirectoryAlias,
                                                   relativeFilePath,
                                                   line,
                                                   matchType);
            return Response.ok("{\"status_message\":" + "\"line from file successfully skipped\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to skip line from file in file system snapshot using resource with id '"
                             + resourceId
                             + "' from session with id '" + sessionId
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
    @Path( "file/property/skip/key")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Skip key from property file in file system snapshot details",
            summary = "Skip key from property file in file system snapshot",
            url = "file/property/skip/key")
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
                                                  description = "The alias of the root directory",
                                                  example = "some root dir alias",
                                                  name = "rootDirectoryAlias",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Path to this file relative to the directory with provided alias",
                                                  example = "some_file.property",
                                                  name = "relativeFilePath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The key to skip. "
                                                                + "Depending on the matchType, this can be either plain text or a regex",
                                                  example = "some text|[a-z].:[0-9]",
                                                  name = "key",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The matching type. "
                                                                + "A key can be skipped by being equal to some text, "
                                                                + "containing some text or being found by some regex",
                                                  example = "TEXT|CONTAINS_TEXT|REGEX",
                                                  name = "matchType",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully skip key from property file in file system snapshot details",
                                       description = "Successfully skip key from property file in file system snapshot",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "key from property file successfully skipped",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while skipping key from property file in file system snapshot details",
                                       description = "Error while skipping key from property file in file system",
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
    public Response skipPropertyWithKey( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String rootDirectoryAlias = null;
        String relativeFilePath = null;
        String key = null;
        String matchType = null;
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
            rootDirectoryAlias = getJsonElement(jsonObject, "rootDirectoryAlias").getAsString();
            if (StringUtils.isNullOrEmpty(rootDirectoryAlias)) {
                throw new NoSuchElementException("rootDirectoryAlias is not provided with the request");
            }
            relativeFilePath = getJsonElement(jsonObject, "relativeFilePath").getAsString();
            if (StringUtils.isNullOrEmpty(relativeFilePath)) {
                throw new NoSuchElementException("relativeFilePath is not provided with the request");
            }
            key = getJsonElement(jsonObject, "key").getAsString();
            if (StringUtils.isNullOrEmpty(key)) {
                throw new NoSuchElementException("key is not provided with the request");
            }
            matchType = getJsonElement(jsonObject, "matchType").getAsString();
            if (StringUtils.isNullOrEmpty(matchType)) {
                throw new NoSuchElementException("matchType is not provided with the request");
            }
            FileSystemSnapshotManager.skipPropertyWithKey(sessionId, resourceId, rootDirectoryAlias,
                                                          relativeFilePath,
                                                          key,
                                                          matchType);
            return Response.ok("{\"status_message\":" + "\"key from property file successfully skipped\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to skip key from property file in file system snapshot using resource with id '"
                             + resourceId
                             + "' from session with id '" + sessionId
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
    @Path( "file/property/skip/value")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Skip value from property file in file system snapshot details",
            summary = "Skip value from property file in file system snapshot",
            url = "file/property/skip/value")
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
                                                  description = "The alias of the root directory",
                                                  example = "some root dir alias",
                                                  name = "rootDirectoryAlias",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Path to this file relative to the directory with provided alias",
                                                  example = "some_file.property",
                                                  name = "relativeFilePath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The value to skip. "
                                                                + "Depending on the matchType, this can be either plain text or a regex",
                                                  example = "some text|[a-z].:[0-9]",
                                                  name = "value",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The matching type. "
                                                                + "A value can be skipped by being equal to some text, "
                                                                + "containing some text or being found by some regex",
                                                  example = "TEXT|CONTAINS_TEXT|REGEX",
                                                  name = "matchType",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully skip value from property file in file system snapshot details",
                                       description = "Successfully skip value from property file in file system snapshot",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "value from property file successfully skipped",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while skipping value from property file in file system snapshot details",
                                       description = "Error while skipping value from property file in file system",
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
    public Response skipPropertyWithValue( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String rootDirectoryAlias = null;
        String relativeFilePath = null;
        String value = null;
        String matchType = null;
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
            rootDirectoryAlias = getJsonElement(jsonObject, "rootDirectoryAlias").getAsString();
            if (StringUtils.isNullOrEmpty(rootDirectoryAlias)) {
                throw new NoSuchElementException("rootDirectoryAlias is not provided with the request");
            }
            relativeFilePath = getJsonElement(jsonObject, "relativeFilePath").getAsString();
            if (StringUtils.isNullOrEmpty(relativeFilePath)) {
                throw new NoSuchElementException("relativeFilePath is not provided with the request");
            }
            value = getJsonElement(jsonObject, "value").getAsString();
            if (StringUtils.isNullOrEmpty(value)) {
                throw new NoSuchElementException("value is not provided with the request");
            }
            matchType = getJsonElement(jsonObject, "matchType").getAsString();
            if (StringUtils.isNullOrEmpty(matchType)) {
                throw new NoSuchElementException("matchType is not provided with the request");
            }
            FileSystemSnapshotManager.skipPropertyWithValue(sessionId, resourceId, rootDirectoryAlias,
                                                            relativeFilePath,
                                                            value,
                                                            matchType);
            return Response.ok("{\"status_message\":" + "\"value from property file successfully skipped\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to skip value from property file in file system snapshot using resource with id '"
                             + resourceId
                             + "' from session with id '" + sessionId
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
    @Path( "file/xml/skip/node/attribute")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Skip XML node by attribute from XML file in file system snapshot details",
            summary = "Skip XML node by attribute from XML file in file system snapshot",
            url = "file/xml/skip/node/attribute")
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
                                                  description = "The alias of the root directory",
                                                  example = "some root dir alias",
                                                  name = "rootDirectoryAlias",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Path to this file relative to the directory with provided alias",
                                                  example = "some_file.xml",
                                                  name = "relativeFilePath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The XPath to the XML node. Can be either relative or absolute",
                                                  example = "/shop/item",
                                                  name = "nodeXpath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The XML node's attribute key",
                                                  example = "id",
                                                  name = "attributeKey",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The value to skip. "
                                                                + "Depending on the matchType, this can be either plain text or a regex",
                                                  example = "some text|[a-z].:[0-9]",
                                                  name = "attributeValue",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The matching type. "
                                                                + "An attribute can be skipped by being equal to some text, "
                                                                + "containing some text or being found by some regex",
                                                  example = "TEXT|CONTAINS_TEXT|REGEX",
                                                  name = "attributeValueMatchType",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully skip XML node by attribute from XML file in file system snapshot details",
                                       description = "Successfully Skip XML node by attribute from XML file in file system snapshot",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "XML node by attribute from XML file successfully skipped",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while skipping XML node by attribute from XML file in file system snapshot details",
                                       description = "Error while skipping XML node by attribute from XML file in file system snapshot",
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
    public Response skipNodeByAttribute( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String rootDirectoryAlias = null;
        String relativeFilePath = null;
        String nodeXpath = null;
        String attributeKey = null;
        String attributeValue = null;
        String attributeValueMatchType = null;
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
            rootDirectoryAlias = getJsonElement(jsonObject, "rootDirectoryAlias").getAsString();
            if (StringUtils.isNullOrEmpty(rootDirectoryAlias)) {
                throw new NoSuchElementException("rootDirectoryAlias is not provided with the request");
            }
            relativeFilePath = getJsonElement(jsonObject, "relativeFilePath").getAsString();
            if (StringUtils.isNullOrEmpty(relativeFilePath)) {
                throw new NoSuchElementException("relativeFilePath is not provided with the request");
            }

            nodeXpath = getJsonElement(jsonObject, "nodeXpath").getAsString();
            if (StringUtils.isNullOrEmpty(nodeXpath)) {
                throw new NoSuchElementException("nodeXpath is not provided with the request");
            }

            attributeKey = getJsonElement(jsonObject, "attributeKey").getAsString();
            if (StringUtils.isNullOrEmpty(attributeKey)) {
                throw new NoSuchElementException("attributeKey is not provided with the request");
            }

            attributeValue = getJsonElement(jsonObject, "attributeValue").getAsString();
            if (StringUtils.isNullOrEmpty(attributeValue)) {
                throw new NoSuchElementException("attributeValue is not provided with the request");
            }

            attributeValueMatchType = getJsonElement(jsonObject, "attributeValueMatchType").getAsString();
            if (StringUtils.isNullOrEmpty(attributeValueMatchType)) {
                throw new NoSuchElementException("attributeValueMatchType is not provided with the request");
            }
            FileSystemSnapshotManager.skipNodeByAttribute(sessionId, resourceId, rootDirectoryAlias,
                                                          relativeFilePath,
                                                          nodeXpath,
                                                          attributeKey,
                                                          attributeValue,
                                                          attributeValueMatchType);
            return Response.ok("{\"status_message\":" + "\"XML node by attribute from XML file successfully skipped\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to skip XML node by attribute from XML file in file system snapshot using resource with id '"
                             + resourceId
                             + "' from session with id '" + sessionId
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
    @Path( "file/xml/skip/node/value")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Skip XML node by value from XML file in file system snapshot details",
            summary = "Skip XML node by value from XML file in file system snapshot",
            url = "file/xml/skip/node/value")
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
                                                  description = "The alias of the root directory",
                                                  example = "some root dir alias",
                                                  name = "rootDirectoryAlias",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Path to this file relative to the directory with provided alias",
                                                  example = "some_file.xml",
                                                  name = "relativeFilePath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The XPath to the XML node. Can be either relative or absolute",
                                                  example = "/shop/item",
                                                  name = "nodeXpath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The value to skip. "
                                                                + "Depending on the matchType, this can be either plain text or a regex",
                                                  example = "some text|[a-z].:[0-9]",
                                                  name = "value",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The matching type. "
                                                                + "A value can be skipped by being equal to some text, "
                                                                + "containing some text or being found by some regex",
                                                  example = "TEXT|CONTAINS_TEXT|REGEX",
                                                  name = "matchType",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully skip XML node by value from XML file in file system snapshot details",
                                       description = "Successfully Skip XML node by value from XML file in file system snapshot",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "XML node by value from XML file successfully skipped",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while skipping XML node by value from XML file in file system snapshot details",
                                       description = "Error while skipping XML node by value from XML file in file system snapshot",
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
    public Response skipNodeByValue( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String rootDirectoryAlias = null;
        String relativeFilePath = null;
        String nodeXpath = null;
        String value = null;
        String matchType = null;
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
            rootDirectoryAlias = getJsonElement(jsonObject, "rootDirectoryAlias").getAsString();
            if (StringUtils.isNullOrEmpty(rootDirectoryAlias)) {
                throw new NoSuchElementException("rootDirectoryAlias is not provided with the request");
            }
            relativeFilePath = getJsonElement(jsonObject, "relativeFilePath").getAsString();
            if (StringUtils.isNullOrEmpty(relativeFilePath)) {
                throw new NoSuchElementException("relativeFilePath is not provided with the request");
            }
            nodeXpath = getJsonElement(jsonObject, "nodeXpath").getAsString();
            if (StringUtils.isNullOrEmpty(nodeXpath)) {
                throw new NoSuchElementException("nodeXpath is not provided with the request");
            }
            value = getJsonElement(jsonObject, "value").getAsString();
            if (StringUtils.isNullOrEmpty(value)) {
                throw new NoSuchElementException("value is not provided with the request");
            }
            matchType = getJsonElement(jsonObject, "matchType").getAsString();
            if (StringUtils.isNullOrEmpty(matchType)) {
                throw new NoSuchElementException("matchType is not provided with the request");
            }
            FileSystemSnapshotManager.skipNodeByValue(sessionId, resourceId, rootDirectoryAlias,
                                                      relativeFilePath,
                                                      nodeXpath,
                                                      value,
                                                      matchType);
            return Response.ok("{\"status_message\":" + "\"XML node by value from XML file successfully skipped\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to skip XML node by value from XML file in file system snapshot using resource with id '"
                             + resourceId
                             + "' from session with id '" + sessionId
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
    @Path( "file/ini/skip/section")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Skip section from INI file in file system snapshot details",
            summary = "Skip section from INI file in file system snapshot",
            url = "file/ini/skip/section")
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
                                                  description = "The alias of the root directory",
                                                  example = "some root dir alias",
                                                  name = "rootDirectoryAlias",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Path to this file relative to the directory with provided alias",
                                                  example = "some_file.ini",
                                                  name = "relativeFilePath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The section to skip. "
                                                                + "Depending on the matchType, this can be either plain text or a regex",
                                                  example = "some section id/name|[a-z].:[0-9]",
                                                  name = "section",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The matching type. "
                                                                + "A section can be skipped by being equal to some text, "
                                                                + "containing some text or being found by some regex",
                                                  example = "TEXT|CONTAINS_TEXT|REGEX",
                                                  name = "matchType",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully skip section from INI file in file system snapshot details",
                                       description = "Successfully skip section from INI file in file system snapshot",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "section from INI file successfully skipped",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while skipping section from INI file in file system snapshot details",
                                       description = "Error while skipping section from INI file in file system snapshot",
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
    public Response skipIniSection( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String rootDirectoryAlias = null;
        String relativeFilePath = null;
        String section = null;
        String matchType = null;
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
            rootDirectoryAlias = getJsonElement(jsonObject, "rootDirectoryAlias").getAsString();
            if (StringUtils.isNullOrEmpty(rootDirectoryAlias)) {
                throw new NoSuchElementException("rootDirectoryAlias is not provided with the request");
            }
            relativeFilePath = getJsonElement(jsonObject, "relativeFilePath").getAsString();
            if (StringUtils.isNullOrEmpty(relativeFilePath)) {
                throw new NoSuchElementException("relativeFilePath is not provided with the request");
            }
            section = getJsonElement(jsonObject, "section").getAsString();
            if (StringUtils.isNullOrEmpty(section)) {
                throw new NoSuchElementException("section is not provided with the request");
            }
            matchType = getJsonElement(jsonObject, "matchType").getAsString();
            if (StringUtils.isNullOrEmpty(matchType)) {
                throw new NoSuchElementException("matchType is not provided with the request");
            }
            FileSystemSnapshotManager.skipIniSection(sessionId, resourceId, rootDirectoryAlias,
                                                     relativeFilePath,
                                                     section,
                                                     matchType);
            return Response.ok("{\"status_message\":" + "\"section from INI file successfully skipped\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to skip section from INI file in file system snapshot using resource with id '"
                             + resourceId
                             + "' from session with id '" + sessionId
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
    @Path( "file/ini/skip/property/key")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Skip property by key from INI file in file system snapshot details",
            summary = "Skip property by key from INI file in file system snapshot",
            url = "file/ini/skip/property/key")
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
                                                  description = "The alias of the root directory",
                                                  example = "some root dir alias",
                                                  name = "rootDirectoryAlias",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Path to this file relative to the directory with provided alias",
                                                  example = "some_file.ini",
                                                  name = "relativeFilePath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The INI section where the key is presented",
                                                  example = "Mail",
                                                  name = "section",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The key to skip. "
                                                                + "Depending on the matchType, this can be either plain text or a regex",
                                                  example = "some key|[a-z].:[0-9]",
                                                  name = "key",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The matching type. "
                                                                + "A property can be skipped by its key being equal to some text, "
                                                                + "containing some text or being found by some regex",
                                                  example = "TEXT|CONTAINS_TEXT|REGEX",
                                                  name = "matchType",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully skip property by key from INI file in file system snapshot details",
                                       description = "Successfully skip property by key from INI file in file system snapshot",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "property by key from INI file successfully skipped",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while skipping property by key from INI file in file system snapshot details",
                                       description = "Error while skipping property by key from INI file in file system snapshot",
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
    public Response skipIniPropertyWithKey( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String rootDirectoryAlias = null;
        String relativeFilePath = null;
        String section = null;
        String key = null;
        String matchType = null;
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
            rootDirectoryAlias = getJsonElement(jsonObject, "rootDirectoryAlias").getAsString();
            if (StringUtils.isNullOrEmpty(rootDirectoryAlias)) {
                throw new NoSuchElementException("rootDirectoryAlias is not provided with the request");
            }
            relativeFilePath = getJsonElement(jsonObject, "relativeFilePath").getAsString();
            if (StringUtils.isNullOrEmpty(relativeFilePath)) {
                throw new NoSuchElementException("relativeFilePath is not provided with the request");
            }
            section = getJsonElement(jsonObject, "section").getAsString();
            if (StringUtils.isNullOrEmpty(section)) {
                throw new NoSuchElementException("section is not provided with the request");
            }
            key = getJsonElement(jsonObject, "key").getAsString();
            if (StringUtils.isNullOrEmpty(key)) {
                throw new NoSuchElementException("key is not provided with the request");
            }
            matchType = getJsonElement(jsonObject, "matchType").getAsString();
            if (StringUtils.isNullOrEmpty(matchType)) {
                throw new NoSuchElementException("matchType is not provided with the request");
            }
            FileSystemSnapshotManager.skipIniPropertyWithKey(sessionId,
                                                             resourceId,
                                                             rootDirectoryAlias,
                                                             relativeFilePath,
                                                             section,
                                                             key,
                                                             matchType);
            return Response.ok("{\"status_message\":" + "\"property by key from INI file successfully skipped\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to skip property by key from INI file in file system snapshot using resource with id '"
                             + resourceId
                             + "' from session with id '" + sessionId
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
    @Path( "file/ini/skip/property/value")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "post",
            parametersDefinition = "Skip property by value from INI file in file system snapshot details",
            summary = "Skip property by value from INI file in file system snapshot",
            url = "file/ini/skip/property/value")
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
                                                  description = "The alias of the root directory",
                                                  example = "some root dir alias",
                                                  name = "rootDirectoryAlias",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "Path to this file relative to the directory with provided alias",
                                                  example = "some_file.ini",
                                                  name = "relativeFilePath",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The INI section where the value is presented",
                                                  example = "Mail",
                                                  name = "section",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The value to skip. "
                                                                + "Depending on the matchType, this can be either plain text or a regex",
                                                  example = "some value|[a-z].:[0-9]",
                                                  name = "value",
                                                  type = "string"),
                                          @SwaggerMethodParameterDefinition(
                                                  description = "The matching type. "
                                                                + "A property can be skipped by its value being equal to some text, "
                                                                + "containing some text or being found by some regex",
                                                  example = "TEXT|CONTAINS_TEXT|REGEX",
                                                  name = "matchType",
                                                  type = "string")
    })
    @SwaggerMethodResponses( {
                               @SwaggerMethodResponse(
                                       code = 200,
                                       definition = "Successfully skip property by value from INI file in file system snapshot details",
                                       description = "Successfully skip property by value from INI file in file system snapshot",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "Status message",
                                               example = "property by value from INI file successfully skipped",
                                               name = "status_message",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while skipping property by value from INI file in file system snapshot details",
                                       description = "Error while skipping property by value from INI file in file system snapshot",
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
    public Response skipIniPropertyWithValue( @Context HttpServletRequest request ) {

        String sessionId = null;
        int resourceId = -1;
        String rootDirectoryAlias = null;
        String relativeFilePath = null;
        String section = null;
        String value = null;
        String matchType = null;
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
            rootDirectoryAlias = getJsonElement(jsonObject, "rootDirectoryAlias").getAsString();
            if (StringUtils.isNullOrEmpty(rootDirectoryAlias)) {
                throw new NoSuchElementException("rootDirectoryAlias is not provided with the request");
            }
            relativeFilePath = getJsonElement(jsonObject, "relativeFilePath").getAsString();
            if (StringUtils.isNullOrEmpty(relativeFilePath)) {
                throw new NoSuchElementException("relativeFilePath is not provided with the request");
            }
            section = getJsonElement(jsonObject, "section").getAsString();
            if (StringUtils.isNullOrEmpty(section)) {
                throw new NoSuchElementException("section is not provided with the request");
            }
            value = getJsonElement(jsonObject, "value").getAsString();
            if (StringUtils.isNullOrEmpty(value)) {
                throw new NoSuchElementException("value is not provided with the request");
            }
            matchType = getJsonElement(jsonObject, "matchType").getAsString();
            if (StringUtils.isNullOrEmpty(matchType)) {
                throw new NoSuchElementException("matchType is not provided with the request");
            }
            FileSystemSnapshotManager.skipIniPropertyWithValue(sessionId,
                                                               resourceId,
                                                               rootDirectoryAlias,
                                                               relativeFilePath,
                                                               section,
                                                               value,
                                                               matchType);
            return Response.ok("{\"status_message\":" + "\"property by value from INI file successfully skipped\"}")
                           .build();
        } catch (Exception e) {
            String message = "Unable to skip property by value from INI file in file system snapshot using resource with id '"
                             + resourceId
                             + "' from session with id '" + sessionId
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
    @Path( "description")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    @SwaggerMethod(
            httpOperation = "get",
            parametersDefinition = "",
            summary = "Get file system snapshot description",
            url = "description")
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
                                       definition = "Successfully get file system snapshot description details",
                                       description = "Successfully get file system snapshot description",
                                       parametersDefinitions = { @SwaggerMethodParameterDefinition(
                                               description = "The description of the file system snapshot",
                                               example = "snapshot [fss1] taken at 2018-06-25 10:26:18.352+0300",
                                               name = "action_request",
                                               type = "string") }),
                               @SwaggerMethodResponse(
                                       code = 500,
                                       definition = "Error while getting file system snapshot description details",
                                       description = "Error while getting file system snapshot description",
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
            String description = FileSystemSnapshotManager.getDescription(sessionId, resourceId);
            return Response.ok("{\"action_result\":" + GSON.toJson(description) + "}").build();
        } catch (Exception e) {
            String message = "Unable to get file system snapshot description from session with id '" + sessionId
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
