/*
 * Copyright 2020 Axway Software
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
package com.axway.ats.log;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.DbUtils;
import com.axway.ats.core.dbaccess.mssql.DbConnSQLServer;
import com.axway.ats.core.dbaccess.postgresql.DbConnPostgreSQL;
import com.axway.ats.core.utils.ExceptionUtils;
import com.axway.ats.log.appenders.ActiveDbAppender;
import com.axway.ats.log.autodb.entities.Checkpoint;
import com.axway.ats.log.autodb.entities.CheckpointSummary;
import com.axway.ats.log.autodb.entities.LoadQueue;
import com.axway.ats.log.autodb.entities.Machine;
import com.axway.ats.log.autodb.entities.Run;
import com.axway.ats.log.autodb.entities.Scenario;
import com.axway.ats.log.autodb.entities.Statistic;
import com.axway.ats.log.autodb.entities.StatisticDescription;
import com.axway.ats.log.autodb.entities.Suite;
import com.axway.ats.log.autodb.entities.Testcase;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.log.autodb.io.PGDbReadAccess;
import com.axway.ats.log.autodb.io.SQLServerDbReadAccess;
import com.axway.ats.log.autodb.model.IDbReadAccess;
import com.axway.ats.log.model.CheckpointResult;

import io.netty.util.internal.StringUtil;

/**
 * <p>Utility class for obtaining test execution data from an ATS Log DB</p>
 * <p>Note that by default, using {@link AtsDbReader#getDefaultInstance()}, test execution data is obtained from the
 * current log database, specified in the ActiveDbAppender section on the log4j2.xml file.<br>
 * So, if no such entry exists, or DB logging is disabled, you'll have to provide custom data for the log DB
 * via {@link AtsDbReader#getInstance(String, int, String, String, String, Map)}
 * </p>
 */
@PublicAtsApi
public class AtsDbReader {
    /*
    TODO notes
     1. Add method for obtaining test case/suite/run messages
     2. Add possibility to obtain checkpoints for one, two or more machines
     3. More methods that use test case/suite/load queue/etc name (not only IDs) for obtaining Log DB data
     4. Move metadata-related methods from AtsDbLogger here
     */

    /**
     * Use this value to "tell" ATS that you want aggregated information for all of the machines, when invoking
     * {@link AtsDbReader#getStatisticsDescriptionForDateRange(int, int, int, long, long)}
     */
    public static final int ALL_MACHINES = -1024;

    public static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");
    public static final ZoneId SYSTEM_DEFAULT_ZONE_ID = ZoneId.systemDefault();

    private static Logger log = LogManager.getLogger(AtsDbReader.class);
    private static AtsDbReader instance = null;

    private IDbReadAccess readAccess;
    private boolean isPGSQLServer = false;

    /**
     * This is not the constructor you are looking for.<br>
     * Check {@link AtsDbReader#getInstance(String, int, String, String, String, Map)} or {@link AtsDbReader#getDefaultInstance()}
     */
    private AtsDbReader(DbConnection dbConnection) throws DatabaseAccessException {

        if (dbConnection == null) {

            if (!ActiveDbAppender.isAttached) {
                throw new IllegalStateException(ActiveDbAppender.class.getName()
                        + " not attached! Check the ATS documetation for information about enabling DB logging and try again");
            }

            readAccess = ActiveDbAppender.getCurrentInstance().obtainDbReadAccessObject();

            isPGSQLServer = readAccess instanceof PGDbReadAccess;

        } else {
            if (dbConnection instanceof DbConnSQLServer) {
                readAccess = new SQLServerDbReadAccess(dbConnection);
                isPGSQLServer = false;
            } else if (dbConnection instanceof DbConnPostgreSQL) {
                readAccess = new PGDbReadAccess(dbConnection);
                isPGSQLServer = true;
            } else {
                throw new IllegalArgumentException("Database '" + dbConnection.getDbType()
                        + "' is not supported as an ATS Log DB. Use either '"
                        + DbConnSQLServer.DATABASE_TYPE + "' or '"
                        + DbConnPostgreSQL.DATABASE_TYPE + "' .");
            }
        }
    }

    /**
     * Create AtsDbReader, by using the default database login parameters from the ActiveDbAppender section in log4j2.xml<br>
     * To use other log database, use {@link AtsDbReader#getInstance(String, int, String, String, String, Map)}
     *
     * @throws DatabaseAccessException - if error occurred while checking log db availability
     */
    @PublicAtsApi
    public static AtsDbReader getDefaultInstance() throws DatabaseAccessException {

        if (instance == null) {

            synchronized (AtsDbReader.class) {
                if (instance == null) {
                    instance = new AtsDbReader(null);
                }
            }

        }

        return instance;
    }

    /**
     * Create AtsDbReader, by using the custom database login parameters
     * For using the ActiveDbAppender database (currently used log database), use {@link AtsDbReader#getDefaultInstance()}
     *
     * @param host             - the DB host
     * @param port             - the DB port. You can use {@link DbConnSQLServer#DEFAULT_PORT} or {@link DbConnPostgreSQL#DEFAULT_PORT}
     * @param db               - the DB name
     * @param user             - the DB user's name or null to use the default one (AtsUser)
     * @param password         - the DB user's password or null to use the default one (AtsPassword)
     * @param customProperties - map of properties for the DB connection or null for none. Check {@link DbKeys} for supported properties
     * @throws DatabaseAccessException - if error occurred while checking log db availability
     */
    @PublicAtsApi
    public static AtsDbReader getInstance(String host, int port, String db, String user, String password,
                                          Map<String, Object> customProperties) throws DatabaseAccessException {

        DbConnection dbConnection = null;

        if (StringUtil.isNullOrEmpty(host)) {
            throw new IllegalArgumentException("'host' argument must not be null/empty");
        }

        if (StringUtil.isNullOrEmpty(user)) {
            user = "AtsUser";
        }

        if (StringUtil.isNullOrEmpty(password)) {
            password = "AtsPassword";
        }

        if (port == DbConnSQLServer.DEFAULT_PORT) {
            dbConnection = new DbConnSQLServer(host, port, db, user, password, customProperties);
        } else if (port == DbConnPostgreSQL.DEFAULT_PORT) {
            dbConnection = new DbConnPostgreSQL(host, port, db, user, password, customProperties);
        } else {

            //what if the SQL server or PGSQL server are accessible only via secure connections?

            Exception mssqlException = DbUtils.isMSSQLDatabaseAvailable(host, port, db, user, password);
            if (mssqlException == null) {
                dbConnection = new DbConnSQLServer(host, port, db, user, password, customProperties);
            } else {
                Exception pgsqlException = DbUtils.isPostgreSQLDatabaseAvailable(host, port, db, user, password);
                if (pgsqlException == null) {
                    dbConnection = new DbConnPostgreSQL(host, port, db, user, password, customProperties);
                } else {
                    throw new DatabaseAccessException(constructSQLServerNotFoundExceptionMessage(mssqlException,
                            pgsqlException, host,
                            port, db, user,
                            password,
                            customProperties));
                }
            }
        }
        return new AtsDbReader(dbConnection);

    }

    /**
     * Convert some time stamp (milliseconds since Epoch) to a time zone
     *
     * @param timestamp - the date/time in milliseconds
     * @param zoneId    - the zone ID of the Time zone you want to convert the time stamp. You can use
     *                    {@link AtsDbReader#UTC_ZONE_ID} for UTC and {@link AtsDbReader#SYSTEM_DEFAULT_ZONE_ID} for
     *                    the system default one
     * @return {@link ZonedDateTime}. You can use the toString() method on the returned object as well
     */
    @PublicAtsApi
    public ZonedDateTime convertTimestampToZone(long timestamp, ZoneId zoneId) {

        Instant instant = Instant.ofEpochMilli(timestamp);
        ZonedDateTime zonedDateTime = instant.atZone(zoneId);
        return zonedDateTime;
    }

    /**
     * Get RUNs from ATS Log DB within specific time range
     *
     * @param startTimestamp the start time stamp in milliseconds
     * @param endTimestamp   the end time stamp in milliseconds
     * @param runName        run name filtering criteria
     * @param productName
     * @param versionName
     * @param buildName
     * @param osName
     * @param userNote
     * @return list of {@link Run}s
     * @throws DatabaseAccessException - if error occurred while obtaining RUNs from ATS Log DB
     */
    @PublicAtsApi
    public List<Run> getRuns(long startTimestamp, long endTimestamp, String runName, String productName,
                             String versionName, String buildName, String osName,
                             String userNote) throws DatabaseAccessException {

        String whereClause = constructGetRunsWhereClause(isPGSQLServer, startTimestamp, endTimestamp, runName,
                productName, versionName, buildName, osName, userNote);

        return readAccess.getRuns(0, Integer.MAX_VALUE, whereClause, "runId", true, 0);

    }

    /**
     * Get all {@link Suite}s from RUN.<br>Note that the time stamps for start and end date will be in UTC
     *
     * @param runId - the parent run ID
     * @return list of {@link Suite}s
     * @throws DatabaseAccessException - if error occurred while obtaining data from the DB
     */
    @PublicAtsApi
    public List<Suite> getSuites(int runId) throws DatabaseAccessException {

        return readAccess.getSuites(0, Integer.MAX_VALUE, "WHERE runId = " + runId, "suiteId", true, 0);
    }

    /**
     * Get all {@link Scenario}s from SUITE.<br>Note that the time stamps for start and end date will be in UTC
     *
     * @param suiteId - the suite ID
     * @return list of {@link Scenario}s
     * @throws DatabaseAccessException - if error occurred while obtaining data from the DB
     */
    @PublicAtsApi
    public List<Scenario> getScenarios(int suiteId) throws DatabaseAccessException {

        return readAccess.getScenarios(0, Integer.MAX_VALUE, "WHERE suiteId = " + suiteId, "scenarioId", true,
                0);
    }

    /**
     * Get all {@link Testcase}s from SUITE.<br>Note that the time stamps for start and end date will be in UTC
     *
     * @param suiteId - the parent suite ID
     * @return list of {@link Testcase}s
     * @throws DatabaseAccessException - if error occurred while obtaining data from the DB
     */
    @PublicAtsApi
    public List<Testcase> getTestcases(int suiteId) throws DatabaseAccessException {

        return getTestcases("suiteId = " + suiteId);
    }

    /**
     * Get all {@link Testcase}s from SUITE and SCENARIO.<br>Note that the time stamps for start and end date will be in UTC
     *
     * @param suiteId    - the parent suite ID
     * @param scenarioId - the scenario ID
     * @return list of {@link Testcase}s
     * @throws DatabaseAccessException - if error occurred while obtaining data from the DB
     */
    @PublicAtsApi
    public List<Testcase> getTestcases(int suiteId, int scenarioId) throws DatabaseAccessException {

        return getTestcases("suiteId = " + suiteId + " AND scenarioId = " + scenarioId);
    }

    /**
     * Get all {@link LoadQueue}s from TESTCASE.<br>Note that the time stamps for start and end date will be in UTC
     *
     * @param testcaseId - the parent test case ID
     * @return list of {@link LoadQueue}s
     * @throws DatabaseAccessException - if error occurred while obtaining data from the DB
     */
    @PublicAtsApi
    public List<LoadQueue> getLoadQueues(int testcaseId) throws DatabaseAccessException {

        return getLoadQueues("testcaseId = " + testcaseId);
    }

    /**
     * Get all {@link CheckpointSummary}s from TESTCASE.
     *
     * @param loadQueueId - the parent loadQueue ID
     * @return list of {@link CheckpointSummary}s
     * @throws DatabaseAccessException - if error occurred while obtaining data from the DB
     */
    @PublicAtsApi
    public List<CheckpointSummary> getCheckpointSummaries(int loadQueueId) throws DatabaseAccessException {

        return readAccess.getCheckpointsSummary("loadQueueId = "
                        + loadQueueId,
                "checkpointSummaryId",
                true);
    }

    /**
     * Get all machines from the ATS Log DB<br>
     * Note that you should not expect every machine to be used in each RUN/SUITE/TESTCASE/etc, because if a machine was used in some test case for example,
     * and that test case is deleted, the machine continues to exist in the DB. So keep that in mind, when using data, returned by this method
     *
     * @return list of {@link Machine}s
     * @throws DatabaseAccessException - if an error occurred while obtaining data from DB
     */
    @PublicAtsApi
    public List<Machine> getMachines() throws DatabaseAccessException {

        String whereClause = "WHERE 1=1";
        return getMachines(whereClause);
    }

    /**
     * Get all machines, that were monitoring statistics, referenced by the statisticTypeId, and which were configured to monitor via the given test case<br>
     * Note that you should not expect every machine to be used in each RUN/SUITE/TESTCASE/etc, because if a machine was used in some test case for example,
     * and that test case is deleted, the machine continues to exist in the DB. So keep that in mind, when using data, returned by this method
     *
     * @param testcaseId      - the test case ID
     * @param statisticTypeId - the statistic type ID
     * @return list of {@link Machine}s
     * @throws DatabaseAccessException - if an error occurred while obtaining data from DB
     */
    @PublicAtsApi
    public List<Machine> getMachines(int testcaseId, int statisticTypeId) throws DatabaseAccessException {

        String whereClause = null;
        if (isPGSQLServer) {
            whereClause = "WHERE machineId IN (SELECT machineId FROM \"tSystemStats\" WHERE testcaseId = "
                    + testcaseId + " AND statsTypeId = " + statisticTypeId + ")";
        } else {
            whereClause = "WHERE machineId IN (SELECT machineId FROM tSystemStats WHERE testcaseId = "
                    + testcaseId + " AND statsTypeId = " + statisticTypeId + ")";
        }

        return getMachines(whereClause);
    }

    /**
     * Get all monitoring statistic descriptions from all machines from a test case
     *
     * @param testcaseId - the test case ID where some sort of monitoring was started
     * @return list of {@link StatisticDescription}s
     * @throws DatabaseAccessException if error occurred with working with the ATS Log DB
     */
    @PublicAtsApi
    public List<StatisticDescription> getStatisticDescriptions(int testcaseId) throws DatabaseAccessException {

        String whereClause = "WHERE ss.testcaseId = " + testcaseId;
        return getStatisticDescriptions(whereClause);

    }

    /**
     * Get all monitoring statistic descriptions from single machine from a test case
     *
     * @param testcaseId - the test case ID where some sort of monitoring was started
     * @param machineId  - the machine ID where monitoring was started
     * @return list of {@link StatisticDescription}s
     * @throws DatabaseAccessException if error occurred with working with the ATS Log DB
     */
    @PublicAtsApi
    public List<StatisticDescription> getStatisticDescriptions(int testcaseId,
                                                               int machineId) throws DatabaseAccessException {

        String whereClause = "WHERE ss.testcaseId = " + testcaseId + " AND ss.machineId = " + machineId;
        return getStatisticDescriptions(whereClause);

    }

    /**
     * Get aggregated information about particular checkpoint summary for a given date range<br>
     * In other words you can use this method to see how a given action is being executed for some time interval.<br>
     * For example you can see information for some FTPS file transfer (upload), like response time and transfer rate for the first 15 minutes of a given load queue
     *
     * @param checkpointSummaryId - the ID of the checkpoint. You can use {@link AtsDbReader#getCheckpointSummaries(int)} to obtain this value
     * @param loadQueueId         - the ID of the load queue, where the action you are interested in was executed. You can use {@link AtsDbReader#getLoadQueues(int)} to obtain this value
     * @param testcaseId          - the ID of the test case, where the action you are interested in was executed. You can use {@link AtsDbReader#getTestcases(int)} to obtain this value
     * @param startTimestamp      - the start time stamp in milliseconds
     * @param endTimestamp        - the end time stamp in milliseconds
     * @return list of {@link CheckpointSummary}s
     * @throws IllegalArgumentException if any of the arguments is invalid
     * @throws DatabaseAccessException  if an error occurred while obtaining data from the DB
     * @throws IllegalStateException    if {@link ActiveDbAppender} was not attached (not found in log4j2.xml configuration file)
     */
    @PublicAtsApi
    public CheckpointSummary
    getCheckpointSummaryForDateRange(int checkpointSummaryId, int loadQueueId, int testcaseId,
                                     long startTimestamp,
                                     long endTimestamp) throws DatabaseAccessException {

        // check input arguments
        if (checkpointSummaryId < 0) {
            throw new IllegalArgumentException("checkpointSummary ID is invalid (" + checkpointSummaryId + ")");
        }

        if (loadQueueId < 0) {
            throw new IllegalArgumentException("loadQueue ID is invalid (" + loadQueueId + ")");
        }

        if (testcaseId < 0) {
            throw new IllegalArgumentException("testcase ID is invalid (" + testcaseId + ")");
        }

        if (startTimestamp < 0) {
            throw new IllegalArgumentException("startTimestamp is invalid (" + startTimestamp + ")");
        }

        if (endTimestamp < 0) {
            throw new IllegalArgumentException("endTimestamp is invalid (" + endTimestamp + ")");
        }

        if (isDateAfter(startTimestamp, endTimestamp)) {
            throw new IllegalArgumentException("startTimestamp (" + startTimestamp
                    + ") must be lower than endTimestamp (" + endTimestamp + ")");
        }

        // obtain DB Read access

        return obtainCheckpointsData(readAccess, startTimestamp, endTimestamp, checkpointSummaryId,
                loadQueueId, testcaseId);
    }

    /**
     * Get aggregated information about particular statistic description for a given date range<br>
     * In other words you can use this method to see how a given metric is being executed for some time interval.<br>
     * For example you can see information for CPU Usage of some machine, while some operation was performing on that machine
     *
     * @param statisticTypeId     - the ID of the checkpoint. You can use {@link AtsDbReader#getCheckpointSummaries(int)} to obtain this value
     * @param testcaseId          - the ID of the test case, where the action you are interested in was executed. You can use {@link AtsDbReader#getTestcases(int)} to obtain this value
     * @param machineId           - the ID of the machine, where some monitoring was started during the given test case. You can use {@link AtsDbReader#getMachines(int, int)} to obtain such value or pass {@link AtsDbReader#ALL_MACHINES} to obtain data from all of the machines involved in some kind of monitoring
     * @param startTimestamp      - the start time stamp in milliseconds
     * @param endTimestamp        - the end time stamp in milliseconds
     * @return list of {@link StatisticDescription}s
     * @throws IllegalArgumentException if any of the arguments is invalid
     * @throws DatabaseAccessException  if an error occurred while obtaining data from the DB
     * @throws IllegalStateException    if {@link ActiveDbAppender} was not attached (not found in log4j2.xml configuration file)
     */
    @PublicAtsApi
    public StatisticDescription
    getStatisticsDescriptionForDateRange(int statisticTypeId, int testcaseId, int machineId,
                                         long startTimestamp,
                                         long endTimestamp) throws DatabaseAccessException {

        // check input arguments
        if (statisticTypeId < 0) {
            throw new IllegalArgumentException("statistic type ID is invalid (" + statisticTypeId + ")");
        }

        if (testcaseId < 0) {
            throw new IllegalArgumentException("testcase ID is invalid (" + testcaseId + ")");
        }

        if (machineId < 0) {
            if (machineId != ALL_MACHINES) {
                throw new IllegalArgumentException("machine ID is invalid (" + machineId + ")");
            }
        }

        if (startTimestamp < 0) {
            throw new IllegalArgumentException("startTimestamp is invalid (" + startTimestamp + ")");
        }

        if (endTimestamp < 0) {
            throw new IllegalArgumentException("endTimestamp is invalid (" + endTimestamp + ")");
        }

        if (isDateAfter(startTimestamp, endTimestamp)) {
            throw new IllegalArgumentException("startTimestamp (" + startTimestamp
                    + ") must be lower than endTimestamp (" + endTimestamp + ")");
        }

        return obtainSystemStatisticsData(readAccess, statisticTypeId, testcaseId, machineId, startTimestamp,
                endTimestamp);

    }

    private static String constructSQLServerNotFoundExceptionMessage(Exception mssqlException,
                                                                     Exception pgsqlException, String host, int port,
                                                                     String db, String user, String password,
                                                                     Map<String, Object> customProperties) {

        StringBuilder sb = new StringBuilder();
        sb.append("Could not connect to ATS Log database '" + host + ":" + port + "/" + db
                + ", using username:password '" + user + ":" + password + "'");

        if (customProperties != null && !customProperties.isEmpty()) {
            sb.append(" with custom properties provided.");
        }

        sb.append(ExceptionUtils.getExceptionMsg(mssqlException, "\nSQL Server exception is"))
                .append(ExceptionUtils.getExceptionMsg(mssqlException, "\nPgSQL Server exception is"));
        return sb.toString();
    }

    private List<LoadQueue> getLoadQueues(String whereClause) throws DatabaseAccessException {

        return readAccess.getLoadQueues(whereClause, "loadQueueId", true, 0);
    }

    private List<Testcase> getTestcases(String whereClause) throws DatabaseAccessException {

        return readAccess.getTestcases(0, Integer.MAX_VALUE, "WHERE " + whereClause,
                "testcaseId",
                true, 0);
    }

    /**
     * Get MACHINEs from ATS Log DB
     *
     * @param whereClause - the where clause. Note that the WHERE keyword is mandatory
     * @return list of {@link Machine}s
     * @throws DatabaseAccessException - if error occurred
     */
    private List<Machine> getMachines(String whereClause) throws DatabaseAccessException {

        return readAccess.getMachines(whereClause);

    }

    private List<StatisticDescription>
    getStatisticDescriptions(String whereClause) throws DatabaseAccessException {

        return readAccess.getSystemStatisticDescriptions(0, whereClause,
                null, 0, false);

    }

    private StatisticDescription obtainSystemStatisticsData(IDbReadAccess readAccess, int statisticTypeId,
                                                            int testcaseId, int machineId, long startTimestamp,
                                                            long endTimestamp) throws DatabaseAccessException {

        String whereClause = "WHERE ss.testcaseId = " + testcaseId + " AND ss.statsTypeId = " + statisticTypeId;

        if (machineId != ALL_MACHINES) {
            // user specified a single machine
            whereClause += " AND ss.machineId = " + machineId;
        }

        List<StatisticDescription> statisticDescriptions = readAccess.getSystemStatisticDescriptions(0, whereClause,
                null,
                0,
                false);

        if (statisticDescriptions == null || statisticDescriptions.isEmpty() || statisticDescriptions.get(0) == null) {
            // should we log information about the log database as well ?!?
            log.warn("No system statistics description found for ID '" + statisticTypeId
                    + "' in testcase with ID '" + testcaseId + "' for machine with ID '" + machineId + "'");
        }

        if (statisticDescriptions.size() > 1) {
            throw new IllegalStateException("Query returned more than one system statistic description!");
        }

        StatisticDescription description = statisticDescriptions.get(0);

        StringBuilder whereClauseBuilder = new StringBuilder();

        String statisticSQLTimeStamp = null;
        if (!isPGSQLServer) {
            statisticSQLTimeStamp = "CAST(Datediff(s, '1970-01-01 00:00:00', timestamp) AS BIGINT)*1000";
        } else {
            statisticSQLTimeStamp = "(CAST(EXTRACT(EPOCH FROM timestamp - CAST( '1970-01-01 00:00:00' AS TIMESTAMP))*1000 AS BIGINT))";
        }

        whereClauseBuilder.append(statisticSQLTimeStamp + " >= " + startTimestamp + " AND "
                + statisticSQLTimeStamp + " <= " + endTimestamp);

        String machineIds = "";
        if (machineId == ALL_MACHINES) {
            List<Machine> machines = this.getMachines(testcaseId, statisticTypeId);
            if (machines == null || machines.isEmpty()) {
                throw new RuntimeException("No machines information found in ATS Log DB");
            }
            for (Machine machine : machines) {
                machineIds += machine.machineId + ",";
            }
            // assume that the string is not empty
            machineIds = machineIds.substring(0, machineIds.length() - 1); // remove trailing comma
        } else {
            machineIds = machineId + "";
        }

        List<Statistic> statistics = readAccess.getSystemStatistics(0, testcaseId + "", machineIds + "",
                statisticTypeId + "", whereClauseBuilder.toString(),
                0, false);

        return createSystemStatisticDescription(statistics, description.statisticTypeId, description.statisticName,
                description.unit, description.params, description.parent,
                description.internalName, description.testcaseId,
                description.testcaseName, description.machineId,
                description.machineName, startTimestamp,
                endTimestamp);
    }

    private StatisticDescription
    createSystemStatisticDescription(List<Statistic> statistics, int statisticTypeId, String statisticName,
                                     String unit, String params, String parentName, String internalName,
                                     int testcaseId, String testcaseName, int machineId, String machineName,
                                     long startTimestamp, long endTimestamp) {

        StatisticDescription statisticDescription = new StatisticDescription();
        statisticDescription.setTimeOffset(0);

        statisticDescription.setStartTimestamp(startTimestamp);
        statisticDescription.setEndTimestamp(endTimestamp);

        statisticDescription.statisticTypeId = statisticTypeId;
        statisticDescription.statisticName = statisticName;
        statisticDescription.unit = unit;
        statisticDescription.params = params;
        statisticDescription.parent = parentName;
        statisticDescription.internalName = internalName;

        statisticDescription.testcaseId = testcaseId;
        statisticDescription.testcaseName = testcaseName;
        statisticDescription.machineId = machineId;
        statisticDescription.machineName = machineName;

        statisticDescription.minValue = Integer.MAX_VALUE;
        statisticDescription.maxValue = -1;
        statisticDescription.avgValue = 0;

        if (statistics != null && !statistics.isEmpty()) {

            Map<Long, Double> avgValues = new HashMap<Long, Double>();

            long statisticsCountUntilNow = 0;
            double totalStatisticValueUntilNow = 0;

            statisticDescription.numberMeasurements = statistics.size();

            for (Statistic statistic : statistics) {

                statisticDescription.minValue = Math.min(statisticDescription.minValue, statistic.value);
                statisticDescription.maxValue = Math.max(statisticDescription.maxValue, statistic.value);

                // temporary save AVG values
                if (totalStatisticValueUntilNow + statistic.value >= Double.MAX_VALUE) {
                    // overflow will occur, so take care
                    /* up until now we have statisticsCountUntilNow statistics count, so we do the following:
                     * 1. save the number of statistics, which is statisticsCountUntilNow
                     * 2. save the total statistic value until now
                     * */
                    avgValues.put(statisticsCountUntilNow, totalStatisticValueUntilNow);
                    totalStatisticValueUntilNow = 0;
                }

                // continue adding values, w/o worrying about overflow
                totalStatisticValueUntilNow += statistic.value;
                statisticsCountUntilNow++;
            }

            // set the last result, or, if not overflow occurred, the only result
            avgValues.put(statisticsCountUntilNow, totalStatisticValueUntilNow);

            if (avgValues != null && !avgValues.isEmpty()) {

                // now calculate AVG values at last
                for (Map.Entry<Long, Double> entry : avgValues.entrySet()) {
                    long statisticsCount = entry.getKey();
                    double value = entry.getValue();

                    double avgValue = value / statisticsCount;

                    double weight = ((double) statisticsCount) / ((double) statisticDescription.numberMeasurements);

                    // could an overflow occur here?!?
                    statisticDescription.avgValue += (avgValue * weight);
                }

            }

        } else {
            statisticDescription.minValue = statisticDescription.maxValue = statisticDescription.avgValue = -1;
        }

        return statisticDescription;
    }

    private CheckpointSummary obtainCheckpointsData(IDbReadAccess readAccess,
                                                    long startTimestamp,
                                                    long endTimestamp,
                                                    int checkpointSummaryId, int loadQueueId,
                                                    int testcaseId) throws DatabaseAccessException {

        String whereClause = "checkpointSummaryId = " + checkpointSummaryId + " AND " + " loadQueueId = "
                + loadQueueId;
        List<CheckpointSummary> checkpointSummaries = readAccess.getCheckpointsSummary(whereClause,
                "checkpointSummaryId", false);

        if (checkpointSummaries == null || checkpointSummaries.isEmpty() || checkpointSummaries.get(0) == null) {
            // should we log information about the log database as well ?!?
            log.warn("No checkpoint summary found with ID '" + checkpointSummaryId
                    + "' in loadQueue with ID '" + loadQueueId + "'");
        }

        if (checkpointSummaries.size() > 1) {
            throw new IllegalStateException("Query returned more than one checkpoint summary!");
        }

        CheckpointSummary checkpointSummary = checkpointSummaries.get(0);

        StringBuilder whereClauseBuilder = new StringBuilder();

        String insertCheckpointSQLTimeStamp = null;
        if (!isPGSQLServer) {
            insertCheckpointSQLTimeStamp = "CAST(Datediff(s, '1970-01-01 00:00:00', ch.endTime) AS BIGINT)*1000";
        } else {
            insertCheckpointSQLTimeStamp = "(CAST(EXTRACT(EPOCH FROM ch.endTime - CAST( '1970-01-01 00:00:00' AS TIMESTAMP))*1000 AS BIGINT))";
        }

        whereClauseBuilder.append(insertCheckpointSQLTimeStamp + " >= " + startTimestamp + " AND "
                + insertCheckpointSQLTimeStamp + " <= " + endTimestamp);

        // get each checkpoint for that time stamp
        // Note that there maybe no checkpoints for that time stamp
        List<Checkpoint> checkpoints = readAccess.getCheckpoints(testcaseId + "", loadQueueId, checkpointSummary.name,
                whereClauseBuilder.toString(),
                0, false);

        return createCheckpointSummary(checkpoints, testcaseId, loadQueueId,
                checkpointSummary.checkpointSummaryId,
                checkpointSummary.name,
                checkpointSummary.transferRateUnit,
                startTimestamp, endTimestamp);
    }

    private CheckpointSummary createCheckpointSummary(List<Checkpoint> checkpoints, int testcaseId,
                                                      int loadQueueId,
                                                      int checkpointSummaryId,
                                                      String checkpointSummaryName, String transferRateUnit,
                                                      long startTimestamp,
                                                      long endTimestamp) {

        CheckpointSummary summary = new CheckpointSummary();

        summary.setTimeOffset(0);

        summary.checkpointSummaryId = checkpointSummaryId;
        summary.name = checkpointSummaryName;
        summary.loadQueueId = loadQueueId;
        summary.setStartTimestamp(startTimestamp);
        summary.setEndTimestamp(endTimestamp);
        summary.transferRateUnit = transferRateUnit;

        if (checkpoints != null && checkpoints.size() > 0) {

            summary.minResponseTime = Integer.MAX_VALUE;
            summary.maxResponseTime = -1;

            Map<Long, Double[]> avgValues = new HashMap<Long, Double[]>();

            long checkpointsUntilNow = 0;
            double totalResponseTimeUntilNow = 0;
            double totalTransferRateUntilNow = 0;

            summary.numTotal = checkpoints.size();
            for (Checkpoint checkpoint : checkpoints) {
                if (checkpoint.result == CheckpointResult.PASSED.toInt()) {
                    summary.numPassed++;
                } else if (checkpoint.result == CheckpointResult.FAILED.toInt()) {
                    summary.numFailed++;
                    // do not use this checkpoint values for calculation of min, max, avr
                    continue;
                } else if (checkpoint.result == CheckpointResult.RUNNING.toInt()) {
                    summary.numRunning++;
                    // do not use this checkpoint values for calculation of min, max, avr
                    continue;
                } else {
                    // something terrible happened
                    throw new RuntimeException("Checkpoint result '" + checkpoint.result
                            + "' is not supported. Checkpoint's ID is '" + checkpoint.checkpointId
                            + "'");
                }

                // only PASSED checkpoints can be here

                // update MIN, MAX response times
                summary.minResponseTime = Math.min(summary.minResponseTime, checkpoint.responseTime);
                summary.maxResponseTime = Math.max(summary.maxResponseTime, checkpoint.responseTime);
                // update MIN, MAX transfer rates
                summary.minTransferRate = Math.min(summary.minTransferRate, checkpoint.transferRate);
                summary.maxTransferRate = Math.max(summary.maxTransferRate, checkpoint.transferRate);

                // temporary save AVG values
                // keep them in sync (response time, transfer rate), e.g. if one is about to overflow, the other must be divided as well
                if (totalResponseTimeUntilNow + checkpoint.responseTime >= Double.MAX_VALUE
                        || totalTransferRateUntilNow + checkpoint.transferRate >= Double.MAX_VALUE) {
                    // overflow will occur, so take care
                    /* up until now we have checkpointsUntilNow checkpoints count, so we do the following:
                     * 1. save the number of checkpoints, which is checkpointsUntilNow
                     * 2. save the total response time until now
                     * 3. save the total transfer rate until now
                     * */
                    avgValues.put(checkpointsUntilNow,
                            new Double[]{totalResponseTimeUntilNow, totalTransferRateUntilNow});
                    checkpointsUntilNow = 0;
                }

                // continue adding values, w/o worrying about overflow
                totalResponseTimeUntilNow += checkpoint.responseTime;
                totalTransferRateUntilNow += checkpoint.transferRate;
                checkpointsUntilNow++;
            }

            // set the last result, or, if not overflow occurred, the only result
            avgValues.put(checkpointsUntilNow,
                    new Double[]{totalResponseTimeUntilNow, totalTransferRateUntilNow});

            if (avgValues != null && !avgValues.isEmpty()) {
                // now calculate AVG values at last
                for (Map.Entry<Long, Double[]> entry : avgValues.entrySet()) {
                    long checkpointsCount = entry.getKey();
                    double responseTimes = entry.getValue()[0];
                    double transferRates = entry.getValue()[1];

                    double avrResponseTime = responseTimes / checkpointsCount;
                    double avrTransferRates = transferRates / checkpointsCount;

                    double weight = ((double) checkpointsCount) / ((double) summary.numPassed);

                    // could an overflow occur here?!?
                    summary.avgResponseTime += (avrResponseTime * weight);
                    summary.avgTransferRate += (avrTransferRates * weight);
                }
            }

        } else {
            summary.minResponseTime = summary.maxResponseTime = (int) (summary.avgResponseTime = summary.minTransferRate = summary.maxTransferRate = summary.avgTransferRate = -1);
        }

        return summary;
    }

    private String constructGetRunsWhereClause(boolean isPGSQLServer, long startTimestamp, long endTimestamp,
                                               String runName, String productName, String versionName,
                                               String buildName, String osName, String userNote) {

        // TODO check it
        StringBuilder sb = new StringBuilder();
        sb.append("WHERE 1=1");

        if (startTimestamp > 0) {
            String startTimestampSQL = null;
            if(!isPGSQLServer) {
                startTimestampSQL = "CAST(Datediff(s, '1970-01-01 00:00:00', dateStart) AS BIGINT)*1000";
            } else {
                startTimestampSQL = "(CAST(EXTRACT(EPOCH FROM dateStart - CAST( '1970-01-01 00:00:00' AS TIMESTAMP))*1000 AS BIGINT))";
            }
            
            sb.append(" AND " + startTimestampSQL + " >= " + startTimestamp);
        }
        if (endTimestamp > 0) {
            String endTimestampSQL = null;
            if(!isPGSQLServer) {
                endTimestampSQL = "CAST(Datediff(s, '1970-01-01 00:00:00', dateEnd) AS BIGINT)*1000";
            } else {
                endTimestampSQL = "(CAST(EXTRACT(EPOCH FROM dateEnd - CAST( '1970-01-01 00:00:00' AS TIMESTAMP))*1000 AS BIGINT))";
            }
            sb.append(" AND " + endTimestampSQL + " <= " + endTimestamp);
        }

        if (!StringUtil.isNullOrEmpty(runName)) {
            sb.append(" AND runName LIKE '%" + runName + "%'");
        }

        if (!StringUtil.isNullOrEmpty(productName)) {
            sb.append(" AND productName LIKE '%" + productName + "%'");
        }

        if (!StringUtil.isNullOrEmpty(versionName)) {
            sb.append(" AND versionName LIKE '%" + versionName + "%'");
        }

        if (!StringUtil.isNullOrEmpty(buildName)) {
            sb.append(" AND buildName LIKE '%" + buildName + "%'");
        }

        if (!StringUtil.isNullOrEmpty(osName)) {
            sb.append(" AND osName LIKE '%" + osName + "%'");
        }

        if (!StringUtil.isNullOrEmpty(userNote)) {
            sb.append(" AND userNote LIKE '%" + userNote + "%'");
        }
        return sb.toString();
    }

    /**
     * Check if one date is AFTER another
     *
     * @param timestampOne
     * @param timestampTwo
     * @return true if timestamOne is after timestampTwo, false otherwise
     */
    private boolean isDateAfter(long timestampOne, long timestampTwo) {

        Date dateOne = new Date(timestampOne);
        Date dateTwo = new Date(timestampTwo);
        return dateOne.after(dateTwo);
    }

}
