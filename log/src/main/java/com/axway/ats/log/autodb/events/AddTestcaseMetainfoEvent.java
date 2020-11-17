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
public class AddTestcaseMetainfoEvent extends AbstractLoggingEvent {

    private int    testcaseId;
    private String metaKey;
    private String metaValue;

    public AddTestcaseMetainfoEvent( String loggerFQCN,
                                     Logger logger,
                                     String metaKey,
                                     String metaValue ) {

        this(loggerFQCN, logger, -1, metaKey, metaValue);
    }

    public AddTestcaseMetainfoEvent( String loggerFQCN,
                                     Logger logger,
                                     int testcaseId,
                                     String metaKey,
                                     String metaValue ) {

        super(loggerFQCN,
              logger,
              "Add testcase meta info '" + metaKey + "=" + metaValue + "'",
              LoggingEventType.ADD_TESTCASE_METAINFO);

        this.testcaseId = testcaseId;
        this.metaKey = metaKey;
        this.metaValue = metaValue;
    }

    /**
     * Return the testcase ID that will be associated with that meta info.<br>
     * @return the testcase's ID or -1 if no testcase ID is explicitly specified. In the later case, the current testcase's ID will be associated with that meta info
     * */
    public int getTestcaseId() {

        return testcaseId;

    }

    public String getMetaKey() {

        return metaKey;
    }

    public String getMetaValue() {

        return metaValue;
    }

    @Override
    protected LifeCycleState getExpectedLifeCycleState(
                                                        LifeCycleState state ) {

        if (this.testcaseId != -1) {
            // testcase id is specified, so this event can be sent any time during RUN execution
            return LifeCycleState.ATLEAST_RUN_STARTED;
        } else {
            // testcase id is NOT specified, so this event can be sent only during testcase execution
            return LifeCycleState.TEST_CASE_STARTED;
        }
    }
}
