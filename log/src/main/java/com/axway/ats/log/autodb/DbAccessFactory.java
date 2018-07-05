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
package com.axway.ats.log.autodb;

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
     * Retrieves the DB info from the log4j system and then creates a new
     * instance for writing into the DB
     * 
     * @return
     * @throws DatabaseAccessException
     */
    public SQLServerDbWriteAccess getNewDbWriteAccessObject() throws DatabaseAccessException {

        // Our DB appender keeps the DB connection info
        ActiveDbAppender loggingAppender = ActiveDbAppender.getCurrentInstance();
        if (loggingAppender == null) {
            throw new DatabaseAccessException("Unable to initialize connection to the logging database as the ATS ActiveDbAppender is not attached to log4j system");
        }

        DbConnection dbConnection = null;
        if (DbUtils.isMSSQLDatabaseAvailable(loggingAppender.getHost(),
                                             Integer.parseInt(loggingAppender.getPort()),
                                             loggingAppender.getDatabase(),
                                             loggingAppender.getUser(),
                                             loggingAppender.getPassword())) {

            // Create DB connection based on the log4j system settings
            dbConnection = new DbConnSQLServer(loggingAppender.getHost(),
                                               Integer.parseInt(loggingAppender.getPort()),
                                               loggingAppender.getDatabase(),
                                               loggingAppender.getUser(),
                                               loggingAppender.getPassword(), null);

            // Create the database access layer
            return new SQLServerDbWriteAccess(dbConnection, false);

        } else if (DbUtils.isPostgreSQLDatabaseAvailable(loggingAppender.getHost(),
                                                         Integer.parseInt(loggingAppender.getPort()),
                                                         loggingAppender.getDatabase(),
                                                         loggingAppender.getUser(),
                                                         loggingAppender.getPassword())) {

            // Create DB connection based on the log4j system settings
            dbConnection = new DbConnPostgreSQL(loggingAppender.getHost(),
                                                Integer.parseInt(loggingAppender.getPort()),
                                                loggingAppender.getDatabase(),
                                                loggingAppender.getUser(),
                                                loggingAppender.getPassword(), null);

            // Create the database access layer
            return new PGDbWriteAccess(dbConnection, false);

        } else {
            String errMsg = "Neither MSSQL, nor PostgreSQL server at '" + loggingAppender.getHost() +
                            "' has database with name '" + loggingAppender.getDatabase() + "'";
            throw new DatabaseAccessException(errMsg);

        }
    }

    /**
     * Retrieves the DB info from the log4j system and then creates a new
     * instance for writing into the DB
     * This method differs from the getNewDbWriteAccessObject(), 
     * because we use the PassiveDbAppender to retrieve the DB info
     * 
     * @return DbWriteAccess Object
     * @throws DatabaseAccessException
     */

    public SQLServerDbWriteAccess getNewDbWriteAccessObjectViaPassiveDbAppender( ) throws DatabaseAccessException {

        PassiveDbAppender loggingAppender = PassiveDbAppender.getCurrentInstance();
        if (loggingAppender == null) {
            throw new DatabaseAccessException("Unable to initialize connection to the logging database as the ATS PassiveDbAppender for caller '"
                                              + ThreadsPerCaller.getCaller()
                                              + "' is not attached to log4j system");
        }

        DbConnection dbConnection = null;
        if (DbUtils.isMSSQLDatabaseAvailable(loggingAppender.getAppenderConfig().getHost(),
                                             Integer.parseInt(loggingAppender.getAppenderConfig().getPort()),
                                             loggingAppender.getAppenderConfig().getDatabase(),
                                             loggingAppender.getAppenderConfig().getUser(),
                                             loggingAppender.getAppenderConfig().getPassword())) {

            // Create DB connection based on the log4j system settings
            dbConnection = new DbConnSQLServer(loggingAppender.getAppenderConfig().getHost(),
                                               Integer.parseInt(loggingAppender.getAppenderConfig().getPort()),
                                               loggingAppender.getAppenderConfig().getDatabase(),
                                               loggingAppender.getAppenderConfig().getUser(),
                                               loggingAppender.getAppenderConfig().getPassword(), null);

            // Create the database access layer
            return new SQLServerDbWriteAccess(dbConnection, false);

        } else if (DbUtils.isPostgreSQLDatabaseAvailable(loggingAppender.getAppenderConfig().getHost(),
                                                         Integer.parseInt(loggingAppender.getAppenderConfig().getPort()),
                                                         loggingAppender.getAppenderConfig().getDatabase(),
                                                         loggingAppender.getAppenderConfig().getUser(),
                                                         loggingAppender.getAppenderConfig().getPassword())) {

            // Create DB connection based on the log4j system settings
            dbConnection = new DbConnPostgreSQL(loggingAppender.getAppenderConfig().getHost(),
                                                Integer.parseInt(loggingAppender.getAppenderConfig().getPort()),
                                                loggingAppender.getAppenderConfig().getDatabase(),
                                                loggingAppender.getAppenderConfig().getUser(),
                                                loggingAppender.getAppenderConfig().getPassword(), null);

            // Create the database access layer
            return new PGDbWriteAccess(dbConnection, false);

        } else {
            String errMsg = "Neither MSSQL, nor PostgreSQL server at '" + loggingAppender.getAppenderConfig().getHost()
                            +
                            "' has database with name '" + loggingAppender.getAppenderConfig().getDatabase() + "'";
            throw new DatabaseAccessException(errMsg);

        }

    }

}
