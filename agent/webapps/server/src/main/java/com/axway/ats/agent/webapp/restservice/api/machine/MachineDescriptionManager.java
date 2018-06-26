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
package com.axway.ats.agent.webapp.restservice.api.machine;

import java.lang.reflect.InvocationTargetException;

import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.agent.webapp.restservice.api.ResourcesManager;
import com.axway.ats.agent.webapp.restservice.api.actions.ActionPojo;

public class MachineDescriptionManager {

    private static final int    UNINITIALIZED_RESOURCE_ID = -1;
    private static final String COMPONENT_NAME            = "auto-system-operations";

    public synchronized static int initialize( String sessionId ) throws NoSuchActionException,
                                                                  NoCompatibleMethodFoundException,
                                                                  NoSuchComponentException, ClassNotFoundException,
                                                                  InstantiationException, IllegalAccessException,
                                                                  IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, UNINITIALIZED_RESOURCE_ID, COMPONENT_NAME,
                                         "Machine Description Operations Get Description",
                                         null,
                                         null);

        // create only the action class instance first
        int resourceId = ResourcesManager.initializeResource(pojo);
        pojo.setResourceId(resourceId);

        return resourceId;
    }

    public synchronized static String
            getDescription( String sessionId, int resourceId ) throws NoSuchActionException,
                                                               NoCompatibleMethodFoundException,
                                                               NoSuchComponentException, ClassNotFoundException,
                                                               InstantiationException, IllegalAccessException,
                                                               IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "Machine Description Operations Get Description",
                                         null,
                                         null);

        return (String) ResourcesManager.executeOverResource(pojo);
    }

}
