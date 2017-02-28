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

import java.sql.Driver;
import java.util.Map;

import javax.sql.DataSource;

import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.core.dbaccess.DbConnection;
import com.ibm.db2.jcc.DB2SimpleDataSource;

/**
 * Connection descriptor for DB2 databases 
 */
public class DbConnDb2 extends DbConnection {

    /**
     * Default DB port
     */
    private static final int    DEFAULT_PORT    = 50000;

    /**
     * The JDBC DB2 prefix string
     */
    private static final String JDBC_DB2_PREFIX = "jdbc:db2://";

    /**
     * The connection URL
     */
    private String              url;
    
    public static final String DATABASE_TYPE = "DB2";

    /**
     * Constructor
     * 
     * @param host host
     * @param db database name
     * @param user login user name
     * @param password login password
     */
    public DbConnDb2( String host, String db, String user, String password ) {

        this( host, db, user, password, null );
    }

    /**
     * Constructor
     * 
     * @param host host
     * @param db database name
     * @param user login user name
     * @param password login password
     * @param customProperties map of custom properties
     */
    public DbConnDb2( String host, String db, String user, String password,
                      Map<String, Object> customProperties ) {

        super( DATABASE_TYPE, host, db, user, password, customProperties );

        url = new StringBuilder().append( JDBC_DB2_PREFIX )
                                 .append( host )
                                 .append( ":" )
                                 .append( port )
                                 .append( "/" )
                                 .append( db )
                                 .toString();
    }

    @Override
    protected void initializeCustomProperties( Map<String, Object> customProperties ) {

        this.port = DEFAULT_PORT;

        if( customProperties != null && !customProperties.isEmpty() ) {
            //read the port if such is set
            Object portValue = customProperties.get( DbKeys.PORT_KEY );
            if( portValue != null ) {
                this.port = ( Integer ) portValue;
            }
        }
    }

    @Override
    public DataSource getDataSource() {

        DB2SimpleDataSource dataSource = new DB2SimpleDataSource();
        dataSource.setServerName( this.host );
        dataSource.setPortNumber( this.port );
        dataSource.setDatabaseName( this.db );
        dataSource.setUser( this.user );
        dataSource.setPassword( this.password );

        /*
         * We should use driver type 4 as it is all implemented in the java layer, no natives:
         * http://www.ibm.com/developerworks/data/library/techarticle/dm-0512kokkat/
         */
        dataSource.setDriverType( 4 );

        return dataSource;
    }

    @Override
    public Class<? extends Driver> getDriverClass() {

        return com.ibm.db2.jcc.DB2Driver.class;
    }

    @Override
    public String getURL() {

        return url;
    }

    @Override
    public String getDescription() {

        StringBuilder description = new StringBuilder( "DB2 connection to " );
        description.append( host );
        description.append( ":" ).append( port );
        description.append( "/" ).append( db );

        return description.toString();
    }

    @Override
    public void disconnect() {

        throw new RuntimeException( "Not implemented" );
    }
}
