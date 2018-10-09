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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.axway.ats.common.dbaccess.DbQuery;
import com.axway.ats.core.dbaccess.ColumnDescription;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.DbProvider;
import com.axway.ats.core.dbaccess.DbRecordValuesList;
import com.axway.ats.core.dbaccess.DbReturnModes;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.environment.database.exceptions.ColumnHasNoDefaultValueException;
import com.axway.ats.environment.database.exceptions.DatabaseEnvironmentCleanupException;
import com.axway.ats.environment.database.model.BackupHandler;
import com.axway.ats.environment.database.model.DbTable;
import com.axway.ats.environment.database.model.RestoreHandler;

/**
 * The {@link AbstractEnvironmentHandler} abstract class takes care that
 * of all the common operations that are not provider specific, calling
 * abstract methods wherever a provider specific operation is required.
 */
abstract class AbstractEnvironmentHandler implements BackupHandler, RestoreHandler {

    private static final Logger    log                        = Logger.getLogger(AbstractEnvironmentHandler.class);
    private static final String    ERROR_CREATING_BACKUP      = "Could not create backup in file ";
    protected static final String  ERROR_RESTORING_BACKUP     = "Could not restore backup from file ";
    private static final String    DAMAGED_BACKUP_FILE_SUFFIX = "_damaged";
    protected static final String  DROP_TABLE_MARKER          = " -- ATS DROP TABLE ";
    protected static final String  EOL_MARKER                 = " -- ATS EOL;";

    protected boolean              addLocks;
    protected boolean              disableForeignKeys;
    protected boolean              includeDeleteStatements;
    protected Map<String, DbTable> dbTables;
    protected DbConnection         dbConnection;
    protected DbProvider           dbProvider;
    // whether the delete statements are already written to file
    protected boolean              deleteStatementsInserted;
    protected boolean              dropEntireTable;


    /**
     * Constructor
     */
    public AbstractEnvironmentHandler( DbConnection dbConnection,
                                       DbProvider dbProvider ) {

        this.dbConnection = dbConnection;
        this.dbProvider = dbProvider;
        this.addLocks = true;
        this.disableForeignKeys = true;
        this.includeDeleteStatements = true;
        this.dbTables = new LinkedHashMap<>();
    }

    /**
     * Create the database backup for the selected tables
     *
     * @param backupFileName                        the name of the backup file
     * @throws DatabaseEnvironmentCleanupException  if the backup file cannot be created
     * @see com.axway.ats.environment.database.model.BackupHandler#createBackup(java.lang.String)
     */
    public void createBackup( String backupFileName ) throws DatabaseEnvironmentCleanupException {

        // reset flag, so delete statements will be inserted
        this.deleteStatementsInserted = false;

        BufferedWriter fileWriter = null;
        try {
            fileWriter = new BufferedWriter(new FileWriter(new File(backupFileName)));
            log.info("Started creation of database backup in file '" + backupFileName + "'");

            writeBackupToFile(fileWriter);

            log.info("Completed creation of database backup in file '" + backupFileName + "'");
        } catch (Exception pe) {
            markBackupFileAsDamaged(fileWriter, backupFileName);
            throw new DatabaseEnvironmentCleanupException(ERROR_CREATING_BACKUP + backupFileName, pe);
        } finally {
        	IoUtils.closeStream(fileWriter, ERROR_CREATING_BACKUP + backupFileName);
        }
    }

    /**
     * Marks faulty backup file as damaged
     * @param fileWriter backup file writer
     * @param backupFileName backup file name
     */
    private void markBackupFileAsDamaged(
                                          Writer fileWriter,
                                          String backupFileName ) {

        try {
            if (fileWriter != null) {
                //stream must be closed to flush writing to the file and to be ready for move
                fileWriter.close();
                //avoid any further operation with the stream
                fileWriter = null;

                File bkFile = new File(backupFileName);
                if (bkFile.exists()) {
                    String dmgFileName = backupFileName + DAMAGED_BACKUP_FILE_SUFFIX;
                    if (bkFile.renameTo(new File(dmgFileName))) {
                        log.debug("Faulty backup file is renamed to: " + dmgFileName);
                    } else {
                    	log.warn("Faulty backup file '" + backupFileName
                                + "' can not be marked as damaged. The rename operation failed");
                    }
                }
            }
        } catch (IOException ioe) {
            log.error(ERROR_CREATING_BACKUP + backupFileName, ioe);
        }
    }

    /**
     * Write the backup to a file
     *
     * @param fileWriter the file writer
     * @throws IOException on io error
     * @throws DatabaseEnvironmentCleanupException on error
     * @throws DbException on error reading from the database
     * @throws ParseException
     */
    protected void writeBackupToFile( Writer fileWriter ) throws IOException,
                                                          DatabaseEnvironmentCleanupException,
                                                          DbException, ParseException {
    	
        if (disableForeignKeys) {
            fileWriter.write(disableForeignKeyChecksStart());
        }

        for (Entry<String, DbTable> entry : dbTables.entrySet()) {
            DbTable dbTable = entry.getValue();

            if (log.isDebugEnabled()) {
                log.debug("Preparing data for backup of table " + (dbTable != null
                                                                                   ? dbTable.getTableName()
                                                                                   : null));
            }
            List<ColumnDescription> columnsToSelect = null;
            columnsToSelect = getColumnsToSelect(dbTable, dbConnection.getUser());
            if (columnsToSelect == null || columnsToSelect.size() == 0) {
                // NOTE: if needed change behavior to continue if the table has no columns.
                // Currently it is assumed that if the table is described for backup then
                // it contains some meaningful data and so it has columns
                throw new DatabaseEnvironmentCleanupException("No columns to backup for table "
                                                              + (dbTable != null
                                                                                 ? dbTable.getTableName()
                                                                                 : ""));
            }

            StringBuilder selectQuery = new StringBuilder();
            selectQuery.append("SELECT ");
            selectQuery.append(getColumnsString(columnsToSelect));
            selectQuery.append(" FROM ");
            if (!StringUtils.isNullOrEmpty(dbTable.getTableSchema())) {
                selectQuery.append(dbTable.getTableSchema() + ".") ;
            }
            selectQuery.append(dbTable.getTableName());

            DbQuery query = new DbQuery(selectQuery.toString());
            // assuming not very large tables
            DbRecordValuesList[] records = dbProvider.select(query, DbReturnModes.ESCAPED_STRING);

            writeTableToFile(columnsToSelect, dbTable, records, fileWriter);
        }

        if (disableForeignKeys) {
            fileWriter.write(disableForeignKeyChecksEnd());
        }
    }
    

    /**
     * Abstract method for
     *
     * @param columnsToSelect
     * @param dbTable
     * @param records
     * @param fileWriter
     * @throws IOException
     * @throws ParseException
     */
    protected abstract void writeTableToFile(
                                              List<ColumnDescription> columnsToSelect,
                                              DbTable dbTable,
                                              DbRecordValuesList[] records,
                                              Writer fileWriter ) throws IOException, ParseException;

    protected String getColumnsString( List<ColumnDescription> columns ) {

        StringBuilder columnsBuilder = new StringBuilder();

        //create the columns string
        for (ColumnDescription column : columns) {
            columnsBuilder.append(column.getName());
            columnsBuilder.append(",");

        }
        //remove the last comma
        if (columnsBuilder.length() > 1) {
            columnsBuilder.delete(columnsBuilder.length() - 1, columnsBuilder.length());
        }

        return columnsBuilder.toString();
    }

    /**
     * Gets all columns for backup for the given table
     * @param dbTable
     * @param userName
     * @return
     * @throws DbException
     * @throws ColumnHasNoDefaultValueException
     */
    protected abstract List<ColumnDescription> getColumnsToSelect(
                                                                   DbTable dbTable,
                                                                   String userName ) throws DbException,
                                                                                     ColumnHasNoDefaultValueException;

    protected abstract String disableForeignKeyChecksStart();

    protected abstract String disableForeignKeyChecksEnd();

    /**
     * Add a table to backup
     *
     * @param dbTable   an instance of DbTable describing the database table
     * @see com.axway.ats.environment.database.model.BackupHandler#addTable(com.axway.ats.environment.database.model.DbTable)
     */
    public void addTable( DbTable dbTable ) {

        if (dbTables.containsKey(dbTable.getTableName())) {
            log.warn("DB table with name '" + dbTable.getTableName()
                     + "' has already been added for backup.");
        } else {
            dbTables.put(dbTable.getTableName(), dbTable);
        }

    }

    /**
     * Enable or disable the foreign key check prior to restoring the
     * database - default value should be true
     *
     * @param foreignKeyCheck   enable or disable
     * @see com.axway.ats.environment.database.model.BackupHandler#setForeignKeyCheck(boolean)
     */
    public void setForeignKeyCheck( boolean foreignKeyCheck ) {

        this.disableForeignKeys = foreignKeyCheck;

    }

    /**
     * Choose whether to include "DELETE FROM *" SQL statements in
     * the backup - the default value should be true. If this option is
     * turned on, tables will be deleted prior to inserting data in them
     *
     * @param includeDeleteStatements   enable or disable
     * @see com.axway.ats.environment.database.model.BackupHandler#setIncludeDeleteStatements(boolean)
     */
    public void setIncludeDeleteStatements( boolean includeDeleteStatements ) {

        this.includeDeleteStatements = includeDeleteStatements;

    }

    /**
     * Choose whether to lock the tables during restore - default
     * value should be true, as other processes might modify the tables
     * during restore, which may lead to inconsistency
     *
     * @param lockTables    enable or disable
     * @see com.axway.ats.environment.database.model.BackupHandler#setLockTables(boolean)
     */
    public void setLockTables( boolean lockTables ) {

        this.addLocks = lockTables;

    }
    
    /**
     * Choose whether to recreate the tables during restore - default
     * value should be false
     * 
     * @param dropEntireTable    enable or disable
     * @see com.axway.ats.environment.database.model.BackupHandler#setDropTables(boolean)
     */
    public void setDropTables( boolean dropEntireTable ) {

        this.dropEntireTable = dropEntireTable;

    }

    protected abstract void writeDeleteStatements( Writer fileWriter ) throws IOException;

    /**
     * Release the database connection
     */
    @Override
    public void disconnect() {

        this.dbProvider.disconnect();
    }
}
