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
package com.axway.ats.action.mail;

import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.event.TransportEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import com.axway.ats.action.ActionLibraryConfigurator;
import com.axway.ats.action.BaseTest;
import com.axway.ats.action.mail.model.MailTransportListener.DELIVERY_STATE;
import com.axway.ats.action.model.ActionException;
import com.axway.ats.action.objects.FilePackage;
import com.axway.ats.action.objects.MimePackage;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.action.objects.model.RecipientType;
import com.axway.ats.action.objects.model.WrongPackageException;
import com.axway.ats.common.system.OperatingSystemType;

public class Test_SmtpSender extends BaseTest {
    private final static Logger LOG = LogManager.getLogger(Test_SmtpSender.class);

    private MailSender  mailSender;
    private MimePackage mail;

    private TransportMock transportMock;

    @Before
    public void setUp() throws ActionException {

        mailSender = new MailSender();
        mailSender.setMailServer("127.0.0.1", 65535); // Used for fast fail. No DNS queries

        // mock the transport
        transportMock = new TransportMock();
        Whitebox.setInternalState(mailSender, "transport", transportMock);

        // create a message for test purposes
        mail = new MimePackage();
        mail.setRecipient(RecipientType.TO, new String[]{ "sender@test.com" });
        mail.setSender("sender@test.com");
    }

    @Test
    public void sendAndGetDelivered() throws ActionException {

        transportMock.setDeliveryResult(DELIVERY_STATE.DELIVERED);
        mailSender.send(mail);

    }

    @Test( expected = ActionException.class )
    public void sendAndGetErrorDelivering() throws ActionException {

        transportMock.setDeliveryResult(DELIVERY_STATE.ERROR_DELIVERING);
        mailSender.send(mail);

    }

    @Test( expected = ActionException.class )
    public void sendAndPartiallyGetDelivered() throws ActionException {

        transportMock.setDeliveryResult(DELIVERY_STATE.PARTIALLY_DELIVERED);
        mailSender.send(mail);

    }

    @Test( expected = ActionException.class )
    public void noConnectionToHost() throws ActionException {

        // we are trying to get send a message to a fake smtp server,
        // the connection timeout will be triggered
        final long expectedTimeoutTime = System.currentTimeMillis()
                                         + ActionLibraryConfigurator.getInstance().getMailTimeout();

        boolean exceptionThrown = false;
        try {
            mailSender.send(mail);
        } catch (Exception e) {
            exceptionThrown = true;
            LOG.info("Exception: ",  e);
        } finally {
            long actualTimeoutTime = System.currentTimeMillis();
            Assert.assertTrue(actualTimeoutTime >= expectedTimeoutTime);
        }
        if (!exceptionThrown) {
            Assert.fail("No exception thrown as expected");
        }
    }

    @Test( expected = WrongPackageException.class )
    public void wrongPackage() throws PackageException, ActionException {

        mailSender.send(new FilePackage("127.0.0.1", "", OperatingSystemType.LINUX));
    }
}

class TransportMock extends Transport {

    private static final Logger log = LogManager.getLogger(TransportMock.class);

    private DELIVERY_STATE deliveryState;

    public TransportMock() {

        super(Session.getInstance(new Properties()), new URLName(""));
    }

    @Override
    public void connect() {

        log.info("fake SMTP connect");
    }

    @Override
    public void sendMessage(
            Message arg0,
            Address[] arg1 ) throws MessagingException {

        log.info("Fake message sending");

        if (deliveryState == DELIVERY_STATE.DELIVERED) {
            notifyTransportListeners(TransportEvent.MESSAGE_DELIVERED, null, null, null, null);
        } else if (deliveryState == DELIVERY_STATE.PARTIALLY_DELIVERED) {
            notifyTransportListeners(TransportEvent.MESSAGE_PARTIALLY_DELIVERED, null, null, null, null);
        } else if (deliveryState == DELIVERY_STATE.ERROR_DELIVERING) {
            notifyTransportListeners(TransportEvent.MESSAGE_NOT_DELIVERED, null, null, null, null);
        }
    }

    public void setDeliveryResult(
            DELIVERY_STATE deliveryState ) {

        this.deliveryState = deliveryState;
    }
}
