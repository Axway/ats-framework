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
package com.axway.ats.core.log;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.axway.ats.core.utils.ExceptionUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.core.utils.TimeUtils;

/**
 * This class logs messages to STDOUT streams only.
 * Use this class when logging via Apache's Log4J will cause re-entrance to AbstractDbAppender@append() when the main thread is blocked
 */
public class AtsConsoleLogger {

    public static final String ATS_CONSOLE_MESSAGE_PREFIX = "*** ATS *** ";
    /**
     * Current level of the logger. Used for alternative logging when there is ATS Log4J blocking event like StartRun.
     * If it is null, the Logger.getRootLogger().getLevel() value is used
     * */
    private static Level       level                      = null;

    private Class<?>           callingClass;

    private String[]           classNameTokens;
    private String             classNamePrefix;                                 // one-time calculated class location prefix

    private Logger             logger;

    /**
     * Construct new AtsConsoleLogger
     * @param callingClass the class that invoked the constructor
     * */
    public AtsConsoleLogger( Class<?> callingClass ) {

        this.callingClass = callingClass;

        this.logger = Logger.getLogger(callingClass);

        this.classNameTokens = this.callingClass.getName().split("\\.");
        this.classNamePrefix = generateClassNamePrefix(callingClass, classNameTokens);
    }

    public void fatal( String message ) {

        if (isLogLevelEnabled(Level.FATAL)) {
            log(Level.FATAL.toString(), message);
        }
    }

    public void error( Throwable e ) {

        if (isLogLevelEnabled(Level.ERROR)) {
            log(Level.ERROR.toString(), ExceptionUtils.getExceptionMsg(e));
        }
    }

    public void error( String message ) {

        if (isLogLevelEnabled(Level.ERROR)) {
            log(Level.ERROR.toString(), message);
        }

    }

    public void error( String message, Throwable e ) {

        if (isLogLevelEnabled(Level.ERROR)) {
            log(Level.ERROR.toString(), ExceptionUtils.getExceptionMsg(e, message));
        }

    }

    public void warn( String message ) {

        if (isLogLevelEnabled(Level.WARN)) {
            log(Level.WARN.toString(), message);
        }

    }

    public void info( String message ) {

        if (isLogLevelEnabled(Level.INFO)) {
            log(Level.INFO.toString(), message);
        }
    }

    public void debug( String message ) {

        if (isLogLevelEnabled(Level.DEBUG)) {
            log(Level.DEBUG.toString(), message);
        }

    }

    public void trace( String message ) {

        if (isLogLevelEnabled(Level.TRACE)) {
            log(Level.TRACE.toString(), message);
        }

    }

    public void trace( String message, Throwable th ) {

        if (isLogLevelEnabled(Level.TRACE)) {
            log(Level.TRACE.toString(), ExceptionUtils.getExceptionMsg(th, message));
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

    private boolean isLogLevelEnabled( Level logLevel ) {
        if (logLevel == null) {
            throw new IllegalArgumentException("Level provided is null");
        }

        if (AtsConsoleLogger.level != null) {
            return logLevel.isGreaterOrEqual(AtsConsoleLogger.level);
        } else {
            return Logger.getRootLogger().isEnabledFor(logLevel);
        }
    }

   /* private boolean isLogLevelEnabled( String level ) {

        if (AtsConsoleLogger.level != null) {
            return Level.toLevel(level).isGreaterOrEqual(AtsConsoleLogger.level);
        } else {
            return Logger.getRootLogger().isEnabledFor(Level.toLevel(level));
        }
    }*/

    private void log( String level, String message ) {
        int mesageLen = message == null ? 0 : message.length();
        StringBuilder sb = new StringBuilder(128 + mesageLen); // estimated capacity
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

        System.out.println(sb);
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