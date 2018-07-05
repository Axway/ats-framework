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
package com.axway.ats.agent.components.system.operations;

import java.util.Map;

import com.axway.ats.agent.core.model.Action;
import com.axway.ats.agent.core.model.ActionRequestInfo;
import com.axway.ats.agent.core.model.Parameter;
import com.axway.ats.common.filesystem.EndOfLineStyle;
import com.axway.ats.common.filesystem.FileMatchInfo;
import com.axway.ats.common.filesystem.FileTailInfo;
import com.axway.ats.common.filesystem.Md5SumMode;
import com.axway.ats.core.filesystem.LocalFileSystemOperations;

public class InternalFileSystemOperations {

    private LocalFileSystemOperations localFSOperations = null;

    @Action(
            name = "Internal File System Operations initialize")
    @ActionRequestInfo(
            requestUrl = "filesystem",
            requestMethod = "PUT")
    public void initialize() {

        localFSOperations = new LocalFileSystemOperations();
    }

    @Action(
            name = "Internal File System Operations get File Permissions")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/permissions",
            requestMethod = "GET")
    public String getFilePermissions( @Parameter(
            name = "fileName") String fileName ) {

        return localFSOperations.getFilePermissions(fileName);
    }

    @Action(
            name = "Internal File System Operations get File Group")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/filegroup",
            requestMethod = "GET")
    public String getFileGroup( @Parameter(
            name = "fileName") String fileName ) {

        return localFSOperations.getFileGroup(fileName);
    }

    @Action(
            name = "Internal File System Operations get File G I D")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/gid",
            requestMethod = "GET")
    public long getFileGID( @Parameter(
            name = "fileName") String fileName ) {

        return localFSOperations.getFileGID(fileName);
    }

    @Action(
            name = "Internal File System Operations get File Owner")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/owner",
            requestMethod = "GET")
    public String getFileOwner( @Parameter(
            name = "fileName") String fileName ) {

        return localFSOperations.getFileOwner(fileName);
    }

    @Action(
            name = "Internal File System Operations get File U I D")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/uid",
            requestMethod = "GET")
    public long getFileUID( @Parameter(
            name = "fileName") String fileName ) {

        return localFSOperations.getFileUID(fileName);
    }

    @Action(
            name = "Internal File System Operations get File Modification Time")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/modificationTime",
            requestMethod = "GET")
    public long getFileModificationTime( @Parameter(
            name = "fileName") String fileName ) {

        return localFSOperations.getFileModificationTime(fileName);
    }

    @Action(
            name = "Internal File System Operations get File Size")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/size",
            requestMethod = "GET")
    public long getFileSize( @Parameter(
            name = "fileName") String fileName ) {

        return localFSOperations.getFileSize(fileName);
    }

    @Action(
            name = "Internal File System Operations get File Unique Id")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/uniqueId",
            requestMethod = "GET")
    public String getFileUniqueId( @Parameter(
            name = "fileName") String fileName ) {

        return localFSOperations.getFileUniqueId(fileName);
    }

    @Action(
            name = "Internal File System Operations does File Exist")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/exist",
            requestMethod = "GET")
    public boolean doesFileExist( @Parameter(
            name = "fileName") String fileName ) {

        return localFSOperations.doesFileExist(fileName);
    }

    @Action(
            name = "Internal File System Operations does Directory Exist")
    @ActionRequestInfo(
            requestUrl = "filesystem/directory/exist",
            requestMethod = "GET")
    public boolean doesDirectoryExist(
                                       @Parameter(
                                               name = "dirName") String dirName ) {

        return localFSOperations.doesDirectoryExist(dirName);
    }

    @Action(
            name = "Internal File System Operations set File U I D")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/uid",
            requestMethod = "POST")
    public void setFileUID( @Parameter(
            name = "fileName") String fileName,
                            @Parameter(
                                    name = "uid") long uid ) {

        localFSOperations.setFileUID(fileName, uid);
    }

    @Action(
            name = "Internal File System Operations set File G I D")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/gid",
            requestMethod = "POST")
    public void setFileGID( @Parameter(
            name = "fileName") String fileName,
                            @Parameter(
                                    name = "gid") long gid ) {

        localFSOperations.setFileGID(fileName, gid);
    }

    @Action(
            name = "Internal File System Operations set File Permissions")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/permissions",
            requestMethod = "POST")
    public void setFilePermissions( @Parameter(
            name = "fileName") String fileName,
                                    @Parameter(
                                            name = "permissions") String permissions ) {

        localFSOperations.setFilePermissions(fileName, permissions);
    }

    @Action(
            name = "Internal File System Operations set File Modification Time")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/modificationTime",
            requestMethod = "POST")
    public void setFileModificationTime( @Parameter(
            name = "fileName") String fileName,
                                         @Parameter(
                                                 name = "modificationTime") long modificationTime ) {

        localFSOperations.setFileModificationTime(fileName, modificationTime);
    }

    @Action(
            name = "Internal File System Operations set File Hidden Attribute")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/hidden",
            requestMethod = "POST")
    public void setFileHiddenAttribute( @Parameter(
            name = "fileName") String fileName,
                                        @Parameter(
                                                name = "hidden") boolean hidden ) {

        localFSOperations.setFileHiddenAttribute(fileName, hidden);
    }

    /**
     *You have to set to false 'isBinary' flag, if you will parse a text in the file
     *
     * @param fileName the name of the file to generate
     * @param fileSize the size of the randomly generated file
     * @param fileContent the text that will be parsed in the file
     * @param uid the identification number of the user this file should belong to. Default value is Integer.MIN_VALUE
     * @param gid the identification number of the group this file should belong to. Default value is Integer.MIN_VALUE
     * @param eol the end of line style. If null it is set to the current OS's EOL style
     * @param isRandomContent if true the method would generate a file with a random content
     * @param isBinary if true the method would generate a binary file
     */
    @Action(
            name = "Internal File System Operations Create File")
    @ActionRequestInfo(
            requestUrl = "filesystem/file",
            requestMethod = "PUT")
    public void createFile( @Parameter(
            name = "fileName") String fileName,
                            @Parameter(
                                    name = "fileContent") String fileContent,
                            @Parameter(
                                    name = "fileSize") long fileSize,
                            @Parameter(
                                    name = "uid") long uid,
                            @Parameter(
                                    name = "gid") long gid,
                            @Parameter(
                                    name = "eol") String eol,
                            @Parameter(
                                    name = "isRandomContent") boolean isRandomContent,
                            @Parameter(
                                    name = "isBinary") boolean isBinary ) {

        EndOfLineStyle eolStyle = null;
        if (eol != null) {
            eolStyle = EndOfLineStyle.valueOf(eol.toUpperCase());
        }

        if (uid == Integer.MIN_VALUE || gid == Integer.MIN_VALUE) {
            if (isBinary) {
                localFSOperations.createBinaryFile(fileName, fileSize, isRandomContent);
            } else if (fileContent != null) {
                localFSOperations.createFile(fileName, fileContent);
            } else {
                localFSOperations.createFile(fileName, fileSize, isRandomContent, eolStyle);
            }
        } else {
            if (isBinary) {
                localFSOperations.createBinaryFile(fileName, fileSize, uid, gid, isRandomContent);
            } else if (fileContent != null) {
                localFSOperations.createFile(fileName, fileContent, uid, gid);
            } else {
                localFSOperations.createFile(fileName, fileSize, uid, gid, isRandomContent, eolStyle);
            }
        }
    }

    /**
    *
    * @param filePath the file to work with
    * @param contentToAdd the content to append
    */
    @Action(
            name = "Internal File System Operations Append To File")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/append",
            requestMethod = "POST")
    public void appendToFile( @Parameter(
            name = "filePath") String filePath,
                              @Parameter(
                                      name = "contentToAdd") String contentToAdd ) {

        localFSOperations.appendToFile(filePath, contentToAdd);
    }

    /**
     *
     * @param directoryName the name of the directory to generate
     * @param uid the identification number of the user this file should belong to. Default value is Integer.MIN_VALUE
     * @param gid the identification number of the group this file should belong to. Default value is Integer.MIN_VALUE
     */
    @Action(
            name = "Internal File System Operations Create Directory")
    @ActionRequestInfo(
            requestUrl = "filesystem/directory",
            requestMethod = "PUT")
    public void createDirectory( @Parameter(
            name = "directoryName") String directoryName,
                                 @Parameter(
                                         name = "uid") long uid,
                                 @Parameter(
                                         name = "gid") long gid ) {

        if (uid == Integer.MIN_VALUE || gid == Integer.MIN_VALUE) {
            localFSOperations.createDirectory(directoryName);
        } else {
            localFSOperations.createDirectory(directoryName, uid, gid);
        }
    }

    @Action(
            name = "Internal File System Operations find Files")
    @ActionRequestInfo(
            requestUrl = "filesystem/findFiles",
            requestMethod = "POST")
    public String[] findFiles( @Parameter(
            name = "location") String location,
                               @Parameter(
                                       name = "searchString") String searchString,
                               @Parameter(
                                       name = "isRegex") boolean isRegex,
                               @Parameter(
                                       name = "acceptDirectories") boolean acceptDirectories,
                               @Parameter(
                                       name = "recursiveSearch") boolean recursiveSearch ) {

        return this.localFSOperations.findFiles(location, searchString, isRegex, acceptDirectories,
                                                recursiveSearch);
    }

    @Action(
            name = "Internal File System Operations get Last Lines")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/content/lastLines",
            requestMethod = "GET")
    public String[] getLastLines( @Parameter(
            name = "fileName") String fileName,
                                  @Parameter(
                                          name = "numberOfLines") int numberOfLines ) {

        return localFSOperations.getLastLinesFromFile(fileName, numberOfLines);
    }

    @Action(
            name = "Internal File System Operations get Last Lines")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/content/lastLines",
            requestMethod = "GET")
    public String[] getLastLines( @Parameter(
            name = "fileName") String fileName,
                                  @Parameter(
                                          name = "numberOfLines") int numberOfLines,
                                  @Parameter(
                                          name = "charset") String charset ) {

        return localFSOperations.getLastLinesFromFile(fileName, numberOfLines, charset);
    }

    @Action(
            name = "Internal File System Operations file Grep")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/content/grep",
            requestMethod = "GET")
    public String[] fileGrep( @Parameter(
            name = "fileName") String fileName,
                              @Parameter(
                                      name = "searchPattern") String searchPattern,
                              @Parameter(
                                      name = "isSimpleMode") boolean isSimpleMode ) {

        return localFSOperations.fileGrep(fileName, searchPattern, isSimpleMode);
    }

    @Action(
            name = "Internal File System Operations compute Md5 Sum")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/md5sum",
            requestMethod = "GET")
    public String computeMd5Sum( @Parameter(
            name = "fileName") String fileName,
                                 @Parameter(
                                         name = "md5SumMode") String md5SumMode ) {

        Md5SumMode mode = Md5SumMode.BINARY;
        if (md5SumMode != null) {
            mode = Md5SumMode.valueOf(md5SumMode);
        }
        return localFSOperations.computeMd5Sum(fileName, mode);
    }

    @Action(
            name = "Internal File System Operations rename File")
    @ActionRequestInfo(
            requestUrl = "filesystem/file",
            requestMethod = "POST")
    public void renameFile( @Parameter(
            name = "oldFileName") String oldFileName,
                            @Parameter(
                                    name = "newFileName") String newFileName,
                            @Parameter(
                                    name = "overwrite") boolean overwrite ) {

        localFSOperations.renameFile(oldFileName, newFileName, overwrite);
    }

    @Action(
            name = "Internal File System Operations replace Text In File")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/content/replace",
            requestMethod = "POST")
    public void replaceTextInFile( @Parameter(
            name = "fileName") String fileName,
                                   @Parameter(
                                           name = "searchString") String searchString,
                                   @Parameter(
                                           name = "newString") String newString,
                                   @Parameter(
                                           name = "isRegex") boolean isRegex ) {

        localFSOperations.replaceTextInFile(fileName, searchString, newString, isRegex);
    }

    @Action(
            name = "Internal File System Operations replace Text In File")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/content/replace",
            requestMethod = "POST")
    public void replaceTextInFile( @Parameter(
            name = "fileName") String fileName,
                                   @Parameter(
                                           name = "searchTokens") Map<String, String> searchTokens,
                                   @Parameter(
                                           name = "isRegex") boolean isRegex ) {

        localFSOperations.replaceTextInFile(fileName, searchTokens, isRegex);
    }

    @Action(
            name = "Internal File System Operations delete File")
    @ActionRequestInfo(
            requestUrl = "filesystem/file",
            requestMethod = "DELETE")
    public void deleteFile( @Parameter(
            name = "fileName") String fileName ) {

        localFSOperations.deleteFile(fileName);
    }

    @Action(
            name = "Internal File System Operations delete Directory")
    @ActionRequestInfo(
            requestUrl = "filesystem/directory",
            requestMethod = "DELETE")
    public void deleteDirectory( @Parameter(
            name = "directoryName") String directoryName,
                                 @Parameter(
                                         name = "deleteRecursively") boolean deleteRecursively ) {

        localFSOperations.deleteDirectory(directoryName, deleteRecursively);
    }

    @Action(
            name = "Internal File System Operations purge Directory Contents")
    @ActionRequestInfo(
            requestUrl = "filesystem/directory/purgeContent",
            requestMethod = "POST")
    public void purgeDirectoryContents( @Parameter(
            name = "directoryName") String directoryName ) {

        localFSOperations.purgeDirectoryContents(directoryName);
    }

    @Action(
            name = "Internal File System Operations set Copy File Port Range")
    @ActionRequestInfo(
            requestUrl = "filesystem/copyPortRange",
            requestMethod = "POST")
    public void setCopyFilePortRange( @Parameter(
            name = "copyFileStartPort") Integer copyFileStartPort,
                                      @Parameter(
                                              name = "copyFileEndPort") Integer copyFileEndPort ) throws Exception {

        localFSOperations.setCopyFilePortRange(copyFileStartPort, copyFileEndPort);
    }

    @Action(
            name = "Internal File System Operations open File Transfer Socket")
    @ActionRequestInfo(
            requestUrl = "filesystem/transferSocket",
            requestMethod = "GET")
    public int openFileTransferSocket() throws Exception {

        return localFSOperations.openFileTransferSocket();
    }

    @Action(
            name = "Internal File System Operations send File To")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/send",
            requestMethod = "POST")
    public void sendFileTo( @Parameter(
            name = "fromFileName") String fromFileName,
                            @Parameter(
                                    name = "toFileName") String toFileName,
                            @Parameter(
                                    name = "machineIP") String machineIP,
                            @Parameter(
                                    name = "port") int port,
                            @Parameter(
                                    name = "failOnError") boolean failOnError ) throws Exception {

        localFSOperations.sendFileTo(fromFileName, toFileName, machineIP, port, failOnError);
    }

    @Action(
            name = "Internal File System Operations Copy File Locally")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/copy",
            requestMethod = "PUT")
    public void copyFileLocally( @Parameter(
            name = "fromFileName") String fromFileName,
                                 @Parameter(
                                         name = "toFileName") String toFileName,
                                 @Parameter(
                                         name = "failOnError") boolean failOnError ) throws Exception {

        localFSOperations.copyFile(fromFileName, toFileName, failOnError);
    }

    @Action(
            name = "Internal File System Operations wait For File Transfer Completion")
    @ActionRequestInfo(
            requestUrl = "filesystem/waitTransferComepletion",
            requestMethod = "POST")
    public void waitForFileTransferCompletion( @Parameter(
            name = "port") int port ) throws Exception {

        localFSOperations.waitForFileTransferCompletion(port);
    }

    @Action(
            name = "Internal File System Operations send Directory To")
    @ActionRequestInfo(
            requestUrl = "filesystem/directory/send",
            requestMethod = "POST")
    public void sendDirectoryTo( @Parameter(
            name = "fromDirName") String fromDirName,
                                 @Parameter(
                                         name = "toDirName") String toDirName,
                                 @Parameter(
                                         name = "machineIP") String machineIP,
                                 @Parameter(
                                         name = "port") int port,
                                 @Parameter(
                                         name = "isRecursive") boolean isRecursive,
                                 @Parameter(
                                         name = "failOnError") boolean failOnError ) throws Exception {

        localFSOperations.sendDirectoryTo(fromDirName, toDirName, machineIP, port, isRecursive,
                                          failOnError);
    }

    @Action(
            name = "Internal File System Operations Copy Directory Locally")
    @ActionRequestInfo(
            requestUrl = "filesystem/directory/copy",
            requestMethod = "PUT")
    public void copyDirectoryLocally( @Parameter(
            name = "fromDirName") String fromDirName,
                                      @Parameter(
                                              name = "toDirName") String toDirName,
                                      @Parameter(
                                              name = "isRecursive") boolean isRecursive,
                                      @Parameter(
                                              name = "failOnError") boolean failOnError ) throws Exception {

        localFSOperations.copyDirectory(fromDirName, toDirName, isRecursive, failOnError);
    }

    @Action(
            name = "Internal File System Operations read File")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/content",
            requestMethod = "GET")
    public String readFile( @Parameter(
            name = "fileName") String fileName,
                            @Parameter(
                                    name = "fileEncoding") String fileEncoding ) {

        return localFSOperations.readFile(fileName, fileEncoding);
    }

    @Action(
            name = "Internal File System Operations find Text In File After Given Position")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/content/find",
            requestMethod = "POST")
    public FileMatchInfo findTextInFileAfterGivenPosition( @Parameter(
            name = "fileName") String fileName,
                                                           @Parameter(
                                                                   name = "searchTexts") String[] searchTexts,
                                                           @Parameter(
                                                                   name = "isRegex") boolean isRegex,
                                                           @Parameter(
                                                                   name = "searchFromPosition") long searchFromPosition,
                                                           @Parameter(
                                                                   name = "currentLineNumber") int currentLineNumber ) {

        return localFSOperations.findTextInFileAfterGivenPosition(fileName, searchTexts, isRegex,
                                                                  searchFromPosition, currentLineNumber);
    }

    @Action(
            name = "Internal File System Operations lock File")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/lock",
            requestMethod = "POST")
    public void lockFile( @Parameter(
            name = "fileName") String fileName ) {

        localFSOperations.lockFile(fileName);
    }

    @Action(
            name = "Internal File System Operations unlock File")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/unlock",
            requestMethod = "POST")
    public void unlockFile( @Parameter(
            name = "fileName") String fileName ) {

        localFSOperations.unlockFile(fileName);
    }

    @Action(
            name = "Internal File System Operations read File From Position")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/content/tail",
            requestMethod = "GET")
    public FileTailInfo readFileFromPosition( @Parameter(
            name = "fileName") String fileName,
                                              @Parameter(
                                                      name = "fromBytePosition") long fromBytePosition ) {

        return localFSOperations.readFile(fileName, fromBytePosition);
    }

    @Action(
            name = "Internal File System Operations unzip")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/unzip",
            requestMethod = "POST")
    public void unzip( @Parameter(
            name = "zipFilePath") String zipFilePath,
                       @Parameter(
                               name = "outputDirPath") String outputDirPath ) throws Exception {

        localFSOperations.unzip(zipFilePath, outputDirPath);
    }

    @Action(
            name = "Internal File System Operations extract")
    @ActionRequestInfo(
            requestUrl = "filesystem/file/extract",
            requestMethod = "POST")
    public void extract( @Parameter(
            name = "archiveFilePath") String archiveFilePath,
                         @Parameter(
                                 name = "outputDirPath") String outputDirPath ) throws Exception {

        localFSOperations.extract(archiveFilePath, outputDirPath);
    }

    @Action(
            name = "Internal File System Operations construct destination file path")
    @ActionRequestInfo(
            requestUrl = "filesystem/constructPath",
            requestMethod = "POST")
    public String constructDestinationFilePath(
                                                @Parameter(
                                                        name = "srcFileName") String srcFileName,
                                                @Parameter(
                                                        name = "dstFilePath") String dstFilePath ) throws Exception {

        return localFSOperations.constructDestinationFilePath(srcFileName, dstFilePath);
    }
}
