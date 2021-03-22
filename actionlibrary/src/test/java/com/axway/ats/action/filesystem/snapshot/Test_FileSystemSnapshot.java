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
package com.axway.ats.action.filesystem.snapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import com.axway.ats.action.ActionLibraryConfigurator;
import com.axway.ats.action.BaseTest;
import com.axway.ats.common.filesystem.snapshot.FileSystemSnapshotException;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.filesystem.LocalFileSystemOperations;
import com.axway.ats.core.system.LocalSystemOperations;
import com.axway.ats.core.utils.IoUtils;

public class Test_FileSystemSnapshot extends BaseTest {

    private static final String       FILES_ROOT;
    private static String             TMP_FILES_ROOT;

    private ActionLibraryConfigurator configurator  = ActionLibraryConfigurator.getInstance();

    private static int                tmpDirCounter = 0;

    static {

        String projectRoot = getProjectRoot();
        if (OperatingSystemType.getCurrentOsType().isWindows()) {
            // remove the leading "/"
            if (projectRoot.startsWith("/")) {
                projectRoot = projectRoot.substring(1);
            }
        }

        FILES_ROOT = IoUtils.normalizeUnixDir(projectRoot) + "src/test/resources/"
                     + Test_FileSystemSnapshot.class.getPackage().getName().replace('.', '/') + "/";

        TMP_FILES_ROOT = IoUtils.normalizeUnixDir(projectRoot) + "build/tmp_files/";

        new LocalFileSystemOperations().deleteDirectory(TMP_FILES_ROOT, true);
        // we wait some time after cleanup this directory, otherwise we sometimes get
        // file write errors in some of the following tests
        sleep(200);

        new File(TMP_FILES_ROOT).mkdirs();
        sleep(50);

        /*
         * As some of the tests are dealing with the last modify time, we must maintain these times accurate.
         *
         * Otherwise when the project is placed on a new place, these times get changed and this breaks
         * some tests
         */
        fixFileModificationTimes();

        new LocalFileSystemOperations().setFileHiddenAttribute(FILES_ROOT + "dir1"
                                                               + AtsSystemProperties.SYSTEM_FILE_SEPARATOR
                                                               + ".hidden_dir", true);
    }

    private static String getProjectRoot() {

        String root = Test_FileSystemSnapshot.class.getResource("/").getPath();
        do {
            root = IoUtils.normalizeDirPath(root);
            if (new File(root + "pom.xml").exists()) {
                return root;
            }
        } while ( (root = new File(root).getParent()) != null);

        throw new RuntimeException("Unable to determine the project's root directory.");
    }

    private static void fixFileModificationTimes() {

        // map with all modification times of all files used in the tests
        long now = new Date().getTime();

        Map<Long, String> map = new TreeMap<Long, String>();
        map.put(now, "dir3/sub-dir1/file2.xml");

        map.put(now + 10000,
                "dir1/file1.xml;dir1_copy/file1.xml;dir3/file1.xml;dir4/file1.xml;dir5/file1.xml");
        map.put(now + 20000,
                "dir1/file2.xml;dir1_copy/file2.xml;dir3/file2.xml;dir4/file2.xml;dir5/file2.xml");
        map.put(now + 30000,
                "dir1/sub-dir1/file2.xml;dir1/sub-dir1/sub-dir3/file2.xml;dir1_copy/sub-dir1/file2.xml;dir1_copy/sub-dir1/sub-dir3/file2.xml;dir3/sub-dir1/sub-dir3/file2.xml;dir4/sub-dir1/file2.xml;dir4/sub-dir1/file3.xml;dir4/sub-dir1/file4.xml;dir4/sub-dir1/file5.xml;dir4/sub-dir1/sub-dir3/file2.xml;dir5/sub-dir1/file2.xml;dir5/sub-dir1/sub-dir3/file2.xml;dir5/sub-dir2/file2.xml;modification_time/sub-dir1/file2.xml");
        map.put(now + 40000, "dir2/file1.xml");
        map.put(now + 50000, "dir2/file2.xml");
        map.put(now + 60000, "dir2/file3.xml");
        map.put(now + 70000, "dir2/file4.xml");
        map.put(now + 80000, "modification_time/sub-dir2/file2.xml");
        map.put(now + 90000,
                "md5/sub-dir1/file3.xml;md5/sub-dir2/file3.xml;missing_file/sub-dir1/file3.xml;missing_file/sub-dir2/file3.xml;modification_time/sub-dir1/file3.xml;modification_time/sub-dir2/file3.xml;size/sub-dir1/file3.xml;size/sub-dir2/file3.xml");
        map.put(now + 100000,
                "missing_file/sub-dir1/file2.xml;size/sub-dir1/file2.xml;size/sub-dir2/file2.xml");
        map.put(now + 110000, "md5/sub-dir1/file2.xml;md5/sub-dir2/file2.xml");

        for (long time : map.keySet()) {
            // get all the files for one timestamp
            String[] files = map.get(time).split(";");
            for (String file : files) {
                // apply the correct timestamp
                new LocalFileSystemOperations().setFileModificationTime(FILES_ROOT + file, time);
            }
        }
    }

    @Before
    public void setUp() {

        // restore the default configuration settings
        configurator.snapshots.setCheckFileSize(true);
        configurator.snapshots.setCheckModificationTime(true);
        configurator.snapshots.setCheckFileMd5(true);
        configurator.snapshots.setCheckFilePermissions(false);
        configurator.snapshots.setSupportHiddenFiles(false);

        // these tests do not worry about file content
        configurator.snapshots.setCheckPropertiesFilesContent(false);
        configurator.snapshots.setCheckXmlFilesContent(false);
    }

    @Test
    public void newSnapshotInstance() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = snapshot1.newSnapshot("snap2");
        snapshot2.takeSnapshot();

        snapshot1.compare(snapshot2);

    }

    @Test
    public void sameSnapshots() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot2.takeSnapshot();

        snapshot1.compare(snapshot2);
    }

    @Test
    public void sameSnapshotsDifferentDirs() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "dir1_copy");
        snapshot2.takeSnapshot();

        snapshot1.compare(snapshot2);
    }

    @Test
    public void skipFileFromTheSecondSnapshot() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "dir3");
        snapshot2.skipFile("F1", "sub-dir1/file2.xml");
        snapshot2.takeSnapshot();

        try {
            snapshot1.compare(snapshot2);
        } catch (FileSystemSnapshotException se) {
            verifyError(se, ".*File is present in [snap1] snapshot only.*sub-dir1/file2.xml.*");
        }
    }

    @Test
    public void skipTwoFilesFromDifferentDirs() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir5");
        snapshot1.skipFile("F1", "sub-dir1/file2.xml");
        snapshot1.skipFile("F1", "sub-dir2/file2.xml");
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "dir5");
        snapshot2.skipFile("F1", "sub-dir2/file2.xml");
        snapshot2.skipFile("F1", "sub-dir1/file2.xml");
        snapshot2.takeSnapshot();

        snapshot1.compare(snapshot2);
    }

    @Test
    public void skipFileByRegex() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot1.skipFile("F1", "sub-dir1/file2.xml");
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "dir4");
        snapshot2.skipFileByRegex("F1", "sub-dir1/file.*");
        snapshot2.takeSnapshot();

        snapshot1.compare(snapshot2);
    }

    @Test
    public void skipDirectoryByRegex() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot1.skipDirectoryByRegex("F1", FILES_ROOT + "dir1/[0-9]");
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot2.skipDirectory("F1", FILES_ROOT + "dir1/sub-dir1");
        snapshot2.takeSnapshot();

        snapshot1.compare(snapshot2);
    }

    @Test
    public void skipFileByRegex2() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "modification_time/sub-dir1");
        snapshot1.skipFile("F1", "file2.xml", FileSystemSnapshot.SKIP_FILE_MODIFICATION_TIME);
        snapshot1.skipFile("F1", "file3.xml", FileSystemSnapshot.SKIP_FILE_MODIFICATION_TIME);
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "modification_time/sub-dir2");
        snapshot2.skipFileByRegex("F1", "file.*", FileSystemSnapshot.SKIP_FILE_MODIFICATION_TIME);
        snapshot2.takeSnapshot();

        snapshot1.compare(snapshot2);
    }

    @Test
    public void skipFile_notExistingFile() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot1.skipFile("F1", "sub-dir1/not_existing_file");
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot2.takeSnapshot();

        snapshot1.compare(snapshot2); // exception is not expected

    }

    @Test
    public void negative_differentFile() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "dir3");
        snapshot2.takeSnapshot();

        try {
            snapshot1.compare(snapshot2);
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se, ".*MD5 checksum: .*");
        }
    }

    @Test
    public void negative_addSameDir() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir1");

        try {
            snapshot1.addDirectory("F1", FILES_ROOT + "dir1");
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se, "There is already a directory with alias 'F1' for snapshot 'snap1'");
        }
    }

    @Test
    public void skipFileSize_notExistingFile() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot1.skipFile("F1", "sub-dir1/non_existing_file.xml", FileSystemSnapshot.SKIP_FILE_SIZE);
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "dir1_copy");
        snapshot2.takeSnapshot();

        snapshot1.compare(snapshot2); // exception is not expected
    }

    @Test
    public void skipFileSizeAndModificationTimeAndMd5() {

        /* In this case we want to skip the file size and this automatically
         * add rule to skip the check of file MD5 - if not the test will fail
        */
        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot1.skipFile("F1", "sub-dir1/file2.xml", FileSystemSnapshot.SKIP_FILE_SIZE,
                           FileSystemSnapshot.SKIP_FILE_MODIFICATION_TIME);
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "dir3");
        snapshot2.skipFile("F1", "sub-dir1/file2.xml", FileSystemSnapshot.SKIP_FILE_SIZE,
                           FileSystemSnapshot.SKIP_FILE_MODIFICATION_TIME);
        snapshot2.takeSnapshot();

        snapshot1.compare(snapshot2);
    }

    @Test
    public void skipFile_addSameRuleManyTimes() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot1.skipFile("F1", "sub-dir1/file2.xml", FileSystemSnapshot.SKIP_FILE_SIZE,
                           FileSystemSnapshot.SKIP_FILE_SIZE, FileSystemSnapshot.SKIP_FILE_SIZE,
                           FileSystemSnapshot.SKIP_FILE_SIZE, FileSystemSnapshot.SKIP_FILE_MODIFICATION_TIME,
                           FileSystemSnapshot.SKIP_FILE_SIZE, FileSystemSnapshot.SKIP_FILE_SIZE);
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "dir3");
        snapshot2.skipFile("F1", "sub-dir1/file2.xml", FileSystemSnapshot.SKIP_FILE_MODIFICATION_TIME,
                           FileSystemSnapshot.SKIP_FILE_MODIFICATION_TIME, FileSystemSnapshot.SKIP_FILE_SIZE,
                           FileSystemSnapshot.SKIP_FILE_SIZE);
        snapshot2.takeSnapshot();

        snapshot1.compare(snapshot2);
    }

    @Test
    public void aDirMissingFromOneSnapshot() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "dir5");
        snapshot2.takeSnapshot();

        try {
            snapshot1.compare(snapshot2);
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se, ".*Directory is present in [snap2] snapshot only.*");
        }
    }

    @Test
    public void aFileMissingFromOneSnapshot() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "missing_file/sub-dir1");
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "missing_file/sub-dir2");
        snapshot2.takeSnapshot();

        try {
            snapshot1.compare(snapshot2);
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se,
                        ".*File is present in [snap1] snapshot only.*missing_file/sub-dir1/file2.xml.*");
        }
    }

    @Test
    public void firstSnapshotFromFile() {

        final String tempDir = getTempDir();
        final String tempFile = tempDir + "tmp.xml";

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("some_snap");
        snapshot2.addDirectory("F1", FILES_ROOT + "dir5");
        snapshot2.takeSnapshot();
        snapshot2.toLocalFile(tempFile);

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap_from_file");
        snapshot1.loadFromLocalFile(null, tempFile);
        snapshot1.compare(snapshot2);
    }

    @Test
    public void secondSnapshotFromFile() {

        final String tempDir = getTempDir();
        final String tempFile = tempDir + "tmp.xml";

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("some_snap");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir5");
        snapshot1.takeSnapshot();
        snapshot1.toLocalFile(tempFile);

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap_from_file");
        snapshot2.loadFromLocalFile(null, tempFile);
        snapshot1.compare(snapshot2);
    }

    @Test
    public void bothSnapshotsFromFile() {

        final String tempDir = getTempDir();
        final String tempFile1 = tempDir + "tmp1.xml";
        final String tempFile2 = tempDir + "tmp2.xml";

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot1.takeSnapshot();
        snapshot1.toLocalFile(tempFile1);

        FileSystemSnapshot snapshot1_fromFile = new FileSystemSnapshot("snap1_from_file");
        snapshot1_fromFile.loadFromLocalFile(null, tempFile1);

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "dir1_copy");
        snapshot2.takeSnapshot();
        snapshot2.toLocalFile(tempFile2);

        FileSystemSnapshot snapshot2_fromFile = new FileSystemSnapshot("snap2_from_file");
        snapshot2_fromFile.loadFromLocalFile("", tempFile2);

        snapshot1_fromFile.compare(snapshot2_fromFile);
    }

    @Test
    public void loadSnapshotWithFileToSkip() {

        final String tempDir = getTempDir();
        final String tempFile1 = tempDir + "tmp1.xml";

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot1.skipFile("F1", "sub-dir1/file2.xml");
        snapshot1.takeSnapshot();
        snapshot1.toLocalFile(tempFile1);

        FileSystemSnapshot snapshot1_fromFile = new FileSystemSnapshot("snap1_from_file");
        snapshot1_fromFile.loadFromLocalFile(null, tempFile1);

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "dir3");
        snapshot2.skipFile("F1", "sub-dir1/file2.xml");
        snapshot2.takeSnapshot();

        snapshot2.compare(snapshot1_fromFile);
    }

    @Test
    public void loadSnapshotWithFileToSkip_specifyWhichAttributes() {

        final String tempDir = getTempDir();
        final String tempFile1 = tempDir + "tmp1.xml";

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot1.skipFile("F1", "sub-dir1/file2.xml", FileSystemSnapshot.SKIP_FILE_SIZE,
                           FileSystemSnapshot.SKIP_FILE_MODIFICATION_TIME,
                           FileSystemSnapshot.SKIP_FILE_MD5);
        snapshot1.takeSnapshot();
        snapshot1.toLocalFile(tempFile1);

        FileSystemSnapshot snapshot1_fromFile = new FileSystemSnapshot("snap1_from_file");
        snapshot1_fromFile.loadFromLocalFile(null, tempFile1);
        snapshot1_fromFile.takeSnapshot();

        snapshot1.compare(snapshot1_fromFile);
    }

    @Test
    public void loadSnapshotWithFileToSkip_findFileByRegex() {

        final String tempDir = getTempDir();
        final String tempFile1 = tempDir + "tmp1.xml";

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot1.skipFileByRegex("F1", "sub-dir1/file.*xml");
        snapshot1.takeSnapshot();
        snapshot1.toLocalFile(tempFile1);

        FileSystemSnapshot snapshot1_fromFile = new FileSystemSnapshot("snap1_from_file");
        snapshot1_fromFile.loadFromLocalFile(null, tempFile1);
        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "dir3");
        snapshot2.skipFile("F1", "sub-dir1/file2.xml");
        snapshot2.takeSnapshot();

        snapshot2.compare(snapshot1_fromFile);
    }

    @Test
    public void loadSnapshotWithFileToSkip_andTakeSnapshot() {

        final String tempDir = getTempDir();
        final String tempFile1 = tempDir + "tmp1.xml";

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot1.skipFileByRegex("F1", "sub-dir1/file.*xml");
        snapshot1.takeSnapshot();
        snapshot1.toLocalFile(tempFile1);
        System.out.println(snapshot1.toString() + "\n\n");

        FileSystemSnapshot snapshot1_fromFile = new FileSystemSnapshot("snap1_from_file");
        snapshot1_fromFile.loadFromLocalFile(null, tempFile1);
        snapshot1_fromFile.takeSnapshot();
        System.out.println(snapshot1_fromFile.toString() + "\n\n");

        snapshot1.compare(snapshot1_fromFile);
    }

    @Test
    public void loadSnapshotWithDirAndFileToSkip() {

        final String tempDir = getTempDir();
        final String tempFile1 = tempDir + "tmp1.xml";
        final String tempFile2 = tempDir + "tmp2.xml";

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir4");
        snapshot1.skipFile("F1", "sub-dir1/file3.xml");
        snapshot1.skipFile("F1", "sub-dir1/file4.xml");
        snapshot1.skipFile("F1", "sub-dir1/file5.xml");
        snapshot1.takeSnapshot();
        snapshot1.toLocalFile(tempFile1);

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "dir5");
        snapshot2.skipDirectory("F1", "sub-dir2");
        snapshot2.takeSnapshot();
        snapshot2.toLocalFile(tempFile2);

        // compare the java snapshots
        snapshot1.compare(snapshot2);

        // do same compare, but load the snapshots from files
        FileSystemSnapshot snapshot1_fromFile = new FileSystemSnapshot("snap1_from_file");
        snapshot1_fromFile.loadFromLocalFile(null, tempFile1);
        FileSystemSnapshot snapshot2_fromFile = new FileSystemSnapshot("snap2_from_file");
        snapshot2_fromFile.loadFromLocalFile(null, tempFile2);

        snapshot2_fromFile.compare(snapshot1_fromFile);
    }

    @Test
    public void loadSnapshotAndSkipFileFromSecondSnapshot() {

        final String tempDir = getTempDir();
        final String tempFile1 = tempDir + "tmp1.xml";

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir3");
        snapshot1.takeSnapshot();
        snapshot1.toLocalFile(tempFile1);

        FileSystemSnapshot snapshot1_fromFile = new FileSystemSnapshot("snap1_from_file");
        snapshot1_fromFile.loadFromLocalFile(null, tempFile1);

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot2.skipFile("F1", "sub-dir1/file2.xml");
        snapshot2.takeSnapshot();

        try {
            snapshot2.compare(snapshot1_fromFile);
        } catch (FileSystemSnapshotException se) {
            verifyError(se, ".*File is present in [snap1_from_file] snapshot only.*sub-dir1/file2.xml.*");
        }
    }

    @Test
    public void loadSnapshotFromFile_TryToAddExistingDir() {

        final String tempDir = getTempDir();
        final String tempFile1 = tempDir + "tmp1.xml";

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir3");
        snapshot1.takeSnapshot();
        snapshot1.toLocalFile(tempFile1);

        FileSystemSnapshot snapshot1_fromFile = new FileSystemSnapshot("snap1_from_file");
        snapshot1_fromFile.loadFromLocalFile(null, tempFile1);

        try {
            snapshot1_fromFile.addDirectory("F1", FILES_ROOT + "dir300");
        } catch (FileSystemSnapshotException se) {
            verifyError(se,
                        ".*There is already a directory with alias 'F1' for snapshot 'snap1_from_file'.*");
        }
    }

    @Test
    public void skipSizeGlobally() {

        // when the files have different sizes, they will have different MD5.
        // so globally disable checking the file MD5 as we do not want to deal with it in this test
        configurator.snapshots.setCheckFileMd5(false);

        // globally disable checking the file size
        configurator.snapshots.setCheckFileSize(false);

        // TEST1: One pair of files have different sizes, but we have disabled the check, so no
        // error will be thrown when comparing
        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "size/sub-dir1");
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "size/sub-dir2");
        snapshot2.takeSnapshot();

        snapshot1.compare(snapshot2);

        // globally enable checking the file size
        configurator.snapshots.setCheckFileSize(true);

        // TEST2: Now we will do the check and error must be registered
        FileSystemSnapshot snapshot3 = new FileSystemSnapshot("snap1");
        snapshot3.addDirectory("F1", FILES_ROOT + "size/sub-dir1");
        snapshot3.takeSnapshot();

        FileSystemSnapshot snapshot4 = new FileSystemSnapshot("snap2");
        snapshot4.addDirectory("F1", FILES_ROOT + "size/sub-dir2");
        snapshot4.takeSnapshot();

        try {
            snapshot3.compare(snapshot4);
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se, ".*Size: .*");
        }
    }

    @Test
    public void skipSizeGloballyButCheckOneParticularFile() {

        // when the files have different sizes, they will have different MD5.
        // so globally disable checking the file MD5 as we do not want to deal with it in this test
        configurator.snapshots.setCheckFileMd5(false);

        // globally disable checking the file size
        configurator.snapshots.setCheckFileSize(false);

        // TEST1: One pair of files have different sizes, but we have disabled the check, so no
        // error will be thrown when comparing
        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "size/sub-dir1");
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "size/sub-dir2");
        snapshot2.takeSnapshot();

        snapshot1.compare(snapshot2);

        // TEST2: Now we explicitly request check on the problematic pair of files
        // and this overwrites the global setting
        FileSystemSnapshot snapshot3 = new FileSystemSnapshot("snap1");
        snapshot3.addDirectory("F1", FILES_ROOT + "size/sub-dir1");
        snapshot3.takeSnapshot();

        FileSystemSnapshot snapshot4 = new FileSystemSnapshot("snap2");
        snapshot4.addDirectory("F1", FILES_ROOT + "size/sub-dir2");
        snapshot4.checkFile("F1", "file2.xml");
        snapshot4.takeSnapshot();

        try {
            snapshot3.compare(snapshot4);
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se, ".*MD5 checksum: .*");
        }
    }

    @Test
    public void skipModificationTimeGlobally() {

        // globally disable checking the modification time
        configurator.snapshots.setCheckModificationTime(false);

        // TEST1: One pair of files have different modification times, but we have disabled the check, so no
        // error will be thrown when comparing
        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "modification_time/sub-dir1");
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "modification_time/sub-dir2");
        snapshot2.takeSnapshot();

        snapshot1.compare(snapshot2);

        // globally enable checking the modification time
        configurator.snapshots.setCheckModificationTime(true);

        // TEST2: Now we will do the check and error must be registered
        FileSystemSnapshot snapshot3 = new FileSystemSnapshot("snap1");
        snapshot3.addDirectory("F1", FILES_ROOT + "modification_time/sub-dir1");
        snapshot3.takeSnapshot();

        FileSystemSnapshot snapshot4 = new FileSystemSnapshot("snap2");
        snapshot4.addDirectory("F1", FILES_ROOT + "modification_time/sub-dir2");
        snapshot4.takeSnapshot();

        try {
            snapshot3.compare(snapshot4);
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se, ".*Modification time: .*");
        }
    }

    @Test
    public void skipModificationTimeGloballyButCheckOneParticularFile() {

        // globally disable checking the modification time
        configurator.snapshots.setCheckModificationTime(false);

        // TEST1: One pair of files have different modification times, but we have disabled the check, so no
        // error will be thrown when comparing
        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "modification_time/sub-dir1");
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "modification_time/sub-dir2");
        snapshot2.takeSnapshot();

        snapshot1.compare(snapshot2);

        // TEST2: Now we explicitly request check on the problematic pair of files
        // and this overwrites the global setting
        FileSystemSnapshot snapshot3 = new FileSystemSnapshot("snap1");
        snapshot3.addDirectory("F1", FILES_ROOT + "modification_time/sub-dir1");
        snapshot3.checkFile("F1", "file2.xml", FileSystemSnapshot.CHECK_FILE_MODIFICATION_TIME);
        snapshot3.takeSnapshot();

        FileSystemSnapshot snapshot4 = new FileSystemSnapshot("snap2");
        snapshot4.addDirectory("F1", FILES_ROOT + "modification_time/sub-dir2");
        snapshot4.takeSnapshot();

        try {
            snapshot3.compare(snapshot4);
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se, ".*Modification time: .*");
        }
    }

    @Test
    public void skipMd5Globally() {

        // globally disable checking the file MD5
        configurator.snapshots.setCheckFileMd5(false);

        // TEST1: One pair of files have different MD5, but we have disabled the check, so no
        // error will be thrown when comparing
        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "md5/sub-dir1");
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "md5/sub-dir2");
        snapshot2.takeSnapshot();

        snapshot1.compare(snapshot2);

        // globally enable checking the file MD5
        configurator.snapshots.setCheckFileMd5(true);

        // TEST2: Now we will do the check and error must be registered
        FileSystemSnapshot snapshot3 = new FileSystemSnapshot("snap1");
        snapshot3.addDirectory("F1", FILES_ROOT + "md5/sub-dir1");
        snapshot3.takeSnapshot();

        FileSystemSnapshot snapshot4 = new FileSystemSnapshot("snap2");
        snapshot4.addDirectory("F1", FILES_ROOT + "md5/sub-dir2");
        snapshot4.takeSnapshot();

        try {
            snapshot3.compare(snapshot4);
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se, ".*MD5 checksum: .*");
        }
    }

    @Test
    public void skipMd5GloballyButCheckOneParticularFile() {

        // globally disable checking the file MD5
        configurator.snapshots.setCheckFileMd5(false);

        // TEST1: One pair of files have MD5, but we have disabled the check, so no
        // error will be thrown when comparing
        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "md5/sub-dir1");
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "md5/sub-dir2");
        snapshot2.takeSnapshot();

        snapshot1.compare(snapshot2);

        // TEST2: Now we explicitly request check on the problematic pair of files
        // and this overwrites the global setting
        FileSystemSnapshot snapshot3 = new FileSystemSnapshot("snap1");
        snapshot3.addDirectory("F1", FILES_ROOT + "md5/sub-dir1");
        snapshot3.takeSnapshot();

        FileSystemSnapshot snapshot4 = new FileSystemSnapshot("snap2");
        snapshot4.addDirectory("F1", FILES_ROOT + "md5/sub-dir2");
        snapshot4.checkFile("F1", "file2.xml", FileSystemSnapshot.CHECK_FILE_MD5,
                            FileSystemSnapshot.CHECK_FILE_SIZE);
        snapshot4.takeSnapshot();

        try {
            snapshot3.compare(snapshot4);
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se, ".*MD5 checksum: .*");
        }
    }

    @Test
    public void checkPermissions() {

        if (new LocalSystemOperations().getOperatingSystemType().isWindows()) {
            LogManager.getLogger(Test_FileSystemSnapshot.class)
                  .warn("We skip this test as it is not applicable for Windows OS");
            return;
        }

        // disable all checks
        configurator.snapshots.setCheckFileSize(false);
        configurator.snapshots.setCheckModificationTime(false);
        configurator.snapshots.setCheckFileMd5(false);
        configurator.snapshots.setCheckFilePermissions(false);

        // we work with 2 files only
        String firstFile = FILES_ROOT + "permissions/sub-dir1/file3.xml";
        String secondFile = FILES_ROOT + "permissions/sub-dir2/file3.xml";

        // remember the current permissions
        String firstPermissions = new LocalFileSystemOperations().getFilePermissions(firstFile);
        String secondPermissions = new LocalFileSystemOperations().getFilePermissions(secondFile);

        // make permissions different
        new LocalFileSystemOperations().setFilePermissions(firstFile, "333");
        new LocalFileSystemOperations().setFilePermissions(secondFile, "777");

        try {
            // TEST1: The pair of files have different permissions, but we have disabled the check, so no
            // error will be thrown when comparing
            FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
            snapshot1.addDirectory("F1", FILES_ROOT + "permissions/sub-dir1");
            snapshot1.takeSnapshot();

            FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
            snapshot2.addDirectory("F1", FILES_ROOT + "permissions/sub-dir2");
            snapshot2.takeSnapshot();

            snapshot1.compare(snapshot2);

            // globally enable checking the file permissions
            configurator.snapshots.setCheckFilePermissions(true);

            // TEST2: Now we will do the check and error must be registered
            FileSystemSnapshot snapshot3 = new FileSystemSnapshot("snap1");
            snapshot3.addDirectory("F1", FILES_ROOT + "permissions/sub-dir1");
            snapshot3.takeSnapshot();

            FileSystemSnapshot snapshot4 = new FileSystemSnapshot("snap2");
            snapshot4.addDirectory("F1", FILES_ROOT + "permissions/sub-dir2");
            snapshot4.takeSnapshot();

            try {
                snapshot3.compare(snapshot4); // expected exception - file permissions difference
                // log permissions
                LocalFileSystemOperations lfs = new LocalFileSystemOperations();
                System.err.println("Snapshots compare is expected to fail. Permissions dump:");
                System.err.println("Permissions for " + firstFile + ": "
                                   + lfs.getFilePermissions(firstFile));
                System.err.println("Permissions for " + secondFile + ": "
                                   + lfs.getFilePermissions(secondFile));
                thisShouldNotBeReached();
            } catch (FileSystemSnapshotException se) {
                verifyError(se, ".*Permissions: .*");
            }
        } finally {
            // restore the original permissions
            new LocalFileSystemOperations().setFilePermissions(firstFile, firstPermissions);
            new LocalFileSystemOperations().setFilePermissions(secondFile, secondPermissions);
        }
    }

    @Test
    public void takeSnapshot_notExistingDir() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot1.addDirectory("F2", FILES_ROOT + "not_existing_dir");

        try {
            snapshot1.takeSnapshot();
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se, "Directory.*not_existing_dir.*does not exist");
        }
    }

    @Test
    public void hiddenDir() {

        configurator.snapshots.setSupportHiddenFiles(true);

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "dir1_copy");
        snapshot2.takeSnapshot();

        try {
            snapshot1.compare(snapshot2);
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se, ".*Directory is present in [snap1] snapshot only.*dir1/\\.hidden_dir.*");
        }
    }

    @Test
    public void checkListOfEntitiesReturnedByCompareException() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "dir2");
        snapshot2.takeSnapshot();

        try {
            snapshot1.compare(snapshot2);
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException e) {
            // 1. directories in first snapshot only
            List<String> dirsFirstSnapshot = e.getDirectoriesPresentInOneSnapshotOnly("snap1");
            assertEquals(1, dirsFirstSnapshot.size());
            assertTrue(dirsFirstSnapshot.get(0).endsWith("/sub-dir1/"));

            // 2. directories in second snapshot only
            List<String> dirsSecondSnapshot = e.getDirectoriesPresentInOneSnapshotOnly("snap2");
            assertEquals(0, dirsSecondSnapshot.size());

            // 3. files in first snapshot only
            List<String> filesFirstSnapshot = e.getFilesPresentInOneSnapshotOnly("snap1");
            assertEquals(0, filesFirstSnapshot.size());

            // 4. files in second snapshot only
            List<String> filesSecondSnapshot = e.getFilesPresentInOneSnapshotOnly("snap2");
            List<String> expected = new ArrayList<String>();
            expected.add("file3.xml");
            expected.add("file4.xml");

            assertTrue(compareEntities(expected, filesSecondSnapshot, false));

            // 5. different files present in both snapshots
            List<String> differentFiles = e.getDifferentFilesPresentInBothSnapshots();
            assertEquals(2, differentFiles.size());
            for (String tokens : differentFiles) {

                String[] token = tokens.split("\n");
                List<String> real = new LinkedList<String>(Arrays.asList(token)
                                                                 .subList(1, token.length - 1));
                // we have 2 files with different properties, with different jdk versions we get the files
                // in different order. So we will get the right file by checking its length
                if (token.length == 2) {
                    assertTrue(token[0].trim().endsWith("file2.xml\":"));
                    expected.add("Modification time:");
                    assertTrue(compareEntities(expected, real, true));
                } else if (token.length == 4) {
                    assertTrue(token[0].trim().endsWith("file1.xml\":"));
                    expected.clear();
                    expected.add("MD5 checksum:");
                    expected.add("Modification time:");
                    assertTrue(compareEntities(expected, real, true));
                }
            }
        }
    }

    /**
     * @param expectedEntities a list with the expected entities
     * @param realEntities a list with the entities we will check
     * @param searchFromStart set to true if we will look for the expected entity from the beginning
     *          of the real entity, otherwise we will look for in at real entity`s end
     * @return true if expected entities and real entities are equal
     */
    private boolean compareEntities( List<String> expectedEntities, List<String> realEntities,
                                     boolean searchFromStart ) {

        for (String expectedEntity : expectedEntities) {
            for (int i = 0; i < realEntities.size(); i++) {
                if (searchFromStart) {
                    if (realEntities.get(i).trim().startsWith(expectedEntity.trim())) {
                        realEntities.remove(i);
                        break;
                    } else if (i == realEntities.size() - 1) {
                        return false;
                    }
                } else {
                    if (realEntities.get(i).trim().endsWith(expectedEntity.trim())) {
                        realEntities.remove(i);
                        break;
                    } else if (i == realEntities.size() - 1) {
                        return false;
                    }
                }
            }
        }
        return realEntities.isEmpty();
    }

    /*
     * The following tests prefixed with 'minor' are not considered as very important.
     * They test mainly negative scenarios dealing with wrong data entered by the user.
     */
    @Test
    public void minor_nullSnapshotName() {

        try {
            new FileSystemSnapshot(null);
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se, "Invalid snapshot name 'null'");
        }
    }

    @Test
    public void minor_addDir_nullDirDirAlias() {

        try {
            new FileSystemSnapshot("snap1").addDirectory(null, FILES_ROOT + "dir1");
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se, "Invalid directory alias 'null'");
        }
    }

    @Test
    public void minor_addDir_nullDirPath() {

        try {
            new FileSystemSnapshot("snap1").addDirectory("F1", null);
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se, "Invalid directory path 'null'");
        }
    }

    @Test
    public void minor_addDir_alreadyExistingDirAlias() {

        try {
            FileSystemSnapshot snap1 = new FileSystemSnapshot("snap1");
            snap1.addDirectory("F1", FILES_ROOT + "dir1");
            snap1.addDirectory("F1", FILES_ROOT + "dir1");
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se, "There is already a directory with alias 'F1' for snapshot 'snap1'");
        }
    }

    @Test
    public void minor_skipDir_nonExistingDirAlias() {

        try {
            new FileSystemSnapshot("snap1").skipDirectory("non existing snapshot", FILES_ROOT + "dir1");
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se, "There is no directory snapshot with alias 'non existing snapshot'");
        }
    }

    @Test
    public void minor_skipDir_nullDirPath() {

        try {
            new FileSystemSnapshot("snap1").skipDirectory(null, FILES_ROOT + "dir1");
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se, "Invalid directory alias 'null'");
        }
    }

    @Test
    public void minor_skipDir_nullSnapsthoName() {

        try {
            new FileSystemSnapshot("snap1").skipDirectory("F1", null);
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se, "Invalid directory path 'null'");
        }
    }

    @Test
    public void minor_skipFile_nonExistingDirAlias() {

        try {
            new FileSystemSnapshot("snap1").skipFile("non existing snapshot",
                                                     FILES_ROOT + "dir1/sub-dir1/file2.xml");
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se, "There is no directory snapshot with alias 'non existing snapshot'");
        }
    }

    @Test
    public void minor_skipFile_nullDirPath() {

        try {
            new FileSystemSnapshot("snap1").skipFile(null, FILES_ROOT + "dir1/sub-dir1/file2.xml");
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se, "Invalid directory alias 'null'");
        }
    }

    @Test
    public void minor_skipFile_nullSnapsthoName() {

        try {
            new FileSystemSnapshot("snap1").skipFile("F1", null);
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se, "Invalid file path 'null'");
        }
    }

    @Test
    public void minor_checkFile_nonExistingDirAlias() {

        try {
            new FileSystemSnapshot("snap1").checkFile("non existing snapshot",
                                                      FILES_ROOT + "dir1/sub-dir1/file2.xml");
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se, "There is no directory snapshot with alias 'non existing snapshot'");
        }
    }

    @Test
    public void minor_checkFile_nullDirPath() {

        try {
            new FileSystemSnapshot("snap1").checkFile(null, FILES_ROOT + "dir1/sub-dir1/file2.xml");
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se, "Invalid directory alias 'null'");
        }
    }

    @Test
    public void minor_checkFile_nullSnapsthoName() {

        try {
            new FileSystemSnapshot("snap1").checkFile("F1", null);
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se, "Invalid file path 'null'");
        }
    }

    @Test
    public void minor_checkFile_notExistingFile() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot1.checkFile("F1", "sub-dir1/not_existing_file");
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap2");
        snapshot2.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot2.takeSnapshot();

        snapshot1.compare(snapshot2);
    }

    @Test
    public void minor_compareSnashotsWithSameNames() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        FileSystemSnapshot snapshot2 = new FileSystemSnapshot("snap1");

        try {
            snapshot1.compare(snapshot2);
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se, "You are trying to compare snapshots with same name.*");
        }
    }

    @Test
    public void minor_invalidFindRuleValue() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir1");

        try {
            snapshot1.skipFile("F1", "sub-dir1/file2.xml", -1000);
            thisShouldNotBeReached();
        } catch (FileSystemSnapshotException se) {
            verifyError(se, "Invalid FIND RULE.*Please use one of the public .* constants");
        }
    }

    @Test
    public void minor_toStringMethod() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot("snap1");
        snapshot1.addDirectory("F1", FILES_ROOT + "dir1");
        snapshot1.skipDirectory("F1", "sub-dir1");
        snapshot1.skipFile("F1", "file1.xml");
        snapshot1.takeSnapshot();

        snapshot1.toString();
    }

    private void verifyError( FileSystemSnapshotException se, String expected ) {

        expected = "(?s)" + expected.replace("]", "\\]").replace("[", "\\[");

        assertTrue("The actual exception message \"" + se.getMessage()
                   + "\" doesn't match the expected one \"" + expected + "\"",
                   se.getMessage().matches(expected));

    }

    private String getTempDir() {

        final String tmpDir = TMP_FILES_ROOT + "dir" + (tmpDirCounter++) + "/";

        new File(tmpDir).mkdir();
        sleep(50);

        return tmpDir;
    }

    private static void sleep( long millis ) {

        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {}
    }

    private void thisShouldNotBeReached() {

        throw new IllegalStateException("This is not expected");
    }
}
