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

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.Connection;
import java.util.HashMap;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.log4j.Logger;

import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.dbaccess.mssql.DbConnSQLServer;
import com.axway.ats.core.log.AtsConsoleLogger;
import com.axway.ats.core.utils.ExceptionUtils;

public class ConnectionPool {

    private static final Logger                log                              = Logger.getLogger(ConnectionPool.class);

    private static final AtsConsoleLogger      atsLog                           = new AtsConsoleLogger(ConnectionPool.class);

    /**
     * The default number of times ATS will retry to open a connection when a network exception occurred
     * */
    public static final int                    DEFAULT_CONNECTION_RETRY_COUNT   = 5;

    /**
     * The default interval (in seconds) between each retry for a failed connection
     * */
    public static final int                    DEFAULT_CONNECTION_RETRY_TIMEOUT = 10;

    /**
     * we keep a static list of connections in order to reuse them when we have multiple calls for creating the same
     * connection
     */
    private static HashMap<String, DataSource> dataSourceMap                    = new HashMap<String, DataSource>();

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
        Integer connectionRetryCount = AtsSystemProperties.getPropertyAsNumber(DbKeys.CONNECTION_RETRY_COUNT);
        if (connectionRetryCount == null) {
            connectionRetryCount = DEFAULT_CONNECTION_RETRY_COUNT;
        }
        Integer connectionRetryTimeout = AtsSystemProperties.getPropertyAsNumber(DbKeys.CONNECTION_RETRY_TIMEOUT);
        if (connectionRetryTimeout == null) {
            connectionRetryTimeout = DEFAULT_CONNECTION_RETRY_TIMEOUT;
        }
        int retries = 0;
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
                if (retries > 0) {
                    if (Logger.getRootLogger().getAppender("com.axway.ats.log.appenders.ActiveDbAppender") != null
                        || Logger.getRootLogger()
                                 .getAppender("com.axway.ats.log.appenders.PassiveDbAppender") != null) {
                        log.info("DB connection to '" + dbConnection.getURL()
                                 + "' obtained. Network connectivity issue resolved.");
                    } else {
                        atsLog.info("DB connection to '" + dbConnection.getURL()
                                    + "' obtained. Network connectivity issue resolved.");
                    }

                }
                return newConnection;

            } catch (Exception e) {
                // check if the exception is network unreachable or connection/socket timeout
                retries++;
                if (Logger.getRootLogger().getAppender("com.axway.ats.log.appenders.ActiveDbAppender") != null
                    || Logger.getRootLogger()
                             .getAppender("com.axway.ats.log.appenders.PassiveDbAppender") != null) {
                    log.warn("Could not obtain DB connection to '" + dbConnection.getURL() + "'. "
                             + "\nConnection Status: \n" + getDbConnectionStatus(dbConnection, e)
                             + "\nRetries left ("
                             + (connectionRetryCount - retries) + ") .");
                } else {
                    atsLog.warn("Could not obtain DB connection to '" + dbConnection.getURL() + "'. "
                                + "\nConnection Status: \n" + getDbConnectionStatus(dbConnection, e)
                                + "\nRetries left ("
                                + (connectionRetryCount - retries) + ") .");
                }

                if (retries >= connectionRetryCount) {
                    throw new DbException("Unable to connect to database using location '" + dbConnection.getURL()
                                          + "' and user '" + dbConnection.getUser() + "'", e);
                } else {
                    // more retries ahead, sleep for a bit
                    try {
                        Thread.sleep(connectionRetryTimeout * 1000);
                    } catch (InterruptedException ie) {
                        // do nothing
                    }
                }
            }
        } while (retries <= connectionRetryCount);

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

            if (Logger.getRootLogger().getAppender("com.axway.ats.log.appenders.ActiveDbAppender") != null
                || Logger.getRootLogger()
                         .getAppender("com.axway.ats.log.appenders.PassiveDbAppender") != null) {
                log.info("Cannot remove the connection " + dbConnection.hashCode()
                         + " from the pool, as it is not present in there");
            } else {
                atsLog.info("Cannot remove the connection " + dbConnection.hashCode()
                            + " from the pool, as it is not present in there");
            }

        }
    }

    private static String getDbConnectionStatus( DbConnection dbConnection, Exception exception ) {

        Boolean dbHostReachable = null;
        Boolean dbServerListening = null;

        dbHostReachable = isDbHostReachable(dbConnection);
        if (dbHostReachable) {
            dbServerListening = isDbServerListening(dbConnection);
        }

        StringBuilder sb = new StringBuilder();

        sb.append("\tHost reachable:      " + getStatusAsString(dbHostReachable) + ",\n");

        sb.append("\tDb Server listening: " + getStatusAsString(dbServerListening) + ",\n");

        if (dbConnection.getDbType().equals(DbConnSQLServer.DATABASE_TYPE)) {
            if (ExceptionUtils.containsMessage("The login failed", exception)) {
                sb.append("\tLogin successful:    " + getStatusAsString(false) + "\n");
            } else {
                sb.append("\tLogin successful:    " + getStatusAsString(null) + "\n");
            }
        }

        return sb.toString();
    }

    private static String getStatusAsString( Boolean bool ) {

        if (bool == null) {
            return "N/A";
        }
        if (bool) {
            return "YES";
        } else {
            return "NO";
        }
    }

    private static boolean isDbHostReachable( DbConnection dbConnection ) {

        try {
            return InetAddress.getByName(dbConnection.getHost())
                              .isReachable(AtsSystemProperties.getPropertyAsNumber(DbKeys.CONNECTION_TIMEOUT,
                                                                                   DbConnection.DEFAULT_TIMEOUT));
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isDbServerListening( DbConnection dbConnection ) {

        Socket so = null;
        try {
            so = new Socket(dbConnection.getHost(), dbConnection.getPort());
            so.setSoTimeout(AtsSystemProperties.getPropertyAsNumber(DbKeys.CONNECTION_TIMEOUT,
                                                                    DbConnection.DEFAULT_TIMEOUT)
                            * 1000);
            so.getOutputStream().write(0);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (so != null && !so.isClosed()) {
                try {
                    so.close();
                } catch (IOException e) {
                    // do nothing
                }
            }
        }
    }
}
