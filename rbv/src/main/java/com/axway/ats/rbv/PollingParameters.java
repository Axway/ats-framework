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
package com.axway.ats.rbv;

public class PollingParameters {

    private long initialDelay;
    private long pollInterval;
    private int  pollAttempts;

    public PollingParameters( long initialDelay,
                              long pollInterval,
                              int pollAttempts ) {

        this.initialDelay = initialDelay;
        this.pollInterval = pollInterval;
        this.pollAttempts = pollAttempts;
    }

    public long getInitialDelay() {

        return initialDelay;
    }

    public void setInitialDelay(
                                 long initialDelay ) {

        this.initialDelay = initialDelay;
    }

    public int getPollAttempts() {

        return pollAttempts;
    }

    public void setPollAttempts(
                                 int pollAttempts ) {

        this.pollAttempts = pollAttempts;
    }

    public long getPollInterval() {

        return pollInterval;
    }

    public void setPollInterval(
                                 long pollInterval ) {

        this.pollInterval = pollInterval;
    }
}
