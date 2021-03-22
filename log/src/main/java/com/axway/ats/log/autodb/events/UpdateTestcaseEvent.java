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

import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.autodb.logqueue.LifeCycleState;
import com.axway.ats.log.autodb.model.LoggingEventType;

@SuppressWarnings( "serial")
public class UpdateTestcaseEvent extends StartTestCaseEvent {

    private int    testcaseId;
    private int    testcaseResult;
    private String userNote;

    public UpdateTestcaseEvent( String loggerFQCN, Logger logger, int testcaseId, String suiteFullName,
                                String suiteSimpleName, String scenarioName, String inputArguments,
                                String scenarioDescription, int testcaseResult, long timestamp ) {

        this(loggerFQCN, logger, testcaseId, suiteFullName, suiteSimpleName, scenarioName, inputArguments,
             scenarioDescription, null, testcaseResult, timestamp);

    }

    public UpdateTestcaseEvent( String loggerFQCN, Logger logger, int testcaseId, String suiteFullName,
                                String suiteSimpleName, String scenarioName, String inputArguments,
                                String scenarioDescription, String userNote, int testcaseResult,
                                long timestamp ) {

        super(loggerFQCN, logger,
              "Update testcase with id '" + testcaseId + "'",
              suiteFullName, suiteSimpleName, scenarioName, inputArguments, scenarioDescription,
              LoggingEventType.UPDATE_TEST_CASE);

        this.testcaseId = testcaseId;
        this.testcaseResult = testcaseResult;
        this.userNote = userNote;

        if (StringUtils.isNullOrEmpty(scenarioName) || StringUtils.isNullOrEmpty(inputArguments)) {

            this.testcaseName = null;
        }

    }

    @Override
    protected LifeCycleState getExpectedLifeCycleState( LifeCycleState event ) {

        return LifeCycleState.ATLEAST_SUITE_STARTED;
    }

    public int getTestcaseId() {

        return testcaseId;
    }

    public int getTestcaseResult() {

        return testcaseResult;
    }

    public String getUserNote() {

        return userNote;
    }

}
