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
public class DeleteTestCaseEvent extends AbstractLoggingEvent {

    private final int testcaseId;

    public DeleteTestCaseEvent( String loggerFQCN,
                                Logger logger,
                                int testcaseId ) {

        super(loggerFQCN, logger, "Delete test case", LoggingEventType.DELETE_TEST_CASE);

        this.testcaseId = testcaseId;
    }

    public int getTestCaseId() {

        return testcaseId;
    }

    @Override
    protected LifeCycleState getExpectedLifeCycleState(
                                                        LifeCycleState state ) {

        // this is event is not going through the logging queue, a life-cycle state is not checked
        return null;
    }
}
