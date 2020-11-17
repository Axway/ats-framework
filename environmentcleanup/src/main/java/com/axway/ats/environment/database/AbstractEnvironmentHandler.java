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
import java.io.InputStream;
import java.io.Writer;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.dbaccess.DbQuery;
import com.axway.ats.core.dbaccess.ColumnDescription;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.DbProvider;
import com.axway.ats.core.dbaccess.DbRecordValuesList;
import com.axway.ats.core.dbaccess.DbReturnModes;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.utils.IoUtils;
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

    private static final Logger    log                        = LogManager.getLogger(AbstractEnvironmentHandler.class);
    protected static final String  ERROR_CREATING_BACKUP      = "Could not create backup in file ";
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
    protected boolean              skipTableContent;
    protected boolean              writeGenerateForeignKeyProcedure;

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
        this.writeGenerateForeignKeyProcedure = true;
        this.dbTables = new LinkedHashMap<>(); // keep the order of the tables as they will be added
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
    protected void markBackupFileAsDamaged(
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

            // use both table schema (if presented) and table name for the final table name
            String fullTableName = null;
            if (dbTable != null) {
                fullTableName = dbTable.getFullTableName();
            }

            if (log.isDebugEnabled()) {
                log.debug("Preparing data for backup of table " + fullTableName);
            }
            List<ColumnDescription> columnsToSelect = null;
            columnsToSelect = getColumnsToSelect(dbTable, dbConnection.getUser());
            if (columnsToSelect == null || columnsToSelect.size() == 0) {
                // NOTE: if needed change behavior to continue if the table has no columns.
                // Currently it is assumed that if the table is described for backup then
                // it contains some meaningful data and so it has columns

                // NOTE: it is a good idea to print null instead of empty string for table name when table is null,
                // so it is more obvious for the user that something is wrong
                throw new DatabaseEnvironmentCleanupException("No columns to backup for table "
                                                              + fullTableName);
            }

            DbRecordValuesList[] records = new DbRecordValuesList[0];
            if (!skipTableContent) {
                StringBuilder selectQuery = new StringBuilder();
                selectQuery.append("SELECT ");
                selectQuery.append(getColumnsString(columnsToSelect));
                selectQuery.append(" FROM ");
                selectQuery.append(fullTableName);

                DbQuery query = new DbQuery(selectQuery.toString());
                // assuming not very large tables
                records = dbProvider.select(query, DbReturnModes.ESCAPED_STRING);

            }

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

        if (dbTable != null) {
            String fullTableName = dbTable.getFullTableName();

            if (dbTables.containsKey(fullTableName)) {
                log.warn("DB table with name '" + fullTableName
                         + "' has already been added for backup.");
            } else {
                dbTables.put(fullTableName, dbTable);
            }
        } else {
            log.warn("Could not add DB table that is null");
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

    /**
     * Choose whether to exclude the tables' content during backup and only backup the tables' metadata - defaul
     * value is false, which means that both the tables' metadata and content will be backed up
     * 
     * @param skipTablesContent - true to skip, false otherwise
     * @see com.axway.ats.environment.database.model.BackupHandler#setSkipTablesContent(boolean)
     */
    public void setSkipTablesContent( boolean skipTablesContent ) {

        this.skipTableContent = skipTablesContent;

    }

    /**
     * <p>Whether to drop table can be specified by either {@link #setDropTables(boolean)} or {@link DbTable#setDropTable(boolean)}</p>
     * This method here wraps the logic that determines what must be done for a particular table
     * */
    protected boolean shouldDropTable( DbTable table ) {

        if (table.isDropTable() == null) {
            return dropEntireTable;
        } else {
            return table.isDropTable();
        }
    }

    protected abstract void writeDeleteStatements( Writer fileWriter ) throws IOException;

    /**
     * Get file contents from classpath
     * @param scriptFileName Relative path is relative to the package of current class.
     * @return String of  
     */
    protected String loadScriptFromClasspath( String scriptFileName ) {

        String scriptContents = null;
        try (InputStream is = this.getClass().getResourceAsStream(scriptFileName)) {
            if (is != null) {
                scriptContents = IoUtils.streamToString(is);
            }
        } catch (Exception e) {
            if (e.getSuppressed() != null) { // possible close resources
                Throwable[] suppressedExc = e.getSuppressed();
                for (int i = 0; i < suppressedExc.length; i++) {
                    log.warn("Suppressed exception [" + i + "] details", suppressedExc[i]);
                }
            }
            throw new DbException("Could not load SQL server script needed for DB environment restore from classpath. Check "
                                  + "location of " + scriptFileName + " file", e);
        }
        return scriptContents;
    }

    /**
     * Release the database connection
     */
    @Override
    public void disconnect() {

        this.dbProvider.disconnect();
    }
}
