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
package com.axway.ats.core.dbaccess.mssql;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.axway.ats.core.dbaccess.AbstractDbProvider;
import com.axway.ats.core.dbaccess.ConnectionPool;
import com.axway.ats.core.dbaccess.DbColumn;
import com.axway.ats.core.dbaccess.DbRecordValue;
import com.axway.ats.core.dbaccess.DbRecordValuesList;
import com.axway.ats.core.dbaccess.DbUtils;
import com.axway.ats.core.dbaccess.MssqlColumnDescription;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.utils.ExceptionUtils;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;

/**
 * Base class implementing SQLServer database access related methods
 *
 */
public class MssqlDbProvider extends AbstractDbProvider {

    private static final Logger log = Logger.getLogger( MssqlDbProvider.class );

    /**
     * Constructor to create authenticated connection to a database.
     *
     * @param dbconn db-connection object
     */
    public MssqlDbProvider( DbConnSQLServer dbConnection ) {

        super( dbConnection );
    }

    /**
     * Gets a first row of a result set of a query and returns it as a HashMap.
     *
     * @param sQuery the query to be executed - expected to return only one row
     * @return HashMap object with the column names and values of the row
     * @exception SQLException - if a database error occurs
     */
    public Map<String, String> getFirstRow( String sQuery ) throws DbException {

        ResultSet rs = null;
        Connection connection = ConnectionPool.getConnection( dbConnection );

        HashMap<String, String> hash = new HashMap<String, String>();

        PreparedStatement stmnt = null;
        try {
            stmnt = connection.prepareStatement( sQuery );
            rs = stmnt.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();

            // get the first row
            if( rs.next() ) {
                // iterate the columns and fill the hash map
                for( int i = 1; i <= rsmd.getColumnCount(); i++ ) {
                    String columnName = rsmd.getColumnName( i );
                    String columnValue = rs.getString( i );
                    hash.put( columnName, columnValue );
                }
            }
        } catch( SQLException ex ) {
            log.error( ExceptionUtils.getExceptionMsg( ex ) );
        } finally {
            DbUtils.closeResultSet( rs );
            DbUtils.close( connection, stmnt );
        }

        return hash;
    }

    @Override
    protected String getResultAsEscapedString( ResultSet resultSet, int index,
                                               String columnTypeName ) throws SQLException, IOException {

        String value;
        Object valueAsObject = resultSet.getObject( index );
        if( valueAsObject == null ) {
            return null;
        }
        if( valueAsObject != null && valueAsObject.getClass().isArray() ) {
            if( ! ( valueAsObject instanceof byte[] ) ) {
                // FIXME other array types might be needed to be tracked in a different way
                log.warn( "Array type that needs attention" );
            }
            // we have an array of primitive data type
            InputStream is = null;
            try {
                is = resultSet.getAsciiStream( index );
                value = IoUtils.streamToString( is );
            } finally {
                IoUtils.closeStream( is );
            }
        } else if( valueAsObject instanceof Blob ) {
            // we have a blob
            log.debug( "Blob detected. Will try to dump as hex" );
            Blob blobValue = ( Blob ) valueAsObject;
            InputStream blobInputStream = blobValue.getBinaryStream();
            StringBuilder hexString = new StringBuilder();

            //read the binary data from the stream and convert it to hex according to the sample from
            // http://www.herongyang.com/jdbc/Oracle-BLOB-SQL-INSERT.html - see 3 variants for Oracle, MsSQL and MySQL
            hexString = addBinDataAsHexAndCloseStream( hexString, blobInputStream );
            value = hexString.toString();
        } else {
            // treat as a string
            value = resultSet.getString( index );
            logDebugInfoForDBValue( value, index, resultSet );
        }

        return value;
    }

    @Override
    protected DbRecordValue parseDbRecordAsObject( DbColumn dbColumn, ResultSet res,
                                                   int columnIndex ) throws IOException, SQLException {

        DbRecordValue recordValue = null;
        String type = dbColumn.getColumnType().toLowerCase();
        String name = dbColumn.getColumnName().toLowerCase();

        MssqlColumnDescription columnDescription = new MssqlColumnDescription( name, type );

        // we check additionally for 'text' and 'ntext', because res.getObject(index) return String type,
        // otherwise we get the object address
        if( columnDescription.isTypeBinary() || "text".equals( type ) || "ntext".equals( type ) ) {
            /*
             * http://jtds.sourceforge.net/faq.html
             * By default useLOBs is true. In such cases calling getObject on the result set returns
             * a LOB object, not a java String. In order to get java String it is needed to use the getString method.
             * If useLOBs is false - getObject returns java String
             *
             * "useLOBs=false;" can be added in the connection URL, or we can simply call the getString method here
             */
            recordValue = new DbRecordValue( dbColumn, res.getString( columnIndex ) );
        } else {
            recordValue = new DbRecordValue( dbColumn, res.getObject( columnIndex ) );
        }

        return recordValue;
    }

    /**
     * Currently handling the case where a system table is returned, we do not want such table.
     * In such case the TABLE_SCEM is 'sys', but the regular tables have the DB name instead.
     */
    @Override
    protected boolean isTableAccepted( ResultSet tableResultSet, String dbName, String tableName ) {

        try {
            String tableSchema = tableResultSet.getString( "TABLE_SCHEM" );
            if( !StringUtils.isNullOrEmpty( tableSchema ) && "sys".equalsIgnoreCase( tableSchema ) ) {
                log.debug( "Table '" + tableName + "' is skipped because its JDBC TABLE_SCHEM property is '"
                           + tableSchema + "'" );
                return false;
            }
        } catch( SQLException e ) {
            log.warn( "Unable to assess if table '" + tableName + "' should be processed or skipped", e );
        }

        return true;
    }
    
    @Override
    protected Map<String, String> extractTableIndexes( String tableName, DatabaseMetaData databaseMetaData,
                                                       String catalog ) throws DbException {

        StringBuilder sql = new StringBuilder();
        sql.append( "SELECT" );
        sql.append( " indexes.name as Name," );
        sql.append( " indexes.type_desc as Type," );
        sql.append( " ds.name as DataSpaceName," );
        sql.append( " ds.type as DataSpaceType," );
        sql.append( " indexes.is_primary_key as IsPrimaryKey," );
        sql.append( " indexes.is_unique as IsUnique," );
        sql.append( " indexes.ignore_dup_key as IsDuplicateKey," );
        sql.append( " indexes.is_unique_constraint as IsUniqueConstraint" );
        sql.append( " FROM" );
        sql.append( " sys.indexes as indexes" );
        sql.append( " JOIN" );
        sql.append( " sys.data_spaces ds on ds.data_space_id = indexes.data_space_id" );
        sql.append( " WHERE object_id = (select object_id from sys.objects where name = '" + tableName
                    + "');" );

        String indexName = null;
        Map<String, String> indexes = new HashMap<>();
        for( DbRecordValuesList valueList : select( sql.toString() ) ) {
            StringBuilder info = new StringBuilder();
            for( DbRecordValue dbValue : valueList ) {
                String value = dbValue.getValueAsString();
                String name = dbValue.getDbColumn().getColumnName();
                if( "Name".equalsIgnoreCase( name ) ) {
                    indexName = value;
                } else {
                    info.append( ", " + name + "=" + value );
                }
            }

            if( indexName == null ) {
                indexName = "NULL_NAME_FOUND_FOR_INDEX_OF_TABLE_" + tableName;
                log.warn( "IndexName column not found in query polling for index properties:\nQuery: "
                          + sql.toString() + "\nQuery result: " + valueList.toString()
                          + "\nWe will use the following as an index name: " + indexName );
            }

            indexes.put( indexName, info.toString() );
        }

        return indexes;
    }
}
