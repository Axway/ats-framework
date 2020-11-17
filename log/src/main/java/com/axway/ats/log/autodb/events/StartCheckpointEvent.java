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
package com.axway.ats.log.autodb.events;

import java.util.Calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.log.autodb.model.LoggingEventType;
import com.axway.ats.log.autodb.model.TestCaseLoggingEvent;

@SuppressWarnings( "serial")
public class StartCheckpointEvent extends TestCaseLoggingEvent {

    private long   startTimestamp;
    private String name;
    private String transferUnit;
    private String threadName;

    public StartCheckpointEvent( String fqnOfCategoryClass,
                                 Logger logger,
                                 String name,
                                 String transferUnit,
                                 String threadName ) {

        this(fqnOfCategoryClass, logger, name, transferUnit, threadName, Calendar.getInstance()
                                                                                 .getTimeInMillis());
    }

    public StartCheckpointEvent( String fqnOfCategoryClass,
                                 Logger logger,
                                 String name,
                                 String transferUnit,
                                 String threadName,
                                 long startTimestamp ) {

        super(fqnOfCategoryClass,
              logger,
              "Start checkpoint '" + name + "'",
              LoggingEventType.START_CHECKPOINT);

        this.startTimestamp = startTimestamp;
        this.name = name;
        this.transferUnit = transferUnit;
        this.threadName = threadName;
    }

    public long getStartTimestamp() {

        return startTimestamp;
    }

    public String getName() {

        return name;
    }

    /**
     * Get the transfer unit for this checkpoint
     * 
     * @return the transfer unit, null if not set
     */
    public String getTransferUnit() {

        return transferUnit;
    }

    /**
     * @return the name of the thread this checkpoint belongs to
     */
    public String getThread() {

        return threadName;
    }
}
