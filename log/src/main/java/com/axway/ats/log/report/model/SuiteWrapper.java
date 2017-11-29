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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.axway.ats.log.autodb.entities.Suite;
import com.axway.ats.log.autodb.entities.Testcase;
import com.axway.ats.log.model.ScenarioResult;

/**
 * This wrapper keeps the testcases of the suite.
 */
@SuppressWarnings("serial")
public class SuiteWrapper extends Suite {

    private Map<String, List<Testcase>> testcasesMap = new HashMap<>();
    private Testcase                    lastTestcase;

    private boolean                     dataLoadedFromDb;

    /**
     * Used when attached to some run. All data is captured on the fly
     */
    public SuiteWrapper() {

    }

    /**
     * Used when loading data from the database. 
     * All data comes ready, no calculation is needed in this case.
     * 
     * @param suite database suite
     */
    public SuiteWrapper( Suite suite ) {

        dataLoadedFromDb = true;

        scenariosTotal = suite.scenariosTotal;
        scenariosFailed = suite.scenariosFailed;
        scenariosSkipped = suite.scenariosSkipped;

        testcasesTotal = suite.testcasesTotal;
        testcasesFailed = suite.testcasesFailed;
        testcasesSkipped = suite.testcasesSkipped;

    }

    public Testcase getLastTestcase() {

        return this.lastTestcase;
    }

    public void addTestcase(
                             Testcase testcase ) {

        List<Testcase> testcasesPerScenario = testcasesMap.get( testcase.scenarioName );
        if( testcasesPerScenario == null ) {
            testcasesPerScenario = new ArrayList<>();
        }
        testcasesPerScenario.add( testcase );

        this.testcasesMap.put( testcase.scenarioName, testcasesPerScenario );

        this.lastTestcase = testcase;
    }

    public void calculateFinalStatistics() {

        if( dataLoadedFromDb ) {
            // all statistics came calculated
            return;
        }

        for( Entry<String, List<Testcase>> scenarioEntry : testcasesMap.entrySet() ) {
            ++scenariosTotal;

            ScenarioResult scenarioResult = ScenarioResult.PASSED;

            Collection<Testcase> testcasesPerScenario = scenarioEntry.getValue();
            for( Testcase testcase : testcasesPerScenario ) {
                ++testcasesTotal;
                switch( testcase.result ){
                    case 0: // FAILED
                        ++testcasesFailed;
                        scenarioResult = ScenarioResult.FAILED;
                        break;
                    case 2: // SKIPPED
                        ++testcasesSkipped;
                        if( scenarioResult == ScenarioResult.PASSED ) {
                            scenarioResult = ScenarioResult.SKIPPED;
                        }
                        break;
                    default:
                        break;
                }
            }

            if( scenarioResult == ScenarioResult.FAILED ) {
                ++scenariosFailed;
            } else if( scenarioResult == ScenarioResult.SKIPPED ) {
                ++scenariosSkipped;
            }
        }
    }
}
