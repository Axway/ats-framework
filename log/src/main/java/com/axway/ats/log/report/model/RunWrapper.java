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
package com.axway.ats.log.report.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.axway.ats.log.autodb.entities.Run;
import com.axway.ats.log.autodb.entities.Suite;

/**
 * This wrapper keeps the suites of the run.
 */
@SuppressWarnings( "serial")
public class RunWrapper extends Run {

    private Collection<SuiteWrapper> suites = new ArrayList<SuiteWrapper>();

    public RunWrapper() {

    }

    /**
     * This constructor creates a RunWrapper from existing Run. 
     * This constructor is called when the data for the report is loaded from the DB.
     * 
     * @param run
     * @param suites
     */
    public RunWrapper( Run run,
                       List<Suite> suites ) {

        this.runId = run.runId;
        this.productName = run.productName;
        this.versionName = run.versionName;
        this.buildName = run.versionName;
        this.runName = run.runName;
        this.os = run.os;
        this.hostName = run.hostName;

        this.startTimestamp = run.getStartTimestamp();

        this.scenariosTotal = run.scenariosTotal;
        this.scenariosFailed = run.scenariosFailed;
        this.scenariosSkipped = run.scenariosSkipped;

        this.testcasesTotal = run.testcasesTotal;
        this.testcasesFailed = run.testcasesFailed;
        this.testcasesPassedPercent = run.testcasesPassedPercent;
        this.testcaseIsRunning = run.testcaseIsRunning;

        this.total = run.total;
        this.failed = run.failed;

        this.userNote = run.userNote;

        // remember the suites
        for (Suite suite : suites) {
            this.suites.add(new SuiteWrapper(suite));
        }
    }

    public Collection<SuiteWrapper> getSuites() {

        return suites;
    }

    public void addSuite(
                          SuiteWrapper suite ) {

        suites.add(suite);
    }

    // After suites' statistics calculation,
    // we can now caculate the run statistics 
    public void calculateFinalStatistics() {

        for (SuiteWrapper suite : suites) {

            this.scenariosTotal += suite.scenariosTotal;
            this.scenariosFailed += suite.scenariosFailed;
            this.scenariosSkipped += suite.scenariosSkipped;

            this.testcasesTotal += suite.testcasesTotal;
            this.testcasesFailed += suite.testcasesFailed;
            this.testcasesSkipped += suite.testcasesSkipped;
        }
    }
}
