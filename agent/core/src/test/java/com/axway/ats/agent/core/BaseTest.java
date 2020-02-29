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
package com.axway.ats.agent.core;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.axway.ats.log.autodb.filters.NoSystemLevelEventsFilter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

public class BaseTest {

    protected static final String TEST_COMPONENT_NAME = "agenttest";

    // TODO - if Maven only will be run then this could be revised and target/test-classes for example could be used
    public final static String RELATIVE_PATH_TO_TESTS          = "src/test";
    public final static String RELATIVE_PATH_TO_TEST_RESOURCES = RELATIVE_PATH_TO_TESTS + "/resources";
    public final static String RELATIVE_PATH_TO_TEST_SOURCES   = RELATIVE_PATH_TO_TESTS + "/java";
    private final       Logger logger                          = Logger.getLogger(this.getClass());

    @Rule public TestName name = new TestName();

    static {
        ConsoleAppender appender = new ConsoleAppender(new PatternLayout("%-5p %d{HH:mm:ss-SSS} %c{2}: %m%n"));
        appender.addFilter(new NoSystemLevelEventsFilter());

        //init log4j
        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure(appender);
    }

    @Before
    public void beforeParentMethod() {

        logger.info("Starting test method " + name.getClass() + ":" + name.getMethodName());
    }
}
