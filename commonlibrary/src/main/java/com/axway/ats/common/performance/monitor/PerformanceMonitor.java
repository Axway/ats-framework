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
package com.axway.ats.common.performance.monitor;

import java.util.List;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.performance.monitor.beans.ReadingBean;

/**
 * A Monitor that can be used by the ATS Performance Monitor Service
 * to collect statistical data about a tested system  
 */
@PublicAtsApi
public abstract class PerformanceMonitor {

    private int     pollInterval;

    /**
     * flag that says if the 'first time poll' succeeded
     */
    private boolean initialized = false;

    /**
     * 
     * Called before start polling the monitor for statistical data. 
     * <br>It can be used to take some preparation steps(for example to establish a DB connection)
     * <br><br>
     * The user will provide a list of statistics to the monitor. 
     * If this monitor supports for example 10 statistics, but the user wants just 2 of them - the monitor class
     * is notified to collect information only about the 2 specified statistics.
     * 
     * @param readings list of statistical readings the user wants to get monitored
     * @throws Exception
     */
    @PublicAtsApi
    public abstract void init(
                               ReadingBean[] readings) throws Exception;

    /**
     * Set polling interval in seconds
     * 
     * @param pollInterval the polling interval in seconds
     */
    public void setPollInterval(
                                 int pollInterval) {

        this.pollInterval = pollInterval;
    }

    /**
     * @return the polling interval
     */
    public int getPollInterval() {

        return this.pollInterval;
    }

    public boolean isInitialized() {

        return initialized;
    }

    public void setInitialized() {

        this.initialized = true;
    }

    /**
     * Called only once, then the pollNewData method is called repetitively in intervals specified by the user. 
     * 
     * @return the new data
     * @throws Exception
     */
    @PublicAtsApi
    public abstract List<ReadingBean> pollNewDataForFirstTime() throws Exception;

    /**
     * Called repetitively in intervals specified by the user.
     * 
     * @return the new data
     * @throws Exception
     */
    @PublicAtsApi
    public abstract List<ReadingBean> pollNewData() throws Exception;

    /**
     * Called to do some clean up procedures, for example to release some resources allocated in the init() method
     * 
     * @throws Exception
     */
    @PublicAtsApi
    public abstract void deinit() throws Exception;

    /**
     * @return some human understandable description, it is included in some error messages
     */
    @PublicAtsApi
    public abstract String getDescription();
}
