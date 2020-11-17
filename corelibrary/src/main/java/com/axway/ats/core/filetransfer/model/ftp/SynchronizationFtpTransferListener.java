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
package com.axway.ats.core.filetransfer.model.ftp;

import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.commons.net.ftp.FTPCmd;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.core.filetransfer.model.IFileTransferClient;
import com.axway.ats.core.filetransfer.model.TransferListener;

public class SynchronizationFtpTransferListener implements ProtocolCommandListener, TransferListener {

    private static final Logger       log                 = LogManager.getLogger(SynchronizationFtpTransferListener.class);

    /**
     * The consequent progress event on which the transfer will pause
     */
    private final int                 progressEventNumber;
    /**
     * The number of the current progress event being received.
     */
    private int                       currentProgessEvent = 0;

    /**
     * The client which initiated the transfer.
     */
    private final IFileTransferClient owner;

    /**
     * @param progressEventNumber The number of the progress event on which to pause.
     */
    public SynchronizationFtpTransferListener( IFileTransferClient owner,
                                               int progressEventNumber ) {

        this.progressEventNumber = progressEventNumber;
        this.owner = owner;

        // If the transferred file is empty and the current thread has
        // the owner's monitor prevent a deadlock by waiting for the resume.
        if (progressEventNumber < 0 && Thread.holdsLock(owner)) {
            try {
                log.warn("Empty transferred file. Waiting in listener constructor for the transfer to be resumed...");
                // Release the monitor and wait to be notified to continue the transfer.
                owner.wait();
            } catch (InterruptedException e) {
                log.error("Transfer thread interrupted while paused in constructor.", e);
            }
        }
    }

    @Override
    public void protocolCommandSent(
                                     ProtocolCommandEvent event ) {

        /* because we can only pause a file upload, we check if the event has a STOR command */
        if (event.getCommand().equals(FTPCmd.STOR.getCommand())) {
            // Check only progress events so the transfer be paused when the 
            // transfer is taking place not before or after it.
            log.debug("Progress event #" + (currentProgessEvent));
            if (currentProgessEvent++ == progressEventNumber && Thread.holdsLock(owner)) {
                try {
                    log.debug("Waiting for the transfer to be resumed...");
                    // Release the monitor and wait to be notified to continue the transfer.
                    owner.wait();
                } catch (InterruptedException e) {
                    log.error("Transfer thread interrupted while paused. Continue transfer.", e);
                }
            }

        }

    }

    @Override
    public void protocolReplyReceived(
                                       ProtocolCommandEvent event ) {

    }

}
