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
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.axway.ats.log.report.exceptions.MailReportPropertyException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.report.exceptions.MailReportSendException;
import com.axway.ats.log.report.model.ReportConfigurator;

/**
 * Can be used for sending a mail
 */
public class MailReportSender {

    private static final Logger log = LogManager.getLogger(MailReportSender.class);

    private String              subject;
    private String              body;

    /**
     * Constructor
     * 
     * @param subject mail subject
     * @param body mail content
     */
    public MailReportSender( String subject,
                             String body ) {

        this.subject = subject;
        this.body = body;
    }

    /**
     * Email the report
     * @throws MailReportPropertyException in case of detected property configuration issue
     */
    public void send() throws MailReportPropertyException {

        log.info("Sending log mail report");

        // get the info needed for sending a mail
        ReportConfigurator reportConfigurator = ReportConfigurator.getInstance();
        String smtpServerName = reportConfigurator.getSmtpServerName();
        String smtpServerPort = reportConfigurator.getSmtpServerPort();
        String[] addressesTo = reportConfigurator.getAddressesTo();
        String[] addressesCc = reportConfigurator.getAddressesCc();
        String[] addressesBcc = reportConfigurator.getAddressesBcc();
        String addressFrom = reportConfigurator.getAddressFrom();
        
        // Basic server name validation
        if (StringUtils.isNullOrEmpty(smtpServerName)) {
            throw new MailReportPropertyException("SMTP mail server name is empty. Do you have defined values in /ats.report.properties file (in the classpath)");
        }

        // Attaching to default Session
        Properties mailProperties = new Properties();
        mailProperties.put("mail.smtp.host", smtpServerName);
        mailProperties.put("mail.smtp.port", smtpServerPort);
        Session session = Session.getDefaultInstance(mailProperties);
        Message msg = new MimeMessage(session);

        String errMsg = "Error creating mail object";
        try {
            // mail addresses
            msg.setFrom(new InternetAddress(addressFrom));

            msg.setRecipients(Message.RecipientType.TO, transformAdresses(addressesTo));
            msg.setRecipients(Message.RecipientType.CC, transformAdresses(addressesCc));
            msg.setRecipients(Message.RecipientType.BCC, transformAdresses(addressesBcc));

            // mail subject
            msg.setSubject(subject);

            // mail content
            msg.setContent(body, "text/html");

            // other header information
            msg.setSentDate(new Date());
        } catch (AddressException e) {
            throw new MailReportSendException(errMsg, e);
        } catch (MessagingException e) {
            throw new MailReportSendException(errMsg, e);
        }

        // send the message
        errMsg = "Error sending mail";
        try {
            Transport.send(msg);
        } catch (MessagingException e) {
            throw new MailReportSendException(errMsg, e);
        }

        log.info("Log mail report sent ok");
    }

    /**
     * Transfer string addresses into java-mail addresses
     * 
     * @param stringAddresses
     * @return
     * @throws AddressException
     */
    private InternetAddress[] transformAdresses(
                                                 String[] stringAddresses ) throws AddressException {

        List<InternetAddress> mailAddresses = new ArrayList<InternetAddress>();
        for (String stringAddress : stringAddresses) {
            mailAddresses.add(new InternetAddress(stringAddress));
        }

        return mailAddresses.toArray(new InternetAddress[mailAddresses.size()]);
    }
}
