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
package com.axway.ats.log.autodb.exceptions;

import java.sql.SQLException;

import com.axway.ats.core.log.AtsConsoleLogger;

@SuppressWarnings( "serial")
public class DatabaseAccessException extends LoggingException {

    private AtsConsoleLogger atsConsoleLogger = new AtsConsoleLogger(getClass());

    public DatabaseAccessException( String message ) {

        super(message);
    }

    public DatabaseAccessException( String message, Throwable cause ) {

        super(message, cause);

        if (cause instanceof java.sql.SQLException) {

            atsConsoleLogger.error("Got SQL exception while trying to work with ATS logging DB: "
                                   + message);
            atsConsoleLogger.error("Printing exception chain: ");

            SQLException next = (SQLException) cause;
            while (next != null) {
                next = printStackTrace(next);

            }
        }
    }

    private SQLException printStackTrace( SQLException sqle ) {

        atsConsoleLogger.error("SQL Exception:\nMessage: " + sqle.getMessage() + "\nSQL state: "
                               + sqle.getSQLState() + "\nVendor code: " + sqle.getErrorCode());
        sqle.printStackTrace();
        atsConsoleLogger.error("------------------------------------------------");

        return sqle.getNextException();
    }
}
