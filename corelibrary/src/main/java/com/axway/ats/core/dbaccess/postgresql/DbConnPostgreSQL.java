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

import java.io.PrintWriter;
import java.sql.Driver;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;

import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.log.AtsConsoleLogger;
import com.axway.ats.core.utils.StringUtils;

public class DbConnPostgreSQL extends DbConnection {

    public static final String            DATABASE_TYPE          = "POSTGRESQL";

    /**
     * Default DB port
     */
    public static final int               DEFAULT_PORT           = 5432;

    private static final AtsConsoleLogger log                    = new AtsConsoleLogger(DbConnPostgreSQL.class);

    /**
     * The JDBC PostgreSQL prefix string
     */
    private static final String           JDBC_POSTGRESQL_PREFIX = "jdbc:postgresql://";

    private BasicDataSource               ds;

    /**
     * The connection URL
     */
    private String                        url;

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

        this(host, DEFAULT_PORT, db, user, password, customProperties);

    }

    /**
     * Constructor
     *
     * @param host host
     * @param port the database port
     * @param db database name
     * @param user login user name
     * @param password login password
     * @param customProperties map of custom properties
     */
    public DbConnPostgreSQL( String host, int port, String db, String user, String password,
                             Map<String, Object> customProperties ) {

        super(DATABASE_TYPE, host, port, db, user, password, customProperties);

        url = new StringBuilder().append(JDBC_POSTGRESQL_PREFIX)
                                 .append(host)
                                 .append(":")
                                 // because the port can be changed after execution of the parent constructor, 
                                 // use this.port, instead of port
                                 .append(this.port)
                                 .append("/")
                                 .append(db)
                                 .toString();
    }

    @Override
    protected void initializeCustomProperties( Map<String, Object> customProperties ) {

        if (customProperties != null && !customProperties.isEmpty()) {
            //read the port if such is set
            Object portValue = customProperties.get(DbKeys.PORT_KEY);
            if (portValue != null) {
                if (this.port != -1 && this.port != DEFAULT_PORT) {
                    log.warn("New port value found in custom properties. Old value will be overridden");
                }
                this.port = (Integer) portValue;
            }
        }

        if (this.port < 1) {
            this.port = DEFAULT_PORT;
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

    /**
     * NOTE: This method must not use log4j2 for logging as this may cause locking issues
     */
    @Override
    public DataSource getDataSource() {

        // PostgreSQL does not provide connection pool (as of version 42.1.3) so make one using Apache Commons DBCP 
        ds = new BasicDataSource();

        // max number of active connections
        Integer maxTotal = AtsSystemProperties.getPropertyAsNumber("dbcp.maxTotal");
        if (maxTotal == null) {
            maxTotal = 8;
        } else {
            log.info("Max number of active connections is "
                     + maxTotal);
        }
        ds.setMaxTotal(maxTotal);

        // wait time for new connection
        Integer maxWaitMillis = AtsSystemProperties.getPropertyAsNumber("dbcp.maxWaitMillis");
        if (maxWaitMillis == null) {
            maxWaitMillis = 60 * 1000;
        } else {
            log.info("Connection creation wait is "
                     + maxWaitMillis
                     + " msec");
        }
        ds.setMaxWaitMillis(maxWaitMillis);

        String logAbandoned = System.getProperty("dbcp.logAbandoned");
        if (logAbandoned != null && ("true".equalsIgnoreCase(logAbandoned))
            || "1".equalsIgnoreCase(logAbandoned)) {
            String removeAbandonedTimeoutString = System.getProperty("dbcp.removeAbandonedTimeout");
            int removeAbandonedTimeout = (int) ds.getMaxWaitMillis() / (2 * 1000);
            if (!StringUtils.isNullOrEmpty(removeAbandonedTimeoutString)) {
                removeAbandonedTimeout = Integer.parseInt(removeAbandonedTimeoutString);
            }
            log.info(
                     "Will log and remove abandoned connections if not cleaned in "
                     + removeAbandonedTimeout
                     + " sec");
            // log not closed connections
            ds.setLogAbandoned(true); // issue stack trace of not closed connection
            ds.setAbandonedUsageTracking(true);
            ds.setLogExpiredConnections(true);
            ds.setRemoveAbandonedTimeout(removeAbandonedTimeout);
            ds.setRemoveAbandonedOnBorrow(true);
            ds.setRemoveAbandonedOnMaintenance(true);
            ds.setAbandonedLogWriter(new PrintWriter(System.err));
        }
        ds.setValidationQuery("SELECT 1");
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

        if( ds != null ) {
            try {
                ds.close();
            } catch (Exception e) {
                throw new DbException("Unable to close database source", e);
            }
        }
    }
}
