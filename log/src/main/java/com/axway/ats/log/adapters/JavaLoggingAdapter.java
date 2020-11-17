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
package com.axway.ats.log.adapters;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This is a java.util.logging.Handler which logs to a log4J Logger
 * 
 */
public class JavaLoggingAdapter extends Handler {

    protected Logger logger;

    public JavaLoggingAdapter( Logger logger ) {

        this.logger = logger;
    }

    @Override
    public void close() throws SecurityException {

    }

    @Override
    public void flush() {

    }

    @Override
    public void publish(
                         LogRecord record ) {

        Level level = record.getLevel();
        String message = record.getMessage();
        if (level.intValue() == Level.ALL.intValue()) {
            throw new RuntimeException("ALL");
        }

        if (level.intValue() <= Level.FINEST.intValue()) {
            logger.debug(message);
            return;//300
        }

        if (level.intValue() <= Level.FINE.intValue()) {
            logger.debug(message);
            return;//500
        }

        if (level.intValue() <= Level.CONFIG.intValue()) {
            logger.debug(message);
            return;//700
        }

        if (level.intValue() <= Level.INFO.intValue()) {
            logger.info(message);
            return;//800
        }

        if (level.intValue() <= Level.WARNING.intValue()) {
            logger.warn(message);
            return;//900
        }

        if (level.intValue() <= Level.SEVERE.intValue()) {
            logger.error(message);
            return;//1000
        }

        if (level.intValue() <= Level.OFF.intValue()) {
            throw new RuntimeException("OFF");
        }

    }
}
