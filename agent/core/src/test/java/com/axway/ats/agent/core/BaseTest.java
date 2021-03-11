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
package com.axway.ats.agent.core;

import com.axway.ats.core.log.AtsLog4jLogger;

public class BaseTest {

    protected static final String TEST_COMPONENT_NAME             = "agenttest";

    // TODO - if Maven only will be run then this could be revised and target/test-classes for example could be used
    public final static String    RELATIVE_PATH_TO_TESTS          = "src/test";
    public final static String    RELATIVE_PATH_TO_TEST_RESOURCES = RELATIVE_PATH_TO_TESTS + "/resources";
    public final static String    RELATIVE_PATH_TO_TEST_SOURCES   = RELATIVE_PATH_TO_TESTS + "/java";

    static {
        AtsLog4jLogger.setLog4JConsoleLoggingOnly();
    }
}
