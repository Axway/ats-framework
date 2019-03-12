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
package com.axway.ats.common.systemproperties;

import com.axway.ats.common.PublicAtsApi;

/**
 * This class is used as a central place for working with ATS related system properties.
 * It is good if all such system properties are prefixed with "ats."
 */
@PublicAtsApi
public class AtsSystemProperties {

    // Agent properties
    @PublicAtsApi
    private static final String DEFAULT_AGENT_PORT_KEY                                            = "ats.agent.default.port";
    @PublicAtsApi
    public static final int     DEFAULT_AGENT_PORT_VALUE                                          = 8089;
    @PublicAtsApi
    public static final String  AGENT_HOME_FOLDER                                                 = "ats.agent.home";

    public static final String  AGENT__MONITOR_POLL_INTERVAL                                      = "ats.agent.monitor.poll.interval";
    @PublicAtsApi
    public static final String  AGENT__MONITOR_INITIAL_POLL_DELAY                                 = "ats.agent.monitor.initial.poll.delay";
    @PublicAtsApi
    public static final String  AGENT__COMPONENTS_FOLDER                                          = "ats.agent.components.folder";

    @PublicAtsApi
    public static final String  AGENT__TEMPLATE_ACTIONS_PROXY_PROPERTY                            = "ats.agent.template.actions.proxy";                             // Key to specify proxy for template action requests
    public static final String  AGENT__REGISTER_FULL_AND_NET_ACTION_TIME_FOR_TEMPLATE_ACTIONS_KEY = "ats.agent.template.actions.register_full_and_net_action_time"; // Property to enable full action time logging in addition to net+server think time
    public static final String  AGENT__TEMPLATE_ACTIONS_FOLDER                                    = "ats.agent.template.actions.folder";
    public static final String  AGENT__TEMPLATE_ACTIONS_MATCH_FILES_BY_SIZE                       = "ats.agent.template.actions.match.files.by.size";
    public static final String  AGENT__TEMPLATE_ACTIONS_MATCH_FILES_BY_CONTENT                    = "ats.agent.template.actions.match.files.by.content";

    // Log properties
    @PublicAtsApi
    public static final String  LOG__MONITOR_EVENTS_QUEUE                                         = "ats.log.monitor.events.queue";
    @PublicAtsApi
    public static final String  LOG__CLASSPATH_ON_START                                           = "ats.log.classpath.on.start";

    /**
     * <p>Enable caching of the class and method that created any of the AbstractLoggingEvent(s).</p>
     * <p>In other words, when for example a call like AtsDbLogger.startRun() is executed,
     * the StartRunEvent will cache/save where that call was made in the format: full_class_name.method:line_number</p>
     * 
     * */
    public static final String  LOG__CACHE_EVENTS_SOURCE_LOCATION                                 = "ats.log.cache.events.source.location";

    // TestHarness properties
    // Run name for JUnit executions
    @PublicAtsApi
    public static final String  TEST_HARNESS__JUNIT_RUN_NAME                                      = "ats.junit.RunName";
    // source location of the running tests
    @PublicAtsApi
    public static final String  TEST_HARNESS__TESTS_SOURCE_LOCATION                               = "ats.tests.source.location";

    // UI Engine properties
    // milliseconds to delay between swing events
    @PublicAtsApi
    public static final String  UI_ENGINE__SWING_ROBOT_DELAY_BETWEEN_EVENTS                       = "ats.uiengine.swing.RobotDelayBetweenEvents";

    // Core properties
    // Java secure channel verbose mode
    @PublicAtsApi
    public static final String  CORE__JSCH_VERBOSE_MODE                                           = "ats.core.ssh.verbose.mode";

    /**
     * Toggle whether ATS Agent will be configured and flag as configured by both SystemMonitor and TestcaseStateListener.
     * This fix is when user wants to stop monitoring and continue using the same agent (executing actions).
     * */
    public static final String  MONITORING_CONFIG_AGENT_EXPLICITELY                               = "ats.monitor.config.agent.explicitly";

    /*
     * Following are system properties which are not supposed to be changed
     * during the VM lifetime, so we do not need to read them more than once.
     */
    public static final String  SYSTEM_LINE_SEPARATOR                                             = System.getProperty("line.separator");
    public static final String  SYSTEM_FILE_SEPARATOR                                             = System.getProperty("file.separator");
    public static final String  SYSTEM_OS_NAME                                                    = System.getProperty("os.name");
    public static final String  SYSTEM_USER_HOME_DIR                                              = System.getProperty("user.dir");
    public static final String  SYSTEM_USER_TEMP_DIR                                              = System.getProperty("java.io.tmpdir");
    public static final String  SYSTEM_JAVA_HOME_DIR                                              = System.getProperty("java.home");
    public static final String  SYSTEM_HTTP_PROXY_HOST                                            = System.getProperty("http.proxyHost");
    public static final String  SYSTEM_HTTP_PROXY_PORT                                            = System.getProperty("http.proxyPort");

    /**
     * @return the default ATS agent port number
     */
    @PublicAtsApi
    public static Integer getAgentDefaultPort() {

        Integer defaultPort = null;
        try {
            defaultPort = getPropertyAsNumber(DEFAULT_AGENT_PORT_KEY);
        } catch (IllegalArgumentException iae) {
            System.err.println(iae.getMessage());
        }

        if (defaultPort == null) {
            defaultPort = DEFAULT_AGENT_PORT_VALUE;
        }
        return defaultPort;
    }

    /**
     * @param defaultAgentPort the new default ATS agent port number
     */
    @PublicAtsApi
    public static void setAgentDefaultPort( int defaultAgentPort ) {

        System.setProperty(DEFAULT_AGENT_PORT_KEY, String.valueOf(defaultAgentPort));
    }

    /**
     * Return a system property as an integer number.
     * Return null if key is not present.
     * Throw an exception if value is invalid
     *
     * @param key the name of the searched system property
     * @return a number
     */
    @PublicAtsApi
    public static Integer getPropertyAsNumber( String key ) {

        String strValue = getPropertyAsString(key);

        if (strValue == null) {
            return null;
        } else {
            try {
                return Integer.parseInt(strValue);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("System property with name '" + key
                                                   + "' has a non integer value '" + strValue + "'");
            }
        }
    }

    /**
     * Return a system property as an integer number
     *
     * @param key the name of the searched system property
     * @param defaultValue default value in case the system property does not exist or is invalid
     * @return a number
     */
    @PublicAtsApi
    public static Integer getPropertyAsNumber( String key, Integer defaultValue ) {

        String strValue = getPropertyAsString(key);

        if (strValue != null) {
            try {
                return Integer.parseInt(strValue);
            } catch (NumberFormatException nfe) {
                if (defaultValue == null) {
                    throw new IllegalArgumentException("System property with name '" + key
                                                       + "' has a non integer value '" + strValue + "'");
                }
            }
        }
        return defaultValue;
    }

    /**
     * If key is found and is parsed OK - the wanted value is returned.</br>
     * If key is found and fail parsing - an error is thrown
     *
     * If key is not found - null is returned.</br>
     *
     * @param key the name of the searched system property
     * @return a boolean value
     */
    @PublicAtsApi
    public static Boolean getPropertyAsBoolean( String key ) {

        return getPropertyAsBoolean(key, null);
    }

    /**
     * If key is found and is parsed OK - the wanted value is returned.</br>
     * If key is found and fail parsing - default value is returned if provided, otherwise an error is thrown
     *
     * If key is not found - default value is returned if provided, otherwise null is returned.</br>
     *
     * @param key the name of the searched system property
     * @param defaultValue default value in case the system property does not exist or is invalid
     * @return a boolean value
     */
    @PublicAtsApi
    public static Boolean getPropertyAsBoolean( String key, Boolean defaultValue ) {

        String strValue = getPropertyAsString(key);

        if (strValue != null) {
            if (strValue.equalsIgnoreCase("true") || strValue.equals("1")) {
                return Boolean.TRUE;
            }

            if (strValue.equalsIgnoreCase("false") || strValue.equals("0")) {
                return Boolean.FALSE;
            }

            if (defaultValue == null) {
                throw new IllegalArgumentException("System property with name '" + key
                                                   + "' has a non boolean value '" + strValue
                                                   + "'. Expected values are 'true' or '1' for TRUE and 'false' or '0' for FALSE.");
            }
        }
        return defaultValue;
    }

    /**
     * If key is found and is parsed OK - the wanted value is returned.</br>
     * If key is found and fail parsing - an error is thrown
     *
     * If key is not found - null is returned.</br>
     *
     * @param key the name of the searched system property
     * @return a number that is 0 or above
     */
    @PublicAtsApi
    public static Integer getPropertyAsNonNegativeNumber( String key ) {

        return getPropertyAsNonNegativeNumber(key, null);
    }

    /**
     * If key is found and is parsed OK - the wanted value is returned.</br>
     * If key is found and fail parsing - default value is returned if provided, otherwise an error is thrown
     *
     * If key is not found - default value is returned if provided, otherwise null is returned.</br>
     *
     * @param key the name of the searched system property
     * @param defaultValue default value in case the system property does not exist or is invalid
     * @return a number that is 0 or above
     */
    @PublicAtsApi
    public static Integer getPropertyAsNonNegativeNumber( String key, Integer defaultValue ) {

        String strValue = getPropertyAsString(key);

        if (strValue != null) {
            try {
                int intValue = Integer.parseInt(strValue);
                if (intValue < 0 && defaultValue == null) {
                    throw new IllegalArgumentException("System property with name '" + key
                                                       + "' has a non positive number '" + strValue + "'");
                }
                return intValue;
            } catch (NumberFormatException nfe) {
                if (defaultValue == null) {
                    throw new IllegalArgumentException("System property with name '" + key
                                                       + "' has a non number value '" + strValue + "'");
                }
            }
        }
        return defaultValue;
    }

    /**
     * Return a system property as a String. Return null if not present.
     *
     * @param key the name of the searched system property
     * @return
     */
    @PublicAtsApi
    public static String getPropertyAsString( String key ) {

        String value = System.getProperty(key);

        if (value == null) {
            return value;
        } else {
            return value.trim();
        }
    }

    /**
     * Return a system property as a String if existing. Otherwise return defaultValue
     *
     * @param key the name of the searched system property
     * @param defaultValue default value to return if property not defined
     * @return
     */
    @PublicAtsApi
    public static String getPropertyAsString( String key, String defaultValue ) {

        String value = System.getProperty(key);

        if (value == null) {
            return defaultValue;
        } else {
            return value.trim();
        }
    }
}
