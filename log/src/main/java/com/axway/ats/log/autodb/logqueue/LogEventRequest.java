/*
 * Copyright 2017-2019 Axway Software
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
package com.axway.ats.log.autodb.logqueue;

import org.apache.logging.log4j.core.LogEvent;

public class LogEventRequest {

    private String   threadName;
    private LogEvent event;
    private long     timestamp;

    public LogEventRequest( String threadName,
                            LogEvent event,
                            long timestamp ) {

        this.threadName = threadName;
        this.event = event;
        this.timestamp = timestamp;
    }

    public String getThreadName() {

        return threadName;
    }

    public LogEvent getEvent() {

        return event;
    }

    public long getTimestamp() {

        return timestamp;
    }

    /**
    
     * Used to align timestamp on events occuring on the Agent side with Test Executor time.
    
     */
    public void applyTimeOffset(
                                 long offset ) {

        this.timestamp += offset;
    }
}
