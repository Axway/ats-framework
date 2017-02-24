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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hyperic.sigar.SigarException;

import com.axway.ats.common.performance.monitor.PerformanceMonitor;
import com.axway.ats.common.performance.monitor.beans.BasicReadingBean;
import com.axway.ats.common.performance.monitor.beans.FullReadingBean;
import com.axway.ats.common.performance.monitor.beans.ParentProcessReadingBean;
import com.axway.ats.core.monitoring.SystemMonitorDefinitions;

/**
 * The default ATS system monitor
 */
public class AtsSystemMonitor extends PerformanceMonitor {

    private static final Logger                   log                           = Logger.getLogger( AtsSystemMonitor.class );

    private SigarWrapper                          sigarWrapper;

    private List<ReadingInstance>                 staticReadingInstances        = new ArrayList<ReadingInstance>();
    private List<ReadingInstance>                 dynamicReadingInstances       = new ArrayList<ReadingInstance>();
    private Map<String, ParentProcessReadingBean> parentProcessReadingInstances = new HashMap<String, ParentProcessReadingBean>();

    List<FullReadingBean>                         initialDynamicReadings;

    @Override
    public void init(
                      FullReadingBean[] readings ) throws Exception {

        log.info( "Initializing the ATS System Monitor" );

        try {
            this.sigarWrapper = new SigarWrapper();
        } catch( SigarException e ) {
            log.error( "Error initializing the Sigar System", e );
            throw new Exception( "Error initializing the Sigar System", e );
        }

        List<FullReadingBean> staticReadings = new ArrayList<FullReadingBean>();
        List<FullReadingBean> dynamicReadings = new ArrayList<FullReadingBean>();
        for( FullReadingBean reading : readings ) {
            if( !reading.isDynamicReading() ) {
                staticReadings.add( reading );
            } else {
                // check if this process has parent
                String parentProcessName = reading.getParameter( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_PARENT_NAME );
                if( parentProcessName != null ) {
                    final String parentProcessId = parentProcessName + "-" + reading.getName();
                    if( !parentProcessReadingInstances.containsKey( parentProcessId ) ) {
                        parentProcessReadingInstances.put( parentProcessId,
                                                           new ParentProcessReadingBean( reading.getId(),
                                                                                         reading.getMonitorName(),
                                                                                         parentProcessName,
                                                                                         reading.getName(),
                                                                                         reading.getUnit() ) );
                    }
                }
                dynamicReadings.add( reading );
            }
        }

        ReadingInstancesFactory.init( sigarWrapper, getPollInterval() );

        // create the actual static reading instances
        staticReadingInstances = ReadingInstancesFactory.createStaticReadingInstances( sigarWrapper,
                                                                                       staticReadings );

        // remember the initial dynamic readings
        initialDynamicReadings = new ArrayList<FullReadingBean>( dynamicReadings );

        // create the list of dynamic reading instances. Initializing lastPollTime and lastLongValue to make correct
        // calculations on the first poll
        if( initialDynamicReadings.size() > 0 ) {
            dynamicReadingInstances = ReadingInstancesFactory.createOrUpdateDynamicReadingInstances( sigarWrapper,
                                                                                                     parentProcessReadingInstances,
                                                                                                     initialDynamicReadings,
                                                                                                     dynamicReadingInstances );
        }
    }

    @Override
    public void deinit() throws Exception {

        this.sigarWrapper.stopUsingSigar();
        this.sigarWrapper = null;
    }

    @Override
    public List<BasicReadingBean> pollNewDataForFirstTime() throws Exception {

        return doPoll( true );
    }

    @Override
    public List<BasicReadingBean> pollNewData() throws Exception {

        return doPoll( false );
    }

    public List<BasicReadingBean> doPoll(
                                          boolean isFirstTime ) throws Exception {

        List<BasicReadingBean> redingsResult = new ArrayList<BasicReadingBean>();

        // clear the values from previous poll for all parent processes
        for( ParentProcessReadingBean parentProcessReading : parentProcessReadingInstances.values() ) {
            parentProcessReading.resetValue();
        }

        // update the list of dynamic reading instances
        if( initialDynamicReadings.size() > 0 ) {
            dynamicReadingInstances = ReadingInstancesFactory.createOrUpdateDynamicReadingInstances( sigarWrapper,
                                                                                                     parentProcessReadingInstances,
                                                                                                     initialDynamicReadings,
                                                                                                     dynamicReadingInstances );
        }

        // refresh the Sigar's info
        this.sigarWrapper.refresh();

        // poll the static reading instances
        redingsResult.addAll( pollReadingInstances( staticReadingInstances ) );

        // poll the dynamic reading instances
        if( initialDynamicReadings.size() > 0 ) {
            redingsResult.addAll( pollReadingInstances( dynamicReadingInstances ) );
        }

        // add the parent process values
        redingsResult.addAll( pollParentProcessInstances( parentProcessReadingInstances.values() ) );

        return redingsResult;
    }

    private List<BasicReadingBean> pollReadingInstances(
                                                         List<ReadingInstance> readingInstances )
                                                                                                 throws Exception {

        List<BasicReadingBean> redingsResult = new ArrayList<BasicReadingBean>();

        for( ReadingInstance readingInstance : readingInstances ) {
            float value = readingInstance.poll();

            BasicReadingBean newResult;
            if( readingInstance.isNewInstance() ) {
                readingInstance.setNewInstanceFlag( false );
                // the Test Executor needs to be informed about this reading instance
                // we pass back a Full Reading Bean
                newResult = readingInstance.getNewCopy();
            } else {
                // the Test Executor is already aware we are collecting data about this reading instance
                newResult = new BasicReadingBean();
                newResult.setId( readingInstance.getId() );
            }
            newResult.setValue( String.valueOf( value ) );
            redingsResult.add( newResult );
        }

        return redingsResult;
    }

    private List<BasicReadingBean> pollParentProcessInstances(
                                                               Collection<ParentProcessReadingBean> parentProcessReadingInstances ) {

        List<BasicReadingBean> redingsResult = new ArrayList<BasicReadingBean>();

        for( ParentProcessReadingBean readingInstance : parentProcessReadingInstances ) {
            // get the collected value from all children
            float value = readingInstance.poll();

            BasicReadingBean newResult;
            if( readingInstance.isNewInstance() ) {
                readingInstance.setNewInstanceFlag( false );
                // the Test Executor needs to be informed about this reading instance
                // we pass back a Full Reading Bean
                newResult = readingInstance.getNewCopy();
            } else {
                // the Test Executor is already aware we are collecting data about this reading instance
                newResult = new BasicReadingBean();
                newResult.setId( readingInstance.getId() );
            }
            newResult.setValue( String.valueOf( value ) );
            redingsResult.add( newResult );
        }

        return redingsResult;
    }

    @Override
    public String getDescription() {

        return "ATS System Performance Monitor";
    }
}
