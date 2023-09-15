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

import com.axway.ats.action.BaseTest;
import com.axway.ats.common.filetransfer.FileTransferException;
import com.axway.ats.common.filetransfer.TransferMode;
import org.apache.http.ssl.SSLContextBuilder;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectNew;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

/**
 * Tests the {@link org.apache.commons.net.ftp.FTPSClient} class
 */
@RunWith( PowerMockRunner.class)
@PrepareForTest( FtpsClient.class)
@PowerMockIgnore( "javax.net.ssl.*")
public class Test_FTPSClient extends BaseTest {

    private static final int                      PORT_NUMBER           = 21;
    private static final int                      CUSTOM_PORT_NUMBER    = 23;
    private static final int                      CUSTOM_TIMEOUT        = 10;
    private static final int                      DEFAULT_TIMEOUT       = 30000;
    private static final String                   USERNAME              = "sample.username";
    private static final String                   PASSWORD              = "sample.password";
    private static final String                   HOSTNAME              = "sample.host.corp";
    private static final String                   KEYSTORE_FILE         = "sample.keystore.file";
    private static final String                   KEYSTORE_PASSWORD     = "sample.keystore.password";
    private static final String                   ALIAS                 = "sample.certificate.alias";
    private static final String                   REMOTE_FILE_NAME      = "sample.remote.file.name";
    private static final String                   REMOTE_DIRECTORY_NAME = "sample/directory/name";
    private static final String                   CURRENT_REMOTE_DIR    = "sample/current/directory/name";
    private static final String                   LOCAL_FILE_NAME       = "sample.local.filename";

    private FtpsClient testObject            = null;
    private org.apache.commons.net.ftp.FTPSClient mockFtp               = null;

    @BeforeClass
    public static void beforeClass() throws IOException {

        new File(LOCAL_FILE_NAME).createNewFile();
    }

    @AfterClass
    public static void afterClass() {

        new File(LOCAL_FILE_NAME).delete();
    }

    @Before
    public void makeMeHappy() throws KeyManagementException, NoSuchAlgorithmException {

        mockFtp = createMock(org.apache.commons.net.ftp.FTPSClient.class, SSLContextBuilder.create().build());
        testObject = new FtpsClient();
        testObject.setCustomPort(PORT_NUMBER);
    }

    @Test( expected = UnknownHostException.class)
    public void testConnectBinary() throws Exception {

        expectNew(org.apache.commons.net.ftp.FTPSClient.class).andReturn(mockFtp);

        //mockFtp.setDebugStream( isA( DebugStream.class ) );
        //mockFtp.setDebug( true );

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPSClient.BINARY_FILE_TYPE)).andReturn(true);

        replayAll();

        testObject.setDebugMode(true);
        testObject.connect(HOSTNAME, USERNAME, PASSWORD);

        verifyAll();
    }

    @Test( expected = UnknownHostException.class)
    public void testConnectAscii() throws Exception {

        expectNew(org.apache.commons.net.ftp.FTPSClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPSClient.ASCII_FILE_TYPE)).andReturn(true);

        replayAll();

        testObject.setTransferMode(TransferMode.ASCII);
        testObject.connect(HOSTNAME, USERNAME, PASSWORD);

        verifyAll();
    }

    @Test( expected = UnknownHostException.class)
    public void testDisconnect() throws Exception {

        expectNew(org.apache.commons.net.ftp.FTPSClient.class).andReturn(mockFtp);

        expect(mockFtp.isConnected()).andReturn(true);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPSClient.BINARY_FILE_TYPE)).andReturn(true);

        mockFtp.disconnect();

        replayAll();

        testObject.connect(HOSTNAME, USERNAME, PASSWORD);
        testObject.disconnect();

        verifyAll();
    }

    @Test( expected = UnknownHostException.class)
    public void testConnectCustomPort() throws Exception {

        expectNew(org.apache.commons.net.ftp.FTPSClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, CUSTOM_PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPSClient.BINARY_FILE_TYPE)).andReturn(true);

        replayAll();

        testObject.setCustomPort(CUSTOM_PORT_NUMBER);
        testObject.connect(HOSTNAME, USERNAME, PASSWORD);

        verifyAll();
    }

    @Test( expected = UnknownHostException.class)
    public void testConnectCustomTimeout() throws Exception {

        expectNew(org.apache.commons.net.ftp.FTPSClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(CUSTOM_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPSClient.BINARY_FILE_TYPE)).andReturn(true);

        replayAll();

        testObject.setConnectionTimeout(CUSTOM_TIMEOUT);
        testObject.connect(HOSTNAME, USERNAME, PASSWORD);

        verifyAll();
    }

    @Test( expected = RuntimeException.class)
    public void testSecureConnect() {

        testObject.connect(HOSTNAME, KEYSTORE_FILE, KEYSTORE_PASSWORD, ALIAS);
    }

    @Test( expected = FtpException.class)
    public void testConnectNegative() {

        FtpsClient client = new FtpsClient();
        client.setCustomPort(PORT_NUMBER);
        client.connect(HOSTNAME, USERNAME, PASSWORD);
    }

    @Test( expected = UnknownHostException.class)
    public void testChangeModeASCII() throws Exception {

        expectNew(org.apache.commons.net.ftp.FTPSClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPSClient.BINARY_FILE_TYPE)).andReturn(true);

        expect(mockFtp.isConnected()).andReturn(true);
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPSClient.ASCII_FILE_TYPE)).andReturn(true);

        replayAll();

        testObject.connect(HOSTNAME, USERNAME, PASSWORD);
        testObject.setTransferMode(TransferMode.ASCII);

        verifyAll();
    }

    @Test( expected = UnknownHostException.class)
    public void testChangeModeBinary() throws Exception {

        expectNew(org.apache.commons.net.ftp.FTPSClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPSClient.ASCII_FILE_TYPE)).andReturn(true);

        expect(mockFtp.isConnected()).andReturn(true);
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPSClient.BINARY_FILE_TYPE)).andReturn(true);

        replayAll();

        testObject.setTransferMode(TransferMode.ASCII);
        testObject.connect(HOSTNAME, USERNAME, PASSWORD);
        testObject.setTransferMode(TransferMode.BINARY);

        verifyAll();
    }

    @Test( expected = UnknownHostException.class)
    public void testChangeModeNegative() throws Exception {

        expectNew(org.apache.commons.net.ftp.FTPSClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPSClient.BINARY_FILE_TYPE)).andReturn(true);

        expect(mockFtp.isConnected()).andReturn(true);
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPSClient.ASCII_FILE_TYPE)).andReturn(true);

        expect(mockFtp.isConnected()).andReturn(true);

        replayAll();

        testObject.connect(HOSTNAME, USERNAME, PASSWORD);
        testObject.setTransferMode(TransferMode.ASCII); //BINARY
        testObject.setTransferMode(TransferMode.ASCII);

        verifyAll();
    }

    @Test( expected = UnknownHostException.class)
    public void testDownload() throws Exception {

        expectNew(org.apache.commons.net.ftp.FTPSClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPSClient.BINARY_FILE_TYPE)).andReturn(true);

        expect(mockFtp.printWorkingDirectory()).andReturn(CURRENT_REMOTE_DIR);
        expect(mockFtp.changeWorkingDirectory(REMOTE_DIRECTORY_NAME)).andReturn(true);
        expect(mockFtp.retrieveFile(eq(REMOTE_FILE_NAME), isA(FileOutputStream.class))).andReturn(true);
        expect(mockFtp.changeWorkingDirectory(CURRENT_REMOTE_DIR)).andReturn(true);

        expect(mockFtp.getPassiveHost()).andReturn(HOSTNAME);

        replayAll();

        testObject.connect(HOSTNAME, USERNAME, PASSWORD);
        testObject.storeFile(LOCAL_FILE_NAME, REMOTE_DIRECTORY_NAME, REMOTE_FILE_NAME);

        verifyAll();
    }

    @Test( expected = UnknownHostException.class)
    public void testDownloadException() throws Exception {

        expectNew(org.apache.commons.net.ftp.FTPSClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPSClient.BINARY_FILE_TYPE)).andReturn(true);

        expect(mockFtp.printWorkingDirectory()).andThrow(new IOException(""));

        replayAll();

        testObject.connect(HOSTNAME, USERNAME, PASSWORD);
        testObject.storeFile(LOCAL_FILE_NAME, REMOTE_DIRECTORY_NAME, REMOTE_FILE_NAME);

        verifyAll();
    }

    @Test( expected = UnknownHostException.class)
    public void testUpload() throws Exception {

        expectNew(org.apache.commons.net.ftp.FTPSClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPSClient.BINARY_FILE_TYPE)).andReturn(true);

        expect(mockFtp.printWorkingDirectory()).andReturn(CURRENT_REMOTE_DIR);
        expect(mockFtp.changeWorkingDirectory(REMOTE_DIRECTORY_NAME)).andReturn(true);
        expect(mockFtp.storeFile(eq(REMOTE_FILE_NAME), isA(FileInputStream.class))).andReturn(true);
        expect(mockFtp.changeWorkingDirectory(CURRENT_REMOTE_DIR)).andReturn(true);

        expect(mockFtp.getPassiveHost()).andReturn(HOSTNAME);

        replayAll();

        testObject.connect(HOSTNAME, USERNAME, PASSWORD);
        testObject.retrieveFile(LOCAL_FILE_NAME, REMOTE_DIRECTORY_NAME, REMOTE_FILE_NAME);

        verifyAll();
    }

    @Test( expected = UnknownHostException.class)
    public void testUploadException() throws Exception {

        expectNew(org.apache.commons.net.ftp.FTPSClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPSClient.BINARY_FILE_TYPE)).andReturn(true);

        expect(mockFtp.printWorkingDirectory()).andThrow(new IOException(""));

        replayAll();

        testObject.connect(HOSTNAME, USERNAME, PASSWORD);
        testObject.retrieveFile(LOCAL_FILE_NAME, REMOTE_DIRECTORY_NAME, REMOTE_FILE_NAME);

        verifyAll();
    }


    @Test( expected = UnknownHostException.class)
    public void testResumeWOStartUploadAndPause() throws Exception {

        // connect
        expectNew(org.apache.commons.net.ftp.FTPSClient.class).andReturn(mockFtp);

        mockFtp.setConnectTimeout(DEFAULT_TIMEOUT);
        mockFtp.connect(HOSTNAME, PORT_NUMBER);
        expect(mockFtp.login(USERNAME, PASSWORD)).andReturn(true);
        mockFtp.enterLocalPassiveMode();
        expect(mockFtp.setFileType(org.apache.commons.net.ftp.FTPSClient.BINARY_FILE_TYPE)).andReturn(true);

        replayAll();

        testObject.connect(HOSTNAME, USERNAME, PASSWORD);

        Assert.assertFalse(testObject.isTransferStartedAndPaused);
        Assert.assertFalse(testObject.canResume);

        testObject.resumePausedTransfer();

        verifyAll();
    }

}
