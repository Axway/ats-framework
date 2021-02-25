/*
 * Copyright 2017-2020 Axway Software
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
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.ValidHost;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.ValidPort;

import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.DbUtils;
import com.axway.ats.core.dbaccess.mssql.DbConnSQLServer;
import com.axway.ats.core.dbaccess.postgresql.DbConnPostgreSQL;
import com.axway.ats.core.log.AtsConsoleLogger;
import com.axway.ats.core.utils.TimeUtils;
import com.axway.ats.log.autodb.DbAppenderConfiguration;
import com.axway.ats.log.autodb.events.DeleteTestCaseEvent;
import com.axway.ats.log.autodb.events.GetCurrentTestCaseEvent;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.log.autodb.exceptions.DbAppenederException;
import com.axway.ats.log.autodb.io.PGDbReadAccess;
import com.axway.ats.log.autodb.io.SQLServerDbReadAccess;
import com.axway.ats.log.autodb.logqueue.DbEventRequestProcessor;
import com.axway.ats.log.autodb.logqueue.LogEventRequest;
import com.axway.ats.log.autodb.model.AbstractLoggingEvent;
import com.axway.ats.log.autodb.model.EventRequestProcessorListener;
import com.axway.ats.log.autodb.model.IDbReadAccess;

/**
 * This appender is bridge between Log4J2 events and ATS database. In addition it keeps the test execution state like
 * DB connection info, current run and testcase ID. 
 * <p><em>Note</em> that this class is internal for the framework and DB-related public operations are available via 
 * AtsDbLogger</p>
 * It is used only on the Test Executor side and not on ATS Agents.
 */
@Plugin( name = "ActiveDbAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class ActiveDbAppender extends AbstractDbAppender {

    /**
     * We must wait for some event to be processed by the logging thread.
     * This is the time we wait for event processing.
     */
    private static final long       EVENT_WAIT_TIMEOUT                       = 60 * 1000;
    private static final long       EVENT_WAIT_LONG_TIMEOUT                  = 15 * 60 * 1000;
    private static ActiveDbAppender instance                                 = null;

    public static boolean           isAttached                               = false;

    /** enables/disabled logging of messages from @BeforeXXX and @AfterXXX annotated Java methods **/
    public static boolean           isBeforeAndAfterMessagesLoggingSupported = false;

    /* 
     * Sometimes the main thread needs to wait until the logger thread has processed the log event.
     * We use this mutex for synchronization aid. 
     */
    private Object                  listenerMutex                            = new Object();

    public static final String      DUMMY_DB_HOST                            = "ATS_NO_DB_HOST_SET";
    public static final String      DUMMY_DB_DATABASE                        = "ATS_NO_DB_NAME_SET";
    public static final String      DUMMY_DB_USER                            = "ATS_NO_DB_USER_SET";
    public static final String      DUMMY_DB_PASSWORD                        = "ATS_NO_DB_PASSWORD_SET";

    private static IDbReadAccess    dbReadAccess;

    @PluginBuilderFactory
    public static ActiveDbAppenderBuilder newBuilder() {

        return new ActiveDbAppenderBuilder();
    }

    // Note: Try to share this builder by both Active and Passive DbAppender
    // Maybe create AbstractDbAppenderBuilder (note that this class is abstract)
    // And the two separate builders (children of the aforementioned builder) for both Active/Passive DB appenders
    public static class ActiveDbAppenderBuilder
            implements org.apache.logging.log4j.core.util.Builder<ActiveDbAppender> {

        @PluginBuilderAttribute( "name")
        @Required( message = "ActiveDbAppender: no name provided")

        private String         name;

        @PluginElement( "Layout")
        private Layout<String> layout;

        @PluginElement( "Filter")
        private Filter         filter;

        @PluginBuilderAttribute( "host")
        @Required( message = "ActiveDbAppender: no host provided")
        @ValidHost
        private String         host;

        @PluginBuilderAttribute( "port")
        @Required( message = "ActiveDbAppender: no port provided")
        @ValidPort
        private int            port              = -1;

        @PluginBuilderAttribute( "database")
        @Required( message = "ActiveDbAppender: no database provided")
        private String         database;

        @PluginBuilderAttribute( "user")
        @Required( message = "ActiveDbAppender: no user provided")
        private String         user;

        @PluginBuilderAttribute( "password")
        @Required( message = "ActiveDbAppender: no password provided")
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

        public ActiveDbAppenderBuilder setName( String name ) {

            this.name = name;

            return this;
        }

        public Layout<String> getLayout() {

            return layout;
        }

        public ActiveDbAppenderBuilder setLayout( Layout<String> layout ) {

            this.layout = layout;

            return this;
        }

        public Filter getFilter() {

            return filter;
        }

        public ActiveDbAppenderBuilder setFilter( Filter filter ) {

            this.filter = filter;

            return this;
        }

        public String getHost() {

            return host;
        }

        public ActiveDbAppenderBuilder setHost( String host ) {

            this.host = host;

            return this;
        }

        public int getPort() {

            return port;
        }

        public ActiveDbAppenderBuilder setPort( int port ) {

            this.port = port;

            return this;
        }

        public String getDatabase() {

            return database;
        }

        public ActiveDbAppenderBuilder setDatabase( String database ) {

            this.database = database;

            return this;
        }

        public String getUser() {

            return user;
        }

        public ActiveDbAppenderBuilder setUser( String user ) {

            this.user = user;

            return this;
        }

        public String getPassword() {

            return password;
        }

        public ActiveDbAppenderBuilder setPassword( String password ) {

            this.password = password;

            return this;
        }

        public String getDriver() {

            return driver;
        }

        public ActiveDbAppenderBuilder setDriver( String driver ) {

            this.driver = driver;

            return this;
        }

        public String getMode() {

            return mode;
        }

        public ActiveDbAppenderBuilder setMode( String mode ) {

            this.mode = mode;

            return this;
        }

        public String getChunkSize() {

            return chunkSize;
        }

        public ActiveDbAppenderBuilder setChunkSize( String chunkSize ) {

            this.chunkSize = chunkSize;

            return this;
        }

        public int getEvents() {

            return events;
        }

        public ActiveDbAppenderBuilder setEvents( int events ) {

            this.events = events;

            return this;
        }

        public boolean getEnableCheckpoints() {

            return this.enableCheckpoints;
        }

        public ActiveDbAppenderBuilder setChunkSize( boolean enableCheckpoints ) {

            this.enableCheckpoints = enableCheckpoints;

            return this;
        }

        @Override
        public ActiveDbAppender build() {

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
            return new ActiveDbAppender(name, filter, layout, appenderConfiguration);
        }

    }

    /**
     * Constructor that creates a dummy appender.
     * Actually this constructor is not invoked via the log4j2 library, but 
     * is used, then there is no ActiveDbAppender entry in the log4j2.xml file
     * or such appender was not created during runtime.
     * In that way, the user can still use methods from this appender, and those will not result in a NPE being thrown.
     * But those methods will return dummy values, instead of some meaningful once
     */
    public ActiveDbAppender( DbAppenderConfiguration appenderConfiguration ) {

        super("ActiveDbAppender", null, null, appenderConfiguration);

        /**
         * Create dummy event request processor.
         * This processor will be replaced once config from log4j2.xml is loaded
         * */
        eventProcessor = new DbEventRequestProcessor();

        // set the layout to the event processor as well
        if (eventProcessor != null) {
            eventProcessor.setLayout(this.getLayout());
        }

    }

    /**
     * Actual Constructor
     */
    public ActiveDbAppender( String name, Filter filter, Layout<? extends Serializable> layout,
                             DbAppenderConfiguration appenderConfiguration ) {

        super(name, filter, layout, appenderConfiguration);

        /**
         * Create dummy event request processor.
         * This processor will be replaced once config from log4j2.xml is loaded
         * */
        eventProcessor = new DbEventRequestProcessor();

        /* this flag is changed here, since this is the first place where the Apache log4j2 package interacts with this class
         */
        // or modify this flag in the onStart() method(s) ?!?
        isAttached = true;
    }

    @Override
    protected EventRequestProcessorListener getEventRequestProcessorListener() {

        return new SimpleEventRequestProcessorListener(listenerMutex);
    }

    @Override
    public void append( LogEvent event ) {

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
                    // this event has already been through the queue
                    return;
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
                    Level level = LogManager.getRootLogger().getLevel();
                    Configurator.setRootLevel(Level.OFF);

                    AtsConsoleLogger.setLevel(level);

                    // create the queue logging thread and the DbEventRequestProcessor
                    if (queueLogger == null) {
                        initializeDbLogging();
                    }

                    waitForEventToBeExecuted(packedEvent, dbLoggingEvent, false);
                    //this event has already been through the queue

                    /*Revert Logger's level*/
                    Configurator.setRootLevel(level);
                    AtsConsoleLogger.setLevel(level);

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
                    level = LogManager.getRootLogger().getLevel();
                    Configurator.setRootLevel(Level.OFF);

                    AtsConsoleLogger.setLevel(level);

                    waitForEventToBeExecuted(packedEvent, dbLoggingEvent, true);

                    /*Revert Logger's level*/
                    Configurator.setRootLevel(level);
                    AtsConsoleLogger.setLevel(level);

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
     * Get {@link IDbReadAccess} using the appender's db configuration
     * @throws DatabaseAccessException In case of DB error
     * */
    public IDbReadAccess obtainDbReadAccessObject() throws DatabaseAccessException {

        DbConnection dbConnection = null;
        if (dbReadAccess == null) {
            Exception mssqlException = DbUtils.isMSSQLDatabaseAvailable(appenderConfig.getHost(),
                                                                        Integer.parseInt(appenderConfig.getPort()),
                                                                        appenderConfig.getDatabase(),
                                                                        appenderConfig.getUser(),
                                                                        appenderConfig.getPassword());
            if (mssqlException == null) {

                Map<String, Object> props = new HashMap<>();
                props.put(DbKeys.DRIVER, appenderConfig.getDriver().toUpperCase());
                dbConnection = new DbConnSQLServer(appenderConfig.getHost(),
                                                   Integer.parseInt(appenderConfig.getPort()),
                                                   appenderConfig.getDatabase(),
                                                   appenderConfig.getUser(), appenderConfig.getPassword(), props);

                //create the db access layer
                dbReadAccess = new SQLServerDbReadAccess((DbConnSQLServer) dbConnection);

            } else {
                Exception pgsqlException = DbUtils.isPostgreSQLDatabaseAvailable(appenderConfig.getHost(),
                                                                                 Integer.parseInt(appenderConfig.getPort()),
                                                                                 appenderConfig.getDatabase(),
                                                                                 appenderConfig.getUser(),
                                                                                 appenderConfig.getPassword());

                if (pgsqlException == null) {
                    dbConnection = new DbConnPostgreSQL(appenderConfig.getHost(),
                                                        Integer.parseInt(appenderConfig.getPort()),
                                                        appenderConfig.getDatabase(),
                                                        appenderConfig.getUser(), appenderConfig.getPassword(), null);

                    //create the db access layer
                    dbReadAccess = new PGDbReadAccess((DbConnPostgreSQL) dbConnection);
                } else {
                    String errMsg = "Neither MSSQL, nor PostgreSQL server at '" + appenderConfig.getHost() + ":"
                                    + appenderConfig.getPort() +
                                    "' has database with name '" + appenderConfig.getDatabase()
                                    + "'. Exception for MSSQL is : \n\t" + mssqlException
                                    + "\n\nException for PostgreSQL is: \n\t"
                                    + pgsqlException;
                    throw new DatabaseAccessException(errMsg);
                }
            }
        }
        return dbReadAccess;
    }

    /**
     * Here we block the test execution until this event gets executed.
     * If this event fail, we will abort the execution of the tests.
     *
     * @param packedEvent
     * @param event
     */
    private void waitForEventToBeExecuted( LogEventRequest packedEvent, LogEvent event,
                                           boolean waitMoreTime ) {

        synchronized (listenerMutex) {

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

                listenerMutex.wait(timeout);

                if (System.currentTimeMillis() - startTime > timeout - 100) {
                    atsConsoleLogger.warn("The expected "
                                          + event.getClass().getSimpleName()
                                          + " logging event did not complete in " + timeout + " ms");
                }
            } catch (InterruptedException ie) {
                throw new DbAppenederException(TimeUtils.getFormattedDateTillMilliseconds()
                                               + ": "
                                               + "Main thread interrupted while waiting for event "
                                               + event.getClass().getSimpleName(), ie);
            }
        }

        //check for exceptions - if they are none, then we are good to go
        checkForExceptions();

        //this event has already been through the queue
    }

    public String getHost() {

        return appenderConfig.getHost();
    }

    public void setHost( String host ) {

        appenderConfig.setHost(host);
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

    public String getDriver() {

        return appenderConfig.getDriver();
    }

    public void setDriver( String driver ) {

        appenderConfig.setDriver(driver);
    }

    public String geChunkSize() {

        return appenderConfig.getChunkSize();
    }

    public void setChunkSize( String chunkSize ) {

        appenderConfig.setChunkSize(chunkSize);
    }

    /**
     * This method doesn't create a new instance,
     * but returns the already created one (from log4j2) or null if there is no such.
     *
     * @return the current DB appender instance
     */
    public static ActiveDbAppender getCurrentInstance() {

        if (instance == null) {
            final LoggerContext context = LoggerContext.getContext(false);
            final Configuration config = context.getConfiguration();
            Map<String, Appender> appenders = config.getAppenders();
            // there is a method called -> context.getConfiguration().getAppender(java.lang.String name)
            // maybe we should use this one to obtain Active and Passive DB appenders?
            if (appenders != null && appenders.size() > 0) {
                for (Map.Entry<String, Appender> entry : appenders.entrySet()) {
                    Appender appender = entry.getValue();

                    if (appender instanceof ActiveDbAppender) {
                        instance = (ActiveDbAppender) appender;
                        isAttached = true;
                        return instance;
                    }
                }
            }
        }

        if (instance != null) {
            return instance;
        }

        /*
         * Configuration in log4j2.xml file was not found for ActiveDbAppender
         * A dummy com.axway.ats.log.autodb.DbEventRequestProcessor is
         * created in order to prevent NPE when invoking methods such as getRunId()
         */
        new AtsConsoleLogger(ActiveDbAppender.class).warn(
                                                          "ATS Database appender is not specified in log4j2.xml file. "
                                                          + "Methods such as ActiveDbAppender@getRunId() will not work.");

        isAttached = false;
        /** create dummy appender configuration 
         *  This configuration will be replaced with one from log4j2.xml file
         * */
        DbAppenderConfiguration appenderConfig = new DbAppenderConfiguration();
        appenderConfig.setHost(DUMMY_DB_HOST);
        appenderConfig.setDatabase(DUMMY_DB_DATABASE);
        appenderConfig.setUser(DUMMY_DB_USER);
        appenderConfig.setPassword(DUMMY_DB_PASSWORD);
        instance = new ActiveDbAppender(appenderConfig);
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

        private Object listenerMutex;

        SimpleEventRequestProcessorListener( Object listenerMutex ) {

            this.listenerMutex = listenerMutex;
        }

        public void onRunStarted() {

            synchronized (listenerMutex) {
                listenerMutex.notifyAll();
            }
        }

        public void onRunFinished() {

            synchronized (listenerMutex) {
                listenerMutex.notifyAll();
            }
        }

        public void onTestcaseStarted() {

            synchronized (listenerMutex) {
                listenerMutex.notifyAll();
            }
        }

        public void onTestcaseFinished() {

            synchronized (listenerMutex) {
                listenerMutex.notifyAll();
            }
        }
    }

}
