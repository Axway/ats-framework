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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.core.utils.ExceptionUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.core.utils.TimeUtils;

/**
 * This class logs messages to STDOUT streams only.
 * Use this class when logging via Apache's Log4J will cause re-entrance to AbstractDbAppender@append() when the main thread is blocked
 * */
public class AtsConsoleLogger {

    public static final String ATS_CONSOLE_MESSAGE_PREFIX = "*** ATS *** ";
    /**
     * Current level of the logger. Used for alternative logging when there is ATS Log4J blocking event like StartRun.
     * If it is null, the LogManager.getRootLogger().getLevel() value is used
     * */
    private static Level       level                      = null;

    private Class<?>           callingClass;

    private String[]           classNameTokens;
    private String             classNamePrefix;                                 // one-time calculated class location prefix

    private StringBuilder      sb                         = new StringBuilder();

    private Logger             logger;

    /**
     * Construct new AtsConsoleLogger
     * @param callingClass the class that invoked the constructor
     * */
    public AtsConsoleLogger( Class<?> callingClass ) {

        this.callingClass = callingClass;

        this.logger = LogManager.getLogger(callingClass);

        this.classNameTokens = this.callingClass.getName().split("\\.");
        this.classNamePrefix = generateClassNamePrefix(callingClass, classNameTokens);
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

    public void trace( String message, Throwable th ) {

        String logLevel = "TRACE";

        if (isLogLevelEnabled(logLevel)) {
            log(logLevel, ExceptionUtils.getExceptionMsg(th, message));
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

    public void log( String message, Throwable th ) {

        log("", ExceptionUtils.getExceptionMsg(th, message)); // this message will be logged without log level
    }

    public static void setLevel( Level newLevel ) {

        level = newLevel;
    }

    private boolean isLogLevelEnabled( String level ) {

        if (AtsConsoleLogger.level != null) {
            return Level.toLevel(level).isLessSpecificThan(AtsConsoleLogger.level); // or isMore?!?
        } else {
            return LogManager.getRootLogger().getLevel().isLessSpecificThan(Level.toLevel(level)); // or isMore?!?
        }

    }

    private void log( String level, String message ) {

        sb.setLength(0); // clear the builder
        sb.append(ATS_CONSOLE_MESSAGE_PREFIX).append(" ");

        String now = TimeUtils.getFormattedDateTillMilliseconds();

        sb.append(now).append(" ");

        if (!StringUtils.isNullOrEmpty(level)) {
            if (level.length() < 5) { // 5 is the max number of chars for level ID String (ERROR, TRACE, DEBUG)
                // in order to preserve some kind of padding, if the level ID String is shorter, add additional space char after it
                sb.append(level.toUpperCase()).append(" ").append(" ");
            } else {
                sb.append(level.toUpperCase()).append(" ");
            }
        }

        sb.append("[").append(Thread.currentThread().getName()).append("] ");
        sb.append(classNamePrefix);
        sb.append(": ").append(message);

        System.out.println(sb.toString());
    }

    //

    /**
     * Calculates string to be displayed for class logging (either classname or last package + classname)
     * Example: zzz.MyClass or MyClass if not package is detected.
     */
    private String generateClassNamePrefix( Class callingClass, String[] classNameTokens ) {

        if (classNameTokens == null || classNameTokens.length == 1) {
            throw new IllegalArgumentException("Empty or one element array provided for class/package tokens.");
        }
        if (classNameTokens.length < 2) {
            return callingClass.getSimpleName();
        } else {
            return classNameTokens[classNameTokens.length - 2]
                   + "." + classNameTokens[classNameTokens.length - 1];
        }
    }

}
