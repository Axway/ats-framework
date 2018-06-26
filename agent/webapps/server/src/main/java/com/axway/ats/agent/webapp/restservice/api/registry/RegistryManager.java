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
package com.axway.ats.agent.webapp.restservice.api.registry;

import java.lang.reflect.InvocationTargetException;

import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.agent.webapp.restservice.api.ResourcesManager;
import com.axway.ats.agent.webapp.restservice.api.actions.ActionPojo;
import com.google.gson.Gson;

public class RegistryManager {

    private static final int    UNINITIALIZED_RESOURCE_ID = -1;
    private static final String COMPONENT_NAME            = "auto-system-operations";

    private static final Gson   GSON                      = new Gson();

    public synchronized static int initialize( String sessionId ) throws NoSuchActionException,
                                                                  NoCompatibleMethodFoundException,
                                                                  NoSuchComponentException, ClassNotFoundException,
                                                                  InstantiationException, IllegalAccessException,
                                                                  IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, UNINITIALIZED_RESOURCE_ID, COMPONENT_NAME,
                                         "Internal Registry Operations initialize",
                                         null,
                                         null);

        // create only the action class instance first
        int resourceId = ResourcesManager.initializeResource(pojo);
        pojo.setResourceId(resourceId);

        // then execute the action method
        ResourcesManager.executeOverResource(pojo);

        return resourceId;
    }

    public synchronized static void
            createPath( String sessionId, int resourceId, String rootKey,
                        String keyPath ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                         NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                         IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "Internal Registry Operations Create Path",
                                         new String[]{ String.class.getName(), String.class.getName() },
                                         new String[]{ GSON.toJson(rootKey), GSON.toJson(keyPath) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void
            deleteKey( String sessionId, int resourceId, String rootKey, String keyPath,
                       String keyName ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                        NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                        IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "Internal Registry Operations Delete Key",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       String.class.getName() },
                                         new String[]{ GSON.toJson(rootKey), GSON.toJson(keyPath),
                                                       GSON.toJson(keyName) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static boolean
            isKeyPresent( String sessionId, int resourceId, String rootKey, String keyPath,
                          String keyName ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                           NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                           IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "Internal Registry Operations Is Key Present",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       String.class.getName() },
                                         new String[]{ GSON.toJson(rootKey), GSON.toJson(keyPath),
                                                       GSON.toJson(keyName) });

        return (boolean) ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static String getStringValue( String sessionId, int resourceId, String rootKey, String keyPath,
                                                      String keyName ) throws NoSuchActionException,
                                                                       NoCompatibleMethodFoundException,
                                                                       NoSuchComponentException, ClassNotFoundException,
                                                                       InstantiationException, IllegalAccessException,
                                                                       IllegalArgumentException,
                                                                       InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "Internal Registry Operations Get String Value",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       String.class.getName() },
                                         new String[]{ GSON.toJson(rootKey), GSON.toJson(keyPath),
                                                       GSON.toJson(keyName) });

        return (String) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static int getIntValue( String sessionId, int resourceId, String rootKey, String keyPath,
                                                String keyName ) throws NoSuchActionException,
                                                                 NoCompatibleMethodFoundException,
                                                                 NoSuchComponentException, ClassNotFoundException,
                                                                 InstantiationException, IllegalAccessException,
                                                                 IllegalArgumentException,
                                                                 InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "Internal Registry Operations Get Int Value",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       String.class.getName() },
                                         new String[]{ GSON.toJson(rootKey), GSON.toJson(keyPath),
                                                       GSON.toJson(keyName) });

        return (int) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static long getLongValue( String sessionId, int resourceId, String rootKey, String keyPath,
                                                  String keyName ) throws NoSuchActionException,
                                                                   NoCompatibleMethodFoundException,
                                                                   NoSuchComponentException, ClassNotFoundException,
                                                                   InstantiationException, IllegalAccessException,
                                                                   IllegalArgumentException,
                                                                   InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "Internal Registry Operations Get Long Value",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       String.class.getName() },
                                         new String[]{ GSON.toJson(rootKey), GSON.toJson(keyPath),
                                                       GSON.toJson(keyName) });

        return (long) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static byte[] getBinaryValue( String sessionId, int resourceId, String rootKey, String keyPath,
                                                      String keyName ) throws NoSuchActionException,
                                                                       NoCompatibleMethodFoundException,
                                                                       NoSuchComponentException, ClassNotFoundException,
                                                                       InstantiationException, IllegalAccessException,
                                                                       IllegalArgumentException,
                                                                       InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "Internal Registry Operations Get Binary Value",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       String.class.getName() },
                                         new String[]{ GSON.toJson(rootKey), GSON.toJson(keyPath),
                                                       GSON.toJson(keyName) });

        return (byte[]) ResourcesManager.executeOverResource(pojo);
    }

    public synchronized static void
            setStringValue( String sessionId, int resourceId, String rootKey, String keyPath, String keyName,
                            String keyValue ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                              NoSuchComponentException, ClassNotFoundException,
                                              InstantiationException, IllegalAccessException,
                                              IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "Internal Registry Operations Set String Value",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       String.class.getName(), String.class.getName() },
                                         new String[]{ GSON.toJson(rootKey), GSON.toJson(keyPath),
                                                       GSON.toJson(keyName), GSON.toJson(keyValue) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void
            setIntValue( String sessionId, int resourceId, String rootKey, String keyPath, String keyName,
                         int keyValue ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                        NoSuchComponentException, ClassNotFoundException,
                                        InstantiationException, IllegalAccessException,
                                        IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "Internal Registry Operations Set Int Value",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       String.class.getName(), int.class.getName() },
                                         new String[]{ GSON.toJson(rootKey), GSON.toJson(keyPath),
                                                       GSON.toJson(keyName), GSON.toJson(keyValue) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void
            setLongValue( String sessionId, int resourceId, String rootKey, String keyPath, String keyName,
                          long keyValue ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                          NoSuchComponentException, ClassNotFoundException,
                                          InstantiationException, IllegalAccessException,
                                          IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "Internal Registry Operations Set Long Value",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       String.class.getName(), long.class.getName() },
                                         new String[]{ GSON.toJson(rootKey), GSON.toJson(keyPath),
                                                       GSON.toJson(keyName), GSON.toJson(keyValue) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void
            setBinaryValue( String sessionId, int resourceId, String rootKey, String keyPath, String keyName,
                            byte[] keyValue ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                              NoSuchComponentException, ClassNotFoundException,
                                              InstantiationException, IllegalAccessException,
                                              IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "Internal Registry Operations Set Binary Value",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       String.class.getName(), byte[].class.getName() },
                                         new String[]{ GSON.toJson(rootKey), GSON.toJson(keyPath),
                                                       GSON.toJson(keyName), GSON.toJson(keyValue) });

        ResourcesManager.executeOverResource(pojo);

    }

}
