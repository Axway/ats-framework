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
package com.axway.ats.agent.webapp.restservice.model.pojo;

public class StartMonitoringPojo extends BasePojo {

    private int  pollingInterval;
    private long startTimestamp;

    public StartMonitoringPojo() {

    }

    public StartMonitoringPojo( int pollingInterval,
                                long startTimestamp ) {
        this.pollingInterval = pollingInterval;
        this.startTimestamp = startTimestamp;
    }

    public int getPollingInterval() {

        return pollingInterval;
    }

    public void setPollingInterval(
                                    int pollingInterval ) {

        this.pollingInterval = pollingInterval;
    }

    public long getStartTimestamp() {

        return startTimestamp;
    }

    public void setStartTimestamp(
                                   long startTimestamp ) {

        this.startTimestamp = startTimestamp;
    }

}
