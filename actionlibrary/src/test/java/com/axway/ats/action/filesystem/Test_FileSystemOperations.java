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
package com.axway.ats.action.filesystem;

import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.expectNew;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.MockRepository;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.axway.ats.action.BaseTest;
import com.axway.ats.action.filesystem.FileSystemOperations;
import com.axway.ats.action.filesystem.RemoteFileSystemOperations;
import com.axway.ats.common.filesystem.EndOfLineStyle;
import com.axway.ats.common.filesystem.FileSystemOperationException;
import com.axway.ats.common.filesystem.Md5SumMode;
import com.axway.ats.core.filesystem.LocalFileSystemOperations;
import com.axway.ats.core.validation.exceptions.InvalidInputArgumentsException;

/**
 * Unit tests for the {@link FileSystemOperations} class
 */
@RunWith( PowerMockRunner.class)
@PrepareForTest( { FileSystemOperations.class,
                   LocalFileSystemOperations.class,
                   RemoteFileSystemOperations.class })
@PowerMockIgnore( "javax.net.*")
public class Test_FileSystemOperations extends BaseTest {

    private static final String        DESTINATION_FILE_NAME_VALID    = "destination.file";
    private static final String        SOURCE_FILE_NAME_VALID         = "source.file";
    private static final String        DESTINATION_FILE_NAME_INVALID  = "";
    private static final String        SOURCE_FILE_NAME_INVALID       = null;
    private static final String        REMOTE_HOST_NAME_VALID         = "some.remote.host.name:8080";
    private static final String        REMOTE_HOST_NAME_INVALID       = "!@#$%^&*";
    private static final String        ANOTHER_REMOTE_HOST_NAME_VALID = "another.remote.host.name:8080";
    private static final String        SOURCE_HOST_NAME_VALID         = "127.0.0.1:8080";
    private static final String        SOURCE_HOST_NAME_INVALID       = "!@#$%^&*";
    private static final String        SOURCE_DIRECTORY_NAME_VALID    = "/valid/directory/name";
    private static final String        SOURCE_DIRECTORY_NAME_INVALID  = "";
    private static final long          FILE_SIZE_VALID                = 100;
    private static final int           FILE_UID_VALID                 = 1001;
    private static final int           FILE_GID_VALID                 = 1001;
    private static final long          FILE_SIZE_INVALID              = -1;
    private static final int           FILE_UID_INVALID               = -235;
    private static final int           FILE_GID_INVALID               = -3;
    private FileSystemOperations       fileSystemOperationsLocal      = null;
    private FileSystemOperations       fileSystemOperationsRemote     = null;
    private LocalFileSystemOperations  localFSOperationsMock;
    private RemoteFileSystemOperations remoteFSOperationsMock;

    /**
     * Setup method
     * @throws Exception 
     */
    @Before
    public void setUpTest_FileSystemOperations() throws Exception {

        fileSystemOperationsLocal = new FileSystemOperations();
        fileSystemOperationsLocal.createFile(SOURCE_FILE_NAME_VALID, "");
        fileSystemOperationsLocal.createFile(DESTINATION_FILE_NAME_VALID, "");
        fileSystemOperationsRemote = new FileSystemOperations(REMOTE_HOST_NAME_VALID);
        localFSOperationsMock = createMock(LocalFileSystemOperations.class);
        remoteFSOperationsMock = createMock(RemoteFileSystemOperations.class);
    }

    @After
    public void setDownTest_FileSystemOperations() throws Exception {

        // we have to clean the mock repository, otherwise this method fails 
        // mock check see it as another class invocation and throws an exception
        MockRepository.remove(LocalFileSystemOperations.class);

        fileSystemOperationsLocal.deleteFile(SOURCE_FILE_NAME_VALID);
        fileSystemOperationsLocal.deleteFile(DESTINATION_FILE_NAME_VALID);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testConstructorWithValidRemoteHost() throws Exception {

        new FileSystemOperations(REMOTE_HOST_NAME_VALID);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void testConstructorWithInvalidRemoteHost() throws Exception {

        new FileSystemOperations(REMOTE_HOST_NAME_INVALID);
    }

    // -----------------------------------------------------------------------------
    // ------------------------------ TEST COPY FILE -------------------------------
    // -----------------------------------------------------------------------------

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testFileCopy() throws Exception {

        // setup expectations
        expectNew(LocalFileSystemOperations.class).andReturn(localFSOperationsMock);
        localFSOperationsMock.copyFile(SOURCE_FILE_NAME_VALID, DESTINATION_FILE_NAME_VALID, true);

        replayAll();

        // execute operation
        fileSystemOperationsLocal.copyFileTo(SOURCE_FILE_NAME_VALID, DESTINATION_FILE_NAME_VALID);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void testFileCopyNegativeWrongDestination() throws Exception {

        // execute operation
        fileSystemOperationsLocal.copyFileTo(SOURCE_FILE_NAME_VALID, DESTINATION_FILE_NAME_INVALID);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void testFileCopyNegativeWrongSource() throws Exception {

        // execute operation
        fileSystemOperationsLocal.copyFileTo(SOURCE_FILE_NAME_INVALID, DESTINATION_FILE_NAME_VALID);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testFileCopyRemote() throws Exception {

        // setup expectations
        expectNew(RemoteFileSystemOperations.class, REMOTE_HOST_NAME_VALID).andReturn(remoteFSOperationsMock);
        remoteFSOperationsMock.copyFile(SOURCE_FILE_NAME_VALID, DESTINATION_FILE_NAME_VALID, true);

        replayAll();

        // execute operation
        fileSystemOperationsRemote.copyFileTo(SOURCE_FILE_NAME_VALID, DESTINATION_FILE_NAME_VALID);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testFileCopyBothRemote() throws Exception {

        // setup expectations
        expectNew(RemoteFileSystemOperations.class, ANOTHER_REMOTE_HOST_NAME_VALID).andReturn(remoteFSOperationsMock);
        expectNew(RemoteFileSystemOperations.class, REMOTE_HOST_NAME_VALID).andReturn(remoteFSOperationsMock);
        remoteFSOperationsMock.copyFileTo(SOURCE_FILE_NAME_VALID, ANOTHER_REMOTE_HOST_NAME_VALID,
                                          DESTINATION_FILE_NAME_VALID, true);

        replayAll();

        // execute operation
        fileSystemOperationsRemote.copyRemoteFile(REMOTE_HOST_NAME_VALID,
                                                  SOURCE_FILE_NAME_VALID,
                                                  ANOTHER_REMOTE_HOST_NAME_VALID,
                                                  DESTINATION_FILE_NAME_VALID);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void testFileCopyBothRemoteNegativeWrongDestination() throws Exception {

        // execute operation
        fileSystemOperationsLocal.copyRemoteFile(SOURCE_HOST_NAME_VALID,
                                                 SOURCE_FILE_NAME_VALID,
                                                 REMOTE_HOST_NAME_INVALID,
                                                 DESTINATION_FILE_NAME_VALID);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void testFileCopyBothRemoteNegativeWrongSourceHost() throws Exception {

        // execute operation
        fileSystemOperationsLocal.copyRemoteFile(SOURCE_HOST_NAME_INVALID,
                                                 SOURCE_FILE_NAME_VALID,
                                                 REMOTE_HOST_NAME_VALID,
                                                 DESTINATION_FILE_NAME_INVALID);
    }

    // -----------------------------------------------------------------------------
    // ------------------------------ TEST DELETE FILE -----------------------------
    // -----------------------------------------------------------------------------

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testFileDeleteFile() throws Exception {

        // setup expectations
        expectNew(LocalFileSystemOperations.class).andReturn(localFSOperationsMock);
        localFSOperationsMock.deleteFile(SOURCE_FILE_NAME_VALID);

        replayAll();

        // execute operation
        fileSystemOperationsLocal.deleteFile(SOURCE_FILE_NAME_VALID);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void testFileDeleteFileNegative() throws Exception {

        // execute operation
        fileSystemOperationsLocal.deleteFile(SOURCE_FILE_NAME_INVALID);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testFileDeleteFileRemote() throws Exception {

        // setup expectations
        expectNew(RemoteFileSystemOperations.class, REMOTE_HOST_NAME_VALID).andReturn(remoteFSOperationsMock);
        remoteFSOperationsMock.deleteFile(DESTINATION_FILE_NAME_VALID);

        replayAll();

        // execute operation
        fileSystemOperationsRemote.deleteFile(DESTINATION_FILE_NAME_VALID);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void testFileDeleteFileRemoteNegativeInvalidFile() throws Exception {

        // execute operation
        fileSystemOperationsRemote.deleteFile(SOURCE_FILE_NAME_INVALID);
    }

    // -----------------------------------------------------------------------------
    // -------------------------- TEST CREATE DIRECTORY ----------------------------
    // -----------------------------------------------------------------------------

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testCreateDirectory() throws Exception {

        // setup expectations
        expectNew(LocalFileSystemOperations.class).andReturn(localFSOperationsMock);
        localFSOperationsMock.createDirectory(SOURCE_DIRECTORY_NAME_VALID);

        replayAll();

        // execute operation
        fileSystemOperationsLocal.createDirectory(SOURCE_DIRECTORY_NAME_VALID);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void testCreateDirectoryNegative() throws Exception {

        // execute operation
        fileSystemOperationsLocal.createDirectory(SOURCE_DIRECTORY_NAME_INVALID);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testCreateRemoteDirectory() throws Exception {

        // setup expectations
        expectNew(RemoteFileSystemOperations.class, REMOTE_HOST_NAME_VALID).andReturn(remoteFSOperationsMock);
        remoteFSOperationsMock.createDirectory(SOURCE_DIRECTORY_NAME_VALID);

        replayAll();

        // execute operation
        fileSystemOperationsRemote.createDirectory(SOURCE_DIRECTORY_NAME_VALID);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void testCreateRemoteNegativeInvalidDirectory() throws Exception {

        // execute operation
        fileSystemOperationsRemote.createDirectory(SOURCE_DIRECTORY_NAME_INVALID);
    }

    // -----------------------------------------------------------------------------
    // ------------------ TEST CREATE DIRECTORY WITH UID AND GID -------------------
    // -----------------------------------------------------------------------------

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testCreateDirectoryWithUidAndGid() throws Exception {

        // setup expectations
        expectNew(LocalFileSystemOperations.class).andReturn(localFSOperationsMock);
        localFSOperationsMock.createDirectory(SOURCE_DIRECTORY_NAME_VALID, 10, 20);

        replayAll();

        // execute operation
        fileSystemOperationsLocal.createDirectory(SOURCE_DIRECTORY_NAME_VALID, 10, 20);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void testCreateNegativeWithUidAndGid() throws Exception {

        // execute operation
        fileSystemOperationsLocal.createDirectory(SOURCE_DIRECTORY_NAME_INVALID, 10, 20);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testCreateRemoteWithUidAndGid() throws Exception {

        // setup expectations
        expectNew(RemoteFileSystemOperations.class, REMOTE_HOST_NAME_VALID).andReturn(remoteFSOperationsMock);
        remoteFSOperationsMock.createDirectory(SOURCE_DIRECTORY_NAME_VALID, 10, 20);

        replayAll();

        // execute operation
        fileSystemOperationsRemote.createDirectory(SOURCE_DIRECTORY_NAME_VALID, 10, 20);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void testCreateDirectoryWithUidAndGidNegativeInvalidName() throws Exception {

        // execute operation
        fileSystemOperationsLocal.createDirectory(SOURCE_DIRECTORY_NAME_INVALID, 10, 20);
    }

    // -----------------------------------------------------------------------------
    // --------------------------- TEST DELETE DIRECTORY ---------------------------
    // -----------------------------------------------------------------------------

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testDeleteDirectory() throws Exception {

        // setup expectations
        expectNew(LocalFileSystemOperations.class).andReturn(localFSOperationsMock);
        localFSOperationsMock.deleteDirectory(SOURCE_DIRECTORY_NAME_VALID, true);

        replayAll();

        // execute operation
        fileSystemOperationsLocal.deleteDirectory(SOURCE_DIRECTORY_NAME_VALID);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void testDeleteDirectoryNegative() throws Exception {

        // execute operation
        fileSystemOperationsLocal.deleteDirectory(SOURCE_DIRECTORY_NAME_INVALID);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testDeleteDirectoryRemote() throws Exception {

        // setup expectations
        expectNew(RemoteFileSystemOperations.class, REMOTE_HOST_NAME_VALID).andReturn(remoteFSOperationsMock);
        remoteFSOperationsMock.deleteDirectory(SOURCE_DIRECTORY_NAME_VALID, true);

        replayAll();

        // execute operation
        fileSystemOperationsRemote.deleteDirectory(SOURCE_DIRECTORY_NAME_VALID);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = FileSystemOperationException.class)
    public void testDeleteDirectoryException() throws Exception {

        // setup expectations
        expectNew(LocalFileSystemOperations.class).andReturn(localFSOperationsMock);
        localFSOperationsMock.deleteDirectory(SOURCE_DIRECTORY_NAME_VALID, true);
        expectLastCall().andThrow(new FileSystemOperationException("Test"));

        replayAll();

        // execute operation
        fileSystemOperationsLocal.deleteDirectory(SOURCE_DIRECTORY_NAME_VALID);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = FileSystemOperationException.class)
    public void testDeleteDirectoryRemoteException() throws Exception {

        // setup expectations
        expectNew(RemoteFileSystemOperations.class, REMOTE_HOST_NAME_VALID).andReturn(remoteFSOperationsMock);
        remoteFSOperationsMock.deleteDirectory(SOURCE_DIRECTORY_NAME_VALID, true);

        expectLastCall().andThrow(new FileSystemOperationException("Test"));

        replayAll();

        // execute operation
        fileSystemOperationsRemote.deleteDirectory(SOURCE_DIRECTORY_NAME_VALID);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void testDeleteDirectoryRemoteNegativeInvalidDirectory() throws Exception {

        // execute operation
        fileSystemOperationsRemote.deleteDirectory(SOURCE_DIRECTORY_NAME_INVALID);
    }

    // -----------------------------------------------------------------------------
    // --------------------------- TEST PURGE DIRECTORY ---------------------------
    // -----------------------------------------------------------------------------

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testPurgeDirectory() throws Exception {

        // setup expectations
        expectNew(LocalFileSystemOperations.class).andReturn(localFSOperationsMock);
        localFSOperationsMock.purgeDirectoryContents(SOURCE_DIRECTORY_NAME_VALID);

        replayAll();

        // execute operation
        fileSystemOperationsLocal.deleteDirectoryContent(SOURCE_DIRECTORY_NAME_VALID);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void testPurgeDirectoryNegative() throws Exception {

        // execute operation
        fileSystemOperationsLocal.deleteDirectoryContent(SOURCE_DIRECTORY_NAME_INVALID);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testPurgeDirectoryRemote() throws Exception {

        // setup expectations
        expectNew(RemoteFileSystemOperations.class, REMOTE_HOST_NAME_VALID).andReturn(remoteFSOperationsMock);
        remoteFSOperationsMock.purgeDirectoryContents(SOURCE_DIRECTORY_NAME_VALID);

        replayAll();

        // execute operation
        fileSystemOperationsRemote.deleteDirectoryContent(SOURCE_DIRECTORY_NAME_VALID);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = FileSystemOperationException.class)
    public void testPurgeDirectoryException() throws Exception {

        // setup expectations
        expectNew(LocalFileSystemOperations.class).andReturn(localFSOperationsMock);
        localFSOperationsMock.purgeDirectoryContents(SOURCE_DIRECTORY_NAME_VALID);
        expectLastCall().andThrow(new FileSystemOperationException("Test"));

        replayAll();

        // execute operation
        fileSystemOperationsLocal.deleteDirectoryContent(SOURCE_DIRECTORY_NAME_VALID);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = FileSystemOperationException.class)
    public void testPurgeDirectoryRemoteException() throws Exception {

        // setup expectations
        expectNew(RemoteFileSystemOperations.class, REMOTE_HOST_NAME_VALID).andReturn(remoteFSOperationsMock);
        remoteFSOperationsMock.purgeDirectoryContents(SOURCE_DIRECTORY_NAME_VALID);
        expectLastCall().andThrow(new FileSystemOperationException("Test"));

        replayAll();

        // execute operation
        fileSystemOperationsRemote.deleteDirectoryContent(SOURCE_DIRECTORY_NAME_VALID);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void testPurgeDirectoryRemoteNegativeInvalidDirectory() throws Exception {

        // execute operation
        fileSystemOperationsRemote.deleteDirectoryContent(SOURCE_DIRECTORY_NAME_INVALID);
    }

    // -----------------------------------------------------------------------------
    // ------------------------------ TEST CREATE FILE -----------------------------
    // -----------------------------------------------------------------------------

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testCreateFileRemote() throws Exception {

        // setup expectations
        expectNew(RemoteFileSystemOperations.class, REMOTE_HOST_NAME_VALID).andReturn(remoteFSOperationsMock);
        remoteFSOperationsMock.createFile(SOURCE_FILE_NAME_VALID,
                                          FILE_SIZE_VALID,
                                          FILE_UID_VALID,
                                          FILE_GID_VALID,
                                          true,
                                          null);

        replayAll();

        // execute operation
        fileSystemOperationsRemote.createFile(SOURCE_FILE_NAME_VALID,
                                              FILE_SIZE_VALID,
                                              FILE_UID_VALID,
                                              FILE_GID_VALID,
                                              null,
                                              true);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testCreateFileEOLWindows() throws Exception {

        // setup expectations
        expectNew(LocalFileSystemOperations.class).andReturn(localFSOperationsMock);
        localFSOperationsMock.createFile(SOURCE_FILE_NAME_VALID,
                                         FILE_SIZE_VALID,
                                         FILE_UID_VALID,
                                         FILE_GID_VALID,
                                         true,
                                         EndOfLineStyle.WINDOWS);

        replayAll();

        // execute operation
        fileSystemOperationsLocal.createFile(SOURCE_FILE_NAME_VALID,
                                             FILE_SIZE_VALID,
                                             FILE_UID_VALID,
                                             FILE_GID_VALID,
                                             EndOfLineStyle.WINDOWS,
                                             true);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testCreateFileEOLUnix() throws Exception {

        // setup expectations
        expectNew(LocalFileSystemOperations.class).andReturn(localFSOperationsMock);
        localFSOperationsMock.createFile(SOURCE_FILE_NAME_VALID,
                                         FILE_SIZE_VALID,
                                         FILE_UID_VALID,
                                         FILE_GID_VALID,
                                         true,
                                         EndOfLineStyle.UNIX);

        replayAll();

        // execute operation
        fileSystemOperationsLocal.createFile(SOURCE_FILE_NAME_VALID,
                                             FILE_SIZE_VALID,
                                             FILE_UID_VALID,
                                             FILE_GID_VALID,
                                             EndOfLineStyle.UNIX,
                                             true);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testCreateFileLocal() throws Exception {

        // setup expectations
        expectNew(LocalFileSystemOperations.class).andReturn(localFSOperationsMock);
        localFSOperationsMock.createFile(SOURCE_FILE_NAME_VALID,
                                         FILE_SIZE_VALID,
                                         FILE_UID_VALID,
                                         FILE_GID_VALID,
                                         true,
                                         null);

        replayAll();

        // execute operation
        fileSystemOperationsLocal.createFile(SOURCE_FILE_NAME_VALID,
                                             FILE_SIZE_VALID,
                                             FILE_UID_VALID,
                                             FILE_GID_VALID,
                                             null,
                                             true);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testCreateFileLocalAlt() throws Exception {

        // setup expectations
        expectNew(LocalFileSystemOperations.class).andReturn(localFSOperationsMock);
        localFSOperationsMock.createFile(SOURCE_FILE_NAME_VALID, FILE_SIZE_VALID, true, EndOfLineStyle.UNIX);

        replayAll();

        // execute operation
        fileSystemOperationsLocal.createFile(SOURCE_FILE_NAME_VALID,
                                             FILE_SIZE_VALID,
                                             EndOfLineStyle.UNIX,
                                             true);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = FileSystemOperationException.class)
    public void testCreateFileLocalAltNegativeException() throws Exception {

        // setup expectations
        expectNew(LocalFileSystemOperations.class).andReturn(localFSOperationsMock);
        localFSOperationsMock.createFile(SOURCE_FILE_NAME_VALID, FILE_SIZE_VALID, true, EndOfLineStyle.UNIX);
        expectLastCall().andThrow(new FileSystemOperationException("Test"));

        replayAll();

        // execute operation
        fileSystemOperationsLocal.createFile(SOURCE_FILE_NAME_VALID,
                                             FILE_SIZE_VALID,
                                             EndOfLineStyle.UNIX,
                                             true);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void testCreateFileLocalAltNegativeInvalidFileName() throws Exception {

        // execute operation
        fileSystemOperationsLocal.createFile(SOURCE_FILE_NAME_INVALID,
                                             FILE_SIZE_VALID,
                                             EndOfLineStyle.UNIX,
                                             true);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void testCreateFileLocalAltNegativeInvalidSize() throws Exception {

        // execute operation
        fileSystemOperationsLocal.createFile(SOURCE_FILE_NAME_VALID,
                                             FILE_SIZE_INVALID,
                                             EndOfLineStyle.UNIX,
                                             true);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void testCreateFileLocalNegativeInvalidFileName() throws Exception {

        // execute operation
        fileSystemOperationsLocal.createFile(SOURCE_FILE_NAME_INVALID,
                                             FILE_SIZE_VALID,
                                             FILE_UID_VALID,
                                             FILE_GID_VALID,
                                             true);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void testCreateFileLocalNegativeInvalidFileSize() throws Exception {

        // execute operation
        fileSystemOperationsLocal.createFile(SOURCE_FILE_NAME_VALID,
                                             FILE_SIZE_INVALID,
                                             FILE_UID_VALID,
                                             FILE_GID_VALID,
                                             true);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void testCreateFileLocalNegativeInvalidUID() throws Exception {

        // execute operation
        fileSystemOperationsLocal.createFile(SOURCE_FILE_NAME_VALID,
                                             FILE_SIZE_VALID,
                                             FILE_UID_INVALID,
                                             FILE_GID_VALID,
                                             true);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void testCreateFileLocalNegativeInvalidGID() throws Exception {

        // execute operation
        fileSystemOperationsLocal.createFile(SOURCE_FILE_NAME_VALID,
                                             FILE_SIZE_VALID,
                                             FILE_UID_VALID,
                                             FILE_GID_INVALID,
                                             true);
    }

    // -----------------------------------------------------------------------------
    // ------------------------- TEST CREATE BINARY FILE ---------------------------
    // -----------------------------------------------------------------------------

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void createBinaryFileRemote() throws Exception {

        // setup expectations
        expectNew(RemoteFileSystemOperations.class, REMOTE_HOST_NAME_VALID).andReturn(remoteFSOperationsMock);
        remoteFSOperationsMock.createBinaryFile(SOURCE_FILE_NAME_VALID,
                                                FILE_SIZE_VALID,
                                                FILE_UID_VALID,
                                                FILE_GID_VALID,
                                                true);

        replayAll();

        // execute operation
        fileSystemOperationsRemote.createBinaryFile(SOURCE_FILE_NAME_VALID,
                                                    FILE_SIZE_VALID,
                                                    FILE_UID_VALID,
                                                    FILE_GID_VALID,
                                                    true);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void createBinaryFileLocal() throws Exception {

        // setup expectations
        expectNew(LocalFileSystemOperations.class).andReturn(localFSOperationsMock);
        localFSOperationsMock.createBinaryFile(SOURCE_FILE_NAME_VALID,
                                               FILE_SIZE_VALID,
                                               FILE_UID_VALID,
                                               FILE_GID_VALID,
                                               true);

        replayAll();

        // execute operation
        fileSystemOperationsLocal.createBinaryFile(SOURCE_FILE_NAME_VALID,
                                                   FILE_SIZE_VALID,
                                                   FILE_UID_VALID,
                                                   FILE_GID_VALID,
                                                   true);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void createBinaryFileLocalNegativeInvalidFileName() throws Exception {

        // execute operation
        fileSystemOperationsLocal.createBinaryFile(SOURCE_FILE_NAME_INVALID,
                                                   FILE_SIZE_VALID,
                                                   FILE_UID_VALID,
                                                   FILE_GID_VALID,
                                                   true);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void createBinaryFileLocalNegativeInvalidFileSize() throws Exception {

        // execute operation
        fileSystemOperationsLocal.createBinaryFile(SOURCE_FILE_NAME_VALID,
                                                   FILE_SIZE_INVALID,
                                                   FILE_UID_VALID,
                                                   FILE_GID_VALID,
                                                   true);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void createBinaryFileLocalNegativeInvalidUID() throws Exception {

        // execute operation
        fileSystemOperationsLocal.createBinaryFile(SOURCE_FILE_NAME_VALID,
                                                   FILE_SIZE_VALID,
                                                   FILE_UID_INVALID,
                                                   FILE_GID_VALID,
                                                   true);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void createBinaryFileLocalNegativeInvalidGID() throws Exception {

        // execute operation
        fileSystemOperationsLocal.createBinaryFile(SOURCE_FILE_NAME_VALID,
                                                   FILE_SIZE_VALID,
                                                   FILE_UID_VALID,
                                                   FILE_GID_INVALID,
                                                   true);
    }

    // -----------------------------------------------------------------------------
    // --------------------------- TEST COMPUTE MD5 SUM ----------------------------
    // -----------------------------------------------------------------------------

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void computeMD5PositiveRemote() throws Exception {

        String expected = "MD5@#@!$%$@#%@#@#!";

        // setup expectations
        expectNew(RemoteFileSystemOperations.class, REMOTE_HOST_NAME_VALID).andReturn(remoteFSOperationsMock);
        expect(remoteFSOperationsMock.computeMd5Sum(SOURCE_FILE_NAME_VALID, Md5SumMode.BINARY)).andReturn(expected);

        replayAll();

        // execute operation
        String result = fileSystemOperationsRemote.computeMd5Sum(SOURCE_FILE_NAME_VALID, Md5SumMode.BINARY);

        // verify results
        Assert.assertEquals(expected, result);
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = FileSystemOperationException.class)
    public void computeMD5NegativeExceptionRemote() throws Exception {

        // setup expectations
        expectNew(RemoteFileSystemOperations.class, REMOTE_HOST_NAME_VALID).andReturn(remoteFSOperationsMock);
        expect(remoteFSOperationsMock.computeMd5Sum(SOURCE_FILE_NAME_VALID,
                                                    Md5SumMode.BINARY)).andThrow(new FileSystemOperationException("Test"));

        replayAll();

        // execute operation
        fileSystemOperationsRemote.computeMd5Sum(SOURCE_FILE_NAME_VALID, Md5SumMode.BINARY);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void computeMD5NegativeWrongFile() throws Exception {

        // execute operation
        fileSystemOperationsLocal.computeMd5Sum(SOURCE_FILE_NAME_INVALID, Md5SumMode.BINARY);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void computeMD5PositiveLocal() throws Exception {

        String expected = "MD5@#@!$%$@#%@#@#!";

        // setup expectations
        expectNew(LocalFileSystemOperations.class).andReturn(localFSOperationsMock);
        expect(localFSOperationsMock.computeMd5Sum(SOURCE_FILE_NAME_VALID, Md5SumMode.BINARY)).andReturn(expected);

        replayAll();

        // execute operation
        String result = fileSystemOperationsLocal.computeMd5Sum(SOURCE_FILE_NAME_VALID, Md5SumMode.BINARY);

        // verify results
        Assert.assertEquals(expected, result);
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = FileSystemOperationException.class)
    public void computeMD5NegativeExceptionLocal() throws Exception {

        // setup expectations
        expectNew(LocalFileSystemOperations.class).andReturn(localFSOperationsMock);
        expect(localFSOperationsMock.computeMd5Sum(SOURCE_FILE_NAME_VALID,
                                                   Md5SumMode.BINARY)).andThrow(new FileSystemOperationException("Test"));

        replayAll();

        // execute operation
        fileSystemOperationsLocal.computeMd5Sum(SOURCE_FILE_NAME_VALID, Md5SumMode.BINARY);

        // verify results
        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test(
            expected = InvalidInputArgumentsException.class)
    public void computeMD5NegativeWrongFileLocal() throws Exception {

        // execute operation
        fileSystemOperationsLocal.computeMd5Sum(SOURCE_FILE_NAME_INVALID, Md5SumMode.BINARY);
    }
}
