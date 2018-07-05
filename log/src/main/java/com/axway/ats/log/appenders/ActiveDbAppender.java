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

<<<<<<< 9eeb7d42a8d5d8bd4b1e44fc93c05f170744823e
import com.axway.ats.core.log.AtsConsoleLogger;
import com.axway.ats.core.utils.TimeUtils;
import com.axway.ats.log.autodb.DbEventRequestProcessor;
import com.axway.ats.log.autodb.LogEventRequest;
import com.axway.ats.log.autodb.events.DeleteTestCaseEvent;
||||||| merged common ancestors
import com.axway.ats.core.log.AtsConsoleLogger;
import com.axway.ats.core.utils.TimeUtils;
import com.axway.ats.log.autodb.LogEventRequest;
import com.axway.ats.log.autodb.events.DeleteTestCaseEvent;
=======
import com.axway.ats.core.threads.ImportantThread;
import com.axway.ats.core.utils.ExecutorUtils;
>>>>>>> Initial commit for running tests in parallel
import com.axway.ats.log.autodb.events.GetCurrentTestCaseEvent;

/**
 * This appender is capable of arranging the database storage and storing messages into it.
 * It works on the Test Executor side.
 */
public class ActiveDbAppender extends AbstractDbAppender {

    private static ActiveDbAppender instance                                 = null;

    public static boolean           isAttached                               = false;

    /** enables/disabled logging of messages from @BeforeXXX and @AfterXXX annotated Java methods **/
    public static boolean           isBeforeAndAfterMessagesLoggingSupported = false;

<<<<<<< 9eeb7d42a8d5d8bd4b1e44fc93c05f170744823e
    /* 
     * Sometimes the main thread needs to wait until the logger thread has processed the log event.
     * We use this mutex for synchronization aid. 
     */
    private Object                  listenerMutex                            = new Object();
    
    public static final String DUMMY_DB_HOST = "ATS_NO_DB_HOST_SET";
    public static final String DUMMY_DB_DATABASE = "ATS_NO_DB_NAME_SET";
    public static final String DUMMY_DB_USER = "ATS_NO_DB_USER_SET";
    public static final String DUMMY_DB_PASSWORD = "ATS_NO_DB_PASSWORD_SET";

||||||| merged common ancestors
    /* 
     * Sometimes the main thread needs to wait until the logger thread has processed the log event.
     * We use this mutex for synchronization aid. 
     */
    private Object                  listenerMutex                            = new Object();

=======
>>>>>>> Initial commit for running tests in parallel
    /**
     * Constructor
     */
    public ActiveDbAppender() {

        super();
        
        /** create dummy appender configuration 
         *  This configuration will be replaced with one from log4j.xml file
         * */
        appenderConfig.setHost(DUMMY_DB_HOST);
        appenderConfig.setDatabase(DUMMY_DB_DATABASE);
        appenderConfig.setUser(DUMMY_DB_USER);
        appenderConfig.setPassword(DUMMY_DB_PASSWORD);
        
        /**
         * Create dummy event request processor.
         * This processor will be replaced once config from log4j.xml is loaded
         * */
        eventProcessor = new DbEventRequestProcessor();
    }
    
    @Override
    public void activateOptions(){
        super.activateOptions();
        
        isAttached = true;
    }

    /* (non-Javadoc)
     * @see org.apache.log4j.AppenderSkeleton#append(org.apache.log4j.spi.LoggingEvent)
     */
    @Override
    protected void append( LoggingEvent event ) {

<<<<<<< 9eeb7d42a8d5d8bd4b1e44fc93c05f170744823e
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

                    // clear last testcase id
                    testCaseState.clearLastExecutedTestcaseId();

                    //this event has already been through the queue
                    return;
                }
                case END_TEST_CASE: {

                    // on Test Executor side we block until the test case start is committed in the DB
                    waitForEventToBeExecuted(packedEvent, dbLoggingEvent, true);

                    // remember the last executed test case id
                    testCaseState.setLastExecutedTestcaseId(testCaseState.getTestcaseId());

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
                    atsConsoleLogger.info("Waiting for "
                                          + event.getClass().getSimpleName()
                                          + " event completion");

                    /** disable root logger's logging in order to prevent deadlock **/
                    Level level = Logger.getRootLogger().getLevel();
                    Logger.getRootLogger().setLevel(Level.OFF);

                    AtsConsoleLogger.level = level;

                    // create the queue logging thread and the DbEventRequestProcessor
                    if (queueLogger == null) {
                        initializeDbLogging();
                    }

                    waitForEventToBeExecuted(packedEvent, dbLoggingEvent, false);
                    //this event has already been through the queue

                    /*Revert Logger's level*/
                    Logger.getRootLogger().setLevel(level);
                    AtsConsoleLogger.level = null;

                    return;
                case END_RUN: {
                    /* We synchronize the run end.
                     * This way if there are remaining log events in the Test Executor's queue,
                     * the JVM will not be shutdown prior to committing all events in the DB, as
                     * the END_RUN event is the last one in the queue
                     */
                    atsConsoleLogger.info("Waiting for "
                                          + event.getClass().getSimpleName()
                                          + " event completion");

                    /** disable root logger's logging in order to prevent deadlock **/
                    level = Logger.getRootLogger().getLevel();
                    Logger.getRootLogger().setLevel(Level.OFF);

                    AtsConsoleLogger.level = level;

                    waitForEventToBeExecuted(packedEvent, dbLoggingEvent, true);

                    /*Revert Logger's level*/
                    Logger.getRootLogger().setLevel(level);
                    AtsConsoleLogger.level = null;

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
||||||| merged common ancestors
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

                    // clear last testcase id
                    testCaseState.clearLastExecutedTestcaseId();

                    //this event has already been through the queue
                    return;
                }
                case END_TEST_CASE: {
                    
                    // on Test Executor side we block until the test case start is committed in the DB
                    waitForEventToBeExecuted(packedEvent, dbLoggingEvent, true);

                    // remember the last executed test case id
                    testCaseState.setLastExecutedTestcaseId(testCaseState.getTestcaseId());

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
                    atsConsoleLogger.info("Waiting for "
                                          + event.getClass().getSimpleName()
                                          + " event completion");

                    /** disable root logger's logging in order to prevent deadlock **/
                    Level level = Logger.getRootLogger().getLevel();
                    Logger.getRootLogger().setLevel(Level.OFF);

                    AtsConsoleLogger.level = level;

                    // create the queue logging thread and the DbEventRequestProcessor
                    if (queueLogger == null) {
                        initializeDbLogging();
                    }

                    waitForEventToBeExecuted(packedEvent, dbLoggingEvent, false);
                    //this event has already been through the queue

                    /*Revert Logger's level*/
                    Logger.getRootLogger().setLevel(level);
                    AtsConsoleLogger.level = null;

                    return;
                case END_RUN: {
                    /* We synchronize the run end.
                     * This way if there are remaining log events in the Test Executor's queue,
                     * the JVM will not be shutdown prior to committing all events in the DB, as
                     * the END_RUN event is the last one in the queue
                     */
                    atsConsoleLogger.info("Waiting for "
                                          + event.getClass().getSimpleName()
                                          + " event completion");

                    /** disable root logger's logging in order to prevent deadlock **/
                    level = Logger.getRootLogger().getLevel();
                    Logger.getRootLogger().setLevel(Level.OFF);

                    AtsConsoleLogger.level = level;

                    waitForEventToBeExecuted(packedEvent, dbLoggingEvent, true);

                    /*Revert Logger's level*/
                    Logger.getRootLogger().setLevel(level);
                    AtsConsoleLogger.level = null;

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
=======
        // We call the next method, so log4j will internally remember the thread name.
        // See the internal implementation for details.
        event.getThreadName();
        
        getDbChannel( event ).append( event );
>>>>>>> Initial commit for running tests in parallel
    }

    @Override
    public GetCurrentTestCaseEvent getCurrentTestCaseState( GetCurrentTestCaseEvent event ) {

        DbChannel channel = getDbChannel( null );

        channel.testCaseState.setRunId( channel.eventProcessor.getRunId() );
        // get current test case id which will be passed to ATS agent
        event.setTestCaseState( channel.testCaseState );
        return event;
    }

    public String getHost() {

        return appenderConfig.getHost();
    }

    public void setHost( String host ) {

        appenderConfig.setHost( host );
    }

    public String getPort() {

        return appenderConfig.getPort();
    }

    public void setPort( String port ) {

        appenderConfig.setPort(port);
    }

    public String getDatabase() {

        return appenderConfig.getDatabase();
    }

    public void setDatabase( String database ) {

        appenderConfig.setDatabase( database );
    }

    public String getUser() {

        return appenderConfig.getUser();
    }

    public void setUser( String user ) {

        appenderConfig.setUser( user );
    }

    public String getPassword() {

        return appenderConfig.getPassword();
    }

    public void setPassword( String password ) {

        appenderConfig.setPassword( password );
    }

    /**
     * This method doesn't create a new instance,
     * but returns the already created one (from log4j) or null if there is no such.
     *
     * @return the current DB appender instance
     */
    @SuppressWarnings("unchecked")
    public static ActiveDbAppender getCurrentInstance() {

        if( instance == null ) {
            Enumeration<Appender> appenders = Logger.getRootLogger().getAllAppenders();
            while( appenders.hasMoreElements() ) {
                Appender appender = appenders.nextElement();

<<<<<<< 9eeb7d42a8d5d8bd4b1e44fc93c05f170744823e
                if (appender instanceof ActiveDbAppender) {
                    instance = (ActiveDbAppender) appender;
                    isAttached = true;
                    return instance;
||||||| merged common ancestors
                if (appender instanceof ActiveDbAppender) {
                    instance = (ActiveDbAppender) appender;
=======
                if( appender instanceof ActiveDbAppender ) {
                    instance = ( ActiveDbAppender ) appender;
>>>>>>> Initial commit for running tests in parallel
                }
            }
        }
        
        if (instance != null) {
            return instance;
        }

        /*
         * Configuration in log4j.xml file was not found for ActiveDbAppender
         * A dummy com.axway.ats.log.autodb.DbEventRequestProcessor is
         * created in order to prevent NPE when invoking methods such as getRunId()
         */
        new AtsConsoleLogger(ActiveDbAppender.class).warn(
                                                          "ATS Database appender is not specified in log4j.xml file. "
                                                          + "Methods such as ActiveDbAppender@getRunId() will not work.");
        
        isAttached = false;
        instance = new ActiveDbAppender();
        return instance;
    }

    @Override
    protected String getDbChannelKey(LoggingEvent event) {

        // Works on Test Executor side
        // Have a channel per execution thread

        String executorId = null;
        
        if( event != null ) {
            // the executor might be comming from the logging event
            executorId = event.getProperty( ExecutorUtils.ATS_RANDOM_TOKEN );
        }

        if( executorId == null ) {
            Thread thisThread = Thread.currentThread();
            if( thisThread instanceof ImportantThread ) {
                // a special thread, it holds the executor ID
                executorId = ( ( ImportantThread ) thisThread ).getExecutorId();
            } else {
                // use the thread name
                executorId = thisThread.getName();
            }
        }
<<<<<<< 9eeb7d42a8d5d8bd4b1e44fc93c05f170744823e

        public void onTestcaseFinished() {
||||||| merged common ancestors
        
        public void onTestcaseFinished() {
=======
>>>>>>> Initial commit for running tests in parallel

        return executorId;
    }

}
