/*
 * Copyright 2017-2020 Axway Software
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
package com.axway.ats.agent.webapp.agentservice;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class BaseTestWebapps {

    static {
        PatternLayout layout = PatternLayout.newBuilder().withPattern("%-5p %d{HH:MM:ss} %c{2}: %m%n").build();
        ConsoleAppender appender = ConsoleAppender.newBuilder().setLayout(layout).setName("ConsoleAppender").build();

        //init log4j
        final LoggerContext context = LoggerContext.getContext(false);
        final Configuration config = context.getConfiguration();
        appender.start();
        config.addAppender(appender);
        // context.getRootLogger().addAppender(config.getAppender(appender.getName())); Is this needed?!?
        context.updateLoggers(); // TODO is this needed
    }
}
