/*
 * Copyright 2017-2021 Axway Software
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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.axway.ats.common.filesystem.FileSystemOperationException;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.BaseTest;
import com.axway.ats.core.filesystem.exceptions.FileDoesNotExistException;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;

/**
 * Unit tests for the {@link LocalFileSystemOperations} class
 */
public class Test_LocalFileSystemOperationsRealFiles extends BaseTest {

    private static Logger        log                = LogManager.getLogger(Test_LocalFileSystemOperationsRealFiles.class);

    private static final Pattern longListingPattern = Pattern.compile(
                                                                      "[a-z\\-]{1}([rwxtTsS\\-]{9})[\\.\\+]?\\s+\\d+\\s+([^\\s]+)\\s+([^\\s]+)\\s+\\d+.*");
    private static File          file               = null;
    private OperatingSystemType  realOsType;

    /**
     * Setup method
     * @throws IOException
     */
    @Before
    public void setUp() throws IOException {

        realOsType = OperatingSystemType.getCurrentOsType();
        file = File.createTempFile("ats_temporary", ".tmp",
                                   new File(AtsSystemProperties.SYSTEM_USER_TEMP_DIR));
    }

    @After
    public void tearDown() {

        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    public void getFileUidPositive() throws Exception {

        if (realOsType.isUnix()) {
            String[] stats = getFileStats(file.getAbsolutePath(), true);

            LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
            assertEquals(Long.parseLong(stats[2]),
                         localFileSystemOperations.getFileUID(file.getPath()));
        } else {
            log.warn("Test 'getFileUidPositive' is unable to pass on Windows, so it will be skipped!");
        }
    }

    @Test
    public void setFileUidNegativeNoSuchFile() throws Exception {

        if (realOsType.isUnix()) {
            try {
                LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
                localFileSystemOperations.setFileUID("fakeFile.txt", 123);
            } catch (Exception e) {
                assertEquals(FileDoesNotExistException.class.getName(), e.getClass().getName());
            }
        } else {
            log.warn("Test 'setFileUidNegativeNoSuchFile' is unable to pass on Windows, so it will be skipped!");
        }
    }

    @Test
    public void setFilePermissionsNoSuchFile() throws Exception {

        if (realOsType.isUnix()) {
            try {
                LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
                localFileSystemOperations.setFilePermissions("fakeFile.txt", "511");
            } catch (Exception e) {
                assertEquals(FileDoesNotExistException.class.getName(), e.getClass().getName());
            }
        } else {
            log.warn("Test 'setFilePermissionsNoSuchFile' is unable to pass on Windows, so it will be skipped!");
        }
    }

    @Ignore
    // TODO this test can be run only with root privileges
    @Test( )
    public void setFileUidPositive() throws Exception {

        if (realOsType.isUnix()) {
            LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
            localFileSystemOperations.setFileUID(file.getPath(), 123);

            String[] stats = getFileStats(file.getAbsolutePath(), true);
            Assert.assertEquals(150, Long.parseLong(stats[2]));
        } else {
            log.warn("Test 'setFileUidPositive' is unable to pass on Windows, so it will be skipped!");
        }
    }

    @Test( )
    public void setFilePermissionsPositive() throws Exception {

        if (realOsType.isUnix()) {
            LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
            localFileSystemOperations.setFilePermissions(file.getPath(), "511");

            String[] stats = getFileStats(file.getAbsolutePath(), true);
            Assert.assertEquals("0511", convertToOctalPermissions(stats[0]));
        } else {
            log.warn("Test 'setFilePermissionsPositive' is unable to pass on Windows, so it will be skipped!");
        }
    }

    @Ignore
    // TODO this test can be run only with root privileges
    @Test( )
    public void setFileGidPositive() throws Exception {

        if (realOsType.isUnix()) {
            LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
            localFileSystemOperations.setFileGID(file.getPath(), 150);

            String[] stats = getFileStats(file.getAbsolutePath(), true);
            Assert.assertEquals(150, Long.parseLong(stats[1]));
        } else {
            log.warn("Test 'setFileGidPositive' is unable to pass on Windows, so it will be skipped!");
        }
    }

    @Test( )
    public void getFilePermissionsPositive() throws Exception {

        if (realOsType.isUnix()) {

            LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
            String[] stats = getFileStats(file.getAbsolutePath(), true);
            assertEquals(convertToOctalPermissions(stats[0]),
                         localFileSystemOperations.getFilePermissions(file.getPath()));
        } else {
            log.warn("Test 'getFilePermissionsPositive' is unable to pass on Windows, so it will be skipped!");
        }
    }

    @Test( )
    public void getFileGidPositive() throws Exception {

        if (realOsType.isUnix()) {

            String[] stats = getFileStats(file.getAbsolutePath(), true);
            LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
            assertEquals(Long.parseLong(stats[2]),
                         localFileSystemOperations.getFileGID(file.getPath()));
        } else {
            log.warn("Test 'getFileGidPositive' is unable to pass on Windows, so it will be skipped!");
        }
    }

    //@Test
    public void findFiles_byExtension_RegEx() throws IOException {

        File file1 = null;
        try {
            String filePrefix = "ats_temporary";
            String fileSuffixUnique = ".ats_tmp";
            file1 = File.createTempFile(filePrefix, fileSuffixUnique,
                                        new File(AtsSystemProperties.SYSTEM_USER_TEMP_DIR));
            LocalFileSystemOperations localFileSystemOperations = new LocalFileSystemOperations();
            String[] list = localFileSystemOperations.findFiles(AtsSystemProperties.SYSTEM_USER_TEMP_DIR,
                                                                ".*\\" + fileSuffixUnique /* "\\" escape the dot (.) before file extension */,
                                                                true, false, false);
            Assert.assertEquals("Only one file matched expected", 1, list.length);
            String fileMatchedFullPath = list[0];
            log.info("File found: " + fileMatchedFullPath); // Sample path: /tmp + / +  *ats_temporary* + _1234 + .ats_tmp
            if (OperatingSystemType.getCurrentOsType().isWindows() && AtsSystemProperties.SYSTEM_USER_TEMP_DIR.contains("~")) {
                // short DOS 8.3 format handling
                Assert.assertTrue(fileMatchedFullPath.startsWith(new File(AtsSystemProperties.SYSTEM_USER_TEMP_DIR).getCanonicalPath())); // /tmp
            } else {
                Assert.assertTrue(fileMatchedFullPath.startsWith(AtsSystemProperties.SYSTEM_USER_TEMP_DIR)); // /tmp
            }
            Assert.assertTrue(fileMatchedFullPath.contains(filePrefix));
            Assert.assertTrue(fileMatchedFullPath.endsWith(fileSuffixUnique));
        } finally {
            if (file1 != null && file1.exists()) {
                file1.delete();
            }
        }
    }

    private String[] getFileStats( String filename,
                                   boolean numericUidAndGid ) throws FileSystemOperationException,
                                                              IOException {

        filename = IoUtils.normalizeFilePath(filename, OperatingSystemType.getCurrentOsType());
        File file = new File(filename);
        String command = file.isDirectory()
                                            ? "ls -ld "
                                            : "ls -la ";
        if (numericUidAndGid) {
            command = command.trim() + "n ";
        }

        String[] commandTokens = new String[]{ "/bin/sh", "-c", command + "'" + filename + "' 2>&1" };
        String[] result = executeExternalProcess(commandTokens);

        String[] lines = result[0].split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.endsWith(filename) || line.contains(" " + filename + " ")) {
                Matcher m = longListingPattern.matcher(line);
                if (m.matches()) {
                    return new String[]{ m.group(1), m.group(2), m.group(3) };
                }
            }
        }

        throw new FileSystemOperationException("Could not get statistics for '" + filename + "' file"
                                               + "\nby running the following command: "
                                               + Arrays.toString(commandTokens)
                                               + "\nCould not parse the result form the 'ls' command! Result: \n"
                                               + result[0]);
    }

    private String[] executeExternalProcess( String[] command ) {

        String stdOut = "";
        String stdErr = "";
        String exitCode = "";
        try {
            // start the external process
            Process process = Runtime.getRuntime().exec(command);

            // read the external process output streams
            // reading both streams helps releasing OS resources
            stdOut = IoUtils.streamToString(process.getInputStream()).trim();
            stdErr = IoUtils.streamToString(process.getErrorStream()).trim();

            //process.getOutputStream().close();

            exitCode = String.valueOf(process.waitFor());
        } catch (Exception e) {
            StringBuilder err = new StringBuilder();
            err.append("Error executing command '");
            err.append(Arrays.toString(command));
            err.append("'");
            if (!StringUtils.isNullOrEmpty(stdOut)) {
                err.append("\nSTD OUT: ");
                err.append(stdOut);
            }
            if (!StringUtils.isNullOrEmpty(stdErr)) {
                err.append("\nSTD ERR: ");
                err.append(stdErr);
            }
            if (!StringUtils.isNullOrEmpty(exitCode)) {
                err.append("\nexit code: ");
                err.append(exitCode);
            }
            throw new FileSystemOperationException(err.toString(), e);
        }

        return new String[]{ stdOut, stdErr, exitCode };
    }

    /**
     *
     * @param permString permissions {@link String}
     * @return octal permissions {@link String}
     */
    private String convertToOctalPermissions( String permString ) {

        if (permString == null) {
            return null;
        }
        permString = permString.trim();
        Integer[] octets = new Integer[]{ 0, 0, 0, 0 };
        for (int idx = 0; idx < permString.length(); idx++) {
            int pos = 1; // owner
            if (idx >= 3 && idx < 6) { // group
                pos = 2;
            } else if (idx >= 6) { // others
                pos = 3;
            }
            char ch = permString.charAt(idx);
            if (ch == 'r') {
                octets[pos] += 4;
            } else if (ch == 'w') {
                octets[pos] += 2;
            } else if (ch == 'x' || ch == 't' || ch == 's') {
                octets[pos] += 1;
            }

            // building the first octet - Special modes (setuid, setgid and sticky bit)
            if (ch == 't' || ch == 'T') {
                octets[0] += 1;
            } else if (ch == 's' || ch == 'S') {
                if (pos == 1) {
                    octets[0] += 4;
                } else {
                    octets[0] += 2;
                }
            }
        }

        return String.valueOf(octets[0]) + String.valueOf(octets[1]) + String.valueOf(octets[2])
               + String.valueOf(octets[3]);
    }

}
