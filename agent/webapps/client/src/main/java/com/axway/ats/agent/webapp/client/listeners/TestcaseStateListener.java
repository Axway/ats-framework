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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    class AgentInfo {

        String  address        = null;
        boolean logConfigured  = false; // whether agent was configured to log in database
        boolean testConfigured = false; // whether onTestStart event was sent to agent successfully

    }

    private static Logger                       log        = Logger.getLogger(TestcaseStateListener.class);

    private static Map<String, List<AgentInfo>> agentInfos = Collections.synchronizedMap(new HashMap<String, List<AgentInfo>>());

    private static TestcaseStateListener        instance;

    private final Object                        configurationMutex;

    // force using getInstace()
    private TestcaseStateListener() {

        // hidden constructor body

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

        // get current caller ID
        String callerId = ExecutorUtils.createCallerId();

        sentOnTestEndEvent(callerId);

    }

    @Override
    public void onTestEnd( List<String> callerIDs ) {

        for (String callerId : callerIDs) {
            sentOnTestEndEvent(callerId);
        }

    }

    private void sentOnTestEndEvent( String callerId ) {

        // FIXME: this must be split per testcase in case of parallel tests
        waitImportantThreadsToFinish();

        // need synchronization when running parallel tests
        synchronized (configurationMutex) {

            List<AgentInfo> agentInfosList = agentInfos.get(callerId);
            if (agentInfosList != null) {
                RestHelper helper = new RestHelper();

                for (int i = 0; i < agentInfosList.size(); i++) {
                    AgentInfo ai = agentInfosList.get(i);

                    if (ai.testConfigured) {
                        try {
                            helper.executeRequest(ai.address,
                                                  "/testcases?callerId=" + URLEncoder.encode(callerId, "UTF-8"),
                                                  "DELETE", null, null,
                                                  null);

                            ai.testConfigured = false;
                        } catch (Exception e) {
                            log.error("Can't send onTestEnd event to ATS agent '" + ai.address
                                      + "'", e);
                        }
                    }
                }
            }
        }

    }

    /**
     * This event is send right before running a regular action or starting
     * an action queue.
     *
     * It is also expected to be send between onTestStart and onTestEnd events.
     * <br> But if it is not, the testcase id that the agent will receive will be -1
     */
    @Override
    public void onConfigureAtsAgents( List<String> atsAgents ) throws Exception {

        if (ActiveDbAppender.getCurrentInstance() == null) {
            // database logger attached/specified in log4j.xml
            return;
        }

        // get current caller ID
        String callerId = ExecutorUtils.createCallerId();

        // need synchronization when running parallel tests
        synchronized (configurationMutex) {

            List<AgentInfo> agentInfosList = agentInfos.get(callerId);
            if (agentInfosList == null) {
                agentInfosList = new ArrayList<>();
                agentInfos.put(callerId, agentInfosList);
                // no agent are configured from the current caller ID
                // create AgentInfo entry for each of the provided ATS Agents
                for (String atsAgent : atsAgents) {
                    AgentInfo ai = new AgentInfo();
                    ai.address = atsAgent;
                    agentInfosList.add(ai);
                }
            } else {
                boolean isRegistered = false;
                for (String atsAgent : atsAgents) {
                    for (AgentInfo ai : agentInfosList) {
                        if (ai.address.equals(atsAgent)) {
                            isRegistered = true;
                            break;
                        }
                    }
                    if (!isRegistered) {
                        AgentInfo newAi = new AgentInfo();
                        newAi.address = atsAgent;
                        agentInfosList.add(newAi);
                        break;
                    }
                }
            }

            for (AgentInfo ai : agentInfosList) {
                
                log.info("Pushing configuration to ATS Agent at '" + ai.address + "'");

                RemoteLoggingConfigurator remoteLoggingConfigurator = new RemoteLoggingConfigurator();
                new RemoteConfigurationManager().pushConfiguration(ai.address,
                                                                   remoteLoggingConfigurator);
                ai.logConfigured = true;

                if (!ai.testConfigured) {
                    TestCaseState testCaseState = null;
                    try {
                        testCaseState = getCurrentTestCaseState();
                        RestHelper helper = new RestHelper();
                        helper.executeRequest(ai.address, "/testcases",
                                              "PUT",
                                              "{\"callerId\":\"" + callerId
                                                     + "\",\"testCaseState\":"
                                                     + helper.serializeJavaObject(testCaseState) + "}",
                                              null, null);
                        ai.testConfigured = true;
                    } catch (Exception e) {
                        String message = "Unable to start testcase with id '" + testCaseState.getTestcaseId()
                                         + "' from run with id '" + testCaseState.getRunId() + "' on agent '"
                                         + ai.address
                                         + "'";
                        log.error(message);
                        throw new AgentException(message, e);
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
            wsTestCaseState.setLastExecutedTestcaseId(testCaseState.getLastExecutedTestcaseId());
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
