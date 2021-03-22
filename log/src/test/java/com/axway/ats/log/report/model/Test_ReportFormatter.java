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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import com.axway.ats.log.autodb.entities.Suite;
import com.axway.ats.log.report.exceptions.ReportFormatterException;

@RunWith( PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
public class Test_ReportFormatter {

    @Test
    public void passedRun() throws MessagingException {

        RunWrapper run = createRun();

        ReportFormatter formatter = new ReportFormatter(run);
        assertTrue(formatter.getDescription().length() > 0);
        assertTrue(formatter.toHtml().length() > 0);
    }

    @Test
    public void failedRun() throws MessagingException {

        RunWrapper run = createRun();
        run.scenariosFailed = 1;

        Suite passedSuite1 = new Suite();
        passedSuite1.scenariosTotal = 1;
        run.addSuite(new SuiteWrapper(passedSuite1));

        Suite passedSuite2 = new Suite();
        passedSuite2.scenariosTotal = 1;
        run.addSuite(new SuiteWrapper(passedSuite2));

        Suite passedSuite3 = new Suite();
        passedSuite3.scenariosTotal = 1;
        run.addSuite(new SuiteWrapper(passedSuite3));

        Suite failedSuite = new Suite();
        failedSuite.scenariosFailed = 1;
        failedSuite.scenariosTotal = 1;
        run.addSuite(new SuiteWrapper(failedSuite));

        ReportFormatter formatter = new ReportFormatter(run);
        assertTrue(formatter.getDescription().length() > 0);
        assertTrue(formatter.toHtml().length() > 0);
    }

    @Test( expected = ReportFormatterException.class)
    public void noRuns() throws MessagingException {

        List<RunWrapper> runs = new ArrayList<RunWrapper>();
        String dbHost = "";
        String dbName = "";
        int port = 0;
        String testExplorerPath = "";

        ReportFormatter formatter = new ReportFormatter(runs,
                                                        "descirption",
                                                        dbHost,
                                                        dbName,
                                                        port,
                                                        testExplorerPath);
        formatter.toHtml();
    }

    private RunWrapper createRun() {

        RunWrapper run = new RunWrapper();
        run.productName = "product";
        run.versionName = "version";
        run.buildName = "build";
        run.os = "os";
        run.scenariosTotal = 1;

        Suite suite = new Suite();
        suite.scenariosTotal = 1;
        run.addSuite(new SuiteWrapper(suite));

        return run;
    }
}
