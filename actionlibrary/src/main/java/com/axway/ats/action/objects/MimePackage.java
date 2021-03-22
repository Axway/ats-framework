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
package com.axway.ats.action.objects;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.mail.smime.SMIMESigned;

import com.axway.ats.action.ActionLibraryConfigurator;
import com.axway.ats.action.model.ActionException;
import com.axway.ats.action.objects.model.MimePartWithoutContentException;
import com.axway.ats.action.objects.model.NoSuchHeaderException;
import com.axway.ats.action.objects.model.NoSuchMimePackageException;
import com.axway.ats.action.objects.model.NoSuchMimePartException;
import com.axway.ats.action.objects.model.ObjectCannotBeTaggedException;
import com.axway.ats.action.objects.model.ObjectNotTaggedException;
import com.axway.ats.action.objects.model.Package;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.action.objects.model.PackageHeader;
import com.axway.ats.action.objects.model.PackagePriority;
import com.axway.ats.action.objects.model.RecipientType;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.io.SeekInputStream;
import com.axway.ats.core.utils.IoUtils;

/**
 * A representation of a MIME message
 */
@PublicAtsApi
public class MimePackage implements Package {

    // the default charset
    private static final String   DEFAULT_CHARSET                    = "us-ascii";

    /**
     * Part types
     */
    public static final String    PART_TYPE_TEXT_HTML                = "html";
    public static final String    PART_TYPE_TEXT_PLAIN               = "plain";

    /**
     * Content types
     */
    public static final String    CONTENT_PART_TYPE_TEXT_HTML        = "text/html";
    public static final String    CONTENT_PART_TYPE_TEXT_PLAIN       = "text/plain";

    public static final String    CONTENT_TYPE_TEXT_PLAIN            = "text/plain";
    public static final String    CONTENT_TYPE_RFC822_HEADERS        = "text/rfc822-headers";
    public static final String    CONTENT_TYPE_MULTIPART_ALTERNATIVE = "multipart/alternative";
    public static final String    CONTENT_TYPE_MULTIPART_PREFIX      = "multipart";

    /**
     * The from header name
     */
    private static final String   FROM_HEADER                        = "FROM";

    /**
     * something really huge - this is passed to MimeMultipart.addBodyPart() the
     * part will be always last
     */
    private static final int      PART_POSITION_LAST                 = 9999;

    /**
     * The logger - it is initialized in the constructor, so that child classes
     * can log in the appropriate logger and not in the MimePackage logger
     */
    private static final Logger   log                                = LogManager.getLogger(MimePackage.class);

    private static final String   CONTENT_TYPE_MULTIPART_SIGNED      = "multipart/signed";                 // TODO move in security package

    /**
     * The MimeMessage instance - this is the JavaMail representation of an
     * email message
     */
    protected MimeMessage         message;

    /**
     * The SMIMESigned instance - this is the BoncyCastle representation of an
     * email (SMIME) signed message
     */
    protected SMIMESigned         smimeSignedMessage;

    /**
     * This array holds a list of MIME parts the way you see them if you open a
     * message with a simple text editor - the parent part is always before its
     * children in the list
     */
    protected ArrayList<MimePart> parts                              = new ArrayList<MimePart>();

    /**
     * Array to hold the indices of regular parts
     */
    protected ArrayList<Integer>  regularPartIndices                 = new ArrayList<Integer>();

    /**
     * Array to hold the indices of attachment parts
     */
    protected ArrayList<Integer>  attachmentPartIndices              = new ArrayList<Integer>();

    /**
     * Array to hold the MIME packages nested into this Mime package
     */
    private List<MimePackage>     nestedMimePackages                 = new ArrayList<MimePackage>();

    /**
     * The tag applied to this package
     */
    private String                tag;

    /**
     * Keep the environment sender separate, because it's only used when sending
     * - it's not part of the message itself
     */
    private String                envelopeSender;

    /**
     * Hold the current subject encoding
     */
    private String                subjectCharset;

    /**
     * Path to this package in the structure of all packages.
     * It is null for the top level package.
     * This is used for debug purposes only
     */
    private String                nestedPath;

    /**
     * Folder of the originating mail (MimePackage and respectively MimeMessage)
     * A reference is kept in case that some nested lazy-parsed content is needed
     * The folder is empty when the message is not loaded from IMAP server, but from a file
     */
    private Folder                partOfImapFolder;

    /**
     * Flag to show that message for interrupted parsing of nested MIME parts is already logged.
     * Such message is logged to indicate that some MIME parts are not parsed because of reached max nested level
     * {@link ActionLibraryConfigurator#getMimePackageMaxNestedLevel()}
     */
    private boolean               skippedParsingMsgIsAlreadyLogged   = false;

    /**
     * Create an empty MIME package - use the add* methods to manipulate it
     *
     * @throws PackageException
     */
    @PublicAtsApi
    public MimePackage() throws PackageException {

        this.message = new MimeMessage(Session.getInstance(new Properties()));
        try {
            this.message.setContent(new MimeMultipart());
        } catch (MessagingException me) {
            throw new PackageException(me);
        }
        this.subjectCharset = null;
    }

    /**
     * Load a MIME package from an existing source
     *
     * @param packageStream
     *            the stream with the package content
     *
     * @throws PackageException
     */
    @PublicAtsApi
    public MimePackage( InputStream packageStream ) throws PackageException {

        try {
            this.message = new MimeMessage(Session.getInstance(new Properties()), packageStream);
            partOfImapFolder = message.getFolder(); // initial best effort. Null for nested or newly created MimeMessages
        } catch (MessagingException me) {
            throw new PackageException(me);
        }

        decompose();
    }

    /**
     * Create a MIME package from an existing message
     *
     * @param message
     * @throws PackageException
     */
    @PublicAtsApi
    public MimePackage( MimeMessage message ) throws PackageException {

        this.message = message;
        partOfImapFolder = message.getFolder(); // initial best effort. Null for nested or newly created MimeMessages

        decompose();
    }

    private MimePackage( String parentNestedPath,
                         int previousSiblings,
                         MimeMessage message,
                         Folder folder ) throws PackageException {

        setNestedPath(parentNestedPath, previousSiblings);
        this.message = message;
        this.partOfImapFolder = folder;

        decompose();
    }

    /**
     * Returns the MIME package we want to work with. This could be the top
     * level package or some nested package.
     *
     * @param packagePath  path to the needed package
     * @return {@link MimePackage} instance of the specific nested mail part
     * @throws NoSuchMimePackageException If mail message is not found matching specific path
     */
    @PublicAtsApi
    public MimePackage getNeededMimePackage(
                                             int[] packagePath ) throws NoSuchMimePackageException {

        return getNeededMimePackage(packagePath, packagePath);
    }

    private MimePackage getNeededMimePackage(
                                              int[] packagePath,
                                              int[] fullPackagePath ) throws NoSuchMimePackageException {

        if (packagePath.length == 0) {
            return this;
        }

        if (packagePath.length == 1) {
            if (packagePath[0] >= nestedMimePackages.size()) {
                throw new NoSuchMimePackageException("No nested MIME package at position '"
                                                     + Arrays.toString(fullPackagePath) + "'");
            }
            return nestedMimePackages.get(packagePath[0]);
        }

        int[] newPackagePath = new int[packagePath.length - 1];
        System.arraycopy(packagePath, 1, newPackagePath, 0, newPackagePath.length);

        return nestedMimePackages.get(packagePath[0]).getNeededMimePackage(newPackagePath,
                                                                           fullPackagePath);
    }

    @SuppressWarnings( "unchecked")
    @PublicAtsApi
    public List<PackageHeader> getAllHeaders() throws PackageException {

        try {
            List<PackageHeader> headers = new ArrayList<PackageHeader>();

            Enumeration<Header> messageHeaders = message.getAllHeaders();
            while (messageHeaders.hasMoreElements()) {
                Header messageHeader = messageHeaders.nextElement();
                headers.add(new PackageHeader(messageHeader.getName(), messageHeader.getValue()));
            }

            return headers;

        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    @PublicAtsApi
    public List<InputStream> getAllStreams() throws PackageException {

        boolean storeReconnected = false;
        try {
            // store should be opened for actions including getting InputStream.
            storeReconnected = reconnectStoreIfClosed();
            ArrayList<InputStream> streams = new ArrayList<InputStream>();

            try {
                for (MimePart part : parts) {
                    streams.add(part.getInputStream());
                }
            } finally {
                closeStoreConnection(storeReconnected);
            }
            return streams;

        } catch (MessagingException me) {
            throw new PackageException("Could not read mime parts", me);
        } catch (IOException ioe) {
            throw new PackageException("Could not read mime parts", ioe);
        }
    }

    @PublicAtsApi
    public InputStream getWholePackage() throws PackageException {

        boolean storeReconnected = false;
        try {
            storeReconnected = reconnectStoreIfClosed();
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            message.writeTo(outStream);

            return new ByteArrayInputStream(outStream.toByteArray());
        } catch (MessagingException me) {
            throw new PackageException("Could not write message content", me);
        } catch (IOException ioe) {
            throw new PackageException("Could not write message content", ioe);
        } finally {
            try {
                closeStoreConnection(storeReconnected);
            } catch (MessagingException ex) {
                log.warn(ex);
            }

        }
    }

    /**
     * Get the first header
     *
     * @param header
     * @return
     * @throws PackageException
     */
    @PublicAtsApi
    public String getHeader(
                             String header ) throws PackageException {

        return getHeader(header, 0);
    }

    /**
     * Get the first header
     *
     * @param header
     * @return
     * @throws PackageException
     */
    @PublicAtsApi
    public String getHeader(
                             String header,
                             int headerIndex ) throws PackageException {

        try {
            String[] headerValues = message.getHeader(header);
            if (headerValues != null && headerValues.length > headerIndex) {
                return headerValues[headerIndex];
            } else {
                throw new NoSuchHeaderException(header, headerIndex);
            }
        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    @PublicAtsApi
    public String[] getHeaderValues(
                                     String header ) throws PackageException {

        try {
            return message.getHeader(header);
        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    /**
     * Tagging the message by adding "Automation-Message-Tag" header with the current time value(in milliseconds).
     * This is useful if want to recognize this message between others in the same IMAP folder.
     *      *
     * @throws ActionException
     */
    @PublicAtsApi
    public void tag() throws ActionException {

        // we use the message's sent time as a unique tag
        String tagValue = Long.toString(Calendar.getInstance().getTimeInMillis());

        try {
            setHeader("Automation-Message-Tag", tagValue);

            // if everything is OK, then this is our tag
            tag = tagValue;

            log.info(getDescription() + " tagged with tag '" + tag + "'");
        } catch (PackageException e) {
            throw new ObjectCannotBeTaggedException(getDescription(), null, e);
        }
    }

    @PublicAtsApi
    public String getTag() throws ActionException {

        if (tag == null) {
            throw new ObjectNotTaggedException(getDescription());
        }

        return tag;
    }

    @PublicAtsApi
    public String getSubject() throws PackageException {

        try {
            return message.getSubject();
        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    @PublicAtsApi
    public String getDescription() {

        String messageSubject;
        try {
            messageSubject = "MIME package with subject '" + message.getSubject() + "'";
        } catch (MessagingException me) {
            messageSubject = "MIME package with no subject";
        }

        return messageSubject;
    }

    /**
     * Get the recipients of the specified type
     *
     * @param recipientType
     *            the type of recipient - to, cc or bcc
     * @return array with recipients, emtpy array of no recipients of this type
     *         are present
     * @throws PackageException
     */
    @PublicAtsApi
    public String[] getRecipients(
                                   RecipientType recipientType ) throws PackageException {

        try {
            Address[] recipientAddresses = message.getRecipients(recipientType.toJavamailType());

            // return an empty string if no recipients are present
            if (recipientAddresses == null) {
                return new String[]{};
            }

            String[] recipients = new String[recipientAddresses.length];
            for (int i = 0; i < recipientAddresses.length; i++) {
                recipients[i] = recipientAddresses[i].toString();
            }

            return recipients;

        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    /**
     * Get all recipients from the specified type
     *
     * @param recipientType
     * @return
     * @throws PackageException
     */
    @PublicAtsApi
    public Address[] getRecipientAddresses(
                                            RecipientType recipientType ) throws PackageException {

        try {
            Address[] allAddresses = message.getRecipients(recipientType.toJavamailType());
            if (allAddresses == null) {
                allAddresses = new Address[0];
            }
            return allAddresses;
        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    /**
     * Set the message subject
     *
     * @param subject
     * @throws PackageException
     */
    @PublicAtsApi
    public void setSubject(
                            String subject ) throws PackageException {

        try {
            message.setSubject(subject);
        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    /**
     * Set the message subject
     *
     * @param subject
     * @param charset
     * @throws PackageException
     */
    @PublicAtsApi
    public void setSubject(
                            String subject,
                            String charset ) throws PackageException {

        try {
            message.setSubject(subject, charset);
            subjectCharset = charset;
        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    /**
     * Get the subject charser
     *
     * @return the subject charser, null if not specifically set
     */
    @PublicAtsApi
    public String getSubjectCharset() {

        return subjectCharset;
    }

    /**
     * Set the sender (From header) for the package
     *
     * @param sender
     *            the sender email address
     * @throws PackageException
     */
    @PublicAtsApi
    public void setSender(
                           String sender ) throws PackageException {

        String newSenderAddress;
        String newSenderPersonal;
        try {
            InternetAddress address = new InternetAddress();

            sender = sender.replaceAll("[<>]", "").trim();

            boolean hasPersonal = sender.contains(" ");
            if (hasPersonal) {
                newSenderAddress = sender.substring(sender.lastIndexOf(' '));
                newSenderPersonal = sender.substring(0, sender.lastIndexOf(' '));
                address.setPersonal(newSenderPersonal.trim());
            } else {
                newSenderAddress = sender;
            }

            // set the sender address
            address.setAddress(newSenderAddress.trim());

            message.setFrom(address);

        } catch (ArrayIndexOutOfBoundsException aioobe) {
            throw new PackageException("Sender not present");
        } catch (MessagingException me) {
            throw new PackageException(me);
        } catch (UnsupportedEncodingException uee) {
            throw new PackageException("Error setting address personal", uee);
        }
    }

    /**
     * Set the sender display name on the From header
     *
     * @param name
     *            the display name to set
     * @throws PackageException
     */
    @PublicAtsApi
    public void setSenderName(
                               String name ) throws PackageException {

        try {
            InternetAddress address = new InternetAddress();

            String[] fromHeaders = getHeaderValues(FROM_HEADER);
            if (fromHeaders != null && fromHeaders.length > 0) {

                // parse the from header if such exists
                String fromHeader = fromHeaders[0];
                if (fromHeader != null) {
                    address = InternetAddress.parse(fromHeader)[0];
                }
            }

            address.setPersonal(name);
            message.setFrom(address);

        } catch (ArrayIndexOutOfBoundsException aioobe) {
            throw new PackageException("Sender not present");
        } catch (MessagingException me) {
            throw new PackageException(me);
        } catch (UnsupportedEncodingException uee) {
            throw new PackageException(uee);
        }
    }

    /**
     * This method returns the full sender address contained in the first From
     * header
     *
     * @return the sender address
     * @throws PackageException
     */
    @PublicAtsApi
    public String getSender() throws PackageException {

        try {
            String[] fromAddresses = message.getHeader("From");
            if (fromAddresses == null || fromAddresses.length == 0) {
                throw new PackageException("Sender not present");
            }

            return fromAddresses[0];
        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    /**
     * This method resturns only the email address portion of the sender
     * contained in the first From header
     *
     * @return the sender address
     * @throws PackageException
     */
    @PublicAtsApi
    public String getSenderAddress() throws PackageException {

        try {
            Address[] fromAddresses = message.getFrom();
            if (fromAddresses == null || fromAddresses.length == 0) {
                throw new PackageException("Sender not present");
            }

            InternetAddress fromAddress = (InternetAddress) fromAddresses[0];
            return fromAddress.getAddress();

        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    /**
     * Get the envelope sender if set
     *
     * @return the envelope sender
     */
    @PublicAtsApi
    public String getEnvelopeSender() {

        return envelopeSender;
    }

    /**
     * Set the envelope sender
     *
     * @param envelopeSender
     *            the email address for the envelope sender
     */
    @PublicAtsApi
    public void setEnvelopeSender(
                                   String envelopeSender ) {

        this.envelopeSender = envelopeSender;
    }

    /**
     * Get the recipient count for a specific recipient type
     *
     * @param type
     * @return
     * @throws PackageException
     */
    @PublicAtsApi
    public int getRecipientCount(
                                  RecipientType type ) throws PackageException {

        try {
            return message.getRecipients(type.toJavamailType()).length;
        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    /**
     * Set the To recipient of a mime package, the CC and BCC recipients are
     * cleared
     *
     * @param address the email address of the recipient
     * @throws PackageException
     */
    @PublicAtsApi
    public void setRecipient(
                              String address ) throws PackageException {

        try {
            // add the recipient
            InternetAddress inetAddress = new InternetAddress(address);
            message.setRecipients(javax.mail.internet.MimeMessage.RecipientType.TO,
                                  new InternetAddress[]{ inetAddress });
            message.setRecipients(javax.mail.internet.MimeMessage.RecipientType.CC,
                                  new InternetAddress[]{});
            message.setRecipients(javax.mail.internet.MimeMessage.RecipientType.BCC,
                                  new InternetAddress[]{});
        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    /**
     * Set the specified type of recipients of a mime package
     *
     * @param type the recipients' type
     * @param address the email addresses of the recipients
     * @throws PackageException
     */
    @PublicAtsApi
    public void setRecipient(
                              RecipientType type,
                              String[] addresses ) throws PackageException {

        try {
            // add the recipient
            InternetAddress[] address = new InternetAddress[addresses.length];
            for (int i = 0; i < addresses.length; i++)
                address[i] = new InternetAddress(addresses[i]);
            message.setRecipients(type.toJavamailType(), address);
        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    /**
     * Add recipients of a specified type
     *
     * @param type the recipients' type
     * @param addresses the email addresses of the recipients
     * @throws PackageException
     */
    @PublicAtsApi
    public void addRecipient(
                              RecipientType type,
                              String[] addresses ) throws PackageException {

        try {
            // add the recipient
            InternetAddress[] address = new InternetAddress[addresses.length];
            for (int i = 0; i < addresses.length; i++)
                address[i] = new InternetAddress(addresses[i]);
            message.addRecipients(type.toJavamailType(), address);
        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    /**
     * @param headerName
     * @param partNum
     * @return
     * @throws PackageException
     */
    @PublicAtsApi
    public String getPartHeader(
                                 String headerName,
                                 int partNum ) throws PackageException {

        return getPartHeader(headerName, partNum, 0);
    }

    /**
     * Get the specified header value from a specified MIME part
     *
     * @param headerName
     * @param partNum
     * @param headerIndex
     * @return
     * @throws PackageException
     */
    @PublicAtsApi
    public String getPartHeader(
                                 String headerName,
                                 int partNum,
                                 int headerIndex ) throws PackageException {

        try {
            String[] headers;
            if (partNum >= parts.size()) {
                throw new NoSuchMimePartException("No MIME part at position '" + partNum + "'");
            }

            MimePart part = parts.get(partNum);
            headers = part.getHeader(headerName);

            if ( (headers != null) && (headers.length > headerIndex)) {
                return headers[headerIndex];
            } else {
                throw new NoSuchHeaderException(headerName, partNum, headerIndex);
            }
        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    /**
     * Get all header values from a specified MIME part
     *
     * @param headerName
     * @param partNum
     * @return
     * @throws PackageException
     */
    @PublicAtsApi
    public String[] getPartHeaderValues(
                                         String headerName,
                                         int partNum ) throws PackageException {

        try {
            String[] headers;
            if (partNum >= parts.size()) {
                throw new NoSuchMimePartException("No MIME part at position '" + partNum + "'");
            }

            MimePart part = parts.get(partNum);
            headers = part.getHeader(headerName);

            if ( (headers != null) && (headers.length > 0)) {
                return headers;
            } else {
                throw new NoSuchHeaderException(headerName, partNum);
            }
        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    /**
     * Set a header of the message
     *
     * @param headerName
     *            name of the header
     * @param headerValue
     *            header value
     * @throws PackageException
     */
    @PublicAtsApi
    public void setHeader(
                           String headerName,
                           String headerValue ) throws PackageException {

        try {
            message.setHeader(headerName, headerValue);
        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    /**
     * Add a custom header to the message
     *
     * @param headerName
     *            name of the header
     * @param headerValue
     *            header value
     * @throws PackageException
     */
    @PublicAtsApi
    public void addHeader(
                           String headerName,
                           String headerValue ) throws PackageException {

        try {
            message.addHeader(headerName, headerValue);
        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    /**
     * Set the message priority
     *
     * @param priority
     * @throws PackageException
     */
    @PublicAtsApi
    public void setPriority(
                             PackagePriority priority ) throws PackageException {

        try {
            // set the priority
            message.setHeader("X-Priority", String.valueOf(priority.toInt()));

            // set MS Outlook display-priority header
            message.setHeader("Importance", priority.toString());

        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    /**
     * Add a part with the specified text to the specified position using the default "us-ascii" encoding
     *
     * @param content the part's content
     * @param contentTypeSubtype the part's subtype of content type like plain (subtype of text/plain) or html (subtype of text/html)
     * @throws PackageException
     */
    @PublicAtsApi
    public void addPart(
                         String content,
                         String contentType ) throws PackageException {

        addPart(content, contentType, DEFAULT_CHARSET);
    }

    /**
     * Add a part with the specified text to the specified position
     *
     * @param content the part's content
     * @param contentTypeSubtype the part's subtype of content type like plain (sub type of text/plain) or html (sub type of text/html)
     * @param charset the part's charset
     * @throws PackageException
     */
    @PublicAtsApi
    public void addPart(
                         String content,
                         String contentTypeSubtype,
                         String charset ) throws PackageException {

        // create a new inline part
        MimeBodyPart part = new MimeBodyPart();

        try {
            part.setText(content, charset, contentTypeSubtype);
            part.setDisposition(MimeBodyPart.INLINE);
        } catch (MessagingException me) {
            throw new PackageException(me);
        }

        addPart(part, PART_POSITION_LAST);
    }

    /**
     * Add a multipart/alternative part to message body
     *
     * @param plainContent
     *            the content of the text/plain sub-part
     * @param htmlContent
     *            the content of the text/html sub-part
     * @throws PackageException
     */
    @PublicAtsApi
    public void addAlternativePart(
                                    String plainContent,
                                    String htmlContent ) throws PackageException {

        addAlternativePart(plainContent, htmlContent, DEFAULT_CHARSET);
    }

    /**
     * Add a multipart/alternative part to message body
     *
     * @param plainContent
     *            the content of the text/plain sub-part
     * @param htmlContent
     *            the content of the text/html sub-part
     * @param charset
     *            the character set for the part
     * @throws PackageException
     */
    @PublicAtsApi
    public void addAlternativePart(
                                    String plainContent,
                                    String htmlContent,
                                    String charset ) throws PackageException {

        MimeMultipart alternativePart = new MimeMultipart("alternative");

        try {
            // create a new text/plain part
            MimeBodyPart plainPart = new MimeBodyPart();
            plainPart.setText(plainContent, charset, PART_TYPE_TEXT_PLAIN);
            plainPart.setDisposition(MimeBodyPart.INLINE);

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setText(htmlContent, charset, PART_TYPE_TEXT_HTML);
            htmlPart.setDisposition(MimeBodyPart.INLINE);

            alternativePart.addBodyPart(plainPart, 0);
            alternativePart.addBodyPart(htmlPart, 1);

            MimeBodyPart mimePart = new MimeBodyPart();
            mimePart.setContent(alternativePart);

            addPart(mimePart, PART_POSITION_LAST);
        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    /**
     * adds a given body part of a specified type from a specified file to the
     * specified position all line endings are set to "\r\n"
     *
     * @param fileName
     *            name of the file to get the data from
     * @param contentType
     *            the content type of the part
     * @throws PackageException
     *             on error
     */
    @PublicAtsApi
    public void addPartFromFile(
                                 String fileName,
                                 String contentType ) throws PackageException {

        // the normalized buffer - Windows like line ending
        StringBuffer normalizedBuff = new StringBuffer();

        try (BufferedReader buffReader = new BufferedReader(new java.io.FileReader(fileName))) {
            String currLine;

            do {
                currLine = buffReader.readLine();
                if (currLine != null) {
                    normalizedBuff.append(currLine);
                    normalizedBuff.append("\r\n");
                }
            } while (currLine != null);

            // add the new body part
            addPart(normalizedBuff.toString(), contentType);
        } catch (IOException ioe) {
            throw new PackageException(ioe);
        }
    }

    /**
     * Add the specified file as an attachment
     *
     * @param fileName
     *            name of the file
     * @throws PackageException
     *             on error
     */
    @PublicAtsApi
    public void addAttachment(
                               String fileName ) throws PackageException {

        try {
            // add attachment to multipart content
            MimeBodyPart attPart = new MimeBodyPart();
            FileDataSource ds = new FileDataSource(fileName);
            attPart.setDataHandler(new DataHandler(ds));
            attPart.setDisposition(MimeBodyPart.ATTACHMENT);
            attPart.setFileName(ds.getName());

            addPart(attPart, PART_POSITION_LAST);
        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    /**
     * Add an attachment with the specified content - the attachment will have a
     * content type text\plain
     *
     * @param content
     *            the content of the attachment
     * @param fileName
     *            the file name for the content-disposition header
     * @throws PackageException
     *             on error
     */
    @PublicAtsApi
    public void addAttachment(
                               String content,
                               String fileName ) throws PackageException {

        addAttachment(content, DEFAULT_CHARSET, fileName);
    }

    /**
     * Add an attachment with the specified content - the attachment will have a
     * content type text\plain and the specified character set
     *
     * @param content
     *            the content of the attachment
     * @param charset
     *            the character set
     * @param fileName
     *            the file name for the content-disposition header
     * @throws PackageException
     *             on error
     */
    @PublicAtsApi
    public void addAttachment(
                               String content,
                               String charset,
                               String fileName ) throws PackageException {

        try {
            // add attachment to multipart content
            MimeBodyPart attPart = new MimeBodyPart();
            attPart.setText(content, charset, PART_TYPE_TEXT_PLAIN);
            attPart.setDisposition(MimeBodyPart.ATTACHMENT);
            attPart.setFileName(fileName);

            addPart(attPart, PART_POSITION_LAST);
        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    /**
     * Appends all files in a specified folder as attachments
     *
     * @param folder the folder containing all files to be attached
     * @throws PackageException
     */
    @PublicAtsApi
    public void addAttachmentDir(
                                  String folder ) throws PackageException {

        // fetch list of files in specified directory
        File dir = new File(folder);
        File[] list = dir.listFiles();
        if (null == list) {
            throw new PackageException("Could not read from directory '" + folder + "'.");
        } else {
            // process all files, skipping directories
            for (int i = 0; i < list.length; i++) {
                if ( (null != list[i]) && (!list[i].isDirectory())) {
                    // add attachment to multipart content
                    MimeBodyPart attPart = new MimeBodyPart();
                    FileDataSource ds = new FileDataSource(list[i].getPath());

                    try {
                        attPart.setDataHandler(new DataHandler(ds));
                        attPart.setDisposition(MimeBodyPart.ATTACHMENT);
                        attPart.setFileName(ds.getName());
                    } catch (MessagingException me) {
                        throw new PackageException(me);
                    }

                    addPart(attPart, PART_POSITION_LAST);
                }
            }
        }
    }

    /**
     * Get the content of the first text (&quot;text/plain&quot; content type) part.
     * <br>If this is a multipart message, we search only into the first nested level.
     *
     * @return
     * @throws PackageException
     */
    @PublicAtsApi
    public String getPlainTextBody() throws PackageException {

        return getFirstBody(CONTENT_PART_TYPE_TEXT_PLAIN);
    }

    /**
     * Get the content of the first HTML (&quot;text/html&quot; content type) part.
     * <br>If this is a multipart message, we search only into the first nested level.
     *
     * @throws PackageException
     */
    @PublicAtsApi
    public String getHtmlTextBody() throws PackageException {

        return getFirstBody(CONTENT_PART_TYPE_TEXT_HTML);
    }

    /**
     * Get a MIME part based on it's index and type
     *
     * @param partIndex
     *            the index of the MIME part
     * @param isAttachment
     *            the type - true if the part is attachment, false otherwise
     * @return the mime part
     * @throws NoSuchMimePartException
     *             if there is no such part
     */
    @PublicAtsApi
    public MimePart getPart(
                             int partIndex,
                             boolean isAttachment ) throws NoSuchMimePartException {

        // first check if there is part at this position at all
        if (isAttachment) {
            if (partIndex >= attachmentPartIndices.size()) {
                throw new NoSuchMimePartException("No attachment at position '" + partIndex + "'");
            }
        } else {
            if (partIndex >= regularPartIndices.size()) {
                throw new NoSuchMimePartException("No regular part at position '" + partIndex + "'");
            }
        }

        MimePart part;
        if (isAttachment) {
            part = getPart(attachmentPartIndices.get(partIndex));
        } else {
            part = getPart(regularPartIndices.get(partIndex));
        }

        return part;
    }

    /**
     * Get a stream representing the data of the selected part
     *
     * @param partIndex
     *            the index of the part
     * @param isAttachment
     *            is the part an attachment
     * @return the data stream
     * @throws PackageException
     */
    @PublicAtsApi
    public InputStream getPartData(
                                    int partIndex,
                                    boolean isAttachment ) throws PackageException {

        if (!isAttachment) {
            return getRegularPartData(partIndex);
        } else {
            return getAttachmentPartData(partIndex);
        }
    }

    /**
     * Get the content type of a regular part
     *
     * @param partIndex
     *            the index of the regular part
     * @return the content type as string
     * @throws PackageException
     */
    @PublicAtsApi
    public String getRegularPartContentType(
                                             int partIndex ) throws PackageException {

        // first check if there is part at this position at all
        if (partIndex >= regularPartIndices.size()) {
            throw new NoSuchMimePartException("No regular part at position '" + partIndex + "'");
        }

        try {
            MimePart part = getPart(regularPartIndices.get(partIndex));

            // get the content type header
            ContentType contentType = new ContentType(part.getContentType());
            return contentType.getBaseType();
        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    /**
     * Get the character set of a regular part
     *
     * @param partIndex
     *            the index of the part
     * @return the charset
     * @throws PackageException
     */
    @PublicAtsApi
    public String getRegularPartCharset(
                                         int partIndex ) throws PackageException {

        // first check if there is part at this position at all
        if (partIndex >= regularPartIndices.size()) {
            throw new NoSuchMimePartException("No regular part at position '" + partIndex + "'");
        }

        try {
            MimePart part = getPart(regularPartIndices.get(partIndex));

            // get the content type header
            ContentType contentType = new ContentType(part.getContentType());
            return contentType.getParameter("charset");
        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    /**
     * Get the decoded data of a specified part
     *
     * @param partIndex
     * @return
     * @throws PackageException
     */
    @PublicAtsApi
    public InputStream getRegularPartData(
                                           int partIndex ) throws PackageException {

        boolean storeReconnected = false;
        try {
            // store should be opened for actions including getting InputStream.
            // Hence store open is not in getPart
            storeReconnected = reconnectStoreIfClosed();
            MimePart part = getPart(partIndex, false);
            return part.getInputStream();
        } catch (MessagingException e) {
            throw new PackageException(e);
        } catch (IOException ioe) {
            throw new PackageException(ioe);
        } finally {
            try {
                closeStoreConnection(storeReconnected);
            } catch (MessagingException e) {
                log.error(e);
            }
        }
    }

    /**
     * Set/modify attachment file name
     *
     * @param attachmentPartIndex index among attachment parts only
     * @param fileName the file name. Add one or reset existing one
     * @throws NoSuchMimePartException if not such attachment part is found
     * @throws PackageException in case of other mail messaging exception
     */
    @PublicAtsApi
    public void setAttachmentFileName(
                                       int attachmentPartIndex,
                                       String fileName ) throws PackageException {

        // first check if there is part at this position at all
        if (attachmentPartIndex >= attachmentPartIndices.size()) {
            throw new NoSuchMimePartException("No attachment at position '" + attachmentPartIndex + "'");
        }

        try {
            MimePart part = getPart(attachmentPartIndices.get(attachmentPartIndex));

            // set the attachment file name
            part.setFileName(fileName);
            // must save now
            this.message.saveChanges();
        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    /**
     * Get an attachment's file name
     *
     * @param partIndex
     * @return
     * @throws PackageException
     */
    @PublicAtsApi
    public String getAttachmentFileName(
                                         int partIndex ) throws PackageException {

        // first check if there is part at this position at all
        if (partIndex >= attachmentPartIndices.size()) {
            throw new NoSuchMimePartException("No attachment at position '" + partIndex + "'");
        }

        try {
            MimePart part = getPart(attachmentPartIndices.get(partIndex));

            // get the attachment file name
            String fileName = part.getFileName();
            if (fileName == null) {
                throw new PackageException("Could not determine file name for attachment at position "
                                           + partIndex);
            }

            return fileName;

        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    /**
     * Get the attachment content type
     *
     * @param partIndex
     * @return
     * @throws PackageException
     */
    @PublicAtsApi
    public String getAttachmentContentType(
                                            int partIndex ) throws PackageException {

        // first check if there is part at this position at all
        if (partIndex >= attachmentPartIndices.size()) {
            throw new NoSuchMimePartException("No attachment at position '" + partIndex + "'");
        }

        try {
            MimePart part = getPart(attachmentPartIndices.get(partIndex));

            // get the content type header
            ContentType contentType = new ContentType(part.getContentType());
            return contentType.getBaseType();
        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    /**
     * Get the attachment character set
     *
     * @param partIndex
     *            the index of the attachment
     * @return the character set for this attachment, null if there is no such
     * @throws PackageException
     */
    @PublicAtsApi
    public String getAttachmentCharset(
                                        int partIndex ) throws PackageException {

        // first check if there is part at this position at all
        if (partIndex >= attachmentPartIndices.size()) {
            throw new NoSuchMimePartException("No attachment at position '" + partIndex + "'");
        }

        try {
            MimePart part = getPart(attachmentPartIndices.get(partIndex));

            // get the content type header
            ContentType contentType = new ContentType(part.getContentType());
            return contentType.getParameter("charset");
        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    /**
     * Return the attachment data
     *
     * @param partIndex
     *            the index of the attachment
     * @return an InputStream with the attachment data
     * @throws PackageException
     */
    @PublicAtsApi
    public InputStream getAttachmentPartData(
                                              int partIndex ) throws PackageException {

        try {
            boolean storeReconnected = reconnectStoreIfClosed();
            // store should be opened for actions including getting InputStream. Hence store open is not in getPart
            try {
                MimePart part = getPart(partIndex, true);
                return part.getInputStream();
            } finally {
                closeStoreConnection(storeReconnected);
            }
        } catch (MessagingException me) {
            throw new PackageException("Error getting attachment data for part " + partIndex, me);
        } catch (IOException ioe) {
            throw new PackageException("Error getting attachment data for part " + partIndex, ioe);
        }
    }

    @PublicAtsApi
    public MimePart getAttachmentPart(
                                       int partIndex ) throws PackageException {

        return getPart(partIndex, true);
    }

    /**
     * @return the number of attachments
     */
    @PublicAtsApi
    public int getAttachmentPartCount() {

        return attachmentPartIndices.size();
    }

    /**
     * @return the number of regular parts
     */
    @PublicAtsApi
    public int getRegularPartCount() {

        return regularPartIndices.size();
    }

    /**
     * Return the CRC checksum of a given part
     *
     * @param partIndex
     *            the index of the part
     * @param isAttachment
     *            true if the part is an attachment
     * @return the part checksum
     * @throws PackageException
     */
    @PublicAtsApi
    public long getPartChecksum(
                                 int partIndex,
                                 boolean isAttachment ) throws PackageException {

        InputStream partDataStream = getPartData(partIndex, isAttachment);

        if (partDataStream != null) {
            try {
                SeekInputStream seekDataStream = new SeekInputStream(partDataStream);
                seekDataStream.seek(0);

                // create a new crc and reset it
                CRC32 crc = new CRC32();

                // use checked stream to get the checksum
                CheckedInputStream stream = new CheckedInputStream(seekDataStream, crc);

                int bufLen = 4096;
                byte[] buffer = new byte[bufLen];
                int numBytesRead = bufLen;

                while (numBytesRead == bufLen) {
                    numBytesRead = stream.read(buffer, 0, bufLen);
                }

                long checksum = stream.getChecksum().getValue();
                stream.close();

                return checksum;
            } catch (IOException ioe) {
                throw new PackageException(ioe);
            }
        } else {
            throw new MimePartWithoutContentException("MIME part does not have any content");
        }
    }

    /**
     * Get the internal MIME message
     *
     * @return the MimeMessage instance
     */
    @PublicAtsApi
    public MimeMessage getMimeMessage() {

        return message;
    }

    /**
     * Get the internal SMIMESigned message
     *
     * @return the SMIMESigned message instance
     * @throws PackageException
     */
    public SMIMESigned getSMIMESignedMessage() throws PackageException {

        if (smimeSignedMessage == null && message != null) {
            // smimeSignedMessage is not 'null' if the message is signed and already decrypted
            final String notASignedMessage = "The Content-Type is '"
                                             + CONTENT_TYPE_MULTIPART_SIGNED
                                             + "' but could not create SMIMESigned message";
            try {
                if (message.isMimeType(CONTENT_TYPE_MULTIPART_SIGNED)) {

                    smimeSignedMessage = new SMIMESigned((MimeMultipart) message.getContent());
                }
            } catch (MessagingException me) {
                throw new PackageException("Could not get message details", me);
            } catch (CMSException e) {
                throw new PackageException(notASignedMessage, e);
            } catch (IOException e) {
                throw new PackageException(notASignedMessage, e);
            }
        }

        return smimeSignedMessage;
    }

    /**
     *
     * @param smimeSignedMessage the SMIMESigned message object
     */
    public void setSMIMESignedMessage(
                                       SMIMESigned smimeSignedMessage ) {

        this.smimeSignedMessage = smimeSignedMessage;
    }

    /**
     * Return a list of all leaf parts (e.g. not including composite MIME parts)
     *
     * @return a list of all leaf parts in the MIME tree
     */
    @PublicAtsApi
    public List<MimePart> getMimeParts() {

        return parts;
    }

    /**
     * Write the constructed MIME message to a file
     *
     * @param fileName
     *            the name of the file to write to
     *
     * @throws IOException
     * @throws PackageException
     */
    @PublicAtsApi
    public void writeToFile(
                             String fileName ) throws IOException, PackageException {

        FileOutputStream outStream = null;
        boolean storeReconnected = false;
        try {
            storeReconnected = reconnectStoreIfClosed();
            // store should be opened for actions including getting InputStream. Hence store open is not in getPart
            outStream = new FileOutputStream(new File(fileName));
            message.writeTo(outStream);
        } catch (MessagingException me) {
            throw new PackageException("Could not write message content", me);
        } finally {
            if (outStream != null) {
                outStream.close();
            }
            try {
                closeStoreConnection(storeReconnected);
            } catch (MessagingException ex) {
                log.warn("Error closing IMAP connection", ex);
            }
        }
    }

    /**
     * Get the content of the first part with the specified content type
     * <br>If this is a multipart message, we search only into the first nested level.
     *
     * @return the contents (body) of the first part with given content type.
     *  Null if such is not found in current or the direct sub-level
     * @throws PackageException
     */
    @PublicAtsApi
    private String getFirstBody(
                                 String contentType ) throws PackageException {

        String textBody = null;
        boolean storeReconnected;
        try {
            String messageContentType = message.getContentType().toLowerCase();
            String contentDisposition = null;
            storeReconnected = reconnectStoreIfClosed();
            try {
                if (messageContentType.startsWith(contentType)) {
                    contentDisposition = message.getDisposition();
                    if (!Part.ATTACHMENT.equalsIgnoreCase(contentDisposition)) {
                        // this is a plain text message
                        textBody = message.getContent().toString();
                    }
                } else {
                    Object content = message.getContent();
                    if (content instanceof Multipart) {
                        // a multi-part message
                        Multipart parts = (Multipart) message.getContent();
                        // first look on top level
                        for (int i = 0; i < parts.getCount(); i++) {
                            BodyPart mimePart = parts.getBodyPart(i);
                            if (! (mimePart.getContent() instanceof Multipart)) {
                                textBody = getBodyIfNotAttachment(mimePart, contentType);
                                if (textBody != null) {
                                    break;
                                }
                            }
                        }
                        if (textBody == null) {
                            // not found on top level - look multipart entries
                            for (int i = 0; i < parts.getCount(); i++) {
                                BodyPart mimePart = parts.getBodyPart(i);
                                if (mimePart.getContent() instanceof Multipart) {
                                    Multipart nestedParts = (Multipart) mimePart.getContent();
                                    for (int m = 0; m < nestedParts.getCount(); m++) {
                                        BodyPart nestedMimePart = nestedParts.getBodyPart(m);
                                        textBody = getBodyIfNotAttachment(nestedMimePart, contentType);
                                        if (textBody != null) {
                                            break;
                                        }
                                    }
                                }
                                if (textBody != null) {
                                    break;
                                }
                            }
                        }
                    }
                }
            } finally {
                closeStoreConnection(storeReconnected);
            }

            return textBody;
        } catch (MessagingException e) {
            throw new PackageException(e);
        } catch (IOException e) {
            throw new PackageException(e);
        }
    }

    /**
     * Check if the body is from the content type and returns it if not attachment
     *
     * @param mimePart
     * @param contentType
     * @return null if not with specific content type or part is attachment
     */
    private String getBodyIfNotAttachment(
                                           BodyPart mimePart,
                                           String contentType ) throws MessagingException, IOException {

        String mimePartContentType = mimePart.getContentType().toLowerCase();
        if (mimePartContentType.startsWith(contentType)) { // found a part with given mime type
            String contentDisposition = mimePart.getDisposition();
            if (!Part.ATTACHMENT.equalsIgnoreCase(contentDisposition)) {
                Object partContent = mimePart.getContent();
                if (partContent instanceof InputStream) {
                    return IoUtils.streamToString((InputStream) partContent);
                } else {
                    return partContent.toString();
                }
            }
        }
        return null;
    }

    /**
     * Sets the nested path of this package
     *
     * @param parentNestedPath the nested path of the parent
     * @param siblingsIndex how many siblings are before this package at this nested level
     */
    private void setNestedPath(
                                String parentNestedPath,
                                int siblingsIndex ) {

        if (parentNestedPath == null) {
            this.nestedPath = String.valueOf(siblingsIndex);
        } else {
            this.nestedPath = parentNestedPath + "," + String.valueOf(siblingsIndex);
        }
    }

    /**
     * Check if this package is nested too many levels into the top level package
     *
     * @return
     */
    private boolean exceedsMaxNestedLevel() {

        boolean exceedsMaxNestedLevel = false;
        if (nestedPath != null) {
            // it is a nested package
            int maxNestedLevel = ActionLibraryConfigurator.getInstance().getMimePackageMaxNestedLevel();
            if (nestedPath.split(",").length >= maxNestedLevel) {
                exceedsMaxNestedLevel = true;
            }
        }

        return exceedsMaxNestedLevel;
    }

    protected void decompose() throws PackageException {

        parts.clear();
        regularPartIndices.clear();
        attachmentPartIndices.clear();
        nestedMimePackages.clear();

        parseContent(this.message);

        if (log.isDebugEnabled() && nestedPath == null) {
            log.debug("Loaded MIME package with content:\n" + toStringTrace("", ""));
        }
    }

    private void parseContent(
                               MimePart part ) throws PackageException {

        parseContent(part, true);
    }

    private void parseContent(
                               MimePart part,
                               boolean doNotParseBrokenParts ) throws PackageException {

        if (exceedsMaxNestedLevel()) {
            if (log.isInfoEnabled() && !skippedParsingMsgIsAlreadyLogged) {
                log.info("Skipping parsing of nested message parts from current MimePackage because max nested level is reached."
                         + " Current max nesting level is "
                         + ActionLibraryConfigurator.getInstance().getMimePackageMaxNestedLevel());
                skippedParsingMsgIsAlreadyLogged = true;
            }
            return;
        }

        try {
            Object content = part.getContent();
            if (content instanceof Multipart) {
                // if multipart recurse through all child parts
                MimeMultipart mimeMultipart = (MimeMultipart) content;
                int partCount = mimeMultipart.getCount();
                for (int i = 0; i < partCount; i++) {
                    try {
                        parseContent((MimeBodyPart) mimeMultipart.getBodyPart(i));
                    } catch (PackageException pe) {
                        if (doNotParseBrokenParts) {
                            log.warn("Could not parse part: "
                                     + mimeMultipart.getBodyPart(i).getContentType());
                        } else {
                            log.error("Could not parse part: "
                                      + mimeMultipart.getBodyPart(i).getContentType());
                            throw new PackageException(pe);
                        }
                    }
                }

            } else if (content instanceof MimeMessage) {
                MimeMessage mimeMessage = (MimeMessage) content;
                nestedMimePackages.add(new MimePackage(this.nestedPath,
                                                       nestedMimePackages.size(),
                                                       mimeMessage,
                                                       partOfImapFolder));

                // if the nested message has been added as attachment, we need
                // to treat it as such - it will not be decomposed
                if (isPartAttachment(part)) {
                    parts.add(part);
                    attachmentPartIndices.add(parts.size() - 1);
                } else {
                    try {
                        parseContent(mimeMessage);
                    } catch (PackageException pe) {
                        if (doNotParseBrokenParts) {
                            log.warn("Could not parse part: " + mimeMessage.getContentID());
                        } else {
                            log.error("Could not parse part: " + mimeMessage.getContentID());
                            throw pe;
                        }
                    }
                }

            } else {

                InternetHeaders internetHeaders = null;
                if (part.getContentType().toLowerCase().startsWith(CONTENT_TYPE_RFC822_HEADERS)) {
                    try {
                        // check for "text/rfc822-headers"
                        internetHeaders = getInternetHeaders(content);
                    } catch (PackageException e) {
                        throw new PackageException("Content type " + CONTENT_TYPE_RFC822_HEADERS
                                                   + " is found but headers are not parsed successfully.",
                                                   e);
                    }
                    if (internetHeaders == null) { // javax.mail implementation is not very strict
                        log.error("Mail part with content type " + CONTENT_TYPE_RFC822_HEADERS
                                  + " is found but could not be parsed. Contents: " + content);
                    }
                }
                if (internetHeaders != null) {
                    // the "Content-Type" is "text/rfc822-headers" and javamail returns it as InternetHeaders.
                    // this is a message with headers only, we will keep it as a
                    // nested MimePackage
                    MimePackage nestedMimePackage = new MimePackage();
                    nestedMimePackage.setNestedPath(this.nestedPath, nestedMimePackages.size());

                    Enumeration<?> enumerator = internetHeaders.getAllHeaders();
                    while (enumerator.hasMoreElements()) {
                        Header inetHeader = (Header) enumerator.nextElement();
                        nestedMimePackage.addHeader(inetHeader.getName(), inetHeader.getValue());
                    }
                    nestedMimePackages.add(nestedMimePackage);
                } else {
                    // add the body
                    parts.add(part);
                    if (isPartAttachment(part)) {
                        attachmentPartIndices.add(parts.size() - 1);
                    } else {
                        regularPartIndices.add(parts.size() - 1);
                    }
                }
            }
        } catch (MessagingException me) {
            throw new PackageException("Could not parse MIME part", me);
        } catch (IOException ioe) {
            throw new PackageException("Could not parse MIME message", ioe);
        }
    }

    private boolean isPartAttachment(
                                      MimePart part ) throws PackageException {

        try {
            String disposition = part.getDisposition();
            if (disposition != null && disposition.equalsIgnoreCase(Part.ATTACHMENT)) {
                return true;
            }
        } catch (MessagingException me) {
            throw new PackageException("Could not determine if part is an attachment", me);
        }

        return false;
    }

    private InternetHeaders getInternetHeaders(
                                                Object partContent ) throws PackageException {

        InternetHeaders internetHeaders = null;
        if (partContent instanceof InputStream) {
            try {
                InputStream is = (InputStream) partContent;
                internetHeaders = new InternetHeaders(is);
            } catch (MessagingException e) {
                // error converting to InternetHeaders
                throw new PackageException("Error parsing internet headers with type rfc822-headers", e);
            }
        }
        return internetHeaders;
    }

    private void addPart(
                          BodyPart part,
                          int position ) throws NoSuchMimePartException, PackageException {

        try {
            Object messageContent = message.getContent();
            if (messageContent instanceof MimeMultipart) {
                MimeMultipart multipartContent = (MimeMultipart) messageContent;

                int positionToInsertAt = position;
                if (position > multipartContent.getCount()) {
                    positionToInsertAt = multipartContent.getCount();
                }
                multipartContent.addBodyPart(part, positionToInsertAt);

                // set back the modified content
                message.setContent(multipartContent);

                // make sure all changes to the message are saved before
                // decomposing
                try {
                    message.saveChanges();
                } catch (MessagingException me) {
                    throw new PackageException("Could not save message changes", me);
                }

                // we need to decompose again, as a new part has been added
                decompose();
            } else {
                // TODO: we can transform the part to MimeMultipart if desired
                throw new PackageException("Message is not multipart!");
            }

        } catch (MessagingException me) {
            throw new PackageException(me);
        } catch (IOException ioe) {
            throw new PackageException(ioe);
        }
    }

    private MimePart getPart(
                              int index ) throws NoSuchMimePartException {

        if (index >= parts.size()) {
            throw new NoSuchMimePartException("No MIME part at position '" + index + "'");
        }

        MimePart part = this.parts.get(index);
        if (part != null) {
            return part;
        } else {
            throw new NoSuchMimePartException("No part at position '" + index + "'");
        }
    }

    /**
     * Replaces body part with the specified content. <br>
     * If this is a plain text message, only the 'text/plain' is set. <br>
     * If this is a multipart message, both 'text/plain' and 'text/html' body
     * parts are set.
     *
     * @param content
     *            the body content
     * @throws PackageException
     */

    @PublicAtsApi
    public void setBody(
                         String content ) throws PackageException {

        setBody(content, content);

    }

    /**
     * Replaces body parts with the specified content. <br>
     * If this is a plain text only message, the provided HTML content will not
     * affect the message in any way.
     * <em>Note:</em> This method has effect only if there are already such (plain and/or HTML) parts.
     *
     * @param plainTextContent
     *            the text content of the message
     * @param htmlContent
     *            the HTML content of the message
     * @throws PackageException
     */
    @PublicAtsApi
    public void setBody(
                         String plainTextContent,
                         String htmlContent ) throws PackageException {

        try {
            String messageContentType = message.getContentType();
            if (messageContentType == null) {
                // not expected as default should be "text/plain"
                log.info("No content type is set yet. Body of message is not changed");
                return;
            } else {
                // type is not not case-sensitive as mentioned in
                // http://www.w3.org/Protocols/rfc1341/4_Content-Type.html and RFC 2045
                messageContentType = messageContentType.toLowerCase();
            }

            if (messageContentType.startsWith(CONTENT_TYPE_TEXT_PLAIN)) {
                // this is a text/plain message
                message.setContent(plainTextContent, message.getContentType()
                /* preserve any additional parameters like charset */ );
            } else {
                if (messageContentType.startsWith(CONTENT_TYPE_MULTIPART_PREFIX)) {
                    // this is a MULTIPART message
                    try {
                        BodyPart tmpBodyPart;
                        MimeMultipart tmpMultipartContent = (MimeMultipart) message.getContent();
                        for (int index = 0; index < tmpMultipartContent.getCount(); index++) {
                            tmpBodyPart = tmpMultipartContent.getBodyPart(index);

                            if (tmpBodyPart.getContentType().startsWith(CONTENT_PART_TYPE_TEXT_HTML)) {
                                // just replace content, do not create additional part
                                tmpBodyPart.setContent(htmlContent, tmpBodyPart.getContentType());
                                // for some reason after setting content there is match for text/plain too for the same body part
                                // so use if-else or continue
                            } else if (tmpBodyPart.getContentType()
                                                  .startsWith(CONTENT_PART_TYPE_TEXT_PLAIN)) {
                                // replace text part
                                tmpBodyPart.setContent(plainTextContent, tmpBodyPart.getContentType());
                                continue;
                            }
                            // do not check content type and go to process next part
                        }
                    } catch (IOException ioe) {
                        throw new PackageException("Could not add MIME body parts", ioe);
                    }
                }
            }

            // make sure all changes to the message are saved before decomposing
            try {
                message.saveChanges();
            } catch (MessagingException me) {
                throw new PackageException("Could not save message changes", me);
            }

            // we need to decompose again, as a new part has been added
            decompose();

        } catch (MessagingException me) {
            throw new PackageException(me);
        }
    }

    /**
     * Print some description of this MIME package.
     */
    @Override
    public String toString() {

        return getDescription();
    }

    /**
     * Reconnects if connection is closed.
     * <b>Note</b>Internal method
     * @return true if store re-connection is performed and this means that close should be closed after the work is done
     * @throws MessagingException
     */
    public boolean reconnectStoreIfClosed() throws MessagingException {

        boolean storeReconnected = false;

        // the folder is empty when the message is not loaded from IMAP server, but from a file
        Folder imapFolder = message.getFolder();
        if (imapFolder == null) {
            imapFolder = this.partOfImapFolder;
        } else {
            partOfImapFolder = imapFolder; // keep reference
        }
        if (imapFolder != null) {
            Store store = imapFolder.getStore();
            if (store != null) {
                if (!store.isConnected()) {
                    log.debug("Reconnecting store... ");
                    store.connect();
                    storeReconnected = true;
                }

                // Open folder in read-only mode
                if (!imapFolder.isOpen()) {
                    log.debug("Reopening folder " + imapFolder.getFullName()
                              + " in order to get contents of mail message");
                    imapFolder.open(Folder.READ_ONLY);
                }
            }
        }
        return storeReconnected;
    }

    /**
     * Close connection
     * <b>Note</b>Internal method
     * @throws MessagingException
     */
    public void closeStoreConnection(
                                      boolean storeConnected ) throws MessagingException {

        if (storeConnected) {
            // the folder is empty when the message is not loaded from IMAP server, but from a file
            Folder imapFolder = message.getFolder();
            if (imapFolder == null) {
                imapFolder = partOfImapFolder; // in case of nested package but still originating from IMAP server
            }
            if (imapFolder != null) {
                Store store = imapFolder.getStore();
                if (store != null && store.isConnected()) {
                    // closing store closes and its folders
                    log.debug("Closing store (" + store.toString() + ") and associated folders");
                    store.close();
                }
            }
        }
    }

    // -----------------------------------
    // START OF TRACING METHODS
    // -----------------------------------
    private String toStringTrace(
                                  String msgString,
                                  String level ) {

        final String level1 = level;
        final String level2 = "\t" + level1;

        StringBuilder msg = new StringBuilder(msgString);

        try {
            final String prefix = getPrefixTrace(level1, "MIME PACKAGE START: ");
            final String contentType = normalizeNewLinesTrace(prefix, message.getContentType()) + "\n";
            msg.append(level1 + "MIME PACKAGE START: " + contentType);

            if (this.nestedPath != null) {
                msg.append(level2 + "NESTED PATH: [" + nestedPath + "]\n");
            }

            // HEADERS
            msg.append(addHeadersTrace(message.getAllHeaders(), level2));

            if (this.tag != null) {
                msg.append(level2 + "TAG: " + tag + "\n");
            }
            if (envelopeSender != null) {
                msg.append(level2 + "ENVELOP SENDER: " + envelopeSender + "\n");
            }
            if (this.subjectCharset != null) {
                msg.append(level2 + "SUBJECT CHARSET: " + subjectCharset + "\n");
            }

            // BODY CONTENT
            Object content = message.getContent();
            msg.append(addContentTrace(content, level));

            // NESTED MESSAGES
            if (nestedMimePackages.size() > 0) {
                for (MimePackage nestedPackage : nestedMimePackages) {
                    msg.append(nestedPackage.toStringTrace("", "\t" + level));
                }
            }

            msg.append(level1 + "MIME PACKAGE END: " + contentType + "\n");
        } catch (Exception e) {
            // stack trace to string
            final Writer writer = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);

            msg.append("ERROR CONVERTING MIME PACKAGE TO STRING:\n" + writer.toString());
        }

        return msg.toString();
    }

    private String addHeadersTrace(
                                    Enumeration<?> headers,
                                    String level ) {

        final String level1 = level;
        final String level2 = "\t" + level1;
        final String prefix = getPrefixTrace(level1, "HEADERS START: ");

        StringBuilder headersString = new StringBuilder();

        boolean hasHeaders = headers.hasMoreElements();
        if (hasHeaders) {
            headersString.append(level1 + "HEADERS START:\n");
            while (headers.hasMoreElements()) {
                Header header = (Header) headers.nextElement();
                headersString.append(level2 + header.getName() + ": "
                                     + normalizeNewLinesTrace(prefix, header.getValue()) + "\n");
            }
            headersString.append(level1 + "HEADERS END:\n");
        }

        return headersString.toString();
    }

    private String addContentTrace(
                                    Object content,
                                    String level ) throws MessagingException {

        final String level1 = level;
        final String level2 = "\t" + level1;

        StringBuilder msg = new StringBuilder();
        if (content instanceof String) {
            msg.append(level2 + "BODY STRING START:\n");
            msg.append((String) content);
            msg.append(level2 + "BODY STRING END:\n");
        } else if (content instanceof MimeMultipart) {
            MimeMultipart multipart = (MimeMultipart) content;
            for (int i = 0; i < multipart.getCount(); i++) {
                msg.append(addBodyPartTrace(multipart.getBodyPart(i), level2));
            }
        } else {
            msg.append(level2 + "*** CANNOT CONVERT UNSUPPORTED CONTENT: "
                       + content.getClass().getCanonicalName() + " ***\n");
        }

        return msg.toString();
    }

    private String addBodyPartTrace(
                                     BodyPart bodyPart,
                                     String level ) throws MessagingException {

        final String level1 = level;
        final String level2 = "\t" + level1;

        StringBuilder msg = new StringBuilder();

        final String prefix = getPrefixTrace(level1, "BODY PART START: ");
        final String contentType = normalizeNewLinesTrace(prefix, bodyPart.getContentType()) + "\n";
        msg.append(level1 + "BODY PART START: " + contentType);
        msg.append(addHeadersTrace(bodyPart.getAllHeaders(), level2));
        msg.append(level1 + "BODY PART END: " + contentType);

        return msg.toString();
    }

    private String normalizeNewLinesTrace(
                                           String prefix,
                                           String textToNormalize ) {

        textToNormalize = textToNormalize.replaceAll("\r\n", "\n" + prefix);
        textToNormalize = textToNormalize.replaceAll("\r", "\n" + prefix);
        return textToNormalize;
    }

    private String getPrefixTrace(
                                   String level,
                                   String prefix ) {

        StringBuilder wholePrefix = new StringBuilder();
        wholePrefix.append(level);
        for (int i = 0; i < prefix.length(); i++) {
            wholePrefix.append(" ");
        }
        return wholePrefix.toString();
    }

    // -----------------------------------
    // END OF TRACING METHODS
    // -----------------------------------

}
