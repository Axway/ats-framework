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
package com.axway.ats.action.filesystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.filesystem.EndOfLineStyle;
import com.axway.ats.common.filesystem.FileMatchInfo;
import com.axway.ats.common.filesystem.FileSystemOperationException;
import com.axway.ats.common.filesystem.FileTailInfo;
import com.axway.ats.common.filesystem.Md5SumMode;
import com.axway.ats.core.filesystem.LocalFileSystemOperations;
import com.axway.ats.core.filesystem.model.IFileSystemOperations;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.validation.Validate;
import com.axway.ats.core.validation.ValidationType;
import com.axway.ats.core.validation.Validator;

/**
 * Operations on the file system.
 * If an ATS Agent is given (by the appropriate constructor), we are working remotely.
 *
 * <br/><br/>Note: On error all methods in this class are likely to throw FileSystemOperationException
 *
 * <br/><br/>
 * <b>User guide</b>
 * <a href="https://axway.github.io/ats-framework/File-System-Operations.html">page</a>
 * related to this class
 */
@PublicAtsApi
public class FileSystemOperations {

    private static final Logger        log                      = Logger.getLogger(FileSystemOperations.class);

    private static final String        LOCAL_HOST_NAME_AND_PORT = HostUtils.LOCAL_HOST_NAME + ":0000";

    private String                     atsAgent;

    /**
     * A map with file markers. Contains [fileName, lastFileReadPosition] pairs
     */
    private Map<String, FileMatchInfo> savedFileMatchDetails    = new HashMap<String, FileMatchInfo>();

    private boolean                    failOnError              = true;

    /**
     * Constructor when working on the local host
     */
    @PublicAtsApi
    public FileSystemOperations() {

    }

    /**
     * Constructor when working on a remote host
     *
     * @param atsAgent the address of the remote ATS agent which will run the operation
     * <p>
     *    <b>Note:</b> If you want to specify port to IPv6 address, the supported format is: <i>[IP]:PORT</i>
     * </p>
     */
    @PublicAtsApi
    public FileSystemOperations( @Validate( name = "atsAgent", type = ValidationType.STRING_SERVER_WITH_PORT) String atsAgent ) {

        // validate input parameters
        atsAgent = HostUtils.getAtsAgentIpAndPort(atsAgent);
        new Validator().validateMethodParameters(new Object[]{ atsAgent });

        this.atsAgent = atsAgent;
    }

    /**
     * Creates a binary file. The content of the file is a byte sequence. The bytes
     * themselves are either fixed sequence, or randomly generated, depending on the
     * value of the randomContent parameter.
     * </br>File's UID and GID will the ones of the system user which started the remote ATS agent.
     *
     * @param filePath the file to work with
     * @param size the size of the generated file
     * @param randomContent if true the method would generate a file with a random content
     */
    @PublicAtsApi
    public void createBinaryFile(

                                  @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath,
                                  @Validate( name = "size", type = ValidationType.NUMBER_POSITIVE) long size,
                                  boolean randomContent ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath, size, randomContent });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.createBinaryFile(filePath, size, randomContent);

        // log the result of the operation
        String message = new StringBuilder().append("Successfully created binary file by the name of ")
                                            .append(filePath)
                                            .append(" with size ")
                                            .append(size)
                                            .toString();
        log.info(message);
    }

    /**
     * Creates a binary file. The content of the file is a byte sequence. The bytes
     * themselves are either fixed sequence, or randomly generated, depending on the
     * value of the randomContent parameter.
     *
     * @param filePath the file to work with
     * @param size the size of the generated file
     * @param userId the identification number of the user this file should belong to
     * @param groupId the identification number of the group this file should belong to
     * @param randomContent if true the method would generate a file with a random content
     */
    @PublicAtsApi
    public void createBinaryFile(

                                  @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath,
                                  @Validate( name = "size", type = ValidationType.NUMBER_POSITIVE) long size,
                                  @Validate( name = "userId", type = ValidationType.NUMBER_POSITIVE) int userId,
                                  @Validate( name = "groupId", type = ValidationType.NUMBER_POSITIVE) int groupId,
                                  boolean randomContent ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath, size, userId, groupId,
                                                               randomContent });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.createBinaryFile(filePath, size, userId, groupId, randomContent);

        // log the result of the operation
        String message = new StringBuilder().append("Successfully created binary file by the name of ")
                                            .append(filePath)
                                            .append(" with size ")
                                            .append(size)
                                            .append(" and UID/GID ")
                                            .append(userId)
                                            .append("/")
                                            .append(groupId)
                                            .toString();
        log.info(message);
    }

    /**
     * Creates a file. The content is set by the user.
     *
     * @param filePath the file to work with
     * @param fileContent the text that will be parsed in the file
     */
    @PublicAtsApi
    public void createFile( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath,
                            @Validate( name = "fileContent", type = ValidationType.NOT_NULL) String fileContent ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath, fileContent });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.createFile(filePath, fileContent);

        // log the result of the operation
        log.info("Successfully created file by the name of " + filePath + " with content " + fileContent);
    }

    /**
     * Creates a file. The content of the file is the letters of the English alphabet.
     * The letters themselves are either alphabetically sorted, or randomly generated,
     * depending on the value of the randomContent parameter.
     * The end of line character will be the default one for the system where this action is run.
     *
     * @param filePath the file to work with
     * @param size the size of the generated file
     * @param randomContent if true the method would generate a file with a random content
     */
    @PublicAtsApi
    public void createFile( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath,
                            @Validate( name = "size", type = ValidationType.NUMBER_POSITIVE) long size,
                            boolean randomContent ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath, size, randomContent });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.createFile(filePath, size, randomContent);

        // log the result of the operation
        log.info("Successfully created file by the name of " + filePath + " with size " + size);
    }

    /**
     * Creates a file. The content of the file is the letters of the English alphabet.
     * The letters themselves are either alphabetically sorted, or randomly generated,
     * depending on the value of the randomContent parameter.
     *
     * @param filePath the file to work with
     * @param size the size of the generated file
     * @param eolStyle the EOL style for this file. If null it uses the EOL style of the current system
     * @param randomContent if true the method would generate a file with a random content
     */
    @PublicAtsApi
    public void createFile( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath,
                            @Validate( name = "size", type = ValidationType.NUMBER_POSITIVE) long size,
                            EndOfLineStyle eolStyle, boolean randomContent ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath, size, null, randomContent });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.createFile(filePath, size, randomContent, eolStyle);

        // log the result of the operation
        log.info("Successfully created file by the name of " + filePath + " with size " + size);
    }

    /**
     * Creates a file. The content is set by the user.
     *
     * @param filePath the file to work with
     * @param fileContent the text that will be parsed in the file
     * @param userId the identification number of the user this file should belong to
     * @param groupId the identification number of the group this file should belong to
     */
    @PublicAtsApi
    public void createFile( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath,
                            @Validate( name = "fileContent", type = ValidationType.NOT_NULL) String fileContent,
                            @Validate( name = "userId", type = ValidationType.NUMBER_POSITIVE) int userId,
                            @Validate( name = "groupId", type = ValidationType.NUMBER_POSITIVE) int groupId ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath, fileContent, userId, groupId });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.createFile(filePath, fileContent, userId, groupId);

        // log the result of the operation
        String message = new StringBuilder().append("Successfully created file by the name of ")
                                            .append(filePath)
                                            .append(" with content ")
                                            .append(fileContent)
                                            .append(" and UID/GID ")
                                            .append(userId)
                                            .append("/")
                                            .append(groupId)
                                            .toString();
        log.info(message);
    }

    /**
     * Creates a file. The content of the file is the letters of the English alphabet.
     * The letters themselves are either alphabetically sorted, or randomly generated,
     * depending on the value of the randomContent parameter.
     *
     * @param filePath the file to work with
     * @param size the size of the generated file
     * @param userId the identification number of the user this file should belong to
     * @param groupId the identification number of the group this file should belong to
     * @param eolStyle the EOL style for this file. If null it uses the EOL style of the current system
     * @param randomContent if true, the method would generate a file with a random content
     */
    @PublicAtsApi
    public void createFile( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath,
                            @Validate( name = "size", type = ValidationType.NUMBER_POSITIVE) long size,
                            @Validate( name = "userId", type = ValidationType.NUMBER_POSITIVE) int userId,
                            @Validate( name = "groupId", type = ValidationType.NUMBER_POSITIVE) int groupId,
                            EndOfLineStyle eolStyle, boolean randomContent ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath, size, userId, groupId, null,
                                                               randomContent });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.createFile(filePath, size, userId, groupId, randomContent, eolStyle);

        // log the result of the operation
        String message = new StringBuilder().append("Successfully created file by the name of ")
                                            .append(filePath)
                                            .append(" with size ")
                                            .append(size)
                                            .append(" and UID/GID ")
                                            .append(userId)
                                            .append("/")
                                            .append(groupId)
                                            .toString();
        log.info(message);
    }

    /**
     * Creates a file. The content of the file is the letters of the English alphabet.
     * The letters themselves are either alphabetically sorted, or randomly generated,
     * depending on the value of the randomContent parameter.
     *
     * @param filePath the file to work with
     * @param size the size of the generated file
     * @param userId the identification number of the user this file should belong to
     * @param groupId the identification number of the group this file should belong to
     * @param randomContent if true, the method would generate a file with a random content
     */
    @PublicAtsApi
    public void createFile( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath,
                            @Validate( name = "size", type = ValidationType.NUMBER_POSITIVE) long size,
                            @Validate( name = "userId", type = ValidationType.NUMBER_POSITIVE) int userId,
                            @Validate( name = "groupId", type = ValidationType.NUMBER_POSITIVE) int groupId,
                            boolean randomContent ) {

        createFile(filePath, size, userId, groupId, null, randomContent);
    }

    /**
     * Appends content to existing file.
     * It simply appends the provided bytes to the file.
     * It does not touch the new line characters in the new content.
     *
     * <br/><b>Note:</b> It will fail if the file does not exist
     *
     * @param filePath the file to work with
     * @param contentToAdd the content to add
     */
    @PublicAtsApi
    public void appendToFile( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath,
                              @Validate( name = "contentToAdd", type = ValidationType.STRING_NOT_EMPTY) String contentToAdd ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath, contentToAdd });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.appendToFile(filePath, contentToAdd);

        // log the result of the operation
        String message = new StringBuilder().append("Successfully appended ")
                                            .append(contentToAdd.length())
                                            .append(" characters to file ")
                                            .append(filePath)
                                            .toString();
        log.info(message);
    }

    /**
     * Get the last modification time for a specified file
     *
     * @param filePath the file to work with
     * @param modificationTime the modification time to set as a timestamp in milliseconds
     */
    @PublicAtsApi
    public long
            getFileModificationTime( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath });

        long lastModificationTime = -1l;

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        lastModificationTime = operations.getFileModificationTime(filePath);

        // log the result of the operation
        log.info("Successfully get the last modification timestamp of file '" + filePath + "'"
                 + getHostDescriptionSuffix());

        return lastModificationTime;
    }

    /**
     * Set the last modification time for a specified file
     *
     * @param filePath the file to work with
     * @param modificationTime the modification time to set as a timestamp in milliseconds
     */
    @PublicAtsApi
    public void
            setFileModificationTime( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath,
                                     @Validate( name = "modificationTime", type = ValidationType.NUMBER_POSITIVE) long modificationTime ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath, modificationTime });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.setFileModificationTime(filePath, modificationTime);

        // log the result of the operation
        log.info("Successfully set the last modification timestamp of file '" + filePath + "'"
                 + getHostDescriptionSuffix());
    }

    /**
     * @param filePath the file to work with
     * @return unique id formed by the file name, last modification time, GID and UID values,
     * separated with dots
     */
    public String
            getFileUniqueId( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        return operations.getFileUniqueId(filePath);
    }

    /**
     * Make a file hidden or not hidden. <br/>
     * On Unix systems a file is made hidden by inserting '.' in front of its name. <br/>
     * On Windows a file is made hidden by setting the appropriate file attribute.
     *
     * @param filePath the file to work with
     * @param hidden switch hidden/not hidden
     */
    @PublicAtsApi
    public void
            setFileHiddenAttribute( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath,
                                    boolean hidden ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath, hidden });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.setFileHiddenAttribute(filePath, hidden);

        // log the result of the operation
        log.info("Successfully made the " + filePath + " to be " + (hidden
                                                                           ? ""
                                                                           : "not ")
                 + "hidden" + getHostDescriptionSuffix());
    }

    /**
     * Define whether you want not to be thrown an exception,
     * if there is still a process writing in the file that
     * is being copied. <br />
     * It could be useful for log files. By default exception is thrown.
     * @param failOnError set to false in order not to have exception if file is still modified
     */
    @PublicAtsApi
    public void failCopyFileIfSizeChanged( boolean failOnError ) {

        this.failOnError = failOnError;

    }

    /**
     * Copies the contents of a file from atsAgent host to a new file on the local host. <br/>
     * <b>Note:</b> If no atsAgent is used or it is local, then the source files is searched on the local host
     *
     * @param fromFile the source file
     * @param toFile the local destination file
     */
    @PublicAtsApi
    public void copyFileFrom( @Validate( name = "fromFile", type = ValidationType.STRING_NOT_EMPTY) String fromFile,
                              @Validate( name = "toFile", type = ValidationType.STRING_NOT_EMPTY) String toFile ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ fromFile, toFile });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        if (operations instanceof LocalFileSystemOperations) {
            ((LocalFileSystemOperations) operations).copyFile(fromFile, toFile, this.failOnError);

            log.info("Successfully copied " + fromFile + " to " + toFile);
        } else {
            ((RemoteFileSystemOperations) operations).copyFileFrom(fromFile, toFile, this.failOnError);

            log.info("Successfully copied " + fromFile + " from " + atsAgent + " to file " + toFile
                     + " on the localhost");
        }
    }

    /**
     * Copies the contents of a file from the local host to a new file on the atsAgent host
     *
     * @param fromFile the source file to copy
     * @param toFile the destination file to copy to
     */
    @PublicAtsApi
    public void copyFileTo( @Validate( name = "fromFile", type = ValidationType.STRING_NOT_EMPTY) String fromFile,
                            @Validate( name = "toFile", type = ValidationType.STRING_NOT_EMPTY) String toFile ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ fromFile, toFile });

        try {
            checkIfFileExistsAndIsFile(fromFile);
        } catch (Exception e) {
            throw new FileSystemOperationException("Unable to copy '" + fromFile + "' to '" + toFile + "'", e);
        }

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        if (operations instanceof LocalFileSystemOperations) {
            ((LocalFileSystemOperations) operations).copyFile(fromFile, toFile, this.failOnError);

            log.info("Successfully copied " + fromFile + " to " + toFile);
        } else {
            ((RemoteFileSystemOperations) operations).copyFile(fromFile, toFile, this.failOnError);

            log.info("Successfully copied " + fromFile + " from local host to file " + toFile + " on "
                     + atsAgent);
        }
    }

    /**
     * Copies the contents of a file from one remote host to another remote host
     *
     * @param fromHost the address of the ATS agent on the source host.<br />
     * If you provide null then local host will be used. In such case it is recommended to use
     * {@link #copyFileTo(String, String)} method after FileSytemOperations is constructed with target agent (toHost)
     * @param fromFile the source file to copy
     * @param toHost the address of the ATS agent on the destination host.<br />
     * If you provide null then local host will be used. In such case it is recommended to use
     * {@link #copyFileFrom(String, String)} method after FileSytemOperations is constructed with target agent (fromHost)
     * @param toFile the destination file to copy to
     */
    @PublicAtsApi
    public void
            copyRemoteFile( @Validate( name = "fromHost", type = ValidationType.STRING_SERVER_WITH_PORT) String fromHost,
                            @Validate( name = "fromFile", type = ValidationType.STRING_NOT_EMPTY) String fromFile,
                            @Validate( name = "toHost", type = ValidationType.STRING_SERVER_WITH_PORT) String toHost,
                            @Validate( name = "toFile", type = ValidationType.STRING_NOT_EMPTY) String toFile ) {

        // replace to pass validation
        if (fromHost == null) {
            fromHost = LOCAL_HOST_NAME_AND_PORT;
        }
        if (toHost == null) {
            toHost = LOCAL_HOST_NAME_AND_PORT;
        }

        // validate input parameters
        fromHost = HostUtils.getAtsAgentIpAndPort(fromHost);
        toHost = HostUtils.getAtsAgentIpAndPort(toHost);
        new Validator().validateMethodParameters(new Object[]{ fromHost, fromFile, toHost, toFile });

        // execute action
        IFileSystemOperations fromHostOperations = getOperationsImplementationFor(fromHost);
        if (fromHostOperations instanceof LocalFileSystemOperations) {

            IFileSystemOperations toHostOperations = getOperationsImplementationFor(toHost);
            if (toHostOperations instanceof LocalFileSystemOperations) {
                ((LocalFileSystemOperations) toHostOperations).copyFile(fromFile, toFile,
                                                                        this.failOnError);

                log.info("Successfully copied " + fromFile + " to " + toFile);
            } else {
                ((RemoteFileSystemOperations) toHostOperations).copyFile(fromFile, toFile,
                                                                         this.failOnError);

                log.info("Successfully copied " + fromFile + " from local host to file " + toFile + " on "
                         + toHost);
            }
        } else {

            IFileSystemOperations toHostOperations = getOperationsImplementationFor(toHost);
            if (toHostOperations instanceof LocalFileSystemOperations) {
                ((RemoteFileSystemOperations) fromHostOperations).copyFileFrom(fromFile, toFile,
                                                                               this.failOnError);

                log.info("Successfully copied " + fromFile + " from " + fromHost + " to file " + toFile
                         + " on the localhost");
            } else {
                if (fromHost.equalsIgnoreCase(toHost)) {
                    // source and target hosts are remote, but they are same host indeed
                    ((RemoteFileSystemOperations) fromHostOperations).copyFileLocally(fromFile, toFile,
                                                                                      this.failOnError);

                } else {
                    ((RemoteFileSystemOperations) fromHostOperations).copyFileTo(fromFile, toHost,
                                                                                 toFile,
                                                                                 this.failOnError);
                }
                log.info("Successfully copied " + fromFile + " from " + fromHost + " to file " + toFile
                         + " on " + toHost);
            }
        }
    }

    /**
     * Deletes a file <br/>
     * <b>Note: </b>It does nothing if the file does not exist
     *
     * @param filePath the file to work with
     */
    @PublicAtsApi
    public void deleteFile( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.deleteFile(filePath);

        // log the result of the operation
        log.info("Successfully deleted " + filePath + getHostDescriptionSuffix());
    }

    /**
     * Renames a file
     *
     * @param sourceFile source file name
     * @param destinationFile destination file name
     * @param overwrite whether to override the destination file if already exists
     */
    @PublicAtsApi
    public void renameFile( @Validate( name = "sourceFile", type = ValidationType.STRING_NOT_EMPTY) String sourceFile,
                            @Validate( name = "destinationFile", type = ValidationType.STRING_NOT_EMPTY) String destinationFile,
                            @Validate( name = "overwrite", type = ValidationType.NONE) boolean overwrite ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ sourceFile, destinationFile, overwrite });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.renameFile(sourceFile, destinationFile, overwrite);

        // log the result of the operation
        log.info("Successfully renamed '" + sourceFile + "' to '" + destinationFile + "'"
                 + getHostDescriptionSuffix());
    }

    /**
     * Creates a directory
     *
     * @param directoryName the name of the new directory
     */
    @PublicAtsApi
    public void
            createDirectory( @Validate( name = "directoryName", type = ValidationType.STRING_NOT_EMPTY) String directoryName ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ directoryName });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.createDirectory(directoryName);

        // log the result of the operation
        log.info("Successfully created directory by the name of " + directoryName
                 + getHostDescriptionSuffix());
    }

    /**
     * Creates a directory on with a specific user and group id
     *
     * @param directoryName the name of the new directory
     * @param userId the identification number of the user this file should belong to
     * @param groupId the identification number of the group this file should belong to
     */
    @PublicAtsApi
    public void
            createDirectory( @Validate( name = "directoryName", type = ValidationType.STRING_NOT_EMPTY) String directoryName,
                             @Validate( name = "userId", type = ValidationType.NUMBER_POSITIVE) int userId,
                             @Validate( name = "groupId", type = ValidationType.NUMBER_POSITIVE) int groupId ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ directoryName, userId, groupId });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.createDirectory(directoryName, userId, groupId);

        // log the result of the operation
        log.info("Successfully created directory by the name of " + directoryName
                 + getHostDescriptionSuffix());
    }

    /**
     * Copies the contents of a directory to a new one
     *
     * @param fromDirName the source file to copy
     * @param toDirName the destination file to copy to
     * @param isRecursive whether to copy recursively or not
     */
    @PublicAtsApi
    public void
            copyDirectoryTo( @Validate( name = "fromDirName", type = ValidationType.STRING_NOT_EMPTY) String fromDirName,
                             @Validate( name = "toDirName", type = ValidationType.STRING_NOT_EMPTY) String toDirName,
                             @Validate( name = "isRecursive", type = ValidationType.NONE) boolean isRecursive ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ fromDirName, toDirName, isRecursive });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(this.atsAgent);
        if (operations instanceof LocalFileSystemOperations) {

            ((LocalFileSystemOperations) operations).copyDirectory(fromDirName, toDirName, isRecursive,
                                                                   this.failOnError);

            log.info("Successfully copied directory " + fromDirName + " to " + toDirName);
        } else {

            ((RemoteFileSystemOperations) operations).copyDirectory(fromDirName, toDirName, isRecursive,
                                                                    this.failOnError);

            log.info("Successfully copied directory " + fromDirName + " from local host to " + toDirName
                     + " on " + this.atsAgent);
        }
    }

    /**
     * Copies the contents of a directory to a new one
     *
     * @param fromDirName the source file to copy
     * @param toDirName the destination file to copy to
     * @param isRecursive whether to copy recursively or not
     */
    @PublicAtsApi
    public void
            copyDirectoryFrom( @Validate( name = "fromDirName", type = ValidationType.STRING_NOT_EMPTY) String fromDirName,
                               @Validate( name = "toDirName", type = ValidationType.STRING_NOT_EMPTY) String toDirName,
                               @Validate( name = "isRecursive", type = ValidationType.NONE) boolean isRecursive ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ fromDirName, toDirName, isRecursive });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(this.atsAgent);
        if (operations instanceof LocalFileSystemOperations) {

            ((LocalFileSystemOperations) operations).copyDirectory(fromDirName, toDirName, isRecursive,
                                                                   this.failOnError);

            log.info("Successfully copied directory " + fromDirName + " to " + toDirName);
        } else {

            ((RemoteFileSystemOperations) operations).copyDirectoryFrom(fromDirName, toDirName,
                                                                        isRecursive, this.failOnError);

            log.info("Successfully copied directory " + fromDirName + " from " + this.atsAgent + " to "
                     + toDirName + " on the local host");
        }
    }

    /**
     * Copies the contents of a directory located on a remote host to another remote host
     *
     * @param fromHost the address of the ATS agent on the source host
     * @param fromDirectory the source file to copy
     * @param toHost the address of the  ATS agent on the destination host
     * @param toDirectory the destination file to copy to
     * @param isRecursive whether to copy recursively or not
     */
    @PublicAtsApi
    public void
            copyRemoteDirectory( @Validate( name = "fromHost", type = ValidationType.STRING_SERVER_WITH_PORT) String fromHost,
                                 @Validate( name = "fromDirectory", type = ValidationType.STRING_NOT_EMPTY) String fromDirectory,
                                 @Validate( name = "toHost", type = ValidationType.STRING_SERVER_WITH_PORT) String toHost,
                                 @Validate( name = "toDirectory", type = ValidationType.STRING_NOT_EMPTY) String toDirectory,
                                 @Validate( name = "isRecursive", type = ValidationType.NONE) boolean isRecursive ) {

        // replace to pass validation
        if (fromHost == null) {
            fromHost = LOCAL_HOST_NAME_AND_PORT;
        }
        if (toHost == null) {
            toHost = LOCAL_HOST_NAME_AND_PORT;
        }

        // validate input parameters
        fromHost = HostUtils.getAtsAgentIpAndPort(fromHost);
        toHost = HostUtils.getAtsAgentIpAndPort(toHost);
        new Validator().validateMethodParameters(new Object[]{ fromHost, fromDirectory, toHost, toDirectory,
                                                               isRecursive });

        // execute action
        IFileSystemOperations fromHostOperations = getOperationsImplementationFor(fromHost);
        if (fromHostOperations instanceof LocalFileSystemOperations) {

            IFileSystemOperations toHostOperations = getOperationsImplementationFor(toHost);
            if (toHostOperations instanceof LocalFileSystemOperations) {
                ((LocalFileSystemOperations) toHostOperations).copyDirectory(fromDirectory, toDirectory,
                                                                             isRecursive,
                                                                             this.failOnError);

                log.info("Successfully copied directory " + fromDirectory + " to " + toDirectory);
            } else {
                ((RemoteFileSystemOperations) toHostOperations).copyDirectory(fromDirectory, toDirectory,
                                                                              isRecursive,
                                                                              this.failOnError);

                log.info("Successfully copied directory " + fromDirectory + " from local host to "
                         + toDirectory + " on " + toHost);
            }
        } else {

            IFileSystemOperations toHostOperations = getOperationsImplementationFor(toHost);
            if (toHostOperations instanceof LocalFileSystemOperations) {
                ((RemoteFileSystemOperations) fromHostOperations).copyDirectoryFrom(fromDirectory,
                                                                                    toDirectory,
                                                                                    isRecursive,
                                                                                    this.failOnError);

                log.info("Successfully copied directory " + fromDirectory + " from " + fromHost + " to "
                         + toDirectory + " on the local host");
            } else {
                if (fromHost.equalsIgnoreCase(toHost)) {
                    // source and target hosts are remote, but they are same host indeed
                    ((RemoteFileSystemOperations) fromHostOperations).copyDirectoryLocally(fromDirectory,
                                                                                           toDirectory,
                                                                                           isRecursive,
                                                                                           this.failOnError);

                } else {
                    ((RemoteFileSystemOperations) fromHostOperations).copyDirectoryTo(fromDirectory,
                                                                                      toHost,
                                                                                      toDirectory,
                                                                                      isRecursive,
                                                                                      this.failOnError);
                }
                log.info("Successfully copied directory " + fromDirectory + " from " + fromHost + " to "
                         + toDirectory + " on " + toHost);
            }
        }
    }

    /**
     * Deletes a directory and all its content. <br/>
     * <b>Note: </b>It does nothing if the directory does not exist
     *
     * @param directoryPath the directory to work with
     */
    @PublicAtsApi
    public void
            deleteDirectory( @Validate( name = "directoryPath", type = ValidationType.STRING_NOT_EMPTY) String directoryPath ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ directoryPath });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.deleteDirectory(directoryPath, true);

        // log the result of the operation
        log.info("Successfully deleted a directory by the name of " + directoryPath
                 + getHostDescriptionSuffix());
    }

    /**
     * Deletes all directory's content, but does not touch the directory itself<br/>
     * <b>Note: </b>It does nothing if the directory does not exist
     *
     * @param directoryPath the directory to work with
     */
    @PublicAtsApi
    public void
            deleteDirectoryContent( @Validate( name = "directoryPath", type = ValidationType.STRING_NOT_EMPTY) String directoryPath ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ directoryPath });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.purgeDirectoryContents(directoryPath);

        // log the result of the operation
        log.info("Successfully deleted the content of directory by the name of " + directoryPath
                 + getHostDescriptionSuffix());
    }

    /**
     * Replaces specific text in file. Supports regular expressions
     *
     * @param filePath the file to work with
     * @param searchText the text to replace
     * @param newText the replacement text
     * @param isRegex if the searched text is a regular expression
     */
    @PublicAtsApi
    public void replaceTextInFile(
                                   @Validate(name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath,
                                   @Validate(name = "searchText", type = ValidationType.STRING_NOT_EMPTY) String searchText,
                                   @Validate(name = "newText", type = ValidationType.NONE) String newText,
                                   @Validate(name = "isRegex", type = ValidationType.NONE) boolean isRegex ) {

        // validate input parameters
        new Validator().validateMethodParameters( new Object[]{ filePath, searchText, newText, isRegex } );

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor( atsAgent );
        operations.replaceTextInFile( filePath, searchText, newText, isRegex );

        // log the result of the operation
        log.info( "Successfully replaced text '" + searchText + "' with '" + newText + "' in file '"
                  + filePath + "'" + getHostDescriptionSuffix() );
    }

    /**
     * Replaces specific texts in file. Supports regular expressions
     *
     * @param filePath the file to work with
     * @param searchTokens a map in the form <text to replace, replacement text>
     * @param isRegex if the searched texts are regular expressions
     */
    @PublicAtsApi
    public void replaceTextInFile(
                                   @Validate(name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath,
                                   @Validate(name = "searchTokens", type = ValidationType.NOT_NULL) Map<String, String> searchTokens,
                                   @Validate(name = "isRegex", type = ValidationType.NONE) boolean isRegex ) {

        // validate input parameters
        new Validator().validateMethodParameters( new Object[]{ filePath, searchTokens, isRegex } );

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor( atsAgent );
        operations.replaceTextInFile( filePath, searchTokens, isRegex );

        // log the result of the operation
        log.info( "Successfully replaced all tokens in file '" + filePath + "'"
                  + getHostDescriptionSuffix() );
    }

    /**
     * Compute a file's MD5 sum
     *
     * @param filePath the file to work with
     * @param mode mode for computing the MD5 sum
     * <blockquote>
     * ASCII mode - the line endings will be ignored when computing the sum.
     * E.g. same file with Windows and Linux style line endings will give same MD5 sum <br/>
     * BINARY mode - each byte is affecting the returned result
     * </blockquote>
     * @return the MD5 sum in hex format
     */
    @PublicAtsApi
    public String computeMd5Sum( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath,
                                 Md5SumMode mode ) {

        String md5 = null;

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath, Md5SumMode.BINARY });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        md5 = operations.computeMd5Sum(filePath, mode);

        // log the result of the operation
        log.debug("Successfully extracted the MD5 sum '" + md5 + "' of " + filePath
                  + getHostDescriptionSuffix());

        return md5;
    }

    /**
     * Get the last lines from a file
     *
     * @param filePath the file to work with
     * @param numberOfLinesToRead the number of lines to read
     * @return the last lines
     */
    @PublicAtsApi
    public String[]
            getLastLinesFromFile( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath,
                                  @Validate( name = "numLinesToRead", type = ValidationType.NUMBER_GREATER_THAN_ZERO) int numberOfLinesToRead ) {

        String[] lastLines = null;

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath, numberOfLinesToRead });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        lastLines = operations.getLastLinesFromFile(filePath, numberOfLinesToRead);

        // log the result of the operation
        log.debug("Successfully got the last " + numberOfLinesToRead + " lines of '" + filePath + "'"
                  + getHostDescriptionSuffix());

        return lastLines;
    }

    /**
     * Returns file lines that match some regular expression. <br />
     * <em>Note</em> that search pattern should match whole line, i.e. it should
     * start/end with some wildcard matcher if you search for text somewhere in the line.
     *
     * @param filePath the file to work with
     * @param searchPattern the search pattern
     * @param isSimpleMode
     * <blockquote>
     * true - expects match using only these (DOS/Win-style) special characters:
     *      <blockquote>
     *      '*' character - matches a sequence of any characters<br/>
     *      '?' character - matches one single character
     *      </blockquote>
     * false - supports any Java regular expression
     * </blockquote>
     * <em>Example:</em>
     * <ul>
     *   <li>for simple mode (true): '*Hello*' matches lines like ' Hello colleagues,' and 'Hello'</li>
     *   <li>for not simple mode: '.*Hello.*' matches lines ' Hello colleagues,' and 'Hello'</li>
     * </ul>
     * @return the matched lines
     *
     */
    @PublicAtsApi
    public String[] fileGrep( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath,
                              @Validate( name = "searchPattern", type = ValidationType.STRING_NOT_EMPTY) String searchPattern,
                              @Validate( name = "isSimpleMode", type = ValidationType.NONE) boolean isSimpleMode ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath, searchPattern, isSimpleMode });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        return operations.fileGrep(filePath, searchPattern, isSimpleMode);
    }

    /**
     * Get the group of a file or directory
     *
     * @param filePath the file to work with
     * @return the group
     */
    @PublicAtsApi
    public String
            getFileGroup( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath });

        String result = null;

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        result = operations.getFileGroup(filePath);

        // log the result of the operation
        log.debug("Successfully got the group of " + filePath + getHostDescriptionSuffix());
        return result;
    }

    /**
     * Get the owner of a file or directory
     *
     * @param filePath the file to work with
     * @return the owner
     */
    @PublicAtsApi
    public String
            getFileOwner( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath });

        String result = null;

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        result = operations.getFileOwner(filePath);

        // log the result of the operation
        log.debug("Successfully got the owner of " + filePath + getHostDescriptionSuffix());
        return result;
    }

    /**
     * Get the GID of a file or directory
     *
     * @param filePath the file to work with
     * @return the GID number
     */
    @PublicAtsApi
    public long getFileGID( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath });

        long gid = -1L;

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        gid = operations.getFileGID(filePath);

        // log the result of the operation
        log.debug("Successfully got the GID of " + filePath + getHostDescriptionSuffix());
        return gid;
    }

    /**
     * Set the GID of a file or directory
     *
     * @param filePath the file to work with
     * @param gid the GID number
     */
    @PublicAtsApi
    public void setFileGID( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath,
                            @Validate( name = "gid", type = ValidationType.NUMBER_POSITIVE) long gid ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath, gid });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.setFileGID(filePath, gid);

        // log the result of the operation
        log.debug("Successfully set the GID '" + gid + "' of " + filePath + getHostDescriptionSuffix());
    }

    /**
     * Get the UID of a file or directory
     *
     * @param filePath the file to work with
     * @return the UID number
     */
    @PublicAtsApi
    public long getFileUID( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath });

        long uid = -1L;

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        uid = operations.getFileUID(filePath);

        // log the result of the operation
        log.debug("Successfully got the UID of " + filePath + getHostDescriptionSuffix());
        return uid;
    }

    /**
     * Set the UID of a file or directory
     *
     * @param filePath the file to work with
     * @param uid the UID number
     */
    @PublicAtsApi
    public void setFileUID( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath,
                            @Validate( name = "uid", type = ValidationType.NUMBER_POSITIVE) long uid ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath, uid });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.setFileUID(filePath, uid);

        // log the result of the operation
        log.debug("Successfully set the UID '" + uid + "' of " + filePath + getHostDescriptionSuffix());
    }

    /**
     * Get the permissions ( in octal format ) of a file or directory. E.g. 0644 or 1750 or 7605
     *
     * @param filePath the file to work with
     * @return the permissions
     */
    @PublicAtsApi
    public String
            getFilePermissions( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath });

        String result = null;

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        result = operations.getFilePermissions(filePath);

        // log the result of the operation
        log.debug("Successfully got the permissions of " + filePath + getHostDescriptionSuffix());
        return result;
    }

    /**
     * Set the permissions ( in octal format ) of a file or directory. E.g. 0644 or 1750 or 7605
     *
     * @param filePath the file to work with
     * @param permissions the permissions to set
     */
    @PublicAtsApi
    public void
            setFilePermissions( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath,
                                @Validate( name = "permissions", type = ValidationType.STRING_NOT_EMPTY) String permissions ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath, permissions });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.setFilePermissions(filePath, permissions);

        // log the result of the operation
        log.debug("Successfully set the permissions '" + permissions + "' of " + filePath
                  + getHostDescriptionSuffix());
    }

    /**
     * Get the size of a file or directory
     *
     * @param filePath the file to work with
     * @return the size in bytes
     */
    @PublicAtsApi
    public long getFileSize( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath });

        long result = -1l;

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        result = operations.getFileSize(filePath);

        // log the result of the operation
        log.debug("Successfully got the size of " + filePath + getHostDescriptionSuffix());
        return result;
    }

    /**
     * Reads the file content<br/>
     * <b>NOTE:</b> This method should be used for relatively small files as it loads the whole file in the memory
     *
     * @param filePathh the file to work with
     * @param fileEncoding the file encoding. If null the default encoding will be used
     * @return the file content
     */
    @PublicAtsApi
    public String readFile( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath,
                            String fileEncoding ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath, fileEncoding });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        return operations.readFile(filePath, fileEncoding);
    }

    /**
     * Searches some text in file and it remembers where the search stopped.
     * The following search starts from the point where the last search stopped<br/>
     *
     * This method is appropriate for growing text files(for example some kind of a log file)
     *
     * @param filePath the file to work with
     * @param searchText the text to search for
     * @param isRegex if the searchText is regular expression, otherwise the text must
     * simply be contained by the file line
     * @return {@link FileMatchInfo} object
     */
    @PublicAtsApi
    public FileMatchInfo
            findNewTextInFile( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath,
                               @Validate( name = "searchText", type = ValidationType.STRING_NOT_EMPTY) String searchText,
                               boolean isRegex ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath, searchText, isRegex });

        return findNewTextInFile(filePath, new String[]{ searchText }, isRegex);
    }

    /**
     * Searches some texts in file and it remembers where the search stopped.
     * The following search starts from the point where the last search stopped<br/>
     *
     * This method is appropriate for growing text files(for example some kind of a log file)
     *
     * @param filePath the file to work with
     * @param searchTexts the texts to search for. At least one should match.
     * @param isRegex if the searchText is regular expression, otherwise the text must
     * simply be contained by the file line
     * @return {@link FileMatchInfo} object
     */
    @PublicAtsApi
    public FileMatchInfo
            findNewTextInFile( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath,
                               @Validate( name = "searchTexts", type = ValidationType.NOT_NULL) String[] searchTexts,
                               boolean isRegex ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath, searchTexts, isRegex });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);

        long searchFromPosition = 0l;
        int lastReadLineNumber = 0;
        FileMatchInfo fileInfo = this.savedFileMatchDetails.get(filePath);
        if (fileInfo != null) {
            searchFromPosition = fileInfo.lastReadByte;
            lastReadLineNumber = fileInfo.lastReadLineNumber;
        }

        fileInfo = operations.findTextInFileAfterGivenPosition(filePath, searchTexts, isRegex,
                                                               searchFromPosition, lastReadLineNumber);
        this.savedFileMatchDetails.put(filePath, fileInfo);

        return fileInfo;
    }

    /**
     * Searches for files and/or directories on the file system and returns the
     * path of the matched ones. </br>
     * <b>Note: </b>When path points to a directory, it ends with host's file path separator "/" or "\"
     *
     * @param startLocation the folder where search starts
     * @param searchName the name of the searched files or folders
     * @param isRegex whether we search the names by RegEx or a static name
     * @param acceptDirectories if we will include matching directories as well
     * @param recursiveSearch if will search in sub-directories
     * @return the array of matched files and directories
     */
    @PublicAtsApi
    public String[]
            findFiles( @Validate( name = "startLocation", type = ValidationType.STRING_NOT_EMPTY) String startLocation,
                       @Validate( name = "searchName", type = ValidationType.STRING_NOT_EMPTY) String searchName,
                       @Validate( name = "isRegex", type = ValidationType.NONE) boolean isRegex,
                       @Validate( name = "acceptDirectories", type = ValidationType.NONE) boolean acceptDirectories,
                       @Validate( name = "recursiveSearch", type = ValidationType.NONE) boolean recursiveSearch ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ startLocation, searchName, isRegex,
                                                               acceptDirectories, recursiveSearch });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        return operations.findFiles(startLocation, searchName, isRegex, acceptDirectories, recursiveSearch);
    }

    /**
     * Check if a file exists
     *
     * @param filePath the target file path
     * @return <code>true</code> if the file exists and <code>false</code> if it doesn't
     */
    @PublicAtsApi
    public boolean
            doesFileExist( @Validate( name = "filePath", type = ValidationType.STRING_NOT_EMPTY) String filePath ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ filePath });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        return operations.doesFileExist(filePath);
    }

    /**
     * Check if a directory exists
     *
     * @param dirPath the target directory path
     * @return <code>true</code> if the directory exists and <code>false</code> if it doesn't
     * @throws IllegalArgumentExeption if the file exists, but it is not a directory
     */
    @PublicAtsApi
    public boolean doesDirectoryExist(
                                       @Validate( name = "dirPath", type = ValidationType.STRING_NOT_EMPTY) String dirPath ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ dirPath });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        return operations.doesDirectoryExist(dirPath);
    }

    /**
     * <pre>
     * Acquires an exclusive lock on a file
     *
     * <b>Platform dependencies</b>
     *
     * - In Windows it works as expected
     * - In Linux it depends on the locking mechanism of the system. The file locking types are two - advisory and mandatory:
     *
     *    a) <b>Advisory locking</b> - advisory locking will work, only if the participating process are cooperative.
     *       Advisory locking sometimes also called as "unenforced" locking.
     *
     *    b) <b>Mandatory locking</b> - mandatory locking doesn’t require cooperation from the participating processes.
     *       It causes the kernel to check every open, read and write to verify that the calling process isn’t
     *       violating a lock on the given file. To enable mandatory locking in Linux, you need to enable it on
     *       a file system level and also on the individual files. The steps to be followed are:
     *           1. Mount the file system with "<i>-o mand</i>" option
     *           2. For the lock_file, turn on the set-group-ID bit and turn off the group-execute bit, to enable
     *              mandatory locking on that particular file. (This way has been chosen because when you turn off
     *              the group-execute bit, set-group-ID has no real meaning to it )
     *
     *       How to do mandatory locking:
     *           Note: You need to be root to execute the below command
     *           <i># mount -oremount,mand /</i>
     *           <i># touch mandatory.txt</i>
     *           <i># chmod g+s,g-x mandatory.txt</i>
     * </pre>
     *
     * @param fileName file name
     */
    @PublicAtsApi
    public void lockFile( @Validate( name = "fileName", type = ValidationType.STRING_NOT_EMPTY) String fileName ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ fileName });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.lockFile(fileName);

        // log the result of the operation
        log.info("File '" + fileName + "'" + getHostDescriptionSuffix() + " is successfully locked");
    }

    /**
     * Unlock file already locked with {@link #lockFile(String) lockFile()} method
     *
     * @param fileName file name
     */
    @PublicAtsApi
    public void unlockFile( @Validate( name = "fileName", type = ValidationType.STRING_NOT_EMPTY) String fileName ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ fileName });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.unlockFile(fileName);

        // log the result of the operation
        log.info("File '" + fileName + "'" + getHostDescriptionSuffix() + " is successfully unlocked");
    }

    /**
     * Read file from specific byte position. Used for file tail.<br/>
     * <b>NOTE:</b> If the file is replaced with the same byte content, then no change is assumed and 'null' is returned
     *
     * @param fileName file name
     * @param fromBytePosition byte offset. Example: for already read 100 bytes next method call is expected to have 100 as value for this parameter
     * @return {@link FileTailInfo} object
     */
    @PublicAtsApi
    public FileTailInfo readFile( @Validate( name = "fileName", type = ValidationType.STRING_NOT_EMPTY) String fileName,
                                  @Validate( name = "fromBytePosition", type = ValidationType.NONE) long fromBytePosition ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ fileName, fromBytePosition });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        return operations.readFile(fileName, fromBytePosition);
    }

    /**
     * <p>Unzip archive to local or remote machine.</p>
     * <p>If the machine is UNIX-like it will preserve the permissions</p>
     * <p>This method is deprecated. Use extract().</p>
     *
     * @param zipFilePath the ZIP file path
     * @param outputDirPath output directory which is used as base directory for extracted files
     *
     * @see #extract(String, String)
     */
    @Deprecated
    @PublicAtsApi
    public void unzip( @Validate( name = "zipFilePath", type = ValidationType.STRING_NOT_EMPTY) String zipFilePath,
                       @Validate( name = "outputDirPath", type = ValidationType.STRING_NOT_EMPTY) String outputDirPath ) throws FileSystemOperationException {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ zipFilePath, outputDirPath });

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.unzip(zipFilePath, outputDirPath);
    }

    /**
     * Extract archive to local or remote machine.
     * If the machine is UNIX-like it will preserve the permissions
     *
     * @param archiveFilePath the archive file path
     * @param outputDirPath output directory which is used as base directory for extracted files
     */
    @PublicAtsApi
    public void
            extract( @Validate( name = "archiveFilePath", type = ValidationType.STRING_NOT_EMPTY) String archiveFilePath,
                     @Validate( name = "outputDirPath", type = ValidationType.STRING_NOT_EMPTY) String outputDirPath ) throws FileSystemOperationException {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ archiveFilePath, outputDirPath });

        checkIfArchiveFormatIsSupported(archiveFilePath);

        // execute action
        IFileSystemOperations operations = getOperationsImplementationFor(atsAgent);
        operations.extract(archiveFilePath, outputDirPath);
    }

    private void checkIfArchiveFormatIsSupported( String archiveFilePath ) {

        if (archiveFilePath.endsWith(".zip")) {
            return;
        } else if (archiveFilePath.endsWith(".gz") && !archiveFilePath.endsWith(".tar.gz")) {
            return;
        } else if (archiveFilePath.endsWith("tar.gz")) {
            return;
        } else if (archiveFilePath.endsWith(".tar")) {
            return;
        } else {
            String[] filenameTokens = IoUtils.getFileName(archiveFilePath).split("\\.");
            if (filenameTokens.length <= 1) {
                throw new FileSystemOperationException("Archive format was not provided.");
            } else {
                throw new FileSystemOperationException("Archive with format '"
                                                       + filenameTokens[filenameTokens.length - 1]
                                                       + "' is not supported. Available once are 'zip', 'gz', 'tar' and 'tar.gz' .");
            }
        }

    }

    // checks if file exists and is a file
    private void checkIfFileExistsAndIsFile(
                                             String filePath ) throws FileNotFoundException {

        File file = new File(filePath);

        // check if file exists
        if (file.exists()) {
            // check if the filePath does NOT point to a file
            if (!file.isFile()) {
                throw new IllegalArgumentException("'" + filePath + "' does not point to a regular file");
            }
        } else {
            throw new FileNotFoundException("'" + filePath + "' does not exist");
        }

    }

    private String getHostDescriptionSuffix() {

        return atsAgent != null
                                ? " on " + atsAgent
                                : "";
    }

    /**
     * Returns a {@link LocalFileSystemOperations} or a
     * {@link RemoteFileSystemOperations} depending on the arguments passed.
     *
     * @param atsAgent the source host to execute the operations from.
     * Should be null if want to use local FS operations.
     * @return a new instance of either a {@link RemoteFileSystemOperations} or
     * a {@link LocalFileSystemOperations}
     */
    private IFileSystemOperations getOperationsImplementationFor( String atsAgent ) {

        if (HostUtils.isLocalAtsAgent(atsAgent)) {
            return new LocalFileSystemOperations();
        } else {
            try {
                return new RemoteFileSystemOperations(atsAgent);
            } catch (Exception e) {
                throw new RuntimeException("Unable to create remote file system operations impl object", e);
            }
            
        }
    }
}
