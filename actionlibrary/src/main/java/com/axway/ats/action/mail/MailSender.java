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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.action.ActionLibraryConfigurator;
import com.axway.ats.action.mail.model.MailTransportListener;
import com.axway.ats.action.mail.model.MailTransportListener.DELIVERY_STATE;
import com.axway.ats.action.model.ActionException;
import com.axway.ats.action.model.PackageSender;
import com.axway.ats.action.objects.MimePackage;
import com.axway.ats.action.objects.model.Package;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.action.objects.model.RecipientType;
import com.axway.ats.action.objects.model.WrongPackageException;
import com.axway.ats.action.security.PackageEncryptor;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.config.exceptions.NoSuchPropertyException;
import com.axway.ats.core.utils.StringUtils;

/**
 * Implementation for sending MIME packages
 *
 * <br><br>
 * <b>User guide</b>
 * <a href="https://axway.github.io/ats-framework/Mail-and-SMTP-operations.html">page</a>
 * related to this class
 */
@PublicAtsApi
public class MailSender extends PackageSender {

    private static final Logger             log            = LogManager.getLogger(MailSender.class);

    private final ActionLibraryConfigurator configurator;

    private Transport                       transport;

    protected Session                       session;                                            // it is accessed by the derived MailTLSSender

    private String                          mailHost;

    private long                            mailPort;

    protected Properties                    mailProperties = new Properties();

    private PackageEncryptor                encryptor;

    private PackageEncryptor                signer;

    /**
     * Generic constructor. It sets the mail(SMTP) host and port using the default values taken from the
     * ats.actionlibrary.properties file.
     *
     * @throws ActionException
     */
    @PublicAtsApi
    public MailSender() {

        // load the info needed for sending a mail
        configurator = ActionLibraryConfigurator.getInstance();

        // initialize transport
        setMailServer(configurator.getMailHost(), configurator.getMailPort());
    }

    /**
     * Overwrite the default mail host and port
     *
     * @param mailHost new mail host
     * @param mailPort new mail port
     * @throws ActionException
     */
    @PublicAtsApi
    public void setMailServer( String mailHost, long mailPort ) {

        this.mailHost = mailHost;
        this.mailPort = mailPort;

        mailProperties.put("mail.smtp.host", this.mailHost);
        mailProperties.put("mail.smtp.port", this.mailPort);

        try {
            String mailLocalAddress = configurator.getMailLocalAddress();
            if (!StringUtils.isNullOrEmpty(mailLocalAddress)) {
                mailProperties.put("mail.smtp.localaddress", mailLocalAddress);
            }
        } catch (NoSuchPropertyException nspe) {
            // this property is optional, no issue when not present
        }
    }

    /**
     * Sends a MIME package by invoking the following actions:
     *
     * <blockquote> 1. Tags the package, this can be later used for IMAP verification<br>
     * 2. Sings the package when a signer is specified<br>
     * 3. Encrypts the package when a encryptor is specified<br>
     * 4. Sends it </blockquote>
     *
     * @see com.axway.ats.action.model.PackageSender#send(com.axway.ats.action.objects.model.Package)
     */
    @Override
    @PublicAtsApi
    public void send( Package sourcePackage ) throws ActionException {

        if (! (sourcePackage instanceof MimePackage)) {
            throw new WrongPackageException("Could not send '" + sourcePackage.getClass().getSimpleName()
                                            + "' packages. " + MimePackage.class.getSimpleName() + " is expected");
        }

        // initialize the SMTP session
        initSession();

        MimePackage mimePackage = (MimePackage) sourcePackage;

        // tag the package
        mimePackage.tag();

        // sign the package if needed
        mimePackage = sign(mimePackage);

        // encrypt the package if needed
        mimePackage = encrypt(mimePackage);

        // then send
        final DELIVERY_STATE messageDeliveryState;
        try {

            log.info("Connect to mail server " + mailHost + " at port " + mailPort);
            Object messageSendingMutex = new Object();
            MailTransportListener transListener = new MailTransportListener(messageSendingMutex);

            transport.addTransportListener(transListener);
            transport.connect();

            log.info("Sending " + mimePackage.getDescription());
            transport.sendMessage(mimePackage.getMimeMessage(), extractAllRecipients(mimePackage));

            synchronized (messageSendingMutex) {

                /*
                 * Wait some time for message delivery.
                 *
                 * We are either notified by the mail transport listener when the send has finished(successfully or not)
                 * or we have reached the wait timeout
                 */
                messageSendingMutex.wait(configurator.getMailTimeout());
            }

            messageDeliveryState = transListener.getDeliveryState();

            transport.close();
            transport.removeTransportListener(transListener);
        } catch (MessagingException e) {
            throw new ActionException("Could not send package via SMTP to host '" + mailHost + "' and port " + mailPort, e);
        } catch (InterruptedException e) {
            throw new ActionException("Could not send package", e);
        }

        // evaluate the mail send result
        if (messageDeliveryState == DELIVERY_STATE.DELIVERED) {
            log.info(mimePackage.getDescription() + " " + messageDeliveryState);
        } else {
            throw new ActionException("Result of sending " + mimePackage.getDescription() + ": "
                                      + messageDeliveryState.toString());
        }
    }

    /**
     * Specify a package signer
     *
     * @param signer
     */
    @PublicAtsApi
    public void setSigner( PackageEncryptor signer ) {

        this.signer = signer;
    }

    /**
     * Specify a package encryptor
     *
     * @param encryptor
     */
    @PublicAtsApi
    public void setEncryptor( PackageEncryptor encryptor ) {

        this.encryptor = encryptor;
    }

    /**
     * Set mail session property.<br>
     * SMTP properties reference: https://javamail.java.net/nonav/docs/api/com/sun/mail/smtp/package-summary.html
     *
     * @param propertyKey property key
     * @param propertyValue property value
     */
    @PublicAtsApi
    public void setSessionProperty( Object propertyKey, Object propertyValue ) {

        this.mailProperties.put(propertyKey, propertyValue);
    }

    /**
     * Initialize the SMTP session
     *
     * @throws ActionException
     */
    private void initSession() throws ActionException {

        // initialize the mail session with the current properties
        session = Session.getInstance(mailProperties);
        // user can get more debug info with the session's debug mode
        session.setDebug(configurator.getMailSessionDebugMode());

        // initialize the SMPT transport
        try {
            transport = session.getTransport("smtp");
        } catch (NoSuchProviderException e) {
            throw new ActionException(e);
        }
    }

    /**
     * Sign the package before sending
     *
     * @param mimePackage
     * @return
     * @throws ActionException
     */
    private MimePackage sign(
                              MimePackage mimePackage ) throws ActionException {

        if (signer != null) {
            return (MimePackage) signer.sign(mimePackage);
        } else {
            return mimePackage;
        }
    }

    /**
     * Encrypt the package before sending
     *
     * @param mimePackage
     * @return
     * @throws ActionException
     */
    private MimePackage encrypt(
                                 MimePackage mimePackage ) throws ActionException {

        if (encryptor != null) {
            return (MimePackage) encryptor.encrypt(mimePackage);
        } else {
            return mimePackage;
        }
    }

    private Address[] extractAllRecipients( MimePackage mimePackage ) throws PackageException {

        List<Address> allRecipients = new ArrayList<Address>();
        allRecipients.addAll(Arrays.asList(mimePackage.getRecipientAddresses(RecipientType.TO)));
        allRecipients.addAll(Arrays.asList(mimePackage.getRecipientAddresses(RecipientType.CC)));
        allRecipients.addAll(Arrays.asList(mimePackage.getRecipientAddresses(RecipientType.BCC)));

        return allRecipients.toArray(new Address[allRecipients.size()]);
    }
}
