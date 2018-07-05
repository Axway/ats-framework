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
package com.axway.ats.core.events;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * This singleton must stay in the Core library as it is referenced from Agent Webapp and Test Harness.
 *
 * It loads the actual class which is handling testcase state change events, that class must stay in
 * Agent WS Client library as it passes these events to remote ATS agents.
 */
public class TestcaseStateEventsDispacher implements ITestcaseStateListener {

    private static final Logger                 log                                = Logger.getLogger(TestcaseStateEventsDispacher.class);

    private static TestcaseStateEventsDispacher instance;

    // reference to the actual Test Listener Implementation in Agent WS CLIENT
    private static final String                 TESTCASE_STATE_LISTENER_CLASS_NAME = "com.axway.ats.agent.webapp.client.listeners.TestcaseStateListener";
    private ITestcaseStateListener              testcaseStateListener;
    boolean                                     atLeastOneQueueFailed              = false;

    private TestcaseStateEventsDispacher() {

        try {
            // try to load a class from Agent WS CLIENT library
            Class<?> testcaseStateListenerClass = Class.forName(TESTCASE_STATE_LISTENER_CLASS_NAME);
            Method method = testcaseStateListenerClass.getMethod("getInstance", new Class[0]);
            testcaseStateListener = (ITestcaseStateListener) method.invoke(null, new Object[0]);

        } catch (ClassNotFoundException e) {

            testcaseStateListener = null;
            log.info("Class '" + TESTCASE_STATE_LISTENER_CLASS_NAME
                     + "' not found. It seems that ats-agent-webapp-client-nnn.jar is not in the classpath. This means you will get no messages from any ATS agent called by your code running on this host");
        } catch (Exception e) {

            testcaseStateListener = null;
            log.info("Initialization of class '" + TESTCASE_STATE_LISTENER_CLASS_NAME
                     + "' failed. This means you will get no messages from any ATS agent called by your code running on this host",
                     e);
        }
    }

    synchronized public static TestcaseStateEventsDispacher getInstance() {

        if (instance == null) {
            instance = new TestcaseStateEventsDispacher();
        }

        return instance;
    }

    @Override
    public void onTestStart() {

        atLeastOneQueueFailed = false;
        if (testcaseStateListener != null) {
            testcaseStateListener.onTestStart();
        }
    }

    @Override
    public void onTestEnd() {

        if (testcaseStateListener != null) {
            testcaseStateListener.onTestEnd();
        }
    }

    @Override
    public void onConfigureAtsAgents( List<String> atsAgents ) throws Exception {

        if (testcaseStateListener != null) {
            testcaseStateListener.onConfigureAtsAgents(atsAgents);
        }
    }

    /**
     * Collect status of loading queues (considering queue pass rate setting).
     * If at least one have failed then whole test will be considered as failed.
     */
    public void setQueueFinishedAsFailed( boolean isFailed ) {

        if (isFailed) {
            atLeastOneQueueFailed = true;
        }
    }

    /**
     * Currently used by QC Notifier in order to set final test status
     */
    public boolean hasAnyQueueFailed() {

        return atLeastOneQueueFailed;
    }
}
