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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.ValidHost;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.ValidPort;

import com.axway.ats.core.log.AtsConsoleLogger;
import com.axway.ats.core.threads.ThreadsPerCaller;
import com.axway.ats.log.Log4j2Utils;
import com.axway.ats.log.autodb.DbAppenderConfiguration;
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
@Plugin( name = "PassiveDbAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class PassiveDbAppender extends AbstractDbAppender {

    private static final AtsConsoleLogger         consoleLogger = new AtsConsoleLogger(PassiveDbAppender.class);

    private static Map<String, PassiveDbAppender> instances     = Collections.synchronizedMap(new HashMap<String, PassiveDbAppender>());

    /* 
     * The caller this appender is serving.
     * We are calling this constructor in a way which guarantees the provided caller is not null
     */
    private String                                caller;

    @PluginBuilderFactory
    public static PassiveDbAppenderBuilder newBuilder() {

        return new PassiveDbAppenderBuilder();
    }

    public static class PassiveDbAppenderBuilder
            implements org.apache.logging.log4j.core.util.Builder<PassiveDbAppender> {

        @PluginBuilderAttribute( "name")
        @Required( message = "PassiveDbAppender: no name provided")

        private String         name;

        @PluginElement( "Layout")
        private Layout<String> layout;

        @PluginElement( "Filter")
        private Filter         filter;

        @PluginBuilderAttribute( "host")
        @Required( message = "PassiveDbAppender: no host provided")
        @ValidHost
        private String         host;

        @PluginBuilderAttribute( "port")
        @Required( message = "PassiveDbAppender: no port provided")
        @ValidPort
        private int            port              = -1;

        @PluginBuilderAttribute( "database")
        @Required( message = "PassiveDbAppender: no database provided")
        private String         database;

        @PluginBuilderAttribute( "user")
        @Required( message = "PassiveDbAppender: no user provided")
        private String         user;

        @PluginBuilderAttribute( "password")
        @Required( message = "PassiveDbAppender: no password provided")
        private String         password;

        @PluginBuilderAttribute( "driver")
        private String         driver;

        // Note that only "batch" value is supported
        @PluginBuilderAttribute( "mode")
        private String         mode;

        // Note that this is supported only if mode = 'batch'
        @PluginBuilderAttribute( "chunkSize")
        private String         chunkSize;

        @PluginBuilderAttribute( "events")
        private int            events;

        @PluginBuilderAttribute( "enableCheckpoints")
        private boolean        enableCheckpoints = true;

        public String getName() {

            return name;
        }

        public PassiveDbAppenderBuilder setName( String name ) {

            this.name = name;

            return this;
        }

        public Layout<String> getLayout() {

            return layout;
        }

        public PassiveDbAppenderBuilder setLayout( Layout<String> layout ) {

            this.layout = layout;

            return this;
        }

        public Filter getFilter() {

            return filter;
        }

        public PassiveDbAppenderBuilder setFilter( Filter filter ) {

            this.filter = filter;

            return this;
        }

        public String getHost() {

            return host;
        }

        public PassiveDbAppenderBuilder setHost( String host ) {

            this.host = host;

            return this;
        }

        public int getPort() {

            return port;
        }

        public PassiveDbAppenderBuilder setPort( int port ) {

            this.port = port;

            return this;
        }

        public String getDatabase() {

            return database;
        }

        public PassiveDbAppenderBuilder setDatabase( String database ) {

            this.database = database;

            return this;
        }

        public String getUser() {

            return user;
        }

        public PassiveDbAppenderBuilder setUser( String user ) {

            this.user = user;

            return this;
        }

        public String getPassword() {

            return password;
        }

        public PassiveDbAppenderBuilder setPassword( String password ) {

            this.password = password;

            return this;
        }

        public String getDriver() {

            return driver;
        }

        public PassiveDbAppenderBuilder setDriver( String driver ) {

            this.driver = driver;

            return this;
        }

        public String getMode() {

            return mode;
        }

        public PassiveDbAppenderBuilder setMode( String mode ) {

            this.mode = mode;

            return this;
        }

        public String getChunkSize() {

            return chunkSize;
        }

        public PassiveDbAppenderBuilder setChunkSize( String chunkSize ) {

            this.chunkSize = chunkSize;

            return this;
        }

        public int getEvents() {

            return events;
        }

        public PassiveDbAppenderBuilder setEvents( int events ) {

            this.events = events;

            return this;
        }

        public boolean getEnableCheckpoints() {

            return this.enableCheckpoints;
        }

        public PassiveDbAppenderBuilder setEnableCheckpoints( boolean enableCheckpoints ) {

            this.enableCheckpoints = enableCheckpoints;

            return this;
        }

        @Override
        public PassiveDbAppender build() {

            DbAppenderConfiguration appenderConfiguration = new DbAppenderConfiguration();
            appenderConfiguration.setHost(this.host);
            if (port == -1) {
                appenderConfiguration.setPort(null);
            } else {
                appenderConfiguration.setPort(this.port + "");
            }
            appenderConfiguration.setDatabase(this.database);
            appenderConfiguration.setUser(this.user);
            appenderConfiguration.setPassword(this.password);
            appenderConfiguration.setMode(this.mode);
            appenderConfiguration.setDriver(this.driver);
            appenderConfiguration.setChunkSize(this.chunkSize);
            appenderConfiguration.setEnableCheckpoints(enableCheckpoints);
            appenderConfiguration.setMaxNumberLogEvents(this.events + "");
            // Note: logging threshold is set in the parent constructor
            return new PassiveDbAppender(ThreadsPerCaller.getCaller(), name, filter, layout, appenderConfiguration);
        }

    }

    /**
     * Constructor
     */
    public PassiveDbAppender( String caller, String name, Filter filter, Layout<? extends Serializable> layout,
                              DbAppenderConfiguration appenderConfiguration ) {

        super(name, filter, layout, appenderConfiguration);

        this.caller = caller;

        initializeDbLogging(this.caller);

        boolean explicitSetOfLevel = false;
        if (AtsConsoleLogger.getLevel() == null) {
            explicitSetOfLevel = true;
            AtsConsoleLogger.setLevel(Level.WARN);
        }
        consoleLogger.info("PassiveDbAppender for caller '" + caller + "' is attached.");
        if(explicitSetOfLevel) {
            AtsConsoleLogger.setLevel(null);
        }
        // save the newly created PassiveDbAppender
        instances.put(this.caller, this);
    }

    @Override
    protected EventRequestProcessorListener getEventRequestProcessorListener() {

        return null;
    }

    public String getCaller() {

        return caller;
    }

    /* (non-Javadoc)
     * @see org.apache.logging.log4j.core.appender.AbstractAppender#append(org.apache.logging.log4j.core.LogEvent)
     */
    @Override
    public void append(
                        LogEvent event ) {

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
    public static PassiveDbAppender getCurrentInstance(
                                                        String caller ) {

        boolean explicitSetOfLevel = false;
        if (AtsConsoleLogger.getLevel() == null) {
            explicitSetOfLevel = true;
            AtsConsoleLogger.setLevel(Level.WARN);
        }

        // try via log4j2 configuration
        Map<String, Appender> appenders = Log4j2Utils.getAllAppenders();
        if (appenders != null) {
            for (Map.Entry<String, Appender> entry : appenders.entrySet()) {
                String appenderName = entry.getKey();
                Appender appender = entry.getValue();
                if (appender == null) {
                    consoleLogger.warn("Null appender with name '" + appenderName
                                       + "' found in log4j2 configuration. Removing the appender...");
                    Log4j2Utils.removeAppender(appenderName);
                    consoleLogger.warn("Appender with name '" + appenderName + "' successfully removed.");
                    continue;
                }
                // non-null appender found. check if it is a instance of PassiveDbAppender
                if (appender instanceof PassiveDbAppender) {
                    // PassiveDbAppender found!
                    if (appender.isStopped()) {
                        // stopped PassiveDbAppender found. Remove it!
                        consoleLogger.warn("Stopped PassiveDbAppender for caller '"
                                           + ((PassiveDbAppender) appender).getCaller()
                                           + "' found in log4j2 configuration. Removing the appender...");
                        Log4j2Utils.removeAppender(appenderName);
                        consoleLogger.warn("PassiveDbAppender for caller '" + ((PassiveDbAppender) appender).getCaller()
                                           + "' successfully removed.");
                        // remove the appender in the instances variables as well, since it can no longer be used
                        instances.remove( ((PassiveDbAppender) appender).getCaller());
                        continue;
                    }
                    // non-stopped PassiveDbAppender found
                    if ( ((PassiveDbAppender) appender).getCaller().equals(caller)) {
                        // return this appender ASAP
                        consoleLogger.warn("Previous PassiveDbAppender for caller '"
                                           + ((PassiveDbAppender) appender).getCaller()
                                           + "' found in log4j2 configuration.");
                        if (explicitSetOfLevel) {
                            AtsConsoleLogger.setLevel(null);
                        }
                        return ((PassiveDbAppender) appender);
                    }
                }
            }
        } else {
            consoleLogger.warn("No PassiveDbAppender found in log4j2 configuration. Trying workaround method....");
            // no appender found in log4j2 configuration
            // try using instances variables to obtain PassiveDbAppender
            if (instances.containsKey(caller)) {
                // PassiveDbAppender found! Check if it is non-null and still running
                PassiveDbAppender dbAppender = instances.get(caller);
                if (dbAppender == null) {
                    consoleLogger.warn("Null PassiveDbAppender for caller '" + caller
                                       + "' found via workaround. Removing the appender...");
                    instances.remove(caller);
                    consoleLogger.warn("Null PassiveDbAppender for caller '" + caller
                                       + "' found via workaround successfully removed.");
                    // since this was last chance to find a PassiveDbAppender for this caller.
                    // and this appender was null, just return null
                    if (explicitSetOfLevel) {
                        AtsConsoleLogger.setLevel(null);
                    }
                    return null;
                } else {
                    if (dbAppender.isStopped()) {
                        consoleLogger.warn("Stopped PassiveDbAppender for caller '" + caller
                                           + "' found via workaround. Removing the appender...");
                        instances.remove(caller);
                        consoleLogger.warn("Stopped PassiveDbAppender for caller '" + caller
                                           + "' found via workaround successfully removed.");
                        // since this was last chance to find a PassiveDbAppender for this caller.
                        // and this appender was null, just return null
                        if (explicitSetOfLevel) {
                            AtsConsoleLogger.setLevel(null);
                        }
                        return null;
                    } else {
                        // the appender was found, but why it is not in the log4j2 configuration?
                        // One possible reason is that event if we have multiple passiveDbAppenders, they all have the same name
                        consoleLogger.info("PassiveDbAppender for caller '" + caller + "' found via workaround. Attaching it to the log4j2 configuration...");
                        Log4j2Utils.addAppenderToLogger(dbAppender, Log4j2Utils.getRootLogger().getName());
                        consoleLogger.info("PassiveDbAppender for caller '" + caller + "' found via workaround successfully attached.");
                        if (explicitSetOfLevel) {
                            AtsConsoleLogger.setLevel(null);
                        }
                        return dbAppender;
                    }
                }
            }
        }

        if (explicitSetOfLevel) {
            AtsConsoleLogger.setLevel(null);
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

    @Override
    public boolean stop( long timeout, TimeUnit timeUnit ) {
        instances.remove(this.caller);
        return super.stop(timeout, timeUnit);
    }

    @Override
    protected boolean stop( long timeout, TimeUnit timeUnit, boolean changeLifeCycleState ) {
        instances.remove(this.caller);
        return super.stop(timeout, timeUnit, changeLifeCycleState);
    }

    @Override
    public void stop() {
        super.stop();
        instances.remove(this.caller);
    }

    @Override
    protected boolean stop( Future<?> future ) {
        instances.remove(this.caller);
        return super.stop(future);
    }
    
    
}
