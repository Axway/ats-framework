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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.agent.core.configuration.RemoteLoggingConfigurator;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.threading.ImportantThread;
import com.axway.ats.agent.webapp.client.ActionClient;
import com.axway.ats.agent.webapp.client.AgentException_Exception;
import com.axway.ats.agent.webapp.client.AgentService;
import com.axway.ats.agent.webapp.client.AgentServicePool;
import com.axway.ats.agent.webapp.client.TestCaseState;
import com.axway.ats.agent.webapp.client.configuration.AgentConfigurationLandscape;
import com.axway.ats.agent.webapp.client.configuration.RemoteConfigurationManager;
import com.axway.ats.core.AtsVersion;
import com.axway.ats.core.events.ITestcaseStateListener;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.log.AtsDbLogger;

public class TestcaseStateListener implements ITestcaseStateListener {

    private static Logger                log = LogManager.getLogger(TestcaseStateListener.class);

    // list of configured agents
    private static List<String>          configuredAgents;

    private static TestcaseStateListener instance;

    // force using getInstace()
    private TestcaseStateListener() {

        // hidden constructor body
    }

    public static synchronized TestcaseStateListener getInstance() {

        if (instance == null) {
            instance = new TestcaseStateListener();
        }
        return instance;
    }

    @Override
    public void onTestStart() {

        // this is a new testcase, clear the list of configured agents
        configuredAgents = new ArrayList<String>();
    }

    @Override
    public void onTestEnd() {

        if (configuredAgents == null) {

            // onTestStart had never been called,
            // maybe onTestEnd is produced by test skipped event before starting a testcase
            return;
        }

        waitImportantThreadsToFinish();

        for (String atsAgent : configuredAgents) {

            try {

                //get the client
                AgentService agentServicePort = AgentServicePool.getInstance().getClient(atsAgent);
                agentServicePort.onTestEnd();

            } catch (Exception e) {

                log.warn("Could not nofity ATS agent on '" + atsAgent
                         + "' that testcase has ended. Probably the agent have become unreachable during test execution.",
                         e);
            }
        }
        configuredAgents = null;
    }

    /**
     * This event is send right before running a regular action or starting
     * an action queue.
     *
     * It is also expected to be send between onTestStart and onTestEnd events.
     */
    @Override
    public void onConfigureAtsAgents( List<String> atsAgents ) throws Exception {

        if (configuredAgents == null) {

            // No TestCase is started so we won't configure the remote agents
            return;
        }

        synchronized (configuredAgents) {

            for (String atsAgent : atsAgents) {

                // configure only not configured agents
                if (!configuredAgents.contains(atsAgent)) {

                    if (!configuredAgents.contains(atsAgent)) {

                        log.info("Pushing configuration to ATS Agent at '" + atsAgent + "'");

                        try {

                            String agentVersion = AgentServicePool.getInstance().getClient(atsAgent).getAgentVersion();
                            String atsVersion = AtsVersion.getAtsVersion();
                            if (agentVersion != null) {
                                if (!AtsVersion.getAtsVersion().equals(agentVersion)) {
                                    log.warn("*** ATS WARNING *** You are using ATS version '" + atsVersion
                                             + "' with ATS Agent version '" + agentVersion + "' located at '"
                                             + HostUtils.getAtsAgentIpAndPort(atsAgent)
                                             + "'. This might cause incompatibility problems!");
                                }
                            }

                            // Pass the logging configuration to the remote agent
                            RemoteLoggingConfigurator remoteLoggingConfigurator = new RemoteLoggingConfigurator(AgentConfigurationLandscape.getInstance(atsAgent)
                                                                                                                                           .getDbLogLevel(),
                                                                                                                AgentConfigurationLandscape.getInstance(atsAgent)
                                                                                                                                           .getChunkSize());
                            new RemoteConfigurationManager().pushConfiguration(atsAgent,
                                                                               remoteLoggingConfigurator);

                            AgentService agentServicePort = AgentServicePool.getInstance()
                                                                            .getClient(atsAgent);
                            agentServicePort.onTestStart(getCurrentTestCaseState());
                        } catch (AgentException_Exception ae) {

                            throw new AgentException(ae.getMessage());
                        } catch (Exception e) {
                            throw new AgentException("Exception while trying to configure agent at " + atsAgent + ": "
                                                     + e.getMessage(), e);
                        }

                        configuredAgents.add(atsAgent);
                    }
                }

            }
        }
    }

    @Override
    public void invalidateConfiguredAtsAgents( List<String> atsAgents ) {

        String message = "Invalidating ATS Log DB configuration for ATS agent '%s'";

        if (configuredAgents == null || configuredAgents.isEmpty()) {
            return;
        } else {

            synchronized (configuredAgents) {
                for (String agent : atsAgents) {
                    boolean agentCleared = false;

                    if (configuredAgents.contains(agent)) {
                        configuredAgents.remove(agent);
                        agentCleared = true;
                    } else {
                        agent = HostUtils.getAtsAgentIpAndPort(agent); // set the default port if needed
                        if (configuredAgents.contains(agent)) {
                            configuredAgents.remove(agent);
                            agentCleared = true;
                        }
                    }

                    if (agentCleared) {
                        log.info(String.format(message, agent));
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
            wsTestCaseState.setLastExecutedTestcaseId(testCaseState.getLastExecutedTestcaseId());
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

    /**
     * Called to release the internal object resources on the agent side
     */
    @Override
    public void cleanupInternalObjectResources( String atsAgent, String internalObjectResourceId ) {

        try {
            AgentService agentServicePort = AgentServicePool.getInstance().getClient(atsAgent);
            agentServicePort.cleanupInternalObjectResources(internalObjectResourceId);
        } catch (Exception e) {
            // As this code is triggered when the garbage collector disposes the client side object
            // at the Test Executor side, the following message seems confusing:
            // log.error( "Can't cleanup resource with identifier " + internalObjectResourceId + " on ATS agent '" + atsAgent + "'", e );
        }
    }

}
