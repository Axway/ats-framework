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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.log.autodb.model.LoggingEventType;
import com.axway.ats.log.autodb.model.TestCaseLoggingEvent;

/**
 * Event for inserting a system statistic in the log DB
 */
@SuppressWarnings( "serial")
public class InsertSystemStatisticEvent extends TestCaseLoggingEvent {

    private String monitoredMachine;
    private String statisticIds;
    private String statisticValues;
    private long   timestamp;

    /**
     * Constructor
     *
     * @param loggerFQCN
     * @param logger
     * @param monitoredMachine
     * @param statisticIdentifiers
     * @param statisticValues
     * @param timestamp
     */
    public InsertSystemStatisticEvent( String loggerFQCN,
                                       Logger logger,
                                       String monitoredMachine,
                                       String statisticIds,
                                       String statisticValues,
                                       long timestamp ) {

        super(loggerFQCN, logger, "Insert system statistic", LoggingEventType.INSERT_SYSTEM_STAT);

        this.monitoredMachine = monitoredMachine;
        this.statisticIds = statisticIds;
        this.statisticValues = statisticValues;
        this.timestamp = timestamp;
    }

    /**
     * @return the name of the monitored machine
     */
    public String getMonitoredMachine() {

        return monitoredMachine;
    }

    /**
     * @return the DB IDs of the statistics
     */
    public String getStatisticIds() {

        return statisticIds;
    }

    /**
     * @return the timestamp of this statistic
     */
    public long getTimestamp() {

        return timestamp;
    }

    /**
     * @return the values of the statistic
     */
    public String getStatisticValues() {

        return statisticValues;
    }
}
