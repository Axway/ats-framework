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
package com.axway.ats.action.ftp;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.filetransfer.FileTransferException;
import com.axway.ats.common.filetransfer.TransferMode;
import com.axway.ats.core.filetransfer.model.IFileTransferClient;
import com.axway.ats.core.filetransfer.model.TransferListener;
import com.axway.ats.core.filetransfer.model.ftp.FtpResponseListener;
import com.axway.ats.core.filetransfer.model.ftp.SynchronizationFtpTransferListener;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * The {@link FileTransferFtpClient} uses the Apache Commons Net component suite for Java
 * ( https://commons.apache.org/proper/commons-net/ ) to initiate and execute FTP
 * connections to a remote server.
 */
@PublicAtsApi
public class FileTransferFtpClient implements IFileTransferClient {
    private static final String UNSUPPORTED_OPERATION_MESSAGE="FTP Connections over SSL are not supported.";
    private FtpClient ftpClient = null;

    @PublicAtsApi
    @Override
    public void setTransferMode(TransferMode mode) throws FileTransferException {

        if (this.ftpClient != null && this.ftpClient.isConnected()) {
            this.ftpClient.setTransferMode(mode);
        }
    }

    @PublicAtsApi
    @Override
    public void uploadFile(String localFile, String remoteDir, String remoteFile) throws FileTransferException {
        this.ftpClient.storeFile(localFile, remoteDir, remoteFile);
    }

    @PublicAtsApi
    @Override
    public void downloadFile(String localFile, String remoteDir, String remoteFile) throws FileTransferException {
        this.ftpClient.retrieveFile(localFile, remoteDir, remoteFile);
    }

    @PublicAtsApi
    @Override
    public void connect(String hostname, String userName, String password) throws FtpException {
        disconnect();
        this.ftpClient = new FtpClient();
        this.ftpClient.connect(hostname, userName, password);
    }

    @PublicAtsApi
    @Override
    public void connect(String hostname, String keystoreFile, String keystorePassword, String publicKeyAlias)
            throws FtpException {
        disconnect();
        this.ftpClient = new FtpClient();
        this.ftpClient.connect(hostname, keystoreFile, keystorePassword, publicKeyAlias);
    }

    @PublicAtsApi
    @Override
    public void disconnect() throws FtpException {
        if (this.ftpClient != null && this.ftpClient.isConnected()) {
            this.ftpClient.disconnect();
            this.ftpClient = null;
        }
    }

    @PublicAtsApi
    @Override
    public void setCustomPort(int port) {
        this.ftpClient.setCustomPort(port);
    }

    @PublicAtsApi
    @Override
    public void setConnectionTimeout(int newValue) {
        this.ftpClient.setConnectionTimeout(newValue);
    }

    @Override
    public void setKeystore(String keystoreFile, String keystorePassword, String alias) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public void setTrustStore(String truststoreFile, String truststorePassword) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public void setTrustedServerSSLCertificate(String certificateFile) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public String executeCommand(String command) throws FileTransferException {
        return this.ftpClient.executeCommand(command);
    }

    @PublicAtsApi
    @Override
    public Object executeCommand(String command, Object[] arguments) throws FileTransferException {
        return this.ftpClient.executeCommand(command, arguments);
    }

    @PublicAtsApi
    @Override
    public String executeCommand(String command, InputStream payload) throws FileTransferException {
        return this.ftpClient.executeCommand(command, payload);
    }

    @PublicAtsApi
    @Override
    public void resumePausedTransfer() throws FileTransferException {
        this.ftpClient.resumePausedTransfer();
    }

    @PublicAtsApi
    @Override
    public void startUploadAndPause(String localFile, String remoteDir, String remoteFile)
            throws FileTransferException {

        this.ftpClient.checkPausedTransferRunning(false);
        this.ftpClient.setTransferStartedAndPaused(true); // a paused transfer is started.

        final Thread currentThread = Thread.currentThread(); // get the executor thread.

        final Thread uploadThread = new UploadThread("Upload Thread", this.ftpClient) {
            private final Logger log = Logger.getLogger(this.getName());
            FtpClient currentFtpClient = this.getFtpClient();

            @Override
            public void run() {
                synchronized (FileTransferFtpClient.this) {
                    // Notify the executor thread that the upload is starting.
                    // The executor thread will stop waiting for the upload to start
                    // but will not do anything until it receives the object's monitor.
                    currentFtpClient.setCanResume(true);
                    FileTransferFtpClient.this.notifyAll();

                    int progressEventNumber = 0; // the number of the progress event on which to wait
                    if (new File(localFile).length() == 0) {
                        // if the file is empty, no progress events will be fired
                        progressEventNumber = -1;
                    }

                    // Add a listener to notify the executor that the transfer is paused.
                    TransferListener listener = addListener(progressEventNumber);

                    try {
                        // Start the upload.
                        FileTransferFtpClient.this.uploadFile(localFile, remoteDir, remoteFile);

                        // Notify the executor thread that the upload has finished successfully.
                        FileTransferFtpClient.this.notifyAll();
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
            this.ftpClient.listener = new FtpResponseListener();
            // If it's connected, add the listener to gather the responses
            if (this.ftpClient != null) {
                this.ftpClient.addProtocolCommandListener((FtpResponseListener) this.ftpClient.listener);
            }
        } else {
            // If it's connected, remove the listener
            if (this.ftpClient != null) {
                this.ftpClient.removeProtocolListener((FtpResponseListener) this.ftpClient.listener);
                this.ftpClient.listener = null;
            }
        }

    }

    @PublicAtsApi
    @Override
    public String[] getResponses() {
        if (this.ftpClient.listener == null) {
            return new String[] {};
        }

        List<String> responses = this.ftpClient.listener.getResponses();

        return responses.toArray(new String[responses.size()]);
    }

    @Override
    public void addCustomProperty(String key, Object value) throws IllegalArgumentException {

        throw new IllegalArgumentException("Not supported");
    }

    @Override
    public void applyCustomProperties() throws IllegalArgumentException {
        // do nothing
    }

    @Override
    public boolean isDebugMode() {
        return this.ftpClient.isDebugMode();
    }

    @Override
    public void setDebugMode(boolean turnDebug) {
        this.ftpClient.setDebugMode(turnDebug);
    }

    public TransferListener addListener(int progressEventNumber) {

        SynchronizationFtpTransferListener listener = new SynchronizationFtpTransferListener(this, progressEventNumber);
        this.ftpClient.addProtocolCommandListener(listener);

        return listener;
    }

    public void removeListener(TransferListener listener) {
        this.ftpClient.removeProtocolListener((ProtocolCommandListener) listener);
    }

    static class UploadThread extends Thread {
        private final FtpClient ftpClient;

        public UploadThread(String threadName, FtpClient ftpClient) {
            super(threadName);
            this.ftpClient = ftpClient;
        }

        public FtpClient getFtpClient() {
            return ftpClient;
        }
    }
}
