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
package com.axway.ats.core.log;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Logger sending messages through Log4j2
 */
public class AtsLog4jLogger extends AbstractAtsLogger {

    private final Logger log;

    /**
     * Useful for test logging. Mostly could be set in (static blocks of) BaseTest classes of unit tests.
     * This is separate non-logger specific utility method
     * Note that the StatusLogger's level is set to OFF, due to PowerMock errors causing Unit tests to take too long to execute
     * causing them to fail.
     * If you want more details about what exactly Log4j2 is doing, set this level to either DEBUG or TRACE
     * Also the RootLogger level is set to INFO
     */
    public static void setLog4JConsoleLoggingOnly() {
        
        // Since this class is not available in the configuration submodule, in the com.axway.ats.config.BaseTest there, there is an exact copy of this static block
        // So changing anything here should also be changed in com.axway.ats.config.BaseTest
        

        StatusLogger.getLogger().setLevel(Level.OFF);
        PatternLayout layout = PatternLayout.newBuilder().withPattern("%-5p %d{HH:MM:ss} %c{2}: %m%n").build();
        ConsoleAppender appender = ConsoleAppender.newBuilder().setName("ConsoleAppender").setLayout(layout).build();

        Configurator.setRootLevel(Level.INFO);

        final LoggerContext context = LoggerContext.getContext(false);
        final Configuration config = context.getConfiguration();
        appender.start();
        config.addAppender(appender);
        context.updateLoggers();
    }

    public AtsLog4jLogger( Class<?> callingClass ) {

        if (callingClass == null) {
            // this will probably never happen, as our code gives calling class when initializing log4j2 logger
            log = LogManager.getLogger("ATS Logger");
        } else {
            log = LogManager.getLogger(callingClass);
        }
    }

    private AtsLog4jLogger( String callingClassName ) {

        log = LogManager.getLogger(callingClassName);
    }

    @Override
    public AbstractAtsLogger newInstance() {

        return new AtsLog4jLogger(log.getName());
    }

    @Override
    public void setLevel(
                          String level ) {

        Configurator.setLevel(log.getName(), Level.toLevel(level));
        // or this ? -> Configurator.setAllLevels(log.getName(), Level.toLevel(level));
        //log.setLevel(Level.toLevel(level));
    }

    @Override
    public void setCallingClass(
                                 Class<?> callingClass ) {

        // do nothing as we get this from the constructor
    }

    @Override
    public void debug(
                       String message ) {

        log.debug(message);
    }

    @Override
    public void info(
                      String message ) {

        log.info(message);
    }

    @Override
    public void warn(
                      String message ) {

        log.warn(message);
    }

    @Override
    public void error(
                       String message ) {

        log.error(message);
    }

    @Override
    public void error(
                       String message,
                       Throwable exception ) {

        log.error(message, exception);
    }

}
