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

import com.axway.ats.log.autodb.TestCaseState;
import com.axway.ats.log.autodb.logqueue.LifeCycleState;
import com.axway.ats.log.autodb.model.AbstractLoggingEvent;
import com.axway.ats.log.autodb.model.LoggingEventType;

@SuppressWarnings( "serial")
public class GetCurrentTestCaseEvent extends AbstractLoggingEvent {

    private TestCaseState testCaseState = new TestCaseState();

    public GetCurrentTestCaseEvent( String loggerFQCN,
                                    Logger logger ) {

        super(loggerFQCN,
              logger,
              "Get the current test case state",
              LoggingEventType.GET_CURRENT_TEST_CASE_STATE);
    }

    public TestCaseState getTestCaseState() {

        return testCaseState;
    }

    public void setTestCaseState(
                                  TestCaseState testCaseState ) {

        this.testCaseState = testCaseState;
    }

    @Override
    protected LifeCycleState getExpectedLifeCycleState(
                                                        LifeCycleState state ) {

        return LifeCycleState.TEST_CASE_STARTED;
    }
}
