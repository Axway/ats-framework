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
package com.axway.ats.log;

import org.apache.logging.log4j.Level;

import com.axway.ats.common.PublicAtsApi;

/**
 * List with supported log levels
 */
@PublicAtsApi
public enum LogLevel {

    /**
     * TRACE log level
     */
    @PublicAtsApi
    TRACE(Level.TRACE.intLevel()),

    /**
     * DEBUG log level
     */
    @PublicAtsApi
    DEBUG(Level.DEBUG.intLevel()),

    /**
     * INFO log level
     */
    @PublicAtsApi
    INFO(Level.INFO.intLevel()),

    /**
     * WARN log level
     */
    @PublicAtsApi
    WARN(Level.WARN.intLevel()),

    /**
     * ERROR log level
     */
    @PublicAtsApi
    ERROR(Level.ERROR.intLevel()),

    /**
     * FATAL log level
     */
    @PublicAtsApi
    FATAL(Level.FATAL.intLevel());

    private int value;

    private LogLevel( int value ) {

        this.value = value;
    }

    public int toInt() {

        return value;
    }
}
