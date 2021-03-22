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
package com.axway.ats.environment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.core.utils.IoUtils;

/**
 * This the base class for environment units - an environment unit can be any
 * piece of the environment - e.g. file, database, etc.
 */
@PublicAtsApi
public abstract class EnvironmentUnit {

    private final Logger           log;

    private String                 tempBackupDir;

    private List<AdditionalAction> actions;

    /**
     * Constructor
     */
    public EnvironmentUnit() {

        this(new ArrayList<AdditionalAction>());
    }

    /**
     * Constructor
     *
     * @param actions list of additional actions to execute after the environment
     * unit is restored
     */
    public EnvironmentUnit( List<AdditionalAction> actions ) {

        this.actions = actions;
        this.log = LogManager.getLogger(this.getClass());
    }

    /**
     * Add actions that will be executed as part of the restore process
     *
     * @param additionalActions
     */
    @PublicAtsApi
    public final void addAdditionalActions( List<AdditionalAction> additionalActions ) {

        if (additionalActions.size() > 0) {
            actions.addAll(additionalActions);
        }
    }

    /**
     * @return the actions that will be executed as part of the restore process
     */
    public final List<AdditionalAction> getAdditionalActions() {

        return actions;
    }

    /**
     * Backup the environment unit
     *
     * @throws EnvironmentCleanupException
     */
    @PublicAtsApi
    public abstract void backup() throws EnvironmentCleanupException;

    /**
     * Restores the environment unit if needed.
     *
     * Any additional actions are scheduled for later executions.
     * @return true if restore is performed and false if not needed.
     */
    @PublicAtsApi
    public final boolean restore() throws EnvironmentCleanupException {

        try {
            // restore the environment unit if needed
            if (executeRestoreIfNecessary()) {
                // it was restored

                // schedule any additional actions for execution
                for (AdditionalAction action : actions) {
                    AdditionalActionsQueue.getInstance().addActionToQueue(action, this.getDescription());
                }
                log.info("Successfully restored " + getDescription());
                return true;
            } else {
                log.debug("No need to restore " + getDescription());
                return false;
            }
        } finally {
            setTempBackupDir(null);
        }
    }

    protected abstract boolean executeRestoreIfNecessary() throws EnvironmentCleanupException;

    protected abstract String getDescription();

    protected void createDirIfNotExist( String fileName ) throws EnvironmentCleanupException {

        String filePath = IoUtils.getFilePath(fileName);

        File directory = new File(filePath);
        if (directory != null && !directory.exists()) {
            if (directory.mkdirs()) {
                log.info("Directory was created: \"" + filePath + "\".");
            } else {
                throw new EnvironmentCleanupException("Could not create directory: "
                                                      + getFileCanonicalPath(directory));
            }
        }
    }

    /**
     * Returns file/directory canonical path
     * We are using canonical, not absolute path, because in Windows OS as absolute name we got
     * DOS names (uses an Eight Dot Three file naming) e.g. C:\Docume~1\...
     * @param file - a file or directory
     * @return canonical file/directory path
     * @throws EnvironmentCleanupException if there is IO Exception during getting the canonical path
     */
    protected String getFileCanonicalPath( File file ) throws EnvironmentCleanupException {

        try {

            return file.getCanonicalPath();
        } catch (IOException e) {

            throw new EnvironmentCleanupException("Can't get file canonical path", e);
        }
    }

    /**
     * Not public
     * @param tempBackupDir
     * @throws EnvironmentCleanupException if canonical path could not be retrieved
     */
    public void setTempBackupDir( String tempBackupDir ) throws EnvironmentCleanupException {

        if (tempBackupDir != null) {
            if (OperatingSystemType.getCurrentOsType().isWindows()) {
                // unify dir. paths to compare them as strings. 
                // getCanonicalPath() retunrs logical drive letter (Win) in upper case
                tempBackupDir = getFileCanonicalPath(new File(tempBackupDir));
            }
            this.tempBackupDir = IoUtils.normalizeDirPath(tempBackupDir);
        } else {
            this.tempBackupDir = null;
        }
    }

    protected String getTempBackupDir() {

        return tempBackupDir;
    }

}
