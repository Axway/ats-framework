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

import com.axway.ats.log.autodb.model.CacheableEvent;
import com.axway.ats.log.autodb.model.LoggingEventType;
import com.axway.ats.log.autodb.model.TestCaseLoggingEvent;
import com.axway.ats.log.model.CheckpointResult;

@SuppressWarnings( "serial")
public class InsertCheckpointEvent extends TestCaseLoggingEvent implements CacheableEvent {

    private String           name;
    private long             startTimestamp;
    private long             responseTime;
    private long             transferSize;
    private String           transferUnit;
    private String           threadName;
    private CheckpointResult result;

    public InsertCheckpointEvent( String fqnOfCategoryClass,
                                  Logger logger,
                                  String name,
                                  long startTimestamp,
                                  long responseTime,
                                  long transferSize,
                                  String transferUnit,
                                  String threadName,
                                  CheckpointResult result ) {

        super(fqnOfCategoryClass,
              logger,
              "Insert checkpoint '" + name + "'",
              LoggingEventType.INSERT_CHECKPOINT);

        if (startTimestamp > 0) {
            this.startTimestamp = startTimestamp;
        } else {
            this.startTimestamp = System.currentTimeMillis();
        }
        this.responseTime = responseTime;
        this.name = name;
        this.transferSize = transferSize;
        this.transferUnit = transferUnit;
        this.threadName = threadName;
        this.result = result;
    }

    public long getStartTimestamp() {

        return startTimestamp;
    }

    public long getResponseTime() {

        return responseTime;
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
