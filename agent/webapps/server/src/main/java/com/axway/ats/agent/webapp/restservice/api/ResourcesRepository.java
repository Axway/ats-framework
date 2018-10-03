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
    private static long                         resourceId   = -1;
    private static final ResourcesRepository    instance     = new ResourcesRepository();

    public synchronized static ResourcesRepository getInstance() {

        return instance;
    }

    public synchronized long putResource( Object resource ) {

        String callerId = ThreadsPerCaller.getCaller();
        int testcaseId = -1;
        /*
         * There are cases when we want to execute action on the agent, before that agent is configured.
         * Since the agent is not configured, there is no PassiveDbAppender available.
         * In that case testcaseId will be -1
         * TODO: create map for run and suite resources, but that means that the agent will need to know when a run and suite is started on the Test executor
         * or at least know when a particular run or suite is ended on the Test executor so those resources can be deleted
         * */
        if (PassiveDbAppender.getCurrentInstance() != null) {
            testcaseId = PassiveDbAppender.getCurrentInstance().getTestCaseId();
        }
        CallerResources callerResources = resourcesMap.get(callerId);
        if (callerResources == null) {
            callerResources = new CallerResources();
        }
        callerResources.addTestcaseResource(testcaseId, ++resourceId, resource);

        resourcesMap.put(callerId, callerResources);
        return resourceId;
    }

    public synchronized Object getResource( long resourceId ) {

        String callerId = ThreadsPerCaller.getCaller();
        int testcaseId = -1;
        /*
         * There are cases when we want to execute action on the agent, before that agent is configured.
         * Since the agent is not configured, there is no PassiveDbAppender available.
         * In that case testcaseId will be -1
         * TODO: create map for run and suite resources, but that means that the agent will need to know when a run and suite is started on the Test executor
         * or at least know when a particular run or suite is ended on the Test executor so those resources can be deleted
         * */
        if (PassiveDbAppender.getCurrentInstance() != null) {
            testcaseId = PassiveDbAppender.getCurrentInstance().getTestCaseId();
        }
        CallerResources callerResources = resourcesMap.get(callerId);
        if (callerResources == null) {
            throw new RuntimeException("There are no resources, created from caller '" + callerId + "'");
        }
        return callerResources.getTestcaseResource(testcaseId, resourceId);

    }

    public synchronized Object deleteResource( long resourceId ) {

        String callerId = ThreadsPerCaller.getCaller();
        int testcaseId = -1;
        /*
         * There are cases when we want to execute action on the agent, before that agent is configured.
         * Since the agent is not configured, there is no PassiveDbAppender available.
         * In that case testcaseId will be -1
         * TODO: create map for run and suite resources, but that means that the agent will need to know when a run and suite is started on the Test executor
         * or at least know when a particular run or suite is ended on the Test executor so those resources can be deleted
         * */
        if (PassiveDbAppender.getCurrentInstance() != null) {
            testcaseId = PassiveDbAppender.getCurrentInstance().getTestCaseId();
        }
        CallerResources callerResources = resourcesMap.get(callerId);
        return callerResources.deleteTestcaseResource(testcaseId, resourceId);
    }

    public synchronized Set<Long> deleteResources() {

        String callerId = ThreadsPerCaller.getCaller();
        int testcaseId = -1;
        /*
         * There are cases when we want to execute action on the agent, before that agent is configured.
         * Since the agent is not configured, there is no PassiveDbAppender available.
         * In that case testcaseId will be -1
         * TODO: create map for run and suite resources, but that means that the agent will need to know when a run and suite is started on the Test executor
         * or at least know when a particular run or suite is ended on the Test executor so those resources can be deleted
         * */
        if (PassiveDbAppender.getCurrentInstance() != null) {
            testcaseId = PassiveDbAppender.getCurrentInstance().getTestCaseId();
        }
        CallerResources callerResources = resourcesMap.get(callerId);
        Set<Long> deletedResources = null;
        if (callerResources != null) {
            deletedResources = callerResources.deleteAllTestcaseResources(testcaseId);
            if (callerResources.testcasesResources.isEmpty()) {
                /*
                 * There is no more testcase resources for this caller,
                 * so delete the entire caller resources information
                 * */
                resourcesMap.remove(callerId);
            }
        } else {
            deletedResources = new HashSet<Long>();
        }

        return deletedResources;
    }

    /**
     * Class that keeps resources, created from the same caller
     * */
    class CallerResources {

        Map<Integer, Map<Long, Object>> testcasesResources = new HashMap<>();

        public void addTestcaseResource( int testcaseId, long resourceId, Object resource ) {

            Map<Long, Object> testcasesRes = testcasesResources.get(testcaseId);
            if (testcasesRes == null) {
                testcasesRes = new HashMap<>();
            }
            testcasesRes.put(resourceId, resource);
            testcasesResources.put(testcaseId, testcasesRes);
        }

        /**
         * Delete a certain testcase resource
         * */
        public Object deleteTestcaseResource( int testcaseId, long resourceId ) {

            return testcasesResources.get(testcaseId).remove(resourceId);
        }

        public Object getTestcaseResource( int testcaseId, long resourceId ) {

            Map<Long, Object> testcaseResources = testcasesResources.get(testcaseId);

            if (testcaseResources == null) {

                throw new RuntimeException("There are no testcase resources, created from testcase with id '"
                                           + testcaseId + "'");
            }

            return testcaseResources.get(resourceId);
        }

        /**
         * Delete all testcase resources for the provided testcase ID
         * @return 
         * */
        public Set<Long> deleteAllTestcaseResources( int testcaseId ) {

            Map<Long, Object> testcaseResources = testcasesResources.remove(testcaseId);
            if (testcaseResources != null) {
                return testcaseResources.keySet();
            } else {
                return new HashSet<Long>();
            }
        }

    }

}
