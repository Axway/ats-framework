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
package com.axway.ats.agent.webapp.client;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.List;

import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.common.performance.monitor.beans.MonitorResults;

/**
 * Used to control the Agent monitoring system
 */
public final class AgentMonitoringClient extends ActionClient {

    /**
     * @param atsAgent
     *            the ATS agent to work with - if you
     *            pass LOCAL_JVM, the action execution will be performed in the
     *            current JVM without routing through the web service
     */
    public AgentMonitoringClient( String atsAgent ) {

        // this client works on the level of Agent distribution, 
        // not on the level of Agent components
        super( atsAgent, "some fake component" );
    }

    /**
     * Start monitoring Agent users activity
     *
     * @param startTimestamp the start timestamp is provided by the test executor,
     * so it can be mapped between different monitoring processes
     * @param pollInterval the monitor poll interval
     *
     * @throws AgentException
     */
    public void startMonitoring( long startTimestamp, int pollInterval ) throws AgentException {

        // get the client instance
        AgentService agentServicePort = AgentServicePool.getInstance().getClient( atsAgent );

        try {
            agentServicePort.startMonitoring( startTimestamp, pollInterval );
        } catch( Exception ioe ) {
            // log hint for further serialization issue investigation
            log.error( ioe );
            throw new AgentException( ioe );
        }
    }

    /**
     * Stop monitoring Agent users activity
     *
     * @throws AgentException
     */
    public void stopMonitoring() throws AgentException {

        // get the client instance
        AgentService agentServicePort = AgentServicePool.getInstance().getClient( atsAgent );

        try {
            agentServicePort.stopMonitoring();
        } catch( Exception ioe ) {
            // log hint for further serialization issue investigation
            log.error( "Error stopping monitoring on Agent host '" + atsAgent + "'", ioe );
            throw new AgentException( ioe );
        }
    }

    /**
     * @return the latest info about the Agent users activity
     * @throws AgentException
     */
    @SuppressWarnings("unchecked")
    public List<MonitorResults> getMonitoringResults() throws AgentException {

        // get the client instance
        AgentService agentServicePort = AgentServicePool.getInstance().getClient( atsAgent );

        try {
            ByteArrayInputStream byteInStream = new ByteArrayInputStream( agentServicePort.getMonitoringResults() );
            ObjectInputStream objectInStream = new ObjectInputStream( byteInStream );
            return ( List<MonitorResults> ) objectInStream.readObject();
        } catch( Exception ioe ) {
            // log hint for further serialization issue investigation
            log.error( ioe );
            throw new AgentException( ioe );
        }
    }
}
