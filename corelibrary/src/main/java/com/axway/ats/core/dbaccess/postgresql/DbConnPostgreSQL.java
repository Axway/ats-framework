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
package com.axway.ats.core.dbaccess.postgresql;

import java.sql.Driver;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;

import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.exceptions.DbException;

public class DbConnPostgreSQL extends DbConnection {

    private static final Logger log                    = Logger.getLogger(DbConnPostgreSQL.class);

    public static final String  DATABASE_TYPE          = "PostgreSQL";

    /**
     * Default DB port
     */
    public static final int     DEFAULT_PORT           = 5432;

    /**
     * The JDBC PostgreSQL prefix string
     */
    private static final String JDBC_POSTGRESQL_PREFIX = "jdbc:postgresql://";

    private BasicDataSource     ds;

    /**
     * The connection URL
     */
    private String              url;

    /**
     * Constructor
     *
     * @param host host
     * @param db database name
     * @param user login user name
     * @param password login password
     */
    public DbConnPostgreSQL( String host, String db, String user, String password ) {

        this(host, db, user, password, null);
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
    public DbConnPostgreSQL( String host, String db, String user, String password,
                             Map<String, Object> customProperties ) {

        super(DATABASE_TYPE, host, db, user, password, customProperties);

        url = new StringBuilder().append(JDBC_POSTGRESQL_PREFIX)
                                 .append(host)
                                 .append(":")
                                 .append(port)
                                 .append("/")
                                 .append(db)
                                 .toString();
    }

    @Override
    protected void initializeCustomProperties( Map<String, Object> customProperties ) {

        this.port = DEFAULT_PORT;

        if (customProperties != null && !customProperties.isEmpty()) {
            //read the port if such is set
            Object portValue = customProperties.get(DbKeys.PORT_KEY);
            if (portValue != null) {
                this.port = (Integer) portValue;
            }
        }

    }

    @Override
    public String getDescription() {

        StringBuilder description = new StringBuilder("PostgreSQL connection to ");
        description.append(host);
        description.append(":").append(port);
        description.append("/").append(db);

        return description.toString();
    }

    @Override
    public DataSource getDataSource() {

        // PostgreSQL does not provide connection pool (as of version 42.1.3) so make one using Apache Commons DBCP 
        ds = new BasicDataSource();
        ds.setMaxWait(60 * 1000); // wait 60 sec for new connection
        //ds.setMaxActive( -1 );
        //ds.setMaxIdle( 1000 );

        String logAbandoned = System.getProperty("dbcp.logAbandoned");
        if (logAbandoned != null && ("true".equalsIgnoreCase(logAbandoned))
            || "1".equalsIgnoreCase(logAbandoned)) {
            log.info("Will log abandoned connections if not cleaned in 120 sec");
            // log not closed connections
            ds.setRemoveAbandoned(true);
            ds.setLogAbandoned(true); // issue stack trace of not closed connection
            ds.setRemoveAbandonedTimeout(120); // 120 sec - 2 min
        }

        ds.setDriverClassName(getDriverClass().getName());
        ds.setUsername(user);
        ds.setPassword(password);
        ds.setUrl(getURL());
        return ds;
    }

    @Override
    public Class<? extends Driver> getDriverClass() {

        return org.postgresql.Driver.class;
    }

    @Override
    public String getURL() {

        return url;
    }

    @Override
    public void disconnect() {

        try {
            ds.close();
        } catch (Exception e) {
            throw new DbException("Unable to close database source", e);
        }

    }

}
