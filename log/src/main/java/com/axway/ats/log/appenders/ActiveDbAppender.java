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

import com.axway.ats.core.log.AtsConsoleLogger;
import com.axway.ats.core.threads.ImportantThread;
import com.axway.ats.core.utils.ExecutorUtils;
import com.axway.ats.log.autodb.events.EndRunEvent;
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

    public static final String      DUMMY_DB_HOST                            = "ATS_NO_DB_HOST_SET";
    public static final String      DUMMY_DB_DATABASE                        = "ATS_NO_DB_NAME_SET";
    public static final String      DUMMY_DB_USER                            = "ATS_NO_DB_USER_SET";
    public static final String      DUMMY_DB_PASSWORD                        = "ATS_NO_DB_PASSWORD_SET";

    /**
     * Constructor
     */
    public ActiveDbAppender() {

        super();

        /** create dummy appender configuration 
         *  This configuration will be replaced with one from log4j.xml file
         **/
        appenderConfig.setHost(DUMMY_DB_HOST);
        appenderConfig.setDatabase(DUMMY_DB_DATABASE);
        appenderConfig.setUser(DUMMY_DB_USER);
        appenderConfig.setPassword(DUMMY_DB_PASSWORD);
        
        isAttached = true;

    }

    /* (non-Javadoc)
     * @see org.apache.log4j.AppenderSkeleton#append(org.apache.log4j.spi.LoggingEvent)
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

    /**
     * This method doesn't create a new instance,
     * but returns the already created one (from log4j) or null if there is no such.
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
         * Configuration in log4j.xml file was not found for ActiveDbAppender
         * A dummy DbAppenderConfiguration will be provided
         * in order to prevent NPE when invoking methods such as getRunId()
         */
        new AtsConsoleLogger(ActiveDbAppender.class).warn(
                                                          "ATS Database appender is not specified in log4j.xml file. "
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
            // the executor might be comming from the logging event
            executorId = event.getProperty(ExecutorUtils.ATS_RANDOM_TOKEN);
        }

        if (executorId == null) {
            Thread thisThread = Thread.currentThread();
            if (thisThread instanceof ImportantThread) {
                // a special thread, it holds the executor ID
                executorId = ((ImportantThread) thisThread).getExecutorId();
            } else {
                // use the thread name
                executorId = thisThread.getName();
            }
        }

        return executorId;
    }
}
