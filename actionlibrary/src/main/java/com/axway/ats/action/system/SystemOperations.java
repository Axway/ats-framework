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
package com.axway.ats.action.system;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Set;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.log4j.Logger;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.common.system.SystemOperationException;
import com.axway.ats.core.system.LocalSystemOperations;
import com.axway.ats.core.system.model.ISystemOperations;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.validation.Validate;
import com.axway.ats.core.validation.ValidationType;
import com.axway.ats.core.validation.Validator;

/**
 * Operations on the OS level like getting OS type, get/set time, get classpath and
 * perform keyboard/mouse operations.
 *
 * <br/>
 * <p>User guide page related to this class is
 * <a href="https://axway.github.io/ats-framework/Basic-System-Operations.html">here</a>
 * </p>
 */
@PublicAtsApi
public class SystemOperations {

    private String  atsAgent;

    private Logger  log      = Logger.getLogger(SystemOperations.class);

    @PublicAtsApi
    public Mouse    mouse    = new Mouse();

    @PublicAtsApi
    public Keyboard keyboard = new Keyboard();

    /**
     * Constructor when working on the local host
     */
    @PublicAtsApi
    public SystemOperations() {

    }

    /**
     * Constructor when working on a remote host
     *
     * @param atsAgent the address of the remote ATS agent which will run the wanted operation
     * <p>
     *    <b>Note:</b> If you want to specify port to IPv6 address, the supported format is: <i>[IP]:PORT</i>
     * </p>
     */
    @PublicAtsApi
    public SystemOperations( @Validate( name = "atsAgent", type = ValidationType.STRING_SERVER_WITH_PORT) String atsAgent ) {

        // validate input parameters
        atsAgent = HostUtils.getAtsAgentIpAndPort(atsAgent);
        new Validator().validateMethodParameters(new Object[]{ atsAgent });

        this.atsAgent = atsAgent;
    }

    /**
     * Get Operating System type/name
     *
     * @return the {@link OperatingSystemType}
     */
    @PublicAtsApi
    public OperatingSystemType getOperatingSystemType() {

        ISystemOperations operations = getOperationsImplementationFor(atsAgent);
        return operations.getOperatingSystemType();
    }

    /**
     * Get the value of the environment's system property.<br/>
     *
     * It calls internally System.getProperty("property name");
     *
     * @param propertyName the name of the system property
     * @return the value of the system property
     */
    @PublicAtsApi
    public String getSystemProperty(
                                     @Validate( name = "propertyName", type = ValidationType.NOT_NULL) String propertyName ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ propertyName });

        // execute action
        ISystemOperations operations = getOperationsImplementationFor(atsAgent);
        return operations.getSystemProperty(propertyName);
    }

    /**
     * Get current system time
     *
     * @param inMilliseconds whether the time value to be in milliseconds or a formated date string
     * @return the current system time
     */
    @PublicAtsApi
    public String getTime(
                           boolean inMilliseconds ) {

        ISystemOperations operations = getOperationsImplementationFor(atsAgent);
        return operations.getTime(inMilliseconds);
    }

    /**
     * Set the system time
     *
     * @param timestamp the timestamp to set
     * @param inMilliseconds whether the timestamp is in milliseconds or a formatted date string
     * @throws SystemOperationException
     */
    @PublicAtsApi
    public void setTime(
                         @Validate( name = "timestamp", type = ValidationType.STRING_NOT_EMPTY) String timestamp,
                         @Validate( name = "inMilliseconds", type = ValidationType.NONE) boolean inMilliseconds ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ timestamp, inMilliseconds });

        // execute action
        ISystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.setTime(timestamp, inMilliseconds);
    }

    /**
     * Get the ATS version
     *
     * @return the ATS version
     */
    @PublicAtsApi
    public String getAtsVersion() {

        ISystemOperations operations = getOperationsImplementationFor(atsAgent);
        return operations.getAtsVersion();
    }

    /**
    * Check if some process is listening on some port on some host.
    * Note that we cannot give the name of the listening process.
    *
    * @param host host address
    * @param port port number
    * @param timeout timeout value in milliseconds
    * @return <code>true</code> if the address is listening on this port
    * @throws SystemOperationException when the target host is unknown
    */
    @PublicAtsApi
    public boolean isListening(
                                @Validate( name = "host", type = ValidationType.STRING_NOT_EMPTY) String host,
                                @Validate( name = "port", type = ValidationType.NUMBER_PORT_NUMBER) int port,
                                @Validate( name = "timeout", type = ValidationType.NUMBER_POSITIVE) int timeout ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ host, port, timeout });

        // execute action
        ISystemOperations operations = getOperationsImplementationFor(atsAgent);
        return operations.isListening(host, port, timeout);
    }

    /**
     * Creates screenshot image file.<br/>
     * The currently supported image formats/types are PNG, JPG, JPEG, GIF and BMP
     *
     * @param filePath the screenshot image file path. If the file extension is not specified, the default format PNG will be used
     */
    @PublicAtsApi
    public void createScreenshot(
                                  @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath });

        ISystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.createScreenshot(filePath);
    }

    /**
     * Return array of all detected JARs from classpath
     */
    @PublicAtsApi
    public String[] getClassPath() {

        ISystemOperations operations = getOperationsImplementationFor(atsAgent);
        return operations.getClassPath();
    }

    /**
     * Log all JARs in current application's ClassPath
     */
    @PublicAtsApi
    public void logClassPath() {

        ISystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.logClassPath();
    }

    /**
     * Return array containing all duplicated jars in the ClassPath
     */
    @PublicAtsApi
    public String[] getDuplicatedJars() {

        ISystemOperations operations = getOperationsImplementationFor(atsAgent);
        return operations.getDuplicatedJars();
    }

    /**
     * Log all duplicated JARs in current application's ClassPath
     */
    @PublicAtsApi
    public void logDuplicatedJars() {

        ISystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.logDuplicatedJars();
    }

    /**
     * @param host the address of the host machine
     * @param jmxPort the jmx port
     *
     * @return all MBeans with their attributes and type
     * @throws SystemOperationException
     */
    @PublicAtsApi
    public String getJvmMbeans(
                                String host,
                                String jmxPort ) {

        JMXConnector jmxCon = null;
        try {
            // Connect to JMXConnector
            JMXServiceURL serviceUrl = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + host + ":"
                                                         + jmxPort + "/jmxrmi");
            jmxCon = JMXConnectorFactory.newJMXConnector(serviceUrl, null);
            jmxCon.connect();

            // Access the MBean
            MBeanServerConnection con = jmxCon.getMBeanServerConnection();
            Set<ObjectName> queryResults = con.queryNames(null, null);
            StringBuilder results = new StringBuilder();
            for (ObjectName theName : queryResults) {
                results.append("\n---");
                results.append("\nMBean name: " + theName.getCanonicalName());
                MBeanAttributeInfo[] attributes = con.getMBeanInfo(theName).getAttributes();
                for (MBeanAttributeInfo attribute : attributes) {
                    if (attribute.getType() != null) {
                        if (!"javax.management.openmbean.CompositeData".equals(attribute.getType())) {
                            if ("java.lang.Long".equals(attribute.getType())
                                || "java.lang.Integer".equals(attribute.getType())
                                || "int".equals(attribute.getType())
                                || "long".equals(attribute.getType()))

                                results.append("\r   " + attribute.getName() + " | " + attribute.getType());

                        } else {
                            results.append("\r   " + attribute.getName() + " | " + attribute.getType());
                            CompositeData comdata = (CompositeData) con.getAttribute(theName,
                                                                                     attribute.getName());
                            if (comdata != null) {
                                for (String key : comdata.getCompositeType().keySet()) {
                                    Object value = comdata.get(key);
                                    if (value instanceof Integer || value instanceof Double
                                        || value instanceof Long)
                                        results.append("\r      " + key + " | " + value.getClass());
                                }
                            }
                        }
                    }
                }
            }
            return results.toString();
        } catch (Exception e) {
            throw new SystemOperationException("MBeans with their attributes cannot be get.", e);
        } finally {
            if (jmxCon != null)
                try {
                    jmxCon.close();
                } catch (IOException e) {
                    log.error("JMX connection was not closed!");
                }
        }
    }

    /**
     * @return Machine hostname
     */
    @PublicAtsApi
    public String getHostname() {

        ISystemOperations operations = getOperationsImplementationFor(atsAgent);
        return operations.getHostname();

    }

    private ISystemOperations getOperationsImplementationFor(
                                                              String atsAgent ) {

        if (HostUtils.isLocalAtsAgent(atsAgent)) {
            return new LocalSystemOperations();
        } else {
            
            try {
                return new RemoteSystemOperations(atsAgent);
            } catch (Exception e) {
                throw new RuntimeException("Unable to create remote process executor impl object", e);
            }
        }
    }

    /**
     * Simulate keyboard actions
     *
     */
    @PublicAtsApi
    public class Keyboard {

        /**
         * Presses a given key. The key should be released using the keyRelease method.
         *
         * @param keyCode Key to press (e.g. {@link KeyEvent}.VK_A)
         */
        @PublicAtsApi
        public void keyPress(
                              int keyCode ) {

            ISystemOperations operations = getOperationsImplementationFor(atsAgent);
            operations.getInputOperations().keyPress(keyCode);
        }

        /**
         * Releases a given key.
         *
         * @param keyCode Key to release (e.g. {@link KeyEvent}.VK_A)
         */
        @PublicAtsApi
        public void keyRelease(
                                int keyCode ) {

            ISystemOperations operations = getOperationsImplementationFor(atsAgent);
            operations.getInputOperations().keyRelease(keyCode);
        }

        /**
         * Press the ENTER key
         */
        @PublicAtsApi
        public void pressEnter() {

            ISystemOperations operations = getOperationsImplementationFor(atsAgent);
            operations.getInputOperations().pressEnter();
        }

        /**
         * Press the Escape key
         */
        @PublicAtsApi
        public void pressEsc() {

            ISystemOperations operations = getOperationsImplementationFor(atsAgent);
            operations.getInputOperations().pressEsc();
        }

        /**
         * Press the Tab key
         */
        @PublicAtsApi
        public void pressTab() {

            ISystemOperations operations = getOperationsImplementationFor(atsAgent);
            operations.getInputOperations().pressTab();
        }

        /**
         * Press the Space key
         */
        @PublicAtsApi
        public void pressSpace() {

            ISystemOperations operations = getOperationsImplementationFor(atsAgent);
            operations.getInputOperations().pressSpace();
        }

        /**
         * Press Alt + F4 keys
         */
        @PublicAtsApi
        public void pressAltF4() {

            ISystemOperations operations = getOperationsImplementationFor(atsAgent);
            operations.getInputOperations().pressAltF4();
        }

        /**
         * Type some keys defined in {@link KeyEvent}
         *
         * @param keyCodes the special key codes
         */
        @PublicAtsApi
        public void type(
                          int... keyCodes ) {

            ISystemOperations operations = getOperationsImplementationFor(atsAgent);
            operations.getInputOperations().type(keyCodes);
        }

        /**
         * Type some text
         *
         * @param text the text to type
         */
        @PublicAtsApi
        public void type(
                          String text ) {

            ISystemOperations operations = getOperationsImplementationFor(atsAgent);
            operations.getInputOperations().type(text);
        }

        /**
         * Type some text but combine them with some keys defined in {@link KeyEvent}
         * It first presses the special key codes(for example Alt + Shift), then it types the provided text
         * and then it releases the special keys in reversed order(for example Shift + Alt )
         *
         * @param text the text to type
         * @param keyCodes the special key codes
         */
        @PublicAtsApi
        public void type(
                          String text,
                          int... keyCodes ) {

            ISystemOperations operations = getOperationsImplementationFor(atsAgent);
            operations.getInputOperations().type(text, keyCodes);
        }

    }

    /**
     * Simulate mouse actions
     *
     */
    @PublicAtsApi
    public class Mouse {

        /**
         * Move the mouse at (X,Y) screen position and then click the mouse button 1
         *
         * @param x the X coordinate
         * @param y the Y coordinate
         */
        @PublicAtsApi
        public void clickAt(
                             int x,
                             int y ) {

            ISystemOperations operations = getOperationsImplementationFor(atsAgent);
            operations.getInputOperations().clickAt(x, y);
        }
    }

}
