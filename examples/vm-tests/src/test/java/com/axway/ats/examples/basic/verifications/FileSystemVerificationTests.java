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
package com.axway.ats.examples.basic.verifications;

import com.axway.ats.action.filesystem.FileSystemOperations;
import com.axway.ats.common.filesystem.Md5SumMode;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.examples.common.BaseTestClass;
import com.axway.ats.rbv.clients.FileSystemVerification;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * File system verifications are used to verify some file/folder is present on some target host and
 * it matches some needed attributes like content, size, MD5 sum, UIG etc
 *
 * Basic ATS verifications functionality is introduced at:
 *      https://axway.github.io/ats-framework/Common-test-verifications.html
 *
 * File system verifications are particularly introduced at:
 *      https://axway.github.io/ats-framework/File-system-verifications.html
 *
 *
 * In reality this class is often used to verify a transferred file
 * is available at its target location and it has all the needed attributes as expected.
 *
 * For example, while transferring a file:
 *  - first the file gets created, so we verify this fact
 *  - second the file gets filled with all its bytes. While this is going on, our check
 *          on file expected size or MD5 sum will keep failing
 *  - finally the file is all present and all needed checks pass as expected

 */

public class FileSystemVerificationTests extends BaseTestClass {

    private static final String ROOT_DIR;

    private static final String testDir1;

    private static final String testDir2;

    private static final String testFile1Name = "test_file1.txt";

    private static final String testFile1;

    static {

        // Note: the IoUtils methods are not a public. They may get changed at any moment without notice

        ROOT_DIR = IoUtils.normalizeDirPath(userTempDir) + "root_dir/";

        testDir1 = IoUtils.normalizeDirPath(ROOT_DIR + "test_dir1/");

        testDir2 = IoUtils.normalizeDirPath(ROOT_DIR + "test_dir2/");

        testFile1 = IoUtils.normalizeFilePath(testDir1 + testFile1Name);

    }

    /**
     * Prior to each test we make sure we have the folder we worked with
     * in same state
    
     */

    @BeforeMethod
    public void beforeMethod() {

        FileSystemOperations fileOperations = new FileSystemOperations();

        // create the dir for all tests(if not present)

        fileOperations.createDirectory(ROOT_DIR);

        // cleanup the content of the tests dir (if has any)

        fileOperations.deleteDirectoryContent(ROOT_DIR);

        // create some test file and folders

        fileOperations.createDirectory(testDir1);

        fileOperations.createDirectory(testDir2);

        fileOperations.createFile(testFile1, "file with some lines\nline 2\n line 3\n");

    }

    /**
     * The verification will success on the very first try as the file exists
    
     */

    @Test
    public void verifyAnExistingFile() {

        // just verify there is a file with this name and path

        // without paying attention on anything else like size, content, attributes ...

        FileSystemVerification fsv = new FileSystemVerification(testFile1);

        fsv.verifyFileExists();

    }

    /**
     * The verification will success on the very first try as the directory exists
    
     */

    @Test
    public void verifyAnExistingDirectory() {

        // just verify there is a directory with this path

        FileSystemVerification fsv = new FileSystemVerification(testDir1);

        fsv.verifyFolderExists();

    }

    /**
     * The verification will success on the very first try as the file exists
     * and has all attributes as specified.
     *
     * In reality you will probably not need to check so many attributes.
    
     */

    @Test
    public void verifyAnExistingFile_applyManyChecks() {

        // We use the following few lines to help us calculate some expected properties

        FileSystemOperations fileOperations = new FileSystemOperations();

        long expectedFileSize = fileOperations.getFileSize(testFile1);

        String expectedMD5 = fileOperations.computeMd5Sum(testFile1, Md5SumMode.BINARY);

        FileSystemVerification fsv = new FileSystemVerification(testFile1);

        // it must match some regular expression

        fsv.checkContents(".*line 2.*", true, true);

        // it must not match some regular expression

        fsv.checkContents(".*line 22.*", true, false);

        // it must contain the following data

        fsv.checkContents("line 2", false, true);

        // it must not contain the following data

        fsv.checkContents("line 22", false, false);

        // check its size

        fsv.checkSize(expectedFileSize);

        // check its MD5 sum

        fsv.checkMd5(expectedMD5);

        fsv.verifyFileExists();

    }

    /**
     * When started, the verification keeps failing because the files does not
     * match the expected size.
     *
     * In this test a background thread keeps adding bytes to the file every 1000 ms
     * and eventually the verification succeeds.
     * The background thread in this example mimics the behavior of the real tested application.
    
     */

    @Test

    public void verifyBySize() {

        final String fileToCheck = IoUtils.normalizeFilePath(testDir1 + testFile1Name);

        final String fileContent = "12345";

        // create a file and keep adding data to it

        createFileInBackground(fileToCheck, fileContent);

        // while the file is getting filled with data, we keep

        // checking if it matches the expected size

        FileSystemVerification fsv = new FileSystemVerification(fileToCheck);

        fsv.checkSize(fileContent.length());

        fsv.verifyFileExists();

    }

    /**
     * When started, the verification keeps failing because the files does not
     * match the expected MD5 sum.
     *
     * In this test a background thread keeps adding bytes to the file every 1000 ms
     * and eventually the verification succeeds.
     * The background thread in this example mimics the behavior of the real tested application.
    
     */

    @Test

    public void verifyByMd5() {

        final String fileToCheck = IoUtils.normalizeFilePath(testDir1 + testFile1Name);

        final String fileContent = "12345";

        final String tempFile = IoUtils.normalizeFilePath(testDir1 + "temp_file.txt");

        new FileSystemOperations().createFile(tempFile, fileContent);

        final String expectedMd5 = new FileSystemOperations().computeMd5Sum(tempFile, Md5SumMode.BINARY);

        // create a file and keep adding data to it

        createFileInBackground(fileToCheck, fileContent);

        // while the file is getting filled with data, we keep checking if it matches the expected MD5

        FileSystemVerification fsv = new FileSystemVerification(fileToCheck);

        // specify the expected MD5 by providing it as a value

        fsv.checkMd5(expectedMd5);

        /*
        
         * OR specify the expected MD5 by providing another file to extract the MD5 from.
        
         * Usually when doing file transfers, it is convenient to point here to the source file,
        
         * its MD5 is calculated and used for the verification on the target file.
        
         *
        
         * Note that we give here null for first parameter - this means the source file is on the local host.
        
         * If it is on another host, you need to provide here the ATS Agent address.
        
         */

        // fsv.checkMd5( null, tempFile );

        fsv.verifyFileExists();

    }

    @Test

    public void verifyFileDisappears() {

        // deleted that file, but in a few seconds

        removeFileInBackground(testFile1);

        FileSystemVerification fsv = new FileSystemVerification(testFile1);

        // wait until that file disappear

        // it will pass on the first time when the searched file is not present

        fsv.verifyFileDoesNotExist();

    }

    @Test

    public void verifyDirectoryDisappears() {

        // deleted that file, but in a few seconds

        removeDirectoryInBackground(testDir1);

        FileSystemVerification fsv = new FileSystemVerification(testDir1);

        // wait until that file disappear

        // it will pass on the first time when the searched file is not present

        fsv.verifyFileDoesNotExist();

    }

    /**
     * Helper method used to create a file using a background thread.
     * This way the main thread is not blocked and the verification works as expected.
    
     */

    private void createFileInBackground( final String fileToCreate, final String content ) {

        new Thread(new Runnable() {

            @Override

            public void run() {

                FileSystemOperations fileOperations = new FileSystemOperations();

                try {

                    // create an empty file

                    Thread.sleep(1000);

                    fileOperations.createFile(fileToCreate, "");

                    // add slowly one byte at a time

                    for (int i = 0; i < content.length(); i++) {

                        Thread.sleep(1000);

                        String nextChar = String.valueOf(content.charAt(i));

                        fileOperations.appendToFile(fileToCreate, nextChar);

                    }

                } catch (InterruptedException e) {}

            }

        }).start();

    }

    /**
     * Helper method used to remove a file using a background thread.
     * This way the main thread is not blocked and the verification works as expected.
    
     */

    private void removeFileInBackground( final String fileToRemove ) {

        new Thread(new Runnable() {

            @Override

            public void run() {

                FileSystemOperations fileOperations = new FileSystemOperations();

                try {

                    // delete this file

                    Thread.sleep(3000);

                    fileOperations.deleteFile(fileToRemove);

                } catch (InterruptedException e) {}

            }

        }).start();

    }

    /**
     * Helper method used to remove a folder using a background thread.
     * This way the main thread is not blocked and the verification works as expected.
    
     */

    private void removeDirectoryInBackground( final String directoryToRemove ) {

        new Thread(new Runnable() {

            @Override

            public void run() {

                FileSystemOperations fileOperations = new FileSystemOperations();

                try {

                    // delete this directory

                    Thread.sleep(3000);

                    fileOperations.deleteDirectory(directoryToRemove);

                } catch (InterruptedException e) {}

            }

        }).start();

    }

}