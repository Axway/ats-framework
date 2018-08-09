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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p> This class keeps all of resources on the agent.</p>
 * <p> Resource on the ATS agent means Action class instance or other Java objects (like RestSystemMonitor)</p>
 * **/
public class ResourcesRepository {

    /**
     * Map that keeps the resources for each caller
     * < callerId, < resourceId, resource > >
     * */
    private static Map<String, Map<Integer, Object>> resourcesMap = Collections.synchronizedMap(new HashMap<String, Map<Integer, Object>>());
    private static int                               resourceId   = -1;

    private static final ResourcesRepository         instance     = new ResourcesRepository();

    public synchronized static ResourcesRepository getInstance() {

        return instance;
    }

    public synchronized int putResource( String callerId, Object resource ) {

        Map<Integer, Object> map = resourcesMap.get(callerId);
        if (map == null) {
            map = new HashMap<>();
        }
        map.put(++resourceId, resource);
        resourcesMap.put(callerId, map);
        return resourceId;
    }

    public synchronized Object getResource( String callerId, int resourceId ) {

        return resourcesMap.get(callerId).get(resourceId);
    }

    public synchronized Object deleteResource( String callerId, int resourceId ) {

        return resourcesMap.get(callerId).remove(resourceId);
    }

    /**
     * Delete all resources created by the provided caller
     * 
     * @param callerId
     * 
     * @return the deleted caller's resources
     * */
    public synchronized Map<Integer, Object> deleteAllCallerResources( String callerId ) {

        return resourcesMap.remove(callerId);
    }

}
