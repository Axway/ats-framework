/*
 * Copyright 2017-2019 Axway Software
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
import com.axway.ats.log.appenders.ActiveDbAppender;
import com.axway.ats.log.appenders.PassiveDbAppender;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;

public class DbAccessFactory {

    public DbAccessFactory() {

    }

    /**
     * Retrieves the DB info from the log4j2 system and then creates a new
     * instance for writing into the DB
     * 
     * @return
     * @throws DatabaseAccessException
     */
    public SQLServerDbWriteAccess getNewDbWriteAccessObject() throws DatabaseAccessException {

        // Our DB appender keeps the DB connection info
        ActiveDbAppender loggingAppender = ActiveDbAppender.getCurrentInstance();
        if (loggingAppender == null) {
            throw new DatabaseAccessException("Unable to initialize connection to the logging database as the ATS ActiveDbAppender is not attached to log4j2 system");
        }

        DbConnection dbConnection = null;
        Exception mssqlException = DbUtils.isMSSQLDatabaseAvailable(loggingAppender.getHost(),
                                                                    Integer.parseInt(loggingAppender.getPort()),
                                                                    loggingAppender.getDatabase(),
                                                                    loggingAppender.getUser(),
                                                                    loggingAppender.getPassword());
        SQLServerDbWriteAccess writeAccess = null;
        if (mssqlException == null) {

            // Create the database access layer
            if (DbKeys.SQL_SERVER_DRIVER_MICROSOFT.equalsIgnoreCase(loggingAppender.getAppenderConfig().getDriver())) {
                // Create DB connection based on the log4j2 system settings
                Map<String, Object> props = new HashMap<>();
                props.put(DbKeys.DRIVER, DbKeys.SQL_SERVER_DRIVER_MICROSOFT);
                dbConnection = new DbConnSQLServer(loggingAppender.getHost(),
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
                // Create DB connection based on the log4j2 system settings
                Map<String, Object> props = new HashMap<>();
                props.put(DbKeys.DRIVER, DbKeys.SQL_SERVER_DRIVER_JTDS);
                dbConnection = new DbConnSQLServer(loggingAppender.getHost(),
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

        } else {
            Exception pgsqlException = DbUtils.isPostgreSQLDatabaseAvailable(loggingAppender.getHost(),
                                                                             Integer.parseInt(loggingAppender.getPort()),
                                                                             loggingAppender.getDatabase(),
                                                                             loggingAppender.getUser(),
                                                                             loggingAppender.getPassword());
            if (pgsqlException == null) {
                // Create DB connection based on the log4j2 system settings
                dbConnection = new DbConnPostgreSQL(loggingAppender.getHost(),
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
                String errMsg = "Neither MSSQL, nor PostgreSQL server at '" + loggingAppender.getHost() + ":"
                                + loggingAppender.getPort() +
                                "' has database with name '" + loggingAppender.getDatabase()
                                + "'. Exception for MSSQL is : \n\t" + mssqlException
                                + "\n\nException for PostgreSQL is: \n\t"
                                + pgsqlException;
                throw new DatabaseAccessException(errMsg);
            }
        }

    }

    /**
     * Retrieves the DB info from the log4j2 system and then creates a new
     * instance for writing into the DB
     * This method differs from the getNewDbWriteAccessObject(), 
     * because we use the PassiveDbAppender to retrieve the DB info
     * 
     * @return DbWriteAccess Object
     * @throws DatabaseAccessException
     */

    public SQLServerDbWriteAccess getNewDbWriteAccessObjectViaPassiveDbAppender(
                                                                                 String callerId ) throws DatabaseAccessException {

        PassiveDbAppender loggingAppender = PassiveDbAppender.getCurrentInstance(callerId);
        if (loggingAppender == null) {
            throw new DatabaseAccessException("Unable to initialize connection to the logging database as the ATS PassiveDbAppender for caller '"
                                              + ThreadsPerCaller.getCaller()
                                              + "' is not attached to log4j2 system");
        }

        DbConnection dbConnection = null;
        Exception mssqlException = DbUtils.isMSSQLDatabaseAvailable(loggingAppender.getAppenderConfig().getHost(),
                                                                    Integer.parseInt(loggingAppender.getAppenderConfig()
                                                                                                    .getPort()),
                                                                    loggingAppender.getAppenderConfig().getDatabase(),
                                                                    loggingAppender.getAppenderConfig().getUser(),
                                                                    loggingAppender.getAppenderConfig().getPassword());
        SQLServerDbWriteAccess writeAccess = null;
        if (mssqlException == null) {

            // Create the database access layer
            if (DbKeys.SQL_SERVER_DRIVER_MICROSOFT.equalsIgnoreCase(loggingAppender.getAppenderConfig().getDriver())) {
                // Create DB connection based on the log4j2 system settings
                Map<String, Object> props = new HashMap<>();
                props.put(DbKeys.DRIVER, DbKeys.SQL_SERVER_DRIVER_MICROSOFT);
                dbConnection = new DbConnSQLServer(loggingAppender.getAppenderConfig().getHost(),
                                                   Integer.parseInt(loggingAppender.getAppenderConfig().getPort()),
                                                   loggingAppender.getAppenderConfig().getDatabase(),
                                                   loggingAppender.getAppenderConfig().getUser(),
                                                   loggingAppender.getAppenderConfig().getPassword(), null);
                writeAccess = new SQLServerDbWriteAccessMSSQL(dbConnection, false);
            } else if (DbKeys.SQL_SERVER_DRIVER_JTDS.equalsIgnoreCase(loggingAppender.getAppenderConfig()
                                                                                     .getDriver())) {
                // Create DB connection based on the log4j2 system settings
                Map<String, Object> props = new HashMap<>();
                props.put(DbKeys.DRIVER, DbKeys.SQL_SERVER_DRIVER_JTDS);
                dbConnection = new DbConnSQLServer(loggingAppender.getAppenderConfig().getHost(),
                                                   Integer.parseInt(loggingAppender.getAppenderConfig().getPort()),
                                                   loggingAppender.getAppenderConfig().getDatabase(),
                                                   loggingAppender.getAppenderConfig().getUser(),
                                                   loggingAppender.getAppenderConfig().getPassword(), props);
                writeAccess = new SQLServerDbWriteAccess(dbConnection, false);
            } else {
                throw new IllegalArgumentException("Appender configuration specified SQL Server driver to be '"
                                                   + loggingAppender.getAppenderConfig().getDriver()
                                                   + "' which is not supported");
            }

        } else {
            Exception pgsqlException = DbUtils.isPostgreSQLDatabaseAvailable(loggingAppender.getAppenderConfig()
                                                                                            .getHost(),
                                                                             Integer.parseInt(loggingAppender.getAppenderConfig()
                                                                                                             .getPort()),
                                                                             loggingAppender.getAppenderConfig()
                                                                                            .getDatabase(),
                                                                             loggingAppender.getAppenderConfig()
                                                                                            .getUser(),
                                                                             loggingAppender.getAppenderConfig()
                                                                                            .getPassword());
            if (pgsqlException == null) {
                // Create DB connection based on the log4j2 system settings
                dbConnection = new DbConnPostgreSQL(loggingAppender.getAppenderConfig().getHost(),
                                                    Integer.parseInt(loggingAppender.getAppenderConfig().getPort()),
                                                    loggingAppender.getAppenderConfig().getDatabase(),
                                                    loggingAppender.getAppenderConfig().getUser(),
                                                    loggingAppender.getAppenderConfig().getPassword(), null);

                // Create the database access layer
                writeAccess = new PGDbWriteAccess(dbConnection, false);
            } else {
                String errMsg = "Neither MSSQL, nor PostgreSQL server at '"
                                + loggingAppender.getAppenderConfig().getHost() +
                                "' has database with name '" + loggingAppender.getAppenderConfig().getHost() + ":"
                                + loggingAppender.getAppenderConfig().getPort()
                                + "'. Exception for MSSQL is : \n\t" + mssqlException
                                + "\n\nException for PostgreSQL is: \n\t"
                                + pgsqlException;
                throw new DatabaseAccessException(errMsg);
            }

        }
        if (writeAccess != null) {
            writeAccess.setMaxNumberOfCachedEvents(Integer.parseInt(loggingAppender.getAppenderConfig()
                                                                                   .getChunkSize()));
        }
        return writeAccess;

    }

}
