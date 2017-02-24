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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.axway.ats.agent.components.monitoring.operations.clients.InternalSystemMonitoringOperations;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.webapp.client.AgentMonitoringClient;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.performance.monitor.beans.FullReadingBean;
import com.axway.ats.core.monitoring.SystemMonitorDefinitions;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.core.validation.Validate;
import com.axway.ats.core.validation.ValidationType;
import com.axway.ats.core.validation.Validator;
import com.axway.ats.monitoring.model.AbstractLoggerTask;
import com.axway.ats.monitoring.model.MonitoringContext;
import com.axway.ats.monitoring.model.ReadingTypes;
import com.axway.ats.monitoring.model.SystemStatsLoggerTask;
import com.axway.ats.monitoring.model.UserActivityLoggerTask;
import com.axway.ats.monitoring.model.exceptions.MonitoringException;
import com.axway.ats.monitoring.model.readings.ReadingsRepository;

/**
* The public interface for interacting with the System Monitor.
*
* The monitored host parameter accepted by some methods is actually the ATS Agent address.
*
* <br/><br/>
* <b>User guide</b>
* <a href="https://techweb.axway.com/confluence/display/ATS/System+monitoring+service">page</a>
* for the System monitor
*/
@PublicAtsApi
public class SystemMonitor {

    private static final Logger log = Logger.getLogger( SystemMonitor.class );

    /** All CPU related statistics */
    @PublicAtsApi
    public static final class MONITOR_CPU {
        @PublicAtsApi
        public static final String ALL                        = ReadingTypes.READING_CPU;
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
        public static final String ALL         = ReadingTypes.READING_MEMORY;
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
        public static final String ALL       = ReadingTypes.READING_VIRTUAL_MEMORY;
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
        public static final String ALL                     = ReadingTypes.READING_IO;
        @PublicAtsApi
        public static final String READ_BYTES_ALL_DEVICES  = SystemMonitorDefinitions.READING_IO__READ_BYTES_ALL_DEVICES;
        @PublicAtsApi
        public static final String WRITE_BYTES_ALL_DEVICES = SystemMonitorDefinitions.READING_IO__WRITE_BYTES_ALL_DEVICES;
    }

    /** The network activity on all network interfaces */
    @PublicAtsApi
    public static final class MONITOR_NETWORK_INTERFACES {
        @PublicAtsApi
        public static final String ALL     = ReadingTypes.READING_NETWORK_INTERFACES;
        @PublicAtsApi
        public static final String TRAFFIC = SystemMonitorDefinitions.READING_NETWORK_TRAFFIC;
    }

    @PublicAtsApi
    public static final class MONITOR_NETSTAT {
        @PublicAtsApi
        public static final String ALL                         = ReadingTypes.READING_NETSTAT;;
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
        public static final String ALL            = ReadingTypes.READING_TCP;
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
        public static final String ALL          = ReadingTypes.READING_PROCESS_CPU;
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
        public static final String ALL         = ReadingTypes.READING_PROCESS_MEMORY;
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

    private ScheduledExecutorService          scheduler;

    private Map<String, ScheduledFuture<?>>   loggerTasksPerHost;

    // remember the requested monitor types in a Set, so there is no duplicated types
    private Map<String, Set<FullReadingBean>> requestedReadingTypesPerHosts;

    private Set<String>                       monitoredHosts;

    private Set<String>                       monitoredAgents;

    private long                              startTimestamp;
    private int                               pollInterval;
    private int                               loggingInterval;

    //the task which polls for user activity readings from the monitored agents
    private UserActivityLoggerTask            lastUserActivityLoggerTask;

    private boolean                           isStarted = false;

    /**
     * Create the system monitor instance.
     * <br>
     * The first time an instance of this class is created, we initialize the Performance Monitoring service
     * by passing the default configuration values. It is also possible to specify a custom
     * configuration by placing a "custom.linux.shell.config.xml" file in the classpath. Refer to
     * the online documentation for the correct template to use.
     */
    @PublicAtsApi
    public SystemMonitor() {

        this.loggerTasksPerHost = new HashMap<String, ScheduledFuture<?>>();
        this.requestedReadingTypesPerHosts = new HashMap<String, Set<FullReadingBean>>();
        this.monitoredAgents = new HashSet<String>();
        this.monitoredHosts = new HashSet<String>();

        MonitoringContext.getInstance().init();
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
    public void scheduleSystemMonitoring( @Validate(name = "monitoredHost", type = ValidationType.STRING_SERVER_WITH_PORT) String monitoredHost,
                                          @Validate(name = "systemReadingTypes", type = ValidationType.NOT_NULL) String[] systemReadingTypes ) {

        // validate input parameters
        monitoredHost = HostUtils.getAtsAgentIpAndPort( monitoredHost );
        new Validator().validateMethodParameters( "Could not schedule monitoring system statistics on '"
                                                  + monitoredHost + "'",
                                                  new Object[]{ monitoredHost, systemReadingTypes } );

        Set<FullReadingBean> readingTypes = requestedReadingTypesPerHosts.get( monitoredHost );
        if( readingTypes == null ) {
            readingTypes = new HashSet<FullReadingBean>();
        }
        readingTypes.addAll( ReadingTypes.expandSystemReadings( systemReadingTypes ) );

        requestedReadingTypesPerHosts.put( monitoredHost, readingTypes );
        monitoredHosts.add( monitoredHost );
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
    public void scheduleMonitoring( @Validate(name = "monitoredHost", type = ValidationType.STRING_SERVER_WITH_PORT) String monitoredHost,
                                    @Validate(name = "readingType", type = ValidationType.STRING_NOT_EMPTY) String readingType,
                                    @Validate(name = "readingParameters", type = ValidationType.NOT_NULL) Map<String, String> readingParameters ) {

        // validate input parameters
        monitoredHost = HostUtils.getAtsAgentIpAndPort( monitoredHost );
        new Validator().validateMethodParameters( "Could not schedule monitoring a statistic on '"
                                                  + monitoredHost + "'",
                                                  new Object[]{ monitoredHost, readingType,
                                                                readingParameters } );

        Set<FullReadingBean> readingTypes = requestedReadingTypesPerHosts.get( monitoredHost );
        if( readingTypes == null ) {
            readingTypes = new HashSet<FullReadingBean>();
        }

        Set<String> readingNames = new HashSet<String>();
        readingNames.add( readingType );

        FullReadingBean reading = ReadingsRepository.getInstance()
                                                    .getReadingXmlDefinition( readingType,
                                                                              readingParameters );
        readingTypes.add( reading );

        requestedReadingTypesPerHosts.put( monitoredHost, readingTypes );
        monitoredHosts.add( monitoredHost );
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
    public void scheduleProcessMonitoring( @Validate(name = "monitoredHost", type = ValidationType.STRING_SERVER_WITH_PORT) String monitoredHost,
                                           @Validate(name = "processPattern", type = ValidationType.STRING_NOT_EMPTY) String processPattern,
                                           @Validate(name = "processAlias", type = ValidationType.STRING_NOT_EMPTY) String processAlias,
                                           @Validate(name = "processReadingTypes", type = ValidationType.NOT_NULL) String[] processReadingTypes ) {

        // validate input parameters
        monitoredHost = HostUtils.getAtsAgentIpAndPort( monitoredHost );
        new Validator().validateMethodParameters( new Object[]{ monitoredHost, processPattern, processAlias,
                                                                processReadingTypes } );

        scheduleProcessMonitoring( monitoredHost, null, processPattern, processAlias, null,
                                   processReadingTypes );
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
    public void scheduleProcessMonitoring( @Validate(name = "monitoredHost", type = ValidationType.STRING_SERVER_WITH_PORT) String monitoredHost,
                                           @Validate(name = "processPattern", type = ValidationType.STRING_NOT_EMPTY) String processPattern,
                                           @Validate(name = "processAlias", type = ValidationType.STRING_NOT_EMPTY) String processAlias,
                                           @Validate(name = "processUsername", type = ValidationType.STRING_NOT_EMPTY) String processUsername,
                                           @Validate(name = "processReadingTypes", type = ValidationType.NOT_NULL) String[] processReadingTypes ) {

        // validate input parameters
        monitoredHost = HostUtils.getAtsAgentIpAndPort( monitoredHost );
        new Validator().validateMethodParameters( new Object[]{ monitoredHost, processPattern, processAlias,
                                                                processUsername, processReadingTypes } );

        scheduleProcessMonitoring( monitoredHost, null, processPattern, processAlias, processUsername,
                                   processReadingTypes );
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
    public void scheduleChildProcessMonitoring( @Validate(name = "monitoredHost", type = ValidationType.STRING_SERVER_WITH_PORT) String monitoredHost,
                                                @Validate(name = "parentProcess", type = ValidationType.STRING_NOT_EMPTY) String parentProcess,
                                                @Validate(name = "processPattern", type = ValidationType.STRING_NOT_EMPTY) String processPattern,
                                                @Validate(name = "processAlias", type = ValidationType.STRING_NOT_EMPTY) String processAlias,
                                                @Validate(name = "processReadingTypes", type = ValidationType.NOT_NULL) String[] processReadingTypes ) {

        // validate input parameters
        monitoredHost = HostUtils.getAtsAgentIpAndPort( monitoredHost );
        new Validator().validateMethodParameters( new Object[]{ monitoredHost, parentProcess, processPattern,
                                                                processAlias, processReadingTypes } );

        scheduleProcessMonitoring( monitoredHost, parentProcess, processPattern, processAlias, null,
                                   processReadingTypes );
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
    public void scheduleChildProcessMonitoring( @Validate(name = "monitoredHost", type = ValidationType.STRING_SERVER_WITH_PORT) String monitoredHost,
                                                @Validate(name = "parentProcess", type = ValidationType.STRING_NOT_EMPTY) String parentProcess,
                                                @Validate(name = "processPattern", type = ValidationType.STRING_NOT_EMPTY) String processPattern,
                                                @Validate(name = "processAlias", type = ValidationType.STRING_NOT_EMPTY) String processAlias,
                                                @Validate(name = "processUsername", type = ValidationType.STRING_NOT_EMPTY) String processUsername,
                                                @Validate(name = "processReadingTypes", type = ValidationType.NOT_NULL) String[] processReadingTypes ) {

        // validate input parameters
        monitoredHost = HostUtils.getAtsAgentIpAndPort( monitoredHost );
        new Validator().validateMethodParameters( "Could not schedule a process for monitoring '"
                                                  + monitoredHost + "'",
                                                  new Object[]{ monitoredHost, parentProcess, processPattern,
                                                                processAlias, processUsername,
                                                                processReadingTypes } );

        scheduleProcessMonitoring( monitoredHost, parentProcess, processPattern, processAlias,
                                   processUsername, processReadingTypes );
    }

    private void scheduleProcessMonitoring( String monitoredHost, String parentProcess, String processPattern,
                                            String processAlias, String processUsername,
                                            String[] processReadingTypes ) {

        Set<FullReadingBean> readingTypes = requestedReadingTypesPerHosts.get( monitoredHost );
        if( readingTypes == null ) {
            readingTypes = new HashSet<FullReadingBean>();
        }
        readingTypes.addAll( ReadingTypes.expandProcessReadings( parentProcess, processPattern, processAlias,
                                                                 processUsername, processReadingTypes ) );

        requestedReadingTypesPerHosts.put( monitoredHost, readingTypes );
        monitoredHosts.add( monitoredHost );
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
    public void scheduleJvmMonitoring( @Validate(name = "monitoredHost", type = ValidationType.STRING_SERVER_WITH_PORT) String monitoredHost,
                                       @Validate(name = "jvmPort", type = ValidationType.NUMBER_PORT_NUMBER) String jvmPort,
                                       @Validate(name = "jvmReadingTypes", type = ValidationType.NOT_NULL) String[] jvmReadingTypes ) {

        scheduleJvmMonitoring( monitoredHost, jvmPort, "", jvmReadingTypes );

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
                                       @Validate(name = "monitoredHost", type = ValidationType.STRING_SERVER_WITH_PORT) String monitoredHost,
                                       @Validate(name = "jvmPort", type = ValidationType.NUMBER_PORT_NUMBER) String jvmPort,
                                       @Validate(name = "alias", type = ValidationType.NOT_NULL) String alias,
                                       @Validate(name = "jvmReadingTypes", type = ValidationType.NOT_NULL) String[] jvmReadingTypes ) {

        // validate input parameters
        monitoredHost = HostUtils.getAtsAgentIpAndPort( monitoredHost );
        new Validator().validateMethodParameters( "Could not schedule monitoring JVM statistics on '"
                                                  + monitoredHost + "' at " + jvmPort + " port",
                                                  new Object[]{ monitoredHost, jvmPort, jvmReadingTypes } );

        Set<FullReadingBean> readingTypes = requestedReadingTypesPerHosts.get( monitoredHost );
        if( readingTypes == null ) {
            readingTypes = new HashSet<FullReadingBean>();
        }

        Map<String, String> readingParameters = new HashMap<String, String>();
        readingParameters.put( "JMX_PORT", jvmPort );
        if(!StringUtils.isNullOrEmpty( alias )){
            readingParameters.put( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_ALIAS, alias );
        }
        for( String readingType : jvmReadingTypes ) {
            FullReadingBean reading = ReadingsRepository.getInstance()
                                                        .getReadingXmlDefinition( readingType,
                                                                                  readingParameters );
            readingTypes.add( reading );
        }

        requestedReadingTypesPerHosts.put( monitoredHost, readingTypes );
        monitoredHosts.add( monitoredHost );
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
                                             @Validate(name = "monitoredHost", type = ValidationType.STRING_SERVER_WITH_PORT) String monitoredHost,
                                             @Validate(name = "jmxPort", type = ValidationType.NUMBER_PORT_NUMBER) String jmxPort,
                                             @Validate(name = "alias", type = ValidationType.NOT_NULL) String alias,
                                             @Validate(name = "mbeanName", type = ValidationType.NOT_NULL) String mbeanName,
                                             @Validate(name = "unit", type = ValidationType.NOT_NULL) String unit,
                                             @Validate(name = "mbeanAttributes", type = ValidationType.NOT_NULL) String... mbeanAttributes ) {

        // validate input parameters
        monitoredHost = HostUtils.getAtsAgentIpAndPort( monitoredHost );
        String jvmMonitor = "com.axway.ats.agent.components.monitoring.model.jvmmonitor.AtsJvmMonitor";

        Set<FullReadingBean> readingTypes = requestedReadingTypesPerHosts.get( monitoredHost );
        if( readingTypes == null ) {
            readingTypes = new HashSet<FullReadingBean>();
        }

        Map<String, String> readingParameters = new LinkedHashMap<String, String>();
        readingParameters.put( "JMX_PORT", jmxPort );
        readingParameters.put( "MBEAN_NAME", mbeanName );
        if( !StringUtils.isNullOrEmpty( alias ) ) {
            readingParameters.put( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_ALIAS, alias );
        }

        // we just need a list of values, so we are using only the keys of the already existing map
        if( mbeanAttributes.length > 1 ) {
            for( String att : mbeanAttributes ) {
                readingParameters.put( att, "" );
            }
        }
        // the first element in the array is always the mbean name
        FullReadingBean reading = new FullReadingBean( jvmMonitor, mbeanAttributes[0], unit );
        reading.setId( String.valueOf( ReadingsRepository.getInstance().getNewUniqueId() ) );
        reading.setParameters( readingParameters );
        readingTypes.add( reading );

        requestedReadingTypesPerHosts.put( monitoredHost, readingTypes );
        monitoredHosts.add( monitoredHost );
    }

    /**
     * Schedule monitoring the user activity on an ATS Agent(usually used as a performance test loader).
     * </br>No statistics collection will be triggered until the startMonitor method is called.
     *
     * @param atsAgent the ATS Agent which runs the monitored virtual users
     */
    @PublicAtsApi
    public void scheduleUserActivityMonitoring( @Validate(name = "atsAgent", type = ValidationType.STRING_SERVER_WITH_PORT) String atsAgent ) {

        // validate input parameters
        atsAgent = HostUtils.getAtsAgentIpAndPort( atsAgent );
        new Validator().validateMethodParameters( "Could not schedule users activity monitoring on '"
                                                  + atsAgent + "'", new Object[]{ atsAgent } );

        monitoredAgents.add( atsAgent );
    }

    /**
     * Start monitoring
     *
     * @param monitoredHost the host to monitor
     * @param collectInterval in how many seconds to record the requested data.
     * The logging interval will be 50 times the collect interval.
     */
    @PublicAtsApi
    public void startMonitoring( int collectInterval ) {

        startMonitoring( collectInterval, collectInterval * 50 );
    }

    /**
     * Start monitoring
     *
     * @param monitoredHost the host to monitor
     * @param collectInterval in how many seconds to record the requested data
     * @param loggingInterval in how many seconds to move the recorded data to the logging database
     */
    @PublicAtsApi
    public void startMonitoring( int collectInterval, int loggingInterval ) {

        if( isStarted ) {

            throw new MonitoringException( "The system monitor is already started" );
        }

        checkTimeIntervals( collectInterval, loggingInterval );

        this.scheduler = Executors.newScheduledThreadPool( 10 );
        this.startTimestamp = System.currentTimeMillis();
        this.pollInterval = collectInterval;
        this.loggingInterval = loggingInterval;

        List<MonitoringException> errorsStartingMonitoringPhysicalHosts = startMonitoringPhysicalHosts();
        if( errorsStartingMonitoringPhysicalHosts.size() > 0 ) {
            for( MonitoringException e : errorsStartingMonitoringPhysicalHosts ) {
                log.error( "The following error occured while stating the system monitoring process", e );
            }

            cancelAnyMonitoringActivity();
            throw new MonitoringException( "There were error starting the system monitoring process" );
        }

        List<MonitoringException> errorsStartingMonitoringAgents = startMonitoringAgent();
        if( errorsStartingMonitoringAgents.size() > 0 ) {
            for( MonitoringException e : errorsStartingMonitoringAgents ) {
                log.error( "The following error occured while stating the monitoring process on ATS Agent",
                           e );
            }

            cancelAnyMonitoringActivity();
            throw new MonitoringException( "There were errors starting the monitoring process on ATS Agent" );
        }

        isStarted = true;
    }

    /**
     * Stop all monitoring activity
     */
    @PublicAtsApi
    public void stopMonitoring() {

        try {
            boolean succesfulStopMonitoringPhysicalHosts = stopMonitoringPhysicalHosts( true );
            boolean succesfulStopMonitoringUserActivity = stopMonitoringAgents( true );

            if( !succesfulStopMonitoringPhysicalHosts || !succesfulStopMonitoringUserActivity ) {
                throw new MonitoringException( "There were errors stopping the monitoring process" );
            }
        } finally {
            // we have canceled all logging task ran by the scheduler, now stop the scheduler
            scheduler.shutdownNow();
        }

        isStarted = false;
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
     * We enter this method on error, so try to cancel any monitoring collection activity,
     * do not collect any remaining results
     */
    private void cancelAnyMonitoringActivity() {

        try {
            log.info( "Canceling any system monitoring activity, if there is such" );
            stopMonitoringPhysicalHosts( false );
            log.info( "Canceling any agent monitoring activity, if there is such" );
            stopMonitoringAgents( false );
        } finally {
            // we have canceled all logging task ran by the scheduler, now stop the scheduler
            scheduler.shutdownNow();
        }

    }

    private List<MonitoringException> startMonitoringPhysicalHosts() {

        List<MonitoringException> errors = new ArrayList<MonitoringException>();

        // initialize the monitoring processes
        Iterator<String> monitoredHostsIterator = monitoredHosts.iterator();
        while( monitoredHostsIterator.hasNext() ) {
            String monitoredHost = monitoredHostsIterator.next();

            log.debug( "Initializing system monitoring on " + monitoredHost );
            final String ERR_MSG = "Could not initialize monitoring " + monitoredHost + ". ";

            Set<FullReadingBean> readings = requestedReadingTypesPerHosts.get( monitoredHost );
            if( readings == null || readings.size() == 0 ) {
                errors.add( new MonitoringException( ERR_MSG + " as no monitor types are provided" ) );
                break;
            }

            try {
                initializeSystemMonitoringProcess( monitoredHost, readings );
            } catch( MonitoringException e ) {
                errors.add( e );
                break;
            }

            log.info( "Successfully initialized monitoring " + monitoredHost );
        }

        // run the monitoring processes
        monitoredHostsIterator = monitoredHosts.iterator();
        while( monitoredHostsIterator.hasNext() ) {
            String monitoredHost = monitoredHostsIterator.next();
            log.debug( "Starting system monitoring on " + monitoredHost );

            try {
                startSystemMonitoringProcess( monitoredHost );
            } catch( MonitoringException e ) {
                errors.add( e );
                break;
            }

            // start the task which will log into the database at a scheduled interval
            log.debug( "Starting the logging task for the system monitoring on " + monitoredHost );
            AbstractLoggerTask loggerTask = new SystemStatsLoggerTask( monitoredHost );
            ScheduledFuture<?> loggerTaskFuture = scheduler.scheduleAtFixedRate( loggerTask, loggingInterval,
                                                                                 loggingInterval,
                                                                                 TimeUnit.SECONDS );
            //put the task in the map
            loggerTasksPerHost.put( monitoredHost, loggerTaskFuture );

            log.info( "Successfully started monitoring " + monitoredHost
                      + ". Monitoring results will be collected on every " + loggingInterval + " seconds" );
        }

        return errors;
    }

    private List<MonitoringException> startMonitoringAgent() {

        List<MonitoringException> errors = new ArrayList<MonitoringException>();

        int numberAgents = 0;

        // iterate the monitored hosts
        Iterator<String> monitoredAgentsIterator = monitoredAgents.iterator();
        while( monitoredAgentsIterator.hasNext() ) {
            numberAgents++;
            String monitoredAgent = monitoredAgentsIterator.next();
            log.debug( "Starting ATS Agent monitoring on " + monitoredAgent );

            try {
                new AgentMonitoringClient( monitoredAgent ).startMonitoring( startTimestamp, pollInterval );
            } catch( AgentException e ) {
                errors.add( new MonitoringException( "Could not start monitoring ATS Agent " + monitoredAgent,
                                                     e ) );
            }
        }

        // Start the task which will log into the database at a scheduled interval
        // Note that we use just 1 task for monitoring all agents
        if( numberAgents > 0 && errors.size() == 0 ) {
            log.debug( "Starting the logging task for the agent monitoring" );
            lastUserActivityLoggerTask = new UserActivityLoggerTask( monitoredAgents );
            ScheduledFuture<?> loggerTaskFuture = scheduler.scheduleAtFixedRate( lastUserActivityLoggerTask,
                                                                                 loggingInterval,
                                                                                 loggingInterval,
                                                                                 TimeUnit.SECONDS );
            // put the task in the map
            loggerTasksPerHost.put( UserActivityLoggerTask.ATS_AGENT_HOSTS, loggerTaskFuture );

            log.info( "User activity on " + Arrays.toString( monitoredAgents.toArray() )
                      + " agent(s) will be monitored every " + loggingInterval + " seconds" );
        }

        return errors;
    }

    private boolean stopMonitoringPhysicalHosts( boolean getRemainingData ) {

        boolean successfulOperation = true;

        // cancel the logging tasks
        Iterator<String> loggerTasksIterator = monitoredHosts.iterator();
        while( loggerTasksIterator.hasNext() ) {
            String monitoredHost = loggerTasksIterator.next();

            log.debug( "Stopping the logging task for the system monitoring on " + monitoredHost );
            ScheduledFuture<?> loggerTaskFuture = loggerTasksPerHost.get( monitoredHost );
            if( loggerTaskFuture != null ) {
                // the loggerTaskFuture is null when we scheduled to monitor this host but got
                // error when tried to start the monitoring process

                if( loggerTaskFuture.isCancelled() ) {
                    throw new MonitoringException( "Logging task for the system monitoring process on "
                                                   + monitoredHost + " has been cancelled" );
                }
                loggerTaskFuture.cancel( false );

                try {
                    // log any remaining results by explicitly calling the task to get the results
                    if( getRemainingData ) {
                        log.debug( "Get any remaining monitoring results for " + monitoredHost );
                        AbstractLoggerTask loggerTask = new SystemStatsLoggerTask( monitoredHost );
                        loggerTask.run();
                    }
                } catch( Exception e ) {
                    successfulOperation = false;
                    log.error( "Error getting final monitoring results for " + monitoredHost, e );
                }
            }
        }

        // stop the monitoring process
        Iterator<String> monitoredHostsIterator = monitoredHosts.iterator();
        while( monitoredHostsIterator.hasNext() ) {
            String monitoredHost = monitoredHostsIterator.next();
            try {
                stopSystemMonitoringProcess( monitoredHost );
            } catch( MonitoringException e ) {
                successfulOperation = false;
                log.error( "Could not stop monitoring " + monitoredHost, e );
            }
        }

        return successfulOperation;
    }

    private boolean stopMonitoringAgents( boolean getRemainingData ) {

        boolean successfulOperation = true;

        String monitoredAgentsString = Arrays.toString( monitoredAgents.toArray() );

        // cancel the logging task
        ScheduledFuture<?> loggerTaskFuture = loggerTasksPerHost.get( UserActivityLoggerTask.ATS_AGENT_HOSTS );
        if( loggerTaskFuture != null ) {
            log.debug( "Stopping the logging task for monitoring " + monitoredAgentsString
                       + " ATS agent(s) " );
            if( loggerTaskFuture.isCancelled() ) {
                throw new MonitoringException( "Logging task for monitoring " + monitoredAgentsString
                                               + " ATS agent(s) has been cancelled" );
            }
            loggerTaskFuture.cancel( false );

            try {
                // log any remaining results by explicitly calling the task to get the results
                if( getRemainingData ) {
                    AbstractLoggerTask loggerTask = new UserActivityLoggerTask( monitoredAgents,
                                                                                lastUserActivityLoggerTask.getcollectTimesPerLoader() );
                    loggerTask.run();
                }
            } catch( Exception e ) {
                successfulOperation = false;
                log.error( "Error getting final monitoring results for " + monitoredAgentsString
                           + " ATS agent(s)", e );
            }

            // Stop the monitoring process on all agents
            Iterator<String> monitoredAgentsIterator = monitoredAgents.iterator();
            while( monitoredAgentsIterator.hasNext() ) {
                String monitoredAgent = monitoredAgentsIterator.next();
                try {
                    stopMonitoringProcessOnAgent( monitoredAgent );
                } catch( MonitoringException e ) {
                    successfulOperation = false;
                    log.error( e );
                }
            }
        }
        return successfulOperation;
    }

    private void checkTimeIntervals( int collectInterval, int loggingInterval ) {

        if( collectInterval < 1 ) {
            throw new MonitoringException( "The interval for collecting statistical data must be at least 1 second. You have specified "
                                           + collectInterval + " seconds" );
        }

        if( loggingInterval < 10 ) {
            throw new MonitoringException( "The interval for moving the statistical data to the logging server must be at least 10 seconds. You have specified "
                                           + loggingInterval + " seconds" );
        }
    }

    private void initializeSystemMonitoringProcess( String monitoredHost,
                                                    Set<FullReadingBean> readings ) throws MonitoringException {

        log.debug( "Initializing the system monitoring process on " + monitoredHost );
        try {
            InternalSystemMonitoringOperations sysMonitoringActions = new InternalSystemMonitoringOperations( monitoredHost );
            sysMonitoringActions.initializeMonitoring( new ArrayList<FullReadingBean>( readings ),
                                                       startTimestamp, pollInterval );
        } catch( AgentException e ) {
            throw new MonitoringException( "Could not start the system monitoring process on " + monitoredHost
                                           + ". For more details check loader logs on that machine", e );
        }
    }

    private void startSystemMonitoringProcess( String monitoredHost ) throws MonitoringException {

        log.debug( "Starting the system monitoring process on " + monitoredHost );
        try {
            InternalSystemMonitoringOperations sysMonitoringActions = new InternalSystemMonitoringOperations( monitoredHost );
            sysMonitoringActions.startMonitoring();
        } catch( AgentException e ) {
            throw new MonitoringException( "Could not start the system monitoring process on "
                                           + monitoredHost, e );
        }
    }

    private void stopSystemMonitoringProcess( String monitoredHost ) throws MonitoringException {

        log.debug( "Stopping system monitoring on " + monitoredHost );
        try {
            InternalSystemMonitoringOperations sysMonitoringActions = new InternalSystemMonitoringOperations( monitoredHost );
            sysMonitoringActions.stopMonitoring();
            log.debug( "Successfully stopped system monitoring on " + monitoredHost );
        } catch( AgentException e ) {
            throw new MonitoringException( "Could not stop the system monitoring process on " + monitoredHost,
                                           e );
        }
    }

    private void stopMonitoringProcessOnAgent( String monitoredAgent ) throws MonitoringException {

        try {
            log.debug( "Stopping system monitoring on " + monitoredAgent + " agent" );
            new AgentMonitoringClient( monitoredAgent ).stopMonitoring();
            log.debug( "Successfully stopped monitoring " + monitoredAgent + " agent" );
        } catch( AgentException e ) {
            throw new MonitoringException( "Could not stop monitoring " + monitoredAgent + " agent", e );
        }
    }
}
