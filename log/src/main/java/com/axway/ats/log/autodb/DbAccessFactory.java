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
import com.axway.ats.core.dbaccess.mssql.DbConnSQLServer;
import com.axway.ats.log.appenders.ActiveDbAppender;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;

public class DbAccessFactory {

    public DbAccessFactory() {

    }

    /**
     * Retrieves the DB info from the log4j system
     * and then creates a new instance for writing into the DB
     * 
     * @return
     * @throws DatabaseAccessException
     */
    public DbWriteAccess getNewDbWriteAccessObject() throws DatabaseAccessException {

        // Our DB appender keeps the DB connection info
        ActiveDbAppender loggingAppender = ActiveDbAppender.getCurrentInstance();
        if( loggingAppender == null ) {
            throw new DatabaseAccessException( "Unable to initialize connection to the logging database as the ATS ActiveDbAppender is not attached to log4j system" );
        }

        // Create DB connection based on the log4j system settings
        DbConnection dbConnection = new DbConnSQLServer( loggingAppender.getHost(),
                                                         loggingAppender.getDatabase(),
                                                         loggingAppender.getUser(),
                                                         loggingAppender.getPassword() );

        // Create the database access layer
        return new DbWriteAccess( dbConnection, false );
    }
}
