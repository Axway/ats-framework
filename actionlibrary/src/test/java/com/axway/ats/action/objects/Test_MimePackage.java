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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.action.BaseTest;
import com.axway.ats.action.objects.MimePackage;
import com.axway.ats.action.objects.PackageLoader;
import com.axway.ats.action.objects.model.NoSuchHeaderException;
import com.axway.ats.action.objects.model.NoSuchMimePartException;
import com.axway.ats.action.objects.model.ObjectNotTaggedException;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.action.objects.model.PackagePriority;
import com.axway.ats.action.objects.model.RecipientType;

public class Test_MimePackage extends BaseTest {

    private static String mailMessagePath;
    private static String mailMessageDir;
    MimePackage           eMailMessage;

    @BeforeClass
    public static void setUpTest_EmailMessage() {

        mailMessagePath = Test_MimePackage.class.getResource("mail.msg").getPath();
        mailMessageDir = (new File(mailMessagePath)).getParent();
    }

    @Before
    public void setUp() throws Exception {

        eMailMessage = new MimePackage(new FileInputStream(mailMessagePath));
    }

    @Test
    public void constructWithSessionOnly() throws Exception {

        MimePackage message = new MimePackage();
        assertEquals(0, message.getRegularPartCount());
        assertEquals(0, message.getAttachmentPartCount());
    }

    @Test
    public void constructFromMimeMessage() throws Exception {

        MimePackage message = new MimePackage(new MimeMessage(Session.getDefaultInstance(new Properties()),
                                                              new FileInputStream(mailMessagePath)));
        assertEquals(4, message.getRegularPartCount());
        assertEquals(2, message.getAttachmentPartCount());
    }

    @Test
    public void constructFromInputStream() throws Exception {

        MimePackage message = new MimePackage(new FileInputStream(mailMessagePath));
        assertEquals(4, message.getRegularPartCount());
        assertEquals(2, message.getAttachmentPartCount());
    }

    @Test
    public void constructFromSessionAndInputStream() throws Exception {

        MimePackage message = new MimePackage(new FileInputStream(mailMessagePath));
        assertEquals(4, message.getRegularPartCount());
        assertEquals(2, message.getAttachmentPartCount());
    }

    @Test
    public void constructFromInputStreamWithJpeg() throws Exception {

        MimePackage message = new MimePackage(new FileInputStream(mailMessageDir + "/jpeg.msg"));
        assertEquals(1, message.getRegularPartCount());
        assertEquals(1, message.getAttachmentPartCount());
    }

    @Test
    public void getPlainTextAsInputStream() throws Exception {

        MimePackage message = new MimePackage(new FileInputStream(mailMessageDir
                                                                  + "/plainTextAsInputStream.msg"));
        assertTrue(message.getPlainTextBody().endsWith("Please see the inline doc fore more information."));
    }

    @Test
    public void addAttachment() throws Exception {

        assertEquals(4, eMailMessage.getRegularPartCount());
        assertEquals(2, eMailMessage.getAttachmentPartCount());

        eMailMessage.addAttachment(mailMessageDir + "/attachment.txt");
        assertEquals(4, eMailMessage.getRegularPartCount());
        assertEquals(3, eMailMessage.getAttachmentPartCount());
    }

    @Test
    public void addAttachmentFromString() throws Exception {

        assertEquals(4, eMailMessage.getRegularPartCount());
        assertEquals(2, eMailMessage.getAttachmentPartCount());

        eMailMessage.addAttachment("attachement content", "attachment.txt");
        assertEquals(4, eMailMessage.getRegularPartCount());
        assertEquals(3, eMailMessage.getAttachmentPartCount());
    }

    @Test
    public void addAttachmentFromStringDifferentCharset() throws Exception {

        assertEquals(4, eMailMessage.getRegularPartCount());
        assertEquals(2, eMailMessage.getAttachmentPartCount());

        eMailMessage.addAttachment("български текст", "utf8", "attachment.txt");
        assertEquals(4, eMailMessage.getRegularPartCount());
        assertEquals(3, eMailMessage.getAttachmentPartCount());
    }

    @Test
    public void addAttachmentDir() throws Exception {

        assertEquals(4, eMailMessage.getRegularPartCount());
        assertEquals(2, eMailMessage.getAttachmentPartCount());

        eMailMessage.addAttachmentDir(mailMessageDir + "/attachmentDir");
        assertEquals(4, eMailMessage.getRegularPartCount());
        assertEquals(4, eMailMessage.getAttachmentPartCount());
    }

    @Test
    public void addPart() throws Exception {

        String content = "content1";
        String contentType = MimePackage.PART_TYPE_TEXT_PLAIN;

        eMailMessage.addPart(content, contentType);
        assertEquals(5, eMailMessage.getRegularPartCount());
        assertEquals(2, eMailMessage.getAttachmentPartCount());

        assertNotNull(eMailMessage.getRegularPartData(4));
    }

    @Test
    public void addPartSpecificCharset() throws Exception {

        String content = "български текст";
        String contentType = MimePackage.PART_TYPE_TEXT_PLAIN;

        eMailMessage.addPart(content, contentType, "utf-8");
        assertEquals(5, eMailMessage.getRegularPartCount());
        assertEquals(2, eMailMessage.getAttachmentPartCount());

        assertNotNull(eMailMessage.getRegularPartData(4));
    }

    @Test
    public void addPartFromFile() throws Exception {

        String fileName = Test_MimePackage.class.getResource("attachmentDir/INSTALL.LOG").getPath();
        String contentType = MimePackage.PART_TYPE_TEXT_PLAIN;

        eMailMessage.addPartFromFile(fileName, contentType);
        assertEquals(5, eMailMessage.getRegularPartCount());
        assertEquals(2, eMailMessage.getAttachmentPartCount());

        assertNotNull(eMailMessage.getRegularPartData(4));
    }

    @Test
    public void addRecpTo() throws Exception {

        String[] addresses = new String[]{ "test1@test.com", "test2@test.com" };

        eMailMessage.addRecipient(RecipientType.TO, addresses);
        assertEquals(3, eMailMessage.getRecipientCount(RecipientType.TO));
    }

    @Test
    public void addRecipientCc() throws Exception {

        String[] addresses = new String[]{ "test1@test.com", "test2@test.com" };

        eMailMessage.addRecipient(RecipientType.CC, addresses);
        assertEquals(2, eMailMessage.getRecipientCount(RecipientType.CC));
    }

    @Test
    public void addRecipientBcc() throws Exception {

        String[] addresses = new String[]{ "test1@test.com", "test2@test.com" };

        eMailMessage.addRecipient(RecipientType.BCC, addresses);
        assertEquals(2, eMailMessage.getRecipientCount(RecipientType.BCC));
    }

    @Test
    public void getRegularPartContentTypePositive() throws Exception {

        assertEquals("text/plain", eMailMessage.getRegularPartContentType(0));
        assertEquals("text/plain", eMailMessage.getRegularPartContentType(1));
    }

    @Test( expected = NoSuchMimePartException.class)
    public void getRegularPartContentTypeNegative() throws Exception {

        eMailMessage.getRegularPartContentType(25);
    }

    @Test
    public void getAttachmentPartDataPositive() throws Exception {

        InputStream attachmentStream = eMailMessage.getAttachmentPartData(0);

        assertNotNull(attachmentStream);

        byte[] packageBodyBytes = new byte[attachmentStream.available()];
        attachmentStream.read(packageBodyBytes);

        assertTrue(packageBodyBytes.length > 30000);
    }

    @Test( expected = NoSuchMimePartException.class)
    public void getAttachmentPartDataNegative() throws Exception {

        eMailMessage.getAttachmentPartData(25);
    }

    @Test
    public void getAttachmentFileNamePositive() throws Exception {

        assertEquals(eMailMessage.getAttachmentFileName(0), "24thekilt.jpg");
        assertEquals(eMailMessage.getAttachmentFileName(1), "24thekilt.jpg");
    }

    @Test( expected = NoSuchMimePartException.class)
    public void getAttachmentFileNameNegative() throws Exception {

        eMailMessage.getAttachmentPartData(25);
    }

    @Test
    public void getAttachmentContentTypePositive() throws Exception {

        assertEquals("image/jpeg", eMailMessage.getAttachmentContentType(0));
        assertEquals("image/jpeg", eMailMessage.getAttachmentContentType(1));
    }

    @Test( expected = NoSuchMimePartException.class)
    public void getAttachmentContentTypeNegative() throws Exception {

        eMailMessage.getAttachmentContentType(25);
    }

    @Test
    public void getAttachmentCharsetPositive() throws Exception {

        eMailMessage.addAttachment(mailMessageDir + "/attachmentDir/MSADDNDR.DLL");
        assertEquals(null, eMailMessage.getAttachmentCharset(2));

        eMailMessage.addAttachment("attachement content", "attachment.txt");
        assertEquals("us-ascii", eMailMessage.getAttachmentCharset(3));

        eMailMessage.addAttachment("български текст", "utf8", "attachment.txt");
        assertEquals("utf8", eMailMessage.getAttachmentCharset(4));
    }

    @Test( expected = NoSuchMimePartException.class)
    public void getAttachmentCharsetNegative() throws Exception {

        eMailMessage.getAttachmentCharset(10);
    }

    @Test
    public void setEnvelopeSender() {

        String envelopeSender = "test1@test0.com";

        assertEquals(null, eMailMessage.getEnvelopeSender());

        eMailMessage.setEnvelopeSender(envelopeSender);
        assertEquals(envelopeSender, eMailMessage.getEnvelopeSender());
    }

    @Test
    public void getHeaderMainBodyPart() throws Exception {

        String headerName = "To";
        int index = 0;

        assertEquals("\"pop408\" <pop408@host.localdomain>",
                     eMailMessage.getHeader(headerName, index));
    }

    @Test
    public void getHeaderMainBodyPartShort() throws Exception {

        String headerName = "To";
        assertEquals("\"pop408\" <pop408@host.localdomain>", eMailMessage.getHeader(headerName));
    }

    @Test
    public void getHeaderSecondBodyPart() throws Exception {

        String headerName = "Content-type";
        int partNum = 1;

        assertEquals("text/plain; charset=iso-8859-4", eMailMessage.getPartHeader(headerName, partNum));
    }

    @Test( expected = NoSuchMimePartException.class)
    public void getHeaderNegaitveNoSuchPart() throws Exception {

        String headerName = "To";
        int partNum = 16;

        eMailMessage.getPartHeader(headerName, partNum);
    }

    @Test( expected = NoSuchHeaderException.class)
    public void getHeaderNegativeNoHeaderAtThisPosition() throws Exception {

        String headerName = "To";
        eMailMessage.getHeader(headerName, 16);
    }

    @Test
    public void getPartChecksumPositive() throws Exception {

        assertTrue(eMailMessage.getPartChecksum(2, false) > 0);
        assertTrue(eMailMessage.getPartChecksum(1, true) > 0);
    }

    @Test
    public void getRegularPartCharset() throws PackageException {

        String content = "content1";
        String contentType = MimePackage.PART_TYPE_TEXT_PLAIN;

        eMailMessage.addPart(content, contentType);
        assertEquals("us-ascii", eMailMessage.getRegularPartCharset(4));

        content = "български текст";
        contentType = MimePackage.PART_TYPE_TEXT_HTML;

        eMailMessage.addPart(content, contentType, "utf-8");
        assertEquals("utf-8", eMailMessage.getRegularPartCharset(5));
    }

    @Test
    public void getSubject() throws Exception {

        assertEquals("RE: MUNCH", eMailMessage.getSubject());
    }

    @Test
    public void getSubjectCharset() throws Exception {

        assertNull(eMailMessage.getSubjectCharset());

        //set the subject with a specific encoding
        eMailMessage.setSubject("Subject", "ISO-8859-1");
        assertEquals("ISO-8859-1", eMailMessage.getSubjectCharset());
    }

    @Test
    public void setCustomHeader() throws Exception {

        String headerName = "Custom-header";
        String value = "test";

        eMailMessage.setHeader(headerName, value);
        assertEquals("test", eMailMessage.getHeader(headerName, 0));
    }

    @Test
    public void setPriority() throws Exception {

        eMailMessage.setPriority(PackagePriority.HIGH);
        assertEquals("1", eMailMessage.getHeader("X-Priority"));
        assertEquals("High", eMailMessage.getHeader("Importance"));
    }

    @Test
    public void setRecipientTo() throws Exception {

        String[] addresses = new String[]{ "test1@test.com", "test2@test.com" };

        eMailMessage.setRecipient(RecipientType.TO, addresses);
        assertEquals(2, eMailMessage.getRecipientCount(RecipientType.TO));
    }

    @Test
    public void setRecipientCc() throws Exception {

        String[] addresses = new String[]{ "test1@test.com", "test2@test.com" };

        eMailMessage.setRecipient(RecipientType.CC, addresses);
        assertEquals(2, eMailMessage.getRecipientCount(RecipientType.CC));
    }

    @Test
    public void setSender() throws Exception {

        eMailMessage.setSender("test1@test.com");
        assertEquals(eMailMessage.getSender(), "test1@test.com");

        eMailMessage.setSender("    test1@test.com    ");
        assertEquals(eMailMessage.getSender(), "test1@test.com");
    }

    @Test
    public void setSenderWithPersonalName() throws Exception {

        final String expectedSender = "Firtstname Lastname <test1@test.com>";

        eMailMessage.setSender("Firtstname Lastname <test1@test.com>");
        assertEquals(eMailMessage.getSender(), expectedSender);

        eMailMessage.setSender("Firtstname Lastname test1@test.com");
        assertEquals(eMailMessage.getSender(), expectedSender);

        eMailMessage.setSender("   Firtstname Lastname   <test1@test.com>    ");
        assertEquals(eMailMessage.getSender(), expectedSender);

        eMailMessage.setSender("   Firtstname Lastname   test1@test.com    ");
        assertEquals(eMailMessage.getSender(), expectedSender);
    }

    @Test
    public void setSenderName() throws Exception {

        String senderName = "test123";

        eMailMessage.setSenderName(senderName);
    }

    @Test
    public void getSender() throws Exception {

        eMailMessage.setSender("test1@test.com");
        eMailMessage.setSenderName("test1");
        assertEquals("test1 <test1@test.com>", eMailMessage.getSender());
    }

    @Test
    public void getSenderAddress() throws Exception {

        String sender = "test1@test.com";

        eMailMessage.setSender(sender);
        eMailMessage.setSenderName("test1");
        assertEquals(sender, eMailMessage.getSenderAddress());
    }

    @Test
    public void setBody_textPart_only() throws Exception {

        // create a new message. It comes by default with TEXT part only(no multiparts)
        MimePackage newMailMessage = new MimePackage();

        // verify there are no any parts yet
        try {
            newMailMessage.getPart(0, false);
            assertTrue("There are some parts, while we expect to have none", false);
        } catch (NoSuchMimePartException e) {}

        // set the body, effectively the only TEXT part
        newMailMessage.setBody("text plain body");

        MimePart textPart = newMailMessage.getPart(0, false);
        assertEquals(textPart.getContent(), "text plain body");
        assertEquals(textPart.getContentType(), "text/plain; charset=us-ascii");

        // verify there are no more parts
        try {
            newMailMessage.getPart(1, false);
            assertTrue("There is more than 1 part, while we expect to have just 1", false);
        } catch (NoSuchMimePartException e) {}
    }

    @Test
    public void setBody_multiParts_with_textPart_only() throws Exception {

        // create a new message and add TEXT part to it
        MimePackage newMailMessage = new MimePackage();
        newMailMessage.addPart("text plain body", MimePackage.PART_TYPE_TEXT_PLAIN);

        MimePart textPart = newMailMessage.getPart(0, false);
        assertEquals(textPart.getContent(), "text plain body");
        assertEquals(textPart.getContentType(), "text/plain; charset=us-ascii");

        // modify the only part
        newMailMessage.setBody("new body");

        // verify the modifications
        MimePart newTextPart = newMailMessage.getPart(0, false);
        assertEquals(newTextPart.getContent(), "new body");
        assertEquals(newTextPart.getContentType(), "text/plain; charset=us-ascii");

        // verify there are no more parts
        try {
            newMailMessage.getPart(1, false);
            assertTrue("There is more than 1 part, while we expect to have just 1", false);
        } catch (NoSuchMimePartException e) {}
    }

    @Test
    public void setBody_multiParts_with_htmlPart_only() throws Exception {

        // create a new message and add HTML part to it
        MimePackage newMailMessage = new MimePackage();
        newMailMessage.addPart("html body", MimePackage.PART_TYPE_TEXT_HTML);

        MimePart htmlPart = newMailMessage.getPart(0, false);
        assertEquals(htmlPart.getContent(), "html body");
        assertEquals(htmlPart.getContentType(), "text/html; charset=us-ascii");

        // modify the only part
        newMailMessage.setBody("new body");

        // verify the modifications
        MimePart newHtmlPart = newMailMessage.getPart(0, false);
        assertEquals(newHtmlPart.getContent(), "new body");
        assertEquals(newHtmlPart.getContentType(), "text/html; charset=us-ascii");

        // verify there are no more parts
        try {
            newMailMessage.getPart(1, false);
            assertTrue("There is more than 1 part, while we expect to have just 1", false);
        } catch (NoSuchMimePartException e) {}
    }

    @Test
    public void setBody_multiParts_with_textAndHtmlParts() throws Exception {

        // create a new message and add TEXT and HTML parts to it
        MimePackage newMailMessage = new MimePackage();
        newMailMessage.addPart("text plain body", MimePackage.PART_TYPE_TEXT_PLAIN);
        newMailMessage.addPart("html body", MimePackage.PART_TYPE_TEXT_HTML);

        MimePart textPart = newMailMessage.getPart(0, false);
        assertEquals(textPart.getContent(), "text plain body");
        assertEquals(textPart.getContentType(), "text/plain; charset=us-ascii");

        MimePart htmlPart = newMailMessage.getPart(1, false);
        assertEquals(htmlPart.getContent(), "html body");
        assertEquals(htmlPart.getContentType(), "text/html; charset=us-ascii");

        // modify both parts
        newMailMessage.setBody("new body");

        // verify the modifications
        MimePart newTextPart = newMailMessage.getPart(0, false);
        assertEquals(newTextPart.getContent(), "new body");
        assertEquals(newTextPart.getContentType(), "text/plain; charset=us-ascii");

        MimePart newHtmlPart = newMailMessage.getPart(1, false);
        assertEquals(newHtmlPart.getContent(), "new body");
        assertEquals(newHtmlPart.getContentType(), "text/html; charset=us-ascii");

        // verify there are no more parts
        try {
            newMailMessage.getPart(2, false);
            assertTrue("There is more than 2 parts, while we expect to have just 2", false);
        } catch (NoSuchMimePartException e) {}
    }

    @Test
    public void setSubject() throws Exception {

        String charset = "utf8";
        String subject = "subject1";

        eMailMessage.setSubject(subject, "");
        assertEquals(subject, eMailMessage.getSubject());

        eMailMessage.setSubject(subject, charset);
        assertEquals(subject, eMailMessage.getSubject());
    }

    @Test
    public void setHeadersEmptyMessage() throws Exception {

        MimePackage mimeMessage = new MimePackage();
        mimeMessage.setSubject("test subject");
        mimeMessage.setSender("sender@sender.com");
        mimeMessage.setSenderName("sendercho");
        mimeMessage.setRecipient("test0@test.com");

        assertEquals("test subject", mimeMessage.getSubject());
        assertEquals("sender@sender.com", mimeMessage.getSenderAddress());
        assertEquals("sendercho <sender@sender.com>", mimeMessage.getSender());
        assertEquals(1, mimeMessage.getRecipientCount(RecipientType.TO));
    }

    @Test
    public void addPartEmptyMessage() throws Exception {

        MimePackage mimeMessage = new MimePackage();
        mimeMessage.addPart("This is message body", MimePackage.PART_TYPE_TEXT_PLAIN);

        assertEquals(1, mimeMessage.getRegularPartCount());
        assertEquals(0, mimeMessage.getAttachmentPartCount());

    }

    @Test
    public void addAlternativePartEmptyMessage() throws Exception {

        MimePackage mimeMessage = new MimePackage();
        mimeMessage.addAlternativePart("text123", "<html>alternative</html>");

        assertEquals(2, mimeMessage.getRegularPartCount());
        assertEquals(0, mimeMessage.getAttachmentPartCount());

        mimeMessage = new MimePackage();
        mimeMessage.addAlternativePart("text123", "<html>alternative</html>", "utf-8");

        assertEquals(2, mimeMessage.getRegularPartCount());
        assertEquals(0, mimeMessage.getAttachmentPartCount());

        assertEquals("utf-8", mimeMessage.getRegularPartCharset(0));
        assertEquals("utf-8", mimeMessage.getRegularPartCharset(1));
    }

    @Test
    public void addAttachmentsEmptyMessage() throws Exception {

        MimePackage mimeMessage = new MimePackage();
        mimeMessage.addAttachment("test123", "attachment123.txt");
        mimeMessage.addAttachment(mailMessageDir + "/attachment.txt");

        assertEquals(0, mimeMessage.getRegularPartCount());
        assertEquals(2, mimeMessage.getAttachmentPartCount());
    }

    @Test
    public void addAttachmentsCheckContentType() throws Exception {

        MimePackage mimeMessage = new MimePackage();
        mimeMessage.addAttachment(mailMessageDir + "/attachmentDir/MSADDNDR.DLL");

        assertEquals("application/octet-stream", mimeMessage.getAttachmentContentType(0));
    }

    @Test
    public void writeToPositive() throws Exception {

        File tempFile = File.createTempFile("mimePackageTest", ".tmp");

        try {
            eMailMessage.addAttachment("attachement content", "attachment.txt");
            eMailMessage.writeToFile(tempFile.getAbsolutePath());

            assertTrue(tempFile.length() > 0);
        } finally {
            tempFile.delete();
        }
    }

    @Test
    public void tag() throws Exception {

        long minTagValue = Calendar.getInstance().getTimeInMillis();

        MimePackage mimeMessage = new MimePackage();
        mimeMessage.tag();
        long tagValue = Long.parseLong(mimeMessage.getTag());

        long maxTagValue = Calendar.getInstance().getTimeInMillis();

        assertTrue(minTagValue <= tagValue);
        assertTrue(tagValue <= maxTagValue);
    }

    @Test( expected = ObjectNotTaggedException.class)
    public void getTagWhenNoTagIsSet() throws Exception {

        MimePackage mimeMessage = new MimePackage();
        mimeMessage.getTag();
    }

    // getPlainTextBody() related tests

    @Test
    public void testGetContentOfNewlyCreatedAlternativeMail() throws PackageException {

        MimePackage mimePack = new MimePackage();
        // mimePack.setBody( "my test mail content" );
        mimePack.setHeader("Content-type", "multipart/alternative");
        String contentPlain = "my test mail content";
        String contentHtml = "<html>" + contentPlain + "</html>";
        mimePack.addPart("dummy", MimePackage.PART_TYPE_TEXT_PLAIN);
        mimePack.addPart("dummyHtml", MimePackage.PART_TYPE_TEXT_HTML);

        mimePack.setBody(contentPlain, contentHtml);

        assertEquals(contentPlain, mimePack.getPlainTextBody());
        assertEquals(contentHtml, mimePack.getHtmlTextBody());
    }

    @Test
    public void testGetContentOfNewlyCreatedMixedMail() throws PackageException {

        MimePackage mimePack = new MimePackage();
        mimePack.setHeader("Content-type", "multipart/alternative");
        String contentPlain = "my test mail content";
        String contentHtml = "<html>" + contentPlain + "</html>";

        mimePack.addPart("dummy", MimePackage.PART_TYPE_TEXT_PLAIN);
        mimePack.addPart("dummyHtml", MimePackage.PART_TYPE_TEXT_HTML);

        mimePack.setBody(contentPlain, contentHtml);

        assertEquals(contentPlain, mimePack.getPlainTextBody());
        assertEquals(contentHtml, mimePack.getHtmlTextBody());
    }

    @Test
    public void testGetContentOfNewlyCreatedTextOnlyMail() throws PackageException {

        MimePackage mimePack = new MimePackage();
        String contentPlain = "my test mail content";
        // optional as by default the content type is text/plain
        mimePack.addPart("dummy", MimePackage.PART_TYPE_TEXT_PLAIN);

        mimePack.setBody(contentPlain);

        assertEquals(contentPlain, mimePack.getPlainTextBody());
        assertEquals(null, mimePack.getHtmlTextBody());
    }

    @Test
    public void testGetContentOfNewlyCreatedHtmlOnlyMail() throws PackageException {

        MimePackage mimePack = new MimePackage();
        String contentHtml = "<html>my test mail content</html>";
        mimePack.addPart(contentHtml, MimePackage.PART_TYPE_TEXT_HTML, "UTF-8");

        assertEquals(contentHtml, mimePack.getHtmlTextBody());
        assertEquals(null, mimePack.getPlainTextBody());
    }

    @Test
    public void testGetContentOfSavedMail() throws PackageException {

        String nestedMailPath = Test_MimePackage.class.getResource("nestedMessageGetContentTest.msg")
                                                      .getPath();
        MimePackage mimePack = PackageLoader.loadMimePackageFromFile(nestedMailPath);
        String contentHtml = mimePack.getHtmlTextBody();
        assertNotNull(contentHtml);
        assertTrue(contentHtml.contains("Some text<br>"));
        assertEquals(null, mimePack.getPlainTextBody());

        // nested MimePackage test
        MimePackage nested = mimePack.getNeededMimePackage(new int[]{ 0 });
        String nestedHtmlBody = nested.getHtmlTextBody();
        System.out.println("nestedHtmlBody: " + nestedHtmlBody);
        assertTrue(nestedHtmlBody.contains("Message body<br>"));
        assertNull(nested.getPlainTextBody());
    }

    @Test
    public void testGetContentOfSavedMailWithAttachmentOnly() throws PackageException {
        // expected null since attachments should not be checked for text/plain or text/html

        String nestedMailPath = Test_MimePackage.class.getResource("getPlainTextBody__attachment_only.eml")
                                                      .getPath();
        MimePackage mimePack = PackageLoader.loadMimePackageFromFile(nestedMailPath);
        String contentText = mimePack.getPlainTextBody();
        assertNull(contentText);

        String contentHtml = mimePack.getHtmlTextBody();
        assertNull(contentHtml);

    }

    @Test
    public void testGetContentOfSavedMailWithAttachmentAndAnotherBody() throws PackageException {
        // expected to get contents of inline body, not of attachment body

        String nestedMailPath = Test_MimePackage.class.getResource("getPlainTextBody__body_and_attachment.eml")
                                                      .getPath();
        MimePackage mimePack = PackageLoader.loadMimePackageFromFile(nestedMailPath);
        String contentText = mimePack.getPlainTextBody();
        assertEquals("TEST", contentText);

        String contentHtml = mimePack.getHtmlTextBody();
        assertNull(contentHtml);

    }

    @Test
    public void testGetExceptionCauseWithWrongMimeBoundary() throws Exception {

        String nestedMailPath = Test_MimePackage.class.getResource("nestedMessageGetContentTestWrongBoundary.msg")
                                                      .getPath();
        try {
            PackageLoader.loadMimePackageFromFile(nestedMailPath);
        }
        catch(PackageException e) {
            String message = recurseCauses(e);
            if (message.contains("Missing start boundary")) {
                return;
            } else {
                throw new Exception("Test scenario threw a different exception "
                                    + "to the one that was expected: " + e + " was thrown ");
            }
        }
        Assert.fail("Exception was expected to be thrown");
    }

    private String recurseCauses( Throwable e ) {

        StringBuilder buffer = new StringBuilder();

        buffer.append(". CAUSE: ").append(e.getMessage());
        if (e.getCause() != null) {
            buffer.append(recurseCauses(e.getCause()));
        }

        return buffer.toString();
    }

}
