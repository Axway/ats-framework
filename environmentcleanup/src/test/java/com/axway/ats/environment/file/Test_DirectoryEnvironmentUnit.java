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
package com.axway.ats.environment.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.environment.BaseTest;
import com.axway.ats.environment.EnvironmentCleanupException;
import com.axway.ats.environment.EnvironmentUnit;

public class Test_DirectoryEnvironmentUnit extends BaseTest {

    private static final Logger log = LogManager.getLogger(Test_DirectoryEnvironmentUnit.class);
    public static String        restoreDirPath;
    public static String        backupDirPath;
    public static String        backupDirName;
    public static String        tempBackupDirName;                                              // destination folder for backups

    @BeforeClass
    public static void setUpTest_FileEnvironmentUnit() throws IOException {

        backupDirPath = Test_DirectoryEnvironmentUnit.class.getResource("dir_backup").getFile();
        // here backupDirPath under Maven run is with lower case of logical drive letter (Win).
        // It should be corrected in DirectoryUnit:  backupDirPath = new File(backupDirPath).getCanonicalPath();
        backupDirPath = IoUtils.normalizeDirPath(new File(backupDirPath).getParent());

        backupDirName = "dir_backup";
        tempBackupDirName = "temp_dir_backup";

        restoreDirPath = IoUtils.normalizeDirPath(backupDirPath + "dir_to_restore");

        // create an empty folder required by some of the tests
        new File(backupDirPath + backupDirName + "/emptydir").mkdir();
    }

    @Before
    public void setUp() throws Exception {

        //remove the backup dir if exists
        deleteFolder(new File(restoreDirPath));
    }

    @Test
    public void backupPositive() throws EnvironmentCleanupException, IOException {

        log.debug("backupDirPath: " + backupDirPath);
        String originalDir = IoUtils.normalizeDirPath(backupDirPath + backupDirName);
        log.debug("originalDir: " + originalDir);
        EnvironmentUnit dirEnvUnit = new DirectoryEnvironmentUnit(originalDir,
                                                                  backupDirPath,
                                                                  tempBackupDirName);
        dirEnvUnit.backup();

        // verify the content of the backup folder
        /*
         *  TODO: the check here will not be good if we change the folder we create backup from
         *  We should make the check independent to particular folder content
         */

        String tempBackupDir = IoUtils.normalizeDirPath(backupDirPath + tempBackupDirName);

        log.debug("tempBackupDir: " + tempBackupDir);
        assertTrue(new File(tempBackupDir).exists());
        assertEquals(3, new File(tempBackupDir).listFiles().length);

        String file1 = tempBackupDir + "file1.txt";
        assertTrue(new File(file1).exists());
        assertEquals(new File(file1).length(), new File(tempBackupDir + "file1.txt").length());

        String emptyDir = IoUtils.normalizeDirPath(tempBackupDir + "emptydir/");
        LogManager.getLogger(Test_DirectoryEnvironmentUnit.class)
                                   .error("emptydir='" + emptyDir + "'");
        assertTrue(new File(emptyDir).exists());

        assertEquals(new File(emptyDir).listFiles().length, 0);

        String subDir = IoUtils.normalizeDirPath(tempBackupDir + "subdir/");
        assertTrue(new File(subDir).exists());
        assertEquals(new File(subDir).listFiles().length, 1);

        String file2 = subDir + "file2.txt";
        assertTrue(new File(file2).exists());
        assertEquals(new File(file2).length(), new File(tempBackupDir + "subdir/file2.txt").length());
    }

    @Test
    public void backupNegative_noOriginalDir() throws EnvironmentCleanupException, IOException {

        String originalDir = IoUtils.normalizeDirPath(backupDirPath + "fakeDirName");
        EnvironmentUnit dirEnvUnit = new DirectoryEnvironmentUnit(originalDir,
                                                                  backupDirPath,
                                                                  "backupFakeDirName");
        dirEnvUnit.backup();
        // check if the backup is really skipped
        assertFalse(new File(IoUtils.normalizeDirPath(backupDirPath + "backupFakeDirName")).exists());
    }

    @Test
    public void restore() throws EnvironmentCleanupException {

        EnvironmentUnit dirEnvUnit = new DirectoryEnvironmentUnit(restoreDirPath,
                                                                  backupDirPath,
                                                                  backupDirName);
        Assert.assertTrue(dirEnvUnit.restore());
    }

    @Test
    public void restoreNotNeeded() throws EnvironmentCleanupException {

        EnvironmentUnit dirEnvUnit = new DirectoryEnvironmentUnit(restoreDirPath,
                                                                  backupDirPath,
                                                                  backupDirName);
        Assert.assertTrue(dirEnvUnit.restore());

        // now the restore is not needed
        Assert.assertFalse(dirEnvUnit.restore());
    }

    @Test
    public void restore_aNewFileMustBeDeleted() throws EnvironmentCleanupException, IOException {

        EnvironmentUnit dirEnvUnit = new DirectoryEnvironmentUnit(restoreDirPath,
                                                                  backupDirPath,
                                                                  backupDirName);
        Assert.assertTrue(dirEnvUnit.restore());

        new File(restoreDirPath + "newFile.txt").createNewFile();

        // now the restore is not needed
        Assert.assertTrue(dirEnvUnit.restore());
    }

    @Test
    public void restore_aNewSubdirMustBeDeleted() throws EnvironmentCleanupException, IOException {

        EnvironmentUnit dirEnvUnit = new DirectoryEnvironmentUnit(restoreDirPath,
                                                                  backupDirPath,
                                                                  backupDirName);
        Assert.assertTrue(dirEnvUnit.restore());

        new File(restoreDirPath + "new_sub_dir").mkdir();

        // now the restore is not needed
        Assert.assertTrue(dirEnvUnit.restore());
    }

    @Test( expected = EnvironmentCleanupException.class)
    public void restore_originalDirectoryIsFile() throws EnvironmentCleanupException, IOException {

        EnvironmentUnit dirEnvUnit = new DirectoryEnvironmentUnit(restoreDirPath,
                                                                  backupDirPath,
                                                                  backupDirName);

        new File(restoreDirPath).createNewFile();

        dirEnvUnit.restore();
        //        Assert.assertTrue( dirEnvUnit.restore() );
        //
        //        FileEnvironmentUnit fileEnvUnit = new FileEnvironmentUnit( IoUtils.getFilePath( originalFileName ),
        //                                                                   backupFileName );
        //        assertTrue( fileEnvUnit.restore() );
    }

    @Test
    public void useTempBackupDir() throws Exception {

        String originalDir = IoUtils.normalizeDirPath(backupDirPath + backupDirName);
        DirectoryEnvironmentUnit dirEnvUnit = new DirectoryEnvironmentUnit(originalDir,
                                                                           backupDirPath,
                                                                           tempBackupDirName);

        String tmpBackupDir = IoUtils.normalizeDirPath(backupDirPath) + "tmpBackupDir/";

        dirEnvUnit.setTempBackupDir(tmpBackupDir);
        assertNotNull(getTempBackupDir(dirEnvUnit));
        dirEnvUnit.backup();
        assertNull(getTempBackupDir(dirEnvUnit));

        dirEnvUnit.setTempBackupDir(tmpBackupDir);
        assertNotNull(getTempBackupDir(dirEnvUnit));
        assertFalse(dirEnvUnit.restore());
        assertNull(getTempBackupDir(dirEnvUnit));
    }
}
