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
package com.axway.ats.core.filesystem.model;

import java.util.Map;

import com.axway.ats.common.filesystem.EndOfLineStyle;
import com.axway.ats.common.filesystem.FileMatchInfo;
import com.axway.ats.common.filesystem.FileSystemOperationException;
import com.axway.ats.common.filesystem.FileTailInfo;

public interface IFileSystemOperations {

    public void createBinaryFile(
                                  String filename,
                                  long size,
                                  boolean randomContent );

    public void createBinaryFile(
                                  String filename,
                                  long size,
                                  long userId,
                                  long groupId,
                                  boolean randomContent );

    public void createFile(
                            String filename,
                            String fileContent );

    public void createFile(
                            String filename,
                            long size,
                            boolean randomContent,
                            EndOfLineStyle eol );

    public void createFile(
                            String filename,
                            long size,
                            boolean randomContent );

    public void createFile(
                            String filename,
                            long size,
                            long userId,
                            long groupId,
                            boolean randomContent );

    public void createFile(
                            String filename,
                            String fileContent,
                            long userId,
                            long groupId );

    public void createFile(
                            String filename,
                            long size,
                            long userId,
                            long groupId,
                            boolean randomContent,
                            EndOfLineStyle eol );

    public void appendToFile(
                              String filePath,
                              String contentToAdd );

    public void copyFile(
                          String sourceFile,
                          String destinationFile,
                          boolean failOnError );

    public void deleteFile(
                            String fileName );

    public void renameFile(
                            String sourceFile,
                            String destinationFile,
                            boolean overwrite );

    public boolean doesFileExist(
                                  String fileName );

    public boolean doesDirectoryExist(
                                       String dirName );

    public String getFilePermissions(
                                      String sourceFile );

    public void setFilePermissions(
                                    String sourceFile,
                                    String permissions );

    public long getFileUID(
                            String sourceFile );

    public void setFileUID(
                            String sourceFile,
                            long uid );

    public long getFileGID(
                            String sourceFile );

    public void setFileGID(
                            String sourceFile,
                            long gid );

    public String getFileGroup(
                                String sourceFile );

    public String getFileOwner(
                                String sourceFile );

    public long getFileModificationTime(
                                         String sourceFile );

    public void setFileModificationTime(
                                         String sourceFile,
                                         long lastModificationTime );

    public void setFileHiddenAttribute(
                                        String sourceFile,
                                        boolean hidden );

    public long getFileSize(
                             String sourceFile );

    public String computeMd5Sum(
                                 String sourceFile,
                                 com.axway.ats.common.filesystem.Md5SumMode mode );

    public void createDirectory(
                                 String directoryName );

    public void createDirectory(
                                 String directoryName,
                                 long userId,
                                 long groupId );

    public void copyDirectory(
                               String sourceDir,
                               String destinationDir,
                               boolean isRecursive,
                               boolean failOnError );

    public void deleteDirectory(
                                 String directoryName,
                                 boolean deleteRecursively );

    public void purgeDirectoryContents(
                                        String directoryName );

    public String readFile(
                            String fileName,
                            String encoding );

    public void replaceTextInFile( String fileName, String searchString, String newString, boolean isRegex );

    public void replaceTextInFile( String fileName, Map<String, String> searchTokens, boolean isRegex );

    public FileMatchInfo findTextInFileAfterGivenPosition(
                                                           String fileName,
                                                           String[] searchTexts,
                                                           boolean isRegex,
                                                           long searchFromPosition,
                                                           int currentLineNumber );

    public String[] getLastLinesFromFile(
                                          String fileName,
                                          int numLinesToRead );

    public String[] getLastLinesFromFile(
                                          String fileName,
                                          int numLinesToRead,
                                          String charset );

    public String[] findFiles(
                               String location,
                               String searchString,
                               boolean isRegex,
                               boolean acceptDirectories,
                               boolean recursiveSearch );

    public String[] fileGrep(
                              String fileName,
                              String searchPattern,
                              boolean isSimpleMode );

    public String getFileUniqueId(
                                   String fileName );

    public void lockFile(
                          String fileName );

    public void unlockFile(
                            String fileName );

    public FileTailInfo readFile(
                                  String fileName,
                                  long fromBytePosition );

    @Deprecated
    public void unzip(
                       String zipFilePath,
                       String outputDirPath ) throws FileSystemOperationException;

    public void extract(
                         String archiveFilePath,
                         String outputDirPath ) throws FileSystemOperationException;

}
