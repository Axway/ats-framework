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
package com.axway.ats.framework.examples.vm.actions;

import java.io.File;

import com.axway.ats.action.filetransfer.FileTransferClient;
import com.axway.ats.agent.core.model.Action;
import com.axway.ats.agent.core.model.Parameter;
import com.axway.ats.common.filetransfer.TransferProtocol;

/**
 * Class containing actions which can be used to run simultaneous file transfers 
 *
 * Creating actions is presented at:
 *      https://axway.github.io/ats-framework/Creating-ATS-actions.html
 *
 * This class is the one presented at:
 *      https://axway.github.io/ats-framework/Creating-an-Agent-Component.html
 * but here we made some minor changes to make it work for more than one transfer protocol.
 *
 * Note that our action classes can have as many as needed action methods
 * which will do whatever code you've put in them.
 */
public class FileTransferActions {

    /*
     * This is the ATS client used for sending files over variety of transfer protocols.
     * That client cannot work in more than one thread at once, but we will make this
     * possible by wrapping it in the current action class.
     */
    private FileTransferClient transferClient;

    /**
     * Do a connect
     *
     * @param protocol the transfer protocol to use
     * @param port the remote port
     * @param hostname the remote host
     * @param username the user name
     * @param password the user password
     */
    @Action
    public void connect( @Parameter( name = "protocol" ) String protocol, @Parameter( name = "port" ) int port,
                         @Parameter( name = "hostname" ) String hostname,
                         @Parameter( name = "username" ) String username,
                         @Parameter( name = "password" ) String password ) {

        // make instance of the transfer client and do a connect
        transferClient = new FileTransferClient(TransferProtocol.valueOf(protocol));
        transferClient.setPort(port);

        transferClient.connect(hostname, username, password);
    }

    /**
     * Uploads a file over specified protocol.
     * It returns the length of the transfered data, this way you will see in Test Explorer how fast the transfer was.
     *
     * @param localFilePath path to the file to upload
     * @return the length of the transfered data
     */
    @Action( transferUnit = "Kbytes" )
    public Long upload( @Parameter( name = "localFilePath" ) String localFilePath ) {

        transferClient.uploadFile(localFilePath, "/");

        return new File(localFilePath).length() / 1024;
    }

    /**
     * Uploads a file over specified protocol.
     * It returns the length of the transfered data, this way you will see in Test Explorer how fast the transfer was.
     *
     * @param localFilePath path to the file to upload
     * @param remoteDir the remote directory to place the file into
     * @param remoteFile the name of the transfered file on the remote location
     * @return the length of the transfered data
     */
    @Action( transferUnit = "Kbytes" )
    public Long upload( @Parameter( name = "localFilePath" ) String localFilePath,
                        @Parameter( name = "remoteDir" ) String remoteDir,
                        @Parameter( name = "remoteFile" ) String remoteFile ) {

        transferClient.uploadFile(localFilePath, remoteDir, remoteFile);

        return new File(localFilePath).length() / 1024;
    }

    /**
     * Disconnect the transfer client
     */
    @Action
    public void disconnect() {

        transferClient.disconnect();
    }
}
