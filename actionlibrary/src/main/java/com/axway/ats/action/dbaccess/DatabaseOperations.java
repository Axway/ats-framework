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
package com.axway.ats.action.dbaccess;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.action.dbaccess.model.DatabaseCell;
import com.axway.ats.action.dbaccess.model.DatabaseRow;
import com.axway.ats.action.exceptions.DatabaseOperationsException;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.dbaccess.DbQuery;
import com.axway.ats.common.dbaccess.OracleKeys;
import com.axway.ats.core.dbaccess.DatabaseProviderFactory;
import com.axway.ats.core.dbaccess.DbProvider;
import com.axway.ats.core.dbaccess.DbRecordValue;
import com.axway.ats.core.dbaccess.DbRecordValuesList;
import com.axway.ats.core.dbaccess.DbReturnModes;
import com.axway.ats.core.dbaccess.cassandra.CassandraDbProvider;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.harness.config.TestBox;

/**
 * This class allows the user to execute general database operations.
 *
 * <br><br>
 * <b>User guide</b>
 * <a href="https://axway.github.io/ats-framework/Database-Operations.html">page</a>
 * related to this class
 */
@PublicAtsApi
public class DatabaseOperations {

    private final Logger log = LogManager.getLogger(DatabaseOperations.class);

    protected DbProvider dbProvider;

    /**
     * Used only in the Unit Tests, for Mock
     * @param dbProvider database provider
     */
    DatabaseOperations( DbProvider dbProvider ) {

        this.dbProvider = dbProvider;
    }

    /**
     *
     * @param testBox testBox object for configuration of the database provider
     */
    @PublicAtsApi
    public DatabaseOperations( TestBox testBox ) {

        this(testBox, null);
    }

    /**
    *
    * @param testBox testBox object for configuration of the database provider
    * @param customProperties a set of custom properties for the connection
    *
    * TIP: if you use DbKeys.USE_ADMIN_CREDENTIALS with value 'true' , you can use the admin credentials for the connection
    */
    @PublicAtsApi
    public DatabaseOperations( TestBox testBox,
                               Map<String, Object> customProperties ) {

        String dbUser = testBox.getDbUser();

        String dbPass = testBox.getDbPass();

        if (customProperties != null && customProperties.containsKey(OracleKeys.USE_ADMIN_CREDENTIALS)
            && "true".equals(customProperties.get(OracleKeys.USE_ADMIN_CREDENTIALS))) {
            dbUser = testBox.getAdminUser();
            dbPass = testBox.getAdminPass();
        }

        createDbProvider(testBox.getDbType(), testBox.getHost(), testBox.getDbName(), dbUser, dbPass,
                         testBox.getDbPort(), customProperties);
    }

    /**
     *
     * @param dbType database type
     * @param dbHost database host
     * @param dbName database name
     * @param dbUser database user
     * @param dbPass database user password
     * @param dbPort database port number
     * @param customProperties a set of custom properties for the connection
     */
    @PublicAtsApi
    public DatabaseOperations( String dbType,
                               String dbHost,
                               String dbName,
                               String dbUser,
                               String dbPass,
                               int dbPort,
                               Map<String, Object> customProperties ) {

        createDbProvider(dbType, dbHost, dbName, dbUser, dbPass, dbPort, customProperties);
    }

    private void createDbProvider( String dbType,
                                   String dbHost,
                                   String dbName,
                                   String dbUser,
                                   String dbPass,
                                   int dbPort,
                                   Map<String, Object> customProperties ) {

        dbProvider = DatabaseProviderFactory.getDatabaseProvider(dbType,
                                                                 dbHost,
                                                                 dbName,
                                                                 dbUser,
                                                                 dbPass,
                                                                 dbPort,
                                                                 customProperties);

    }

    /**
     * Gets some data from a database table. <br>
     * It constructs a SQL query based on the input arguments.
     *
     * @param tableName the table to query
     * @param selectColumnName the column which value we need
     * @param whereColumnName the "where" column name
     * @param whereColumnValue the "where" column value
     * @return the found database data
     */
    @PublicAtsApi
    public DatabaseRow[] getDatabaseData(
                                          String tableName,
                                          String selectColumnName,
                                          String whereColumnName,
                                          String whereColumnValue ) {

        String quotation = getQuotation();
        String sqlQuery = new StringBuilder().append("SELECT ")
                                             .append(selectColumnName)
                                             .append(" FROM ")
                                             .append(tableName)
                                             .append(" WHERE ")
                                             .append(whereColumnName)
                                             .append(" = ")
                                             .append(quotation)
                                             .append(whereColumnValue)
                                             .append(quotation)
                                             .toString();

        return getDatabaseData(sqlQuery);
    }

    /**
     * Gets some data from a database table. <br>
     * <em>Note</em> that client may need to inspect returned type of each value and
     * optionally convert it to get value in specific format.
     *
     * @param sqlQuery the SQL SELECT query to run. Client is resposible for any
     *      possible escaping needed so this is not security-safe method
     * @return the found database data
     */
    @PublicAtsApi
    public DatabaseRow[] getDatabaseData(
                                          String sqlQuery ) {

        List<DatabaseRow> dbRows = new ArrayList<DatabaseRow>();
        try {
            log.debug("Executing query: " + sqlQuery);
            DbRecordValuesList[] rsList = dbProvider.select(sqlQuery);

            if (rsList != null) {
                for (DbRecordValuesList rs : rsList) {
                    Iterator<DbRecordValue> it = rs.iterator();
                    if (it.hasNext()) {
                        DatabaseRow dbRow = new DatabaseRow();
                        while (it.hasNext()) {
                            DbRecordValue dbRecordValue = it.next();
                            dbRow.addCell(new DatabaseCell(dbRecordValue.getDbColumn().getColumnName(),
                                                           dbRecordValue.getValueAsString()));
                        }
                        dbRows.add(dbRow);
                    }
                }
            }

            return dbRows.toArray(new DatabaseRow[dbRows.size()]);
        } catch (DbException e) {
            throw new DatabaseOperationsException("Error getting data from DB with query '"
                                                  + sqlQuery + "'", e);
        }
    }

    /**
     * <p>
     * Gets data from a database table. All values as returned as String representations as needed.<br>
     * For example:
     * <ul>
     *  <li>TIMESTAMPs are retuned as java.sql.Timestamp.toString() format
     *  (yyyy-MM-dd HH:mm:ss.SSS and millis instead of nanos as documented in JavaSE Doc)</li>
     *  <li>BLOBs bytes are represented in hex format. Beware not to select too big data cells.</li>
     * </ul>
     * </p>
     * @param sqlQuery the SQL SELECT query to run. Client is resposible for any
     *      possible escaping needed so thread this as not security-safe method
     * @return the found database data
     */
    @PublicAtsApi
    public DatabaseRow[] getDatabaseDataAsStrings(
                                                   String sqlQuery ) {

        List<DatabaseRow> dbRows = new LinkedList<DatabaseRow>();
        try {
            log.debug("Executing query: " + sqlQuery);
            DbRecordValuesList[] rsList = dbProvider.select(new DbQuery(sqlQuery), DbReturnModes.STRING);

            if (rsList != null) {
                for (DbRecordValuesList rs : rsList) {
                    Iterator<DbRecordValue> it = rs.iterator();
                    if (it.hasNext()) {
                        DatabaseRow dbRow = new DatabaseRow();
                        while (it.hasNext()) {
                            DbRecordValue dbRecordValue = it.next();
                            dbRow.addCell(new DatabaseCell(dbRecordValue.getDbColumn().getColumnName(),
                                                           dbRecordValue.getValueAsString()));
                        }
                        dbRows.add(dbRow);
                    }
                }
            }

            return dbRows.toArray(new DatabaseRow[dbRows.size()]);
        } catch (DbException e) {
            throw new DatabaseOperationsException("Error getting data from DB with query '"
                                                  + sqlQuery + "'", e);
        }
    }

    /**
     *
     * Gets a single value from a database table. <br>
     * It constructs a SQL query based on the input arguments. It returns the first value of the first returned row.
     *
     * <p><strong>Note</strong>: If the selected DB object is array - BLOB, LONG BLOB ... we return it this way:
     *  <code>[ 10, 24, -35]</code>
     *  </p>
     *
     * @param tableName the table to query
     * @param selectColumnName the column which value we need
     * @param whereColumnName the "where" column name
     * @param whereColumnValue the "where" column value
     * @return the found value<br>
     * the first one if more than one are returned<br>
     * null if none is returned.
     */
    @PublicAtsApi
    public String getValue(
                            String tableName,
                            String selectColumnName,
                            String whereColumnName,
                            String whereColumnValue ) {

        String quotation = getQuotation();
        String sqlQuery = new StringBuilder().append("SELECT ")
                                             .append(selectColumnName)
                                             .append(" FROM ")
                                             .append(tableName)
                                             .append(" WHERE ")
                                             .append(whereColumnName)
                                             .append(" = ")
                                             .append(quotation)
                                             .append(whereColumnValue)
                                             .append(quotation)
                                             .toString();

        return getValue(sqlQuery);
    }

    /**
     * Gets a single value from a database. <br>
     * It returns the first value of the first returned row.
     *
     * <p><strong>Note</strong>: If the selected DB object is array - BLOB, LONG BLOB ... we return it this way:
     *  <code>[ 10, 24, -35]</code>
     *  </p>
     *
     * @param sqlQuery the SQL query to run
     * @return
     */
    @PublicAtsApi
    public String getValue(
                            String sqlQuery ) {

        try {
            log.debug("Executing query: " + sqlQuery);
            DbRecordValuesList[] rsList = dbProvider.select(sqlQuery);
            if (rsList != null && rsList.length > 0) {
                if (rsList.length > 1) {
                    log.warn("SQL query '" + sqlQuery + "' returned " + rsList.length + " rows of results");
                }
                if (rsList[0].size() > 1) {
                    log.warn("SQL query '" + sqlQuery + "' returned " + rsList[0].size() + " values per row");
                }
                return rsList[0].get(0).getValueAsString();
            }
        } catch (DbException e) {
            throw new DatabaseOperationsException("Error getting value from DB with query '"
                                                  + sqlQuery + "'", e);
        }

        return null;
    }

    /**
     * Run update query.
     *
     * @param tableName the table to update
     * @param columnToUpdate the column to update
     * @param updateValue the new value
     * @param whereColumnName the "where" column name
     * @param whereColumnValue the "where" column value
     * @return the number of updated rows
     */
    @PublicAtsApi
    public int updateValue(
                            String tableName,
                            String columnToUpdate,
                            String updateValue,
                            String whereColumnName,
                            String whereColumnValue ) {

        String quotation = getQuotation();
        return updateValue("UPDATE " + tableName + " SET " + columnToUpdate + "='" + updateValue
                           + "' WHERE " + whereColumnName + "=" + quotation + whereColumnValue + quotation);
    }

    /**
     * Run update query.
     *
     * @param sqlQuery the SQL query to run
     * @return the number of updated rows
     */
    @PublicAtsApi
    public int updateValue(
                            String sqlQuery ) {

        int rowsUpdated;
        try {
            log.debug("Executing update query: '" + sqlQuery + "'");
            rowsUpdated = dbProvider.executeUpdate(sqlQuery);
            if (! (dbProvider instanceof CassandraDbProvider)) {
                if (rowsUpdated == 0) {
                    log.warn("SQL query '" + sqlQuery + "' updated 0 rows");
                } else {
                    log.debug("SQL query '" + sqlQuery + "' updated '" + rowsUpdated + "' rows");
                }
            }
        } catch (DbException e) {
            throw new DatabaseOperationsException("Error executing update query '" + sqlQuery + "'",
                                                  e);
        }
        return rowsUpdated;
    }

    /**
     * Run insert query
     *
     * @param tableName the table to insert values in
     * @param columnNames the columns where values will be inserted
     * @param columnValues the values to insert
     */
    @PublicAtsApi
    public void insertValues(
                              String tableName,
                              String[] columnNames,
                              String[] columnValues ) {

        Map<String, String> columns = new HashMap<String, String>();

        if (columnNames.length != columnValues.length) {
            throw new DatabaseOperationsException("The number of columns [" + columnNames.length
                                                  + "] is not the same as the number of values ["
                                                  + columnValues.length + "]");
        }

        for (int i = 0; i < columnNames.length; i++) {
            columns.put(columnNames[i], columnValues[i]);
        }

        try {
            log.debug("Executing insert query in table '" + tableName + "'");
            int rowsInserted = dbProvider.insertRow(tableName, columns);
            if (rowsInserted == 0) {
                // this should never happen
                throw new DatabaseOperationsException("No data was inserted into table '" + tableName
                                                      + "'");
            }
        } catch (DbException e) {
            throw new DatabaseOperationsException("Error executing insert query in table '"
                                                  + tableName + "'", e);
        }
    }

    /**
     *
     * Executes SQL statement, which must be an SQL Data Manipulation Language (DML) statement,
     * such as INSERT, UPDATE or DELETE, or an SQL statement that returns nothing, such as a DDL statement.
     *
     * @param sqlQuery the SQL query to run
     */
    @PublicAtsApi
    public void execute(
                         String sqlQuery ) {

        try {
            log.debug("Executing query: '" + sqlQuery + "'");
            dbProvider.executeUpdate(sqlQuery);
        } catch (DbException e) {
            throw new DatabaseOperationsException("Error executing query '" + sqlQuery + "'", e);
        }
    }

    /**
     * Run delete query
     *
     * @param tableName the table we will delete in
     * @param whereClause the where clause, pass 'null' if want to delete all table values.
     * <br><b>Note: </b>The where clause must content inside any needed parameter escaping.
     * @return the number of deleted rows
     */
    @PublicAtsApi
    public int delete(
                       String tableName,
                       String whereClause ) {

        String sqlQuery = "DELETE FROM " + tableName;
        if (whereClause != null) {
            whereClause = whereClause.trim();
            if (whereClause.length() > 0) {
                sqlQuery = sqlQuery + " WHERE " + whereClause;
            }
        }

        int rowsDeleted;
        try {
            log.debug("Executing query: '" + sqlQuery + "'");
            rowsDeleted = dbProvider.executeUpdate(sqlQuery);
            if (rowsDeleted == 0) {
                log.warn("SQL query '" + sqlQuery + "' affected 0 rows");
            } else {
                log.debug("SQL query '" + sqlQuery + "' affected '" + rowsDeleted + "' rows");
            }
        } catch (DbException e) {
            throw new DatabaseOperationsException("Error executing query '" + sqlQuery + "'", e);
        }
        return rowsDeleted;
    }

    /**
     *  Closes and releases all idle connections that are currently stored
     *  in the connection pool associated with this data source.
     */
    @PublicAtsApi
    public void disconnect() {

        dbProvider.disconnect();
    }

    /**
     * Returns the JDBC meta data about this database,
     * for example if want to get the list of tables into some database.</p>
     * <b>Note:</b> The connection is not closed internally as it must remain open while reading the metadata.
     * The user must call the {@link #disconnect() disconnect} method when done.
     *
     * @return the database meta data instance
     */
    @PublicAtsApi
    public DatabaseMetaData getDatabaseMetaData() {

        return dbProvider.getDatabaseMetaData();
    }

    private String getQuotation() {

        if (dbProvider instanceof CassandraDbProvider) {
            // in this case the use is responsible to provide quotation marks when needed
            return "";
        } else {
            return "'";
        }
    }
}
