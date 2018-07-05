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

import java.util.HashMap;
import java.util.Map;

/**
 * <p> This class keeps all of resources on the agent.</p>
 * <p> Resource on the ATS agent means Action class instance</p>
 * **/
public class ResourcesRepository {

    /**
     * Map that keeps the resources for each session
     * < sessionId, < resourceId, resource > >
     * */
    private static Map<String, Map<Integer, Object>> resourcesMap = new HashMap<>();
    private static int                               resourceId   = -1;

    private static final ResourcesRepository         instance     = new ResourcesRepository();

    public synchronized static ResourcesRepository getInstance() {

        return instance;
    }

    public synchronized int putResource( String sessionId, Object resource ) {

        Map<Integer, Object> map = resourcesMap.get(sessionId);
        if (map == null) {
            map = new HashMap<>();
        }
        map.put(++resourceId, resource);
        resourcesMap.put(sessionId, map);
        return resourceId;
    }

    public synchronized Object getResource( String sessionId, int resourceId ) {

        return resourcesMap.get(sessionId).get(resourceId);
    }

    public synchronized Object deleteResource( String sessionId, int resourceId ) {

        return resourcesMap.get(sessionId).remove(resourceId);
    }

    public synchronized Map<Integer, Object> deleteAllSessionResources( String sessionId ) {

        return resourcesMap.remove(sessionId);
    }

}
