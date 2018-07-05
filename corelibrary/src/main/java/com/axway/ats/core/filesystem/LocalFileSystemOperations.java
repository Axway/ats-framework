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
package com.axway.ats.core.filesystem;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.log4j.Logger;

import com.axway.ats.common.filesystem.EndOfLineStyle;
import com.axway.ats.common.filesystem.FileMatchInfo;
import com.axway.ats.common.filesystem.FileSystemOperationException;
import com.axway.ats.common.filesystem.FileTailInfo;
import com.axway.ats.common.filesystem.Md5SumMode;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.filesystem.exceptions.AttributeNotSupportedException;
import com.axway.ats.core.filesystem.exceptions.FileDoesNotExistException;
import com.axway.ats.core.filesystem.model.FileAttributes;
import com.axway.ats.core.filesystem.model.IFileSystemOperations;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;

/**
 * This implementation of the {@link IFileSystemOperations} interface uses the forces of Java to implement
 * all the necessary methods
 */
public class LocalFileSystemOperations implements IFileSystemOperations {

    private static final Logger              log                                  = Logger.getLogger(LocalFileSystemOperations.class);

    private static final Charset             DEFAULT_CHARSET                      = Charset.forName("UTF-8");

    //  line-terminator chars
    private static final byte                LINE_TERM_LF                         = '\n';
    private static final byte                LINE_TERM_CR                         = '\r';

    //line separator for ASCII mode (content is normalized to Windows CRLF)
    private static final String              NORM_LINESEP                         = "\r\n";

    //file transfer socket commands (during file/directory copy)
    private static final String              FILE_COPY_SOCKET_COMMAND             = "file";
    private static final String              DIR_CREATE_SOCKET_COMMAND            = "dir";
    private static final int                 INTERNAL_SOCKET_PARAMETER_MAX_LENGTH = 1024;                                             // used for file/dir command and  file name length

    //read buffer
    private static final int                 READ_BUFFER_SIZE                     = 16384;

    private static final int                 FILE_TRANSFER_BUFFER_SIZE            = 512 * 1024;
    private static final int                 FILE_TRANSFER_TIMEOUT                = 60 * 1000;

    // file size threshold after which a warning is issued for too-big file and that problems might arise
    private static final int                 FILE_SIZE_FOR_WARNING                = 10 * 1024 * 1024;                                 // 10 MB

    /**
     * The ASCII decimal code of the character at the beginning of the allowed
     * range for generated file content
     */
    private static final int                 START_CHARACTER_CODE_DECIMAL         = 48;

    /**
     * The ASCII decimal code of the character at the end of the allowed
     * range for generated file content
     */
    private static final int                 END_CHARACTER_CODE_DECIMAL           = 122;

    /**
     * The type of the local OS
     */
    private final OperatingSystemType        osType;

    /**
     * Random generator used by the createFile and createBinaryFile
     */
    private static final Random              randomGenerator                      = new Random();

    // Used to keep track of pending file transfers and wait to complete. Map of open-port:FileTransferStatus pairs. Instance should be Hashtable to synchronize access operations;
    private Map<Integer, FileTransferStatus> fileTransferStates                   = new Hashtable<Integer, FileTransferStatus>();

    private static Map<String, FileLock>     lockedFiles                          = new HashMap<String, FileLock>();

    private Integer                          copyFileStartPort;
    private Integer                          copyFileEndPort;

    /**
     * Constructor
     */
    public LocalFileSystemOperations() {

        this.osType = OperatingSystemType.getCurrentOsType();
    }

    @Override
    public void createBinaryFile(
                                  String filename,
                                  long size,
                                  boolean randomContent ) {

        BufferedOutputStream outputStream = null;

        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(filename));
            //if the file size is bigger than 0, fill the file, otherwise create an empty file
            if (size > 0) {
                if (randomContent) {
                    byte[] nextByte = new byte[1];
                    long currentSize = 0;

                    //Write random bytes until size is reached
                    while (currentSize < size) {
                        //write a byte
                        randomGenerator.nextBytes(nextByte);
                        outputStream.write(nextByte);
                        currentSize++;
                    }
                } else {
                    byte nextByte = Byte.MIN_VALUE;
                    long currentSize = 0;

                    while (currentSize < size) {
                        outputStream.write(nextByte);

                        if (nextByte == Byte.MAX_VALUE) {
                            nextByte = 0;
                        } else {
                            nextByte++;
                        }
                        currentSize++;
                    }
                }
            }

        } catch (IOException ioe) {
            throw new FileSystemOperationException("Could not generate file", ioe);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();

                    log.info("Successfully created binary file '" + filename + "' with size " + size);
                } catch (IOException ioe) {
                    log.error("Could not close file output stream", ioe);
                }
            }
        }
    }

    @Override
    public void createBinaryFile(
                                  String filename,
                                  long size,
                                  long userId,
                                  long groupId,
                                  boolean randomContent ) {

        createBinaryFile(filename, size, randomContent);

        if (OperatingSystemType.getCurrentOsType().isUnix()) {
            //set the file attributes if OS is Unix
            chown(userId, groupId, filename);
            log.info("Successfully changed UID to " + userId + " and GID to " + groupId);

        } else {
            log.info("Target OS is not Unix. UID and GID attributes will be ignored");
        }
    }

    private void createFile(
                             String filename,
                             String fileContent,
                             long size,
                             boolean randomContent,
                             EndOfLineStyle eol ) {

        String terminationString;
        if (eol == null) {
            terminationString = EndOfLineStyle.getCurrentOsStyle().getTerminationString();
        } else {
            terminationString = eol.getTerminationString();
        }
        BufferedWriter fileWriter = null;

        try {
            fileWriter = new BufferedWriter(new FileWriter(filename));

            //if the file size is bigger than 0, fill the file, otherwise create an empty file
            if (size > 0) {
                if (randomContent) {
                    //init the random number generator class
                    Random randomGen = new Random();

                    int charAsciiCode = randomGen.nextInt(END_CHARACTER_CODE_DECIMAL
                                                          - START_CHARACTER_CODE_DECIMAL)
                                        + START_CHARACTER_CODE_DECIMAL;

                    //write a preceding random char before the line separator if possible,
                    //then output the line-separator and fill the remainder with random data
                    fileWriter.write((char) charAsciiCode);
                    fileWriter.write(terminationString);

                    long currentSize = terminationString.length() + 1;
                    while (currentSize < size) {
                        //write a random character
                        charAsciiCode = randomGen.nextInt(END_CHARACTER_CODE_DECIMAL
                                                          - START_CHARACTER_CODE_DECIMAL)
                                        + START_CHARACTER_CODE_DECIMAL;

                        fileWriter.write((char) charAsciiCode);
                        currentSize++;
                    }
                } else {
                    int charAsciiCode = START_CHARACTER_CODE_DECIMAL;

                    //write a preceding random char before the line separator if possible,
                    //then output the line-separator and fill the remainder with random data
                    fileWriter.write((char) charAsciiCode);
                    fileWriter.write(terminationString);

                    long currentSize = terminationString.length() + 1;
                    while (currentSize < size) {

                        //reset to the first character in the sequence if we are passed
                        //the last character
                        if (++charAsciiCode > END_CHARACTER_CODE_DECIMAL) {
                            charAsciiCode = START_CHARACTER_CODE_DECIMAL;
                        }

                        fileWriter.write((char) charAsciiCode);
                        currentSize++;
                    }
                }
            } else if (fileContent != null) {
                fileWriter.write(fileContent);
            }

        } catch (IOException ioe) {
            throw new FileSystemOperationException("Could not generate file", ioe);
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                    log.info("Successfully created file '" + filename + (fileContent != null
                                                                                             ? "' with user defined content."
                                                                                             : "' with size "
                                                                                               + size));
                } catch (IOException ioe) {
                    log.error("Could not close file writer", ioe);
                }
            }
        }
    }

    @Override
    public void createFile(
                            String filename,
                            long size,
                            boolean randomContent,
                            EndOfLineStyle eol ) {

        createFile(filename, null, size, randomContent, eol);
    }

    @Override
    public void createFile(
                            String filename,
                            long size,
                            boolean randomContent ) {

        createFile(filename, size, randomContent, null);
    }

    @Override
    public void createFile(
                            String filename,
                            String fileContent ) {

        createFile(filename, fileContent, -1, false, null);
    }

    @Override
    public void createFile(
                            String filename,
                            long size,
                            long userId,
                            long groupId,
                            boolean randomContent ) {

        createFile(filename, size, userId, groupId, randomContent, null);
    }

    @Override
    public void createFile(
                            String filename,
                            long size,
                            long userId,
                            long groupId,
                            boolean randomContent,
                            EndOfLineStyle eol ) {

        createFile(filename, size, randomContent, eol);
        setFileOwnerAndGroupIDs(filename, userId, groupId);
    }

    @Override
    public void createFile(
                            String filename,
                            String fileContent,
                            long userId,
                            long groupId ) {

        createFile(filename, fileContent, -1, false, null);
        setFileOwnerAndGroupIDs(filename, userId, groupId);
    }

    private void setFileOwnerAndGroupIDs(
                                          String filename,
                                          long userId,
                                          long groupId ) {

        if (OperatingSystemType.getCurrentOsType().isUnix()) {
            //set the file attributes if OS is Unix
            chown(userId, groupId, filename);
            log.info("Successfully changed UID to " + userId + " and GID to " + groupId);

        } else {
            log.info("Target OS is not Unix. UID and GID attributes will be ignored");
        }
    }

    @Override
    public void appendToFile(
                              String filePath,
                              String contentToAdd ) {

        if (!new File(filePath).exists()) {
            throw new FileSystemOperationException("Unable to append content to file '" + filePath
                                                   + "' as it does not exist");
        }

        if (new File(filePath).isDirectory()) {
            throw new FileSystemOperationException("Unable to append content to '" + filePath
                                                   + "' as it is a directory, but file was expected");
        }

        BufferedWriter fileWriter = null;

        try {
            fileWriter = new BufferedWriter(new FileWriter(filePath, true));
            fileWriter.write(contentToAdd);
        } catch (IOException ioe) {
            throw new FileSystemOperationException("Unable to append content to file '" + filePath + "'",
                                                   ioe);
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();

                    log.info("Successfully appended " + contentToAdd.length() + " characters to file '"
                             + filePath + "'");
                } catch (IOException ioe) {
                    log.error("Could not close file writer", ioe);
                }
            }
        }
    }

    @SuppressWarnings( "resource")
    @Override
    public void copyFile(
                          String sourceFile,
                          String destinationFile,
                          boolean failOnError ) {

        File inputFile = new File(sourceFile);
        checkFileExistence(inputFile);

        FileChannel srcChannel = null;
        FileChannel dstChannel = null;

        try {
            // Create channel on the source
            srcChannel = new FileInputStream(sourceFile).getChannel();

            // Create channel on the destination
            dstChannel = new FileOutputStream(destinationFile).getChannel();

            // Copy file contents from source to destination
            dstChannel.truncate(0);

            if (log.isDebugEnabled()) {
                log.debug("Copying file '" + sourceFile + "' of " + srcChannel.size() + "bytes to '"
                          + destinationFile + "'");
            }

            /* Copy the file in chunks.
             * If we provide the whole file at once, the copy process does not start or does not
             * copy the whole file on some systems when the file is a very large one - usually
             * bigger then 2 GBs
             */
            final long CHUNK = 16 * 1024 * 1024; // 16 MB chunks
            for (long pos = 0; pos < srcChannel.size();) {
                pos += dstChannel.transferFrom(srcChannel, pos, CHUNK);
            }

            if (srcChannel.size() != dstChannel.size()) {
                throw new FileSystemOperationException("Size of the destination file \"" + destinationFile
                                                       + "\" and the source file \"" + sourceFile
                                                       + "\" missmatch!");
            }

            if (osType.isUnix()) {
                // set the original file permission to the new file
                setFilePermissions(destinationFile, getFilePermissions(sourceFile));
            }

        } catch (IOException e) {
            throw new FileSystemOperationException("Unable to copy file '" + sourceFile + "' to '"
                                                   + destinationFile + "'", e);
        } finally {
            // Close the channels
            IoUtils.closeStream(srcChannel,
                                "Unable to close input channel while copying file '" + sourceFile + "' to '"
                                            + destinationFile + "'");
            IoUtils.closeStream(dstChannel,
                                "Unable to close destination channel while copying file '" + sourceFile
                                            + "' to '" + destinationFile + "'");
        }
    }

    /**
     * Send directory contents to another machine
     *
     * @param fromDirName the source directory name
     * @param toDirName the destination directory name
     * @param toHost the destination machine host address
     * @param toPort the destination machine port
     * @param isRecursive whether to send content recursively or not
     * @param failOnError set to true if you want to be thrown an exception, 
     *           if there is still a process writing in the file that is being copied 
     * @throws FileSystemOperationException
     */
    public void sendDirectoryTo(
                                 String fromDirName,
                                 String toDirName,
                                 String toHost,
                                 int toPort,
                                 boolean isRecursive,
                                 boolean failOnError ) {

        if (fromDirName == null) {
            throw new IllegalArgumentException("Could not copy directories. The source directory name is null");
        }
        if (toDirName == null) {
            throw new IllegalArgumentException("Could not copy directories. The target directory name is null");
        }
        if (isRecursive && toDirName.startsWith(fromDirName)) {
            throw new IllegalArgumentException("Could not copy directories. The target directory is subdirectory of the source one");
        }

        File fromDir = new File(fromDirName);
        checkFileExistence(fromDir);

        Socket socket = null;
        OutputStream sos = null;
        try {
            socket = new Socket(toHost, toPort);
            sos = socket.getOutputStream();

            sendFileToSocketStream(fromDir, toDirName, sos, failOnError);
            sendFilesToSocketStream(fromDir.listFiles(),
                                    fromDirName,
                                    toDirName,
                                    sos,
                                    isRecursive,
                                    failOnError);
        } catch (IOException ioe) {

            throw new FileSystemOperationException("Unable to send directory '" + fromDirName + "' to '"
                                                   + toDirName + "' on " + toHost + ":" + toPort, ioe);
        } finally {
            IoUtils.closeStream(sos);
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    log.error("Could not close the socket", e);
                }
            }
        }
    }

    /**
     * Send file contents to another machine
     *
     * @param fromFileName the source file name
     * @param toFileName the destination file name
     * @param toHost the destination host address
     * @param toPort the destination port
     * @param failOnError set to true if you want to be thrown an exception, 
     * if there is still a process writing in the file that is being copied
     * @throws FileSystemOperationException
     */
    public void sendFileTo(
                            String fromFileName,
                            String toFileName,
                            String toHost,
                            int toPort,
                            boolean failOnError ) {

        File file = new File(fromFileName);
        checkFileExistence(file);

        Socket socket = null;
        OutputStream sos = null;
        try {

            socket = new Socket(toHost, toPort);
            sos = socket.getOutputStream();

            sendFileToSocketStream(file, toFileName, sos, failOnError);
        } catch (IOException ioe) {

            throw new FileSystemOperationException("Unable to send file '" + fromFileName + "' to '"
                                                   + toFileName + "' on " + toHost + ":" + toPort, ioe);
        } finally {
            IoUtils.closeStream(sos);
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    log.error("Could not close the socket", e);
                }
            }
        }
    }

    /**
     * Set port range for copy file operations
     * 
     * @param copyFileStartPort starting range port
     * @param copyFileEndPort   ending range port
     */
    public void setCopyFilePortRange(
                                      Integer copyFileStartPort,
                                      Integer copyFileEndPort ) {

        if (copyFileStartPort != null || copyFileEndPort != null) {
            String startErrorMsg = "Specified port for copy file '";
            String endErrorMsg = "' port must be a positive integer!";

            // check if both values are positive
            if (copyFileStartPort == null || copyFileStartPort < 0) {
                throw new RuntimeException(startErrorMsg + copyFileStartPort + endErrorMsg);
            } else if (copyFileEndPort == null || copyFileEndPort < 0) {
                throw new RuntimeException(startErrorMsg + copyFileEndPort + endErrorMsg);
            } else if (copyFileStartPort == 0 || copyFileEndPort == 0) {
                if (copyFileStartPort == 0 && copyFileEndPort != 0) {
                    throw new RuntimeException(startErrorMsg + copyFileStartPort + endErrorMsg);
                } else {
                    throw new RuntimeException(startErrorMsg + copyFileEndPort + endErrorMsg);
                }
            }

            if (copyFileStartPort > copyFileEndPort) {
                log.warn("Specified start port for copy file '" + copyFileStartPort
                         + "' is bigger than the end port '" + copyFileEndPort + "'. We will switch them!");
                this.copyFileStartPort = copyFileEndPort;
                this.copyFileEndPort = copyFileStartPort;

                return;
            }

            this.copyFileStartPort = copyFileStartPort;
            this.copyFileEndPort = copyFileEndPort;
        }
    }

    /**
     * Search for free port
     * 
     * @return
     */
    private ServerSocket getServerSocket() {

        ServerSocket server = null;
        Integer copyFileCurrentPort;
        for (copyFileCurrentPort = copyFileStartPort; copyFileCurrentPort <= copyFileEndPort; copyFileCurrentPort++) {
            try {
                server = new ServerSocket(copyFileCurrentPort);
                return server;
            } catch (IOException e) {
                log.debug("Searching free port for remote file copy. Port " + copyFileCurrentPort
                          + " is busy. We will check next one.");
                continue;
            }
        }
        throw new RuntimeException("Remote file transfer copy. No free port found in range "
                                   + copyFileStartPort + " to " + copyFileEndPort + ".");
    }

    /**
     * Open file transfer socket
     *
     * @return the port where the socket is listening
     * @throws FileSystemOperationException
     */
    public int openFileTransferSocket() {

        int freePort = -1;
        try {

            final ServerSocket server;
            if (copyFileStartPort == null && copyFileEndPort == null) {
                server = new ServerSocket(0);
            } else {
                server = getServerSocket();
            }
            freePort = server.getLocalPort();

            log.debug("Starting file transfer server on port: " + freePort);

            final FileTransferStatus transferStatus = new FileTransferStatus();
            fileTransferStates.put(freePort, transferStatus);
            Thread thread = new Thread(new Runnable() {

                @Override
                public void run() {

                    Socket socket = null;
                    FileOutputStream fos = null;
                    DataInputStream dis = null;
                    try {
                        server.setReuseAddress(true);
                        server.setSoTimeout(FILE_TRANSFER_TIMEOUT);
                        socket = server.accept();

                        dis = new DataInputStream(socket.getInputStream());
                        int fdTypeLength = dis.readInt();
                        for (;;) {
                            checkParamLengthForSocketTransfer(fdTypeLength, "file type length");
                            String fdType = readString(dis, fdTypeLength); // directory or file
                            int fileNameLength = dis.readInt();
                            checkParamLengthForSocketTransfer(fileNameLength, "file name length");
                            String fileName = readString(dis, fileNameLength);
                            fileName = IoUtils.normalizeFilePath(fileName, osType); // switch file separators according to the current OS
                            File file = new File(fileName);
                            if (fdType.equals(FILE_COPY_SOCKET_COMMAND)) {

                                long fileSize = dis.readLong();
                                log.debug("Creating file: " + fileName + " with size: " + fileSize
                                          + " bytes");

                                // check if file's directory (full directory path) exists
                                // if not, try to create all missing parent directories
                                if (!file.getParentFile().exists()) {
                                    // the file's parent directory does not exist
                                    if (!file.getParentFile().mkdirs()) {
                                        throw new IOException("Could not create parent directories of file '" + file
                                                              + "'");
                                    }
                                }

                                try {
                                    fos = new FileOutputStream(file, false);
                                } catch (IOException e) {
                                    throw new IOException("Could not create destination file '" + file + "'",
                                                          e);
                                }
                                byte[] buff = new byte[FILE_TRANSFER_BUFFER_SIZE];
                                int readBytes = -1;
                                while (fileSize > 0 && (readBytes = dis.read(buff,
                                                                             0,
                                                                             (int) Math.min(buff.length,
                                                                                            fileSize))) > -1) {
                                    fos.write(buff, 0, readBytes);
                                    fos.flush();
                                    fileSize -= readBytes;
                                }
                                IoUtils.closeStream(fos);
                            } else if (fdType.equals(DIR_CREATE_SOCKET_COMMAND)) {

                                if (!file.exists()) {

                                    log.debug("Creating directory: " + fileName);
                                    if (!file.mkdirs()) {
                                        throw new RuntimeException();
                                    }
                                }
                            } else {

                                log.error("Unknown socket command (must be the file descriptor type): "
                                          + fdType);
                                return;
                            }

                            // check for more files/directories
                            try {
                                fdTypeLength = dis.readInt();
                            } catch (EOFException eofe) {
                                // this is the end of the input stream
                                break;
                            }
                        }

                    } catch (SocketTimeoutException ste) {
                        // timeout usually will be when waiting for client connection but theoretically could be also in the middle of reading data
                        log.error("Reached timeout of " + (FILE_TRANSFER_TIMEOUT / 1000)
                                  + " seconds while waiting for file/directory copy operation.", ste);
                        transferStatus.transferException = ste;
                    } catch (IOException e) {
                        log.error("An I/O error occurred", e);
                        transferStatus.transferException = e;
                    } finally {

                        IoUtils.closeStream(fos);
                        IoUtils.closeStream(dis);
                        if (socket != null) {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                log.error("Could not close the Socket", e);
                            }
                        }
                        if (server != null) {
                            try {
                                server.close();
                            } catch (IOException e) {
                                log.error("Could not close the ServerSocket", e);
                            }
                        }

                        synchronized (transferStatus) {

                            transferStatus.finished = true;
                            transferStatus.notify();
                        }
                    }
                }

                private void checkParamLengthForSocketTransfer(
                                                                int cmdParamLength,
                                                                String commandType ) throws IOException {

                    if (cmdParamLength > INTERNAL_SOCKET_PARAMETER_MAX_LENGTH) {
                        throw new IOException("Illegal length for command " + commandType + ": "
                                              + cmdParamLength + "(max allowed is "
                                              + INTERNAL_SOCKET_PARAMETER_MAX_LENGTH
                                              + "); Probably non ATS agent has connected. Closing communication");
                    }
                }

                /**
                 *
                 * @param dis data input stream
                 * @param length the length of bytes to be read
                 * @return the String representation of the read bytes
                 * @throws IOException
                 */
                private String readString(
                                           DataInputStream dis,
                                           int length ) throws IOException {

                    byte[] buff = new byte[length];

                    // this method blocks until the specified bytes are read from the stream
                    dis.readFully(buff, 0, length);
                    return new String(buff, DEFAULT_CHARSET);
                }

            });

            thread.setName("ATSFileTransferSocket-port" + freePort + "__" + thread.getName());
            thread.start();
        } catch (Exception e) {

            throw new FileSystemOperationException("Unable to open file transfer socket", e);
        }

        return freePort;
    }

    /**
     * Waits the file transfer on a specific port to complete
     *
     * @param port the file transfer port
     * @throws Exception the exception logged during transfer reading/writing thread
     */
    public void waitForFileTransferCompletion(
                                               int port ) throws Exception {

        FileTransferStatus transferStatus = fileTransferStates.get(port);
        if (transferStatus != null) {

            synchronized (transferStatus) {
                if (!transferStatus.finished) {

                    try {
                        transferStatus.wait(FILE_TRANSFER_TIMEOUT);
                    } catch (InterruptedException e) {
                        throw new FileSystemOperationException("Timeout while waiting to read the last data chunk from the remote agent. The timeout is "
                                                               + (FILE_TRANSFER_TIMEOUT / 1000) + " sec.",
                                                               e);
                    }
                }
            }
            synchronized (fileTransferStates) {
                fileTransferStates.remove(port);
            }
            if (transferStatus.transferException != null) {
                throw new Exception("Error while transferring data", transferStatus.transferException);
            }
        }
    }

    @Override
    public void deleteFile(
                            String fileName ) {

        File file = new File(fileName);
        boolean fileExists = checkFileExistence(file, false);

        if (fileExists && !file.delete()) {
            throw new FileSystemOperationException("Unable to delete " + (file.isDirectory()
                                                                                             ? "directory"
                                                                                             : "file")
                                                   + " '" + fileName + "'");
        }
    }

    @Override
    public void renameFile(
                            String sourceFile,
                            String destinationFile,
                            boolean overwrite ) {

        File oldFile = new File(sourceFile);
        File newFile = new File(destinationFile);

        //first check if the file exists
        checkFileExistence(oldFile);

        //check if the destination file exists - overwrite it if
        //required, otherwise we need to notify the client by throwing an exception
        if (newFile.exists()) {
            if (overwrite) {
                if (!newFile.delete()) {
                    throw new FileSystemOperationException("Could not delete file '" + destinationFile
                                                           + "'");
                }
            } else {
                throw new FileSystemOperationException("File '" + destinationFile
                                                       + "' already exists and overwrite mode is not turned on");
            }
        }

        //rename the file
        if (!oldFile.renameTo(newFile)) {
            throw new FileSystemOperationException("Could not rename file '" + sourceFile + "' to '"
                                                   + destinationFile + "'");
        }

        log.info("Successfully renamed file '" + sourceFile + "' to '" + destinationFile + "'");
    }

    @Override
    public void replaceTextInFile( String fileName, String searchString, String newString, boolean isRegex ) {

        Map<String, String> tokensMap = new HashMap<>();
        tokensMap.put(searchString, newString);

        replaceTextInFile(fileName, tokensMap, isRegex);
    }

    @Override
    public void replaceTextInFile( String fileName, Map<String, String> searchTokens, boolean isRegex ) {

        BufferedReader inFileReader = null;
        BufferedWriter outFileWriter = null;

        File inputFile = null;
        File outputFile = null;
        StringBuilder info = new StringBuilder();

        for (String token : searchTokens.keySet()) {
            info.append(token);
            info.append(",");
        }
        info = info.deleteCharAt(info.length() - 1);

        try {
            try {
                inputFile = new File(fileName);
                outputFile = new File(fileName + "_" + System.currentTimeMillis() + ".tmp");

                inFileReader = new BufferedReader(new FileReader(inputFile));
                outFileWriter = new BufferedWriter(new FileWriter(outputFile, false));

                String currentLine = inFileReader.readLine();
                while (currentLine != null) {

                    for (Entry<String, String> tokens : searchTokens.entrySet()) {
                        if (isRegex) {
                            currentLine = currentLine.replaceAll(tokens.getKey(), tokens.getValue());
                        } else {
                            currentLine = currentLine.replace(tokens.getKey(), tokens.getValue());
                        }
                    }
                    outFileWriter.write(currentLine);
                    outFileWriter.newLine();

                    //read a new line
                    currentLine = inFileReader.readLine();
                }

                log.info("Successfully replaced all" + (isRegex
                                                                ? " regular expression"
                                                                : "")
                         + " instances of '" + info.toString() + "' in file '" + fileName + "'");
            } finally {
                IoUtils.closeStream(inFileReader);
                IoUtils.closeStream(outFileWriter);
            }

            //after we are finished, rename the temporary file to the original one
            if (OperatingSystemType.getCurrentOsType().isUnix()) {

                // getting original file permissions before overriding operation
                String permissions = getFilePermissions(inputFile.getCanonicalPath());

                renameFile(outputFile.getCanonicalPath(), fileName, true);
                // restoring file permissions
                setFilePermissions(fileName, permissions);
            } else {

                renameFile(outputFile.getCanonicalPath(), fileName, true);
            }
        } catch (IOException ioe) {

            throw new FileSystemOperationException("Unable to replace" + (isRegex
                                                                                  ? " regular expression"
                                                                                  : "")
                                                   + " instances of '" + info.toString() + "' in file '"
                                                   + fileName + "'", ioe);
        }
    }

    @Override
    public boolean doesFileExist(
                                  String fileName ) {

        File file = new File(fileName);
        boolean fileExists = file.exists();
        if (fileExists) {
            log.info("File '" + fileName + "' exists");
        } else {
            log.info("File '" + fileName + "' does not exist");
        }

        return fileExists;
    }

    @Override
    public boolean doesDirectoryExist(
                                       String dirName ) {

        File file = new File(dirName);
        // check if the file/directory exists
        boolean fileExists = file.exists();

        if (fileExists) {
            // check if the file is a directory
            boolean isDirectory = file.isDirectory();
            if (!isDirectory) {
                // the file, referred by dirName, does exist, but it is not a directory
                log.error("File '" + dirName + "' is not a directory");
                throw new IllegalArgumentException("'" + dirName + "' does not point to a directory");
            } else {
                // the file, referred by dirName, does exist and is a directory
                return true;
            }
        }

        return false;
    }

    @Override
    public String getFilePermissions(
                                      String sourceFile ) {

        File file = new File(sourceFile);

        checkFileExistence(file);
        checkAttributeOsSupport(FileAttributes.PERMISSIONS);

        return getPermissions(sourceFile);
    }

    private String getPermissions(
                                   String sourceFile ) {

        int ownerPermissions = 0;
        int groupPermissions = 0;
        int othersPermissions = 0;

        try {
            Path path = Paths.get(sourceFile);
            PosixFileAttributes attr;
            attr = Files.readAttributes(path, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            Set<PosixFilePermission> filePermissions = attr.permissions();

            if (filePermissions.contains(PosixFilePermission.OWNER_READ)) {
                ownerPermissions += 4;
            }
            if (filePermissions.contains(PosixFilePermission.OWNER_WRITE)) {
                ownerPermissions += 2;
            }
            if (filePermissions.contains(PosixFilePermission.OWNER_EXECUTE)) {
                ownerPermissions += 1;
            }
            if (filePermissions.contains(PosixFilePermission.GROUP_READ)) {
                groupPermissions += 4;
            }
            if (filePermissions.contains(PosixFilePermission.GROUP_WRITE)) {
                groupPermissions += 2;
            }
            if (filePermissions.contains(PosixFilePermission.GROUP_EXECUTE)) {
                groupPermissions += 1;
            }
            if (filePermissions.contains(PosixFilePermission.OTHERS_READ)) {
                othersPermissions += 4;
            }
            if (filePermissions.contains(PosixFilePermission.OTHERS_WRITE)) {
                othersPermissions += 2;
            }
            if (filePermissions.contains(PosixFilePermission.OTHERS_EXECUTE)) {
                othersPermissions += 1;
            }

        } catch (IOException ioe) {
            throw new FileSystemOperationException("Could not get permissions for file '" + sourceFile + "'",
                                                   ioe);
        }

        return "0" + String.valueOf(ownerPermissions) + String.valueOf(groupPermissions)
               + String.valueOf(othersPermissions);
    }

    @Override
    public void setFilePermissions(
                                    String sourceFile,
                                    String permissions ) {

        sourceFile = IoUtils.normalizeFilePath(sourceFile, osType);
        File file = new File(sourceFile);

        checkFileExistence(file);
        checkAttributeOsSupport(FileAttributes.PERMISSIONS);

        try {
            Files.setPosixFilePermissions(new File(sourceFile).getCanonicalFile().toPath(),
                                          getPosixFilePermission(Integer.parseInt(permissions, 8)));
        } catch (IOException ioe) {
            throw new FileSystemOperationException("Could not update permissions for file '" + sourceFile
                                                   + "'", ioe);
        }
        log.info("Successfully set permissions of file '" + sourceFile + "' to " + permissions);
    }

    @Override
    public long getFileUID(
                            String sourceFile ) {

        File file = new File(sourceFile);

        checkFileExistence(file);
        checkAttributeOsSupport(FileAttributes.UID);

        return getUserId(sourceFile);
    }

    @Override
    public void setFileUID(
                            String sourceFile,
                            long uid ) {

        File file = new File(sourceFile);

        checkFileExistence(file);
        checkAttributeOsSupport(FileAttributes.UID);

        long gid = getGroupId(sourceFile);
        chown(uid, gid, sourceFile);
    }

    @Override
    public long getFileGID(
                            String sourceFile ) {

        File file = new File(sourceFile);

        checkFileExistence(file);
        checkAttributeOsSupport(FileAttributes.GID);

        return getGroupId(sourceFile);
    }

    @Override
    public void setFileGID(
                            String sourceFile,
                            long gid ) {

        File file = new File(sourceFile);

        checkFileExistence(file);
        checkAttributeOsSupport(FileAttributes.GID);

        long uid = getUserId(sourceFile);
        chown(uid, gid, sourceFile);
    }

    @Override
    public String getFileGroup(
                                String sourceFile ) {

        File file = new File(sourceFile);

        checkFileExistence(file);
        checkAttributeOsSupport(FileAttributes.GROUP);

        return getGroup(sourceFile);
    }

    @Override
    public String getFileOwner(
                                String sourceFile ) {

        File file = new File(sourceFile);

        checkFileExistence(file);
        checkAttributeOsSupport(FileAttributes.OWNER);

        return getOwner(sourceFile);
    }

    @Override
    public long getFileModificationTime(
                                         String sourceFile ) {

        File file = new File(sourceFile);
        checkFileExistence(file);

        return file.lastModified();
    }

    @Override
    public void setFileModificationTime(
                                         String sourceFile,
                                         long lastModificationTime ) {

        File file = new File(sourceFile);
        checkFileExistence(file);

        if (!file.setLastModified(lastModificationTime)) {
            throw new FileSystemOperationException("Could not set last modification time for file '"
                                                   + sourceFile + "'");
        }
    }

    @Override
    public void setFileHiddenAttribute(
                                        String sourceFile,
                                        boolean hidden ) {

        sourceFile = IoUtils.normalizeFilePath(sourceFile, osType);
        checkFileExistence(new File(sourceFile));

        final String errMsg = "Could not " + (hidden
                                                     ? "set"
                                                     : "unset")
                              + " the hidden attribute of file '" + sourceFile + "'";
        if (OperatingSystemType.getCurrentOsType().isWindows()) {
            try {
                Path path = Paths.get(sourceFile);
                DosFileAttributes attr;
                attr = Files.readAttributes(path, DosFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

                boolean goHidden = attr.isHidden();
                if (!hidden && goHidden) {
                    Files.setAttribute(path, "dos:hidden", false, LinkOption.NOFOLLOW_LINKS);
                } else if (hidden && !goHidden) {
                    Files.setAttribute(path, "dos:hidden", true, LinkOption.NOFOLLOW_LINKS);
                }
            } catch (IOException e) {
                throw new FileSystemOperationException(errMsg, e);
            }
        } else if (OperatingSystemType.getCurrentOsType().isUnix()) {
            // a '.' prefix makes the file hidden
            String filePath = IoUtils.getFilePath(sourceFile);
            String fileName = IoUtils.getFileName(sourceFile);
            if (hidden) {
                if (fileName.startsWith(".")) {
                    log.warn("File '" + sourceFile + "' is already hidden. No changes are made!");
                    return;
                } else {
                    fileName = "." + fileName;
                }
            } else {
                if (!fileName.startsWith(".")) {
                    log.warn("File '" + sourceFile + "' is already NOT hidden. No changes are made!");
                    return;
                } else {
                    fileName = fileName.substring(1);
                }
            }
            renameFile(sourceFile, filePath + fileName, false);
        } else {
            throw new FileSystemOperationException(errMsg + ": Unknown OS type");
        }
    }

    @Override
    public long getFileSize(
                             String sourceFile ) {

        File file = new File(sourceFile);
        checkFileExistence(file);

        return file.length();
    }

    @Override
    public String[] findFiles(
                               String location,
                               String searchString,
                               boolean isRegex,
                               boolean acceptDirectories,
                               boolean recursiveSearch ) {

        File startLocation = new File(location);
        if (!startLocation.exists()) {

            if (log.isDebugEnabled()) {
                log.debug("Start location '" + location + "' does not exist");
            }
            return new String[0];
        }
        if (!startLocation.isDirectory()) {

            throw new FileSystemOperationException("Start location '" + location + "' is not a directory");
        }

        FileNameSearchFilter fileNameSearchFilter = new FileNameSearchFilter(searchString,
                                                                             isRegex,
                                                                             acceptDirectories);
        List<String> matchedFiles = getMatchingFiles(startLocation, fileNameSearchFilter, recursiveSearch);

        return matchedFiles.toArray(new String[0]);
    }

    @Override
    public String getFileUniqueId(
                                   String fileName ) {

        String modTime = Long.toString(getFileModificationTime(fileName));
        String gid = null;
        String uid = null;
        if (OperatingSystemType.getCurrentOsType().isUnix()) {

            gid = Long.toString(getFileGID(fileName));
            uid = Long.toString(getFileUID(fileName));
        } else {

            gid = "-1";
            uid = "-1";
        }
        return new StringBuilder().append(fileName)
                                  .append(".")
                                  .append(modTime)
                                  .append(".")
                                  .append(gid)
                                  .append(".")
                                  .append(uid)
                                  .toString();
    }

    @Override
    public String computeMd5Sum(
                                 String sourceFile,
                                 Md5SumMode mode ) {

        InputStream input = null;
        MessageDigest digest;
        try {
            input = new BufferedInputStream(new FileInputStream(sourceFile));

            //obtain MD5 digest
            digest = MessageDigest.getInstance("MD5");

            switch (mode) {
                case BINARY: {
                    digestBinContent(input, digest);
                    break;
                }
                case ASCII: {
                    digestNormContent(input, digest);
                    break;
                }
                default: {
                    throw new FileSystemOperationException("MD5 digest mode '" + mode + "' not supported");
                }
            }
        } catch (FileNotFoundException fnfe) {
            throw new FileDoesNotExistException(sourceFile);
        } catch (NoSuchAlgorithmException nsae) {
            throw new FileSystemOperationException("MD5 sum cannot be calculated", nsae);
        } catch (IOException ioe) {
            throw new FileSystemOperationException("Could read content of file '" + sourceFile + "'", ioe);
        } finally {
            IoUtils.closeStream(input, "Could not close stream");
        }

        //retrieve final MD5 value and convert to hex
        return StringUtils.byteArray2Hex(digest.digest());
    }

    @Override
    public void createDirectory(
                                 String directoryName ) {

        File directory = new File(directoryName);

        if (!directory.exists()) {

            if (!directory.mkdirs()) {
                throw new FileSystemOperationException("Unable to create directory " + directoryName);
            }
        }
    }

    @Override
    public void createDirectory(
                                 String directoryName,
                                 long userId,
                                 long groupId ) {

        createDirectory(directoryName);

        if (OperatingSystemType.getCurrentOsType().isUnix()) {
            //set the file attributes if OS is Unix
            chown(userId, groupId, directoryName);
            log.info("Successfully changed UID to " + userId + " and GID to " + groupId);
        } else {
            log.info("Target OS is not Unix. UID and GID attributes will be ignored");
        }
    }

    @Override
    public void copyDirectory(
                               String fromDirName,
                               String toDirName,
                               boolean isRecursive,
                               boolean failOnError ) {

        if (log.isDebugEnabled()) {
            log.debug("Copy contents of directory '" + fromDirName + "' to '" + toDirName + "'");
        }
        if (fromDirName == null) {
            throw new IllegalArgumentException("Could not copy directories. The source directory name is null");
        }
        if (toDirName == null) {
            throw new IllegalArgumentException("Could not copy directories. The target directory name is null");
        }
        if (isRecursive
            && IoUtils.normalizeDirPath(toDirName).startsWith(IoUtils.normalizeDirPath(fromDirName))) {
            throw new IllegalArgumentException("Could not copy directories. The target directory is subdirectory of the source one");
        }
        File sourceDir = new File(fromDirName);
        if (! (sourceDir.exists() && sourceDir.isDirectory())) {
            throw new FileSystemOperationException("Could not read source directory. Directory named '"
                                                   + fromDirName + "' does not exist.");
        }
        copyDirectoryInternal(sourceDir,
                              new File(toDirName),
                              (String[]) null,
                              isRecursive,
                              failOnError);
    }

    @Override
    public void deleteDirectory(
                                 String directoryName,
                                 boolean deleteRecursively ) {

        if (deleteRecursively) {
            // we need to also delete all content within the directory
            purgeContents(directoryName);
        }
        deleteFile(directoryName);
    }

    @Override
    public void purgeDirectoryContents(
                                        String directoryName ) {

        purgeContents(directoryName);
    }

    @Override
    public String[] getLastLinesFromFile(
                                          String fileName,
                                          int numLinesToRead ) {

        return getLastLinesFromFile(fileName, numLinesToRead, StandardCharsets.ISO_8859_1.name());
    }

    @Override
    public String[] getLastLinesFromFile(
                                          String fileName,
                                          int numLinesToRead,
                                          String charset ) {

        LinkedList<String> lastLinesList = new LinkedList<String>();
        ReversedLinesFileReader reversedReader = null;
        try {
            reversedReader = new ReversedLinesFileReader(new File(fileName), 4096, charset);
            while (lastLinesList.size() < numLinesToRead) {
                String line = reversedReader.readLine();
                // check if the file has less lines than the wanted
                if (line != null) {
                    lastLinesList.addFirst(line);
                } else {
                    break;
                }
            }

            return lastLinesList.toArray(new String[lastLinesList.size()]);
        } catch (IOException ioe) {
            throw new FileSystemOperationException("Error reading file '" + fileName + "'", ioe);
        } finally {
            if (reversedReader != null) {
                IoUtils.closeStream(reversedReader);
            }
        }
    }

    @Override
    public String readFile(
                            String fileName,
                            String fileEncoding ) {

        File file = new File(fileName);
        if (!file.exists()) {
            throw new FileSystemOperationException("File '" + fileName + "' does not exist");
        }
        if (!file.isFile()) {
            throw new FileSystemOperationException("File '" + fileName + "' is not a regular file");
        }
        if (file.length() > FILE_SIZE_FOR_WARNING) {
            log.warn("Large file detected for reading contents! Name '" + fileName + "', " + file.length()
                     + " bytes. Make sure you have " + "enough heap size specified to be able to read it.");
        }
        StringBuilder fileContents = new StringBuilder();

        BufferedReader fileReader = null;
        try {
            if (fileEncoding == null) { // use default system file encoding
                fileReader = new BufferedReader(new FileReader(file));
            } else {
                fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(file),
                                                                      fileEncoding));
            }

            char[] charBuffer = new char[READ_BUFFER_SIZE];

            int charsRead = -1;
            while ( (charsRead = fileReader.read(charBuffer)) > -1) {
                fileContents.append(charBuffer, 0, charsRead);
            }

            return fileContents.toString();
        } catch (FileNotFoundException fnfe) {
            throw new FileSystemOperationException("File '" + fileName + "' does not exist");
        } catch (IOException ioe) {
            throw new FileSystemOperationException("Error reading file '" + fileName + "'", ioe);
        } finally {
            if (fileReader != null) {
                IoUtils.closeStream(fileReader);
            }
        }
    }

    @Override
    public FileMatchInfo findTextInFileAfterGivenPosition(
                                                           String fileName,
                                                           String[] searchTexts,
                                                           boolean isRegex,
                                                           long searchFromPosition,
                                                           int currentLineNumber ) {

        File targetFile = new File(fileName);
        if (!targetFile.exists()) {
            throw new FileDoesNotExistException(fileName);
        }
        if (searchFromPosition < 0l) {
            searchFromPosition = 0l;
        } else if (searchFromPosition > targetFile.length()) {
            throw new FileSystemOperationException("The file '" + fileName + "' has size("
                                                   + targetFile.length()
                                                   + " bytes) lower than the starting position for search ("
                                                   + searchFromPosition
                                                   + " byte). It is possible that file size had been cut since previous inspection but this is not supported.");
        }
        if (currentLineNumber <= 0) {
            currentLineNumber = 1;
        }

        int matches = 0;
        RandomAccessFile raf = null;
        try {
            List<Pattern> searchPatterns = null;
            if (isRegex) {
                searchPatterns = new ArrayList<Pattern>();
                for (String searchText : searchTexts) {
                    searchPatterns.add(Pattern.compile(searchText, Pattern.DOTALL));
                }
            }

            raf = new RandomAccessFile(targetFile, "r");
            raf.seek(searchFromPosition);

            List<String> matchedLines = new ArrayList<String>();
            List<String> matchedPatterns = new ArrayList<String>();
            List<Integer> matchedLineNumbers = new ArrayList<Integer>();
            String line = null;
            long lastLineByteOffset = searchFromPosition;
            while ( (line = IoUtils.readLineWithEOL(raf)) != null) {

                for (int i = 0; i < searchTexts.length; i++) {
                    if ( (isRegex && searchPatterns.get(i).matcher(line).matches())
                         || (!isRegex && line.contains(searchTexts[i]))) {

                        matches++;
                        matchedLines.add(line.trim());
                        matchedPatterns.add(searchTexts[i]);
                        matchedLineNumbers.add(currentLineNumber);
                        lastLineByteOffset = raf.getFilePointer();
                        break;
                    }
                }
                if (line.endsWith("\n") || line.endsWith("\r")) {
                    currentLineNumber++;
                    lastLineByteOffset = raf.getFilePointer();
                }
            }

            return new FileMatchInfo(matches,
                                     currentLineNumber,
                                     lastLineByteOffset,
                                     matchedLines.toArray(new String[matchedLines.size()]),
                                     matchedLineNumbers.toArray(new Integer[matchedLines.size()]),
                                     matchedPatterns.toArray(new String[matchedPatterns.size()]));
        } catch (IOException ioe) {

            throw new FileSystemOperationException("Could not read file '" + fileName
                                                   + "' seeking from byte " + searchFromPosition, ioe);
        } finally {

            IoUtils.closeStream(raf);
        }
    }

    /**
     * Find lines in files which match given pattern
     * @param searchPattern - pattern to match. <em>Note</em> that patter should match whole line so usually wildcards should be set in front and at the end
     * @param isSimpleMode - when true we should support * and ? as wildcard characters
     *
     * (non-Javadoc)
     * @see com.axway.ats.core.filesystem.model.IFileSystemOperations#fileGrep(java.lang.String, java.lang.String, boolean)
     */
    @Override
    public String[] fileGrep(
                              String fileName,
                              String searchPattern,
                              boolean isSimpleMode ) {

        // generate a regular expression if in simple mode
        if (isSimpleMode) {
            searchPattern = constructRegex(searchPattern);
        }

        Pattern pattern = Pattern.compile(searchPattern);

        //list to store the matched lines
        List<String> matchedLines = new ArrayList<String>();
        BufferedReader inputReader = null;

        try {
            //open the file
            inputReader = new BufferedReader(new FileReader(fileName));

            // read contents
            String line = inputReader.readLine();
            while (line != null) {
                //check current line for match
                if (pattern.matcher(line).matches()) {
                    matchedLines.add(line);
                }

                line = inputReader.readLine();
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Could not grep file '" + fileName + "'", ioe);
        } finally {
            IoUtils.closeStream(inputReader);
        }

        return matchedLines.toArray(new String[0]);
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
    @Override
    public void lockFile(
                          String fileName ) {

        synchronized (lockedFiles) {

            if (lockedFiles.containsKey(fileName)) {

                log.warn("File '" + fileName + "' is already locked");
            } else {

                try {
                    File fileToLock = new File(fileName);
                    @SuppressWarnings( "resource")
                    //keep lock to the file
                    FileChannel channel = new RandomAccessFile(fileToLock, "rw").getChannel();

                    FileLock fileLock = channel.lock();
                    lockedFiles.put(fileName, fileLock);
                } catch (FileNotFoundException fnfe) {
                    throw new FileSystemOperationException("File '" + fileName + "' is not found", fnfe);
                } catch (OverlappingFileLockException ofle) {
                    throw new FileSystemOperationException("File '" + fileName
                                                           + "' is already locked in the current JVM"
                                                           + ", but not from this class, so we can't unlock it later.",
                                                           ofle);
                } catch (Exception e) {
                    throw new FileSystemOperationException("Could not lock file '" + fileName + "'", e);
                }
            }
        }
    }

    /**
     * Unlock file already locked with {@link #lockFile(String) lockFile()} method
     *
     * @param fileName file name
     */
    @Override
    public void unlockFile(
                            String fileName ) {

        synchronized (lockedFiles) {

            FileLock fileLock = lockedFiles.get(fileName);
            if (fileLock != null) {
                try {
                    fileLock.release();
                } catch (Exception e) {
                    throw new FileSystemOperationException("Could not unlock file '" + fileName + "'", e);
                } finally {
                    IoUtils.closeStream(fileLock.channel());
                    lockedFiles.remove(fileName);
                }
            } else {
                log.warn("File '" + fileName + "' is not locked, so we will not try to unlock it");
            }
        }
    }

    /**
     * Read file from specific position. Used for file tail.<br/>
     *
     * <b>NOTE:</b> If the file is replaced with the same byte content, then no change is assumed and 'null' is returned
     *
     * @param fileName file name
     * @param fromBytePosition byte offset. Example: for already read 100 bytes next method call is expected to have 100 as value for this parameter
     * return  {@link FileTailInfo} object
     */
    public FileTailInfo readFile(
                                  String fileName,
                                  long fromBytePosition ) {

        RandomAccessFile reader = null;
        try {
            long position = 0;
            boolean isFileRotated = false;

            // Open the file for read only
            File file = new File(fileName);
            try {
                reader = new RandomAccessFile(file, "r");
            } catch (FileNotFoundException fne) {
                throw new FileSystemOperationException("File '" + fileName + "' not found.", fne);
            }

            long length = file.length();
            if (length < fromBytePosition) {
                // File was rotated
                isFileRotated = true;
                position = 0;
            } else {
                position = fromBytePosition;
            }
            reader.seek(position);

            String line = IoUtils.readLineWithEOL(reader);
            if (line != null) {

                StringBuilder sb = new StringBuilder(line.length());
                while (line != null) {
                    sb.append(line);
                    //TODO: consider file encoding
                    line = IoUtils.readLineWithEOL(reader);
                }
                position = reader.getFilePointer();
                return new FileTailInfo(position, isFileRotated, sb.toString());
            }

        } catch (Exception e) {
            throw new FileSystemOperationException("Could not read file '" + fileName
                                                   + "' from byte position " + fromBytePosition, e);
        } finally {
            IoUtils.closeStream(reader);
        }
        return null;
    }

    /**
     * Deletes any contents that the directory might have, both files and folders; the directory
     * itself is not deleted
     *
     * @param directoryName the target directory name
     * @throws FileSystemOperationException if the parameter is not a directory or does not exist
     */
    private void purgeContents(
                                String directoryName ) {

        File file = new File(directoryName);

        boolean exists = checkFileExistence(file, false);

        if (exists) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) {
                    for (File c : files) {
                        deleteRecursively(c);
                    }
                }
            } else {
                throw new FileSystemOperationException(directoryName + " is not a directory! ");
            }
        }
    }

    /**
     * If the parameter is a file it is deleted. Otherwise if it is a directory then it's contents
     * are first deleted (if any exist) and then the directory itself is deleted.
     *
     * @param file the target file
     * @throws FileSystemOperationException
     */
    private void deleteRecursively(
                                    File file ) {

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File c : files) {
                    deleteRecursively(c);
                }
            }
        }

        if (!file.delete()) {
            throw new FileSystemOperationException("Unable to delete " + file);
        }
    }

    /**
     * Digest a binary content with the digest provided
     *
     * @param input the binary stream
     * @param digest the digest to use
     * @throws IOException on error
     */
    private void digestBinContent(
                                   InputStream input,
                                   MessageDigest digest ) throws IOException {

        //init read-buffer
        byte[] readBuffer = new byte[READ_BUFFER_SIZE];

        //read in chunks and update
        int nr;
        while ( (nr = input.read(readBuffer)) > 0) {
            digest.update(readBuffer, 0, nr);
        }
    }

    /**
     * Generates a normalized (ASCII mode) content of the given stream and updates the provided digest with it.
     *
     * @param input the binary stream
     * @param digest the digest to use
     * @throws IOException on error
     */
    private void digestNormContent(
                                    InputStream input,
                                    MessageDigest digest ) throws IOException {

        byte[] normSepData = NORM_LINESEP.getBytes();
        for (int inValue = input.read(); -1 < inValue; inValue = input.read()) {
            //skip line-terminator chars
            byte value = (byte) inValue;
            if (LINE_TERM_LF == value) {
                //we have a line-feed char, digest the normal line-terminator if prev char was not a line-term
                digest.update(normSepData);
            } else if (LINE_TERM_CR != value) {
                //we're not in line-terminator sequence, update digest and mark
                digest.update(value);
            }
        }
    }

    /**
     * This method will construct a regular expression from a simple
     * wildcard pattern - allowed characters are * and ?
     *
     * @param wildcardPattern the wildcard pattern to transform to regex
     * @return the constructed regex
     */
    private String constructRegex(
                                   String wildcardPattern ) {

        final char QUESTION = '?';
        final char STAR = '*';
        final char DOT = '.';

        final String REGEX_ANYCHAR = ".";
        final String REGEX_ANYCHARS = ".*";
        final String REGEX_DOT = "\\.?";

        final char[] searchArr = wildcardPattern.toCharArray();
        StringBuilder regExp = new StringBuilder();
        for (int j = 0; j < searchArr.length; j++) {
            if (searchArr[j] == QUESTION) {
                regExp.append(REGEX_ANYCHAR);
            } else if (searchArr[j] == STAR) {
                regExp.append(REGEX_ANYCHARS);
            } else if (searchArr[j] == DOT) {
                regExp.append(REGEX_DOT);
            } else {
                regExp.append(searchArr[j]);
            }
        }
        return regExp.toString();
    }

    /**
     *
     * @param userId user id
     * @param groupId group id
     * @param filename the file name
     * @throws FileSystemOperationException
     */
    private void chown(
                        long userId,
                        long groupId,
                        String filename ) {

        filename = IoUtils.normalizeFilePath(filename, osType);
        String[] command = new String[]{ "/bin/sh",
                                         "-c",
                                         "chown " + String.valueOf(userId) + ":" + String.valueOf(groupId)
                                               + " '" + filename + "'" };

        String[] result = executeExternalProcess(command);

        if (!result[2].equals("0")) {
            throw new FileSystemOperationException("Could not update UID and GID for '" + filename + "': "
                                                   + result[1]);
        }
    }

    /**
     *
     * @param filename the file name
     * @return the file user id
     * @throws FileSystemOperationException
     */
    private long getUserId(
                            String filename ) {

        try {
            Integer uid = (Integer) Files.getAttribute(new File(filename).toPath(), "unix:uid",
                                                       LinkOption.NOFOLLOW_LINKS);
            return uid.longValue();

        } catch (Exception e) {
            throw new FileSystemOperationException("Could not get UID for '" + filename + "'", e);
        }
    }

    /**
     *
     * @param filename the file name
     * @return the file owner
     * @throws FileSystemOperationException
     */
    private String getOwner(
                             String filename ) {

        try {
            UserPrincipal owner = Files.readAttributes(new File(filename).toPath(),
                                                       PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS)
                                       .owner();
            return owner.getName();

        } catch (Exception e) {
            throw new FileSystemOperationException("Could not get owner for '" + filename + "'", e);
        }
    }

    /**
     *
     * @param filename file name
     * @return the file group id
     * @throws FileSystemOperationException
     */
    private long getGroupId(
                             String filename ) {

        try {
            Integer gid = (Integer) Files.getAttribute(new File(filename).toPath(), "unix:gid",
                                                       LinkOption.NOFOLLOW_LINKS);
            return gid.longValue();

        } catch (Exception e) {
            throw new FileSystemOperationException("Could not get GID for '" + filename + "'", e);
        }
    }

    /**
     *
     * @param filename the file name
     * @return the file group name
     * @throws FileSystemOperationException
     */
    private String getGroup(
                             String filename ) {

        try {
            GroupPrincipal group = Files.readAttributes(new File(filename).toPath(),
                                                        PosixFileAttributes.class,
                                                        LinkOption.NOFOLLOW_LINKS)
                                        .group();
            return group.getName();

        } catch (Exception e) {
            throw new FileSystemOperationException("Could not get group for '" + filename + "'", e);
        }
    }

    /**
     * Copy directory
     * @param from source directory to copy from.
     * @param to directory destination.
     * @param filter array of names not to copy.
     * @param isRecursive should sub directories be copied too
     * @param failOnError set to true if you want to be thrown an exception, 
     * if there is still a process writing in the file that is being copied
     * @throws FileSystemOperationException
     */
    private void copyDirectoryInternal(
                                        File from,
                                        File to,
                                        String[] filter,
                                        boolean isRecursive,
                                        boolean failOnError ) {

        if (from == null || !from.exists() || !from.isDirectory()) {
            return;
        }

        if (!to.exists()) {
            if (!to.mkdirs()) {
                throw new FileSystemOperationException("Could not create target directory "
                                                       + to.getAbsolutePath());
            }
        }

        if (osType.isUnix()) {
            // set the original file permission to the new file
            setFilePermissions(to.getAbsolutePath(), getFilePermissions(from.getAbsolutePath()));
        }

        String[] list = from.list();

        // Some JVMs return null for File.list() when the directory is empty.
        if (list != null) {
            boolean skipThisFile = false;
            for (int i = 0; i < list.length; i++) {
                skipThisFile = false;
                String fileName = list[i];

                if (filter != null) {
                    for (int j = 0; j < filter.length; j++) {
                        if (fileName.equals(filter[j])) {
                            skipThisFile = true;
                            break;
                        }
                    }
                }

                if (!skipThisFile) {
                    File entry = new File(from, fileName);
                    if (entry.isDirectory()) {
                        if (isRecursive) {
                            copyDirectoryInternal(entry,
                                                  new File(to, fileName),
                                                  filter,
                                                  isRecursive,
                                                  failOnError);
                        } // else - skip
                    } else {
                        copyFile(entry.getAbsolutePath(),
                                 to.getAbsolutePath() + "/" + fileName,
                                 failOnError);
                    }
                }
            }
        }
    }

    /**
     * Checking file for existence
     *
     * @param file the file to check for existence
     * @throws FileDoesNotExistException if the file doesn't exist
     */
    private void checkFileExistence(
                                     File file ) throws FileDoesNotExistException {

        checkFileExistence(file, true);
    }

    private boolean checkFileExistence(
                                        File file,
                                        boolean failIfNotExist ) throws FileDoesNotExistException {

        boolean exists = file.exists();
        if (!exists) {
            if (failIfNotExist) {
                throw new FileDoesNotExistException(file);
            } else {
                log.warn("The " + (file.isDirectory()
                                                      ? "directory"
                                                      : "file")
                         + " does not exist");
            }
        }

        return exists;
    }

    /**
     * Check if the attribute is supported by the current OS
     *
     * @param attr file attribute
     * @throws AttributeNotSupportedException if the attribute is not supported by the current OS
     */
    private void checkAttributeOsSupport(
                                          FileAttributes attr ) throws AttributeNotSupportedException {

        switch (attr) {
            case PERMISSIONS:
            case GROUP:
            case OWNER:
            case GID:
            case UID:
                if (!this.osType.isUnix()) {
                    throw new AttributeNotSupportedException(attr, this.osType);
                }
                break;
            default:;
        }
    }

    /**
     *
     * @param files file list
     * @param fromDirName the source directory name
     * @param toDirName the destination directory name
     * @param outputStream the output stream
     * @param isRecursive whether to send files/folders recursively or not
     * @param failOnError set to true if you want to be thrown an exception, 
     * if there is still a process writing in the file that is being copied
     * @throws FileDoesNotExistException
     * @throws IOException
     */
    private void sendFilesToSocketStream(
                                          File[] files,
                                          String fromDirName,
                                          String toDirName,
                                          OutputStream outputStream,
                                          boolean isRecursive,
                                          boolean failOnError ) throws FileDoesNotExistException,
                                                                IOException {

        if (files != null) {
            // fix possible path on Windows: d:/work/path -> D:\work\path
            File fromDir = new File(fromDirName);
            fromDirName = fromDir.getCanonicalPath();
            /* 
             * getCanonicalPath() does not append slash or backslash at the end of the filepath, so we manually append it
             * This is done, because later, this slash or backslash is needed, when constructing the target file name
             */
            if (!fromDirName.endsWith(AtsSystemProperties.SYSTEM_FILE_SEPARATOR)) {
                fromDirName += AtsSystemProperties.SYSTEM_FILE_SEPARATOR;
            }
            if (!toDirName.endsWith("/") && !toDirName.endsWith("\\")) {
                toDirName += AtsSystemProperties.SYSTEM_FILE_SEPARATOR;
            }
            for (File file : files) {
                checkFileExistence(file);

                String fileName = file.getCanonicalPath();
                String toFileName = fileName.replace(fromDirName, toDirName);

                sendFileToSocketStream(file, toFileName, outputStream, failOnError);
                if (file.isDirectory() && isRecursive) {
                    /* Append slash, so we can concatenate files properly.
                     * Even though, on Windows, we well concatenate slash as well,
                     * it manages to transform it to \\, when saving files to disk,
                     * ( ..path\\to/file -> saved as ..path\\to\\file ) 
                     * whereas, Linux, thinks \\ or \ is part of the filename.
                     * ( ../path/to/file -> saved as ../path/to/\file )
                     */

                    sendFilesToSocketStream(file.listFiles(),
                                            file.getCanonicalPath(),
                                            toFileName,
                                            outputStream,
                                            isRecursive,
                                            failOnError);
                }
            }
        }
    }

    /**
     *
     * @param file the file to send
     * @param toFileName the destination file name
     * @param outputStream the output stream
     * @param failOnError set to true if you want to be thrown an exception, 
     * if there is still a process writing in the file that is being copied
     * @throws IOException
     */
    private void sendFileToSocketStream(
                                         File file,
                                         String toFileName,
                                         OutputStream outputStream,
                                         boolean failOnError ) throws IOException {

        FileInputStream fis = null;
        try {

            DataOutputStream dos = new DataOutputStream(outputStream);

            if (file.isDirectory()) {

                byte[] dirCreateCommandBytes = DIR_CREATE_SOCKET_COMMAND.getBytes(DEFAULT_CHARSET);
                dos.writeInt(dirCreateCommandBytes.length);
                dos.write(dirCreateCommandBytes);

                byte[] fileNameBytes = toFileName.getBytes(DEFAULT_CHARSET);
                dos.writeInt(fileNameBytes.length);
                dos.write(fileNameBytes);
                dos.flush();
            } else {

                long initialFileSize = file.length();
                long bytesLeftToWrite = initialFileSize;

                byte[] fileCopyCommandBytes = FILE_COPY_SOCKET_COMMAND.getBytes(DEFAULT_CHARSET);
                dos.writeInt(fileCopyCommandBytes.length);
                dos.write(fileCopyCommandBytes);

                byte[] fileNameBytes = toFileName.getBytes(DEFAULT_CHARSET);
                dos.writeInt(fileNameBytes.length);
                dos.write(fileNameBytes);
                dos.writeLong(initialFileSize);

                fis = new FileInputStream(file);
                byte[] buff = new byte[FILE_TRANSFER_BUFFER_SIZE];
                int bytesCount = -1;
                while ( (bytesCount = fis.read(buff)) > -1) {
                    if (bytesCount <= bytesLeftToWrite) {
                        dos.write(buff, 0, bytesCount);
                        dos.flush();
                        bytesLeftToWrite -= bytesCount;
                    } else {
                        if (failOnError) {
                            throw new FileSystemOperationException("The size of file \"" + file.getName()
                                                                   + "\" was increased with "
                                                                   + (bytesCount - bytesLeftToWrite)
                                                                   + " bytes! The initial file size was "
                                                                   + initialFileSize
                                                                   + ". ATS will ignore this error if you set the failOnError flag to false.");
                        }
                        // The file is growing while we are sending it.
                        // We will send only the initial number of bytes, because we already told the recipient side how many bytes to expect.
                        dos.write(buff, 0, (int) bytesLeftToWrite);
                        bytesLeftToWrite = 0;
                        dos.flush();

                        // we have sent as many bytes as we told the recipient, we will not send the remaining bytes
                        break;
                    }
                }

                if (bytesLeftToWrite > 0) {
                    if (failOnError) {
                        throw new FileSystemOperationException("The size of file \"" + file.getName()
                                                               + "\" was decreased with " + bytesLeftToWrite
                                                               + " bytes! The initial file size was "
                                                               + initialFileSize
                                                               + ". ATS will ignore this error if you set the failOnError flag to false.");
                    }
                    // The file is shrinking while we are sending it.
                    // We will fill it to the initial size, because we already told the recipient side how many bytes to expect.
                    log.warn("File " + file.getPath()
                             + " is getting smaller while copying it. We will append " + bytesLeftToWrite
                             + " zero bytes to reach its initial size of " + initialFileSize + " bytes");
                    while (bytesLeftToWrite-- > 0) {
                        dos.write(0);
                    }
                    dos.flush();
                }
            }
        } finally {

            IoUtils.closeStream(fis);
        }
    }

    /**
     * Get the files in a folder which match the given {@link FileFilter}
     *
     * @param startLocation the start location in which to look
     * @param searchFilter the search filter to apply
     * @param recursiveSearch
     * @return list of matching files
     */
    private List<String> getMatchingFiles(
                                           File startLocation,
                                           FileFilter searchFilter,
                                           boolean recursiveSearch ) {

        List<String> matchingFiles = new ArrayList<String>();

        File[] files = startLocation.listFiles();
        // listFiles() can return 'null' even when we have no rights to read in the 'startLocaltion' directory
        if (files != null) {

            for (File child : files) {
                if (searchFilter.accept(child)) {
                    try {
                        String path = child.getCanonicalPath();
                        if (child.isDirectory()) {
                            // when the path points to a directory, we add file separator character at the end
                            matchingFiles.add(IoUtils.normalizeDirPath(path));
                        } else {
                            matchingFiles.add(path);
                        }
                    } catch (IOException ioe) {
                        throw new FileSystemOperationException("Could not get the canonical path of file: "
                                                               + child.getAbsolutePath(), ioe);
                    }
                }

                //if recursion is allowed
                if (recursiveSearch && child.isDirectory()) {
                    matchingFiles.addAll(getMatchingFiles(child, searchFilter, recursiveSearch));
                }
            }
        }
        return matchingFiles;
    }

    /**
     * Class for filtering file names based on a pattern
     */
    private static class FileNameSearchFilter implements FileFilter {

        private String  searchPattern;
        private boolean isRegex;
        private boolean acceptDirectories;

        private FileNameSearchFilter( String searchPattern,
                                      boolean isRegex,
                                      boolean acceptDirectories ) {

            this.searchPattern = searchPattern;
            this.isRegex = isRegex;
            this.acceptDirectories = acceptDirectories;
        }

        /* (non-Javadoc)
         * @see java.io.FileFilter#accept(java.io.File)
         */
        public boolean accept(
                               File pathname ) {

            boolean fileMatches = false;

            if (pathname.isFile() || acceptDirectories) {
                if ( (isRegex && pathname.getName().matches(searchPattern))
                     || (!isRegex && pathname.getName().equals(searchPattern))) {

                    fileMatches = true;
                }
            }
            return fileMatches;
        }
    }

    /**
     * File transfer status holder<br/>
     * Used for waiting the file transfer to complete<br/>
     * <br/>
     * <b>NOTE</b>: We can't use {@link Boolean} instead of this class, because in some moment we have to
     * change the boolean value, but {@link Boolean} is immutable and we also need to use the same object,
     * because we are synchronizing on it.
     */
    private class FileTransferStatus {

        boolean   finished = false;
        /**
         * Any exception that might be caught in the file/dir reading thread.
         */
        Exception transferException;
    }

    /**
     * Unzip file to local or remote machine. If the machine is UNIX-like it will preserve the permissions
     *
     * @param zipFilePath the zip file path
     * @param outputDirPath output directory. The directory will be created if it does not exist
     * @throws FileSystemOperationException
     */
    @Deprecated
    @Override
    public void unzip(
                       String zipFilePath,
                       String outputDirPath ) throws FileSystemOperationException {

        ZipArchiveEntry zipEntry = null;
        File outputDir = new File(outputDirPath);
        outputDir.mkdirs();//check if the dir is created

        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            Enumeration<? extends ZipArchiveEntry> entries = zipFile.getEntries();
            int unixPermissions = 0;

            while (entries.hasMoreElements()) {
                zipEntry = entries.nextElement();
                if (log.isDebugEnabled()) {
                    log.debug("Extracting " + zipEntry.getName());
                }
                File entryDestination = new File(outputDirPath, zipEntry.getName());

                unixPermissions = zipEntry.getUnixMode();
                if (zipEntry.isDirectory()) {
                    entryDestination.mkdirs();
                } else {
                    entryDestination.getParentFile().mkdirs();
                    InputStream in = null;
                    OutputStream out = null;

                    in = zipFile.getInputStream(zipEntry);
                    out = new BufferedOutputStream(new FileOutputStream(entryDestination));

                    IoUtils.copyStream(in, out);
                }
                if (OperatingSystemType.getCurrentOsType() != OperatingSystemType.WINDOWS) {//check if the OS is UNIX
                    // set file/dir permissions, after it is created
                    Files.setPosixFilePermissions(entryDestination.getCanonicalFile().toPath(),
                                                  getPosixFilePermission(unixPermissions));
                }
            }
        } catch (Exception e) {
            String errorMsg = "Unable to unzip " + ( (zipEntry != null)
                                                                        ? zipEntry.getName() + " from "
                                                                        : "")
                              + zipFilePath + ".Target directory '" + outputDirPath
                              + "' is in inconsistent state.";
            throw new FileSystemOperationException(errorMsg, e);
        }
    }

    /**
     * Extract archive file to local or remote machine.
     * Supported file formats are Zip, GZip, TAR and TAR GZip
     * If the machine is UNIX-like it will preserve the permissions
     *
     * @param archiveFilePath the archive file path
     * @param outputDirPath output directory. The directory will be created if it does not exist
     * @throws FileSystemOperationException
     */
    @Override
    public void extract( String archiveFilePath, String outputDirPath ) throws FileSystemOperationException {

        if (archiveFilePath.endsWith(".zip")) {
            extractZip(archiveFilePath, outputDirPath);
        } else if (archiveFilePath.endsWith(".gz") && !archiveFilePath.endsWith(".tar.gz")) {
            extractGZip(archiveFilePath, outputDirPath);
        } else if (archiveFilePath.endsWith("tar.gz")) {
            extractTarGZip(archiveFilePath, outputDirPath);
        } else if (archiveFilePath.endsWith(".tar")) {
            extractTar(archiveFilePath, outputDirPath);
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

    private void extractTar( String tarFilePath, String outputDirPath ) {

        TarArchiveEntry entry = null;
        try (TarArchiveInputStream tis = new TarArchiveInputStream(new FileInputStream(tarFilePath))) {
            while ( (entry = (TarArchiveEntry) tis.getNextEntry()) != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Extracting " + entry.getName());
                }
                File entryDestination = new File(outputDirPath, entry.getName());
                if (entry.isDirectory()) {
                    entryDestination.mkdirs();
                } else {
                    entryDestination.getParentFile().mkdirs();
                    OutputStream out = new BufferedOutputStream(new FileOutputStream(entryDestination));
                    IoUtils.copyStream(tis, out, false, true);
                }
                if (OperatingSystemType.getCurrentOsType() != OperatingSystemType.WINDOWS) {//check if the OS is UNIX
                    // set file/dir permissions, after it is created
                    Files.setPosixFilePermissions(entryDestination.getCanonicalFile().toPath(),
                                                  getPosixFilePermission(entry.getMode()));
                }
            }
        } catch (Exception e) {
            String errorMsg = null;
            if (entry != null) {
                errorMsg = "Unable to untar " + entry.getName() + " from " + tarFilePath
                           + ".Target directory '" + outputDirPath + "' is in inconsistent state.";
            } else {
                errorMsg = "Could not read data from " + tarFilePath;
            }
            throw new FileSystemOperationException(errorMsg, e);
        }

    }

    private void extractTarGZip( String tarGzipFilePath, String outputDirPath ) {

        TarArchiveEntry entry = null;
        try (TarArchiveInputStream tis = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(tarGzipFilePath)))) {
            while ( (entry = (TarArchiveEntry) tis.getNextEntry()) != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Extracting " + entry.getName());
                }
                File entryDestination = new File(outputDirPath, entry.getName());
                if (entry.isDirectory()) {
                    entryDestination.mkdirs();
                } else {
                    entryDestination.getParentFile().mkdirs();
                    OutputStream out = new BufferedOutputStream(new FileOutputStream(entryDestination));
                    IoUtils.copyStream(tis, out, false, true);
                }
                if (OperatingSystemType.getCurrentOsType() != OperatingSystemType.WINDOWS) {//check if the OS is UNIX
                    // set file/dir permissions, after it is created
                    Files.setPosixFilePermissions(entryDestination.getCanonicalFile().toPath(),
                                                  getPosixFilePermission(entry.getMode()));
                }
            }
        } catch (Exception e) {
            String errorMsg = null;
            if (entry != null) {
                errorMsg = "Unable to gunzip " + entry.getName() + " from " + tarGzipFilePath
                           + ".Target directory '" + outputDirPath + "' is in inconsistent state.";
            } else {
                errorMsg = "Could not read data from " + tarGzipFilePath;
            }
            throw new FileSystemOperationException(errorMsg, e);
        }

    }

    private void extractGZip( String gzipFilePath, String outputDirPath ) {

        String outputFileName = new File(gzipFilePath).getName();
        outputFileName = outputFileName.substring(0, outputFileName.lastIndexOf(".gz"));
        String outputFilePath = outputDirPath + File.separator + outputFileName;
        new File(outputDirPath).mkdirs();
        InputStream in = null;
        try {
            String filePermissions = getFilePermissions(gzipFilePath);
            in = new GZIPInputStream(new FileInputStream(gzipFilePath));
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFilePath));

            IoUtils.copyStream(in, out);
            if (OperatingSystemType.getCurrentOsType() != OperatingSystemType.WINDOWS) {//check if the OS is UNIX
                // set file permissions, after it is created
                this.setFilePermissions(outputFilePath, filePermissions);
            }

        } catch (Exception e) {
            String errorMsg = "Unable to gunzip " + gzipFilePath + ".Target directory '" + outputDirPath
                              + "' is in inconsistent state.";
            throw new FileSystemOperationException(errorMsg, e);
        } finally {
            IoUtils.closeStream(in, "Could not close stream for file '" + gzipFilePath + "'");
        }

    }

    private void extractZip( String zipFilePath, String outputDirPath ) {

        ZipArchiveEntry zipEntry = null;
        File outputDir = new File(outputDirPath);
        outputDir.mkdirs();//check if the dir is created

        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            Enumeration<? extends ZipArchiveEntry> entries = zipFile.getEntries();
            int unixPermissions = 0;

            while (entries.hasMoreElements()) {
                zipEntry = entries.nextElement();
                if (log.isDebugEnabled()) {
                    log.debug("Extracting " + zipEntry.getName());
                }
                File entryDestination = new File(outputDirPath, zipEntry.getName());

                unixPermissions = zipEntry.getUnixMode();
                if (zipEntry.isDirectory()) {
                    entryDestination.mkdirs();
                } else {
                    entryDestination.getParentFile().mkdirs();
                    InputStream in = null;
                    OutputStream out = null;

                    in = zipFile.getInputStream(zipEntry);
                    out = new BufferedOutputStream(new FileOutputStream(entryDestination));

                    IoUtils.copyStream(in, out);
                }
                if (OperatingSystemType.getCurrentOsType() != OperatingSystemType.WINDOWS) {//check if the OS is UNIX
                    // set file/dir permissions, after it is created
                    Files.setPosixFilePermissions(entryDestination.getCanonicalFile().toPath(),
                                                  getPosixFilePermission(unixPermissions));
                }
            }
        } catch (Exception e) {
            String errorMsg = "Unable to unzip " + ( (zipEntry != null)
                                                                        ? zipEntry.getName() + " from "
                                                                        : "")
                              + zipFilePath + ".Target directory '" + outputDirPath
                              + "' is in inconsistent state.";
            throw new FileSystemOperationException(errorMsg, e);
        }

    }

    private Set<PosixFilePermission> getPosixFilePermission(
                                                             int permissions ) {

        Set<PosixFilePermission> filePermissions = new HashSet<PosixFilePermission>();

        // using bitwise operations check the file permissions in decimal
        // numeric system
        // e.g. 100100 AND 100, we will have for result 100
        if ( (permissions & 256) > 0) {
            filePermissions.add(PosixFilePermission.OWNER_READ);
        }
        if ( (permissions & 128) > 0) {
            filePermissions.add(PosixFilePermission.OWNER_WRITE);
        }
        if ( (permissions & 64) > 0) {
            filePermissions.add(PosixFilePermission.OWNER_EXECUTE);
        }
        if ( (permissions & 32) > 0) {
            filePermissions.add(PosixFilePermission.GROUP_READ);
        }
        if ( (permissions & 16) > 0) {
            filePermissions.add(PosixFilePermission.GROUP_WRITE);
        }
        if ( (permissions & 8) > 0) {
            filePermissions.add(PosixFilePermission.GROUP_EXECUTE);
        }
        if ( (permissions & 4) > 0) {
            filePermissions.add(PosixFilePermission.OTHERS_READ);
        }
        if ( (permissions & 2) > 0) {
            filePermissions.add(PosixFilePermission.OTHERS_WRITE);
        }
        if ( (permissions & 1) > 0) {
            filePermissions.add(PosixFilePermission.OTHERS_EXECUTE);
        }

        return filePermissions;
    }

    private String[] executeExternalProcess(
                                             String[] command ) {

        String stdOut = "";
        String stdErr = "";
        String exitCode = "";
        try {
            // start the external process
            Process process = Runtime.getRuntime().exec(command);

            // read the external process output streams
            // reading both streams helps releasing OS resources
            stdOut = IoUtils.streamToString(process.getInputStream()).trim();
            stdErr = IoUtils.streamToString(process.getErrorStream()).trim();

            //process.getOutputStream().close();

            exitCode = String.valueOf(process.waitFor());
        } catch (Exception e) {
            StringBuilder err = new StringBuilder();
            err.append("Error executing command '");
            err.append(Arrays.toString(command));
            err.append("'");
            if (!StringUtils.isNullOrEmpty(stdOut)) {
                err.append("\nSTD OUT: ");
                err.append(stdOut);
            }
            if (!StringUtils.isNullOrEmpty(stdErr)) {
                err.append("\nSTD ERR: ");
                err.append(stdErr);
            }
            if (!StringUtils.isNullOrEmpty(exitCode)) {
                err.append("\nexit code: ");
                err.append(exitCode);
            }
            throw new FileSystemOperationException(err.toString(), e);
        }

        return new String[]{ stdOut, stdErr, exitCode };
    }

    /**
     * This is method for internal use only.
     * Currently used on the agent when copying files from the executor
     * 
     * @param srcFilename the file name without path, existing on the executor
     * @param dstFilePath the target file path - desired file location (directory or directory + file name)
     * 
     * @return full file path on the agent, used for file copy operations
     * */
    public String constructDestinationFilePath(
                                                String srcFileName,
                                                String dstFilePath ) throws Exception {

        File dstFile = new File(dstFilePath);

        // check if file exists
        if (dstFile.exists()) {
            // check if dstFile is indeed a File
            if (dstFile.isFile()) {
                log.debug(" will overwrite '" + dstFilePath + "'");
                return dstFilePath;
            } else if (dstFile.isDirectory()) {// check if dstFile is a directory
                dstFilePath = new File(dstFilePath, srcFileName).getAbsolutePath();
                return dstFilePath;
            } else {
                throw new IllegalArgumentException("File '" + dstFilePath
                                                   + "' is neither file nor directory");
            }
        } else {
            // check if dstFile points to a directory (which is not existing)
            if (dstFilePath.endsWith(File.separator)) {
                throw new FileNotFoundException("File path '" + dstFilePath
                                                + "' points to a non-existing directory");
            } else {
                // dstFile points to a non-existing file
                // check if dstFile's parent exists and is directory
                File dstFileParent = new File(dstFilePath).getParentFile();
                if (dstFileParent.exists() && dstFileParent.isDirectory()) {
                    return dstFilePath;
                } else {
                    throw new FileNotFoundException("Directory '" + dstFileParent.getAbsolutePath()
                                                    + "' does not exist or is a regular file");
                }
            }
        }
    }

//    private Charset loadCharset( String charset ) {
//
//        if (StringUtils.isNullOrEmpty(charset) || StandardCharsets.ISO_8859_1.name().equals(charset)) {
//            return StandardCharsets.ISO_8859_1;
//        }
//        if (StandardCharsets.US_ASCII.name().equals(charset)) {
//            return StandardCharsets.US_ASCII;
//        }
//        if (StandardCharsets.UTF_16.name().equals(charset)) {
//            return StandardCharsets.UTF_16;
//        }
//        if (StandardCharsets.UTF_16BE.name().equals(charset)) {
//            return StandardCharsets.UTF_16BE;
//        }
//        if (StandardCharsets.UTF_16LE.name().equals(charset)) {
//            return StandardCharsets.UTF_16LE;
//        }
//        if (StandardCharsets.UTF_8.name().equals(charset)) {
//            return StandardCharsets.UTF_8;
//        }
//
//        throw new IllegalArgumentException("Charset '" + charset
//                                           + "' is not supported. See java.nio.charset.StandardCharsets for supported ones.");
//    }
}
