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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.dbaccess.ColumnDescription;
import com.axway.ats.core.dbaccess.ConnectionPool;
import com.axway.ats.core.dbaccess.DbRecordValue;
import com.axway.ats.core.dbaccess.DbRecordValuesList;
import com.axway.ats.core.dbaccess.DbUtils;
import com.axway.ats.core.dbaccess.OracleColumnDescription;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.dbaccess.oracle.DbConnOracle;
import com.axway.ats.core.dbaccess.oracle.OracleDbProvider;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.environment.database.exceptions.ColumnHasNoDefaultValueException;
import com.axway.ats.environment.database.exceptions.DatabaseEnvironmentCleanupException;
import com.axway.ats.environment.database.model.DbTable;

class OracleEnvironmentHandler extends AbstractEnvironmentHandler {

    // The MAX value for a binary column that will not fail an INSERT statement 
    private static final int MAX_BINARY_COLUMN_INSERT_LENGTH = 4000;

    // Class that contains table constraint creation and enable statements
    // Currently only foreign key is taken into consideration
    class TableConstraints {

        List<String> foreignKeyStatements;
        List<String> enableForeignKeyConstraintStatements;

        public TableConstraints() {

            foreignKeyStatements = new ArrayList<>();
            enableForeignKeyConstraintStatements = new ArrayList<>();
        }

    }

    private static final Logger    log = LogManager.getLogger(OracleEnvironmentHandler.class);

    private List<TableConstraints> tablesConstraints;

    OracleEnvironmentHandler( DbConnOracle dbConnection,
                              OracleDbProvider dbProvider ) {

        super(dbConnection, dbProvider);

        tablesConstraints = new ArrayList<>();
    }

    @Override
    protected List<ColumnDescription> getColumnsToSelect(
            DbTable table,
            String userName ) throws DbException,
                                     ColumnHasNoDefaultValueException {

        // TODO Implementation might be replaced with JDBC DatabaseMetaData.getColumns() but should be verified
        // with default column values

        // ALL_TAB_COLS - All columns of tables accessible by this user. OWNER restriction is used because user might
        // have access to other user's tables and columns
        String selectColumnsInfo = "SELECT * FROM ALL_TAB_COLS WHERE TABLE_NAME='"
                                   + table.getTableName().toUpperCase() + "' AND OWNER='"
                                   + userName.toUpperCase() + "'";
        ArrayList<ColumnDescription> columnsToSelect = new ArrayList<ColumnDescription>();
        DbRecordValuesList[] columnsMetaData = null;
        try {
            columnsMetaData = this.dbProvider.select(selectColumnsInfo);
        } catch (DbException e) {
            throw new DbException("Could not get columns for table " + table.getTableName()
                                  + ". You may check if the table exists, if the you are using the right user and it has the right permissions. See more details in the trace.",
                                  e);
        }

        if (columnsMetaData.length == 0) {
            throw new DbException("Could not get columns for table "
                                  + table.getTableName()
                                  + ". You may check if the table exists, if the you are using the right user and it has the right permissions.");
        }

        for (DbRecordValuesList columnMetaData : columnsMetaData) {

            String columnName = (String) columnMetaData.get("COLUMN_NAME");

            //check if the column should be skipped in the backup
            if (!table.getColumnsToExclude().contains(columnName)) {

                ColumnDescription colDescription = new OracleColumnDescription(columnName,
                                                                               (String) columnMetaData.get(
                                                                                       "DATA_TYPE"));

                columnsToSelect.add(colDescription);
            } else {
                //if this column has no default value, we cannot skip it in the backup
                if (columnMetaData.get("DATA_DEFAULT") == null) {
                    log.error("Cannot skip columns with no default values while creating backup");
                    throw new ColumnHasNoDefaultValueException(table.getTableName(), columnName);
                }
            }
        }

        return columnsToSelect;
    }

    @Override
    protected void writeTableToFile(
            List<ColumnDescription> columns,
            DbTable table,
            DbRecordValuesList[] records,
            Writer fileWriter ) throws IOException, ParseException {

        // TODO : exclusive table locks START

        if (shouldDropTable(table)) {
            //            String tableFullName = table.getFullTableName();
            //            if (StringUtils.isNullOrEmpty(table.getTableSchema())) {
            //                // we need to know the schema/owner of that table and
            //                // since table schema is null or empty, we will use the login username as such
            //                tableFullName = dbProvider.getDbConnection().getUser() + "." + table.getTableName();
            //            }

            /*fileWriter.write(DROP_TABLE_MARKER + tableFullName
                             + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);*/
            String owner = table.getTableSchema();
            if (StringUtils.isNullOrEmpty(owner)) {
                owner = dbProvider.getDbConnection().getUser();
            }
            Connection connection = null;
            try {
                connection = ConnectionPool.getConnection(dbConnection);
                TableConstraints tbConst = writeDropTableStatements(fileWriter, table.getTableName(), owner,
                                                                    connection);
                if (tbConst != null) {
                    this.tablesConstraints.add(tbConst);
                }
            } finally {
                DbUtils.closeConnection(connection);
            }

        } else if (!this.deleteStatementsInserted) {
            writeDeleteStatements(fileWriter);
        }

        /*
         *  For resetting some sequence to given value (in this case 100), we need to execute something like this:
         *
            DECLARE
                currVal NUMBER;
            BEGIN
                SELECT test_seq.NEXTVAL INTO currVal FROM dual;
                EXECUTE IMMEDIATE 'ALTER SEQUENCE test_seq INCREMENT BY -' || TO_CHAR(currVal - 100);
                SELECT test_seq.NEXTVAL INTO currVal FROM dual;
                EXECUTE IMMEDIATE 'ALTER SEQUENCE test_seq INCREMENT BY 1';
                COMMIT;
            END;
         */

        if (records.length > 0) {

            if (containsBinaryTypes(columns)) {

                /*
                 * If the binary value is too long the INSERT operation will fail.
                 * The workaround is to assign the binary value to a variable and then use it in the INSERT(or UPDATE) statement
                 * We have to use DECLARE/BEGIN/END anonymous block statement like this:
                 *
                   DECLARE
                      binValue_0 TABLE_NAME.BLOB_COLUMN_NAME%type;
                    BEGIN
                      binValue_0 := to_blob('3C3F786D6C2076657273696F6E3D22312E3022');
                      INSERT INTO TABLE_NAME(NAME,BLOB_COLUMN_NAME,DESCRIPTION) VALUES ('index.xhtml', binValue_0, 'Index file');
                    END; -- ATS EOL;
                 */

                final String INDENTATION = "  ";
                final String VAR_PREFIX = "binValue_";
                StringBuilder stmtBlockBuilder = new StringBuilder("DECLARE"
                                                                   + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);

                int variableIndex = 0;
                for (ColumnDescription column : columns) {
                    if (column.isTypeBinary()) {
                        stmtBlockBuilder.append(INDENTATION + VAR_PREFIX + (variableIndex++) + " "
                                                + table.getTableName() + "." + column.getName() + "%type;"
                                                + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                    }
                }
                stmtBlockBuilder.append("BEGIN" + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                int stmtBlockStart = stmtBlockBuilder.length();
                String insertBegin = INDENTATION + "INSERT INTO " + table.getTableName() + "("
                                     + getColumnsString(columns) + ") VALUES (";
                String insertEnd = ");" + AtsSystemProperties.SYSTEM_LINE_SEPARATOR;

                for (DbRecordValuesList record : records) {

                    StringBuilder insertStatement = new StringBuilder();
                    variableIndex = 0;
                    for (int i = 0; i < record.size(); i++) {

                        ColumnDescription column = columns.get(i);
                        DbRecordValue recordValue = record.get(i);
                        // extract the value depending on the column type
                        String fieldValue = extractValue(column, (String) recordValue.getValue()).toString();

                        if (column.isTypeBinary()) { // BLOB, CLOB, NCLOB
                            String varName = VAR_PREFIX + (variableIndex++);
                            String origValue = ((String) recordValue.getValue());
                            long length = (origValue != null)
                                                              ? origValue.length()
                                                              : -1;
                            stmtBlockBuilder.append(INDENTATION + varName + " := " + fieldValue + ";"
                                                    + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                            // length == -1 means NULL object, so no need to create temporary blob/clob/nclob
                            if (length != -1) {
                                stmtBlockBuilder.append(INDENTATION + "dbms_lob.createtemporary(" + varName + ",true);"
                                + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                            }
                            String binaryMethod = "to_" + column.getType().toLowerCase();
                            if (length > MAX_BINARY_COLUMN_INSERT_LENGTH) {
                                int currentBinaryIdx = 0;
                                while (currentBinaryIdx <= length) {
                                    if (currentBinaryIdx + MAX_BINARY_COLUMN_INSERT_LENGTH <= length) {
                                        stmtBlockBuilder.append(INDENTATION + "dbms_lob.append(" + varName + ","
                                                                + binaryMethod + "('"
                                                                + origValue.substring(currentBinaryIdx,
                                                                                      currentBinaryIdx
                                                                                      + MAX_BINARY_COLUMN_INSERT_LENGTH)
                                                                + "'));" + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                                        currentBinaryIdx += MAX_BINARY_COLUMN_INSERT_LENGTH;
                                    } else {
                                        stmtBlockBuilder.append(INDENTATION + "dbms_lob.append(" + varName + ","
                                                                + binaryMethod + "('"
                                                                + origValue.substring(currentBinaryIdx,
                                                                                      (int) (currentBinaryIdx
                                                                                             + (length
                                                                                                - currentBinaryIdx)))
                                                                + "'));" + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                                        // safely break the loop here
                                        break;
                                    }
                                }
                            }
                            insertStatement.append(varName);
                        } else {
                            insertStatement.append(fieldValue);
                        }
                        insertStatement.append(",");
                    }
                    //remove the last comma
                    insertStatement.delete(insertStatement.length() - 1, insertStatement.length());

                    stmtBlockBuilder.append(insertBegin);
                    stmtBlockBuilder.append(insertStatement.toString());
                    stmtBlockBuilder.append(insertEnd);
                    stmtBlockBuilder.append("END;" + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                    fileWriter.write(stmtBlockBuilder.toString());
                    fileWriter.flush();

                    // clear to block BEGIN tag
                    stmtBlockBuilder.delete(stmtBlockStart, stmtBlockBuilder.length());
                }
            } else {

                StringBuilder insertStatement = new StringBuilder();
                String insertBegin = "INSERT INTO " + table.getTableName() + "(" + getColumnsString(columns)
                                     + ") VALUES (";
                String insertEnd = ");" + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR;

                for (DbRecordValuesList record : records) {

                    insertStatement.append(insertBegin);

                    for (int i = 0; i < record.size(); i++) {

                        DbRecordValue recordValue = record.get(i);
                        String fieldValue = (String) recordValue.getValue();

                        // extract specific values depending on their type
                        insertStatement.append(extractValue(columns.get(i), fieldValue));
                        insertStatement.append(",");
                    }
                    //remove the last comma
                    insertStatement.delete(insertStatement.length() - 1, insertStatement.length());
                    insertStatement.append(insertEnd);

                }
                fileWriter.write(insertStatement.toString());
            }
        }

        // TODO : exclusive table locks END
    }

    private TableConstraints writeDropTableStatements( Writer fileWriter, String tableName, String owner,
                                                       Connection connection ) throws IOException {

        TableConstraints tbConstraints = new TableConstraints();

        Map<String, List<String>> foreignKeys = getForeignKeys(owner, tableName, connection);

        // generate script for restoring the exact table and return the CREATE TABLE script
        String generateTableScript = generateTableScript(owner, tableName, connection);

        for (Entry<String, List<String>> entryKey : foreignKeys.entrySet()) {
            String parentTableName = entryKey.getKey();
            for (String key : entryKey.getValue()) {
                // generate scripts for creating all foreign keys
                tbConstraints.foreignKeyStatements.add(generateForeignKeyScript(owner, key, connection));
                // disable the foreign keys
                fileWriter.write("ALTER TABLE " + owner + "." + parentTableName + " DISABLE CONSTRAINT " + key
                                 + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
            }
        }

        // get the create scripts for all of the table's indexes
        Map<String, String> tableIndexesCreateScripts = getTableIndexesCreateScripts(owner, tableName, connection);

        // drop the table
        fileWriter.write("DROP TABLE " + tableName + " CASCADE CONSTRAINTS PURGE " + EOL_MARKER
                         + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);

        // create new table
        fileWriter.write(generateTableScript + EOL_MARKER
                         + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);

        // create the table's indexes
        for (String indexCreateScript : tableIndexesCreateScripts.values()) {
            fileWriter.write(indexCreateScript + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
        }

        // enable the foreign keys
        for (Entry<String, List<String>> entryKey : foreignKeys.entrySet()) {
            String parentTableName = entryKey.getKey();
            for (String key : entryKey.getValue()) {
                tbConstraints.enableForeignKeyConstraintStatements.add("ALTER TABLE " + owner + "." + parentTableName
                                                                       + " ENABLE CONSTRAINT " + key);
            }
        }

        return tbConstraints;

    }

    @Override
    protected void writeDeleteStatements( Writer fileWriter ) throws IOException {

        if (this.addLocks) {
            for (Entry<String, DbTable> entry : dbTables.entrySet()) {
                DbTable dbTable = entry.getValue();
                if (shouldDropTable(dbTable)) {
                    continue;
                }
                fileWriter.write("LOCK TABLE " + dbTable.getTableName() + " IN EXCLUSIVE MODE NOWAIT;"
                                 + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
            }
        }

        if (this.includeDeleteStatements) {
            for (Entry<String, DbTable> entry : dbTables.entrySet()) {
                DbTable dbTable = entry.getValue();
                if (shouldDropTable(dbTable)) {
                    continue;
                }
                fileWriter.write("DELETE FROM " + dbTable.getTableName() + ";" + EOL_MARKER
                                 + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
            }
            this.deleteStatementsInserted = true;
        }
    }

    private boolean containsBinaryTypes(
            List<ColumnDescription> columns ) {

        for (ColumnDescription column : columns) {
            if (column.isTypeBinary()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected String disableForeignKeyChecksStart() {

        return "SET CONSTRAINTS ALL DEFERRED;" + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR;
    }

    @Override
    protected String disableForeignKeyChecksEnd() {

        return "";
    }

    /**
     * Extracts the specific value for INSERT statement, considering it's type and the specifics associated with it
     * If value is too big for one-time get then initial/default value is returned. And later it is accumulated with
     *  more statements.
     *  Specific cases: DATE types will be extracted as to_date(<STRING_REPRESENTATION_OF_THE_SQL_VALUE>).
     *  The same is true for TIMESTAMP (only the function to_timestamp is used).
     *  BLOB/CLOB/NCLOB can be returned as: to_blob|clob|nclob or empty_blob|clob if their value is more than 4k characters long.
     */
    private StringBuilder extractValue( ColumnDescription column, String fieldValue ) throws ParseException {

        if (fieldValue == null) {
            return new StringBuilder("NULL");
        }

        StringBuilder exportedValueForAssignment = new StringBuilder();

        String typeInUpperCase = column.getType().toUpperCase();
        if ("DATE".equals(typeInUpperCase)) {
            SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.0");
            SimpleDateFormat outputDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            exportedValueForAssignment.append("to_date('");
            exportedValueForAssignment.append(outputDateFormat.format(inputDateFormat.parse(fieldValue)));
            exportedValueForAssignment.append("','YYYY-MM-DD hh24:mi:ss')");

        } else if (typeInUpperCase.startsWith("TIMESTAMP")) {
            exportedValueForAssignment.append("to_timestamp('");
            exportedValueForAssignment.append(fieldValue);
            exportedValueForAssignment.append("','YYYY-MM-DD hh24:mi:ss.FF')");
        } else if ("BLOB".equals(typeInUpperCase)) {
            //Get the binary type length
            long length = fieldValue.length();
            if (length <= MAX_BINARY_COLUMN_INSERT_LENGTH) {
                exportedValueForAssignment.append("to_blob('");
                exportedValueForAssignment.append(fieldValue);
                exportedValueForAssignment.append("')");
            } else {
                // just create empty blob, which will be populated later via append
                exportedValueForAssignment.append("empty_blob()");
            }

        } else if ("CLOB".equals(typeInUpperCase)) {
            long length = fieldValue.length();
            if (length <= MAX_BINARY_COLUMN_INSERT_LENGTH) {
                exportedValueForAssignment.append("to_clob('");
                exportedValueForAssignment.append(fieldValue.replace("'", "''"));
                exportedValueForAssignment.append("')");
            } else {
                // just create empty clob, which will be populated later via append
                exportedValueForAssignment.append("empty_clob()");
            }
        } else if ("NCLOB".equals(typeInUpperCase)) {
            long length = fieldValue.length();
            if (length <= MAX_BINARY_COLUMN_INSERT_LENGTH) {
                exportedValueForAssignment.append("to_nclob('");
                exportedValueForAssignment.append(fieldValue.replace("'", "''"));
                exportedValueForAssignment.append("')");
            } else {
                // just create empty nclob, which will be populated later via append
                // Note that, yes, use empty_clob(), not empty_nclob(), since the later does not exist
                exportedValueForAssignment.append("empty_clob()");
            }
        } else {
            exportedValueForAssignment.append("'");
            exportedValueForAssignment.append(fieldValue.replace("'", "''"));
            exportedValueForAssignment.append("'");
        }

        return exportedValueForAssignment;
    }

    /**
     * @see com.axway.ats.environment.database.model.BackupHandler#createBackup(java.lang.String)
     */
    @Override
    public void createBackup( String backupFileName ) throws DatabaseEnvironmentCleanupException {

        super.createBackup(backupFileName);

        // In order to add and enable the foreign keys, that are related to the tables for backup
        // all of the columns, referenced in those foreign keys must have values in the referenced table
        // That's why we insert records into all of the tables, added to backup,
        // and then add and enable all of the foreign keys
        BufferedWriter fileWriter = null;
        try {
            fileWriter = new BufferedWriter(new FileWriter(new File(backupFileName), true));

            // create all of the foreign keys
            for (TableConstraints tbConst : tablesConstraints) {
                for (String fkQuery : tbConst.foreignKeyStatements) {
                    fileWriter.append(fkQuery + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                }
            }
            // enable the foreign keys
            for (TableConstraints tbConst : tablesConstraints) {
                for (String fkEnableQuery : tbConst.enableForeignKeyConstraintStatements) {
                    fileWriter.append(fkEnableQuery + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                }
            }

            log.info("Completed backup of foreign keys in '" + backupFileName + "'");
        } catch (Exception pe) {
            markBackupFileAsDamaged(fileWriter, backupFileName);
            throw new DatabaseEnvironmentCleanupException(ERROR_CREATING_BACKUP + backupFileName, pe);
        } finally {
            IoUtils.closeStream(fileWriter, ERROR_CREATING_BACKUP + backupFileName);
        }

    }

    /**
     * @see com.axway.ats.environment.database.model.RestoreHandler#restore(java.lang.String)
     */
    public void restore( String backupFileName ) throws DatabaseEnvironmentCleanupException {

        BufferedReader backupReader = null;
        Connection connection = null;

        //we need to preserve the auto commit option, as
        //the connections are pooled
        boolean isAutoCommit = true;

        try {
            log.info("Started restore of database backup from file '" + backupFileName + "'");

            backupReader = new BufferedReader(new FileReader(new File(backupFileName)));

            connection = ConnectionPool.getConnection(dbConnection);

            isAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            StringBuilder sql = new StringBuilder();
            String line = backupReader.readLine();
            List<TableConstraints> tablesConstraints = new ArrayList<>();
            while (line != null) {

                sql.append(line);
                if (line.startsWith(DROP_TABLE_MARKER)) {

                    String table = line.substring(DROP_TABLE_MARKER.length()).trim();
                    String owner = table.substring(0, table.indexOf("."));
                    String simpleTableName = table.substring(table.indexOf(".") + 1);
                    TableConstraints tbConst = dropAndRecreateTable(connection, simpleTableName, owner);
                    if (tbConst != null) {
                        tablesConstraints.add(tbConst);
                    }

                    sql = new StringBuilder();

                }
                if (line.endsWith(EOL_MARKER)) {

                    // remove the EOL marker and the trailing semicolon because, strangely, Oracle JDBC driver
                    // does not require it, as opposing to any other, excluding blocks ([DECLARE]BEGIN-END;)
                    if (line.contains("END;")) { // in this case semicolon is mandatory
                        sql.delete(sql.length() - EOL_MARKER.length(), sql.length());
                    } else {
                        sql.setLength(sql.length() - EOL_MARKER.length());
                        if (sql.toString().endsWith(";")) {
                            sql.setLength(sql.length() - 1);
                        }
                        //sql.delete(sql.length() - EOL_MARKER.length() - 1, sql.length());
                    }

                    PreparedStatement updateStatement = connection.prepareStatement(sql.toString());

                    if (log.isTraceEnabled()) {
                        log.trace("Executing SQL query: " + sql.toString());
                    }
                    //catch the exception and rollback, otherwise we are locked
                    try {
                        updateStatement.execute();
                    } catch (SQLException sqle) {
                        //we have to roll back the transaction and re throw the exception
                        connection.rollback();
                        throw new SQLException("Error invoking restore satement: " + sql.toString(), sqle);
                    } finally {
                        try {
                            updateStatement.close();
                        } catch (SQLException sqle) {
                            log.error("Unable to close prepared statement", sqle);
                        }
                    }
                    sql = new StringBuilder();
                } else {
                    //add a new line
                    //FIXME: this code will add the system line ending - it
                    //is not guaranteed that this was the actual line ending
                    sql.append(AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                }

                line = backupReader.readLine();
            }

            /*// create all of the foreign keys
            for (TableConstraints tbConst : tablesConstraints) {
                for (String fkQuery : tbConst.foreignKeyStatements) {
                    executeUpdate(fkQuery, connection);
                }
            }
            // enable the foreign keys
            for (TableConstraints tbConst : tablesConstraints) {
                for (String fkEnableQuery : tbConst.enableForeignKeyConstraintStatements) {
                    executeUpdate(fkEnableQuery, connection);
                }
            }*/

            try {
                //commit the transaction
                connection.commit();

            } catch (SQLException sqle) {
                //we have to roll back the transaction and re throw the exception
                connection.rollback();
                throw sqle;
            }

            log.info("Completed restore of database backup from file '" + backupFileName + "'");

        } catch (IOException ioe) {
            throw new DatabaseEnvironmentCleanupException(ERROR_RESTORING_BACKUP + backupFileName, ioe);
        } catch (SQLException sqle) {
            throw new DatabaseEnvironmentCleanupException(ERROR_RESTORING_BACKUP + backupFileName, sqle);
        } catch (DbException dbe) {
            throw new DatabaseEnvironmentCleanupException(ERROR_RESTORING_BACKUP + backupFileName, dbe);
        } finally {
            try {
                IoUtils.closeStream(backupReader, "Could not close reader for backup file "
                                                  + backupFileName);

                if (connection != null) {
                    connection.setAutoCommit(isAutoCommit);
                    connection.close();
                }
            } catch (SQLException sqle) {
                log.error(ERROR_RESTORING_BACKUP + backupFileName, sqle);
            }
        }
    }

    public TableConstraints dropAndRecreateTable( Connection connection, String tableName, String owner ) {

        TableConstraints tbConstraints = new TableConstraints();

        Map<String, List<String>> foreignKeys = getForeignKeys(owner, tableName, connection);

        // generate script for restoring the exact table
        String generateTableScript = generateTableScript(owner, tableName, connection);

        for (Entry<String, List<String>> entryKey : foreignKeys.entrySet()) {
            String parentTableName = entryKey.getKey();
            for (String key : entryKey.getValue()) {
                // generate scripts for creating all foreign keys
                tbConstraints.foreignKeyStatements.add(generateForeignKeyScript(owner, key, connection));
                // disable the foreign keys
                executeUpdate("ALTER TABLE " + owner + "." + parentTableName + " DISABLE CONSTRAINT " + key,
                              connection);
            }
        }

        Map<String, String> tableIndexesCreateScripts = getTableIndexesCreateScripts(owner, tableName, connection);

        // drop the table
        executeUpdate("DROP TABLE " + tableName + " CASCADE CONSTRAINTS PURGE", connection);

        // create new table
        executeUpdate(generateTableScript, connection);

        // create the table's indexes
        for (String indexCreateScript : tableIndexesCreateScripts.values()) {
            executeUpdate(indexCreateScript, connection);
        }

        // enable the foreign keys
        for (Entry<String, List<String>> entryKey : foreignKeys.entrySet()) {
            String parentTableName = entryKey.getKey();
            for (String key : entryKey.getValue()) {
                tbConstraints.enableForeignKeyConstraintStatements.add("ALTER TABLE " + owner + "." + parentTableName
                                                                       + " ENABLE CONSTRAINT " + key);
            }
        }

        return tbConstraints;
    }

    private Map<String, String> getTableIndexesCreateScripts( String owner, String tableName, Connection connection ) {

        List<String> indexesNames = getIndexesNames(owner, tableName, connection);

        Map<String, String> indexesCreateScripts = new HashMap<>();

        // query for getting the table's index names
        for (String indexName : indexesNames) {
            String query = "SELECT dbms_metadata.get_ddl('INDEX','" + indexName + "','" + owner + "') FROM dual";
            PreparedStatement stmnt = null;
            ResultSet rs = null;
            try {
                if (log.isTraceEnabled()) {
                    log.trace("Executing SQL query: " + query);
                }
                stmnt = connection.prepareStatement(query);
                rs = stmnt.executeQuery();
                while (rs.next()) {
                    indexesCreateScripts.put(indexName, rs.getString(1));
                }
            } catch (SQLException e) {
                throw new DbException(
                        "SQL errorCode=" + e.getErrorCode() + " sqlState=" + e.getSQLState() + " "
                        + e.getMessage(), e);
            } finally {
                DbUtils.closeStatement(stmnt);
            }
        }

        return indexesCreateScripts;

    }

    private List<String> getIndexesNames( String owner, String tableName, Connection connection ) {

        // query for getting the table's index names
        String query = "SELECT INDEX_NAME FROM all_indexes WHERE table_name='" + tableName + "' AND table_owner='"
                       + owner + "'";

        PreparedStatement stmnt = null;
        ResultSet rs = null;
        List<String> indexesNames = new ArrayList<>();
        try {
            if (log.isTraceEnabled()) {
                log.trace("Executing SQL query: " + query);
            }
            stmnt = connection.prepareStatement(query);
            rs = stmnt.executeQuery();
            while (rs.next()) {
                indexesNames.add(rs.getString("INDEX_NAME"));
            }
        } catch (SQLException e) {
            throw new DbException(
                    "SQL errorCode=" + e.getErrorCode() + " sqlState=" + e.getSQLState() + " "
                    + e.getMessage(), e);
        } finally {
            DbUtils.closeStatement(stmnt);
        }

        return indexesNames;
    }

    private Map<String, List<String>> getForeignKeys( String owner, String tableName, Connection connection ) {

        String query = "select table_name, constraint_name "
                       + " from all_constraints "
                       + " where r_owner = '" + owner + "' "
                       + " and constraint_type = 'R' "
                       + " and r_constraint_name in "
                       + " ( select constraint_name from all_constraints "
                       + " where constraint_type in ('P', 'U') "
                       + " and table_name = '" + tableName + "'"
                       + " and owner = '" + owner + "' )";

        PreparedStatement stmnt = null;
        Map<String, List<String>> tableForeignKey = new HashMap<String, List<String>>();
        ResultSet rs = null;
        try {
            if (log.isTraceEnabled()) {
                log.trace("Executing SQL query: " + query);
            }
            stmnt = connection.prepareStatement(query);
            rs = stmnt.executeQuery();
            while (rs.next()) {
                String parentTableName = rs.getString("TABLE_NAME");
                String fKeyName = rs.getString("CONSTRAINT_NAME");

                if (tableForeignKey.containsKey(parentTableName)) {
                    tableForeignKey.get(parentTableName).add(fKeyName);
                } else {
                    List<String> fKeys = new ArrayList<String>();
                    fKeys.add(fKeyName);
                    tableForeignKey.put(parentTableName, fKeys);
                }
            }

            return tableForeignKey;
        } catch (SQLException e) {
            throw new DbException(
                    "SQL errorCode=" + e.getErrorCode() + " sqlState=" + e.getSQLState() + " "
                    + e.getMessage(), e);
        } finally {
            DbUtils.closeStatement(stmnt);
        }
    }

    private String generateTableScript( String owner, String tableName, Connection connection ) {

        String query = "select dbms_metadata.get_ddl('TABLE','" + tableName + "','" + owner + "') from dual";

        PreparedStatement stmnt = null;
        ResultSet rs = null;
        String createTableScript = new String();
        try {
            if (log.isTraceEnabled()) {
                log.trace("Executing SQL query: " + query);
            }
            stmnt = connection.prepareStatement(query);
            rs = stmnt.executeQuery();
            if (rs.next()) {
                createTableScript = rs.getString(1);
            }

            return createTableScript;
        } catch (SQLException e) {
            throw new DbException(
                    "SQL errorCode=" + e.getErrorCode() + " sqlState=" + e.getSQLState() + " "
                    + e.getMessage(), e);
        } finally {
            DbUtils.closeStatement(stmnt);
        }
    }

    private String generateForeignKeyScript( String owner, String foreignKey, Connection connection ) {

        String query = "select DBMS_METADATA.GET_DDL('REF_CONSTRAINT','" + foreignKey + "','" + owner
                       + "') from dual";

        PreparedStatement stmnt = null;
        ResultSet rs = null;
        String createTableScript = new String();
        try {
            if (log.isTraceEnabled()) {
                log.trace("Executing SQL query: " + query);
            }
            stmnt = connection.prepareStatement(query);
            rs = stmnt.executeQuery();
            if (rs.next()) {
                createTableScript = rs.getString(1);
            }

            return createTableScript;
        } catch (SQLException e) {
            throw new DbException(
                    "SQL errorCode=" + e.getErrorCode() + " sqlState=" + e.getSQLState() + " "
                    + e.getMessage(), e);
        } finally {
            DbUtils.closeStatement(stmnt);
        }
    }

    private void executeUpdate( String query, Connection connection ) throws DbException {

        PreparedStatement stmnt = null;
        try {
            if (log.isTraceEnabled()) {
                log.trace("Executing SQL query: " + query);
            }
            stmnt = connection.prepareStatement(query);
            stmnt.executeUpdate();
        } catch (SQLException e) {
            throw new DbException(
                    "SQL errorCode=" + e.getErrorCode() + " sqlState=" + e.getSQLState() + " "
                    + e.getMessage(), e);
        } finally {
            DbUtils.closeStatement(stmnt);
        }
    }
}
