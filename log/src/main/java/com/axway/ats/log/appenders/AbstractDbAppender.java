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

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.utils.TimeUtils;
import com.axway.ats.log.autodb.DbAppenderConfiguration;
import com.axway.ats.log.autodb.DbEventRequestProcessor;
import com.axway.ats.log.autodb.LogEventRequest;
import com.axway.ats.log.autodb.QueueLoggerThread;
import com.axway.ats.log.autodb.TestCaseState;
import com.axway.ats.log.autodb.events.GetCurrentTestCaseEvent;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.log.autodb.exceptions.DbAppenederException;
import com.axway.ats.log.autodb.exceptions.InvalidAppenderConfigurationException;
import com.axway.ats.log.autodb.model.EventRequestProcessorListener;

/**
 * This appender is capable of arranging the database storage and storing
 * messages into it. It works on the Test Executor side.
 */
public abstract class AbstractDbAppender extends AppenderSkeleton {

    /**
     * The appender's data for the current thread
     */
    protected ArrayBlockingQueue<LogEventRequest> queue;

    /**
     * the logger thread
     */
    protected QueueLoggerThread                   queueLogger;

    /**
     * The configuration for this appender
     */
    protected DbAppenderConfiguration             appenderConfig;

    /**
     * The class which will process the logging requests
     */
    protected DbEventRequestProcessor             eventProcessor;

    /**
     * Here we are caching the state of the currently executed test case. This
     * way we do not need to go through the queue(which is in another thread)
     */
    protected TestCaseState                       testCaseState;

    /**
     * When true - we dump info about the usage of the events queue. It is
     * targeted as a debug tool when cannot sent the events to the DB fast
     * enough.
     */
    private boolean                               isMonitoringEventsQueue;
    private long                                  lastQueueCapacityTick;

    /**
     * Keeps track what was the minimum value of the remaining queue capacity;
     * */
    private int                                   minRemainingQueueCapacity = -1;

    /**
    
     * The Test Executor time is the leading time.
    
     * The time offset value here keeps the time difference between Test Executor and a particular Agent.
    
     * So the offset will be zero on Test Executor side (where ActiveDbAppender is used) and
    
     * probably different then zero on the Agent side (where PassiveDbAppender is used)
    
     */
    private long                                  timeOffset                = 0;

    /**
     * Constructor
     */
    public AbstractDbAppender() {

        super();

        // init the appender configuration
        // it will be populated when the setters are called
        appenderConfig = new DbAppenderConfiguration();

        testCaseState = new TestCaseState();

        isMonitoringEventsQueue = AtsSystemProperties.getPropertyAsBoolean(AtsSystemProperties.LOG__MONITOR_EVENTS_QUEUE,
                                                                           false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.log4j.AppenderSkeleton#activateOptions()
     */
    @Override
    public void activateOptions() {

        // check whether the configuration is valid first
        try {
            appenderConfig.validate();
        } catch (InvalidAppenderConfigurationException iace) {
            throw new DbAppenederException(iace);
        }

        // set the threshold if there is such
        appenderConfig.setLoggingThreshold(getThreshold());

        // the logging queue
        queue = new ArrayBlockingQueue<LogEventRequest>(getMaxNumberLogEvents());

    }

    protected void initializeDbLogging() {

        // enable batch mode at ATS Agent side only
        boolean isWorkingAtAgentSide = this instanceof PassiveDbAppender;
        boolean isBatchMode = false;
        if (isWorkingAtAgentSide) {
            isBatchMode = isBatchMode();
        }

        // create new event processor
        try {
            eventProcessor = new DbEventRequestProcessor(appenderConfig,
                                                         layout,
                                                         getEventRequestProcessorListener(),
                                                         isBatchMode);
        } catch (DatabaseAccessException e) {
            throw new RuntimeException("Unable to create DB event processor", e);
        }

        // start the logging thread
        queueLogger = new QueueLoggerThread(queue, eventProcessor, isBatchMode);
        queueLogger.setDaemon(true);
        queueLogger.start();
    }

    protected void passEventToLoggerQueue(
                                           LogEventRequest packedEvent ) {

        // Events on both Test Executor and Agent sides are processed here.

        // Events on Agent get their timestamps aligned with Test Executor time.
        if (timeOffset != 0) {
            packedEvent.applyTimeOffset(timeOffset);
        }

        if (isMonitoringEventsQueue) {
            // Tell the user how many new events can be placed in the queue.
            // Do this every second.
            long newTick = System.currentTimeMillis();
            if (newTick - lastQueueCapacityTick > 1000) {
                if (minRemainingQueueCapacity == -1) {
                    minRemainingQueueCapacity = queue.remainingCapacity();
                } else {
                    minRemainingQueueCapacity = Math.min(minRemainingQueueCapacity, queue.remainingCapacity());
                }
                System.out.println(TimeUtils.getFormattedDateTillMilliseconds()
                                   + " Remaining queue capacity is " + queue.remainingCapacity()
                                   + " out of " + (queue.remainingCapacity() + queue.size())
                                   + ". Bottom remaining capacity is " + minRemainingQueueCapacity);
                lastQueueCapacityTick = newTick;
            }
        }

        // this thread passes the events to the queue,
        // while another thread is reading them on the other side
        try {
            queue.add(packedEvent);
        } catch (IllegalStateException ex) {
            if (queue.remainingCapacity() < 1) {
                throw new IllegalStateException("There are too many messages queued"
                                                + " for TestExplorer DB logging. Decrease messages count"
                                                + " by lowering effective log4j severity or check whether"
                                                + " connection to DB is too slow", ex);
            } else {
                throw ex;
            }
        }
    }

    public abstract GetCurrentTestCaseEvent getCurrentTestCaseState(
                                                                     GetCurrentTestCaseEvent event );

    protected abstract EventRequestProcessorListener getEventRequestProcessorListener();

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.log4j.AppenderSkeleton#close()
     */
    public void close() {

        // When the appender is unloaded, terminate the logging thread
        if (queueLogger != null) {
            queueLogger.interrupt();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.log4j.AppenderSkeleton#requiresLayout()
     */
    public boolean requiresLayout() {

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.log4j.AppenderSkeleton#setLayout(org.apache.log4j.Layout)
     */
    @Override
    public void setLayout(
                           Layout layout ) {

        super.setLayout(layout);

        // set the layout to the event processor as well
        if (eventProcessor != null) {
            eventProcessor.setLayout(layout);
        }
    }

    /**
     * log4j system reads the "events" parameter from the log4j.xml and calls
     * this method
     *
     * @param maxNumberLogEvents
     */
    public void setEvents(
                           String maxNumberLogEvents ) {

        this.appenderConfig.setMaxNumberLogEvents(maxNumberLogEvents);
    }

    /**
     * @return the capacity of the logging queue
     */
    public int getMaxNumberLogEvents() {

        return appenderConfig.getMaxNumberLogEvents();
    }

    /**
     * @return the current size of the logging queue
     */
    public int getNumberPendingLogEvents() {

        return queue.size();
    }

    /**
     * @return if sending log messages in batch mode
     */
    public boolean isBatchMode() {

        return appenderConfig.isBatchMode();
    }

    /**
     * log4j system reads the "mode" parameter from the log4j.xml and calls this
     * method
     *
     * Expected value is "batch", everything else is skipped.
     *
     * @param mode
     */
    public void setMode(
                         String mode ) {

        this.appenderConfig.setMode(mode);
    }

    /**
     * Get the current run id
     *
     * @return the current run id
     */
    public int getRunId() {

        return eventProcessor.getRunId();
    }

    /**
     * Get the current suite id
     *
     * @return the current suite id
     */
    public int getSuiteId() {

        return eventProcessor.getSuiteId();
    }

    /**
     * Get the current run name
     *
     * @return the current run name
     */
    public String getRunName() {

        return eventProcessor.getRunName();
    }

    /**
     * Get the current run user note
     *
     * @return the current run user note
     */
    public String getRunUserNote() {

        return eventProcessor.getRunUserNote();
    }

    /**
     *
     * @return the current testcase id
     */
    public int getTestCaseId() {

        return eventProcessor.getTestCaseId();
    }

    public boolean getEnableCheckpoints() {

        return appenderConfig.getEnableCheckpoints();
    }

    public void setEnableCheckpoints(
                                      boolean enableCheckpoints ) {

        appenderConfig.setEnableCheckpoints(enableCheckpoints);
    }

    public DbAppenderConfiguration getAppenderConfig() {

        return appenderConfig;
    }

    public void setAppenderConfig(
                                   DbAppenderConfiguration appenderConfig ) {

        this.appenderConfig = appenderConfig;
        this.threshold = appenderConfig.getLoggingThreshold();
    }

    public void calculateTimeOffset(
                                     long executorTimestamp ) {

        this.timeOffset = (System.currentTimeMillis() - executorTimestamp);
    }
}
