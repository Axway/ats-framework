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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.DbUtils;
import com.axway.ats.core.dbaccess.mssql.DbConnSQLServer;
import com.axway.ats.core.dbaccess.postgresql.DbConnPostgreSQL;
import com.axway.ats.core.log.AtsConsoleLogger;
import com.axway.ats.core.threads.ImportantThread;
import com.axway.ats.core.utils.ExecutorUtils;
import com.axway.ats.log.autodb.events.EndRunEvent;
import com.axway.ats.log.autodb.events.GetCurrentTestCaseEvent;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.log.autodb.io.PGDbReadAccess;
import com.axway.ats.log.autodb.io.SQLServerDbReadAccess;
import com.axway.ats.log.autodb.logqueue.DbEventRequestProcessor;
import com.axway.ats.log.autodb.logqueue.EventProcessorState;
import com.axway.ats.log.autodb.model.IDbReadAccess;

/**
 * This appender is bridge between Log4J events and ATS database. In addition it keeps the test execution state like
 * It works on the Test Executor side.	 * DB connection info, current run and testcase ID. 
 * <p><em>Note</em> that this class is internal for the framework and DB-related public operations are available via 
 * AtsDbLogger</p>
 * It is used only on the Test Executor side and not on ATS Agents.
 */

public class ActiveDbAppender extends AbstractDbAppender {

    private static ActiveDbAppender   instance                                 = null;

    public static boolean             isAttached                               = false;

    /**
     * enables/disabled logging of messages from @BeforeXXX and @AfterXXX annotated
     * Java methods
     **/
    public static boolean             isBeforeAndAfterMessagesLoggingSupported = false;

    /**
     * Holds information about the run (id, name, etc) <br>
     * The information is populated inside {@link DbEventRequestProcessor} startRun
     * method <br>
     * Even when we have parallel test execution, the starting and ending of a Run,
     * is performed on the main thread, so this is where we populate the run
     * information. <br>
     * Each other thread does not have that Run information.
     */
    public static EventProcessorState runState                                 = new EventProcessorState();

    public static final String        DUMMY_DB_HOST                            = "ATS_NO_DB_HOST_SET";
    public static final String        DUMMY_DB_DATABASE                        = "ATS_NO_DB_NAME_SET";
    public static final String        DUMMY_DB_USER                            = "ATS_NO_DB_USER_SET";
    public static final String        DUMMY_DB_PASSWORD                        = "ATS_NO_DB_PASSWORD_SET";

    private static IDbReadAccess      dbReadAccess;

    /**
     * Constructor
     */
    public ActiveDbAppender() {

        super();

        /**
         * create dummy appender configuration This configuration will be replaced with
         * one from log4j.xml file
         **/
        appenderConfig.setHost(DUMMY_DB_HOST);
        appenderConfig.setDatabase(DUMMY_DB_DATABASE);
        appenderConfig.setUser(DUMMY_DB_USER);
        appenderConfig.setPassword(DUMMY_DB_PASSWORD);

        isAttached = true;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.log4j.AppenderSkeleton#append(org.apache.log4j.spi.LoggingEvent)
     */
    @Override
    protected void append( LoggingEvent event ) {

        // We call the next method, so log4j will internally remember the thread name.
        // See the internal implementation for details.
        event.getThreadName();

        getDbChannel(event).append(event);

        if (event instanceof EndRunEvent) {
            destroyAllChannels(true);
        }

    }

    @Override
    public GetCurrentTestCaseEvent getCurrentTestCaseState( GetCurrentTestCaseEvent event ) {

        DbChannel channel = getDbChannel(null);

        channel.testCaseState.setRunId(channel.eventProcessor.getRunId());
        // get current test case id which will be passed to ATS agent
        event.setTestCaseState(channel.testCaseState);
        return event;
    }

    /**
     * Get {@link IDbReadAccess} using the appender's db configuration
     * 
     * @throws DatabaseAccessException In case of DB error
     */
    public IDbReadAccess obtainDbReadAccessObject() throws DatabaseAccessException {

        DbConnection dbConnection = null;
        if (dbReadAccess == null) {
            Exception mssqlException = null;
            try {
                DbUtils.checkMssqlDatabaseAvailability(appenderConfig.getHost(),
                                                       Integer.parseInt(appenderConfig.getPort()),
                                                       appenderConfig.getDatabase(),
                                                       appenderConfig.getUser(),
                                                       appenderConfig.getPassword());
            } catch (Exception e) {
                mssqlException = e;
            }

            if (mssqlException == null) {

                Map<String, Object> props = new HashMap<>();
                props.put(DbKeys.DRIVER, appenderConfig.getDriver().toUpperCase());
                dbConnection = new DbConnSQLServer(appenderConfig.getHost(), Integer.parseInt(appenderConfig.getPort()),
                                                   appenderConfig.getDatabase(), appenderConfig.getUser(),
                                                   appenderConfig.getPassword(), props);

                // create the db access layer
                dbReadAccess = new SQLServerDbReadAccess((DbConnSQLServer) dbConnection);

            } else {
                Exception pgsqlException = null;
                try {
                    DbUtils.checkPgsqlDatabaseAvailability(appenderConfig.getHost(),
                                                           Integer.parseInt(appenderConfig.getPort()),
                                                           appenderConfig.getDatabase(),
                                                           appenderConfig.getUser(),
                                                           appenderConfig.getPassword());
                } catch (Exception e) {
                    pgsqlException = e;
                }

                if (pgsqlException == null) {
                    dbConnection = new DbConnPostgreSQL(appenderConfig.getHost(),
                                                        Integer.parseInt(appenderConfig.getPort()),
                                                        appenderConfig.getDatabase(),
                                                        appenderConfig.getUser(), appenderConfig.getPassword(), null);

                    // create the db access layer
                    dbReadAccess = new PGDbReadAccess((DbConnPostgreSQL) dbConnection);
                } else {
                    String errMsg = "Neither MSSQL, nor PostgreSQL server at '" + appenderConfig.getHost() + ":"
                                    + appenderConfig.getPort() + "' has database with name '"
                                    + appenderConfig.getDatabase()
                                    + "'. Exception for MSSQL is : \n\t" + mssqlException
                                    + "\n\nException for PostgreSQL is: \n\t" + pgsqlException;
                    throw new DatabaseAccessException(errMsg);
                }
            }
        }
        return dbReadAccess;
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
     * This method doesn't create a new instance, but returns the already created
     * one (from log4j) or null if there is no such.
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
                    isAttached = true;
                    return instance;
                }
            }
        }

        if (instance != null) {
            return instance;
        }

        /*
         * Configuration in log4j.xml file was not found for ActiveDbAppender A dummy
         * DbAppenderConfiguration will be provided in order to prevent NPE when
         * invoking methods such as getRunId()
         */
        new AtsConsoleLogger(ActiveDbAppender.class).warn("ATS Database appender is not specified in log4j.xml file. "
                                                          + "Methods such as ActiveDbAppender@getRunId() will not work.");

        isAttached = false;
        instance = new ActiveDbAppender();
        return instance;
    }

    @Override
    protected String getDbChannelKey( LoggingEvent event ) {

        // Works on Test Executor side
        // Have a channel per execution thread

        String executorId = null;

        if (event != null) {
            // the executor might be coming from the logging event's properties
            executorId = event.getProperty(ExecutorUtils.ATS_THREAD_ID);
        }

        if (executorId == null) {
            Thread thisThread = Thread.currentThread();
            if (thisThread instanceof ImportantThread) {
                // a special thread, it holds the executor ID
                executorId = ((ImportantThread) thisThread).getExecutorId();
            } else {
                // use the thread name
                executorId = thisThread.getId() + "";
            }
        }

        return executorId;
    }
}
