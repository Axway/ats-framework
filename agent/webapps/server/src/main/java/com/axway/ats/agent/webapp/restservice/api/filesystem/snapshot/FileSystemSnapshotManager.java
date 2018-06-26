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

import java.lang.reflect.InvocationTargetException;

import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.agent.webapp.restservice.api.ResourcesManager;
import com.axway.ats.agent.webapp.restservice.api.actions.ActionPojo;
import com.axway.ats.core.filesystem.snapshot.LocalFileSystemSnapshot;
import com.axway.ats.core.filesystem.snapshot.SnapshotConfiguration;
import com.google.gson.Gson;

public class FileSystemSnapshotManager {

    private static final int    UNINITIALIZED_RESOURCE_ID = -1;
    private static final String COMPONENT_NAME            = "auto-system-operations";

    private static final Gson   GSON                      = new Gson();

    public synchronized static int initFileSystemSnapshot( String sessionId, String name,
                                                           SnapshotConfiguration configuration ) throws NoSuchActionException,
                                                                                                 NoCompatibleMethodFoundException,
                                                                                                 NoSuchComponentException,
                                                                                                 ClassNotFoundException,
                                                                                                 InstantiationException,
                                                                                                 IllegalAccessException,
                                                                                                 IllegalArgumentException,
                                                                                                 InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, UNINITIALIZED_RESOURCE_ID, COMPONENT_NAME,
                                         "InternalFileSystemSnapshot init File System Snapshot",
                                         new String[]{ String.class.getName(), SnapshotConfiguration.class.getName() },
                                         new String[]{ GSON.toJson(name), GSON.toJson(configuration) });

        // create only the action class instance first
        int resourceId = ResourcesManager.initializeResource(pojo);
        pojo.setResourceId(resourceId);

        // then execute the action method
        ResourcesManager.executeOverResource(pojo);

        return resourceId;
    }

    public synchronized static void addDirectory( String sessionId, int resourceId, String directoryAlias,
                                                  String directoryPath ) throws NoSuchActionException,
                                                                         NoCompatibleMethodFoundException,
                                                                         NoSuchComponentException,
                                                                         ClassNotFoundException,
                                                                         InstantiationException,
                                                                         IllegalAccessException,
                                                                         IllegalArgumentException,
                                                                         InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "InternalFileSystemSnapshot add Directory",
                                         new String[]{ String.class.getName(), String.class.getName() },
                                         new String[]{ GSON.toJson(directoryAlias), GSON.toJson(directoryPath) });

        // execute the action method
        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void takeSnapshot( String sessionId, int resourceId ) throws NoSuchActionException,
                                                                                     NoCompatibleMethodFoundException,
                                                                                     NoSuchComponentException,
                                                                                     ClassNotFoundException,
                                                                                     InstantiationException,
                                                                                     IllegalAccessException,
                                                                                     IllegalArgumentException,
                                                                                     InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "InternalFileSystemSnapshot take Snapshot",
                                         new String[]{},
                                         new String[]{});

        // execute the action method
        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static LocalFileSystemSnapshot
            getFileSystemSnapshot( String sessionId,
                                   int resourceId ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                                    NoSuchComponentException, ClassNotFoundException,
                                                    InstantiationException, IllegalAccessException,
                                                    IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "InternalFileSystemSnapshot get File System Snapshot",
                                         new String[]{},
                                         new String[]{});

        // execute the action method
        return (LocalFileSystemSnapshot) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static void
            toFile( String sessionId, int resourceId,
                    String backupFile ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                        NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                        IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "InternalFileSystemSnapshot to File",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(backupFile) });

        // execute the action method
        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void loadFromFile( String sessionId, int resourceId,
                                                  String sourceFile ) throws NoSuchActionException,
                                                                      NoCompatibleMethodFoundException,
                                                                      NoSuchComponentException, ClassNotFoundException,
                                                                      InstantiationException, IllegalAccessException,
                                                                      IllegalArgumentException,
                                                                      InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "InternalFileSystemSnapshot load From File",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(sourceFile) });

        // execute the action method
        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void pushFileSystemSnapshot( String sessionId, int resourceId,
                                                            LocalFileSystemSnapshot newSnapshot ) throws NoSuchActionException,
                                                                                                  NoCompatibleMethodFoundException,
                                                                                                  NoSuchComponentException,
                                                                                                  ClassNotFoundException,
                                                                                                  InstantiationException,
                                                                                                  IllegalAccessException,
                                                                                                  IllegalArgumentException,
                                                                                                  InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "InternalFileSystemSnapshot push File System Snapshot",
                                         new String[]{ LocalFileSystemSnapshot.class.getName() },
                                         new String[]{ GSON.toJson(newSnapshot) });

        // execute the action method
        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void newFileSystemSnapshot( String sessionId, int resourceId,
                                                           LocalFileSystemSnapshot srcFileSystemSnapshot,
                                                           String newSnapshotName ) throws NoSuchActionException,
                                                                                    NoCompatibleMethodFoundException,
                                                                                    NoSuchComponentException,
                                                                                    ClassNotFoundException,
                                                                                    InstantiationException,
                                                                                    IllegalAccessException,
                                                                                    IllegalArgumentException,
                                                                                    InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "InternalFileSystemSnapshot new Snapshot",
                                         new String[]{ LocalFileSystemSnapshot.class.getName(),
                                                       String.class.getName() },
                                         new String[]{ GSON.toJson(srcFileSystemSnapshot),
                                                       GSON.toJson(newSnapshotName) });

        // execute the action method
        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void setName( String sessionId, int resourceId,
                                             String name ) throws NoSuchActionException,
                                                           NoCompatibleMethodFoundException,
                                                           NoSuchComponentException, ClassNotFoundException,
                                                           InstantiationException,
                                                           IllegalAccessException, IllegalArgumentException,
                                                           InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "InternalFileSystemSnapshot set Name",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(name) });

        // execute the action method
        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void checkFile( String sessionId, int resourceId, String rootDirectoryAlias,
                                               String relativeFilePath,
                                               int[] checkRules ) throws NoSuchActionException,
                                                                  NoCompatibleMethodFoundException,
                                                                  NoSuchComponentException, ClassNotFoundException,
                                                                  InstantiationException, IllegalAccessException,
                                                                  IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "InternalFileSystemSnapshot check File",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       int[].class.getName() },
                                         new String[]{ GSON.toJson(rootDirectoryAlias),
                                                       GSON.toJson(relativeFilePath),
                                                       GSON.toJson(checkRules) });

        // execute the action method
        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void skipDirectory( String sessionId, int resourceId, String rootDirectoryAlias,
                                                   String relativeDirectoryPath ) throws NoSuchActionException,
                                                                                  NoCompatibleMethodFoundException,
                                                                                  NoSuchComponentException,
                                                                                  ClassNotFoundException,
                                                                                  InstantiationException,
                                                                                  IllegalAccessException,
                                                                                  IllegalArgumentException,
                                                                                  InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "InternalFileSystemSnapshot skip Directory",
                                         new String[]{ String.class.getName(), String.class.getName()
                                         },
                                         new String[]{
                                                       GSON.toJson(rootDirectoryAlias),
                                                       GSON.toJson(relativeDirectoryPath) });

        // execute the action method
        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void skipDirectoryByRegex( String sessionId, int resourceId, String rootDirectoryAlias,
                                                          String relativeDirectoryPath ) throws NoSuchActionException,
                                                                                         NoCompatibleMethodFoundException,
                                                                                         NoSuchComponentException,
                                                                                         ClassNotFoundException,
                                                                                         InstantiationException,
                                                                                         IllegalAccessException,
                                                                                         IllegalArgumentException,
                                                                                         InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "InternalFileSystemSnapshot skip Directory By Regex",
                                         new String[]{ String.class.getName(), String.class.getName()
                                         },
                                         new String[]{
                                                       GSON.toJson(rootDirectoryAlias),
                                                       GSON.toJson(relativeDirectoryPath) });

        // execute the action method
        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void skipFile( String sessionId, int resourceId, String rootDirectoryAlias,
                                              String relativeFilePath, int[] skipRules ) throws NoSuchActionException,
                                                                                         NoCompatibleMethodFoundException,
                                                                                         NoSuchComponentException,
                                                                                         ClassNotFoundException,
                                                                                         InstantiationException,
                                                                                         IllegalAccessException,
                                                                                         IllegalArgumentException,
                                                                                         InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "InternalFileSystemSnapshot skip File",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       int[].class.getName()
                                         },
                                         new String[]{
                                                       GSON.toJson(rootDirectoryAlias),
                                                       GSON.toJson(relativeFilePath),
                                                       GSON.toJson(skipRules) });

        // execute the action method
        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void skipFileByRegex( String sessionId, int resourceId, String rootDirectoryAlias,
                                                     String relativeFilePath,
                                                     int[] skipRules ) throws NoSuchActionException,
                                                                       NoCompatibleMethodFoundException,
                                                                       NoSuchComponentException,
                                                                       ClassNotFoundException,
                                                                       InstantiationException,
                                                                       IllegalAccessException,
                                                                       IllegalArgumentException,
                                                                       InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "InternalFileSystemSnapshot skip File By Regex",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       int[].class.getName()
                                         },
                                         new String[]{
                                                       GSON.toJson(rootDirectoryAlias),
                                                       GSON.toJson(relativeFilePath),
                                                       GSON.toJson(skipRules) });

        // execute the action method
        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void skipTextLine( String sessionId, int resourceId, String rootDirectoryAlias,
                                                  String relativeFilePath, String line,
                                                  String matchType ) throws NoSuchActionException,
                                                                     NoCompatibleMethodFoundException,
                                                                     NoSuchComponentException, ClassNotFoundException,
                                                                     InstantiationException, IllegalAccessException,
                                                                     IllegalArgumentException,
                                                                     InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "InternalFileSystemSnapshot skip Text Line",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       String.class.getName(), String.class.getName() },
                                         new String[]{ GSON.toJson(rootDirectoryAlias),
                                                       GSON.toJson(relativeFilePath),
                                                       GSON.toJson(line),
                                                       GSON.toJson(matchType) });

        // execute the action method
        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void skipPropertyWithKey( String sessionId, int resourceId, String rootDirectoryAlias,
                                                         String relativeFilePath, String key,
                                                         String matchType ) throws NoSuchActionException,
                                                                            NoCompatibleMethodFoundException,
                                                                            NoSuchComponentException,
                                                                            ClassNotFoundException,
                                                                            InstantiationException,
                                                                            IllegalAccessException,
                                                                            IllegalArgumentException,
                                                                            InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "InternalFileSystemSnapshot skip Property With Key",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       String.class.getName(), String.class.getName() },
                                         new String[]{ GSON.toJson(rootDirectoryAlias),
                                                       GSON.toJson(relativeFilePath),
                                                       GSON.toJson(key),
                                                       GSON.toJson(matchType) });

        // execute the action method
        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void skipPropertyWithValue( String sessionId, int resourceId, String rootDirectoryAlias,
                                                           String relativeFilePath, String value,
                                                           String matchType ) throws NoSuchActionException,
                                                                              NoCompatibleMethodFoundException,
                                                                              NoSuchComponentException,
                                                                              ClassNotFoundException,
                                                                              InstantiationException,
                                                                              IllegalAccessException,
                                                                              IllegalArgumentException,
                                                                              InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "InternalFileSystemSnapshot skip Property With Value",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       String.class.getName(), String.class.getName() },
                                         new String[]{ GSON.toJson(rootDirectoryAlias),
                                                       GSON.toJson(relativeFilePath),
                                                       GSON.toJson(value),
                                                       GSON.toJson(matchType) });

        // execute the action method
        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void skipNodeByAttribute( String sessionId, int resourceId, String rootDirectoryAlias,
                                                         String relativeFilePath, String nodeXpath, String attributeKey,
                                                         String attributeValue,
                                                         String attributeValueMatchType ) throws NoSuchActionException,
                                                                                          NoCompatibleMethodFoundException,
                                                                                          NoSuchComponentException,
                                                                                          ClassNotFoundException,
                                                                                          InstantiationException,
                                                                                          IllegalAccessException,
                                                                                          IllegalArgumentException,
                                                                                          InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "InternalFileSystemSnapshot skip Node By Attribute",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       String.class.getName(), String.class.getName(),
                                                       String.class.getName(), String.class.getName() },
                                         new String[]{ GSON.toJson(rootDirectoryAlias),
                                                       GSON.toJson(relativeFilePath),
                                                       GSON.toJson(nodeXpath),
                                                       GSON.toJson(attributeKey),
                                                       GSON.toJson(attributeValue),
                                                       GSON.toJson(attributeValueMatchType) });

        // execute the action method
        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void skipNodeByValue( String sessionId, int resourceId, String rootDirectoryAlias,
                                                     String relativeFilePath, String nodeXpath,
                                                     String value,
                                                     String matchType ) throws NoSuchActionException,
                                                                        NoCompatibleMethodFoundException,
                                                                        NoSuchComponentException,
                                                                        ClassNotFoundException,
                                                                        InstantiationException,
                                                                        IllegalAccessException,
                                                                        IllegalArgumentException,
                                                                        InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "InternalFileSystemSnapshot skip Node By Value",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       String.class.getName(), String.class.getName(),
                                                       String.class.getName() },
                                         new String[]{ GSON.toJson(rootDirectoryAlias),
                                                       GSON.toJson(relativeFilePath),
                                                       GSON.toJson(nodeXpath),
                                                       GSON.toJson(value),
                                                       GSON.toJson(matchType) });

        // execute the action method
        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void skipIniSection( String sessionId, int resourceId, String rootDirectoryAlias,
                                                    String relativeFilePath, String section,
                                                    String matchType ) throws NoSuchActionException,
                                                                       NoCompatibleMethodFoundException,
                                                                       NoSuchComponentException, ClassNotFoundException,
                                                                       InstantiationException, IllegalAccessException,
                                                                       IllegalArgumentException,
                                                                       InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "InternalFileSystemSnapshot skip Ini Section",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       String.class.getName(), String.class.getName() },
                                         new String[]{ GSON.toJson(rootDirectoryAlias),
                                                       GSON.toJson(relativeFilePath),
                                                       GSON.toJson(section),
                                                       GSON.toJson(matchType) });

        // execute the action method
        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void skipIniPropertyWithKey( String sessionId, int resourceId, String rootDirectoryAlias,
                                                            String relativeFilePath, String section, String key,
                                                            String matchType ) throws NoSuchActionException,
                                                                               NoCompatibleMethodFoundException,
                                                                               NoSuchComponentException,
                                                                               ClassNotFoundException,
                                                                               InstantiationException,
                                                                               IllegalAccessException,
                                                                               IllegalArgumentException,
                                                                               InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "InternalFileSystemSnapshot skip Ini Property With Key",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       String.class.getName(), String.class.getName(),
                                                       String.class.getName() },
                                         new String[]{ GSON.toJson(rootDirectoryAlias),
                                                       GSON.toJson(relativeFilePath),
                                                       GSON.toJson(section),
                                                       GSON.toJson(key),
                                                       GSON.toJson(matchType) });

        // execute the action method
        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void skipIniPropertyWithValue( String sessionId, int resourceId,
                                                              String rootDirectoryAlias,
                                                              String relativeFilePath, String section, String value,
                                                              String matchType ) throws NoSuchActionException,
                                                                                 NoCompatibleMethodFoundException,
                                                                                 NoSuchComponentException,
                                                                                 ClassNotFoundException,
                                                                                 InstantiationException,
                                                                                 IllegalAccessException,
                                                                                 IllegalArgumentException,
                                                                                 InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "InternalFileSystemSnapshot skip Ini Property With Value",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       String.class.getName(), String.class.getName(),
                                                       String.class.getName() },
                                         new String[]{ GSON.toJson(rootDirectoryAlias),
                                                       GSON.toJson(relativeFilePath),
                                                       GSON.toJson(section),
                                                       GSON.toJson(value),
                                                       GSON.toJson(matchType) });

        // execute the action method
        ResourcesManager.executeOverResource(pojo);

    }

    public static String getDescription( String sessionId,
                                         int resourceId ) throws NoSuchActionException,
                                                          NoCompatibleMethodFoundException, NoSuchComponentException,
                                                          ClassNotFoundException, InstantiationException,
                                                          IllegalAccessException, IllegalArgumentException,
                                                          InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "InternalFileSystemSnapshot get Description",
                                         new String[]{},
                                         new String[]{});

        // execute the action method
        return (String) ResourcesManager.executeOverResource(pojo);
    }

}
