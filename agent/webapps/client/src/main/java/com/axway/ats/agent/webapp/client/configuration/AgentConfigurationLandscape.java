/*
 * Copyright 2017-2019 Axway Software
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
package com.axway.ats.agent.webapp.client.configuration;

import java.util.HashMap;
import java.util.Map;

import com.axway.ats.agent.core.configuration.AgentConfigurator;
import com.axway.ats.agent.core.configuration.Configurator;
import com.axway.ats.agent.core.configuration.EnvironmentConfigurator;
import com.axway.ats.agent.core.configuration.GenericAgentConfigurator;
import com.axway.ats.agent.core.configuration.RemoteLoggingConfigurator;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.log.LogLevel;

/**
 * Contains all configurations passed to ATS agents.
 * 
 * It is supposed to help debugging issues concerning agent configurations
 * by allowing to quickly see what kind of configuration settings we have passed
 * to an agent from the test.
 * 
 * Note that we have not added all possible configurators. 
 * For the whole list see all the classes extending com.axway.ats.agent.core.configuration.Configurator
 */
public class AgentConfigurationLandscape {

    // one instance per agent
    private static Map<String, AgentConfigurationLandscape> instances = new HashMap<>();

    // Configures the log4j DB appender
    private RemoteLoggingConfigurator                       loggingConfigurator;

    // Basic agent configuration properties defining how we load the action components
    private AgentConfigurator                               agentConfigurator;

    // Some extra properties
    private GenericAgentConfigurator                        genericConfigurator;

    // Helps get connected to a DB to be backed up or restored
    private EnvironmentConfigurator                         environmentConfigurator;

    // Logger level used for our DB appender
    private LogLevel                                        dbLoggerLevelSetFromTest;

    // The protocol used to communicate with an Agent - "http" or "https"
    private String                                          protocol;

    // the chunk size used for batch mode db logging
    private int                                             chunkSize;

    /**
     * Get instance for a particular agent.
     * 
     * Synchronization latency is not considered as an issue as 
     * this method is not called too often.
     * 
     * @param agent
     * @return
     */
    public static synchronized AgentConfigurationLandscape getInstance( String agent ) {

        AgentConfigurationLandscape instance = instances.get(agent);
        if (instance == null) {
            instance = new AgentConfigurationLandscape();
            instances.put(agent, instance);
        }

        return instance;
    }

    /**
     * This is the entry point to cache the used configurator
     * 
     * @param configurator
     */
    public void cacheConfigurator( Configurator configurator ) {

        if (configurator instanceof RemoteLoggingConfigurator) {
            loggingConfigurator = (RemoteLoggingConfigurator) configurator;
        } else if (configurator instanceof AgentConfigurator) {
            agentConfigurator = (AgentConfigurator) configurator;
        } else if (configurator instanceof GenericAgentConfigurator) {
            genericConfigurator = (GenericAgentConfigurator) configurator;
        }
    }

    public RemoteLoggingConfigurator getLoggingConfigurator() {

        return loggingConfigurator;
    }

    public AgentConfigurator getAgentConfigurator() {

        return agentConfigurator;
    }

    public GenericAgentConfigurator getGenericConfigurator() {

        return genericConfigurator;
    }

    public EnvironmentConfigurator getEnvironmentConfigurator() {

        return environmentConfigurator;
    }

    /**
     * Set the log level used for our DB appender
     * 
     * @param logLevel new log level
     * @throws AgentException
     */
    public void setDbLogLevel( LogLevel logLevel ) throws AgentException {

        dbLoggerLevelSetFromTest = logLevel;
    }

    /**
     * @return the log level used for our DB appender
     */
    public LogLevel getDbLogLevel() {

        return dbLoggerLevelSetFromTest;
    }

    /**
     * Set the chunk size used for db logging in batch mode
     * @param chunkSize
     * */
    public void setChunkSize( int chunkSize ) {

        this.chunkSize = chunkSize;
    }

    /**
     * @return the log level used for our DB appender
     */
    public int getChunkSize() {

        return this.chunkSize;
    }

    /**
     * @return the connection protocol
     */
    public String getConnectionProtocol() {

        return protocol;
    }

    /**
     * @param protocol the communication protocol
     */
    public void setConnectionProtocol( String protocol ) {

        this.protocol = protocol;
    }
}
