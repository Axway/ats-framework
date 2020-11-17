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
package com.axway.ats.config;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.config.exceptions.ConfigSourceDoesNotExistException;
import com.axway.ats.config.exceptions.ConfigurationException;

/**
 * Base class for all configuration classes. 
 * It implements certain functionality for reloading of data when the repository is updated, 
 * as well as methods for getting/setting properties from/to the repository.
 */
public abstract class AbstractConfigurator {

    protected final Logger          log;

    // a handle to the global singleton repository
    private ConfigurationRepository configRepository;

    public AbstractConfigurator() {

        //create the logger instance
        log = LogManager.getLogger(this.getClass());

        // Get the instance of the configuration repository.
        // It is our only entry point for getting and setting properties
        configRepository = ConfigurationRepository.getInstance();
    }

    /**
     * Get a property value from the repository.
     * If there is no such property no exception will be thrown, instead it will return null
     *
     * @param propertyName the property name
     * @return the property value or null if it does not exist
     */
    public String getOptionalProperty(
                                       String propertyName ) {

        return configRepository.getOptionalProperty(propertyName);
    }

    /**
     * Get a property from the repository.
     * All resources will be queried according to their order and the first matching property will be returned
     * 
     * @param name name of the property
     * @return the value of the property
     */
    public final String getProperty(
                                     String name ) {

        return configRepository.getProperty(name);
    }

    /**
     * Get a set of properties that match a certain prefix. All available
     * configuration resources will be queried for matching properties
     * 
     * @param prefix the prefix to look for
     * @return map of all properties that match this prefix
     */
    public final Map<String, String> getProperties(
                                                    String prefix ) {

        return configRepository.getProperties(prefix);
    }

    /**
     * Get a property by name and convert it to an integer
     * 
     * @param name the name of the property
     * @return the property converted to integer
     */
    protected final int getIntegerProperty(
                                            String name ) {

        String propertyValue = configRepository.getProperty(name);
        try {
            return Integer.parseInt(propertyValue);
        } catch (NumberFormatException nfe) {
            throw new ConfigurationException("Configuration setting '" + name + "' is not a number: '"
                                             + propertyValue + "'");
        }
    }

    /**
     * Get a property by name and convert it to a long
     * 
     * @param name the name of the property
     * @return the property converted to long
     */
    protected final long getLongProperty(
                                          String name ) {

        String propertyValue = configRepository.getProperty(name);
        try {
            return Long.parseLong(propertyValue);
        } catch (NumberFormatException nfe) {
            throw new ConfigurationException("Configuration setting '" + name + "' is not a number: '"
                                             + propertyValue + "'");
        }
    }

    /**
     * Get a property by name and convert it to a boolean.
     * <br>Valid values are "true" and "false". The letter case is not important.
     * 
     * @param name the name of the property
     * @return the property converted to boolean
     * @throws ConfigurationException if a not valid property is provided 
     */
    protected final boolean getBooleanProperty(
                                                String name ) {

        String propertyValue = configRepository.getProperty(name);
        if ("true".equalsIgnoreCase(propertyValue)) {
            return true;
        } else if ("false".equalsIgnoreCase(propertyValue)) {
            return false;
        } else {
            throw new ConfigurationException("Configuration setting '" + name + "' is not a boolean: '"
                                             + propertyValue + "'");
        }
    }

    /**
     * Get a property by name and convert it to a char.
     * <br>Valid values are 1 character long Strings
     * 
     * @param name the name of the property
     * @return the property converted to char
     * @throws ConfigurationException if a not valid property is provided 
     */
    protected final char getCharProperty( String name ) {

        String propertyValue = configRepository.getProperty(name);
        if (propertyValue.length() == 1) {
            return propertyValue.toCharArray()[0];
        } else {
            throw new ConfigurationException("Configuration setting '" + name + "' is not a char: '"
                                             + propertyValue + "'");
        }
    }

    /**
     * Use setTempProperty() method instead this one.
     */
    @Deprecated
    public final void setCustomProperty(
                                         String name,
                                         String value ) {

        setTempProperty(name, value);
    }

    /**
    * Set a property in the temporary configuration resource. If this property
    * already exists, it will be overridden.
    * 
    * Usually it is called before the execution of a test or the first test in a class.
    * 
    * @param name the name of the property
    * @param value the value of the property
    */
    public final void setTempProperty(
                                       String name,
                                       String value ) {

        configRepository.setTempProperty(name, value);

        //check if the new data is correct and if so - reload it
        reloadDataAndCheckItsIntegrity(null, name, value);
    }

    /**
     * Use clearTempProperties() method instead this one.
     */
    @Deprecated
    public final void clearCustomProperties() {

        clearTempProperties();
    }

    /**
     * Clear all temporary properties.
     * Usually it is call after the execution of a test or the last test in a class.
     */
    public final void clearTempProperties() {

        configRepository.clearTempResources();

        //check if the new data is correct and if so - reload it
        reloadDataAndCheckItsIntegrity(null, null, null);
    }

    /**
    * Register a regular configuration file.
    * These settings will be added to the permanent resources.
    * 
    * @param configFilePath the configuration source
    */
    protected final void addConfigFile(
                                        String configFilePath ) {

        ConfigurationRepository.getInstance().registerConfigFile(configFilePath);

        //check if the new data is correct and if so reload it
        reloadDataAndCheckItsIntegrity(configFilePath, null, null);
    }

    /**
     * Register a regular configuration file from the classpath.
     * These settings will be added to the permanent resources.
     * 
     * @param configFilePath the configuration resource to add
     * @param catchException should exceptions from file loading be catched and logged 
     *          (if true) or directly passed for handling by caller method  
     * @param overrideExistProperties whether existing properties will be overridden
     */
    protected final void addConfigFileFromClassPath(
                                                     String configFilePath,
                                                     boolean catchException,
                                                     boolean overrideExistProperties ) {

        if (catchException) {
            try {
                addConfigFileFromClassPath(configFilePath, overrideExistProperties);
            } catch (ConfigSourceDoesNotExistException e) {
                log.warn("Configuration file '" + configFilePath
                         + "' is not available and will not be loaded");

                return;
            }
        } else {
            addConfigFileFromClassPath(configFilePath, overrideExistProperties);
        }

        //check if the new data is correct and if so reload it
        reloadDataAndCheckItsIntegrity(configFilePath, null, null);
    }

    private final void addConfigFileFromClassPath(
                                                   String configFilePath, boolean overrideExistProperties ) {

        ConfigurationRepository.getInstance().registerConfigFileFromClassPath(configFilePath, overrideExistProperties);
    }

    /**
     * We will load the new data, if not successful - we will rollback the configuration properties
     */
    private void reloadDataAndCheckItsIntegrity(
                                                 String newConfigFile,
                                                 String newPropertyName,
                                                 String newPropertyValue ) {

        try {
            //check if the configuration is OK
            reloadData();
        } catch (ConfigurationException ce) {
            //there is some problem with the data, rollback the previous configuration
            ConfigurationRepository.getInstance().rollBackNewPermConfiguration();

            String errorMessage = "Error loading configuration data";
            if (newConfigFile != null) {
                errorMessage += " from " + newConfigFile;
            } else {
                errorMessage += ": Bad new property - name='" + newPropertyName + "', value='"
                                + newPropertyValue + "'";
            }

            throw new ConfigurationException(errorMessage, ce);
        }
    }

    /**
     * Reload all test data from the repository
     */
    protected abstract void reloadData();
}
