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
package com.axway.ats.agent.core.action;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Data used by actions which are sensitive to the remote caller
 */
public class CallerRelatedInfoRepository {

    public static final String                              DEFAULT_CALLER          = "127.0.0.1";

    // keep one instance per caller
    private static Map<String, CallerRelatedInfoRepository> instances               = new HashMap<String, CallerRelatedInfoRepository>();

    // Map with data that an Action class will use as needed
    // We use Hashtable for implicit synchronization
    private Hashtable<String, Object>                       data;

    // Growing ID counter for creating UID.
    // It should not be static. If it is static, we will have to synchronize between calls from different remote callers
    // as each remote caller triggers the creation of another instance of this class
    private int                                             idCounter               = 0;

    // key prefix for ProcessExecutor
    public static final String                              KEY_PROCESS_EXECUTOR    = "Process Executor -> ";
    // key prefix for ProcessTalker
    public static final String                              KEY_PROCESS_TALKER      = "Process Talker -> ";
    // key prefix for FileSystemSnapshot
    public static final String                              KEY_FILESYSTEM_SNAPSHOT = "File System Snapshot -> ";

    synchronized public static CallerRelatedInfoRepository getInstance(
                                                                        String caller ) {

        CallerRelatedInfoRepository instance = instances.get( caller );
        if( instance == null ) {
            instance = new CallerRelatedInfoRepository();
            instances.put( caller, instance );
        }
        return instance;
    }

    private CallerRelatedInfoRepository() {

        data = new Hashtable<String, Object>();
    }

    /**
     * @param uid the object's UID
     * @return the object found in the repository. Not expected to ever return null
     */
    public Object getObject(
                             String uid ) {

        return data.get( uid );
    }

    /**
     * Add an object into the repository and return its unique counter which will be used
     * in next calls to this object
     *  
     * @param key key describing the incoming object. This key is used in generating the UID.
     * @param object the object to put into the repository
     * @return the object's UID
     */
    public String addObject(
                             String keyPrefix,
                             Object object ) {

        /**
         * Create a unique ID for each instance. The UID consists of:
         *  1. key prefix
         *      - describes the type of the object(for example 'process executor' or similar)
         *  2. counter
         *      - in performance tests it is possible to get more than one call at the 
         *      same moment(same millisecond) - so we need a growing counter as well
         *  3. current timestamp
         *      - if agent is restarted, the counter will get duplicated - so we need timestamp as well
         * 
         * Performance tests are a reason to synchronize the next code
         */
        String uniqueCounter;
        synchronized( this ) {
            uniqueCounter = ( ++idCounter ) + "-" + System.currentTimeMillis();
        }

        String uid = keyPrefix + uniqueCounter;
        data.put( uid, object );

        return uniqueCounter;
    }

    /**
     * this method should be called when we know this data is not needed anymore
     * 
     * @param key
     */
    public void removeObject(
                              String key ) {

        data.remove( key );
    }
}
