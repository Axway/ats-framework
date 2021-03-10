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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.filetransfer.FileTransferException;
import com.axway.ats.common.filetransfer.TransferMode;
import com.axway.ats.core.filetransfer.model.AbstractResponseListener;
import com.axway.ats.core.filetransfer.model.IFileTransferClient;
import com.axway.ats.core.filetransfer.model.TransferListener;

/**
 * The {@link AbstractFileTransferClient} implements logic that is basic for all {@link IFileTransferClient}s
 */
public abstract class AbstractFileTransferClient implements IFileTransferClient {

    private static final int           DEFAULT_CONNECTION_TIMEOUT = 30 * 1000;                    // thirty seconds

    /**
     * Initial connection timeout in milliseconds. If the server does not accept connection within the specified time then an exception is raised.
     */
    protected int                      timeout                    = DEFAULT_CONNECTION_TIMEOUT;

    protected int                      port;

    protected TransferMode             transferMode               = TransferMode.BINARY;

    /**
     * Flag indicating if the resumePausedTransferMethod can resume a transfer
     * or should wait until it is permitted.
     */
    protected boolean                  canResume                  = false;

    /**
     * Flag indicating if transfer has been started and paused
     */
    protected boolean                  isTransferStartedAndPaused = false;

    /**
     * Listener for gathering command responses
     */
    protected AbstractResponseListener listener                   = null;

    /**
     * Flag to track for debug mode. Clients may use this and issue verbose info.
     */
    protected boolean                  debugMode                  = false;

    /**
     * Container for accumulating custom properties and then for apply in
     * initialization just before connect.
     * @see #addCustomProperty(String, Object)
     */
    protected Map<String, Object>      customProperties           = new HashMap<String, Object>();

    /**
     * Basic constructor. Used when loading the client via java reflection
     */
    public AbstractFileTransferClient() {

    }

    /**
     * Set the {@link TransferMode} that this protocol should use
     *
     * @param mode
     * @throws FileTransferException
     */
    public void setTransferMode(
                                 TransferMode mode ) throws FileTransferException {

        this.transferMode = mode;
    }

    /**
     * Sets a custom port that would be used when connecting to the remote host
     *
     * @param port the port number
     */
    public void setCustomPort(
                               int portNumber ) {

        this.port = portNumber;
    }

    /**
     * Overrides the default connection timeout with a new custom value
     *
     * @param newValue the new value of the default connection timeout in milliseconds
     */
    public void setConnectionTimeout(
                                      int newValue ) {

        this.timeout = newValue;
    }

    /**
     * Uploads a the file to the specified directory and with the specified file name
     *
     * @param localFile the local file to upload
     * @param remoteDir the remote directory to upload the file to
     * @param remoteFile the remote file name that the file should have
     * @throws FileTransferException
     */
    public void uploadFile(
                            final String localFile,
                            final String remoteDir,
                            final String remoteFile ) throws FileTransferException {

        checkPausedTransferRunning(false);

        performUploadFile(localFile, remoteDir, remoteFile);
    }

    /**
     * Implement this method for performing actual uploads.
     * This method is called in uploadFile to perform the upload.
     * @param localFile
     * @param remoteDir
     * @param remoteFile
     * @throws FileTransferException
     */
    protected abstract void performUploadFile(
                                               final String localFile,
                                               final String remoteDir,
                                               final String remoteFile ) throws FileTransferException;

    /**
     * Downloads a file from the specified directory and with the specified file name
     *
     * @param localFile the local file that will be created
     * @param remoteDir the remote directory to download from
     * @param remoteFile the remote file to download
     * @throws FileTransferException
     */
    public void downloadFile(
                              String localFile,
                              String remoteDir,
                              String remoteFile ) throws FileTransferException {

        checkPausedTransferRunning(false);

        performDownloadFile(localFile, remoteDir, remoteFile);
    }

    /**
     * Implement this method for performing actual downloads.
     * This method is called in downloadFile to perform the download.
     * @param localFile name of the path to store downloaded file. Could contain directory path
     * @param remoteDir the remote path for the file/resource to be downloaded. Should end with "/"
     * @param remoteFile the file in the remoteDir path. Should not start with path separator
     * @throws FileTransferException
     */
    protected abstract void performDownloadFile(
                                                 String localFile,
                                                 String remoteDir,
                                                 String remoteFile ) throws FileTransferException;

    public synchronized void resumePausedTransfer() throws FileTransferException {

        checkPausedTransferRunning(true);

        final Logger log = LogManager.getLogger(AbstractFileTransferClient.class);

        while (!canResume) {
            try {
                log.debug("Waiting for the transfer to start...");
                // Wait to be notified when the transfer is started and will be paused.
                this.wait();
            } catch (InterruptedException e) {
                throw new FileTransferException("Interrupted while waiting for a transfer to start", e);
            }
        }

        canResume = false; // for the next resume

        // Notify the thread that is performing the transfer to continue.
        this.notifyAll();

        try {
            log.debug("Waiting for the transfer to finish...");
            // Wait to be notified that the transfer is done.
            this.wait();
        } catch (InterruptedException e) {
            throw new FileTransferException("Interrupted while waiting for a transfer to finish", e);
        } finally {
            this.isTransferStartedAndPaused = false; // the paused transfer has finished
        }
    }

    public void startUploadAndPause(
                                     final String localFile,
                                     final String remoteDir,
                                     final String remoteFile ) throws FileTransferException {

        checkPausedTransferRunning(false);

        this.isTransferStartedAndPaused = true; // a paused transfer is started.
        final Thread currentThread = Thread.currentThread(); // get the executor thread.

        final Thread uploadThread = new Thread("Upload Thread") {

            private final Logger log = LogManager.getLogger(this.getName());

            @Override
            public void run() {

                synchronized (AbstractFileTransferClient.this) {

                    // Notify the executor thread that the upload is starting.
                    // The executor thread will stop waiting for the upload to start
                    // but will not do anything until it receives the object's monitor.
                    canResume = true;
                    AbstractFileTransferClient.this.notifyAll();

                    int progressEventNumber = 0; // the number of the progress event on which to wait
                    if (new File(localFile).length() == 0) {
                        // if the file is empty no progress events will be fired
                        progressEventNumber = -1;
                    }

                    // Add a listener to notify the executor that the transfer is paused.
                    TransferListener listener = addListener(progressEventNumber);

                    try {
                        // Start the upload.
                        AbstractFileTransferClient.this.performUploadFile(localFile, remoteDir, remoteFile);

                        // Notify the executor thread that the upload has finished successfully.
                        AbstractFileTransferClient.this.notifyAll();
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

    /**
     * Check if
     * @param expected the expected result: true if a running transfer is expected, false otherwise.
     * @throws FileTransferException
     */
    protected void checkPausedTransferRunning(
                                               boolean expected ) throws FileTransferException {

        if (isTransferStartedAndPaused && !expected) {
            throw new FileTransferException("A transfer is already started and paused. It must be resumed before another one is initiated.");
        }
        if (!isTransferStartedAndPaused && expected) {
            throw new FileTransferException("A transfer is not started and paused. Cannot perform resume.");
        }
    }

    /**
     * Implement this method to create and  add a listener to the actual transfer client.
     * The listener should be implemented to wait when it receives a progress event
     * so the transfer can be paused.
     * This method is called before the upload is started in the upload thread created in
     * startUploadAndPause method.
     * @param progressEventNumber the consequent number of the progress event on which
     * the transfer should be paused.
     * @return The listener.
     */
    protected abstract TransferListener addListener(

                                                     int progressEventNumber );

    /**
     * Implement this method to remove the listener added in the addListener method.
     * This method is called after the upload is performed in the startUploadAndPause method.
     * @param listener The listener to be removed.
     */
    protected abstract void removeListener(
                                            TransferListener listener );

    /**
     * Enable or disable response gathering. If enabled the responses for each protocol command
     * will be gathered.
     * @param enable <b>true</b> to enable, <b>false</b> to disable response gathering.
     */
    public void enableResponseCollection(
                                          boolean enable ) {

    }

    /**
     * @return array of all the responses so far.
     */
    public String[] getResponses() {

        if (this.listener == null) {
            return new String[]{};
        }

        List<String> responses = this.listener.getResponses();

        return responses.toArray(new String[responses.size()]);
    }

    /**
     * Add custom property for the specific client.
     * Default implementation throws {@link IllegalArgumentException} since it
     * does not support any parameters.
     * <p>Note: For implementations you should use the {@link #customProperties} Map</p>
     * @see IFileTransferClient
     * @param key property key name. About possible values check the protocol specific client implementations
     * @param value object with supported value
     * @throws IllegalArgumentException if the key or value is not supported
     */
    public void addCustomProperty(
                                   String key,
                                   Object value ) throws IllegalArgumentException {

        throw new IllegalArgumentException("Not supported");

    }

    /**
     * Method which is intended to be used to apply set/new properties on each connect()
     * if there are connection initialization parameters set.
     * Default implementation throws {@link IllegalArgumentException}
     * <p>Note: For implementations you should use the {@link #customProperties} Map</p>
     * @see IFileTransferClient
     * @throws IllegalArgumentException if the key or value is not supported
     */
    public void applyCustomProperties() throws IllegalArgumentException {

        // do nothing
    }

    /**
     * Breaks "prop1,prop2,prop3" into array of tokens.
     * It trims each token.
     * 
     * @param source string
     * @return result tokens
     */
    protected String[] parseCustomProperties(
                                              String source ) {

        String[] result = source.split(",");
        for (int i = 0; i < result.length; i++) {
            result[i] = result[i].trim();
        }

        return result;
    }

    //@Override - interface impl., JavaSE 6
    public boolean isDebugMode() {

        return debugMode;
    }

    //@Override - interface impl., JavaSE 6
    public void setDebugMode(
                              boolean turnDebug ) {

        debugMode = turnDebug;
    }

}
