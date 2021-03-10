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
package com.axway.ats.action.filetransfer;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.action.ActionLibraryConfigurator;
import com.axway.ats.action.http.FileTransferHttpClient;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.filetransfer.FileTransferException;
import com.axway.ats.common.filetransfer.SshCipher;
import com.axway.ats.common.filetransfer.TransferMode;
import com.axway.ats.common.filetransfer.TransferProtocol;
import com.axway.ats.core.filetransfer.FtpsClient;
import com.axway.ats.core.filetransfer.SftpClient;
import com.axway.ats.core.filetransfer.model.IFileTransferClient;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.validation.Validate;
import com.axway.ats.core.validation.ValidationType;
import com.axway.ats.core.validation.Validator;

/**
 * The <code>FileTransferClient</code> can be used to execute actions specific for
 * the different file transfer protocols. The supported protocols are :
 * <ul>
 * <li>FTP, FTPS</li>
 * <li>HTTP, HTTPS</li>
 * <li>SFTP</li>
 * </ul>
 *
 * <b>User guide</b> pages related to this class:
 * <ul>
 *   <li><a href="https://axway.github.io/ats-framework/Basic-functionalities.html#Basicfunctionalities-FileTransfers">basic</a></li>
 *   <li><a href="https://axway.github.io/ats-framework/File-transfers.html">detailed</a></li>
 * </ul>
 */
@PublicAtsApi
public class FileTransferClient {

    private static final Logger   log                                              = LogManager.getLogger(FileTransferClient.class);

    /** Constants for setting FTPS connection type. */
    @PublicAtsApi
    public static final String    FTPS_CONNNECTION_TYPE                            = FtpsClient.FTPS_CONNECTION_TYPE;                             // duplicate in order to prevent reverse reference from CoreLibrary to ActionLibrary
    @PublicAtsApi
    public static final Integer   FTPS_CONNNECTION_TYPE__IMPLICIT                  = FtpsClient.FTPS_CONNECTION_TYPE__IMPLICIT;
    @PublicAtsApi
    public static final Integer   FTPS_CONNNECTION_TYPE__AUTH_SSL                  = FtpsClient.FTPS_CONNECTION_TYPE__AUTH_SSL;
    @PublicAtsApi
    public static final Integer   FTPS_CONNNECTION_TYPE__AUTH_TLS                  = FtpsClient.FTPS_CONNECTION_TYPE__AUTH_TLS;

    /** FTPS encryption protocol. Currently only one value can be specified */
    @PublicAtsApi
    public static final String    FTPS_ENCRYPTION_PROTOCOLS                        = FtpsClient.FTPS_ENCRYPTION_PROTOCOLS;

    /** Username when authenticating over SFTP. If not specified, the public key alias name is used instead of a user name */
    @PublicAtsApi
    public static final String    SFTP_USERNAME                                    = SftpClient.SFTP_USERNAME;
    /** Accepts SFTP {@link SshCipher} or {@link SshCipher}s array. Only the specified ciphers will be used */
    @PublicAtsApi
    public static final String    SFTP_CIPHERS                                     = SftpClient.SFTP_CIPHERS;

    /** Property for customizing the HTTP/HTTPS client's socket buffer in bytes */
    @PublicAtsApi
    public final static String    HTTP_HTTPS_SOCKET_BUFFER_SIZE                    = FileTransferHttpClient.SOCKET_BUFFER_SIZE;

    /** Property for customizing the HTTP/HTTPS client's request <strong>Transfer-Encoding</strong>.<br>
     *  Currently only supported value is <strong>chunked</strong>
     *  */
    @PublicAtsApi
    public final static String    HTTP_HTTPS_TRANSFER_ENCODING_MODE                = FileTransferHttpClient.TRANSFER_ENCODING_MODE;
    @PublicAtsApi
    public final static String    HTTP_HTTPS_TRANSFER_ENCODING_MODE__CHUNKED       = FileTransferHttpClient.TRANSFER_ENCODING_MODE_CHUNKED;
    /**
     * Constants for setting HTTP/HTTPS client's upload method.
     * The default value is PUT
     */
    @PublicAtsApi
    public final static String    HTTP_HTTPS_UPLOAD_METHOD                         = FileTransferHttpClient.UPLOAD_METHOD;
    @PublicAtsApi
    public final static String    HTTP_HTTPS_UPLOAD_METHOD__PUT                    = FileTransferHttpClient.UPLOAD_METHOD__PUT;
    @PublicAtsApi
    public final static String    HTTP_HTTPS_UPLOAD_METHOD__POST                   = FileTransferHttpClient.UPLOAD_METHOD__POST;

    /**
     * By default the HTTP/HTTPS client will first try to do the transfer without passing user credentials,
     * if it fail due to "not authorized" then it will retry with the credentials passed.
     * This constant allows forcing the client to pass the user credentials with the very first headers.
     */
    @PublicAtsApi
    public final static String    HTTP_HTTPS_PREEMPTIVE_BASIC_AUTHENTICATION       = FileTransferHttpClient.PREEMPTIVE_BASIC_AUTHENTICATION;
    @PublicAtsApi
    public final static String    HTTP_HTTPS_PREEMPTIVE_BASIC_AUTHENTICATION__TRUE = FileTransferHttpClient.PREEMPTIVE_BASIC_AUTHENTICATION__TRUE;

    /**
     * Allows adding HTTP headers to the HTTP/S requests.
     * The values passed must be in the form '<header key>:<header value>'
     */
    @PublicAtsApi
    public final static String    HTTP_HTTPS_REQUEST_HEADER                        = FileTransferHttpClient.REQUEST_HEADER;

    /**
     * By the default the content type header on upload is set to "application/octet-stream".
     * This constant allows setting some other value for this header.
     */
    @PublicAtsApi
    public final static String    HTTP_HTTPS_UPLOAD_CONTENT_TYPE                   = FileTransferHttpClient.UPLOAD_CONTENT_TYPE;

    /**
     * Property to specify time in milliseconds for socket read timeout. By default there is no timeout and client waits indefinitely.
     * Value could be specified either as Integer object or String representing the number.
     */
    @PublicAtsApi
    public final static String    HTTP_HTTPS_SOCKET_READ_TIMEOUT                   = FileTransferHttpClient.SOCKET_READ_TIMEOUT;

    protected IFileTransferClient client                                           = null;

    protected FileTransferClient() {

        // this constructor is used as an alternative for classes that extend
        // this one, in
        // case their logic does not require the same actions that are
        // undertaken by the other
        // parameterized constructor
    }

    /**
     * Overrides the default connection timeout with a new custom value
     *
     * @param newValue the new value of the default connection timeout in milliseconds
     */
    @PublicAtsApi
    public void setConnectionTimeout( int newValue ) {

        this.client.setConnectionTimeout(newValue);
    }

    /**
     * Creates a new file transfer client, which would work with the specified
     * {@link TransferProtocol}
     *
     * @param protocol the {@link TransferProtocol} the client would work with
     */
    @PublicAtsApi
    public FileTransferClient( TransferProtocol protocol ) {

        try {
            int port = protocol.getDefaultPort();

            // a product specific client
            this.client = ClientFactory.getInstance().getClient(protocol, port);

            this.client.setDebugMode(ActionLibraryConfigurator.getInstance().getFileTransferVerboseMode());

        } catch (Exception e) {
            throw new FileTransferException(e);
        }
    }

    /**
     * Creates a new file transfer client, which would work with the specified custom protocol
     *
     * @param protocol the transfer protocol the client would work with
     * or the ending token of the property
     * <p>
     * e.g. 'my_protocol_name' from actionlibrary.filetransfer.client.my_protocol_name in ats-adapters.properties file
     * @param port     the port to use
     */
    @PublicAtsApi
    public FileTransferClient( String protocol, int port ) {

        try {
            TransferProtocol transferProtocol = checkTransferProtocolType(protocol);
            if (transferProtocol != null) {
                // a regular client we develop
                this.client = ClientFactory.getInstance().getClient(transferProtocol, port);
            } else {
                // a product specific client
                String customFileTransferClient = FileTransferConfigurator.getInstance()
                                                                          .getFileTransferClient(protocol);

                this.client = ClientFactory.getInstance().getClient(customFileTransferClient, port);
            }

            this.client.setDebugMode(ActionLibraryConfigurator.getInstance().getFileTransferVerboseMode());

        } catch (Exception e) {
            throw new FileTransferException(e);
        }
    }

    private TransferProtocol checkTransferProtocolType( String transferProtocol ) {

        switch (transferProtocol.toUpperCase()) {
            case "FTP":
                return TransferProtocol.FTP;
            case "HTTP":
                return TransferProtocol.HTTP;
            case "SFTP":
                return TransferProtocol.SFTP;
            case "FTPS":
                return TransferProtocol.FTPS;
            case "HTTPS":
                return TransferProtocol.HTTPS;
            default:
                return null;
        }
    }

    // -------------------- SETTINGS ------------------

    /**
     * Sets a new port for the {@link FileTransferClient} to use<br>
     * <br>
     * If no port is set via this method then the default one for the currently
     * set protocol is used
     *
     * @param port
     *            the port to use
     */
    @PublicAtsApi
    public void setPort( @Validate( name = "port", type = ValidationType.NUMBER_PORT_NUMBER) int port ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ port });

        this.client.setCustomPort(port);
    }

    /**
     * Set the client key store which will be used for authentication
     * @param keystoreFile the key store file path ( Must be in JKS or PKCS12 format ) 
     * @param keystorePassword the key store password
     * @param alias the private key alias ( Currently this parameter is used only for SFTP operations, for other cases you may pass null as well )
     * **/
    @PublicAtsApi
    public void setKeystore( String keystoreFile, String keystorePassword, String alias ) {

        this.client.setKeystore(keystoreFile, keystorePassword, alias);
    }

    /**
     * Set the client trust store which will be used for validating trust server certificates
     * @param truststoreFile the trust store file path ( Must be in JKS or PKCS12 format ) 
     * @param truststorePassword the trust store password
     * 
     * <p><b>Note that call to this method will override any effect from both this method and setTrustedServerSSLCertificate</b></p>
     * **/
    @PublicAtsApi
    public void setTruststore( String truststoreFile, String truststorePassword ) {

        this.client.setTrustStore(truststoreFile, truststorePassword);
    }

    /**
     * Set the trust store certificate which will be used for connection/session validation
     * @param certificateFile the trust server certificate file path ( Must be in .PEM format )
     * 
     * <p><b>Note that call to this method will override any effect from both this method and setTruststore</b></p>
     * **/
    @PublicAtsApi
    public void setTrustedServerSSLCertificate( String certificateFile ) {

        this.client.setTrustedServerSSLCertificate(certificateFile);
    }

    // -------------------- ACTIONS --------------------

    /**
     * Set the current {@link TransferMode} to be used by the
     * {@link FileTransferClient}
     *
     * @param mode
     *            the {@link TransferMode} to use
     */
    @PublicAtsApi
    public void setTransferMode( TransferMode mode ) {

        this.client.setTransferMode(mode);
    }

    /**
     * Add custom client/protocol specific parameters. This should be set before connect.
     * Specific keys and values are defined as constants in this class.
     * @param key {@link #FTPS_CONNNECTION_TYPE}, {@link #HTTP_HTTPS_SOCKET_READ_TIMEOUT} or similar constants
     * @param value the value for the corresponding key. It could be either constant if property is either:
     *  <ul>
     *      <li>enumerable value from constants from this class like {@link #HTTP_HTTPS_UPLOAD_METHOD__POST}</li>
     *      <li>or string (for keys like {@link #HTTP_HTTPS_UPLOAD_CONTENT_TYPE})</li>
     *      <li>or number (for keys like {@link #HTTP_HTTPS_SOCKET_BUFFER_SIZE}, {@link #HTTP_HTTPS_SOCKET_READ_TIMEOUT})</li>
     *  </ul>
     * @throws IllegalArgumentException if property passed is not supported
     */
    @PublicAtsApi
    public void addCustomProperty( String key, Object value ) throws IllegalArgumentException {

        this.client.addCustomProperty(key, value);
    }

    /**
     * Uploads a the file to the specified directory and with the specified file
     * name
     *
     * @param localFile
     *            the local file to upload
     * @param remoteDir
     *            the remote directory to upload the file to
     * @param remoteFile
     *            the remote file name that the file should have
     */
    @PublicAtsApi
    public void uploadFile( @Validate( name = "localFile", type = ValidationType.STRING_NOT_EMPTY) String localFile,
                            @Validate( name = "remoteDir", type = ValidationType.STRING_NOT_EMPTY) String remoteDir,
                            @Validate( name = "remoteFile", type = ValidationType.STRING_NOT_EMPTY) String remoteFile ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ localFile, remoteDir, remoteFile });

        // upload the file itself
        this.client.uploadFile(localFile, remoteDir, remoteFile);
    }

    /**
     * Upload a the file to the specified directory and with the specified file
     * name
     *
     * @param localFile
     *            the local file to upload
     * @param remoteDir
     *            the remote directory to upload the file to
     */
    @PublicAtsApi
    public void uploadFile( String localFile, String remoteDir ) {

        File local = new File(localFile);
        uploadFile(localFile, remoteDir, local.getName());
    }

    /**
     * Download a file from the specified directory and with the specified file
     * name
     *
     * @param localDir
     *            the local directory to download the file to
     * @param localFile
     *            the local file that will be created
     * @param remoteDir
     *            the remote directory to download from
     * @param remoteFile
     *            the remote file to download
     */
    @PublicAtsApi
    public void downloadFile( @Validate( name = "localFile", type = ValidationType.STRING_NOT_EMPTY) String localFile,
                              @Validate( name = "localDir", type = ValidationType.STRING_NOT_EMPTY) String localDir,
                              @Validate( name = "remoteDir", type = ValidationType.STRING_NOT_EMPTY) String remoteDir,
                              @Validate( name = "remoteFile", type = ValidationType.STRING_NOT_EMPTY) String remoteFile ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ localFile, localDir, remoteDir,
                                                               remoteFile });

        // upload the file itself
        this.client.downloadFile(IoUtils.normalizeDirPath(localDir) + localFile, remoteDir, remoteFile);
    }

    /**
     * Downloads a file from the specified directory and with the specified file
     * name, the download will preserve the name of the file
     *
     * @param localDir
     *            the local directory to download the file to
     * @param remoteDir
     *            the remote directory to download from
     * @param remoteFile
     *            the remote file to download
     */
    @PublicAtsApi
    public void downloadFile( @Validate( name = "localDir", type = ValidationType.STRING_NOT_EMPTY) String localDir,
                              @Validate( name = "remoteDir", type = ValidationType.STRING_NOT_EMPTY) String remoteDir,
                              @Validate( name = "remoteFile", type = ValidationType.STRING_NOT_EMPTY) String remoteFile ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ localDir, remoteDir, remoteFile });

        // upload the file itself
        this.client.downloadFile(IoUtils.normalizeDirPath(localDir) + remoteFile, remoteDir, remoteFile);
    }

    /**
     * Execute some custom command.
     * This is specific for each protocol and remote server.
     *
     * @param command the command to execute
     * @return the command output
     */
    @PublicAtsApi
    public String
            executeCommand( @Validate( name = "command", type = ValidationType.STRING_NOT_EMPTY) String command ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ command });

        // execute the command
        return this.client.executeCommand(command);
    }

    /**
     * Starts an upload in a different thread and then pauses it
     * and waits to be resumed by {@link resumePausedTransfer}.
     *
     * <p><b>Only one upload should be started and paused. Other transfers with the same object
     * should be done only after this one is resumed.</b></p>
     *
     * @param localFile
     *            the local file to upload
     * @param remoteDir
     *            the remote directory to upload the file to
     * @param remoteFile
     *            the remote file name that the file should have
     */
    @PublicAtsApi
    public void
            startUploadAndPause( @Validate( name = "localFile", type = ValidationType.STRING_NOT_EMPTY) String localFile,
                                 @Validate( name = "remoteDir", type = ValidationType.STRING_NOT_EMPTY) String remoteDir,
                                 @Validate( name = "remoteFile", type = ValidationType.STRING_NOT_EMPTY) String remoteFile ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ localFile, remoteDir, remoteFile });

        this.client.startUploadAndPause(localFile, remoteDir, remoteFile);
    }

    /**
     * Starts an upload in a different thread and then pauses it
     * and waits to be resumed by {@link resumePausedTransfer}.
     *
     * <p><b>Only one upload should be started and paused. Other transfers with the same object
     * should be done only after this one is resumed.</b></p>
     *
     * @param localFile
     *            the local file to upload
     * @param remoteDir
     *            the remote directory to upload the file to
     */
    @PublicAtsApi
    public void startUploadAndPause( String localFile, String remoteDir ) {

        File local = new File(localFile);
        startUploadAndPause(localFile, remoteDir, local.getName());
    }

    /**
     * Resumes a started and paused transfer.
     */
    @PublicAtsApi
    public void resumePausedTransfer() {

        this.client.resumePausedTransfer();
    }

    @PublicAtsApi
    public void connect( @Validate( name = "hostname", type = ValidationType.STRING_SERVER_ADDRESS) String hostname ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ hostname });

        doConnect(hostname, null, null);
    }

    /**
     * Connect to a remote host using basic authentication
     *
     * @param hostname
     *            the host to connect to
     * @param userName
     *            the user name
     * @param password
     *            the password for the provided user name
     */
    @PublicAtsApi
    public void connect( @Validate( name = "hostname", type = ValidationType.STRING_SERVER_ADDRESS) String hostname,
                         @Validate( name = "userName", type = ValidationType.STRING_NOT_EMPTY) String userName,
                         @Validate( name = "password", type = ValidationType.STRING_NOT_EMPTY) String password ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ hostname, userName, password });

        doConnect(hostname, userName, password);
    }

    private void doConnect( String hostname, String userName, String password ) {

        // connect using base authentication
        Throwable throwable = null;
        try {
            this.client.connect(hostname, userName, password);
            return;
        } catch (FileTransferException e) {
            throwable = e;
            log.error("Connection attempt failed", e);
        }

        throw new FileTransferException("Could not connect. Look up the reason in the log.", (Exception) throwable);
    }

    /**
     * Connect to a remote host using secure authentication
     *
     * @param hostname
     *            the host to connect to
     * @param keystoreFile
     *            the file containing the key store
     * @param keystorePassword
     *            the key store password
     * @param privateKeyAlias
     *            the private key alias
     */
    @PublicAtsApi
    public void connect( @Validate( name = "hostname", type = ValidationType.STRING_SERVER_ADDRESS) String hostname,
                         @Validate( name = "keystoreFile", type = ValidationType.STRING_NOT_EMPTY) String keystoreFile,
                         @Validate( name = "keystorePassword", type = ValidationType.STRING_NOT_EMPTY) String keystorePassword,
                         @Validate( name = "privateKeyAlias", type = ValidationType.STRING_NOT_EMPTY) String privateKeyAlias ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ hostname, keystoreFile, keystorePassword,
                                                               privateKeyAlias });

        // connect using base authentication
        Throwable throwable = null;
        try {
            this.client.connect(hostname, keystoreFile, keystorePassword, privateKeyAlias);
            return;
        } catch (FileTransferException e) {
            throwable = e;
            log.error("Connection attempt failed", e);
        }

        throw new FileTransferException("Could not connect. Look up the reason in the log.", (Exception) throwable);
    }

    /**
     * Disconnect from the remote host
     */
    @PublicAtsApi
    public void disconnect() {

        // connect using base authentication
        this.client.disconnect();
    }

    /**
     * Enable or disable gathering of the responses for the protocol commands.
     * Not implemented for all protocols. Available for FTP and FTPS
     * @param enable <b>true</b> to enable, <b>false</b> to disable response gathering.
     */
    @PublicAtsApi
    public void enableResponseCollection( boolean enable ) {

        this.client.enableResponseCollection(enable);
    }

    /**
     * Gets the gathered responses for the protocol commands. Available for FTP and FTPS.
     * @return array of all the responses since gatherResponses(true) was invoked or <b>null</b> otherwise.
     */
    @PublicAtsApi
    public String[] getResponses() {

        return this.client.getResponses();
    }

    /**
     * <b>Note:</b> This method is unofficial. It returns object from
     * the core of ATS framework which is not supposed to be used directly
     * by customers.
     * It might be changed or removed at any moment without notice.
     *
     * @return the internal object handling the actual operations
     */
    public IFileTransferClient getInternalObject() {

        return this.client;
    }
}
