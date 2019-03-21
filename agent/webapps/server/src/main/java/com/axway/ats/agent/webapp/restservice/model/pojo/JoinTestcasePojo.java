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
package com.axway.ats.agent.webapp.restservice.model.pojo;

public class JoinTestcasePojo extends BasePojo {

    private int runId;
    private int testcaseId;
    private int lastExecutedTestcaseId;

    public JoinTestcasePojo() {

    }

    public JoinTestcasePojo( int runId,
                             int testcaseId,
                             int lastExecutedTestcaseId ) {

        this.runId = runId;
        this.testcaseId = testcaseId;
        this.lastExecutedTestcaseId = lastExecutedTestcaseId;
    }

    public int getRunId() {

        return runId;
    }

    public void setRunId(
                          int runId ) {

        this.runId = runId;
    }

    public int getTestcaseId() {

        return testcaseId;
    }

    public void setTestcaseId(
                               int testcaseId ) {

        this.testcaseId = testcaseId;
    }

    public int getLastExecutedTestcaseId() {

        return lastExecutedTestcaseId;
    }

    public void setLastExecutedTestcaseId( int lastExecutedTestcaseId ) {

        this.lastExecutedTestcaseId = lastExecutedTestcaseId;
    }

}
