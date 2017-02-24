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
package com.axway.ats.environment.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.environment.BaseTest;
import com.axway.ats.environment.EnvironmentCleanupException;
import com.axway.ats.environment.EnvironmentUnit;

public class Test_FileEnvironmentUnit extends BaseTest {

    public static String backupFolder;

    public static String originalFileName;
    public static String backupFileName;

    @BeforeClass
    public static void setUpTest_FileEnvironmentUnit() {

        originalFileName = Test_FileEnvironmentUnit.class.getResource( "orig_file.txt" ).getFile();
        backupFolder = IoUtils.getFilePath( originalFileName );
        backupFileName = "backup_file.txt";
    }

    @Before
    public void setUp() throws Exception {

        //remove the backup file if exists
        File backupFile = new File( backupFolder + backupFileName );
        if( backupFile.exists() ) {
            backupFile.delete();
        }

        //remove the test backup directory if exist
        File testBackupDir = new File( backupFolder + "agent_test_backup" );
        if( testBackupDir.exists() ) {
            deleteFolder( testBackupDir );
        }
    }

    @Test
    public void testBackupFolder_ifNotExist() throws EnvironmentCleanupException {

        String backupFileNameInDeepDir = backupFolder
                                         + "agent_test_backup/test_folder/other_folder/backup_file.txt";
        EnvironmentUnit fileEnvUnit = new FileEnvironmentUnit( originalFileName,
                                                               backupFolder
                                                                       + "agent_test_backup/test_folder/other_folder/",
                                                               "backup_file.txt" );
        fileEnvUnit.backup();

        //verify if the backup file exists on the right place
        File backupFile = new File( backupFileNameInDeepDir );
        Assert.assertTrue( backupFile.exists() );
    }

    @Test
    public void backupPositive() throws EnvironmentCleanupException, IOException {

        FileEnvironmentUnit fileEnvUnit = new FileEnvironmentUnit( originalFileName,
                                                                   backupFolder,
                                                                   backupFileName );
        fileEnvUnit.backup();

        //verify the files are the same
        assertEquals( new File( backupFolder + backupFileName ).length(),
                      new File( originalFileName ).length() );
    }

    @Test
    public void backupPositive_deleteWrongBackupFile() throws EnvironmentCleanupException, IOException {

        String originalFileNameChanged = originalFileName + "_changed";
        new File( originalFileNameChanged ).delete();

        String backupFileNameChanged = backupFileName + "_changed";
        File backupFileChanged = new File( backupFolder + backupFileNameChanged );
        backupFileChanged.createNewFile();

        FileEnvironmentUnit fileEnvUnit = new FileEnvironmentUnit( originalFileNameChanged,
                                                                   backupFolder,
                                                                   backupFileNameChanged );

        assertTrue( backupFileChanged.exists() );
        fileEnvUnit.backup();
        assertFalse( backupFileChanged.exists() );
    }

    @Test
    public void restorePositiveNoNeedToRestore() throws EnvironmentCleanupException, IOException {

        FileEnvironmentUnit fileEnvUnit = new FileEnvironmentUnit( originalFileName,
                                                                   backupFolder,
                                                                   backupFileName );
        fileEnvUnit.backup();
        assertFalse( fileEnvUnit.restore() );
    }

    @Test
    public void restorePositiveNoOriginalFile() throws EnvironmentCleanupException, IOException {

        FileEnvironmentUnit fileEnvUnitBackup = new FileEnvironmentUnit( originalFileName,
                                                                         backupFolder,
                                                                         backupFileName );
        fileEnvUnitBackup.backup();

        FileEnvironmentUnit fileEnvUnitRestore = new FileEnvironmentUnit( originalFileName + "1",
                                                                          backupFolder,
                                                                          backupFileName );
        assertTrue( fileEnvUnitRestore.restore() );
    }

    @Test
    public void restorePositiveNoBackupFile() throws EnvironmentCleanupException, IOException {

        try {
            FileEnvironmentUnit fileEnvUnit = new FileEnvironmentUnit( originalFileName,
                                                                       backupFolder,
                                                                       backupFileName + "1" );
            assertTrue( fileEnvUnit.restore() );
            assertTrue( !new File( originalFileName ).exists() );
        } finally {
            new File( originalFileName ).createNewFile();
        }
    }

    @Test
    public void restorePositiveFilesAreDifferent() throws Exception {

        FileEnvironmentUnit fileEnvUnit = new FileEnvironmentUnit( originalFileName,
                                                                   backupFolder,
                                                                   backupFileName );
        fileEnvUnit.backup();

        //1sec wait is needed to avoid getting same last modification times after the next file edit operation
        Thread.sleep( 1000 );

        //add one letter to the original file
        FileOutputStream originalFileStream = null;
        try {
            originalFileStream = new FileOutputStream( new File( originalFileName ) );
            originalFileStream.write( 65 );
            originalFileStream.flush();
        } finally {
            IoUtils.closeStream( originalFileStream );
        }

        assertTrue( fileEnvUnit.restore() );
    }

    @Test(expected = EnvironmentCleanupException.class)
    public void restoreNegativeOriginalFileIsDirectory() throws EnvironmentCleanupException {

        //this test requires original file directory modification date to be different from the original file date
        //therefore we will change the directory modification date with 1sec
        new File( IoUtils.getFilePath( originalFileName ) ).setLastModified( new File( originalFileName ).lastModified() - 1000 );

        FileEnvironmentUnit fileEnvUnitBackup = new FileEnvironmentUnit( originalFileName,
                                                                         backupFolder,
                                                                         backupFileName );
        fileEnvUnitBackup.backup();

        FileEnvironmentUnit fileEnvUnit = new FileEnvironmentUnit( IoUtils.getFilePath( originalFileName ),
                                                                   backupFolder,
                                                                   backupFileName );
        assertTrue( fileEnvUnit.restore() );
    }

    @Test
    public void useTempBackupDir() throws Exception {

        FileEnvironmentUnit fileEnvUnit = new FileEnvironmentUnit( originalFileName,
                                                                   backupFolder,
                                                                   backupFileName );

        String tmpBackupDir = IoUtils.normalizeDirPath( backupFolder ) + "tmpBackupDir/";

        fileEnvUnit.setTempBackupDir( tmpBackupDir );
        assertNotNull( getTempBackupDir( fileEnvUnit ) );
        fileEnvUnit.backup();
        assertNull( getTempBackupDir( fileEnvUnit ) );

        fileEnvUnit.setTempBackupDir( tmpBackupDir );
        assertNotNull( getTempBackupDir( fileEnvUnit ) );
        assertFalse( fileEnvUnit.restore() );
        assertNull( getTempBackupDir( fileEnvUnit ) );
    }

}
