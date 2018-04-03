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

package com.axway.ats.log.appenders;

import java.util.Enumeration;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import com.axway.ats.core.threads.ThreadsPerCaller;
import com.axway.ats.core.utils.ExecutorUtils;
import com.axway.ats.log.autodb.DbEventRequestProcessor;
import com.axway.ats.log.autodb.TestCaseState;
import com.axway.ats.log.autodb.events.GetCurrentTestCaseEvent;

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

    /**
     * Constructor
     */
    public PassiveDbAppender() {

        super();
    }

    @Override
    public void activateOptions() {

        super.activateOptions();
    }

    /* (non-Javadoc)
     * @see org.apache.log4j.AppenderSkeleton#append(org.apache.log4j.spi.LoggingEvent)
     */
    @Override
    protected void append(
                           LoggingEvent event ) {

        if( ThreadsPerCaller.getCaller() == null ) {
            return;
        }

        // Remember the caller prior passing this event to the logging queue.
        // We use the log4j's map inside, this is some kind of misuse. 
        event.setProperty( ExecutorUtils.ATS_RANDOM_TOKEN, ThreadsPerCaller.getCaller() );

        getDbChannel( null ).append( event );
    }

    @Override
    public GetCurrentTestCaseEvent getCurrentTestCaseState( GetCurrentTestCaseEvent event ) {

        if( ThreadsPerCaller.getCaller() == null ) {
            return null;
        } else {
            event.setTestCaseState( getDbChannel( null ).testCaseState );
            return event;
        }
    }

    /**
     * This method doesn't create a new instance,
     * but returns the already created one or null if there is no such.
     *
     * @return the current DB appender instance
     */
    @SuppressWarnings("unchecked")
    public static PassiveDbAppender getCurrentInstance() {

        Enumeration<Appender> appenders = Logger.getRootLogger().getAllAppenders();
        while( appenders.hasMoreElements() ) {
            Appender appender = appenders.nextElement();

            if( appender instanceof PassiveDbAppender ) {
                PassiveDbAppender passiveAppender = ( PassiveDbAppender ) appender;
                return passiveAppender;
            }
        }

        return null;
    }

    /**
     * Manually set the current testcaseState
     */
    public void setTestcaseState(
                                  TestCaseState testCaseState ) {

        getDbChannel( null ).testCaseState = testCaseState;
    }

    public DbEventRequestProcessor getDbEventRequestProcessor() {

        return getDbChannel( null ).eventProcessor;
    }

    @Override
    protected String getDbChannelKey( LoggingEvent event ) {

        // Works on Agent side
        // Have a channel per caller(Host name + Random key + Thread name)
        return ThreadsPerCaller.getCaller();
    }
}
