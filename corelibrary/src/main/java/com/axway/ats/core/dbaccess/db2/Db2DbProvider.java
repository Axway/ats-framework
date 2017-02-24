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
package com.axway.ats.core.dbaccess.db2;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.axway.ats.core.dbaccess.AbstractDbProvider;
import com.axway.ats.core.utils.IoUtils;

/**
 * DB2 implementation of the DB provider
 */
public class Db2DbProvider extends AbstractDbProvider {

    private static final Logger log = Logger.getLogger( Db2DbProvider.class );

    /**
     * Constructor to create authenticated connection to a database.
     * Takes DbConnection object
     * 
     * @param dbconn db-connection object
     */
    public Db2DbProvider( DbConnDb2 dbConnection ) {

        super( dbConnection );
    }

    @Override
    protected String getResultAsEscapedString(
                                               ResultSet resultSet,
                                               int index,
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
}
