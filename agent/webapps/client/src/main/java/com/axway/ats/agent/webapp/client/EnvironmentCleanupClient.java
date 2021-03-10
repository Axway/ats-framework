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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.axway.ats.agent.core.MainComponentLoader;
import com.axway.ats.agent.core.configuration.Configurator;
import com.axway.ats.agent.core.configuration.EnvironmentConfigurator;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.webapp.client.configuration.RemoteConfigurationManager;
import com.axway.ats.agent.webapp.client.executors.LocalExecutor;
import com.axway.ats.agent.webapp.client.executors.RemoteExecutor;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.utils.StringUtils;

@PublicAtsApi
public class EnvironmentCleanupClient extends AbstractAgentClient {

    /**
     * Constructor for this class - sets the host on which the cleanup to be performed
     *
     * @param atsAgent the host on which to perform the cleanup
     */
    @PublicAtsApi
    public EnvironmentCleanupClient( String atsAgent ) {

        super(atsAgent, null);
    }

    /**
     * Restore the environment for this component
     *
     * @param componentName name of the component
     * @throws AgentException
     */
    @PublicAtsApi
    public void restore( String componentName ) throws AgentException {

        log.info("Executing restore for component '" + componentName + "'");

        if (atsAgent.equals(LOCAL_JVM)) {
            LocalExecutor localExecutor = new LocalExecutor();
            localExecutor.restore(componentName, null, null);
        } else {
            RemoteExecutor remoteExecutor = new RemoteExecutor(atsAgent);
            remoteExecutor.restore(componentName, null, null);
        }

        log.info("Finished executing restore for component '" + componentName + "'");
    }

    /**
     * Restore the environment for given component with a specific environment configuration
     *
     * @param componentName the name of the component
     * @param environmentName the name of the environment configuration
     * @throws AgentException
     */
    @PublicAtsApi
    public void restore( String componentName, String environmentName ) throws AgentException {

        log.info("Executing restore for component '" + componentName + "' using environment configuration '"
                 + environmentName + "'");

        if (atsAgent.equals(LOCAL_JVM)) {
            LocalExecutor localExecutor = new LocalExecutor();
            localExecutor.restore(componentName, environmentName, null);
        } else {
            RemoteExecutor remoteExecutor = new RemoteExecutor(atsAgent);
            remoteExecutor.restore(componentName, environmentName, null);
        }

        log.info("Finished executing restore for component '" + componentName
                 + "' using environment configuration '" + environmentName + "'");
    }

    /**
     * Restore the component environment from a specific folder
     *
     * @param componentName the name of the component
     * @param folderPath the backup folder to use for restore
     * @throws AgentException
     */
    @PublicAtsApi
    public void restoreFrom( String componentName, String folderPath ) throws AgentException {

        log.info("Executing restore for component '" + componentName + "' from folder '" + folderPath
                 + "'");

        if (atsAgent.equals(LOCAL_JVM)) {
            LocalExecutor localExecutor = new LocalExecutor();
            localExecutor.restore(componentName, null, folderPath);
        } else {
            RemoteExecutor remoteExecutor = new RemoteExecutor(atsAgent);
            remoteExecutor.restore(componentName, null, folderPath);
        }

        log.info("Finished executing restore for component '" + componentName + "' from folder '"
                 + folderPath + "'");
    }

    /**
     * Restore the component environment from a specific folder using a specific environment configuration
     *
     * @param componentName the name of the component
     * @param environmentName the name of the environment configuration
     * @param folderPath the backup folder to use for restore
     * @throws AgentException
     */
    @PublicAtsApi
    public void restoreFrom( String componentName, String environmentName,
                             String folderPath ) throws AgentException {

        log.info("Executing restore for component '" + componentName + "' using environment '"
                 + environmentName + "' from folder '" + folderPath + "'");

        if (atsAgent.equals(LOCAL_JVM)) {
            LocalExecutor localExecutor = new LocalExecutor();
            localExecutor.restore(componentName, environmentName, folderPath);
        } else {
            RemoteExecutor remoteExecutor = new RemoteExecutor(atsAgent);
            remoteExecutor.restore(componentName, environmentName, folderPath);
        }

        log.info("Finished executing restore for component '" + componentName + "' using environment '"
                 + environmentName + "' from folder '" + folderPath + "'");
    }

    /**
     * Restore the environment for all components
     *
     * @throws AgentException
     */
    @PublicAtsApi
    public void restoreAllComponents() throws AgentException {

        log.info("Executing restore for all registered components");

        if (atsAgent.equals(LOCAL_JVM)) {
            LocalExecutor localExecutor = new LocalExecutor();
            localExecutor.restoreAll(null);
        } else {
            RemoteExecutor remoteExecutor = new RemoteExecutor(atsAgent);
            remoteExecutor.restoreAll(null);
        }

        log.info("Finished executing restore for all registered components");
    }

    /**
     * Restore the environment for all components with a specific environment configuration
     * Keep in mind that all the components must have environment configuration with this name
     *
     * @throws AgentException
     */
    @PublicAtsApi
    public void restoreAllComponents( String environmentName ) throws AgentException {

        log.info("Executing cleanup for all registered components using environment configuration '"
                 + environmentName + "'");

        if (atsAgent.equals(LOCAL_JVM)) {
            LocalExecutor localExecutor = new LocalExecutor();
            localExecutor.restoreAll(environmentName);
        } else {
            RemoteExecutor remoteExecutor = new RemoteExecutor(atsAgent);
            remoteExecutor.restoreAll(environmentName);
        }

        log.info("Finished executing cleanup for all registered components using environment configuration '"
                 + environmentName + "'");
    }

    /**
     * Create backup for given component
     *
     * @param componentName the name of the component
     * @throws AgentException
     */
    @PublicAtsApi
    public void backup( String componentName ) throws AgentException {

        log.info("Executing backup for component '" + componentName + "'");

        if (atsAgent.equals(LOCAL_JVM)) {
            LocalExecutor localExecutor = new LocalExecutor();
            localExecutor.backup(componentName, null, null);
        } else {
            RemoteExecutor remoteExecutor = new RemoteExecutor(atsAgent);
            remoteExecutor.backup(componentName, null, null);
        }

        log.info("Finished executing backup for component '" + componentName + "'");
    }

    /**
     * Create backup for given component with a specific environment configuration
     *
     * @param componentName the name of the component
     * @param environmentName the name of the environment configuration
     * @throws AgentException
     */
    @PublicAtsApi
    public void backup( String componentName, String environmentName ) throws AgentException {

        log.info("Executing backup for component '" + componentName + "' using environment '"
                 + environmentName + "'");

        if (atsAgent.equals(LOCAL_JVM)) {
            LocalExecutor localExecutor = new LocalExecutor();
            localExecutor.backup(componentName, environmentName, null);
        } else {
            RemoteExecutor remoteExecutor = new RemoteExecutor(atsAgent);
            remoteExecutor.backup(componentName, environmentName, null);
        }

        log.info("Finished executing backup for component '" + componentName + "' using environment '"
                 + environmentName + "'");
    }

    /**
     * Create backup for given component to specific backup folder
     *
     * @param componentName the name of the component
     * @param folderPath backup folder path
     * @throws AgentException
     */
    @PublicAtsApi
    public void backupTo( String componentName, String folderPath ) throws AgentException {

        log.info("Executing backup for component '" + componentName + "' to folder '" + folderPath + "'");

        if (atsAgent.equals(LOCAL_JVM)) {
            LocalExecutor localExecutor = new LocalExecutor();
            localExecutor.backup(componentName, null, folderPath);
        } else {
            RemoteExecutor remoteExecutor = new RemoteExecutor(atsAgent);
            remoteExecutor.backup(componentName, null, folderPath);
        }

        log.info("Finished executing backup for component '" + componentName + "' to folder '" + folderPath
                 + "'");
    }

    /**
     * Create backup for given component using specific environment configuration to a specific backup folder
     *
     * @param componentName the name of the component
     * @param environmentName the name of the environment configuration
     * @param folderPath backup folder path
     * @throws AgentException
     */
    @PublicAtsApi
    public void backupTo( String componentName, String environmentName,
                          String folderPath ) throws AgentException {

        log.info("Executing backup for component '" + componentName + "' using environment configuration '"
                 + environmentName + "' to folder '" + folderPath + "'");

        if (atsAgent.equals(LOCAL_JVM)) {
            LocalExecutor localExecutor = new LocalExecutor();
            localExecutor.backup(componentName, environmentName, folderPath);
        } else {
            RemoteExecutor remoteExecutor = new RemoteExecutor(atsAgent);
            remoteExecutor.backup(componentName, environmentName, folderPath);
        }

        log.info("Finished executing backup for component '" + componentName
                 + "' using environment configuration '" + environmentName + "' to folder '" + folderPath
                 + "'");
    }

    /**
     * Create backup for all registered components
     *
     * @throws AgentException
     */
    @PublicAtsApi
    public void backupAllComponents() throws AgentException {

        log.info("Executing backup for all registered components");

        if (atsAgent.equals(LOCAL_JVM)) {
            LocalExecutor localExecutor = new LocalExecutor();
            localExecutor.backupAll(null);
        } else {
            RemoteExecutor remoteExecutor = new RemoteExecutor(atsAgent);
            remoteExecutor.backupAll(null);
        }

        log.info("Finished executing backup for all registered components");
    }

    /**
     * Create backup for all registered components with a specific environment configuration
     * Keep in mind that all the components must have environment configuration with this name
     *
     * @param environmentName the name of the environment configuration
     * @throws AgentException
     */
    @PublicAtsApi
    public void backupAllComponents( String environmentName ) throws AgentException {

        log.info("Executing backup for all registered components using environment configuration '"
                 + environmentName + "'");

        if (atsAgent.equals(LOCAL_JVM)) {
            LocalExecutor localExecutor = new LocalExecutor();
            localExecutor.backupAll(environmentName);
        } else {
            RemoteExecutor remoteExecutor = new RemoteExecutor(atsAgent);
            remoteExecutor.backupAll(environmentName);
        }

        log.info("Finished executing backup for all registered components using environment configuration '"
                 + environmentName + "'");
    }

    /**
     * Set database connection properties for the first database (element) listed in the configuration file (descriptor xml)
     *
     * @param dbHost database host
     * @param dbName database name
     * @param dbPort database port
     * @param userName user name
     * @param userPassword user password
     * @throws AgentException
     */
    @PublicAtsApi
    public void setDatabaseConnection( String dbHost, String dbName, int dbPort, String userName,
                                       String userPassword ) throws AgentException {

        setDatabaseConnection(0, dbHost, dbName, dbPort, userName, userPassword);
    }

    /**
     * Set database connection properties for the specified database listed in the configuration file (descriptor xml)
     *
     * @param databaseIndex - the number(index) of database element in the configuration file (descriptor xml)
     * @param dbHost database host
     * @param dbName database name
     * @param dbPort database port
     * @param userName user name
     * @param userPassword user password
     * @throws AgentException
     */
    @PublicAtsApi
    public void setDatabaseConnection( int databaseIndex, String dbHost, String dbName, int dbPort,
                                       String userName, String userPassword ) throws AgentException {

        Properties dbProperties = new Properties();
        dbProperties.put(EnvironmentConfigurator.DB_CONFIGURATION_INDEX, databaseIndex);
        if (!StringUtils.isNullOrEmpty(dbHost)) {
            dbProperties.put(EnvironmentConfigurator.DB_HOST, dbHost);
        }
        if (dbPort > 0) {
            dbProperties.put(EnvironmentConfigurator.DB_PORT, dbPort);
        }
        if (!StringUtils.isNullOrEmpty(dbName)) {
            dbProperties.put(EnvironmentConfigurator.DB_NAME, dbName);
        }
        if (!StringUtils.isNullOrEmpty(userName)) {
            dbProperties.put(EnvironmentConfigurator.DB_USER_NAME, userName);
        }
        if (!StringUtils.isNullOrEmpty(userPassword)) {
            dbProperties.put(EnvironmentConfigurator.DB_USER_PASSWORD, userPassword);
        }

        List<Properties> dbPropertiesList = new ArrayList<Properties>();
        dbPropertiesList.add(dbProperties);

        EnvironmentConfigurator envronmentConfigurator = new EnvironmentConfigurator(component,
                                                                                     dbPropertiesList);
        pushConfiguration(envronmentConfigurator);
    }

    /**
     * Apply the new settings
     *
     * @param agentSettings a list with Agent settings to change
     * @throws AgentException
     */
    private void pushConfiguration( Configurator configurator ) throws AgentException {

        List<Configurator> configurators = new ArrayList<Configurator>();
        configurators.add(configurator);

        if (atsAgent.equals(LOCAL_JVM)) {
            // the already loaded Agent components are first unloaded
            MainComponentLoader.getInstance().destroy();

            // the initialization procedure implicitly applies the new configurations and then loads up again the Agent components
            MainComponentLoader.getInstance().initialize(configurators);
        } else {
            // send the Agent configuration
            new RemoteConfigurationManager().pushConfiguration(atsAgent, configurator);
        }
    }
}
