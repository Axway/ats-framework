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
package com.axway.ats.monitoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.axway.ats.agent.webapp.client.RestHelper;
import com.axway.ats.agent.webapp.client.listeners.TestcaseStateListener;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.monitoring.MonitoringException;
import com.axway.ats.core.monitoring.SystemMonitorDefinitions;
import com.axway.ats.core.utils.ExceptionUtils;
import com.axway.ats.core.utils.ExecutorUtils;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.utils.StringUtils;

/**
 * The public interface for interacting with the System Monitor.
 *
 * The monitored host parameter accepted by some methods is actually the ATS
 * Agent address.
 *
 * <br/>
 * <br/>
 * <b>User guide</b> <a href=
 * "https://axway.github.io/ats-framework/ATS-OS-Documentation">page</a> for the
 * System monitor
 */
public class SystemMonitor {

    private static final Logger log = Logger.getLogger(SystemMonitor.class);

    /** All CPU related statistics */
    @PublicAtsApi
    public static final class MONITOR_CPU {
        @PublicAtsApi
        public static final String ALL                        = SystemMonitorDefinitions.READING_CPU;
        @PublicAtsApi
        public static final String LOAD_LAST_MINUTE           = SystemMonitorDefinitions.READING_CPU_LOAD__LAST_MINUTE;
        @PublicAtsApi
        public static final String LOAD_LOAD__LAST_5_MINUTES  = SystemMonitorDefinitions.READING_CPU_LOAD__LAST_5_MINUTES;
        @PublicAtsApi
        public static final String LOAD_LOAD__LAST_15_MINUTES = SystemMonitorDefinitions.READING_CPU_LOAD__LAST_15_MINUTES;

        @PublicAtsApi
        public static final String USAGE__TOTAL               = SystemMonitorDefinitions.READING_CPU_USAGE__TOTAL;
        @PublicAtsApi
        public static final String USAGE__WAIT                = SystemMonitorDefinitions.READING_CPU_USAGE__WAIT;
        @PublicAtsApi
        public static final String USAGE__KERNEL              = SystemMonitorDefinitions.READING_CPU_USAGE__KERNEL;
        @PublicAtsApi
        public static final String USAGE__USER                = SystemMonitorDefinitions.READING_CPU_USAGE__USER;
    }

    /** All MEMORY related statistics */
    @PublicAtsApi
    public static final class MONITOR_MEMORY {
        @PublicAtsApi
        public static final String ALL         = SystemMonitorDefinitions.READING_MEMORY;
        @PublicAtsApi
        public static final String ACTUAL_USED = SystemMonitorDefinitions.READING_MEMORY__ACTUAL_USED;
        @PublicAtsApi
        public static final String ACTUAL_FREE = SystemMonitorDefinitions.READING_MEMORY__ACTUAL_FREE;
        @PublicAtsApi
        public static final String USED        = SystemMonitorDefinitions.READING_MEMORY__USED;
        @PublicAtsApi
        public static final String FREE        = SystemMonitorDefinitions.READING_MEMORY__FREE;
    }

    /** All VIRTUAL MEMORY related statistics */
    @PublicAtsApi
    public static final class MONITOR_VIRTUAL_MEMORY {
        @PublicAtsApi
        public static final String ALL       = SystemMonitorDefinitions.READING_VIRTUAL_MEMORY;
        @PublicAtsApi
        public static final String TOTAL     = SystemMonitorDefinitions.READING_VIRTUAL_MEMORY__TOTAL;
        @PublicAtsApi
        public static final String USED      = SystemMonitorDefinitions.READING_VIRTUAL_MEMORY__USED;
        @PublicAtsApi
        public static final String FREE      = SystemMonitorDefinitions.READING_VIRTUAL_MEMORY__FREE;
        @PublicAtsApi
        public static final String PAGES_IN  = SystemMonitorDefinitions.READING_VIRTUAL_MEMORY__PAGES_IN;
        @PublicAtsApi
        public static final String PAGES_OUT = SystemMonitorDefinitions.READING_VIRTUAL_MEMORY__PAGES_OUT;
    }

    /** All IO related statistics */
    @PublicAtsApi
    public static final class MONITOR_IO {
        @PublicAtsApi
        public static final String ALL                     = SystemMonitorDefinitions.READING_IO;
        @PublicAtsApi
        public static final String READ_BYTES_ALL_DEVICES  = SystemMonitorDefinitions.READING_IO__READ_BYTES_ALL_DEVICES;
        @PublicAtsApi
        public static final String WRITE_BYTES_ALL_DEVICES = SystemMonitorDefinitions.READING_IO__WRITE_BYTES_ALL_DEVICES;
    }

    /** The network activity on all network interfaces */
    @PublicAtsApi
    public static final class MONITOR_NETWORK_INTERFACES {
        @PublicAtsApi
        public static final String ALL     = SystemMonitorDefinitions.READING_NETWORK_INTERFACES;
        @PublicAtsApi
        public static final String TRAFFIC = SystemMonitorDefinitions.READING_NETWORK_TRAFFIC;
    }

    @PublicAtsApi
    public static final class MONITOR_NETSTAT {
        @PublicAtsApi
        public static final String ALL                         = SystemMonitorDefinitions.READING_NETSTAT;
        @PublicAtsApi
        public static final String ACTIVE_CONNECTION_OPENINGS  = SystemMonitorDefinitions.READING_NETSTAT__ACTIVE_CONNECTION_OPENINGS;
        @PublicAtsApi
        public static final String PASSIVE_CONNECTION_OPENINGS = SystemMonitorDefinitions.READING_NETSTAT__PASSIVE_CONNECTION_OPENINGS;
        @PublicAtsApi
        public static final String FAILED_CONNECTION_ATTEMPTS  = SystemMonitorDefinitions.READING_NETSTAT__FAILED_CONNECTION_ATTEMPTS;
        @PublicAtsApi
        public static final String RESET_CONNECTIONS           = SystemMonitorDefinitions.READING_NETSTAT__RESET_CONNECTIONS;
        @PublicAtsApi
        public static final String CURRENT_CONNECTIONS         = SystemMonitorDefinitions.READING_NETSTAT__CURRENT_CONNECTIONS;
        @PublicAtsApi
        public static final String SEGMENTS_RECEIVED           = SystemMonitorDefinitions.READING_NETSTAT__SEGMENTS_RECEIVED;
        @PublicAtsApi
        public static final String SEGMENTS_SENT               = SystemMonitorDefinitions.READING_NETSTAT__SEGMENTS_SENT;
        @PublicAtsApi
        public static final String SEGMENTS_RETRANSMITTED      = SystemMonitorDefinitions.READING_NETSTAT__SEGMENTS_RETRANSMITTED;
        @PublicAtsApi
        public static final String OUT_RESETS                  = SystemMonitorDefinitions.READING_NETSTAT__OUT_RESETS;
        @PublicAtsApi
        public static final String IN_ERRORS                   = SystemMonitorDefinitions.READING_NETSTAT__IN_ERRORS;
    }

    @PublicAtsApi
    public static final class MONITOR_TCP {
        @PublicAtsApi
        public static final String ALL            = SystemMonitorDefinitions.READING_TCP;
        @PublicAtsApi
        public static final String CLOSE          = SystemMonitorDefinitions.READING_TCP__CLOSE;
        @PublicAtsApi
        public static final String LISTEN         = SystemMonitorDefinitions.READING_TCP__LISTEN;
        @PublicAtsApi
        public static final String SYN_SENT       = SystemMonitorDefinitions.READING_TCP__SYN_SENT;
        @PublicAtsApi
        public static final String SYN_RECEIVED   = SystemMonitorDefinitions.READING_TCP__SYN_RECEIVED;
        @PublicAtsApi
        public static final String ESTABLISHED    = SystemMonitorDefinitions.READING_TCP__ESTABLISHED;
        @PublicAtsApi
        public static final String CLOSE_WAIT     = SystemMonitorDefinitions.READING_TCP__CLOSE_WAIT;
        @PublicAtsApi
        public static final String LAST_ACK       = SystemMonitorDefinitions.READING_TCP__LAST_ACK;
        @PublicAtsApi
        public static final String FIN_WAIT1      = SystemMonitorDefinitions.READING_TCP__FIN_WAIT1;
        @PublicAtsApi
        public static final String FIN_WAIT2      = SystemMonitorDefinitions.READING_TCP__FIN_WAIT2;
        @PublicAtsApi
        public static final String CLOSING        = SystemMonitorDefinitions.READING_TCP__CLOSING;
        @PublicAtsApi
        public static final String TIME_WAIT      = SystemMonitorDefinitions.READING_TCP__TIME_WAIT;
        @PublicAtsApi
        public static final String BOUND          = SystemMonitorDefinitions.READING_TCP__BOUND;
        @PublicAtsApi
        public static final String IDLE           = SystemMonitorDefinitions.READING_TCP__IDLE;
        @PublicAtsApi
        public static final String TOTAL_INBOUND  = SystemMonitorDefinitions.READING_TCP__TOTAL_INBOUND;
        @PublicAtsApi
        public static final String TOTAL_OUTBOUND = SystemMonitorDefinitions.READING_TCP__TOTAL_OUTBOUND;
    }

    /** A process CPU statistics */
    @PublicAtsApi
    public static final class MONITOR_PROCESS_CPU {
        @PublicAtsApi
        public static final String ALL          = SystemMonitorDefinitions.READING_PROCESS_CPU;
        @PublicAtsApi
        public static final String USAGE_USER   = SystemMonitorDefinitions.READING_PROCESS_CPU__USAGE_USER;
        @PublicAtsApi
        public static final String USAGE_KERNEL = SystemMonitorDefinitions.READING_PROCESS_CPU__USAGE_KERNEL;
        @PublicAtsApi
        public static final String USAGE_TOTAL  = SystemMonitorDefinitions.READING_PROCESS_CPU__USAGE_TOTAL;
    }

    /** A process MEMORY statistics */
    @PublicAtsApi
    public static final class MONITOR_PROCESS_MEMORY {
        @PublicAtsApi
        public static final String ALL         = SystemMonitorDefinitions.READING_PROCESS_MEMORY;
        @PublicAtsApi
        public static final String VIRTUAL     = SystemMonitorDefinitions.READING_PROCESS_MEMORY__VIRTUAL;
        @PublicAtsApi
        public static final String RESIDENT    = SystemMonitorDefinitions.READING_PROCESS_MEMORY__RESIDENT;
        @PublicAtsApi
        public static final String SHARED      = SystemMonitorDefinitions.READING_PROCESS_MEMORY__SHARED;
        @PublicAtsApi
        public static final String PAGE_FAULTS = SystemMonitorDefinitions.READING_PROCESS_MEMORY__PAGE_FAULTS;
    }

    /** JVM related statistics */
    @PublicAtsApi
    public static final class MONITOR_JVM {
        @PublicAtsApi
        public static final String MEMORY_HEAP                           = SystemMonitorDefinitions.READING_JVM__MEMORY_HEAP;
        @PublicAtsApi
        public static final String MEMORY_HEAP_YOUNG_GENERATION_EDEN     = SystemMonitorDefinitions.READING_JVM__MEMORY_HEAP_YOUNG_GENERATION_EDEN;
        @PublicAtsApi
        public static final String MEMORY_HEAP_YOUNG_GENERATION_SURVIVOR = SystemMonitorDefinitions.READING_JVM__MEMORY_HEAP_YOUNG_GENERATION_SURVIVOR;
        @PublicAtsApi
        public static final String MEMORY_HEAP_OLD_GENERATION            = SystemMonitorDefinitions.READING_JVM__MEMORY_HEAP_OLD_GENERATION;
        @PublicAtsApi
        public static final String MEMORY_PERMANENT_GENERATION           = SystemMonitorDefinitions.READING_JVM__MEMORY_PERMANENT_GENERATION;
        @PublicAtsApi
        public static final String MEMORY_CODE_CACHE                     = SystemMonitorDefinitions.READING_JVM__MEMORY_CODE_CACHE;

        @PublicAtsApi
        public static final String CLASSES_COUNT                         = SystemMonitorDefinitions.READING_JVM__CLASSES_COUNT;

        @PublicAtsApi
        public static final String THREADS_COUNT                         = SystemMonitorDefinitions.READING_JVM__THREADS_COUNT;
        @PublicAtsApi
        public static final String THREADS_DAEMON_COUNT                  = SystemMonitorDefinitions.READING_JVM__THREADS_DAEMON_COUNT;

        @PublicAtsApi
        public static final String CPU_USAGE                             = SystemMonitorDefinitions.READING_JVM__CPU_USAGE;
    }

    private Map<String, ConnectionInfo> connectionsInformation;

    private boolean                     isStarted = false;

    /**
     * This class encapsulates necessary connection information to each ATS agent on which a monitoring is started
     * */
    private class ConnectionInfo {

        /**
         * The class that will execute the REST/HTTP request
         * */
        RestHelper helper     = null;
        /**
         * The resource ID that corresponds to a {@link RestSystemMonitor} instance created on some agent
         * */
        int        resourceId = -1;
        /**
         * The sessionID/caller that identifies the current thread, host and project (working directory)
         * */
        String     sessionId  = null;
    }

    /**
     * Create the system monitor instance. <br>
     * The first time an instance of this class is created, we initialize the
     * Performance Monitoring service by passing the default configuration
     * values. It is also possible to specify a custom configuration by placing
     * a "custom.linux.shell.config.xml" file in the classpath. Refer to the
     * online documentation for the correct template to use.
     */
    @PublicAtsApi
    public SystemMonitor() {

        this.connectionsInformation = new HashMap<String, ConnectionInfo>();

    }

    /**
     * Schedule some system monitors.
     * <br>No statistics collection will be triggered until the startMonitor method is called.
     *
     * @param monitoredHost the host to monitor
     * @param systemReadingTypes what kind of data to collect. Use some of the following constants:
     * <ul>
     * <li>SystemMonitor.MONITOR_CPU.*
     * <li>SystemMonitor.MONITOR_MEMORY.*
     * <li>SystemMonitor.MONITOR_VIRTUAL_MEMORY.*
     * <li>SystemMonitor.MONITOR_IO.*
     * <li>SystemMonitor.MONITOR_NETWORK_INTERFACES.*
     * <li>SystemMonitor.MONITOR_NETSTAT.*
     * <li>SystemMonitor.MONITOR_TCP.*
     * </ul>
     */
    @PublicAtsApi
    public void scheduleSystemMonitoring(
                                          String monitoredHost,
                                          String[] systemReadingTypes ) {

        monitoredHost = HostUtils.getAtsAgentIpAndPort(monitoredHost);

        ConnectionInfo info = performSetup(monitoredHost);
        StringBuilder sb = new StringBuilder();
        sb.append("{")
          .append("\"")
          .append("sessionId")
          .append("\"")
          .append(":")
          .append("\"")
          .append(info.sessionId)
          .append("\"")
          .append(",")
          .append("\"")
          .append("resourceId")
          .append("\"")
          .append(":")
          .append(info.resourceId)
          .append(",")
          .append("systemReadingTypes")
          .append(":")
          .append(info.helper.serializeJavaObject(systemReadingTypes))
          .append("}");

        try {
            info.helper.executeRequest(monitoredHost, "system/monitoring/schedule/monitoring/system", "POST",
                                       sb.toString(), null, null);
        } catch (Exception e) {
            throw new MonitoringException("Unable to schedule system monitoring on '" + monitoredHost + "'", e);
        }

    }

    /**
     * Schedule a monitor and pass some custom parameters to it.
     * <br>No statistics collection will be triggered until the startMonitor method is called.
     *
     * @param monitoredHost the host to monitor
     * @param readingType what kind of data to collect. You call a custom monitor here or some of the SystemMonitor.MONITORTYPE_* constants
     * @param readingParameters the parameters this monitor knows how to work with
     */
    @PublicAtsApi
    public void scheduleMonitoring(
                                    String monitoredHost,
                                    String readingType,
                                    Map<String, String> readingParameters ) {

        monitoredHost = HostUtils.getAtsAgentIpAndPort(monitoredHost);
        ConnectionInfo info = performSetup(monitoredHost);
        StringBuilder sb = new StringBuilder();
        sb.append("{")
          .append("\"")
          .append("sessionId")
          .append("\"")
          .append(":")
          .append("\"")
          .append(info.sessionId)
          .append("\"")
          .append(",")
          .append("\"")
          .append("resourceId")
          .append("\"")
          .append(":")
          .append(info.resourceId)
          .append(",")
          .append("readingType")
          .append(":")
          .append("\"")
          .append(readingType)
          .append("\"")
          .append(",")
          .append("\"")
          .append("readingParameters")
          .append("\"")
          .append(":")
          .append(info.helper.serializeJavaObject(readingParameters))
          .append("}");

        try {
            info.helper.executeRequest(monitoredHost, "system/monitoring/schedule/monitoring", "POST",
                                       sb.toString(), null, null);
        } catch (Exception e) {
            throw new MonitoringException("Unable to schedule monitoring on '" + monitoredHost + "'", e);
        }
    }

    /**
     * Schedule monitoring on a system process.
     * </br>No statistics collection will be triggered until the startMonitor method is called.
     *
     * @param monitoredHost the host where the monitored process lives
     * @param processPattern the pattern to use in order to find the process among all system processes.
     * <br><b>Note: </b>We match the processes by finding the given processPattern in the start command of the process.
     * <br>This means that it is possible to match more than one process. In this case a number is appended to the processAlias.
     * <br>For example if the processPattern is "my_process" matching 2 processes will give "my_process" and "my_process [2]"
     * @param processAlias the process alias to use when logging into the database
     * @param processReadingTypes what kind of data to collect. Use some of the SystemMonitor.MONITOR_PROCESS_* constants
     */
    @PublicAtsApi
    public void scheduleProcessMonitoring(
                                           String monitoredHost,
                                           String processPattern,
                                           String processAlias,
                                           String[] processReadingTypes ) {

        scheduleProcessMonitoring(monitoredHost,
                                  null,
                                  processPattern,
                                  processAlias,
                                  null,
                                  processReadingTypes);

    }

    /**
     * Schedule monitoring on a system process. This method specifies the name of the user who started the process to monitor.
     * </br>No statistics collection will be triggered until the startMonitor method is called.
     *
     * @param monitoredHost the host where the monitored process lives
     * @param processPattern the pattern to use in order to find the process among all system processes.
     * <br><b>Note: </b>We match the processes by finding the given processPattern in the start command of the process.
     * <br>This means that it is possible to match more than one process. In this case a number is appended to the processAlias.
     * <br>For example if the processPattern is "my_process" matching 2 processes will give "my_process" and "my_process [2]"
     * @param processAlias the process alias to use when logging into the database
     * @param processUsername the name of the user who started this process
     * @param processReadingTypes what kind of data to collect. Use some of the SystemMonitor.MONITOR_PROCESS_* constants
     */
    @PublicAtsApi
    public void scheduleProcessMonitoring(
                                           String monitoredHost,
                                           String processPattern,
                                           String processAlias,
                                           String processUsername,
                                           String[] processReadingTypes ) {

        scheduleProcessMonitoring(monitoredHost,
                                  null,
                                  processPattern,
                                  processAlias,
                                  processUsername,
                                  processReadingTypes);
    }

    /**
    * It works in the same way as the <b>scheduleProcessMonitoring</b> method works with an extra parameter specifying
    * a name of a parent process.
    * </br>When one or more processes have a parent process specified, the parent process will combine
    * the statistics of all of its children processes.
    * </br>This way it is possible to get a picture of the resource usage of a whole tested product
    * which is running more than one actual system processes
    *
    * @param monitoredHost the host where the monitored process lives
    * @param parentProcess the virtual parent process
    * @param processPattern the pattern to use in order to find the process among all system processes.
    * @param processAlias the process alias to use when logging into the database
    * @param processReadingTypes what kind of data to collect. Use some of the SystemMonitor.MONITOR_PROCESS_* constants
    */
    @PublicAtsApi
    public void scheduleChildProcessMonitoring(
                                                String monitoredHost,
                                                String parentProcess,
                                                String processPattern,
                                                String processAlias,
                                                String[] processReadingTypes ) {

        monitoredHost = HostUtils.getAtsAgentIpAndPort(monitoredHost);
        performSetup(monitoredHost);
        scheduleProcessMonitoring(monitoredHost,
                                  parentProcess,
                                  processPattern,
                                  processAlias,
                                  null,
                                  processReadingTypes);
    }

    /**
    * It works in the same way as the <b>scheduleProcessMonitoring</b> method works with an extra parameter specifying
    * a name of a parent process.
    * </br>When one or more processes have a parent process specified, the parent process will combine
    * the statistics of all of its children processes.
    * </br>This way it is possible to get a picture of the resource usage of a whole tested product
    * which is running more than one actual system processes
    *
    * @param monitoredHost the host where the monitored process lives
    * @param parentProcess the virtual parent process
    * @param processPattern the pattern to use in order to find the process among all system processes.
    * @param processAlias the process alias to use when logging into the database
    * @param processUsername the name of the user who started this process
    * @param processReadingTypes what kind of data to collect. Use some of the SystemMonitor.MONITOR_PROCESS_* constants
    */
    @PublicAtsApi
    public void scheduleChildProcessMonitoring(
                                                String monitoredHost,
                                                String parentProcess,
                                                String processPattern,
                                                String processAlias,
                                                String processUsername,
                                                String[] processReadingTypes ) {

        monitoredHost = HostUtils.getAtsAgentIpAndPort(monitoredHost);
        performSetup(monitoredHost);
        scheduleProcessMonitoring(monitoredHost,
                                  parentProcess,
                                  processPattern,
                                  processAlias,
                                  processUsername,
                                  processReadingTypes);
    }

    /**
     * Schedule monitoring a JVM application
     * <br><b>Note:</b> You must open the tested JVM application for JMX connections by adding the following
     * runtime properties:
     * "-Dcom.sun.management.jmxremote.port=[PORT NUMBER] -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
     *
     * @param monitoredHost the host where the monitored application is
     * @param jvmPort the JMX port which is used for monitoring the JVM application
     * @param jvmReadingTypes what kind of data to collect. Use some of the SystemMonitor.MONITOR_JVM_* constants
     */
    @PublicAtsApi
    public void scheduleJvmMonitoring(
                                       String monitoredHost,
                                       String jvmPort,
                                       String[] jvmReadingTypes ) {

        monitoredHost = HostUtils.getAtsAgentIpAndPort(monitoredHost);
        scheduleJvmMonitoring(monitoredHost, jvmPort, null, jvmReadingTypes);
    }

    /**
     * Schedule monitoring a JVM application
     * <br><b>Note:</b> You must open the tested JVM application for JMX connections by adding the following
     * runtime properties:
     * "-Dcom.sun.management.jmxremote.port=[PORT NUMBER] -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
     *
     * @param monitoredHost the host where the monitored application is
     * @param jvmPort the JMX port which is used for monitoring the JVM application
     * @param alias the alias to use when logging into the database
     * @param jvmReadingTypes what kind of data to collect. Use some of the SystemMonitor.MONITOR_JVM_* constants
     */
    @PublicAtsApi
    public void scheduleJvmMonitoring(
                                       String monitoredHost,
                                       String jvmPort,
                                       String alias,
                                       String[] jvmReadingTypes ) {

        monitoredHost = HostUtils.getAtsAgentIpAndPort(monitoredHost);
        ConnectionInfo info = performSetup(monitoredHost);
        StringBuilder sb = new StringBuilder();
        sb.append("{")
          .append("\"")
          .append("sessionId")
          .append("\"")
          .append(":")
          .append("\"")
          .append(info.sessionId)
          .append("\"")
          .append(",")
          .append("\"")
          .append("resourceId")
          .append("\"")
          .append(":")
          .append(info.resourceId)
          .append(",")
          .append("\"")
          .append("jvmPort")
          .append("\"")
          .append(":")
          .append("\"")
          .append(jvmPort)
          .append("\"")
          .append(",")
          .append("\"")
          .append("jvmReadingTypes")
          .append("\"")
          .append(":")
          .append(info.helper.serializeJavaObject(jvmReadingTypes))
          .append(",")
          .append("\"")
          .append("alias")
          .append("\"")
          .append(":");
        if (StringUtils.isNullOrEmpty(alias)) {
            sb.append(alias);
        } else {
            sb.append("\"")
              .append(alias)
              .append("\"");
        }
        sb.append("}");
        try {
            info.helper.executeRequest(monitoredHost, "system/monitoring/schedule/monitoring/jvm", "POST",
                                       sb.toString(), null, null);
        } catch (Exception e) {
            throw new MonitoringException("Unable to schedule JVM monitoring on '" + monitoredHost + "'", e);
        }
    }

    /**
     * Schedule custom monitoring a JVM application
     * <br><b>Note:</b>You can monitor just a single process.
     * You must open the tested JVM application for JMX connections by adding the following
     * runtime properties:
     * "-Dcom.sun.management.jmxremote.port=[PORT NUMBER] -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
     *
     * @param monitoredHost the host where the monitored application is
     * @param jmxPort the JMX port which is used for monitoring the JVM application
     * @param alias the process alias name to be used when displaying the results
     * @param mbeanName the name of the mbean than would be monitored
     * @param unit the metric unit
     * @param mbeanAttributes the MBean attribute to capture values for. 
     * </br><b>Note: </b>This can be an array of nested attributes as sometimes the first MBean attribute is not a simple value but a composite element. Then the following attribute is the actual one to track.
     * </br>For example when monitoring the Heap Used Memory, you have to provide "java.lang:type=Memory" as MBean name, the first level attribute is "HeapMemoryUsage" which is a composite element and its "used" attribute is the one of interest. In such case you need to provide here: "HeapMemoryUsage", "used"
     * </br><b>Note: </b> Order of the mbeanAttributes is important
     */
    @PublicAtsApi
    public void scheduleCustomJvmMonitoring(
                                             String monitoredHost,
                                             String jmxPort,
                                             String alias,
                                             String mbeanName,
                                             String unit,
                                             String... mbeanAttributes ) {

        monitoredHost = HostUtils.getAtsAgentIpAndPort(monitoredHost);
        ConnectionInfo info = performSetup(monitoredHost);
        StringBuilder sb = new StringBuilder();
        sb.append("{")
          .append("\"")
          .append("sessionId")
          .append("\"")
          .append(":")
          .append("\"")
          .append(info.sessionId)
          .append("\"")
          .append(",")
          .append("\"")
          .append("resourceId")
          .append("\"")
          .append(":")
          .append(info.resourceId)
          .append(",")
          .append("\"")
          .append("jmxPort")
          .append("\"")
          .append(":")
          .append("\"")
          .append(jmxPort)
          .append("\"")
          .append(",")
          .append("\"")
          .append("mbeanName")
          .append("\"")
          .append(":")
          .append("\"")
          .append(mbeanName)
          .append("\"")
          .append(",")
          .append("\"")
          .append("unit")
          .append("\"")
          .append(":")
          .append("\"")
          .append(unit)
          .append("\"")
          .append(",")
          .append("\"")
          .append("mbeanAttributes")
          .append("\"")
          .append(":")
          .append(info.helper.serializeJavaObject(mbeanAttributes))
          .append(",")
          .append("\"")
          .append("alias")
          .append("\"")
          .append(":")
          .append("\"")
          .append(alias)
          .append("\"");
        sb.append("}");
        try {
            info.helper.executeRequest(monitoredHost, "system/monitoring/schedule/monitoring/jvm/custom", "POST",
                                       sb.toString(), null, null);
        } catch (Exception e) {
            throw new MonitoringException("Unable to schedule custom JVM monitoring on '" + monitoredHost + "'", e);
        }
    }

    /**
     * Schedule monitoring the user activity on an ATS Agent(usually used as a performance test loader).
     * </br>No statistics collection will be triggered until the startMonitor method is called.
     *
     * @param atsAgent the ATS Agent which runs the monitored virtual users
     */
    @PublicAtsApi
    public void scheduleUserActivityMonitoring(
                                                String atsAgent ) {

        atsAgent = HostUtils.getAtsAgentIpAndPort(atsAgent);

        ConnectionInfo info = performSetup(atsAgent);
        StringBuilder sb = new StringBuilder();
        sb.append("{")
          .append("\"")
          .append("sessionId")
          .append("\"")
          .append(":")
          .append("\"")
          .append(info.sessionId)
          .append("\"")
          .append(",")
          .append("\"")
          .append("resourceId")
          .append("\"")
          .append(":")
          .append(info.resourceId)
          .append("}");

        try {
            info.helper.executeRequest(atsAgent, "system/monitoring/schedule/userActivity", "POST",
                                       sb.toString(), null, null);
        } catch (Exception e) {
            throw new MonitoringException("Unable to schedule user activity monitoring on '" + atsAgent + "'", e);
        }
    }

    /**
     * Start monitoring
     *
     * @param pollingInterval in how many seconds to record the requested data
     */
    @PublicAtsApi
    public void startMonitoring(
                                 int pollingInterval ) {

        List<String> errorsMessages = new ArrayList<>();
        for (Map.Entry<String, ConnectionInfo> entry : this.connectionsInformation.entrySet()) {
            String monitoredHost = entry.getKey();
            // start monitoring
            String errorMessage = startMonitoring(pollingInterval, monitoredHost, entry.getValue());
            if (errorMessage != null) {
                errorsMessages.add(errorMessage);
            }
        }
        // any monitoring operations on the agents is already cancelled,
        // so here, we just log the errors from each agent
        if (errorsMessages.size() > 0) {
            for (String errorMessage : errorsMessages) {
                log.error("The following error occured while starting the system monitoring process: "
                          + errorMessage);
            }
            throw new MonitoringException("There were error starting the system monitoring process");
        }

        isStarted = true;

    }

    private String startMonitoring( int pollingInterval, String monitoredHost, ConnectionInfo info ) {

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{")
              .append("\"")
              .append("sessionId")
              .append("\"")
              .append(":")
              .append("\"")
              .append(info.sessionId)
              .append("\"")
              .append(",")
              .append("\"")
              .append("resourceId")
              .append("\"")
              .append(":")
              .append(info.resourceId)
              .append(",")
              .append("\"")
              .append("pollingInterval")
              .append("\"")
              .append(":")
              .append(pollingInterval)
              .append(",")
              .append("\"")
              .append("startTimestamp")
              .append("\"")
              .append(":")
              .append(System.currentTimeMillis())
              .append("}");
            info.helper.executeRequest(monitoredHost, "system/monitoring/start", "POST", sb.toString(),
                                       null, null);
            return null;
        } catch (Exception e) {
            return ExceptionUtils.getExceptionMsg(e);
        }
    }

    /**
     * Stop all monitoring activity
     */
    @PublicAtsApi
    public void stopMonitoring() {

        List<String> errorsMessages = new ArrayList<>();
        for (Map.Entry<String, ConnectionInfo> entry : this.connectionsInformation.entrySet()) {
            String monitoredHost = entry.getKey();
            // stop monitoring
            String errorMessage = stopMonitoring(monitoredHost, entry.getValue());
            if (errorMessage != null) {
                errorsMessages.add(errorMessage);
            }
        }
        // any monitoring operations on the agents is already cancelled,
        // so here, we just log the errors from each agent
        if (errorsMessages.size() > 0) {
            for (String errorMessage : errorsMessages) {
                log.error("The following error occured while stopping the system monitoring process: "
                          + errorMessage);
            }
            throw new MonitoringException("There were error stopping the system monitoring process");
        }

        isStarted = false;
    }

    private String stopMonitoring( String monitoredHost, ConnectionInfo info ) {

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{")
              .append("\"")
              .append("sessionId")
              .append("\"")
              .append(":")
              .append("\"")
              .append(info.sessionId)
              .append("\"")
              .append(",")
              .append("\"")
              .append("resourceId")
              .append("\"")
              .append(":")
              .append(info.resourceId)
              .append("}");
            info.helper.executeRequest(monitoredHost, "system/monitoring/stop", "POST", sb.toString(),
                                       null, null);
            return null;
        } catch (Exception e) {
            return ExceptionUtils.getExceptionMsg(e);
        }
    }

    /**
    *
    * @return <code>true</code> if the monitoring is started and <code>false</code> if it is not
    */
    @PublicAtsApi
    public boolean isStarted() {

        return isStarted;
    }

    /**
     * We will push db connection configuration to the agent (if necessary)
     * <br>Also we will execute join testcase to the agent
     * 
     * @param monitoredHost the AGENT_IP:AGENT_PORT
     * 
     * @return {@link ConnectionInfo} connection informanion
     */
    private ConnectionInfo performSetup(
                                         String monitoredHost ) {

        ConnectionInfo info = this.connectionsInformation.get(monitoredHost);
        if (info != null) {
            return info;
        }
        // this monitored host/agent is not configured
        try {
            TestcaseStateListener.getInstance().onConfigureAtsAgents(Arrays.asList(monitoredHost));
            info = new ConnectionInfo();
            info.helper = new RestHelper();
            info.sessionId = ExecutorUtils.createExecutorId(monitoredHost, ExecutorUtils.getUserRandomToken(),
                                                            Thread.currentThread().getName());
        } catch (Exception e) {
            throw new MonitoringException("Unable to configure ATS agent at '" + monitoredHost + "'", e);
        }

        // initialize system monitor on the agent
        initializeAgentSystemMonitor(monitoredHost, info);
        // initialize monitoring context for the newly creates system monitor
        initializeAgentMonitoringContext(monitoredHost, info);
        this.connectionsInformation.put(monitoredHost, info);

        return info;
    }

    private void initializeAgentMonitoringContext( String monitoredHost, ConnectionInfo info ) {

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{")
              .append("\"sessionId\"")
              .append(":")
              .append("\"")
              .append(info.sessionId)
              .append("\"")
              .append(",")
              .append("\"")
              .append("resourceId")
              .append("\"")
              .append(":")
              .append(info.resourceId)
              .append("}");
            // add this connection info to the map
            info.helper.executeRequest(monitoredHost, "system/monitoring/initializeMonitoringContext", "POST",
                                       sb.toString(), null, null);
        } catch (Exception e) {
            throw new MonitoringException("Unable to initialize monitoring context on agent '" + monitoredHost + "'",
                                          e);
        }

    }

    private void initializeAgentSystemMonitor( String monitoredHost, ConnectionInfo info ) {

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{")
              .append("\"sessionId\"")
              .append(":")
              .append("\"")
              .append(info.sessionId)
              .append("\"")
              .append(",")
              .append("\"")
              .append("resourceId")
              .append("\"")
              .append(":")
              .append(info.resourceId)
              .append("}");
            info.resourceId = (int) info.helper.executeRequest(monitoredHost, "system/monitoring", "PUT", sb.toString(),
                                                               "resourceId", Integer.class);
        } catch (Exception e) {
            throw new MonitoringException("Unable to initialize monitoring on agent '" + monitoredHost + "'", e);
        }

    }

    private void scheduleProcessMonitoring(
                                            String monitoredHost,
                                            String parentProcess,
                                            String processPattern,
                                            String processAlias,
                                            String processUsername,
                                            String[] processReadingTypes ) {

        try {
            ConnectionInfo info = performSetup(monitoredHost);
            StringBuilder sb = new StringBuilder();
            sb.append("{")
              .append("\"")
              .append("sessionId")
              .append("\"")
              .append(":")
              .append("\"")
              .append(info.sessionId)
              .append("\"")
              .append(",")
              .append("\"")
              .append("resourceId")
              .append("\"")
              .append(":")
              .append(info.resourceId)
              .append(",")
              .append("\"")
              .append("parentProcess")
              .append("\"")
              .append(":");
            if (StringUtils.isNullOrEmpty(parentProcess)) {
                sb.append(parentProcess);
            } else {
                sb.append("\"")
                  .append(parentProcess)
                  .append("\"");
            }
            sb.append(",")
              .append("\"")
              .append("processPattern")
              .append("\"")
              .append(":")
              .append("\"")
              .append(processPattern)
              .append("\"")
              .append(",")
              .append("\"")
              .append("processAlias")
              .append("\"")
              .append(":")
              .append("\"")
              .append(processAlias)
              .append("\"")
              .append(",")
              .append("\"")
              .append("processUsername")
              .append("\"")
              .append(":");
            if (StringUtils.isNullOrEmpty(processUsername)) {
                sb.append(processUsername);
            } else {
                sb.append("\"")
                  .append(processUsername)
                  .append("\"");
            }
            sb.append(",")
              .append("\"")
              .append("processReadingTypes")
              .append("\"")
              .append(":")
              .append(info.helper.serializeJavaObject(processReadingTypes))
              .append("}");
            info.helper.executeRequest(monitoredHost,
                                       "system/monitoring/schedule/monitoring/process", "POST",
                                       sb.toString(),
                                       null, null);
        } catch (Exception e) {
            throw new MonitoringException("Unable to schedule process monitoring on agent '" + monitoredHost + "'", e);
        }
    }
}
