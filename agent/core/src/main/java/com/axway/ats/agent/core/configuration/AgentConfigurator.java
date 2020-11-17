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
package com.axway.ats.agent.core.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.systemproperties.AtsSystemProperties;

/**
 * It is run explicitly from the tests, probably just once for the whole set of
 * tests in some "setUp" like method
 */
@SuppressWarnings( "serial")
public class AgentConfigurator implements Configurator {

    private static final Logger        log                               = LogManager.getLogger(AgentConfigurator.class);

    private static final String        SETTINGS_FILENAME                 = "ats.agent.properties";

    // the supported keys - could be read from file, system property or set
    public static final String         MONITOR_POLL_INTERVAL             = AtsSystemProperties.AGENT__MONITOR_POLL_INTERVAL;
    public static final String         MONITOR_INITIAL_DELAY_BEFORE_POLL = AtsSystemProperties.AGENT__MONITOR_INITIAL_POLL_DELAY;
    public static final String         COMPONENTS_FOLDER                 = AtsSystemProperties.AGENT__COMPONENTS_FOLDER;

    // properties passed remotely from the test executor
    private Properties                 agentRemoteProperties             = new Properties();

    // properties coming from a local file(e.g. on the same physical machine where the Agent instance is running)
    private Properties                 agentFileProperties               = new Properties();

    /**
     * This constructor is called by the container during the initialization phase
     *
     * @param pathToConfigFile path to Agent configuration file
     */
    public AgentConfigurator( String pathToConfigFile ) {

        try (FileInputStream fis = new FileInputStream(new File(pathToConfigFile, SETTINGS_FILENAME))) {
            agentFileProperties.load(fis);
        } catch (Exception e) {
            // Agent settings file not provided, or there is an error reading it
            log.debug("Could not load " + SETTINGS_FILENAME + " from directory " + pathToConfigFile);
        }
    }

    /**
     * This constructor is called by someone when setting the Agent remotely
     *
     * @param agentProperties the new settings
     */
    public AgentConfigurator( Properties agentProperties ) {

        this.agentRemoteProperties = agentProperties;
    }

    @Override
    public void apply() throws ConfigurationException {

        ConfigurationSettings configSettings = ConfigurationSettings.getInstance();

        /*
         *  Sequence of searching for required settings:
         *
         *  We will use the setting provided remotely. If not provided we will use the last value,
         *  but when this is the first time(e.g. during Agent loading) we will:
         *   - use the one provided as a system property
         *   - if not provided, use the one provided from an Agent configuration file(placed in the Agent war file)
         *   - if not provided, use the default setting
         */

        // set the Agent components folder
        setAgentComponentsFolder(configSettings);

        // set initial delay before start for polling
        setMonitorInitialDelay(configSettings);

        // set the poll interval
        setMonitorPollInterval(configSettings);

    }

    private void setAgentComponentsFolder( ConfigurationSettings configSettings ) {

        String loadSource;

        // value provided from remote Agent client
        String oldComponentsFolder = configSettings.getComponentsFolder();
        String newComponentsFolder = agentRemoteProperties.getProperty(COMPONENTS_FOLDER);
        loadSource = "remotely provided value";

        // look for other configuration ways just the first time(e.g. during load time)
        if (newComponentsFolder == null && oldComponentsFolder == null) {
            // look for other configuration ways just the first time(e.g. during load time)

            // value provided as a system property
            newComponentsFolder = AtsSystemProperties.getPropertyAsString(COMPONENTS_FOLDER);
            loadSource = "system property value";
            if (newComponentsFolder == null) {

                // value provided from an Agent configuration file(placed in the Agent war file)
                newComponentsFolder = agentFileProperties.getProperty(COMPONENTS_FOLDER);
                loadSource = "Agent configuration file value";
                if (newComponentsFolder == null) {

                    // default value
                    newComponentsFolder = "../actions";
                    loadSource = "default value";
                }
            }
        }

        if (newComponentsFolder != null && !newComponentsFolder.equals(oldComponentsFolder)) {
            configSettings.setComponentsFolder(newComponentsFolder);
            log.info("Agent components are expected in '" + newComponentsFolder + "' folder - using "
                     + loadSource);
        } else {
            log.info("Agent components are expected in '" + oldComponentsFolder + "' folder");
        }
    }

    private void setMonitorPollInterval( ConfigurationSettings configSettings ) {

        String loadSource;

        // value provided from remote Agent client
        int oldMonitorPollInterval = configSettings.getMonitorPollInterval();
        int newMonitorPollInterval = parseStringAsNonNegativeNumber(agentRemoteProperties.getProperty(MONITOR_POLL_INTERVAL));
        loadSource = "remotely provided value";

        if (newMonitorPollInterval == 0 && oldMonitorPollInterval == 0) {
            // look for other configuration ways just the first time(e.g. during load time)

            // value provided as a system property
            newMonitorPollInterval = AtsSystemProperties.getPropertyAsNonNegativeNumber(MONITOR_POLL_INTERVAL,
                                                                                        0);
            loadSource = "system property value";
            if (newMonitorPollInterval == 0) {

                // value provided from an Agent configuration file(placed in the Agent war file)
                newMonitorPollInterval = parseStringAsNonNegativeNumber(agentFileProperties.getProperty(MONITOR_POLL_INTERVAL));
                loadSource = "Agent configuration file value";
                if (newMonitorPollInterval == 0) {

                    // default value
                    newMonitorPollInterval = 5;
                    loadSource = "default value";
                }
            }
        }

        if (newMonitorPollInterval != 0 && newMonitorPollInterval != oldMonitorPollInterval) {
            configSettings.setMonitorPollInterval(newMonitorPollInterval);
            log.info("Agent components poll interval is " + newMonitorPollInterval + " seconds - using "
                     + loadSource);
        } else {
            log.info("Agent components poll interval is " + oldMonitorPollInterval + " seconds");
        }
    }

    private void setMonitorInitialDelay( ConfigurationSettings configSettings ) {

        String loadSource = null;

        // do not check settings provided from remote Agent client as it is too late for this
        int oldMonitorInitialDelay = configSettings.getMonitorInitialDelay();
        if (oldMonitorInitialDelay == 0) {
            // look for other configuration ways just the first time(e.g. during load time)

            // value provided as a system property
            int newMonitorInitialDelay = AtsSystemProperties.getPropertyAsNonNegativeNumber(MONITOR_INITIAL_DELAY_BEFORE_POLL,
                                                                                            0);
            loadSource = "system property value";
            if (newMonitorInitialDelay == 0) {

                // value provided from an Agent configuration file(placed in the Agent WAR file)
                newMonitorInitialDelay = parseStringAsNonNegativeNumber(agentFileProperties.getProperty(MONITOR_INITIAL_DELAY_BEFORE_POLL));
                loadSource = "Agent configuration file value";
                if (newMonitorInitialDelay == 0) {

                    // default value
                    newMonitorInitialDelay = 0;
                    loadSource = "default value";
                }
            }

            configSettings.setMonitorInitialDelay(newMonitorInitialDelay);
            log.info("Agent components loading initial delay  is set to " + newMonitorInitialDelay
                     + " seconds using " + loadSource);
        }
    }

    @Override
    public boolean needsApplying() throws ConfigurationException {

        // always apply
        return true;
    }

    @Override
    public void revert() throws ConfigurationException {

        // do nothing
    }

    private static int parseStringAsNonNegativeNumber( String valueAsString ) {

        int intValue = 0;
        if (valueAsString != null) {
            try {
                intValue = Integer.parseInt(valueAsString);
            } catch (NumberFormatException nfe) {
                log.warn("String '" + valueAsString
                         + "' could not be parsed as a valid number and will be ignored", nfe);
            }
            if (intValue < 0) {
                log.warn(new IllegalArgumentException("String '" + valueAsString
                                                      + "' is not a positive number and will be ignored"));
                intValue = 0;
            }
        }
        return intValue;
    }

    @Override
    public String getDescription() {

        return "Agent configuration";
    }
}
