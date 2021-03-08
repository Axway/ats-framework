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
package com.axway.ats.core.filetransfer.model;

import com.axway.ats.core.filetransfer.AbstractFileTransferClient;
import com.axway.ats.common.filetransfer.FileTransferException;

/**
 * Common methods that each {@link IFileTransferClient} should implement
 */
public interface IFileTransferClient {

    /**
     * Set the {@link com.axway.ats.common.filetransfer.TransferMode} that this protocol should use
     *
     * @param mode
     * @throws FileTransferException
     */
    public void setTransferMode(
                                 com.axway.ats.common.filetransfer.TransferMode mode )
                                                                                       throws FileTransferException;

    /**
     * Upload a the file to the specified directory and with the specified file name
     *
     * @param localFile the local file to upload
     * @param remoteDir the remote directory to upload the file to
     * @param remoteFile the remote file name that the file should have
     * @throws FileTransferException
     */
    public void uploadFile(
                            String localFile,
                            String remoteDir,
                            String remoteFile ) throws FileTransferException;

    /**
     * Download a file from the specified directory and with the specified file name
     *
     * @param localFile the local file that will be created
     * @param remoteDir the remote directory to download from
     * @param remoteFile the remote file to download
     * @throws FileTransferException
     */
    public void downloadFile(
                              String localFile,
                              String remoteDir,
                              String remoteFile ) throws FileTransferException;

    /**
     * Connect to a remote host using basic authentication
     *
     * @param hostname the host to connect to
     * @param userName the user name
     * @param password the password for the provided user name
     * @throws FileTransferException
     */
    public void connect(
                         String hostname,
                         String userName,
                         String password ) throws FileTransferException;

    /**
     * Connect to a remote host using secure authentication
     *
     * @param hostname the host to connect to
     * @param keystoreFile the file containing the key store
     * @param keystorePassword the key store password
     * @param publicKeyAlias the public key alias
     * @throws FileTransferException
     */
    public void connect(
                         String hostname,
                         String keystoreFile,
                         String keystorePassword,
                         String publicKeyAlias ) throws FileTransferException;

    /**
     * Disconnect from the remote host
     *
     * @throws FileTransferException
     */
    public void disconnect() throws FileTransferException;

    /**
     * Sets a custom port that would be used when connecting to the remote host
     *
     * @param port the port number
     */
    public void setCustomPort(
                               int port );

    /**
     * Overrides the default connection timeout with a new custom value
     *
     * @param newValue the new value of the default connection timeout
     */
    public void setConnectionTimeout(
                                      int newValue );

    /**
     * Set a client key store which will be used for authentication
     * @param keystoreFile the key store file path ( Must be in JKS or PKCS12 format )
     * @param keystorePassword the key store password
     * @param alias the private key alias ( Currently this parameter is used only for SFTP operations, for other cases, you may pass null as well)
     * **/
    public void setKeystore( String keystoreFile, String keystorePassword, String alias );

    /**
     * Set the client trust store which will be used for validating trust server certificates
     * @param truststoreFile the trust store file path ( Must be in JKS or PKCS12 format )
     * @param truststorePassword the trust store password
     * **/
    public void setTrustStore( String truststoreFile, String truststorePassword );

    /**
     * Set the trust store certificate which will be used for connection/session validation
     * @param certificateFile the trust server certificate file path ( Must be in .PEM format )
     * **/
    public void setTrustedServerSSLCertificate( String certificateFile );

    /**
     * Execute some custom command. This is specific for each protocol and remote server.
     * 
     * @param command the command to run
     * @return the command output
     * @throws FileTransferException thrown on failure
     */
    public String executeCommand(
                                  String command ) throws FileTransferException;

    /**
     * Resumes a transfer that was started and paused.
     * This method should be called for one use of a start[Transfer]AndPause method.
     *
     * @throws FileTransferException If the transfer fails for some reason.
     */
    public void resumePausedTransfer() throws FileTransferException;

    /**
     * Starts an upload and pauses it before the file is uploaded.
     * Resume the upload with the {@link #resumePausedTransfer()} method.
     * If an error occurs it is logged but an exception is not thrown here since
     * the operation is performed in a different thread. The current thread is interrupted
     * and the exception should be thrown in the {@link #resumePausedTransfer()} method.
     * @param localFile the local file to upload
     * @param remoteDir the remote directory to upload the file to
     * @param remoteFile the remote file name that the file should have
     * @throws FileTransferException
     */
    public void startUploadAndPause(
                                     final String localFile,
                                     final String remoteDir,
                                     final String remoteFile ) throws FileTransferException;

    /**
     * Enable or disable response gathering. If enabled the responses for each protocol command
     * will be gathered.
     * @param enable <b>true</b> to enable, <b>false</b> to disable response gathering.
     */
    public void enableResponseCollection(
                                          boolean enable );

    /**
     * @return array of all the responses so far.
     */
    public String[] getResponses();

    /**
     * Method to pass additional protocol-specific parameters to the client.
     * @param key property key name. About possible values check the protocol specific client implementations
     * @param value object with supported value
     * @throws IllegalArgumentException if the key or value is not supported
     */
    public void addCustomProperty(
                                   String key,
                                   Object value ) throws IllegalArgumentException;

    /**
     * Method used to apply custom parameters for the specific client before connect.
     * If you develop custom client and do not support any custom parameter it
     * is advised just to extend {@link AbstractFileTransferClient}. It has empty implementation
     * <p>Note: For implementations you should use the {@link #customProperties} Map</p>
     */
    public void applyCustomProperties() throws IllegalArgumentException;

    /**
     * Check for turned debug mode in order clients to log more info
     * @return true if set
     */
    public boolean isDebugMode();

    /**
     * Set debug mode in order to suggest clients to log more info
     * @param turnDebug true to set debug mode on
     */
    public void setDebugMode(
                              boolean turnDebug );
}
