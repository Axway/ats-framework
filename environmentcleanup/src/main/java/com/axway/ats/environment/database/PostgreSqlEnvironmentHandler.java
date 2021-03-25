/*
 * Copyright 2020-2021 Axway Software
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

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
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.dbaccess.postgresql.DbConnPostgreSQL;
import com.axway.ats.core.dbaccess.postgresql.PostgreSqlColumnDescription;
import com.axway.ats.core.dbaccess.postgresql.PostgreSqlDbProvider;
import com.axway.ats.core.utils.ExceptionUtils;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.environment.database.exceptions.ColumnHasNoDefaultValueException;
import com.axway.ats.environment.database.exceptions.DatabaseEnvironmentCleanupException;
import com.axway.ats.environment.database.model.DbTable;

/**
 * PostgreSQL DB implementation of the environment handler
 */
class PostgreSqlEnvironmentHandler extends AbstractEnvironmentHandler {

    protected static final String PRIMARY_KEY_INDEX                         = "PRIMARY_KEY_INDEX";
    protected static final String FOREIGN_KEYS_INDEXES                      = "FOREIGN_KEYS_INDEXES";
    protected static final String GENERAL_INDEXES                           = "GENERAL_INDEXES";
    protected static final String CONSTRAINTS                               = "CONSTRAINTS";

    private static final Logger   LOG                                       = LogManager.getLogger(PostgreSqlEnvironmentHandler.class);
    private static final String   HEX_PREFIX_STR                            = "\\x";

    // see getForeignKeysReferencingTable() method for details
    private static boolean        logForeignKeyQueryFailure                 = true;

    // used only when at least one table is about to be dropped
    // keeps track of which FOREIGN KEY INDEX is created, so we do not end up with duplication error if we try to create it one more time
    private Set<String>           alreadyCreatedForeignKeys                 = new LinkedHashSet<String>();

    private Map<String, String>   allDropTablesPrimaryKeysNames             = new HashMap<String, String>();

    private Set<String>           allPartitionsIndexesAndConstraintsScripts = new LinkedHashSet<String>();

    /**
     * Constructor
     *
     * @param dbConnection the database connection
     */
    PostgreSqlEnvironmentHandler( DbConnPostgreSQL dbConnection,
                                  PostgreSqlDbProvider dbProvider ) {

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

                    /*if (line.startsWith(DROP_TABLE_MARKER)) {
                    
                        String table = line.substring(DROP_TABLE_MARKER.length()).trim();
                        String owner = table.substring(0, table.indexOf("."));
                        String simpleTableName = table.substring(table.indexOf(".") + 1);
                        dropAndRecreateTable(connection, simpleTableName, owner);
                    
                    }*/
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
                              "Cannot skip column named " + columnName
                              + " with no default values while creating backup");
                    throw new ColumnHasNoDefaultValueException(table.getFullTableName(), columnName);
                }
            }
        }

        return columnsToSelect;
    }

    @Override
    protected void writeBackupToFile( Writer fileWriter ) throws IOException,
                                                          DatabaseEnvironmentCleanupException,
                                                          DbException, ParseException {

        // write DROP TABLE
        // write DISABLE TRIGGER ALL for tables which are to be DELETEd
        // write DELETE statements
        // write CREATE TABLE for the dropped tables
        // write SET OWNER for the dropped tables
        // write CREATE TABLE PARTITION OF for each dropped table, that is partitioned
        // write INSERT statements for all tables, that are to be backed-up (lock each table first. no unlock needed)
        // write PK for those partitions
        // attach those PK to the parent table
        // write other indexes for the partitioned table
        // attache those indexes to the parent table
        // write PK for all dropped tables
        // write CONSTRAINT(s) for all dropped tables
        // write other indexes for all dropped tables
        // write FKs for all dropped tables
        // write FKs which references any of the dropped tables
        // write ATTACH scripts for all partitions indexes
        // write INSERT statements
        // write ENABLE TRIGGER ALL for all deleted tables

        if (disableForeignKeys) {
            fileWriter.write(disableForeignKeyChecksStart());
        }

        // write CREATE TABLE statements
        Connection connection = null;
        try {
            connection = ConnectionPool.getConnection(dbConnection);

            // table name -> index type -> index script
            Map<String, Map<String, Set<String>>> tablesScripts = new LinkedHashMap<String, Map<String, Set<String>>>();

            // get ALL CREATE INDEX statements
            for (Entry<String, DbTable> entry : dbTables.entrySet()) {
                if (shouldDropTable(entry.getValue())) {
                    String fullTableName = getFullTableName(entry.getValue());
                    Map<String, Set<String>> scripts = getTableIndexesScripts(getFullTableName(entry.getValue()),
                                                                              connection);
                    tablesScripts.put(fullTableName, scripts);

                }
            }

            // write DROP statements
            for (Entry<String, DbTable> entry : dbTables.entrySet()) {
                if (shouldDropTable(entry.getValue())) {
                    // DROP TABLE <TABLE_NAME> CASCADE
                    fileWriter.write("DROP TABLE " + getFullTableName(entry.getValue()) + " CASCADE;" + EOL_MARKER
                                     + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                }
            }

            // WRITE ALTER TABLE ONLY <TABLE_SCHEMA>.<TABLE> DISABLE TRIGGER ALL
            // SET session_replication_role = replica;
            fileWriter.write("SET session_replication_role = replica;" + EOL_MARKER
                             + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
            /*for (Entry<String, DbTable> entry : dbTables.entrySet()) {
                if (!shouldDropTable(entry.getValue())) {
                    fileWriter.write("ALTER TABLE ONLY " + getFullTableName(entry.getValue()) + " DISABLE TRIGGER ALL;"
                                     + EOL_MARKER
                                     + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                }
            
            }*/

            // write DELETE statements
            if (this.includeDeleteStatements) {
                for (Entry<String, DbTable> entry : dbTables.entrySet()) {
                    if (!shouldDropTable(entry.getValue())) {
                        fileWriter.write("DELETE FROM " + getFullTableName(entry.getValue()) + ";" + EOL_MARKER
                                         + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                    }
                }

            }

            for (Entry<String, DbTable> entry : dbTables.entrySet()) {
                if (shouldDropTable(entry.getValue())) {
                    String fullTableName = getFullTableName(entry.getValue());
                    // CREATE TABLE <TABLE_NAME>
                    String createTableScript = generateCreateTableScript(fullTableName,
                                                                         connection);
                    fileWriter.write(createTableScript + EOL_MARKER
                                     + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                    String setOwnerScript = "ALTER TABLE " + getFullTableName(entry.getValue()) + " OWNER to \""
                                            + dbConnection.getUser() + "\";";
                    fileWriter.write(setOwnerScript + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);

                    // write CREATE TABLE <TABLE_NAME> PARTITION OF (if needed)
                    List<String> tablePartitions = getTablePartitionsScript(fullTableName,
                                                                            allDropTablesPrimaryKeysNames.get(fullTableName),
                                                                            tablesScripts.get(fullTableName)
                                                                                         .get(GENERAL_INDEXES),
                                                                            connection);
                    if (tablePartitions != null && !tablePartitions.isEmpty()) {
                        for (String partition : tablePartitions) {
                            fileWriter.write(partition + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                        }
                    }

                }
            }

            // WRITE INSERT statements
            for (Entry<String, DbTable> entry : dbTables.entrySet()) {
                String fullTableName = getFullTableName(entry.getValue());

                // get table INSERT DATA
                List<ColumnDescription> columnsToSelect = null;
                columnsToSelect = getColumnsToSelect(entry.getValue(), dbConnection.getUser());
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
                    selectQuery.append(getFullTableName(entry.getValue()));

                    DbQuery query = new DbQuery(selectQuery.toString());
                    // assuming not very large tables
                    records = dbProvider.select(query, DbReturnModes.ESCAPED_STRING);

                    // lock table and write INSERT statements
                    writeTableToFile(columnsToSelect, entry.getValue(), records, fileWriter);

                }
            }

            // write PRIMARY KEY INDEXES statements
            for (Entry<String, Map<String, Set<String>>> entry : tablesScripts.entrySet()) {
                //String fullTableName = entry.getKey();
                Map<String, Set<String>> allTableScripts = entry.getValue();
                // it is a Set<String> but in fact only one primary key script per table is available
                Set<String> primaryKeyScript = allTableScripts.get(PRIMARY_KEY_INDEX);
                if (primaryKeyScript != null && !primaryKeyScript.isEmpty()) {
                    for (String script : primaryKeyScript) {
                        fileWriter.write(script + EOL_MARKER
                                         + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                    }
                }
            }

            // write ALTER TABLE ONLY <TABLE_SCHEMA>.<TABLE_NAME> ADD CONSTRAINT <CONSTRAINT_NAME> [CONSTRAINT OPTIONS]
            for (Entry<String, Map<String, Set<String>>> entry : tablesScripts.entrySet()) {
                //String fullTableName = entry.getKey();
                Map<String, Set<String>> allTableScripts = entry.getValue();
                Set<String> constraintsScripts = allTableScripts.get(CONSTRAINTS);
                if (constraintsScripts != null && !constraintsScripts.isEmpty()) {
                    for (String script : constraintsScripts) {
                        fileWriter.write(script + EOL_MARKER
                                         + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                    }
                }
            }

            // write GENERAL INDEXES statements
            for (Entry<String, Map<String, Set<String>>> entry : tablesScripts.entrySet()) {
                //String fullTableName = entry.getKey();
                Map<String, Set<String>> allTableScripts = entry.getValue();
                Set<String> generalIndexesScripts = allTableScripts.get(GENERAL_INDEXES);
                if (generalIndexesScripts != null && !generalIndexesScripts.isEmpty()) {
                    for (String script : generalIndexesScripts) {
                        fileWriter.write(script + EOL_MARKER
                                         + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                    }
                }
            }

            // write FOREIGN INDEXES statements
            for (Entry<String, Map<String, Set<String>>> entry : tablesScripts.entrySet()) {
                //String fullTableName = entry.getKey();
                Map<String, Set<String>> allTableScripts = entry.getValue();
                Set<String> foreignKeyScripts = allTableScripts.get(FOREIGN_KEYS_INDEXES);
                if (foreignKeyScripts != null && !foreignKeyScripts.isEmpty()) {
                    for (String script : foreignKeyScripts) {
                        fileWriter.write(script + EOL_MARKER
                                         + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                    }
                }
            }

            // WRITE FOREIGN KEYs that are referencing the tables to be dropped
            for (Entry<String, DbTable> entry : dbTables.entrySet()) {
                if (shouldDropTable(entry.getValue())) {
                    String fullTableName = getFullTableName(entry.getValue());
                    Set<String> fkScripts = getForeignKeysReferencingTable(fullTableName, alreadyCreatedForeignKeys,
                                                                           connection);
                    if (fkScripts != null && !fkScripts.isEmpty()) {
                        for (String fkScript : fkScripts) {
                            fileWriter.write(fkScript + EOL_MARKER
                                             + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                        }
                    }
                }
            }

            // WRITE CREATE [PRIMAR KEY|INDEX|FOREIGN KEY]
            // as well as attach the indexes to the parent (base) table if needed
            for (String script : allPartitionsIndexesAndConstraintsScripts) {
                fileWriter.write(script + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
            }

            // WRITE ALTER TABLE ONLY <TABLE_SCHEMA>.<TABLE> ENABLE TRIGGER ALL
            fileWriter.write("SET session_replication_role = DEFAULT;" + EOL_MARKER
                             + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
            /*for (Entry<String, DbTable> entry : dbTables.entrySet()) {
                if (!shouldDropTable(entry.getValue())) {
                    fileWriter.write("ALTER TABLE ONLY " + getFullTableName(entry.getValue()) + " ENABLE TRIGGER ALL;"
                                     + EOL_MARKER
                                     + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                }
            
            }*/

        } finally {
            DbUtils.closeConnection(connection);
        }

        if (disableForeignKeys) {
            fileWriter.write(disableForeignKeyChecksEnd());
        }

    }

    private Set<String> getForeignKeysReferencingTable( String fullTableName, Set<String> alreadyCreatedForeignKeys,
                                                        Connection connection ) {

        Set<String> scripts = new LinkedHashSet<>();
        String tableSchema = fullTableName.split(Pattern.quote("."))[0];
        if (tableSchema.startsWith("\"") && tableSchema.endsWith("\"")) {
            tableSchema = tableSchema.substring(1, tableSchema.length() - 1);
        }

        String tableName = fullTableName.split(Pattern.quote("."))[1];
        if (tableName.startsWith("\"") && tableName.endsWith("\"")) {
            tableName = tableName.substring(1, tableName.length() - 1);
        }

        /* Since in some versions of PostgreSQL, at least on 12+,
         * using c.conrelid = t.tablename::regclass causes the exception "sql_packages" does not exist
         * There is two variants of the query, where the only difference is that the afforementioned row is changed with
         * c.conrelid::text = t.tablename instead of c.conrelid = t.tablename::regclass
         * 
         * And since I'm not sure in which exact versions this works, I'll leave both of them here.
         * 
         * As such the current behaviour will be as follows:
         * 
         * First try with the ::regclass query (sql_w_regclass)
         * If an exception is thrown, try with the other query
         * 
         */
        
        String sql_w_regclass = "SELECT c.conname, t.tablename "
                + "FROM pg_constraint as c "
                + "JOIN pg_catalog.pg_tables AS t ON c.conrelid = t.tablename::regclass "
                + "WHERE c.contype = 'f' "
                + "AND ( SELECT pg_catalog.pg_get_constraintdef(c.oid, true) LIKE '% REFERENCES "
                + tableName
                + "%' ) "
                + "AND t.schemaname = '" + tableSchema + "' " // move in the beginning of the where clause for optimization
                + "ORDER BY t.tablename::regclass;";
        
        String sql_wo_regclass = "SELECT c.conname, t.tablename "
                                 + "FROM pg_constraint as c "
                                 + "JOIN pg_catalog.pg_tables AS t ON c.conrelid::text = t.tablename "
                                 + "WHERE c.contype = 'f' "
                                 + "AND ( SELECT pg_catalog.pg_get_constraintdef(c.oid, true) LIKE '% REFERENCES "
                                 + tableName
                                 + "%' ) "
                                 + "AND t.schemaname = '" + tableSchema + "' " // move in the beginning of the where clause for optimization
                                 + "ORDER BY t.tablename::regclass;";

        DbException exceptionWRegclass = null;
        DbException exceptionWoRegclass = null;
        for (int i = 0; i < 2; i++) {

            String sql = (i == 0)
                                  ? sql_w_regclass
                                  : sql_wo_regclass;

            try {
                PreparedStatement stmnt = null;
                ResultSet rs = null;
                try {
                    stmnt = connection.prepareStatement(sql);
                    rs = stmnt.executeQuery();
                    while (rs.next()) {
                        String fkName = rs.getString("conname");
                        if (alreadyCreatedForeignKeys.contains(fkName)) {
                            continue;
                        }
                        String refTableName = rs.getString("tablename");
                        // should refTableName table be locked?
                        String script = getForeignKeyIndexScript(tableSchema, refTableName, fkName, connection);
                        if (!StringUtils.isNullOrEmpty(script)) {
                            scripts.add(script);
                        }
                    }
                } catch (SQLException e) {
                    throw new DbException(
                                          "SQL errorCode=" + e.getErrorCode() + " sqlState=" + e.getSQLState() + " "
                                          + e.getMessage(), e);
                } finally {
                    DbUtils.closeResultSet(rs);
                    DbUtils.closeStatement(stmnt);
                }
                // no exception was thrown, so exit the loop
                break;
            } catch (Exception e) {
                if (i == 0) {

                    // check if this is the right exception
                    if (ExceptionUtils.containsMessage("\"sql_packages\" does not exist", e)) {
                        if (logForeignKeyQueryFailure) {
                            LOG.warn("Foreign key query with regclass failed! Trying workaround query.");
                            logForeignKeyQueryFailure = false;
                        }

                        exceptionWRegclass = new DbException("Error while obtaining FOREIGN KEYs, referencing table '"
                                                             + tableSchema + "'.'"
                                                             + tableName
                                                             + "'",
                                                             e);
                    } else {
                        // there is another exception that should be thrown ASAP
                        throw new DbException("Error while obtaining FOREIGN KEYs, referencing table '"
                                              + tableSchema + "'.'"
                                              + tableName
                                              + "'",
                                              e);
                    }

                } else {
                    // since I do not know what exception (if any) is thrown here, just treat each exception as the result of the afforementioned cause for two queries
                    exceptionWoRegclass = new DbException("Error while obtaining FOREIGN KEYs, referencing table '"
                                                          + tableSchema + "'.'"
                                                          + tableName
                                                          + "'",
                                                          e);
                }
            }
        }

        if (exceptionWoRegclass != null && exceptionWRegclass != null) {
            // both queries failed, so throw an exception
            // and since we know that the first exception (exceptionWoRegclass) is about the "sql_packages" error,
            // throw only the 2nd one, since this one has all the relevant information
            throw exceptionWoRegclass;
        }
        
        return scripts;

    }

    private String getForeignKeyIndexScript( String tableSchema, String tableName, String foreignKeyName,
                                             Connection connection ) {

        String sql = loadScriptFromClasspath("PgSQLgetForeignKeyInformationScript.sql");
        sql = sql.replace("<SCHEMA_NAME>", tableSchema);
        sql = sql.replace("<TABLE_NAME>", tableName);
        sql = sql.replace("<FK_NAME>", foreignKeyName);
        try {
            PreparedStatement stmnt = null;
            ResultSet rs = null;
            try {
                stmnt = connection.prepareStatement(sql);
                rs = stmnt.executeQuery();
                while (rs.next()) {
                    StringBuilder sb = new StringBuilder();
                    String fkName = rs.getString("constraint_name");
                    //String constraintDef = rs.getString("condef");
                    String fromCols = rs.getString("from_cols");
                    String toTable = rs.getString("to_table");
                    String toCols = rs.getString("to_cols");
                    String matchType = rs.getString("match_type");
                    String onUpdateAction = rs.getString("on_update");
                    String onDeleteAction = rs.getString("on_delete");
                    String deferrableType = rs.getString("deferrable_type");
                    /**
                     * ALTER TABLE ONLY '<SCHEMA_NAME>.<TABLE_NAME>' 
                     * ADD CONSTRAINT <CONSTRAINT_NAME> FOREIGN KEY (<FROM_COLS>)
                     * REFERENCES <TO_TABLE_SCHEMA>.<TO_TABLE_NAME> (<TO_COLS>) <MATCH_TYPE>
                     * ON UPDATE <ACTION>
                     * ON DELETE <ACTION>
                     * <DEFERRABLE_TYPE>;
                     */
                    sb.append("ALTER TABLE ONLY " + tableSchema + "." + tableName + " \n\t")
                      .append("ADD CONSTRAINT " + fkName)
                      .append(" FOREIGN KEY (" + fromCols + ") ")
                      .append("REFERENCES " + toTable + "(" + toCols + ")")
                      .append(" \n\tMATCH " + matchType)
                      .append(" \n\tON UPDATE " + onUpdateAction)
                      .append(" \n\tON DELETE " + onDeleteAction)
                      .append(" \n\t" + deferrableType);
                    return sb.toString() + ";";
                }
            } catch (SQLException e) {
                throw new DbException(
                                      "SQL errorCode=" + e.getErrorCode() + " sqlState=" + e.getSQLState() + " "
                                      + e.getMessage(), e);
            } finally {
                DbUtils.closeResultSet(rs);
                DbUtils.closeStatement(stmnt);
            }
        } catch (Exception e) {
            throw new DbException("Error while obtaining FOREIGN KEYs for table '" + tableSchema + "'.'" + tableName
                                  + "'",
                                  e);
        }
        return null;
    }

    private String generateCreateTableScript( String fullTableName, Connection connection ) {

        List<String> tableColumnScripts = getTableColumnScripts(fullTableName, connection);
        String tablePartitionScript = getTablePartitionScript(fullTableName, connection);
        String tableTableSpaceScript = getTableTableSpaceScript(fullTableName, connection);

        StringBuilder tableColumnsScript = new StringBuilder();
        boolean firstTime = true;
        for (String script : tableColumnScripts) {
            if (firstTime) {
                firstTime = false;
                tableColumnsScript.append(script);
            } else {
                tableColumnsScript.append(",\n\t").append(script);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ")
          .append(fullTableName)
          .append(" ( \n\t")
          .append(tableColumnsScript.toString())
          .append("\n) ");
        boolean isPartitioned = false;
        if (!StringUtils.isNullOrEmpty(tablePartitionScript)) {
            isPartitioned = true;
            sb.append(tablePartitionScript);
        }
        if (!StringUtils.isNullOrEmpty(tableTableSpaceScript)) {
            if (isPartitioned) {
                sb.append("\n");
            }
            sb.append(tableTableSpaceScript);
        }
        sb.append(";");
        return sb.toString();
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

        if (this.addLocks && table.isLockTable()) {
            // LOCK this single table for update. Lock is released after delete and then insert of backup data.
            // This leads to less potential data integrity issues. If another process updates tables at same time
            // LOCK at once for ALL tables is not applicable as in reality DB connection hangs/blocked

            // PgSQL: ACCESS EXCLUSIVE is most restricted mode
            // https://www.postgresql.org/docs/current/explicit-locking.html
            fileWriter.write("LOCK TABLE " + fullTableName + " IN EXCLUSIVE MODE;" + EOL_MARKER
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
     * Build full table name from schema and table. Add quotes so it will work even for mixed-case names.
     * @param table table object. Note that DbTable.getFullTableName() is not valid for PostgreSQL case.
     * @return Full PostgreSQL-friendly name "schema_name"."table_name" which works not only for table named
     *    "people_table" but also for "People_Table".
     */
    static String getFullTableName( DbTable table ) {

        StringBuilder sb = new StringBuilder();
        // schema quoting
        String schema = table.getTableSchema();
        if (StringUtils.isNullOrEmpty(schema)) {
            schema = "public";
        }
        if (!schema.startsWith("\"")) { // add quotes if missing
            boolean addEndingQuote = true;
            if (schema.endsWith("\"")) {
                LOG.warn("Db schema name does not start with quote but ends with such. Check provided schema name: "
                         + schema);
                addEndingQuote = false;
            }
            sb.append("\"");
            sb.append(schema);
            if (addEndingQuote) {
                sb.append("\"");
            }

        } else { // has leading quote
            sb.append(schema);
            if (!schema.endsWith("\"")) {
                LOG.warn("Db schema name starts with quote but does not end with such. Check provided schema name: "
                         + schema);
                sb.append("\"");
            }
        }
        sb.append(".");

        // table name quoting
        String tableName = table.getTableName();
        if (!tableName.startsWith("\"")) { // add quotes if missing
            boolean addEndingQuote = true;
            if (tableName.endsWith("\"")) {
                LOG.warn("Db table name does not start with quote but ends with such. Check provided table name: "
                         + tableName);
                addEndingQuote = false;
            }
            sb.append("\"");
            sb.append(tableName);
            if (addEndingQuote) {
                sb.append("\"");
            }
        } else { // has leading quote
            sb.append(tableName);
            if (!tableName.endsWith("\"")) {
                LOG.warn("Db table name starts with quote but does not end with such. Check provided table name: "
                         + tableName);
                sb.append("\"");
            }
        }

        return sb.toString();
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
     * ( inserts) is not correct. Also this is the only possible way if there are cyclic references between 2 tables.
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

    private String getTableTableSpaceScript( String fullTableName, Connection connection ) {

        String schemaName = fullTableName.split(Pattern.quote("."))[0].replace("\"", "'");
        String tableName = fullTableName.split(Pattern.quote("."))[1].replace("\"", "'");
        String sql = "SELECT tablespace FROM pg_tables WHERE tablename = " + tableName + " AND schemaname = "
                     + schemaName + ";";
        PreparedStatement stmnt = null;
        ResultSet rs = null;
        try {
            try {
                stmnt = connection.prepareStatement(sql);
                rs = stmnt.executeQuery();
                while (rs.next()) {
                    String tableSpaceName = rs.getString("tablespace");
                    if (StringUtils.isNullOrEmpty(tableSpaceName)) {
                        return "";
                    }
                    return "TABLESPACE " + tableSpaceName;
                }
            } catch (SQLException e) {
                throw new DbException(
                                      "SQL errorCode=" + e.getErrorCode() + " sqlState=" + e.getSQLState() + " "
                                      + e.getMessage(), e);
            } finally {
                DbUtils.closeResultSet(rs);
                DbUtils.closeStatement(stmnt);
            }
        } catch (Exception e) {
            throw new DbException("Error while obtaining tablespace for table '" + fullTableName + "'",
                                  e);
        }
        return null;
    }

    private List<String> getTablePartitionsScript( String fullTableName, String parentTablePrimaryKeyName,
                                                   Set<String> parentTableIndexes, // CREATE INDEX scripts
                                                   Connection connection ) {

        List<String> scripts = new ArrayList<String>();
        String schemaName = fullTableName.split(Pattern.quote("."))[0].replace("\"", "'");
        String tableName = fullTableName.split(Pattern.quote("."))[1].replace("\"", "'");
        // query to find if the table has partitions
        String findPartitionsSql = "SELECT pg_partition_tree(" + tableName + "::regclass);";
        PreparedStatement stmnt = null;
        ResultSet rs = null;
        try {
            try {
                stmnt = connection.prepareStatement(findPartitionsSql);
                rs = stmnt.executeQuery();
                String parentTableName = tableName.substring(1, tableName.length() - 1);
                while (rs.next()) {
                    String partitionName = rs.getObject(1).toString();
                    partitionName = partitionName.substring(1).split(java.util.regex.Pattern.quote(","))[0];
                    if (parentTableName.equalsIgnoreCase(partitionName)) {
                        // this is the parent table, so skip the rest of the code
                        continue;
                    }
                    String parentTableSchema = schemaName.substring(1, schemaName.length() - 1);
                    String findPartitionScriptSql = "SELECT pg_get_expr(c.relpartbound, c.oid, true) FROM pg_class c WHERE relname = '"
                                                    + partitionName + "';";
                    PreparedStatement stmntB = null;
                    ResultSet rsB = null;
                    try {
                        stmntB = connection.prepareStatement(findPartitionScriptSql);
                        rsB = stmntB.executeQuery();
                        while (rsB.next()) {
                            StringBuilder sb = new StringBuilder();
                            String script = rsB.getString("pg_get_expr");

                            // CREATE TABLE for partition
                            sb.append("CREATE TABLE " + parentTableSchema + "."
                                      + partitionName + " PARTITION OF "
                                      + fullTableName + "\n\t" + script + ";" + EOL_MARKER
                                      + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                            // ALTER partition table owner
                            sb.append("ALTER TABLE ONLY ")
                              .append(parentTableSchema + "." + partitionName)
                              .append(" OWNER TO \"" + this.dbConnection.getUser() + "\";" + EOL_MARKER
                                      + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);

                            // get PRIMARY KEY
                            String[] pk = getPrimaryKeyIndexScript(parentTableSchema + "." + partitionName, connection);
                            allPartitionsIndexesAndConstraintsScripts.add(pk[1]);
                            allPartitionsIndexesAndConstraintsScripts.add("ALTER INDEX " + parentTablePrimaryKeyName
                                                                          + " ATTACH PARTITION "
                                                                          + parentTableSchema + "." + pk[0]);
                            // get CONSTRAINTS
                            Map<String, String> constraints = getConstraintsScripts(parentTableSchema + "."
                                                                                    + partitionName, connection);
                            for (Entry<String, String> entry : constraints.entrySet()) {
                                allPartitionsIndexesAndConstraintsScripts.add(entry.getValue());
                            }
                            // TODO ATTACH CONSTRAINTs to parent constraint (if ever needed)
                            Set<String> constraintsNames = new LinkedHashSet<>(new HashSet<>(constraints.values()));
                            constraintsNames.add(pk[0]);
                            // get other INDEX(es)
                            Map<String, String> otherIndexes = getOtherIndexesScripts(parentTableSchema + "."
                                                                                      + partitionName,
                                                                                      constraintsNames, connection);

                            // get the ATTACH scripts for each index (if needed)
                            for (Entry<String, String> otherPartitionIndexEntry : otherIndexes.entrySet()) {
                                allPartitionsIndexesAndConstraintsScripts.add(otherPartitionIndexEntry.getValue());
                                for (String parentCreateIndexScript : parentTableIndexes) {

                                    Map<String, String> parentMap = getIndexNameAndColumnList(parentTableName,
                                                                                              parentCreateIndexScript.substring(0,
                                                                                                                                parentCreateIndexScript.lastIndexOf("TABLESPACE")
                                                                                                                                   - 2),
                                                                                              connection);

                                    if (parentMap == null || parentMap.isEmpty()) {
                                        continue;
                                    }

                                    Map<String, String> partitionMap = getIndexNameAndColumnList(partitionName,
                                                                                                 otherPartitionIndexEntry.getValue()
                                                                                                                         .substring(0,
                                                                                                                                    otherPartitionIndexEntry.getValue()
                                                                                                                                                            .lastIndexOf("TABLESPACE")
                                                                                                                                       - 2),
                                                                                                 connection);

                                    if (partitionMap == null || partitionMap.isEmpty()) {
                                        continue;
                                    }
                                    // ugly, not optimized, but at least working.
                                    String parentIndexColumnList = new ArrayList<>(parentMap.values()).get(0);
                                    String partitionIndexColumnList = new ArrayList<>(partitionMap.values()).get(0);
                                    String parentIndexName = new ArrayList<>(parentMap.keySet()).get(0);
                                    String partitionIndexName = new ArrayList<>(partitionMap.keySet()).get(0);

                                    if (parentIndexColumnList.equals(partitionIndexColumnList)) {
                                        StringBuilder indexBuilder = new StringBuilder();
                                        indexBuilder.append("ALTER INDEX " + parentTableSchema
                                                            + "." + parentIndexName + " ATTACH PARTITION "
                                                            + parentTableSchema + "." + partitionIndexName + ";");
                                        allPartitionsIndexesAndConstraintsScripts.add(indexBuilder.toString());
                                    }

                                }
                            }

                            scripts.add(sb.toString());
                        }
                    } catch (SQLException ex) {
                        throw new DbException(
                                              "SQL errorCode=" + ex.getErrorCode() + " sqlState=" + ex.getSQLState()
                                              + " "
                                              + ex.getMessage(), ex);
                    } finally {
                        DbUtils.closeResultSet(rsB);
                        DbUtils.closeStatement(stmntB);
                    }
                }
            } catch (SQLException e) {
                throw new DbException(
                                      "SQL errorCode=" + e.getErrorCode() + " sqlState=" + e.getSQLState() + " "
                                      + e.getMessage(), e);
            } finally {
                DbUtils.closeResultSet(rs);
                DbUtils.closeStatement(stmnt);
            }
        } catch (Exception e) {
            throw new DbException("Error while obtaining partitions for table '" + fullTableName + "'",
                                  e);
        }
        return scripts;
    }

    private Map<String, String> getIndexNameAndColumnList( String tableName, String indexScript,
                                                           Connection connection ) {

        Map<String, String> map = new HashMap<String, String>();
        String sql = "SELECT schemaname, tablename, indexname, oid, indkey, column_name, ordinal_position "
                     + "FROM pg_indexes "
                     + "JOIN pg_class ON indexname = relname "
                     + "JOIN pg_index ON oid = indexrelid "
                     + "JOIN INFORMATION_SCHEMA.COLUMNS ON tablename = table_name AND table_schema = schemaname "
                     + "WHERE tablename = '" + tableName + "' "
                     + "AND indexdef = '" + indexScript + "';";
        try {
            PreparedStatement stmnt = null;
            ResultSet rs = null;
            try {
                stmnt = connection.prepareStatement(new String(sql));
                rs = stmnt.executeQuery();
                String indexName = null;
                StringBuilder columnsList = new StringBuilder();
                Map<Integer, String> allColumns = new HashMap<>();
                String[] indexColumnsPositions = null;
                while (rs.next()) {
                    if (indexName == null) {
                        indexName = rs.getString("indexname");
                    }
                    allColumns.put(rs.getInt("ordinal_position"), rs.getString("column_name"));
                    indexColumnsPositions = rs.getArray("indkey").toString().split(" ");
                }
                boolean firstTime = false;
                for (String pos : indexColumnsPositions) {
                    if (firstTime) {
                        columnsList.append(allColumns.get(Integer.parseInt(pos)));
                    } else {
                        columnsList.append(", " + allColumns.get(Integer.parseInt(pos)));
                    }
                }
                map.put(indexName, columnsList.toString());
            } catch (SQLException e) {
                throw new DbException(
                                      "SQL errorCode=" + e.getErrorCode() + " sqlState=" + e.getSQLState() + " "
                                      + e.getMessage(), e);
            } finally {
                DbUtils.closeResultSet(rs);
                DbUtils.closeStatement(stmnt);
            }
        } catch (Exception e) {
            throw new DbException("Error while obtaining name and columns list for index on table '" + tableName
                                  + "' and with script '"
                                  + indexScript + "'",
                                  e);
        }
        return map;
    }

    private String getTablePartitionScript( String fullTableName, Connection connection ) {

        StringBuilder sb = new StringBuilder();

        String tableSchema = fullTableName.split(Pattern.quote("."))[0];
        tableSchema = tableSchema.substring(1, tableSchema.length() - 1);

        String tableName = fullTableName.split(Pattern.quote("."))[1];
        tableName = tableName.substring(1, tableName.length() - 1);

        String sql = "SELECT c.relnamespace::regnamespace::text AS schema, c.relname AS table_name, pg_get_partkeydef(c.oid) AS partition_key "
                     + "FROM pg_class c "
                     + "WHERE c.relkind = 'p' AND c.relname = '" + tableName
                     + "' AND c.relnamespace::regnamespace::text = '" + tableSchema + "';";

        PreparedStatement stmnt = null;
        ResultSet rs = null;
        try {
            try {
                stmnt = connection.prepareStatement(sql);
                rs = stmnt.executeQuery();
                while (rs.next()) {
                    sb.append("PARTITION BY ").append(rs.getString("partition_key"));
                }
            } catch (SQLException e) {
                throw new DbException(
                                      "SQL errorCode=" + e.getErrorCode() + " sqlState=" + e.getSQLState() + " "
                                      + e.getMessage(), e);
            }

        } catch (Exception e) {
            throw new DbException("Error while obtaining partition type and columns for table '" + fullTableName + "'",
                                  e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.closeStatement(stmnt);
        }
        return sb.toString();
    }

    private Map<String, Set<String>> getTableIndexesScripts( String fullTableName, Connection connection ) {

        Map<String, Set<String>> scripts = new LinkedHashMap<>();

        Set<String> constraintsNames = new HashSet<String>();

        // primaryKeyNameAndIndexStript[0]  = indeName, primaryKeyNameAndIndexStript[1]  = indexCreateScript
        String[] primaryKeyNameAndIndexScript = getPrimaryKeyIndexScript(fullTableName, connection);
        if (!StringUtils.isNullOrEmpty(primaryKeyNameAndIndexScript[1])) {
            Set<String> primaryKey = new HashSet<String>();
            primaryKey.add(primaryKeyNameAndIndexScript[1]);
            scripts.put(PRIMARY_KEY_INDEX, primaryKey);

            constraintsNames.add(primaryKeyNameAndIndexScript[0]);
        }

        // get foreign keys
        Map<String, String> foreignKeysIndexesScripts = getForeignKeysIndexesScripts(fullTableName, connection);
        scripts.put(FOREIGN_KEYS_INDEXES, new HashSet<>(foreignKeysIndexesScripts.values()));
        alreadyCreatedForeignKeys.addAll(foreignKeysIndexesScripts.keySet());

        // get constraints
        Map<String, String> constraintsScripts = getConstraintsScripts(fullTableName,
                                                                       connection);
        scripts.put(CONSTRAINTS, new HashSet<>(constraintsScripts.values()));
        constraintsNames.addAll(constraintsScripts.keySet());

        // get other indexes
        Set<String> otherIndexesScripts = new HashSet<>(getOtherIndexesScripts(fullTableName, constraintsNames,
                                                                               connection).values());
        scripts.put(GENERAL_INDEXES, otherIndexesScripts);

        return scripts;
    }

    private Map<String, String> getConstraintsScripts( String fullTableName,
                                                       Connection connection ) {

        String tableSchema = fullTableName.split(Pattern.quote("."))[0];
        if (tableSchema.startsWith("\"") && tableSchema.endsWith("\"")) {
            tableSchema = tableSchema.substring(1, tableSchema.length() - 1);
        }

        String tableName = fullTableName.split(Pattern.quote("."))[1];
        if (tableName.startsWith("\"") && tableName.endsWith("\"")) {
            tableName = tableName.substring(1, tableName.length() - 1);
        }
        Map<String, String> scripts = new HashMap<>();
        // get all constraints that are neither foreign, not primary key
        String sql = "SELECT c.conname, pg_catalog.pg_get_constraintdef(c.oid, true) as condef "
                     + "FROM pg_constraint as c "
                     + "WHERE c.conrelid = '" + tableName + "'::regclass AND c.contype != 'p' AND c.contype != 'f'";

        try {
            PreparedStatement stmnt = null;
            ResultSet rs = null;
            try {
                stmnt = connection.prepareStatement(sql);
                rs = stmnt.executeQuery();
                while (rs.next()) {
                    String constraintName = rs.getString("conname");
                    String script = "ALTER TABLE ONLY " + fullTableName + " ADD CONSTRAINT";
                    script += " " + constraintName;
                    String tmp = rs.getString("condef");
                    script += " " + tmp + ";";
                    scripts.put(constraintName, script);
                }
            } catch (SQLException e) {
                throw new DbException(
                                      "SQL errorCode=" + e.getErrorCode() + " sqlState=" + e.getSQLState() + " "
                                      + e.getMessage(), e);
            } finally {
                DbUtils.closeStatement(stmnt);
                DbUtils.closeResultSet(rs);
            }
        } catch (Exception e) {
            throw new DbException("Error while obtaining CONSTRAINT(s) for table '" + tableSchema + "'.'" + tableName
                                  + "'",
                                  e);
        }
        return scripts;
    }

    private Map<String, String> getOtherIndexesScripts( String fullTableName,
                                                        Set<String> constraintsNames,
                                                        Connection connection ) {

        String tableSchema = fullTableName.split(Pattern.quote("."))[0];
        if (tableSchema.startsWith("\"") && tableSchema.endsWith("\"")) {
            tableSchema = tableSchema.substring(1, tableSchema.length() - 1);
        }

        String tableName = fullTableName.split(Pattern.quote("."))[1];
        if (tableName.startsWith("\"") && tableName.endsWith("\"")) {
            tableName = tableName.substring(1, tableName.length() - 1);
        }
        String sql = "SELECT * FROM pg_indexes WHERE schemaname = '" + tableSchema + "' AND tablename = '" + tableName
                     + "';";
        Map<String, String> scripts = new HashMap<String, String>();
        try {
            PreparedStatement stmnt = null;
            ResultSet rs = null;
            try {
                stmnt = connection.prepareStatement(sql);
                rs = stmnt.executeQuery();
                while (rs.next()) {
                    String indexname = rs.getString("indexname");
                    if (constraintsNames.contains(indexname)) {
                        continue;
                    }
                    String indexDef = rs.getString("indexdef");
                    String tableSpace = rs.getString("tablespace");
                    String script = null;
                    if (StringUtils.isNullOrEmpty(tableSpace)) {
                        script = indexDef;
                    } else {
                        script = indexDef + "\n TABLESPACE " + tableSpace;
                    }

                    scripts.put(indexname, script + ";");
                }
            } catch (SQLException e) {
                throw new DbException(
                                      "SQL errorCode=" + e.getErrorCode() + " sqlState=" + e.getSQLState() + " "
                                      + e.getMessage(), e);
            } finally {
                DbUtils.closeStatement(stmnt);
                DbUtils.closeResultSet(rs);
            }
        } catch (Exception e) {
            throw new DbException("Error while obtaining INDEX(s) for table '" + tableSchema + "'.'" + tableName
                                  + "'",
                                  e);
        }
        return scripts;
    }

    private Map<String, String> getForeignKeysIndexesScripts( String fullTableName, Connection connection ) {

        String tableSchema = fullTableName.split(Pattern.quote("."))[0];
        if (tableSchema.startsWith("\"") && tableSchema.endsWith("\"")) {
            tableSchema = tableSchema.substring(1, tableSchema.length() - 1);
        }

        String tableName = fullTableName.split(Pattern.quote("."))[1];
        if (tableName.startsWith("\"") && tableName.endsWith("\"")) {
            tableName = tableName.substring(1, tableName.length() - 1);
        }

        Map<String, String> scripts = new HashMap<>();
        String sql = loadScriptFromClasspath("PgSQLgetForeignKeysInformationScript.sql");
        sql = sql.replace("<SCHEMA_NAME>", tableSchema);
        sql = sql.replace("<TABLE_NAME>", tableName);

        try {
            PreparedStatement stmnt = null;
            ResultSet rs = null;
            try {
                stmnt = connection.prepareStatement(sql);
                rs = stmnt.executeQuery();
                while (rs.next()) {
                    StringBuilder sb = new StringBuilder();
                    String fkName = rs.getString("constraint_name");
                    //String constraintDef = rs.getString("condef");
                    String fromCols = rs.getString("from_cols");
                    String toTable = rs.getString("to_table");
                    String toCols = rs.getString("to_cols");
                    String matchType = rs.getString("match_type");
                    String onUpdateAction = rs.getString("on_update");
                    String onDeleteAction = rs.getString("on_delete");
                    String deferrableType = rs.getString("deferrable_type");
                    /**
                     * ALTER TABLE ONLY '<SCHEMA_NAME>.<TABLE_NAME>' 
                     * ADD CONSTRAINT <CONSTRAINT_NAME> FOREIGN KEY (<FROM_COLS>)
                     * REFERENCES <TO_TABLE_SCHEMA>.<TO_TABLE_NAME> (<TO_COLS>) <MATCH_TYPE>
                     * ON UPDATE <ACTION>
                     * ON DELETE <ACTION>
                     * <DEFERRABLE_TYPE>;
                     */
                    sb.append("ALTER TABLE ONLY " + tableSchema + "." + tableName + " \n\t")
                      .append("ADD CONSTRAINT " + fkName)
                      .append(" FOREIGN KEY (" + fromCols + ") ")
                      .append("REFERENCES " + toTable + "(" + toCols + ")")
                      .append(" \n\tMATCH " + matchType)
                      .append(" \n\tON UPDATE " + onUpdateAction)
                      .append(" \n\tON DELETE " + onDeleteAction)
                      .append(" \n\t" + deferrableType);
                    scripts.put(fkName, sb.toString() + ";");
                }
            } catch (SQLException e) {
                throw new DbException(
                                      "SQL errorCode=" + e.getErrorCode() + " sqlState=" + e.getSQLState() + " "
                                      + e.getMessage(), e);
            } finally {
                DbUtils.closeStatement(stmnt);
                DbUtils.closeResultSet(rs);
            }
        } catch (Exception e) {
            throw new DbException("Error while obtaining FOREIGN KEYs for table '" + tableSchema + "'.'" + tableName
                                  + "'",
                                  e);
        }

        return scripts;
    }

    private String[] getPrimaryKeyIndexScript( String fullTableName, Connection connection ) {

        String tableSchema = fullTableName.split(Pattern.quote("."))[0];
        if (tableSchema.startsWith("\"") && tableSchema.endsWith("\"")) {
            tableSchema = tableSchema.substring(1, tableSchema.length() - 1);
        }

        String tableName = fullTableName.split(Pattern.quote("."))[1];
        if (tableName.startsWith("\"") && tableName.endsWith("\"")) {
            tableName = tableName.substring(1, tableName.length() - 1);
        }

        String sql = "SELECT kcu.table_schema, kcu.table_name, tco.constraint_name, kcu.ordinal_position as position, kcu.column_name as key_column "
                     + "FROM information_schema.table_constraints tco "
                     + "JOIN information_schema.key_column_usage kcu ON kcu.constraint_name = tco.constraint_name "
                     + "AND kcu.constraint_schema = tco.constraint_schema "
                     + "AND kcu.constraint_name = tco.constraint_name "
                     + "WHERE tco.constraint_type = 'PRIMARY KEY' "
                     + "AND kcu.table_name = '" + tableName + "' AND kcu.table_schema = '" + tableSchema + "' "
                     + "ORDER BY kcu.table_schema, kcu.table_name, position;";
        StringBuilder sb = new StringBuilder();
        PreparedStatement stmnt = null;
        ResultSet rs = null;
        String pkName = null;
        try {
            try {
                stmnt = connection.prepareStatement(sql);
                rs = stmnt.executeQuery();
                StringBuilder columns = new StringBuilder();
                boolean firstTime = true;
                while (rs.next()) {
                    if (pkName == null) {
                        pkName = rs.getString("constraint_name");
                    }
                    if (firstTime) {
                        columns.append(rs.getString("key_column"));
                        firstTime = false;
                    } else {
                        columns.append(", " + rs.getString("key_column"));
                    }
                }
                if (!StringUtils.isNullOrEmpty(pkName)) {
                    // ALTER TABLE ONLY public.table_one ADD CONSTRAINT table_one_pkey PRIMARY KEY (left, id);
                    sb.append("ALTER TABLE ONLY ")
                      .append(tableSchema + "." + tableName)
                      .append(" ADD CONSTRAINT ")
                      .append(pkName)
                      .append(" PRIMARY KEY (")
                      .append(columns.toString())
                      .append(")");
                }

            } catch (SQLException e) {
                throw new DbException(
                                      "SQL errorCode=" + e.getErrorCode() + " sqlState=" + e.getSQLState() + " "
                                      + e.getMessage(), e);
            } finally {
                DbUtils.closeStatement(stmnt);
                DbUtils.closeResultSet(rs);
            }
        } catch (Exception e) {
            throw new DbException("Error while obtaining PRIMARY KEY for table '" + tableSchema + "'.'" + tableName
                                  + "'",
                                  e);
        }

        allDropTablesPrimaryKeysNames.put(fullTableName, pkName);
        return new String[]{ pkName, sb.toString() + ";" };
    }

    private List<String> getTableColumnScripts( String fullTableName, Connection connection ) {

        List<String> scripts = new ArrayList<String>();
        String tableSchema = fullTableName.split(Pattern.quote("."))[0];
        String tableName = fullTableName.split(Pattern.quote("."))[1];
        String sql = "SELECT column_name, data_type, character_maximum_length, column_default, is_nullable FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema = "
                     + tableSchema.replace("\"", "'")
                     + " and table_name = " + tableName.replace("\"", "'") + ";";

        PreparedStatement stmnt = null;
        ResultSet rs = null;
        try {
            try {
                stmnt = connection.prepareStatement(sql);
                rs = stmnt.executeQuery();
            } catch (SQLException e) {
                throw new DbException(
                                      "SQL errorCode=" + e.getErrorCode() + " sqlState=" + e.getSQLState() + " "
                                      + e.getMessage(), e);
            }
            while (rs.next()) {
                StringBuilder columnScript = new StringBuilder();
                String columnName = rs.getString("column_name");
                String dataType = rs.getString("data_type");
                int charMaxLen = rs.getInt("character_maximum_length");
                String isNullable = rs.getString("is_nullable");
                String columnDefault = rs.getString("column_default");
                columnScript.append(columnName).append(" ").append(dataType);
                if (charMaxLen > 0) {
                    columnScript.append("(").append(charMaxLen).append(")");
                }
                if (!StringUtils.isNullOrEmpty(columnDefault)) {
                    columnScript.append(" DEFAULT ").append(columnDefault);
                }
                if ("NO".equalsIgnoreCase(isNullable)) {
                    columnScript.append(" NOT NULL");
                }
                scripts.add(columnScript.toString());
            }
        } catch (Exception e) {
            throw new DbException("Error while obtaining column information/scripts for table '" + fullTableName + "'",
                                  e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.closeStatement(stmnt);
        }
        return scripts;
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
