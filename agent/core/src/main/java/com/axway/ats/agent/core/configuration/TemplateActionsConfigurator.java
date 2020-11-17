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
public class TemplateActionsConfigurator implements Configurator {

    private static final long   serialVersionUID                        = 1L;

    private static final Logger log                                     = LogManager.getLogger(TemplateActionsConfigurator.class);

    private static final String SETTINGS_FILENAME                       = "ats.agent.properties";

    // the supported keys
    public static final String  AGENT__TEMPLATE_ACTIONS_FOLDER_PROPERTY = AtsSystemProperties.AGENT__TEMPLATE_ACTIONS_FOLDER;
    public static final String  MATCH_FILES_BY_SIZE                     = AtsSystemProperties.AGENT__TEMPLATE_ACTIONS_MATCH_FILES_BY_SIZE;
    public static final String  MATCH_FILES_BY_CONTENT                  = AtsSystemProperties.AGENT__TEMPLATE_ACTIONS_MATCH_FILES_BY_CONTENT;

    // properties passed remotely from the test executor
    private Properties          agentRemoteProperties                   = new Properties();

    // properties coming from a local file(e.g. on the same physical machine where the Agent instance is running)
    private Properties          agentFileProperties                     = new Properties();

    /**
     * This constructor is called by the container during the initialization phase
     *
     * @param pathToConfigFile path to Agent configuration file
     */
    public TemplateActionsConfigurator( String pathToConfigFile ) {

        try (FileInputStream fis = new FileInputStream(new File(pathToConfigFile, SETTINGS_FILENAME))) {
            agentFileProperties.load(fis);
        } catch (Exception e) {
            // Agent settings file not provided, or there is an error reading it
        }
    }

    /**
     * This constructor is called by someone when setting the Agent remotely
     *
     * @param agentProperties the new settings
     */
    public TemplateActionsConfigurator( Properties agentProperties ) {

        this.agentRemoteProperties = agentProperties;
    }

    @Override
    public void apply() throws ConfigurationException {

        ConfigurationSettings configSettings = ConfigurationSettings.getInstance();

        // set the home folder with template actions
        setAgentTemplateActionsFolder(configSettings);

        // set the boolean value for downloaded files verification method - by size or not
        setMatchFilesBySize(configSettings);

        // set the boolean value for downloaded files verification method - by its content or not
        setMatchFilesByContent(configSettings);
    }

    private void setMatchFilesByContent( ConfigurationSettings configSettings ) {

        String loadSource;

        String oldMatchFilesByContent = configSettings.isTemplateActionsMatchFilesByContent() == null
                                                                                                      ? null
                                                                                                      : configSettings.isTemplateActionsMatchFilesByContent()
                                                                                                                      .toString();
        // value provided from remote Agent client
        String matchFilesByContent = agentRemoteProperties.getProperty(MATCH_FILES_BY_CONTENT);
        loadSource = "remotely provided value";

        if (matchFilesByContent == null && oldMatchFilesByContent == null) {

            // value provided as a system property
            loadSource = "system property value";
            if (null == AtsSystemProperties.getPropertyAsBoolean(MATCH_FILES_BY_CONTENT)) {

                // default value
                matchFilesByContent = "false";
                loadSource = "default value";
            } else {
                matchFilesByContent = String.valueOf(AtsSystemProperties.getPropertyAsBoolean(MATCH_FILES_BY_CONTENT));
            }
        }

        if (matchFilesByContent != null && !matchFilesByContent.equals(oldMatchFilesByContent)) {

            configSettings.setTemplateActionsMatchFilesByContent(Boolean.parseBoolean(matchFilesByContent));
            log.info("Agent template actions will " + (configSettings.isTemplateActionsMatchFilesByContent()
                                                                                                             ? ""
                                                                                                             : "NOT ")
                     + "verify downloaded files by its content - using " + loadSource);
        } else {
            log.info("Agent template actions will " + (configSettings.isTemplateActionsMatchFilesByContent()
                                                                                                             ? ""
                                                                                                             : "NOT ")
                     + "verify downloaded files by its content");
        }
    }

    private void setMatchFilesBySize( ConfigurationSettings configSettings ) {

        String loadSource;

        String oldMatchFilesBySize = configSettings.isTemplateActionsMatchFilesBySize() == null
                                                                                                ? null
                                                                                                : configSettings.isTemplateActionsMatchFilesBySize()
                                                                                                                .toString();
        // value provided from remote Agent client
        String matchFilesBySize = agentRemoteProperties.getProperty(MATCH_FILES_BY_SIZE);
        loadSource = "remotely provided value";

        if (matchFilesBySize == null && oldMatchFilesBySize == null) {

            // value provided as a system property
            loadSource = "system property value";
            if (null == AtsSystemProperties.getPropertyAsBoolean(MATCH_FILES_BY_SIZE)) {

                // default value
                matchFilesBySize = "false";
                loadSource = "default value";
            } else {
                matchFilesBySize = String.valueOf(AtsSystemProperties.getPropertyAsBoolean(MATCH_FILES_BY_SIZE));
            }
        }

        if (matchFilesBySize != null && !matchFilesBySize.equals(oldMatchFilesBySize)) {

            configSettings.setTemplateActionsMatchFilesBySize(Boolean.parseBoolean(matchFilesBySize));
            log.info("Agent template actions will " + (configSettings.isTemplateActionsMatchFilesBySize()
                                                                                                          ? ""
                                                                                                          : "NOT ")
                     + "verify downloaded files by size - using " + loadSource);
        } else {
            log.info("Agent template actions will " + (configSettings.isTemplateActionsMatchFilesBySize()
                                                                                                          ? ""
                                                                                                          : "NOT ")
                     + "verify downloaded files by size");
        }
    }

    private void setAgentTemplateActionsFolder( ConfigurationSettings configSettings ) {

        String loadSource;

        String oldTemplateActionsFolder = configSettings.getTemplateActionsFolder();
        // value provided from remote Agent client
        String templateActionsFolder = agentRemoteProperties.getProperty(AGENT__TEMPLATE_ACTIONS_FOLDER_PROPERTY);
        loadSource = "remotely provided value";

        if (templateActionsFolder == null && oldTemplateActionsFolder == null) {

            // value provided as a system property
            templateActionsFolder = AtsSystemProperties.getPropertyAsString(AGENT__TEMPLATE_ACTIONS_FOLDER_PROPERTY);
            loadSource = "system property value";
            if (templateActionsFolder == null) {

                // value provided from an Agent configuration file(placed in the Agent war file)
                templateActionsFolder = agentFileProperties.getProperty(AGENT__TEMPLATE_ACTIONS_FOLDER_PROPERTY);
                loadSource = "Agent configuration file value";
                if (templateActionsFolder == null) {

                    // default value
                    templateActionsFolder = "../templateactions";
                    loadSource = "default value";
                }
            }
        }

        if (templateActionsFolder != null && !templateActionsFolder.equals(oldTemplateActionsFolder)) {

            configSettings.setTemplateActionsFolder(templateActionsFolder);
            log.info("Agent template actions are expected in '" + templateActionsFolder + "' folder - using "
                     + loadSource);
        } else {
            log.info("Agent template actions are expected in '" + oldTemplateActionsFolder + "' folder");
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

    @Override
    public String getDescription() {

        return "template actions configuration";
    }
}
