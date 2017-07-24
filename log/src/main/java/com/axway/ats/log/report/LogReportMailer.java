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
package com.axway.ats.log.report;

import java.util.List;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.log.report.model.MailReportSender;
import com.axway.ats.log.report.model.ReportExtractor;
import com.axway.ats.log.report.model.ReportFormatter;
import com.axway.ats.log.report.model.RunWrapper;

/**
 * Extracts runs from the log database, formats the extracted data into a report 
 * which then send by email
 */
@PublicAtsApi
public class LogReportMailer {

    private String dbHost;
    private String dbName;
    private String dbUser;
    private String dbPassword;
    private int[]  runIds;
    private int    testExplorerWebPort;
    private String testExplorerInstanceName;
    private String mailSubject;

    /**
     * Construct mail report
     * @param dbHost the database host to get runs from
     * @param dbName the database name to get runs from
     * @param dbUser the user name used for authentication to the database
     * @param dbPassword the user password used for authentication to the database
     * @param runIds the ids of the runs to extract
     * @param mailSubject mail subject
     */
    @PublicAtsApi
    public LogReportMailer( String dbHost,
                            String dbName,
                            String dbUser,
                            String dbPassword,
                            int[] runIds,
                            String mailSubject ) {

       this(dbHost, dbName, dbUser, dbPassword, -1, null, runIds, mailSubject);
    }
    
    /**
     * @param dbHost the database host to get runs from
     * @param dbName the database name to get runs from
     * @param dbUser the user name used for authentication to the database
     * @param dbPassword the user password used for authentication to the database
     * @param runIds the ids of the runs to extract
     * @param testExplorerWebPort the HTTP(S) port of the TestExplorer web application
     * @param testExplorerInstanceName the database test explorer to get runs from
     * @param mailSubject mail subject
     */
    @PublicAtsApi
    public LogReportMailer( String dbHost,
                            String dbName,
                            String dbUser,
                            String dbPassword,
                            int testExplorerWebPort,
                            String testExplorerInstanceName,
                            int[] runIds,
                            String mailSubject ) {

        this.dbHost = dbHost;
        this.dbName = dbName;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.testExplorerWebPort = testExplorerWebPort;
        this.testExplorerInstanceName = testExplorerInstanceName;
        this.runIds = runIds;
        this.mailSubject = mailSubject;
    }

    /**
     * Email the report
     */
    @PublicAtsApi
    public void send() {

        // get runs from log database
        ReportExtractor reportExtactor = new ReportExtractor( dbHost, dbName, dbUser, dbPassword );
        List<RunWrapper> runs = reportExtactor.extract( runIds );

        // format the report
        ReportFormatter reportFormatter = new ReportFormatter( runs,
                                                               mailSubject,
                                                               dbHost,
                                                               dbName,
                                                               testExplorerWebPort,
                                                               testExplorerInstanceName );

        // send report by mail
        MailReportSender mailReportSender = new MailReportSender( reportFormatter.getDescription(),
                                                                  reportFormatter.toHtml() );
        mailReportSender.send();
    }
}
