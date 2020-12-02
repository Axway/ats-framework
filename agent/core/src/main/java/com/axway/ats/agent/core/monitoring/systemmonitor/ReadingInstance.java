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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.ISystemInformation;
import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.exceptions.SystemInformationException;
import com.axway.ats.common.performance.monitor.beans.ParentProcessReadingBean;
import com.axway.ats.common.performance.monitor.beans.SharedReadingBean;
import com.axway.ats.core.monitoring.SystemMonitorDefinitions;

/**
 * Abstraction of a reading instance.
 * Each particular implementation must implement its own polling logic
 *
 *
 *
 * This bean lives in the environment of the monitoring service only.
 * It is never serialized back to the Test Executor.

 * We send back a FullReadingBean when this instance is new for the Test Executor

 * We send back a BasicReadingBean when the Test Executor knows it and we are
 * just passing a new poll value
 */
public abstract class ReadingInstance extends SharedReadingBean {

    private static final long        serialVersionUID                        = 1L;

    protected long                   lastLongValue                           = 0L;
    protected long                   lastPollTime;

    private boolean                  newInstanceFlag;

    // a process id, if applicable
    private long                     pid;

    // a reference to the parent process, if applicable
    private ParentProcessReadingBean parentProcess;

    /**
     * Holds sigar last values, needed to check if overflow happens
     *
     * Long[0] - last value returned by Sigar
     * Long[1] - number of overflows
     */
    private Map<String, Long[]>      lastSigarValues                         = new HashMap<String, Long[]>();

    /* Sigar overflows after the following value when measuring:
     *  - IO read/write bytes for local devices
     *  - Network traffic
     */
    private static final long        OVERFLOW_VALUE                          = 2 * (long) Integer.MAX_VALUE; // 4 294 967 294
    // Sigar overflows after the following value when measuring CPU usage for a system process
    public static final long         CPU_PROCESS_OVERFLOW_VALUE              = OVERFLOW_VALUE / 10000;
    // We guess Sigar overflows after the following value when measuring memory page faults for a system process
    // It is not easy to prove in reality this value
    public static final long         MEMORYPAGEFAULTS_PROCESS_OVERFLOW_VALUE = OVERFLOW_VALUE / 10000;

    protected ISystemInformation     systemInfo;

    public ReadingInstance( ISystemInformation systemInfo,
                            String dbId,
                            String monitorClass,
                            String name,
                            String unit,
                            float normalizationFactor ) throws SystemInformationException {

        this(systemInfo, null, dbId, 0, monitorClass, name, unit, null, normalizationFactor);
    }

    /**
     * This constructor is currently used for monitoring processes
     */
    public ReadingInstance( ISystemInformation systemInfo,
                            ParentProcessReadingBean parentProcess,
                            String dbId,
                            long pid,
                            String monitorClass,
                            String name,
                            String unit,
                            Map<String, String> parameters,
                            float normalizationFactor ) throws SystemInformationException {

        super(monitorClass, name, unit, normalizationFactor);

        this.parentProcess = parentProcess;
        this.dbId = Integer.parseInt(dbId);
        this.pid = pid;
        this.systemInfo = systemInfo;

        setParameters(parameters);

        if (parentProcess != null) {
            this.parameters.put(SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_PARENT_NAME,
                                parentProcess.getTheNameOfThisParentProcess());
        }

        lastPollTime = System.currentTimeMillis();

        newInstanceFlag = true;

        init();
    }

    public void init() throws SystemInformationException {

    }

    public long getPid() {

        return pid;
    }

    /**
     * @return the new polled value
     * @throws Exception
     */
    abstract public float poll() throws Exception;

    /**
     * This process is a child of a parent process.
     * Provide the newly retrieved child value
     * @param newValue
     */
    public void addValueToParentProcess(
                                         float newValue ) {

        if (parentProcess != null) {
            parentProcess.addValue(newValue);
        }
    }

    public boolean isNewInstance() {

        return newInstanceFlag;
    }

    public void setNewInstanceFlag(
                                    boolean newInstance ) {

        this.newInstanceFlag = newInstance;
    }

    /**
     * Fix Sigar overflow of its internal counters
     *
     * @param readingName the name of possibly to fail reading
     * @param value the value returned by the Sigar API
     *
     * @return the fixed value
     */
    protected long fixOverflow(
                                String readingName,
                                long value ) {

        return fixOverflow(readingName, value, OVERFLOW_VALUE);
    }

    /**
     * Fix Sigar overflow of its internal counters
     *
     * @param readingName the name of possibly to fail reading
     * @param value the value returned by the Sigar API
     * @param overflowBarrier the overflow number
     *
     * @return the fixed value
     */
    protected long fixOverflow(
                                String readingName,
                                long value,
                                long overflowBarrier ) {

        long lastSigarValue = 0L;
        long overflows = 0L;

        if (this.lastSigarValues.containsKey(readingName)) {
            lastSigarValue = this.lastSigarValues.get(readingName)[0].longValue();
            overflows = this.lastSigarValues.get(readingName)[1].longValue();
        }

        if (value < lastSigarValue && value >= 0) {
            overflows++;
        }

        this.lastSigarValues.put(readingName, new Long[]{ value, overflows });
        return value + overflowBarrier * overflows;
    }

    protected float toFloatWith2DecimalDigits(
                                               double doubleValue ) {

        if (doubleValue >= 0) {
            doubleValue = doubleValue * normalizationFactor;
        }

        // return a float with 2 digits after the decimal point
        return new BigDecimal(doubleValue).setScale(2, BigDecimal.ROUND_DOWN).floatValue();
    }

    /**
     *
     * @return the elapsed time from last poll in milliseconds and sets the last poll time to the current time
     */
    protected long getElapsedTime() {

        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastPollTime;
        lastPollTime = currentTime;
        return elapsedTime;
    }
}
