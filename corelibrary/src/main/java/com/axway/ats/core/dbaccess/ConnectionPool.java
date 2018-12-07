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

import java.sql.Connection;
import java.util.HashMap;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.log4j.Logger;

import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.log.AtsConsoleLogger;


public class ConnectionPool {

    private static Logger                      log;

    private static final AtsConsoleLogger      atsLog                            = new AtsConsoleLogger(ConnectionPool.class);

    /**
     * The number of times ATS will retry to open a connection when a network exception occurred
     * */
    public static final int                    DEFAULT_CONNECTION_TIMEOUT_RETRY  = 5;

    /**
     * The interval (in seconds) between each retry for a failed connection
     * */
    public static final int                    DEFAULT_CONNECTION_RETRY_INTERVAL = 10;

    /**
     * we keep a static list of connections in order to reuse them when we have multiple calls for creating the same
     * connection
     */
    private static HashMap<String, DataSource> dataSourceMap                     = new HashMap<String, DataSource>();

    //prevent instantiation
    private ConnectionPool() {

    }

    /**
     * Get JDBC connection - already existing or create a new one if needed.
     * Searches the hash map if this connection already exists and returns it or it creates a new connection,
     * adds it to the map and returns it.
     *
     * Note: Each connection is identified by "connection string" identifying particular DB/schema instance
     *
     * @param dbConnection The connection descriptor
     * @return a JDBC Connection
     * @throws DbException on error
     */
    public static synchronized Connection getConnection(
                                                         DbConnection dbConnection ) throws DbException {

        // create the connection identifier
        String connectionDescription = dbConnection.getConnHash();
        DataSource dataSource;
        Integer connectionTimeoutRetry = AtsSystemProperties.getPropertyAsNumber(DbKeys.CONNECTION_RETRY_TIMEOUT);
        if (connectionTimeoutRetry == null) {
            connectionTimeoutRetry = DEFAULT_CONNECTION_TIMEOUT_RETRY;
        }
        Integer connectionRetryInterval = AtsSystemProperties.getPropertyAsNumber(DbKeys.CONNECTION_RETRY_INTERVAL);
        if (connectionRetryInterval == null) {
            connectionRetryInterval = DEFAULT_CONNECTION_RETRY_INTERVAL;
        }
        int connectionRetriesCount = 0;
        do {
            if (dataSourceMap.containsKey(connectionDescription)) {
                // use the cached connection
                dataSource = dataSourceMap.get(connectionDescription);
            } else {
                dataSource = dbConnection.getDataSource();
                dataSourceMap.put(connectionDescription, dataSource);
            }

            try {
                Connection newConnection;
                if (dataSource instanceof BasicDataSource) {
                    // DBCP BasicDataSource does not support getConnection(user,pass) method
                    newConnection = dataSource.getConnection();
                } else {
                    newConnection = dataSource.getConnection(dbConnection.getUser(),
                                                             dbConnection.getPassword());
                }
                if (connectionRetriesCount > 0) {
                    atsLog.info("DB connection to '" + dbConnection.getURL()
                                + "' obtained. Network connectivity issue resolved.");
                }
                return newConnection;

            } catch (Exception e) {
                // check if the exception is network unreachable or connection/socket timeout
                connectionRetriesCount++;
                atsLog.warn("Could not obtain DB connection to '" + dbConnection.getURL() + "'. "
                            + "Reason: '" + e.getMessage() + "'. Retries left "
                            + (connectionTimeoutRetry - connectionRetriesCount));
                try {
                    Thread.sleep(connectionRetryInterval * 1000);
                } catch (InterruptedException ie) {
                    // do nothing
                }
                if (connectionRetriesCount >= connectionTimeoutRetry) {
                    throw new DbException("Unable to connect to database using location '" + dbConnection.getURL()
                                          + "' and user '" + dbConnection.getUser() + "'", e);
                }
            }
        } while (connectionRetriesCount <= connectionTimeoutRetry);

        return null; // or some exception saying that we can not obtain connection
    }

    /**
     * Search the hash map for this connection and remove it before it is disconnected
     *
     * @param dbConnection The connection descriptor
     */
    public static synchronized void removeConnection(
                                                      DbConnection dbConnection ) throws DbException {

        if (dataSourceMap.containsKey(dbConnection.getConnHash())) {
            dataSourceMap.remove(dbConnection.getConnHash());
        } else {
            log = Logger.getLogger(ConnectionPool.class);

            log.info("Cannot remove the connection " + dbConnection.hashCode()
                     + " from the pool, as it is not present in there");

        }
    }
}
