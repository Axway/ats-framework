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
package com.axway.ats.log.autodb.entities;

public class Run extends DbEntity {

    private static final long serialVersionUID = 1L;

    public String             runId;
    public String             productName;
    public String             versionName;
    public String             buildName;
    public String             runName;
    public String             os;
    public String             hostName;

    public int                scenariosTotal;
    public int                scenariosFailed;
    public int                scenariosSkipped;

    public int                testcasesTotal;
    public int                testcasesFailed;
    public int                testcasesSkipped;
    public String             testcasesPassedPercent;
    public boolean            testcaseIsRunning;

    public String             total; // Composite string: scenarios / testcases
    public String             failed; // Composed string: scenarios / testcases

    public String             userNote;

    @Override
    public String toString() {

        return "Run: id=" + runId + ", runName=" + runName;
    }

    @Override
    public int hashCode() {

        return runId.hashCode();
    }

    @Override
    public boolean equals(
                           Object obj ) {

        if (obj == null || ! (obj instanceof Run)) {
            return false;
        }
        Run run = (Run) obj;
        return this.runId.equals(run.runId);
    }

}
