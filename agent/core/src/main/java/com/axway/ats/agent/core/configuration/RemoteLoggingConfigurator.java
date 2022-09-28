/*
 * Copyright 2017-2022 Axway Software
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
package com.axway.ats.agent.core.configuration;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Appender;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.axway.ats.core.threads.ThreadsPerCaller;
import com.axway.ats.log.LogLevel;
import com.axway.ats.log.appenders.AbstractDbAppender;
import com.axway.ats.log.appenders.ActiveDbAppender;
import com.axway.ats.log.appenders.PassiveDbAppender;
import com.axway.ats.log.autodb.DbAppenderConfiguration;
import com.axway.ats.log.autodb.filters.NoSystemLevelEventsFilter;

/**
 * This configurator is used for configuring logging on remote systems
 */
public class RemoteLoggingConfigurator implements Configurator {

    private static final long       serialVersionUID  = 1L;

    private DbAppenderConfiguration appenderConfiguration;
    private String                  appenderLogger;

    private static final int        DEFAULT_LOG_LEVEL = Level.DEBUG_INT;

    // list of other logger levels defined by the user in their log4j configuration file
    private Map<String, Integer>    otherLoggerLevels = new HashMap<String, Integer>();

    // flags telling us what exactly to configure
    private boolean                 needsToConfigureDbAppender;
    private boolean                 needsToConfigureUserLoggers;

    /**
     * We will try to find if a DB appender has been configured on the local system
     * 
     * @param customLogLevel log level specified in the test ( or use AgentConfigurationLandscape.getInstance(atsAgent).getDbLogLevel() )
     * @param chunkSize chunk size (for batch db logging) specified in the test ( or use AgentConfigurationLandscape.getInstance(atsAgent).getChunkSize() )
     */
    @SuppressWarnings( "unchecked")
    public RemoteLoggingConfigurator( LogLevel customLogLevel, int chunkSize ) {

        /*
         * This code is run on:
         *  - Test Executor side prior to calling an agent for first time in this testcase
         *  - Agent side prior to calling another chained agent for first time in this testcase 
         * 
         * we use this code to remember the logging configuration which we will pass to some agent
         */

        // look for the DB appender
        Category log = Logger.getLogger("com.axway.ats");
        boolean dbAppenderIsProcessed = false;
        while (log != null && !dbAppenderIsProcessed) {

            Enumeration<Appender> appenders = log.getAllAppenders();
            while (appenders.hasMoreElements()) {
                Appender appender = appenders.nextElement();

                if (appender.getClass() == ActiveDbAppender.class // running on Test Executor side
                    || appender.getClass() == PassiveDbAppender.class // running on Agent side 
                ) {
                     if (appender.getClass() == PassiveDbAppender.class) {
                        // since there can be multiple PassiveDbAppenders (each with multiple channels) on the Agent
                        // we must find the one created from the current caller
                        if (!ThreadsPerCaller.getCaller().equals( ((PassiveDbAppender) appender).getCallerId())) {
                            continue;
                        }
                    }
                    //we found the appender, read all properties
                    appenderConfiguration = ((AbstractDbAppender) appender).getAppenderConfig();
                    if (chunkSize > 0) {
                        // should a warning be logged here if the chunk size is not valid?
                        appenderConfiguration.setChunkSize(chunkSize + "");
                    }
                    appenderLogger = log.getName();

                    int atsDbLogLevel = DEFAULT_LOG_LEVEL;
                    if (customLogLevel != null) {
                        // user specified in the test the log level for this agent
                        atsDbLogLevel = customLogLevel.toInt();
                    } else if (log.getLevel() != null) {
                        // user specified the log level in log4j configuration file
                        atsDbLogLevel = log.getLevel().toInt();

                    }

                    //set the effective logging level for threshold if new one is set
                    if (appenderConfiguration.getLoggingThreshold() == -1
                        || appenderConfiguration.getLoggingThreshold() != atsDbLogLevel) {

                        /*
                         * Log4j is deprecating the Priority class used by setLoggingThreshold,
                         * but we cannot make an instance of this class as its constructor is not public.
                         * 
                         * So here we first change the log level on the Test Executor,
                         * then get the Priority object, then restore back the value on the Test Executor
                         */
                        final Level currentLevelBackup = log.getLevel();
                        log.setLevel(Level.toLevel(atsDbLogLevel));
                        appenderConfiguration.setLoggingThreshold(log.getEffectiveLevel().toInt());
                        log.setLevel(currentLevelBackup);
                    }

                    //exit the loop
                    dbAppenderIsProcessed = true;
                    break;
                }
            }

            log = log.getParent();
            }

        // look for any user loggers
        Enumeration<Logger> allLoggers = Logger.getRootLogger().getLoggerRepository().getCurrentLoggers();
        while (allLoggers.hasMoreElements()) {
            Logger logger = allLoggers.nextElement();

            Level level = logger.getLevel();
            if (level != null) {
                // user explicitly specified a level for this logger
                otherLoggerLevels.put(logger.getName(), level.toInt());
            }
        }
    }

    @Override
    @SuppressWarnings( "unchecked")
    public void apply() {

        /*
         * This code is run on remote agent's side
         */

        if (needsToConfigureDbAppender) {
            //first get all appenders in the root category and apply the filter
            //which will deny logging of system events
            Logger rootLogger = Logger.getRootLogger();
            Enumeration<Appender> appenders = rootLogger.getAllAppenders();
            while (appenders.hasMoreElements()) {
                Appender appender = appenders.nextElement();
                if (! (appender instanceof AbstractDbAppender)) {
                    // apply this filter on all appenders which are not coming from ATS
                    appender.addFilter(new NoSystemLevelEventsFilter());
                }
            }

            //attach the appender to the logging system
            Category log;
            if ("root".equals(appenderLogger)) {
                log = Logger.getRootLogger();
            } else {
                log = Logger.getLogger(appenderLogger);
            }

            log.setLevel(Level.toLevel(appenderConfiguration.getLoggingThreshold()));

            //final String caller = ThreadsPerCaller.getCaller();

            //create the new appender
            PassiveDbAppender attachedAppender = new PassiveDbAppender();
            attachedAppender.setAppenderConfig(appenderConfiguration);
            //use a default pattern, as we log in the db
            attachedAppender.setLayout(new PatternLayout("%c{2}: %m%n"));
            attachedAppender.activateOptions();

            log.addAppender(attachedAppender);
        }

        if (needsToConfigureUserLoggers) {
            for (Entry<String, Integer> userLogger : otherLoggerLevels.entrySet()) {
                /* 
                 * We want to set the level of this logger.
                 * It is not important if this logger is already attached to log4j system or 
                 * not as the next code will obtain it(in case logger exists) or will create it 
                 * and then will set its level
                 */
                Logger.getLogger(userLogger.getKey())
                      .setLevel(Level.toLevel(userLogger.getValue()));
            }
        }
    }

    @Override
    @SuppressWarnings( "unchecked")
    public boolean needsApplying() {

        /*
         * This code is run on remote agent's side
         */

        final String caller = ThreadsPerCaller.getCaller();

        needsToConfigureDbAppender = false;
        if (appenderConfiguration != null) {
            PassiveDbAppender dbAppender = PassiveDbAppender.getCurrentInstance();
            if (dbAppender == null || !dbAppender.getAppenderConfig().equals(appenderConfiguration)) {
                // we did not have a DB appender
                // or the DB appender configuration is changed
                needsToConfigureDbAppender = true;
            }
        }

        needsToConfigureUserLoggers = false;
        for (Entry<String, Integer> userLogger : otherLoggerLevels.entrySet()) {
            Enumeration<Logger> allLoggers = Logger.getRootLogger().getLoggerRepository().getCurrentLoggers();

            boolean loggerAlreadyExists = false;
            boolean loggerLevelIsDifferent = false;
            while (allLoggers.hasMoreElements()) {
                Logger logger = allLoggers.nextElement();

                if (logger.getName().equals(userLogger.getKey())) {
                    // this logger is already available, check its level
                    loggerAlreadyExists = true;

                    if (logger.getLevel() == null
                        || logger.getLevel().toInt() != userLogger.getValue()) {
                        // logger level is not set or it is not correct
                        loggerLevelIsDifferent = true;
                    }

                    break;
                }
            }

            if (!loggerAlreadyExists || loggerLevelIsDifferent) {
                // there is at lease one logger that must be reconfigured
                needsToConfigureUserLoggers = true;
                break;
            }
        }

        return needsToConfigureDbAppender || needsToConfigureUserLoggers;
    }

    @Override
    @SuppressWarnings( "unchecked")
    public void revert() {

        /*
         * This code is run on remote agent's side
         */

        /*
         * The outer code will call this method when the "needs applying" method return true.
         * 
         * Currently we could come here because of 2 reasons:
         *  - the DB appenders must be reconfigured
         *  - the user loggers must be reconfigured
         */

        if (appenderLogger != null && needsToConfigureDbAppender) {
            // there is a DB appender and it is out-of-date

            //remove the filter which will deny logging of system events
            Logger rootLogger = Logger.getRootLogger();
            Enumeration<Appender> appenders = rootLogger.getAllAppenders();
            while (appenders.hasMoreElements()) {
                Appender appender = appenders.nextElement();

                    //remove the filter
                    //FIXME: This is very risky, as someone may have added other filters
                //the current implementation of the filter chain in log4j will not allow
                //us to easily remove a single filter
                appender.clearFilters();
            }

            Category log;
            if ("root".equals(appenderLogger)) {
                log = Logger.getRootLogger();
            } else {
                log = Logger.getLogger(appenderLogger);
            }

            Appender dbAppender = PassiveDbAppender.getCurrentInstance();
            if (dbAppender != null) {
                //close the appender
                dbAppender.close();

                //remove it
                log.removeAppender(dbAppender);
            }
        }

        // in case we must reconfigure the custom loggers, here we should remove them from log4j, 
        // but log4j does not provide a way to do it - so we do nothing here 
    }

    @Override
    public String getDescription() {

        return "logging configuration";
    }
}
