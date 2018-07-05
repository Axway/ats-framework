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

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.agent.webapp.restservice.api.ResourcesManager;
import com.axway.ats.agent.webapp.restservice.api.actions.ActionPojo;
import com.axway.ats.common.filesystem.FileMatchInfo;
import com.axway.ats.common.filesystem.FileTailInfo;
import com.google.gson.Gson;

public class FileSystemManager {

    private static final int    UNINITIALIZED_RESOURCE_ID = -1;
    private static final String COMPONENT_NAME            = "auto-system-operations";

    private static final Gson   GSON                      = new Gson();

    public synchronized static int
            initialize( String sessionId ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                           NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                           IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, UNINITIALIZED_RESOURCE_ID, COMPONENT_NAME,
                                         "Internal File System Operations initialize",
                                         null,
                                         null);

        // create only the action class instance first
        int resourceId = ResourcesManager.initializeResource(pojo);
        pojo.setResourceId(resourceId);

        // then execute the action method
        ResourcesManager.executeOverResource(pojo);

        return resourceId;
    }

    public synchronized static void appendToFile( String sessionId, int resourceId, String filePath,
                                                  String contentToAdd ) throws NoSuchActionException,
                                                                        NoCompatibleMethodFoundException,
                                                                        NoSuchComponentException,
                                                                        ClassNotFoundException, InstantiationException,
                                                                        IllegalAccessException,
                                                                        IllegalArgumentException,
                                                                        InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations Append To File",
                                         new String[]{ String.class.getName(),
                                                       String.class.getName() },
                                         new String[]{ GSON.toJson(filePath),
                                                       GSON.toJson(contentToAdd) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static String
            getFilePermissions( String sessionId, int resourceId,
                                String fileName ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                                  NoSuchComponentException, ClassNotFoundException,
                                                  InstantiationException, IllegalAccessException,
                                                  IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations get File Permissions",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(fileName) });

        return (String) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static String
            getFileGroup( String sessionId, int resourceId,
                          String fileName ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                            NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                            IllegalAccessException, IllegalArgumentException,
                                            InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations get File Group",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(fileName) });

        return (String) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static long
            getFileGID( String sessionId, int resourceId,
                        String fileName ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                          NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                          IllegalAccessException, IllegalArgumentException,
                                          InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations get File G I D",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(fileName) });

        return (long) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static String
            getFileOwner( String sessionId, int resourceId,
                          String fileName ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                            NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                            IllegalAccessException, IllegalArgumentException,
                                            InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations get File Owner",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(fileName) });

        return (String) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static long
            getFileUID( String sessionId, int resourceId,
                        String fileName ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                          NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                          IllegalAccessException, IllegalArgumentException,
                                          InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations get File U I D",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(fileName) });

        return (long) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static long getFileModificationTime( String sessionId, int resourceId,
                                                             String fileName ) throws NoSuchActionException,
                                                                               NoCompatibleMethodFoundException,
                                                                               NoSuchComponentException,
                                                                               ClassNotFoundException,
                                                                               InstantiationException,
                                                                               IllegalAccessException,
                                                                               IllegalArgumentException,
                                                                               InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations get File Modification Time",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(fileName) });

        return (long) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static long getFileSize( String sessionId, int resourceId,
                                                 String fileName ) throws NoSuchActionException,
                                                                   NoCompatibleMethodFoundException,
                                                                   NoSuchComponentException, ClassNotFoundException,
                                                                   InstantiationException, IllegalAccessException,
                                                                   IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations get File Size",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(fileName) });

        return (long) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static String
            getFileUniqueID( String sessionId, int resourceId,
                             String fileName ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                               NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                               IllegalAccessException, IllegalArgumentException,
                                               InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations get File Unique Id",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(fileName) });

        return (String) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static void setFileUID( String sessionId, int resourceId, String fileName,
                                                long uid ) throws NoSuchActionException,
                                                           NoCompatibleMethodFoundException,
                                                           NoSuchComponentException, ClassNotFoundException,
                                                           InstantiationException,
                                                           IllegalAccessException, IllegalArgumentException,
                                                           InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations set File U I D",
                                         new String[]{ String.class.getName(), long.class.getName() },
                                         new String[]{ GSON.toJson(fileName), GSON.toJson(uid) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void setFileGID( String sessionId, int resourceId, String fileName,
                                                long gid ) throws NoSuchActionException,
                                                           NoCompatibleMethodFoundException,
                                                           NoSuchComponentException, ClassNotFoundException,
                                                           InstantiationException,
                                                           IllegalAccessException, IllegalArgumentException,
                                                           InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations set File G I D",
                                         new String[]{ String.class.getName(), long.class.getName() },
                                         new String[]{ GSON.toJson(fileName), GSON.toJson(gid) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void setFilePermissions( String sessionId, int resourceId, String fileName,
                                                        String permissions ) throws NoSuchActionException,
                                                                             NoCompatibleMethodFoundException,
                                                                             NoSuchComponentException,
                                                                             ClassNotFoundException,
                                                                             InstantiationException,
                                                                             IllegalAccessException,
                                                                             IllegalArgumentException,
                                                                             InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations set File Permissions",
                                         new String[]{ String.class.getName(), String.class.getName() },
                                         new String[]{ GSON.toJson(fileName), GSON.toJson(permissions) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void setFileModicationTime( String sessionId, int resourceId, String fileName,
                                                           long modificationTime ) throws NoSuchActionException,
                                                                                   NoCompatibleMethodFoundException,
                                                                                   NoSuchComponentException,
                                                                                   ClassNotFoundException,
                                                                                   InstantiationException,
                                                                                   IllegalAccessException,
                                                                                   IllegalArgumentException,
                                                                                   InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations set File Modification Time",
                                         new String[]{ String.class.getName(), long.class.getName() },
                                         new String[]{ GSON.toJson(fileName), GSON.toJson(modificationTime) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void setFileHiddenAttribute( String sessionId, int resourceId, String fileName,
                                                            boolean hidden ) throws NoSuchActionException,
                                                                             NoCompatibleMethodFoundException,
                                                                             NoSuchComponentException,
                                                                             ClassNotFoundException,
                                                                             InstantiationException,
                                                                             IllegalAccessException,
                                                                             IllegalArgumentException,
                                                                             InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations set File Hidden Attribute",
                                         new String[]{ String.class.getName(), boolean.class.getName() },
                                         new String[]{ GSON.toJson(fileName), GSON.toJson(hidden) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static boolean
            doesFileExists( String sessionId, int resourceId,
                            String fileName ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                              NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                              IllegalAccessException, IllegalArgumentException,
                                              InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations does File Exist",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(fileName) });

        return (boolean) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static void
            createFile( String sessionId, int resourceId, String fileName, String fileContent, long fileSize,
                        long uid, long gid, String eol,
                        boolean isRandomContent,
                        boolean isBinary ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                           NoSuchComponentException, ClassNotFoundException,
                                           InstantiationException, IllegalAccessException,
                                           IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations Create File",
                                         new String[]{ String.class.getName(),
                                                       String.class.getName(),
                                                       long.class.getName(),
                                                       long.class.getName(),
                                                       long.class.getName(),
                                                       String.class.getName(),
                                                       boolean.class.getName(),
                                                       boolean.class.getName() },
                                         new String[]{ GSON.toJson(fileName),
                                                       GSON.toJson(fileContent),
                                                       GSON.toJson(fileSize),
                                                       GSON.toJson(uid),
                                                       GSON.toJson(gid),
                                                       GSON.toJson(eol),
                                                       GSON.toJson(isRandomContent),
                                                       GSON.toJson(isBinary)
                                         });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void
            deleteFile( String sessionId, int resourceId,
                        String fileName ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                          NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                          IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations delete File",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(fileName) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void renameFile( String sessionId, int resourceId, String oldFileName,
                                                String newFileName,
                                                boolean overwrite ) throws NoSuchActionException,
                                                                    NoCompatibleMethodFoundException,
                                                                    NoSuchComponentException, ClassNotFoundException,
                                                                    InstantiationException, IllegalAccessException,
                                                                    IllegalArgumentException,
                                                                    InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations rename File",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       boolean.class.getName() },
                                         new String[]{ GSON.toJson(oldFileName), GSON.toJson(newFileName),
                                                       GSON.toJson(overwrite) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static String[]
            getLastLines( String sessionId, int resourceId, String fileName, int numberOfLines,
                          String charset ) throws NoSuchActionException,
                                           NoCompatibleMethodFoundException,
                                           NoSuchComponentException, ClassNotFoundException,
                                           InstantiationException, IllegalAccessException,
                                           IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations get Last Lines",
                                         new String[]{ String.class.getName(), int.class.getName(),
                                                       String.class.getName() },
                                         new String[]{ GSON.toJson(fileName), GSON.toJson(numberOfLines),
                                                       GSON.toJson(charset) });

        return (String[]) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static String readFile( String sessionId, int resourceId, String fileName,
                                                String fileEncoding ) throws NoSuchActionException,
                                                                      NoCompatibleMethodFoundException,
                                                                      NoSuchComponentException, ClassNotFoundException,
                                                                      InstantiationException, IllegalAccessException,
                                                                      IllegalArgumentException,
                                                                      InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations read File",
                                         new String[]{ String.class.getName(), String.class.getName() },
                                         new String[]{ GSON.toJson(fileName), GSON.toJson(fileEncoding) });

        return (String) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static FileTailInfo readFileFromPosition( String sessionId, int resourceId, String fileName,
                                                                  long fromBytePosition ) throws NoSuchActionException,
                                                                                          NoCompatibleMethodFoundException,
                                                                                          NoSuchComponentException,
                                                                                          ClassNotFoundException,
                                                                                          InstantiationException,
                                                                                          IllegalAccessException,
                                                                                          IllegalArgumentException,
                                                                                          InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations read File From Position",
                                         new String[]{ String.class.getName(), long.class.getName() },
                                         new String[]{ GSON.toJson(fileName), GSON.toJson(fromBytePosition) });

        return (FileTailInfo) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static String computeMd5Sum( String sessionId, int resourceId, String fileName,
                                                     String md5SumMode ) throws NoSuchActionException,
                                                                         NoCompatibleMethodFoundException,
                                                                         NoSuchComponentException,
                                                                         ClassNotFoundException,
                                                                         InstantiationException,
                                                                         IllegalAccessException,
                                                                         IllegalArgumentException,
                                                                         InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations compute Md5 Sum",
                                         new String[]{ String.class.getName(), String.class.getName() },
                                         new String[]{ GSON.toJson(fileName), GSON.toJson(md5SumMode) });

        return (String) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static void replaceText( String sessionId, int resourceId, String fileName,
                                                 Map<String, String> searchTokens,
                                                 boolean isRegex ) throws NoSuchActionException,
                                                                   NoCompatibleMethodFoundException,
                                                                   NoSuchComponentException, ClassNotFoundException,
                                                                   InstantiationException, IllegalAccessException,
                                                                   IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations replace Text In File",
                                         new String[]{ String.class.getName(), Map.class.getName(),
                                                       boolean.class.getName() },
                                         new String[]{ GSON.toJson(fileName), GSON.toJson(searchTokens),
                                                       GSON.toJson(isRegex) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void replaceText( String sessionId, int resourceId, String fileName, String searchString,
                                                 String newString,
                                                 boolean isRegex ) throws NoSuchActionException,
                                                                   NoCompatibleMethodFoundException,
                                                                   NoSuchComponentException, ClassNotFoundException,
                                                                   InstantiationException, IllegalAccessException,
                                                                   IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations replace Text In File",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       String.class.getName(), boolean.class.getName() },
                                         new String[]{ GSON.toJson(fileName), GSON.toJson(searchString),
                                                       GSON.toJson(newString), GSON.toJson(isRegex) });

        ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static FileMatchInfo
            findTextAfterGivenPositionInFile( String sessionId, int resourceId, String fileName,
                                              String[] searchTexts, boolean isRegex,
                                              long searchFromPosition,
                                              int currentLineNumber ) throws NoSuchActionException,
                                                                      NoCompatibleMethodFoundException,
                                                                      NoSuchComponentException, ClassNotFoundException,
                                                                      InstantiationException, IllegalAccessException,
                                                                      IllegalArgumentException,
                                                                      InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations find Text In File After Given Position",
                                         new String[]{ String.class.getName(), String[].class.getName(),
                                                       boolean.class.getName(), long.class.getName(),
                                                       int.class.getName() },
                                         new String[]{ GSON.toJson(fileName), GSON.toJson(searchTexts),
                                                       GSON.toJson(isRegex),
                                                       GSON.toJson(searchFromPosition),
                                                       GSON.toJson(currentLineNumber) });

        return (FileMatchInfo) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static String[] fileGrep( String sessionId, int resourceId, String fileName,
                                                  String searchPattern,
                                                  boolean isSimpleMode ) throws NoSuchActionException,
                                                                         NoCompatibleMethodFoundException,
                                                                         NoSuchComponentException,
                                                                         ClassNotFoundException, InstantiationException,
                                                                         IllegalAccessException,
                                                                         IllegalArgumentException,
                                                                         InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations file Grep",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       boolean.class.getName() },
                                         new String[]{ GSON.toJson(fileName), GSON.toJson(searchPattern),
                                                       GSON.toJson(isSimpleMode) });

        return (String[]) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static void
            lockFile( String sessionId, int resourceId,
                      String fileName ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                        NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                        IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations lock File",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(fileName) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void
            unlockFile( String sessionId, int resourceId,
                        String fileName ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                          NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                          IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations unlock File",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(fileName) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void unzipFile( String sessionId, int resourceId, String zipFilePath,
                                               String outputDirPath ) throws NoSuchActionException,
                                                                      NoCompatibleMethodFoundException,
                                                                      NoSuchComponentException, ClassNotFoundException,
                                                                      InstantiationException, IllegalAccessException,
                                                                      IllegalArgumentException,
                                                                      InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations unzip",
                                         new String[]{ String.class.getName(), String.class.getName() },
                                         new String[]{ GSON.toJson(zipFilePath), GSON.toJson(outputDirPath) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void extractFile( String sessionId, int resourceId, String archiveFilePath,
                                                 String outputDirPath ) throws NoSuchActionException,
                                                                        NoCompatibleMethodFoundException,
                                                                        NoSuchComponentException,
                                                                        ClassNotFoundException, InstantiationException,
                                                                        IllegalAccessException,
                                                                        IllegalArgumentException,
                                                                        InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations extract",
                                         new String[]{ String.class.getName(), String.class.getName() },
                                         new String[]{ GSON.toJson(archiveFilePath), GSON.toJson(outputDirPath) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void
            sendFileTo( String sessionId, int resourceId, String fromFileName, String toFileName,
                        String machineIP, int port,
                        boolean failOnError ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                              NoSuchComponentException, ClassNotFoundException,
                                              InstantiationException, IllegalAccessException,
                                              IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations send File To",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       String.class.getName(), int.class.getName(),
                                                       boolean.class.getName() },
                                         new String[]{ GSON.toJson(fromFileName), GSON.toJson(toFileName),
                                                       GSON.toJson(machineIP), GSON.toJson(port),
                                                       GSON.toJson(failOnError) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static String constructDestinationFilePath( String sessionId, int resourceId,
                                                                    String srcFileName,
                                                                    String dstFilePath ) throws NoSuchActionException,
                                                                                         NoCompatibleMethodFoundException,
                                                                                         NoSuchComponentException,
                                                                                         ClassNotFoundException,
                                                                                         InstantiationException,
                                                                                         IllegalAccessException,
                                                                                         IllegalArgumentException,
                                                                                         InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations construct destination file path",
                                         new String[]{ String.class.getName(), String.class.getName() },
                                         new String[]{ GSON.toJson(srcFileName), GSON.toJson(dstFilePath) });

        return (String) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static void setCopyPortRange( String sessionId, int resourceId, int copyFileStartPort,
                                                      int copyFileEndPort ) throws NoSuchActionException,
                                                                            NoCompatibleMethodFoundException,
                                                                            NoSuchComponentException,
                                                                            ClassNotFoundException,
                                                                            InstantiationException,
                                                                            IllegalAccessException,
                                                                            IllegalArgumentException,
                                                                            InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations set Copy File Port Range",
                                         new String[]{ int.class.getName(), int.class.getName() },
                                         new String[]{ GSON.toJson(copyFileStartPort), GSON.toJson(copyFileEndPort) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static int
            openTransferSocket( String sessionId, int resourceId ) throws NoSuchActionException,
                                                                   NoCompatibleMethodFoundException,
                                                                   NoSuchComponentException, ClassNotFoundException,
                                                                   InstantiationException, IllegalAccessException,
                                                                   IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations open File Transfer Socket",
                                         new String[]{},
                                         new String[]{});

        return (int) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static void
            waitForTransferToComplete( String sessionId, int resourceId,
                                       int port ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                                  NoSuchComponentException, ClassNotFoundException,
                                                  InstantiationException, IllegalAccessException,
                                                  IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations wait For File Transfer Completion",
                                         new String[]{ int.class.getName() },
                                         new String[]{ GSON.toJson(port) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static String[]
            findFiles( String sessionId, int resourceId, String location, String searchString,
                       boolean isRegex, boolean acceptDirectories,
                       boolean recursiveSearch ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                                 NoSuchComponentException, ClassNotFoundException,
                                                 InstantiationException, IllegalAccessException,
                                                 IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations find Files",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       boolean.class.getName(), boolean.class.getName(),
                                                       boolean.class.getName() },
                                         new String[]{ GSON.toJson(location), GSON.toJson(searchString),
                                                       GSON.toJson(isRegex),
                                                       GSON.toJson(acceptDirectories),
                                                       GSON.toJson(recursiveSearch) });

        return (String[]) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static boolean
            doesDirectoryExists( String sessionId, int resourceId,
                                 String dirName ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                                  NoSuchComponentException, ClassNotFoundException,
                                                  InstantiationException,
                                                  IllegalAccessException, IllegalArgumentException,
                                                  InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations does Directory Exist",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(dirName) });

        return (boolean) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static void
            createDirectory( String sessionId, int resourceId, String directoryName,
                             long uid, long gid ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                                  NoSuchComponentException, ClassNotFoundException,
                                                  InstantiationException, IllegalAccessException,
                                                  IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations Create Directory",
                                         new String[]{ String.class.getName(),
                                                       long.class.getName(),
                                                       long.class.getName() },
                                         new String[]{ GSON.toJson(directoryName),
                                                       GSON.toJson(uid),
                                                       GSON.toJson(gid)
                                         });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void
            deleteDirectory( String sessionId, int resourceId,
                             String directoryName,
                             boolean deleteRecursively ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                                         NoSuchComponentException, ClassNotFoundException,
                                                         InstantiationException,
                                                         IllegalAccessException, IllegalArgumentException,
                                                         InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations delete Directory",
                                         new String[]{ String.class.getName(), boolean.class.getName() },
                                         new String[]{ GSON.toJson(directoryName), GSON.toJson(deleteRecursively) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void
            purgeDirectoryContent( String sessionId, int resourceId,
                                   String directoryName ) throws NoSuchActionException,
                                                          NoCompatibleMethodFoundException,
                                                          NoSuchComponentException, ClassNotFoundException,
                                                          InstantiationException,
                                                          IllegalAccessException, IllegalArgumentException,
                                                          InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations purge Directory Contents",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(directoryName) });

        ResourcesManager.executeOverResource(pojo);

    }

    public static void sendDirectoryTo( String sessionId, int resourceId, String fromDirName, String toDirName,
                                        String machineIP, int port, boolean isRecursive,
                                        boolean failOnError ) throws NoSuchActionException,
                                                              NoCompatibleMethodFoundException,
                                                              NoSuchComponentException, ClassNotFoundException,
                                                              InstantiationException, IllegalAccessException,
                                                              IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations send Directory To",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       String.class.getName(), int.class.getName(),
                                                       boolean.class.getName(), boolean.class.getName() },
                                         new String[]{ GSON.toJson(fromDirName),
                                                       GSON.toJson(toDirName),
                                                       GSON.toJson(machineIP),
                                                       GSON.toJson(port),
                                                       GSON.toJson(isRecursive),
                                                       GSON.toJson(failOnError) });

        ResourcesManager.executeOverResource(pojo);

    }

    public static void copyFileLocally( String sessionId, int resourceId, String fromFileName, String toFileName,
                                        boolean failOnError ) throws NoSuchActionException,
                                                              NoCompatibleMethodFoundException,
                                                              NoSuchComponentException, ClassNotFoundException,
                                                              InstantiationException, IllegalAccessException,
                                                              IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations Copy Directory Locally",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       boolean.class.getName() },
                                         new String[]{ GSON.toJson(fromFileName),
                                                       GSON.toJson(toFileName),
                                                       GSON.toJson(failOnError) });

        ResourcesManager.executeOverResource(pojo);

    }

    public static void copyDirectoryLocally( String sessionId, int resourceId, String fromFileName, String toFileName,
                                             boolean failOnError, boolean isRecursive ) throws NoSuchActionException,
                                                                                        NoCompatibleMethodFoundException,
                                                                                        NoSuchComponentException,
                                                                                        ClassNotFoundException,
                                                                                        InstantiationException,
                                                                                        IllegalAccessException,
                                                                                        IllegalArgumentException,
                                                                                        InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal File System Operations Copy Directory Locally",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       boolean.class.getName(),
                                                       boolean.class.getName() },
                                         new String[]{ GSON.toJson(fromFileName),
                                                       GSON.toJson(toFileName),
                                                       GSON.toJson(isRecursive),
                                                       GSON.toJson(failOnError) });

        ResourcesManager.executeOverResource(pojo);

    }

}
