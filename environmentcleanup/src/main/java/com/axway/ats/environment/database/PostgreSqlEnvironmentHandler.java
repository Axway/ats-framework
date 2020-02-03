/*
 * Copyright 2020 Axway Software
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

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.dbaccess.*;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.dbaccess.postgresql.DbConnPostgreSQL;
import com.axway.ats.core.dbaccess.postgresql.PostgreSqlColumnDescription;
import com.axway.ats.core.dbaccess.postgresql.PostgreSqlProvider;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.environment.database.exceptions.ColumnHasNoDefaultValueException;
import com.axway.ats.environment.database.exceptions.DatabaseEnvironmentCleanupException;
import com.axway.ats.environment.database.model.DbTable;
import org.apache.log4j.Logger;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL DB implementation of the environment handler
 */
class PostgreSqlEnvironmentHandler extends AbstractEnvironmentHandler {

    private static final Logger LOG            = Logger.getLogger(PostgreSqlEnvironmentHandler.class);
    private static final String HEX_PREFIX_STR = "\\x";

    /**
     * Constructor
     *
     * @param dbConnection the database connection
     */
    PostgreSqlEnvironmentHandler( DbConnPostgreSQL dbConnection,
                                  PostgreSqlProvider dbProvider ) {

        super(dbConnection, dbProvider);
    }

    public void restore( String backupFileName ) throws DatabaseEnvironmentCleanupException {

        BufferedReader backupReader = null;
        Connection connection = null;

        // used to preserve the initial auto commit option, as the connections are pooled
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

                if (line.startsWith("--")) {
                    LOG.debug("Skipping commented line: " + line);
                } else {
                    sql.append(line);

                    if (line.startsWith(DROP_TABLE_MARKER)) {

                        String table = line.substring(DROP_TABLE_MARKER.length()).trim();
                        String owner = table.substring(0, table.indexOf("."));
                        String simpleTableName = table.substring(table.indexOf(".") + 1);
                        dropAndRecreateTable(connection, simpleTableName, owner);

                    }
                    if (line.endsWith(EOL_MARKER)) {

                        // remove the EOL marker
                        sql.delete(sql.length() - EOL_MARKER.length(), sql.length());
                        PreparedStatement updateStatement = connection.prepareStatement(sql.toString());
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("About to execute restore SQL statement: " + sql.toString());
                        }

                        //catch the exception and roll back, otherwise we are locked
                        try {
                            updateStatement.execute();

                        } catch (SQLException sqle) {
                            //we have to roll back the transaction and re throw the exception
                            connection.rollback();
                            throw new SQLException("Error invoking restore statement: " + sql.toString(), sqle);
                        } finally {
                            try {
                                updateStatement.close();
                            } catch (SQLException sqle) {
                                LOG.error("Unable to close prepared statement", sqle);
                            }
                        }
                        sql.delete(0, sql.length());
                    } else {
                        // Add a new line.  Note: this code will add the current system line ending
                        sql.append(AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                    }
                }

                line = backupReader.readLine();
            }

            try {
                //commit the transaction
                connection.commit();

            } catch (SQLException sqle) {
                //we have to roll back the transaction and re throw the exception
                connection.rollback();
                throw sqle;
            }

            LOG.info("Completed restore of database backup from file '" + backupFileName + "'");

        } catch (IOException | DbException ex) {
            throw new DatabaseEnvironmentCleanupException("Could not restore backup from file "
                                                          + backupFileName, ex);
        } catch (SQLException ex) {
            throw new DatabaseEnvironmentCleanupException("Could not restore backup from file "
                                                          + backupFileName
                                                          + ".\n Details of full SQL exception follow: "
                                                          + DbUtils.getFullSqlException("SQLException", ex));
        } finally {
            try {
                IoUtils.closeStream(backupReader, "Could not close reader for backup file "
                                                  + backupFileName);
                if (connection != null) {
                    connection.setAutoCommit(isAutoCommit);
                    connection.close();
                }
            } catch (SQLException sqle) {
                LOG.error("Could not reset autocommit state and close DB connection", sqle);
            }
        }
    }

    @Override
    protected List<ColumnDescription> getColumnsToSelect(
            DbTable table,
            String userName ) throws DbException,
                                     ColumnHasNoDefaultValueException {

        String tableName = table.getTableName(); // just table name, w/o schema
        String schemaName = table.getTableSchema();
        if (StringUtils.isNullOrEmpty(table.getTableSchema())) {
            schemaName = "public"; // public schema in the default schema if not specified. TODO: could also check search_path
        }
        // Alternative: check with DatabaseMetaData

        String selectColumnsInfo = "SELECT column_name, data_type, is_nullable, column_default "
                                   + " FROM information_schema.columns "
                                   + " WHERE table_name = '" + tableName + "' AND table_schema = '" + schemaName + "'"
                                   + " ORDER BY ordinal_position"; // Use SQL standards defined system schema

        ArrayList<ColumnDescription> columnsToSelect = new ArrayList<>();
        DbRecordValuesList[] columnsMetaData;
        try {
            columnsMetaData = this.dbProvider.select(selectColumnsInfo);
        } catch (DbException e) {
            throw new DbException("Could not get columns for table " + table.getFullTableName()
                                  + ". Check if the table exists and that the user has permissions. See more details in the trace.",
                                  e);
        }

        // the Identity column currently not reported. See details below
        table.setIdentityColumnPresent(false);
        for (DbRecordValuesList columnMetaData : columnsMetaData) {

            String columnName = (String) columnMetaData.get("column_name");

            //check if the column should be skipped in the backup
            if (!table.getColumnsToExclude().contains(columnName)) {

                ColumnDescription colDescription = new PostgreSqlColumnDescription(columnName,
                                                                                   (String) columnMetaData.get(
                                                                                           "data_type"));
                columnsToSelect.add(colDescription);
                /* is_identity" This column is available, but not populated by PostgreSQL, details https://www.postgresql.org/docs/current/infoschema-columns.html
                   Object isIdentityObj = columnMetaData.get("is_identity");
                   table.setIdentityColumnPresent(true/false)
                   */
            } else {
                // if this column has no default value, we cannot skip it in the backup
                if (columnMetaData.get("column_default") == null) {
                    LOG.error(
                            "Cannot skip column named " + columnName + " with no default values while creating backup");
                    throw new ColumnHasNoDefaultValueException(table.getFullTableName(), columnName);
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
            Writer fileWriter ) throws IOException {

        String fullTableName = getFullTableName(table);
        // TODO: SET search_path TO my_schema; could be used to mention only once the schema instead of referring it many times
        //      https://www.postgresql.org/docs/current/ddl-schemas.html#DDL-SCHEMAS-PATH
        boolean writeDeleteStatementForCurrTable = true;
        if (shouldDropTable(table)) {
            throw new IllegalStateException("Not implemented");
            /*TODO: check
               fileWriter.write(DROP_TABLE_MARKER + table.getTableSchema() + "." + table.getTableName()
                             + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
            Connection connection = null;
            try {
                connection = ConnectionPool.getConnection(dbConnection);
                writeDropTableStatements(fileWriter, fullTableName, connection);
                writeDeleteStatementForCurrTable = false;
            } finally {
                DbUtils.closeConnection(connection);
            }
            */
        } /*else if (!this.deleteStatementsInserted) { // moved later after LOCK TABLE
            writeDeleteStatements(fileWriter);
          }*/

        if (this.addLocks && table.isLockTable()) {
            // LOCK this single table for update. Lock is released after delete and then insert of backup data.
            // This leads to less potential data integrity issues. If another process updates tables at same time
            // LOCK at once for ALL tables is not applicable as in reality DB connection hangs/blocked

            // PgSQL: ACCESS EXCLUSIVE is most restricted mode
            // https://www.postgresql.org/docs/current/explicit-locking.html
            fileWriter.write("LOCK TABLE " + fullTableName + " IN EXCLUSIVE MODE;" + EOL_MARKER
                             + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
        }
        if (this.includeDeleteStatements && writeDeleteStatementForCurrTable) {
            fileWriter.write("DELETE FROM " + fullTableName + ";" + EOL_MARKER
                             + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
        }

        if (table.getAutoIncrementResetValue() != null) {
            fileWriter.write("TODO: fixme SET SESSION sql_mode='NO_AUTO_VALUE_ON_ZERO';" + EOL_MARKER
                             + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
            fileWriter.write("TODO: fixme ALTER TABLE " + fullTableName + " AUTO_INCREMENT = "
                             + table.getAutoIncrementResetValue() + ";" + EOL_MARKER
                             + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
        }

        if (records.length > 0) {

            StringBuilder insertStatement = new StringBuilder();

            String insertBegin = "INSERT INTO " + getFullTableName(table) + " ("
                                 + getColumnsString(columns) + ") VALUES(";
            String insertEnd = ");" + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR;

            for (DbRecordValuesList record : records) {

                // clear the StringBuilder current data
                // it is a little better (almost the same) than insertStatement.setLength( 0 ); as performance
                insertStatement.delete(0, insertStatement.length());
                insertStatement.append(insertBegin);

                for (int i = 0; i < record.size(); i++) {

                    DbRecordValue recordValue = record.get(i);
                    String fieldValue = (String) recordValue.getValue();
                    if (fieldValue == null) {
                        fieldValue = "NULL";
                    }
                    // extract specific values depending on their type
                    insertStatement.append(extractValue(columns.get(i), fieldValue));
                    insertStatement.append(",");
                }
                //remove the last comma
                insertStatement.delete(insertStatement.length() - 1, insertStatement.length());
                insertStatement.append(insertEnd);

                fileWriter.write(insertStatement.toString());
                fileWriter.flush();
            }
        }

        // unlock table - no UNLOCK table command. Unlock is automatic at the end of transaction
        fileWriter.write(AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
    }

    /**
     * Build full table name from schema and table.
     * @param table table object
     * @return Full SQL-friendly name "schema_name"."table_name"
     */
    private String getFullTableName( DbTable table ) {

        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        if (StringUtils.isNullOrEmpty(table.getTableSchema())) {
            sb.append("public");
        } else {
            sb.append(table.getTableSchema());
        }
        sb.append("\".\"");
        sb.append(table.getTableName());
        sb.append("\"");
        return sb.toString();
    }

    private void writeDropTableStatements( Writer fileWriter, String fullTableName,
                                           Connection connection ) throws IOException {

        // generate script for restoring the exact table
        String generateTableScript = generateTableScript(fullTableName, connection);

        // drop the table
        fileWriter.write("DROP TABLE " + fullTableName + ";" + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);

        // create new table
        fileWriter.write(generateTableScript + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);

    }

    @Override
    protected void writeDeleteStatements( Writer fileWriter ) {
        // Empty. Currently delete and lock are handled PER table in writeTableToFile(). Reused design from MySQL case
    }

    // escapes the characters in the value string, according to the MySQL manual. This
    // method escape each symbol *even* if the symbol itself is part of an escape sequence
    protected String escapeValue( String fieldValue ) {

        StringBuilder result = new StringBuilder();
        for (char currentCharacter : fieldValue.toCharArray()) {
            // Currently seems that " and \ should not be escaped
            // double quote result.append("\\\"");
            // \ backslash result.append("\\\\");
            if (currentCharacter == '\'') {
                result.append("''");
            } else {
                result.append(currentCharacter);
            }
        }

        return result.toString();
    }

    /**
     * Extracts the specific value, considering it's type and the specifics associated with it
     *
     * @param column the column description
     * @param fieldValue the value of the field as String
     * @return the value as it should be represented in the backup
     */
    private StringBuilder extractValue(
            ColumnDescription column,
            String fieldValue ) {

        StringBuilder insertStatement = new StringBuilder();
        if (LOG.isDebugEnabled()) {
            LOG.info("Getting backup-friendly string for DB value: '" + fieldValue + "' for column " + column + ".");
        }

        if ("NULL".equals(fieldValue)) {
            insertStatement.append(fieldValue);
        } else if (column.isTypeNumeric()) {
            // non-string values. Should not be in quotes and do not need escaping

            // BIT type stores only two types of values - 0 and 1, we need to
            // extract them and pass them back as string
            if (column.isTypeBit()) {
                // BIT type has possibility to store fixed length or varying bits. Represent value like: B'101'
                // https://www.postgresql.org/docs/current/datatype-bit.html
                if (true) {
                    throw new IllegalStateException("Not implemented. Use case not provided yet.");
                }
                if (fieldValue.startsWith(HEX_PREFIX_STR)) {
                    // value already in hex notation. This is because for BIT(>1) resultSet.getObject(col) currently
                    // returns byte[]
                    insertStatement.append(fieldValue);
                } else {
                    insertStatement.append(HEX_PREFIX_STR);
                    long bitsInLong = -1;
                    try {
                        bitsInLong = Long.parseLong(fieldValue);
                        if (bitsInLong < 0) { // overflow for BIT(64) case
                            LOG.error(new IllegalArgumentException("Bit value '" + fieldValue
                                                                   + "' is too large for parsing."));
                        }
                    } catch (NumberFormatException e) {
                        LOG.error("Error parsing bit representation to long for field value: '" + fieldValue
                                  + "', DB column " + column.toString()
                                  + ". Check if you are running with old JDBC driver.", e);
                    }
                    insertStatement.append(Long.toHexString(bitsInLong));
                }
            } else {
                insertStatement.append(fieldValue);
            }
        } else if (column.isTypeBinary()) {
            // value is already in hex mode. In PostgreSQL apostrophes should  be also used as boundaries: Example: '\x2B3C0A'
            insertStatement.append("'" + fieldValue + "'");
        } else {
            // String variant. Needs escaping of possible special chars like ', \, "
            insertStatement.append("'");
            insertStatement.append(escapeValue(fieldValue));
            insertStatement.append("'");
        }

        return insertStatement;
    }

    /**
     * Start block for deferring constraint checks because of possible foreign key violations if the order of restore
     * ( inserts) is not correct. Also this is only possible way if there are cyclic references between 2 tables.
     * Disable ALL triggers is not preferred way.
     *
     * <p>For details you may check:
     * <ul>
     *     <li>1st - https://www.postgresql.org/docs/current/sql-set-constraints.html</li>
     *     <li>2nd - https://begriffs.com/posts/2017-08-27-deferrable-sql-constraints.html</li>
     * </ul></p>
     * @return returns SQL statement for the start defer block
     */
    @Override
    protected String disableForeignKeyChecksStart() {

        return "SET CONSTRAINTS ALL DEFERRED;" + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR;
    }

    @Override
    protected String disableForeignKeyChecksEnd() {
        // Possibility to remove this statement as TX commit should also revert behavior
        return "SET CONSTRAINTS ALL IMMEDIATE;" + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR;
    }

    // DROP table (fast cleanup) functionality methods
    private void dropAndRecreateTable( Connection connection, String table, String schema ) {

        String tableName = schema + "." + table;
        // generate script for restoring the exact table
        String generateTableScript = generateTableScript(tableName, connection);

        // drop the table
        executeUpdate("DROP TABLE " + tableName + ";", connection);

        // create new table
        executeUpdate(generateTableScript, connection);
    }

    /**
     * Used for Drop table functionality.
     * @param fullTableName "schema"."table"
     * @param connection DB connection
     * @return constructed generate table script
     * @throws DbException in case of an error
     */
    private String generateTableScript( String fullTableName, Connection connection ) throws DbException {
        // TODO - not supported yet
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {
            String query = "TODO: SHOW CREATE TABLE " + fullTableName + ";";
            callableStatement = connection.prepareCall(query);

            rs = callableStatement.executeQuery();
            String createQuery = new String();
            if (rs.next()) {
                createQuery = rs.getString(2);
            }

            return createQuery;

        } catch (Exception e) {
            throw new DbException("Error while generating script for the table '" + fullTableName + "'.", e);
        } finally {
            DbUtils.closeStatement(callableStatement);
        }
    }

    private void executeUpdate( String query, Connection connection ) throws DbException {

        PreparedStatement stmnt = null;
        try {
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
