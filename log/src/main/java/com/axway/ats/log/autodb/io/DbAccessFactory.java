/*
 * Copyright 2017-2022 Axway Software
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
package com.axway.ats.log.autodb.io;

import java.util.HashMap;
import java.util.Map;

import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.DbUtils;
import com.axway.ats.core.dbaccess.mssql.DbConnSQLServer;
import com.axway.ats.core.dbaccess.postgresql.DbConnPostgreSQL;
import com.axway.ats.core.threads.ThreadsPerCaller;
import com.axway.ats.log.appenders.AbstractDbAppender;
import com.axway.ats.log.appenders.ActiveDbAppender;
import com.axway.ats.log.appenders.PassiveDbAppender;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import org.apache.log4j.Logger;

public class DbAccessFactory {

    private static final Logger log = Logger.getLogger(DbAccessFactory.class);

    public DbAccessFactory() {
    }

    /**
     * Retrieves the DB info from the log4j system and then creates a new
     * instance for writing into the DB
     * 
     * @return
     * @throws DatabaseAccessException
     */
    public SQLServerDbWriteAccess getNewDbWriteAccessObject() throws DatabaseAccessException {

        // Our DB appender keeps the DB connection info
        ActiveDbAppender loggingAppender = ActiveDbAppender.getCurrentInstance();
        String availableDbType = null;
        if (loggingAppender == null) {
            throw new DatabaseAccessException("Unable to initialize connection to the logging database as the ATS ActiveDbAppender is not attached to log4j system");
        }

        if (String.valueOf(DbConnSQLServer.DEFAULT_PORT).equals(loggingAppender.getAppenderConfig().getPort())) {

            checkIfMssqlAvailable(loggingAppender);
            availableDbType = DbConnSQLServer.DATABASE_TYPE;
        } else if (String.valueOf(DbConnPostgreSQL.DEFAULT_PORT)
                         .equals(loggingAppender.getAppenderConfig().getPort())) {

            checkIfPgsqlAvailable(loggingAppender);
            availableDbType = DbConnPostgreSQL.DATABASE_TYPE;
        } else {
            Throwable mssqlException = null;
            Throwable pgsqlException = null;

            try {
                checkIfMssqlAvailable(loggingAppender);
                availableDbType = DbConnSQLServer.DATABASE_TYPE;
            } catch (Exception e) {
                mssqlException = e;
            }

            try {
                checkIfPgsqlAvailable(loggingAppender);
                availableDbType = DbConnPostgreSQL.DATABASE_TYPE;
            } catch (Exception e) {
                pgsqlException = e;
            }

            if (mssqlException != null && pgsqlException != null) {
                log.error(mssqlException); // TODO - switch to AtsConsoleLogger
                log.error(pgsqlException);
                String errMsg = "Neither MSSQL, nor PostgreSQL server at '"
                                + loggingAppender.getHost() + ":" + loggingAppender.getPort() +
                                "' has database with name '" + loggingAppender.getDatabase()
                                + "'. Exception for MSSQL is : \n\t" + mssqlException
                                + "\n\nException for PostgreSQL is: \n\t" + pgsqlException;
                throw new DatabaseAccessException(errMsg);
            }
        }
        
        SQLServerDbWriteAccess writeAccess = null;
        if (availableDbType.equals(DbConnSQLServer.DATABASE_TYPE)) {

            // Create the database access layer based on log4j appender properties
            if (DbKeys.SQL_SERVER_DRIVER_MICROSOFT.equalsIgnoreCase(loggingAppender.getAppenderConfig().getDriver())) {
                // Create DB connection based on the log4j system settings
                Map<String, Object> props = new HashMap<>();
                props.put(DbKeys.DRIVER, DbKeys.SQL_SERVER_DRIVER_MICROSOFT);
                DbConnection dbConnection = new DbConnSQLServer(loggingAppender.getHost(),
                                                   Integer.parseInt(loggingAppender.getPort()),
                                                   loggingAppender.getDatabase(),
                                                   loggingAppender.getUser(),
                                                   loggingAppender.getPassword(), props);
                writeAccess = new SQLServerDbWriteAccessMSSQL(dbConnection, false);
                writeAccess.setMaxNumberOfCachedEvents(Integer.parseInt(loggingAppender.getAppenderConfig()
                                                                                       .getChunkSize()));
                return writeAccess;
            } else if (DbKeys.SQL_SERVER_DRIVER_JTDS.equalsIgnoreCase(loggingAppender.getAppenderConfig()
                                                                                     .getDriver())) {
                // Create DB connection based on the log4j system settings
                Map<String, Object> props = new HashMap<>();
                props.put(DbKeys.DRIVER, DbKeys.SQL_SERVER_DRIVER_JTDS);
                DbConnection dbConnection = new DbConnSQLServer(loggingAppender.getHost(),
                                                   Integer.parseInt(loggingAppender.getPort()),
                                                   loggingAppender.getDatabase(),
                                                   loggingAppender.getUser(),
                                                   loggingAppender.getPassword(), props);
                writeAccess = new SQLServerDbWriteAccess(dbConnection, false);
                writeAccess.setMaxNumberOfCachedEvents(Integer.parseInt(loggingAppender.getAppenderConfig()
                                                                                       .getChunkSize()));
                return writeAccess;
            } else {
                throw new IllegalArgumentException("Appender configuration specified SQL Server driver to be '"
                                                   + loggingAppender.getAppenderConfig().getDriver()
                                                   + "' which is not supported");
            }

        } else if (availableDbType.equals(DbConnPostgreSQL.DATABASE_TYPE)) {
            /*Exception pgsqlException = DbUtils.isPostgreSQLDatabaseAvailable(loggingAppender.getHost(),
                                                                             Integer.parseInt(loggingAppender.getPort()),
                                                                             loggingAppender.getDatabase(),
                                                                             loggingAppender.getUser(),
                                                                             loggingAppender.getPassword());
            if (pgsqlException == null) { */
                // Create DB connection based on the log4j system settings
                DbConnection dbConnection = new DbConnPostgreSQL(loggingAppender.getHost(),
                                                    Integer.parseInt(loggingAppender.getPort()),
                                                    loggingAppender.getDatabase(),
                                                    loggingAppender.getUser(),
                                                    loggingAppender.getPassword(), null);

                // Create the database access layer
                writeAccess = new PGDbWriteAccess(dbConnection, false);
                writeAccess.setMaxNumberOfCachedEvents(Integer.parseInt(loggingAppender.getAppenderConfig()
                                                                                       .getChunkSize()));
                return writeAccess;
        } else {
            // shouldn't happen - unsupported DB type, w/o exceptions for MsSQL and PGSQL
            throw new UnsupportedOperationException("Could not use database '" + availableDbType + "' to create write access object");
        }

    }

    /**
     * Retrieves the DB info from the log4j system and then creates a new
     * instance for writing into the DB.
     * <p>Invoked on ATS Agent side.</p>
     * This method differs from the getNewDbWriteAccessObject(), 
     * because we use the PassiveDbAppender to retrieve the DB info
     * 
     * @return DbWriteAccess Object
     * @throws DatabaseAccessException
     */
    public SQLServerDbWriteAccess getNewDbWriteAccessObjectViaPassiveDbAppender( ) throws DatabaseAccessException {

        // Our DB appender keeps the DB connection info
        PassiveDbAppender loggingAppender = PassiveDbAppender.getCurrentInstance();
        if (loggingAppender == null) {
            throw new DatabaseAccessException("Unable to initialize connection to the logging database as the ATS PassiveDbAppender for caller '"
                                              + ThreadsPerCaller.getCaller()
                                              + "' is not attached to log4j system");
        }

        String availableDbType = null;
        if (String.valueOf(DbConnSQLServer.DEFAULT_PORT).equals(loggingAppender.getAppenderConfig().getPort())) {

            checkIfMssqlAvailable(loggingAppender);
            availableDbType = DbConnSQLServer.DATABASE_TYPE;
        } else if (String.valueOf(DbConnPostgreSQL.DEFAULT_PORT)
                         .equals(loggingAppender.getAppenderConfig().getPort())) {

            checkIfPgsqlAvailable(loggingAppender);
            availableDbType = DbConnPostgreSQL.DATABASE_TYPE;
        } else {

            Throwable mssqlException = null;
            Throwable pgsqlException = null;

            try {
                checkIfMssqlAvailable(loggingAppender);
                availableDbType = DbConnSQLServer.DATABASE_TYPE;
            } catch (Exception e) {
                mssqlException = e;
            }

            try {
                checkIfPgsqlAvailable(loggingAppender);
                availableDbType = DbConnPostgreSQL.DATABASE_TYPE;
            } catch (Exception e) {
                pgsqlException = e;
            }

            if (mssqlException != null && pgsqlException != null) {
                log.error(mssqlException);
                log.error(pgsqlException);
                String errMsg = "Neither MSSQL, nor PostgreSQL server at '"
                                + loggingAppender.getAppenderConfig().getHost() + ":"
                                + loggingAppender.getAppenderConfig().getPort() +
                                "' has database with name '"
                                + loggingAppender.getAppenderConfig().getDatabase()
                                + "'. Exception for MSSQL is : \n\t" + mssqlException
                                + "\n\nException for PostgreSQL is: \n\t"
                                + pgsqlException;

                throw new DatabaseAccessException(errMsg);
            }
        }

        SQLServerDbWriteAccess writeAccess = null;
       if (availableDbType.equals(DbConnSQLServer.DATABASE_TYPE)) {

            // Create DB connection based on the log4j system settings
            if (DbKeys.SQL_SERVER_DRIVER_MICROSOFT.equalsIgnoreCase(loggingAppender.getAppenderConfig().getDriver())) {
                // Create DB connection based on the log4j system settings
                Map<String, Object> props = new HashMap<>();
                props.put(DbKeys.DRIVER, DbKeys.SQL_SERVER_DRIVER_MICROSOFT);
                DbConnection dbConnection = new DbConnSQLServer(loggingAppender.getAppenderConfig().getHost(),
                                                   Integer.parseInt(loggingAppender.getAppenderConfig().getPort()),
                                                   loggingAppender.getAppenderConfig().getDatabase(),
                                                   loggingAppender.getAppenderConfig().getUser(),
                                                   loggingAppender.getAppenderConfig().getPassword(), props);
                writeAccess = new SQLServerDbWriteAccessMSSQL(dbConnection, false);
                writeAccess.setMaxNumberOfCachedEvents(Integer.parseInt(loggingAppender.getAppenderConfig()
                                                                                       .getChunkSize()));
                return writeAccess;
            } else if (DbKeys.SQL_SERVER_DRIVER_JTDS.equalsIgnoreCase(loggingAppender.getAppenderConfig()
                                                                                     .getDriver())) {
                // Create DB connection based on the log4j system settings
                Map<String, Object> props = new HashMap<>();
                props.put(DbKeys.DRIVER, DbKeys.SQL_SERVER_DRIVER_JTDS);
                // Create DB connection based on the log4j system settings
                DbConnection dbConnection = new DbConnSQLServer(loggingAppender.getAppenderConfig().getHost(),
                                                   Integer.parseInt(loggingAppender.getAppenderConfig().getPort()),
                                                   loggingAppender.getAppenderConfig().getDatabase(),
                                                   loggingAppender.getAppenderConfig().getUser(),
                                                   loggingAppender.getAppenderConfig().getPassword(), props);
                writeAccess = new SQLServerDbWriteAccess(dbConnection, false);
                writeAccess.setMaxNumberOfCachedEvents(Integer.parseInt(loggingAppender.getAppenderConfig()
                                                                   .getChunkSize()));
                return writeAccess;
            } else {
                throw new IllegalArgumentException("Appender configuration specified SQL Server driver to be '"
                                                   + loggingAppender.getAppenderConfig().getDriver()
                                                   + "' which is not supported");
            }

        } else if (availableDbType.equals(DbConnPostgreSQL.DATABASE_TYPE)) {

            // Create DB connection based on the log4j system settings
            DbConnection dbConnection = new DbConnPostgreSQL(loggingAppender.getAppenderConfig().getHost(),
                                                 Integer.parseInt(loggingAppender.getAppenderConfig().getPort()),
                                                 loggingAppender.getAppenderConfig().getDatabase(),
                                                 loggingAppender.getAppenderConfig().getUser(),
                                                 loggingAppender.getAppenderConfig().getPassword(), null);

            // Create the database access layer
            writeAccess = new PGDbWriteAccess(dbConnection, false);
            writeAccess.setMaxNumberOfCachedEvents(Integer.parseInt(loggingAppender.getAppenderConfig()
                                                                                   .getChunkSize()));
            return writeAccess;

        } else {
            throw new UnsupportedOperationException("Could not use database '" + availableDbType
                                                    + "' to create write access object");
        } 
    }

    private void checkIfPgsqlAvailable( AbstractDbAppender loggingAppender ) throws DatabaseAccessException {

        log.info("Checking connectivity to [" + DbConnPostgreSQL.DATABASE_TYPE + "] ATS LOG database ...");

        try {
            DbUtils.checkPgsqlDatabaseAvailability(loggingAppender.getAppenderConfig().getHost(),
                                                   Integer.parseInt(loggingAppender.getAppenderConfig().getPort()),
                                                   loggingAppender.getAppenderConfig().getDatabase(),
                                                   loggingAppender.getAppenderConfig().getUser(),
                                                   loggingAppender.getAppenderConfig().getPassword());

            log.info("[" + DbConnPostgreSQL.DATABASE_TYPE + "] ATS LOG DB available: YES");
        } catch (Exception e) {
            throw new DatabaseAccessException("Unable to connect to " + DbConnPostgreSQL.DATABASE_TYPE
                                              + " ATS Log database.", e);
        }
    }

    private void checkIfMssqlAvailable( AbstractDbAppender loggingAppender ) throws DatabaseAccessException {

        log.info("Checking connectivity to [" + DbConnSQLServer.DATABASE_TYPE + "] ATS LOG database ...");

        try {
            DbUtils.checkMssqlDatabaseAvailability(loggingAppender.getAppenderConfig().getHost(),
                                                   Integer.parseInt(loggingAppender.getAppenderConfig().getPort()),
                                                   loggingAppender.getAppenderConfig().getDatabase(),
                                                   loggingAppender.getAppenderConfig().getUser(),
                                                   loggingAppender.getAppenderConfig().getPassword());

            log.info("[" + DbConnSQLServer.DATABASE_TYPE + "] ATS LOG DB available: YES");
        } catch (Exception e) {
            throw new DatabaseAccessException("Unable to connect to " + DbConnSQLServer.DATABASE_TYPE
                                              + " ATS Log database.", e);
        }

    }

}
