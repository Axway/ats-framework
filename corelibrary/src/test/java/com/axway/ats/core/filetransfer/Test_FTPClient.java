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

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectNew;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.axway.ats.common.filetransfer.FileTransferException;
import com.axway.ats.common.filetransfer.TransferMode;
import com.axway.ats.core.BaseTest;
import com.axway.ats.core.filetransfer.model.ftp.FtpResponseListener;
import com.axway.ats.core.filetransfer.model.ftp.SynchronizationFtpTransferListener;

/**
* Tests the {@link Apacheorg.apache.commons.net.ftp.FTPClient} class
*/
@RunWith( PowerMockRunner.class)
@PrepareForTest( FtpClient.class)
public class Test_FTPClient extends BaseTest {

    private static final int                     PORT_NUMBER           = 21;
    private static final int                     CUSTOM_PORT_NUMBER    = 23;
    private static final int                     CUSTOM_TIMEOUT        = 10;
    private static final int                     DEFAULT_TIMEOUT       = 30000;
    private static final String                  USERNAME              = "sample.username";
    private static final String                  PASSWORD              = "sample.password";
    private static final String                  HOSTNAME              = "sample.host.corp";
    private static final String                  KEYSTORE_FILE         = "sample.kesytore.file";
    private static final String                  KEYSTORE_PASSWORD     = "sample.keystore.password";
    private static final String                  ALIAS                 = "sample.certificate.alias";
    private static final String                  REMOTE_FILE_NAME      = "sample.remote.file.name";
    private static final String                  REMOTE_DIRECTORY_NAME = "sample/directory/name";
    private static final String                  CURRENT_REMOTE_DIR    = "sample/current/directory/name";
    private static final String                  LOCAL_FILE_NAME       = "sample.local.filename";

    private FtpClient                            testObject            = null;
    private org.apache.commons.net.ftp.FTPClient mockFtp               = null;

    @BeforeClass
    public static void beforeClass() throws IOException {

        new File(LOCAL_FILE_NAME).createNewFile();
    }

    @AfterClass
    public static void afterClass() throws IOException {

        new File(LOCAL_FILE_NAME).delete();
    }

    @Before
    public void makeMeHappy() {

        mockFtp = createMock(org.apache.commons.net.ftp.FTPClient.class);
        testObject = new FtpClient();
        testObject.setCustomPort(PORT_NUMBER);
    }

    @Test( expected = AssertionError.class)
    public void testConnectBinary() throws Exception {

        expectNew(org.apache.commons.net.ftp.FTPClient.class).andReturn(mockFtp);

        //mockFtp.setDebugStream( isA( DebugStream.class ) );
        //mockFtp.setDebug( true );

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPClient.BINARY_FILE_TYPE)).andReturn(true);

        replayAll();

        testObject.setDebugMode(true);
        testObject.connect(HOSTNAME, USERNAME, PASSWORD);

        verifyAll();
    }

    @Test
    public void testConnectAscii() throws Exception {

        expectNew(org.apache.commons.net.ftp.FTPClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPClient.ASCII_FILE_TYPE)).andReturn(true);

        replayAll();

        testObject.setTransferMode(TransferMode.ASCII);
        testObject.connect(HOSTNAME, USERNAME, PASSWORD);

        verifyAll();
    }

    @Test
    public void testDisconnect() throws Exception {

        expectNew(org.apache.commons.net.ftp.FTPClient.class).andReturn(mockFtp);

        expect(mockFtp.isConnected()).andReturn(true);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPClient.BINARY_FILE_TYPE)).andReturn(true);

        mockFtp.disconnect();

        replayAll();

        testObject.connect(HOSTNAME, USERNAME, PASSWORD);
        testObject.disconnect();

        verifyAll();
    }

    @Test
    public void testConnectCustomPort() throws Exception {

        expectNew(org.apache.commons.net.ftp.FTPClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, CUSTOM_PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPClient.BINARY_FILE_TYPE)).andReturn(true);

        replayAll();

        testObject.setCustomPort(CUSTOM_PORT_NUMBER);
        testObject.connect(HOSTNAME, USERNAME, PASSWORD);

        verifyAll();
    }

    @Test
    public void testConnectCustomTimeout() throws Exception {

        expectNew(org.apache.commons.net.ftp.FTPClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(CUSTOM_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPClient.BINARY_FILE_TYPE)).andReturn(true);

        replayAll();

        testObject.setConnectionTimeout(CUSTOM_TIMEOUT);
        testObject.connect(HOSTNAME, USERNAME, PASSWORD);

        verifyAll();
    }

    @Test( expected = FileTransferException.class)
    public void testSecureConnect() throws FileTransferException {

        testObject.connect(HOSTNAME, KEYSTORE_FILE, KEYSTORE_PASSWORD, ALIAS);
    }

    @Test( expected = FileTransferException.class)
    public void testConnectNegative() throws FileTransferException {

        FtpClient client = new FtpClient();
        client.setCustomPort(PORT_NUMBER);
        client.connect(HOSTNAME, USERNAME, PASSWORD);
    }

    @Test
    public void testChangeModeASCII() throws Exception {

        expectNew(org.apache.commons.net.ftp.FTPClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPClient.BINARY_FILE_TYPE)).andReturn(true);

        expect(mockFtp.isConnected()).andReturn(true);
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPClient.ASCII_FILE_TYPE)).andReturn(true);

        replayAll();

        testObject.connect(HOSTNAME, USERNAME, PASSWORD);
        testObject.setTransferMode(TransferMode.ASCII);

        verifyAll();
    }

    @Test
    public void testChangeModeBinary() throws Exception {

        expectNew(org.apache.commons.net.ftp.FTPClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPClient.ASCII_FILE_TYPE)).andReturn(true);

        expect(mockFtp.isConnected()).andReturn(true);
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPClient.BINARY_FILE_TYPE)).andReturn(true);

        replayAll();

        testObject.setTransferMode(TransferMode.ASCII);
        testObject.connect(HOSTNAME, USERNAME, PASSWORD);
        testObject.setTransferMode(TransferMode.BINARY);

        verifyAll();
    }

    @Test
    public void testChangeModeNegative() throws Exception {

        expectNew(org.apache.commons.net.ftp.FTPClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPClient.BINARY_FILE_TYPE)).andReturn(true);

        expect(mockFtp.isConnected()).andReturn(true);
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPClient.ASCII_FILE_TYPE)).andReturn(true);

        expect(mockFtp.isConnected()).andReturn(true);

        replayAll();

        testObject.connect(HOSTNAME, USERNAME, PASSWORD);
        testObject.setTransferMode(TransferMode.ASCII); //BINARY
        testObject.setTransferMode(TransferMode.ASCII);

        verifyAll();
    }

    @Test
    public void testDownload() throws Exception {

        expectNew(org.apache.commons.net.ftp.FTPClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPClient.BINARY_FILE_TYPE)).andReturn(true);

        expect(mockFtp.retrieveFile(eq(REMOTE_DIRECTORY_NAME + "/" + REMOTE_FILE_NAME),
                                    isA(FileOutputStream.class))).andReturn(true);

        expect(mockFtp.getPassiveHost()).andReturn(HOSTNAME);

        replayAll();

        testObject.connect(HOSTNAME, USERNAME, PASSWORD);
        testObject.downloadFile(LOCAL_FILE_NAME, REMOTE_DIRECTORY_NAME, REMOTE_FILE_NAME);

        verifyAll();
    }

    @Test( expected = AssertionError.class)
    public void testDownloadException() throws Exception {

        expectNew(org.apache.commons.net.ftp.FTPClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPClient.BINARY_FILE_TYPE)).andReturn(true);

        replayAll();

        testObject.connect(HOSTNAME, USERNAME, PASSWORD);
        testObject.downloadFile(LOCAL_FILE_NAME, REMOTE_DIRECTORY_NAME, REMOTE_FILE_NAME);

        verifyAll();
    }

    @Test
    public void testUpload() throws Exception {

        expectNew(org.apache.commons.net.ftp.FTPClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPClient.BINARY_FILE_TYPE)).andReturn(true);

        expect(mockFtp.storeFile(eq(REMOTE_DIRECTORY_NAME + "/" + REMOTE_FILE_NAME),
                                 isA(FileInputStream.class))).andReturn(true);

        expect(mockFtp.getPassiveHost()).andReturn(HOSTNAME);

        replayAll();

        testObject.connect(HOSTNAME, USERNAME, PASSWORD);
        testObject.uploadFile(LOCAL_FILE_NAME, REMOTE_DIRECTORY_NAME, REMOTE_FILE_NAME);

        verifyAll();
    }

    @Test( expected = AssertionError.class)
    public void testUploadException() throws Exception {

        expectNew(org.apache.commons.net.ftp.FTPClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPClient.BINARY_FILE_TYPE)).andReturn(true);

        replayAll();

        testObject.connect(HOSTNAME, USERNAME, PASSWORD);
        testObject.uploadFile(LOCAL_FILE_NAME, REMOTE_DIRECTORY_NAME, REMOTE_FILE_NAME);

        verifyAll();
    }

    @Test
    public void testStartUploadAndPauseThenResume() throws Exception {

        // connect
        expectNew(org.apache.commons.net.ftp.FTPClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPClient.BINARY_FILE_TYPE)).andReturn(true);

        mockFtp.addProtocolCommandListener(isA(SynchronizationFtpTransferListener.class));

        // upload
        expect(mockFtp.storeFile(eq(REMOTE_DIRECTORY_NAME + "/" + REMOTE_FILE_NAME),
                                 isA(FileInputStream.class))).andReturn(true);
        expect(mockFtp.getPassiveHost()).andReturn(HOSTNAME);

        mockFtp.removeProtocolCommandListener(isA(SynchronizationFtpTransferListener.class));

        replayAll();

        testObject.connect(HOSTNAME, USERNAME, PASSWORD);

        Assert.assertFalse(testObject.isTransferStartedAndPaused);
        Assert.assertFalse(testObject.canResume);

        testObject.startUploadAndPause(LOCAL_FILE_NAME, REMOTE_DIRECTORY_NAME, REMOTE_FILE_NAME);

        Assert.assertTrue(testObject.isTransferStartedAndPaused);

        testObject.resumePausedTransfer();

        Assert.assertFalse(testObject.canResume);
        Assert.assertFalse(testObject.isTransferStartedAndPaused);

        verifyAll();
    }

    @Test( expected = FileTransferException.class)
    public void testStartUploadAndPauseTwice() throws Exception {

        // connect
        expectNew(org.apache.commons.net.ftp.FTPClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPClient.BINARY_FILE_TYPE)).andReturn(true);

        mockFtp.addProtocolCommandListener(isA(SynchronizationFtpTransferListener.class));

        // upload
        expect(mockFtp.printWorkingDirectory()).andReturn(CURRENT_REMOTE_DIR);
        expect(mockFtp.changeWorkingDirectory(REMOTE_DIRECTORY_NAME)).andReturn(true);
        expect(mockFtp.storeFile(eq(REMOTE_FILE_NAME), isA(FileInputStream.class))).andReturn(true);
        expect(mockFtp.changeWorkingDirectory(CURRENT_REMOTE_DIR)).andReturn(true);
        expect(mockFtp.getPassiveHost()).andReturn(HOSTNAME);

        mockFtp.removeProtocolCommandListener(isA(SynchronizationFtpTransferListener.class));

        replayAll();

        testObject.connect(HOSTNAME, USERNAME, PASSWORD);

        Assert.assertFalse(testObject.isTransferStartedAndPaused);
        Assert.assertFalse(testObject.canResume);

        testObject.startUploadAndPause(LOCAL_FILE_NAME, REMOTE_DIRECTORY_NAME, REMOTE_FILE_NAME);

        Assert.assertTrue(testObject.isTransferStartedAndPaused);

        testObject.startUploadAndPause(LOCAL_FILE_NAME, REMOTE_DIRECTORY_NAME, REMOTE_FILE_NAME);

        verifyAll();
    }

    @Test( expected = FileTransferException.class)
    public void testResumeWOStartUploadAndPause() throws Exception {

        // connect
        expectNew(org.apache.commons.net.ftp.FTPClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPClient.BINARY_FILE_TYPE)).andReturn(true);

        replayAll();

        testObject.connect(HOSTNAME, USERNAME, PASSWORD);

        Assert.assertFalse(testObject.isTransferStartedAndPaused);
        Assert.assertFalse(testObject.canResume);

        testObject.resumePausedTransfer();

        verifyAll();
    }

    @Test( expected = FileTransferException.class)
    public void testStartUploadAndPauseThenUpload() throws Exception {

        // connect
        expectNew(org.apache.commons.net.ftp.FTPClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPClient.BINARY_FILE_TYPE)).andReturn(true);

        mockFtp.addProtocolCommandListener(isA(SynchronizationFtpTransferListener.class));

        // upload
        expect(mockFtp.printWorkingDirectory()).andReturn(CURRENT_REMOTE_DIR);
        expect(mockFtp.changeWorkingDirectory(REMOTE_DIRECTORY_NAME)).andReturn(true);
        expect(mockFtp.storeFile(eq(REMOTE_FILE_NAME), isA(FileInputStream.class))).andReturn(true);
        expect(mockFtp.changeWorkingDirectory(CURRENT_REMOTE_DIR)).andReturn(true);
        expect(mockFtp.getPassiveHost()).andReturn(HOSTNAME);

        mockFtp.removeProtocolCommandListener(isA(SynchronizationFtpTransferListener.class));

        replayAll();

        testObject.connect(HOSTNAME, USERNAME, PASSWORD);

        Assert.assertFalse(testObject.isTransferStartedAndPaused);
        Assert.assertFalse(testObject.canResume);

        testObject.startUploadAndPause(LOCAL_FILE_NAME, REMOTE_DIRECTORY_NAME, REMOTE_FILE_NAME);

        Assert.assertTrue(testObject.isTransferStartedAndPaused);

        testObject.uploadFile(LOCAL_FILE_NAME, REMOTE_DIRECTORY_NAME, REMOTE_FILE_NAME);

        verifyAll();
    }

    @Test( expected = FileTransferException.class)
    public void testStartUploadAndPauseThenDownload() throws Exception {

        // connect
        expectNew(org.apache.commons.net.ftp.FTPClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPClient.BINARY_FILE_TYPE)).andReturn(true);

        mockFtp.addProtocolCommandListener(isA(SynchronizationFtpTransferListener.class));

        // upload
        expect(mockFtp.printWorkingDirectory()).andReturn(CURRENT_REMOTE_DIR);
        expect(mockFtp.changeWorkingDirectory(REMOTE_DIRECTORY_NAME)).andReturn(true);
        expect(mockFtp.storeFile(eq(REMOTE_FILE_NAME), isA(FileInputStream.class))).andReturn(true);
        expect(mockFtp.changeWorkingDirectory(CURRENT_REMOTE_DIR)).andReturn(true);
        expect(mockFtp.getPassiveHost()).andReturn(HOSTNAME);

        mockFtp.removeProtocolCommandListener(isA(SynchronizationFtpTransferListener.class));

        replayAll();

        testObject.connect(HOSTNAME, USERNAME, PASSWORD);

        Assert.assertFalse(testObject.isTransferStartedAndPaused);
        Assert.assertFalse(testObject.canResume);

        testObject.startUploadAndPause(LOCAL_FILE_NAME, REMOTE_DIRECTORY_NAME, REMOTE_FILE_NAME);

        Assert.assertTrue(testObject.isTransferStartedAndPaused);

        testObject.downloadFile(LOCAL_FILE_NAME, REMOTE_DIRECTORY_NAME, REMOTE_FILE_NAME);

        verifyAll();
    }

    @Test
    public void testGatherResponses() throws Exception {

        expectNew(org.apache.commons.net.ftp.FTPClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPClient.BINARY_FILE_TYPE)).andReturn(true);

        FtpResponseListener listener = new FtpResponseListener();
        expectNew(FtpResponseListener.class).andReturn(listener);

        mockFtp.addProtocolCommandListener(listener);
        mockFtp.removeProtocolCommandListener(listener);

        replayAll();

        testObject.connect(HOSTNAME, USERNAME, PASSWORD);
        testObject.enableResponseCollection(true);
        testObject.enableResponseCollection(false);

        verifyAll();
    }

    @Test
    public void testGetResponses() throws Exception {

        FtpResponseListener listener = new FtpResponseListener();
        List<String> initializeResponses = new ArrayList<String>();
        initializeResponses.add("1");
        listener.setResponses(initializeResponses);

        expectNew(org.apache.commons.net.ftp.FTPClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPClient.BINARY_FILE_TYPE)).andReturn(true);

        expectNew(FtpResponseListener.class).andReturn(listener);

        mockFtp.addProtocolCommandListener(listener);

        replayAll();

        testObject.connect(HOSTNAME, USERNAME, PASSWORD);
        testObject.enableResponseCollection(true);
        String[] responses = testObject.getResponses();

        Assert.assertEquals(initializeResponses.size(), responses.length);
        Assert.assertEquals(initializeResponses.get(0), responses[0]);

        verifyAll();
    }
}
