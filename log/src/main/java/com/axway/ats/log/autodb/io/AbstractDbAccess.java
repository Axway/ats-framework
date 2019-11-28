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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.dbaccess.ConnectionPool;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.DbUtils;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.dbaccess.mssql.DbConnSQLServer;
import com.axway.ats.core.dbaccess.postgresql.DbConnPostgreSQL;
import com.axway.ats.core.log.AtsConsoleLogger;
import com.axway.ats.core.utils.BackwardCompatibility;
import com.axway.ats.core.utils.ExceptionUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;

/**
 * Class containing methods, shared between {@link SQLServerDbWriteAccess} and {@link PGDbWriteAccess}.
 * */

public abstract class AbstractDbAccess {

    protected final AtsConsoleLogger     log;
    
    public static final int              DEFAULT_CHUNK_SIZE       = 2000;

    public static final String           UNABLE_TO_CONNECT_ERRROR = "Unable to connect to log DB";

    // full date formats
    public final static SimpleDateFormat DATE_FORMAT              = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public final static SimpleDateFormat DATE_FORMAT_IN_UTC       = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // date formats without the year component
    public final static SimpleDateFormat DATE_FORMAT_NO_YEAR      = new SimpleDateFormat("MMM dd HH:mm:ss");

    // time format
    public static final SimpleDateFormat TIME_FORMAT              = new SimpleDateFormat("HH:mm:ss:SSS");

    private static final int             MIN_IN_SECONDS           = 60;
    private static final int             HOUR_IN_SECONDS          = MIN_IN_SECONDS * 60;
    private static final int             DAY_IN_SECONDS           = HOUR_IN_SECONDS * 24;

    private String                       dbVersion                = null;

    protected int                        chunkSize                = 2000;

    /**
     * dbInternalVersion is introduced in version 3.10.0
    **/
    @BackwardCompatibility
    private int                          dbInternalVersion        = -1;

    /**
     * dbInitialVersion is introduced in version 3.10.0
    **/
    @BackwardCompatibility
    private int                          dbInitialVersion         = -1;

    static {
        DATE_FORMAT_IN_UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * SQL connection which could be shared between different method invocations
     * like in the sanity check case where test run data is inserted.
     * The code invoking methods of this class is required to:
     * <ol>
     * <li>get connection</li>
     * <li>assign it to this variable - {@link #setInternalConnection(Connection)}</li>
     * <li>invoke methods here</li>
     * <li>set local connection variable to null - {@link #clearInternalConnection()}</li>
     * <li>close connection from outside after dbAccess work is processed</li>
     */
    protected Connection   connection;

    /**
     * The database connection information and provider for MsSQL DB connections.
     */
    protected DbConnection dbConnectionFactory;

    public AbstractDbAccess( DbConnection dbConnection ) {

        this.log = new AtsConsoleLogger(getClass());

        this.dbConnectionFactory = dbConnection;

        if (System.getProperties().containsKey(AtsSystemProperties.LOG__MAX_CACHE_EVENTS)) {
            String propVal = null;
            try {
                propVal = System.getProperty(AtsSystemProperties.LOG__MAX_CACHE_EVENTS);
                this.chunkSize = Integer.parseInt(propVal);
            } catch (Exception e) {
                log.warn("Could not set chunk size, due to ATS-related System property '"
                         + AtsSystemProperties.LOG__MAX_CACHE_EVENTS + "' set to invalid INT value '" + propVal + "'");
            }
        }
    }

    /**
     * Provide reference for connection for general use
     * @return
     * @throws DatabaseAccessException
     */
    protected Connection getConnection() throws DatabaseAccessException {

        try {
            return ConnectionPool.getConnection(dbConnectionFactory);
        } catch (DbException dbe) {
            throw new DatabaseAccessException(UNABLE_TO_CONNECT_ERRROR, dbe);
        }
    }

    protected void closeConnection( Connection connection ) {

        DbUtils.closeConnection(connection);
    }

    public void checkConnection() throws DatabaseAccessException {

        closeConnection(getConnection());
    }

    public String getDatabaseVersion() throws DatabaseAccessException {

        if (dbVersion == null) {

            Connection connection = getConnection();
            PreparedStatement statement = null;
            ResultSet rs = null;
            String sql = createGetDatabaseVersionStatementQuery();
            try {
                statement = connection.prepareStatement(sql);
                rs = statement.executeQuery();

                // we expect only one record
                if (rs.next()) {
                    dbVersion = rs.getString(1);
                } else {
                    throw new DatabaseAccessException("Could not fetch the DB version");
                }
            } catch (Exception e) {
                throw new DatabaseAccessException("Error fetching DB version", e);
            } finally {
                DbUtils.closeResultSet(rs);
                DbUtils.close(connection, statement);
            }
        }
        return dbVersion;
    }

    private String createGetDatabaseVersionStatementQuery() {

        if (this.dbConnectionFactory instanceof DbConnSQLServer) {
            return "SELECT value from tInternal where [key] = 'version'";
        } else if (this.dbConnectionFactory instanceof DbConnPostgreSQL) {
            return "SELECT value from \"tInternal\" where key = 'version'";
        } else {
            throw new UnsupportedOperationException("Could not construct statement query for getting database version for connection of class '"
                                                    + this.connection.getClass().getName() + "'");
        }
    }

    public int getDatabaseInternalVersion() throws NumberFormatException {

        if (dbInternalVersion == -1) { // not yet tried to be extracted from DB

            Connection connection = null;
            PreparedStatement statement = null;
            ResultSet rs = null;
            String sql = createGetInternalVersionStatementQuery();
            try {
                connection = getConnection();
                statement = connection.prepareStatement(sql);
                rs = statement.executeQuery();

                // we expect only one record
                if (rs.next()) {
                    String value = rs.getString(1);
                    if (StringUtils.isNullOrEmpty(value)) {
                        dbInternalVersion = 0;
                    } else {
                        dbInternalVersion = Integer.parseInt(value.trim());
                    }
                } else {
                    dbInternalVersion = 0;
                }
                if (dbInternalVersion == 0) {
                    log.debug("DB internalVersion not found.");
                }
            } catch (NumberFormatException nfe) {
                throw new NumberFormatException("Error parsing DB internalVersion");
            } catch (Exception e) {
                log.debug(ExceptionUtils.getExceptionMsg(e, "Error fetching DB internalVersion"));
                dbInternalVersion = 0;
            } finally {
                DbUtils.close(connection, statement);
                DbUtils.closeResultSet(rs);
            }

        }
        return dbInternalVersion;
    }

    private String createGetInternalVersionStatementQuery() {

        if (this.dbConnectionFactory instanceof DbConnSQLServer) {
            return "SELECT value FROM tInternal WHERE [key] = 'internalVersion'";
        } else if (this.dbConnectionFactory instanceof DbConnPostgreSQL) {
            return "SELECT value FROM \"tInternal\" WHERE key = 'internalVersion'";
        } else {
            throw new UnsupportedOperationException("Could not construct statement query for getting internal database version for connection of class '"
                                                    + this.connection.getClass().getName() + "'");
        }
    }

    public int getDatabaseInitialVersion() throws NumberFormatException {

        if (dbInitialVersion == -1) { // not yet tried to be extracted from DB

            Connection connection = null;
            PreparedStatement statement = null;
            ResultSet rs = null;
            String sql = createGetInitialVersionStatementQuery();
            try {
                connection = getConnection();
                statement = connection.prepareStatement(sql);
                rs = statement.executeQuery();

                // we expect only one record
                if (rs.next()) {
                    String value = rs.getString(1);
                    if (StringUtils.isNullOrEmpty(value)) {
                        dbInitialVersion = 0;
                    } else {
                        dbInitialVersion = Integer.parseInt(value.trim());
                    }
                } else {
                    dbInitialVersion = 0;
                }
                if (dbInitialVersion == 0) {
                    log.debug("DB initialVersion not found.");
                }
            } catch (NumberFormatException nfe) {
                throw new NumberFormatException("Error parsing DB initialVersion");
            } catch (Exception e) {
                log.debug(ExceptionUtils.getExceptionMsg(e, "Error fetching DB initialVersion"));
                dbInitialVersion = 0;
            } finally {
                DbUtils.closeResultSet(rs);
                DbUtils.close(connection, statement);
            }

        }
        return dbInitialVersion;
    }

    private String createGetInitialVersionStatementQuery() {

        if (this.dbConnectionFactory instanceof DbConnSQLServer) {
            return "SELECT value from tInternal where [key] = 'initialVersion'";
        } else if (this.dbConnectionFactory instanceof DbConnPostgreSQL) {
            return "SELECT value from \"tInternal\" where key = 'initialVersion'";
        } else {
            throw new UnsupportedOperationException("Could not construct statement query for getting initial database version for connection of class '"
                                                    + this.connection.getClass().getName() + "'");
        }
    }

    protected String formatDate( Timestamp timestamp ) {

        if (timestamp != null) {
            return DATE_FORMAT.format(timestamp);
        } else {
            return "";
        }
    }

    protected String formatDateNoYear( Timestamp timestamp ) {

        if (timestamp != null) {
            return DATE_FORMAT_NO_YEAR.format(timestamp);
        } else {
            return "";
        }
    }

    /**
     * Convert time in hours to full date and time
     * @param timeOffset time in hours
     * @return
     */
    protected String formatDateFromEpoch( float timeOffset ) {

        Calendar fdate = Calendar.getInstance();
        fdate.setTimeInMillis((int) (timeOffset * 3600000));
        return DATE_FORMAT_IN_UTC.format(fdate.getTime());
    }

    /**
     * Convert duration in seconds to days, hours, minutes and seconds
     * @param time time in seconds
     * @return
     */
    public static String formatTimeDiffereceFromSecondsToString( int time ) {

        int days = time / DAY_IN_SECONDS;
        time -= days * DAY_IN_SECONDS;

        int hours = time / HOUR_IN_SECONDS;
        time -= hours * HOUR_IN_SECONDS;

        int minutes = time / MIN_IN_SECONDS;
        time -= minutes * MIN_IN_SECONDS;

        int seconds = time;

        NumberFormat nf = NumberFormat.getIntegerInstance();
        nf.setMinimumIntegerDigits(2);

        StringBuilder duration = new StringBuilder();
        if (days > 0) {
            duration.append(days);
            duration.append(" days, ");
        }
        duration.append(nf.format(hours));
        duration.append(":");
        duration.append(nf.format(minutes));
        duration.append(":");
        duration.append(nf.format(seconds));
        return duration.toString();
    }

    public static int formatTimeDiffereceFromStringToSeconds( String duration ) {

        int daysInt = 0;
        if (duration.contains("days")) {
            String daysString = duration.substring(0, duration.indexOf(' '));
            daysInt = Integer.parseInt(daysString);

            // remove the days from the duration string
            duration = duration.substring(duration.indexOf(',') + 1).trim();
        }

        String[] durationTokens = duration.split(":");
        int hoursInt = calculateTimeValue(durationTokens[0].toCharArray());
        int minutesInt = calculateTimeValue(durationTokens[1].toCharArray());
        int secondsInt = calculateTimeValue(durationTokens[2].toCharArray());

        return daysInt * DAY_IN_SECONDS + hoursInt * HOUR_IN_SECONDS + minutesInt * MIN_IN_SECONDS
               + secondsInt;
    }

    private static int calculateTimeValue( char[] timeChars ) {

        int timeValue = 0;
        if (timeChars[0] != '0') {
            timeValue += (Integer.valueOf(timeChars[0]) - '0') * 10;
        }
        timeValue += Integer.valueOf(timeChars[1]) - '0';

        return timeValue;
    }

    protected void logQuerySuccess( String query, String entities, int records ) {

        log.debug(query + "\nFetched " + records + " " + entities);
    }
}
