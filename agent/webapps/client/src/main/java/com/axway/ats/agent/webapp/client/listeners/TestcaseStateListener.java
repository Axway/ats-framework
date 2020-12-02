/*
 * Copyright 2017-2020 Axway Software
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
import com.axway.ats.agent.webapp.client.configuration.AgentConfigurationLandscape;
import com.axway.ats.agent.webapp.client.configuration.RemoteConfigurationManager;
import com.axway.ats.core.events.ITestcaseStateListener;
import com.axway.ats.core.threads.ImportantThread;
import com.axway.ats.core.utils.ExecutorUtils;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.utils.StringUtils;
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
                            log.warn("Could not nofity ATS agent on '" + ai.address
                                     + "' that testcase has ended. Probably the agent have become unreachable during test execution.",
                                     e);
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
            // database logger not attached/specified in log4j.xml
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

                RemoteLoggingConfigurator remoteLoggingConfigurator = new RemoteLoggingConfigurator(AgentConfigurationLandscape.getInstance(ai.address)
                                                                                                                               .getDbLogLevel(),
                                                                                                    AgentConfigurationLandscape.getInstance(ai.address)
                                                                                                                               .getChunkSize());
                new RemoteConfigurationManager().pushConfiguration(ai.address,
                                                                   remoteLoggingConfigurator);
                ai.logConfigured = true;

                if (!ai.testConfigured) {
                    TestCaseState testCaseState = null;
                    try {
                        testCaseState = getCurrentTestCaseState();
                        if (testCaseState != null) {
                            if (testCaseState.getTestcaseId() != -1
                                || testCaseState.getLastExecutedTestcaseId() != -1) {
                                RestHelper helper = new RestHelper();
                                helper.executeRequest(ai.address, "/testcases",
                                                      "PUT",
                                                      "{\"callerId\":\"" + callerId
                                                             + "\",\"testCaseState\":"
                                                             + helper.serializeJavaObject(testCaseState) + "}",
                                                      null, null);
                                ai.testConfigured = true;
                            } else {
                                /**
                                 * Or log/throw an error like:
                                 * 
                                 * String message = "Could not join testcase on ATS Agent at '" + monitoredHost + "'. "
                                 *                  + "Either you did not attach AtsTestngListener listener to your test class hierarchy or "
                                 *                  + "you are invoking System monitoring operation outside of @Test, @BeforeMethod or @AfterMethod annotated methods.";
                                 * throw new MonitoringException(message); //or log.error(message)
                                 * */
                                log.warn("Agent at '" + ai.address + "' used outside of a TESTCASE");
                            }
                        } else {
                            // Do we really need to end in this else block? Even inside a RUN, monitoring does not work. The current LifeCycle state must be TESTCASE_STARTED
                            log.warn("Agent at '" + ai.address + "' used outside of both TESTCASE and RUN"); // or log.error()?
                        }

                    } catch (Exception e) {
                        String message = "Exception while trying to configure agent at " + ai.address + ": ";
                        if (testCaseState != null) {
                            message += "Unable to start testcase with id '" + testCaseState.getTestcaseId()
                                       + "' from run with id '" + testCaseState.getRunId() + "' on agent '"
                                       + ai.address
                                       + "'";
                        } else {
                            message += "Unable to start testcase, because ATS could not obtain testcase state information";
                        }

                        log.error(message, e);
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

    @Override
    public void invalidateConfiguredAtsAgents( List<String> atsAgents ) {

        String message = "Invalidating ATS Log DB configuration for ATS agent '%s', configured by caller '%s'";

        if (agentInfos == null || agentInfos.isEmpty() || atsAgents == null || atsAgents.isEmpty()) {
            return;
        } else {
            synchronized (configurationMutex) {
                for (String agent : atsAgents) {
                    for (Map.Entry<String, List<AgentInfo>> entry : agentInfos.entrySet()) {
                        String callerId = entry.getKey();
                        List<AgentInfo> agentsInfos = entry.getValue();
                        if (agentsInfos == null || agentsInfos.isEmpty()) {
                            continue;
                        }
                        for (AgentInfo info : agentsInfos) {
                            boolean agentCleared = false;
                            if (info == null || StringUtils.isNullOrEmpty(info.address)) {
                                continue; // not sure if possible. And maybe if possible, throw an Exception
                            }
                            // do we have to mark only the agent as deleted, or delete the entry from the list?
                            if (info.address.equals(agent)) {
                                info.logConfigured = false;
                                info.testConfigured = false;
                                agentCleared = true;
                            } else {
                                if (info.address.equals(HostUtils.getAtsAgentIpAndPort(agent))) { // try with the <HOST>:<default port> (if needed)
                                    info.logConfigured = false;
                                    info.testConfigured = false;
                                    agentCleared = true;
                                }
                            }

                            if (agentCleared) {
                                log.info(String.format(message, agent, callerId));
                            }
                        }
                    }
                }
            }
        }

    }

}
