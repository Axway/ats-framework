/*
 * Copyright 2017-2020 Axway Software
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

import java.sql.Driver;
import java.util.Map;

import javax.sql.DataSource;

/**
 * Base class for all database connections
 */
public abstract class DbConnection {

    /**
     * The type of the database
     */
    protected String              dbType;

    //required attributes
    protected String              host;
    protected String              db;
    protected int                 port;
    protected String              user;
    protected String              password;

    protected Map<String, Object> customProperties;

    /**
     * Constructor
     * 
     * @param dbType the database type
     * @param host database host
     * @param db database name
     * @param user database user
     * @param password database user-password
     * @param customProperties list of custom properties
     */
    protected DbConnection( String dbType,
                            String host,
                            String db,
                            String user,
                            String password,
                            Map<String, Object> customProperties ) {

        this(dbType, host, -1, db, user, password, customProperties);
    }

    /**
     * Constructor
     * 
     * @param dbType the database type
     * @param host database host
     * @param port the database port
     * @param db database name
     * @param user database user
     * @param password database user-password
     * @param customProperties list of custom properties
     */
    protected DbConnection( String dbType,
                            String host,
                            int port,
                            String db,
                            String user,
                            String password,
                            Map<String, Object> customProperties ) {

        this.dbType = dbType;
        this.host = host;
        this.port = port;
        this.db = db;
        this.user = user;
        this.password = password;

        this.customProperties = customProperties;
        initializeCustomProperties(customProperties);
    }

    /**
     * Get the type of the database provider this connection describes
     * 
     * @return the provider type
     */
    public String getDbType() {

        return dbType;
    }

    /**
     * Get the connection host
     * 
     * @return the connection host
     */
    public String getHost() {

        return host;
    }
    
    /**
     * Get the connection port<br>
     * 
     * @return the connection port
     */
    public int getPort() {
        
        return port;
    }

    /**
     * Get the connection database
     * 
     * @return the connection DB
     */
    public String getDb() {

        return db;
    }

    /**
     * Get the connection user
     * 
     * @return db user
     */
    public String getUser() {

        return this.user;
    }

    /**
     * Get the connection password
     * 
     * @return db password
     */
    public String getPassword() {

        return this.password;
    }

    public Map<String, Object> getCustomProperties() {

        return this.customProperties;
    }

    /**
     * Initialize the connection descriptor custom properties - this method
     * will be called by the constructor if properties are supplied
     * 
     * @param customProperties map of additional properties
     */
    protected abstract void initializeCustomProperties(
                                                        Map<String, Object> customProperties );

    /**
     * Get the description of this database connection
     * 
     * @return the connection description
     */
    public abstract String getDescription();

    /**
     * Get the connection hash
     * 
     * @return the connection hash - it is based on the host port and database
     */
    public String getConnHash() {

        StringBuilder connHash = new StringBuilder();
        connHash.append(host);
        connHash.append("_");
        connHash.append(port);
        connHash.append("_");
        connHash.append(db);
        connHash.append("_");
        connHash.append(user);
        if (customProperties != null) {
            connHash.append("_");
            connHash.append(customProperties.hashCode());
        }

        return connHash.toString();
    }

    /**
     * Get a new DataSource from this connection.
     * <p><em>Note</em> that each call returns new DataSource instance.
     * It is recommended to use {@link ConnectionPool#getConnection(DbConnection)} which keeps cache of
     * existing DataSources based on connection hash of the provided {@link DbConnection} descriptor.</p>
     * @return new {@link DataSource} instance based on current instance parameters and credentials.
     */
    public abstract DataSource getDataSource();

    /**
     * Get the class of the driver which will be used for this connection
     * 
     * @return the driver class
     */
    public abstract Class<? extends Driver> getDriverClass();

    /**
     * Get the URL which corresponds to this connection
     * 
     * @return the connection URL
     */
    public abstract String getURL();

    /**
     *  Closes and releases all idle connections that are currently stored
     *  in the connection pool associated with this data source.
     */
    public abstract void disconnect();

}
