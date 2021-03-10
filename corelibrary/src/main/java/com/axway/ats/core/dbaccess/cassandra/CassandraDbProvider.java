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
package com.axway.ats.core.dbaccess.cassandra;

import java.io.InputStream;

import java.nio.ByteBuffer;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.dbaccess.DbQuery;
import com.axway.ats.common.dbaccess.snapshot.TableDescription;
import com.axway.ats.core.dbaccess.DbColumn;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.DbProvider;
import com.axway.ats.core.dbaccess.DbRecordValue;
import com.axway.ats.core.dbaccess.DbRecordValuesList;
import com.axway.ats.core.dbaccess.DbReturnModes;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.validation.exceptions.NumberValidationException;
import com.axway.ats.core.validation.exceptions.ValidationException;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.CodecRegistry;
import com.datastax.driver.core.ColumnDefinitions.Definition;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.DataType.Name;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.ExponentialReconnectionPolicy;
import com.datastax.driver.core.policies.RoundRobinPolicy;
import com.datastax.driver.core.policies.TokenAwarePolicy;

/**
 * Base class implementing Cassandra database access related methods
 *
 */
public class CassandraDbProvider implements DbProvider {

    private static final Logger log = LogManager.getLogger(CassandraDbProvider.class);

    private String              dbHost;
    private int                 dbPort;
    private String              dbName;
    private String              dbUser;
    private String              dbPassword;

    private boolean             allowFiltering;

    private Cluster             cluster;
    private Session             session;

    protected DbConnection      dbConnection;

    public CassandraDbProvider( DbConnCassandra dbConnection ) {

        this.dbHost = dbConnection.getHost();
        this.dbPort = dbConnection.getPort();
        this.dbName = dbConnection.getDb();
        this.dbUser = dbConnection.getUser();
        this.dbPassword = dbConnection.getPassword();

        this.allowFiltering = dbConnection.isAllowFiltering();
        this.dbConnection = dbConnection;
    }

    /**
     * Currently we connect just once and then reuse the connection.
     * We do not bother with closing the connection.
     *
     * It is normal to use one Session per DB. The Session is thread safe.
     */
    private void connect() {

        if (cluster == null) {

            log.info("Connecting to Cassandra server on " + this.dbHost + " at port " + this.dbPort);

            // allow fetching as much data as present in the DB
            QueryOptions queryOptions = new QueryOptions();
            queryOptions.setFetchSize(Integer.MAX_VALUE);
            queryOptions.setConsistencyLevel(ConsistencyLevel.ONE);

            cluster = Cluster.builder()
                             .addContactPoint(this.dbHost)
                             .withPort(this.dbPort)
                             .withLoadBalancingPolicy(new TokenAwarePolicy(new RoundRobinPolicy()))
                             .withReconnectionPolicy(new ExponentialReconnectionPolicy(500, 30000))
                             .withQueryOptions(queryOptions)
                             .withCredentials(this.dbUser, this.dbPassword)
                             .build();

        }

        if (session == null) {

            log.info("Connecting to Cassandra DB with name " + this.dbName);
            session = cluster.connect(dbName);
        }
    }

    @Override
    public void disconnect() {

        try {
            if (session != null) {
                session.close();
                session = null;
            }
        } catch (Exception e) {
            log.warn("Error shutting down Cassandra session", e);
        }

        try {
            if (cluster != null) {
                cluster.close();
                cluster = null;
            }
        } catch (Exception e) {
            log.warn("Error shutting down Cassandra cluster", e);
        }
    }

    @Override
    public DbConnection getDbConnection() {

        return this.dbConnection;
    }

    @Override
    public int executeUpdate(
                              String query ) throws DbException {

        if (log.isDebugEnabled()) {
            log.debug("Run SQL query: '" + query + "'");
        }

        connect();

        session.execute(query);

        // we do not know the number of updated rows
        return -1;
    }

    @Override
    public DbRecordValuesList[] select(
                                        String query ) throws DbException {

        return this.select(new DbQuery(query, new ArrayList<Object>()));
    }

    @Override
    public DbRecordValuesList[] select(
                                        DbQuery dbQuery ) throws DbException {

        return select(dbQuery, DbReturnModes.OBJECT);
    }

    @Override
    public DbRecordValuesList[] select(
                                        DbQuery dbQuery,
                                        DbReturnModes dbReturnMode ) throws DbException {

        connect();

        ArrayList<DbRecordValuesList> dbRecords = new ArrayList<DbRecordValuesList>();

        String sqlQuery = dbQuery.getQuery();
        if (allowFiltering) {
            sqlQuery += " ALLOW FILTERING";
        }

        if (log.isDebugEnabled()) {
            log.debug(sqlQuery);
        }

        ResultSet results = session.execute(sqlQuery);

        int currentRow = 0;
        Iterator<Row> it = results.iterator();
        while (it.hasNext()) {
            Row row = it.next();

            currentRow++;
            if (log.isDebugEnabled()) {
                log.debug("Result row number: " + currentRow);
            }

            DbRecordValuesList recordList = new DbRecordValuesList();

            for (Definition columnDefinition : row.getColumnDefinitions()) {
                DbColumn dbColumn = new DbColumn(columnDefinition.getTable(), columnDefinition.getName());
                dbColumn.setColumnType(columnDefinition.getType().getName().toString());

                Object value = extractObjectFromResultSet(row, columnDefinition);

                DbRecordValue recordValue = new DbRecordValue(dbColumn, value);
                recordList.add(recordValue);
            }
            dbRecords.add(recordList);
        }

        return dbRecords.toArray(new DbRecordValuesList[]{});
    }

    /**
     * Returns a map with column name as key and column date type as value.
     *
     * The value might be as simple as "Boolean" or more complex like
     *  - "Set|Boolean"
     *  - "List|String"
     *  - "Map|String|Integer"
     *  these are cases when the data type is a container of primitive data types.
     *
     * @param tableName
     * @return
     * @throws DbException
     */
    public Map<String, String> getColumnInfo(
                                              String tableName ) throws DbException {

        connect();

        ResultSet results = session.execute("SELECT * FROM " + this.dbName + "." + tableName + " LIMIT 1");

        Map<String, String> columnInfo = new HashMap<String, String>();
        for (Definition columnDefinition : results.getColumnDefinitions()) {
            DataType dataType = columnDefinition.getType();
            String dataTypeName = dataType.getName().name();
            if ("Set".equalsIgnoreCase(dataTypeName)) {
                dataTypeName = dataTypeName + "|" + dataType.getTypeArguments().get(0);
            } else if ("List".equalsIgnoreCase(dataTypeName)) {
                dataTypeName = dataTypeName + "|" + dataType.getTypeArguments().get(0);
            } else if ("Map".equalsIgnoreCase(dataTypeName)) {
                dataTypeName = dataTypeName + "|" + dataType.getTypeArguments().get(0) + "|"
                               + dataType.getTypeArguments().get(1);
            }
            columnInfo.put(columnDefinition.getName(), dataTypeName);
        }

        return columnInfo;
    }

    private static Object extractObjectFromResultSet(
                                                      Row row,
                                                      Definition columnDefinition ) {

        Object object;

        String columnName = columnDefinition.getName();
        DataType dataType = columnDefinition.getType();
        Name columnTypeName = dataType.getName();

        if (columnTypeName.equals(DataType.Name.UUID)) {
            object = row.getUUID(columnName);
        } else if (columnTypeName.equals(DataType.Name.TIMEUUID)) {
            object = row.getUUID(columnName);
        } else if (columnTypeName.equals(DataType.Name.BOOLEAN)) {
            /* By default null values are deserialized to false (False) 
             * That's why this check is needed, in order to distinguish between null and false values
             * */
            if (row.isNull(columnName)) {
                object = null;
            } else {
                object = row.getBool(columnName);
            }
        } else if (columnTypeName.equals(DataType.Name.INT)) {
            /* By default null values are deserialized to 0
             * That's why this check is needed, in order to distinguish between null and 0 values
             * */
            if (row.isNull(columnName)) {
                object = null;
            } else {
                object = row.getInt(columnName);
            }
        } else if (columnTypeName.equals(DataType.Name.BIGINT)) {
            /* By default null values are deserialized to 0
             * That's why this check is needed, in order to distinguish between null and 0 values
             * */
            if (row.isNull(columnName)) {
                object = null;
            } else {
                object = row.getLong(columnName);
            }
        } else if (columnTypeName.equals(DataType.Name.FLOAT)) {
            /* By default null values are deserialized to 0
             * That's why this check is needed, in order to distinguish between null and 0 values
             * */
            if (row.isNull(columnName)) {
                object = null;
            } else {
                object = row.getFloat(columnName);
            }
        } else if (columnTypeName.equals(DataType.Name.DOUBLE)) {
            /* By default null values are deserialized to 0
             * That's why this check is needed, in order to distinguish between null and 0 values
             * */
            if (row.isNull(columnName)) {
                object = null;
            } else {
                object = row.getDouble(columnName);
            }
        } else if (columnTypeName.equals(DataType.Name.COUNTER)) {
            /* By default null values are deserialized to 0
             * That's why this check is needed, in order to distinguish between null and 0 values
             * */
            if (row.isNull(columnName)) {
                object = null;
            } else {
                object = row.getLong(columnName);
            }
        } else if (columnTypeName.equals(DataType.Name.DECIMAL)) {
            object = row.getDecimal(columnName);
        } else if (columnTypeName.equals(DataType.Name.TEXT)
                   || columnTypeName.equals(DataType.Name.VARCHAR)) {
            object = row.getString(columnName);
        } else if (columnTypeName.equals(DataType.Name.TIMESTAMP)) {
            object = row.getTimestamp(columnName);
        } else if (columnTypeName.equals(DataType.Name.DATE)) {
            object = row.getDate(columnName);
        } else if (columnTypeName.equals(DataType.Name.BLOB)) {
            object = row.getBytes(columnName);
        } else if (columnTypeName.equals(DataType.Name.SET)) {
            // this is the class of the set's elements (i.e. for a Set<String>, clazz variable will be equal to String.class)
            Class<?> clazz = new CodecRegistry().codecFor(dataType.getTypeArguments().get(0))
                                                .getJavaType()
                                                .getRawType();
            object = row.getSet(columnName, clazz);
        } else if (columnTypeName.equals(DataType.Name.LIST)) {
            // this is the class of the list's elements (i.e. for a Set<String>, clazz variable will be equal to String.class)
            Class<?> clazz = new CodecRegistry().codecFor(dataType.getTypeArguments().get(0))
                                                .getJavaType()
                                                .getRawType();
            object = row.getList(columnName, clazz);
        } else if (columnTypeName.equals(DataType.Name.MAP)) {
            /* this is the class of the map's key and value elements
             * for a Map<Integer, String>,
             * keyClazz variable will be equal to Integer.class
             * and valueClazz variable will be equal to String.class
             */
            Class<?> keyClazz = new CodecRegistry().codecFor(dataType.getTypeArguments().get(0))
                                                   .getJavaType()
                                                   .getRawType();
            Class<?> valueClazz = new CodecRegistry().codecFor(dataType.getTypeArguments().get(1))
                                                     .getJavaType()
                                                     .getRawType();
            object = row.getMap(columnName, keyClazz, valueClazz);
        } else {
            throw new DbException("Unsupported data type '" + columnDefinition.getType().toString()
                                  + "' for table '" + columnDefinition.getTable() + "' and column '"
                                  + columnName + "'");
        }
        return object;
    }

    @Override
    public InputStream selectValue(
                                    String tableName,
                                    String keyColumn,
                                    String keyValue,
                                    String queryColumn ) throws DbException {

        throw new DbException("Not implemented");
    }

    @Override
    public InputStream selectValue(
                                    String tableName,
                                    String keyColumn,
                                    String keyValue,
                                    String queryColumn,
                                    int recordNumber ) throws DbException {

        throw new DbException("Not implemented");
    }

    @Override
    public InputStream selectValue(
                                    String tableName,
                                    String[] keyColumns,
                                    String[] keyValues,
                                    String queryColumn ) throws DbException, ValidationException {

        throw new DbException("Not implemented");
    }

    @Override
    public InputStream selectValue(
                                    String tableName,
                                    String[] keyColumns,
                                    String[] keyValues,
                                    String queryColumn,
                                    int recordNumber ) throws DbException, ValidationException {

        throw new DbException("Not implemented");
    }

    @Override
    public int rowCount(
                         String tableName ) throws DbException, NumberValidationException {

        return rowCount(tableName, null);
    }

    @Override
    public int rowCount(
                         String tableName,
                         String columnNameWhere,
                         String whereValue ) throws DbException, NumberValidationException {

        String whereClause = columnNameWhere + "='" + whereValue + "'";
        return rowCount(tableName, whereClause);
    }

    @Override
    public int rowCount(
                         String tableName,
                         String whereCondition ) throws DbException, NumberValidationException {

        int returnCount;
        String sql = "SELECT " + " COUNT(*)" + " FROM " + tableName;

        if (whereCondition != null && whereCondition.length() > 0) {
            sql += " WHERE " + whereCondition;
        }

        DbRecordValuesList[] records = select(new DbQuery(sql, new ArrayList<Object>()),
                                              DbReturnModes.STRING);

        try {
            returnCount = records == null
                                          ? 0
                                          : ((Long) records[0].get("count")).intValue();
        } catch (NumberFormatException nfe) {
            throw new NumberValidationException("The row count could not be converted to integer", nfe);
        }

        return returnCount;
    }

    @Override
    public int insertRow(
                          String tableName,
                          Map<String, String> columns ) throws DbException {

        int iRowsInserted = -1;

        StringBuilder columnsString = new StringBuilder();
        StringBuilder valuesString = new StringBuilder();

        //get iterator for the columns and add them to a string
        Iterator<Entry<String, String>> iter = columns.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, String> coumnEntry = iter.next();

            columnsString.append(coumnEntry.getKey()).append(",");

            String columnValue = coumnEntry.getValue();

            String dataType = coumnEntry.getKey().toLowerCase();
            if (dataType.startsWith("string") || dataType.startsWith("text")
                || dataType.startsWith("varchar") || dataType.startsWith("timestamp")
                || dataType.startsWith("date")) {
                columnValue = "'" + columnValue + "'";
            }

            valuesString.append(columnValue).append(",");
        }
        columnsString.setLength(columnsString.length() - 1);
        valuesString.setLength(valuesString.length() - 1);

        //execute the query
        String sSql = "INSERT INTO " + tableName + " (" + columnsString + ") VALUES (" + valuesString + ")";
        iRowsInserted = executeUpdate(sSql);

        return iRowsInserted;
    }

    @Override
    public boolean isReservedWord(
                                   String value ) {

        return false;
    }

    @Override
    public List<TableDescription> getTableDescriptions( List<String> tablesToSkip ) {

        throw new RuntimeException("Method not implemented");
    }

    @Override
    public DatabaseMetaData getDatabaseMetaData() throws DbException {

        throw new RuntimeException("Method not implemented");
    }
}
