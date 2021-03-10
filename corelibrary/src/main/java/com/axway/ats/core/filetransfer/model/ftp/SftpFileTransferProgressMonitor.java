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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jcraft.jsch.SftpProgressMonitor;

/** Class used to log file transfer progress when debugMode is true (enabled) for @SftpClient **/
public class SftpFileTransferProgressMonitor implements SftpProgressMonitor {

    private static final Logger log                  = LogManager.getLogger(SftpFileTransferProgressMonitor.class);

    private String              source               = null;
    private String              destination          = null;
    private long                totalBytesTransfered = -1;
    private long                fileSize             = -1;

    public SftpFileTransferProgressMonitor() {

    }

    public SftpFileTransferProgressMonitor( String source,
                                            String destination,
                                            long fileSize ) {

        this.source = source;
        this.destination = destination;
        this.fileSize = fileSize;
    }

    @Override
    public void init(
                      int op,
                      String src,
                      String dest,
                      long max ) {

        String operation = (op == SftpProgressMonitor.PUT)
                                                           ? "UPLOAD"
                                                           : "DOWNLOAD";

        log.debug("Begin " + operation + " from " + this.source + " to " + this.destination);

    }

    @Override
    public void end() {

        if (this.fileSize > 0) {
            log.debug("Transfer finished. Successfully transfered " + this.totalBytesTransfered + " from "
                      + this.fileSize);
        } else {
            log.debug("Transfer finished. Successfully transfered " + this.totalBytesTransfered);
        }

    }

    @Override
    public boolean count(
                          long count ) {

        this.totalBytesTransfered += count;
        /* if the operation is UPLOAD, we know what is the file size of the file to be uploaded,
         * so we log the total transfered bytes as a percentage from the whole file size */
        if (this.fileSize > 0) {
            float transferedPercentage = (float) (this.totalBytesTransfered) / (this.fileSize) * 100.0f;

            log.debug(Math.ceil(transferedPercentage) + " % uploaded.");
        } else {
            /* if the operation is DOWNLOAD, we do not know the file size of the file to be downloaded,
             * so we log only the total bytes transfered from the start of the operation up to the current moment*/
            log.debug(this.totalBytesTransfered + " bytes downloaded overall.");
        }
        return true;
    }

    public void setTransferMetadata(
                                     String source,
                                     String destination,
                                     long fileSize ) {

        this.source = source;
        this.destination = destination;
        this.fileSize = fileSize;
    }
}
