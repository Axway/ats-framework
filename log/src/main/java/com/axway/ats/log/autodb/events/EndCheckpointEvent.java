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
import com.axway.ats.log.model.CheckpointResult;

/**
 * An event for ending an already started checkpoint
 */
@SuppressWarnings( "serial")
public class EndCheckpointEvent extends TestCaseLoggingEvent {

    private long             endTimestamp;
    private String           name;
    private String           threadName;
    private long             transferSize;
    private CheckpointResult result;

    public EndCheckpointEvent( String fqnOfCategoryClass,
                               Logger logger,
                               String name,
                               String threadName,
                               CheckpointResult result ) {

        this(fqnOfCategoryClass, logger, name, threadName, result, Calendar.getInstance().getTimeInMillis());
    }

    public EndCheckpointEvent( String fqnOfCategoryClass,
                               Logger logger,
                               String name,
                               String threadName,
                               CheckpointResult result,
                               long endTimestamp ) {

        super(fqnOfCategoryClass, logger, "End checkpoint '" + name + "'", LoggingEventType.END_CHECKPOINT);

        this.endTimestamp = endTimestamp;
        this.name = name;
        this.threadName = threadName;
        this.result = result;
    }

    public EndCheckpointEvent( String fqnOfCategoryClass,
                               Logger logger,
                               String name,
                               String threadName,
                               long transferSize,
                               CheckpointResult result ) {

        this(fqnOfCategoryClass, logger, name, threadName, transferSize, result, Calendar.getInstance()
                                                                                         .getTimeInMillis());
    }

    public EndCheckpointEvent( String fqnOfCategoryClass,
                               Logger logger,
                               String name,
                               String threadName,
                               long transferSize,
                               CheckpointResult result,
                               long endTimestamp ) {

        super(fqnOfCategoryClass, logger, "End checkpoint '" + name + "'", LoggingEventType.END_CHECKPOINT);

        this.endTimestamp = endTimestamp;
        this.name = name;
        this.threadName = threadName;
        this.transferSize = transferSize;
        this.result = result;
    }

    /**
     * Get the timestamp at which the event was logged
     * 
     * @return the timestamp at which the event was logged
     */
    public long getEndTimestamp() {

        return endTimestamp;
    }

    /**
     * Get the name of the checkpoint
     * 
     * @return the name
     */
    public String getName() {

        return name;
    }

    /**
     * Get the name of the thread which ended the checkpoint
     * 
     * @return
     */
    public String getThread() {

        return threadName;
    }

    /**
     * Get the transfer size for this checkpoint
     * 
     * @return the transfer size, 0 if not set
     */
    public long getTransferSize() {

        return transferSize;
    }

    /**
     * Get the result of the checkpoint execution
     * 
     * @return the result
     */
    public CheckpointResult getResult() {

        return result;
    }
}
