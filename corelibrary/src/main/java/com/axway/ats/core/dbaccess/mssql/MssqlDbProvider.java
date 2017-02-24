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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.axway.ats.common.dbaccess.snapshot.TableDescription;
import com.axway.ats.core.dbaccess.AbstractDbProvider;
import com.axway.ats.core.dbaccess.ConnectionPool;
import com.axway.ats.core.dbaccess.DatabaseProviderFactory;
import com.axway.ats.core.dbaccess.DbColumn;
import com.axway.ats.core.dbaccess.DbProvider;
import com.axway.ats.core.dbaccess.DbRecordValue;
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

    //////////////////////////////////////////////////////////////
    // Utility methods for DB usage
    //////////////////////////////////////////////////////////////

    //********************************************************************************************
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

        try {
            PreparedStatement stmnt = connection.prepareStatement( sQuery );
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
            rs.close();
            stmnt.close();

        } catch( SQLException ex ) {
            log.error( ExceptionUtils.getExceptionMsg( ex ) );
        } finally {
            DbUtils.closeResultSet( rs );
            try {
                connection.close();
            } catch( SQLException sqle ) {
                log.error( ExceptionUtils.getExceptionMsg( sqle ) );
            }
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
            if( !StringUtils.isNullOrEmpty( tableSchema ) && !tableSchema.equalsIgnoreCase( dbName ) ) {
                log.debug( "Table '" + tableName
                           + "' is skipped because its JDBC TABLE_SCHEM property is not the expected '"
                           + dbName + "', but is '" + tableSchema + "'" );
                return false;
            }
        } catch( SQLException e ) {
            log.warn( "Unable to assess if table '" + tableName + "' should be processed or skipped", e );
        }

        return true;
    }
}
