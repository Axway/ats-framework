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

import java.text.SimpleDateFormat;
import java.util.Date;

import com.axway.ats.core.utils.TimeUtils;

/**
 * This class logs messages to STDOUT and STDERR streams only.
 * */
public class AtsConsoleLogger {

    public static final String ATS_CONSOLE_MESSAGE_PREFIX = "*** ATS *** ";

    private Class<?>           callingClass;

    private String[]           classNameTokens;

    private StringBuilder      sb                         = new StringBuilder();

    private class Level {

        public static final String FATAL = "FATAL";
        public static final String ERROR = "ERROR";
        public static final String WARN  = "WARN";
        public static final String INFO  = "INFO";
        public static final String DEBUG = "DEBUG";
        public static final String TRACE = "TRACE";

    }

    /**
     * Construct new AtsConsoleLogger
     * @param callingClass the class that invoked the constructor
     * */
    public AtsConsoleLogger( Class<?> callingClass ) {

        this.callingClass = callingClass;

        this.classNameTokens = this.callingClass.getName().split("\\.");
    }

    public void fatal( String message ) {

        log(Level.FATAL, message);
    }

    public void error( String message ) {

        log(Level.ERROR, message);
    }

    public void warn( String message ) {

        log(Level.WARN, message);
    }

    public void info( String message ) {

        log(Level.INFO, message);
    }

    public void debug( String message ) {

        log(Level.DEBUG, message);
    }

    public void trace( String message ) {

        log(Level.TRACE, message);
    }

    private void log( String level, String message ) {

        sb.setLength(0); // clear the builder
        sb.append(ATS_CONSOLE_MESSAGE_PREFIX).append(" ");

        String now = TimeUtils.getFormattedDateTillMilliseconds();

        if (level.length() < 5) { // 5 is the max number of chars for level ID String (ERROR, TRACE, DEBUG)
            // in order to preserve some kind of padding, if the level ID String is shorted, add additional space char after it
            sb.append(level.toUpperCase()).append(" ").append(" ");
        } else {
            sb.append(level.toUpperCase()).append(" ");
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

        if (level.equalsIgnoreCase(Level.ERROR) || level.equalsIgnoreCase(Level.FATAL)) {
            System.err.println(sb.toString());
        } else {
            System.out.println(sb.toString());
        }

    }

}
