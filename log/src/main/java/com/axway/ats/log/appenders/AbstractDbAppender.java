/*
 * Copyright 2017-2021 Axway Software
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

import java.io.Serializable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.apache.logging.log4j.core.filter.ThresholdFilter;

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.log.AtsConsoleLogger;
import com.axway.ats.log.autodb.DbAppenderConfiguration;
import com.axway.ats.log.autodb.TestCaseState;
import com.axway.ats.log.autodb.events.GetCurrentTestCaseEvent;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.log.autodb.exceptions.DbAppenederException;
import com.axway.ats.log.autodb.exceptions.InvalidAppenderConfigurationException;
import com.axway.ats.log.autodb.logqueue.DbEventRequestProcessor;
import com.axway.ats.log.autodb.logqueue.LogEventRequest;
import com.axway.ats.log.autodb.logqueue.QueueLoggerThread;
import com.axway.ats.log.autodb.model.EventRequestProcessorListener;

//@Plugin( name = "AbstractDbAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public abstract class AbstractDbAppender extends AbstractAppender {

    protected AtsConsoleLogger                    atsConsoleLogger          = new AtsConsoleLogger(getClass());

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
    protected AbstractDbAppender( String name, Filter filter, Layout<? extends Serializable> layout,
                                  DbAppenderConfiguration appenderConfiguration ) {

        super(name, filter, layout, false, null); // or maybe true?!?

        // init the appender configuration
        // it will be populated when the setters are called
        this.appenderConfig = appenderConfiguration;

        testCaseState = new TestCaseState();

        isMonitoringEventsQueue = AtsSystemProperties.getPropertyAsBoolean(AtsSystemProperties.LOG__MONITOR_EVENTS_QUEUE,
                                                                           false);

        // check whether the configuration is valid first
        try {
            appenderConfig.validate();
        } catch (InvalidAppenderConfigurationException iace) {
            throw new DbAppenederException(iace);
        }

        // set the threshold if there is such
        if (this.hasFilter()) {
            if (this.getFilter() instanceof ThresholdFilter) {
                appenderConfig.setLoggingThreshold( ((ThresholdFilter) this.getFilter()).getLevel());
            } else if (this.getFilter() instanceof CompositeFilter) {
                Filter[] allAppliedFilters = ((CompositeFilter) this.getFilter()).getFiltersArray();
                for (Filter f : allAppliedFilters) {
                    if (f instanceof ThresholdFilter) {
                        appenderConfig.setLoggingThreshold( ((ThresholdFilter) f).getLevel());
                        // note what if there are multiple ThresholdFilter(s) ?!?
                        break;
                    }
                }
            }

        } else {
            // nothing is filtered
            //throw new RuntimeException("No Threshold filter provided!");
        }

        // the logging queue
        queue = new ArrayBlockingQueue<LogEventRequest>(getMaxNumberLogEvents());

    }

    @Override
    public boolean stop( long timeout, TimeUnit timeUnit ) {

        // When the appender is unloaded, terminate the logging thread
        if (queueLogger != null && !queueLogger.isInterrupted()) {
            queueLogger.interrupt();
            queueLogger = null;
        }

        return super.stop(timeout, timeUnit);
    }

    @Override
    protected boolean stop( long timeout, TimeUnit timeUnit, boolean changeLifeCycleState ) {

        // When the appender is unloaded, terminate the logging thread
        if (queueLogger != null && !queueLogger.isInterrupted()) {
            queueLogger.interrupt();
            queueLogger = null;
        }

        return super.stop(timeout, timeUnit, changeLifeCycleState);
    }

    @Override
    public void stop() {

        // When the appender is unloaded, terminate the logging thread
        if (queueLogger != null && !queueLogger.isInterrupted()) {
            queueLogger.interrupt();
            queueLogger = null;
        }
        super.stop();
    }

    @Override
    protected boolean stop( Future<?> future ) {

        // When the appender is unloaded, terminate the logging thread
        if (queueLogger != null && !queueLogger.isInterrupted()) {
            queueLogger.interrupt();
            queueLogger = null;
        }
        return super.stop(future);
    }

    @Override
    public abstract void append( LogEvent event );

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
                                                         this.getLayout(),
                                                         getEventRequestProcessorListener(),
                                                         isBatchMode);

        } catch (DatabaseAccessException e) {
            throw new RuntimeException("Unable to create DB event processor", e);
        }

        // start the logging thread
        // can be moved to the start() method, but only if needed
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
                atsConsoleLogger.info("Remaining queue capacity is " + queue.remainingCapacity()
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
                                                + " by lowering effective log4j2 severity or check whether"
                                                + " connection to DB is too slow", ex);
            } else {
                throw ex;
            }
        }
    }

    public abstract GetCurrentTestCaseEvent getCurrentTestCaseState(
                                                                     GetCurrentTestCaseEvent event );

    protected abstract EventRequestProcessorListener getEventRequestProcessorListener();

    /**
     * log4j2 system reads the "events" parameter from the log4j2.xml and calls
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
     * log4j2 system reads the "mode" parameter from the log4j2.xml and calls this
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

    /**
     * @return the last executed, regardless of the finish status (e.g passed/failed/skipped), testcase ID
     * */
    public int getLastExecutedTestCaseId() {

        return eventProcessor.getLastExecutedTestCaseId();
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
        this.addFilter(ThresholdFilter.createFilter(appenderConfig.getLoggingThreshold(), Result.ACCEPT, Result.DENY)); // or Result.NEUTRAL ?!?
    }

    public void calculateTimeOffset(
                                     long executorTimestamp ) {

        this.timeOffset = (System.currentTimeMillis() - executorTimestamp);
    }

}
