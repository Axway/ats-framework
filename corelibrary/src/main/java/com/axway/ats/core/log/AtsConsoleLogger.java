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
package com.axway.ats.core.log;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.axway.ats.core.utils.ExceptionUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.core.utils.TimeUtils;

/**
 * This class logs messages to STDOUT streams only.
 * Use this class when logging via Apache's Log4J will cause re-entrance to AbstractDbAppender@append() when the main thread is blocked
 * */
public class AtsConsoleLogger {

    public static final String ATS_CONSOLE_MESSAGE_PREFIX = "*** ATS *** ";

    private Class<?>           callingClass;

    private String[]           classNameTokens;

    private StringBuilder      sb                         = new StringBuilder();

    private Logger             logger;

    /**
     * Current level of the logger
     * If it is null, the Logger.getRootLogger().getLevel() value is used
     * */
    public static Level        level                      = null;

    /**
     * Construct new AtsConsoleLogger
     * @param callingClass the class that invoked the constructor
     * */
    public AtsConsoleLogger( Class<?> callingClass ) {

        this.callingClass = callingClass;

        this.logger = Logger.getLogger(callingClass);

        this.classNameTokens = this.callingClass.getName().split("\\.");
    }

    public void fatal( String message ) {

        String logLevel = "FATAL";

        if (isLogLevelEnabled(logLevel)) {
            log(logLevel, message);
        }

    }

    public void error( Throwable e ) {

        String logLevel = "ERROR";

        if (isLogLevelEnabled(logLevel)) {
            log(logLevel, ExceptionUtils.getExceptionMsg(e));
        }

    }

    public void error( String message ) {

        String logLevel = "ERROR";

        if (isLogLevelEnabled(logLevel)) {
            log(logLevel, message);
        }

    }

    public void error( String message, Throwable e ) {

        String logLevel = "ERROR";

        if (isLogLevelEnabled(logLevel)) {
            log(logLevel, ExceptionUtils.getExceptionMsg(e, message));
        }

    }

    public void warn( String message ) {

        String logLevel = "WARN";

        if (isLogLevelEnabled(logLevel)) {
            log(logLevel, message);
        }

    }

    public void info( String message ) {

        String logLevel = "INFO";

        if (isLogLevelEnabled(logLevel)) {
            log(logLevel, message);
        }

    }

    public void debug( String message ) {

        String logLevel = "DEBUG";

        if (isLogLevelEnabled(logLevel)) {
            log(logLevel, message);
        }

    }

    public void trace( String message ) {

        String logLevel = "TRACE";

        if (isLogLevelEnabled(logLevel)) {
            log(logLevel, message);
        }

    }

    public Logger getLog4jLogger() {

        return logger;

    }

    /*
     * Log message without log level
     * */
    public void log( String message ) {

        log("", message); // this message will be logged without log level
    }

    private boolean isLogLevelEnabled( String level ) {

        if (AtsConsoleLogger.level != null) {

            if (Level.toLevel(level).isGreaterOrEqual(AtsConsoleLogger.level)) {
                return true;
            } else {
                return false;
            }

        } else {
            return Logger.getRootLogger().isEnabledFor(Level.toLevel(level));
        }

    }

    private void log( String level, String message ) {

        sb.setLength(0); // clear the builder
        sb.append(ATS_CONSOLE_MESSAGE_PREFIX).append(" ");

        String now = TimeUtils.getFormattedDateTillMilliseconds();

        if (!StringUtils.isNullOrEmpty(level)) {
            if (level.length() < 5) { // 5 is the max number of chars for level ID String (ERROR, TRACE, DEBUG)
                // in order to preserve some kind of padding, if the level ID String is shorter, add additional space char after it
                sb.append(level.toUpperCase()).append(" ").append(" ");
            } else {
                sb.append(level.toUpperCase()).append(" ");
            }
        }

        sb.append(now).append(" ");

        if (this.classNameTokens.length < 2) {
            sb.append(this.callingClass.getSimpleName());
        } else {
            sb.append(this.classNameTokens[this.classNameTokens.length - 2])
              .append(".")
              .append(this.classNameTokens[this.classNameTokens.length - 1]);
        }

        sb.append(": ").append(message);

        System.out.println(sb.toString());

    }

}
