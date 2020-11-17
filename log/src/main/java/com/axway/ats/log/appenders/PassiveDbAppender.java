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

package com.axway.ats.log.appenders;

import java.util.Enumeration;

import org.apache.log4j.Appender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import com.axway.ats.core.threads.ThreadsPerCaller;
import com.axway.ats.log.autodb.TestCaseState;
import com.axway.ats.log.autodb.events.GetCurrentTestCaseEvent;
import com.axway.ats.log.autodb.events.JoinTestCaseEvent;
import com.axway.ats.log.autodb.logqueue.DbEventRequestProcessor;
import com.axway.ats.log.autodb.logqueue.LogEventRequest;
import com.axway.ats.log.autodb.model.AbstractLoggingEvent;
import com.axway.ats.log.autodb.model.EventRequestProcessorListener;

/**
 * This appender is capable of arranging the database storage and storing messages into it.
 * This appender works on the ATS Agent's side.
 * 
 * It is expected to:
 *  - JOIN to an existing test case(the Test Executor passes the testcase id)
 *  - INSERT into the testcase messages, statistics etc.
 *  - LEAVE testcase when it is over
 */
public class PassiveDbAppender extends AbstractDbAppender {

    /* 
     * The caller this appender is serving.
     * We are calling this constructor in a way which guarantees the provided caller is not null
     */
    private String caller;

    /**
     * Constructor
     */
    public PassiveDbAppender( String caller ) {

        super();

        this.caller = caller;
    }

    @Override
    protected EventRequestProcessorListener getEventRequestProcessorListener() {

        return null;
    }

    public String getCaller() {

        return caller;
    }

    @Override
    public void activateOptions() {

        super.activateOptions();

        // create the queue logging thread and the DbEventRequestProcessor
        initializeDbLogging();
    }

    /* (non-Javadoc)
     * @see org.apache.log4j.AppenderSkeleton#append(org.apache.log4j.spi.LoggingEvent)
     */
    @Override
    protected void append(
                           LoggingEvent event ) {

        if (!doWeServiceThisCaller()) {
            return;
        }

        if (event instanceof AbstractLoggingEvent) {
            AbstractLoggingEvent dbLoggingEvent = (AbstractLoggingEvent) event;
            switch (dbLoggingEvent.getEventType()) {

                case JOIN_TEST_CASE: {
                    // remember test case id
                    testCaseState.setTestcaseId( ((JoinTestCaseEvent) event).getTestCaseState()
                                                                            .getTestcaseId());
                    break;
                }
                case LEAVE_TEST_CASE: {
                    // clear test case id
                    testCaseState.clearTestcaseId();
                    break;
                }
                default:
                    // do nothing about this event
                    break;
            }
        }

        // All events from all threads come into here
        long eventTimestamp;
        if (event instanceof AbstractLoggingEvent) {
            eventTimestamp = ((AbstractLoggingEvent) event).getTimestamp();
        } else {
            eventTimestamp = System.currentTimeMillis();
        }
        LogEventRequest packedEvent = new LogEventRequest(Thread.currentThread().getName(), // Remember which thread this event belongs to
                                                          event,
                                                          eventTimestamp); // Remember the event time

        passEventToLoggerQueue(packedEvent);
    }

    public GetCurrentTestCaseEvent getCurrentTestCaseState(
                                                            GetCurrentTestCaseEvent event ) {

        if (!doWeServiceThisCaller()) {
            return null;
        } else {
            event.setTestCaseState(testCaseState);
            return event;
        }
    }

    private boolean doWeServiceThisCaller() {

        final String caller = ThreadsPerCaller.getCaller();
        if (caller == null) {
            // unknown caller, skip this event
            return false;
        }

        if (!this.caller.equals(caller)) {
            // this appender is not serving this caller, skip this event
            return false;
        }

        return true;
    }

    /**
     * This method doesn't create a new instance,
     * but returns the already created one or null if there is no such.
     *
     * @return the current DB appender instance
     */
    @SuppressWarnings( "unchecked")
    public static PassiveDbAppender getCurrentInstance(
                                                        String caller ) {

        Enumeration<Appender> appenders = LogManager.getRootLogger().getAllAppenders();
        while (appenders.hasMoreElements()) {
            Appender appender = appenders.nextElement();

            if (appender instanceof PassiveDbAppender) {
                PassiveDbAppender passiveAppender = (PassiveDbAppender) appender;
                if (passiveAppender.getCaller().equals(caller)) {
                    return passiveAppender;
                }
            }
        }

        return null;
    }

    /**
     * Manually set the current testcaseState
     */
    public void setTestcaseState(
                                  TestCaseState testCaseState ) {

        this.testCaseState = testCaseState;
    }

    public DbEventRequestProcessor getDbEventRequestProcessor() {

        return this.eventProcessor;
    }
}
