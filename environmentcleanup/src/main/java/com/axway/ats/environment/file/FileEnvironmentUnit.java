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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.environment.EnvironmentCleanupException;
import com.axway.ats.environment.EnvironmentUnit;

/**
 * File environment unit implementation - this environment unit represents
 * a single file on the file system. Backup and restore can be executed for this file.
 * Any additional actions can be executed after the restore as well
 */
@PublicAtsApi
public class FileEnvironmentUnit extends EnvironmentUnit {

    private static final Logger log = LogManager.getLogger(FileEnvironmentUnit.class);

    private String              origFileName;
    private String              backupDirPath;
    private String              backupFileName;

    //the environment unit description
    private final String        description;

    @PublicAtsApi
    public FileEnvironmentUnit( String origFileName, String backupDirPath, String backupFileName ) {

        super();

        this.origFileName = origFileName;
        this.backupDirPath = IoUtils.normalizeDirPath(backupDirPath);
        this.backupFileName = backupFileName;
        this.description = "file " + origFileName;
    }

    @Override
    @PublicAtsApi
    public void backup() throws EnvironmentCleanupException {

        try {
            String backupFileAsString = getBackupFile();
            createDirIfNotExist(backupFileAsString);
            if (new File(origFileName).exists()) {

                copyFile(origFileName, backupFileAsString);

                // fix the modification date of the backup file
                File origFile = new File(origFileName);
                File backupFile = new File(backupFileAsString);
                backupFile.setLastModified(origFile.lastModified());
            } else if (new File(backupFileAsString).exists()) {

                if (!new File(backupFileAsString).delete()) {
                    throw new EnvironmentCleanupException("File " + backupFileAsString
                                                          + " must be removed, but the delete operation fails.");
                }
            }

        } catch (FileNotFoundException fnfe) {

            log.warn("Cannot backup file: " + origFileName + " Skipping it.", fnfe);
        } catch (IOException ioe) {

            throw new EnvironmentCleanupException("Could not backup file " + origFileName, ioe);
        } finally {
            setTempBackupDir(null);
        }
    }

    @Override
    protected boolean executeRestoreIfNecessary() throws EnvironmentCleanupException {

        if (isRestoreNecessary()) {

            String backupFileAsString = getBackupFile();
            try {
                File origFile = new File(origFileName);
                File backupFile = new File(backupFileAsString);

                if (origFile.exists() && origFile.isDirectory()) {

                    throw new EnvironmentCleanupException("File " + getFileCanonicalPath(origFile)
                                                          + " is actually a directory and can not be restored.");
                } else if (origFile.exists() && !backupFile.exists()) {

                    if (origFile.delete()) {
                        log.info("File " + getFileCanonicalPath(origFile) + " is deleted.");
                    } else {
                        throw new EnvironmentCleanupException("File " + getFileCanonicalPath(origFile)
                                                              + " must be removed, but the delete opeation fails.");
                    }
                } else if (backupFile.exists()) {

                    createDirIfNotExist(origFileName);
                    copyFile(backupFileAsString, origFileName);

                    // fix the modification date of the original file
                    origFile.setLastModified(backupFile.lastModified());
                }
            } catch (FileNotFoundException fnfe) {
                log.warn("Cannot restore from backup file: " + backupFileAsString + " Skipping it.", fnfe);
            } catch (IOException ioe) {
                throw new EnvironmentCleanupException("Could not restore file " + origFileName, ioe);
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean isRestoreNecessary() throws EnvironmentCleanupException {

        File origFile = new File(origFileName);
        File backupFile = new File(getBackupFile());

        boolean needRestore = (origFile.exists() && !backupFile.exists())
                              || (!origFile.exists() && backupFile.exists())
                              || (origFile.lastModified() != backupFile.lastModified());

        if (needRestore) {
            log.debug(getDescription() + " is not the same as backup - restoring...");
        }

        return needRestore;
    }

    @Override
    protected String getDescription() {

        return description;
    }

    /**
     * Copy file
     *
     * @param sourceFileName the source file name
     * @param destinationFileName the destination file name
     * @throws IOException on error
     */
    private void copyFile( String sourceFileName, String destinationFileName ) throws IOException {

        FileChannel srcChannel = null;
        FileChannel dstChannel = null;

        try {
            // Create channel on the source
            srcChannel = new FileInputStream(sourceFileName).getChannel();

            // Create channel on the destination
            dstChannel = new FileOutputStream(destinationFileName).getChannel();

            // Copy file contents from source to destination
            dstChannel.truncate(0);
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        } finally {
            // Close the channels
            IoUtils.closeStream(srcChannel);
            IoUtils.closeStream(dstChannel);
        }
    }

    private String getBackupFile() {

        String tempBackupDir = getTempBackupDir();
        if (tempBackupDir != null) {
            return tempBackupDir + backupFileName;
        }
        return backupDirPath + backupFileName;
    }

    public EnvironmentUnit getNewCopy() {

        return new FileEnvironmentUnit(this.origFileName, this.backupDirPath, this.backupFileName);
    }

}
