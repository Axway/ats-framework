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
package com.axway.ats.agent.core.monitoring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.axway.ats.agent.core.monitoring.agents.AtsSystemMonitoringAgent;
import com.axway.ats.agent.core.monitoring.exceptions.OperationUnsuccessfulException;
import com.axway.ats.agent.core.monitoring.systemmonitor.MonitoringContext;
import com.axway.ats.agent.core.monitoring.systemmonitor.ReadingTypes;
import com.axway.ats.agent.core.monitoring.systemmonitor.ReadingsRepository;
import com.axway.ats.common.performance.monitor.PerformanceMonitor;
import com.axway.ats.common.performance.monitor.beans.ReadingBean;
import com.axway.ats.core.monitoring.MonitorConfigurationException;
import com.axway.ats.core.monitoring.MonitoringException;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;

/**
 * Used by @RestSystemMonitor to perform monitoring operations on the agent
 * */
public class AgentSystemMonitor {

    private AtsSystemMonitoringAgent monitoringAgent;

    public Set<ReadingBean> scheduleSystemMonitoring(
                                                          String[] systemReadingTypes ) {

        return ReadingTypes.expandSystemReadings( systemReadingTypes );
    }

    public void initializeMonitoring(
                                      List<ReadingBean> readings,
                                      int pollInterval,
                                      long executorTimeOffset ) {

        monitoringAgent = new AtsSystemMonitoringAgent( pollInterval, executorTimeOffset );

        if( pollInterval < 1 ) {
            throw new MonitoringException( "The interval for collecting statistical data must be at least 1 second. You have specified "
                                           + pollInterval + " seconds" );
        }

        Map<String, List<ReadingBean>> readingsPerMonitor = new HashMap<String, List<ReadingBean>>();

        // load all the monitors and initialize them
        for( ReadingBean reading : readings ) {
            List<ReadingBean> readingsForThisMonitor = readingsPerMonitor.get( reading.getMonitorName() );
            if( readingsForThisMonitor == null ) {
                readingsForThisMonitor = new ArrayList<ReadingBean>();
                readingsPerMonitor.put( reading.getMonitorName(), readingsForThisMonitor );
            }
            readingsForThisMonitor.add( reading );

        }

        for( String monitorClassName : readingsPerMonitor.keySet() ) {
            initializeMonitor( monitorClassName, readingsPerMonitor.get( monitorClassName ), pollInterval );
        }
    }

    public ReadingBean scheduleMonitoring(
                                               String readingType,
                                               Map<String, String> readingParameters ) {

        return ReadingsRepository.getInstance().getReadingXmlDefinition( readingType, readingParameters );
    }

    public Set<ReadingBean> scheduleProcessMonitoring(
                                                           String parentProcess,
                                                           String processPattern,
                                                           String processAlias,
                                                           String processUsername,
                                                           String[] processReadingTypes ) {

        return ReadingTypes.expandProcessReadings( parentProcess,
                                                   processPattern,
                                                   processAlias,
                                                   processUsername,
                                                   processReadingTypes );
    }

    public ReadingBean scheduleJvmMonitoring(
                                                  String readingType,
                                                  Map<String, String> readingParameters ) {

        return ReadingsRepository.getInstance().getReadingXmlDefinition( readingType, readingParameters );
    }

    public int scheduleCustomJvmMonitoring() {

        return ReadingsRepository.getInstance().getNewUniqueId();
    }

    public void updateDatabaseRepository(
                                          String monitoredHost,
                                          List<ReadingBean> readings ) throws DatabaseAccessException {

        ReadingsRepository.getInstance().updateDatabaseRepository( monitoredHost, readings );
    }

    public void initializeMonitoringContext() throws IOException {

        MonitoringContext.getInstance().init();
    }

    public void startMonitoring() throws Exception {

        // start all the monitors
        try {
            monitoringAgent.startMonitoring();
        } catch( Exception e ) {
            // unable to start the monitoring process
            // deinitialize all monitors
            monitoringAgent.resetTheMonitoringAgent();
            throw new OperationUnsuccessfulException( "Unable to start the monitoring agent", e );
        }
    }

    public void stopMonitoring() throws Exception {

        try {
            monitoringAgent.stopMonitoring();
        } catch( Exception e ) {
            throw new OperationUnsuccessfulException( "Unable to stop the monitoring agent", e );
        }
    }

    private void initializeMonitor(
                                    String monitorClassName,
                                    List<ReadingBean> readings,
                                    int pollInterval ) throws MonitorConfigurationException {

        // try to make an instance of the monitor class
        Object monitorInstance;
        try {
            monitorInstance = Class.forName( monitorClassName ).newInstance();
        } catch( InstantiationException e ) {
            throw new MonitorConfigurationException( "InstantiationException while constructing '"
                                                     + monitorClassName
                                                     + "' monitor. Exception error message: "
                                                     + e.getMessage() );
        } catch( IllegalAccessException e ) {
            throw new MonitorConfigurationException( "IllegalAccessException while constructing '"
                                                     + monitorClassName
                                                     + "' monitor. Exception error message: "
                                                     + e.getMessage() );
        } catch( ClassNotFoundException e ) {
            throw new MonitorConfigurationException( "ClassNotFoundException while constructing '"
                                                     + monitorClassName
                                                     + "' monitor. Exception error message: "
                                                     + e.getMessage() );
        }

        // check if it implements the needed interface
        if( ! ( monitorInstance instanceof PerformanceMonitor ) ) {
            throw new MonitorConfigurationException( monitorClassName + " is not a valid monitor class" );
        }
        PerformanceMonitor monitor = ( PerformanceMonitor ) monitorInstance;

        try {
            // initialize the monitor
            monitor.setPollInterval( pollInterval );
            monitor.init( readings.toArray( new ReadingBean[readings.size()] ) );
        } catch( Throwable e ) {
            throw new MonitorConfigurationException( "Error initializing " + monitorClassName + " monitor: "
                                                     + e.getMessage(), e );
        }
        monitoringAgent.addMonitor( monitor );
    }

}
