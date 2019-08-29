/*
 * Copyright 2018-2019 Axway Software
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
package com.axway.ats.examples.basic;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.axway.ats.action.filesystem.FileSystemOperations;
import com.axway.ats.action.filetransfer.FileTransferClient;
import com.axway.ats.action.http.HttpClient;
import com.axway.ats.common.filetransfer.TransferProtocol;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.examples.common.BaseTestClass;

/**
 * File transfers over variety of protocols.
 *
 * Basic introduction: 
 *      https://axway.github.io/ats-framework/Basic-functionalities.html
 *
 * More examples per protocol:
 *      https://axway.github.io/ats-framework/File-transfers.html
 */
public class FileTransferTests extends BaseTestClass {

    // the local folder where we keep needed files
    private static final String RESOURCES_ROOT_DIR = configuration.getResourcesRootDir();

    // folder where we keep the uploaded files
    private static final String UPLOADS_DIR   = IoUtils.normalizeDirPath(RESOURCES_ROOT_DIR
                                                                         + "uploads");
    // folder where we keep the downloaded files
    private static final String DOWNLOADS_DIR = IoUtils.normalizeDirPath(RESOURCES_ROOT_DIR
                                                                         + "downloads");
    ;

    // the name of the file to upload
    private static final String LOCAL_FILE_NAME = "file.txt";

    // the name of the remote files
    private static final String REMOTE_FILE_NAME = "uploaded_file.txt";

    @BeforeClass
    public void beforeClass() {

        // This step is needed for the HTTP tests only.
        // We tell the remote HTTP server the folder to store to and read files from.
        // Of course, you can find a nicer way to do that.
        HttpClient httpClient = new HttpClient("http://" + configuration.getServerIp() + ":"
                                               + configuration.getHttpServerPort() + "/"
                                               + configuration.getHttpServerWebappWar() + "/transfers/");
        httpClient.addRequestHeader("repository", UPLOADS_DIR);
        httpClient.put();
        httpClient.close();
    }

    /**
     * Prior to each test we make sure we have the folder we worked with
     * in same state
     */
    @BeforeMethod
    public void beforeMethod() {

        FileSystemOperations fileOperations = new FileSystemOperations();

        // cleanup the content of base files dir
        fileOperations.deleteDirectoryContent(RESOURCES_ROOT_DIR);

        // create the dir used for uploaded files
        fileOperations.createDirectory(UPLOADS_DIR);

        // create the dir used for files to download
        fileOperations.createDirectory(DOWNLOADS_DIR);

        // create a file that will be uploaded
        fileOperations.createFile(RESOURCES_ROOT_DIR + LOCAL_FILE_NAME, "file with some simple content");
    }

    /**
     * Upload and download over FTP
     */
    @Test
    public void ftpTransfer() {

        // Instantiate the File Transfer client by providing the protocol to use
        FileTransferClient transferClient = new FileTransferClient(TransferProtocol.FTP);
        transferClient.setPort(configuration.getFtpServerPort());

        // 1. upload a file
        transferClient.connect(configuration.getServerIp(), configuration.getUserName(),
                               configuration.getUserPassword());
        transferClient.uploadFile(RESOURCES_ROOT_DIR + LOCAL_FILE_NAME, "/", REMOTE_FILE_NAME);
        transferClient.disconnect();

        // 2. make sure our we do not have the file on the local folder
        assertFalse(new FileSystemOperations().doesFileExist(DOWNLOADS_DIR + REMOTE_FILE_NAME));

        // 3. download a file
        transferClient.connect(configuration.getServerIp(), configuration.getUserName(),
                               configuration.getUserPassword());
        transferClient.downloadFile(DOWNLOADS_DIR, "/", REMOTE_FILE_NAME);
        transferClient.disconnect();

        // 4. make sure the file is downloaded
        assertTrue(new FileSystemOperations().doesFileExist(DOWNLOADS_DIR + REMOTE_FILE_NAME));
    }

    /**
     * Run some direct FTP commands
     */
    @Test
    public void executFtpCommands() {

        // connect in the regular way, it is easier than sending separated FTP commands
        FileTransferClient transferClient = new FileTransferClient(TransferProtocol.FTP);
        transferClient.setPort(configuration.getFtpServerPort());

        transferClient.connect(configuration.getServerIp(), configuration.getUserName(),
                               configuration.getUserPassword());

        // send FTP commands and check the returned results
        log.info("List of supported FTP commands:\n" + transferClient.executeCommand("help"));
        log.info("Get current directory:\n" + transferClient.executeCommand("pwd"));
        log.info("The connection status is:\n" + transferClient.executeCommand("stat"));

        transferClient.disconnect();
    }

    /**
     * Upload and download over HTTP
     */
    @Test
    public void httpTransfer() {

        FileTransferClient transferClient = new FileTransferClient(TransferProtocol.HTTP);
        // set a custom port(if not using the default one)
        transferClient.setPort(configuration.getHttpServerPort());

        // connect using appropriate parameters(we do not need user information)
        transferClient.connect(configuration.getServerIp());

        // upload a local file
        transferClient.uploadFile(RESOURCES_ROOT_DIR + LOCAL_FILE_NAME,
                                  "/" + configuration.getHttpServerWebappWar() + "/transfers/",
                                  "remoteFile.txt");

        // download the just uploaded file
        transferClient.downloadFile(DOWNLOADS_DIR,
                                    "/" + configuration.getHttpServerWebappWar() + "/transfers/",
                                    "remoteFile.txt");

        transferClient.disconnect();
    }

}
