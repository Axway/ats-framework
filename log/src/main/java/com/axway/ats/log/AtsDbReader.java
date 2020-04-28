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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.log.appenders.ActiveDbAppender;
import com.axway.ats.log.autodb.entities.Checkpoint;
import com.axway.ats.log.autodb.entities.CheckpointSummary;
import com.axway.ats.log.autodb.entities.LoadQueue;
import com.axway.ats.log.autodb.entities.Machine;
import com.axway.ats.log.autodb.entities.Statistic;
import com.axway.ats.log.autodb.entities.StatisticDescription;
import com.axway.ats.log.autodb.entities.Suite;
import com.axway.ats.log.autodb.entities.Testcase;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.log.autodb.io.PGDbReadAccess;
import com.axway.ats.log.autodb.model.IDbReadAccess;
import com.axway.ats.log.model.CheckpointResult;

/**
 * TODO
 * 1. Returned time stamps must not be in UTC, but local time
 * 2. Add possibility to obtain checkpoints for one, two or more machines
 * 3. Add method for obtaining scenarios
 * 4. Add method for obtaining testcase/scenario/run metadata
 * 5. Add method for obtaining testcase/suite/run messages
 * 
 * */

/**
 * Utility class for obtaining test data from an ATS Log DB<br>
 * */
@PublicAtsApi
public class AtsDbReader {

    private static Logger log = Logger.getLogger(AtsDbLogger.class);

    /**
     * Get all {@link Suite}s from RUN.<br>Note that the time stamps for start and end date will be in UTC
     * @param runId - the parent run ID 
     * @return list of {@link Suite}s
     * @throws DatabaseAccessException - if error occurred while obtaining data from the DB
     * */
    @PublicAtsApi
    public static List<Suite> getSuites( int runId ) throws DatabaseAccessException {

        IDbReadAccess readAccess = obtainDbReadAccessFromActiveDbAppender();
        return readAccess.getSuites(0, Integer.MAX_VALUE, "WHERE runId = " + runId, "suiteId", true, 0);
    }

    /**
     * Get all {@link Testcase}s from SUITE.<br>Note that the time stamps for start and end date will be in UTC
     * @param suiteId - the parent suite ID 
     * @return list of {@link Testcase}s
     * @throws DatabaseAccessException - if error occurred while obtaining data from the DB
     * */
    @PublicAtsApi
    public static List<Testcase> getTestcases( int suiteId ) throws DatabaseAccessException {

        IDbReadAccess readAccess = obtainDbReadAccessFromActiveDbAppender();
        return readAccess.getTestcases(0, Integer.MAX_VALUE, "WHERE suiteId = " + suiteId,
                                       "testcaseId",
                                       true, 0);
    }

    /**
     * Get all {@link LoadQueue}s from TESTCASE.<br>Note that the time stamps for start and end date will be in UTC
     * @param testcaseId - the parent test case ID 
     * @return list of {@link LoadQueue}s
     * @throws DatabaseAccessException - if error occurred while obtaining data from the DB
     * */
    @PublicAtsApi
    public static List<LoadQueue> getLoadQueues( int testcaseId ) throws DatabaseAccessException {

        IDbReadAccess readAccess = obtainDbReadAccessFromActiveDbAppender();

        return readAccess.getLoadQueues("testcaseId = " + testcaseId,
                                        "loadQueueId",
                                        true, 0);
    }

    /**
     * Get all {@link CheckpointSummary}s from TESTCASE.
     * @param loadQueueId - the parent loadQueue ID 
     * @return list of {@link CheckpointSummary}s
     * @throws DatabaseAccessException - if error occurred while obtaining data from the DB
     * */
    @PublicAtsApi
    public static List<CheckpointSummary> getCheckpointSummaries( int loadQueueId ) throws DatabaseAccessException {

        IDbReadAccess readAccess = obtainDbReadAccessFromActiveDbAppender();

        return readAccess.getCheckpointsSummary("loadQueueId = "
                                                + loadQueueId,
                                                "checkpointSummaryId",
                                                true);
    }

    /**
     * Get all machines from the ATS Log DB<br>
     * Note that you should not expect every machine to be used in each RUN/SUITE/TESTCASE/etc, because if a machine was used in some test case for example,
     * and that test case is deleted, the machine continues to exist in the DB. So keep that in mind, when using data, returned by this method
     * @return list of {@link Machine}s
     * @throws DatabaseAccessException - if an error occurred while obtaining data from DB
     * */
    @PublicAtsApi
    public static List<Machine> getMachines() throws DatabaseAccessException {

        String whereClause = "WHERE 1=1";
        return getMachines(whereClause);
    }

    /**
     * Get all machines, that were monitoring statistics, referenced by the statisticTypeId, and which were configured to monitor via the given test case<br>
     * Note that you should not expect every machine to be used in each RUN/SUITE/TESTCASE/etc, because if a machine was used in some test case for example,
     * and that test case is deleted, the machine continues to exist in the DB. So keep that in mind, when using data, returned by this method
     * @return list of {@link Machine}s
     * @throws DatabaseAccessException - if an error occurred while obtaining data from DB
     * */
    @PublicAtsApi
    public static List<Machine> getMachines( int testcaseId, int statisticTypeId ) throws DatabaseAccessException {

        boolean isPGSQLServer = AtsDbReader.obtainDbReadAccessFromActiveDbAppender() instanceof PGDbReadAccess;

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
     * @param testcaseId - the test case ID where some sort of monitoring was started
     * @return list of {@link StatisticDescription}s
     * @throws DatabaseAccessException if error occurred with working with the ATS Log DB
     * */
    @PublicAtsApi
    public static List<StatisticDescription> getStatisticDescriptions( int testcaseId ) throws DatabaseAccessException {

        String whereClause = "WHERE ss.testcaseId = " + testcaseId;
        return getStatisticDescriptions(whereClause);

    }

    /**
     * Get all monitoring statistic descriptions from single machine from a test case
     * @param testcaseId - the test case ID where some sort of monitoring was started
     * @param machineId - the machine ID where monitoring was started
     * @return list of {@link StatisticDescription}s
     * @throws DatabaseAccessException if error occurred with working with the ATS Log DB
     * */
    @PublicAtsApi
    public static List<StatisticDescription> getStatisticDescriptions( int testcaseId,
                                                                       int machineId ) throws DatabaseAccessException {

        String whereClause = "WHERE ss.testcaseId = " + testcaseId + " AND ss.machineId = " + machineId;
        return getStatisticDescriptions(whereClause);

    }

    /**
     * Get aggregated information about particular checkpoint summary for a given date range<br>
     * In other words you can use this method to see how a given action is being executed for some time interval.<br>
     * For example you can see information for some FTPS file transfer (upload), like response time and transfer rate for the first 15 minutes of a given load queue
     * 
     * @param checkpointSummaryId - the ID of the checkpoint. You can use {@link AtsDbReader#getCheckpointSummaries(int)} to obtain this value
     * @param loadQueueId - the ID of the load queue, where the action you are interested in was executed. You can use {@link AtsDbReader#getLoadQueues(int)} to obtain this value
     * @param testcaseId - the ID of the test case, where the action you are interested in was executed. You can use {@link AtsDbReader#getTestcases(int)} to obtain this value
     * @param startTimestamp - the start time stamp in milliseconds in your local time zone
     * @param endTimestamp - the end time stamp in milliseconds in your local time zone
     * @return list of {@link CheckpointSummary}s
     * @throws IllegalArgumentException if any of the arguments is invalid
     * @throws DatabaseAccessException if an error occurred while obtaining data from the DB
     * @throws IllegalStateException if {@link ActiveDbAppender} was not attached (not found in log4j.xml configuration file)
     * */
    @PublicAtsApi
    public static CheckpointSummary
            getCheckpointSummaryForDateRange( int checkpointSummaryId, int loadQueueId, int testcaseId,
                                              long startTimestamp,
                                              long endTimestamp ) throws DatabaseAccessException {

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

        // convert to UTC
        //startTimestamp = convertToUTC(startTimestamp);
        //endTimestamp = convertToUTC(endTimestamp);

        if (isDateAfter(startTimestamp, endTimestamp)) {
            throw new IllegalArgumentException("startTimestamp (" + startTimestamp
                                               + ") must be lower than endTimestamp (" + endTimestamp + ")");
        }

        // obtain DB Read access
        IDbReadAccess readAccess = obtainDbReadAccessFromActiveDbAppender();

        return obtainCheckpointsData(readAccess, startTimestamp, endTimestamp, checkpointSummaryId,
                                     loadQueueId, testcaseId);
    }

    /**
     * Get aggregated information about particular statistic description for a given date range<br>
     * In other words you can use this method to see how a given metric is being executed for some time interval.<br>
     * For example you can see information for CPU Usage of some machine, while some operation was performing on that machine
     * 
     * @param checkpointSummaryId - the ID of the checkpoint. You can use {@link AtsDbReader#getCheckpointSummaries(int)} to obtain this value
     * @param testcaseId - the ID of the test case, where the action you are interested in was executed. You can use {@link AtsDbReader#getTestcases(int)} to obtain this value
     * @param machineId - the ID of the machine, where some monitoring was started during the given test case. You can use {@link AtsDbReader#getMachines(int, int)} to obtain such value
     * @param startTimestamp - the start time stamp in milliseconds in your local time zone
     * @param endTimestamp - the end time stamp in milliseconds in your local time zone
     * @return list of {@link StatisticDescription}s
     * @throws IllegalArgumentException if any of the arguments is invalid
     * @throws DatabaseAccessException if an error occurred while obtaining data from the DB
     * @throws IllegalStateException if {@link ActiveDbAppender} was not attached (not found in log4j.xml configuration file)
     * */
    @PublicAtsApi
    public static StatisticDescription
            getStatisticsDescriptionForDateRange( int statisticTypeId, int testcaseId, int machineId,
                                                  long startTimestamp,
                                                  long endTimestamp ) throws DatabaseAccessException {

        // check input arguments
        if (statisticTypeId < 0) {
            throw new IllegalArgumentException("statistic type ID is invalid (" + statisticTypeId + ")");
        }

        if (testcaseId < 0) {
            throw new IllegalArgumentException("testcase ID is invalid (" + testcaseId + ")");
        }

        if (machineId < 0) {
            throw new IllegalArgumentException("machine ID is invalid (" + machineId + ")");
        }

        if (startTimestamp < 0) {
            throw new IllegalArgumentException("startTimestamp is invalid (" + startTimestamp + ")");
        }

        if (endTimestamp < 0) {
            throw new IllegalArgumentException("endTimestamp is invalid (" + endTimestamp + ")");
        }

        // convert to UTC
        //startTimestamp = convertToUTC(startTimestamp);
        //endTimestamp = convertToUTC(endTimestamp);

        if (isDateAfter(startTimestamp, endTimestamp)) {
            throw new IllegalArgumentException("startTimestamp (" + startTimestamp
                                               + ") must be lower than endTimestamp (" + endTimestamp + ")");
        }

        // obtain DB Read access
        IDbReadAccess readAccess = obtainDbReadAccessFromActiveDbAppender();

        return obtainSystemStatisticsData(readAccess, statisticTypeId, testcaseId, machineId, startTimestamp,
                                          endTimestamp);

    }

    private static List<Machine> getMachines( String whereClause ) throws DatabaseAccessException {

        IDbReadAccess dbReadAccess = obtainDbReadAccessFromActiveDbAppender();

        return dbReadAccess.getMachines(whereClause);

    }

    private static List<StatisticDescription>
            getStatisticDescriptions( String whereClause ) throws DatabaseAccessException {

        IDbReadAccess readAccess = obtainDbReadAccessFromActiveDbAppender();

        return readAccess.getSystemStatisticDescriptions(0, whereClause,
                                                         null, 0, false);

    }

    private static StatisticDescription obtainSystemStatisticsData( IDbReadAccess readAccess, int statisticTypeId,
                                                                    int testcaseId, int machineId, long startTimestamp,
                                                                    long endTimestamp ) throws DatabaseAccessException {

        String whereClause = "WHERE ss.testcaseId = " + testcaseId + " AND ss.statsTypeId = " + statisticTypeId
                             + " AND ss.machineId = " + machineId;
        List<StatisticDescription> statisticDescriptions = readAccess.getSystemStatisticDescriptions(0, whereClause,
                                                                                                     null, 0, false);

        if (statisticDescriptions == null || statisticDescriptions.isEmpty() || statisticDescriptions.get(0) == null) {
            // TODO should we log information about the log database as well ?!?
            log.warn("No system statistics description found for ID '" + statisticTypeId
                     + "' in testcase with ID '" + testcaseId + "' for machine with ID '" + machineId + "'");
        }

        if (statisticDescriptions.size() > 1) {
            throw new IllegalStateException("Query returned more than one system statistic description!");
        }

        StatisticDescription description = statisticDescriptions.get(0);

        StringBuilder whereClauseBuilder = new StringBuilder();

        boolean isPGSQLServer = readAccess instanceof PGDbReadAccess;
        String statisticSQLTimeStamp = null;
        if (!isPGSQLServer) {
            statisticSQLTimeStamp = "CAST(Datediff(s, '1970-01-01 00:00:00', timestamp) AS BIGINT)*1000";
        } else {
            statisticSQLTimeStamp = "(CAST(EXTRACT(EPOCH FROM timestamp - CAST( '1970-01-01 00:00:00' AS TIMESTAMP))*1000 AS BIGINT))";
        }

        whereClauseBuilder.append(statisticSQLTimeStamp + " >= " + startTimestamp + " AND "
                                  + statisticSQLTimeStamp + " <= " + endTimestamp);

        List<Statistic> statistics = readAccess.getSystemStatistics(0, testcaseId + "", machineId + "",
                                                                    statisticTypeId + "", whereClauseBuilder.toString(),
                                                                    0, false);

        return createSystemStatisticDescription(statistics, description.statisticTypeId, description.statisticName,
                                                description.unit, description.params, description.parent,
                                                description.internalName, description.testcaseId,
                                                description.testcaseName, description.machineId,
                                                description.machineName, startTimestamp,
                                                endTimestamp);
    }

    private static StatisticDescription
            createSystemStatisticDescription( List<Statistic> statistics, int statisticTypeId, String statisticName,
                                              String unit, String params, String parentName, String internalName,
                                              int testcaseId, String testcaseName, int machineId, String machineName,
                                              long startTimestamp, long endTimestamp ) {

        StatisticDescription statisticDescription = new StatisticDescription();
        statisticDescription.setEndTimestamp(startTimestamp);
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

    /**
     * Convert to UTC
     * @param timestamp - the time stamp. Can be both in local or already in UTC.
     * */
    private static long convertToUTC( long timestamp ) {

        return timestamp - TimeZone.getDefault().getOffset(timestamp);
    }

    private static CheckpointSummary obtainCheckpointsData( IDbReadAccess readAccess,
                                                            long startTimestamp,
                                                            long endTimestamp,
                                                            int checkpointSummaryId, int loadQueueId,
                                                            int testcaseId ) throws DatabaseAccessException {

        String whereClause = "checkpointSummaryId = " + checkpointSummaryId + " AND " + " loadQueueId = "
                             + loadQueueId;
        List<CheckpointSummary> checkpointSummaries = readAccess.getCheckpointsSummary(whereClause,
                                                                                       "checkpointSummaryId", false);

        if (checkpointSummaries == null || checkpointSummaries.isEmpty() || checkpointSummaries.get(0) == null) {
            // TODO should we log information about the log database as well ?!?
            log.warn("No checkpoint summary found with ID '" + checkpointSummaryId
                     + "' in loadQueue with ID '" + loadQueueId + "'");
        }

        if (checkpointSummaries.size() > 1) {
            throw new IllegalStateException("Query returned more than one checkpoint summary!");
        }

        CheckpointSummary checkpointSummary = checkpointSummaries.get(0);

        StringBuilder whereClauseBuilder = new StringBuilder();

        boolean isPGSQLServer = readAccess instanceof PGDbReadAccess;
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

    private static CheckpointSummary createCheckpointSummary( List<Checkpoint> checkpoints, int testcaseId,
                                                              int loadQueueId,
                                                              int checkpointSummaryId,
                                                              String checkpointSummaryName, String transferRateUnit,
                                                              long startTimestamp,
                                                              long endTimestamp ) {

        CheckpointSummary summary = new CheckpointSummary();

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
                                  new Double[]{ totalResponseTimeUntilNow, totalTransferRateUntilNow });
                    checkpointsUntilNow = 0;
                }

                // continue adding values, w/o worrying about overflow
                totalResponseTimeUntilNow += checkpoint.responseTime;
                totalTransferRateUntilNow += checkpoint.transferRate;
                checkpointsUntilNow++;
            }

            // set the last result, or, if not overflow occurred, the only result
            avgValues.put(checkpointsUntilNow,
                          new Double[]{ totalResponseTimeUntilNow, totalTransferRateUntilNow });

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

    /**
     * Check if one date is AFTER another
     * @param timestampOne
     * @param timestampTwo
     * @return true if timestamOne is after timestampTwo, false otherwise
     * */
    private static boolean isDateAfter( long timestampOne, long timestampTwo ) {

        Date dateOne = new Date(timestampOne);
        Date dateTwo = new Date(timestampTwo);
        return dateOne.after(dateTwo);
    }

    private static IDbReadAccess obtainDbReadAccessFromActiveDbAppender() throws DatabaseAccessException {

        if (!ActiveDbAppender.isAttached) {
            throw new IllegalStateException(ActiveDbAppender.class.getName()
                                            + " not attached! Check the ATS documetation for information about enabling DB logging and try again");
        }

        return ActiveDbAppender.getCurrentInstance().obtainDbReadAccessObject();
    }

}
