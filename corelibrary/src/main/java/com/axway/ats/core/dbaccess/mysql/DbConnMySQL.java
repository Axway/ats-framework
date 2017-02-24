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
package com.axway.ats.core.dbaccess.mysql;

import java.sql.Driver;
import java.util.Map;

import javax.sql.DataSource;

import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.core.dbaccess.DbConnection;
import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

/**
 * Connection descriptor for MySQL databases
 */
public class DbConnMySQL extends DbConnection {

    /**
     * Default DB port
     */
    private static final int    DEFAULT_PORT      = 3306;

    /**
     * The JDBC MySQL prefix string
     */
    private static final String JDBC_MYSQL_PREFIX = "jdbc:mysql://";

    /**
     * The port to be used for connecting to the database
     */
    private int                 port;

    /**
     * The connection URL
     */
    private String              url;
    
    public static final String DATABASE_TYPE = "MYSQL";

    /**
     * Constructor
     *
     * @param host host
     * @param db database name
     * @param user login user name
     * @param password login password
     */
    public DbConnMySQL( String host, String db, String user, String password ) {

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
    public DbConnMySQL( String host, String db, String user, String password,
                        Map<String, Object> customProperties ) {

        super( DATABASE_TYPE, host, db, user, password, customProperties );

        url = new StringBuilder().append( JDBC_MYSQL_PREFIX )
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

        MysqlConnectionPoolDataSource dataSource = new MysqlConnectionPoolDataSource();// do not use connection pool
        dataSource.setServerName( this.host );
        dataSource.setPort( this.port );
        dataSource.setDatabaseName( this.db );
        dataSource.setUser( this.user );
        dataSource.setPassword( this.password );
        dataSource.setAllowMultiQueries( true );

        return dataSource;
    }

    @Override
    public Class<? extends Driver> getDriverClass() {

        return com.mysql.jdbc.Driver.class;
    }

    @Override
    public String getURL() {

        return url;
    }

    @Override
    public String getConnHash() {

        StringBuilder connHash = new StringBuilder();
        connHash.append( host );
        connHash.append( port );
        connHash.append( db );

        return connHash.toString();
    }

    @Override
    public String getDescription() {

        StringBuilder description = new StringBuilder( "MySQL connection to " );
        description.append( host );
        description.append( ":" ).append( port );
        description.append( "/" ).append( db );

        return description.toString();
    }

    @Override
    public void disconnect() {

        //data source is not cached so there is nothing to be closed
    }
}
