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
package com.axway.ats.environment.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.dbaccess.DatabaseProviderFactory;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.environment.EnvironmentCleanupException;
import com.axway.ats.environment.EnvironmentUnit;
import com.axway.ats.environment.database.exceptions.DatabaseEnvironmentCleanupException;
import com.axway.ats.environment.database.model.BackupHandler;
import com.axway.ats.environment.database.model.DbTable;
import com.axway.ats.environment.database.model.RestoreHandler;
import com.axway.ats.harness.config.TestBox;

/**
 * Database environment unit implementation - this environment unit will backup/restore
 * a database or a set of tables
 */
@PublicAtsApi
public class DatabaseEnvironmentUnit extends EnvironmentUnit {

    private static final Logger       log = LogManager.getLogger(DatabaseEnvironmentUnit.class);

    private EnvironmentHandlerFactory environmentHandlerFactory;

    private DbConnection              dbConnection;
    private List<DbTable>             dbTables;

    private boolean                   addSeparateLocks;
    private boolean                   dropTables;
    private boolean                   skipTablesContent;

    private boolean                   disableForeignKeys;
    private boolean                   includeDeleteStatements;

    private String                    backupDirPath;
    private String                    backupFileName;

    //the environment unit description
    private String                    description;

    /**
     * Constructor
     *
     * @param backupDirPath target folder
     * @param backupFileName the name of the backup file
     * @param testBox {@link TestBox} with all necessary credentials for connection
     * @param customProperties map with custom properties
     * @param dbTables list of database tables to backup
     */
    @PublicAtsApi
    public DatabaseEnvironmentUnit( String backupDirPath,
                                    String backupFileName,
                                    TestBox testBox,
                                    Map<String, Object> customProperties,
                                    List<DbTable> dbTables ) {

        this(backupDirPath,
             backupFileName,
             DatabaseProviderFactory.getDatabaseProvider(testBox.getDbType(),
                                                         testBox.getHost(),
                                                         testBox.getDbName(),
                                                         testBox.getDbUser(),
                                                         testBox.getDbPass(),
                                                         testBox.getDbPort(),
                                                         customProperties)
                                    .getDbConnection(),
             dbTables);
    }

    /**
     * Constructor
     *
     * @param backupDirPath path to the directory where backup file will be created
     * @param backupFileName the name of the backup file
     * @param dbConnection database connection
     * @param dbTables list of database tables to backup
     */
    public DatabaseEnvironmentUnit( String backupDirPath, String backupFileName, DbConnection dbConnection,
                                    List<DbTable> dbTables ) {

        this.dbTables = dbTables;

        this.backupDirPath = IoUtils.normalizeDirPath(backupDirPath);
        this.backupFileName = backupFileName;
        this.addSeparateLocks = true;
        this.dropTables = false;
        this.skipTablesContent = false;
        this.disableForeignKeys = true;
        this.includeDeleteStatements = true;

        this.environmentHandlerFactory = EnvironmentHandlerFactory.getInstance();

        setDbConnection(dbConnection);
    }

    public DbConnection getDbConnection() {

        return dbConnection;
    }

    @PublicAtsApi
    public void setDbConnection( DbConnection dbConnection ) {

        this.dbConnection = dbConnection;
        this.description = dbConnection.getDbType() + " database in file ";
    }

    @Override
    @PublicAtsApi
    public void backup() throws EnvironmentCleanupException {

        BackupHandler dbBackup = null;
        try {
            log.info("Creating backup of environment unit " + getDescription() + "...");

            //create db backup handler instance
            dbBackup = environmentHandlerFactory.createDbBackupHandler(dbConnection);
            dbBackup.setLockTables(addSeparateLocks);
            dbBackup.setDropTables(dropTables);
            dbBackup.setSkipTablesContent(skipTablesContent);
            dbBackup.setForeignKeyCheck(disableForeignKeys);
            dbBackup.setIncludeDeleteStatements(includeDeleteStatements);
            for (DbTable dbTable : dbTables) {
                dbBackup.addTable(dbTable);
            }

            String backupFile = getBackupFile();
            createDirIfNotExist(backupFile);
            dbBackup.createBackup(backupFile);

            log.info("Successfully created backup of environment unit " + getDescription());
        } finally {
            setTempBackupDir(null);

            if (dbBackup != null) {
                dbBackup.disconnect();
            }
        }
    }

    @Override
    @PublicAtsApi
    public boolean executeRestoreIfNecessary() throws DatabaseEnvironmentCleanupException {

        //create db backup handler instance
        RestoreHandler dbRestore = null;
        try {
            dbRestore = environmentHandlerFactory.createDbRestoreHandler(dbConnection);

            dbRestore.restore(getBackupFile());
        } finally {
            if (dbRestore != null) {
                dbRestore.disconnect();
            }
        }

        // we always restore the database data
        return true;
    }

    @Override
    protected String getDescription() {

        return description + getBackupFile();
    }

    private String getBackupFile() {

        String tempBackupDir = getTempBackupDir();
        if (tempBackupDir != null) {
            return tempBackupDir + backupFileName;
        }
        return backupDirPath + backupFileName;
    }

    /**
     * Toggle whether to drop and recreate tables on restore. Default is <strong>false</strong>.
     * @param dropTables true will generate drop table statement before restore
     * */
    @PublicAtsApi
    public void setDropTables( boolean dropTables ) {

        this.dropTables = dropTables;
    }

    /**
     * Toggle whether to add the tables' content to the backup.
     * @param skipTablesContent - true will NOT add the content of the tables in the backup, false will do
     * */
    @PublicAtsApi
    public void setSkipTablesContent( boolean skipTablesContent ) {

        this.skipTablesContent = skipTablesContent;
    }

    public EnvironmentUnit getNewCopy() {

        DatabaseEnvironmentUnit newDatabaseEnvironmentUnit = new DatabaseEnvironmentUnit(this.backupDirPath,
                                                                                         this.backupFileName,
                                                                                         this.dbConnection, // we do not copy this object, seems to be constant
                                                                                         null);

        List<DbTable> newDbTables = new ArrayList<DbTable>();
        for (DbTable dbTable : dbTables) {
            newDbTables.add(dbTable.getNewCopy());
        }
        newDatabaseEnvironmentUnit.dbTables = newDbTables;
        newDatabaseEnvironmentUnit.dropTables = this.dropTables;
        newDatabaseEnvironmentUnit.skipTablesContent = this.skipTablesContent;

        return newDatabaseEnvironmentUnit;
    }
}
