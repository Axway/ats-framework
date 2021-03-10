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
package com.axway.ats.agent.core.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.ThresholdFilter;

import com.axway.ats.core.threads.ThreadsPerCaller;
import com.axway.ats.log.Log4j2Utils;
import com.axway.ats.log.LogLevel;
import com.axway.ats.log.appenders.AbstractDbAppender;
import com.axway.ats.log.appenders.ActiveDbAppender;
import com.axway.ats.log.appenders.PassiveDbAppender;
import com.axway.ats.log.appenders.PassiveDbAppender.PassiveDbAppenderBuilder;
import com.axway.ats.log.autodb.DbAppenderConfiguration;
import com.axway.ats.log.autodb.filters.NoSystemLevelEventsFilter;
import com.axway.ats.log.model.SystemLogLevel;

/**
 * This configurator is used for configuring logging on remote systems
 */
public class RemoteLoggingConfigurator implements Configurator {

    private static final long       serialVersionUID  = 1L;

    private DbAppenderConfiguration appenderConfiguration;
    private String                  appenderLogger;

    private static final int        DEFAULT_LOG_LEVEL = Level.DEBUG.intLevel();

    // list of other logger levels defined by the user in their log4j2 configuration file
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
    public RemoteLoggingConfigurator( LogLevel customLogLevel, int chunkSize ) {

        /*
         * This code is run on:
         *  - Test Executor side prior to calling an agent for first time in this testcase
         *  - Agent side prior to calling another chained agent for first time in this testcase 
         * 
         * we use this code to remember the logging configuration which we will pass to some agent
         */

        // look for the DB appender
        LoggerConfig loggerConfig = Log4j2Utils.getLoggerConfig("com.axway.ats");
        boolean dbAppenderIsProcessed = false;
        // get all logger's appender
        while (loggerConfig != null && !dbAppenderIsProcessed) {

            Map<String, Appender> appenders = loggerConfig.getAppenders();

            for (Appender appender : appenders.values()) {

                if (appender.getClass() == ActiveDbAppender.class // running on Test Executor side
                    || appender.getClass() == PassiveDbAppender.class // running on Agent side 
                ) {
                    //we found the appender, read all properties
                    appenderConfiguration = ((AbstractDbAppender) appender).getAppenderConfig();
                    if (chunkSize > 0) {
                        // should a warning be logged here if the chunk size is not valid?
                        appenderConfiguration.setChunkSize(chunkSize + "");
                    }
                    appenderLogger = loggerConfig.getName();

                    int atsDbLogLevel = DEFAULT_LOG_LEVEL;
                    if (customLogLevel != null) {
                        // user specified in the test the log level for this agent
                        atsDbLogLevel = customLogLevel.toInt();
                    } else if (loggerConfig.getLevel() != null) {
                        // user specified the log level in log4j2 configuration file
                        atsDbLogLevel = loggerConfig.getLevel().intLevel();

                    }

                    //set the effective logging level for threshold if new one is set
                    if (appenderConfiguration.getLoggingThreshold() == null
                        || appenderConfiguration.getLoggingThreshold().intLevel() != atsDbLogLevel) {

                        final Level currentLevelBackup = loggerConfig.getLevel();
                        loggerConfig.setLevel(SystemLogLevel.toLevel(atsDbLogLevel));
                        appenderConfiguration.setLoggingThreshold(loggerConfig.getLevel());
                        loggerConfig.setLevel(currentLevelBackup);

                    }

                    //exit the loop
                    dbAppenderIsProcessed = true;
                    break;
                }

            }

            loggerConfig = loggerConfig.getParent();

        }
        // look for any user loggers
        // should this be done in the previous for cycle?!?
        for (LoggerConfig logger : Log4j2Utils.getAllLoggers().values()) {
            Level level = logger.getLevel();
            if (level != null) {
                // user explicitly specified a level for this logger
                otherLoggerLevels.put(logger.getName(), level.intLevel());
            }
        }
    }

    @Override
    public void apply() {

        /*
         * This code is run on remote agent's side
         */

        if (needsToConfigureDbAppender) {
            //first get all appenders and apply the filter which will deny logging of system events
            Map<String, Appender> appenders = Log4j2Utils.getAllAppenders();
            for (Appender appender : appenders.values()) {
                if (! (appender instanceof AbstractDbAppender)) {
                    // apply this filter on all appenders which are not coming from ATS
                    ((AbstractAppender) appender).addFilter(new NoSystemLevelEventsFilter());
                }
            }

            //attach the appender to the logging system
            Logger log;
            if ("".equals(appenderLogger)) { // don't ask me why, but the root loggerConfig's name is not root, but and empty String!
                log = Log4j2Utils.getRootLogger();
            } else {
                log = Log4j2Utils.getLogger(appenderLogger);
            }
            Log4j2Utils.setLoggerLevel(log.getName(),
                                       SystemLogLevel.toLevel(appenderConfiguration.getLoggingThreshold().intLevel()));

            PassiveDbAppenderBuilder builder = PassiveDbAppender.newBuilder();
            //use a default pattern, as we log in the db
            builder.setLayout(org.apache.logging.log4j.core.layout.PatternLayout.newBuilder()
                                                                                .withPattern("%c{2}: %m%n")
                                                                                .build());

            builder.setChunkSize(appenderConfiguration.getChunkSize());
            builder.setDatabase(appenderConfiguration.getDatabase());
            builder.setDriver(appenderConfiguration.getDriver());
            builder.setEnableCheckpoints(appenderConfiguration.getEnableCheckpoints());
            builder.setEvents(appenderConfiguration.getMaxNumberLogEvents());
            builder.setFilter(ThresholdFilter.createFilter(appenderConfiguration.getLoggingThreshold(),
                                                           Filter.Result.ACCEPT, Filter.Result.DENY));
            builder.setHost(appenderConfiguration.getHost());
            builder.setMode( (appenderConfiguration.isBatchMode())
                                                                   ? "batch"
                                                                   : "");
            builder.setName(PassiveDbAppender.class.getSimpleName());
            builder.setPassword(appenderConfiguration.getPassword());
            builder.setPort(Integer.parseInt(appenderConfiguration.getPort()));
            builder.setUser(appenderConfiguration.getUser());
            //create the new appender
            PassiveDbAppender attachedAppender = builder.build();

            attachedAppender.start();

            // yet again could this cast fail?!?
            ((org.apache.logging.log4j.core.Logger) log).addAppender(attachedAppender);
        }

        if (needsToConfigureUserLoggers) {
            for (Entry<String, Integer> userLogger : otherLoggerLevels.entrySet()) {
                /* 
                 * We want to set the level of this logger.
                 * It is not important if this logger is already attached to log4j2 system or 
                 * not as the next code will obtain it(in case logger exists) or will create it 
                 * and then will set its level
                 */
                // Note: Not exactly true. If the logger does not exists, I am not sure if the line below will create it and the set the level or just silently fail
                Log4j2Utils.setLoggerLevel(userLogger.getKey(), SystemLogLevel.toLevel(userLogger.getValue()));
            }
        }
    }

    @Override
    public boolean needsApplying() {

        /*
         * This code is run on remote agent's side
         */

        final String caller = ThreadsPerCaller.getCaller();

        needsToConfigureDbAppender = false;
        if (appenderConfiguration != null) {
            PassiveDbAppender dbAppender = PassiveDbAppender.getCurrentInstance(caller);
            if (dbAppender == null || !dbAppender.getAppenderConfig().equals(appenderConfiguration)) {
                // we did not have a DB appender
                // or the DB appender configuration is changed
                needsToConfigureDbAppender = true;
            }
        }

        needsToConfigureUserLoggers = false;

        Map<String, LoggerConfig> allLoggers = Log4j2Utils.getAllLoggers();

        for (Entry<String, Integer> userLogger : otherLoggerLevels.entrySet()) {

            boolean loggerAlreadyExists = false;
            boolean loggerLevelIsDifferent = false;
            for (Map.Entry<String, LoggerConfig> entry : allLoggers.entrySet()) {
                LoggerConfig loggerConfig = entry.getValue();

                if (loggerConfig.getName().equals(userLogger.getKey())) {
                    // this logger is already available, check its level
                    loggerAlreadyExists = true;

                    if (loggerConfig.getLevel() == null
                        || loggerConfig.getLevel().intLevel() != userLogger.getValue()) {
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
            Map<String, Appender> allAppenders = Log4j2Utils.getAllAppenders();
            for (Map.Entry<String, Appender> entry : allAppenders.entrySet()) {
                Appender appender = entry.getValue();
                // the org.apache.logging.log4j.core.Appender, which is returned via Log4j2Utils.getAllAppenders(); does not handle filters
                // so cast is needed, but only if this is possible
                if (appender instanceof AbstractAppender) {
                    //remove the filter
                    //FIXME: This is very risky, as someone may have added other filters

                    // Why do we need to do this?!?
                    ((AbstractAppender) appender).removeFilter( ((AbstractAppender) appender).getFilter());
                }
            }

            Logger log;
            if (Log4j2Utils.getRootLogger().getName().equals(appenderLogger)) {
                log = Log4j2Utils.getRootLogger();
            } else {
                log = Log4j2Utils.getLogger(appenderLogger);
            }

            Appender dbAppender = PassiveDbAppender.getCurrentInstance(ThreadsPerCaller.getCaller());
            if (dbAppender != null) {
                // stop the appender
                dbAppender.stop();

                // remove it
                // Is it possible that this cast will fail?!?
                Log4j2Utils.removeAppenderFromLogger(log.getName(), dbAppender.getName());
            }
        }

        // in case we must reconfigure the custom loggers, here we should remove them from log4j2, 
        // but log4j2 does not provide a way to do it - so we do nothing here
    }

    @Override
    public String getDescription() {

        return "logging configuration";
    }
}
