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
package com.axway.ats.agent.webapp.client.listeners;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.axway.ats.agent.core.configuration.RemoteLoggingConfigurator;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.webapp.client.ActionClient;
import com.axway.ats.agent.webapp.client.RestHelper;
import com.axway.ats.agent.webapp.client.configuration.RemoteConfigurationManager;
import com.axway.ats.core.events.ITestcaseStateListener;
import com.axway.ats.core.threads.ImportantThread;
import com.axway.ats.core.utils.ExecutorUtils;
import com.axway.ats.log.AtsDbLogger;
import com.axway.ats.log.appenders.ActiveDbAppender;
import com.axway.ats.log.autodb.TestCaseState;

/**
 * Currently used to configure the remote Agents prior to working with them
 */
public class TestcaseStateListener implements ITestcaseStateListener {

    private static Logger                log = Logger.getLogger(TestcaseStateListener.class);

    // List of agents with configured logging.
    // They know the database to work with.
    // FIXME: as this list only grows, we must clean it up at some appropriate moment
    private static List<String>          logConfiguredAgents;

    // List of test configured agents.
    // They know the particular test to work with.
    private static List<String>          testConfiguredAgents;

    private static TestcaseStateListener instance;

    private final Object                 configurationMutex;

    // force using getInstace()
    private TestcaseStateListener() {

        // hidden constructor body

        logConfiguredAgents = new ArrayList<String>();
        testConfiguredAgents = new ArrayList<>();

        configurationMutex = new Object();
    }

    public static synchronized TestcaseStateListener getInstance() {

        if (instance == null) {
            instance = new TestcaseStateListener();
        }
        return instance;
    }

    @Override
    public void onTestStart() {

    }

    @Override
    public void onTestEnd() {

        // FIXME: this must be split per testcase in case of parallel tests
        waitImportantThreadsToFinish();

        List<String> endedSessions = new ArrayList<>();
        String thisThread = Thread.currentThread().getName();

        // need synchronization when running parallel tests
        synchronized (configurationMutex) {
            for (String agentSessionId : testConfiguredAgents) {

                String thread = ExecutorUtils.extractThread(agentSessionId);

                if (thisThread.equals(thread)) {
                    // found the agent host
                    String agentHost = ExecutorUtils.extractHost(agentSessionId);
                    try {
                        RestHelper helper = new RestHelper();
                        helper.executeRequest(agentHost,
                                              "/testcases?sessionId=" + URLEncoder.encode(agentSessionId, "UTF-8"),
                                              "DELETE", null, null,
                                              null);
                    } catch (Exception e) {
                        log.error("Can't send onTestEnd event to ATS agent '" + agentHost + "'", e);
                    }
                    // remember the ended sessions
                    endedSessions.add(agentSessionId);
                }
            }

            // cleanup
            for (String agentSessionId : endedSessions) {
                testConfiguredAgents.remove(agentSessionId);
            }
        }
    }

    /**
     * This event is send right before running a regular action or starting
     * an action queue.
     *
     * It is also expected to be send between onTestStart and onTestEnd events.
     */
    @Override
    public void onConfigureAtsAgents( List<String> atsAgents ) throws Exception {
        
        if (ActiveDbAppender.getCurrentInstance() == null) {
            // database logger attached/specified in log4j.xml
            return;
        }

        for (String atsAgent : atsAgents) {

            // the remote endpoint is defined by Agent host and Test Executor's current thread(in case of parallel tests)
            String agentSessionId = ExecutorUtils.createExecutorId(atsAgent, ExecutorUtils.getUserRandomToken(),
                                                                   Thread.currentThread().getName());

            if (!testConfiguredAgents.contains(agentSessionId)) {

                // need synchronization when running parallel tests
                synchronized (configurationMutex) {

                    // Pass the logging configuration to the remote agent.
                    // We do this just once for the whole run.
                    if (!logConfiguredAgents.contains(atsAgent)) {

                        RemoteLoggingConfigurator remoteLoggingConfigurator = new RemoteLoggingConfigurator();
                        new RemoteConfigurationManager().pushConfiguration(atsAgent,
                                                                           remoteLoggingConfigurator);
                        logConfiguredAgents.add(atsAgent);
                    }

                    // Pass the testcase configuration to the remote agent.
                    // We do this each time a new test is started
                    if (!testConfiguredAgents.contains(agentSessionId)) {
                        String agentHost = null;
                        TestCaseState testCaseState = null;
                        try {
                            agentHost = ExecutorUtils.extractHost(agentSessionId);
                            testCaseState = getCurrentTestCaseState();
                            RestHelper helper = new RestHelper();
                            helper.executeRequest(agentHost, "/testcases",
                                                  "PUT",
                                                  "{\"sessionId\":\"" + agentSessionId
                                                         + "\",\"testCaseState\":"
                                                         + helper.serializeJavaObject(testCaseState) + "}",
                                                  null, null);

                            testConfiguredAgents.add(agentSessionId);
                        } catch (Exception e) {
                            String message = "Unable to start testcase with id '" + testCaseState.getTestcaseId()
                                             + "' from run with id '" + testCaseState.getRunId() + "' on agent '"
                                             + agentHost
                                             + "'";
                            log.error(message);
                            throw new AgentException(message, e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Wrap the test case state from the log component into the bean
     * which the web service expects
     *
     * @return current {@link TestCaseState}
     */
    private TestCaseState getCurrentTestCaseState() {

    	/** We want to send event to the agents, even if db appender is not attached, so we skip that check **/
        com.axway.ats.log.autodb.TestCaseState testCaseState = AtsDbLogger.getLogger(ActionClient.class.getName(), true)
                                                                          .getCurrentTestCaseState();
        if (testCaseState == null) {
            return null;
        } else {
            TestCaseState wsTestCaseState = new TestCaseState();
            wsTestCaseState.setTestcaseId(testCaseState.getTestcaseId());
            wsTestCaseState.setRunId(testCaseState.getRunId());
            return wsTestCaseState;
        }
    }

    /**
     * Wait for all the {@link ImportantThread}s to finish
     *
     */
    private void waitImportantThreadsToFinish() {

        try {
            Set<Thread> threads = Thread.getAllStackTraces().keySet();
            for (Thread th : threads) {
                if (th != null && th.isAlive() && (th instanceof ImportantThread)) {
                    log.warn("There is a still running Agent action queue '"
                             + ((ImportantThread) th).getDescription()
                             + "'. Now we will wait for its completion. "
                             + "If you want to skip this warning, you must explicitly wait in your test code for the queue completion.");
                    try {
                        th.join();
                    } catch (InterruptedException ie) {}
                }
            }
        } catch (Exception e) {
            log.warn("Error waiting for all important threads to finish.", e);
        }
    }

}
