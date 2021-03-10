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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Class used to log sent commands and received responses, when debugMode is true (enabled) for @FtpClient or @FtpsClient **/
public class FtpListener implements ProtocolCommandListener {

    private static final Logger log = LogManager.getLogger(FtpListener.class);

    public FtpListener() {

    }

    @Override
    public void protocolCommandSent(
                                     ProtocolCommandEvent event ) {

        log.debug("Sending the following command: " + event.getMessage());

    }

    @Override
    public void protocolReplyReceived(
                                       ProtocolCommandEvent event ) {

        log.debug("Receiving the following reply: " + event.getMessage());

    }

}
