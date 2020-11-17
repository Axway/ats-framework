/*
 * Copyright 2017-2020 Axway Software
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
package com.axway.ats.agent.core.monitoring.jvmmonitor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.core.monitoring.MonitorConfigurationException;

/**
 * A wrapper around java managed beans
 */
public class MBeanWrapper {

    private static final Logger    log        = LogManager.getLogger(MBeanWrapper.class);

    private static Set<ObjectName> mBeanNames = null;

    private static Object          lock       = new Object();

    private MBeanServerConnection  connection;
    private int                    jvmPort;

    MBeanWrapper( int jvmPort ) throws MonitorConfigurationException {

        this.jvmPort = jvmPort;
        try {
            JMXConnector connector = JMXConnectorFactory.newJMXConnector(new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:"
                                                                                           + this.jvmPort
                                                                                           + "/jmxrmi"),
                                                                         null);
            connector.connect();

            this.connection = connector.getMBeanServerConnection();
        } catch (Exception e) {
            final String msg = "Error initializing the JMV monitor. Unable to connect to JVM at port "
                               + this.jvmPort;
            log.error(msg, e);
            throw new MonitorConfigurationException(msg, e);
        }
    }

    /**
     * Gets the names of MBeans controlled by the MBean server
     * 
     * @param name MBean name
     * @return MBean object name
     */
    ObjectName getObjectName(
                              String name ) {

        Set<ObjectName> mBeanNames;
        try {
            mBeanNames = connection.queryNames(new ObjectName(name), null);
        } catch (Exception e) {
            final String errorMsg = "Error getting the names of MBeans on the monitored JVM application. Searched patter is "
                                    + name;
            log.error(errorMsg, e);
            throw new MonitorConfigurationException(errorMsg, e);
        }

        if (mBeanNames.size() != 1) {
            final String errorMsg = "Error getting the names of MBeans on the monitored JVM application. Searched patter is "
                                    + name + ". We expected to find 1, but found " + mBeanNames
                                    + " MBean names.";
            log.error(errorMsg);
            throw new MonitorConfigurationException(errorMsg);
        }

        Iterator<ObjectName> it = mBeanNames.iterator();
        return it.next();
    }

    Set<ObjectName> getObjectNames( String regex, boolean oneEntry ) {

        Set<ObjectName> names = new HashSet<ObjectName>();
        try {
            if (mBeanNames == null) {
                synchronized (lock) {
                    if (mBeanNames == null) {
                        mBeanNames = Collections.synchronizedSet(connection.queryNames(null, null));
                    }
                }
            }

            Iterator<ObjectName> it = mBeanNames.iterator();
            while (it.hasNext()) {
                ObjectName name = it.next();
                if (Pattern.matches(regex, name.getCanonicalName())) {
                    names.add(name);
                }
            }

        } catch (Exception e) {
            final String errorMsg = "Error getting the names of MBeans on the monitored JVM application. Searched pattern is "
                                    + regex;
            log.error(errorMsg, e);
            throw new MonitorConfigurationException(errorMsg, e);
        }

        if (oneEntry && names.size() != 1) {
            final String errorMsg = "Error getting the names of MBeans on the monitored JVM application. Searched pattern is "
                                    + regex + ". We expected to find 1, but found " + names
                                    + " MBean names.";
            log.error(errorMsg);
            throw new MonitorConfigurationException(errorMsg);
        }

        return names;
    }

    /**
     * Gets the value of a specific attribute of a named MBean
     * 
     * @param objectName the object name
     * @param attributeName the attribute name
     * @return the attribute value
     */
    Object getMBeanAttribute(
                              ObjectName objectName,
                              String attributeName ) {

        try {
            for (MBeanAttributeInfo attInfo : connection.getMBeanInfo(objectName).getAttributes()) {

                String attName = attInfo.getName();
                if (attName.equals(attributeName)) {
                    return connection.getAttribute(objectName, attributeName);
                }
            }
        } catch (Exception e) {
            final String errorMsg = "Error getting the value of the '" + attributeName
                                    + "' attribute of MBean with name '" + objectName + "'";
            log.error(errorMsg, e);
            throw new MonitorConfigurationException(errorMsg, e);
        }

        final String errorMsg = "Error getting the value of the '" + attributeName
                                + "' attribute of MBean with name '" + objectName
                                + "': The attribute is not found!";
        log.error(errorMsg);
        throw new MonitorConfigurationException(errorMsg);
    }
}
