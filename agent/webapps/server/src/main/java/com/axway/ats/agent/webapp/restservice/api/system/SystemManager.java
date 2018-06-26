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
package com.axway.ats.agent.webapp.restservice.api.system;

import java.lang.reflect.InvocationTargetException;

import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.agent.webapp.restservice.api.ResourcesManager;
import com.axway.ats.agent.webapp.restservice.api.actions.ActionPojo;
import com.axway.ats.common.system.OperatingSystemType;
import com.google.gson.Gson;

public class SystemManager {

    private static final int    UNINITIALIZED_RESOURCE_ID = -1;
    private static final String COMPONENT_NAME            = "auto-system-operations";

    private static final Gson   GSON                      = new Gson();

    public synchronized static int initialize( String sessionId ) throws NoSuchActionException,
                                                                  NoCompatibleMethodFoundException,
                                                                  NoSuchComponentException, ClassNotFoundException,
                                                                  InstantiationException, IllegalAccessException,
                                                                  IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, UNINITIALIZED_RESOURCE_ID, COMPONENT_NAME,
                                         "Internal System Operations initialize",
                                         null,
                                         null);

        // create only the action class instance first
        int resourceId = ResourcesManager.initializeResource(pojo);
        pojo.setResourceId(resourceId);

        // then execute the action method
        ResourcesManager.executeOverResource(pojo);

        return resourceId;
    }

    public synchronized static OperatingSystemType
            getOsType( String sessionId,
                       int resourceId ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                        NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                        IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal System Operations get Operating System Type",
                                         new String[]{},
                                         new String[]{});

        return (OperatingSystemType) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static String
            getSystemProperty( String sessionId,
                               int resourceId, String propertyName ) throws NoSuchActionException,
                                                                     NoCompatibleMethodFoundException,
                                                                     NoSuchComponentException, ClassNotFoundException,
                                                                     InstantiationException,
                                                                     IllegalAccessException, IllegalArgumentException,
                                                                     InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal System Operations Get System Property",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(propertyName) });

        return (String) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static String
            getTime( String sessionId, int resourceId,
                     boolean inMilliseconds ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                              NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                              IllegalAccessException, IllegalArgumentException,
                                              InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal System Operations get Time",
                                         new String[]{ boolean.class.getName() },
                                         new String[]{ GSON.toJson(inMilliseconds) });

        return (String) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static String
            getAtsVersion( String sessionId, int resourceId ) throws NoSuchActionException,
                                                              NoCompatibleMethodFoundException,
                                                              NoSuchComponentException, ClassNotFoundException,
                                                              InstantiationException, IllegalAccessException,
                                                              IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal System Operations get Ats Version",
                                         new String[]{},
                                         new String[]{});

        return (String) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static String
            getHostName( String sessionId, int resourceId ) throws NoSuchActionException,
                                                            NoCompatibleMethodFoundException,
                                                            NoSuchComponentException, ClassNotFoundException,
                                                            InstantiationException, IllegalAccessException,
                                                            IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal System Operations get Hostname",
                                         new String[]{},
                                         new String[]{});

        return (String) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static String[]
            getClasspath( String sessionId, int resourceId ) throws NoSuchActionException,
                                                             NoCompatibleMethodFoundException,
                                                             NoSuchComponentException, ClassNotFoundException,
                                                             InstantiationException, IllegalAccessException,
                                                             IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal System Operations get Class Path",
                                         new String[]{},
                                         new String[]{});

        return (String[]) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static String[]
            getDuplicatedJars( String sessionId, int resourceId ) throws NoSuchActionException,
                                                                  NoCompatibleMethodFoundException,
                                                                  NoSuchComponentException, ClassNotFoundException,
                                                                  InstantiationException, IllegalAccessException,
                                                                  IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal System Operations get Duplicated Jars",
                                         new String[]{},
                                         new String[]{});

        return (String[]) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static void
            logClasspath( String sessionId, int resourceId ) throws NoSuchActionException,
                                                             NoCompatibleMethodFoundException,
                                                             NoSuchComponentException, ClassNotFoundException,
                                                             InstantiationException, IllegalAccessException,
                                                             IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal System Operations log Class Path",
                                         new String[]{},
                                         new String[]{});

        ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static void
            logDuplicatedJars( String sessionId, int resourceId ) throws NoSuchActionException,
                                                                  NoCompatibleMethodFoundException,
                                                                  NoSuchComponentException, ClassNotFoundException,
                                                                  InstantiationException, IllegalAccessException,
                                                                  IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal System Operations log Duplicated Jars",
                                         new String[]{},
                                         new String[]{});

        ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static void setTime( String sessionId, int resourceId, String timestamp,
                                             boolean inMilliseconds ) throws NoSuchActionException,
                                                                      NoCompatibleMethodFoundException,
                                                                      NoSuchComponentException, ClassNotFoundException,
                                                                      InstantiationException, IllegalAccessException,
                                                                      IllegalArgumentException,
                                                                      InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal System Operations set Time",
                                         new String[]{ String.class.getName(), boolean.class.getName() },
                                         new String[]{ GSON.toJson(timestamp), GSON.toJson(inMilliseconds) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static String
            createScreenshot( String sessionId, int resourceId,
                              String filePath ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                                NoSuchComponentException, ClassNotFoundException,
                                                InstantiationException, IllegalAccessException,
                                                IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal System Operations create Screenshot",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(filePath) });

        return (String) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static boolean isListening( String sessionId, int resourceId, String host, int port,
                                                    int timeout ) throws NoSuchActionException,
                                                                  NoCompatibleMethodFoundException,
                                                                  NoSuchComponentException, ClassNotFoundException,
                                                                  InstantiationException, IllegalAccessException,
                                                                  IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal System Operations is Listening",
                                         new String[]{ String.class.getName(), int.class.getName(),
                                                       int.class.getName() },
                                         new String[]{ GSON.toJson(host), GSON.toJson(port), GSON.toJson(timeout) });

        return (boolean) ResourcesManager.executeOverResource(pojo);
    }

}
