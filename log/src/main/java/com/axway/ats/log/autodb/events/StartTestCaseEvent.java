/*
 * Copyright 2017-2019 Axway Software
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
package com.axway.ats.log.autodb.events;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.log.autodb.logqueue.LifeCycleState;
import com.axway.ats.log.autodb.model.AbstractLoggingEvent;
import com.axway.ats.log.autodb.model.LoggingEventType;

@SuppressWarnings( "serial")
public class StartTestCaseEvent extends AbstractLoggingEvent {

    private String   suiteSimpleName;
    private String   suiteFullName;
    private String   scenarioName;
    private String   scenarioDescription;

    protected String testcaseName;

    public StartTestCaseEvent( String loggerFQCN,
                               Logger logger,
                               String suiteFullName,
                               String suiteSimpleName,
                               String scenarioName,
                               String inputArguments,
                               String scenarioDescription ) {

        this(loggerFQCN,
             logger,
             "Start test case '" + scenarioName + inputArguments + "' for suite " + suiteSimpleName,
             suiteFullName,
             suiteSimpleName,
             scenarioName,
             inputArguments,
             scenarioDescription,
             LoggingEventType.START_TEST_CASE);
    }

    public StartTestCaseEvent( String loggerFQCN,
                               Logger logger,
                               String message,
                               String suiteFullName,
                               String suiteSimpleName,
                               String scenarioName,
                               String inputArguments,
                               String scenarioDescription,
                               LoggingEventType loggingEventType ) {

        super(loggerFQCN,
              logger,
              message,
              loggingEventType);

        this.suiteFullName = suiteFullName;
        this.suiteSimpleName = suiteSimpleName;
        this.scenarioName = scenarioName;
        this.scenarioDescription = scenarioDescription;

        this.testcaseName = scenarioName + inputArguments;
    }

    public String getSuiteFullName() {

        return this.suiteFullName;
    }

    public String getSuiteSimpleName() {

        return this.suiteSimpleName;
    }

    public String getScenarioName() {

        return this.scenarioName;
    }

    public String getScenarioDescription() {

        return this.scenarioDescription;
    }

    public String getTestcaseName() {

        return this.testcaseName;
    }

    @Override
    protected LifeCycleState getExpectedLifeCycleState(
                                                        LifeCycleState state ) {

        return LifeCycleState.SUITE_STARTED;
    }
}
