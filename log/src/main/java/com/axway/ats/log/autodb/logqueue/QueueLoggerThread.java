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
/*
 *  Copyright (c) 1993-2010 Axway Inc. All Rights Reserved.
 */

package com.axway.ats.log.autodb.logqueue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.LogEvent;

import com.axway.ats.core.log.AtsConsoleLogger;
import com.axway.ats.core.utils.ExceptionUtils;
import com.axway.ats.log.autodb.exceptions.LoggingException;
import com.axway.ats.log.autodb.io.AbstractDbAccess;
import com.axway.ats.log.autodb.model.AbstractLoggingEvent;
import com.axway.ats.log.autodb.model.EventRequestProcessor;
import com.axway.ats.log.autodb.model.LoggingEventType;

/**
 * The active thread used to log (queued) test execution messages and events into the ATS database. 
 */
public class QueueLoggerThread extends Thread {

    final static AtsConsoleLogger               CONSOLE_LOG                            = new AtsConsoleLogger(QueueLoggerThread.class);
    // max count of exceptions to log for "not critical" SQL exceptions
    private static final int                    MINOR_SQL_EXCEPTIONS_MAX_LOGGING_COUNT = 5;
    private EventRequestProcessor               eventProcessor;
    private LoggingException                    loggingException;

    private boolean                             isBatchMode;

    private boolean                             isUnableToConnect                      = false;

    /**
     * The queue of events waiting to be logged into DB
     */
    private ArrayBlockingQueue<LogEventRequest> queue;
    private int                                 minorSqlExceptionsCounter              = 0;                                            // counter for minor SQL exceptions. Used to prevent flooding of the log

    public QueueLoggerThread( ArrayBlockingQueue<LogEventRequest> queue, EventRequestProcessor eventProcessor,
                              boolean isBatchMode ) {

        this.queue = queue;
        this.eventProcessor = eventProcessor;
        this.isBatchMode = isBatchMode;

        // It is the user's responsibility to close appenders before
        // exiting.
        this.setDaemon(false);
        this.setName(this.getClass().getSimpleName() + "-" + getName());
    }

    /* (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {

        CONSOLE_LOG.info(
                         "Started logger thread named '"
                         + getName() + "' with queue of maximum " + queue.remainingCapacity() + queue.size()
                         + " events. Batch mode is " + (isBatchMode
                                                                    ? "enabled"
                                                                    : "disabled"));
        while (true) {
            LogEventRequest logEventRequest = null;
            try {
                if (isBatchMode) {
                    // get the next event, wait no more than 10 seconds
                    logEventRequest = queue.poll(10, TimeUnit.SECONDS);
                } else {
                    // we are not in a hurry,
                    // block until receive an event in the queue
                    logEventRequest = queue.take();
                }
                eventProcessor.processEventRequest(logEventRequest);
            } catch (InterruptedException ie) {
                // NOTE: In this method we talk to the user using console only as we cannot send it to the log DB
                CONSOLE_LOG.error(
                                  "Logging thread is interrupted and will stop logging.");
                break;
            } catch (Exception e) {
                if (e instanceof LoggingException && logEventRequest != null) {
                    LoggingException le = (LoggingException) e;
                    LogEvent event = logEventRequest.getEvent();
                    if (event instanceof AbstractLoggingEvent) {
                        AbstractLoggingEvent dbAppenderEvent = (AbstractLoggingEvent) event;
                        LoggingEventType eventType = dbAppenderEvent.getEventType();
                        // If START_* log entity event do not work, we can not end it
                        // nor we can insert into that entity its sub-entities

                        // We do not remember other type of failed events, as these are the only ones we check in the main thread.
                        // The Join Testcase event is the one that connects to the DB on the side of ATS Agent
                        if (eventType == LoggingEventType.START_RUN
                            || eventType == LoggingEventType.START_SUITE
                            || eventType == LoggingEventType.START_TEST_CASE
                            || eventType == LoggingEventType.JOIN_TEST_CASE
                            || eventType == LoggingEventType.START_CHECKPOINT) {

                            CONSOLE_LOG.error(ExceptionUtils.getExceptionMsg(le,
                                                                             "Error running "
                                                                                 + eventType
                                                                                 + " event"));

                            synchronized (this) {
                                this.loggingException = le;
                            }
                        } else {
                            // Other "not critical" exceptions. We limit logging of such failures as it would be too 
                            // verbose
                            /**
                             * Explicitly skip error when a message can not be inserted in Log DB
                             * or when Thread has already been registered with a load queue.
                             * This is done, because those errors are expected in some cases when using ATS
                             * */
                            if (eventType != LoggingEventType.REGISTER_THREAD_WITH_LOADQUEUE
                                && eventType != LoggingEventType.INSERT_MESSAGE) {
                                if (minorSqlExceptionsCounter < MINOR_SQL_EXCEPTIONS_MAX_LOGGING_COUNT) {
                                    CONSOLE_LOG.error(ExceptionUtils.getExceptionMsg(le, "Error running " + eventType
                                                                                         + " event"));
                                    minorSqlExceptionsCounter++;
                                }
                            }
                        }
                    } else if (le.getMessage().equalsIgnoreCase(AbstractDbAccess.UNABLE_TO_CONNECT_ERRROR)
                               && !isUnableToConnect) {
                        // We do not log the no connectivity problem on each failure, we do it just once.
                        // This case is likely to happen on a remote Agent host without set DNS servers - in such
                        // case providing FQDN in the log4j2.xml makes the DB logging impossible
                        CONSOLE_LOG.error(ExceptionUtils.getExceptionMsg(e,
                                                                         "Error processing log event"));

                        isUnableToConnect = true;
                    }
                } else {
                    // we do not let this exception break this thread, but only log it into the console
                    // we expect to get here when hit some very unusual errors

                    if (logEventRequest != null) {
                        CONSOLE_LOG.error(ExceptionUtils.getExceptionMsg(e,
                                                                         "Error processing log event "
                                                                            + logEventRequest.getEvent()
                                                                                             .getMessage()));
                    } else {
                        // The 'log event request' object is null because timed out while waiting for it from the queue.
                        // This happens when running in batch mode.
                        // Then we tried to flush the current events, but this was not successful, so came here.
                        CONSOLE_LOG.error(ExceptionUtils.getExceptionMsg(e,
                                                                         "Error processing log events in batch mode"));
                    }
                }
            }
        }
    }

    public synchronized LoggingException readLoggingException() {

        try {
            // here we send a reference to the logging exception and
            // release the local variable, so it can be set again if a new
            // error appear
            if (loggingException != null) {
                return new LoggingException(loggingException);
            } else {
                return null;
            }
        } finally {
            loggingException = null;
        }
    }
}
