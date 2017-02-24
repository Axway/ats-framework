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
package com.axway.ats.agent.components.monitoring.operations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.axway.ats.agent.components.monitoring.model.agents.AtsSystemMonitoringAgent;
import com.axway.ats.agent.components.monitoring.model.exceptions.MonitorConfigurationException;
import com.axway.ats.agent.components.monitoring.model.exceptions.OperationUnsuccessfulException;
import com.axway.ats.agent.core.model.Action;
import com.axway.ats.agent.core.model.Parameter;
import com.axway.ats.common.performance.monitor.PerformanceMonitor;
import com.axway.ats.common.performance.monitor.beans.FullReadingBean;
import com.axway.ats.common.performance.monitor.beans.MonitorResults;

public class InternalSystemMonitoringOperations {

    private AtsSystemMonitoringAgent monitoringAgent;

    @Action(name = "Internal System Monitoring Operations Initialize Monitoring")
    public void initializeMonitoring(
                                      @Parameter(name = "readings") List<FullReadingBean> readings,
                                      @Parameter(name = "startTimestamp") long startTimestamp,
                                      @Parameter(name = "pollInterval") int pollInterval ) throws Exception {

        monitoringAgent = new AtsSystemMonitoringAgent( startTimestamp, pollInterval );

        Map<String, List<FullReadingBean>> readingsPerMonitor = new HashMap<String, List<FullReadingBean>>();

        // load all the monitors and initialize them
        for( FullReadingBean reading : readings ) {
            List<FullReadingBean> readingsForThisMonitor = readingsPerMonitor.get( reading.getMonitorName() );
            if( readingsForThisMonitor == null ) {
                readingsForThisMonitor = new ArrayList<FullReadingBean>();
                readingsPerMonitor.put( reading.getMonitorName(), readingsForThisMonitor );
            }
            readingsForThisMonitor.add( reading );

        }

        for( String monitorClassName : readingsPerMonitor.keySet() ) {
            initializeMonitor( monitorClassName, readingsPerMonitor.get( monitorClassName ), pollInterval );
        }
    }

    @Action(name = "Internal System Monitoring Operations Start Monitoring")
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

    @Action(name = "Internal System Monitoring Operations Get Collected Results")
    public List<MonitorResults> getCollectedResults() throws Exception {

        try {
            return monitoringAgent.getMonitoringResults();
        } catch( Exception e ) {
            throw new OperationUnsuccessfulException( "Unable to get the collected results", e );
        }
    }

    @Action(name = "Internal System Monitoring Operations Stop Monitoring")
    public void stopMonitoring() throws Exception {

        try {
            monitoringAgent.stopMonitoring();
        } catch( Exception e ) {
            throw new OperationUnsuccessfulException( "Unable to stop the monitoring agent", e );
        }
    }

    private void initializeMonitor(
                                    String monitorClassName,
                                    List<FullReadingBean> readings,
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
            monitor.init( readings.toArray( new FullReadingBean[readings.size()] ) );
        } catch( Throwable e ) {
            throw new MonitorConfigurationException( "Error initializing " + monitorClassName + " monitor: "
                                                     + e.getMessage(), e );
        }
        monitoringAgent.addMonitor( monitor );
    }
}
