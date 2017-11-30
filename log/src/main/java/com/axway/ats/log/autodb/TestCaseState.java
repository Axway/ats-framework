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
package com.axway.ats.log.autodb;

public class TestCaseState {

    private static final int UNKNOWN_TESTCASE_ID = -1;

    private int              testcaseId;
    private int              runId;

    public TestCaseState() {

        clearTestcaseId();
    }

    public int getRunId() {

        return runId;
    }

    public void setRunId( int runId ) {

        this.runId = runId;
    }

    public int getTestcaseId() {

        return testcaseId;
    }

    public void setTestcaseId(
                               int testcaseId ) {

        this.testcaseId = testcaseId;
    }

    public void clearTestcaseId() {

        this.testcaseId = UNKNOWN_TESTCASE_ID;
    }

    public boolean isInitialized() {

        return this.testcaseId != UNKNOWN_TESTCASE_ID;
    }

    @Override
    public boolean equals(
                           Object that ) {

        if (that != null && that instanceof TestCaseState) {
            return this.testcaseId == ((TestCaseState) that).testcaseId;
        }
        return false;
    }
}
