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
package com.axway.ats.agent.core.monitoring.systemmonitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.ISystemInformation;
import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.exceptions.SystemInformationException;
import com.axway.ats.common.performance.monitor.PerformanceMonitor;
import com.axway.ats.common.performance.monitor.beans.ParentProcessReadingBean;
import com.axway.ats.common.performance.monitor.beans.ReadingBean;
import com.axway.ats.core.monitoring.SystemMonitorDefinitions;

/**
 * The default ATS system monitor
 */
public class AtsSystemMonitor extends PerformanceMonitor {

    private static final Logger                   log                           = LogManager.getLogger(AtsSystemMonitor.class);

    private ISystemInformation                    systemInfo;

    private List<ReadingInstance>                 staticReadingInstances        = new ArrayList<ReadingInstance>();
    private List<ReadingInstance>                 dynamicReadingInstances       = new ArrayList<ReadingInstance>();
    private Map<String, ParentProcessReadingBean> parentProcessReadingInstances = new HashMap<String, ParentProcessReadingBean>();

    List<ReadingBean>                             initialDynamicReadings;

    @Override
    public void init(
                      ReadingBean[] readings ) throws Exception {

        log.info("Initializing the ATS System Monitor");

        try {
            this.systemInfo = SystemInformationFactory.get();
        } catch (Exception e) {
            String errorMessage = "Error initializing the provider of system information. System monitoring will not work.";
            log.error(errorMessage, e);
            throw new SystemInformationException(errorMessage, e);
        }

        List<ReadingBean> staticReadings = new ArrayList<ReadingBean>();
        List<ReadingBean> dynamicReadings = new ArrayList<ReadingBean>();
        for (ReadingBean reading : readings) {
            if (!reading.isDynamicReading()) {
                staticReadings.add(reading);
            } else {
                // check if this process has a parent
                String parentProcessName = reading.getParameter(SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_PARENT_NAME);
                if (parentProcessName != null) {
                    final String parentProcessId = parentProcessName + "-" + reading.getName();
                    if (!parentProcessReadingInstances.containsKey(parentProcessId)) {
                        ParentProcessReadingBean prentProcessBean = new ParentProcessReadingBean(reading.getId(),
                                                                                                 reading.getMonitorName(),
                                                                                                 parentProcessName,
                                                                                                 reading.getName(),
                                                                                                 reading.getUnit());
                        prentProcessBean.setParameters(reading.getParameters());
                        parentProcessReadingInstances.put(parentProcessId, prentProcessBean);
                    }
                }
                dynamicReadings.add(reading);
            }
        }

        ReadingInstancesFactory.init(systemInfo, getPollInterval());

        // create the actual static reading instances
        staticReadingInstances = ReadingInstancesFactory.createStaticReadingInstances(systemInfo,
                                                                                      staticReadings);

        // remember the initial dynamic readings
        initialDynamicReadings = new ArrayList<ReadingBean>(dynamicReadings);

        // create the list of dynamic reading instances. Initializing lastPollTime and lastLongValue to make correct
        // calculations on the first poll
        if (initialDynamicReadings.size() > 0) {
            dynamicReadingInstances = ReadingInstancesFactory.createOrUpdateDynamicReadingInstances(systemInfo,
                                                                                                    parentProcessReadingInstances,
                                                                                                    initialDynamicReadings,
                                                                                                    dynamicReadingInstances);
        }
    }

    @Override
    public void deinit() throws Exception {

        this.systemInfo.destroy();
        this.systemInfo = null;
    }

    @Override
    public List<ReadingBean> pollNewDataForFirstTime() throws Exception {

        return doPoll(true);
    }

    @Override
    public List<ReadingBean> pollNewData() throws Exception {

        return doPoll(false);
    }

    public List<ReadingBean> doPoll(
                                     boolean isFirstTime ) throws Exception {

        List<ReadingBean> redingsResult = new ArrayList<ReadingBean>();

        // clear the values from previous poll for all parent processes
        for (ParentProcessReadingBean parentProcessReading : parentProcessReadingInstances.values()) {
            parentProcessReading.resetValue();
        }

        // update the list of dynamic reading instances
        if (initialDynamicReadings.size() > 0) {
            dynamicReadingInstances = ReadingInstancesFactory.createOrUpdateDynamicReadingInstances(systemInfo,
                                                                                                    parentProcessReadingInstances,
                                                                                                    initialDynamicReadings,
                                                                                                    dynamicReadingInstances);
        }

        this.systemInfo.refresh();

        // poll the static reading instances
        redingsResult.addAll(pollReadingInstances(staticReadingInstances));

        // poll the dynamic reading instances
        if (initialDynamicReadings.size() > 0) {
            redingsResult.addAll(pollReadingInstances(dynamicReadingInstances));
        }

        // add the parent process values
        redingsResult.addAll(pollParentProcessInstances(parentProcessReadingInstances.values()));

        return redingsResult;
    }

    private List<ReadingBean> pollReadingInstances(
                                                    List<ReadingInstance> readingInstances ) throws Exception {

        List<ReadingBean> redingsResult = new ArrayList<ReadingBean>();

        for (ReadingInstance readingInstance : readingInstances) {
            float value = readingInstance.poll();

            ReadingBean newResult = readingInstance.getNewCopy();
            newResult.setValue(String.valueOf(value));
            redingsResult.add(newResult);
        }

        return redingsResult;
    }

    private List<ReadingBean> pollParentProcessInstances(
                                                          Collection<ParentProcessReadingBean> parentProcessReadingInstances ) {

        List<ReadingBean> redingsResult = new ArrayList<ReadingBean>();

        for (ParentProcessReadingBean readingInstance : parentProcessReadingInstances) {
            // get the collected value from all children
            float value = readingInstance.poll();

            ReadingBean newResult = readingInstance.getNewCopy();
            newResult.setValue(String.valueOf(value));
            redingsResult.add(newResult);
        }

        return redingsResult;
    }

    @Override
    public String getDescription() {

        return "ATS System Performance Monitor";
    }
}
