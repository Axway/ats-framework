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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.axway.ats.common.dbaccess.DbQuery;
import com.axway.ats.common.dbaccess.snapshot.TableDescription;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.dbaccess.exceptions.DbRecordsException;
import com.axway.ats.core.dbaccess.oracle.OracleDbProvider;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.core.validation.exceptions.ArrayEmptyException;
import com.axway.ats.core.validation.exceptions.NumberValidationException;
import com.axway.ats.core.validation.exceptions.ValidationException;

/**
 * Abstract database provider - it provides common functionality for all providers
 */
public abstract class AbstractDbProvider implements DbProvider {

    private static Logger    log;
    private static final int BYTE_BUFFER_SIZE = 1024;

    protected DbConnection   dbConnection;

    private Connection       connection;

    protected String[]       reservedWords    = new String[]{};

    /**
     * Constructor to create authenticated connection to a database.
     *
     * @param dbconn db-connection object
     */
    protected AbstractDbProvider( DbConnection dbconn ) {

        log = Logger.getLogger( this.getClass() );
        this.dbConnection = dbconn;
    }

    /**
     *  Closes and releases all idle connections that are currently stored
     *  in the connection pool associated with this data source.
     */
    public void disconnect() {

        try {
            if( connection != null ) {
                connection.close();
            }
        } catch( SQLException sqle ) {
            throw new DbException( sqle );
        }

        ConnectionPool.removeConnection( dbConnection );
        dbConnection.disconnect();
    }

    public DbConnection getDbConnection() {

        return dbConnection;
    }

    /* (non-Javadoc)
     * @see com.tumbleweed.automation.core.dbaccess.DbProvider#getDatabaseMetaData()
     */
    @Override
    public DatabaseMetaData getDatabaseMetaData() {

        connection = ConnectionPool.getConnection( dbConnection );

        try {
            return connection.getMetaData();
        } catch( SQLException e ) {
            throw new DbException( "Error getting database metadata", e );
        }
    }

    public DbRecordValuesList[] select( String query ) throws DbException {

        return this.select( new com.axway.ats.common.dbaccess.DbQuery( query, new ArrayList<Object>() ) );
    }

    public DbRecordValuesList[] select( com.axway.ats.common.dbaccess.DbQuery dbQuery ) throws DbException {

        return select( dbQuery, DbReturnModes.OBJECT );
    }

    public DbRecordValuesList[] select( com.axway.ats.common.dbaccess.DbQuery dbQuery,
                                        DbReturnModes dbReturnMode ) throws DbException {

        connection = ConnectionPool.getConnection( dbConnection );

        final String errMsg = "Error running or parsing result of sql query '" + dbQuery.getQuery() + "'";

        ArrayList<DbRecordValuesList> dbRecords = new ArrayList<DbRecordValuesList>();
        log.debug( dbQuery.getQuery() ); // debug current query
        try (PreparedStatement st = prepareStatement( connection, dbQuery.getQuery(),
                                                      dbQuery.getArguments() );
                ResultSet res = st.executeQuery()) {

            ResultSetMetaData rsmd = res.getMetaData();

            int numberOfColumns = rsmd.getColumnCount();
            int currentRow = 0;
            while( res.next() ) {
                currentRow++;
                if( log.isDebugEnabled() ) {
                    log.debug( "Result row number: " + currentRow );
                }
                DbRecordValuesList recordList = new DbRecordValuesList();

                for( int i = 1; i <= numberOfColumns; i++ ) {
                    String tableName = rsmd.getTableName( i );
                    String columnName = rsmd.getColumnName( i );
                    DbColumn dbColumn = new DbColumn( tableName, columnName );
                    dbColumn.setColumnType( rsmd.getColumnTypeName( i ) );

                    DbRecordValue recordValue = null;

                    //get the columns in the appropriate type
                    try {
                        //get the columns in the appropriate type
                        switch( dbReturnMode ){
                            case OBJECT:
                                recordValue = parseDbRecordAsObject( dbColumn, res, i );
                                break;

                            case INPUT_STREAM:
                                recordValue = parseDbRecordAsInputStream( dbColumn, res, i );
                                break;

                            case STRING:
                            case ESCAPED_STRING:
                                recordValue = parseDbRecordAsString( dbColumn, res, i );
                                break;

                            default:
                                throw new DbException( "Getting the values as " + dbReturnMode.name()
                                                       + " is not supported. Table '"
                                                       + dbColumn.getTableName() + "', column '"
                                                       + dbColumn.getColumnName() + "'" );
                        }
                    } finally {
                        if( recordValue == null ) {
                            // help locate error case when we have exception from the underlying calls in try block
                            log.error( "Error getting value for table '" + tableName + "', row number "
                                       + currentRow + ",column " + i + ",named '" + columnName + "'" );
                        } else {
                            // Trace. This could produce huge data so using lowest possible severity.
                            if( log.isTraceEnabled() ) {
                                log.trace( "Value for column " + i + ",named '" + columnName + "' is '"
                                           + recordValue.getValue() + "'" );
                            }
                        }
                    }
                    recordList.add( recordValue );
                }

                dbRecords.add( recordList );
            }
        } catch( SQLException e ) {
            throw new DbException( errMsg, e );
        } catch( IOException ioe ) {
            throw new DbException( errMsg, ioe );
        } finally {
            try {
                connection.close();
            } catch( SQLException sqle ) {
                log.error( sqle );
            }
        }

        return dbRecords.toArray( new DbRecordValuesList[]{} );
    }

    protected DbRecordValue parseDbRecordAsObject( DbColumn dbColumn, ResultSet res,
                                                   int columnIndex ) throws IOException, SQLException {

        return new DbRecordValue( dbColumn, res.getObject( columnIndex ) );
    }

    protected DbRecordValue parseDbRecordAsInputStream( DbColumn dbColumn, ResultSet res,
                                                        int columnIndex ) throws IOException, SQLException {

        return new DbRecordValue( dbColumn,
                                  getClonedInputStream( getResultAsInputStream( res, columnIndex,
                                                                                dbColumn.getColumnType() ) ) );
    }

    protected DbRecordValue parseDbRecordAsString( DbColumn dbColumn, ResultSet res,
                                                   int columnIndex ) throws IOException, SQLException {

        return new DbRecordValue( dbColumn,
                                  getResultAsEscapedString( res, columnIndex, dbColumn.getColumnType() ) );
    }

    public InputStream selectValue( String tableName, String keyColumn, String keyValue,
                                    String queryColumn ) throws DbException {

        return selectValue( tableName, keyColumn, keyValue, queryColumn, 0 );
    }

    public InputStream selectValue( String tableName, String keyColumn, String keyValue, String queryColumn,
                                    int recordNumber ) throws DbException {

        InputStream queryValueStream = null;

        String sql = " SELECT " + queryColumn + " FROM " + tableName + " WHERE " + keyColumn + " = '"
                     + escapeSql( keyValue ) + "'";

        DbRecordValuesList[] records = select( new DbQuery( sql, new ArrayList<Object>() ),
                                               DbReturnModes.INPUT_STREAM );
        if( records == null ) {
            throw new DbRecordsException();
        }
        if( records.length < recordNumber + 1 ) {
            throw new DbRecordsException( records.length, recordNumber + 1 );
        }

        queryValueStream = ( InputStream ) records[recordNumber].get( queryColumn );

        return queryValueStream;
    }

    public InputStream selectValue( String tableName, String[] keyColumns, String[] keyValues,
                                    String queryColumn ) throws DbException, ValidationException {

        return selectValue( tableName, keyColumns, keyValues, queryColumn, 0 );
    }

    public InputStream selectValue( String tableName, String[] keyColumns, String[] keyValues,
                                    String queryColumn,
                                    int recordNumber ) throws DbException, ValidationException {

        InputStream queryValueStream = null;

        //all operands are "equals"
        String[] whereOperands = new String[keyColumns.length];
        for( int i = 0; i < whereOperands.length; i++ ) {
            whereOperands[i] = "=";
        }

        String sql = constructSelectStatement( new String[]{ queryColumn }, tableName, keyColumns, keyValues,
                                               whereOperands );

        DbRecordValuesList[] records = select( new DbQuery( sql, new ArrayList<Object>() ),
                                               DbReturnModes.INPUT_STREAM );
        if( records == null ) {
            throw new DbRecordsException();
        }
        if( records.length < recordNumber + 1 ) {
            throw new DbRecordsException( records.length, recordNumber + 1 );
        } else {
            queryValueStream = ( InputStream ) records[recordNumber].get( queryColumn );
        }

        return queryValueStream;
    }

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
    public int insertRow( String tableName, Map<String, String> columns ) throws DbException {

        int iRowsInserted = -1;

        StringBuilder columnString = new StringBuilder();
        StringBuilder valuesString = new StringBuilder();

        //get iterator for the columns and add them to a string
        Iterator<Entry<String, String>> iter = columns.entrySet().iterator();
        while( iter.hasNext() ) {
            Entry<String, String> coumnEntry = iter.next();

            columnString.append( coumnEntry.getKey() ).append( "," );

            String columnValue = coumnEntry.getValue();

            if( !isReservedWord( columnValue ) ) {
                columnValue = wrapData( columnValue );
            }

            valuesString.append( columnValue ).append( "," );
        }
        columnString.setLength( columnString.length() - 1 );
        valuesString.setLength( valuesString.length() - 1 );

        //execute the query
        String sSql = "INSERT INTO " + tableName + " (" + columnString + ") VALUES (" + valuesString + ")";
        iRowsInserted = executeUpdate( sSql );

        return iRowsInserted;
    }

    /**
     * Executes a given SQL update statement and returns number of updated rows
     * @param query the SQL query to execute
     * <br/><b>Note: </b>The SQL query must content inside any needed parameter escaping.
     * @return the number of rows affected
     * @throws DbException
     */
    @Override
    public int executeUpdate( String query ) throws DbException {

        log.debug( "Run SQL query: '" + query + "'" );
        connection = ConnectionPool.getConnection( dbConnection );
        int rowsUpdated = 0;
        PreparedStatement stmnt = null;
        try {
            stmnt = connection.prepareStatement( query );
            rowsUpdated = stmnt.executeUpdate();
        } catch( SQLException e ) {
            log.error( "SQL errorCode=" + e.getErrorCode() + " sqlState=" + e.getSQLState() + " "
                       + e.getMessage(), e );
            throw new DbException( e );
        } finally {
            DbUtils.close( connection, stmnt );
        }
        return rowsUpdated;
    }

    /**
     * @param tableName         the name of the table
     * @return                  returns the number of the rows that
     *                          match the where statement as a int. Returns 0 if there is
     *                          an error or the rowcount is 0
     */
    @Override
    public int rowCount( String tableName ) throws DbException, NumberValidationException {

        return rowCount( tableName, null );
    }

    /**
     * @param tableName         the name of the table
     * @param columnNameWhere   the column name for the where statement
     * @param whereValue        the where value for the where statement
     * @return                  returns the number of the rows that
     *                          match the where statement as a int. Returns 0 if there is
     *                          an error or the rowcount is 0
     */
    @Override
    public int rowCount( String tableName, String columnNameWhere,
                         String whereValue ) throws DbException, NumberValidationException {

        String whereClause = escapeSql( columnNameWhere ) + "='" + escapeSql( whereValue ) + "'";
        return rowCount( tableName, whereClause );
    }

    /**
     * @param tableName         the name of the table
     * @param whereCondition    the where condition ( without the WHERE keyword )
     * @return                  returns the number of the rows that
     *                          match the where statement as a int. Returns 0 if there is
     *                          an error or the rowcount is 0
     */
    @Override
    public int rowCount( String tableName, String whereCondition ) throws DbException,
                                                                   NumberValidationException {

        int returnCount;
        String sql = " SELECT " + " COUNT(*)" + " FROM " + tableName;

        if( whereCondition != null && whereCondition.length() > 0 ) {
            sql += " WHERE " + whereCondition;
        }

        DbRecordValuesList[] records = select( new DbQuery( sql, new ArrayList<Object>() ),
                                               DbReturnModes.STRING );

        try {
            returnCount = records == null
                                          ? 0
                                          : Integer.parseInt( ( String ) records[0].get( "COUNT(*)" ) );
        } catch( NumberFormatException nfe ) {
            throw new NumberValidationException( "The row count could not be converted to integer", nfe );
        }

        return returnCount;
    }

    private Object getClonedInputStream( InputStream is ) throws IOException {

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[BYTE_BUFFER_SIZE];
            int bytesRead = -1;
            while( ( bytesRead = is.read( buffer ) ) > -1 ) {
                out.write( buffer, 0, bytesRead );
            }
            ByteArrayInputStream bis = new ByteArrayInputStream( out.toByteArray() );
            IoUtils.closeStream( out );
            return bis;
        } finally {
            IoUtils.closeStream( is );
        }
    }

    // currently only works with few types
    private PreparedStatement prepareStatement( Connection connection, String query,
                                                List<Object> arguments ) throws SQLException, DbException {

        PreparedStatement result = connection.prepareStatement( query );

        if( arguments.size() > 0 ) {

            int possition = 1;

            // cycle through arguments and add each of them
            // as an parameter to the SQL query
            for( Object argument : arguments ) {

                if( argument instanceof String ) {
                    result.setString( possition++, ( String ) argument );
                } else if( argument instanceof byte[] ) {
                    result.setBytes( possition++, ( byte[] ) argument );
                } else {
                    throw new DbException( "Unable to build query because of inavalid parameter types" );
                }
            }
        }

        return result;
    }

    /**
     * Get particular entry from the result set as {@link InputStream}
     *
     * @param resultSet the result set
     * @param index current result number from the result set
     * @param columnTypeName column type name (as {@link String}) of the current result from the result set.
     *      Column type is required for SQL database types which are not mapped from the JDBC driver to some default
     *      java types. For example TIMESTAMP Oracle SQL type, which is not mapped to java.sql.Timestamp like
     *      like oracle.sql.DATE.
     * @return the value as {@link InputStream}
     * @throws SQLException
     */
    protected InputStream getResultAsInputStream( ResultSet resultSet, int index,
                                                  String columnTypeName ) throws SQLException {

        Object valueAsObject = resultSet.getObject( index );
        if( valueAsObject == null ) {
            return null;
        }
        InputStream is;
        if( valueAsObject != null && valueAsObject.getClass().isArray() ) {
            // we have an array of primitive data type
            if( ! ( valueAsObject instanceof byte[] ) ) {
                // FIXME other array types might be needed to be tracked in a different way
                log.warn( "Array type that needs attention" );
            }
            is = resultSet.getBinaryStream( index );
        } else if( valueAsObject instanceof Blob ) {

            Blob blobValue = ( Blob ) valueAsObject;
            is = blobValue.getBinaryStream();
        } else {

            // treat as a string
            String value = resultSet.getString( index );
            is = new ByteArrayInputStream( value.getBytes() );
            logDebugInfoForDBValue( value, index, resultSet );
        }
        return is;
    }

    /**
     * Get particular entry from the result set as String (escaped).
     * Used mainly for representation in backup script file.
     * Supports long types like BLOB, LONGVARBINARY, BINARY - returned as string
     *  appropriate format w/o control chars inside.
     * @param resultSet the result set
     * @param index current result number from the result set
     * @param columnTypeName column type name (as {@link String}) of the current result from the result set.
     *      Column type is required for SQL database types which are not mapped from the JDBC driver to some default
     *      java types. For example TIMESTAMP Oracle SQL type, which is not mapped to java.sql.Timestamp like
     *      like oracle.sql.DATE.
     * @return the value itself not wrapped in any control characters like apostrophes.
     * @throws SQLException
     * @throws IOException
     */
    protected abstract String getResultAsEscapedString( ResultSet resultSet, int index,
                                                        String columnTypeName ) throws SQLException,
                                                                                IOException;

    /**
     * Convert an array of bytes to a HEX string
     *
     * @param data the byte array
     * @param size the number of bytes to read
     * @return a HEX string representing the binary data
     */
    protected String bytesToHex( byte[] data, int size ) {

        StringBuffer buf = new StringBuffer();
        for( int i = 0; i < size; i++ ) {
            String hexString = Integer.toHexString( data[i] & 0xFF );
            if( hexString.length() == 1 ) {
                buf.append( "0" );
            }

            buf.append( hexString );
        }
        return buf.toString().toUpperCase();
    }

    protected String wrapSQL( String string ) {

        return "`" + string + "`";
    }

    protected String wrapData( String string ) {

        return "'" + escapeSql( string ) + "'";
    }

    public static String escapeSql( String value ) {

        //remove escape characters that are passed from Java
        String escaped = value.toString();

        escaped = escaped.replaceAll( "\\\\", "\\\\\\\\" );
        escaped = escaped.replaceAll( "'", "''" );
        escaped = escaped.replaceAll( "\"", "\\\\\"" );

        return escaped;
    }

    /**
     * Constructs a SELECT statement given a list of parameters
     *
     * @param selectValues an array of all the selected values
     * @param table the table to select from
     * @param whereColumns the columns to match if there is a WHERE clause
     * @param whereValues the values of the columns to match if there is a WHERE clause
     * @param whereOperands any additional WHERE operands
     * @return the resulting SELECT statement
     *
     * @throws ValidationException if the list of parameters is incorrect
     */
    public String constructSelectStatement( String[] selectValues, String table, String[] whereColumns,
                                            String[] whereValues,
                                            String[] whereOperands ) throws ValidationException {

        if( selectValues.length == 0 ) {
            throw new ArrayEmptyException( "Array with columns to select is empty" );
        }
        if( whereColumns.length != whereValues.length || whereColumns.length != whereOperands.length ) {
            throw new ValidationException( "Arrays for \"where\" statements have diffrent sizes. \n\tWhereColumns: "
                                           + whereColumns.length + "\n\tWhereValues:" + whereValues.length
                                           + "\n\tWhereOperands:" + whereOperands.length );
        }

        StringBuilder sql = new StringBuilder().append( "SELECT  " );

        for( int i = 0; i < selectValues.length; i++ ) {
            if( i > 0 ) {
                sql.append( ", " );
            }
            sql.append( selectValues[i] );

        }

        sql.append( " FROM " + table );

        if( whereColumns.length == 0 ) {
            log.debug( "sql->" + sql );
            return sql.toString();
        }

        sql.append( " WHERE " );

        int whereNumber = whereColumns.length;
        for( int i = 0; i < whereNumber; i++ ) {
            if( i > 0 ) {
                sql.append( "  AND  " );
            }
            String value = whereValues[i];

            //Transforming the value string if it is not a reserved word
            if( !isReservedWord( value ) ) {

                if( "IN".equalsIgnoreCase( whereOperands[i] ) ) {
                    value = whereValues[i];
                } else {
                    value = wrapData( whereValues[i] );
                }
            }

            String whereStatement = wrapSQL( whereColumns[i] ) + " " + whereOperands[i] + " " + value;
            sql.append( whereStatement );

        }
        log.debug( "sql->" + sql );
        return sql.toString();

    }

    /**
     * Constructs a SELECT statement given a list of parameters
     *
     * @param selectValues an array of all the selected values
     * @param table the table to select from
     * @return the resulting SELECT statement
     *
     * @throws ValidationException if the list of parameters is incorrect
     */
    public String constructSelectStatement( String[] selectValues, String table ) throws ValidationException {

        return constructSelectStatement( selectValues, table, new String[]{}, new String[]{},
                                         new String[]{} );
    }

    /**
     * @see com.axway.ats.core.dbaccess.DbProvider#isReservedWord(java.lang.String)
     */
    public boolean isReservedWord( String value ) {

        if( value != null ) {
            for( String reservedWord : this.reservedWords ) {
                if( value.equals( reservedWord ) ) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Reads bytes from stream and convert them to hex notation
     * @param sb buffer to fill with hex data
     * @param binInputStream input stream
     * @return the StringBuilder parameter used as a clarification
     * @throws IOException if exception is caused during reading from the input stream
     */
    protected StringBuilder addBinDataAsHexAndCloseStream( StringBuilder sb,
                                                           InputStream binInputStream ) throws IOException {

        if( sb == null ) {
            throw new IllegalArgumentException( "Passed null for StringBuilder" );
        }
        if( binInputStream == null ) {
            throw new IllegalArgumentException( "Passed null for InputStream" );
        }
        // read the binary data from the stream and convert it to hex
        try {
            byte[] buffer = new byte[BYTE_BUFFER_SIZE];
            int bytesRead = binInputStream.read( buffer );
            while( bytesRead > 0 ) {
                sb.append( bytesToHex( buffer, bytesRead ) );
                bytesRead = binInputStream.read( buffer );
            }
        } finally {
            IoUtils.closeStream( binInputStream );
        }
        return sb;
    }

    /**
     * Trace with Debug severity info about retrieved value such as the DB and JDBC type
     * @param value value already got from the database
     * @param index the column index (starting from 1) of the cell
     * @param resultSet needed for extra
     * @throws SQLException
     */
    protected void logDebugInfoForDBValue( Object value, int index,
                                           ResultSet resultSet ) throws SQLException {

        if( log.isDebugEnabled() ) {
            // trace column type too
            ResultSetMetaData metaData = resultSet.getMetaData();
            String dbType = metaData.getColumnTypeName( index );
            int javaType = metaData.getColumnType( index );
            log.debug( "DB value is '" + value + "' (retrieved as " + value.getClass().getSimpleName()
                       + "), JDBC type " + javaType + ", DB type " + dbType );
        }
    }

    /**
     * @return description about all tables present in the database
     */
    @Override
    public List<TableDescription> getTableDescriptions() {

        ResultSet tablesResultSet = null;
        List<TableDescription> tables = new ArrayList<TableDescription>();

        try (Connection connection = ConnectionPool.getConnection( dbConnection )) {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            tablesResultSet = databaseMetaData.getTables( null, null, null, new String[]{ "TABLE" } );
            while( tablesResultSet.next() ) {

                if( this instanceof OracleDbProvider
                    && !tablesResultSet.getString( 2 ).equalsIgnoreCase( dbConnection.user ) ) {
                    // Oracle gives us all tables from all databases, we filter here only ours
                    continue;
                }

                String tableName = tablesResultSet.getString( "TABLE_NAME" );
                if( !isTableAccepted( tablesResultSet, dbConnection.db, tableName ) ) {
                    // Table is skipped
                    continue;
                }
                log.debug( "Extracting description about '" + tableName + "' table" );

                TableDescription table = new TableDescription();
                table.setName( tableName );
                table.setSchema( tablesResultSet.getString( "TABLE_SCHEM" ) );

                table.setPrimaryKeyColumn( exctractPrimaryKeyColumn( tableName, databaseMetaData ) );
                table.setIndexes( extractTableIndexes( tableName, databaseMetaData,
                                                       connection.getCatalog() ) );

                List<String> columnDescriptions = new ArrayList<>();
                extractTableColumns( tableName, databaseMetaData, columnDescriptions );
                table.setColumnDescriptions( columnDescriptions );

                tables.add( table );
            }
        } catch( SQLException sqle ) {
            throw new DbException( "Error extracting DB schema information", sqle );
        } finally {
            if( tablesResultSet != null ) {
                try {
                    tablesResultSet.close();
                } catch( SQLException e ) {
                    log.warn( "Result set resouce could not be closed!", e );
                }
            }
        }

        return tables;
    }

    /**
     * Each provider can put restrictions on the types of tables to be processed
     *
     * @param tablesResultSet
     * @param dbName
     * @param tableName
     * @return
     */
    protected boolean isTableAccepted( ResultSet tableResultSet, String dbName, String tableName ) {

        return true;
    }

    private String exctractPrimaryKeyColumn( String tableName,
                                             DatabaseMetaData databaseMetaData ) throws SQLException {

        ResultSet result = null;
        try {
            result = databaseMetaData.getPrimaryKeys( null, null, tableName );
            while( result.next() ) {
                return result.getString( 4 );
            }
        } finally {
            if( result != null ) {
                result.close();
            }
        }

        return null;
    }

    private void extractTableColumns( String tableName, DatabaseMetaData databaseMetaData,
                                      List<String> columnDescription ) throws SQLException {

        ResultSet columnInformation = null;
        try {
            columnInformation = databaseMetaData.getColumns( null, null, tableName, "%" );
            while( columnInformation.next() ) {
                StringBuilder sb = new StringBuilder();
                String columnName = columnInformation.getString( "COLUMN_NAME" );

                sb.append( "name=" + columnName );
                sb.append( ", type=" + sqlTypeToString( columnInformation.getInt( "DATA_TYPE" ) ) );
                sb.append( extractResultSetAttribute( columnInformation, "IS_AUTOINCREMENT",
                                                      "auto increment" ) );
                sb.append( extractResultSetAttribute( columnInformation, "COLUMN_DEF", "default" ) );
                sb.append( extractResultSetAttribute( columnInformation, "IS_NULLABLE", "nullable" ) );
                sb.append( extractResultSetAttribute( columnInformation, "COLUMN_SIZE", "size" ) );
                //            sb.append( extractResultSetAttribute( columnInformation, "DECIMAL_DIGITS", "decimal digits" ) );
                //            sb.append( extractResultSetAttribute( columnInformation, "NUM_PREC_RADIX", "radix" ) );
                //            sb.append( extractResultSetAttribute( columnInformation, "ORDINAL_POSITION", "sequence number" ) );
                //            sb.append( ", source data type=" + sqlTypeToString( columnInformation.getShort( "SOURCE_DATA_TYPE" ) ) );

                columnDescription.add( sb.toString() );
            }
        } finally {
            if( columnInformation != null ) {
                columnInformation.close();
            }
        }
    }

    private Map<String, String> extractTableIndexes( String tableName, DatabaseMetaData databaseMetaData,
                                                     String catalog ) throws SQLException {

        Map<String, String> indexes = new HashMap<>();

        ResultSet indexInformation = databaseMetaData.getIndexInfo( catalog, null, tableName, true, true );
        while( indexInformation.next() ) {
            StringBuilder sb = new StringBuilder();
            String indexName = indexInformation.getString( "INDEX_NAME" );
            if( !StringUtils.isNullOrEmpty( indexName ) ) {
                sb.append( extractResultSetAttribute( indexInformation, "COLUMN_NAME", "column" ) );
                sb.append( extractResultSetAttribute( indexInformation, "INDEX_QUALIFIER",
                                                      "index catalog" ) );
                sb.append( ", type=" + sqlTypeToString( indexInformation.getInt( "TYPE" ) ) );
                sb.append( extractResultSetAttribute( indexInformation, "ASC_OR_DESC", "asc/desc" ) );
                sb.append( extractResultSetAttribute( indexInformation, "NON_UNIQUE", "non-unique" ) );
                sb.append( extractResultSetAttribute( indexInformation, "FILTER_CONDITION",
                                                      "filter condition" ) );
                sb.append( extractResultSetAttribute( indexInformation, "ORDINAL_POSITION",
                                                      "sequence number" ) );

                //                sb.append( extractIndexAttribute( indexInformation, "TABLE_NAME" ) );
                //                sb.append( extractResultSetAttribute( indexInformation, "TYPE", "type" ) );
                //                sb.append( extractIndexAttribute( indexInformation, "CARDINALITY" ) );
                //                sb.append( extractIndexAttribute( indexInformation, "PAGES" ) );
                indexes.put( indexName, sb.toString() );
            }
        }

        return indexes;
    }

    private String extractResultSetAttribute( ResultSet resultSet, String attribute,
                                              String attributeNiceName ) {

        try {
            return ", " + attributeNiceName + "=" + resultSet.getString( attribute );
        } catch( SQLException e ) {
            return "";
        }
    }

    private String sqlTypeToString( int type ) {

        // unfortunately java.sql.Types cannot convert the internal number to a String. So we do it ourselves.
        switch( type ){
            case Types.BIT:
                return "BIT";
            case Types.TINYINT:
                return "BIT";
            case Types.SMALLINT:
                return "BIT";
            case Types.INTEGER:
                return "BIT";
            case Types.BIGINT:
                return "BIT";
            case Types.FLOAT:
                return "BIT";
            case Types.REAL:
                return "BIT";
            case Types.DOUBLE:
                return "BIT";
            case Types.NUMERIC:
                return "BIT";
            case Types.DECIMAL:
                return "BIT";
            case Types.CHAR:
                return "BIT";
            case Types.VARCHAR:
                return "BIT";
            case Types.LONGVARCHAR:
                return "LONGVARCHAR";
            case Types.DATE:
                return "DATE";
            case Types.TIME:
                return "TIME";
            case Types.TIMESTAMP:
                return "TIMESTAMP";
            case Types.BINARY:
                return "BINARY";
            case Types.VARBINARY:
                return "VARBINARY";
            case Types.LONGVARBINARY:
                return "LONGVARBINARY";
            case Types.NULL:
                return "NULL";
            case Types.OTHER:
                return "OTHER";
            case Types.JAVA_OBJECT:
                return "JAVA_OBJECT";
            case Types.DISTINCT:
                return "DISTINCT";
            case Types.STRUCT:
                return "STRUCT";
            case Types.ARRAY:
                return "ARRAY";
            case Types.BLOB:
                return "BLOB";
            case Types.CLOB:
                return "CLOB";
            case Types.REF:
                return "REF";
            case Types.DATALINK:
                return "DATALINK";
            case Types.BOOLEAN:
                return "BOOLEAN";
            case Types.ROWID:
                return "ROWID";
            case Types.NCHAR:
                return "NCHAR";
            case Types.NVARCHAR:
                return "NVARCHAR";
            case Types.LONGNVARCHAR:
                return "LONGNVARCHAR";
            case Types.NCLOB:
                return "NCLOB";
            case Types.SQLXML:
                return "SQLXML";
            default:
                return String.valueOf( type );
        }
    }
}
