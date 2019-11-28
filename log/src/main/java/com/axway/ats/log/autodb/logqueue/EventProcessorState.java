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
package com.axway.ats.log.autodb.logqueue;

import com.axway.ats.log.autodb.LoadQueuesState;
import com.axway.ats.log.autodb.TestCaseState;

public class EventProcessorState {

    //the appender state
    private LifeCycleState  lifeCycleState;

    //DB table ids - we need to hold the current ids here
    private int             runId;
    private int             previousRunId;
    private String          runName;
    private String          runDescription;
    private String          runUserNote;
    private int             suiteId;
    private TestCaseState   testCaseState;
    private LoadQueuesState loadQueuesState;

    /**
     * Default constructor. State set to "initialized"
     */
    public EventProcessorState() {

        this.lifeCycleState = LifeCycleState.INITIALIZED;
        this.testCaseState = new TestCaseState();
        this.loadQueuesState = new LoadQueuesState();
    }

    public LifeCycleState getLifeCycleState() {

        return lifeCycleState;
    }

    public void setLifeCycleState(
                                   LifeCycleState lifeCycleState ) {

        this.lifeCycleState = lifeCycleState;
    }

    public int getRunId() {

        return runId;
    }

    public void setRunId(
                          int runId ) {

        this.runId = runId;
    }

    public int getPreviousRunId() {

        return previousRunId;
    }

    public void setPreviousRunId(
                                  int previousRunId ) {

        this.previousRunId = previousRunId;
    }

    public String getRunName() {

        return runName;
    }

    public void setRunName(
                            String runName ) {

        this.runName = runName;
    }

    public String getRunDescription() {

        return runDescription;
    }

    public void setRunDescription(
                                   String runDescription ) {

        this.runDescription = runDescription;
    }

    public String getRunUserNote() {

        return runUserNote;
    }

    public void setRunUserNote(
                                String runUserNote ) {

        this.runUserNote = runUserNote;
    }

    public int getSuiteId() {

        return suiteId;
    }

    public void setSuiteId(
                            int suiteId ) {

        this.suiteId = suiteId;
    }

    public int getTestCaseId() {

        return testCaseState.getTestcaseId();
    }
    
    public int getLastExecutedTestCaseId() {

        return testCaseState.getLastExecutedTestcaseId();
    }

    public TestCaseState getTestCaseState() {

        return testCaseState;
    }

    public void setTestCaseState(
                                  TestCaseState testCaseState ) {

        this.testCaseState = testCaseState;
    }

    public LoadQueuesState getLoadQueuesState() {

        return loadQueuesState;
    }
}
