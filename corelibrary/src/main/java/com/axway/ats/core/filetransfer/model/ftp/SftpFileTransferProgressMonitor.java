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

import org.apache.log4j.Logger;

import com.sshtools.sftp.FileTransferProgress;

/** Class used to log file transfer progress when debugMode is true (enabled) for @SftpClient **/
public class SftpFileTransferProgressMonitor implements FileTransferProgress {

    private static final Logger log         = Logger.getLogger( SftpFileTransferProgressMonitor.class );

    private String              source      = null;
    private String              destination = null;

    public SftpFileTransferProgressMonitor() {

    }

    public void setTransferMetadata( String source, String destination ) {

        this.source = source;
        this.destination = destination;
    }

    @Override
    public void started( long bytesTotal, String remoteFile ) {

        log.debug( "Start file transfer from '" + this.source + "' to '" + this.destination
                   + "' with total size " + bytesTotal );

    }

    @Override
    public boolean isCancelled() {

        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void progressed( long bytesSoFar ) {

        log.debug( "Total bytes transfered so far " + bytesSoFar );

    }

    @Override
    public void completed() {

        log.debug( "File successfully transfered." );
    }
}
