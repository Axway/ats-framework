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
package com.axway.ats.core.filesystem;

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectNew;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.axway.ats.common.filesystem.EndOfLineStyle;
import com.axway.ats.common.filesystem.FileSystemOperationException;
import com.axway.ats.common.filesystem.Md5SumMode;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.BaseTest;
import com.axway.ats.core.filesystem.exceptions.AttributeNotSupportedException;
import com.axway.ats.core.filesystem.exceptions.FileDoesNotExistException;
import com.axway.ats.core.filesystem.model.FileAttributes;

/**
 * Unit tests for the {@link LocalFileSystemOperations} class
 */
@RunWith( PowerMockRunner.class)
@PrepareForTest( { LocalFileSystemOperations.class, OperatingSystemType.class })
public class Test_LocalFileSystemOperations extends BaseTest {

    private static Logger             log                        = LogManager.getLogger(Test_LocalFileSystemOperations.class);

    private static final String       NEW_FILE_NAME              = "new.file";
    private static final String       NEW_FILE_NAME_INVALID      = "!@#$%/^&*()";
    private static final String       NEW_DIRECTORY_NAME         = "new.directory";
    private static final String       NEW_DIRECTORY_NAME_INVALID = "!@#$%/^&*";
    private static final String       NEW_DIRECTORY_NAME_2       = "new.directory2";
    private static final String       NEW_DIRECTORY_NAME_3       = null;
    private static File               file                       = null;
    private LocalFileSystemOperations testObject                 = null;

    private OperatingSystemType       realOsType;

    private File                      mockFile;
    private Runtime                   mockRuntime;
    private Process                   mockProcess;

    private ByteArrayInputStream      STD_OUT;
    private ByteArrayInputStream      STD_ERR;

    /**
     * Setup method
     * @throws IOException
     */
    @Before
    public void setUp() throws IOException {

        realOsType = OperatingSystemType.getCurrentOsType();
        mockStatic(Runtime.class);
        testObject = new LocalFileSystemOperations();
        mockFile = createMock(File.class);
        mockProcess = createMock(Process.class);
        mockRuntime = createMock(Runtime.class);
        mockStatic(OperatingSystemType.class);

        file = File.createTempFile("temporary", ".tmp",
                                   new File(AtsSystemProperties.SYSTEM_USER_TEMP_DIR));
        STD_OUT = new ByteArrayInputStream(new String("-r-x--x--x 2 123 150 80 May 20 14:56 "
                                                      + file.getPath()).getBytes());
        STD_ERR = new ByteArrayInputStream(new String("-r-x--x--x 2 123 150 80 May 20 14:56 "
                                                      + file.getPath()).getBytes());
    }

    @After
    public void tearDown() {

        if (file.exists()) {
            file.delete();
        }
    }

    // --------------------------- TEST CREATE FILE ---------------------------
    @Test
    public void testCreateFilePositiveNormalContent() throws IOException {

        testObject.createFile(file.getPath(), 25, false);

        assertTrue(file.exists());
        assertEquals(25L, file.length());

        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

        assertEquals("0", bufferedReader.readLine());
        assertTrue(bufferedReader.readLine().startsWith("123456789:;<=>?@ABCDEF"));

        bufferedReader.close();
    }

    @Test
    public void testCreateFilePositiveRandomContent() throws IOException {

        testObject.createFile(file.getPath(), 25, true);

        assertTrue(file.exists());
        assertEquals(25L, file.length());

        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

        assertEquals(1, bufferedReader.readLine().length());
        assertFalse(bufferedReader.readLine().startsWith("123456789:;<=>?@ABCDEF"));

        bufferedReader.close();
    }

    @Test
    public void testCreateFilePositiveEmptyFile() throws IOException {

        testObject.createFile(file.getPath(), 0, true);

        assertTrue(file.exists());
        assertEquals(0L, file.length());
    }

    @Test
    public void testCreateFilePositiveBigFile() throws IOException {

        testObject.createFile(file.getPath(), 12567, false);

        assertTrue(file.exists());
        assertEquals(12567L, file.length());
    }

    @Test
    public void testCreateFilePositiveEOLWindows() throws IOException {

        testObject.createFile(file.getPath(), 25L, false, EndOfLineStyle.WINDOWS);

        assertTrue(file.exists());
        assertEquals(25L, file.length());

        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

        int currentChar, previousChar = -1;
        while ( (currentChar = bufferedReader.read()) > -1) {
            if (currentChar == '\n') {
                assertTrue('\r' == previousChar);
                break;
            }
            previousChar = currentChar;
        }

        bufferedReader.close();
    }

    @Test
    public void testCreateFilePositiveEOLUnix() throws IOException {

        testObject.createFile(file.getPath(), 25L, false, EndOfLineStyle.UNIX);

        assertTrue(file.exists());
        assertEquals(25L, file.length());

        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

        int currentChar, previousChar = -1;
        while ( (currentChar = bufferedReader.read()) > -1) {
            if (currentChar == '\n') {
                assertFalse('\r' == previousChar);
                break;
            }
            previousChar = currentChar;
        }

        bufferedReader.close();
    }

    @Test
    public void testCreateFilePositiveWithUidAndGid() throws Exception {

        expect(OperatingSystemType.getCurrentOsType()).andReturn(OperatingSystemType.LINUX);
        expect(Runtime.getRuntime()).andReturn(mockRuntime);
        String[] cmdCommand = new String[]{ "/bin/sh", "-c", "chown 120:230 '" + file.getPath() + "'" };
        expect(mockRuntime.exec(aryEq(cmdCommand))).andReturn(mockProcess);
        expect(mockProcess.getInputStream()).andReturn(STD_OUT);
        expect(mockProcess.getErrorStream()).andReturn(STD_ERR);
        expect(mockProcess.waitFor()).andReturn(0);

        //FIXME:
        //        expect( mockProcess.getInputStream() ).andReturn( new ByteArrayInputStream( new String( "drwxr-xrw- 2 120 230 80 May 20 14:56 "
        //                                                                                                + file.getPath() ).getBytes() ) );

        replayAll();

        testObject.createFile(file.getPath(), 25, 120, 230, false);

        // verify results
        verifyAll();

        assertTrue(file.exists());
        assertEquals(25L, file.length());

        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

        assertEquals("0", bufferedReader.readLine());
        assertTrue(bufferedReader.readLine().startsWith("123456789:;<=>?@ABCDEF"));

        bufferedReader.close();
    }

    @Test
    public void testCreateFilePositiveWithUidAndGidOnWindows() throws Exception {

        expect(OperatingSystemType.getCurrentOsType()).andReturn(OperatingSystemType.WINDOWS);

        replayAll();

        testObject.createFile(file.getPath(), 25, 120, 230, false);

        // verify results
        verifyAll();

        assertTrue(file.exists());
        assertEquals(25L, file.length());

        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

        assertEquals("0", bufferedReader.readLine());
        assertTrue(bufferedReader.readLine().startsWith("123456789:;<=>?@ABCDEF"));

        bufferedReader.close();
    }

    @Test( expected = FileSystemOperationException.class)
    public void testCreateFileWithUidAndGidNegative() throws Exception {

        expect(OperatingSystemType.getCurrentOsType()).andReturn(OperatingSystemType.LINUX);
        expect(Runtime.getRuntime()).andReturn(mockRuntime);
        String[] cmdCommand = new String[]{ "/bin/sh", "-c", "chown 120:230 '" + file.getPath() + "'" };
        expect(mockRuntime.exec(aryEq(cmdCommand))).andThrow(new IOException());

        replayAll();

        testObject.createFile(file.getPath(), 25, 120, 230, false);

        // verify results
        verifyAll();
    }

    // --------------------------- TEST CREATE BINARY FILE ---------------------------
    @Test
    public void createBinaryFilePositiveNormalContent() throws IOException {

        testObject.createBinaryFile(file.getPath(), 10, false);

        assertTrue(file.exists());
        assertEquals(10L, file.length());

        byte[] actualBytes = new byte[10];
        byte[] expectedBytes = new byte[10];
        byte nextByte = Byte.MIN_VALUE;
        for (int i = 0; i < expectedBytes.length; i++) {
            expectedBytes[i] = nextByte++;
        }
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        bis.read(actualBytes);
        assertTrue(Arrays.equals(expectedBytes, actualBytes));
        bis.close();
    }

    @Test
    public void createBinaryFilePositiveRandomContent() throws IOException {

        //Replace the randomGenerator with cutsom one.
        Random random = new Random(1);
        Random originalRandom = (Random) Whitebox.getInternalState(testObject.getClass(),
                                                                   "randomGenerator");
        Whitebox.setInternalState(testObject.getClass(), "randomGenerator", random);

        testObject.createBinaryFile(file.getPath(), 10, true);

        //Restore original random generator
        Whitebox.setInternalState(testObject.getClass(), "randomGenerator", originalRandom);

        assertTrue(file.exists());
        assertEquals(10L, file.length());

        byte[] expectedBytes = new byte[]{ 115, -40, 111, -110, -4, -16, -27, -35, -43, 104 };
        byte[] actualBytes = new byte[10];
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        bis.read(actualBytes);
        assertTrue(Arrays.equals(expectedBytes, actualBytes));
        bis.close();

    }

    @Test
    public void createBinaryFilePositiveEmptyFile() throws IOException {

        if (file.exists()) {
            file.delete();
        }

        testObject.createBinaryFile(file.getPath(), 0, true);

        assertTrue(file.exists());
        assertEquals(0L, file.length());
    }

    @Test
    public void createBinaryFilePositiveBigFile() throws IOException {

        testObject.createBinaryFile(file.getPath(), 12567, false);

        assertTrue(file.exists());
        assertEquals(12567L, file.length());
    }

    @Test
    public void createBinaryFilePositiveWithUidAndGid() throws Exception {

        expect(OperatingSystemType.getCurrentOsType()).andReturn(OperatingSystemType.LINUX);
        expect(Runtime.getRuntime()).andReturn(mockRuntime);
        String[] cmdCommand = new String[]{ "/bin/sh", "-c", "chown 120:230 '" + file.getPath() + "'" };
        expect(mockRuntime.exec(aryEq(cmdCommand))).andReturn(mockProcess);
        expect(mockProcess.getInputStream()).andReturn(STD_OUT);
        expect(mockProcess.getErrorStream()).andReturn(STD_ERR);
        expect(mockProcess.waitFor()).andReturn(0);

        replayAll();

        testObject.createBinaryFile(file.getPath(), 18, 120, 230, false);

        // verify results
        verifyAll();

        assertTrue(file.exists());
        assertEquals(18L, file.length());

        byte[] actualBytes = new byte[18];
        byte[] expectedBytes = new byte[18];
        byte nextByte = Byte.MIN_VALUE;
        for (int i = 0; i < expectedBytes.length; i++) {
            expectedBytes[i] = nextByte++;
        }
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        bis.read(actualBytes);
        assertTrue(Arrays.equals(expectedBytes, actualBytes));
        bis.close();
    }

    @Test
    public void createBinaryFilePositiveWithUidAndGidOnWindows() throws Exception {

        expect(OperatingSystemType.getCurrentOsType()).andReturn(OperatingSystemType.WINDOWS);

        replayAll();

        testObject.createBinaryFile(file.getPath(), 18, 120, 230, false);

        // verify results
        verifyAll();

        assertTrue(file.exists());
        assertEquals(18L, file.length());

        byte[] actualBytes = new byte[18];
        byte[] expectedBytes = new byte[18];
        byte nextByte = Byte.MIN_VALUE;
        for (int i = 0; i < expectedBytes.length; i++) {
            expectedBytes[i] = nextByte++;
        }
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        bis.read(actualBytes);
        assertTrue(Arrays.equals(expectedBytes, actualBytes));
        bis.close();
    }

    @Test( expected = FileSystemOperationException.class)
    public void createBinaryFileWithUidAndGidNegative() throws Exception {

        expect(OperatingSystemType.getCurrentOsType()).andReturn(OperatingSystemType.LINUX);
        expect(Runtime.getRuntime()).andReturn(mockRuntime);
        String[] cmdCommand = new String[]{ "/bin/sh", "-c", "chown 120:230 '" + file.getPath() + "'" };
        expect(mockRuntime.exec(aryEq(cmdCommand))).andThrow(new IOException());

        replayAll();

        testObject.createBinaryFile(file.getPath(), 25, 120, 230, false);

        // verify results
        verifyAll();
    }

    @Test( expected = FileSystemOperationException.class)
    public void createBinaryFileNegativeInvalidFile() throws IOException {

        testObject.createBinaryFile(NEW_FILE_NAME_INVALID, 10, false);
    }

    // --------------------------- TEST COPY FILE ---------------------------

    @Test
    public void testCopyFile() throws Exception {

        String newFileName = file.getParent() + File.separator + NEW_FILE_NAME;
        File newFile = new File(newFileName);

        try {
            testObject.copyFile(file.getAbsolutePath(), newFileName, true);

            Assert.assertTrue(newFile.exists());
            Assert.assertTrue(newFile.isFile());
        } finally {
            newFile.delete();
        }
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = FileSystemOperationException.class)
    public void testCopyFileNegativeWrongFileName() throws Exception {

        String newFileName = file.getParent() + File.separator + NEW_FILE_NAME_INVALID;

        testObject.copyFile(file.getAbsolutePath(), newFileName, true);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = FileDoesNotExistException.class)
    public void testCopyFileNegativeNoSuchFile() throws Exception {

        String newFileName = file.getPath() + NEW_FILE_NAME_INVALID;
        testObject.copyFile(newFileName, newFileName, true);

    }

    // --------------------------- TEST CREATE DIRECTORY---------------------------

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testCreateDirectory() throws Exception {

        String newDirectoryPath = file.getParent() + File.separator + NEW_DIRECTORY_NAME;
        File newFile = new File(newDirectoryPath);

        try {
            testObject.createDirectory(newDirectoryPath);

            Assert.assertTrue(newFile.exists());
            Assert.assertTrue(newFile.isDirectory());
        } finally {
            newFile.delete();
        }
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = FileSystemOperationException.class)
    public void testCreateDirectoryNegativeWrongName() throws Exception {

        String newDirectoryPath = file.getPath() + File.separator + NEW_DIRECTORY_NAME_INVALID;

        testObject.createDirectory(newDirectoryPath);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testCreateDirectoryAlreadyExists() throws Exception {

        String newDirectoryPath = file.getParent() + File.separator + NEW_DIRECTORY_NAME;
        File newFile = new File(newDirectoryPath);

        try {
            testObject.createDirectory(newDirectoryPath);
            testObject.createDirectory(newDirectoryPath);
        } finally {
            newFile.delete();
        }
    }

    @Test
    public void testCreateDirectoryPositiveWithUidAndGid() throws Exception {

        String newDirectoryPath = file.getParent() + File.separator + NEW_DIRECTORY_NAME;
        File newDirectory = new File(newDirectoryPath);

        try {
            expect(OperatingSystemType.getCurrentOsType()).andReturn(OperatingSystemType.LINUX);
            expect(Runtime.getRuntime()).andReturn(mockRuntime);
            String[] cmdCommand = new String[]{ "/bin/sh", "-c",
                                                "chown 120:230 '" + newDirectory.getPath() + "'" };
            expect(mockRuntime.exec(aryEq(cmdCommand))).andReturn(mockProcess);
            expect(mockProcess.getInputStream()).andReturn(STD_OUT);
            expect(mockProcess.getErrorStream()).andReturn(STD_ERR);
            expect(mockProcess.waitFor()).andReturn(0);

            replayAll();

            testObject.createDirectory(newDirectory.getPath(), 120, 230);

            // verify results
            verifyAll();

            assertTrue(newDirectory.exists());
            assertTrue(newDirectory.isDirectory());
        } finally {
            newDirectory.delete();
        }
    }

    @Test
    public void testCreateDirectoryPositiveWithUidAndGidOnWindows() throws Exception {

        String newDirectoryPath = file.getParent() + File.separator + NEW_DIRECTORY_NAME;
        File newDirectory = new File(newDirectoryPath);

        try {
            expect(OperatingSystemType.getCurrentOsType()).andReturn(OperatingSystemType.WINDOWS);

            replayAll();

            testObject.createDirectory(newDirectory.getPath(), 120, 230);

            // verify results
            verifyAll();

            assertTrue(newDirectory.exists());
            assertTrue(newDirectory.isDirectory());
        } finally {
            newDirectory.delete();
        }
    }

    @Test( expected = FileSystemOperationException.class)
    public void testCreateDirectoryWithUidAndGidNegative() throws Exception {

        String newDirectoryPath = file.getParent() + File.separator + NEW_DIRECTORY_NAME;
        File newDirectory = new File(newDirectoryPath);

        try {
            expect(OperatingSystemType.getCurrentOsType()).andReturn(OperatingSystemType.LINUX);
            expect(Runtime.getRuntime()).andReturn(mockRuntime);
            String[] cmdCommand = new String[]{ "/bin/sh", "-c",
                                                "chown 120:230 '" + newDirectory.getPath() + "'" };
            expect(mockRuntime.exec(aryEq(cmdCommand))).andThrow(new IOException());

            replayAll();

            testObject.createDirectory(newDirectory.getPath(), 120, 230);

            // verify results
            verifyAll();
        } finally {
            newDirectory.delete();
        }
    }

    // --------------------------- TEST DELETE DIRECTORY---------------------------

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testDeleteDirectory() throws Exception {

        String newDirectoryPath = file.getParent() + File.separator + NEW_DIRECTORY_NAME_2;
        File newFile = new File(newDirectoryPath);

        try {
            testObject.createDirectory(newDirectoryPath);
            testObject.deleteDirectory(newDirectoryPath, false);

            Assert.assertFalse(newFile.exists());
        } finally {
            newFile.delete();
        }
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testDeleteDirectoryRecursive() throws Exception {

        String newDirectoryPath = file.getParent() + File.separator + NEW_DIRECTORY_NAME_2;
        String newSubDirectoryPath = newDirectoryPath + File.separator + NEW_DIRECTORY_NAME_3;
        String newFileName = newSubDirectoryPath + File.separator + NEW_DIRECTORY_NAME_3;

        File newFile = new File(newDirectoryPath);

        try {
            // creates a directory with one subdirectory and a file inside the subdirectory
            testObject.createDirectory(newDirectoryPath);
            testObject.createDirectory(newSubDirectoryPath);
            testObject.createFile(newFileName, 10, false);

            testObject.deleteDirectory(newDirectoryPath, true);

            Assert.assertFalse(newFile.exists());
        } finally {
            newFile.delete();
        }
    }

    /**
     * Test case
     */
    @Test
    public void testDeleteDirectoryInvalidName() throws Exception {

        String newDirectoryPath = file.getPath() + File.separator + NEW_DIRECTORY_NAME_INVALID;

        testObject.deleteDirectory(newDirectoryPath, false);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testDeleteDirectoryNoSuch() throws Exception {

        String newDirectoryPath = file.getPath() + File.separator + NEW_DIRECTORY_NAME;

        testObject.deleteDirectory(newDirectoryPath, false);
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testPurgeDirectoryDoesNotExist() throws Exception {

        String newDirectoryPath = file.getParent() + File.separator + NEW_DIRECTORY_NAME;

        expectNew(File.class, newDirectoryPath).andReturn(mockFile);
        expect(mockFile.exists()).andReturn(false);
        expect(mockFile.isDirectory()).andReturn(true);

        replayAll();

        testObject.purgeDirectoryContents(newDirectoryPath);

        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = FileSystemOperationException.class)
    public void testPurgeDirectoryNotADirectory() throws Exception {

        String newDirectoryPath = file.getParent() + File.separator + NEW_DIRECTORY_NAME;

        expectNew(File.class, newDirectoryPath).andReturn(mockFile);
        expect(mockFile.exists()).andReturn(true);
        expect(mockFile.isDirectory()).andReturn(false);

        replayAll();

        testObject.purgeDirectoryContents(newDirectoryPath);

        verifyAll();
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = FileSystemOperationException.class)
    public void testPurgeDirectoryIOException() throws Exception {

        File innnerFile = createMock(File.class);

        String newDirectoryPath = file.getParent() + File.separator + NEW_DIRECTORY_NAME;

        expectNew(File.class, newDirectoryPath).andReturn(mockFile);
        expect(mockFile.exists()).andReturn(true);
        expect(mockFile.isDirectory()).andReturn(true);
        expect(mockFile.toPath()).andReturn(null);
        expect(innnerFile.isDirectory()).andReturn(false);
        expect(innnerFile.delete()).andReturn(false);
        replayAll();
        EasyMock.createNiceControl();

        testObject.purgeDirectoryContents(newDirectoryPath);

        verifyAll();
    }

    // --------------------------- TEST DELETE FILE ---------------------------

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testDeleteFile() throws Exception {

        File newFile = new File(file.getAbsolutePath());

        try {
            testObject.deleteFile(file.getAbsolutePath());

            Assert.assertFalse(newFile.exists());
        } finally {
            newFile.delete();
        }
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testDeleteFileNoSuch() throws Exception {

        testObject.deleteFile(file.getPath() + NEW_FILE_NAME);

    }

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testDeleteFileInvalidName() throws Exception {

        testObject.deleteFile(file.getPath() + NEW_FILE_NAME_INVALID);

    }

    // --------------------------- TEST MOVE FILE ---------------------------

    /**
     * Test case
     * @throws Exception
     */
    @Test
    public void testRenameFile() throws Exception {

        String newFileName = file.getParent() + File.separator + NEW_FILE_NAME;
        File newFile = new File(newFileName);

        try {
            testObject.renameFile(file.getAbsolutePath(), newFileName, true);

            Assert.assertFalse(file.exists());
            Assert.assertTrue(newFile.exists());
            Assert.assertTrue(newFile.isFile());
        } finally {
            newFile.delete();
        }
    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = FileDoesNotExistException.class)
    public void testRenameFileNegativeNoSuch() throws Exception {

        testObject.renameFile(file.getPath() + NEW_FILE_NAME, file.getPath(), true);

    }

    /**
     * Test case
     * @throws Exception
     */
    @Test( expected = FileSystemOperationException.class)
    public void testRenameFileNegativeInvalidName() throws Exception {

        testObject.renameFile(file.getPath(), file.getPath() + NEW_FILE_NAME_INVALID, true);

    }

    @Test( )
    public void doesFileExistPositive() throws Exception {

        URL testFile = Test_LocalFileSystemOperations.class.getResource("TestFile1.txt");
        assertTrue(testObject.doesFileExist(testFile.getFile()));

        assertFalse(testObject.doesFileExist("dasdasd"));
    }

    @Test( expected = AttributeNotSupportedException.class)
    public void getFilePermissionsNegativeWindows() throws Exception {

        expect(OperatingSystemType.getCurrentOsType()).andReturn(OperatingSystemType.WINDOWS);

        replayAll();

        LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
        localFileSystemOperations.getFilePermissions(file.getPath());

        // verify results
        verifyAll();
    }

    @Test( expected = FileDoesNotExistException.class)
    public void getFilePermissionsNegativeNoSuchFile() throws Exception {

        LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
        localFileSystemOperations.getFilePermissions("dasdasd");
    }

    @Test( expected = AttributeNotSupportedException.class)
    public void setFilePermissionsNegativeWindows() throws Exception {

        if (realOsType.isWindows()) {
            expect(OperatingSystemType.getCurrentOsType()).andReturn(OperatingSystemType.WINDOWS);

            replayAll();

            LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
            localFileSystemOperations.setFilePermissions(file.getPath(), "777");

            // verify results
            verifyAll();
        } else {
            log.warn("Test 'getFileUidPositive' is unable to pass on Windows, so it will be skipped!");
            throw new AttributeNotSupportedException(FileAttributes.PERMISSIONS, realOsType);
        }
    }

    @Test( expected = AttributeNotSupportedException.class)
    public void getFileUidNegativeWindows() throws Exception {

        expect(OperatingSystemType.getCurrentOsType()).andReturn(OperatingSystemType.WINDOWS);

        replayAll();

        LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
        localFileSystemOperations.getFileUID(file.getPath());

        // verify results
        verifyAll();
    }

    @Test( expected = FileDoesNotExistException.class)
    public void getFileUidNegativeNoSuchFile() throws Exception {

        LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
        localFileSystemOperations.getFileUID("dasdasd");
    }

    @Test( expected = FileDoesNotExistException.class)
    public void setFileUidNegativeNoSuchFile() throws Exception {

        LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
        localFileSystemOperations.setFileUID("dasdasd", 123);
    }

    @Test( expected = AttributeNotSupportedException.class)
    public void setFileUidNegativeWindows() throws Exception {

        expect(OperatingSystemType.getCurrentOsType()).andReturn(OperatingSystemType.WINDOWS);

        replayAll();

        LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
        localFileSystemOperations.setFileUID(file.getPath(), 134);

        // verify results
        verifyAll();
    }

    @Test( expected = AttributeNotSupportedException.class)
    public void getFileGidNegativeWindows() throws Exception {

        expect(OperatingSystemType.getCurrentOsType()).andReturn(OperatingSystemType.WINDOWS);

        replayAll();

        LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
        localFileSystemOperations.getFileGID(file.getPath());

        // verify results
        verifyAll();
    }

    @Test( expected = FileDoesNotExistException.class)
    public void getFileGidNegativeNoSuchFile() throws Exception {

        LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
        localFileSystemOperations.getFileGID("dasdasd");
    }

    @Test( expected = AttributeNotSupportedException.class)
    public void setFileGidNegativeWindows() throws Exception {

        expect(OperatingSystemType.getCurrentOsType()).andReturn(OperatingSystemType.WINDOWS);

        replayAll();

        LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
        localFileSystemOperations.setFileGID(file.getPath(), 134);

        // verify results
        verifyAll();
    }

    @Test( expected = FileDoesNotExistException.class)
    public void setFileGidNegativeNoSuchFile() throws Exception {

        LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
        localFileSystemOperations.setFileGID("dasdasd", 123);
    }

    @Test( )
    public void getFileModificationTimePositive() throws Exception {

        expect(OperatingSystemType.getCurrentOsType()).andReturn(OperatingSystemType.LINUX);
        expectNew(File.class, file.getPath()).andReturn(mockFile);
        expect(mockFile.exists()).andReturn(true);
        expect(mockFile.lastModified()).andReturn(123L);

        replayAll();

        LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
        assertEquals(123L, localFileSystemOperations.getFileModificationTime(file.getPath()));

        // verify results
        verifyAll();
    }

    @Test( expected = FileDoesNotExistException.class)
    public void getFileModificationTimeNegativeNoSuchFile() throws Exception {

        LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
        localFileSystemOperations.getFileModificationTime("dasdasd");
    }

    @Test( )
    public void setFileModificationTimePositive() throws Exception {

        expect(OperatingSystemType.getCurrentOsType()).andReturn(OperatingSystemType.LINUX);
        expectNew(File.class, file.getPath()).andReturn(mockFile);
        expect(mockFile.exists()).andReturn(true);
        expect(mockFile.setLastModified(123L)).andReturn(true);

        replayAll();

        LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
        localFileSystemOperations.setFileModificationTime(file.getPath(), 123L);

        // verify results
        verifyAll();
    }

    @Test( expected = FileSystemOperationException.class)
    public void setFileModificationTimeNegative() throws Exception {

        expect(OperatingSystemType.getCurrentOsType()).andReturn(OperatingSystemType.LINUX);
        expectNew(File.class, file.getPath()).andReturn(mockFile);
        expect(mockFile.exists()).andReturn(true);
        expect(mockFile.setLastModified(123L)).andReturn(false);

        replayAll();

        LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
        localFileSystemOperations.setFileModificationTime(file.getPath(), 123L);

        // verify results
        verifyAll();
    }

    @Test( expected = FileDoesNotExistException.class)
    public void setFileModificationTimeNegativeNoSuchFile() throws Exception {

        LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
        localFileSystemOperations.setFileModificationTime("dasdasd", 123);
    }

    @Test( )
    public void getFileSizePositive() throws Exception {

        String testFile = Test_LocalFileSystemOperations.class.getResource("TestFile1.txt").getFile();

        LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
        assertEquals(38L, localFileSystemOperations.getFileSize(testFile));
    }

    @Test( expected = FileDoesNotExistException.class)
    public void getFileSizeNegativeNoSuchFile() throws Exception {

        LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
        localFileSystemOperations.getFileSize("dasdasd");
    }

    @Test( )
    public void computeMd5SumBinaryPositive() {

        String testFile = Test_LocalFileSystemOperations.class.getResource("TestFile1.txt").getFile();

        LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
        assertEquals("0566bd3b54cbc776d08329006520ad8b",
                     localFileSystemOperations.computeMd5Sum(testFile, Md5SumMode.BINARY));
    }

    @Test( )
    public void computeMd5SumAsciiPositive() {

        String testFile = Test_LocalFileSystemOperations.class.getResource("TestFile1Unix.txt").getFile();

        //if ASCII mode is used the file with unix style line endings should have the same
        //md5 as the file with windows style line endings
        LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
        assertEquals("0566bd3b54cbc776d08329006520ad8b",
                     localFileSystemOperations.computeMd5Sum(testFile, Md5SumMode.ASCII));
    }

    @Test( expected = FileSystemOperationException.class)
    public void computeMd5SumNegativeNoSuchFile() {

        LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
        localFileSystemOperations.computeMd5Sum("dasdasd", Md5SumMode.ASCII);
    }
}
