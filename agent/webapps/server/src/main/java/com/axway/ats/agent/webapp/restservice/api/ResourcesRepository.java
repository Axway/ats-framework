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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.axway.ats.core.threads.ThreadsPerCaller;
import com.axway.ats.log.appenders.PassiveDbAppender;

/**
 * A class that is responsible for keeping track which resource (JAVA object) is linked with certain testcase and caller.
 * <br>Note that currently resource and testcase resource are the same, since there is no implementation for suite and run resources yet.
 * */
public class ResourcesRepository {

    private static Map<String, CallerResources> resourcesMap = Collections.synchronizedMap(new HashMap<String, CallerResources>());
    private static int                          resourceId   = -1;
    private static final ResourcesRepository   instance     = new ResourcesRepository();

    public synchronized static ResourcesRepository getInstance() {

        return instance;
    }

    public synchronized int putResource( Object resource ) {

        String callerId = ThreadsPerCaller.getCaller();
        int testcaseId = PassiveDbAppender.getCurrentInstance().getTestCaseId();
        CallerResources callerResources = resourcesMap.get(callerId);
        if (callerResources == null) {
            callerResources = new CallerResources();
        }
        callerResources.addTestcaseResource(testcaseId, ++resourceId, resource);

        resourcesMap.put(callerId, callerResources);
        return resourceId;
    }

    public synchronized Object getResource( int resourceId ) {

        String callerId = ThreadsPerCaller.getCaller();
        int testcaseId = PassiveDbAppender.getCurrentInstance().getTestCaseId();
        CallerResources callerResources = resourcesMap.get(callerId);
        return callerResources.getTestcaseResource(testcaseId, resourceId);

    }

    public synchronized Object deleteResource( int resourceId ) {

        String callerId = ThreadsPerCaller.getCaller();
        int testcaseId = PassiveDbAppender.getCurrentInstance().getTestCaseId();
        CallerResources callerResources = resourcesMap.get(callerId);
        return callerResources.deleteTestcaseResource(testcaseId, resourceId);
    }

    public synchronized Set<Integer> deleteResources() {

        String callerId = ThreadsPerCaller.getCaller();
        int testcaseId = PassiveDbAppender.getCurrentInstance().getTestCaseId();
        CallerResources callerResources = resourcesMap.get(callerId);
        Set<Integer> deletedResources = null;
        if (callerResources != null) {
            deletedResources = callerResources.deleteAllTestcaseResources(testcaseId);
            if(callerResources.testcasesResources.isEmpty()) {
                /*
                 * There is no more testcase resources for this caller,
                 * so delete the entire caller resources information
                 * */
                resourcesMap.remove(callerId);
            }
        } else {
            deletedResources = new HashSet<Integer>();
        }
        
        return deletedResources;
    }

    /**
     * Class that keeps resources, created from the same caller
     * */
    class CallerResources {

        Map<Integer, Map<Integer, Object>> testcasesResources = new HashMap<>();

        public void addTestcaseResource( int testcaseId, int resourceId, Object resource ) {

            Map<Integer, Object> testcasesRes = testcasesResources.get(testcaseId);
            if (testcasesRes == null) {
                testcasesRes = new HashMap<>();
            }
            testcasesRes.put(resourceId, resource);
            testcasesResources.put(testcaseId, testcasesRes);
        }

        /**
         * Delete a certain testcase resource
         * */
        public Object deleteTestcaseResource( int testcaseId, int resourceId ) {

            return testcasesResources.get(testcaseId).remove(resourceId);
        }

        public Object getTestcaseResource( int testcaseId, int resourceId ) {

            return testcasesResources.get(testcaseId).get(resourceId);
        }

        /**
         * Delete all testcase resources for the provided testcase ID
         * @return 
         * */
        public Set<Integer> deleteAllTestcaseResources( int testcaseId ) {
            Map<Integer,Object> testcaseResources = testcasesResources.remove(testcaseId);
            if(testcaseResources != null) {
                return testcaseResources.keySet();
            } else {
                return new HashSet<Integer>();
            }
        }

    }

}
