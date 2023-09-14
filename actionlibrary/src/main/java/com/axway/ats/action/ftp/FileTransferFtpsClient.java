/*
 * Copyright 2017-2020 Axway Software
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
package com.axway.ats.action.ftp;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.filetransfer.FileTransferException;
import com.axway.ats.common.filetransfer.TransferMode;
import com.axway.ats.core.filetransfer.model.IFileTransferClient;
import com.axway.ats.core.filetransfer.model.TransferListener;
import com.axway.ats.core.filetransfer.model.ftp.FtpResponseListener;
import com.axway.ats.core.filetransfer.model.ftp.SynchronizationFtpTransferListener;
import com.axway.ats.core.utils.StringUtils;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The {@link FileTransferFtpsClient} uses the Apache Commons Net component suite for Java
 * ( https://commons.apache.org/proper/commons-net/ ) to initiate and execute FTPS
 * connections to a remote server. <br>
 * <br>
 * The default implementation does *not* verify the server certificate against a
 * local trusted CA store!
 */
@PublicAtsApi
public class FileTransferFtpsClient implements IFileTransferClient {
    private static final Logger log = Logger.getLogger(FileTransferFtpsClient.class);

    private FtpsClient ftpsClient = null;

    @PublicAtsApi
    @Override
    public void setTransferMode(TransferMode mode) throws FileTransferException {
        if (this.ftpsClient != null && this.ftpsClient.isConnected()) {
            this.ftpsClient.setTransferMode(mode);
        }
    }

    @PublicAtsApi
    @Override
    public void uploadFile(String localFile, String remoteDir, String remoteFile) throws FileTransferException {
        this.ftpsClient.storeFile(localFile, remoteDir, remoteFile);
    }

    @PublicAtsApi
    @Override
    public void downloadFile(String localFile, String remoteDir, String remoteFile) throws FileTransferException {
        this.ftpsClient.retrieveFile(localFile, remoteDir, remoteFile);
    }

    @PublicAtsApi
    @Override
    public void connect(String hostname, String userName, String password) throws FtpException {
        disconnect();
        this.ftpsClient = new FtpsClient();
        this.ftpsClient.connect(hostname, userName, password);
    }

    @PublicAtsApi
    @Override
    public void connect(String hostname, String keystoreFile, String keystorePassword, String publicKeyAlias)
            throws FtpException {
        disconnect();
        this.ftpsClient = new FtpsClient();
        this.ftpsClient.connect(hostname, keystoreFile, keystorePassword, publicKeyAlias);
    }

    @PublicAtsApi
    @Override
    public void disconnect() throws FileTransferException {
        if (this.ftpsClient != null && this.ftpsClient.isConnected()) {
            this.ftpsClient.disconnect();
            this.ftpsClient = null;
        }
    }

    @PublicAtsApi
    @Override
    public void setCustomPort(int port) {
        this.ftpsClient.setCustomPort(port);
    }

    @PublicAtsApi
    @Override
    public void setConnectionTimeout(int newValue) {
        this.ftpsClient.setConnectionTimeout(newValue);
    }

    /**
     * Set a client key store which will be used for authentication
     * @param keystoreFile the key store file path (Must be in JKS or PKCS12 format)
     * @param keystorePassword the key store password
     * **/
    @PublicAtsApi
    @Override
    public void setKeystore(String keystoreFile, String keystorePassword, String alias) {
        if (this.ftpsClient != null) {
            this.ftpsClient.setKeyStoreFile(keystoreFile);
            this.ftpsClient.setKeyStorePassword(keystorePassword);
        }
    }

    /**
     * Set a client trust store which will be used for validating trust server certificates
     * @param truststoreFile the trust store file path (Must be in JKS or PKCS12 format)
     * @param truststorePassword the trust store password
     *
     * <p><b>Note that call to this method will override any effect from both this method and setTrustedServerSSLCertificate</b></p>
     * **/
    @PublicAtsApi
    @Override
    public void setTrustStore(String truststoreFile, String truststorePassword) {
        if (this.ftpsClient != null) {
            this.ftpsClient.setTrustStoreFile(truststoreFile);
            this.ftpsClient.setTrustStorePassword(truststorePassword);

            // invalidate any previously set trust server certificate
            if (!StringUtils.isNullOrEmpty(this.ftpsClient.getTrustedServerSSLCertificateFile())) {
                log.warn(
                        "Previously set trust server certificate '" + this.ftpsClient.getTrustedServerSSLCertificateFile() + "' will be overridden and only certificates from truststore '" + truststoreFile + "' will be used for validation");
                this.ftpsClient.setTrustedServerSSLCertificateFile(null);
            }
        }
    }

    /**
     * Set a client certificate which will be used for authentication
     * @param certificateFile the trust server certificate file path (must be a .PEM file)
     *
     * <p><b>Note that call to this method will override any effect from both this method and setTrustStore</b></p>
     * **/
    @PublicAtsApi
    @Override
    public void setTrustedServerSSLCertificate(String certificateFile) {
        if (this.ftpsClient != null) {
            this.ftpsClient.setTrustedServerSSLCertificateFile(certificateFile);

            // invalidate any previously set trust store
            if (!StringUtils.isNullOrEmpty(this.ftpsClient.getTrustStoreFile())) {
                log.warn(
                        "Previously set trust store '" + this.ftpsClient.getTrustStoreFile() + "' will be overridden and only the certificate '" + this.ftpsClient.getTrustedServerSSLCertificateFile() + "' will be used for validation");
                this.ftpsClient.setTrustStoreFile(null);
                this.ftpsClient.setTrustStorePassword(null);
            }
        }
    }

    @PublicAtsApi
    @Override
    public String executeCommand(String command) throws FileTransferException {
        return this.executeCommand(command, (InputStream) null);
    }

    /**
     * Currently not supporting commands requiring opening of data connection
     * @param command the command to run
     * @return String representing the return code
     * @throws FileTransferException
     */
    @Override
    public Object executeCommand(String command, Object[] arguments) throws FileTransferException {
        return this.ftpsClient.executeCommand(command, arguments);
    }

    @PublicAtsApi
    @Override
    public String executeCommand(String command, InputStream payload) throws FileTransferException {
        return this.ftpsClient.executeCommand(command, payload);
    }

    @PublicAtsApi
    @Override
    public void resumePausedTransfer() throws FileTransferException {
        this.ftpsClient.resumePausedTransfer();
    }

    @PublicAtsApi
    @Override
    public void startUploadAndPause(String localFile, String remoteDir, String remoteFile)
            throws FileTransferException {

        this.ftpsClient.checkPausedTransferRunning(false);
        this.ftpsClient.setTransferStartedAndPaused(true); // a paused transfer is started.

        final Thread currentThread = Thread.currentThread(); // get the executor thread.

        final Thread uploadThread = new FileTransferFtpsClient.UploadThread("Upload Thread", this.ftpsClient) {
            private final Logger log = Logger.getLogger(this.getName());
            FtpsClient currentFtpsClient = this.getFtpsClient();

            @Override
            public void run() {
                synchronized (FileTransferFtpsClient.this) {
                    // Notify the executor thread that the upload is starting.
                    // The executor thread will stop waiting for the upload to start
                    // but will not do anything until it receives the object's monitor.
                    currentFtpsClient.setCanResume(true);
                    FileTransferFtpsClient.this.notifyAll();

                    int progressEventNumber = 0; // the number of the progress event on which to wait
                    if (new File(localFile).length() == 0) {
                        // if the file is empty, no progress events will be fired
                        progressEventNumber = -1;
                    }

                    // Add a listener to notify the executor that the transfer is paused.
                    TransferListener listener = addListener(progressEventNumber);

                    try {
                        // Start the upload.
                        FileTransferFtpsClient.this.uploadFile(localFile, remoteDir, remoteFile);

                        // Notify the executor thread that the upload has finished successfully.
                        FileTransferFtpsClient.this.notifyAll();
                    } catch (FileTransferException e) {
                        log.error("Upload failed.", e);

                        // Interrupt the executor thread so that an exception can be thrown
                        // while waiting for the upload to finish.
                        currentThread.interrupt();
                    } finally {
                        // Ensure the listener is removed to prevent deadlocks in further transfers.
                        removeListener(listener);
                    }
                }
            }
        };

        uploadThread.start();

    }

    @PublicAtsApi
    @Override
    public void enableResponseCollection(boolean enable) {

        if (enable) {
            this.ftpsClient.listener = new FtpResponseListener();
            // If it's connected, add the listener to gather the responses
            if (this.ftpsClient != null) {
                this.ftpsClient.addProtocolCommandListener((FtpResponseListener) this.ftpsClient.listener);
            }
        } else {
            // If it's connected, remove the listener
            if (this.ftpsClient != null) {
                this.ftpsClient.removeProtocolListener((FtpResponseListener) this.ftpsClient.listener);
                this.ftpsClient.listener = null;
            }
        }
    }

    @PublicAtsApi
    @Override
    public String[] getResponses() {
        if (this.ftpsClient.listener == null) {
            return new String[] {};
        }

        List<String> responses = this.ftpsClient.listener.getResponses();

        return responses.toArray(new String[responses.size()]);
    }

    @PublicAtsApi
    @Override
    public void addCustomProperty(String key, Object value) throws IllegalArgumentException {
        if (key.equals(FtpsClient.FTPS_CONNECTION_TYPE)) {
            if (!(value instanceof Integer)) {
                throw new IllegalArgumentException(
                        "Value '" + value + "' for property key '" + key + "' has not supported type. " + FtpsClient.USE_ONE_OF_THE_FTPS_CONSTANTS);
            } else {
                this.ftpsClient.customProperties.put(key, value);
            }
        } else if (key.equals(FtpsClient.FTPS_ENCRYPTION_PROTOCOLS)) {
            this.ftpsClient.customProperties.put(key, value);
        } else {
            throw new IllegalArgumentException(
                    "Unknown property with key '" + key + "' is passed. " + FtpsClient.USE_ONE_OF_THE_FTPS_CONSTANTS);
        }
    }

    @Override
    public void applyCustomProperties() throws IllegalArgumentException {

        Set<Map.Entry<String, Object>> customPropertiesSet = this.ftpsClient.customProperties.entrySet();
        Object value;
        boolean clientTypeIsSet = false;
        for (Map.Entry<String, Object> customPropertyEntry : customPropertiesSet) {
            value = customPropertyEntry.getValue();
            if (customPropertyEntry.getKey().equals(FtpsClient.FTPS_CONNECTION_TYPE)) {
                clientTypeIsSet = true;
                if (value.equals(FtpsClient.FTPS_CONNECTION_TYPE__IMPLICIT)) {
                    log.debug("Setting FTPS connection type to IMPLICIT_SSL");
                    this.ftpsClient.setImplicit(true);
                    this.ftpsClient.setProtocol("SSL");
                } else if (value.equals(FtpsClient.FTPS_CONNECTION_TYPE__AUTH_SSL)) {
                    log.debug("Setting FTPS connection type to AUTH_SSL");
                    this.ftpsClient.setImplicit(false);
                    this.ftpsClient.setProtocol("SSL");
                } else if (value.equals(FtpsClient.FTPS_CONNECTION_TYPE__AUTH_TLS)) {
                    log.debug("Setting FTPS connection type to AUTH_TLS");
                    this.ftpsClient.setImplicit(false);
                    this.ftpsClient.setProtocol("TLSv1.2");
                } else {
                    throw new IllegalArgumentException(
                            "Unknown value '" + value + "' for FTPS connection type. " + "Check value used in addCustomProperty() method. Use one of the GenericFileTransferClient.FTPS_CONNECTION_TYPE__* constants for value");
                }
            } else if (customPropertyEntry.getKey().equals(FtpsClient.FTPS_ENCRYPTION_PROTOCOLS)) {
                // currently we can set just one protocol
                String[] encryptionProtocols = this.ftpsClient.parseCustomProperties(value.toString());
                this.ftpsClient.setProtocol(encryptionProtocols[0]);
            } else {
                throw new IllegalArgumentException(
                        "Unknown property with key '" + customPropertyEntry.getKey() + "' is passed. " + FtpsClient.USE_ONE_OF_THE_FTPS_CONSTANTS);
            }
        }
        if (!clientTypeIsSet) { // explicitly set the default connection type
            log.debug("Using by default the FTPS connection type AUTH_TLS");
            this.ftpsClient.setImplicit(false);
            this.ftpsClient.setProtocol("TLSv1.2");
        }
    }

    @PublicAtsApi
    @Override
    public boolean isDebugMode() {
        return this.ftpsClient.isDebugMode();
    }

    @PublicAtsApi
    @Override
    public void setDebugMode(boolean turnDebug) {
        this.ftpsClient.setDebugMode(turnDebug);
    }

    public TransferListener addListener(int progressEventNumber) {

        SynchronizationFtpTransferListener listener = new SynchronizationFtpTransferListener(this, progressEventNumber);
        this.ftpsClient.addProtocolCommandListener(listener);

        return listener;
    }

    public void removeListener(TransferListener listener) {
        this.ftpsClient.removeProtocolListener((ProtocolCommandListener) listener);
    }

    static class UploadThread extends Thread {
        private final FtpsClient ftpsClient;

        public UploadThread(String threadName, FtpsClient ftpsClient) {
            super(threadName);
            this.ftpsClient = ftpsClient;
        }

        public FtpsClient getFtpsClient() {
            return ftpsClient;
        }
    }
}
