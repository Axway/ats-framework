/*
 * Copyright 2017-2021 Axway Software
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

package com.axway.ats.log.appenders;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import com.axway.ats.log.autodb.entities.Testcase;
import com.axway.ats.log.autodb.events.EndRunEvent;
import com.axway.ats.log.autodb.events.EndSuiteEvent;
import com.axway.ats.log.autodb.events.EndTestCaseEvent;
import com.axway.ats.log.autodb.events.StartRunEvent;
import com.axway.ats.log.autodb.events.StartSuiteEvent;
import com.axway.ats.log.autodb.events.StartTestCaseEvent;
import com.axway.ats.log.autodb.model.AbstractLoggingEvent;
import com.axway.ats.log.report.model.MailReportSender;
import com.axway.ats.log.report.model.ReportFormatter;
import com.axway.ats.log.report.model.RunWrapper;
import com.axway.ats.log.report.model.SuiteWrapper;

@Plugin( name = "ReportAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class ReportAppender extends AbstractAppender {

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    static {
        // we must 'zero' the this formatter, or unexpected time differences appear
        TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private RunWrapper                run;
    // <suite full name, suite>
    private Map<String, SuiteWrapper> suitesMap;
    private static boolean            isReportAppenderActive = false;
    //true if ATSReportListener is used and combined report will be generated for nested suite XMLs
    private static boolean            combinedHtmlMailReport = false;

    // here we collect test data for tests, that should be sent together in one mail
    private static List<RunWrapper>   htmlReportsList        = new ArrayList<RunWrapper>();

    /**
     * This token is searched in log4j2.xml which defines the Report Appender.
     *
     * The user has the option to specify a custom mail subject, in the special tokens
     * we will insert runtime info about the run
     */
    private String                    mailSubjectPrefix      = "Report for automated run '{RUN}', product '{PRODUCT}', version '{VERSION}', build '{BUILD}', os '{OS}' - {STATE}";

    private String                    lastPlayedSuite        = "";

    /**
     * Constructor
     */
    protected ReportAppender( String name, Filter filter, Layout<? extends Serializable> layout,
                              boolean ignoreExceptions, Property[] properties ) {

        super(name, filter, layout, ignoreExceptions, properties);
        isReportAppenderActive = true;
    }

    @PluginFactory
    public static ReportAppender createAppender(
                                                 @PluginAttribute( "name") String name,
                                                 @PluginElement( "Filter") Filter filter,
                                                 @PluginElement( "Layout") Layout layout ) {

        /* since there is no requireLayout() method
         * hopefully either NPE will be thrown later then a null layout is used,
         * or some default one will be used that causes an error, either in the tests or visually when comparing this and the old implementation
        */
        return new ReportAppender(name, filter, layout, false, null);
    }

    public String getMailSubjectFormat() {

        return mailSubjectPrefix;
    }

    public void setMailSubjectFormat(
                                      String mailSubjectFormat ) {

        this.mailSubjectPrefix = mailSubjectFormat;
    }

    /**
     * @return true if the Report Appender is active, otherwise returns false
     */
    public static boolean isAppenderActive() {

        return isReportAppenderActive;
    }

    @Override
    public void append( LogEvent event ) {

        // All events from all threads come into here.
        if (event instanceof AbstractLoggingEvent) {
            AbstractLoggingEvent dbLoggingEvent = (AbstractLoggingEvent) event;

            switch (dbLoggingEvent.getEventType()) {

                case START_RUN:
                    run = new RunWrapper();
                    suitesMap = new HashMap<String, SuiteWrapper>();

                    StartRunEvent startRunEvent = (StartRunEvent) event;

                    run.productName = startRunEvent.getProductName();
                    run.versionName = startRunEvent.getVersionName();
                    run.buildName = startRunEvent.getBuildName();
                    run.os = startRunEvent.getOsName();
                    run.runName = startRunEvent.getRunName();
                    run.setStartTimestamp(startRunEvent.getTimestamp());
                    break;

                case START_SUITE:
                    StartSuiteEvent startSuiteEvent = (StartSuiteEvent) event;

                    lastPlayedSuite = startSuiteEvent.getSuiteName();
                    if (!suitesMap.containsKey(lastPlayedSuite)) {
                        SuiteWrapper newSuite = new SuiteWrapper();
                        newSuite.name = startSuiteEvent.getSuiteName();
                        newSuite.packageName = startSuiteEvent.getPackage();
                        newSuite.setStartTimestamp(startSuiteEvent.getTimestamp());
                        suitesMap.put(newSuite.packageName + "." + newSuite.name, newSuite);

                        run.addSuite(newSuite);
                    }
                    break;

                case START_TEST_CASE:

                    StartTestCaseEvent startTestcaseEvent = (StartTestCaseEvent) dbLoggingEvent;
                    startTestcaseEvent.getSuiteFullName();

                    Testcase newTestcase = new Testcase();
                    newTestcase.scenarioName = startTestcaseEvent.getScenarioName();
                    newTestcase.name = startTestcaseEvent.getTestcaseName();
                    suitesMap.get(startTestcaseEvent.getSuiteFullName()).addTestcase(newTestcase);

                    /*
                     * There are very rare cases when the test harness system does not execute all tests from
                     * same suite one after another, but there will be some tests from another suite executed
                     * in between.
                     *
                     * That is why the next line navigates to the right suite when starting the test scenario
                     */
                    lastPlayedSuite = startTestcaseEvent.getSuiteFullName();
                    break;

                case END_TEST_CASE:
                    EndTestCaseEvent endTestCaseEvent = (EndTestCaseEvent) event;

                    // update the result of the current testcase
                    suitesMap.get(lastPlayedSuite).getLastTestcase().result = endTestCaseEvent
                                                                                              .getTestCaseResult()
                                                                                              .toInt();
                    break;

                case END_SUITE:
                    EndSuiteEvent endSuiteEvent = (EndSuiteEvent) event;

                    SuiteWrapper currentSuite = getCurrentSuite();
                    if (currentSuite != null) {
                        currentSuite.calculateFinalStatistics();
                        currentSuite.setEndTimestamp(endSuiteEvent.getTimestamp());
                        currentSuite.testcasesPassedPercent = "0";
                        if (currentSuite.testcasesTotal > 0) {
                            currentSuite.testcasesPassedPercent = String.valueOf( (currentSuite.testcasesTotal
                                                                                   - currentSuite.testcasesFailed
                                                                                   - currentSuite.testcasesSkipped)
                                                                                  * 100
                                                                                  / currentSuite.testcasesTotal);
                        }
                    }

                    // we cleanup here in case first Scenario of next Suite has same name
                    // as last Scenario of current Suite
                    lastPlayedSuite = "";
                    break;

                case END_RUN:
                    // The run is over, we want to mail a report about this run

                    EndRunEvent endRunEvent = (EndRunEvent) event;

                    run.calculateFinalStatistics();
                    run.setEndTimestamp(endRunEvent.getTimestamp());
                    run.testcasesPassedPercent = "0";
                    if (run.testcasesTotal > 0) {
                        run.testcasesPassedPercent = String.valueOf( (run.testcasesTotal
                                                                      - run.testcasesFailed
                                                                      - run.testcasesSkipped)
                                                                     * 100 / run.testcasesTotal);

                        try {
                            // format the report
                            ReportFormatter reportFormatter = new ReportFormatter(run);
                            // check if the AtsReportListener is initialized and if
                            // so, just add the test data to the map
                            if (!combinedHtmlMailReport) {
                                // send report by mail
                                MailReportSender mailReportSender = new MailReportSender(generateMailSubject(run,
                                                                                                             reportFormatter.getRunsState()),
                                                                                         reportFormatter.toHtml());
                                mailReportSender.send();
                            } else {
                                // delay report to collect data for all runs
                                htmlReportsList.add(run);
                            }
                        } catch (Exception e) {
                            /**
                             * apparently using the error handler from the parent, which is DefaultErrorHandler
                             * is preferable, since it prevents the console to be flooded with messages,
                             * e.g. there is a default (5ns) interval between calls to any error() method
                             */
                            error("Error processing/mailing log report", e);
                        }
                    }
                    break;
            }
        }

    }

    /**
     * Set if you will use AtsReportListener or not.
     */
    public static void setCombinedHtmlMailReport(
                                                  boolean useAtsReportListener ) {

        ReportAppender.combinedHtmlMailReport = useAtsReportListener;
    }

    /**
     * Get the map with the tests data, that should be sent at once
     */
    public static List<RunWrapper> getRuns() {

        return htmlReportsList;
    }

    private SuiteWrapper getCurrentSuite() {

        if ("".equals(lastPlayedSuite)) {
            return null;
        } else {
            return suitesMap.get(lastPlayedSuite);
        }
    }

    private String generateMailSubject(
                                        RunWrapper run,
                                        String runState ) {

        return mailSubjectPrefix.replaceAll("\\{RUN\\}", run.runName)
                                .replaceAll("\\{PRODUCT\\}", run.productName)
                                .replaceAll("\\{VERSION\\}", run.versionName)
                                .replaceAll("\\{BUILD\\}", run.buildName)
                                .replaceAll("\\{OS\\}", run.os)
                                .replaceAll("\\{STATE\\}", runState);
    }

}
