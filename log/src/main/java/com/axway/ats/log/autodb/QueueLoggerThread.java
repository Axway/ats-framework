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
/*
 *  Copyright (c) 1993-2010 Axway Inc. All Rights Reserved.
 */

package com.axway.ats.log.autodb;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.spi.LoggingEvent;

import com.axway.ats.core.log.AtsConsoleLogger;
import com.axway.ats.core.utils.ExceptionUtils;
import com.axway.ats.log.autodb.exceptions.LoggingException;
import com.axway.ats.log.autodb.exceptions.LoggingIsOverException;
import com.axway.ats.log.autodb.model.AbstractLoggingEvent;
import com.axway.ats.log.autodb.model.EventRequestProcessor;
import com.axway.ats.log.autodb.model.LoggingEventType;

/**
 * The active logging thread. Capable of arranging the database storage and storing messages into it.
 */
public class QueueLoggerThread extends Thread {

    final static AtsConsoleLogger               log               = new AtsConsoleLogger( QueueLoggerThread.class );
    private EventRequestProcessor               eventProcessor;
    private LoggingException                    loggingException;

    private boolean                             isBatchMode;

    private boolean                             isUnableToConnect = false;

    /**
     * the events queue
     */
    private ArrayBlockingQueue<LogEventRequest> queue;

    public QueueLoggerThread( ArrayBlockingQueue<LogEventRequest> queue, EventRequestProcessor eventProcessor,
                              boolean isBatchMode ) {

        this.queue = queue;
        this.eventProcessor = eventProcessor;
        this.isBatchMode = isBatchMode;

        // It is the user's responsibility to close appenders before
        // exiting.
        this.setDaemon( false );
        this.setName( this.getClass().getSimpleName() );
    }

    /* (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {

        log.info(
                 "Started logger thread named '"
                 + getName() + "' with queue of maximum " + queue.remainingCapacity() + queue.size()
                 + " events." + (isBatchMode
                                                                                                         ? " Batch mode is enabled"
                                                                                                         : "" ) );
        while( true ) {
            LogEventRequest logEventRequest = null;
            try {
                if( isBatchMode ) {
                    // get the next event, wait no more than 10 seconds
                    logEventRequest = queue.poll( 10, TimeUnit.SECONDS );
                } else {
                    // we are not in a hurry,
                    // block until receive an event in the queue
                    logEventRequest = queue.take();
                }
                eventProcessor.processEventRequest( logEventRequest );
            } catch( InterruptedException ie ) {
                // NOTE: In this method we talk to the user using console only as we cannot send it to the log DB
                log.error( "Logging thread is interrupted and will stop logging." );
                break;
            } catch( Exception e ) {
                if( e instanceof LoggingIsOverException ) {
                    // This is not an error. 
                    // We are running parallel tests and we just left one of them on the Agent side.
                    if( queue.size() > 0 ) {
                        // There are more events in this queue.
                        // This means there is another test serviced by this same thread.
                        // We just go on.
                        log.info( "We were about to exit thread " + getName()
                                  + " due to LEAVE_TEST_EVENT, but there are " + queue.size()
                                  + " events left, so we just go on" );
                    } else {
                        // Queue is empty.
                        // log.info( e.getMessage() );

                        // It is time for this thread to stop.
                        // Future calls to its isAlive() method will return false.
                        return;
                    }
                }
                else if( e instanceof LoggingException && logEventRequest != null ) {
                    LoggingException le = ( LoggingException ) e;
                    LoggingEvent event = logEventRequest.getEvent();
                    if( event instanceof AbstractLoggingEvent ) {
                        AbstractLoggingEvent dbAppenderEvent = ( AbstractLoggingEvent ) event;
                        LoggingEventType eventType = dbAppenderEvent.getEventType();
                        // If START_* log entity event do not work, we can not end it
                        // nor we can insert into that entity its sub-entities

                        // We do not let user know about other type of failed events, as it would be too verbose.
                        // We do not remember other type of failed events, as these are the only ones we check in the main thread.
                        // The Join Testcase event is the one that connects to the DB on the side of ATS Agent
                        if( eventType == LoggingEventType.START_RUN
                            || eventType == LoggingEventType.START_SUITE
                            || eventType == LoggingEventType.START_TEST_CASE
                            || eventType == LoggingEventType.JOIN_TEST_CASE
                            || eventType == LoggingEventType.START_CHECKPOINT ) {

                            log.error(ExceptionUtils.getExceptionMsg(le,
                                                                     "Error running "
                                                                         + eventType
                                                                           + " event" ) );

                            synchronized( this ) {
                                this.loggingException = le;
                            }
                        }
                    } else if( le.getMessage().equalsIgnoreCase( AbstractDbAccess.UNABLE_TO_CONNECT_ERRROR )
                               && !isUnableToConnect ) {
                        // We do not log the no connectivity problem on each failure, we do it just once.
                        // This case is likely to happen on a remote Agent host without set DNS servers - in such
                        // case providing FQDN in the log4j.xml makes the DB logging impossible
                        log.error(ExceptionUtils.getExceptionMsg(e,
                                                                 "Error processing log event"));

                        isUnableToConnect = true;
                    }
                } else {
                    // we do not let this exception break this thread, but only log it into the console
                    // we expect to get here when hit some very unusual errors

                    if( logEventRequest != null ) {
                        log.error( ExceptionUtils.getExceptionMsg( e,
                                                                   "Error processing log event "
                                                                      + logEventRequest.getEvent()
                                                                                       .getMessage() ) );
                    } else {
                        // The 'log event request' object is null because timed out while waiting for it from the queue.
                        // This happens when running in batch mode.
                        // Then we tried to flush the current events, but this was not successful, so came here.
                        log.error( ExceptionUtils.getExceptionMsg( e,
                                                                   "Error processing log events in batch mode" ) );
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
            if( loggingException != null ) {
                return new LoggingException( loggingException );
            } else {
                return null;
            }
        } finally {
            loggingException = null;
        }
    }
}
