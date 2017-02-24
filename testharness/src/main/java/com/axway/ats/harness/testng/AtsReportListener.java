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
package com.axway.ats.harness.testng;

import java.util.List;

import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.xml.XmlSuite;

import com.axway.ats.log.appenders.ReportAppender;
import com.axway.ats.log.report.model.MailReportSender;
import com.axway.ats.log.report.model.ReportFormatter;

/**
 * AtsReportListener allows you to use nested XML for tests.
 */
public class AtsReportListener implements IReporter {

    private static boolean initialized       = false;
    // mail subject
    private String         mailSubjectFormat = "Report for automated runs. Execution status - ";

    public AtsReportListener() {
        ReportAppender.setCombinedHtmlMailReport( true );
    }

    @Override
    public void generateReport(
                                List<XmlSuite> arg0,
                                List<ISuite> arg1,
                                String arg2 ) {

        //we just need the report format, that why we set other fields null
        ReportFormatter reportFormatter = new ReportFormatter( ReportAppender.getRuns(),
                                                               mailSubjectFormat,
                                                               null,
                                                               null,
                                                               0,
                                                               null );
        // send report by mail
        MailReportSender mailReportSender = new MailReportSender( reportFormatter.getDescription(),
                                                                  reportFormatter.toHtml() );
        mailReportSender.send();

    }

    /**
     * Check if AtsReportListener is used
     */
    public static boolean isInitialized() {

        return initialized;
    }
}
