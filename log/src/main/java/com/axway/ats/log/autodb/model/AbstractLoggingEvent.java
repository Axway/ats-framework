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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.log.autodb.exceptions.IncorrectProcessorStateException;
import com.axway.ats.log.autodb.exceptions.IncorrectScenarioTypeException;
import com.axway.ats.log.autodb.logqueue.EventProcessorState;
import com.axway.ats.log.autodb.logqueue.LifeCycleState;
import com.axway.ats.log.model.SystemLogLevel;

/**
 * This is the base class for all database-related logging events
 */
@SuppressWarnings( "serial")
public abstract class AbstractLoggingEvent extends Log4jLogEvent {

    /**
     * Type of the event
     */
    private final LoggingEventType eventType;

    /*
     * Event time
     */
    private long                   timestamp;
    
    private Logger logger;

    /**
     * Constructor
     * 
     * @param loggerFQCN this is the fully qualified class name of the logger
     * @param logger the logger which logged the event
     * @param message the message for the event
     * @param eventType type of the event
     */
    public AbstractLoggingEvent( String loggerFQCN,
                                 Logger logger,
                                 String message,
                                 LoggingEventType eventType ) {

        //super(loggerFQCN, logger, SystemLogLevel.SYSTEM, message, null);
        super(logger.getName(), null, loggerFQCN, SystemLogLevel.SYSTEM, new SimpleMessage(message), null, null);

        this.logger = logger;
        this.eventType = eventType;
        this.timestamp = System.currentTimeMillis();

        cacheSourceLocation();

    }

    /**
     * Constructor
     * 
     * @param loggerFQCN this is the fully qualified class name of the logger
     * @param logger the logger which logged the event
     * @param level the level of this event
     * @param message the message for the event
     * @param trowable the exception associated with this event
     * @param eventType type of the event
     */
    public AbstractLoggingEvent( String loggerFQCN,
                                 Logger logger,
                                 Level level,
                                 String message,
                                 Throwable trowable,
                                 LoggingEventType eventType ) {

        super(logger.getName(), null, loggerFQCN, SystemLogLevel.SYSTEM, new SimpleMessage(message), null, trowable);

        this.logger = logger;
        this.eventType = eventType;
        this.timestamp = System.currentTimeMillis();

        cacheSourceLocation();
    }

    private void cacheSourceLocation() {

        if (AtsSystemProperties.getPropertyAsBoolean(AtsSystemProperties.LOG__CACHE_EVENTS_SOURCE_LOCATION, false)) {
            // this will specify that this event should have a source information
            this.setIncludeLocation(true);
            // this call will cache the source location of the event
            this.getSource();
        }

    }

    public LoggingEventType getEventType() {

        return eventType;
    }

    public long getTimestamp() {

        return timestamp;
    }

    /**
     * Check if this event can be processed if the processor
     * is in the given state
     * 
     * @param state the current appender state
     * @throws IncorrectProcessorStateException if the state is incorrect 
     * @throws IncorrectScenarioTypeException if the scenario type is incorrect
     */
    public void checkIfCanBeProcessed(
                                       EventProcessorState state ) throws IncorrectProcessorStateException,
                                                                   IncorrectScenarioTypeException {

        LifeCycleState expectedState = getExpectedLifeCycleState(state.getLifeCycleState());
        LifeCycleState actualState = state.getLifeCycleState();
        if ( (expectedState != null)) {
            if (expectedState == LifeCycleState.ATLEAST_RUN_STARTED) {
                if (actualState == LifeCycleState.INITIALIZED) {
                    throw new IncorrectProcessorStateException("Cannot execute event "
                                                               + this.getClass().getSimpleName()
                                                               + " at this time as run is not yet started",
                                                               expectedState,
                                                               actualState);
                }
            } else if (expectedState == LifeCycleState.ATLEAST_TESTCASE_STARTED) {
                if (actualState != LifeCycleState.ATLEAST_TESTCASE_STARTED
                    && actualState != LifeCycleState.TEST_CASE_STARTED) {
                    throw new IncorrectProcessorStateException("Cannot execute event "
                                                               + this.getClass().getSimpleName()
                                                               + " at this time as testcase is not yet started",
                                                               expectedState,
                                                               actualState);
                }
            } else if (expectedState == LifeCycleState.ATLEAST_SUITE_STARTED) {
                if (actualState == LifeCycleState.INITIALIZED
                    || actualState == LifeCycleState.ATLEAST_RUN_STARTED
                    || actualState == LifeCycleState.RUN_STARTED) {
                    throw new IncorrectProcessorStateException("Cannot execute event "
                                                               + this.getClass().getSimpleName()
                                                               + " at this time as suite is not yet started",
                                                               expectedState, actualState);
                }
            }
            // strict expectations about the state
            else if (expectedState != actualState) {
                throw new IncorrectProcessorStateException("Cannot execute event "
                                                           + this.getClass().getSimpleName()
                                                           + " at this time", expectedState, actualState);
            }
        }
    }
    
    public Logger getLogger() {
        
        return logger;
    }
    
    

    /**
     * Get the expected lifecycle state of the processor, for this event
     * 
     * @return the expected lifecycle state
     */
    protected abstract LifeCycleState getExpectedLifeCycleState(
                                                                 LifeCycleState state );
}
