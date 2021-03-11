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
package com.axway.ats.config;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class BaseTest {

    static {
        PatternLayout layout = PatternLayout.newBuilder().withPattern("%-5p %d{HH:MM:ss} %c{2}: %m%n").build();
        ConsoleAppender appender = ConsoleAppender.newBuilder().setName("ConsoleAppender").setLayout(layout).build();

        Configurator.setRootLevel(Level.OFF);
        
        final LoggerContext context = LoggerContext.getContext(false);
        final Configuration config = context.getConfiguration();
        appender.start();
        config.addAppender(appender);
        context.updateLoggers();
    }
}
