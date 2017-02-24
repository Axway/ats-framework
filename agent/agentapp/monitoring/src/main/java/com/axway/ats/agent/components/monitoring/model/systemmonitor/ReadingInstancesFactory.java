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
package com.axway.ats.agent.components.monitoring.model.systemmonitor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.axway.ats.agent.components.monitoring.model.exceptions.UnsupportedReadingException;
import com.axway.ats.common.performance.monitor.beans.FullReadingBean;
import com.axway.ats.common.performance.monitor.beans.ParentProcessReadingBean;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.core.monitoring.SystemMonitorDefinitions;
import com.axway.ats.core.utils.StringUtils;

/**
 * Factory for creating instances of all needed readings
 */
public class ReadingInstancesFactory {

    private static Logger                                 log          = Logger.getLogger( ReadingInstancesFactory.class );

    private static final boolean                          IS_WINDOWS   = OperatingSystemType.getCurrentOsType()
                                                                                            .isWindows();

    private static int                                    numberOfCPUs = 0;

    // Map<USER REGEX, Map<Process ID, MatchedProcess>>
    private static Map<String, Map<Long, MatchedProcess>> matchedProcessesMap;
    // list of matched process IDs
    private static Set<Long>                              matchedProcessesIds;
    private static Set<String>                            processesReadingInstanceIdentifiers;

    // Map<Regex to match a process, number of matched processes for this regex>
    //
    // We need to remember the last used index for each process.
    // For example if we have 2 processes, we will use index 1 and 2,
    // if the second process die and some time later another process match the same pattern,
    // we must not assign again index 2, it must be index 3
    private static Map<String, Integer>                   matchedProcessesIndexes;

    public static void init( SigarWrapper sigarWrapper,
                             int pollingInterval ) throws UnsupportedReadingException, SigarException {

        /*
         * We need to know the number of CPUs for some readings in some cases. For example
         *      1. Sigar gives average CPU usage in percents.
         *      2. Sigar gives accumulated CPU usage times.
         *      If we have 2 CPUs loaded on 20% and 40%, we will get 30% and 0.60 time usage for 1 second.
         *      We fix that to get get 30% and 0.30 time usage for 1 second.
         */
        getNumberOfCPUs( sigarWrapper );

        // clean up the processes map
        matchedProcessesMap = new HashMap<String, Map<Long, MatchedProcess>>();
        matchedProcessesIds = new HashSet<Long>();
        matchedProcessesIndexes = new HashMap<String, Integer>();
        processesReadingInstanceIdentifiers = new HashSet<String>();

        // On Solaris it takes a significant amount of time (around 1 minute) until Sigar iterates the
        // system processes for first time. We do it here, so the real polls are quick
        Sigar sigar = sigarWrapper.getSigarInstance();
        for( long pid : sigar.getProcList() ) {
            try {
                sigar.getProcArgs( pid );
            } catch( SigarException e ) {
                // some system processes can not be accessed
            }
        }
    }

    public static List<ReadingInstance> createStaticReadingInstances( SigarWrapper sigarWrapper,
                                                                      List<FullReadingBean> readings ) throws UnsupportedReadingException,
                                                                                                       SigarException {

        List<ReadingInstance> readingInstances = new ArrayList<ReadingInstance>();

        // ADD SYSTEM READINGS
        for( FullReadingBean reading : readings ) {

            String readingName = reading.getName();

            ReadingInstance readingInstance = null;
            if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_VIRTUAL_MEMORY__TOTAL ) ) {
                readingInstance = getSwapTotal( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_VIRTUAL_MEMORY__USED ) ) {
                readingInstance = getSwapUsed( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_VIRTUAL_MEMORY__FREE ) ) {
                readingInstance = getSwapFree( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_VIRTUAL_MEMORY__PAGES_IN ) ) {
                readingInstance = getSwapPagesIn( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_VIRTUAL_MEMORY__PAGES_OUT ) ) {
                readingInstance = getSwapPagesOut( sigarWrapper, reading );
            }

            else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_MEMORY__USED ) ) {
                readingInstance = getMemoryUsed( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_MEMORY__FREE ) ) {
                readingInstance = getMemoryFree( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_MEMORY__ACTUAL_USED ) ) {
                readingInstance = getMemoryActualUsed( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_MEMORY__ACTUAL_FREE ) ) {
                readingInstance = getMemoryActualFree( sigarWrapper, reading );
            }

            else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_IO__READ_BYTES_ALL_DEVICES ) ) {
                readingInstance = getReadBytesAllLocalDevices( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_IO__WRITE_BYTES_ALL_DEVICES ) ) {
                readingInstance = getWriteBytesAllLocalDevices( sigarWrapper, reading );
            }

            else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_CPU_LOAD__LAST_MINUTE ) ) {
                if( !IS_WINDOWS ) {
                    readingInstance = getLoadAverage1minute( sigarWrapper, reading );
                }
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_CPU_LOAD__LAST_5_MINUTES ) ) {
                if( !IS_WINDOWS ) {
                    readingInstance = getLoadAverage5minutes( sigarWrapper, reading );
                }
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_CPU_LOAD__LAST_15_MINUTES ) ) {
                if( !IS_WINDOWS ) {
                    readingInstance = getLoadAverage15minutes( sigarWrapper, reading );
                }
            }

            else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_CPU_USAGE__TOTAL ) ) {
                readingInstance = getCpuUsageTotal( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_CPU_USAGE__WAIT ) ) {
                readingInstance = getCpuUsageWaitingForIO( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_CPU_USAGE__KERNEL ) ) {
                readingInstance = getCpuUsageRunningKernelCode( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_CPU_USAGE__USER ) ) {
                readingInstance = getCpuUsageRunningUserCode( sigarWrapper, reading );
            }

            else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_NETWORK_TRAFFIC ) ) {
                List<ReadingInstance> readingsList = getNetworkTraffic( sigarWrapper, reading );
                readingInstances.addAll( readingsList );
            }

            else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_NETSTAT__ACTIVE_CONNECTION_OPENINGS ) ) {
                readingInstance = getNetstatActiveConnectionOpenings( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_NETSTAT__PASSIVE_CONNECTION_OPENINGS ) ) {
                readingInstance = getNetstatPassiveConnectionOpenings( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_NETSTAT__FAILED_CONNECTION_ATTEMPTS ) ) {
                readingInstance = getNetstatFailedConnectionAttemtps( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_NETSTAT__RESET_CONNECTIONS ) ) {
                readingInstance = getNetstatResetConnections( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_NETSTAT__CURRENT_CONNECTIONS ) ) {
                readingInstance = getNetstatCurrentConnections( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_NETSTAT__SEGMENTS_RECEIVED ) ) {
                readingInstance = getNetstatSegmentsReceived( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_NETSTAT__SEGMENTS_SENT ) ) {
                readingInstance = getNetstatSegmentsSent( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_NETSTAT__SEGMENTS_RETRANSMITTED ) ) {
                readingInstance = getNetstatSegmentsRetransmitter( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_NETSTAT__OUT_RESETS ) ) {
                readingInstance = getNetstatOutResets( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_NETSTAT__IN_ERRORS ) ) {
                readingInstance = getNetstatInErrors( sigarWrapper, reading );
            }

            else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_TCP__CLOSE ) ) {
                readingInstance = getTcpClose( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_TCP__LISTEN ) ) {
                readingInstance = getTcpListen( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_TCP__SYN_SENT ) ) {
                readingInstance = getTcpSynSent( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_TCP__SYN_RECEIVED ) ) {
                readingInstance = getTcpSynReceived( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_TCP__ESTABLISHED ) ) {
                readingInstance = getTcpEstablished( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_TCP__CLOSE_WAIT ) ) {
                readingInstance = getTcpCloseWait( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_TCP__LAST_ACK ) ) {
                readingInstance = getTcpLastAck( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_TCP__FIN_WAIT1 ) ) {
                readingInstance = getTcpFinWait1( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_TCP__FIN_WAIT2 ) ) {
                readingInstance = getTcpFinWait2( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_TCP__CLOSING ) ) {
                readingInstance = getTcpClosing( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_TCP__TIME_WAIT ) ) {
                readingInstance = getTcpTimeWait( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_TCP__BOUND ) ) {
                readingInstance = getTcpBound( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_TCP__IDLE ) ) {
                readingInstance = getTcpIdle( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_TCP__TOTAL_INBOUND ) ) {
                readingInstance = getTcpTotalInbound( sigarWrapper, reading );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_TCP__TOTAL_OUTBOUND ) ) {
                readingInstance = getTcpTotalOutbound( sigarWrapper, reading );
            }

            else {
                throw new UnsupportedReadingException( readingName );
            }

            if( readingInstance != null ) {
                readingInstances.add( readingInstance );
            }
        }
        return readingInstances;
    }

    public static List<ReadingInstance> createOrUpdateDynamicReadingInstances( SigarWrapper sigarWrapper,
                                                                               Map<String, ParentProcessReadingBean> parentProcessReadingInstances,
                                                                               List<FullReadingBean> initialReadings,
                                                                               List<ReadingInstance> currentReadingInstances ) throws UnsupportedReadingException,
                                                                                                                               SigarException {

        // update the list of matching processes now, this must be done as quickly as possible
        // as it happens prior to each polling
        currentReadingInstances = updateProcessesMatchingMap( sigarWrapper.getSigarInstance(),
                                                              initialReadings, currentReadingInstances );

        List<ReadingInstance> readingInstances = new ArrayList<ReadingInstance>( currentReadingInstances );

        for( FullReadingBean reading : initialReadings ) {
            // the list of matching processes is ready, now create the reading instances
            String readingName = reading.getName();

            ParentProcessReadingBean parentProcess = null;
            String parentProcessName = reading.getParameter( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_PARENT_NAME );
            if( parentProcessName != null ) {
                parentProcess = parentProcessReadingInstances.get( parentProcessName + "-"
                                                                   + reading.getName() );
            }
            /*
             * the process monitoring methods are adding a reading identifier to the parameters
             * like "reading=1", "reading=2" etc.
             * These values are not important, but they must be unique as they are later used by
             * the Test Explorer to map same processes monitored on different machines.
             * Of course we can use a more descriptive values like "reading=CPU percent reading", but this will take
             * some space on the database, and we know these values are never seen by the user.
             */
            if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_PROCESS_CPU__USAGE_USER ) ) {
                List<ReadingInstance> readingsList = getProcessCpuUsageRunningUser( sigarWrapper, reading,
                                                                                    parentProcess );
                readingInstances.addAll( readingsList );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_PROCESS_CPU__USAGE_KERNEL ) ) {
                List<ReadingInstance> readingsList = getProcessCpuUsageRunningKernel( sigarWrapper, reading,
                                                                                      parentProcess );
                readingInstances.addAll( readingsList );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_PROCESS_CPU__USAGE_TOTAL ) ) {
                List<ReadingInstance> readingsList = getProcessCpuUsageRunningTotal( sigarWrapper, reading,
                                                                                     parentProcess );
                readingInstances.addAll( readingsList );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_PROCESS_MEMORY__VIRTUAL ) ) {
                List<ReadingInstance> readingsList = getProcessVirtualMemory( sigarWrapper, reading,
                                                                              parentProcess );
                readingInstances.addAll( readingsList );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_PROCESS_MEMORY__RESIDENT ) ) {
                List<ReadingInstance> readingsList = getProcessResidentMemory( sigarWrapper, reading,
                                                                               parentProcess );
                readingInstances.addAll( readingsList );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_PROCESS_MEMORY__SHARED )
                       && !IS_WINDOWS ) {
                List<ReadingInstance> readingsList = getProcessSharedMemory( sigarWrapper, reading,
                                                                             parentProcess );
                readingInstances.addAll( readingsList );
            } else if( readingName.equalsIgnoreCase( SystemMonitorDefinitions.READING_PROCESS_MEMORY__PAGE_FAULTS ) ) {
                List<ReadingInstance> readingsList = getProcessMemoryPageFaults( sigarWrapper, reading,
                                                                                 parentProcess );
                readingInstances.addAll( readingsList );
            } else {
                // We do nothing here as for example we do not support Shared Memory on windows
                // throw new UnsupportedReadingException( readingName );
            }
        }
        return readingInstances;
    }

    private static void getNumberOfCPUs( SigarWrapper sigarWrapper ) throws SigarException {

        if( numberOfCPUs == 0 ) {

            numberOfCPUs = sigarWrapper.getSigarInstance().getCpuInfoList()[0].getTotalCores();
            log.info( "Detected " + numberOfCPUs + " CPUs" );
        }
    }

    private static ReadingInstance getSwapTotal( SigarWrapper sigarWrapper,
                                                 FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 0 ) {
            private static final long serialVersionUID = 1L;

            @Override
            public void init() throws SigarException {

                applyMemoryNormalizationFactor();
            }

            @Override
            public float poll() {

                return ( fixLongValue( sigarWrapper.swap.getTotal() ) * normalizationFactor );
            }
        };
    }

    private static ReadingInstance getSwapUsed( SigarWrapper sigarWrapper,
                                                FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 0 ) {
            private static final long serialVersionUID = 1L;

            @Override
            public void init() throws SigarException {

                applyMemoryNormalizationFactor();
            }

            @Override
            public float poll() {

                return ( fixLongValue( sigarWrapper.swap.getUsed() ) * normalizationFactor );
            }
        };
    }

    private static ReadingInstance getSwapFree( SigarWrapper sigarWrapper,
                                                FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 0 ) {
            private static final long serialVersionUID = 1L;

            @Override
            public void init() throws SigarException {

                applyMemoryNormalizationFactor();
            }

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.swap.getFree() ) * normalizationFactor;
            }
        };
    }

    private static ReadingInstance getSwapPagesIn( SigarWrapper sigarWrapper,
                                                   FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.swap.getPageIn() );
            }
        };
    }

    private static ReadingInstance getSwapPagesOut( SigarWrapper sigarWrapper,
                                                    FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.swap.getPageOut() );
            }
        };
    }

    private static ReadingInstance getMemoryUsed( SigarWrapper sigarWrapper,
                                                  FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 0 ) {
            private static final long serialVersionUID = 1L;

            @Override
            public void init() throws SigarException {

                applyMemoryNormalizationFactor();
            }

            @Override
            public float poll() {

                long newValue = fixLongValue( sigarWrapper.memory.getUsed() );
                if( newValue >= 0 ) {
                    return newValue * normalizationFactor;
                } else {
                    return newValue;
                }
            }
        };
    }

    private static ReadingInstance getMemoryFree( SigarWrapper sigarWrapper,
                                                  FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 0 ) {
            private static final long serialVersionUID = 1L;

            @Override
            public void init() throws SigarException {

                applyMemoryNormalizationFactor();
            }

            @Override
            public float poll() {

                long newValue = fixLongValue( sigarWrapper.memory.getFree() );
                if( newValue >= 0 ) {
                    return newValue * normalizationFactor;
                } else {
                    return newValue;
                }
            }
        };
    }

    private static ReadingInstance getMemoryActualUsed( SigarWrapper sigarWrapper,
                                                        FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 0 ) {
            private static final long serialVersionUID = 1L;

            @Override
            public void init() throws SigarException {

                applyMemoryNormalizationFactor();
            }

            @Override
            public float poll() {

                long newValue = fixLongValue( sigarWrapper.memory.getActualUsed() );
                if( newValue >= 0 ) {
                    return newValue * normalizationFactor;
                } else {
                    return newValue;
                }
            }
        };
    }

    private static ReadingInstance getMemoryActualFree( SigarWrapper sigarWrapper,
                                                        FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 0 ) {
            private static final long serialVersionUID = 1L;

            @Override
            public void init() throws SigarException {

                applyMemoryNormalizationFactor();
            }

            @Override
            public float poll() {

                long newValue = fixLongValue( sigarWrapper.memory.getActualFree() );
                if( newValue >= 0 ) {
                    return newValue * normalizationFactor;
                } else {
                    return newValue;
                }
            }
        };
    }

    private static ReadingInstance getReadBytesAllLocalDevices( SigarWrapper sigarWrapper,
                                                                FullReadingBean reading ) throws SigarException {

        return IOReadingInstancesFactory.getReadBytesReadingInstance( sigarWrapper, reading );

    }

    private static ReadingInstance getWriteBytesAllLocalDevices( SigarWrapper sigarWrapper,
                                                                 FullReadingBean reading ) throws SigarException {

        return IOReadingInstancesFactory.getWriteBytesReadingInstance( sigarWrapper, reading );
    }

    private static ReadingInstance getLoadAverage1minute( SigarWrapper sigarWrapper,
                                                          FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                double dValue = fixDoubleValue( sigarWrapper.loadAvrg.getLastMinute() );

                // return a float with 2 digits after the decimal point
                return new BigDecimal( dValue ).setScale( 2, BigDecimal.ROUND_DOWN ).floatValue();
            }
        };
    }

    private static ReadingInstance getLoadAverage5minutes( SigarWrapper sigarWrapper,
                                                           FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                double dValue = fixDoubleValue( sigarWrapper.loadAvrg.getLastFiveMinutes() );

                // return a float with 2 digits after the decimal point
                return new BigDecimal( dValue ).setScale( 2, BigDecimal.ROUND_DOWN ).floatValue();
            }
        };
    }

    private static ReadingInstance getLoadAverage15minutes( SigarWrapper sigarWrapper,
                                                            FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                double dValue = fixDoubleValue( sigarWrapper.loadAvrg.getLast15Minutes() );

                // return a float with 2 digits after the decimal point
                return new BigDecimal( dValue ).setScale( 2, BigDecimal.ROUND_DOWN ).floatValue();
            }
        };
    }

    private static ReadingInstance getCpuUsageWaitingForIO( SigarWrapper sigarWrapper,
                                                            FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 100.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixDoubleValueInPercents( sigarWrapper.cpuPerc.getWait() );
            }
        };
    }

    private static ReadingInstance getCpuUsageRunningKernelCode( SigarWrapper sigarWrapper,
                                                                 FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 100.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixDoubleValueInPercents( sigarWrapper.cpuPerc.getSys() );
            }
        };
    }

    private static ReadingInstance getCpuUsageRunningUserCode( SigarWrapper sigarWrapper,
                                                               FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 100.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixDoubleValueInPercents( sigarWrapper.cpuPerc.getUser() );
            }
        };
    }

    private static ReadingInstance getCpuUsageTotal( SigarWrapper sigarWrapper,
                                                     FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 100.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixDoubleValueInPercents( sigarWrapper.cpuPerc.getSys()
                                                 + sigarWrapper.cpuPerc.getUser()
                                                 + sigarWrapper.cpuPerc.getWait() );
            }
        };
    }

    private static List<ReadingInstance> getNetworkTraffic( SigarWrapper sigarWrapper,
                                                            FullReadingBean reading ) throws SigarException {

        // this reading can actually be expanded in more than one reading
        int nextUniqueReadingId = 1;

        List<ReadingInstance> readingInstancesList = new ArrayList<ReadingInstance>();

        String[] ifNames = sigarWrapper.getSigarInstance().getNetInterfaceList();
        // if there are more than one IP addresses on a single interface, Sigar will show these interface names so many times
        Set<String> uniqueIfNames = new HashSet<String>( Arrays.asList( ifNames ) );
        for( final String ifName : uniqueIfNames ) {

            if( ifName.indexOf( ':' ) > -1 ) {

                // the interface is an alias of secondary IP for another interface e.g. 'eth0:1'
                // it has no TX and RX bytes, its traffic is calculated for its primary interface e.g. 'eth0'
                // that's why we will skip it, also Sigar throws an exception with message: No such device or address
                continue;
            }

            final long txBytes = sigarWrapper.getSigarInstance().getNetInterfaceStat( ifName ).getTxBytes();
            readingInstancesList.add( new ReadingInstance( sigarWrapper,
                                                           reading.getId() + ", " + ( nextUniqueReadingId++ ),
                                                           reading.getMonitorName(),
                                                           reading.getName() + " " + ifName + " TX data",
                                                           reading.getUnit(), 0 ) {
                private static final long serialVersionUID = 1L;

                @Override
                public void init() throws SigarException {

                    applyMemoryNormalizationFactor();

                    this.lastLongValue = fixLongValue( fixOverflow( ifName, txBytes ) );
                }

                @Override
                public float poll() throws SigarException {

                    NetInterfaceStat ifstat = this.sigarWrapper.getSigarInstance()
                                                               .getNetInterfaceStat( ifName );

                    long txBytes = fixLongValue( fixOverflow( getName(), ifstat.getTxBytes() ) );
                    double result;
                    if( txBytes >= 0 ) {
                        result = ( txBytes - this.lastLongValue ) * normalizationFactor;
                        this.lastLongValue = txBytes;
                    } else {
                        return -1.0F;
                    }

                    // calculate TX bytes per second
                    result = result / ( ( double ) getElapsedTime() / 1000 );
                    return new BigDecimal( result ).setScale( 2, BigDecimal.ROUND_DOWN ).floatValue();
                }
            } );

            final long rxBytes = sigarWrapper.getSigarInstance().getNetInterfaceStat( ifName ).getRxBytes();
            readingInstancesList.add( new ReadingInstance( sigarWrapper,
                                                           reading.getId() + ", " + ( nextUniqueReadingId++ ),
                                                           reading.getMonitorName(),
                                                           reading.getName() + " " + ifName + " RX data",
                                                           reading.getUnit(), 0 ) {
                private static final long serialVersionUID = 1L;

                @Override
                public void init() throws SigarException {

                    applyMemoryNormalizationFactor();

                    this.lastLongValue = fixLongValue( fixOverflow( ifName, rxBytes ) );
                }

                @Override
                public float poll() throws SigarException {

                    NetInterfaceStat ifstat = this.sigarWrapper.getSigarInstance()
                                                               .getNetInterfaceStat( ifName );
                    long rxBytes = fixLongValue( fixOverflow( getName(), ifstat.getRxBytes() ) );
                    double result;
                    if( rxBytes >= 0 ) {

                        result = ( rxBytes - this.lastLongValue ) * normalizationFactor;
                        this.lastLongValue = rxBytes;
                    } else {
                        return -1.0F;
                    }

                    // calculate RX bytes per second
                    result = result / ( ( double ) getElapsedTime() / 1000 );
                    return new BigDecimal( result ).setScale( 2, BigDecimal.ROUND_DOWN ).floatValue();
                }
            } );

        }

        return readingInstancesList;
    }

    private static ReadingInstance getNetstatActiveConnectionOpenings( SigarWrapper sigarWrapper,
                                                                       FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.tcp.getActiveOpens() );
            }
        };
    }

    private static ReadingInstance getNetstatPassiveConnectionOpenings( SigarWrapper sigarWrapper,
                                                                        FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.tcp.getPassiveOpens() );
            }
        };
    }

    private static ReadingInstance getNetstatFailedConnectionAttemtps( SigarWrapper sigarWrapper,
                                                                       FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.tcp.getAttemptFails() );
            }
        };
    }

    private static ReadingInstance getNetstatResetConnections( SigarWrapper sigarWrapper,
                                                               FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.tcp.getEstabResets() );
            }
        };
    }

    private static ReadingInstance getNetstatCurrentConnections( SigarWrapper sigarWrapper,
                                                                 FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.tcp.getCurrEstab() );
            }
        };
    }

    private static ReadingInstance getNetstatSegmentsReceived( SigarWrapper sigarWrapper,
                                                               FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.tcp.getInSegs() );
            }
        };
    }

    private static ReadingInstance getNetstatSegmentsSent( SigarWrapper sigarWrapper,
                                                           FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.tcp.getOutSegs() );
            }
        };
    }

    private static ReadingInstance getNetstatSegmentsRetransmitter( SigarWrapper sigarWrapper,
                                                                    FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.tcp.getRetransSegs() );
            }
        };
    }

    private static ReadingInstance getNetstatOutResets( SigarWrapper sigarWrapper,
                                                        FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.tcp.getOutRsts() );
            }
        };
    }

    private static ReadingInstance getNetstatInErrors( SigarWrapper sigarWrapper,
                                                       FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.tcp.getInErrs() );
            }
        };
    }

    private static ReadingInstance getTcpClose( SigarWrapper sigarWrapper,
                                                FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.netstat.getTcpClose() );
            }
        };
    }

    private static ReadingInstance getTcpListen( SigarWrapper sigarWrapper,
                                                 FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.netstat.getTcpListen() );
            }
        };
    }

    private static ReadingInstance getTcpSynSent( SigarWrapper sigarWrapper,
                                                  FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.netstat.getTcpSynSent() );
            }
        };
    }

    private static ReadingInstance getTcpSynReceived( SigarWrapper sigarWrapper,
                                                      FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.netstat.getTcpSynRecv() );
            }
        };
    }

    private static ReadingInstance getTcpEstablished( SigarWrapper sigarWrapper,
                                                      FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.netstat.getTcpEstablished() );
            }
        };
    }

    private static ReadingInstance getTcpCloseWait( SigarWrapper sigarWrapper,
                                                    FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.netstat.getTcpCloseWait() );
            }
        };
    }

    private static ReadingInstance getTcpLastAck( SigarWrapper sigarWrapper,
                                                  FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.netstat.getTcpLastAck() );
            }
        };
    }

    private static ReadingInstance getTcpFinWait1( SigarWrapper sigarWrapper,
                                                   FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.netstat.getTcpFinWait1() );
            }
        };
    }

    private static ReadingInstance getTcpFinWait2( SigarWrapper sigarWrapper,
                                                   FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.netstat.getTcpFinWait2() );
            }
        };
    }

    private static ReadingInstance getTcpClosing( SigarWrapper sigarWrapper,
                                                  FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.netstat.getTcpClosing() );
            }
        };
    }

    private static ReadingInstance getTcpTimeWait( SigarWrapper sigarWrapper,
                                                   FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.netstat.getTcpTimeWait() );
            }
        };
    }

    private static ReadingInstance getTcpBound( SigarWrapper sigarWrapper,
                                                FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.netstat.getTcpBound() );
            }
        };
    }

    private static ReadingInstance getTcpIdle( SigarWrapper sigarWrapper,
                                               FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.netstat.getTcpIdle() );
            }
        };
    }

    private static ReadingInstance getTcpTotalInbound( SigarWrapper sigarWrapper,
                                                       FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.netstat.getTcpInboundTotal() );
            }
        };
    }

    private static ReadingInstance getTcpTotalOutbound( SigarWrapper sigarWrapper,
                                                        FullReadingBean reading ) throws SigarException {

        return new ReadingInstance( sigarWrapper, reading.getId(), reading.getMonitorName(),
                                    reading.getName(), reading.getUnit(), 1.0F ) {
            private static final long serialVersionUID = 1L;

            @Override
            public float poll() {

                return fixLongValue( sigarWrapper.netstat.getTcpOutboundTotal() );
            }
        };
    }

    private static List<ReadingInstance> getProcessCpuUsageRunningUser( SigarWrapper sigarWrapper,
                                                                        FullReadingBean reading,
                                                                        ParentProcessReadingBean parentProcess ) throws SigarException {

        String readingProcessPattern = reading.getParameter( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_RECOGNITION_PATTERN );

        // we can match more than 1 process
        List<ReadingInstance> readingInstancesList = new ArrayList<ReadingInstance>();

        // iterate all processes in the map
        Map<Long, MatchedProcess> processesForThisPattern = matchedProcessesMap.get( readingProcessPattern );
        if( processesForThisPattern != null ) {
            for( final MatchedProcess processInfo : processesForThisPattern.values() ) {

                // check if we are not already measuring this reading for this process
                String processesReadingInstanceIdentifier = reading.getName() + "->" + processInfo.getPid();
                if( !processesReadingInstanceIdentifiers.contains( processesReadingInstanceIdentifier ) ) {
                    processesReadingInstanceIdentifiers.add( processesReadingInstanceIdentifier );

                    // create a new reading for this process
                    Map<String, String> parameters = constructProcessParametersMap( processInfo,
                                                                                    readingProcessPattern,
                                                                                    "1" );
                    readingInstancesList.add( new ReadingInstance( sigarWrapper, parentProcess,
                                                                   reading.getId() + "->" + processInfo.getPid(),
                                                                   processInfo.getPid(),
                                                                   reading.getMonitorName(),
                                                                   "[process] " + processInfo.getAlias()
                                                                                             + " - CPU usage - User",
                                                                   reading.getUnit(), parameters, 1.0F ) {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public void init() throws SigarException {

                            this.lastLongValue = fixLongValue( fixOverflow( getName(),
                                                                            sigarWrapper.getProcessCpuTimeRunningUser( processInfo.getPid() ),
                                                                            ReadingInstance.CPU_PROCESS_OVERFLOW_VALUE ) );
                        }

                        @Override
                        public float poll() throws SigarException {

                            long userTime = fixLongValue( fixOverflow( getName(),
                                                                       sigarWrapper.getProcessCpuTimeRunningUser( processInfo.getPid() ),
                                                                       ReadingInstance.CPU_PROCESS_OVERFLOW_VALUE ) );
                            double deltaUserTime;
                            if( userTime > 0 ) {
                                deltaUserTime = ( userTime - this.lastLongValue ) / numberOfCPUs;
                                this.lastLongValue = userTime;
                            } else {
                                return -1;
                            }

                            float deltaUserPercentUsage = 0.0F;
                            long elapsedTime = getElapsedTime();

                            // the minimal poll interval is 1s (1000ms) so we assume that < 900 is a poll in the same
                            // moment of creating the ReadingInstance, it is normal for new processes
                            if( elapsedTime > 900 ) {

                                // calculate user code time for 1ms
                                deltaUserTime = deltaUserTime / elapsedTime;

                                // convert to percentage usage
                                deltaUserPercentUsage = new BigDecimal( deltaUserTime ).setScale( 2,
                                                                                                  BigDecimal.ROUND_DOWN )
                                                                                       .floatValue()
                                                        * 100.0F;
                            }
                            addValueToParentProcess( deltaUserPercentUsage );
                            return deltaUserPercentUsage;
                        }
                    } );
                }
            }
        }
        return readingInstancesList;
    }

    private static List<ReadingInstance> getProcessCpuUsageRunningKernel( SigarWrapper sigarWrapper,
                                                                          FullReadingBean reading,
                                                                          ParentProcessReadingBean parentProcess ) throws SigarException {

        String readingProcessPattern = reading.getParameter( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_RECOGNITION_PATTERN );

        // we can match more than 1 process
        List<ReadingInstance> readingInstancesList = new ArrayList<ReadingInstance>();

        // iterate all processes in the map
        Map<Long, MatchedProcess> processesForThisPattern = matchedProcessesMap.get( readingProcessPattern );
        if( processesForThisPattern != null ) {
            for( final MatchedProcess processInfo : processesForThisPattern.values() ) {

                // check if we are not already measuring this reading for this process
                String processesReadingInstanceIdentifier = reading.getName() + "->" + processInfo.getPid();
                if( !processesReadingInstanceIdentifiers.contains( processesReadingInstanceIdentifier ) ) {
                    processesReadingInstanceIdentifiers.add( processesReadingInstanceIdentifier );

                    // create a new reading for this process
                    Map<String, String> parameters = constructProcessParametersMap( processInfo,
                                                                                    readingProcessPattern,
                                                                                    "2" );
                    readingInstancesList.add( new ReadingInstance( sigarWrapper, parentProcess,
                                                                   reading.getId() + "->" + processInfo.getPid(),
                                                                   processInfo.getPid(),
                                                                   reading.getMonitorName(),
                                                                   "[process] " + processInfo.getAlias()
                                                                                             + " - CPU usage - Kernel",
                                                                   reading.getUnit(), parameters, 1.0F ) {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public void init() throws SigarException {

                            this.lastLongValue = fixLongValue( fixOverflow( getName(),
                                                                            sigarWrapper.getProcessCpuTimeRunningKernel( processInfo.getPid() ),
                                                                            ReadingInstance.CPU_PROCESS_OVERFLOW_VALUE ) );
                        }

                        @Override
                        public float poll() throws SigarException {

                            long kernelTime = fixLongValue( fixOverflow( getName(),
                                                                         sigarWrapper.getProcessCpuTimeRunningKernel( processInfo.getPid() ),
                                                                         ReadingInstance.CPU_PROCESS_OVERFLOW_VALUE ) );
                            double deltaKernelTime;
                            if( kernelTime > 0 ) {
                                deltaKernelTime = ( kernelTime - this.lastLongValue ) / numberOfCPUs;
                                this.lastLongValue = kernelTime;
                            } else {
                                return -1;
                            }

                            float deltaKernelPercentUsage = 0.0F;
                            long elapsedTime = getElapsedTime();

                            // the minimal poll interval is 1s (1000ms) so we assume that < 900 is a poll in the same
                            // moment of creating the ReadingInstance, it is normal for new processes
                            if( elapsedTime > 900 ) {

                                // calculate kernel code time for 1ms
                                deltaKernelTime = deltaKernelTime / elapsedTime;

                                // convert to percentage usage
                                deltaKernelPercentUsage = new BigDecimal( deltaKernelTime ).setScale( 2,
                                                                                                      BigDecimal.ROUND_DOWN )
                                                                                           .floatValue()
                                                          * 100.0F;
                            }

                            addValueToParentProcess( deltaKernelPercentUsage );
                            return deltaKernelPercentUsage;
                        }
                    } );
                }
            }
        }
        return readingInstancesList;
    }

    private static List<ReadingInstance> getProcessCpuUsageRunningTotal( SigarWrapper sigarWrapper,
                                                                         FullReadingBean reading,
                                                                         ParentProcessReadingBean parentProcess ) throws SigarException {

        String readingProcessPattern = reading.getParameter( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_RECOGNITION_PATTERN );

        // we can match more than 1 process
        List<ReadingInstance> readingInstancesList = new ArrayList<ReadingInstance>();

        // iterate all processes in the map
        Map<Long, MatchedProcess> processesForThisPattern = matchedProcessesMap.get( readingProcessPattern );
        if( processesForThisPattern != null ) {
            for( final MatchedProcess processInfo : processesForThisPattern.values() ) {

                // check if we are not already measuring this reading for this process
                String processesReadingInstanceIdentifier = reading.getName() + "->" + processInfo.getPid();
                if( !processesReadingInstanceIdentifiers.contains( processesReadingInstanceIdentifier ) ) {
                    processesReadingInstanceIdentifiers.add( processesReadingInstanceIdentifier );

                    // create a new reading for this process
                    Map<String, String> parameters = constructProcessParametersMap( processInfo,
                                                                                    readingProcessPattern,
                                                                                    "3" );
                    readingInstancesList.add( new ReadingInstance( sigarWrapper, parentProcess,
                                                                   reading.getId() + "->" + processInfo.getPid(),
                                                                   processInfo.getPid(),
                                                                   reading.getMonitorName(),
                                                                   "[process] " + processInfo.getAlias()
                                                                                             + " - CPU usage - Total",
                                                                   reading.getUnit(), parameters, 1.0F ) {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public void init() throws SigarException {

                            this.lastLongValue = fixLongValue( fixOverflow( getName(),
                                                                            sigarWrapper.getProcessCpuTimeRunningTotal( processInfo.getPid() ),
                                                                            ReadingInstance.CPU_PROCESS_OVERFLOW_VALUE ) );
                        }

                        @Override
                        public float poll() throws SigarException {

                            long totalTime = fixLongValue( fixOverflow( getName(),
                                                                        sigarWrapper.getProcessCpuTimeRunningTotal( processInfo.getPid() ),
                                                                        ReadingInstance.CPU_PROCESS_OVERFLOW_VALUE ) );
                            double deltaTotalTime;
                            if( totalTime > 0 ) {
                                deltaTotalTime = ( totalTime - this.lastLongValue ) / numberOfCPUs;
                                this.lastLongValue = totalTime;
                            } else {
                                return -1;
                            }

                            float deltaTotalPercentUsage = 0.0F;
                            long elapsedTime = getElapsedTime();

                            // the minimal poll interval is 1s (1000ms) so we assume that < 900 is a poll in the same
                            // moment of creating the ReadingInstance, it is normal for new processes
                            if( elapsedTime > 900 ) {

                                // calculate total code time for 1ms
                                deltaTotalTime = deltaTotalTime / elapsedTime;

                                // convert to percentage usage
                                deltaTotalPercentUsage = new BigDecimal( deltaTotalTime ).setScale( 2,
                                                                                                    BigDecimal.ROUND_DOWN )
                                                                                         .floatValue()
                                                         * 100.0F;
                            }
                            addValueToParentProcess( deltaTotalPercentUsage );
                            return deltaTotalPercentUsage;
                        }
                    } );
                }
            }
        }
        return readingInstancesList;
    }

    private static List<ReadingInstance> getProcessVirtualMemory( SigarWrapper sigarWrapper,
                                                                  FullReadingBean reading,
                                                                  ParentProcessReadingBean parentProcess ) throws SigarException {

        String readingProcessPattern = reading.getParameter( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_RECOGNITION_PATTERN );

        // we can match more than 1 process
        List<ReadingInstance> readingInstancesList = new ArrayList<ReadingInstance>();

        // iterate all processes in the map
        Map<Long, MatchedProcess> processesForThisPattern = matchedProcessesMap.get( readingProcessPattern );
        if( processesForThisPattern != null ) {
            for( final MatchedProcess processInfo : processesForThisPattern.values() ) {

                // check if we are not already measuring this reading for this process
                String processesReadingInstanceIdentifier = reading.getName() + "->" + processInfo.getPid();
                if( !processesReadingInstanceIdentifiers.contains( processesReadingInstanceIdentifier ) ) {
                    processesReadingInstanceIdentifiers.add( processesReadingInstanceIdentifier );

                    // create a new reading for this process
                    Map<String, String> parameters = constructProcessParametersMap( processInfo,
                                                                                    readingProcessPattern,
                                                                                    "5" );
                    readingInstancesList.add( new ReadingInstance( sigarWrapper, parentProcess,
                                                                   reading.getId() + "->" + processInfo.getPid(),
                                                                   processInfo.getPid(),
                                                                   reading.getMonitorName(),
                                                                   "[process] " + processInfo.getAlias() + " - Virtual memory",
                                                                   reading.getUnit(), parameters, 0 ) {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public void init() throws SigarException {

                            applyMemoryNormalizationFactor();
                        }

                        @Override
                        public float poll() throws SigarException {

                            float result = toFloatWith2DecimalDigits( sigarWrapper.getProcessVirtualMemory( processInfo.getPid() ) );

                            addValueToParentProcess( result );
                            return result;
                        }
                    } );
                }
            }
        }
        return readingInstancesList;
    }

    private static List<ReadingInstance> getProcessResidentMemory( SigarWrapper sigarWrapper,
                                                                   FullReadingBean reading,
                                                                   ParentProcessReadingBean parentProcess ) throws SigarException {

        String readingProcessPattern = reading.getParameter( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_RECOGNITION_PATTERN );

        // we can match more than 1 process
        List<ReadingInstance> readingInstancesList = new ArrayList<ReadingInstance>();

        // iterate all processes in the map
        Map<Long, MatchedProcess> processesForThisPattern = matchedProcessesMap.get( readingProcessPattern );
        if( processesForThisPattern != null ) {
            for( final MatchedProcess processInfo : processesForThisPattern.values() ) {

                // check if we are not already measuring this reading for this process
                String processesReadingInstanceIdentifier = reading.getName() + "->" + processInfo.getPid();
                if( !processesReadingInstanceIdentifiers.contains( processesReadingInstanceIdentifier ) ) {
                    processesReadingInstanceIdentifiers.add( processesReadingInstanceIdentifier );

                    // create a new reading for this process
                    Map<String, String> parameters = constructProcessParametersMap( processInfo,
                                                                                    readingProcessPattern,
                                                                                    "6" );
                    readingInstancesList.add( new ReadingInstance( sigarWrapper, parentProcess,
                                                                   reading.getId() + "->" + processInfo.getPid(),
                                                                   processInfo.getPid(),
                                                                   reading.getMonitorName(),
                                                                   "[process] " + processInfo.getAlias() + " - Resident memory",
                                                                   reading.getUnit(), parameters, 0 ) {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public void init() throws SigarException {

                            applyMemoryNormalizationFactor();
                        }

                        @Override
                        public float poll() throws SigarException {

                            float result = toFloatWith2DecimalDigits( sigarWrapper.getProcessResidentMemory( processInfo.getPid() ) );
                            addValueToParentProcess( result );
                            return result;
                        }
                    } );
                }
            }
        }
        return readingInstancesList;
    }

    private static List<ReadingInstance> getProcessSharedMemory( SigarWrapper sigarWrapper,
                                                                 FullReadingBean reading,
                                                                 ParentProcessReadingBean parentProcess ) throws SigarException {

        String readingProcessPattern = reading.getParameter( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_RECOGNITION_PATTERN );

        // we can match more than 1 process
        List<ReadingInstance> readingInstancesList = new ArrayList<ReadingInstance>();

        // iterate all processes in the map
        Map<Long, MatchedProcess> processesForThisPattern = matchedProcessesMap.get( readingProcessPattern );
        if( processesForThisPattern != null ) {
            for( final MatchedProcess processInfo : processesForThisPattern.values() ) {

                // check if we are not already measuring this reading for this process
                String processesReadingInstanceIdentifier = reading.getName() + "->" + processInfo.getPid();
                if( !processesReadingInstanceIdentifiers.contains( processesReadingInstanceIdentifier ) ) {
                    processesReadingInstanceIdentifiers.add( processesReadingInstanceIdentifier );

                    // create a new reading for this process
                    Map<String, String> parameters = constructProcessParametersMap( processInfo,
                                                                                    readingProcessPattern,
                                                                                    "7" );
                    readingInstancesList.add( new ReadingInstance( sigarWrapper, parentProcess,
                                                                   reading.getId() + "->" + processInfo.getPid(),
                                                                   processInfo.getPid(),
                                                                   reading.getMonitorName(),
                                                                   "[process] " + processInfo.getAlias() + " - Shared memory",
                                                                   reading.getUnit(), parameters, 0 ) {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public void init() throws SigarException {

                            applyMemoryNormalizationFactor();
                        }

                        @Override
                        public float poll() throws SigarException {

                            float result = toFloatWith2DecimalDigits( sigarWrapper.getProcessSharedMemory( processInfo.getPid() ) );
                            addValueToParentProcess( result );
                            return result;
                        }
                    } );
                }
            }
        }
        return readingInstancesList;
    }

    private static List<ReadingInstance> getProcessMemoryPageFaults( SigarWrapper sigarWrapper,
                                                                     FullReadingBean reading,
                                                                     ParentProcessReadingBean parentProcess ) throws SigarException {

        String readingProcessPattern = reading.getParameter( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_RECOGNITION_PATTERN );

        // we can match more than 1 process
        List<ReadingInstance> readingInstancesList = new ArrayList<ReadingInstance>();

        // iterate all processes in the map
        Map<Long, MatchedProcess> processesForThisPattern = matchedProcessesMap.get( readingProcessPattern );
        if( processesForThisPattern != null ) {
            for( final MatchedProcess processInfo : processesForThisPattern.values() ) {

                // check if we are not already measuring this reading for this process
                String processesReadingInstanceIdentifier = reading.getName() + "->" + processInfo.getPid();
                if( !processesReadingInstanceIdentifiers.contains( processesReadingInstanceIdentifier ) ) {
                    processesReadingInstanceIdentifiers.add( processesReadingInstanceIdentifier );

                    // create a new reading for this process
                    Map<String, String> parameters = constructProcessParametersMap( processInfo,
                                                                                    readingProcessPattern,
                                                                                    "8" );
                    readingInstancesList.add( new ReadingInstance( sigarWrapper, parentProcess,
                                                                   reading.getId() + "->" + processInfo.getPid(),
                                                                   processInfo.getPid(),
                                                                   reading.getMonitorName(),
                                                                   "[process] " + processInfo.getAlias()
                                                                                             + " - Memory page faults",
                                                                   reading.getUnit(), parameters, 1.0F ) {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public void init() throws SigarException {

                            this.lastLongValue = fixLongValue( fixOverflow( getName(),
                                                                            sigarWrapper.getProcessMemoryPageFaults( processInfo.getPid() ),
                                                                            ReadingInstance.MEMORYPAGEFAULTS_PROCESS_OVERFLOW_VALUE ) );
                        }

                        @Override
                        public float poll() throws SigarException {

                            long memoryPageFaults = fixLongValue( fixOverflow( getName(),
                                                                               sigarWrapper.getProcessMemoryPageFaults( processInfo.getPid() ),
                                                                               ReadingInstance.MEMORYPAGEFAULTS_PROCESS_OVERFLOW_VALUE ) );
                            double deltaMemoryPageFaults;
                            if( memoryPageFaults != -1 ) {
                                deltaMemoryPageFaults = ( memoryPageFaults - this.lastLongValue )
                                                        * normalizationFactor;
                                this.lastLongValue = memoryPageFaults;
                            } else {
                                return -1;
                            }

                            float result = 0.0F;
                            long elapsedTime = getElapsedTime();

                            // the minimal poll interval is 1s (1000ms) so we assume that < 900 is a poll in the same
                            // moment of creating the ReadingInstance, it is normal for new processes
                            if( elapsedTime > 900 ) {

                                // calculate memory page faults per second
                                deltaMemoryPageFaults = deltaMemoryPageFaults
                                                        / ( ( double ) elapsedTime / 1000 );

                                // return a float with 2 digits after the decimal point
                                result = new BigDecimal( deltaMemoryPageFaults ).setScale( 2,
                                                                                           BigDecimal.ROUND_DOWN )
                                                                                .floatValue();
                            }
                            addValueToParentProcess( result );
                            return result;
                        }
                    } );
                }
            }
        }
        return readingInstancesList;
    }

    private static List<ReadingInstance> updateProcessesMatchingMap( Sigar sigar,
                                                                     List<FullReadingBean> initialProcessReadings,
                                                                     List<ReadingInstance> currentReadingInstances ) throws SigarException {

        // remember to user process regex and alias
        Map<String, Pattern> processPatterns = new HashMap<String, Pattern>();
        Map<String, String> processAliases = new HashMap<String, String>();
        Map<String, String> processUsernames = new HashMap<String, String>();
        for( FullReadingBean processReading : initialProcessReadings ) {

            String userRegex = processReading.getParameter( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_RECOGNITION_PATTERN );
            Pattern compiledPattern = Pattern.compile( userRegex );
            processPatterns.put( userRegex, compiledPattern );

            String userProcessAlias = processReading.getParameter( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_ALIAS );
            processAliases.put( userRegex, userProcessAlias );

            String processUsername = processReading.getParameter( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_USERNAME );
            processUsernames.put( userRegex, processUsername );
        }

        // at the end only processes that are not alive will be in this list
        Set<Long> finishedProcessesIds = new HashSet<Long>( matchedProcessesIds );

        // iterate all system processes and remember the ones we want to monitor
        for( long pid : sigar.getProcList() ) {
            // check if we know this process from a previous poll, we do not want to add it again
            if( !matchedProcessesIds.contains( pid ) ) {

                // we try to match a process by its start command
                String processStartCommand = constructProcessStartCommand( sigar, pid );
                if( processStartCommand != null && !processStartCommand.isEmpty() ) {

                    String processUsername = null;

                    // check this process against all patterns
                    for( String userRegex : processPatterns.keySet() ) {

                        // by default we search processes from all users
                        boolean isExpectedProcessUsername = true;

                        // check if it matters who started this process
                        String requestedProcessUsername = processUsernames.get( userRegex );
                        if( !StringUtils.isNullOrEmpty( requestedProcessUsername ) ) {
                            // we search processes from a specific user only

                            if( processUsername == null ) {
                                // we still do not know the user of this process
                                try {
                                    processUsername = sigar.getProcCredName( pid ).getUser();
                                } catch( SigarException e ) {
                                    // a specific username is required, but we can not get the info about this process
                                    isExpectedProcessUsername = false;
                                }
                            }

                            isExpectedProcessUsername = requestedProcessUsername.equalsIgnoreCase( processUsername );
                        }

                        if( isExpectedProcessUsername ) {
                            Pattern processPattern = processPatterns.get( userRegex );
                            if( processPattern.matcher( processStartCommand.trim() ).matches() ) {
                                Map<Long, MatchedProcess> processesMatchedThisRegex = matchedProcessesMap.get( userRegex );
                                if( processesMatchedThisRegex == null ) {
                                    processesMatchedThisRegex = new HashMap<Long, MatchedProcess>();
                                }

                                int processIndexForThisPattern = getNextIndexForThisProcessPattern( userRegex );
                                MatchedProcess matchedProcess = new MatchedProcess( pid, userRegex,
                                                                                    processAliases.get( userRegex ),
                                                                                    processIndexForThisPattern,
                                                                                    processStartCommand );
                                processesMatchedThisRegex.put( pid, matchedProcess );
                                matchedProcessesMap.put( userRegex, processesMatchedThisRegex );
                                matchedProcessesIds.add( pid );

                                log.info( "We will monitor process: " + matchedProcess.toString() );
                            }
                        }
                    }
                }
            } else {
                // the process is still alive
                finishedProcessesIds.remove( pid );
            }
        }

        // check if some processes have died, we do not want to monitor them anymore
        for( Long finishedProcessId : finishedProcessesIds ) {

            // cleanup the maps
            matchedProcessesIds.remove( finishedProcessId );
            for( Map<Long, MatchedProcess> matchedProcessMap : matchedProcessesMap.values() ) {
                MatchedProcess removedProcess = matchedProcessMap.remove( finishedProcessId );
                if( removedProcess != null ) {
                    log.info( "This process is no more alive: " + removedProcess.toString() );
                }
            }
            Iterator<String> it = processesReadingInstanceIdentifiers.iterator();
            while( it.hasNext() ) {
                String processesReadingInstanceIdentifier = it.next();
                if( Long.parseLong( processesReadingInstanceIdentifier.split( "->" )[1] ) == finishedProcessId ) {
                    it.remove();
                }
            }
        }

        // return the updated list of reading instances we poll
        List<ReadingInstance> newReadingInstances = new ArrayList<ReadingInstance>();
        for( ReadingInstance currentReadingInstance : currentReadingInstances ) {
            boolean stillActiveInstance = true;
            for( Long finishedProcessId : finishedProcessesIds ) {
                if( finishedProcessId == currentReadingInstance.getPid() ) {
                    stillActiveInstance = false;
                    break;
                }
            }

            if( stillActiveInstance ) {
                newReadingInstances.add( currentReadingInstance );
            }
        }
        return newReadingInstances;
    }

    private static Map<String, String> constructProcessParametersMap( MatchedProcess processInfo,
                                                                      String readingProcessPattern,
                                                                      String readingId ) {

        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_ALIAS, processInfo.getAlias() );

        parameters.put( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_RECOGNITION_PATTERN,
                        readingProcessPattern );

        parameters.put( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_START_COMMAND,
                        processInfo.getStartCommand() );

        parameters.put( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_READING_ID, readingId );

        return parameters;
    }

    private static int getNextIndexForThisProcessPattern( String userRegex ) {

        int nextIndex = -1;
        if( matchedProcessesIndexes.containsKey( userRegex ) ) {
            nextIndex = matchedProcessesIndexes.get( userRegex );
        }
        matchedProcessesIndexes.put( userRegex, ++nextIndex );

        return nextIndex;
    }

    private static String constructProcessStartCommand( Sigar sigar, long pid ) {

        StringBuilder startCommand = new StringBuilder();
        try {
            String[] processArgs = sigar.getProcArgs( pid );
            for( String arg : processArgs ) {
                startCommand.append( arg );
                startCommand.append( " " );
            }
            return startCommand.toString();
        } catch( SigarException e ) {
            // some system processes can not be accessed
            return null;
        }
    }
}

class MatchedProcess {

    private Long   pid;
    private String pattern;
    private String alias;
    private int    aliasIndex;
    private String startCommand;

    public MatchedProcess( Long pid, String pattern, String alias, int aliasIndex, String startCommand ) {

        this.pid = pid;
        this.pattern = pattern;
        this.alias = alias;
        this.aliasIndex = aliasIndex;
        this.startCommand = startCommand;
    }

    public Long getPid() {

        return pid;
    }

    public String getPattern() {

        return pattern;
    }

    public String getAlias() {

        // the first process has index = 0 - do not suffix it
        // the second process has index = 1 - suffix it with [2]
        // the third process has index = 2 - suffix it with [3]
        // etc.
        if( aliasIndex == 0 ) {
            return alias;
        } else {
            return alias + " [" + ( aliasIndex + 1 ) + "]";
        }
    }

    public String getStartCommand() {

        return startCommand;
    }

    @Override
    public String toString() {

        return "pid = " + pid + ", pattern = '" + pattern + "', alias = '" + alias + "', start command = '"
               + startCommand + "'";
    }

}
