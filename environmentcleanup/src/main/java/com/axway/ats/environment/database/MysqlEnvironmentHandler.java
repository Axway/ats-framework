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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.dbaccess.ColumnDescription;
import com.axway.ats.core.dbaccess.ConnectionPool;
import com.axway.ats.core.dbaccess.DbRecordValue;
import com.axway.ats.core.dbaccess.DbRecordValuesList;
import com.axway.ats.core.dbaccess.DbUtils;
import com.axway.ats.core.dbaccess.MysqlColumnDescription;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.dbaccess.mysql.DbConnMySQL;
import com.axway.ats.core.dbaccess.mysql.MysqlDbProvider;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.environment.database.exceptions.ColumnHasNoDefaultValueException;
import com.axway.ats.environment.database.exceptions.DatabaseEnvironmentCleanupException;
import com.axway.ats.environment.database.model.DbTable;
import com.axway.ats.environment.database.mysql.MysqlColumnNames;

/**
 * MySQL implementation of the environment handler
 */
class MysqlEnvironmentHandler extends AbstractEnvironmentHandler {

    private static final Logger log            = LogManager.getLogger(MysqlEnvironmentHandler.class);
    private static final String HEX_PREFIX_STR = "0x";
    private boolean             isJDBC4;

    /**
     * Constructor
     *
     * @param dbConnection the database connection
     */
    MysqlEnvironmentHandler( DbConnMySQL dbConnection,
                             MysqlDbProvider dbProvider ) {

        super(dbConnection, dbProvider);
        isJDBC4 = checkDriverVersion(dbProvider);
    }

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
            while (line != null) {

                sql.append(line);
                if (line.startsWith(DROP_TABLE_MARKER)) {

                    String table = line.substring(DROP_TABLE_MARKER.length()).trim();
                    String owner = table.substring(0, table.indexOf("."));
                    String simpleTableName = table.substring(table.indexOf(".") + 1);
                    dropAndRecreateTable(connection, simpleTableName, owner);

                }
                if (line.endsWith(EOL_MARKER)) {

                    PreparedStatement updateStatement = null;

                    // remove the OEL marker
                    sql.delete(sql.length() - EOL_MARKER.length(), sql.length());
                    if (sql.toString().trim().startsWith("INSERT INTO")) {
                        // This line escapes non-printable string chars. Hex data is already escaped as 0xABC without backslash(\)
                        String insertQuery = sql.toString().replace("\\0x", "\\");
                        updateStatement = connection.prepareStatement(insertQuery);
                    } else {
                        updateStatement = connection.prepareStatement(sql.toString());
                    }

                    //catch the exception and roll back, otherwise we are locked
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
                    sql.delete(0, sql.length());
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
                //we have to roll back the transaction and re throw the exception
                connection.rollback();
                throw sqle;
            }

            log.info("Completed restore of database backup from file '" + backupFileName + "'");

        } catch (IOException ioe) {
            throw new DatabaseEnvironmentCleanupException("Could not restore backup from file "
                                                          + backupFileName, ioe);
        } catch (SQLException sqle) {
            throw new DatabaseEnvironmentCleanupException("Could not restore backup from file "
                                                          + backupFileName, sqle);
        } catch (DbException dbe) {
            throw new DatabaseEnvironmentCleanupException("Could not restore backup from file "
                                                          + backupFileName, dbe);
        } finally {
            try {
                IoUtils.closeStream(backupReader, "Could not close reader for backup file "
                                                  + backupFileName);
                if (connection != null) {
                    connection.setAutoCommit(isAutoCommit);
                    connection.close();
                }
            } catch (SQLException sqle) {
                log.error("Could close DB connection");
            }
        }
    }

    @Override
    protected List<ColumnDescription> getColumnsToSelect(
                                                          DbTable table,
                                                          String userName ) throws DbException,
                                                                            ColumnHasNoDefaultValueException {

        // TODO Might be replaced with JDBC DatabaseMetaData.getColumns() but should be verified with default values
        ArrayList<ColumnDescription> columnsToSelect = new ArrayList<ColumnDescription>();
        DbRecordValuesList[] columnsMetaData = null;
        try {
            columnsMetaData = dbProvider.select("SHOW COLUMNS FROM " + table.getTableName());
        } catch (DbException e) {
            throw new DbException("Could not get columns for table " + table.getTableName()
                                  + ". Check if the table is existing and that the user has permissions. See more details in the trace.");
        }

        for (DbRecordValuesList columnMetaData : columnsMetaData) {

            String columnName = (String) columnMetaData.get(MysqlColumnNames.COLUMN_NAME.getName(isJDBC4));

            //check if the column should be skipped in the backup
            if (!table.getColumnsToExclude().contains(columnName)) {

                ColumnDescription colDescription = new MysqlColumnDescription(columnName,
                                                                              (String) columnMetaData.get(MysqlColumnNames.COLUMN_TYPE.getName(isJDBC4)));

                columnsToSelect.add(colDescription);
            } else {
                //if this column has no default value, we cannot skip it in the backup
                if (columnMetaData.get(MysqlColumnNames.DEFAULT_COLUMN.getName(isJDBC4)) == null) {
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
                                     Writer fileWriter ) throws IOException {

        boolean writeDeleteStatementForCurrTable = true;
        if (shouldDropTable(table)) {
            /*fileWriter.write(DROP_TABLE_MARKER + table.getTableSchema() + "." + table.getTableName()
                             + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);*/
            Connection connection = null;
            try {
                connection = ConnectionPool.getConnection(dbConnection);
                writeDropTableStatements(fileWriter, table.getFullTableName(), connection);
                writeDeleteStatementForCurrTable = false;
            } finally {
                DbUtils.closeConnection(connection);
            }

        } /*else if (!this.deleteStatementsInserted) {
            writeDeleteStatements(fileWriter);
          }*/

        // LOCK this single table for update. Lock is released after delete and then insert of backup data. 
        // This leads to less potential data integrity issues. If another process updates tables at same time
        // LOCK at once for all tables is not applicable as in reality DB connection hangs
        if (this.addLocks && table.isLockTable()) {
            fileWriter.write("LOCK TABLES `" + table.getTableName() + "` WRITE;" + EOL_MARKER
                             + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
        }
        if (this.includeDeleteStatements && writeDeleteStatementForCurrTable) {
            fileWriter.write("DELETE FROM `" + table.getTableName() + "`;" + EOL_MARKER
                             + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
        }

        if (table.getAutoIncrementResetValue() != null) {
            fileWriter.write("SET SESSION sql_mode='NO_AUTO_VALUE_ON_ZERO';" + EOL_MARKER
                             + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
            fileWriter.write("ALTER TABLE `" + table.getTableName() + "` AUTO_INCREMENT = "
                             + table.getAutoIncrementResetValue() + ";" + EOL_MARKER
                             + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);

            // If the table was locked, after using ALTER TABLE it becomes unlocked and will throw an error.
            // ( explained here: http://dev.mysql.com/doc/refman/5.0/en/alter-table-problems.html )
            // To handle this, lock the table again
            if (this.addLocks && table.isLockTable()) {
                fileWriter.write("LOCK TABLES `" + table.getTableName() + "` WRITE;" + EOL_MARKER
                                 + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);

            }
        }

        if (records.length > 0) {

            StringBuilder insertStatement = new StringBuilder();

            String insertBegin = "INSERT INTO `" + table.getTableName() + "` (" + getColumnsString(columns)
                                 + ") VALUES(";
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

                fileWriter.write(StringUtils.escapeNonPrintableAsciiCharacters(insertStatement.toString()));
                fileWriter.flush();
            }
        }

        // unlock table
        if (this.addLocks && table.isLockTable()) {
            fileWriter.write("UNLOCK TABLES;" + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
        }
        fileWriter.write(AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
    }

    private void writeDropTableStatements( Writer fileWriter, String fullTableName,
                                           Connection connection ) throws IOException {

        String tableName = fullTableName;
        // generate script for restoring the exact table
        String generateTableScript = generateTableScript(tableName, connection);

        // drop the table
        fileWriter.write("DROP TABLE " + tableName + ";" + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);

        // create new table
        fileWriter.write(generateTableScript + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);

    }

    @Override
    protected void writeDeleteStatements( Writer fileWriter ) throws IOException {

        // Empty. Delete and lock are handled per table in writeTableToFile 
    }

    // escapes the characters in the value string, according to the MySQL manual. This
    // method escape each symbol *even* if the symbol itself is part of an escape sequence
    protected String escapeValue( String fieldValue ) {

        StringBuilder result = new StringBuilder();
        for (char currentCharacter : fieldValue.toCharArray()) {
            // double quote
            if (currentCharacter == '"') {
                result.append("\\\"");
                // single quote
            } else if (currentCharacter == '\'') {
                result.append("''");
                // backslash
            } else if (currentCharacter == '\\') {
                result.append("\\\\");
                // any other character
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
        if (log.isTraceEnabled()) {
            log.trace("Getting backup-friendly string for db value: '" + fieldValue + "' for column "
                      + column + ".");
        }

        if ("NULL".equals(fieldValue)) {
            insertStatement.append(fieldValue);
        } else if (column.isTypeNumeric()) {
            // non-string values. Should not be in quotes and do not need escaping

            // BIT type stores only two types of values - 0 and 1, we need to
            // extract them and pass them back as string
            if (column.isTypeBit()) {
                // MySQL BIT type has possibility to store 1-64 bits. Represent value as hex number 0xnnnn
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
                            log.error(new IllegalArgumentException("Bit value '" + fieldValue
                                                                   + "' is too large for parsing."));
                        }
                    } catch (NumberFormatException e) {
                        log.error("Error parsing bit representation to long for field value: '" + fieldValue
                                  + "', DB column " + column.toString()
                                  + ". Check if you are running with old JDBC driver.", e);
                    }
                    insertStatement.append(Long.toHexString(bitsInLong));
                }
            } else {
                insertStatement.append(fieldValue);
            }
        } else if (column.isTypeBinary()) {
            // value is already in hex mode. In MySQL apostrophes should not be used as boundaries
            insertStatement.append(fieldValue);
        } else {
            // String variant. Needs escaping
            insertStatement.append("'");
            fieldValue = escapeValue(fieldValue);
            insertStatement.append(fieldValue);
            insertStatement.append("'");
        }

        return insertStatement;
    }

    @Override
    protected String disableForeignKeyChecksStart() {

        return "SET FOREIGN_KEY_CHECKS = 0;" + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR;
    }

    @Override
    protected String disableForeignKeyChecksEnd() {

        return "";
        // TODO: disable foreign checks in current way is session-wide and code relies that at the end it should be reset.
        // better use this explicitly to know potential errors on commit
        // return "SET FOREIGN_KEY_CHECKS = 1;" + EOL_MARKER + ATSSystemProperties.SYSTEM_LINE_SEPARATOR;
    }

    private boolean checkDriverVersion( MysqlDbProvider dbProvider ) {

        try {
            Connection connection = dbProvider.getConnection();
            DatabaseMetaData dmd = connection.getMetaData();
            int majorVersion = dmd.getDriverMajorVersion();
            int minorVersion = dmd.getDriverMinorVersion();
            log.info(new StringBuilder().append("JDBC driver used is : ")
                                        .append(majorVersion)
                                        .append(".")
                                        .append(minorVersion)
                                        .toString());

            // The older specification is used in drivers prior to 5.0 including 5.0
            if (majorVersion < 5) {
                return false;
            } else if (majorVersion == 5 && minorVersion == 0) {
                return false;
            }
        } catch (SQLException e) {
            log.error("Unable to determine driver version, falling back to JDBC3 specs", e);
            return false;
        } catch (DbException e) {
            log.error("Unable to determine driver version, falling back to JDBC3 specs", e);
            return false;
        }
        return true;
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

    private String generateTableScript( String tableName, Connection connection ) throws DbException {

        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {
            String query = "SHOW CREATE TABLE " + tableName + ";";
            callableStatement = connection.prepareCall(query);

            rs = callableStatement.executeQuery();
            String createQuery = new String();
            if (rs.next()) {
                createQuery = rs.getString(2);
            }

            return createQuery;

        } catch (Exception e) {
            throw new DbException("Error while generating script for the table '" + tableName + "'.", e);
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
