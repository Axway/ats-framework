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
package com.axway.ats.agent.core.configuration;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.agent.core.ComponentEnvironment;
import com.axway.ats.agent.core.ComponentRepository;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.core.dbaccess.DatabaseProviderFactory;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.environment.EnvironmentUnit;
import com.axway.ats.environment.database.DatabaseEnvironmentUnit;

@SuppressWarnings( "serial")
public class EnvironmentConfigurator implements Configurator {

    private static final Logger log                    = LogManager.getLogger(EnvironmentConfigurator.class);

    public static final String  DB_CONFIGURATION_INDEX = "DB_CONFIGURATION_INDEX";

    public static final String  DB_HOST                = "DB_HOST";
    public static final String  DB_NAME                = "DB_NAME";
    public static final String  DB_PORT                = "DB_PORT";
    public static final String  DB_USER_NAME           = "DB_USER_NAME";
    public static final String  DB_USER_PASSWORD       = "DB_USER_PASSWORD";

    private String              component;
    private List<Properties>    dbPropertiesList;

    public EnvironmentConfigurator( String component,
                                    List<Properties> dbPropertiesList ) {

        this.component = component;
        this.dbPropertiesList = dbPropertiesList;
    }

    @Override
    public void apply() throws ConfigurationException {

        for (Properties dbProperties : dbPropertiesList) {
            int dbConfigurationIndex = -1;
            if (dbProperties.get(DB_CONFIGURATION_INDEX) != null) {
                dbConfigurationIndex = (Integer) dbProperties.get(DB_CONFIGURATION_INDEX);
            }

            ComponentRepository componentRepository = ComponentRepository.getInstance();
            ComponentEnvironment componentEnvironment;
            try {
                componentEnvironment = componentRepository.getComponentEnvironment(component);
            } catch (NoSuchComponentException nsce) {
                throw new ConfigurationException("Error changing environment configuration. CAUSE: "
                                                 + nsce.getMessage());
            }

            int foundDbConfigurationIndex = 0;
            for (EnvironmentUnit environmentUnit : componentEnvironment.getEnvironmentUnits()) {
                if (environmentUnit instanceof DatabaseEnvironmentUnit) {
                    if (foundDbConfigurationIndex == dbConfigurationIndex) {
                        DatabaseEnvironmentUnit dbEnvironmentUnit = (DatabaseEnvironmentUnit) environmentUnit;
                        DbConnection dbConnection = dbEnvironmentUnit.getDbConnection();

                        // the database port is kept in a list of custom properties
                        Map<String, Object> customProperties = dbConnection.getCustomProperties();

                        // get the new configuration properties
                        String newDbHost = chooseNewProperty(dbProperties.getProperty(DB_HOST),
                                                             dbConnection.getHost());
                        String newDbName = chooseNewProperty(dbProperties.getProperty(DB_NAME),
                                                             dbConnection.getDb());
                        String newDbUserName = chooseNewProperty(dbProperties.getProperty(DB_USER_NAME),
                                                                 dbConnection.getUser());
                        String newDbUserPassword = chooseNewProperty(dbProperties.getProperty(DB_USER_PASSWORD),
                                                                     dbConnection.getPassword());
                        Object newDbPort = chooseDbPort(dbProperties.get(DB_PORT),
                                                        customProperties.get(DbKeys.PORT_KEY));

                        // create a new connection object
                        customProperties.put(DbKeys.PORT_KEY, newDbPort);
                        DbConnection newDbConnection = DatabaseProviderFactory.createDbConnection(dbConnection.getDbType(),
                                                                                                  newDbHost,
                                                                                                  -1,
                                                                                                  newDbName,
                                                                                                  newDbUserName,
                                                                                                  newDbUserPassword,
                                                                                                  customProperties);

                        // apply the changes
                        dbEnvironmentUnit.setDbConnection(newDbConnection);

                        log.info("Database configuration for index " + dbConfigurationIndex
                                 + " is changed. DbConnection: " + newDbConnection.getDescription());
                        return;
                    } else {
                        // still searching the exact database configuration
                        foundDbConfigurationIndex++;
                    }
                }
            }

            throw new ConfigurationException("Database configuration with index " + dbConfigurationIndex
                                             + " is not available");
        }
    }

    @Override
    public boolean needsApplying() {

        return true;
    }

    @Override
    public void revert() throws ConfigurationException {

    }

    /**
     * 
     * @param newDbPort - new database port as {@link Integer} or null if not set
     * @param currentDbPort current database port
     * @return the chosen database port as {@link String}
     */
    private Object chooseDbPort(
                                 Object newDbPort,
                                 Object currentDbPort ) {

        if (newDbPort != null) {
            return newDbPort;
        } else {
            return currentDbPort;
        }
    }

    private String chooseNewProperty(
                                      String newProperty,
                                      String currentProperty ) {

        if (newProperty != null) {
            return newProperty;
        } else {
            return currentProperty;
        }
    }

    @Override
    public String getDescription() {

        return "environment configuration";
    }
}
