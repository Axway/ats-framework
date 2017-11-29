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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.axway.ats.agent.core.configuration.RemoteLoggingConfigurator;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.threading.ImportantThread;
import com.axway.ats.agent.webapp.client.ActionClient;
import com.axway.ats.agent.webapp.client.AgentException_Exception;
import com.axway.ats.agent.webapp.client.AgentService;
import com.axway.ats.agent.webapp.client.AgentServicePool;
import com.axway.ats.agent.webapp.client.TestCaseState;
import com.axway.ats.agent.webapp.client.configuration.RemoteConfigurationManager;
import com.axway.ats.core.events.ITestcaseStateListener;
import com.axway.ats.log.AtsDbLogger;

public class TestcaseStateListener implements ITestcaseStateListener {

    private static Logger                log = Logger.getLogger( TestcaseStateListener.class );

    // list of configured agents
    private static List<String>          configuredAgents;

    private static TestcaseStateListener instance;

    // force using getInstace()
    private TestcaseStateListener() {

        // hidden constructor body
    }

    public static synchronized TestcaseStateListener getInstance() {

        if( instance == null ) {
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

        if( configuredAgents == null ) {

            // onTestStart had never been called,
            // maybe onTestEnd is produced by test skipped event before starting a testcase
            return;
        }

        waitImportantThreadsToFinish();

        for( String atsAgent : configuredAgents ) {

            try {

                //get the client
                AgentService agentServicePort = AgentServicePool.getInstance().getClient( atsAgent );
                agentServicePort.onTestEnd();

            } catch( Exception e ) {

                log.error( "Can't send onTestEnd event to ATS agent '" + atsAgent + "'", e );
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

        if( configuredAgents == null ) {

            // No TestCase is started so we won't configure the remote agents
            return;
        }

        for( String atsAgent : atsAgents ) {

            // configure only not configured agents
            if( !configuredAgents.contains( atsAgent ) ) {

                // Here we need to sync on the 'atsAgent' String, which is not good solution at all,
                // because there can be another sync on the same string in the JVM (possible deadlocks).
                // We will try to prevent that adding a 'unique' prefix
                synchronized( ( "ATS_STRING_LOCK-" + atsAgent ).intern() ) {

                    if( !configuredAgents.contains( atsAgent ) ) {

                        // Pass the logging configuration to the remote agent
                        RemoteLoggingConfigurator remoteLoggingConfigurator = new RemoteLoggingConfigurator( atsAgent );
                        new RemoteConfigurationManager().pushConfiguration( atsAgent,
                                                                            remoteLoggingConfigurator );

                        try {
                            AgentService agentServicePort = AgentServicePool.getInstance()
                                                                          .getClient( atsAgent );
                            agentServicePort.onTestStart( getCurrentTestCaseState() );
                        } catch( AgentException_Exception ae ) {

                            throw new AgentException( ae.getMessage() );
                        }

                        configuredAgents.add( atsAgent );
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

        com.axway.ats.log.autodb.TestCaseState testCaseState = AtsDbLogger.getLogger( ActionClient.class.getName() )
                                                                         .getCurrentTestCaseState();
        if( testCaseState == null ) {
            return null;
        } else {
            TestCaseState wsTestCaseState = new TestCaseState();
            wsTestCaseState.setTestcaseId( testCaseState.getTestcaseId() );
            wsTestCaseState.setRunId( testCaseState.getRunId() );
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
            for( Thread th : threads ) {
                if( th != null && th.isAlive() && ( th instanceof ImportantThread ) ) {
                    log.warn( "There is a still running Agent action queue '"
                              + ( ( ImportantThread ) th ).getDescription()
                              + "'. Now we will wait for its completion. "
                              + "If you want to skip this warning, you must explicitly wait in your test code for the queue completion." );
                    try {
                        th.join();
                    } catch( InterruptedException ie ) {}
                }
            }
        } catch( Exception e ) {
            log.warn( "Error waiting for all important threads to finish.", e );
        }
    }

    /**
     * Called to release the internal object resources on the agent side
     */
    @Override
    public void cleanupInternalObjectResources( String atsAgent, String internalObjectResourceId ) {

        try {
            AgentService agentServicePort = AgentServicePool.getInstance().getClient( atsAgent );
            agentServicePort.cleanupInternalObjectResources( internalObjectResourceId );
        } catch( Exception e ) {
            // As this code is triggered when the garbage collector disposes the client side object
            // at the Test Executor side, the following message seems confusing:
            // log.error( "Can't cleanup resource with identifier " + internalObjectResourceId + " on ATS agent '" + atsAgent + "'", e );
        }
    }
}
