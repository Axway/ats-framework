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

import com.axway.ats.config.AbstractConfigurator;
import com.axway.ats.config.exceptions.NoSuchPropertyException;
import com.axway.ats.log.report.exceptions.MailReportPropertyException;

/**
 * This class is used to read a set of properties used by the log report mailer
 */
public class ReportConfigurator extends AbstractConfigurator {

    private static final String       SMTP_SERVER_NAME        = "log.mailreport.smtpserver.name";
    private static final String       SMTP_SERVER_PORT        = "log.mailreport.smtpserver.port";
    private static final String       ADDRESSES_TO            = "log.mailreport.addresses.to";
    private static final String       ADDRESSES_CC            = "log.mailreport.addresses.cc";
    private static final String       ADDRESSES_BCC           = "log.mailreport.addresses.bcc";
    private static final String       ADDRESSES_FROM          = "log.mailreport.addresses.from";

    private static final String       TEST_EXPLORER_WEB_PATH  = "log.mailreport.testexplorer.webpath";
    private static final String       TEST_EXPLORER_WEB_PORT  = "log.mailreport.testexplorer.webport";

    private static final String       CONFIGURATION_FILE_PATH = "/ats.report.properties";

    private String                    smtpServerName          = "";
    private String                    smtpServerPort          = "";
    private String[]                  addressesTo             = new String[0];
    private String[]                  addressesCc             = new String[0];
    private String[]                  addressesBcc            = new String[0];
    private String                    addressFrom             = "";
    private String                    testExplorerWebPath;
    private String                    testExplorerWebPort;

    /**
     * The singleton instance for this configurator
     */
    private static ReportConfigurator instance;

    private ReportConfigurator() {

        super();
    }

    /**
     * @return an instance of this configurator
     */
    public static ReportConfigurator getInstance() {

        if (instance == null) { // synchronize multithreaded access in the case of running parallel tests
            synchronized (ReportConfigurator.class) {
                if (instance == null) {
                    instance = new ReportConfigurator();

                    instance.addConfigFileFromClassPath(CONFIGURATION_FILE_PATH, true, false);
                }
            }
        }

        return instance;
    }

    @Override
    protected void reloadData() {

        // load mandatory properties
        try {
            smtpServerName = getProperty(SMTP_SERVER_NAME);
            smtpServerPort = getProperty(SMTP_SERVER_PORT);

            addressFrom = getProperty(ADDRESSES_FROM);
            addressesTo = getProperty(ADDRESSES_TO).split(",");
        } catch (NoSuchPropertyException e) {
            final String errMsg = "At least one property needed for mailing a log report is missing:\n"
                                  + SMTP_SERVER_NAME + "\n" + SMTP_SERVER_PORT + "\n" + ADDRESSES_FROM + "\n"
                                  + ADDRESSES_TO;
            throw new MailReportPropertyException(errMsg, e);
        }

        // load optional properties
        testExplorerWebPath = getOptionalPropertyValue(TEST_EXPLORER_WEB_PATH);
        testExplorerWebPort = getOptionalPropertyValue(TEST_EXPLORER_WEB_PORT);
        addressesCc = getOptionalPropertyValues(ADDRESSES_CC);
        addressesBcc = getOptionalPropertyValues(ADDRESSES_BCC);
    }

    public String getOptionalPropertyValue(
                                            String propertyName ) {

        try {
            return getProperty(propertyName);
        } catch (NoSuchPropertyException nspe) {
            return null;
        }

    }

    public String[] getOptionalPropertyValues(
                                               String propertyName ) {

        try {
            return getProperty(propertyName).split(",");
        } catch (NoSuchPropertyException e) {
            return new String[0];
        }
    }

    /**
     * @return the smtpServerName
     */
    public String getSmtpServerName() {

        return smtpServerName;
    }

    /**
     * @return the smtpServerPort
     */
    public String getSmtpServerPort() {

        return smtpServerPort;
    }

    /**
     * @return the addressesTo
     */
    public String[] getAddressesTo() {

        return addressesTo;
    }

    /**
     * @return the addressesCc
     */
    public String[] getAddressesCc() {

        return addressesCc;
    }

    /**
     * @return the addressesBcc
     */
    public String[] getAddressesBcc() {

        return addressesBcc;
    }

    /**
     * @return the addressesFrom
     */
    public String getAddressFrom() {

        return addressFrom;
    }

    public String getTestExplorerWebPath() {

        return testExplorerWebPath;
    }

    public void setTestExplorerWebPath(
                                        String testExplorerWebPath ) {

        this.testExplorerWebPath = testExplorerWebPath;
    }

    public String getTestExplorerWebPort() {

        return testExplorerWebPort;
    }

    public void setTestExplorerWebPort(
                                        String testExplorerWebPort ) {

        this.testExplorerWebPort = testExplorerWebPort;
    }

}
