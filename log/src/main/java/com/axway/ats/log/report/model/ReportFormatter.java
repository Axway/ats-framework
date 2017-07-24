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

import static com.axway.ats.log.report.model.html.ReportHtmlCodes.EMPTY_TBL_ROW;
import static com.axway.ats.log.report.model.html.ReportHtmlCodes.END_BODY;
import static com.axway.ats.log.report.model.html.ReportHtmlCodes.END_HEAD;
import static com.axway.ats.log.report.model.html.ReportHtmlCodes.END_PAGE;
import static com.axway.ats.log.report.model.html.ReportHtmlCodes.END_TBL;
import static com.axway.ats.log.report.model.html.ReportHtmlCodes.END_TBL_CELL;
import static com.axway.ats.log.report.model.html.ReportHtmlCodes.END_TBL_ROW;
import static com.axway.ats.log.report.model.html.ReportHtmlCodes.LINE_BREAK;
import static com.axway.ats.log.report.model.html.ReportHtmlCodes.START_BODY;
import static com.axway.ats.log.report.model.html.ReportHtmlCodes.START_HEAD;
import static com.axway.ats.log.report.model.html.ReportHtmlCodes.START_PAGE;
import static com.axway.ats.log.report.model.html.ReportHtmlCodes.START_TBL_CELL;
import static com.axway.ats.log.report.model.html.ReportHtmlCodes.START_TBL_CELL_TWO_ROWS;
import static com.axway.ats.log.report.model.html.ReportHtmlCodes.START_TBL_EVENROW;
import static com.axway.ats.log.report.model.html.ReportHtmlCodes.START_TBL_HEADERROW;
import static com.axway.ats.log.report.model.html.ReportHtmlCodes.START_TBL_ODDROW;
import static com.axway.ats.log.report.model.html.ReportHtmlCodes.START_TBL_ROW_FAILED_RUN;
import static com.axway.ats.log.report.model.html.ReportHtmlCodes.START_TBL_ROW_FAILED_SUITE;
import static com.axway.ats.log.report.model.html.ReportHtmlCodes.START_TBL_ROW_PASSED_RUN;

import java.util.ArrayList;
import java.util.List;

import com.axway.ats.log.appenders.ActiveDbAppender;
import com.axway.ats.log.autodb.entities.Run;
import com.axway.ats.log.autodb.entities.Suite;
import com.axway.ats.log.report.exceptions.ReportFormatterException;
import com.axway.ats.log.report.model.html.css.CssSettings;

/**
 * Extracts the data from the provided runs and formats it into a suitable
 * report form
 */
public class ReportFormatter {

    private final List<RunWrapper> runs;

    private final String           description;
    private String                 dbHost;
    private String                 dbName;
    private int                    testExplorerWebPort;
    private String                 testExplorerPath;
    private String                 testExplorerURL;

    /**
     * @param run
     *            the run to create a report for
     */
    public ReportFormatter( RunWrapper run ) {

        this.runs = new ArrayList<RunWrapper>();
        this.runs.add( run );

        this.description = "Report for automated run '" + run.runName + "', product '" + run.productName
                           + "', version '" + run.versionName + "', build '" + run.buildName + "'";
    }

    /**
     * @param runs the runs to create a report for
     * @param description   description about these runs
     * @param dbHost    the database hostname
     * @param dbName    the database name
     * @param port  the Test Explorer web port
     * @param testExplorerPath  Context path to the TestExplorer web application like "/TestExplorer-3.4.0"
     */
    public ReportFormatter( List<RunWrapper> runs,
                            String description,
                            String dbHost,
                            String dbName,
                            int testExplorerWebPort,
                            String testExplorerPath ) {

        this.runs = runs;
        this.description = description;
        this.dbHost = dbHost;
        this.dbName = dbName;
        this.testExplorerWebPort = testExplorerWebPort;
        this.testExplorerPath = testExplorerPath;
    }

    /**
     * Get the description. A state is appended - PASSED or FAILED
     * 
     * @return
     */
    public String getDescription() {

        return description + getRunsState();
    }

    /**
     * Format the report into an html form
     * 
     * @return
     */
    public String toHtml() {

        if( runs.size() == 0 ) {
            final String errMsg = "No runs provided";
            throw new ReportFormatterException( errMsg );
        }

        StringBuilder sb = new StringBuilder();

        sb.append( START_PAGE );
        sb.append( START_HEAD );
        sb.append( CssSettings.getCssStyle() );
        sb.append( END_HEAD );

        sb.append( START_BODY );
        sb.append( getContentAsHTMLTable() );
        sb.append( END_BODY );
        sb.append( END_PAGE );

        return sb.toString();
    }

    /**
     * create just the html table with the test data for the report
     */
    public String getContentAsHTMLTable() {

        StringBuilder tableBody = new StringBuilder();
        tableBody.append( "<table width=100% border=0 cellspacing=1 cellpadding=5>" );
        for( RunWrapper run : runs ) {

            // START - Run header
            tableBody.append( START_TBL_HEADERROW );
            tableBody.append( START_TBL_CELL_TWO_ROWS + "Run" + END_TBL_CELL );
            tableBody.append( START_TBL_CELL_TWO_ROWS + "Product" + END_TBL_CELL );
            tableBody.append( START_TBL_CELL_TWO_ROWS + "Version" + END_TBL_CELL );
            tableBody.append( START_TBL_CELL_TWO_ROWS + "Build" + END_TBL_CELL );
            tableBody.append( START_TBL_CELL_TWO_ROWS + "OS" + END_TBL_CELL );
            tableBody.append( "<td colspan=4 align=center>" + "Scenarios" + END_TBL_CELL );
            tableBody.append( "<td colspan=3 align=center>" + "Test cases" + END_TBL_CELL );
            tableBody.append( START_TBL_CELL_TWO_ROWS + "Start" + END_TBL_CELL );
            tableBody.append( START_TBL_CELL_TWO_ROWS + "End" + END_TBL_CELL );
            tableBody.append( START_TBL_CELL_TWO_ROWS + "Duration" + END_TBL_CELL );
            tableBody.append( START_TBL_CELL_TWO_ROWS + "User note" + END_TBL_CELL );
            tableBody.append( END_TBL_ROW );

            tableBody.append( START_TBL_HEADERROW );

            tableBody.append( START_TBL_CELL + "total" + END_TBL_CELL );
            tableBody.append( START_TBL_CELL + "%&nbsp;passed" + END_TBL_CELL );
            tableBody.append( START_TBL_CELL + "failed" + END_TBL_CELL );
            tableBody.append( START_TBL_CELL + "skipped" + END_TBL_CELL );

            tableBody.append( START_TBL_CELL + "total" + END_TBL_CELL );
            tableBody.append( START_TBL_CELL + "%&nbsp;passed" + END_TBL_CELL );
            tableBody.append( START_TBL_CELL + "failed" + END_TBL_CELL );

            tableBody.append( END_TBL_ROW );
            // END - Run header

            // START - Run data
            if( run.scenariosFailed + run.scenariosSkipped + run.testcasesFailed > 0 ) {
                tableBody.append( START_TBL_ROW_FAILED_RUN );
            } else {
                tableBody.append( START_TBL_ROW_PASSED_RUN );
            }

            if( this.dbHost != null ) {

                if( testExplorerWebPort > 0 ) {
                    testExplorerURL = "http://" + dbHost + ":" + testExplorerWebPort;
                } else {
                    testExplorerURL = "http://" + dbHost;
                }
                testExplorerURL += "/" + testExplorerPath + "/suites?runId=" + run.runId + "&dbname="
                                   + dbName;

            } else {

                ActiveDbAppender dbAppender = ActiveDbAppender.getCurrentInstance();
                ReportConfigurator reportConfigurator = ReportConfigurator.getInstance();
                if( dbAppender != null && reportConfigurator.getTestExplorerWebPath() != null ) {

                    if( reportConfigurator.getTestExplorerWebPort() != null ) {

                        testExplorerURL = "http://" + dbAppender.getHost() + ":"
                                          + reportConfigurator.getTestExplorerWebPort();

                    } else {
                        testExplorerURL = "http://" + dbAppender.getHost();
                    }
                    testExplorerURL += "/" + reportConfigurator.getTestExplorerWebPath() + "/suites?runId="
                                       + dbAppender.getRunId() + "&dbname=" + dbAppender.getDatabase();
                }
            }

            if( testExplorerURL != null ) {
                tableBody.append( START_TBL_CELL + "<a href =\"" + testExplorerURL + "\">" + run.runName
                                  + "</a>" + END_TBL_CELL );
            } else {
                tableBody.append( START_TBL_CELL + run.runName + END_TBL_CELL );
            }

            tableBody.append( START_TBL_CELL + run.productName + END_TBL_CELL );
            tableBody.append( START_TBL_CELL + run.versionName + END_TBL_CELL );
            tableBody.append( START_TBL_CELL + run.buildName + END_TBL_CELL );
            tableBody.append( START_TBL_CELL + run.os + END_TBL_CELL );

            tableBody.append( START_TBL_CELL + run.scenariosTotal + END_TBL_CELL );
            tableBody.append( START_TBL_CELL
                              + ( run.scenariosTotal - run.scenariosFailed - run.scenariosSkipped ) * 100
                                / run.scenariosTotal
                              + END_TBL_CELL );
            tableBody.append( START_TBL_CELL + run.scenariosFailed + END_TBL_CELL );
            tableBody.append( START_TBL_CELL + run.scenariosSkipped + END_TBL_CELL );

            tableBody.append( START_TBL_CELL + run.testcasesTotal + END_TBL_CELL );
            tableBody.append( START_TBL_CELL + run.testcasesPassedPercent + END_TBL_CELL );
            tableBody.append( START_TBL_CELL + run.testcasesFailed + END_TBL_CELL );

            tableBody.append( START_TBL_CELL + run.getDateStart() + END_TBL_CELL );
            tableBody.append( START_TBL_CELL + run.getDateEnd() + END_TBL_CELL );
            tableBody.append( START_TBL_CELL + run.getDuration(0) + END_TBL_CELL );

            tableBody.append( START_TBL_CELL + ( run.userNote == null
                                                              ? ""
                                                                      : run.userNote )
                              + END_TBL_CELL );

            tableBody.append( END_TBL_ROW );
            // END - Run data

            // put some space between run and its suites
            tableBody.append( EMPTY_TBL_ROW );

            // START - Suites header
            tableBody.append( START_TBL_HEADERROW );
            tableBody.append( START_TBL_CELL_TWO_ROWS + "Suites" + END_TBL_CELL );
            tableBody.append( "<td colspan=4 rowspan=2 align=center>" + END_TBL_CELL );
            tableBody.append( "<td colspan=4 align=center>" + "Scenarios" + END_TBL_CELL );
            tableBody.append( "<td colspan=3 align=center>" + "Test cases" + END_TBL_CELL );
            tableBody.append( START_TBL_CELL_TWO_ROWS + "Start" + END_TBL_CELL );
            tableBody.append( START_TBL_CELL_TWO_ROWS + "End" + END_TBL_CELL );
            tableBody.append( START_TBL_CELL_TWO_ROWS + "Duration" + END_TBL_CELL );
            tableBody.append( START_TBL_CELL_TWO_ROWS + "User note" + END_TBL_CELL );
            tableBody.append( END_TBL_ROW );

            tableBody.append( START_TBL_HEADERROW );

            tableBody.append( START_TBL_CELL + "total" + END_TBL_CELL );
            tableBody.append( START_TBL_CELL + "%&nbsp;passed" + END_TBL_CELL );
            tableBody.append( START_TBL_CELL + "failed" + END_TBL_CELL );
            tableBody.append( START_TBL_CELL + "skipped" + END_TBL_CELL );

            tableBody.append( START_TBL_CELL + "total" + END_TBL_CELL );
            tableBody.append( START_TBL_CELL + "%&nbsp;passed" + END_TBL_CELL );
            tableBody.append( START_TBL_CELL + "failed" + END_TBL_CELL );

            tableBody.append( END_TBL_ROW );
            // END - Suites header

            // insert some space between run and its suites
            tableBody.append( LINE_BREAK );

            // START - Suites data
            boolean evenRow = false;
            for( Suite suite : run.getSuites() ) {

                if( suite.scenariosFailed + suite.scenariosSkipped + suite.testcasesFailed > 0 ) {
                    // the run has failed if contains a failed or skipped
                    // scenario
                    tableBody.append( START_TBL_ROW_FAILED_SUITE );
                } else {
                    if( evenRow ) {
                        tableBody.append( START_TBL_EVENROW );
                    } else {
                        tableBody.append( START_TBL_ODDROW );
                    }
                }

                tableBody.append( START_TBL_CELL + suite.name + END_TBL_CELL );
                tableBody.append( "<td colspan=4 align=center>" + END_TBL_CELL );

                tableBody.append( START_TBL_CELL + suite.scenariosTotal + END_TBL_CELL );
                tableBody.append( START_TBL_CELL
                                  + ( suite.scenariosTotal - suite.scenariosFailed - suite.scenariosSkipped )
                                    * 100 / suite.scenariosTotal
                                  + END_TBL_CELL );

                tableBody.append( START_TBL_CELL + suite.scenariosFailed + END_TBL_CELL );
                tableBody.append( START_TBL_CELL + suite.scenariosSkipped + END_TBL_CELL );

                tableBody.append( START_TBL_CELL + suite.testcasesTotal + END_TBL_CELL );
                tableBody.append( START_TBL_CELL + suite.testcasesPassedPercent + END_TBL_CELL );
                tableBody.append( START_TBL_CELL + suite.testcasesFailed + END_TBL_CELL );

                tableBody.append( START_TBL_CELL + suite.getDateStart() + END_TBL_CELL );
                tableBody.append( START_TBL_CELL + suite.getDateEnd() + END_TBL_CELL );
                tableBody.append( START_TBL_CELL + suite.getDuration( 0 ) + END_TBL_CELL );

                tableBody.append( "<td align=center>" + ( suite.userNote == null
                                                                         ? ""
                                                                                 : suite.userNote )
                                  + END_TBL_CELL );

                evenRow = !evenRow;
            }
            // END - Suites data

            // insert some space before the next run
            tableBody.append( "<tr><td colspan=16 style=\"height: 40px;\"></td></tr>" );
        }
        tableBody.append( END_TBL );

        return tableBody.toString();
    }

    /**
     * Get the state of the report. It is FAILED if there is at least one failed
     * or skipped test case. It is PASSED when there is no any failed or skipped
     * test cases.
     * 
     * @return
     */
    public String getRunsState() {

        String allRunsPassed = "PASSED";
        for( Run run : runs ) {

            if( run.scenariosFailed + run.scenariosSkipped + run.testcasesFailed > 0 ) {
                allRunsPassed = "FAILED";
                break;
            }
        }

        return allRunsPassed;
    }
}
