package com.axway.ats.action.ftp;

import com.axway.ats.common.filetransfer.FileTransferException;
import com.axway.ats.common.filetransfer.TransferMode;
import com.axway.ats.core.filetransfer.model.AbstractResponseListener;
import com.axway.ats.core.filetransfer.model.TransferListener;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractClient {
    private static final int           DEFAULT_CONNECTION_TIMEOUT = 30 * 1000;                    // thirty seconds

    /**
     * Initial connection timeout in milliseconds. If the server does not accept connection within the specified time then an exception is raised.
     */
    protected int                      timeout                    = DEFAULT_CONNECTION_TIMEOUT;

    protected int                      port;

    protected int                      passivePort               = -1; // available only when working with FTP/FTPS clients

    protected TransferMode transferMode               = TransferMode.BINARY;

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
    protected Map<String, Object> customProperties           = new HashMap<String, Object>();

    /**
     * Basic constructor. Used when loading the client via java reflection
     */
    public AbstractClient() {

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
            String remoteFile ) throws FtpException;

    /**
     * Implement this method for performing actual uploads.
     * This method is called in uploadFile to perform the upload.
     * @param localFile
     * @param remoteDir
     * @param remoteFile
     * @throws FtpException
     */
    protected abstract void performUploadFile(
            final String localFile,
            final String remoteDir,
            final String remoteFile ) throws FtpException;

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
     * Check if
     * @param expected the expected result: true if a running transfer is expected, false otherwise.
     * @throws FileTransferException
     */
    protected void checkPausedTransferRunning(
            boolean expected ) throws FtpException {

        if (isTransferStartedAndPaused && !expected) {
            throw new FileTransferException("A transfer is already started and paused. It must be resumed before another one is initiated.");
        }
        if (!isTransferStartedAndPaused && expected) {
            throw new FileTransferException("A transfer is not started and paused. Cannot perform resume.");
        }
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

}
