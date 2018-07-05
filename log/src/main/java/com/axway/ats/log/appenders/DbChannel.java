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

import java.util.concurrent.ArrayBlockingQueue;

import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.log.AtsConsoleLogger;
import com.axway.ats.core.utils.TimeUtils;
import com.axway.ats.log.autodb.DbAppenderConfiguration;
import com.axway.ats.log.autodb.DbEventRequestProcessor;
import com.axway.ats.log.autodb.LogEventRequest;
import com.axway.ats.log.autodb.QueueLoggerThread;
import com.axway.ats.log.autodb.TestCaseState;
import com.axway.ats.log.autodb.events.DeleteTestCaseEvent;
import com.axway.ats.log.autodb.events.GetCurrentTestCaseEvent;
import com.axway.ats.log.autodb.events.JoinTestCaseEvent;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.log.autodb.exceptions.DbAppenederException;
import com.axway.ats.log.autodb.model.AbstractLoggingEvent;
import com.axway.ats.log.autodb.model.EventRequestProcessorListener;

/**
 * A channel which sends all logging events to a single test case.
 * It receives events from log4j appenders.
 */
public class DbChannel {

    /**
     * We must wait for some event to be processed by the logging thread.
     * This is the time we wait for event processing.
     */
    private static final long                   EVENT_WAIT_TIMEOUT        = 60 * 1000;
    private static final long                   EVENT_WAIT_LONG_TIMEOUT   = 15 * 60 * 1000;

    /**
     * Here we are caching the state of the currently executed test case. This
     * way we do not need to go through the queue(which is in another thread)
     */
    public TestCaseState                        testCaseState;

    /**
     * The class which will process the logging requests
     */
    public DbEventRequestProcessor              eventProcessor;

    /**
     * the logger thread
     */
    private QueueLoggerThread                   queueLogger;

    /**
     * The appender's data for the current thread
     */
    private ArrayBlockingQueue<LogEventRequest> queue;

    /**
     * The configuration for this appender
     */
    private DbAppenderConfiguration             appenderConfig;

    /**
     * When true - we dump info about the usage of the events queue. It is
     * targeted as a debug tool when cannot sent the events to the DB fast
     * enough.
     */
    private boolean                             isMonitoringEventsQueue;
    private long                                lastQueueCapacityTick;

    /**
     * Keeps track what was the minimum value of the remaining queue capacity;
     * */
    private int                                 minRemainingQueueCapacity = -1;

    /**
     * The Test Executor time is the leading time.
     * The time offset value here keeps the time difference between Test Executor and a particular Agent.
     * So the offset will be zero on Test Executor side (where ActiveDbAppender is used) and
     * probably different then zero on the Agent side (where PassiveDbAppender is used)
     */
    private long                                timeOffset                = 0;

    private AtsConsoleLogger                    atsConsoleLogger;

    /* 
     * Sometimes the main thread needs to wait until the logger thread has processed the log event.
     * We use this mutex for synchronization aid. 
     */
    private Object                              listenerMutex             = new Object();

    public DbChannel( DbAppenderConfiguration appenderConfig ) {

        this.appenderConfig = appenderConfig;

        testCaseState = new TestCaseState();

        // the logging queue
        queue = new ArrayBlockingQueue<LogEventRequest>( appenderConfig.getMaxNumberLogEvents() );

        isMonitoringEventsQueue = AtsSystemProperties.getPropertyAsBoolean( AtsSystemProperties.LOG__MONITOR_EVENTS_QUEUE,
                                                                            false );
    }

    protected void initialize( AtsConsoleLogger atsConsoleLogger, Layout layout,
                               boolean attachRequestProcessorListener ) {

        this.atsConsoleLogger = atsConsoleLogger;

        EventRequestProcessorListener listener = null;
        if( attachRequestProcessorListener ) {
            listener = new SimpleEventRequestProcessorListener( listenerMutex );
        }

        // create new event processor
        try {
            eventProcessor = new DbEventRequestProcessor( appenderConfig, layout, listener,
                                                          appenderConfig.isBatchMode() );
        } catch( DatabaseAccessException e ) {
            throw new RuntimeException( "Unable to create DB event processor", e );
        }

        // start the logging thread
        initializeLoggerThread();
    }

    public void close() {

        // When the appender is unloaded, terminate the logging thread
        if( queueLogger != null ) {
            queueLogger.interrupt();
        }
    }

    /**
     * All logging events are processed by this method
     * @param event
     */
    protected void append( LoggingEvent event ) {

        // All events from all threads come into here
        long eventTimestamp;
        if( event instanceof AbstractLoggingEvent ) {
            eventTimestamp = ( ( AbstractLoggingEvent ) event ).getTimestamp();
        } else {
            eventTimestamp = System.currentTimeMillis();
        }
        LogEventRequest packedEvent = new LogEventRequest( Thread.currentThread().getName(), // Remember which thread this event belongs to
                                                           event, eventTimestamp ); // Remember the event time

        if( !queueLogger.isAlive() ) {
            initializeLoggerThread();
        }

        if( event instanceof AbstractLoggingEvent ) {
            AbstractLoggingEvent dbLoggingEvent = ( AbstractLoggingEvent ) event;
            switch( dbLoggingEvent.getEventType() ){

                /* NEXT EVENTS HAPPEN ON TEST EXECUTOR SIDE */
                case START_TEST_CASE: {

                    // on Test Executor side we block until the test case start is committed in the DB
                    waitForEventToBeExecuted( packedEvent, dbLoggingEvent, true );

                    // remember the test case id, which we will later pass to ATS agent
                    testCaseState.setTestcaseId( eventProcessor.getTestCaseId() );

                    // clear last testcase id
                    testCaseState.clearLastExecutedTestcaseId();

                    //this event has already been through the queue
                    return;
                }
                case END_TEST_CASE: {

                    // on Test Executor side we block until the test case start is committed in the DB
                    waitForEventToBeExecuted( packedEvent, dbLoggingEvent, true );

                    // remember the last executed test case id
                    testCaseState.setLastExecutedTestcaseId( testCaseState.getTestcaseId() );

                    // clear test case id
                    testCaseState.clearTestcaseId();
                    // now pass the event to the queue
                    return;
                }
                case GET_CURRENT_TEST_CASE_STATE: {
                    // get current test case id which will be passed to ATS agent
                    ( ( GetCurrentTestCaseEvent ) event ).setTestCaseState( testCaseState );

                    //this event should not go through the queue
                    return;
                }
                case START_RUN:

                    /* We synchronize the run start:
                     *      Here we make sure we are able to connect to the log DB.
                     *      We also check the integrity of the DB schema.
                     * If we fail here, it does not make sense to run tests at all
                     */
                    atsConsoleLogger.info( "Waiting for " + event.getClass().getSimpleName()
                                           + " event completion" );

                    /** disable root logger's logging in order to prevent deadlock **/
                    Level level = Logger.getRootLogger().getLevel();
                    Logger.getRootLogger().setLevel( Level.OFF );

                    AtsConsoleLogger.level = level;

                    waitForEventToBeExecuted( packedEvent, dbLoggingEvent, false );
                    //this event has already been through the queue

                    /*Revert Logger's level*/
                    Logger.getRootLogger().setLevel( level );
                    AtsConsoleLogger.level = null;

                    return;
                case END_RUN: {
                    /* We synchronize the run end.
                     * This way if there are remaining log events in the Test Executor's queue,
                     * the JVM will not be shutdown prior to committing all events in the DB, as
                     * the END_RUN event is the last one in the queue
                     */
                    atsConsoleLogger.info( "Waiting for " + event.getClass().getSimpleName()
                                           + " event completion" );

                    /** disable root logger's logging in order to prevent deadlock **/
                    level = Logger.getRootLogger().getLevel();
                    Logger.getRootLogger().setLevel( Level.OFF );

                    AtsConsoleLogger.level = level;

                    waitForEventToBeExecuted( packedEvent, dbLoggingEvent, true );

                    /*Revert Logger's level*/
                    Logger.getRootLogger().setLevel( level );
                    AtsConsoleLogger.level = null;

                    //this event has already been through the queue
                    return;
                }
                case DELETE_TEST_CASE: {
                    // tell the thread on the other side of the queue, that this test case is to be deleted
                    // on first chance
                    eventProcessor.requestTestcaseDeletion( ( ( DeleteTestCaseEvent ) dbLoggingEvent ).getTestCaseId() );
                    // this event is not going through the queue
                    return;
                }

                /* NEXT EVENTS HAPPEN ON ATS AGENT SIDE */
                case JOIN_TEST_CASE: {
                    // remember test case id
                    testCaseState.setTestcaseId( ( ( JoinTestCaseEvent ) event ).getTestCaseState()
                                                                                .getTestcaseId() );
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

        passEventToLoggerQueue( packedEvent );
    }

    /**
     * Starts the logger thread which process all events from the logger queue.
     * 
     * This method is used to:
     *      1. Start the logger thread for first time
     *          This is a normal situation when instantiating this DB channel
     *          
     *      2. If the logger thread has exited, create a new one and start it
     *          On LEAVE TESTCASE EVENT, we exit the logger thread as we suppose it will never be needed again. 
     *          But in case this channel is used again by the next test(can happen with parallel tests),
     *          we know the DB channel is used, the logger queue gets filled with more events(from the next test)
     *          and we need to start processing the events again, so we create a new thread for this.
     */
    private void initializeLoggerThread() {

        // create the logging thread and start it
        queueLogger = new QueueLoggerThread( queue, eventProcessor, appenderConfig.isBatchMode() );
        queueLogger.setDaemon( true );
        queueLogger.start();
    }

    public void passEventToLoggerQueue( LogEventRequest packedEvent ) {

        // Events on both Test Executor and Agent sides are processed here.

        // Events on Agent get their timestamps aligned with Test Executor time.
        if( timeOffset != 0 ) {
            packedEvent.applyTimeOffset( timeOffset );
        }

        if( isMonitoringEventsQueue ) {
            // Tell the user how many new events can be placed in the queue.
            // Do this every second.
            long newTick = System.currentTimeMillis();
            if( newTick - lastQueueCapacityTick > 1000 ) {
                if( minRemainingQueueCapacity == -1 ) {
                    minRemainingQueueCapacity = queue.remainingCapacity();
                } else {
                    minRemainingQueueCapacity = Math.min( minRemainingQueueCapacity,
                                                          queue.remainingCapacity() );
                }
                atsConsoleLogger.info( "Remaining queue capacity is " + queue.remainingCapacity() + " out of "
                                       + ( queue.remainingCapacity() + queue.size() )
                                       + ". Bottom remaining capacity is " + minRemainingQueueCapacity );
                lastQueueCapacityTick = newTick;
            }
        }

        // this thread passes the events to the queue,
        // while another thread is reading them on the other side
        try {
            queue.add( packedEvent );
        } catch( IllegalStateException ex ) {
            if( queue.remainingCapacity() < 1 ) {
                throw new IllegalStateException( "There are too many messages queued"
                                                 + " for TestExplorer DB logging. Decrease messages count"
                                                 + " by lowering effective log4j severity or check whether"
                                                 + " connection to DB is too slow", ex );
            } else {
                throw ex;
            }
        }
    }

    /**
     * Here we block the test execution until this event gets executed.
     * If this event fail, we will abort the execution of the tests.
     *
     * @param packedEvent
     * @param event
     */
    public void waitForEventToBeExecuted( LogEventRequest packedEvent, LoggingEvent event,
                                          boolean waitMoreTime ) {

        synchronized( listenerMutex ) {

            //we need to wait for the event to be handled
            queue.add( packedEvent );

            try {

                // Start waiting and release the lock. The queue processing thread will notify us after the event is
                // handled or if an exception occurs. In case handling the event hangs - we put some timeout
                long startTime = System.currentTimeMillis();
                long timeout = EVENT_WAIT_TIMEOUT;
                if( waitMoreTime ) {
                    timeout = EVENT_WAIT_LONG_TIMEOUT;
                }

                listenerMutex.wait( timeout );

                if( System.currentTimeMillis() - startTime > timeout - 100 ) {
                    atsConsoleLogger.warn( "The expected " + event.getClass().getSimpleName()
                                           + " logging event did not complete in " + timeout + " ms" );
                }
            } catch( InterruptedException ie ) {
                throw new DbAppenederException( TimeUtils.getFormattedDateTillMilliseconds() + ": "
                                                + "Main thread interrupted while waiting for event "
                                                + event.getClass().getSimpleName(), ie );
            }
        }

        //check for exceptions - if they are none, then we are good to go
        checkForExceptions();
    }

    private synchronized void checkForExceptions() {

        Throwable loggingExceptionWrraper = queueLogger.readLoggingException();
        if( loggingExceptionWrraper != null ) {
            Throwable loggingException = loggingExceptionWrraper.getCause();
            //re-throw the exception in the main thread
            if( loggingException instanceof RuntimeException ) {
                throw ( RuntimeException ) loggingException;
            } else {
                throw new RuntimeException( loggingException.getMessage(), loggingException );
            }
        }
    }

    /**
     * @return the current size of the logging queue
     */
    public int getNumberPendingLogEvents() {

        return queue.size();
    }

    public void calculateTimeOffset( long executorTimestamp ) {

        this.timeOffset = ( System.currentTimeMillis() - executorTimestamp );
    }

    public EventRequestProcessorListener getEventRequestProcessorListener() {

        return new SimpleEventRequestProcessorListener( listenerMutex );
    }

    /**
     * Listener to be notified when events are processed
     */
    private class SimpleEventRequestProcessorListener implements EventRequestProcessorListener {

        private Object listenerMutex;

        SimpleEventRequestProcessorListener( Object listenerMutex ) {
            this.listenerMutex = listenerMutex;
        }

        public void onRunStarted() {

            synchronized( listenerMutex ) {
                listenerMutex.notifyAll();
            }
        }

        public void onRunFinished() {

            synchronized( listenerMutex ) {
                listenerMutex.notifyAll();
            }
        }

        public void onTestcaseStarted() {

            synchronized( listenerMutex ) {
                listenerMutex.notifyAll();
            }
        }

        public void onTestcaseFinished() {

            synchronized( listenerMutex ) {
                listenerMutex.notifyAll();
            }
        }
    }
}
