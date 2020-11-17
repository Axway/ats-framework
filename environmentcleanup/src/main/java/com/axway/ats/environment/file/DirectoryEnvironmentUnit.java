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
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.environment.EnvironmentCleanupException;
import com.axway.ats.environment.EnvironmentUnit;

/**
 * Directory environment unit implementation - this environment unit represents
 * a directory on the file system. Backup and restore can be executed for whole files and directories recursively.
 * Any additional actions can be executed after the restore as well
 */
@PublicAtsApi
public class DirectoryEnvironmentUnit extends EnvironmentUnit {

    private static final Logger log = LogManager.getLogger(DirectoryEnvironmentUnit.class);

    // directory to be processed. Source for backup or destination for restore
    private String              origDirName;
    private String              backupDirPath;
    private String              backupDirName;

    //the environment unit description
    private final String        description;

    private boolean             restored;

    /**
     * Construct directory unit used for backup or restore
     * @param origDirName the source directory to be backed up or destination one for restore
     * @param backupDirPath the target parent directory path
     * @param backupDirName the directory name of the backup. New name of {@link #origDirName}
     */
    @PublicAtsApi
    public DirectoryEnvironmentUnit( String origDirName,
                                     String backupDirPath,
                                     String backupDirName ) {

        super();

        if (OperatingSystemType.getCurrentOsType().isWindows()) {
            try {
                // unify dir. paths in order to compare them as strings
                // as it is used in our implementation to cycle files
                // getCanonicalPath() is used to convert drive letter (Win) to upper case
                origDirName = new File(origDirName).getCanonicalPath();
                backupDirPath = new File(backupDirPath).getCanonicalPath();
            } catch (IOException ex) { // usually should not happen
                throw new IllegalStateException("Error while trying to get canonical "
                                                + "paths of original or backup folder", ex);
            }
        }
        this.origDirName = IoUtils.normalizeDirPath(origDirName); // add trailing slash too
        this.backupDirPath = IoUtils.normalizeDirPath(backupDirPath);
        this.backupDirName = backupDirName;
        this.description = "directory " + origDirName;
    }

    @Override
    @PublicAtsApi
    public void backup() throws EnvironmentCleanupException {

        try {

            File origDir = new File(origDirName);
            if (origDir.isDirectory()) {

                backupAllFilesInDirectory(origDir);
            } else {

                log.warn("Directory with name '" + origDirName
                         + "' does not exist so backup for it will be skipped.");
            }
        } finally {
            setTempBackupDir(null);
        }
    }

    private void backupAllFilesInDirectory(
                                            File origDir ) throws EnvironmentCleanupException {

        File[] files = origDir.listFiles();
        if (files == null) {
            throw new EnvironmentCleanupException("No such directory '" + origDir + "'.");
        }
        for (File file : files) {
            if (file.isDirectory()) {

                backupAllFilesInDirectory(file);
            } else {

                String backupFileName = IoUtils.normalizeFilePath(getFileCanonicalPath(file));
                // important that case matches incl. drive letter so origDirName and backupDir
                // should also come after getCanonicalPath() invocation
                backupFileName = backupFileName.replace(origDirName, getBackupDir());
                new FileEnvironmentUnit(getFileCanonicalPath(file),
                                        IoUtils.getFilePath(backupFileName),
                                        file.getName()).backup();
            }
        }
        // if the directory is empty - create the new empty directory in the backup folder
        if (files.length == 0) {
            String backupDir = IoUtils.normalizeDirPath(getFileCanonicalPath(origDir))
                                      .replace(origDirName, getBackupDir());
            new File(backupDir).mkdirs();
        }
    }

    private void updateRestoredFlag(
                                     boolean restored ) {

        if (!this.restored) {
            this.restored = restored;
        }
    }

    private void restoreAllFilesInDirectory(
                                             File originalDir,
                                             Set<String> fileAndDirectoryPaths )
                                                                                 throws EnvironmentCleanupException {

        File[] files = originalDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {

                    restoreAllFilesInDirectory(file, fileAndDirectoryPaths);
                } else {

                    String backupFilePath = IoUtils.normalizeFilePath(getFileCanonicalPath(file))
                                                   .replace(origDirName, getBackupDir());
                    if (fileAndDirectoryPaths.contains(backupFilePath)) {

                        boolean fileRestored = new FileEnvironmentUnit(getFileCanonicalPath(file),
                                                                       IoUtils.getFilePath(backupFilePath),
                                                                       file.getName()).restore();
                        updateRestoredFlag(fileRestored);

                        // remove from the backup files and directories index
                        fileAndDirectoryPaths.remove(backupFilePath);
                    } else {
                        // the file is new and missing from the backup directory => delete it
                        if (file.delete()) {
                            updateRestoredFlag(true);
                            log.info("File " + getFileCanonicalPath(file) + " is deleted.");
                        } else {
                            throw new EnvironmentCleanupException("File " + getFileCanonicalPath(file)
                                                                  + " must be removed, but the delete operation fails.");
                        }
                    }
                }
            }
        }

        // we have to delete all new created directories, which are missing in the backup directory
        String backupDirPath = IoUtils.normalizeDirPath(getFileCanonicalPath(originalDir))
                                      .replace(origDirName, getBackupDir());
        if (fileAndDirectoryPaths.contains(backupDirPath)) {

            // remove from the backup files and directories index
            fileAndDirectoryPaths.remove(backupDirPath);

        } else if (!backupDirPath.equals(getBackupDir())) { // we must not delete the original root folder

            if (originalDir.delete()) {
                updateRestoredFlag(true);
                log.info("Directory " + getFileCanonicalPath(originalDir) + " is deleted.");
            } else {
                // TODO delete
                log.error("Directory " + getFileCanonicalPath(originalDir)
                          + " must be removed, but the delete operation fails. Details follow");
                log.error("Exists: " + originalDir.exists());
                log.error("Is directory: " + originalDir.isDirectory());
                log.error("Is file: " + originalDir.isFile());
                throw new EnvironmentCleanupException("Directory " + getFileCanonicalPath(originalDir)
                                                      + " must be removed, but the delete operation fails.");
            }
        }
    }

    @Override
    protected boolean executeRestoreIfNecessary() throws EnvironmentCleanupException {

        // reset the restored flag
        this.restored = false;

        File backupDir = new File(getBackupDir());

        if (backupDir.isDirectory()) {

            File origDir = new File(origDirName);
            if (origDir.exists() && !origDir.isDirectory()) {

                throw new EnvironmentCleanupException("'" + origDirName + "' exists, but is not a directory");
            } else if (!origDir.exists()) {

                if (origDir.mkdirs()) {
                    updateRestoredFlag(true);
                    log.debug("Created restore folder '" + origDirName + "'");
                } else {
                    log.warn("Can't create folder '" + origDirName + "'");
                }
            }

            Set<String> fileAndDirectoryPaths = getFileAndDirectoryPathsIndex(backupDir);
            restoreAllFilesInDirectory(origDir, fileAndDirectoryPaths);

            // here in fileAndDirectoryPaths we have left only entries in original folder which are deleted after test run.
            // We should restore them
            for (String backupFilePath : fileAndDirectoryPaths) {
                String originalFilePath = backupFilePath.replace(getBackupDir(), origDirName);
                if (originalFilePath.endsWith("/") || originalFilePath.endsWith("\\")) {
                    File dir = new File(originalFilePath);
                    if (!dir.exists() && !dir.mkdirs()) {
                        log.warn("Can't create folder '" + originalFilePath + "'");
                    }
                } else {
                    boolean fileRestored = new FileEnvironmentUnit(originalFilePath,
                                                                   IoUtils.getFilePath(backupFilePath),
                                                                   IoUtils.getFileName(backupFilePath)).restore();
                    updateRestoredFlag(fileRestored);
                }
            }
        }

        return this.restored;
    }

    private Set<String> getFileAndDirectoryPathsIndex(
                                                       File dir ) throws EnvironmentCleanupException {

        Set<String> fileAndDirectoryPaths = new HashSet<String>();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {

                    fileAndDirectoryPaths.add(IoUtils.normalizeDirPath(getFileCanonicalPath(file)));
                    fileAndDirectoryPaths.addAll(getFileAndDirectoryPathsIndex(file));
                } else {
                    fileAndDirectoryPaths.add(IoUtils.normalizeFilePath(getFileCanonicalPath(file)));
                }
            }
        }
        return fileAndDirectoryPaths;
    }

    @Override
    protected String getDescription() {

        return description;
    }

    private String getBackupDir() {

        String tempBackupDir = getTempBackupDir();
        if (tempBackupDir != null) {
            return IoUtils.normalizeDirPath(tempBackupDir + backupDirName);
        }
        return IoUtils.normalizeDirPath(backupDirPath + backupDirName);
    }

    public EnvironmentUnit getNewCopy() {

        return new DirectoryEnvironmentUnit(this.origDirName, this.backupDirPath, this.backupDirName);
    }
}
