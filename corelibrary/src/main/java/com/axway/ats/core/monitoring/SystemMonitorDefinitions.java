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
package com.axway.ats.core.monitoring;

import java.util.HashSet;
import java.util.Set;

public class SystemMonitorDefinitions {

    // The name of the Agent component we use for monitoring a physical system
    // It must be the same as in the corresponding agent_descriptor.xml file
    public static final String       ATS_SYSTEM_MONITORING_COMPONENT_NAME               = "auto-system-monitoring";

    // CPU related statistics
    private static final Set<String> ALL_CPU_READINGS;
    public static final String       READING_CPU_LOAD__LAST_MINUTE                      = "CPU average load - Last minute";
    public static final String       READING_CPU_LOAD__LAST_5_MINUTES                   = "CPU average load - Last 5 minutes";
    public static final String       READING_CPU_LOAD__LAST_15_MINUTES                  = "CPU average load - Last 15 minutes";

    public static final String       READING_CPU_USAGE__WAIT                            = "CPU usage - Wait";
    public static final String       READING_CPU_USAGE__KERNEL                          = "CPU usage - Kernel";
    public static final String       READING_CPU_USAGE__USER                            = "CPU usage - User";
    public static final String       READING_CPU_USAGE__TOTAL                           = "CPU usage - Total";

    // MEMORY related statistics
    private static final Set<String> ALL_MEMORY_READINGS;
    public static final String       READING_MEMORY__ACTUAL_USED                        = "Memory - Actual Used";
    public static final String       READING_MEMORY__ACTUAL_FREE                        = "Memory - Actual Free";
    public static final String       READING_MEMORY__USED                               = "Memory - Used";
    public static final String       READING_MEMORY__FREE                               = "Memory - Free";

    // VIRTUAL MEMORY related statistics
    private static final Set<String> ALL_VIRTUAL_MEMORY_READINGS;
    public static final String       READING_VIRTUAL_MEMORY__TOTAL                      = "Virtual memory - Total";
    public static final String       READING_VIRTUAL_MEMORY__USED                       = "Virtual memory - Used";
    public static final String       READING_VIRTUAL_MEMORY__FREE                       = "Virtual memory - Free";
    public static final String       READING_VIRTUAL_MEMORY__PAGES_IN                   = "Virtual memory - Pages in";
    public static final String       READING_VIRTUAL_MEMORY__PAGES_OUT                  = "Virtual memory - Pages out";

    // IO related statistics
    private static final Set<String> ALL_IO_READINGS;
    public static final String       READING_IO__READ_BYTES_ALL_DEVICES                 = "IO Read bytes - All local devices";
    public static final String       READING_IO__WRITE_BYTES_ALL_DEVICES                = "IO Write bytes - All local devices";

    // Network traffic
    private static final Set<String> ALL_NETWORK_INTERFACES_READINGS;
    public static final String       READING_NETWORK_TRAFFIC                            = "NIC";

    // Netstat
    private static final Set<String> ALL_NETSTAT_READINGS;
    public static final String       READING_NETSTAT__ACTIVE_CONNECTION_OPENINGS        = "[Netstat] Active connection openings";
    public static final String       READING_NETSTAT__PASSIVE_CONNECTION_OPENINGS       = "[Netstat] Passive connection openings";
    public static final String       READING_NETSTAT__FAILED_CONNECTION_ATTEMPTS        = "[Netstat] Failed connection attempts";
    public static final String       READING_NETSTAT__RESET_CONNECTIONS                 = "[Netstat] Reset connections ?";
    public static final String       READING_NETSTAT__CURRENT_CONNECTIONS               = "[Netstat] Current connections";
    public static final String       READING_NETSTAT__SEGMENTS_RECEIVED                 = "[Netstat] Segments received";
    public static final String       READING_NETSTAT__SEGMENTS_SENT                     = "[Netstat] Segments sent";
    public static final String       READING_NETSTAT__SEGMENTS_RETRANSMITTED            = "[Netstat] Segments retransmitted";
    public static final String       READING_NETSTAT__OUT_RESETS                        = "[Netstat] Out resets ?";
    public static final String       READING_NETSTAT__IN_ERRORS                         = "[Netstat] In errors ?";

    // TCP
    private static final Set<String> ALL_TCP_READINGS;
    public static final String       READING_TCP__CLOSE                                 = "[TCP] Close";
    public static final String       READING_TCP__LISTEN                                = "[TCP] Listen";
    public static final String       READING_TCP__SYN_SENT                              = "[TCP] SYN sent";
    public static final String       READING_TCP__SYN_RECEIVED                          = "[TCP] SYN received";
    public static final String       READING_TCP__ESTABLISHED                           = "[TCP] Established";
    public static final String       READING_TCP__CLOSE_WAIT                            = "[TCP] Close wait";
    public static final String       READING_TCP__LAST_ACK                              = "[TCP] Last ACK";
    public static final String       READING_TCP__FIN_WAIT1                             = "[TCP] FIN wait 1";
    public static final String       READING_TCP__FIN_WAIT2                             = "[TCP] FIN wait 2";
    public static final String       READING_TCP__CLOSING                               = "[TCP] Closing";
    public static final String       READING_TCP__TIME_WAIT                             = "[TCP] Time wait";
    public static final String       READING_TCP__BOUND                                 = "[TCP] Bound";
    public static final String       READING_TCP__IDLE                                  = "[TCP] Idle";
    public static final String       READING_TCP__TOTAL_INBOUND                         = "[TCP] Total inbound";
    public static final String       READING_TCP__TOTAL_OUTBOUND                        = "[TCP] Total outbound";

    // System process - CPU related statistics
    private static final Set<String> ALL_PROCESS_CPU_READINGS;
    public static final String       READING_PROCESS_CPU__USAGE_KERNEL                  = "Process CPU usage - Kernel";
    public static final String       READING_PROCESS_CPU__USAGE_USER                    = "Process CPU usage - User";
    public static final String       READING_PROCESS_CPU__USAGE_TOTAL                   = "Process CPU usage - Total";

    // System process - MEMORY related statistics
    private static final Set<String> ALL_PROCESS_MEMORY_READINGS;
    public static final String       READING_PROCESS_MEMORY__VIRTUAL                    = "Process Memory - Virtual";
    public static final String       READING_PROCESS_MEMORY__RESIDENT                   = "Process Memory - Resident";
    public static final String       READING_PROCESS_MEMORY__SHARED                     = "Process Memory - Shared";
    public static final String       READING_PROCESS_MEMORY__PAGE_FAULTS                = "Process Memory - Page faults";

    // ATS Agent
    public static final String       ATS_AGENT__USER_ACTIVITY                           = "User activity";

    // Keys for process parameters
    public static final String       PARAMETER_NAME__PROCESS_PARENT_NAME                = "PARAMETER_NAME__PROCESS_PARENT_NAME";
    public static final String       PARAMETER_NAME__PROCESS_INTERNAL_NAME              = "PARAMETER_NAME__PROCESS_INTERNAL_NAME";
    public static final String       PARAMETER_NAME__PROCESS_RECOGNITION_PATTERN        = "PARAMETER_NAME__PROCESS_RECOGNITION_PATTERN";
    public static final String       PARAMETER_NAME__PROCESS_ALIAS                      = "PARAMETER_NAME__PROCESS_ALIAS";
    public static final String       PARAMETER_NAME__PROCESS_USERNAME                   = "PARAMETER_NAME__PROCESS_USERNAME";
    public static final String       PARAMETER_NAME__PROCESS_START_COMMAND              = "PARAMETER_NAME__PROCESS_START_COMMAND";
    public static final String       PARAMETER_NAME__PROCESS_READING_ID                 = "PARAMETER_NAME__PROCESS_READING_ID";

    public static final String       PARAMETER_NAME__CUSTOM_MESSAGE                     = "PARAMETER_NAME__CUSTOM_MESSAGE";

    // JVM related statistics
    public static final String       READING_JVM__CPU_USAGE                             = "[JVM] CPU usage";
    public static final String       READING_JVM__MEMORY_HEAP                           = "[JVM] Memory Heap";
    public static final String       READING_JVM__MEMORY_HEAP_YOUNG_GENERATION_EDEN     = "[JVM] Memory Heap - Young Generation - Eden";
    public static final String       READING_JVM__MEMORY_HEAP_YOUNG_GENERATION_SURVIVOR = "[JVM] Memory Heap - Young Generation - Survivor";
    public static final String       READING_JVM__MEMORY_HEAP_OLD_GENERATION            = "[JVM] Memory Heap - Old Generation";
    public static final String       READING_JVM__MEMORY_PERMANENT_GENERATION           = "[JVM] Memory - Permanent Generation";
    public static final String       READING_JVM__MEMORY_CODE_CACHE                     = "[JVM] Memory - Code Cache";
    public static final String       READING_JVM__CLASSES_COUNT                         = "[JVM] Loaded classes";
    public static final String       READING_JVM__THREADS_COUNT                         = "[JVM] Threads";
    public static final String       READING_JVM__THREADS_DAEMON_COUNT                  = "[JVM] Daemon threads";

    static {
        ALL_CPU_READINGS = new HashSet<String>();
        ALL_CPU_READINGS.add( READING_CPU_LOAD__LAST_MINUTE );
        ALL_CPU_READINGS.add( READING_CPU_LOAD__LAST_5_MINUTES );
        ALL_CPU_READINGS.add( READING_CPU_LOAD__LAST_15_MINUTES );
        ALL_CPU_READINGS.add( READING_CPU_USAGE__WAIT );
        ALL_CPU_READINGS.add( READING_CPU_USAGE__KERNEL );
        ALL_CPU_READINGS.add( READING_CPU_USAGE__USER );
        ALL_CPU_READINGS.add( READING_CPU_USAGE__TOTAL );

        ALL_MEMORY_READINGS = new HashSet<String>();
        ALL_MEMORY_READINGS.add( READING_MEMORY__ACTUAL_USED );
        ALL_MEMORY_READINGS.add( READING_MEMORY__ACTUAL_FREE );
        ALL_MEMORY_READINGS.add( READING_MEMORY__USED );
        ALL_MEMORY_READINGS.add( READING_MEMORY__FREE );

        ALL_VIRTUAL_MEMORY_READINGS = new HashSet<String>();
        ALL_VIRTUAL_MEMORY_READINGS.add( READING_VIRTUAL_MEMORY__TOTAL );
        ALL_VIRTUAL_MEMORY_READINGS.add( READING_VIRTUAL_MEMORY__USED );
        ALL_VIRTUAL_MEMORY_READINGS.add( READING_VIRTUAL_MEMORY__FREE );
        ALL_VIRTUAL_MEMORY_READINGS.add( READING_VIRTUAL_MEMORY__PAGES_IN );
        ALL_VIRTUAL_MEMORY_READINGS.add( READING_VIRTUAL_MEMORY__PAGES_OUT );

        ALL_IO_READINGS = new HashSet<String>();
        ALL_IO_READINGS.add( READING_IO__READ_BYTES_ALL_DEVICES );
        ALL_IO_READINGS.add( READING_IO__WRITE_BYTES_ALL_DEVICES );

        ALL_NETWORK_INTERFACES_READINGS = new HashSet<String>();
        ALL_NETWORK_INTERFACES_READINGS.add( READING_NETWORK_TRAFFIC );

        ALL_NETSTAT_READINGS = new HashSet<String>();
        ALL_NETSTAT_READINGS.add( READING_NETSTAT__ACTIVE_CONNECTION_OPENINGS );
        ALL_NETSTAT_READINGS.add( READING_NETSTAT__PASSIVE_CONNECTION_OPENINGS );
        ALL_NETSTAT_READINGS.add( READING_NETSTAT__FAILED_CONNECTION_ATTEMPTS );
        ALL_NETSTAT_READINGS.add( READING_NETSTAT__RESET_CONNECTIONS );
        ALL_NETSTAT_READINGS.add( READING_NETSTAT__CURRENT_CONNECTIONS );
        ALL_NETSTAT_READINGS.add( READING_NETSTAT__SEGMENTS_RECEIVED );
        ALL_NETSTAT_READINGS.add( READING_NETSTAT__SEGMENTS_SENT );
        ALL_NETSTAT_READINGS.add( READING_NETSTAT__SEGMENTS_RETRANSMITTED );
        ALL_NETSTAT_READINGS.add( READING_NETSTAT__OUT_RESETS );
        ALL_NETSTAT_READINGS.add( READING_NETSTAT__IN_ERRORS );

        ALL_TCP_READINGS = new HashSet<String>();
        ALL_TCP_READINGS.add( READING_TCP__CLOSE );
        ALL_TCP_READINGS.add( READING_TCP__LISTEN );
        ALL_TCP_READINGS.add( READING_TCP__SYN_SENT );
        ALL_TCP_READINGS.add( READING_TCP__SYN_RECEIVED );
        ALL_TCP_READINGS.add( READING_TCP__ESTABLISHED );
        ALL_TCP_READINGS.add( READING_TCP__CLOSE_WAIT );
        ALL_TCP_READINGS.add( READING_TCP__LAST_ACK );
        ALL_TCP_READINGS.add( READING_TCP__FIN_WAIT1 );
        ALL_TCP_READINGS.add( READING_TCP__FIN_WAIT2 );
        ALL_TCP_READINGS.add( READING_TCP__CLOSING );
        ALL_TCP_READINGS.add( READING_TCP__TIME_WAIT );
        ALL_TCP_READINGS.add( READING_TCP__BOUND );
        ALL_TCP_READINGS.add( READING_TCP__IDLE );
        ALL_TCP_READINGS.add( READING_TCP__TOTAL_INBOUND );
        ALL_TCP_READINGS.add( READING_TCP__TOTAL_OUTBOUND );

        ALL_PROCESS_CPU_READINGS = new HashSet<String>();
        ALL_PROCESS_CPU_READINGS.add( READING_PROCESS_CPU__USAGE_USER );
        ALL_PROCESS_CPU_READINGS.add( READING_PROCESS_CPU__USAGE_KERNEL );
        ALL_PROCESS_CPU_READINGS.add( READING_PROCESS_CPU__USAGE_TOTAL );

        ALL_PROCESS_MEMORY_READINGS = new HashSet<String>();
        ALL_PROCESS_MEMORY_READINGS.add( READING_PROCESS_MEMORY__VIRTUAL );
        ALL_PROCESS_MEMORY_READINGS.add( READING_PROCESS_MEMORY__RESIDENT );
        ALL_PROCESS_MEMORY_READINGS.add( READING_PROCESS_MEMORY__SHARED );
        ALL_PROCESS_MEMORY_READINGS.add( READING_PROCESS_MEMORY__PAGE_FAULTS );
    }

    public static Set<String> getAllCpuReadings() {

        return ALL_CPU_READINGS;
    }

    public static Set<String> getAllMemoryReadings() {

        return ALL_MEMORY_READINGS;
    }

    public static Set<String> getAllVirtualMemoryReadings() {

        return ALL_VIRTUAL_MEMORY_READINGS;
    }

    public static Set<String> getAllIOReadings() {

        return ALL_IO_READINGS;
    }

    public static Set<String> getAllNetworkInterfacesReadings() {

        return ALL_NETWORK_INTERFACES_READINGS;
    }

    public static Set<String> getAllNetstatReadings() {

        return ALL_NETSTAT_READINGS;
    }

    public static Set<String> getAllTcpReadings() {

        return ALL_TCP_READINGS;
    }

    public static Set<String> getAllProcessCpuReadings() {

        return ALL_PROCESS_CPU_READINGS;
    }

    public static Set<String> getAllProcessMemoryReadings() {

        return ALL_PROCESS_MEMORY_READINGS;
    }

    public static boolean isProcessReading( String readingName ) {

        return ( readingName.equalsIgnoreCase( READING_PROCESS_CPU__USAGE_USER )
                 || readingName.equalsIgnoreCase( READING_PROCESS_CPU__USAGE_KERNEL )
                 || readingName.equalsIgnoreCase( READING_PROCESS_CPU__USAGE_TOTAL )

                 || readingName.equalsIgnoreCase( READING_PROCESS_MEMORY__VIRTUAL )
                 || readingName.equalsIgnoreCase( READING_PROCESS_MEMORY__RESIDENT )
                 || readingName.equalsIgnoreCase( READING_PROCESS_MEMORY__SHARED )
                 || readingName.equalsIgnoreCase( READING_PROCESS_MEMORY__PAGE_FAULTS ) );
    }
}
