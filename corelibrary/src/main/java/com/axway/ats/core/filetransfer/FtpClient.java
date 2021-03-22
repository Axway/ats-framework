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
package com.axway.ats.core.filetransfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ProtocolCommandListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.filetransfer.FileTransferException;
import com.axway.ats.common.filetransfer.TransferMode;
import com.axway.ats.core.CoreLibraryConfigurator;
import com.axway.ats.core.filetransfer.model.TransferListener;
import com.axway.ats.core.filetransfer.model.ftp.FtpListener;
import com.axway.ats.core.filetransfer.model.ftp.FtpResponseListener;
import com.axway.ats.core.filetransfer.model.ftp.SynchronizationFtpTransferListener;
import com.axway.ats.core.utils.StringUtils;
/**
 * The {@link FtpClient} uses the Apache Commons Net component suite for Java
 * ( https://commons.apache.org/proper/commons-net/ ) to initiate and execute FTP
 * connections to a remote server.
 */
public class FtpClient extends AbstractFileTransferClient {

    private org.apache.commons.net.ftp.FTPClient ftpConnection = null;
    private static final Logger                  log           = LogManager.getLogger(FtpClient.class);

    /**
     * Constructor
     *
     */
    public FtpClient() {

        super();
    }

    /**
     * Set the {@link TransferMode} that this protocol should use
     *
     * @param mode
     * @throws FileTransferException
     */
    @Override
    public void setTransferMode(
                                 TransferMode mode ) throws FileTransferException {

        if (this.ftpConnection != null && this.ftpConnection.isConnected() && this.transferMode != mode) {
            try {
                log.info("Set file transfer mode to " + mode);
                if (mode == TransferMode.ASCII) {
                    if (!this.ftpConnection.setFileType(org.apache.commons.net.ftp.FTPClient.ASCII_FILE_TYPE)) {
                        throw new Exception("Unable to set transfer mode to ASCII");
                    }
                } else {
                    if (!this.ftpConnection.setFileType(org.apache.commons.net.ftp.FTPClient.BINARY_FILE_TYPE)) {
                        throw new Exception("Unable to set transfer mode to BINARY");
                    }
                }
            } catch (Exception e) {
                throw new FileTransferException("Error setting file transfer mode to " + mode, e);
            }
        }

        super.setTransferMode(mode);
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
     * @throws FileTransferException
     */
    @Override
    public void connect(
                         String hostname,
                         String userName,
                         String password ) throws FileTransferException {

        log.info("Connecting to " + hostname + " on port " + this.port + " using username " + userName
                 + " and password " + password);
        // make new FTP object for every new connection
        disconnect();
        this.ftpConnection = new org.apache.commons.net.ftp.FTPClient();

        /* 
         * When uploading/downloading file with encoding that the server does not, the server will return 452.
         * So we have to either set the control encoding to UTF-8 (or the one that is desired), or if the needed encoding is UTF-8,
         * set the UTF-8 autodetect to true
        */
        String controlEncoding = CoreLibraryConfigurator.getInstance().getFtpControlEncoding();
        boolean autodetectUTF8 = Boolean.valueOf(CoreLibraryConfigurator.getInstance().getFtpAutodetectUTF8());

        if (!StringUtils.isNullOrEmpty(controlEncoding)) {
            this.ftpConnection.setControlEncoding(controlEncoding);
            if (log.isDebugEnabled()) {
                log.debug("Control encoding is set to " + controlEncoding);
            }

        }

        this.ftpConnection.setAutodetectUTF8(autodetectUTF8);
        if (log.isDebugEnabled()) {
            log.debug("Autodetect for UTF-8 is " + ( (autodetectUTF8)
                                                                      ? "enabled"
                                                                      : "disabled"));
        }

        if (!"UTF-8".equalsIgnoreCase(controlEncoding) && autodetectUTF8) {
            log.warn("Autodetecting UTF-8 is enabled, but additionaly, the control encoding is set to '"
                     + controlEncoding + "'. UTF-8 will be used.");
        }

        if (this.listener != null) {
            this.listener.setResponses(new ArrayList<String>());
            this.ftpConnection.addProtocolCommandListener( ((FtpResponseListener) listener));
        }
        /* if debug mode is true, we log messages from all levels */
        if (isDebugMode()) {
            this.ftpConnection.addProtocolCommandListener(new FtpListener());
        }
        try {
            this.ftpConnection.setConnectTimeout(this.timeout);
            // connect to the host
            this.ftpConnection.connect(hostname, this.port);
            // login to the host
            if (!this.ftpConnection.login(userName, password)) {
                throw new Exception("Invallid username and/or password");
            }
            // enter passive mode
            this.ftpConnection.enterLocalPassiveMode();
            // set transfer mode
            if (this.transferMode == TransferMode.ASCII) {
                if (!this.ftpConnection.setFileType(org.apache.commons.net.ftp.FTPClient.ASCII_FILE_TYPE)) {
                    throw new Exception("Unable to set transfer mode to ASCII");
                }
            } else {
                if (!this.ftpConnection.setFileType(org.apache.commons.net.ftp.FTPClient.BINARY_FILE_TYPE)) {
                    throw new Exception("Unable to set transfer mode to BINARY");
                }
            }

        } catch (Exception e) {
            String errMessage = "Unable to connect to  " + hostname + " on port " + this.port
                                + " using username " + userName + " and password " + password;
            log.error(errMessage, e);
            throw new FileTransferException(e);
        }

        log.info("Successfully connected to " + hostname + " on port " + this.port + " using username "
                 + userName + " and password " + password);

    }

    @Override
    public void connect(
                         String hostname,
                         String keystoreFile,
                         String keystorePassword,
                         String publicKeyAlias ) throws FileTransferException {

        throw new FileTransferException("Not Supported");

    }

    /**
     * Disconnect from the remote host
     *
     * @throws FileTransferException
     */
    @Override
    public void disconnect() throws FileTransferException {

        if (this.ftpConnection != null && this.ftpConnection.isConnected()) {
            try {
                this.ftpConnection.disconnect();
                this.ftpConnection = null;
            } catch (IOException e) {
                throw new FileTransferException(e);
            }
        }

    }

    @Override
    protected void performDownloadFile(
                                        String localFile,
                                        String remoteDir,
                                        String remoteFile ) throws FileTransferException {

        FileOutputStream fos = null;
        try {
            String remoteFileAbsPath = null;
            remoteDir = remoteDir.replace("\\", "/");
            remoteFile = remoteFile.replace("\\", "/");

            if (remoteDir.endsWith("/") && remoteFile.endsWith("/")) {
                remoteFileAbsPath = remoteDir.substring(0, remoteDir.length() - 2) + remoteFile;
            } else if (!remoteDir.endsWith("/") && !remoteFile.endsWith("/")) {
                remoteFileAbsPath = remoteDir + "/" + remoteFile;
            } else {
                remoteFileAbsPath = remoteDir + remoteFile;
            }

            // download the file
            fos = new FileOutputStream(new File(localFile));
            if (!this.ftpConnection.retrieveFile(remoteFileAbsPath, fos)) {
                throw new FileTransferException("Unable to retrieve "
                                                + (remoteDir.endsWith("/")
                                                                           ? remoteDir
                                                                           : remoteDir + "/")
                                                + remoteFile + " from "
                                                + this.ftpConnection.getPassiveHost() + " as a " + localFile);
            }
        } catch (Exception e) {
            log.error("Unable to download file " + localFile, e);
            throw new FileTransferException(e);
        } finally {
            // close the file output stream
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    log.error("Unable to close the file stream after successful download!", e);
                }
            }
        }

        if (remoteDir != null && !remoteDir.endsWith("/")) {
            remoteDir += "/";
        }
        log.info("Successfully downloaded '" + localFile + "' from '" + remoteDir + remoteFile + "', host "
                 + ftpConnection.getPassiveHost());
    }

    @Override
    protected void performUploadFile(
                                      String localFile,
                                      String remoteDir,
                                      String remoteFile ) throws FileTransferException {

        FileInputStream fis = null;

        try {
            String remoteFileAbsPath = null;
            remoteDir = remoteDir.replace("\\", "/");
            remoteFile = remoteFile.replace("\\", "/");

            if (remoteDir.endsWith("/") && remoteFile.endsWith("/")) {
                remoteFileAbsPath = remoteDir.substring(0, remoteDir.length() - 2) + remoteFile;
            } else if (!remoteDir.endsWith("/") && !remoteFile.endsWith("/")) {
                remoteFileAbsPath = remoteDir + "/" + remoteFile;
            } else {
                remoteFileAbsPath = remoteDir + remoteFile;
            }

            // upload the file
            fis = new FileInputStream(new File(localFile));
            if (!this.ftpConnection.storeFile(remoteFileAbsPath, fis)) {
                throw new FileTransferException("Unable to store " + localFile + " to "
                                                + this.ftpConnection.getPassiveHost() + " as a "
                                                + (remoteDir.endsWith("/")
                                                                           ? remoteDir
                                                                           : remoteDir + "/")
                                                + remoteFile);
            }
        } catch (Exception e) {
            log.error("Unable to upload file!", e);
            throw new FileTransferException(e);
        } finally {
            // close the file input stream
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    log.error("Unable to close the file stream after successful upload!", e);
                }
            }
        }

        if (remoteDir != null && !remoteDir.endsWith("/")) {
            remoteDir += "/";
        }
        log.info("Successfully uploaded '" + localFile + "' to '" + remoteDir + remoteFile + "', host "
                 + ftpConnection.getPassiveHost());
    }

    /**
     * Currently not supporting commands requiring opening of data connection
     * @param command the command to run
     * @return String representing the return code
     * @throws FileTransferException
     */
    @Override
    public String executeCommand(
                                  String command ) throws FileTransferException {

        log.info("Run '" + command + "'");
        String returnCode = "";

        try {
            returnCode = String.valueOf(this.ftpConnection.sendCommand(command));
        } catch (IOException e) {
            log.error("Error running command: '" + command + "'", e);
            throw new FileTransferException(e);
        }

        log.info("Return code is '" + returnCode + "'");
        return returnCode;
    }

    @Override
    protected TransferListener addListener(
                                            int progressEventNumber ) {

        SynchronizationFtpTransferListener listener = new SynchronizationFtpTransferListener(this,
                                                                                             progressEventNumber);
        this.ftpConnection.addProtocolCommandListener(listener);

        return listener;
    }

    @Override
    protected void removeListener(
                                   TransferListener listener ) {

        this.ftpConnection.removeProtocolCommandListener((ProtocolCommandListener) listener);

    }

    @Override
    protected void finalize() throws Throwable {

        // ensure the connection is terminated
        this.disconnect();

        this.listener = null;

        super.finalize();
    }

    @Override
    public void enableResponseCollection(
                                          boolean enable ) {

        if (enable) {
            this.listener = new FtpResponseListener();
            // If it's connected add the listener to gather the responses
            if (this.ftpConnection != null) {
                this.ftpConnection.addProtocolCommandListener((FtpResponseListener) listener);
            }
        } else {
            // If it's connected remove the listener
            if (this.ftpConnection != null) {
                this.ftpConnection.removeProtocolCommandListener((FtpResponseListener) listener);
            }
            this.listener = null;
        }
    }

    @Override
    public String[] getResponses() {

        if (this.listener == null) {
            return new String[]{};
        }

        List<String> responses = this.listener.getResponses();

        return responses.toArray(new String[responses.size()]);
    }

    @Override
    public void setKeystore( String keystoreFile, String keystorePassword, String alias ) {

        throw new UnsupportedOperationException("FTP Connections over SSL are not supported.");

    }

    @Override
    public void setTrustStore( String truststoreFile, String truststorePassword ) {

        throw new UnsupportedOperationException("FTP Connections over SSL are not supported.");

    }

    @Override
    public void setTrustedServerSSLCertificate( String certificateFile ) {

        throw new UnsupportedOperationException("FTP Connections over SSL are not supported.");

    }

}
