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
package com.axway.ats.agent.webapp.restservice.api.system.input;

import java.lang.reflect.InvocationTargetException;

import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.agent.webapp.restservice.api.ResourcesManager;
import com.axway.ats.agent.webapp.restservice.api.actions.ActionPojo;
import com.google.gson.Gson;

public class SystemInputManager {

    private static final int    UNINITIALIZED_RESOURCE_ID = -1;
    private static final String COMPONENT_NAME            = "auto-system-operations";

    private static final Gson   GSON                      = new Gson();

    public synchronized static int initialize( String sessionId ) throws NoSuchActionException,
                                                                  NoCompatibleMethodFoundException,
                                                                  NoSuchComponentException, ClassNotFoundException,
                                                                  InstantiationException, IllegalAccessException,
                                                                  IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, UNINITIALIZED_RESOURCE_ID, COMPONENT_NAME,
                                         "Internal System Input Operations initialize",
                                         null,
                                         null);

        // create only the action class instance first
        int resourceId = ResourcesManager.initializeResource(pojo);
        pojo.setResourceId(resourceId);

        // then execute the action method
        ResourcesManager.executeOverResource(pojo);

        return resourceId;
    }

    public synchronized static void clickAt( String sessionId, int resourceId, int x,
                                             int y ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                                     NoSuchComponentException, ClassNotFoundException,
                                                     InstantiationException, IllegalAccessException,
                                                     IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal System Input Operations click At",
                                         new String[]{ int.class.getName(), int.class.getName() },
                                         new String[]{ GSON.toJson(x), GSON.toJson(y) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void
            type( String sessionId, int resourceId,
                  int[] keyCodes ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                   NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                   IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal System Input Operations type",
                                         new String[]{ int[].class.getName() },
                                         new String[]{ GSON.toJson(keyCodes) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void type( String sessionId, int resourceId,
                                          String text ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                                        NoSuchComponentException, ClassNotFoundException,
                                                        InstantiationException, IllegalAccessException,
                                                        IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal System Input Operations type",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(text) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void
            type( String sessionId, int resourceId, String text,
                  int[] keyCodes ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                   NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                   IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal System Input Operations type",
                                         new String[]{ String.class.getName(), int[].class.getName() },
                                         new String[]{ GSON.toJson(text), GSON.toJson(keyCodes) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void
            keyPress( String sessionId, int resourceId,
                      int keyCode ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                    NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                    IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal System Input Operations key Press",
                                         new String[]{ int.class.getName() },
                                         new String[]{ GSON.toJson(keyCode) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void
            keyRelease( String sessionId, int resourceId,
                        int keyCode ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                      NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                      IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal System Input Operations key Release",
                                         new String[]{ int.class.getName() },
                                         new String[]{ GSON.toJson(keyCode) });

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void
            pressAltF4( String sessionId,
                        int resourceId ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                         NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                         IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal System Input Operations press Alt F4",
                                         new String[]{},
                                         new String[]{});

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void
            pressEsc( String sessionId,
                      int resourceId ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                       NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                       IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal System Input Operations press Esc",
                                         new String[]{},
                                         new String[]{});

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void
            pressEnter( String sessionId,
                        int resourceId ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                         NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                         IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal System Input Operations press Enter",
                                         new String[]{},
                                         new String[]{});

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void
            pressTab( String sessionId,
                      int resourceId ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                       NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                       IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal System Input Operations press Tab",
                                         new String[]{},
                                         new String[]{});

        ResourcesManager.executeOverResource(pojo);

    }

    public synchronized static void
            pressSpace( String sessionId,
                        int resourceId ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                         NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                         IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal System Input Operations press Space",
                                         new String[]{},
                                         new String[]{});

        ResourcesManager.executeOverResource(pojo);

    }

}
