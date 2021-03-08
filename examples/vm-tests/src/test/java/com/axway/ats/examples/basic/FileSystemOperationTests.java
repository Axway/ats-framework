/*
 * Copyright 2018-2021 Axway Software
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

import static org.testng.Assert.assertEquals;

import static org.testng.Assert.assertFalse;

import static org.testng.Assert.assertTrue;

import java.util.Arrays;

import org.testng.annotations.BeforeMethod;

import org.testng.annotations.Test;

import com.axway.ats.action.filesystem.FileSystemOperations;

import com.axway.ats.common.filesystem.FileMatchInfo;

import com.axway.ats.common.filesystem.Md5SumMode;

import com.axway.ats.core.utils.IoUtils;

import com.axway.ats.examples.common.BaseTestClass;

/**

 * Here we show different examples on a file system

 *

 * File system operations are introduced at: 

 *      https://axway.github.io/ats-framework/File-System-Operations.html

 */

public class FileSystemOperationTests extends BaseTestClass {

    // The ATS entry point for taking different steps on the file system

    private FileSystemOperations fileOperations;

    private static final String  ROOT_DIR;

    private static final String  testDir1;

    private static final String  testDir2;

    private static final String  testFile1Name = "test_file1.txt";

    private static final String  testFile1;

    static {

        // Note: the IoUtils methods are not a public. They may get changed at any moment without notice

        ROOT_DIR = IoUtils.normalizeDirPath(userTempDir) + "root_dir/";

        testDir1 = IoUtils.normalizeDirPath(ROOT_DIR + "test_dir1/");

        testDir2 = IoUtils.normalizeDirPath(ROOT_DIR + "test_dir2/");

        testFile1 = IoUtils.normalizeFilePath(testDir1 + testFile1Name);

    }

    public FileSystemOperationTests() {

        // Make an instance to work with.

        // You can pass a host/IP in the constructor when want to work on a remote host.

        fileOperations = new FileSystemOperations();

    }

    /**
    
     * Prior to each test we make sure we have the folder we worked with
    
     * in same state
    
     */

    @BeforeMethod

    public void beforeMethod() {

        // create the dir for all tests(if not present)

        fileOperations.createDirectory(ROOT_DIR);

        // cleanup the content of the tests dir (if has any)

        fileOperations.deleteDirectoryContent(ROOT_DIR);

        // create some test file and folders

        fileOperations.createDirectory(testDir1);

        fileOperations.createDirectory(testDir2);

        fileOperations.createFile(testFile1, "file with some lines\nline 2  \n line 3\n");

    }

    @Test

    public void createDirectoryAndFiles() {

        // create a folder to work into

        String newDir = ROOT_DIR + "new_dir/";

        fileOperations.createDirectory(newDir);

        // check whether some file exists

        assertFalse(fileOperations.doesFileExist(newDir + "file1.txt"));

        fileOperations.createFile(newDir + "file1.txt",

                                  "This file's content is provided from the test code");

        assertTrue(fileOperations.doesFileExist(newDir + "file1.txt"));

        // create an empty file

        fileOperations.createFile(newDir + "file2_empty.txt", 0, true);

        // create a file with random content with total length of 100 bytes

        fileOperations.createFile(newDir + "file3__random_chars.txt", 100, true);

        // create a file which contain a sequence of ASCII characters starting with '0' and ending with 'z'

        // this will be repeated as long as needed according to the provided file size

        fileOperations.createFile(newDir + "file4_ordered_chars.txt", 100, false);

        // create a file which does not restrict what kind of characters to be present

        // all possible bytes from -127 to 128 will be used

        fileOperations.createBinaryFile(newDir + "file5_binary.txt", 100, false);

        // delete a file

        fileOperations.deleteFile(newDir + "file5_binary.txt");

        assertFalse(fileOperations.doesFileExist(newDir + "file5_binary.txt"));

    }

    @Test

    public void copyFileAndCheckIt() {

        // copy file to another folder

        fileOperations.copyFileTo(testFile1, testDir2 + testFile1Name);

        // verify both files have same MD5 sum

        // MD5 is calculated byte by byte

        String firstMd5 = fileOperations.computeMd5Sum(testFile1, Md5SumMode.BINARY);

        String secondMd5 = fileOperations.computeMd5Sum(testDir2 + testFile1Name, Md5SumMode.BINARY);

        assertEquals(firstMd5, secondMd5);

        // Change the new line character in one of the files.

        // Verify again the MD5s but do not care about exact new line character.

        // This is useful when comparing same text file on 2 file systems which use a different native new line character

        fileOperations.replaceTextInFile(testFile1, "\n", "\r", false);

        String firstMd5Loose = fileOperations.computeMd5Sum(testFile1, Md5SumMode.ASCII);

        String secondMd5Loose = fileOperations.computeMd5Sum(testDir2 + testFile1Name, Md5SumMode.ASCII);

        assertEquals(firstMd5Loose, secondMd5Loose);

    }

    @Test

    public void findFilesOnFileSystem() {

        // create a folder

        String newDir = ROOT_DIR + "new_find_files_dir/";

        fileOperations.createDirectory(newDir);

        String subDir = newDir + "sub_dir/";

        fileOperations.createDirectory(subDir);

        String file1 = newDir + "file1_empty.txt";

        fileOperations.createFile(file1, 100, false);

        String file2 = newDir + "file2_empty.xml";

        fileOperations.createFile(file2, 100, false);

        String file3 = subDir + "file3_empty.txt";

        fileOperations.createFile(file3, 100, false);

        // get a list of all TXT files from the specified folder and its sub-folders

        String[] foundFiles = fileOperations.findFiles(newDir, ".*.txt", true, false, true);

        String[] expectedFiles = new String[]{
                                               IoUtils.normalizeFilePath(file1),
                                               IoUtils.normalizeFilePath(file3),
        };

        for (String expectedFile : expectedFiles) {
            boolean found = false;
            for (String foundFile : foundFiles) {
                if (expectedFile.equals(foundFile)) {
                    found = true;
                }
            }

            if (!found) {
                throw new RuntimeException("File '" + expectedFile + "' was not found. All found files are: "
                                           + Arrays.toString(foundFiles));
            }
        }

        // get a list of all XML files from the specified folder and its sub-folders

        foundFiles = fileOperations.findFiles(newDir, ".*.xml", true, false, true);

        assertEquals(IoUtils.normalizeFilePath(foundFiles[0]), IoUtils.normalizeFilePath(file2));

    }

    @Test

    public void extractFileContent() {

        // Get the last lines of some file.

        // For example, this can be used for log files

        String[] last2Lines = fileOperations.getLastLinesFromFile(testFile1, 2);

        assertEquals(last2Lines[0].trim(), "line 2");

        assertEquals(last2Lines[1].trim(), "line 3");

        assertEquals(last2Lines.length, 2);

        // get all lines from the file which contain some given text

        String[] matchedLines = fileOperations.fileGrep(testFile1, ".*some.*", false);

        assertEquals(matchedLines[0].trim(), "file with some lines");

        assertEquals(matchedLines.length, 1);

        // Load the whole file content into a String

        // This should not be done with large files

        String content = fileOperations.readFile(testFile1, "UTF-8");

        log.info("Loaded the whole file content:\n" + content);

    }

    /**
    
     * This test shows how to extract ONLY NEW INFO from a text file.
    
     * On each call, we will remember the last file position we worked with, so 
    
     * we will not give you back old content.
    
     *
    
     * This is good for growing log files. 
    
     * On each execution, you will get only the new lines since the last execution. 
    
     */

    @Test

    public void tailFileContent() {

        // some stuff for a growing file

        String[] poem = new String[]{ "Fear No More, By William Shakespeare", "",

                                      "Fear no more the heat o' the sun;", "Nor the furious winter's rages,",

                                      "Thou thy worldly task hast done,",

                                      "Home art gone, and ta'en thy wages;",

                                      "Golden lads and girls all must,",

                                      "As chimney sweepers come to dust." };

        // create the file to work with

        String file = ROOT_DIR + "file_to_tail.txt";

        fileOperations.createFile(file, "");

        // add some lines to the file

        fileOperations.appendToFile(file, poem[0] + "\n");

        fileOperations.appendToFile(file, poem[1] + "\n");

        fileOperations.appendToFile(file, poem[2] + "\n");

        fileOperations.appendToFile(file, poem[3] + "\n");

        fileOperations.appendToFile(file, poem[4] + "\n");

        // 1. on the first call, all available lines will be returned

        FileMatchInfo matchInfo = fileOperations.findNewTextInFile(file, ".*", true);

        assertEquals(matchInfo.lines.length, 5);

        assertEquals(matchInfo.lines[0], poem[0]);

        assertEquals(matchInfo.lines[1], poem[1]);

        assertEquals(matchInfo.lines[2], poem[2]);

        assertEquals(matchInfo.lines[3], poem[3]);

        assertEquals(matchInfo.lines[4], poem[4]);

        // 2. check for new content when there isn't such

        matchInfo = fileOperations.findNewTextInFile(file, ".*", true);

        assertEquals(matchInfo.lines.length, 0);

        // 3. let the file grow, check the new content only

        fileOperations.appendToFile(file, poem[5] + "\n");

        fileOperations.appendToFile(file, poem[6] + "\n");

        fileOperations.appendToFile(file, poem[7] + "\n");

        matchInfo = fileOperations.findNewTextInFile(file, ".*", true);

        assertEquals(matchInfo.lines.length, 3);

        assertEquals(matchInfo.lines[0], poem[5]);

        assertEquals(matchInfo.lines[1], poem[6]);

        assertEquals(matchInfo.lines[2], poem[7]);

    }

}
