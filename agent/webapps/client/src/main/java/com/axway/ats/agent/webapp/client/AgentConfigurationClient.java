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
package com.axway.ats.agent.webapp.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.axway.ats.agent.core.MainComponentLoader;
import com.axway.ats.agent.core.action.ActionRequest;
import com.axway.ats.agent.core.configuration.AgentConfigurator;
import com.axway.ats.agent.core.configuration.Configurator;
import com.axway.ats.agent.core.configuration.GenericAgentConfigurator;
import com.axway.ats.agent.core.configuration.RemoteLoggingConfigurator;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.webapp.client.configuration.AgentConfigurationLandscape;
import com.axway.ats.agent.webapp.client.configuration.RemoteConfigurationManager;
import com.axway.ats.agent.webapp.client.executors.AbstractClientExecutor;
import com.axway.ats.agent.webapp.client.executors.LocalExecutor;
import com.axway.ats.agent.webapp.client.executors.RemoteExecutor;
import com.axway.ats.agent.webapp.client.listeners.TestcaseStateListener;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.system.LocalSystemOperations;
import com.axway.ats.log.LogLevel;
import com.axway.ats.log.model.CheckpointLogLevel;

/**
 * This configuration client is used in two ways:
 * <li>Initial Agent configuration usually used before starting the first test case.<br>
 * <blockquote>
 * On startup, the Agent service gets configured by the ats.agent.properties file placed in the war file. <br>
 * This client provides a way to change the Agent configuration settings while Agent service is up and running.<br>
 * <b>Note:</b> When using some of the methods here, the Agent will unload and load back all components when applying the new settings. All methods leading
 * to such behavior are noted in their javadoc<br><br>
 * </blockquote>
 *
 * <li>Runtime Agent configuration used at any time during test cases execution. This does not lead to Agent components reload.
 */
@PublicAtsApi
public final class AgentConfigurationClient extends ActionClient {

    /**
     * The file system folder where the Agent components will be searched. 
     * The default value is "/actions"
     */
    @PublicAtsApi
    public static final String AGENT_SETTINGS_COMPONENTS_FOLDER     = AgentConfigurator.COMPONENTS_FOLDER;
    /**
     * The polling interval (in seconds) while monitoring the components folder.
     * The default value is 5 seconds.
     */
    @PublicAtsApi
    public static final String AGENT_SETTINGS_MONITOR_POLL_INTERVAL = AgentConfigurator.MONITOR_POLL_INTERVAL;

    /**
     * @param atsAgent
     *            the ATS agent to work with - if you pass
     *            pass LOCAL_JVM, the action execution will be performed in the
     *            current JVM without routing through the web service
     */
    @PublicAtsApi
    public AgentConfigurationClient( String atsAgent ) {

        // this client works on the level of Agent distribution, 
        // not on the level of Agent components
        super(atsAgent, "some fake component");
    }

    /**
     * Applies the settings in the provided Agent configuration file. <br>
     * Consult with the online documentation for the available settings.
     * <br><b>Note:</b> This method causes reloading of all Agent components
     *
     * @param configurationFile Agent configuration file
     * @throws AgentException
     */
    @PublicAtsApi
    public void setAgentConfigurationFile( String configurationFile ) throws AgentException {

        // load the configuration file
        Properties agentSettings = new Properties();
        try (FileInputStream fis = new FileInputStream(new File(configurationFile))) {
            agentSettings.load(fis);
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to initialize Agent settings", ioe);
        }

        applyConfigurationAndReloadAgentComponents(agentSettings);
    }

    /**
     * Applies the settings in the provided Agent configuration settings.<br>
     * The available settings are exposed by the different AGENT_SETTINGS_*
     * constants
     * <br><b>Note:</b> This method causes reloading of all Agent components
     *
     * @param agentSettings a list with Agent settings to change
     * @throws AgentException
     */
    @PublicAtsApi
    public void setAgentConfiguration( Properties agentSettings ) throws AgentException {

        applyConfigurationAndReloadAgentComponents(agentSettings);
    }

    /**
     * Set the checkpoints log level.
     *
     * @param checkpointLogLevel
     * @throws AgentException
     */
    @PublicAtsApi
    public void setCheckpointLogLevel( CheckpointLogLevel checkpointLogLevel ) throws AgentException {

        GenericAgentConfigurator genericAgentConfigurator = new GenericAgentConfigurator();
        genericAgentConfigurator.setCheckpointLogLevel(checkpointLogLevel);

        applyConfiguration(genericAgentConfigurator);
    }

    /**
     * Set log level on ATS agent.
     * This will affect only the logs that are going to Test Explorer DB.
     *
     * @param logLevel the new level
     * @throws AgentException
     */
    @PublicAtsApi
    public void setLogLevel( LogLevel logLevel ) throws AgentException {

        AgentConfigurationLandscape.getInstance(atsAgent).setDbLogLevel(logLevel);

        RemoteLoggingConfigurator rlc = new RemoteLoggingConfigurator(logLevel,
                                                                      AgentConfigurationLandscape.getInstance(atsAgent)
                                                                                                 .getChunkSize());

        applyConfiguration(rlc);
    }

    /**
     * Set the chunk size for db operations, when batch mode is enabled
     * @param chunkSize - the size of the batch. Must be a positive (non-zero) number
     * @throws AgentException
     * */
    @PublicAtsApi
    public void setChunkSize( int chunkSize ) throws AgentException {

        try {
            if (chunkSize <= 0) {
                throw new IllegalArgumentException("Chunk size must be positive number");
            }
            AgentConfigurationLandscape.getInstance(atsAgent).setChunkSize(chunkSize);
            RemoteLoggingConfigurator rlc = new RemoteLoggingConfigurator(AgentConfigurationLandscape.getInstance(atsAgent)
                                                                                                     .getDbLogLevel(),
                                                                          chunkSize);

            applyConfiguration(rlc);

        } catch (Exception e) {
            throw new AgentException("Could not set chunk size to agent '" + atsAgent + "'", e);
        }

    }

    /**
     * Returns the path to the ATS agent location
     *
     * @throws AgentException
     */
    @PublicAtsApi
    public String getAgentHome() throws AgentException {

        if (atsAgent.equals(LOCAL_JVM)) {
            LocalExecutor localExecutor = new LocalExecutor();
            return localExecutor.getAgentHome();
        } else {
            RemoteExecutor remoteExecutor = new RemoteExecutor(atsAgent, false);
            return remoteExecutor.getAgentHome();
        }
    }

    /**
     * Returns the current number of log events that are still not persisted in the log DB
     * 
     * @return pending log events
     * @throws AgentException
     */
    @PublicAtsApi
    public int getNumberPendingLogEvents() throws AgentException {

        if (atsAgent.equals(LOCAL_JVM)) {
            LocalExecutor localExecutor = new LocalExecutor();
            return localExecutor.getNumberPendingLogEvents();
        } else {
            RemoteExecutor remoteExecutor = new RemoteExecutor(atsAgent, false);
            return remoteExecutor.getNumberPendingLogEvents();
        }
    }

    /**
     * Return array of all detected JARs from classpath
     * @throws AgentException 
     */
    @PublicAtsApi
    public List<String> getClassPath() throws AgentException {

        if (atsAgent.equals(LOCAL_JVM)) {
            LocalSystemOperations operations = new LocalSystemOperations();
            return Arrays.asList(operations.getClassPath());
        } else {
            RemoteExecutor remoteExecutor = new RemoteExecutor(atsAgent, false);
            return remoteExecutor.getClassPath();
        }
    }

    /**
     * Log all JARs in current application's ClassPath
     * @throws AgentException 
     */
    @PublicAtsApi
    public void logClassPath() throws AgentException {

        if (atsAgent.equals(LOCAL_JVM)) {
            LocalSystemOperations operations = new LocalSystemOperations();
            operations.logClassPath();
        } else {
            RemoteExecutor remoteExecutor = new RemoteExecutor(atsAgent, false);
            remoteExecutor.logClassPath();
        }
    }

    /**
     * Return array containing all duplicated jars in the ClassPath
     * @throws AgentException 
     */
    @PublicAtsApi
    public List<String> getDuplicatedJars() throws AgentException {

        if (atsAgent.equals(LOCAL_JVM)) {
            LocalSystemOperations operations = new LocalSystemOperations();
            return Arrays.asList(operations.getDuplicatedJars());
        } else {
            RemoteExecutor remoteExecutor = new RemoteExecutor(atsAgent, false);
            return remoteExecutor.getDuplicatedJars();
        }
    }

    /**
     * Log all duplicated JARs in current application's ClassPath
     * @throws AgentException 
     */
    @PublicAtsApi
    public void logDuplicatedJars() throws AgentException {

        if (atsAgent.equals(LOCAL_JVM)) {
            LocalSystemOperations operations = new LocalSystemOperations();
            operations.logDuplicatedJars();
        } else {
            RemoteExecutor remoteExecutor = new RemoteExecutor(atsAgent, false);
            remoteExecutor.logDuplicatedJars();
        }
    }

    /**
     * The usage of this method concerns the very rare case when same user on same host is running 
     * simultaneous executions of more than one test runs and these tests are sending requests to one and same ATS Agent!
     * If this is not your case, you do not have to worry about the meaning of this method.
     * An example of such rare case is if some CI machine is running some scheduled test using same CI user on same CI host
     * and you want to run other test, using the same agent for executing them, without interrupting any previous test on the agent.
     * 
     * @throws AgentException
     */
    @PublicAtsApi
    public void setUseNewCallerIdOnEachRun() throws AgentException {

        AgentServicePool.useNewUniqueId();
    }

    /**
     * Set using of HTTPS communication protocol
     *
     */
    @PublicAtsApi
    public void useHttpsConnection() {

        AgentConfigurationLandscape.getInstance(atsAgent).setConnectionProtocol("https");
    }

    /**
     * Apply the new settings
     *
     * @param agentSettings a list with Agent settings to change
     * @throws AgentException
     */
    private void applyConfigurationAndReloadAgentComponents( Properties agentSettings ) throws AgentException {

        // include the Agent configuration
        AgentConfigurator agentConfigurator = new AgentConfigurator(agentSettings);

        List<Configurator> configurators = new ArrayList<Configurator>();
        configurators.add(agentConfigurator);

        if (atsAgent.equals(LOCAL_JVM)) {
            // executed in the JVM of the test executor

            // the already loaded Agent components are first unloaded
            MainComponentLoader.getInstance().destroy();

            // the initialization procedure implicitly applies the new configurations 
            // and then loads up again the Agent components
            MainComponentLoader.getInstance().initialize(configurators);
        } else {
            // send the new configuration to the remote Agent instance
            new RemoteConfigurationManager().pushConfiguration(atsAgent, agentConfigurator);
        }
    }

    /**
     * Apply the new settings of the provided configurator
     *
     * @param configurator
     * @throws AgentException
     */
    private void applyConfiguration( Configurator configurator ) throws AgentException {

        if (atsAgent.equals(LOCAL_JVM)) {
            // executed in the JVM of the test executor
            configurator.apply();
        } else {
            // send the new configuration to the remote Agent instance
            new RemoteConfigurationManager().pushConfiguration(atsAgent, configurator);
        }
    }

    /**
     * Tells if an Agent component is loaded, so its actions can be called
     *
     * @param componentName the name of the component
     * @return whether it is available
     */
    @PublicAtsApi
    public boolean isComponentLoaded( String componentName ) throws AgentException {

        // construct an action request
        ActionRequest actionRequest = new ActionRequest(componentName, "fake action name", new Object[]{});

        if (atsAgent.equals(LOCAL_JVM)) {
            LocalExecutor localExecutor = new LocalExecutor();
            return localExecutor.isComponentLoaded(actionRequest);
        } else {
            RemoteExecutor remoteExecutor = new RemoteExecutor(atsAgent, false);
            return remoteExecutor.isComponentLoaded(actionRequest);
        }
    }

    /**
     * Tells if an Agent component is loaded, so its actions can be called.
     * This method will wait for the specified timeout period until the wanted condition is met.
     *
     * @param componentName the name of the component
     * @param timeout maximum timeout (in seconds) to wait for the component to be loaded.
     * @return whether it is available
     */
    @PublicAtsApi
    public boolean isComponentLoaded( String componentName, int timeout ) throws AgentException {

        // construct an action request
        ActionRequest actionRequest = new ActionRequest(componentName, "fake action name", new Object[]{});

        AbstractClientExecutor executor;
        if (atsAgent.equals(LOCAL_JVM)) {
            executor = new LocalExecutor();
        } else {
            executor = new RemoteExecutor(atsAgent, false);
        }

        long startTimestamp = System.currentTimeMillis();
        while (true) {
            if (executor.isComponentLoaded(actionRequest)) {
                // it is loaded
                return true;
            } else if (System.currentTimeMillis() - startTimestamp > timeout * 1000) {
                // not loaded for the whole timeout
                return false;
            } else {
                // not loaded, but we will check again
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
            }
        }
    }

    /** Mark the ATS Agent as non-configured
     * @see AgentConfigurationClient#invalidateAtsDbLogConfiguration(List)
     */
    @PublicAtsApi
    public void invalidateAtsDbLogConfiguration() {

        List<String> atsAgents = new ArrayList<String>();
        atsAgents.add(this.atsAgent);
        invalidateAtsDbLogConfiguration(atsAgents);
    }

    /**
     * Explicitly mark ATS agent(s) as not configured. If any of the agent was not previously configured, no error will be thrown<br>
     * Note that this method does not perform any operation to the actual agents, neither checks if agent is still running in the provided address
     */
    public static void invalidateAtsDbLogConfiguration( List<String> atsAgents ) {

        TestcaseStateListener.getInstance()
                             .invalidateConfiguredAtsAgents(atsAgents);
    }
}