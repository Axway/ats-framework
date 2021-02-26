/*
 * Copyright 2020-2021 Axway Software
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
package com.axway.ats.log;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;

/**
 * Utility class for easier Log4j2 manipulation
 * */
public class Log4j2Utils {

    public static synchronized void removeAllAppendersFromLogger( String loggerName ) {

        Map<String, Appender> appenders = getAppendersFromLogger(loggerName);

        for (Map.Entry<String, Appender> entry : appenders.entrySet()) {

            removeAppenderFromLogger(loggerName, entry.getKey());

        }

    }

    public static synchronized Map<String, LoggerConfig> getAllLoggers() {

        final LoggerContext context = LoggerContext.getContext(false);
        final Configuration config = context.getConfiguration();

        return config.getLoggers();

    }

    public static synchronized Map<String, Appender> getAllAppenders() {

        final LoggerContext context = LoggerContext.getContext(false);
        final Configuration config = context.getConfiguration();

        return config.getAppenders();
    }

    public static synchronized LoggerConfig getLoggerConfig( String loggerName ) {

        final LoggerContext context = LoggerContext.getContext(false);
        final Configuration config = context.getConfiguration();

        return config.getLoggerConfig(loggerName);
    }

    public static synchronized Map<String, Appender> getAppendersFromLogger( String loggerName ) {

        LoggerConfig logger = getLoggerConfig(loggerName);

        if (logger != null) {
            return logger.getAppenders();
        }

        return new HashMap<>();
    }

    public static synchronized void setLoggerLevel( String loggerName, Level level ) {

        Configurator.setLevel(loggerName, level);
    }

    public static synchronized AbstractAppender getAppenderFromLogger( String loggerName,
                                                                       String appenderName ) {

        Map<String, Appender> appenders = getAppendersFromLogger(loggerName);

        for (Map.Entry<String, Appender> entry : appenders.entrySet()) {
            if (entry.getKey().equals(appenderName)) {
                return (AbstractAppender) entry.getValue();
            }
        }

        return null;

    }

    public static synchronized void removeAppenderFromLogger( String loggerName,
                                                              String appenderName ) {

        LoggerContext context = LoggerContext.getContext(false);

        ((org.apache.logging.log4j.core.Logger) getLogger(loggerName)).removeAppender(getAppenderFromLogger(loggerName,
                                                                                                            appenderName));

        getLoggerConfig(loggerName).removeAppender(appenderName);

        context.updateLoggers();

        context.reconfigure();

    }

    public static synchronized void addAppenderToLogger( Appender appender, String loggerName ) {

        LoggerContext context = LoggerContext.getContext(false);

        // May use getLoggerConfig(loggerName).addAppender(appender, filter, level), but I do not like the need to specify filter + level here
        // so use this line below instead
        ((org.apache.logging.log4j.core.Logger) getLogger(loggerName)).addAppender(appender);

        context.updateLoggers();
    }

    /**
     * Create new logger or get an existing one
     * */
    public static synchronized Logger getLogger( String loggerName ) {

        return LogManager.getLogger(loggerName);
    }

    public static synchronized Logger getRootLogger() {

        return LogManager.getRootLogger();
    }

    public static synchronized void setRootLevel( Level level ) {

        Configurator.setRootLevel(level);

    }

}
