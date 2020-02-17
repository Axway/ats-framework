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
package com.axway.ats.examples.basic.snapshots;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.axway.ats.action.filesystem.FileSystemOperations;
import com.axway.ats.action.filesystem.snapshot.FileSystemSnapshot;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.examples.common.BaseTestClass;

/**
 * In some cases you might be interested to know what changes were made on some file system.
 * The regular example is when you want to test whether an upgrade of your Application Under Test
 * did not cause any unwanted changes on the file system.
 *
 * File system snapshots are presented at:
 *      https://axway.github.io/ats-framework/File-System-Snapshots.html
 */
public class FileSystemSnapshotTests extends BaseTestClass {

    // The ATS entry point for taking different steps on the file system
    private FileSystemOperations fileOperations;

    private static final String ROOT_DIR;

    private static final String dir1;
    private static final String dir1File1;

    private static final String subdir1;
    private static final String subdir1File1;
    private static final String subdir1File2;

    static {
        ROOT_DIR = IoUtils.normalizeDirPath(userTempDir + "root_dir/");

        dir1 = IoUtils.normalizeDirPath(ROOT_DIR + "test_dir1/");
        dir1File1 = dir1 + "file1";

        subdir1 = IoUtils.normalizeDirPath(dir1 + "sub-dir1/");
        subdir1File1 = subdir1 + "file1";
        subdir1File2 = subdir1 + "file2";
    }

    public FileSystemSnapshotTests() {
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
        fileOperations.createDirectory(dir1);
        fileOperations.createFile(dir1File1, "content of file at " + dir1File1);

        fileOperations.createDirectory(subdir1);
        fileOperations.createFile(subdir1File1,
                                  "content of file at " + subdir1File1 + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
        fileOperations.createFile(subdir1File2,
                                  "content of file at " + subdir1File2 + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
    }

    /**
     * Imagine you want to be sure your product does not make any
     * change on some parts of the file system.
     *
     * Here is how to do it.
     */
    @Test
    public void makeSureNotChangeIsMade() {

        // 1. Make an instance of the FileSystemSnapshot
        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        // add as many as needed directories, each of them with a unique identifier
        // in our case we will work with one folder only(with its sub-folders)
        snapshot1.addDirectory("F1", dir1);
        // now take the snapshot which means we will have a picture of what we have found
        snapshot1.takeSnapshot();

        // if you want to check the result of taking a snapshot, see the output of the next code
        snapshot1.toString();

        // 2. DO SOME REAL WORK
        // In this test, we assume your application does not make any change in the monitored
        // folders, so we should get same snapshots

        // 3. Take a second snapshot on same source
        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", dir1);
        snapshot2.takeSnapshot();

        // 4. check both snapshots are same
        snapshot1.compare(snapshot2);
    }

    /**
     * One of the files is changed. It could be a log file, so it must be taken out of the comparison
     */
    @Test
    public void aFileIsModified() {

        // 1. take first snapshot
        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", dir1);

        // IMPORTANT PART: skip a file which is expected to be different
        snapshot1.skipFile("F1", "\\sub-dir1\\file1");

        snapshot1.takeSnapshot();

        // 2. DO SOME REAL WORK
        // in this case we modify one of the files
        fileOperations.replaceTextInFile(subdir1File1, "content", "modified_content", false);

        // 3. take second snapshot
        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", dir1);

        // IMPORTANT PART: skip a file which is expected to be different
        snapshot2.skipFile("F1", "\\sub-dir1\\file1");

        snapshot2.takeSnapshot();

        // 4. check both snapshots are same
        snapshot1.compare(snapshot2);
    }

    /**
     * A file has same content which means size and MD5 are same as well.
     * But the 'Modification time' is different. It could happen when transferring/copying files
     */
    @Test
    public void aFileWithDifferentTimestamp() {

        // 1. take first snapshot
        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", dir1);

        // IMPORTANT PART: check the file, but do not care about its Modification time
        snapshot1.skipFile("F1", "\\sub-dir1\\file1", FileSystemSnapshot.SKIP_FILE_MODIFICATION_TIME);

        snapshot1.takeSnapshot();

        // 2. DO SOME REAL WORK
        // In this we touch the file, but its content remains the same.
        // Still this results in different Modification time
        fileOperations.replaceTextInFile(subdir1File1, "content", "content", false);

        // 3. take second snapshot
        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", dir1);

        // IMPORTANT PART:check the file, but do not care about its Modification time
        snapshot2.skipFile("F1", "\\sub-dir1\\file1", FileSystemSnapshot.SKIP_FILE_MODIFICATION_TIME);

        snapshot2.takeSnapshot();

        // 4. check both snapshots are same
        snapshot1.compare(snapshot2);
    }

    /**
     * A file has same content length, but the 'Modification time' and 'MD5' are different
     */
    @Test
    public void aFileWithDifferentContentButSameSize() {

        // 1. take first snapshot
        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", dir1);

        // IMPORTANT PART: check the file, but do not care about its Modification time and MD5
        snapshot1.skipFile("F1", "\\sub-dir1\\file1", FileSystemSnapshot.SKIP_FILE_MODIFICATION_TIME,
                           FileSystemSnapshot.SKIP_FILE_MD5);

        snapshot1.takeSnapshot();

        // 2. DO SOME REAL WORK
        // In this we change the file content, but its size remains the same.
        // Still this results in different Modification time and MD5
        fileOperations.replaceTextInFile(subdir1File1, "content", "CONTENT", false);

        // 3. take second snapshot
        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", dir1);

        // IMPORTANT PART:check the file, but do not care about its Modification time and MD5
        snapshot2.skipFile("F1", "\\sub-dir1\\file1", FileSystemSnapshot.SKIP_FILE_MODIFICATION_TIME,
                           FileSystemSnapshot.SKIP_FILE_MD5);

        snapshot2.takeSnapshot();

        // 4. check both snapshots are same
        snapshot1.compare(snapshot2);
    }

    /**
     * A new file is present in the second snapshot
     */
    @Test
    public void aNewFileIsPresent() {

        // 1. take first snapshot
        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", dir1);
        snapshot1.takeSnapshot();

        // 2. DO SOME REAL WORK
        // in this case we create a new file
        fileOperations.createFile(subdir1 + "new_file1", 100, true);

        // 3. take second snapshot
        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", dir1);

        // IMPORTANT PART: Skip the newly arrived file
        snapshot2.skipFile("F1", "\\sub-dir1\\new_file1");

        snapshot2.takeSnapshot();

        // 4. check both snapshots are same
        snapshot1.compare(snapshot2);
    }

    /**
     * Specify a number of files to be skipped by using a regular expression
     */
    @Test
    public void skipManyFilesAtOnce() {

        // 1. take first snapshot
        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", dir1);
        snapshot1.takeSnapshot();

        // 2. DO SOME REAL WORK
        // in this case we create some files
        fileOperations.createFile(subdir1 + "new_file1.log", 100, true);
        fileOperations.createFile(subdir1 + "new_file2.log", 100, true);
        fileOperations.createFile(subdir1 + "new_file3.log", 100, true);

        // 3. take second snapshot
        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", dir1);

        // IMPORTANT PART: Skip all files that match some regular expression
        snapshot2.skipFileByRegex("F1", "sub-dir1/.*log");

        snapshot2.takeSnapshot();

        // 4. check both snapshots are same
        snapshot1.compare(snapshot2);
    }

    /**
     * A new folder is present in the second snapshot.
     * The folder might contain files and other folders.
     * We skip the new folder and all its content
     */
    @Test
    public void aNewFolderIsPresent() {

        // 1. take first snapshot
        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", dir1);
        snapshot1.takeSnapshot();

        // 2. DO SOME REAL WORK
        // in this case we create a new folder and fill it with some files and sub folders
        String newDir = subdir1 + "sub-dir3/";
        fileOperations.createDirectory(newDir);
        fileOperations.createFile(newDir + "new_file1", 100, true);
        fileOperations.createDirectory(newDir + "sub-dir4/");
        fileOperations.createFile(newDir + "sub-dir4/" + "new_file1", 100, true);

        // 3. take second snapshot
        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", dir1);

        // IMPORTANT PART: Skip the new folder with all its content
        snapshot2.skipDirectory("F1", "sub-dir1/sub-dir3/");

        snapshot2.takeSnapshot();

        // 4. check both snapshots are same
        snapshot1.compare(snapshot2);
    }

    /**
     * Specify a number of folders to be skipped by using a regular expression
     */
    @Test
    public void skipManyFoldersAtOnce() {

        // 1. take first snapshot
        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", dir1);
        snapshot1.takeSnapshot();

        // 2. DO SOME REAL WORK
        // in this case we create some folders
        fileOperations.createDirectory(subdir1 + "sub-dir1/");
        fileOperations.createDirectory(subdir1 + "sub-dir2/");
        fileOperations.createDirectory(subdir1 + "sub-dir3/");
        fileOperations.createDirectory(subdir1 + "sub-dir4/");
        fileOperations.createDirectory(subdir1 + "sub-dir5/");
        // 3. take second snapshot
        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", dir1);

        // IMPORTANT PART: Skip all folders which name match some regular expression
        snapshot2.skipDirectoryByRegex("F1", "sub-dir1/sub-dir[0-9]");

        snapshot2.takeSnapshot();

        // 4. check both snapshots are same
        snapshot1.compare(snapshot2);
    }

    /**
     * By default 'properties files' are not checked when it comes to Size and MD5, instead their content is checked.
     * The reason is that these are often used as configuration files, and as such it does not matter if some
     * key-value pair is at another line, but it is important what the values are.
     *
     * This test creates 2 folders with 1 properties file inside. Both files are almost the same, but still different.
     * The file differences are:
     *      - there is a parameter with different value in both files - param1
     *      - there is a parameter present in one of the files only - param5
     *      - parameters are not in same order - for example param1 is the first in one of the files, but it is second in the other
     *      - there is different set of empty spaces
     *      - there is different set of empty lines
     * Only the first 2 listed differences are significant, the others are disregarded.
     *
     * You can specify which key-value pairs to be skipped by pointing to their keys or values using
     * equal strings, containing strings or regular expressions
     *
     * Note: in this test we make sure both files have same modification time, so we do not have to worry about it
     *
     * Related user guide page:
     *      https://axway.github.io/ats-framework/File-System-Snapshots---Inspect-the-content-of-some-file-types.html
     */
    @Test
    public void comparePropertyFiles() {

        // name of compared properties files
        final String propsFileName = "options.properties";

        // the first folder with a properties file
        final String subdir3 = IoUtils.normalizeDirPath(dir1 + "sub-dir3/");
        fileOperations.createDirectory(subdir3);
        fileOperations.createFile(subdir3 + propsFileName,
                                  "param1=value1 \r\n param2 = value2 \r\n      param3 = value3");
        long modificationTime = fileOperations.getFileModificationTime(subdir3 + propsFileName);

        // the second folder with a properties file
        final String subdir4 = IoUtils.normalizeDirPath(dir1 + "sub-dir4/");
        fileOperations.createDirectory(subdir4);
        fileOperations.createFile(subdir4 + propsFileName,
                                  " \r\n\r\n \r\n param2 = value2 \r param1=value4 \n      param3 = value3\r\nparam5 = value5");
        // make both files with same modification time
        fileOperations.setFileModificationTime(subdir4 + propsFileName, modificationTime);

        // 1. take first snapshot
        FileSystemSnapshot snapshot3 = new FileSystemSnapshot("snap3");
        snapshot3.addDirectory("F1", subdir3);

        // IMPORTANT PART: list all the expected differences
        // Note that it does not matter on which snapshot instance you will use the skip methods

        // skip key with name 'param1'
        // effectively this will take the 'param1' out of comparison
        snapshot3.properties.skipPropertyByKeyEqualsText("F1", propsFileName, "param1");

        // skip keys with name containing small caps characters and ending with the number 5
        // effectively this will take the 'param5' out of comparison
        snapshot3.properties.skipPropertyByKeyMatchingText("F1", propsFileName, "[a-z]*5");

        snapshot3.takeSnapshot();

        // 3. take second snapshot
        FileSystemSnapshot snapshot4 = new FileSystemSnapshot("snap4");
        snapshot4.addDirectory("F1", subdir4);
        snapshot4.takeSnapshot();

        // 4. check both snapshots are same
        snapshot3.compare(snapshot4);
    }

    /**
     * By default 'XML files' are not checked when it comes to Size and MD5, instead their content is checked.
     * The reason is that these are often used as configuration files, and as such it does not matter if
     * the nodes formatting, but it is important what the node values are.
     *
     * This test creates 2 folders with 1 XML file inside. Both files are almost the same, but still different.
     * The file differences are:
     *      - Difference 1: the first "employee" have very different content inside, but most importantly they have different attribute - "id".
     *              We treat these nodes as different ones and we will skip them in the comparision
     *      - Difference 2: the second "employee" have is missing a node present in one file only - //employees/employee/division
     *              We will these both employees as same, but we will skip the 'division' sub node
     *      - Empty node in different format - the "//employees/employee/building" node is once presented
     *              as "<building></building>" and then as "<building/>"
     *              We treat this as same node
     *      - there is different set of empty spaces
     *      - there is different set of empty lines
     * Only the first 2 listed differences are significant, the others are disregarded.
     *
     * You can specify which nodes to be skipped by pointing to their value or attributes using
     * equal strings, containing strings or regular expressions
     *
     * Note: in this test we make sure both files have same modification time, so we do not have to worry about it
     *
     * Related user guide page:
     *      https://axway.github.io/ats-framework/File-System-Snapshots---Inspect-the-content-of-some-file-types.html
     */
    @Test
    public void compareXmlFiles() {

        // name of compared XML files
        final String xmlFileName = "options.xml";

        // first file content
        StringBuilder file1 = new StringBuilder();
        file1.append("<employees>\n");
        file1.append("    <employee id=\"007\">\n");
        file1.append("        <name>Will Smith</name>\n");
        file1.append("        <title>Showman</title>\n");
        file1.append("        <division>Hollywood</division>\n");
        file1.append("        <building></building>\n");
        file1.append("    </employee>\n");
        file1.append("    <employee id=\"009\">\n");
        file1.append("     <name>Indiana Jones</name>\n");
        file1.append("        <title>Adventurer</title>\n");
        file1.append("        \n");
        file1.append("        <building></building>\n");
        file1.append("    </employee>\n");
        file1.append("</employees>\n");

        // second file content
        StringBuilder file2 = new StringBuilder();
        file2.append("<employees>\n");
        file2.append("    <employee id=\"008\">\n");
        file2.append("        <name>Somebody</name>\n");
        file2.append("        <title>Some title</title>\n");
        file2.append("        <division>Somewhere</division>\n");
        file2.append("        <building/>\n");
        file2.append("    </employee>\n");
        file2.append("    <employee id=\"009\">\n");
        file2.append("        <name>Indiana Jones</name>\n");
        file2.append("        <title>Adventurer</title>\n");
        file2.append("        <division>Hollywood</division>\n");
        file2.append("        <building></building>\n");
        file2.append("    </employee>\n");
        file2.append("</employees>\n");

        // the first folder with a XML file
        final String subdir3 = IoUtils.normalizeDirPath(dir1 + "sub-dir3/");
        fileOperations.createDirectory(subdir3);
        fileOperations.createFile(subdir3 + xmlFileName, file1.toString());
        long modificationTime = fileOperations.getFileModificationTime(subdir3 + xmlFileName);

        // the second folder with a XML file
        final String subdir4 = IoUtils.normalizeDirPath(dir1 + "sub-dir4/");
        fileOperations.createDirectory(subdir4);
        fileOperations.createFile(subdir4 + xmlFileName, file2.toString());
        // make both files with same modification time
        fileOperations.setFileModificationTime(subdir4 + xmlFileName, modificationTime);

        // 1. take first snapshot
        FileSystemSnapshot snapshot3 = new FileSystemSnapshot("snap3");
        snapshot3.addDirectory("F1", subdir3);

        // IMPORTANT PART: list all the expected differences
        // skip the first "employee" nodes together with their sub nodes
        snapshot3.xml.skipNodeByAttributeValueEqualsText("F1", xmlFileName, "//employees/employee", "id",
                                                         "007");
        snapshot3.takeSnapshot();

        // 3. take second snapshot
        FileSystemSnapshot snapshot4 = new FileSystemSnapshot("snap4");
        snapshot4.addDirectory("F1", subdir4);

        // IMPORTANT PART: list all the expected differences
        // skip the first "employee" nodes together with their sub nodes
        snapshot4.xml.skipNodeByAttributeValueEqualsText("F1", xmlFileName, "//employees/employee", "id",
                                                         "008");
        // skip the "division" node of "employee" with "id=009"
        snapshot4.xml.skipNodeByValueEqualsText("F1", xmlFileName,
                                                "//employees/employee[@id=\"009\"]/division", "Hollywood");
        snapshot4.takeSnapshot();

        // 4. check both snapshots are same
        snapshot3.compare(snapshot4);
    }

    /**
     * By default 'ini files' are not checked when it comes to Size and MD5, instead their content is checked.
     * The reason is that these are often used as configuration files, and as such it does not matter if some
     * key-value pair is at another line, but it is important what the values are.
     *
     * This test creates 2 folders with 1 INI file inside. Both files are almost the same, but still different.
     * The file differences are:
     *      - there is a parameter with different value in both files - param4
     *      - there is a parameter present in one of the files only - param1
     *      - parameters are not in same order - for example param3 and param4 are in opposite order
     *      - commented parameter with different values - param2
     *      - there is different set of empty spaces
     *      - there is different set of empty lines
     * Only the first 2 listed differences are significant, the others are disregarded.
     *
     * You can specify which key-value pairs to be skipped by pointing to their keys or values using
     * equal strings, containing strings or regular expressions
     *
     * Note: in this test we make sure both files have same modification time, so we do not have to worry about it
     *
     * Related user guide page:
     *      https://axway.github.io/ats-framework/File-System-Snapshots---Inspect-the-content-of-some-file-types.html
     *
     */
    @Test
    public void compareIniFiles() {

        // name of compared INI files
        final String propsFileName = "options.ini";

        // first file content
        StringBuilder file1 = new StringBuilder();
        file1.append("[Mail]\n");
        file1.append("param1=value1\n");
        file1.append("#param2=value2\n");
        file1.append("param3 =  value3  \n");
        file1.append("param4=value4\n");
        file1.append("\n");
        file1.append("[languages]\n");
        file1.append("00A=Spanish\n");

        // second file content
        StringBuilder file2 = new StringBuilder();
        file2.append("[Mail]\n");
        file2.append("\n\n");
        file2.append("#param2=value222\n");
        file2.append("param4=value444\n");
        file2.append("param3=value3\n");
        file2.append("\n");
        file2.append("[languages]\n");
        file2.append("00A=Spanish\n");

        // the first folder with a INI file
        final String subdir3 = IoUtils.normalizeDirPath(dir1 + "sub-dir3/");
        fileOperations.createDirectory(subdir3);
        fileOperations.createFile(subdir3 + propsFileName, file1.toString());
        long modificationTime = fileOperations.getFileModificationTime(subdir3 + propsFileName);

        // the second folder with a INI file
        final String subdir4 = IoUtils.normalizeDirPath(dir1 + "sub-dir4/");
        fileOperations.createDirectory(subdir4);
        fileOperations.createFile(subdir4 + propsFileName, file2.toString());
        // make both files with same modification time
        fileOperations.setFileModificationTime(subdir4 + propsFileName, modificationTime);

        // 1. take first snapshot
        FileSystemSnapshot snapshot3 = new FileSystemSnapshot("snap3");
        snapshot3.addDirectory("F1", subdir3);

        // IMPORTANT PART: list all the expected differences
        // Note that it does not matter on which snapshot instance you will use the skip methods

        // skip key with name 'param1'
        snapshot3.ini.skipIniPropertyByKeyEqualsText("F1", propsFileName, "[Mail]", "param1");
        // skip key with value matching some regular expression
        snapshot3.ini.skipIniPropertyByValueMatchingText("F1", propsFileName, "[Mail]", "value4.*");
        snapshot3.takeSnapshot();

        // 3. take second snapshot
        FileSystemSnapshot snapshot4 = new FileSystemSnapshot("snap4");
        snapshot4.addDirectory("F1", subdir4);
        snapshot4.takeSnapshot();

        // 4. check both snapshots are same
        snapshot3.compare(snapshot4);
    }

    /**
     * By default 'text files' are not checked when it comes to Size and MD5, instead their content is checked.
     * Text files are checked line by line, no smaller tokens.
     *
     * This test creates 2 folders with 1 text file inside. Both files are almost the same, but still different.
     * The file differences are:
     *      - there is a line present in one of the files only
     *      - lines are not in same order
     *      - there is different set of empty spaces
     *      - there is different set of empty lines
     * Only the first listed difference is significant, the others are disregarded.
     *
     * You can specify which key-value pairs to be skipped by pointing to their keys or values using
     * equal strings, containing strings or regular expressions
     *
     * Note: in this test we make sure both files have same modification time, so we do not have to worry about it
     *
     * Related user guide page:
     *      https://axway.github.io/ats-framework/File-System-Snapshots---Inspect-the-content-of-some-file-types.html
     *
     */
    @Test
    public void compareTextFiles() {

        // name of compared TEXT files
        final String textFileName = "some_text_file.txt";

        // first file content
        StringBuilder file1 = new StringBuilder();
        file1.append("When a father gives to his son, both laugh; when a son gives to his father, both cry.\n");
        file1.append("By William\n");
        file1.append("\n");
        file1.append(" Shakespeare  \n");

        // second file content
        StringBuilder file2 = new StringBuilder();
        file2.append("     Shakespeare\n");
        file2.append("When a father gives to his son, both laugh; when a son gives to his father, both cry.\n");
        file2.append("\n");
        file2.append("\n");

        // the first folder with a TEXT file
        final String subdir3 = IoUtils.normalizeDirPath(dir1 + "sub-dir3/");
        fileOperations.createDirectory(subdir3);
        fileOperations.createFile(subdir3 + textFileName, file1.toString());
        long modificationTime = fileOperations.getFileModificationTime(subdir3 + textFileName);

        // the second folder with a TEXT file
        final String subdir4 = IoUtils.normalizeDirPath(dir1 + "sub-dir4/");
        fileOperations.createDirectory(subdir4);
        fileOperations.createFile(subdir4 + textFileName, file2.toString());
        // make both files with same modification time
        fileOperations.setFileModificationTime(subdir4 + textFileName, modificationTime);

        // 1. take first snapshot
        FileSystemSnapshot snapshot3 = new FileSystemSnapshot("snap3");
        snapshot3.addDirectory("F1", subdir3);

        // IMPORTANT PART: list all the expected differences
        // Note that it does not matter on which snapshot instance you will use the skip methods

        // There are different ways to skip some line:
        // skip line fully matching some value
        snapshot3.text.skipTextLineEqualsText("F1", textFileName, "By William");
        // skip line containing some value
        // snapshot3.text.skipTextLineContainingText( "F1", textFileName, "William" );
        // skip line matching some regular expression
        // snapshot3.text.skipTextLineMatchingText( "F1", textFileName, ".*William" );
        snapshot3.takeSnapshot();

        // 3. take second snapshot
        FileSystemSnapshot snapshot4 = new FileSystemSnapshot("snap4");
        snapshot4.addDirectory("F1", subdir4);
        snapshot4.takeSnapshot();

        // 4. check both snapshots are same
        snapshot3.compare(snapshot4);
    }
}
