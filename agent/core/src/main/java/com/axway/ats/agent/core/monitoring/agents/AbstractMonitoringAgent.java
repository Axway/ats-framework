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
package com.axway.ats.agent.core.monitoring.agents;

import com.axway.ats.agent.core.monitoring.exceptions.OperationUnsuccessfulException;

/**
 * The {@link AbstractMonitoringAgent} is responsible for :
 * <ul>
 * <li>keeping track of the monitor state and making sure each operation takes place only in the correct
 * {@link MonitoringAgentState}.</li>
 * <li>managing the value of the poll interval between each reading</li>
 * <ul/>
 */
public abstract class AbstractMonitoringAgent {

    protected int  pollInterval;
    protected long startTimestamp;
    protected long executorTimeOffset;

    /**
     * Initializes this instance of the {@link MonitoringAgent}
     */
    public AbstractMonitoringAgent() {

    }

    protected void setPollInterval(
                                    int pollInterval ) {

        this.pollInterval = pollInterval;
    }

    protected void setExecutorTimeOffset(
                                          long executorTimeOffset ) {

        this.executorTimeOffset = executorTimeOffset;
    }

    protected void setStartTimestamp(
                                      long startTimestamp ) {

        this.startTimestamp = startTimestamp;
    }

    /**
     * Starts all the monitors
     * 
     * @throws OperationUnsuccessfulException
     */
    public abstract void startMonitoring();

    /**
     * Stops all the monitors
     * 
     * @throws OperationUnsuccessfulException
     */
    public abstract void stopMonitoring();

}
