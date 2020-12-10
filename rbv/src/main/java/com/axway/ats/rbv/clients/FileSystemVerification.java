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

import java.io.File;
import java.util.List;

import com.axway.ats.action.objects.FilePackage;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.validation.Validate;
import com.axway.ats.core.validation.ValidationType;
import com.axway.ats.core.validation.Validator;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.executors.MetaExecutor;
import com.axway.ats.rbv.filesystem.FileSystemFolderSearchTerm;
import com.axway.ats.rbv.filesystem.FileSystemMetaData;
import com.axway.ats.rbv.filesystem.FileSystemStorage;
import com.axway.ats.rbv.filesystem.rules.FileContentRule;
import com.axway.ats.rbv.filesystem.rules.FileFolderRule;
import com.axway.ats.rbv.filesystem.rules.FileGidRule;
import com.axway.ats.rbv.filesystem.rules.FileGroupNameRule;
import com.axway.ats.rbv.filesystem.rules.FileMd5Rule;
import com.axway.ats.rbv.filesystem.rules.FileModtimeRule;
import com.axway.ats.rbv.filesystem.rules.FileOwnerNameRule;
import com.axway.ats.rbv.filesystem.rules.FilePermRule;
import com.axway.ats.rbv.filesystem.rules.FileSizeRule;
import com.axway.ats.rbv.filesystem.rules.FileUidRule;
import com.axway.ats.rbv.model.RbvException;

/**
 * Class used for verifications of file attributes (remote or local).
 * <p>Note that <code>check</code> methods add rules for match whereas actual rules
 * evaluation is done in one of the methods with <code>verify</code> prefix.</p>
 *
 * <br>
 * <b>User guide</b> pages related to this class:<br>
 * <a href="https://axway.github.io/ats-framework/Common-test-verifications.html">RBV basics</a>
 * and
 * <a href="https://axway.github.io/ats-framework/File-system-verifications.html">File system verification details</a>
*/
@PublicAtsApi
public class FileSystemVerification extends VerificationSkeleton {

    private String atsAgent;
    private String directory;

    /**
     * Create a file system verification component working locally
     *
     * @param fileOrFolderPath  the full path to the file or folder to check
     */
    @PublicAtsApi
    public FileSystemVerification( String fileOrFolderPath ) {

        this(null, fileOrFolderPath);
    }

    /**
     * Create a file system verification component working locally
     *
     * @param directory         the directory to check in
     * @param fileOrFolderName  the file or folder name to check for
     * @param isRegEx           if true <b>fileName</b> will be treated as regular expression
     */
    @PublicAtsApi
    public FileSystemVerification( String directory, String fileOrFolderName, boolean isRegEx ) {

        this(null, directory, fileOrFolderName, isRegEx);
    }

    /**
     * Create a file system verification component working remotely
     *
     * @param atsAgent          the address of the remote ATS agent which will run the operations
     * @param fileOrFolderPath  the full path to the file or folder to check
     */
    @PublicAtsApi
    public FileSystemVerification( @Validate( name = "atsAgent", type = ValidationType.NONE) String atsAgent,
                                   @Validate( name = "fileOrFolderPath", type = ValidationType.STRING_NOT_EMPTY) String fileOrFolderPath ) {

        super();

        // validate input parameters
        atsAgent = HostUtils.getAtsAgentIpAndPort(atsAgent);
        new Validator().validateMethodParameters(new Object[]{ atsAgent, fileOrFolderPath });

        File targetFileOrFolder = new File(fileOrFolderPath);

        this.atsAgent = atsAgent;
        this.directory = targetFileOrFolder.getParent();

        // get the folder
        FileSystemStorage storage;
        if (this.atsAgent == null) {
            storage = new FileSystemStorage();
        } else {
            storage = new FileSystemStorage(this.atsAgent);
        }
        folder = storage.getFolder(new FileSystemFolderSearchTerm(directory, targetFileOrFolder.getName(),
                                                                  false, false));
        this.executor = new MetaExecutor();
    }

    /**
     * Create a file system verification component working remotely
     *
     * @param atsAgent          the address of the remote ATS agent which will run the operations
     * @param directory         the directory to check in
     * @param fileOrFolderName  the file or folder name to check for
     * @param isRegEx           if true <b>fileName</b> will be treated as regular expression
     */
    @PublicAtsApi
    public FileSystemVerification( @Validate( name = "atsAgent", type = ValidationType.NONE) String atsAgent,
                                   @Validate( name = "directory", type = ValidationType.STRING_NOT_EMPTY) String directory,
                                   @Validate( name = "fileOrFolderName", type = ValidationType.STRING_NOT_EMPTY) String fileOrFolderName,
                                   @Validate( name = "isRegEx", type = ValidationType.NONE) boolean isRegEx ) {

        super();

        if (fileOrFolderName == null) {
            fileOrFolderName = ".*";
            isRegEx = true;
        }

        // validate input parameters
        atsAgent = HostUtils.getAtsAgentIpAndPort(atsAgent);
        new Validator().validateMethodParameters(new Object[]{ atsAgent, directory, fileOrFolderName,
                                                               isRegEx });

        this.atsAgent = atsAgent;
        this.directory = directory;

        FileSystemStorage storage;
        if (this.atsAgent == null) {
            storage = new FileSystemStorage();
        } else {
            storage = new FileSystemStorage(this.atsAgent);
        }
        folder = storage.getFolder(new FileSystemFolderSearchTerm(directory, fileOrFolderName, isRegEx,
                                                                  false));

        this.executor = new MetaExecutor();
    }

    /**
     * Add rule to check that the size of the received file is the same
     * as the size of the source file
     *
     * @param srcAtsAgent       the remote ATS agent address on which the source file is located
     * @param srcFile           the full name of the file
     * @throws RbvException     thrown on error
     */
    @PublicAtsApi
    public void checkSize( String srcAtsAgent, String srcFile ) throws RbvException {

        FileSizeRule rule = new FileSizeRule(srcAtsAgent, srcFile, "checkSize", true);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check if the file contains (or does not contain) the specified expression.
     * Accepts both regular expressions and simple strings.
     *
     * @param expression the expression to look for
     * @param isRegularExpression true if the expression is a regular expression
     * @param expectedResult whether or not we would expect the expression to be present in the file
     * @throws RbvException thrown on error
     */
    @PublicAtsApi
    public void checkContents( String expression, boolean isRegularExpression,
                               boolean expectedResult ) throws RbvException {

        FileContentRule rule = new FileContentRule(expression, "checkContents", isRegularExpression,
                                                   expectedResult);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check if the file is formatted in the way that ASCII armor files are
     * formatted. This would mean that it should have the following properties:
     * <ul>
     *  <li> a "-----BEGIN PGP MESSAGE-----" message </li>
     *  <li> no line longer than 65 characters </li>
     *  <li> a "-----END PGP MESSAGE-----" message </li>
     * <ul>
     *
     * <b>The way this method checks the file is not 100% sure. The file could not
     * be encrypted at all or the contents might be garbled - it only checks if
     * the two strings are present and that each line is shorter than 65 characters.
     * However this solution should work for most of the cases</b>
     *
     * @param isAsciiArmour true if we would expect the file to be formatted this way
     * @throws RbvException thrown on error
     */
    @PublicAtsApi
    public void checkAsciiArmor( boolean isAsciiArmour ) throws RbvException {

        // check if the file has a "-----BEGIN PGP MESSAGE-----" message
        checkContents("-----BEGIN PGP MESSAGE-----", false, isAsciiArmour);

        // check if the file has a line longer than 65 characters
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < 65; i++) {
            buffer.append(".");
        }
        checkContents(buffer.toString(), true, !isAsciiArmour);

        // check if the file has a "-----END PGP MESSAGE-----" message
        checkContents("-----END PGP MESSAGE-----", false, isAsciiArmour);
    }

    /**
     * Add rule to check that the size of the received file is different
     * than the size of the source file
     *
     * @param srcAtsAgent       the remote ATS agent address on which the source file is located
     * @param srcFile           the full name of the file
     * @throws RbvException     thrown on error
     */
    @PublicAtsApi
    public void checkSizeDifferent( String srcAtsAgent, String srcFile ) throws RbvException {

        FileSizeRule rule = new FileSizeRule(srcAtsAgent, srcFile, "checkSizeDifferent", false);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check that the size of the received file is the same
     * as the given one
     *
     * @param size
     */
    @PublicAtsApi
    public void checkSize( long size ) {

        FileSizeRule rule = new FileSizeRule(size, "checkSize", true);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check that the size of the received file is different
     * than the given one
     *
     * @param size
     */
    @PublicAtsApi
    public void checkSizeDifferent( long size ) {

        FileSizeRule rule = new FileSizeRule(size, "checkSizeDifferent", false);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check that the last modification time of the received file is the same
     * as the last modification time of the source file
     *
     * @param srcAtsAgent       the remote ATS agent address on which the source file is located
     * @param srcFile           the full name of the file
     * @throws RbvException     thrown on error
     */
    @PublicAtsApi
    public void checkModificationTime( String srcAtsAgent, String srcFile ) throws RbvException {

        FileModtimeRule rule = new FileModtimeRule(srcAtsAgent, srcFile, "checkModificationTime", true);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check that the last modification time of the received file is different
     * than the last modification time of the source file
     *
     * @param srcAtsAgent       the remote ATS agent address on which the source file is located
     * @param srcFile           the full name of the file
     * @throws RbvException     thrown on error
     */
    @PublicAtsApi
    public void checkModificationTimeDifferent( String srcAtsAgent, String srcFile ) throws RbvException {

        FileModtimeRule rule = new FileModtimeRule(srcAtsAgent, srcFile, "checkModificationTimeDifferent",
                                                   false);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check that the last modification time of the received file is the same
     * as the given one
     *
     *  @param modTime
     */
    @PublicAtsApi
    public void checkModificationTime( long modTime ) {

        FileModtimeRule rule = new FileModtimeRule(modTime, "checkModificationTime", true);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check that the last modification time of the received file is different
     * than the given one
     *
     *  @param modTime
     */
    @PublicAtsApi
    public void checkModificationTimeDifferent( long modTime ) {

        FileModtimeRule rule = new FileModtimeRule(modTime, "checkModificationTimeDifferent", false);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check that the owner name of the received file is the same
     * as the owner name of the source file (NOT SUPPORTED ON WINDOWS)
     * 
     * @param owner             the owner name
     * @throws RbvException     thrown on error
     */
    @PublicAtsApi
    public void checkOwnerName( String owner ) throws RbvException {

        FileOwnerNameRule rule = new FileOwnerNameRule(owner, "checkOwnerName", true);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check that the group name of the received file is the same
     * as the group name of the source file (NOT SUPPORTED ON WINDOWS)
     * 
     * @param group             the group name
     * @throws RbvException     thrown on error
     */
    @PublicAtsApi
    public void checkGroupName( String group ) throws RbvException {

        FileGroupNameRule rule = new FileGroupNameRule(group, "checkGroupName", true);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check that the UID of the received file is the same
     * as the UID of the source file (NOT SUPPORTED ON WINDOWS)
     *
     * @param srcAtsAgent       the remote ATS agent address on which the source file is located
     * @param srcFile           the full name of the file
     * @throws RbvException     thrown on error
     */
    @PublicAtsApi
    public void checkUID( String srcAtsAgent, String srcFile ) throws RbvException {

        FileUidRule rule = new FileUidRule(srcAtsAgent, srcFile, "checkUID", true);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check that the UID of the received file is different
     * than the UID of the source file (NOT SUPPORTED ON WINDOWS)
     *
     * @param srcAtsAgent       the remote ATS agent address on which the source file is located
     * @param srcFile           the full name of the file
     * @throws RbvException     thrown on error
     */
    @PublicAtsApi
    public void checkUIDDifferent( String srcAtsAgent, String srcFile ) throws RbvException {

        FileUidRule rule = new FileUidRule(srcAtsAgent, srcFile, "checkUIDDifferent", false);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check that the UID of the received file is the same
     * as the given one (NOT SUPPORTED ON WINDOWS)
     *
     *  @param uid
     */
    @PublicAtsApi
    public void checkUID( long uid ) {

        FileUidRule rule = new FileUidRule(uid, "checkUID", true);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check that the UID of the received file is different
     * than the given one (NOT SUPPORTED ON WINDOWS)
     *
     *  @param uid
     */
    @PublicAtsApi
    public void checkUIDDifferent( long uid ) {

        FileUidRule rule = new FileUidRule(uid, "checkUIDDifferent", false);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check that the GID of the received file is the same
     * as the GID of the source file (NOT SUPPORTED ON WINDOWS)
     *
     * @param srcAtsAgent       the remote ATS agent address on which the source file is located
     * @param srcFile           the full name of the file
     * @throws RbvException     thrown on error
     */
    @PublicAtsApi
    public void checkGID( String srcAtsAgent, String srcFile ) throws RbvException {

        FileGidRule rule = new FileGidRule(srcAtsAgent, srcFile, "checkGID", true);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check that the GID of the received file is different
     * than the GID of the source file (NOT SUPPORTED ON WINDOWS)
     *
     * @param srcAtsAgent       the remote ATS agent address on which the source file is located
     * @param srcFile           the full name of the file
     * @throws RbvException     thrown on error
     */
    @PublicAtsApi
    public void checkGIDDifferent( String srcAtsAgent, String srcFile ) throws RbvException {

        FileGidRule rule = new FileGidRule(srcAtsAgent, srcFile, "checkGIDDifferent", false);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check that the GID of the received file is the same
     * as the given one (NOT SUPPORTED ON WINDOWS)
     *
     *  @param gid
     */
    @PublicAtsApi
    public void checkGID( long gid ) {

        FileGidRule rule = new FileGidRule(gid, "checkGID", true);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check that the UID of the received file is different
     * than the given one (NOT SUPPORTED ON WINDOWS)
     *
     *  @param gid
     */
    @PublicAtsApi
    public void checkGIDDifferent( long gid ) {

        FileGidRule rule = new FileGidRule(gid, "checkGIDDifferent", false);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check that the permissions of the received file are the same
     * as the permissions of the source file (NOT SUPPORTED ON WINDOWS)
     *
     * @param srcAtsAgent       the remote ATS agent address on which the source file is located
     * @param srcFile           the full name of the file
     * @throws RbvException     thrown on error
     */
    @PublicAtsApi
    public void checkPermissions( String srcAtsAgent, String srcFile ) throws RbvException {

        FilePermRule rule = new FilePermRule(srcAtsAgent, srcFile, "checkPermissions", true);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check that the permissions of the received file are different
     * than the permissions of the source file (NOT SUPPORTED ON WINDOWS)
     *
     * @param srcAtsAgent       the remote ATS agent address on which the source file is located
     * @param srcFile           the full name of the file
     * @throws RbvException     thrown on error
     */
    @PublicAtsApi
    public void checkPermissionsDifferent( String srcAtsAgent, String srcFile ) throws RbvException {

        FilePermRule rule = new FilePermRule(srcAtsAgent, srcFile, "checkPermissionsDifferent", false);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check that the permissions of the received file are the same
     * as the given ones (NOT SUPPORTED ON WINDOWS)
     *
     *  @param permissions
     */
    @PublicAtsApi
    public void checkPermissions( long permissions ) {

        FilePermRule rule = new FilePermRule(permissions, "checkPermissions", true);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check that the permissions of the received file are different
     * as the given ones (NOT SUPPORTED ON WINDOWS)
     *
     *  @param permissions
     */
    @PublicAtsApi
    public void checkPermissionsDifferent( long permissions ) {

        FilePermRule rule = new FilePermRule(permissions, "checkPermissionsDifferent", false);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check that the MD5 sum of the received file is the same
     * as the MD5 sum of the source file
     *
     * @param srcAtsAgent       the remote ATS agent address on which the source file is located
     * @param srcFile           the full name of the file
     * @throws RbvException     thrown on error
     */
    @PublicAtsApi
    public void checkMd5( String srcAtsAgent, String srcFile ) throws RbvException {

        FileMd5Rule rule = new FileMd5Rule(srcAtsAgent, srcFile, "checkMd5", true);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check that the MD5 sum of the received file is the same
     * as the MD5 sum of the source file in the selected mode - binary or ASCII
     *
     * @param srcAtsAgent       the remote ATS agent address on which the source file is located
     * @param srcFile           the full name of the file
     * @param binaryMode        true to check in binary mode, false to check in ASCII mode
     * @throws RbvException     thrown on error
     */
    @PublicAtsApi
    public void checkMd5( String srcAtsAgent, String srcFile, boolean binaryMode ) throws RbvException {

        FileMd5Rule rule = new FileMd5Rule(srcAtsAgent, srcFile, binaryMode, "checkMd5", true);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check that the MD5 sum of the received file is different
     * than the MD5 sum of the source file
     *
     * @param srcAtsAgent       the remote ATS agent address on which the source file is located
     * @param srcFile           the full name of the file
     * @throws RbvException     thrown on error
     */
    @PublicAtsApi
    public void checkMd5Different( String srcAtsAgent, String srcFile ) throws RbvException {

        FileMd5Rule rule = new FileMd5Rule(srcAtsAgent, srcFile, "checkMd5Different", false);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check that the MD5 sum of the received file is different
     * than the MD5 sum of the source file in the selected mode - binary or ASCII
     *
     * @param srcAtsAgent       the remote ATS agent address on which the source file is located
     * @param srcFile           the full name of the file
     * @param binaryMode        true to check in binary mode, false to check in ASCII mode
     * @throws RbvException     thrown on error
     */
    @PublicAtsApi
    public void checkMd5Different( String srcAtsAgent, String srcFile,
                                   boolean binaryMode ) throws RbvException {

        FileMd5Rule rule = new FileMd5Rule(srcAtsAgent, srcFile, binaryMode, "checkMd5Different", false);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check that the MD5 sum of the received file is the same
     * as the given one
     *
     *  @param md5
     */
    @PublicAtsApi
    public void checkMd5( String md5 ) {

        FileMd5Rule rule = new FileMd5Rule(md5, "checkMd5", true);
        rootRule.addRule(rule);
    }

    /**
     * Add rule to check that the MD5 sum of the received file is different
     * as the given one
     *
     *  @param md5
     */
    @PublicAtsApi
    public void checkMd5Different( String md5 ) {

        FileMd5Rule rule = new FileMd5Rule(md5, "checkMd5Different", false);
        rootRule.addRule(rule);
    }

    /**
     * Verify the file with the selected properties (all check rules) exists
     *
     * <li> At the first successful poll - it will succeed
     * <li> At unsuccessful poll - it will retry until all polling attempts are over
     *
     * @return array with info about matched file(s)
     * @throws RbvException
     */
    @PublicAtsApi
    public FilePackage[] verifyFileExists() throws RbvException {

        addFileCheckRule();

        List<MetaData> matchedMetaData = verifyExists();

        FilePackage[] matchedFilePackages = new FilePackage[matchedMetaData.size()];
        for (int i = 0; i < matchedMetaData.size(); i++) {
            matchedFilePackages[i] = ((FileSystemMetaData) matchedMetaData.get(i)).getFilePackage();
        }

        return matchedFilePackages;
    }

    /**
     * Verify the file with the selected properties (all check rules) does not exist
     *
     * <li> At the first successful poll - it will succeed
     * <li> At unsuccessful poll - it will retry until all polling attempts are over
     *
     * @throws RbvException
     */
    @PublicAtsApi
    public void verifyFileDoesNotExist() throws RbvException {

        addFileCheckRule();

        verifyDoesNotExist();
    }

    /**
     * Verify the file with the selected properties (all check rules) exists for
     * the whole polling duration
     *
     * <li> At successful poll - it will retry until all polling attempts are over
     * <li> At unsuccessful poll - it will fail
     *
     * @return array with info about matched file(s)
     * @throws RbvException
     */
    @PublicAtsApi
    public FilePackage[] verifyFileAlwaysExists() throws RbvException {

        addFileCheckRule();

        List<MetaData> matchedMetaData = verifyAlwaysExists();

        FilePackage[] matchedFilePackages = new FilePackage[matchedMetaData.size()];
        for (int i = 0; i < matchedMetaData.size(); i++) {
            matchedFilePackages[i] = ((FileSystemMetaData) matchedMetaData.get(i)).getFilePackage();
        }

        return matchedFilePackages;
    }

    /**
     * Verify the file with the selected properties (all check rules) does not
     * exist for the whole polling duration
     *
     * <li> At successful poll - it will retry until all polling attempts are over
     * <li> At unsuccessful poll - it will fail
     *
     * @throws RbvException
     */
    @PublicAtsApi
    public void verifyFileNeverExist() throws RbvException {

        addFileCheckRule();

        verifyNeverExists();
    }

    /**
     * Verify the folder with the selected properties (all check rules) exists
     *
     * <li> At the first successful poll - it will succeed
     * <li> At unsuccessful poll - it will retry until all polling attempts are over
     *
     * @return array with info about matched folder(s)
     * @throws RbvException
     */
    @PublicAtsApi
    public FilePackage[] verifyFolderExists() throws RbvException {

        addFolderCheckRule();

        List<MetaData> matchedMetaData = verifyExists();

        FilePackage[] matchedFilePackages = new FilePackage[matchedMetaData.size()];
        for (int i = 0; i < matchedMetaData.size(); i++) {
            matchedFilePackages[i] = ((FileSystemMetaData) matchedMetaData.get(i)).getFilePackage();
        }

        return matchedFilePackages;
    }

    /**
     * Verify the folder with the selected properties (all check rules) does not exist
     *
     * <li> At the first successful poll - it will succeed
     * <li> At unsuccessful poll - it will retry until all polling attempts are over
     *
     * @throws RbvException
     */
    @PublicAtsApi
    public void verifyFolderDoesNotExist() throws RbvException {

        addFolderCheckRule();

        verifyDoesNotExist();
    }

    /**
     * Verify the folder with the selected properties (all check rules) exists
     * for the whole polling duration
     *
     * <li> At successful poll - it will retry until all polling attempts are over
     * <li> At unsuccessful poll - it will fail
     *
     * @return array with info about matched folder(s)
     * @throws RbvException
     */
    @PublicAtsApi
    public FilePackage[] verifyFolderAlwaysExists() throws RbvException {

        addFolderCheckRule();

        List<MetaData> matchedMetaData = verifyAlwaysExists();

        FilePackage[] matchedFilePackages = new FilePackage[matchedMetaData.size()];
        for (int i = 0; i < matchedMetaData.size(); i++) {
            matchedFilePackages[i] = ((FileSystemMetaData) matchedMetaData.get(i)).getFilePackage();
        }

        return matchedFilePackages;
    }

    /**
     * Verify the folder with the selected properties (all check rules) does not
     * exist for the whole polling duration
     *
     * <li> At successful poll - it will retry until all polling attempts are over
     * <li> At unsuccessful poll - it will fail
     *
     * @throws RbvException
     */
    @PublicAtsApi
    public void verifyFolderNeverExist() throws RbvException {

        addFolderCheckRule();

        verifyNeverExists();
    }

    @Override
    protected String getMonitorName() {

        if (this.atsAgent == null) {
            return "local_file_monitor";
        }
        return "file_monitor_" + this.atsAgent;
    }

    private void addFileCheckRule() {

        // set the second highest priority for this rule - if the file path is correct the second most
        // important thing is to check if the entity is a file
        FileFolderRule rule = new FileFolderRule(true, "checkIsFile", true, Integer.MIN_VALUE);
        rootRule.addRule(rule);
    }

    private void addFolderCheckRule() {

        // set the second highest priority for this rule - if the folder path is correct the second most
        // important thing is to check if the entity is a folder
        FileFolderRule rule = new FileFolderRule(false, "checkIsDirectory", true, Integer.MIN_VALUE);
        rootRule.addRule(rule);
    }
}
