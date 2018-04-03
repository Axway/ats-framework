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
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.axway.ats.action.ActionLibraryConfigurator;
import com.axway.ats.agent.components.system.operations.clients.InternalFileSystemOperations;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.common.filesystem.EndOfLineStyle;
import com.axway.ats.common.filesystem.FileMatchInfo;
import com.axway.ats.common.filesystem.FileSystemOperationException;
import com.axway.ats.common.filesystem.FileTailInfo;
import com.axway.ats.common.filesystem.Md5SumMode;
import com.axway.ats.core.filesystem.LocalFileSystemOperations;
import com.axway.ats.core.filesystem.model.IFileSystemOperations;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.utils.StringUtils;

/**
 * The {@link RemoteFileSystemOperations} class is able to execute file system operations on a remote host
 */
public class RemoteFileSystemOperations implements IFileSystemOperations {

    private String                       atsAgent = null;

    private InternalFileSystemOperations remoteFileSystemOperations;
    private LocalFileSystemOperations    localFileSystemOperations;

    
    /*
     * This constructor is used only for by the unit/mock tests
     * */
    @SuppressWarnings("unused")
    private RemoteFileSystemOperations() throws AgentException {
        this.atsAgent = "local.host";
        this.remoteFileSystemOperations = new InternalFileSystemOperations(atsAgent);
        this.localFileSystemOperations = new LocalFileSystemOperations();
    }
    
    /**
     * Constructor
     *
     * @param atsAgent
     */
    public RemoteFileSystemOperations( String atsAgent ) throws AgentException {

        this.atsAgent = atsAgent;
        this.remoteFileSystemOperations = new InternalFileSystemOperations(atsAgent);
        this.remoteFileSystemOperations.initialize();
        this.localFileSystemOperations = new LocalFileSystemOperations();
    }

    @Override
    public void createBinaryFile( String fileName, long size, boolean isRandomContent ) {

        createBinaryFile(fileName, size, Integer.MIN_VALUE, Integer.MIN_VALUE, isRandomContent);
    }

    @Override
    public void createBinaryFile( String fileName, long size, long userId, long groupId,
                                  boolean isRandomContent ) {

        try {
            remoteFileSystemOperations.createFile(fileName, null, size, userId, groupId, null,
                                                  isRandomContent, true);
        } catch (Exception e) {
            throw new FileSystemOperationException("Unable to create binary file by the name of " + fileName
                                                   + " on " + this.atsAgent, e);
        }

    }

    @Override
    public void createFile( String fileName, String fileContent ) {

        createFile(fileName, fileContent, -1, Integer.MIN_VALUE, Integer.MIN_VALUE, false, null);
    }

    @Override
    public void createFile( String fileName, long size, boolean isRandomContent, EndOfLineStyle eol ) {

        createFile(fileName, size, Integer.MIN_VALUE, Integer.MIN_VALUE, isRandomContent, eol);
    }

    @Override
    public void createFile( String fileName, long size, boolean isRandomContent ) {

        createFile(fileName, size, Integer.MIN_VALUE, Integer.MIN_VALUE, isRandomContent, null);
    }

    @Override
    public void createFile( String filename, String fileContent, long userId, long groupId ) {

        createFile(filename, fileContent, -1, userId, groupId, false, null);
    }

    @Override
    public void createFile( String filename, long size, long userId, long groupId, boolean isRandomContent ) {

        createFile(filename, size, userId, groupId, isRandomContent, null);
    }

    @Override
    public void createFile( String fileName, long size, long userId, long groupId, boolean isRandomContent,
                            EndOfLineStyle eol ) {

        createFile(fileName, null, size, userId, groupId, isRandomContent, eol);
    }

    private void createFile( String fileName, String fileContent, long size, long userId, long groupId,
                             boolean isRandomContent, EndOfLineStyle eol ) {

        try {
            remoteFileSystemOperations.createFile(fileName, fileContent, size, userId, groupId, eol == null
                                                                                                            ? null
                                                                                                            : eol.name(),
                                                  isRandomContent, false);
        } catch (Exception e) {
            throw new FileSystemOperationException("Unable to create file by the name of " + fileName
                                                   + " on " + this.atsAgent, e);
        }
    }

    @Override
    public void appendToFile( String fileName, String contentToAdd ) {

        try {
            remoteFileSystemOperations.appendToFile(fileName, contentToAdd);
        } catch (Exception e) {
            throw new FileSystemOperationException("Unable to append content to file " + fileName + " on "
                                                   + this.atsAgent, e);
        }
    }

    @Override
    public void copyFile( String fromFile, String toFile, boolean failOnError ) {

        try {

            // construct toFile final full filepath
            // action is performed to check toFile existence and file type (file/dir)
            toFile = this.remoteFileSystemOperations.constructDestinationFilePath(new File(fromFile).getName(),
                                                                                  toFile);

            Integer copyFileStartPort = getCopyFilePortProperty(ActionLibraryConfigurator.getInstance()
                                                                                         .getCopyFileStartPort());
            Integer copyFileEndPort = getCopyFilePortProperty(ActionLibraryConfigurator.getInstance()
                                                                                       .getCopyFileEndPort());
            if (copyFileStartPort != null && copyFileStartPort > 0 && copyFileEndPort != null
                && copyFileEndPort > 0) {
                remoteFileSystemOperations.setCopyFilePortRange(copyFileStartPort, copyFileEndPort);
            }
            int port = remoteFileSystemOperations.openFileTransferSocket();
            localFileSystemOperations.sendFileTo(fromFile, toFile,
                                                 HostUtils.splitAddressHostAndPort(atsAgent)[0], port,
                                                 failOnError);
            remoteFileSystemOperations.waitForFileTransferCompletion(port);

        } catch (Exception e) {
            String message = new StringBuilder().append("Unable to copy file ")
                                                .append(fromFile)
                                                .append(" to ")
                                                .append(toFile)
                                                .append(" on host ")
                                                .append(this.atsAgent)
                                                .toString();
            throw new FileSystemOperationException(message, e);
        }
    }

    public void copyFileTo( String fromFile, String toMachine, String toFile, boolean failOnError ) {

        try {

            Integer copyFileStartPort = getCopyFilePortProperty(ActionLibraryConfigurator.getInstance()
                                                                                         .getCopyFileStartPort());
            Integer copyFileEndPort = getCopyFilePortProperty(ActionLibraryConfigurator.getInstance()
                                                                                       .getCopyFileEndPort());
            InternalFileSystemOperations toRemoteFSOperations = new InternalFileSystemOperations(toMachine);

            if (copyFileStartPort != null && copyFileStartPort > 0 && copyFileEndPort != null
                && copyFileEndPort > 0) {
                toRemoteFSOperations.setCopyFilePortRange(copyFileStartPort, copyFileEndPort);
            }
            int port = toRemoteFSOperations.openFileTransferSocket();
            this.remoteFileSystemOperations.sendFileTo(fromFile, toFile,
                                                       HostUtils.splitAddressHostAndPort(HostUtils.getAtsAgentIpAndPort(toMachine) /* append port */ )[0],
                                                       port, failOnError);
            toRemoteFSOperations.waitForFileTransferCompletion(port);

        } catch (Exception e) {
            String message = new StringBuilder().append("Unable to copy file ")
                                                .append(fromFile)
                                                .append(" from ")
                                                .append(this.atsAgent)
                                                .append(" to file ")
                                                .append(toFile)
                                                .append(" on ")
                                                .append(toMachine)
                                                .toString();
            throw new FileSystemOperationException(message, e);
        }
    }

    public void copyFileFrom( String fromFile, String toFile, boolean failOnError ) {

        try {
            Integer copyFileStartPort = getCopyFilePortProperty(ActionLibraryConfigurator.getInstance()
                                                                                         .getCopyFileStartPort());
            Integer copyFileEndPort = getCopyFilePortProperty(ActionLibraryConfigurator.getInstance()
                                                                                       .getCopyFileEndPort());
            if (copyFileStartPort != null && copyFileStartPort > 0 && copyFileEndPort != null
                && copyFileEndPort > 0) {
                localFileSystemOperations.setCopyFilePortRange(copyFileStartPort, copyFileEndPort);
            }

            int port = localFileSystemOperations.openFileTransferSocket();
            remoteFileSystemOperations.sendFileTo(fromFile, toFile,
                                                  HostUtils.getPublicLocalHostIp(this.atsAgent), port,
                                                  failOnError);
            localFileSystemOperations.waitForFileTransferCompletion(port);

        } catch (Exception e) {
            String message = new StringBuilder().append("Unable to copy file ")
                                                .append(fromFile)
                                                .append(" from ")
                                                .append(this.atsAgent)
                                                .append(" to file ")
                                                .append(toFile)
                                                .append(" on the local host")
                                                .toString();
            throw new FileSystemOperationException(message, e);
        }
    }

    public void copyFileLocally( String fromFile, String toFile, boolean failOnError ) {

        try {
            this.remoteFileSystemOperations.copyFileLocally(fromFile, toFile, failOnError);
        } catch (Exception e) {
            String message = new StringBuilder().append("Unable to copy file ")
                                                .append(fromFile)
                                                .append(" to ")
                                                .append(toFile)
                                                .append(" both on host ")
                                                .append(this.atsAgent)
                                                .toString();
            throw new FileSystemOperationException(message, e);
        }
    }

    private Integer getCopyFilePortProperty( String property ) {

        Integer port = null;
        String errorMsg = "Port for file copy operation \"" + port + "\"is illegal !";

        if (StringUtils.isNullOrEmpty(property)) {
            return null;
        }

        try {
            port = Integer.parseInt(property);
        } catch (NumberFormatException nfe) {
            throw new FileSystemOperationException(errorMsg
                                                   + " It must be a valid positive number from 1 to 65535");
        }

        if (port > 65535 || port < 1) {
            throw new FileSystemOperationException(errorMsg + " It must be in range from 1 to 65535.");
        }

        return port;
    }

    @Override
    public void copyDirectory( String fromDirName, String toDirName, boolean isRecursive,
                               boolean failOnError ) {

        try {

            int port = remoteFileSystemOperations.openFileTransferSocket();
            localFileSystemOperations.sendDirectoryTo(fromDirName, toDirName,
                                                      HostUtils.splitAddressHostAndPort(atsAgent)[0], port,
                                                      isRecursive, failOnError);
            remoteFileSystemOperations.waitForFileTransferCompletion(port);

        } catch (Exception e) {
            String message = new StringBuilder().append("Unable to copy directory ")
                                                .append(fromDirName)
                                                .append(" to ")
                                                .append(toDirName)
                                                .append(" on host ")
                                                .append(this.atsAgent)
                                                .toString();
            throw new FileSystemOperationException(message, e);
        }
    }

    public void copyDirectoryTo( String fromDirName, String toMachine, String toDirName, boolean isRecursive,
                                 boolean failOnError ) {

        try {
            InternalFileSystemOperations toRemoteFSOperations = new InternalFileSystemOperations(toMachine);
            int port = toRemoteFSOperations.openFileTransferSocket();
            remoteFileSystemOperations.sendDirectoryTo(fromDirName, toDirName,
                                                       HostUtils.splitAddressHostAndPort(HostUtils.getAtsAgentIpAndPort(toMachine))[0],
                                                       port, isRecursive, failOnError);
            toRemoteFSOperations.waitForFileTransferCompletion(port);

        } catch (Exception e) {
            String message = new StringBuilder().append("Unable to copy directory ")
                                                .append(fromDirName)
                                                .append(" from ")
                                                .append(this.atsAgent)
                                                .append(" to ")
                                                .append(toDirName)
                                                .append(" on ")
                                                .append(toMachine)
                                                .toString();
            throw new FileSystemOperationException(message, e);
        }
    }

    public void copyDirectoryFrom( String fromDirName, String toDirName, boolean isRecursive,
                                   boolean failOnError ) {

        try {
            int port = localFileSystemOperations.openFileTransferSocket();
            remoteFileSystemOperations.sendDirectoryTo(fromDirName, toDirName,
                                                       HostUtils.getPublicLocalHostIp(this.atsAgent), port,
                                                       isRecursive, failOnError);
            localFileSystemOperations.waitForFileTransferCompletion(port);

        } catch (Exception e) {
            String message = new StringBuilder().append("Unable to copy directory ")
                                                .append(fromDirName)
                                                .append(" from ")
                                                .append(this.atsAgent)
                                                .append(" to ")
                                                .append(toDirName)
                                                .append(" on the local host")
                                                .toString();
            throw new FileSystemOperationException(message, e);
        }
    }

    /**
     * Copy directory into another one located on same remote host.
     * In this case we do not need to open a communication socket.
     *
     * @param fromDirName
     * @param toDirName
     * @param isRecursive
     * @param failOnError
     */
    public void copyDirectoryLocally( String fromDirName, String toDirName, boolean isRecursive,
                                      boolean failOnError ) {

        try {
            remoteFileSystemOperations.copyDirectoryLocally(fromDirName, toDirName, isRecursive,
                                                            failOnError);
        } catch (Exception e) {
            String message = new StringBuilder().append("Unable to copy directory ")
                                                .append(fromDirName)
                                                .append(" to ")
                                                .append(toDirName)
                                                .append(" both on host ")
                                                .append(this.atsAgent)
                                                .toString();
            throw new FileSystemOperationException(message, e);
        }
    }

    @Override
    public void deleteFile( String fileName ) {

        try {
            remoteFileSystemOperations.deleteFile(fileName);
        } catch (Exception e) {
            String message = new StringBuilder().append("Unable to delete an entity by the name of ")
                                                .append(fileName)
                                                .append(" on the host ")
                                                .append(this.atsAgent)
                                                .toString();
            throw new FileSystemOperationException(message, e);
        }
    }

    public void renameFile( String sourceFile, String destinationFile, boolean overwrite ) {

        try {
            remoteFileSystemOperations.renameFile(sourceFile, destinationFile, overwrite);
        } catch (Exception e) {
            String message = new StringBuilder().append("Unable to rename the file ")
                                                .append(sourceFile)
                                                .append(" to ")
                                                .append(destinationFile)
                                                .append(" on the host ")
                                                .append(this.atsAgent)
                                                .toString();
            throw new FileSystemOperationException(message, e);
        }
    }

    @Override
    public boolean doesFileExist( String fileName ) {

        try {
            return remoteFileSystemOperations.doesFileExist(fileName);
        } catch (Exception e) {
            throw new FileSystemOperationException("Unable to check if file " + fileName + " exists on "
                                                   + this.atsAgent, e);
        }
    }

    @Override
    public boolean doesDirectoryExist( String dirName ) {

        try {
            return remoteFileSystemOperations.doesDirectoryExist(dirName);
        } catch (Exception e) {
            throw new FileSystemOperationException("Unable to check if directory " + dirName + " exists on "
                                                   + this.atsAgent, e);
        }
    }

    @Override
    public String getFilePermissions( String sourceFile ) {

        try {
            return remoteFileSystemOperations.getFilePermissions(sourceFile);
        } catch (Exception e) {
            throw new FileSystemOperationException("Unable to get permissions for file " + sourceFile
                                                   + " on " + this.atsAgent, e);
        }
    }

    @Override
    public void setFilePermissions( String sourceFile, String permissions ) {

        try {
            remoteFileSystemOperations.setFilePermissions(sourceFile, permissions);
        } catch (Exception e) {
            throw new FileSystemOperationException("Unable to set permissions '" + permissions + "' of file "
                                                   + sourceFile + " on " + this.atsAgent, e);
        }
    }

    @Override
    public long getFileUID( String sourceFile ) {

        try {
            return remoteFileSystemOperations.getFileUID(sourceFile);
        } catch (Exception e) {
            throw new FileSystemOperationException("Unable to get user id for file " + sourceFile + " on "
                                                   + this.atsAgent, e);
        }
    }

    @Override
    public void setFileUID( String sourceFile, long uid ) {

        try {
            remoteFileSystemOperations.setFileUID(sourceFile, uid);
        } catch (Exception e) {
            throw new FileSystemOperationException("Unable to set user id for file " + sourceFile + " on "
                                                   + this.atsAgent, e);
        }
    }

    @Override
    public long getFileGID( String sourceFile ) {

        try {
            return remoteFileSystemOperations.getFileGID(sourceFile);
        } catch (Exception e) {
            throw new FileSystemOperationException("Unable to get group id for file " + sourceFile + " on "
                                                   + this.atsAgent, e);
        }
    }

    @Override
    public void setFileGID( String sourceFile, long gid ) {

        try {
            remoteFileSystemOperations.setFileGID(sourceFile, gid);
        } catch (Exception e) {
            throw new FileSystemOperationException("Unable to set group id for file " + sourceFile + " on "
                                                   + this.atsAgent, e);
        }
    }

    @Override
    public String getFileGroup( String sourceFile ) {

        try {
            return remoteFileSystemOperations.getFileGroup(sourceFile);
        } catch (Exception e) {
            throw new FileSystemOperationException("Unable to get group of file/directory " + sourceFile
                                                   + " on " + this.atsAgent, e);
        }
    }

    @Override
    public String getFileOwner( String sourceFile ) {

        try {
            return remoteFileSystemOperations.getFileOwner(sourceFile);
        } catch (Exception e) {
            throw new FileSystemOperationException("Unable to get owner of file/directory " + sourceFile
                                                   + " on " + this.atsAgent, e);
        }
    }

    @Override
    public long getFileModificationTime( String sourceFile ) {

        try {
            return remoteFileSystemOperations.getFileModificationTime(sourceFile);
        } catch (Exception e) {
            throw new FileSystemOperationException("Unable to get last modification time for file "
                                                   + sourceFile + " on " + this.atsAgent, e);
        }
    }

    @Override
    public void setFileModificationTime( String sourceFile, long lastModificationTime ) {

        try {
            remoteFileSystemOperations.setFileModificationTime(sourceFile, lastModificationTime);
        } catch (Exception e) {
            throw new FileSystemOperationException("Unable to set last modification time for file "
                                                   + sourceFile + " on " + this.atsAgent, e);
        }
    }

    @Override
    public void setFileHiddenAttribute( String sourceFile, boolean hidden ) {

        try {
            remoteFileSystemOperations.setFileHiddenAttribute(sourceFile, hidden);
        } catch (Exception e) {
            throw new FileSystemOperationException("Unable to set the hidden attribute of file " + sourceFile
                                                   + " on " + this.atsAgent, e);
        }
    }

    @Override
    public long getFileSize( String sourceFile ) {

        try {
            return remoteFileSystemOperations.getFileSize(sourceFile);
        } catch (Exception e) {
            throw new FileSystemOperationException("Unable to get size for file " + sourceFile + " on "
                                                   + this.atsAgent, e);
        }
    }

    @Override
    public String computeMd5Sum( String sourceFile, Md5SumMode mode ) {

        try {
            return remoteFileSystemOperations.computeMd5Sum(sourceFile, mode.name());
        } catch (Exception e) {
            throw new FileSystemOperationException("Unable to get size for file " + sourceFile + " on "
                                                   + this.atsAgent, e);
        }
    }

    @Override
    public void createDirectory( String directoryName ) {

        createDirectory(directoryName, Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    @Override
    public void createDirectory( String directoryName, long userId, long groupId ) {

        try {
            remoteFileSystemOperations.createDirectory(directoryName, userId, groupId);
        } catch (Exception e) {
            throw new FileSystemOperationException("Unable to create dirctory by the name of "
                                                   + directoryName + " on " + this.atsAgent, e);
        }
    }

    @Override
    public void deleteDirectory( String directoryName, boolean deleteRecursively ) {

        if (deleteRecursively) {
            try {
                remoteFileSystemOperations.deleteDirectory(directoryName, deleteRecursively);
            } catch (Exception e) {
                String message = new StringBuilder().append("Unable to " + (deleteRecursively
                                                                                              ? "recursively "
                                                                                              : "")
                                                            + "delete the directory ")
                                                    .append(directoryName)
                                                    .append(" on the host ")
                                                    .append(this.atsAgent)
                                                    .toString();
                throw new FileSystemOperationException(message, e);
            }
        } else {
            this.deleteFile(directoryName);
        }
    }

    @Override
    public void purgeDirectoryContents( String directoryName ) {

        try {
            remoteFileSystemOperations.purgeDirectoryContents(directoryName);
        } catch (Exception e) {
            String message = new StringBuilder().append("Unable to purge the contents of ")
                                                .append(directoryName)
                                                .append(" on the host ")
                                                .append(this.atsAgent)
                                                .toString();
            throw new FileSystemOperationException(message, e);
        }

    }

    public String[] getLastLinesFromFile( String fileName, int numLinesToRead ) {

        return getLastLinesFromFile(fileName, numLinesToRead, StandardCharsets.ISO_8859_1.name());
    }

    public String[] getLastLinesFromFile( String fileName, int numLinesToRead, String charset ) {

        try {
            return remoteFileSystemOperations.getLastLines(fileName, numLinesToRead);
        } catch (Exception e) {
            String message = new StringBuilder().append("Unable to get the last ")
                                                .append(numLinesToRead)
                                                .append(" lines of '")
                                                .append(fileName)
                                                .append("' on ")
                                                .append(this.atsAgent)
                                                .toString();
            throw new FileSystemOperationException(message, e);
        }
    }

    @Override
    public String[] fileGrep( String fileName, String searchPattern, boolean isSimpleMode ) {

        try {
            return remoteFileSystemOperations.fileGrep(fileName, searchPattern, isSimpleMode);
        } catch (Exception e) {
            String message = new StringBuilder().append("Unable to grep for '")
                                                .append(searchPattern)
                                                .append("' in file '")
                                                .append(fileName)
                                                .append("' on ")
                                                .append(this.atsAgent)
                                                .toString();
            throw new FileSystemOperationException(message, e);
        }
    }

    @Override
    public String readFile( String fileName, String fileEncoding ) {

        try {
            return remoteFileSystemOperations.readFile(fileName, fileEncoding);
        } catch (Exception e) {
            String message = new StringBuilder().append("Unable to read file '")
                                                .append(fileName)
                                                .append("' from host ")
                                                .append(this.atsAgent)
                                                .toString();
            throw new FileSystemOperationException(message, e);
        }
    }

    @Override
    public FileMatchInfo findTextInFileAfterGivenPosition( String fileName, String[] searchTexts,
                                                           boolean isRegex, long searchFromPosition,
                                                           int currentLineNumber ) {

        try {
            return remoteFileSystemOperations.findTextInFileAfterGivenPosition(fileName, searchTexts,
                                                                               isRegex, searchFromPosition,
                                                                               currentLineNumber);
        } catch (Exception e) {
            String message = new StringBuilder().append("Unable to search for text in file '")
                                                .append(fileName)
                                                .append("' on host ")
                                                .append(this.atsAgent)
                                                .toString();
            throw new FileSystemOperationException(message, e);
        }
    }

    @Override
    public String[] findFiles( String location, String searchString, boolean isRegex,
                               boolean acceptDirectories, boolean recursiveSearch ) {

        try {
            return this.remoteFileSystemOperations.findFiles(location, searchString, isRegex,
                                                             acceptDirectories, recursiveSearch);
        } catch (Exception e) {
            String message = new StringBuilder().append("Unable to find files" + (acceptDirectories
                                                                                                    ? "/directories"
                                                                                                    : "")
                                                        + " in '")
                                                .append(location)
                                                .append("' searching for " + (isRegex
                                                                                      ? "the RegEx "
                                                                                      : "")
                                                        + " '")
                                                .append(searchString)
                                                .append("'" + (recursiveSearch
                                                                               ? ", recursively,"
                                                                               : "")
                                                        + " on the host ")
                                                .append(this.atsAgent)
                                                .toString();
            throw new FileSystemOperationException(message, e);
        }
    }

    @Override
    public String getFileUniqueId( String fileName ) {

        try {
            return this.remoteFileSystemOperations.getFileUniqueId(fileName);
        } catch (Exception e) {
            String message = new StringBuilder().append("Unable to get the unique id of file '")
                                                .append(fileName)
                                                .append("' on the host ")
                                                .append(this.atsAgent)
                                                .toString();
            throw new FileSystemOperationException(message, e);
        }
    }

    @Override
    public void replaceTextInFile( String fileName, String searchString, String newString, boolean isRegex ) {

        try {
            remoteFileSystemOperations.replaceTextInFile(fileName, searchString, newString, isRegex);
        } catch (Exception e) {
            String message = new StringBuilder().append("Unable to replace text '")
                                                .append(searchString)
                                                .append("' with '")
                                                .append(newString)
                                                .append("' in file '")
                                                .append(fileName)
                                                .append("' on ")
                                                .append(this.atsAgent)
                                                .toString();
            throw new FileSystemOperationException(message, e);
        }
    }
    
    @Override
    public void replaceTextInFile( String fileName, Map<String, String> searchTokens, boolean isRegex ) {

        try {
            remoteFileSystemOperations.replaceTextInFile( fileName, searchTokens, isRegex );
        } catch( Exception e ) {

            StringBuilder messageBuilder = new StringBuilder().append( "Unable to replace text '" );
            for( String token : searchTokens.keySet() ) {
                messageBuilder.append( token );
                messageBuilder.append( "," );
            }
            messageBuilder = messageBuilder.deleteCharAt( messageBuilder.length() - 1 );

            messageBuilder.append( "'" )
                          .append( " in file '" )
                          .append( fileName )
                          .append( "' on " )
                          .append( this.atsAgent )
                          .toString();
            throw new FileSystemOperationException( messageBuilder.toString(), e );
        }
    }

    @Override
    public void lockFile( String fileName ) {

        try {
            remoteFileSystemOperations.lockFile(fileName);
        } catch (Exception e) {
            throw new FileSystemOperationException("Unable to lock file '" + fileName + "' on "
                                                   + this.atsAgent, e);
        }
    }

    @Override
    public void unlockFile( String fileName ) {

        try {
            remoteFileSystemOperations.unlockFile(fileName);
        } catch (Exception e) {
            throw new FileSystemOperationException("Unable to unlock file '" + fileName + "' on "
                                                   + this.atsAgent, e);
        }
    }

    @Override
    public FileTailInfo readFile( String fileName, long fromBytePosition ) {

        try {
            return remoteFileSystemOperations.readFileFromPosition(fileName, fromBytePosition);
        } catch (Exception e) {
            throw new FileSystemOperationException("Unable to read file '" + fileName
                                                   + "' from byte position " + fromBytePosition + " on "
                                                   + this.atsAgent, e);
        }
    }

    @Deprecated
    @Override
    public void unzip( String zipFilePath, String outputDirPath ) throws FileSystemOperationException {

        try {
            remoteFileSystemOperations.unzip(zipFilePath, outputDirPath);
        } catch (Exception e) {
            throw new FileSystemOperationException("Error unzipping '" + zipFilePath + "' into '"
                                                   + outputDirPath + "'", e);
        }
    }

    @Override
    public void extract( String archiveFilePath, String outputDirPath ) throws FileSystemOperationException {

        try {
            remoteFileSystemOperations.extract(archiveFilePath, outputDirPath);
        } catch (Exception e) {
            throw new FileSystemOperationException("Error while extracting '" + archiveFilePath + "' into '"
                                                   + outputDirPath + "'", e);
        }

    }

}
