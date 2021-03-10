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
package com.axway.ats.log.autodb.events;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.log.autodb.logqueue.LifeCycleState;
import com.axway.ats.log.autodb.model.AbstractLoggingEvent;
import com.axway.ats.log.autodb.model.LoggingEventType;

@SuppressWarnings( "serial")
public class StartSuiteEvent extends AbstractLoggingEvent {

    private final String suiteName;
    private final String packageName;

    public StartSuiteEvent( String loggerFQCN,
                            Logger logger,
                            String suiteName,
                            String packageName ) {

        super(loggerFQCN, logger, "Start suite '" + suiteName + "'", LoggingEventType.START_SUITE);

        this.suiteName = suiteName;
        this.packageName = packageName;
    }

    public StartSuiteEvent( String loggerFQCN,
                            Logger logger,
                            String message,
                            String suiteName,
                            String packageName,
                            LoggingEventType loggingEventType ) {

        super(loggerFQCN, logger, message, loggingEventType);

        this.suiteName = suiteName;
        this.packageName = packageName;
    }

    public String getSuiteName() {

        return this.suiteName;
    }

    public String getPackage() {

        return this.packageName;
    }

    @Override
    protected LifeCycleState getExpectedLifeCycleState(
                                                        LifeCycleState state ) {

        return LifeCycleState.RUN_STARTED;
    }
}
