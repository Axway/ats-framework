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

import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.core.utils.TimeUtils;
import com.axway.ats.log.autodb.LogEventRequest;
import com.axway.ats.log.autodb.events.DeleteTestCaseEvent;
import com.axway.ats.log.autodb.events.GetCurrentTestCaseEvent;
import com.axway.ats.log.autodb.exceptions.DbAppenederException;
import com.axway.ats.log.autodb.model.AbstractLoggingEvent;
import com.axway.ats.log.autodb.model.EventRequestProcessorListener;

/**
 * This appender is capable of arranging the database storage and storing messages into it.
 * It works on the Test Executor side.
 */
public class ActiveDbAppender extends AbstractDbAppender {

    /**
     * We must wait for some event to be processed by the logging thread.
     * This is the time we wait for event processing.
     */
    private static long             EVENT_WAIT_TIMEOUT                       = 60 * 1000;
    private static long             EVENT_WAIT_LONG_TIMEOUT                  = 15 * 60 * 1000;
    private static ActiveDbAppender instance                                 = null;

    /** enables/disabled logging of messages from @BeforeXXX and @AfterXXX annotated Java methods **/
    public static boolean           isBeforeAndAfterMessagesLoggingSupported = false;

    /**
     * Constructor
     */
    public ActiveDbAppender() {

        super();

    }

    @Override
    protected EventRequestProcessorListener getEventRequestProcessorListener() {

        return new SimpleEventRequestProcessorListener();
    }

    /* (non-Javadoc)
     * @see org.apache.log4j.AppenderSkeleton#append(org.apache.log4j.spi.LoggingEvent)
     */
    @Override
    protected void append( LoggingEvent event ) {

        // All events from all threads come into here
        long eventTimestamp;
        if (event instanceof AbstractLoggingEvent) {
            eventTimestamp = ((AbstractLoggingEvent) event).getTimestamp();
        } else {
            eventTimestamp = System.currentTimeMillis();
        }
        LogEventRequest packedEvent = new LogEventRequest(Thread.currentThread().getName(), // Remember which thread this event belongs to
                                                          event, eventTimestamp); // Remember the event time

        if (event instanceof AbstractLoggingEvent) {
            AbstractLoggingEvent dbLoggingEvent = (AbstractLoggingEvent) event;
            switch (dbLoggingEvent.getEventType()) {

                case START_TEST_CASE: {
                    // on Test Executor side we block until the test case start is committed in the DB
                    waitForEventToBeExecuted(packedEvent, dbLoggingEvent, true);

                    // remember the test case id, which we will later pass to ATS agent
                    testCaseState.setTestcaseId(eventProcessor.getTestCaseId());

                    //this event has already been through the queue
                    return;
                }
                case END_TEST_CASE: {
                    // clear test case id
                    testCaseState.clearTestcaseId();
                    // now pass the event to the queue
                    break;
                }
                case GET_CURRENT_TEST_CASE_STATE: {
                    // get current test case id which will be passed to ATS agent
                    ((GetCurrentTestCaseEvent) event).setTestCaseState(testCaseState);

                    //this event should not go through the queue
                    return;
                }
                case START_RUN:
                    /* We synchronize the run start:
                     *      Here we make sure we are able to connect to the log DB.
                     *      We also check the integrity of the DB schema.
                     * If we fail here, it does not make sense to run tests at all
                     */
                    System.out.println(TimeUtils.getFormattedDateTillMilliseconds()
                                       + StringUtils.ATS_CONSOLE_MESSAGE_PREFIX + "Waiting for "
                                       + event.getClass().getSimpleName()
                                       + " event completion");

                    // create the queue logging thread and the DbEventRequestProcessor
                    if (queueLogger == null) {
                        initializeDbLogging();
                    }

                    waitForEventToBeExecuted(packedEvent, dbLoggingEvent, false);
                    //this event has already been through the queue
                    return;
                case END_RUN: {
                    /* We synchronize the run end.
                     * This way if there are remaining log events in the Test Executor's queue,
                     * the JVM will not be shutdown prior to committing all events in the DB, as
                     * the END_RUN event is the last one in the queue
                     */
                    System.out.println(TimeUtils.getFormattedDateTillMilliseconds()
                                       + StringUtils.ATS_CONSOLE_MESSAGE_PREFIX + "Waiting for "
                                       + event.getClass().getSimpleName()
                                       + " event completion");

                    waitForEventToBeExecuted(packedEvent, dbLoggingEvent, true);

                    //this event has already been through the queue
                    return;
                }
                case DELETE_TEST_CASE: {
                    // tell the thread on the other side of the queue, that this test case is to be deleted
                    // on first chance
                    eventProcessor.requestTestcaseDeletion( ((DeleteTestCaseEvent) dbLoggingEvent).getTestCaseId());
                    // this event is not going through the queue
                    return;
                }
                default:
                    // do nothing about this event
                    break;
            }
        }

        passEventToLoggerQueue(packedEvent);
    }

    @Override
    public GetCurrentTestCaseEvent getCurrentTestCaseState( GetCurrentTestCaseEvent event ) {

        testCaseState.setRunId(eventProcessor.getRunId());
        // get current test case id which will be passed to ATS agent
        event.setTestCaseState(testCaseState);
        return event;
    }

    /**
     * Here we block the test execution until this event gets executed.
     * If this event fail, we will abort the execution of the tests.
     *
     * @param packedEvent
     * @param event
     */
    private void waitForEventToBeExecuted( LogEventRequest packedEvent, LoggingEvent event,
                                           boolean waitMoreTime ) {

        synchronized (this) {

            //we need to wait for the event to be handled
            queue.add(packedEvent);

            try {

                // Start waiting and release the lock. The queue processing thread will notify us after the event is
                // handled or if an exception occurs. In case handling the event hangs - we put some timeout
                long startTime = System.currentTimeMillis();
                long timeout = EVENT_WAIT_TIMEOUT;
                if (waitMoreTime) {
                    timeout = EVENT_WAIT_LONG_TIMEOUT;
                }
                wait(timeout);
                if (System.currentTimeMillis() - startTime > timeout - 100) {
                    System.out.println(TimeUtils.getFormattedDateTillMilliseconds()
                                       + StringUtils.ATS_CONSOLE_MESSAGE_PREFIX + "The expected "
                                       + event.getClass().getSimpleName()
                                       + " logging event did not complete in " + timeout + " ms");
                }
            } catch (InterruptedException ie) {
                throw new DbAppenederException(TimeUtils.getFormattedDateTillMilliseconds()
                                               + StringUtils.ATS_CONSOLE_MESSAGE_PREFIX
                                               + "Main thread interrupted while waiting for event "
                                               + event.getClass().getSimpleName(), ie);
            }
        }

        //check for exceptions - if they are none, then we are good to go
        checkForExceptions();

        //this event has already been through the queue
        return;
    }

    public String getHost() {

        return appenderConfig.getHost();
    }

    public void setHost( String host ) {

        appenderConfig.setHost(host);
    }

    public String getDatabase() {

        return appenderConfig.getDatabase();
    }

    public void setDatabase( String database ) {

        appenderConfig.setDatabase(database);
    }

    public String getUser() {

        return appenderConfig.getUser();
    }

    public void setUser( String user ) {

        appenderConfig.setUser(user);
    }

    public String getPassword() {

        return appenderConfig.getPassword();
    }

    public void setPassword( String password ) {

        appenderConfig.setPassword(password);
    }

    /**
     * This method doesn't create a new instance,
     * but returns the already created one (from log4j) or null if there is no such.
     *
     * @return the current DB appender instance
     */
    @SuppressWarnings( "unchecked")
    public static ActiveDbAppender getCurrentInstance() {

        if (instance == null) {
            Enumeration<Appender> appenders = Logger.getRootLogger().getAllAppenders();
            while (appenders.hasMoreElements()) {
                Appender appender = appenders.nextElement();

                if (appender instanceof ActiveDbAppender) {
                    instance = (ActiveDbAppender) appender;
                }
            }
        }

        return instance;
    }

    private synchronized void checkForExceptions() {

        Throwable loggingExceptionWrraper = queueLogger.readLoggingException();
        if (loggingExceptionWrraper != null) {
            Throwable loggingException = loggingExceptionWrraper.getCause();
            //re-throw the exception in the main thread
            if (loggingException instanceof RuntimeException) {
                throw(RuntimeException) loggingException;
            } else {
                throw new RuntimeException(loggingException.getMessage(), loggingException);
            }
        }
    }

    /**
     * Listener to be notified when events are processed
     */
    private class SimpleEventRequestProcessorListener implements EventRequestProcessorListener {

        public void onRunStarted() {

            synchronized (ActiveDbAppender.this) {
                ActiveDbAppender.this.notifyAll();
            }
        }

        public void onRunFinished() {

            synchronized (ActiveDbAppender.this) {
                ActiveDbAppender.this.notifyAll();
            }
        }

        public void onTestcaseStarted() {

            synchronized (ActiveDbAppender.this) {
                ActiveDbAppender.this.notifyAll();
            }
        }
    }

}
