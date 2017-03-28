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

/**
 * The Agent configuration settings class
 */
public class ConfigurationSettings {

    private static ConfigurationSettings instance;

    //the settings variables
    private int                          monitorPollInterval;
    private int                          monitorInitialDelayInSecBeforePolling;
    private String                       componentsFolder;

    private String                       templateActionsFolder;
    private Boolean                      templateActionsMatchFilesBySize;
    private Boolean                      templateActionsMatchFilesByContent;

    private ConfigurationSettings() {

    }

    public static synchronized ConfigurationSettings getInstance() {

        if( instance == null ) {
            instance = new ConfigurationSettings();
        }
        return instance;
    }

    /**
     * Get the interval at which the folder for loading Agent components will be polled
     *
     * @return the interval in seconds
     */
    public int getMonitorPollInterval() {

        return monitorPollInterval;
    }

    /**
     * Set the interval at which the folder for loading Agent components will be polled
     *
     * @param monitorPollInterval the interval in seconds
     */
    public void setMonitorPollInterval( int monitorPollInterval ) {

        this.monitorPollInterval = monitorPollInterval;
    }

    /**
     * Get the folder which is scanned for Agent components
     *
     * @return the full folder path
     */
    public String getComponentsFolder() {

        return componentsFolder;
    }

    /**
     * Set the folder which is scanned for Agent components
     *
     * @param componentsFolder the full folder path
     */
    public void setComponentsFolder( String componentsFolder ) {

        this.componentsFolder = componentsFolder;
    }

    /**
     * Get the home folder for Agent template actions
     *
     * @return
     */
    public String getTemplateActionsFolder() {

        return templateActionsFolder;
    }

    /**
     * Set the home folder for Agent template actions
     *
     * @param teamplateActionsFolder the full folder path
     */
    public void setTemplateActionsFolder( String teamplateActionsFolder ) {

        this.templateActionsFolder = teamplateActionsFolder;
    }

    public Boolean isTemplateActionsMatchFilesBySize() {

        return templateActionsMatchFilesBySize;
    }

    public void setTemplateActionsMatchFilesBySize( Boolean templateActionsMatchFilesBySize ) {

        this.templateActionsMatchFilesBySize = templateActionsMatchFilesBySize;
    }

    public Boolean isTemplateActionsMatchFilesByContent() {

        return templateActionsMatchFilesByContent;
    }

    public void setTemplateActionsMatchFilesByContent( Boolean templateActionsMatchFilesByContent ) {

        this.templateActionsMatchFilesByContent = templateActionsMatchFilesByContent;
    }

    /**
     * Initial delay (quiet period) before start for polling for new Agent actions
     *
     * @return initial delay in seconds
     */
    public int getMonitorInitialDelay() {

        return monitorInitialDelayInSecBeforePolling;
    }

    public void setMonitorInitialDelay( int monitorInitialDelayInSecBeforePolling ) {

        this.monitorInitialDelayInSecBeforePolling = monitorInitialDelayInSecBeforePolling;
    }

}
