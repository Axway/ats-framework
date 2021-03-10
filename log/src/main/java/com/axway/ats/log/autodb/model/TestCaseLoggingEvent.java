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
package com.axway.ats.log.autodb.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.log.autodb.logqueue.LifeCycleState;

/**
 * This is the base class for all events which expect a test
 * case to be already open
 */
@SuppressWarnings( "serial")
public abstract class TestCaseLoggingEvent extends AbstractLoggingEvent {

    /**
     * @param loggerFQCN
     * @param logger
     * @param message
     * @param eventType
     */
    public TestCaseLoggingEvent( String loggerFQCN,
                                 Logger logger,
                                 String message,
                                 LoggingEventType eventType ) {

        super(loggerFQCN, logger, message, eventType);
    }

    @Override
    protected final LifeCycleState getExpectedLifeCycleState(
                                                              LifeCycleState event ) {

        //all requeable events expect a test case to be started
        return LifeCycleState.TEST_CASE_STARTED;
    }
}
