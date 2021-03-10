/*
 * Copyright 2020 Axway Software
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

import com.axway.ats.common.filesystem.FileSystemOperationException;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.core.utils.IoUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class FileTransferReader {
    private final static Logger              log     = LogManager.getLogger(FileTransferReader.class);
    private final static OperatingSystemType OS_TYPE = OperatingSystemType.getCurrentOsType();

    /**
     * Open file transfer socket
     *
     * @throws com.axway.ats.common.filesystem.FileSystemOperationException
     */
    public static void readTransfer( String host, int port,
                                     final LocalFileSystemOperations.FileTransferStatus transferStatus )
            throws FileSystemOperationException {

        try {
            final Socket socket = new Socket(host, port);
            if (log.isDebugEnabled()) {
                log.debug("Starting file transfer reader socket to " + host + ":" + port);
            }
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    FileOutputStream fos = null;
                    DataInputStream dis = null;
                    try {
                        //socket.setReuseAddress(true);
                        socket.setSoTimeout(LocalFileSystemOperations.FILE_TRANSFER_TIMEOUT);

                        dis = new DataInputStream(socket.getInputStream());
                        int fdTypeLength = dis.readInt();
                        for (; ; ) {
                            checkParamLengthForSocketTransfer(fdTypeLength, "file type length");
                            String fdType = readString(dis, fdTypeLength); // directory or file
                            int fileNameLength = dis.readInt();
                            checkParamLengthForSocketTransfer(fileNameLength, "file name length");
                            String fileName = readString(dis, fileNameLength);
                            fileName = IoUtils.normalizeFilePath(fileName,
                                                                 OS_TYPE); // switch file separators according to the current OS
                            File file = new File(fileName);
                            if (fdType.equals(LocalFileSystemOperations.FILE_COPY_SOCKET_COMMAND)) {

                                long fileSize = dis.readLong();
                                if (log.isDebugEnabled()) {
                                    log.debug("Creating file: " + fileName + " with size: " + fileSize + " bytes");
                                }

                                // check if file's directory (full directory path) exists
                                // if not, try to create all missing parent directories
                                if (!file.getParentFile().exists()) {
                                    // the file's parent directory does not exist
                                    if (!file.getParentFile().mkdirs()) {
                                        throw new IOException("Could not create parent directories of file '" + file
                                                              + "'. File transfer is interrupted.");
                                    }
                                }

                                try {
                                    fos = new FileOutputStream(file, false);
                                } catch (IOException e) {
                                    throw new IOException("Could not create destination file '" + fileName + "'", e);
                                }
                                try {
                                    byte[] buff = new byte[LocalFileSystemOperations.FILE_TRANSFER_BUFFER_SIZE];
                                    int readBytes = -1;
                                    while (fileSize > 0 && (readBytes = dis.read(buff,
                                                                                 0,
                                                                                 (int) Math.min(buff.length,
                                                                                                fileSize))) > -1) {
                                        fos.write(buff, 0, readBytes);
                                        fos.flush();
                                        fileSize -= readBytes;
                                    }
                                } finally {
                                    IoUtils.closeStream(fos, "Error closing descriptor for file " + fileName);
                                }
                            } else if (fdType.equals(LocalFileSystemOperations.DIR_CREATE_SOCKET_COMMAND)) {
                                if (!file.exists()) {
                                    log.debug("Creating directory: " + fileName);
                                    if (!file.mkdirs()) {
                                        throw new IOException(
                                                "Could not create all directories for path '" + fileName + "'");
                                    }
                                }
                            } else {
                                log.error("Unknown socket command (must be the file descriptor type): " + fdType);
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
                        // timeout usually will be when waiting for client connection but theoretically could be also
                        // in the middle of reading data
                        log.error("Reached timeout of " + (LocalFileSystemOperations.FILE_TRANSFER_TIMEOUT / 1000)
                                  + " seconds while waiting for file/directory copy operation.", ste);
                        transferStatus.transferException = ste;
                    } catch (IOException e) {
                        log.error("An I/O error occurred", e);
                        transferStatus.transferException = e;
                    } finally {
                        IoUtils.closeStream(fos);
                        IoUtils.closeStream(dis);
                        IoUtils.closeStream(socket, "Could not close socket");

                        synchronized (transferStatus) {
                            transferStatus.finished = true;
                            transferStatus.notify();
                        }
                    }
                }

                private void checkParamLengthForSocketTransfer(
                        int cmdParamLength,
                        String commandType ) throws IOException {

                    if (cmdParamLength > LocalFileSystemOperations.INTERNAL_SOCKET_PARAMETER_MAX_LENGTH) {
                        throw new IOException("Illegal length for command " + commandType + ": "
                                              + cmdParamLength + "(max allowed is "
                                              + LocalFileSystemOperations.INTERNAL_SOCKET_PARAMETER_MAX_LENGTH
                                              + "); Probably non ATS agent has connected. Closing communication");
                    }
                }

                /**
                 * Reads some bytes from stream and converts them to string
                 * @param dis data input stream
                 * @param length the length of bytes to be read
                 * @return the String representation of the read bytes
                 * @throws IOException
                 */
                private String readString( DataInputStream dis, int length ) throws IOException {

                    byte[] buff = new byte[length];
                    // this method blocks until the specified bytes are read from the stream
                    dis.readFully(buff, 0, length);
                    return new String(buff, LocalFileSystemOperations.DEFAULT_CHARSET);
                }

            });

            thread.setName("ATSFileTransferSocketReader-port" + port + "__" + thread.getName());
            thread.start();
        } catch (Exception e) {
            throw new FileSystemOperationException("Unable to open file transfer socket to " + host + ":" + port, e);
        }
    }
}
