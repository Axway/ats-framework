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
package com.axway.ats.rbv.clients;

import java.util.Arrays;
import java.util.List;

import com.axway.ats.action.objects.MimePackage;
import com.axway.ats.action.security.PackageEncryptor;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.executors.MetaExecutor;
import com.axway.ats.rbv.imap.ImapEncryptedFolderSearchTerm;
import com.axway.ats.rbv.imap.ImapFolderSearchTerm;
import com.axway.ats.rbv.imap.ImapMetaData;
import com.axway.ats.rbv.imap.ImapStorage;
import com.axway.ats.rbv.imap.rules.AttachmentNameRule;
import com.axway.ats.rbv.imap.rules.HeaderRule;
import com.axway.ats.rbv.imap.rules.HeaderRule.HeaderMatchMode;
import com.axway.ats.rbv.imap.rules.MimePartCountRule;
import com.axway.ats.rbv.imap.rules.SMimeSignatureRule;
import com.axway.ats.rbv.imap.rules.StringInMimePartRule;
import com.axway.ats.rbv.imap.rules.SubjectRule;
import com.axway.ats.rbv.imap.rules.SubjectRule.SubjectMatchMode;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.storage.Matchable;

/**
 * Class used for verification of email message stored on an IMAP server.
 * <p>Note that <code>check</code> methods add rules for match whereas actual rules
 * evaluation is done in one of the methods with <code>verify</code> prefix.</p>
 *
 * <br><br>Its verification methods are supported on the top level message and on a nested message as well.
 * <br>For example:
 * <blockquote>- the first nested message is the [0]
 * <br>- the second nested message is the [1]
 * <br>- the second nested message of the first nested message is the [0, 1]
 *
 * <br><br>
 * <b>User guide</b> pages related to this class:<br>
 * <a href="https://axway.github.io/ats-framework/Common-test-verifications.html">RBV basics</a>
 * and
 * <a href="https://axway.github.io/ats-framework/IMAP-verifications.html">IMAP verification details</a>
 */
@PublicAtsApi
public class ImapVerification extends VerificationSkeleton {

    private String monitorName = "imap_monitor_";

    /**
     * Create an IMAP verification component
     *
     * @param imapServer    the IMAP server
     * @param userName      the IMAP user name
     * @param password      the password of the IMAP user
     */
    @PublicAtsApi
    public ImapVerification( String imapServer,
                             String userName,
                             String password ) throws RbvException {

        super();

        this.monitorName += userName;

        ImapStorage storage = new ImapStorage(imapServer);
        folder = storage.getFolder(new ImapFolderSearchTerm(userName, password));
        this.executor = new MetaExecutor();
    }

    /**
     * Create an IMAP verification component for encrypted message
     *
     * @param imapServer    the IMAP server
     * @param userName      the IMAP user name
     * @param password      the password of the IMAP user
     * @param packageEncryptor encryptor to use in order to decrypt the message
     * @throws RBVException
     */
    @PublicAtsApi
    public ImapVerification( String imapServer,
                             String userName,
                             String password,
                             PackageEncryptor packageEncryptor ) throws RbvException {

        super();

        this.monitorName += userName;

        ImapStorage storage = new ImapStorage(imapServer);
        folder = storage.getFolder(new ImapEncryptedFolderSearchTerm(userName, password, packageEncryptor));
        this.executor = new MetaExecutor();
    }

    /**
     * Create an IMAP verification component which uses user
     * provided carrier for message retrieving.<br><br>
     * The particular implementation should override the {@link VerificationSkeleton#getMonitorName()} or the default "imap_monitor_" will be used
     *
     * @param folder the user provided carrier for message retrieving
     * @throws RbvException
     */
    protected ImapVerification( Matchable folder ) throws RbvException {

        super();

        this.folder = folder;
        this.executor = new MetaExecutor();
    }

    /**
     * Check that the IMAP message has the specified number of attachments
     *
     * @param expectNumAttachments
     */
    @PublicAtsApi
    public void checkAttachmentNumber(
                                       int expectNumAttachments ) {

        checkAttachmentNumber(expectNumAttachments, new int[0]);
    }

    /**
     * Check that a nested IMAP message has the specified number of attachments
     *
     * @param expectNumAttachments
     * @param nestedPackagePath path to the nested message
     */
    @PublicAtsApi
    public void checkAttachmentNumber(
                                       int expectNumAttachments,
                                       int... nestedPackagePath ) {

        //create the rule
        MimePartCountRule attachmentCountRule = new MimePartCountRule(nestedPackagePath,
                                                                      expectNumAttachments,
                                                                      true,
                                                                      "checkAttachmentNumber"
                                                                            + getNestedMimePackagePathDescription(nestedPackagePath),
                                                                      true);
        rootRule.addRule(attachmentCountRule);
    }

    /**
     * Check that the IMAP message contains the specified string in the given regular body part
     *
     * @param searchString  the string to search for
     * @param partIndex     the index of the regular part in the MIME structure (attachments are skipped)
     */
    @PublicAtsApi
    public void checkStringInRegularBodyPart(
                                              String searchString,
                                              int partIndex ) {

        checkStringInRegularBodyPart(searchString, partIndex, new int[0]);
    }

    /**
     * Check that the nested IMAP message contains the specified string in the given regular body part
     *
     * @param searchString  the string to search for
     * @param partIndex     the index of the regular part in the MIME structure (attachments are skipped)
     * @param nestedPackagePath path to the nested message
     */
    @PublicAtsApi
    public void checkStringInRegularBodyPart(
                                              String searchString,
                                              int partIndex,
                                              int... nestedPackagePath ) {

        //create the rule
        StringInMimePartRule stringInPartRule = new StringInMimePartRule(nestedPackagePath,
                                                                         searchString,
                                                                         false,
                                                                         partIndex,
                                                                         false,
                                                                         "checkStringInRegularBodyPart"
                                                                                + getNestedMimePackagePathDescription(nestedPackagePath),
                                                                         true);
        rootRule.addRule(stringInPartRule);
    }

    /**
     * Check that the IMAP message contains the specified regular expression in the given regular body part
     *
     * @param searchRegex  the regular expression to search for
     * @param partIndex    the index of the regular part in the MIME structure (attachments are skipped)
     */
    @PublicAtsApi
    public void checkRegexInRegularBodyPart(
                                             String searchRegex,
                                             int partIndex ) {

        checkRegexInRegularBodyPart(searchRegex, partIndex, new int[0]);
    }

    /**
     * Check that the nested IMAP message contains the specified regular expression in the given regular body part
     *
     * @param searchRegex  the regular expression to search for
     * @param partIndex    the index of the regular part in the MIME structure (attachments are skipped)
     * @param nestedPackagePath path to the nested message
     */
    @PublicAtsApi
    public void checkRegexInRegularBodyPart(
                                             String searchRegex,
                                             int partIndex,
                                             int... nestedPackagePath ) {

        //create the rule
        StringInMimePartRule stringInPartRule = new StringInMimePartRule(nestedPackagePath,
                                                                         searchRegex,
                                                                         true,
                                                                         partIndex,
                                                                         false,
                                                                         "checkRegexInRegularBodyPart"
                                                                                + getNestedMimePackagePathDescription(nestedPackagePath),
                                                                         true);
        rootRule.addRule(stringInPartRule);
    }

    /**
     * Check that the IMAP message contains the specified string in the given attachment part
     *
     * @param searchString      the string to search for
     * @param attachmentIndex   the index of the attachment in the MIME structure (regular parts are skipped)
     */
    @PublicAtsApi
    public void checkStringInAttachment(
                                         String searchString,
                                         int attachmentIndex ) {

        checkStringInAttachment(searchString, attachmentIndex, new int[0]);
    }

    /**
     * Check that the nested IMAP message contains the specified string in the given attachment part
     *
     * @param searchString      the string to search for
     * @param attachmentIndex   the index of the attachment in the MIME structure (regular parts are skipped)
     * @param nestedPackagePath path to the nested message
     */
    @PublicAtsApi
    public void checkStringInAttachment(
                                         String searchString,
                                         int attachmentIndex,
                                         int... nestedPackagePath ) {

        //create the rule
        StringInMimePartRule stringInPartRule = new StringInMimePartRule(nestedPackagePath,
                                                                         searchString,
                                                                         false,
                                                                         attachmentIndex,
                                                                         true,
                                                                         "checkStringInAttachment"
                                                                               + getNestedMimePackagePathDescription(nestedPackagePath),
                                                                         true);
        rootRule.addRule(stringInPartRule);
    }

    /**
     * Check that the IMAP message contains the specified regular expression in the given attachment part
     *
     * @param searchRegex       the regular expression to search for
     * @param attachmentIndex   the index of the attachment in the MIME structure (regular parts are skipped)
     */
    @PublicAtsApi
    public void checkRegexInAttachment(
                                        String searchRegex,
                                        int attachmentIndex ) {

        checkRegexInAttachment(searchRegex, attachmentIndex, new int[0]);
    }

    /**
     * Check that the nested IMAP message contains the specified regular expression in the given attachment part
     *
     * @param searchRegex       the regular expression to search for
     * @param attachmentIndex   the index of the attachment in the MIME structure (regular parts are skipped)
     * @param nestedPackagePath path to the nested message
     */
    @PublicAtsApi
    public void checkRegexInAttachment(
                                        String searchRegex,
                                        int attachmentIndex,
                                        int... nestedPackagePath ) {

        //create the rule
        StringInMimePartRule stringInPartRule = new StringInMimePartRule(nestedPackagePath,
                                                                         searchRegex,
                                                                         true,
                                                                         attachmentIndex,
                                                                         true,
                                                                         "checkRegexgInAttachment"
                                                                               + getNestedMimePackagePathDescription(nestedPackagePath),
                                                                         true);
        rootRule.addRule(stringInPartRule);
    }

    /**
     * Check that the IMAP message contains attachment with the specified name
     *
     * @param searchRegex  the regular expression to search for
     * @param attachmentIndex    the index of the attachment in the MIME structure (regular parts are skipped)
     */
    @PublicAtsApi
    public void checkRegexInAttachmentName(
                                            String searchRegex,
                                            int attachmentIndex ) {

        checkRegexInAttachmentName(searchRegex, attachmentIndex, new int[0]);
    }

    /**
     * Check that the IMAP message contains attachment with the the specified name
     *
     * @param searchRegex  the regular expression to search for
     * @param attachmentIndex   the index of the attachment in the MIME structure (regular parts are skipped)
     * @param nestedPackagePath path to the nested message
     */
    @PublicAtsApi
    public void checkRegexInAttachmentName(
                                            String searchRegex,
                                            int attachmentIndex,
                                            int... nestedPackagePath ) {

        //create the rule
        AttachmentNameRule attachmentNameRule = new AttachmentNameRule(nestedPackagePath,
                                                                       searchRegex,
                                                                       attachmentIndex,
                                                                       "checkRegexInAttachmentName"
                                                                                        + getNestedMimePackagePathDescription(nestedPackagePath),
                                                                       true);
        rootRule.addRule(attachmentNameRule);
    }

    /**
     * Check that the body of the message (including all parts) contains the specified string
     *
     * @param body  the string to search for
     */
    @PublicAtsApi
    public void checkBody(
                           String body ) {

        checkBody(body, new int[0]);
    }

    /**
     * Check that the body of the nested message (including all parts) contains the specified string
     *
     * @param body  the string to search for
     * @param nestedPackagePath path to the nested message
     */
    @PublicAtsApi
    public void checkBody(
                           String body,
                           int... nestedPackagePath ) {

        //create the rule
        StringInMimePartRule stringInPartRule = new StringInMimePartRule(nestedPackagePath,
                                                                         body,
                                                                         false,
                                                                         "checkBody"
                                                                                + getNestedMimePackagePathDescription(nestedPackagePath),
                                                                         true);
        rootRule.addRule(stringInPartRule);
    }

    /**
     * Check that the message contains a header with the specified value
     *
     * @param headerName    the name of the header
     * @param headerValue   the value to search for
     * @param searchWhere   type of matching to use
     */
    @PublicAtsApi
    public void checkHeader(
                             String headerName,
                             String headerValue,
                             HeaderMatchMode searchWhere ) {

        checkHeader(headerName, headerValue, searchWhere, new int[0]);
    }

    /**
     * Check that the nested message contains a header with the specified value
     *
     * @param headerName    the name of the header
     * @param headerValue   the value to search for
     * @param searchWhere   type of matching to use
     * @param nestedPackagePath path to the nested message
     */
    @PublicAtsApi
    public void checkHeader(
                             String headerName,
                             String headerValue,
                             HeaderMatchMode searchWhere,
                             int... nestedPackagePath ) {

        //create the rule
        HeaderRule stringInPartRule = new HeaderRule(nestedPackagePath,
                                                     headerName,
                                                     headerValue,
                                                     searchWhere,
                                                     "checkHeader"
                                                                  + getNestedMimePackagePathDescription(nestedPackagePath),
                                                     true);
        rootRule.addRule(stringInPartRule);
    }

    /**
     * Check that the message contains a header with the specified value
     * in one of its MIME parts
     *
     * @param headerName    the name of the header
     * @param headerValue   the value to search for
     * @param partIndex     the MIME part index
     * @param searchWhere   type of matching to use
     */
    @PublicAtsApi
    public void checkHeader(
                             String headerName,
                             String headerValue,
                             int partIndex,
                             HeaderMatchMode searchWhere ) {

        checkHeader(headerName, headerValue, partIndex, searchWhere, new int[0]);
    }

    /**
     * Check that the nested message contains a header with the specified value
     * in one of its MIME parts
     *
     * @param headerName    the name of the header
     * @param headerValue   the value to search for
     * @param partIndex     the MIME part index
     * @param searchWhere   type of matching to use
     * @param nestedPackagePath path to the nested message
     */
    @PublicAtsApi
    public void checkHeader(
                             String headerName,
                             String headerValue,
                             int partIndex,
                             HeaderMatchMode searchWhere,
                             int... nestedPackagePath ) {

        //create the rule
        HeaderRule stringInPartRule = new HeaderRule(nestedPackagePath,
                                                     headerName,
                                                     headerValue,
                                                     partIndex,
                                                     -1,
                                                     searchWhere,
                                                     "checkHeader"
                                                                  + getNestedMimePackagePathDescription(nestedPackagePath),
                                                     true);
        rootRule.addRule(stringInPartRule);
    }

    /**
     * Check that the message is tagged with the specified message tag
     *
     * @param messageTag
     */
    @PublicAtsApi
    public void checkHeaderForMessageTag(
                                          String messageTag ) {

        checkHeaderForMessageTag(messageTag, new int[0]);
    }

    /**
     * Check that the nested message is tagged with the specified message tag
     *
     * @param messageTag
     * @param nestedPackagePath path to the nested message
     */
    @PublicAtsApi
    public void checkHeaderForMessageTag(
                                          String messageTag,
                                          int... nestedPackagePath ) {

        //create the rule
        //set this rule with highest priority, so it is evaluated first
        HeaderRule stringInPartRule = new HeaderRule(nestedPackagePath,
                                                     "Automation-Message-Tag",
                                                     messageTag,
                                                     HeaderMatchMode.FIND,
                                                     "checkMessageTagHeader"
                                                                           + getNestedMimePackagePathDescription(nestedPackagePath),
                                                     true,
                                                     Integer.MIN_VALUE);
        rootRule.addRule(stringInPartRule);
    }

    /**
     * Check that the subject of the message contains the specified string
     *
     * @param subject       the string to search for
     * @param searchWhere   type of matching to use
     */
    @PublicAtsApi
    public void checkSubject(
                              String subject,
                              SubjectMatchMode searchWhere ) {

        checkSubject(subject, searchWhere, new int[0]);
    }

    /**
     * Check that the subject of the nested message contains the specified string
     *
     * @param subject       the string to search for
     * @param searchWhere   type of matching to use
     * @param nestedPackagePath path to the nested message
     */
    @PublicAtsApi
    public void checkSubject(
                              String subject,
                              SubjectMatchMode searchWhere,
                              int... nestedPackagePath ) {

        //create the rule
        SubjectRule subjectRule = new SubjectRule(nestedPackagePath,
                                                  subject,
                                                  searchWhere,
                                                  "checkSubject"
                                                               + getNestedMimePackagePathDescription(nestedPackagePath),
                                                  true);
        rootRule.addRule(subjectRule);
    }

    /**
     * Check that the subject of the message has the specified string
     *
     * @param subject       the expected subject
     */
    @PublicAtsApi
    public void checkSubject(
                              String subject ) {

        checkSubject(subject, new int[0]);
    }

    /**
     * Check that the subject of the nested message has the specified string
     *
     * @param subject       the expected subject
     * @param nestedPackagePath path to the nested message
     */
    @PublicAtsApi
    public void checkSubject(
                              String subject,
                              int... nestedPackagePath ) {

        //create the rule
        SubjectRule subjectRule = new SubjectRule(nestedPackagePath,
                                                  subject,
                                                  SubjectMatchMode.EQUALS,
                                                  "checkSubject"
                                                                           + getNestedMimePackagePathDescription(nestedPackagePath),
                                                  true);
        rootRule.addRule(subjectRule);
    }

    /**
     * Check the signature of a signed message using the embedded public keys.
     *
     */
    @PublicAtsApi
    public void checkSignature() {

        checkSignature(new int[0], null);
    }

    /**
     * Check the signature of a signed nested message using the embedded public keys.
     *
     * @param nestedPackagePath path to the nested message
     */
    @PublicAtsApi
    public void checkSignature(
                                int[] nestedPackagePath ) {

        checkSignature(nestedPackagePath, null);
    }

    /**
     * Check the signature of a signed message using the provided signer.
     *
     * @param signer the signer to use
     */
    @PublicAtsApi
    public void checkSignature(
                                PackageEncryptor signer ) {

        checkSignature(new int[0], signer);
    }

    /**
     * Check the signature of a signed nested message using the provided signer.
     *
     * @param nestedPackagePath path to the nested message
     * @param signer the signer to use
     */
    @PublicAtsApi
    public void checkSignature(
                                int[] nestedPackagePath,
                                PackageEncryptor signer ) {

        //create the rule
        SMimeSignatureRule subjectRule = new SMimeSignatureRule(nestedPackagePath,
                                                                signer,
                                                                "checkSignature"
                                                                        + getNestedMimePackagePathDescription(nestedPackagePath),
                                                                true);
        rootRule.addRule(subjectRule);
    }

    /**
     * Verify the IMAP folder contains a message with the specified properties.
     * <br><b>Note:</b> It returns the matched message in case user wants to extract some data from it.
     * The returned MimePackage object is somewhat complex, but it provides all needed methods to extract data from the message.
     *
     * <li> At the first successful poll - it will succeed
     * <li> At unsuccessful poll - it will retry until all polling attempts are over
     *
     * @return the matched MIME Package
     * @throws RbvException
     */
    @PublicAtsApi
    public MimePackage verifyMessageExists() throws RbvException {

        List<MetaData> matchedMetaData = verifyExists();

        // At this place we know the verification succeeded and there is exactly one MimePackage matched
        return ((ImapMetaData) matchedMetaData.get(0)).getMimePackage();
    }

    /**
     * Verify the IMAP folder for the user does not contain a message
     * with the specified properties
     *
     * <li> At the first successful poll - it will succeed
     * <li> At unsuccessful poll - it will retry until all polling attempts are over
     *
     * @throws RbvException
     */
    @PublicAtsApi
    public void verifyMessageDoesNotExist() throws RbvException {

        verifyDoesNotExist();
    }

    /**
     * Verify the IMAP folder contains a message with the specified properties for the whole polling duration.
     * <br><b>Note:</b> It returns the matched message in case user wants to extract some data from it.
     * The returned MimePackage object is somewhat complex, but it provides all needed methods to extract data from the message.
     *
     * <li> At successful poll - it will retry until all polling attempts are over
     * <li> At unsuccessful poll - it will fail
     *
     * @return the matched MIME Package
     * @throws RbvException
     */
    @PublicAtsApi
    public MimePackage verifyMessageAlwaysExists() throws RbvException {

        List<MetaData> matchedMetaData = verifyAlwaysExists();

        // At this place we know the verification succeeded and there is exactly one MimePackage matched
        return ((ImapMetaData) matchedMetaData.get(0)).getMimePackage();
    }

    /**
     * Verify the IMAP folder for the user does not contain a message
     * with the specified properties for the whole polling duration
     *
     * <li> At successful poll - it will retry until all polling attempts are over
     * <li> At unsuccessful poll - it will fail
     *
     * @throws RbvException
     */
    @PublicAtsApi
    public void verifyMessageNeverExists() throws RbvException {

        verifyNeverExists();
    }

    @Override
    protected String getMonitorName() {

        return monitorName;
    }

    private String getNestedMimePackagePathDescription(
                                                        int[] nestedPackagePath ) {

        if (nestedPackagePath == null || nestedPackagePath.length == 0) {
            return "";
        } else {
            return " in nested MIME package " + Arrays.toString(nestedPackagePath);
        }
    }
}
