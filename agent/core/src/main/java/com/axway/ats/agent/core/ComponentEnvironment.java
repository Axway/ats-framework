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
package com.axway.ats.agent.core;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.environment.AdditionalActionsQueue;
import com.axway.ats.environment.EnvironmentCleanupException;
import com.axway.ats.environment.EnvironmentUnit;
import com.axway.ats.environment.database.DatabaseEnvironmentUnit;
import com.axway.ats.environment.file.DirectoryEnvironmentUnit;
import com.axway.ats.environment.file.FileEnvironmentUnit;

public class ComponentEnvironment {

    private static final Logger           log                   = LogManager.getLogger(ComponentEnvironment.class);

    private String                        componentName;
    private String                        environmentName;
    private List<EnvironmentUnit>         environmentUnits;
    private String                        backupFolder;

    private static final SimpleDateFormat BACKUP_DATE_FORMATTER = new SimpleDateFormat("_yyyy.MM.dd_HH.mm.ss");

    public ComponentEnvironment( String componentName, String environmentName,
                                 List<EnvironmentUnit> environmentUnits, String backupFolder ) {

        this.componentName = componentName;
        this.environmentName = environmentName;
        this.environmentUnits = environmentUnits;
        this.backupFolder = backupFolder;
    }

    public List<EnvironmentUnit> getEnvironmentUnits() {

        return environmentUnits;
    }

    public String getEnvironmentName() {

        return environmentName;
    }

    public void restore( String folderPath ) throws AgentException {

        // Restore all the environment units.
        // All additional actions are implicitly added to the Additional Actions Queue Instance
        try {
            for (EnvironmentUnit environmentUnit : environmentUnits) {
                environmentUnit.setTempBackupDir(folderPath);
                environmentUnit.restore();
            }
        } catch (EnvironmentCleanupException ece) {
            throw new AgentException("Could not restore environment for component '" + this.componentName
                                     + "'" + recurseCauses(ece), ece);
        }

        // Now execute the additional actions and clean the queue
        try {
            AdditionalActionsQueue.getInstance().flushAllActions();
        } catch (EnvironmentCleanupException ece) {
            throw new AgentException("Could not restore environment for component '" + this.componentName
                                     + "'" + recurseCauses(ece), ece);
        }
    }

    public void backup( String folderPath ) throws AgentException {

        log.info("Backuping environment for component " + componentName);
        try {

            String currentBackupFolder = backupFolder;
            if (folderPath != null) {
                currentBackupFolder = folderPath;
            }

            //if the current backup folder already exists and is not empty, we will rename it in order to have
            //a clean backup and to save the previous backup data
            File backupDir = new File(currentBackupFolder);
            if (backupDir.isDirectory() && backupDir.list().length > 0) {

                String backupFolderPath = currentBackupFolder;
                if (currentBackupFolder.endsWith("/") || currentBackupFolder.endsWith("\\")) {
                    backupFolderPath = currentBackupFolder.substring(0,
                                                                     currentBackupFolder.length() - 1);
                }
                backupFolderPath = backupFolderPath + BACKUP_DATE_FORMATTER.format(new Date());
                backupFolderPath = IoUtils.normalizeDirPath(backupFolderPath);
                log.info("In order to have a clean backup, we'll rename the current backup folder: "
                         + backupFolderPath);
                backupDir.renameTo(new File(backupFolderPath));
            }
            for (EnvironmentUnit environmentUnit : environmentUnits) {
                environmentUnit.setTempBackupDir(folderPath);
                environmentUnit.backup();
            }
        } catch (EnvironmentCleanupException ece) {
            throw new AgentException("Could not backup environment for component " + componentName
                                     + recurseCauses(ece), ece);
        }
    }

    public void backupOnlyIfNotAlreadyDone() throws AgentException {

        try {
            File backupDir = new File(backupFolder);
            String[] fileList = backupDir.list();
            if (backupDir.isDirectory() && fileList != null && fileList.length > 0) {

                log.info("Backup directory '" + backupDir.getAbsolutePath()
                         + "' already exists and the backup will be skipped.");
            } else if (backupDir.exists() && !backupDir.isDirectory()) {

                throw new AgentException("Could not create backup directory '" + backupDir.getAbsolutePath()
                                         + "'. File with this name already exists.");
            } else {

                log.info("Creating backup for component " + componentName);
                for (EnvironmentUnit environmentUnit : environmentUnits) {
                    environmentUnit.backup();
                }
            }
        } catch (EnvironmentCleanupException ece) {
            throw new AgentException("Could not backup environment for component '" + componentName + "'"
                                     + recurseCauses(ece), ece);
        }
    }

    // FIXME : we should probably find a way to transport the stack trace
    // this method would recursively go through the causes of this exception
    // and append their messages to the message of the outer exception
    private String recurseCauses( Throwable e ) {

        StringBuilder buffer = new StringBuilder();

        buffer.append(". CAUSE: ").append(e.getMessage());
        if (e.getCause() != null) {
            buffer.append(recurseCauses(e.getCause()));
        }

        return buffer.toString();
    }

    public ComponentEnvironment getNewCopy() {

        ComponentEnvironment newComponentEnvironment = new ComponentEnvironment(this.componentName,
                                                                                this.environmentName, null,
                                                                                this.backupFolder);

        List<EnvironmentUnit> newEnvironmentUnits = new ArrayList<EnvironmentUnit>();
        for (EnvironmentUnit environmentUnit : this.environmentUnits) {
            EnvironmentUnit newEnvironmentUnit;
            if (environmentUnit instanceof DatabaseEnvironmentUnit) {
                newEnvironmentUnit = ((DatabaseEnvironmentUnit) environmentUnit).getNewCopy();
            } else if (environmentUnit instanceof FileEnvironmentUnit) {
                newEnvironmentUnit = ((FileEnvironmentUnit) environmentUnit).getNewCopy();
            } else {
                // it is instance of DirectoryEnvironmentUnit
                newEnvironmentUnit = ((DirectoryEnvironmentUnit) environmentUnit).getNewCopy();
            }

            newEnvironmentUnits.add(newEnvironmentUnit);
        }
        newComponentEnvironment.environmentUnits = newEnvironmentUnits;

        return newComponentEnvironment;
    }
}
