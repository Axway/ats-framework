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
package com.axway.ats.action.filetransfer;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createMockAndExpectNew;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verify;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.io.File;

import com.axway.ats.action.ftp.FileTransferFtpClient;
import com.axway.ats.action.ftp.FtpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.axway.ats.action.BaseTest;
import com.axway.ats.action.filesystem.FileSystemOperations;
import com.axway.ats.common.filetransfer.FileTransferException;
import com.axway.ats.common.filetransfer.TransferMode;
import com.axway.ats.common.filetransfer.TransferProtocol;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.core.filetransfer.model.IFileTransferClient;
import com.axway.ats.core.validation.exceptions.InvalidInputArgumentsException;

import junit.framework.Assert;

/**
 * Unit tests for the {@link FileSystemOperations} class
 */
@RunWith( PowerMockRunner.class)
@PrepareForTest( { ClientFactory.class, FileTransferConfigurator.class })
public class Test_GenericTransferClient extends BaseTest {

    private static final String      SAMPLE_PASSWORD          = "sample.password";
    private static final String      SAMPLE_USER_NAME         = "sample.user.name";
    private static final String      SAMPLE_HOST_NAME         = "sample.host.name";
    private static final String      SAMPLE_LOCAL_DIRECTORY   = normalizeDirUrl("sample/local/directory/");
    private static final String      SAMPLE_REMOTE_DIRECTORY  = "sample/remote/directory/";
    private static final String      SAMPLE_LOCAL_FILE        = "sample.local.file";
    private static final String      SAMPLE_REMOTE_FILE       = "sample.remote.file";
    private static final String      SAMPLE_EXCEPTION_MESSAGE = "sample.exception.message";
    private static final String      SAMPLE_KEY_ALIAS         = "sample.key.alias";
    private static final int         SAMPLE_CUSTOM_PORT       = 22;
    private final FileTransferClient testObject               = new FileTransferClient(TransferProtocol.FTP);
    private FileTransferClient       mockedTestObject         = null;
    private FileTransferFtpClient    ftpMock                  = null;
    private ClientFactory            factoryMock              = null;

    /**
     * Setup method
     */
    @Before
    public void setUpTest_GeneralFileSystemOperations() {

        mockedTestObject = new FileTransferClient(TransferProtocol.FTP);

        // create a mock file transfer client
        ftpMock = createMock(FileTransferFtpClient.class);
        factoryMock = createMock(ClientFactory.class);

        // inject the mock client into the test object
        Whitebox.setInternalState(mockedTestObject, ftpMock);
    }

    // ---------------------------------------------------------------------
    // --------------------------- TEST CONNECT ----------------------------
    // ---------------------------------------------------------------------

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testConnect() throws Exception {

        // setup expectations
        ftpMock.connect(SAMPLE_HOST_NAME, SAMPLE_USER_NAME, SAMPLE_PASSWORD);
        replay(ftpMock);

        // execute operations
        mockedTestObject.connect(SAMPLE_HOST_NAME, SAMPLE_USER_NAME, SAMPLE_PASSWORD);

        // verify results
        verify(ftpMock);
    }

    @Test
    public void testConnectUsingCustomClientNegative() throws Exception {

        try {
            mockedTestObject = new FileTransferClient("HTTP", 80);
        } catch (FileTransferException e) {
            Assert.assertEquals(e.getMessage(),
                                "com.axway.ats.action.filetransfer.FileTransferConfiguratorException: Uknown custom client for HTTP_CUSTOM protocol. Either /ats-adapters.properties file is not in the classpath or actionlibrary.filetransfer.http.client property is missing/empty!");
            if (! (e instanceof FileTransferException)) {
                Assert.fail("Wrong exception type returned");
            }
        }
    }

    @Test( expected = AssertionError.class)
    public void testConnectUsingCustomClient() throws Exception {

        String customFileTransferClientName = "SomeCustomClientClass";

        mockStatic(FileTransferConfigurator.class);
        FileTransferConfigurator mockFileTransferConfigurator = createMock(FileTransferConfigurator.class);
        expect(FileTransferConfigurator.getInstance()).andReturn(mockFileTransferConfigurator);
        expect(mockFileTransferConfigurator.getFileTransferClient("HTTP")).andReturn(customFileTransferClientName);

        IFileTransferClient basicClient = createMock(IFileTransferClient.class);

        mockStatic(ClientFactory.class);
        expect(ClientFactory.getInstance()).andReturn(factoryMock);
        expect(factoryMock.getClient("HTTP", 80)).andReturn(basicClient);
        basicClient.connect(SAMPLE_HOST_NAME, SAMPLE_USER_NAME, SAMPLE_PASSWORD);
        replayAll();

        mockedTestObject = new FileTransferClient("HTTP", 80);
        mockedTestObject.connect(SAMPLE_HOST_NAME, SAMPLE_USER_NAME, SAMPLE_PASSWORD);

        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = FileTransferException.class)
    public void testConnectException() throws Exception {

        // setup expectations
        ftpMock.connect(SAMPLE_HOST_NAME, SAMPLE_USER_NAME, SAMPLE_PASSWORD);
        expectLastCall().andThrow(new FileTransferException(SAMPLE_EXCEPTION_MESSAGE)).times(5);
        replay(ftpMock);

        // execute operations
        mockedTestObject.connect(SAMPLE_HOST_NAME, SAMPLE_USER_NAME, SAMPLE_PASSWORD);

        // verify results
        verify(ftpMock);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = InvalidInputArgumentsException.class)
    public void testConnectWrongHostName() throws Exception {

        // execute operations
        testObject.connect(null, SAMPLE_USER_NAME, SAMPLE_PASSWORD);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = InvalidInputArgumentsException.class)
    public void testConnectWrongUserName() throws Exception {

        // execute operations
        testObject.connect(SAMPLE_HOST_NAME, null, SAMPLE_PASSWORD);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = InvalidInputArgumentsException.class)
    public void testConnectWrongPassword() throws Exception {

        // execute operations
        testObject.connect(SAMPLE_HOST_NAME, SAMPLE_USER_NAME, null);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = FileTransferException.class)
    public void testSecureConnect() throws Exception {

        // execute operations
        testObject.connect(SAMPLE_HOST_NAME, SAMPLE_USER_NAME, SAMPLE_PASSWORD, SAMPLE_KEY_ALIAS);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = InvalidInputArgumentsException.class)
    public void testSecureConnectWrongHostName() throws Exception {

        // execute operations
        testObject.connect(null, SAMPLE_USER_NAME, SAMPLE_PASSWORD, " ");
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = InvalidInputArgumentsException.class)
    public void testSecureConnectWrongUser() throws Exception {

        // execute operations
        testObject.connect(SAMPLE_HOST_NAME, null, SAMPLE_PASSWORD, " ");
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = InvalidInputArgumentsException.class)
    public void testSecureConnectWrongPassword() throws Exception {

        // execute operations
        testObject.connect(SAMPLE_HOST_NAME, SAMPLE_USER_NAME, null, " ");
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = InvalidInputArgumentsException.class)
    public void testSecureConnectWrongKey() throws Exception {

        // execute operations
        testObject.connect(SAMPLE_HOST_NAME, SAMPLE_USER_NAME, SAMPLE_PASSWORD, null);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testDisconnect() throws Exception {

        // setup expectations
        ftpMock.disconnect();
        replay(ftpMock);

        // execute operations
        mockedTestObject.disconnect();

        // verify results
        verify(ftpMock);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = FileTransferException.class)
    public void testDisconnectNegative() throws Exception {

        // setup expectations
        ftpMock.disconnect();
        expectLastCall().andThrow(new FileTransferException("Exception"));

        replay(ftpMock);

        // execute operations
        mockedTestObject.disconnect();

        // verify results
        verify(ftpMock);
    }

    // ---------------------------------------------------------------------
    // --------------------------- TEST DOWNLOAD ---------------------------
    // ---------------------------------------------------------------------

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testFileDownload() throws Exception {

        // setup expectations
        ftpMock.downloadFile(SAMPLE_LOCAL_DIRECTORY + SAMPLE_REMOTE_FILE,
                             SAMPLE_REMOTE_DIRECTORY,
                             SAMPLE_REMOTE_FILE);
        replay(ftpMock);

        // execute operations
        mockedTestObject.downloadFile(SAMPLE_LOCAL_DIRECTORY, SAMPLE_REMOTE_DIRECTORY, SAMPLE_REMOTE_FILE);

        // verify results
        verify(ftpMock);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = FileTransferException.class)
    public void testFileDownloadException() throws Exception {

        // setup expectations
        ftpMock.downloadFile(SAMPLE_LOCAL_DIRECTORY + SAMPLE_REMOTE_FILE,
                             SAMPLE_REMOTE_DIRECTORY,
                             SAMPLE_REMOTE_FILE);
        expectLastCall().andThrow(new FileTransferException(SAMPLE_EXCEPTION_MESSAGE));
        replay(ftpMock);

        // execute operations
        mockedTestObject.downloadFile(SAMPLE_LOCAL_DIRECTORY, SAMPLE_REMOTE_DIRECTORY, SAMPLE_REMOTE_FILE);

        // verify results
        verify(ftpMock);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = InvalidInputArgumentsException.class)
    public void testFileDownloadWrongLocal() throws Exception {

        // execute operations
        testObject.downloadFile("", SAMPLE_REMOTE_DIRECTORY, SAMPLE_REMOTE_FILE);

    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = InvalidInputArgumentsException.class)
    public void testFileDownloadWrongRemoteDir() throws Exception {

        // execute operations
        testObject.downloadFile(SAMPLE_LOCAL_DIRECTORY, "", SAMPLE_REMOTE_FILE);

    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = InvalidInputArgumentsException.class)
    public void testFileDownloadWrongRemoteFile() throws Exception {

        // execute operations
        testObject.downloadFile(SAMPLE_LOCAL_DIRECTORY, SAMPLE_REMOTE_DIRECTORY, "");

    }

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testFileDownloadAlt() throws Exception {

        // setup expectations
        ftpMock.downloadFile(SAMPLE_LOCAL_DIRECTORY + SAMPLE_LOCAL_FILE,
                             SAMPLE_REMOTE_DIRECTORY,
                             SAMPLE_REMOTE_FILE);
        replay(ftpMock);

        // execute operations
        mockedTestObject.downloadFile(SAMPLE_LOCAL_FILE,
                                      SAMPLE_LOCAL_DIRECTORY,
                                      SAMPLE_REMOTE_DIRECTORY,
                                      SAMPLE_REMOTE_FILE);

        // verify results
        verify(ftpMock);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = FileTransferException.class)
    public void testFileDownloadAltException() throws Exception {

        // setup expectations
        ftpMock.downloadFile(SAMPLE_LOCAL_DIRECTORY + SAMPLE_LOCAL_FILE,
                             SAMPLE_REMOTE_DIRECTORY,
                             SAMPLE_REMOTE_FILE);
        expectLastCall().andThrow(new FileTransferException(SAMPLE_EXCEPTION_MESSAGE));
        replay(ftpMock);

        // execute operations
        mockedTestObject.downloadFile(SAMPLE_LOCAL_FILE,
                                      SAMPLE_LOCAL_DIRECTORY,
                                      SAMPLE_REMOTE_DIRECTORY,
                                      SAMPLE_REMOTE_FILE);

        // verify results
        verify(ftpMock);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = InvalidInputArgumentsException.class)
    public void testFileDownloadAltWrongLocalFile() throws Exception {

        // execute operations
        testObject.downloadFile("", SAMPLE_LOCAL_FILE, SAMPLE_REMOTE_DIRECTORY, SAMPLE_REMOTE_FILE);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = InvalidInputArgumentsException.class)
    public void testFileDownloadAltWronglocalDir() throws Exception {

        // execute operations
        testObject.downloadFile(SAMPLE_LOCAL_DIRECTORY, "", SAMPLE_REMOTE_DIRECTORY, SAMPLE_REMOTE_FILE);

    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = InvalidInputArgumentsException.class)
    public void testFileDownloadAltWrongRemoteFile() throws Exception {

        // execute operations
        testObject.downloadFile(SAMPLE_LOCAL_DIRECTORY, SAMPLE_LOCAL_FILE, "", SAMPLE_REMOTE_FILE);

    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = InvalidInputArgumentsException.class)
    public void testFileDownloadAltWrongRemoteDir() throws Exception {

        // execute operations
        testObject.downloadFile(SAMPLE_LOCAL_DIRECTORY, SAMPLE_LOCAL_FILE, SAMPLE_REMOTE_DIRECTORY, "");

    }

    // ---------------------------------------------------------------------
    // --------------------------- TEST UPLOAD -----------------------------
    // ---------------------------------------------------------------------

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testFileUploadAlt() throws Exception {

        // setup expectations
        File fileMock = createMockAndExpectNew(File.class, SAMPLE_LOCAL_DIRECTORY + SAMPLE_LOCAL_FILE);
        expect(fileMock.getName()).andReturn(SAMPLE_LOCAL_FILE);
        ftpMock.uploadFile(SAMPLE_LOCAL_DIRECTORY + SAMPLE_LOCAL_FILE,
                           SAMPLE_REMOTE_DIRECTORY,
                           SAMPLE_LOCAL_FILE);
        replay(ftpMock);
        replay(fileMock, File.class);

        // execute operations
        mockedTestObject.uploadFile(SAMPLE_LOCAL_DIRECTORY + SAMPLE_LOCAL_FILE, SAMPLE_REMOTE_DIRECTORY);

        // verify results
        verify(ftpMock);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = FileTransferException.class)
    public void testFileUploadAltException() throws Exception {

        // setup expectations
        File fileMock = createMockAndExpectNew(File.class, SAMPLE_LOCAL_DIRECTORY + SAMPLE_LOCAL_FILE);
        expect(fileMock.getName()).andReturn(SAMPLE_LOCAL_FILE);
        ftpMock.uploadFile(SAMPLE_LOCAL_DIRECTORY + SAMPLE_LOCAL_FILE,
                           SAMPLE_REMOTE_DIRECTORY,
                           SAMPLE_LOCAL_FILE);
        expectLastCall().andThrow(new FileTransferException(SAMPLE_EXCEPTION_MESSAGE));
        replayAll();

        // execute operations
        mockedTestObject.uploadFile(SAMPLE_LOCAL_DIRECTORY + SAMPLE_LOCAL_FILE, SAMPLE_REMOTE_DIRECTORY);

        // verify results
        verify(ftpMock);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testFileUpload() throws Exception {

        // setup expectations
        ftpMock.uploadFile(SAMPLE_LOCAL_DIRECTORY + SAMPLE_LOCAL_FILE,
                           SAMPLE_REMOTE_DIRECTORY,
                           SAMPLE_REMOTE_FILE);
        replay(ftpMock);

        // execute operations
        mockedTestObject.uploadFile(SAMPLE_LOCAL_DIRECTORY + SAMPLE_LOCAL_FILE,
                                    SAMPLE_REMOTE_DIRECTORY,
                                    SAMPLE_REMOTE_FILE);

        // verify results
        verify(ftpMock);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = InvalidInputArgumentsException.class)
    public void testFileUploadWronglocal() throws Exception {

        // execute operations
        testObject.uploadFile(null, SAMPLE_REMOTE_DIRECTORY, SAMPLE_REMOTE_FILE);

    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = InvalidInputArgumentsException.class)
    public void testFileUploadWrongRemoteFile() throws Exception {

        // execute operations
        testObject.uploadFile(SAMPLE_LOCAL_DIRECTORY + SAMPLE_LOCAL_FILE, null, SAMPLE_REMOTE_FILE);

    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = InvalidInputArgumentsException.class)
    public void testFileUploadWrongRemoteDir() throws Exception {

        // execute operations
        testObject.uploadFile(SAMPLE_LOCAL_DIRECTORY + SAMPLE_LOCAL_FILE, SAMPLE_REMOTE_DIRECTORY, null);

    }

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testStartUploadAndPause() throws Exception {

        // setup expectations
        ftpMock.startUploadAndPause(SAMPLE_LOCAL_DIRECTORY + SAMPLE_LOCAL_FILE,
                                    SAMPLE_REMOTE_DIRECTORY,
                                    SAMPLE_REMOTE_FILE);
        replay(ftpMock);

        // execute operations
        mockedTestObject.startUploadAndPause(SAMPLE_LOCAL_DIRECTORY + SAMPLE_LOCAL_FILE,
                                             SAMPLE_REMOTE_DIRECTORY,
                                             SAMPLE_REMOTE_FILE);

        // verify results
        verify(ftpMock);
    }

    @Test
    public void testStartUploadAndPauseAlt() throws Exception {

        // setup expectations
        ftpMock.startUploadAndPause(SAMPLE_LOCAL_DIRECTORY + SAMPLE_LOCAL_FILE,
                                    SAMPLE_REMOTE_DIRECTORY,
                                    SAMPLE_LOCAL_FILE);
        replay(ftpMock);

        // execute operations
        mockedTestObject.startUploadAndPause(SAMPLE_LOCAL_DIRECTORY + SAMPLE_LOCAL_FILE,
                                             SAMPLE_REMOTE_DIRECTORY);

        // verify results
        verify(ftpMock);
    }

    @Test( expected = FileTransferException.class)
    public void testStartUploadAndPauseAltException() throws Exception {

        // setup expectations
        File fileMock = createMockAndExpectNew(File.class, SAMPLE_LOCAL_DIRECTORY + SAMPLE_LOCAL_FILE);
        expect(fileMock.getName()).andReturn(SAMPLE_LOCAL_FILE);
        ftpMock.startUploadAndPause(SAMPLE_LOCAL_DIRECTORY + SAMPLE_LOCAL_FILE,
                                    SAMPLE_REMOTE_DIRECTORY,
                                    SAMPLE_LOCAL_FILE);
        expectLastCall().andThrow(new FileTransferException(SAMPLE_EXCEPTION_MESSAGE));
        replayAll();

        // execute operations
        mockedTestObject.startUploadAndPause(SAMPLE_LOCAL_DIRECTORY + SAMPLE_LOCAL_FILE,
                                             SAMPLE_REMOTE_DIRECTORY);

        // verify results
        verify(ftpMock);
    }

    @Test( expected = InvalidInputArgumentsException.class)
    public void testStartUploadAndPauseWronglocal() throws Exception {

        // execute operations
        testObject.startUploadAndPause(null, SAMPLE_REMOTE_DIRECTORY, SAMPLE_REMOTE_FILE);
    }

    @Test( expected = InvalidInputArgumentsException.class)
    public void testStartUploadAndPauseWrongRemoteFile() throws Exception {

        // execute operations
        testObject.startUploadAndPause(SAMPLE_LOCAL_DIRECTORY + SAMPLE_LOCAL_FILE,
                                       null,
                                       SAMPLE_REMOTE_FILE);
    }

    @Test( expected = InvalidInputArgumentsException.class)
    public void testStartUploadAndPauseWrongRemoteDir() throws Exception {

        // execute operations
        testObject.startUploadAndPause(SAMPLE_LOCAL_DIRECTORY + SAMPLE_LOCAL_FILE,
                                       SAMPLE_REMOTE_DIRECTORY,
                                       null);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testResumePausedTransfer() throws Exception {

        // setup expectations
        ftpMock.resumePausedTransfer();
        replay(ftpMock);

        // execute operations
        mockedTestObject.resumePausedTransfer();

        // verify results
        verify(ftpMock);
    }

    @Test( expected = FileTransferException.class)
    public void testResumePausedTransferException() throws Exception {

        // setup expectations
        ftpMock.resumePausedTransfer();
        expectLastCall().andThrow(new FileTransferException(SAMPLE_EXCEPTION_MESSAGE));
        replay(ftpMock);

        // execute operations
        mockedTestObject.resumePausedTransfer();

        // verify results
        verify(ftpMock);
    }

    // ---------------------------------------------------------------------
    // ----------------------------- TEST MODE -----------------------------
    // ---------------------------------------------------------------------

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testChangeMode() throws Exception {

        // setup expectations
        ftpMock.setTransferMode(TransferMode.ASCII);
        replay(ftpMock);

        // execute operations
        mockedTestObject.setTransferMode(TransferMode.ASCII);

        // verify results
        verify(ftpMock);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = FileTransferException.class)
    public void testChangeModeException() throws Exception {

        // setup expectations
        ftpMock.setTransferMode(TransferMode.ASCII);
        expectLastCall().andThrow(new FileTransferException(SAMPLE_EXCEPTION_MESSAGE));
        replay(ftpMock);

        // execute operations
        mockedTestObject.setTransferMode(TransferMode.ASCII);

        // verify results
        verify(ftpMock);
    }

    // ---------------------------------------------------------------------
    // ----------------------------- TEST PORT -----------------------------
    // ---------------------------------------------------------------------

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testChangePort() throws Exception {

        // setup expectations
        ftpMock.setCustomPort(SAMPLE_CUSTOM_PORT);
        replay(ftpMock);

        // execute operations
        mockedTestObject.setPort(SAMPLE_CUSTOM_PORT);

        // verify results
        verify(ftpMock);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = InvalidInputArgumentsException.class)
    public void testChangePortWrong() throws Exception {

        // execute operations
        testObject.setPort(-1);

    }

    @Test
    public void testGatherResponses() throws Exception {

        // setup expectations
        ftpMock.enableResponseCollection(true);
        replay(ftpMock);

        mockedTestObject.enableResponseCollection(true);

        // verify results
        verify(ftpMock);
    }

    @Test
    public void testGetResponses() throws Exception {

        // setup expectations
        expect(ftpMock.getResponses()).andReturn(new String[]{ "1" });
        replay(ftpMock);

        String[] responses = mockedTestObject.getResponses();

        Assert.assertEquals(1, responses.length);
        Assert.assertEquals("1", responses[0]);

        // verify results
        verify(ftpMock);
    }

    /**
     * COPIED from GenericTransferClient class
     */
    private static String normalizeDirUrl(
                                           String dir ) {

        String correctFileSeparator;
        String wrongFileSeparator;
        if (OperatingSystemType.getCurrentOsType().isUnix()) {
            correctFileSeparator = "/";
            wrongFileSeparator = "\\";
        } else {
            correctFileSeparator = "\\";
            wrongFileSeparator = "/";
        }
        dir = dir.replace(wrongFileSeparator, correctFileSeparator);
        if (!dir.endsWith(correctFileSeparator)) {
            dir = dir + correctFileSeparator;
        }
        return dir;
    }
}
