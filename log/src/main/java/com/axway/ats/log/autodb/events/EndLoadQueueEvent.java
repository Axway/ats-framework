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
import com.axway.ats.log.model.LoadQueueResult;

@SuppressWarnings( "serial")
public class EndLoadQueueEvent extends TestCaseLoggingEvent {

    private String          name;
    private LoadQueueResult result;

    public EndLoadQueueEvent( String loggerFQCN, Logger logger, String name, LoadQueueResult result ) {

        super(loggerFQCN, logger, "End load queue '" + name + "'", LoggingEventType.END_LOADQUEUE);

        this.name = name;
        this.result = result;
    }

    public String getName() {

        return name;
    }

    public LoadQueueResult getResult() {

        return result;
    }
}
