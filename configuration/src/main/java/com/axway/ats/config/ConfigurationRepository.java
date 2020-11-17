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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.config.exceptions.ConfigSourceDoesNotExistException;
import com.axway.ats.config.exceptions.ConfigurationException;
import com.axway.ats.config.exceptions.NoSuchPropertyException;

/**
 * The configuration repository is a single holder for settings which are used by the ATS framework
 * or user components. These settings are in the form of key-value pairs.
 * 
 * This is a two layer repository. 
 * The lower layer keeps settings loaded from some configuration files, some of settings can be overridden when a new 
 * configuration file is loaded. It is intended to hold long living settings - usually for the span of a whole run,
 * so they are called "permanent".
 * The higher layer keeps settings provided by the user in the test. It is intended to hold short living
 * settings - usually for the span of a single test, so they are called "temporary".
 * 
 * ATS is using this functionality to configure its components and our users can also use it to override the default 
 * ATS components' settings.
 */
public class ConfigurationRepository {

    //the logger instance
    private static final Logger                  log = LogManager.getLogger(ConfigurationRepository.class);

    //the singleton instance
    private static final ConfigurationRepository instance;

    // this is intended to be a short living resource, when we search for properties we will search here first
    private ConfigurationResource                tempResource;

    // this is intended to be a long living resource, 
    // we will search here if the properties are not present in the short living resource
    private ConfigurationResource                permResource;

    // this memory is used just to assure that the new configuration is correct
    private ConfigurationResource                permResourceBackup;

    static {
        // create the instance
        instance = new ConfigurationRepository();
    }

    /**
     * Constructor
     */
    private ConfigurationRepository() {

        //init the repository for the first time
        initialize();
    }

    /**
     * Get the instance of the configuration repository
     * 
     * @return the instance of the configuration repository
     */
    public static synchronized ConfigurationRepository getInstance() {

        return instance;
    }

    /**
     * Initialize the repository - clean all existing configuration resources 
     */
    void initialize() {

        tempResource = new ConfigurationResource();

        permResource = new ConfigurationResource();
        permResourceBackup = new ConfigurationResource();
    }

    /**
     * Register a regular configuration file from the classpath.
     * These settings will be added to the permanent resources.
     * 
     * @param filePath the configuration resource to add
     * @param overrideExistProperties whether existing properties will be overridden
     */
    void registerConfigFileFromClassPath(
                                          String filePath,
                                          boolean overrideExistProperties ) {

        URL configFileURL = ConfigurationRepository.class.getResource(filePath);
        if (configFileURL == null) {
            throw new ConfigSourceDoesNotExistException(filePath);
        }

        InputStream fileStream = ConfigurationRepository.class.getResourceAsStream(filePath);
        ConfigurationResource newPermResource = createConfigurationResourceFromStream(fileStream,
                                                                                      configFileURL.getFile());

        assignTheNewPermResource(newPermResource, overrideExistProperties);
    }

    /**
     * Register a regular configuration file.
     * These settings will be added to the permanent resourcse.
     * 
     * @param filePath the configuration resource to add
     */
    void registerConfigFile(
                             String filePath ) {

        File configFile = new File(filePath);
        ConfigurationResource newPermResource = null;
        try {
            newPermResource = createConfigurationResourceFromStream(new FileInputStream(configFile),
                                                                    configFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            throw new ConfigSourceDoesNotExistException(filePath);
        }

        assignTheNewPermResource(newPermResource, true);
    }

    private void assignTheNewPermResource(
                                           ConfigurationResource newPermResource,
                                           boolean overrideExistProperties ) {

        // backup the current resource
        copyProperties(permResource, permResourceBackup, true);
        // set the new resource
        copyProperties(newPermResource, permResource, overrideExistProperties);
        // if exception is thrown while the new settings are loaded - we will revert back the previous assignments
    }

    /**
     * Get a property from the repository.
     * All resources will be queried according to their order and the first matching property will be returned
     * 
     * @param name name of the property
     * @return the value of the property
     */
    String getProperty(
                        String name ) {

        String value = null;

        //look for the first property that matches from the temporary resources
        value = tempResource.getProperty(name);
        if (value != null) {
            if (log.isTraceEnabled()) {
                log.trace("Returning property '" + name + "' with value '" + value);
            }
            return value;
        }

        //the property is not found in the temporary resources, now look for it in the permanent resources
        value = permResource.getProperty(name);
        if (value != null) {
            if (log.isTraceEnabled()) {
                log.trace("Returning property '" + name + "' with value '" + value);
            }
            return value;
        }

        //we didn't find the property
        throw new NoSuchPropertyException(name);
    }

    /**
     * Get a property from the repository.
     * If there is no such property no exception will be thrown, instead it will return the default value 
     * 
     * @param name name of the property
     * @param defaultValue the default property value
     * @return the value of the property
     */
    String getProperty(
                        String name,
                        String defaultValue ) {

        try {
            return getProperty(name);
        } catch (NoSuchPropertyException nspe) {
            //we didn't find the property, use its default value
            return defaultValue;
        }
    }

    /**
     * Get a property from the repository.
     * If there is no such property no exception will be thrown, instead it will return null 
     * 
     * @param name name of the property
     * @return the value of the property
     */
    String getOptionalProperty(
                                String name ) {

        try {
            return getProperty(name);
        } catch (NoSuchPropertyException nspe) {
            return null;
        }
    }

    /**
     * Get a set of properties that match a certain prefix. All available
     * configuration resources will be queried for matching properties
     * 
     * @param prefix the prefix to look for
     * @return map of all properties that match this prefix
     */
    Map<String, String> getProperties(
                                       String prefix ) {

        Map<String, String> matchingTempProperties = tempResource.getProperties(prefix);
        if (!matchingTempProperties.isEmpty()) {
            return matchingTempProperties;
        } else {
            return permResource.getProperties(prefix);
        }
    }

    /**
     * Set a property in the temporary configuration resource. If this property
     * already exists, it will be overridden.
     * 
     * Usually it is call before the execution of a test or the first test in a class.
     * 
     * @param name the name of the property
     * @param value the value of the property
     */
    void setTempProperty(
                          String name,
                          String value ) {

        tempResource.setProperty(name, value);
    }

    /**
     * Clear all temporary properties.
     * Usually it is call after the execution of a test or the last test in a class.
     */
    void clearTempResources() {

        tempResource = new ConfigurationResource();
    }

    /**
     * We call it if there is a problem while applying the new permanent configuration.
     */
    void rollBackNewPermConfiguration() {

        permResource = permResourceBackup;
    }

    private void copyProperties(
                                 ConfigurationResource from,
                                 ConfigurationResource to,
                                 boolean overrideExistProperties ) {

        for (Map.Entry<Object, Object> property : from.getProperties()) {
            String key = (String) property.getKey();
            String value = (String) property.getValue();
            if (value != null) {
                value = value.trim();
            }
            if (to.getProperty(key) != null) {
                if (overrideExistProperties) {
                    to.setProperty(key, value);
                }
            } else {
                to.setProperty(key, value);
            }
        }
    }

    /**
     * Create a new configuration resource based on a given configuration file
     * 
     * @param configFile the configuration file
     * @return a config resource based on the file type
     */
    private ConfigurationResource createConfigurationResourceFromStream(
                                                                         InputStream resourceStream,
                                                                         String resourceIdentifier ) {

        ConfigurationResource configResource;
        if (resourceIdentifier.endsWith(".xml")) {
            configResource = new ConfigurationResource();
            configResource.loadFromXmlFile(resourceStream, resourceIdentifier);
            return configResource;
        } else if (resourceIdentifier.endsWith(".properties")) {
            configResource = new ConfigurationResource();
            configResource.loadFromPropertiesFile(resourceStream, resourceIdentifier);
            return configResource;
        } else {
            throw new ConfigurationException("Not supported file extension. We expect either 'xml' or 'properties': "
                                             + resourceIdentifier);
        }
    }
}
