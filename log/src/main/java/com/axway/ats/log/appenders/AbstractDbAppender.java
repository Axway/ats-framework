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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

import com.axway.ats.core.log.AtsConsoleLogger;
import com.axway.ats.log.autodb.DbAppenderConfiguration;
import com.axway.ats.log.autodb.events.GetCurrentTestCaseEvent;
import com.axway.ats.log.autodb.exceptions.DbAppenederException;
import com.axway.ats.log.autodb.exceptions.InvalidAppenderConfigurationException;

/**
 * This appender is capable of arranging the database storage and storing
 * messages into it. It works on the Test Executor side.
 */
public abstract class AbstractDbAppender extends AppenderSkeleton {

    // the channels for each test case
    private Map<String, DbChannel>    channels         = new HashMap<String, DbChannel>();

    protected AtsConsoleLogger        atsConsoleLogger = new AtsConsoleLogger(getClass());

    /**
     * The configuration for this appender
     */
    protected DbAppenderConfiguration appenderConfig;

    /**
     * Constructor
     */
    public AbstractDbAppender() {

        super();

        // init the appender configuration
        // it will be populated when the setters are called
        this.appenderConfig = new DbAppenderConfiguration();
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
            this.appenderConfig.validate();
        } catch (InvalidAppenderConfigurationException iace) {
            throw new DbAppenederException(iace);
        }

        // set the threshold if there is such
        if (getThreshold() != null) {
            this.appenderConfig.setLoggingThreshold(getThreshold().toInt());
        }

    }

    protected abstract String getDbChannelKey( LoggingEvent event );

    protected DbChannel getDbChannel( LoggingEvent event ) {

        String channelKey = getDbChannelKey(event);

        DbChannel channel = this.channels.get(channelKey);
        if (channel == null) {
            channel = new DbChannel(this.appenderConfig);

            channel.initialize(atsConsoleLogger, this.layout, true);

            this.channels.put(channelKey, channel);
        }

        return channel;
    }

    protected void distroyDbChannel( String channelKey ) {

        DbChannel channel = this.channels.get( channelKey );
        
        this.channels.remove(channelKey);
    }

    public abstract GetCurrentTestCaseEvent getCurrentTestCaseState( GetCurrentTestCaseEvent event );

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.log4j.AppenderSkeleton#close()
     */
    public void close() {

        getDbChannel(null).close();
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

        // remember it 
        this.layout = layout;

        // set the layout to the event processor as well
        DbChannel channel = getDbChannel(null);
        channel.eventProcessor.setLayout(layout);
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

        return this.appenderConfig.getMaxNumberLogEvents();
    }

    /**
     * @return the current size of the logging queue
     */
    public int getNumberPendingLogEvents() {

        return getDbChannel(null).getNumberPendingLogEvents();
    }

    /**
     * @return if sending log messages in batch mode
     */
    public boolean isBatchMode() {

        return this.appenderConfig.isBatchMode();
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

        return getDbChannel(null).eventProcessor.getRunId();
    }

    /**
     * Get the current suite id
     *
     * @return the current suite id
     */
    public int getSuiteId() {

        return getDbChannel(null).eventProcessor.getSuiteId();
    }

    /**
     * Get the current run name
     *
     * @return the current run name
     */
    public String getRunName() {

        return getDbChannel(null).eventProcessor.getRunName();
    }

    /**
     * Get the current run user note
     *
     * @return the current run user note
     */
    public String getRunUserNote() {

        return getDbChannel(null).eventProcessor.getRunUserNote();
    }

    /**
     *
     * @return the current testcase id
     */
    public int getTestCaseId() {

        return getDbChannel(null).eventProcessor.getTestCaseId();
    }

    /**
     * @return the last executed, regardless of the finish status (e.g passed/failed/skipped), testcase ID
     * */
    public int getLastExecutedTestCaseId() {

        return getDbChannel(null).eventProcessor.getLastExecutedTestCaseId();
    }

    public boolean getEnableCheckpoints() {

        return this.appenderConfig.getEnableCheckpoints();
    }

    public void setEnableCheckpoints( boolean enableCheckpoints ) {

        this.appenderConfig.setEnableCheckpoints(enableCheckpoints);
    }

    public DbAppenderConfiguration getAppenderConfig() {

        return this.appenderConfig;
    }

    public void setAppenderConfig( DbAppenderConfiguration appenderConfig ) {

        this.appenderConfig = appenderConfig;
        threshold = Level.toLevel(appenderConfig.getLoggingThreshold());
    }

    public void calculateTimeOffset( long executorTimestamp ) {

        // FIXME make the next working
        getDbChannel(null).calculateTimeOffset(executorTimestamp);
    }
}
