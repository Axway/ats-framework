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

import com.axway.ats.common.filetransfer.FileTransferException;
import com.axway.ats.common.filetransfer.TransferMode;
import com.axway.ats.core.filetransfer.AbstractFileTransferClient;
import com.axway.ats.core.filetransfer.model.AbstractResponseListener;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractFtpClient {

    private static final int DEFAULT_CONNECTION_TIMEOUT = 30 * 1000;                    // thirty seconds

    /**
     * Initial connection timeout in milliseconds. If the server does not accept connection within the specified time, then an exception is raised.
     */
    protected int timeout = DEFAULT_CONNECTION_TIMEOUT;

    protected int port;

    protected int passivePort = -1; // available only when working with FTP/FTPS clients

    protected TransferMode transferMode = TransferMode.BINARY;

    /**
     * Flag indicating if the resumePausedTransferMethod can resume a transfer
     * or should wait until it is permitted.
     */
    protected boolean canResume = false;

    /**
     * Flag indicating if transfer has been started and paused
     */
    protected boolean isTransferStartedAndPaused = false;

    /**
     * Listener for gathering command responses
     */
    protected AbstractResponseListener listener = null;

    /**
     * Flag to track for debug mode. Clients may use this and issue verbose info.
     */
    protected boolean debugMode = false;

    /**
     * Container for accumulating custom properties and then for apply in
     * initialization just before connect.
     */
    protected Map<String, Object> customProperties = new HashMap<>();

    /**
     * Basic constructor. Used when loading the client via java reflection
     */
    protected AbstractFtpClient() {

    }

    /**
     * Set the {@link TransferMode} that this protocol should use
     *
     * @param mode
     * @throws FtpException
     */
    public void setTransferMode(TransferMode mode) throws FtpException {

        this.transferMode = mode;
    }

    /**
     * Overrides the default connection timeout with a new custom value
     *
     * @param newValue the new value of the default connection timeout in milliseconds
     */
    public void setConnectionTimeout(int newValue) {

        this.timeout = newValue;
    }

    /**
     * Sets a custom port that would be used when connecting to the remote host
     *
     * @param portNumber the port number
     */
    public void setCustomPort(int portNumber) {

        this.port = portNumber;
    }

    /**
     * Check if
     * @param expected the expected result: true if a running transfer is expected, false otherwise.
     * @throws FileTransferException
     */
    public void checkPausedTransferRunning(boolean expected) throws FileTransferException {

        if (isTransferStartedAndPaused && !expected) {
            throw new FileTransferException(
                    "A transfer is already started and paused. It must be resumed before another one is initiated.");
        }
        if (!isTransferStartedAndPaused && expected) {
            throw new FileTransferException("A transfer is not started and paused. Cannot perform resume.");
        }
    }

    public boolean isTransferStartedAndPaused() {
        return isTransferStartedAndPaused;
    }

    public void setTransferStartedAndPaused(boolean transferStartedAndPaused) {
        isTransferStartedAndPaused = transferStartedAndPaused;
    }

    public boolean isCanResume() {
        return canResume;
    }

    public void setCanResume(boolean canResume) {
        this.canResume = canResume;
    }

    /**
     * Implement this method for performing actual uploads.
     * This method is called in uploadFile to perform the upload.
     * @param localFile
     * @param remoteDir
     * @param remoteFile
     * @throws FileTransferException
     */
    protected abstract void performUploadFile(final String localFile, final String remoteDir, final String remoteFile)
            throws FileTransferException;

    /**
     * Implement this method for performing actual downloads.
     * This method is called in downloadFile to perform the download.
     * @param localFile name of the path to store downloaded file. Could contain a directory path
     * @param remoteDir the remote path for the file/resource to be downloaded. Should end with "/"
     * @param remoteFile the file in the remoteDir path. Should not start with path separator
     * @throws FileTransferException
     */
    protected abstract void performDownloadFile(String localFile, String remoteDir, String remoteFile)
            throws FileTransferException;

    protected abstract String[] getAllReplyLines();

    /**
     * Breaks "prop1,prop2,prop3" into an array of tokens.
     * It trims each token.
     *
     * @param source string
     * @return result tokens
     */
    protected String[] parseCustomProperties(String source) {

        String[] result = source.split(",");
        for (int i = 0; i < result.length; i++) {
            result[i] = result[i].trim();
        }

        return result;
    }

    protected String constructExecutionErrorMessage(String command, String[] arguments) {

        StringBuilder sb = new StringBuilder();

        sb.append("Error occurred while executing '").append(command).append(" ");
        if (arguments != null) {
            for (String argument : arguments) {
                sb.append(argument + " ");
            }
        }
        sb.setLength(sb.toString().length() - 1);
        sb.append("' command. Server's response was: ").append(getAllReplyLinesAsString());

        return sb.toString();
    }

    protected String constructExceptionMessage(String command, String[] arguments) {

        StringBuilder sb = new StringBuilder();

        sb.append("Unable to execute '").append(command).append(" ");
        if (arguments != null) {
            for (String argument : arguments) {
                sb.append(argument).append(" ");
            }
        }
        sb.setLength(sb.toString().length() - 1);
        sb.append("' command");

        return sb.toString();

    }

    protected String getAllReplyLinesAsString() {
        StringBuilder sb = new StringBuilder();

        for (String line : getAllReplyLines()) {
            sb.append(line);
        }

        return sb.toString();
    }

    protected synchronized void resumePausedTransfer() throws FileTransferException {

        checkPausedTransferRunning(true);

        final Logger log = Logger.getLogger(AbstractFileTransferClient.class);

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

}
