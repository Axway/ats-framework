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
package com.axway.ats.log.report.model;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.axway.ats.log.report.exceptions.MailReportSendException;

@PowerMockIgnore( "javax.management.*")
@RunWith( PowerMockRunner.class)
@PrepareForTest( { Transport.class, ReportConfigurator.class })
public class Test_MailReportSender {

    private ReportConfigurator mockReportConfigurator;

    @Before
    public void setUp() {

        mockStatic(ReportConfigurator.class);
        mockReportConfigurator = createMock(ReportConfigurator.class);
    }

    @Test
    public void sendReport() throws MessagingException {

        mockStatic(Transport.class);

        expect(ReportConfigurator.getInstance()).andReturn(mockReportConfigurator);
        expect(mockReportConfigurator.getSmtpServerName()).andReturn("localhost");
        expect(mockReportConfigurator.getSmtpServerPort()).andReturn("25");
        expect(mockReportConfigurator.getAddressesTo()).andReturn(new String[]{ "userTo" });
        expect(mockReportConfigurator.getAddressesCc()).andReturn(new String[0]);
        expect(mockReportConfigurator.getAddressesBcc()).andReturn(new String[0]);
        expect(mockReportConfigurator.getAddressFrom()).andReturn("userFrom");

        Transport.send(isA(Message.class));

        replayAll();

        triggerRun();

        verifyAll();
    }

    @Test( expected = MailReportSendException.class)
    public void errorOnSend() throws MessagingException {

        mockStatic(Transport.class);

        expect(ReportConfigurator.getInstance()).andReturn(mockReportConfigurator);
        expect(mockReportConfigurator.getSmtpServerName()).andReturn("localhost");
        expect(mockReportConfigurator.getSmtpServerPort()).andReturn("25");
        expect(mockReportConfigurator.getAddressesTo()).andReturn(new String[]{ "userTo" });
        expect(mockReportConfigurator.getAddressesCc()).andReturn(new String[0]);
        expect(mockReportConfigurator.getAddressesBcc()).andReturn(new String[0]);
        expect(mockReportConfigurator.getAddressFrom()).andReturn("userFrom");

        Transport.send(isA(Message.class));
        expectLastCall().andThrow(new MessagingException());

        replayAll();

        triggerRun();

        verifyAll();
    }

    @Test( expected = MailReportSendException.class)
    public void emptyAddressTo() throws MessagingException {

        expect(ReportConfigurator.getInstance()).andReturn(mockReportConfigurator);
        expect(mockReportConfigurator.getSmtpServerName()).andReturn("localhost");
        expect(mockReportConfigurator.getSmtpServerPort()).andReturn("25");
        expect(mockReportConfigurator.getAddressesTo()).andReturn(new String[]{ "" });
        expect(mockReportConfigurator.getAddressesCc()).andReturn(new String[0]);
        expect(mockReportConfigurator.getAddressesBcc()).andReturn(new String[0]);
        expect(mockReportConfigurator.getAddressFrom()).andReturn("userFrom");

        replayAll();

        triggerRun();

        verifyAll();
    }

    @Test( expected = MailReportSendException.class)
    public void emptyAddressCc() throws MessagingException {

        expect(ReportConfigurator.getInstance()).andReturn(mockReportConfigurator);
        expect(mockReportConfigurator.getSmtpServerName()).andReturn("localhost");
        expect(mockReportConfigurator.getSmtpServerPort()).andReturn("25");
        expect(mockReportConfigurator.getAddressesTo()).andReturn(new String[0]);
        expect(mockReportConfigurator.getAddressesCc()).andReturn(new String[]{ "" });
        expect(mockReportConfigurator.getAddressesBcc()).andReturn(new String[0]);
        expect(mockReportConfigurator.getAddressFrom()).andReturn("userFrom");

        replayAll();

        triggerRun();

        verifyAll();
    }

    @Test( expected = MailReportSendException.class)
    public void emptyAddressBcc() throws MessagingException {

        expect(ReportConfigurator.getInstance()).andReturn(mockReportConfigurator);
        expect(mockReportConfigurator.getSmtpServerName()).andReturn("localhost");
        expect(mockReportConfigurator.getSmtpServerPort()).andReturn("25");
        expect(mockReportConfigurator.getAddressesTo()).andReturn(new String[0]);
        expect(mockReportConfigurator.getAddressesCc()).andReturn(new String[0]);
        expect(mockReportConfigurator.getAddressesBcc()).andReturn(new String[]{ "" });
        expect(mockReportConfigurator.getAddressFrom()).andReturn("userFrom");

        replayAll();

        triggerRun();

        verifyAll();
    }

    @Test( expected = MailReportSendException.class)
    public void emptyAddressFrom() throws MessagingException {

        expect(ReportConfigurator.getInstance()).andReturn(mockReportConfigurator);
        expect(mockReportConfigurator.getSmtpServerName()).andReturn("localhost");
        expect(mockReportConfigurator.getSmtpServerPort()).andReturn("25");
        expect(mockReportConfigurator.getAddressesTo()).andReturn(new String[0]);
        expect(mockReportConfigurator.getAddressesCc()).andReturn(new String[0]);
        expect(mockReportConfigurator.getAddressesBcc()).andReturn(new String[0]);
        expect(mockReportConfigurator.getAddressFrom()).andReturn("");

        replayAll();

        triggerRun();

        verifyAll();
    }

    private void triggerRun() {

        MailReportSender mailReportSender = new MailReportSender("mail subject", "mail content");
        mailReportSender.send();
    }
}
