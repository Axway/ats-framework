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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

/**
 * Utilities to close Database connection, statement
 *
 *
 */
public class DbUtils {

    private static final Logger log = Logger.getLogger( DbUtils.class );

    /**
     * Closes JDBC statement and open ResultSet without throwing exception. If there is one it is just logged.
     */
    public static void closeStatement(
                                       Statement statement ) {

        if( statement == null ) {
            return;
        }
        try {
            boolean isClosed;
            try {
                isClosed = statement.isClosed();
            } catch(AbstractMethodError err) {
                isClosed = false; // no JavaSE 6-compatible driver
            }
            if( statement != null && !isClosed) {
                statement.close();
            }
        } catch( SQLException e ) {
            log.error( "Could not close SQL statement", e );
        }
    }
    
    public static void closeResultSet(
                                       ResultSet resultSet ) {

       try {
           if( resultSet != null ) {
               resultSet.close();
           }
       } catch( SQLException sqle ) {
           String msg = "Error closing resultset connection";
           log.error( getFullSqlException( msg, sqle ) );
       }
   }

    public static void closeConnection(
                                        Connection connection ) {

        try {
            if( connection != null ) {
                if( connection.isClosed() ) {
                    String msg = "SQL connection is already closed";
                    System.out.println( msg );
                } else {
                    connection.close();
                }
            }
        } catch( SQLException sqle ) {
            String msg = "Error closing database connection";
            // TODO - first print to console on new object and then log4j
            log.error( getFullSqlException( msg, sqle ) );
        }
    }

    /**
     * Closes SQL connection and statement with just logging potential exceptions instead of rethrowing them.
     * @param con
     * @param statement
     */
    public static void close(
                              Connection con,
                              Statement statement ) {

        closeStatement( statement );
        closeConnection( con );
    }

    /**
     * Gets all SqlException details including nested exceptions
     * @param message
     * @param sqlException
     * @return
     */
    public static String getFullSqlException(
                                              String message,
                                              SQLException sqlException ) {

        StringBuilder sb = new StringBuilder();
        if( message != null ) {
            sb.append( "Got SQL exception: " ).append( message ).append( "\n" );
        }

        while( sqlException != null ) {
            sqlException = addNestedSqlTrace( sqlException, sb );
        }
        return sb.toString();
    }

    /**
     * Adds single SQLException details and returns reference to the nested one
     * @param sqle
     * @param sb
     * @return
     */
    private static SQLException addNestedSqlTrace(
                                                   SQLException sqle,
                                                   StringBuilder sb ) {

        sb.append( "SQL Exception:" );
        sb.append( "\n\tMessage: " ).append( sqle.getMessage() );
        sb.append( "\n\tSQL state: " ).append( sqle.getSQLState() );
        sb.append( "\n\tVendor code: " ).append( sqle.getErrorCode() );
        StringWriter stringWriter = new StringWriter();
        sqle.printStackTrace( new PrintWriter( stringWriter ) );
        sb.append( stringWriter.toString() );
        sb.append( "\n----------------------------------------\n" );

        return sqle.getNextException();
    }

}
