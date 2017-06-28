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
package com.axway.ats.core.dbaccess;

import java.io.InputStream;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.axway.ats.common.dbaccess.snapshot.TableDescription;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.validation.exceptions.NumberValidationException;
import com.axway.ats.core.validation.exceptions.ValidationException;

/**
 * This interface should be implemented by all database providers.
 */
public interface DbProvider {

    /**
     * Get the database connection associated with this provider
     *
     * @return the DB connection
     */
    public DbConnection getDbConnection();

    /**
     * @param tablesToSkip list of some tables we are not interested in
     * @return description about all important tables
     */
    public List<TableDescription> getTableDescriptions( List<String> tablesToSkip );

    /**
     * Get the JDBC database metadata.</p> 
     * <b>Note:</b> The connection is not closed as it must remain open while reading the metadata.
     * The user must call the {@link #disconnect() disconnect} method when done reading.
     * 
     * @return the database metadata
     * @throws DbException on error
     */
    public DatabaseMetaData getDatabaseMetaData() throws DbException;

    /**
     * Executes a given SQL update statement and returns number of updated rows
     *
     * @param query the query to execute
     * @return number of rows updated
     * @throws DbException on error
     */
    public int executeUpdate(
                              String query ) throws DbException;

    /**
     * Inserts a row in the given table.
     *
     * @param tableName
     * @param colums
     * @param values This param must look like this: "'string_value', int_value, .."
     * @param config
     * @param log the log object
     *
     * @return The inserted rows, 0 or 1
     */
    public int insertRow(
                          String tableName,
                          Map<String, String> columns ) throws DbException;

    /**
     * @param query
     * @return The result set in a list of hash maps
     * @throws SQLException
     */
    public DbRecordValuesList[] select(
                                        String query ) throws DbException;

    /**
     * @param query
     * @return The result set in a list of hash maps
     * @throws SQLException
     */
    public DbRecordValuesList[] select(
                                        com.axway.ats.common.dbaccess.DbQuery dbQuery )
                                                                                                  throws DbException;

    /**
     * Execute the select query and get the values in the appropriate type
     *
     * @param dbQuery   the select query to execute
     * @param dbReturnMode the type in which to return the values - pass OBJECT for getting the values
     *                  in the default Java object type corresponding to the column's SQL type
     * @return
     * @throws DbException
     */
    public DbRecordValuesList[] select(
                                        com.axway.ats.common.dbaccess.DbQuery dbQuery,
                                        DbReturnModes dbReturnMode ) throws DbException;

    //******************************************************************************

    /**
     * Returns a value from the specified table as {@link InputStream} - the result should contain
     * only one row, otherwise an error is logged.
     *
     * @param tableName
     * @param keyColumn
     * @param keyValue
     * @param queryColumn
     * @return
     */
    public InputStream selectValue(
                                    String tableName,
                                    String keyColumn,
                                    String keyValue,
                                    String queryColumn ) throws DbException;

    /**
     * Returns a value from the specified table and the specified record as {@link InputStream}
     *
     * @param tableName
     * @param keyColumn
     * @param keyValue
     * @param queryColumn
     * @param recordNumber - zero based
     * @return
     */
    public InputStream selectValue(
                                    String tableName,
                                    String keyColumn,
                                    String keyValue,
                                    String queryColumn,
                                    int recordNumber ) throws DbException;

    /**
     * Returns a value from the specified table and the specified record as {@link InputStream}
     *
     * @param tableName
     * @param keyColumns
     * @param keyValues
     * @param queryColumn
     * @param recordNumber
     * @return
     * @throws DbException
     * @throws ValidationException
     */
    public InputStream selectValue(
                                    String tableName,
                                    String[] keyColumns,
                                    String[] keyValues,
                                    String queryColumn ) throws DbException, ValidationException;

    /**
     * Returns a value from the specified table and the specified record as {@link InputStream}
     *
     * @param tableName
     * @param keyColumns
     * @param keyValues
     * @param queryColumn
     * @param recordNumber
     * @return
     * @throws DbException
     * @throws ValidationException
     */
    public InputStream selectValue(
                                    String tableName,
                                    String[] keyColumns,
                                    String[] keyValues,
                                    String queryColumn,
                                    int recordNumber ) throws DbException, ValidationException;

    /**
     * @param tableName         the name of the table
     * @return                  returns the number of the rows that
     *                          match the where statement as a int. Returns 0 if there is
     *                          an error or the rowcount is 0
     */
    public int rowCount(
                         String tableName ) throws DbException, NumberValidationException;

    /**
     * @param tableName         the name of the table
     * @param columnNameWhere   the column name for the where statement
     * @param whereValue        the where value for the where statement
     * @return                  returns the number of the rows that
     *                          match the where statement as a int. Returns 0 if there is
     *                          an error or the rowcount is 0
     */
    public int rowCount(
                         String tableName,
                         String columnNameWhere,
                         String whereValue ) throws DbException, NumberValidationException;

    /**
     * @param tableName         the name of the table
     * @param whereCondition    the where condition ( without the WHERE keyword )
     * @return                  returns the number of the rows that
     *                          match the where statement as a int. Returns 0 if there is
     *                          an error or the rowcount is 0
     */
    public int rowCount(
                         String tableName,
                         String whereCondition ) throws DbException, NumberValidationException;

    /**
     * Check if the given string is a reserved word for the given provider
     *
     * @param value     the value to check
     * @return          ture if it is a reserved word
     */
    public boolean isReservedWord(
                                   String value );

    /**
     *  Closes and releases all idle connections that are currently stored
     *  in the connection pool associated with this data source.
     */
    public void disconnect();

}
