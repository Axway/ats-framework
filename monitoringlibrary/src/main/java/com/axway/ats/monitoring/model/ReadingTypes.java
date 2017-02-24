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
package com.axway.ats.monitoring.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.axway.ats.common.performance.monitor.beans.FullReadingBean;
import com.axway.ats.core.monitoring.SystemMonitorDefinitions;
import com.axway.ats.monitoring.model.exceptions.MonitoringException;
import com.axway.ats.monitoring.model.readings.ReadingsRepository;

/**
 * The supported by default reading types
 */
public class ReadingTypes {

    /** All CPU related statistics */
    public static final String READING_CPU                = "READING_CPU";

    /** All MEMORY related statistics */
    public static final String READING_MEMORY             = "READING_MEMORY";

    /** All VIRTUAL MEMORY related statistics */
    public static final String READING_VIRTUAL_MEMORY     = "READING_VIRTUAL_MEMORY";

    /** All IO related statistics */
    public static final String READING_IO                 = "READING_IO";

    /** All network interfaces related statistics */
    public static final String READING_NETWORK_INTERFACES = "READING_NICS";

    /** All Netstat related statistics */
    public static final String READING_NETSTAT            = "READING_NETSTAT";

    /** All TCP related statistics */
    public static final String READING_TCP                = "READING_TCP";

    /** All PROCESS CPU related statistics */
    public static final String READING_PROCESS_CPU        = "READING_PROCESS_CPU";

    /** All PROCESS MEMORY related statistics */
    public static final String READING_PROCESS_MEMORY     = "READING_PROCESS_MEMORY";

    public static Set<FullReadingBean> expandSystemReadings(
                                                             String[] readingTypes ) {

        // expand the reading names
        Set<String> readingNames = new HashSet<String>();
        for( String readingType : readingTypes ) {
            if( readingType.equals( READING_CPU ) ) {
                readingNames.addAll( SystemMonitorDefinitions.getAllCpuReadings() );
            } else if( readingType.equals( READING_MEMORY ) ) {
                readingNames.addAll( SystemMonitorDefinitions.getAllMemoryReadings() );
            } else if( readingType.equals( READING_VIRTUAL_MEMORY ) ) {
                readingNames.addAll( SystemMonitorDefinitions.getAllVirtualMemoryReadings() );
            } else if( readingType.equals( READING_IO ) ) {
                readingNames.addAll( SystemMonitorDefinitions.getAllIOReadings() );
            } else if( readingType.equals( SystemMonitorDefinitions.READING_NETWORK_TRAFFIC ) ) {
                readingNames.add( SystemMonitorDefinitions.READING_NETWORK_TRAFFIC );
            } else if( readingType.equals( READING_NETWORK_INTERFACES ) ) {
                readingNames.addAll( SystemMonitorDefinitions.getAllNetworkInterfacesReadings() );
            } else if( readingType.equals( READING_NETSTAT ) ) {
                readingNames.addAll( SystemMonitorDefinitions.getAllNetstatReadings() );
            } else if( readingType.equals( READING_TCP ) ) {
                readingNames.addAll( SystemMonitorDefinitions.getAllTcpReadings() );
            } else {
                // this is considered as a custom reading
                readingNames.add( readingType );
            }
        }

        // return a set of reading beans
        return new HashSet<FullReadingBean>( ReadingsRepository.getInstance()
                                                               .getReadingXmlDefinitions( readingNames ) );
    }

    /**
     * @param processPattern the pattern to use in order to find the monitored process
     * @param processAlias the alias this process will have into the database
     * @param readingTypes the readings to collect
     *
     * @return a set of readings, if pass more than once the same reading it will be automatically mearged into one
     */
    public static Set<FullReadingBean> expandProcessReadings(
                                                              String parentProcess,
                                                              String processPattern,
                                                              String processAlias,
                                                              String processUsername,
                                                              String[] readingTypes ) {

        Set<String> readingNames = new HashSet<String>();
        for( String readingType : readingTypes ) {
            if( readingType.equals( READING_PROCESS_CPU ) ) {
                // expand all CPU
                readingNames.addAll( SystemMonitorDefinitions.getAllProcessCpuReadings() );
            } else if( readingType.equals( READING_PROCESS_MEMORY ) ) {
                // expand all MEMORY
                readingNames.addAll( SystemMonitorDefinitions.getAllProcessMemoryReadings() );
            } else if( SystemMonitorDefinitions.isProcessReading( readingType ) ) {
                // add this a known process reading
                readingNames.add( readingType );
            } else {
                // unknown process reading
                throw new MonitoringException( "Unknown process monitor type " + readingType );
            }
        }

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_PARENT_NAME, parentProcess );
        parameters.put( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_RECOGNITION_PATTERN, processPattern );
        parameters.put( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_ALIAS, processAlias );
        parameters.put( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_USERNAME, processUsername );

        List<FullReadingBean> processReadingDefinitions = ReadingsRepository.getInstance()
                                                                            .getReadingXmlDefinitions( readingNames );
        for( FullReadingBean processReadingDefinition : processReadingDefinitions ) {
            processReadingDefinition.setParameters( parameters );
        }

        // return a set of reading beans
        return new HashSet<FullReadingBean>( processReadingDefinitions );
    }
}
