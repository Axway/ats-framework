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
package com.axway.ats.harness.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.config.AbstractConfigurator;
import com.axway.ats.config.exceptions.ConfigSourceDoesNotExistException;
import com.axway.ats.config.exceptions.ConfigurationException;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.utils.StringUtils;

/**
 * Register of configuration files containing properties/settings
 * Provides access to defined server configuration data ({@link TestBox})
 */
@PublicAtsApi
public final class CommonConfigurator extends AbstractConfigurator {

    private static final String       TEST_BOXES_PATH    = "common.testboxes.";
    private static final String       MESSAGE_BOXES_PATH = "common.messageboxes.";
    private static final String       MAIL_SERVERS_PATH  = "common.mailservers.";
    private static final String       LOGGING_RUN_PATH   = "ats.db.logging.run.";

    private static final String       KEY__RUN_NAME      = "ats.db.logging.run.name";
    private static final String       KEY__OS_NAME       = "ats.db.logging.run.os";
    private static final String       KEY__PRODUCT_NAME  = "ats.db.logging.run.product";
    private static final String       KEY__VERSION_NAME  = "ats.db.logging.run.version";
    private static final String       KEY__BUILD_NAME    = "ats.db.logging.run.build";

    public static final String        DEFAULT_RUN_NAME   = "Default Run Name";

    // Run parameters
    private String                    runName            = DEFAULT_RUN_NAME;
    private String                    osName             = "Default OS";
    private String                    productName        = "Default Product Name";
    private String                    versionName        = "1.0.0";
    private String                    buildName          = "1000";

    /**
     * The singleton instance for this configurator
     */
    private static CommonConfigurator instance;

    /**
     * Map to hold all the test box data
     */
    private BoxesMap<TestBox>         testBoxMap         = new BoxesMap<TestBox>();

    /**
     * Map to hold all message boxes data
     */
    private BoxesMap<MessagesBox>     messageBoxMap      = new BoxesMap<MessagesBox>();

    /**
     * Map to hold all the mail servers data
     */
    private BoxesMap<MailServer>      mailServersMap     = new BoxesMap<MailServer>();

    private CommonConfigurator( String configurationSource ) {

        super();

        //add the resource to the repository
        try {
            addConfigFileFromClassPath(configurationSource, false, true /* overwrite defaults */ );
        } catch (ConfigSourceDoesNotExistException e) {
            //log a warning here, because this is just a default config source
            log.warn("Default config source '" + configurationSource
                     + "' is not available and will not be loaded");
        }
    }

    /**
     * Get access to default CommonConfigurator instance, 
     * holding data from first found <code>/ats.config.properties</code> file in classpath
     */
    @PublicAtsApi
    public static synchronized CommonConfigurator getInstance() {

        if (instance == null) {
            instance = new CommonConfigurator("/ats.config.properties");
        }
        return instance;
    }

    /**
     * Used in unit tests for clearing the singleton instance
     */
    static void clearInstance() {

        instance = null;
    }

    /**
     * Get a test box with the given name
     * 
     * @param name the name of the test box
     * @return the test box instance
     */
    @PublicAtsApi
    public TestBox getTestBox( String name ) {

        TestBox testBox = testBoxMap.get(name);
        if (testBox == null) {
            throw new ConfigurationException("No test box with name '" + name + "'");
        }

        return testBox;
    }

    /**
     * Get all the test boxes
     * 
     * @return the all the test boxes
     */
    @PublicAtsApi
    public List<TestBox> getTestBoxes() {

        List<TestBox> allTestBoxes = new ArrayList<TestBox>();
        allTestBoxes.addAll(testBoxMap.values());
        return allTestBoxes;
    }

    /**
     * Get a messages box with the given name
     * 
     * @param name the name of the box which stores test messages
     * @return the messages box instance
     */
    @PublicAtsApi
    public MessagesBox getMessagesBox( String name ) {

        MessagesBox messagesBox = messageBoxMap.get(name);
        if (messagesBox == null) {
            throw new ConfigurationException("No messages box with name '" + name + "'");
        }

        return messagesBox;
    }

    /**
     * Get a mail server with the given name
     * 
     * @param name the name of the mail server
     * @return the mail server instance
     */
    @PublicAtsApi
    public MailServer getMailServer( String name ) {

        MailServer mailServer = mailServersMap.get(name);
        if (mailServer == null) {
            throw new ConfigurationException("No mail server with name '" + name + "'");
        }

        return mailServer;
    }

    public String getRunName() {

        return runName;
    }

    public String getOsName() {

        return osName;
    }

    public String getProductName() {

        return productName;
    }

    public String getVersionName() {

        return versionName;
    }

    public String getBuildName() {

        return buildName;
    }

    /**
     * Register a configuration file
     * 
     * @param fileName the absolute file name or relative to current working directory
     */
    @PublicAtsApi
    public void registerConfigFile( String fileName ) {

        if (StringUtils.isNullOrEmpty(fileName)) {
            throw new ConfigurationException("Null or empty file path: '" + fileName + "'");
        }
        fileName = fileName.trim();

        addConfigFile(fileName);
    }

    /**
     * Register a configuration file from the classpath
     * 
     * @param classpathIdentifier the absolute classpath identifier - e.g. /com/axway/config.xml
     */
    @PublicAtsApi
    public void registerConfigFileFromClasspath( String classpathIdentifier ) {

        if (StringUtils.isNullOrEmpty(classpathIdentifier)) {
            throw new ConfigurationException("Null or empty file path: '" + classpathIdentifier + "'");
        }
        classpathIdentifier = classpathIdentifier.trim();

        if (!classpathIdentifier.startsWith("/")) {
            classpathIdentifier = "/" + classpathIdentifier;
        }

        addConfigFileFromClassPath(classpathIdentifier, false, true);
    }

    @Override
    protected void reloadData() {

        //reload the test boxes
        Map<String, String> testBoxesProperties = getProperties(TEST_BOXES_PATH);
        testBoxMap = new BoxesMap<TestBox>(testBoxesProperties, TEST_BOXES_PATH, TestBox.class);

        //reload the messages boxes
        Map<String, String> messageBoxesProperties = getProperties(MESSAGE_BOXES_PATH);
        messageBoxMap = new BoxesMap<MessagesBox>(messageBoxesProperties, MESSAGE_BOXES_PATH,
                                                  MessagesBox.class);

        //reload the mail servers
        Map<String, String> mailServersProperties = getProperties(MAIL_SERVERS_PATH);
        mailServersMap = new BoxesMap<MailServer>(mailServersProperties, MAIL_SERVERS_PATH,
                                                  MailServer.class);

        //reload the test boxes
        Map<String, String> loggingRunMap = getProperties(LOGGING_RUN_PATH);

        if (loggingRunMap.containsKey(KEY__RUN_NAME)) {
            runName = loggingRunMap.get(KEY__RUN_NAME);
        }
        if (loggingRunMap.containsKey(KEY__OS_NAME)) {
            osName = loggingRunMap.get(KEY__OS_NAME);
        }
        if (loggingRunMap.containsKey(KEY__PRODUCT_NAME)) {
            productName = loggingRunMap.get(KEY__PRODUCT_NAME);
        }
        if (loggingRunMap.containsKey(KEY__VERSION_NAME)) {
            versionName = loggingRunMap.get(KEY__VERSION_NAME);
        }
        if (loggingRunMap.containsKey(KEY__BUILD_NAME)) {
            buildName = loggingRunMap.get(KEY__BUILD_NAME);
        }
    }

    /**
     * By default ATS tries to discover on its own if some host is a local or remote one. <br>
     * In case you find this discovery is not accurate, you can use this method 
     * to explicitly set a host locality. <br><br>
     * 
     * This is important for some ATS operations which need to know if some host is a local one or not. <br>
     * For example when copying a file to a remote host we will need to work with an ATS Agent located on that remote host. 
     * But if we find this is a local host, we can simply use the available Java classes for work with the filesystem
     * for the copy process.
     * 
     * @param host host name or IP address. Note that currently this does not accept agent address (IP:port pair).
     *             You should not provide port.
     * @param isLocal whether this host should be treated as a local (<code>true</code>) or remote one
     */
    @PublicAtsApi
    public void setHostLocality( String host, boolean isLocal ) {

        // this method currently does not read data from the configuration properties file
        // this may change in future
        if (StringUtils.isNullOrEmpty(host)) {
            throw new IllegalArgumentException("Bad host parameter provided: '" + host + "'");
        }

        HostUtils.setHostLocality(host, isLocal);
    }
}