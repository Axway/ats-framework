/*
 * Copyright 2017-2020 Axway Software
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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.dbaccess.DbQuery;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.dbaccess.ColumnDescription;
import com.axway.ats.core.dbaccess.ConnectionPool;
import com.axway.ats.core.dbaccess.DbRecordValue;
import com.axway.ats.core.dbaccess.DbRecordValuesList;
import com.axway.ats.core.dbaccess.DbReturnModes;
import com.axway.ats.core.dbaccess.DbUtils;
import com.axway.ats.core.dbaccess.MssqlColumnDescription;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.dbaccess.mssql.DbConnSQLServer;
import com.axway.ats.core.dbaccess.mssql.MssqlDbProvider;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.environment.database.exceptions.ColumnHasNoDefaultValueException;
import com.axway.ats.environment.database.exceptions.DatabaseEnvironmentCleanupException;
import com.axway.ats.environment.database.model.DbTable;

class MssqlEnvironmentHandler extends AbstractEnvironmentHandler {

    private static final Logger LOG            = LogManager.getLogger(MssqlEnvironmentHandler.class);
    private static final String HEX_PREFIX_STR = "0x";
    private String              defaultSchema  = null;

    MssqlEnvironmentHandler( DbConnSQLServer dbConnection,
                             MssqlDbProvider dbProvider ) {

        super(dbConnection, dbProvider);

        defaultSchema = getDefaultSchema();
    }

    @Override
    protected List<ColumnDescription> getColumnsToSelect(
                                                          DbTable table,
                                                          String userName ) throws DbException,
                                                                            ColumnHasNoDefaultValueException {

        String fullTableName = table.getFullTableName();

        String selectColumnsInfo = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_DEFAULT, CHARACTER_MAXIMUM_LENGTH, NUMERIC_PRECISION, NUMERIC_SCALE, IS_NULLABLE, "
                                   + "columnproperty(object_id('"
                                   + fullTableName
                                   + "'), COLUMN_NAME,'IsIdentity') as isIdentity "
                                   + "FROM information_schema.COLUMNS WHERE table_name LIKE '"
                                   + table.getTableName() + "'";
        if (!StringUtils.isNullOrEmpty(table.getTableSchema())) {
            // add table schema to select query as well
            selectColumnsInfo += " AND table_schema LIKE '" + table.getTableSchema() + "'";
        }
        ArrayList<ColumnDescription> columnsToSelect = new ArrayList<ColumnDescription>();
        DbRecordValuesList[] columnsMetaData = null;
        try {
            columnsMetaData = this.dbProvider.select(selectColumnsInfo);
        } catch (DbException e) {
            throw new DbException("Could not get columns for table " + fullTableName
                                  + ". Check if the table is existing and that the user has permissions. See more details in the trace.",
                                  e);
        }

        table.setIdentityColumnPresent(false); // the Identity column can be skipped(excluded)
        for (DbRecordValuesList columnMetaData : columnsMetaData) {

            String columnName = (String) columnMetaData.get("COLUMN_NAME");

            //check if the column should be skipped in the backup
            if (!table.getColumnsToExclude().contains(columnName)) {

                ColumnDescription colDescription = new MssqlColumnDescription(columnName,
                                                                              (String) columnMetaData.get("DATA_TYPE"));
                columnsToSelect.add(colDescription);
                if (columnMetaData.get("isIdentity") != null && (Integer) columnMetaData.get("isIdentity") == 1) {
                    table.setIdentityColumnPresent(true);
                }
            } else {
                //if this column has no default value, we cannot skip it in the backup
                if (columnMetaData.get("COLUMN_DEFAULT") == null) {
                    LOG.error("Cannot skip columns with no default values while creating backup");
                    throw new ColumnHasNoDefaultValueException(fullTableName, columnName);
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

        String fullTableName = null;
        if (table != null) {
            fullTableName = table.getFullTableName();
        }
        if (this.addLocks) {
            fileWriter.write("SET TRANSACTION ISOLATION LEVEL SERIALIZABLE " + EOL_MARKER
                             + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
        }

        if (shouldDropTable(table)) {
            //            fileWriter.write(DROP_TABLE_MARKER + fullTableName
            //                             + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
            Connection connection = null;
            try {
                connection = ConnectionPool.getConnection(dbConnection);
                writeDropTableStatements(fileWriter, table, connection);
            } finally {
                DbUtils.closeConnection(connection);
            }

        } else if (!this.deleteStatementsInserted) {
            writeDeleteStatements(fileWriter);
        }

        if (table.getAutoIncrementResetValue() != null) {
            fileWriter.write("DBCC CHECKIDENT ('" + fullTableName + "', RESEED, "
                             + table.getAutoIncrementResetValue() + ");" + EOL_MARKER
                             + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
        }

        if (records.length > 0) {

            StringBuilder insertStatement = new StringBuilder();
            String insertBegin = "INSERT INTO " + fullTableName + "(" + getColumnsString(columns)
                                 + ") VALUES (";
            String insertEnd = null;
            if (table.isIdentityColumnPresent()) {
                insertBegin = "SET IDENTITY_INSERT " + fullTableName + " ON; " + insertBegin;
                insertEnd = "); SET IDENTITY_INSERT " + fullTableName + " OFF;" + EOL_MARKER
                            + AtsSystemProperties.SYSTEM_LINE_SEPARATOR;
            } else {
                insertEnd = ");" + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR;
            }

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
                fileWriter.write(insertStatement.toString()); // limit memory allocation for big tables. Write after each row
                insertStatement.setLength(0);
            }
            fileWriter.flush();
        }
    }

    private void writeDropTableStatements( Writer fileWriter, DbTable table,
                                           Connection connection ) throws IOException {

        String tableName = table.getFullTableName();
        Map<String, List<String>> foreignKeys = getForeingKeys(tableName, connection);

        // execute that so foreign key checks does not fail
        executeUpdate(disableForeignKeyChecksStart(), connection);

        // generate script for restoring the exact table and return the CREATE TABLE script
        String generateTableScript = generateTableScript(tableName, connection);

        String partitionName = getPartitionSchemeName(table);

        if (!StringUtils.isNullOrEmpty(partitionName)) {
            String partitionColumnName = getPartitionColumnName(table);
            generateTableScript = generateTableScript.replace("<PARTITION_SCHEME_OR_FILEGROUP_PLACEHOLDER>",
                                                              " ON [" + partitionName + "]([" + partitionColumnName
                                                                                                             + "])");
        } else {
            // if the table do not have to be partitioned
            // check if the table is on a filegroup
            String fileGroupName = getFileGroupName(table);
            if (!StringUtils.isNullOrEmpty(fileGroupName)) {
                generateTableScript = generateTableScript.replace("<PARTITION_SCHEME_OR_FILEGROUP_PLACEHOLDER>", " ON [" + fileGroupName + "]");
            } else {
                // Table is neither partitioned, nor its data is on file group other than the default one
                generateTableScript = generateTableScript.replace("<PARTITION_SCHEME_OR_FILEGROUP_PLACEHOLDER>", "");
            }
        }

        String scriptContent = loadScriptFromClasspath("generateForeignKeyScript.sql");

        // create the generateForeignKeyScript procedure
        createDatabaseProcedure(connection, scriptContent);

        List<String> generateForeignKeysScripts = new ArrayList<String>();
        for (Entry<String, List<String>> keyEntry : foreignKeys.entrySet()) {
            String parentTableName = keyEntry.getKey();
            for (String key : keyEntry.getValue()) {
                // generate scripts for creating all foreign keys
                generateForeignKeysScripts.add(generateForeignKeyScript(parentTableName, key,
                                                                        connection));
            }
        }

        // drop the newly created procedure
        executeUpdate("DROP PROCEDURE generateForeignKeyScript", connection);

        // drop foreign keys
        String query = "SELECT "
                       + "'ALTER TABLE [' +  OBJECT_SCHEMA_NAME(parent_object_id) + "
                       + "'].[' + OBJECT_NAME(parent_object_id) + "
                       + "'] DROP CONSTRAINT [' + name + ']' "
                       + " FROM sys.foreign_keys "
                       + " WHERE referenced_object_id = object_id('" + tableName + "') ORDER BY name";

        Statement callableStatementDropForeignKeys = null;
        ResultSet rsDropForeignKeys = null;
        try {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Executing SQL query: " + query);
            }
            callableStatementDropForeignKeys = connection.createStatement();
            rsDropForeignKeys = callableStatementDropForeignKeys.executeQuery(query);

            while (rsDropForeignKeys.next()) {
                fileWriter.write(rsDropForeignKeys.getString(1) + EOL_MARKER
                                 + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
            }

        } catch (Exception e) {
            throw new DbException("Error while droping the foreign keys of table '" + tableName + "'.", e);
        } finally {
            DbUtils.closeStatement(callableStatementDropForeignKeys);
        }

        // drop table
        fileWriter.write("DROP TABLE " + tableName + ";" + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);

        // create indexes
        List<String> indexesCreateScripts = generateIndexesCreateScripts(connection, table);
        StringBuilder sb = new StringBuilder();
        for (String indexCreateScript : indexesCreateScripts) {
            sb.append(indexCreateScript + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
        }

        generateTableScript = generateTableScript.replace("<TABLE_INDEXES_PLACEHOLDER>", sb.toString());

        // add table's key constraints (PRIMARY OR UNIQUE)
        sb = new StringBuilder();
        List<String> keyConstraintsScripts = generateKeyConstraintsCreateScripts(connection, table);
        sb = new StringBuilder();
        for (String keyConstraintScript : keyConstraintsScripts) {
            sb.append(keyConstraintScript + "," + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
        }
        if (sb.length() > 0) {
            sb.setLength(sb.toString().length() - (1 + AtsSystemProperties.SYSTEM_LINE_SEPARATOR.length()));
        }
        generateTableScript = generateTableScript.replace("<KEY_CONSTRAINTS_PLACEHOLDER>", sb.toString());

        // create table
        fileWriter.write(generateTableScript + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);

        // create foreign keys
        for (String script : generateForeignKeysScripts) {
            fileWriter.write(script + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
        }

        // enable again the foreign check
        executeUpdate(disableForeignKeyChecksEnd(), connection);
    }

    private String getFileGroupName( DbTable table ) {

        String query = "SELECT OBJECT_SCHEMA_NAME(t.object_id) AS schema_name\r\n" +
                       ",t.name AS table_name\r\n" +
                       ",i.index_id\r\n" +
                       ",i.name AS index_name\r\n" +
                       ",i.*\r\n" +
                       ",ds.name AS filegroup_name\r\n" +
                       "FROM sys.tables t\r\n" +
                       "INNER JOIN sys.indexes i ON t.object_id=i.object_id\r\n" +
                       "INNER JOIN sys.filegroups ds ON i.data_space_id=ds.data_space_id\r\n" +
                       "INNER JOIN sys.partitions p ON i.object_id=p.object_id AND i.index_id=p.index_id\r\n" +
                       "WHERE t.name = '" + table.getTableName() + "'\r\n" +
                       "ORDER BY t.name, i.index_id";

        DbRecordValuesList[] rows = dbProvider.select(query);
        if (rows != null && rows.length > 0) {
            return (String) rows[0].get("filegroup_name");
        } else {
            return "";
        }
    }

    private List<String> generateKeyConstraintsCreateScripts( Connection connection, DbTable table ) {

        List<String> indexesNames = getAllIndexesNamesForTable(connection, table);
        List<String> indexesCreateScripts = new ArrayList<>();

        final String scriptFileName = "generateTableKeyConstraintScript.sql";

        String scriptContent = loadScriptFromClasspath(scriptFileName);

        // create the db procedure
        try {
            createDatabaseProcedure(connection, scriptContent);
            for (String indexName : indexesNames) {
                CallableStatement callableStatement = null;
                ResultSet rs = null;
                try {
                    callableStatement = connection.prepareCall("{ call generateTableKeyConstraintScript(?,?,?) }");
                    callableStatement.setString(1, table.getTableSchema());
                    callableStatement.setString(2, table.getTableName());
                    callableStatement.setString(3, indexName);
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Executing SQL query: " + callableStatement.toString());
                    }
                    rs = callableStatement.executeQuery();
                    String createQuery = new String();
                    if (rs.next()) {
                        createQuery = rs.getString(1);
                    }
                    if (!StringUtils.isNullOrEmpty(createQuery) && !"NULL".equalsIgnoreCase(createQuery)) {
                        indexesCreateScripts.add(createQuery);
                    }

                } finally {
                    DbUtils.closeStatement(callableStatement);
                }
            }
        } catch (SQLException e) {
            throw new DbException(DbUtils.getFullSqlException("Error while generating Key constraints for table '"
                                                              + table.getFullTableName() + "'", e),
                                  e);
        } finally {
            // drop the newly created procedure
            executeUpdate("DROP PROCEDURE generateTableKeyConstraintScript", connection);
        }
        return indexesCreateScripts;
    }

    private List<String> generateIndexesCreateScripts( Connection connection, DbTable table ) {

        List<String> indexesNames = getAllIndexesNamesForTable(connection, table);
        List<String> indexesCreateScripts = new ArrayList<>();

        final String scriptFileName = "generateIndexCreateScript.sql";

        String scriptContent = loadScriptFromClasspath(scriptFileName);

        // create the db procedure
        try {
            createDatabaseProcedure(connection, scriptContent);
            for (String indexName : indexesNames) {
                CallableStatement callableStatement = null;
                ResultSet rs = null;
                try {
                    callableStatement = connection.prepareCall("{ call generateIndexCreateScript(?,?,?) }");
                    callableStatement.setString(1, table.getTableSchema());
                    callableStatement.setString(2, table.getTableName());
                    callableStatement.setString(3, indexName);
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Executing SQL query: " + callableStatement.toString());
                    }
                    rs = callableStatement.executeQuery();
                    String createQuery = new String();
                    if (rs.next()) {
                        createQuery = rs.getString(1);
                    }
                    if (!StringUtils.isNullOrEmpty(createQuery) && !"NULL".equalsIgnoreCase(createQuery)) {
                        indexesCreateScripts.add(createQuery);
                    }
                } finally {
                    DbUtils.closeStatement(callableStatement);
                }
            }
        } catch (SQLException e) {
            throw new DbException(DbUtils.getFullSqlException("Error while generating CREATE INDEX scripts for table '"
                                                              + table.getFullTableName() + "'", e),
                                  e);
        } finally {
            // drop the newly created procedure
            executeUpdate("DROP PROCEDURE generateIndexCreateScript", connection);
        }
        return indexesCreateScripts;
    }

    private List<String> getAllIndexesNamesForTable( Connection connection, DbTable table ) {

        List<String> indexesNames = new ArrayList<>();
        ResultSet rs = null;
        try {

            StringBuilder query = new StringBuilder();
            query.append("SELECT schemaName = sch.name, tableName = t.name, indexName = ind.name ")
                 .append("FROM sys.schemas sch ")
                 .append("INNER JOIN sys.tables t ON sch.schema_id = t.schema_id ")
                 .append("INNER JOIN sys.indexes ind ON ind.object_id = t.object_id ")
                 .append("WHERE sch.name = '" + table.getTableSchema() + "' ")
                 .append("AND t.name = '" + table.getTableName() + "' ")
                 .append("AND ind.name IS NOT NULL");
            if (LOG.isTraceEnabled()) {
                LOG.trace("Executing SQL query: " + query.toString());
            }
            rs = connection.createStatement().executeQuery(query.toString());
            while (rs.next()) {
                indexesNames.add(rs.getString("indexName"));
            }
        } catch (SQLException e) {
            throw new DbException(DbUtils.getFullSqlException("Error while obtaining ALL indexes' names for table '"
                                                              + table.getFullTableName() + "'", e),
                                  e);
        } finally {
            DbUtils.closeResultSet(rs);
        }
        return indexesNames;
    }

    private String getPartitionColumnName( DbTable table ) {

        String query = "select c.name " +
                       "from  sys.tables t " +
                       "left join  sys.indexes i " +
                       "      on(i.object_id = t.object_id " +
                       "      and i.index_id < 2) " +
                       "left join  sys.index_columns ic " +
                       "      on(ic.partition_ordinal > 0 " +
                       "      and ic.index_id = i.index_id and ic.object_id = t.object_id) " +
                       "left join  sys.columns c " +
                       "      on(c.object_id = ic.object_id " +
                       "      and c.column_id = ic.column_id) " +
                       "where t.object_id  = object_id('" + table.getFullTableName() + "') ";

        DbRecordValuesList[] rows = dbProvider.select(query);
        if (rows != null && rows.length > 0) {
            return (String) rows[0].get("name");
        } else {
            return "";
        }
    }

    private String getPartitionSchemeName( DbTable table ) {

        String query = "select DISTINCT " +
                       "ps.[name] AS Ps_Name " +
                       "from sys.partitions p " +
                       "left join sys.indexes i " +
                       " on p.[object_id] = i.[object_id] " +
                       " and p.index_id = i.index_id " +
                       "left JOIN sys.data_spaces ds " +
                       " on i.data_space_id = ds.data_space_id " +
                       "left JOIN sys.partition_schemes ps " +
                       " on ds.data_space_id = ps.data_space_id " +
                       "left JOIN sys.partition_functions pf " +
                       " on ps.function_id = pf.function_id " +
                       " where OBJECT_NAME(p.[object_id]) = '" + table.getTableName() + "' ";

        DbRecordValuesList[] rows = dbProvider.select(query);
        if (rows != null && rows.length > 0) {
            return (String) rows[0].get("Ps_Name");
        } else {
            return "";
        }
    }

    protected String getColumnsString(
                                       List<ColumnDescription> columns ) {

        StringBuilder columnsBuilder = new StringBuilder();

        //create the columns string
        for (ColumnDescription column : columns) {
            columnsBuilder.append('[' + column.getName());
            columnsBuilder.append("],");

        }
        //remove the last comma
        if (columnsBuilder.length() > 1) {
            columnsBuilder.delete(columnsBuilder.length() - 1, columnsBuilder.length());
        }

        return columnsBuilder.toString();
    }

    @Override
    protected String disableForeignKeyChecksStart() {

        // Disable all constraints
        return "EXEC sp_msforeachtable \"ALTER TABLE ? NOCHECK CONSTRAINT all\";" + EOL_MARKER
               + AtsSystemProperties.SYSTEM_LINE_SEPARATOR;
    }

    @Override
    protected String disableForeignKeyChecksEnd() {

        // Enable all constraints
        return "EXEC sp_msforeachtable @command1=\"ALTER TABLE ? WITH CHECK CHECK CONSTRAINT all\";"
               + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR;
    }

    @Override
    protected void writeDeleteStatements( Writer fileWriter ) throws IOException {

        if (this.includeDeleteStatements) {
            for (Entry<String, DbTable> entry : dbTables.entrySet()) {
                DbTable dbTable = entry.getValue();
                if (shouldDropTable(dbTable)) {
                    continue;
                }
                String fullTableName = null;
                if (dbTable != null) {
                    fullTableName = dbTable.getFullTableName();
                }
                String deleteQuery = "DELETE FROM " + fullTableName;
                fileWriter.write(deleteQuery + ";" + EOL_MARKER
                                 + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
            }
            this.deleteStatementsInserted = true;
        }

    }

    // extracts the specific value, considering it's type and the specifics associated with it
    private StringBuilder extractValue(
                                        ColumnDescription column,
                                        String fieldValue ) throws ParseException {

        if (fieldValue == null) {
            return new StringBuilder("NULL");
        }

        StringBuilder insertStatement = new StringBuilder();
        // non-string values. Should not be in quotes and do not need escaping
        if (column.isTypeNumeric()) {

            // BIT type stores only two types of values - 0 and 1, we need to
            // extract them and pass them back as string
            if (column.isTypeBit()) {
                // The value must be a hex number 0xnnnn
                if (fieldValue.startsWith(HEX_PREFIX_STR)) {
                    // value already in hex notation. This is because for BIT(>1) resultSet.getObject(col) currently
                    // returns byte[]
                    insertStatement.append(fieldValue);
                } else {
                    insertStatement.append(HEX_PREFIX_STR + fieldValue);
                }
            } else {
                insertStatement.append(fieldValue);
            }
        } else if (column.isTypeBinary()) {

            if (fieldValue.startsWith(HEX_PREFIX_STR)) {
                insertStatement.append(fieldValue);
            } else {
                insertStatement.append(HEX_PREFIX_STR + fieldValue);
            }
        } else {

            insertStatement.append('\'');
            insertStatement.append(fieldValue.replace("'", "''"));
            insertStatement.append('\'');
        }

        return insertStatement;
    }

    /**
     * Add table for backup.<br>If the table's schema is not specified, ATS will obtain and use the default schema for the user that will interact with the database during backup/restore
     * */
    @Override
    public void addTable( DbTable table ) {

        if (!StringUtils.isNullOrEmpty(defaultSchema)) {
            // apply that schema to each table that has no schema specified
            if (table != null) { // prevent NPE
                if (StringUtils.isNullOrEmpty(table.getTableSchema())) { // check if the table DOES NOT have schema already specified
                    table.setTableSchema(defaultSchema);
                }
            }
        }
        super.addTable(table);
    }

    /**
     * @see com.axway.ats.environment.database.model.RestoreHandler#restore(java.lang.String)
     */
    public void restore(
                         String backupFileName ) throws DatabaseEnvironmentCleanupException {

        BufferedReader backupReader = null;
        Connection connection = null;

        //we need to preserve the auto commit option, as the connections are pooled
        boolean isAutoCommit = true;

        try {
            LOG.info("Started restore of database backup from file '" + backupFileName + "'");

            backupReader = new BufferedReader(new FileReader(new File(backupFileName)));

            connection = ConnectionPool.getConnection(dbConnection);

            isAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            StringBuilder sql = new StringBuilder();
            String line = backupReader.readLine();
            while (line != null) {

                sql.append(line);

                if (line.startsWith(DROP_TABLE_MARKER)) {

                    String tableName = line.substring(DROP_TABLE_MARKER.length()).trim();
                    dropAndRecreateTable(connection, tableName);

                    sql = new StringBuilder(); // clear the sql query buffer
                } else if (line.endsWith(EOL_MARKER)) {

                    // remove the EOL marker
                    sql.delete(sql.length() - EOL_MARKER.length(), sql.length());
                    PreparedStatement updateStatement = connection.prepareStatement(sql.toString());

                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Executing SQL query: " + sql);
                    }

                    // catch the exception and rollback, otherwise we are locked
                    try {
                        updateStatement.execute();
                    } catch (SQLException sqle) {
                        //we have to roll back the transaction and re-throw the exception
                        connection.rollback();
                        throw new SQLException("Error executing restore satement: " + sql.toString(), sqle);
                    } finally {
                        DbUtils.closeStatement(updateStatement);
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

            try {
                //commit the transaction
                connection.commit();

            } catch (SQLException sqle) {
                //we have to roll back the transaction and re-throw the exception
                connection.rollback();
                throw sqle;
            }

            LOG.info("Completed restore of database backup from file '" + backupFileName + "'");
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
                LOG.error(ERROR_RESTORING_BACKUP + backupFileName, sqle);
            }
        }
    }

    /**
     * <p>Get the default schema for the current user and database.</p>
     * */
    private String getDefaultSchema() {

        // get user's default schema
        String query = "SELECT default_schema_name FROM sys.database_principals WHERE type = 'S' and name = '"
                       + dbProvider.getDbConnection().getUser() + "'";
        DbRecordValuesList[] defaultSchemaVal = dbProvider.select(new DbQuery(query),
                                                                  DbReturnModes.ESCAPED_STRING);
        if (defaultSchemaVal.length > 0) {
            DbRecordValuesList firstVal = defaultSchemaVal[0];
            if (firstVal != null) {
                String defaultSchema = (String) firstVal.get("default_schema_name");
                if (!StringUtils.isNullOrEmpty(defaultSchema)) {
                    LOG.info("Set default schema to '" + defaultSchema
                             + "'. Tables that do not have schema specified will be assigned to that schema."
                             + " Tables that do have provided schema will not be affected by that.");
                    return defaultSchema;
                }
            }
        }

        return null;

    }

    private void dropAndRecreateTable( Connection connection, String tableName ) {

        List<String> generateForeignKeysScripts = new ArrayList<String>();

        Map<String, List<String>> foreignKeys = getForeingKeys(tableName, connection);

        // generate script for restoring the exact table
        String generateTableScript = generateTableScript(tableName, connection);

        String scriptContent = loadScriptFromClasspath("generateForeignKeyScript.sql");

        // create the generateForeignKeyScript procedure
        createDatabaseProcedure(connection, scriptContent);

        for (Entry<String, List<String>> keyEntry : foreignKeys.entrySet()) {
            String parentTableName = keyEntry.getKey();
            for (String key : keyEntry.getValue()) {
                // generate scripts for creating all foreign keys
                generateForeignKeysScripts.add(generateForeignKeyScript(parentTableName, key,
                                                                        connection));
            }
        }

        // drop the newly created procedure
        executeUpdate("DROP PROCEDURE generateForeignKeyScript", connection);

        // drop the foreign keys
        dropForeignKeys(tableName, connection);
        // drop the table
        executeUpdate("DROP TABLE " + tableName + ";", connection);

        // create new table
        executeUpdate(generateTableScript, connection);
        // create all the missing foreign keys
        for (String script : generateForeignKeysScripts) {
            executeUpdate(script, connection);
        }
    }

    private Map<String, List<String>> getForeingKeys( String tableName,
                                                      Connection connection ) throws DbException {

        PreparedStatement stmnt = null;
        Map<String, List<String>> tableForeignKey = new LinkedHashMap<String, List<String>>();
        ResultSet rs = null;
        try {
            String simpleTableName = tableName.substring(tableName.indexOf('.') + 1, tableName.length());
            String query = "EXEC sp_fkeys '" + simpleTableName + "'";
            if (LOG.isTraceEnabled()) {
                LOG.trace("Executing SQL query: " + query);
            }
            stmnt = connection.prepareStatement(query);
            rs = stmnt.executeQuery();
            while (rs.next()) {
                String fKey = rs.getString("FK_NAME");
                String parentTableName = rs.getString("FKTABLE_OWNER") + "."
                                         + rs.getString("FKTABLE_NAME");
                if (tableName.equals(parentTableName)) {
                    // this is the same table, the foreign key is created in the table creation script
                    continue;
                }
                if (tableForeignKey.containsKey(parentTableName)) {
                    if (!tableForeignKey.get(parentTableName).contains(fKey)) {
                        tableForeignKey.get(parentTableName).add(fKey);
                    }
                } else {
                    List<String> fKeys = new ArrayList<String>();
                    fKeys.add(fKey);
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

    private void executeUpdate( String query, Connection connection ) throws DbException {

        PreparedStatement stmnt = null;
        try {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Executing SQL query: " + query);
            }
            stmnt = connection.prepareStatement(query);
            stmnt.executeUpdate();
        } catch (SQLException e) {
            throw new DbException("Error executing statement: '" + query + "'\n"
                                  + "  SQL errorCode=" + e.getErrorCode() + " sqlState=" + e.getSQLState() + " "
                                  + e.getMessage(), e);
        } finally {
            DbUtils.closeStatement(stmnt);
        }
    }

    private void createDatabaseProcedure( Connection conn, String scriptContent ) {

        StringBuilder command = new StringBuilder();
        Statement stmt = null;

        String currentLine;
        try (Scanner scanner = new Scanner(scriptContent)) {
            while (scanner.hasNextLine()) {
                currentLine = scanner.nextLine();
                currentLine = currentLine.trim();
                command.append(currentLine);
                command.append("\r\n");

                if (currentLine.endsWith("GO")) {
                    // commit the transaction
                    try {
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("Executing SQL query: " + command.toString());
                        }
                        stmt = conn.createStatement();
                        stmt.execute(command.toString());
                    } finally {
                        DbUtils.closeStatement(stmt);
                    }
                    command.setLength(0);
                }
            }
        } catch (Exception e) {
            String message = "Error while creating database procedure by running command: " + command;
            if (e instanceof SQLException) {
                throw new DbException(DbUtils.getFullSqlException(message, (SQLException) e));
            } else {
                throw new DbException(message, e);
            }
        }
        String commandStr = command.toString().trim();
        if (commandStr.length() > 0) {
            LOG.warn("Command '" + commandStr
                     + "' will not be executed. If it is needed then add 'GO' statement at the end");
        }
    }

    private String generateForeignKeyScript( String tableName, String foreingKey,
                                             Connection connection ) throws DbException {

        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {
            callableStatement = connection.prepareCall("{ call generateForeignKeyScript(?,?) }");
            callableStatement.setString(1, tableName);
            callableStatement.setString(2, StringUtils.isNullOrEmpty(foreingKey)
                                                                                 ? null
                                                                                 : foreingKey);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Executing SQL query: " + callableStatement.toString());
            }

            rs = callableStatement.executeQuery();
            String createQuery = new String();
            if (rs.next()) {
                createQuery = rs.getString(1);
            }

            return createQuery;

        } catch (Exception e) {
            throw new DbException("Error while generating script for the foreign keys of the table '"
                                  + tableName + "'.", e);
        } finally {
            DbUtils.closeStatement(callableStatement);
        }
    }

    private String generateTableScript( String tableName, Connection connection ) throws DbException {

        // script used from https://www.c-sharpcorner.com/UploadFile/67b45a/how-to-generate-a-create-table-script-for-an-existing-table/
        final String tableScriptFileName = "generateTableScript.sql";
        //String file = classLoader.getResource(tableName).getPath();

        String scriptContents = loadScriptFromClasspath(tableScriptFileName);

        // create the generateTableScript procedure
        createDatabaseProcedure(connection, scriptContents);

        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {
            callableStatement = connection.prepareCall("{ call generateTableScript(?) }");
            callableStatement.setString(1, tableName);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Executing SQL query: " + callableStatement.toString());
            }
            rs = callableStatement.executeQuery();
            String createQuery = new String();
            if (rs.next()) {
                createQuery = rs.getString(1);
            }

            return createQuery;

        } catch (Exception e) {
            throw new DbException("Error while generating script for the table '" + tableName + "'.", e);
        } finally {
            DbUtils.closeStatement(callableStatement);

            // drop the newly created procedure
            // TODO: drop procedure after last table drop invocation
            executeUpdate("DROP PROCEDURE generateTableScript", connection);

        }
    }

    private void dropForeignKeys( String tableName, Connection connection ) throws DbException {

        String query = "SELECT "
                       + "'ALTER TABLE [' +  OBJECT_SCHEMA_NAME(parent_object_id) + "
                       + "'].[' + OBJECT_NAME(parent_object_id) + "
                       + "'] DROP CONSTRAINT [' + name + ']' "
                       + " FROM sys.foreign_keys "
                       + " WHERE referenced_object_id = object_id('" + tableName + "')";

        Statement callableStatement = null;
        ResultSet rs = null;
        try {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Executing SQL query: " + query);
            }
            callableStatement = connection.createStatement();
            rs = callableStatement.executeQuery(query);

            while (rs.next()) {
                executeUpdate(rs.getString(1), connection);
            }

        } catch (Exception e) {
            throw new DbException("Error while droping the foreign keys of table '" + tableName + "'.", e);
        } finally {
            DbUtils.closeStatement(callableStatement);
        }
    }
}
