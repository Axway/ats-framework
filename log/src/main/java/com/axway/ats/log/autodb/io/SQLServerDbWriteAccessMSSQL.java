/*
 * Copyright 2019 Axway Software
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.DbUtils;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.autodb.entities.CheckpointSummary;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.log.model.CheckpointLogLevel;
import com.axway.ats.log.model.CheckpointResult;
import com.microsoft.sqlserver.jdbc.SQLServerBulkCSVFileRecord;
import com.microsoft.sqlserver.jdbc.SQLServerBulkCopy;
import com.microsoft.sqlserver.jdbc.SQLServerBulkCopyOptions;

public class SQLServerDbWriteAccessMSSQL extends SQLServerDbWriteAccess {

    private DbCheckpointsCache dbCheckpointsCache;

    public SQLServerDbWriteAccessMSSQL( DbConnection dbConnection,
                                        boolean isBatchMode ) throws DatabaseAccessException {

        super(dbConnection, isBatchMode);
        if (isBatchMode) {
            this.dbCheckpointsCache = new DbCheckpointsCache(this.getConnection());
            setMaxNumberOfCachedEvents(this.chunkSize);
        }

    }

    @Override
    public void setMaxNumberOfCachedEvents( int maxNumberOfCachedEvents ) {

        super.setMaxNumberOfCachedEvents(maxNumberOfCachedEvents);
        if (this.dbCheckpointsCache != null) {
            this.dbCheckpointsCache.setCacheSize(maxNumberOfCachedEvents);
        }

    }

    /**
     * Expected to be called only in batch mode. Flush any pending events
     *
     * @throws DatabaseAccessException
     */
    public void flushCache() throws DatabaseAccessException {

        super.flushCache();
        dbCheckpointsCache.flush();
    }

    @Override
    public boolean insertCheckpoint(
            String name,
            long startTimestamp,
            long responseTime,
            long transferSize,
            String transferUnit,
            int result,
            int loadQueueId,
            boolean closeConnection ) throws DatabaseAccessException {

        try {
            if (isBatchMode) {
                return this.dbCheckpointsCache.addCheckpoint(name,
                                                             startTimestamp,
                                                             responseTime,
                                                             transferSize,
                                                             transferUnit,
                                                             result,
                                                             loadQueueId);
            } else {
                return super.insertCheckpoint(name, startTimestamp, responseTime, transferSize, transferUnit, result,
                                              loadQueueId, closeConnection);
            }
        } catch (Exception e) {
            throw new DatabaseAccessException("Could not insert checkpoint to ATS log DB", e);
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    DbUtils.closeConnection(connection);
                }
            } catch (SQLException e) {
                log.error(e);
            }

        }

    }

    protected class DbCheckpointsCache {

        private Connection connection;
        private int        cacheSize                 = AbstractDbAccess.DEFAULT_CHUNK_SIZE;
        private int        numberOfCachedCheckpoints = 0;

        private long lastInsertCheckpointTimestamp = 0;

        /**
         * the maximum amount of time that the cache will be held not flushed
         * */
        private long maxCacheWaitTime = TimeUnit.SECONDS.toMillis(
                AtsSystemProperties.getPropertyAsNumber(AtsSystemProperties.LOG__MAX_CACHE_EVENTS_FLUSH_TIMEOUT, 10));

        /*
         * { loadQueueId -> { checkpointName -> checkpoint summary } }
         * */
        private Map<Integer, Map<String, CheckpointSummary>> checkpointSummaries = new HashMap<>();

        /*
         * { loadQueueId -> checkpoint insert data }
         * */
        // The checkpoint insert data is just a StringBuffer where checkpoint information is stored in CSV format
        private Map<Integer, StringBuilder> checkpointsInsertData = new HashMap<>();

        private long batchStartTime;

        public DbCheckpointsCache( Connection connection ) {

            this.connection = connection;
        }

        public void flush() throws DatabaseAccessException {

            boolean exceptionThrown = false;
            try {
                exceptionThrown = true; // precondition to detect exception without catching it
                flushCheckpoints();
                flushCheckpointSummaries();

                exceptionThrown = false; // above methods passed without throwing exception
                if (isMonitorEventsQueue) {
                    log.getLog4jLogger()
                       .info("Flushed " + numberOfCachedCheckpoints + " checkpoints in "
                             + (System.currentTimeMillis() - batchStartTime) + " ms");
                }
            } finally {
                // TODO - limit number of logged warnings
                if (exceptionThrown) {
                    log.warn("Could not flush " + numberOfCachedCheckpoints + " checkpoints (or summaries) into DB! ");
                }
                // cleanup cache
                numberOfCachedCheckpoints = 0;
                checkpointsInsertData.clear();
                lastInsertCheckpointTimestamp = 0;
                checkpointSummaries.clear();
            }
        }

        public void setCacheSize( int cacheSize ) {

            this.cacheSize = cacheSize;
        }

        public boolean addCheckpoint( String name, long startTimestamp, long responseTime, long transferSize,
                                      String transferUnit, int result,
                                      int loadQueueId ) throws DatabaseAccessException {

            try {

                if (lastInsertCheckpointTimestamp == 0) {
                    lastInsertCheckpointTimestamp = System.currentTimeMillis();
                }

                boolean flushed = false;
                if (!checkpointSummaries.containsKey(loadQueueId)) {
                    Map<String, CheckpointSummary> map = new HashMap<>();
                    CheckpointSummary checkpointSummary = new CheckpointSummary();
                    // obtain summaryID from. It is expected that populateCheckpointSummary() was invoked previously and such entry exists in DB
                    checkpointSummary.checkpointSummaryId = getCheckpointSummaryId(loadQueueId, name, transferUnit,
                                                                                   false);
                    checkpointSummary.name = name;

                    checkpointSummary.loadQueueId = loadQueueId;

                    checkpointSummary.minResponseTime = Integer.MAX_VALUE; // so later actual values will, hopefully be always lesser
                    checkpointSummary.minTransferRate = Integer.MAX_VALUE; // so later actual values will, hopefully be always lesser

                    map.put(name, checkpointSummary);
                    checkpointSummaries.put(loadQueueId, map);
                } else {
                    boolean exists = false;
                    for (CheckpointSummary summary : checkpointSummaries.get(loadQueueId).values()) {
                        if (summary.name.equals(name)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        CheckpointSummary checkpointSummary = new CheckpointSummary();
                        // obtain summaryID from. It is expected that populateCheckpointSummary() was invoked previously and such entry exists in DB
                        checkpointSummary.checkpointSummaryId = getCheckpointSummaryId(loadQueueId, name,
                                                                                       transferUnit,
                                                                                       false);
                        checkpointSummary.name = name;

                        checkpointSummary.loadQueueId = loadQueueId;

                        checkpointSummary.minResponseTime = Integer.MAX_VALUE; // so later actual values will, hopefully be always lesser
                        checkpointSummary.minTransferRate = Integer.MAX_VALUE; // so later actual values will, hopefully be always lesser
                        checkpointSummaries.get(loadQueueId).put(name, checkpointSummary);
                    }
                }

                if (!checkpointsInsertData.containsKey(loadQueueId)) {
                    checkpointsInsertData.put(loadQueueId, new StringBuilder());
                }

                // add data for the current checkpoint. This does not perform any DB I/O
                doAddCheckpoint(name, startTimestamp, responseTime, transferSize, transferUnit, result, loadQueueId);

                // update checkpoint summary for the current checkpoint. This does not perform any DB I/O
                updateCheckpointSummary(name, startTimestamp, responseTime, transferSize, transferUnit, result,
                                        loadQueueId);

                if (numberOfCachedCheckpoints >= cacheSize || isTimeToFlush()) {
                    // flush to DB
                    flush();
                    flushed = true;
                }

                return flushed;

            } catch (Exception e) {
                throw new DbException("Could not insert checkpoint", e);
            }

        }

        private int getCheckpointSummaryId( int loadQueueId, String name, String transferUnit,
                                            boolean closeConnection ) {

            int id = -1;
            PreparedStatement preparedStatement = null;
            java.sql.ResultSet rs = null;
            try {
                preparedStatement = connection.prepareStatement(
                        "SELECT checkpointSummaryId FROM tCheckpointsSummary WHERE name = '"
                        + name + "' AND loadQueueId = " + loadQueueId
                        + " ORDER BY name");
                rs = preparedStatement.executeQuery();
                if (rs.next()) {
                    id = rs.getInt(1);
                } else {
                    throw new DbException("No ID for checkpoint " + name + " from load queue " + loadQueueId
                                          + " found in DB. Did you invoke " + this.getClass().getName()
                                          + "#populateCheckpointSummary() ?");
                }

            } catch (Exception e) {
                throw new DbException("Could not get checkpoint summary ID for '" + name
                                      + "' checkpoint from load queue '" + loadQueueId + "'", e);
            } finally {
                DbUtils.closeResultSet(rs);
                if (closeConnection) {
                    DbUtils.close(connection, preparedStatement);
                } else {
                    DbUtils.closeStatement(preparedStatement);
                }
            }
            return id;
        }

        private boolean isTimeToFlush() {

            return (System.currentTimeMillis() - lastInsertCheckpointTimestamp) >= maxCacheWaitTime;
        }

        private void doAddCheckpoint( String name, long startTimestamp, long responseTime, long transferSize,
                                      String transferUnit, int result, int loadQueueId ) {

            if (SQLServerDbWriteAccess.checkpointLogLevel == CheckpointLogLevel.FULL) {
                int checkpointSummaryId = checkpointSummaries.get(loadQueueId).get(name).checkpointSummaryId;
                StringBuilder loadQueueCheckpointsInsertData = checkpointsInsertData.get(loadQueueId);

                double transferRate = 0;
                if (result == CheckpointResult.FAILED.toInt()) {
                    responseTime = 0;
                    transferSize = 0;
                }

                if (responseTime > 0) {
                    transferRate = transferSize * 1000.0 / responseTime;
                } else {
                    transferRate = 0;
                }
                long endTime = startTimestamp + responseTime;

                // since wa want the SQL Server to handle the checkpoint ID, here we pass -1 as the first argument
                loadQueueCheckpointsInsertData.append("-1," + checkpointSummaryId + "," + name + "," + responseTime
                                                      + ","
                                                      + transferRate + "," + ((StringUtils.isNullOrEmpty(transferUnit))
                                                                              ? " " // ''
                                                                              : transferUnit)
                                                      + "," + result + ","
                                                      + new Timestamp(endTime));
                loadQueueCheckpointsInsertData.append("\n");
            }

            numberOfCachedCheckpoints++;

        }

        private void updateCheckpointSummary( String name, long startTimestamp, long responseTime, long transferSize,
                                              String transferUnit, int result, int loadQueueId ) {

            CheckpointSummary checkpointSummary = checkpointSummaries.get(loadQueueId).get(name);
            if (result == CheckpointResult.FAILED.toInt()) {
                checkpointSummary.numFailed++;
            } else if (result == CheckpointResult.PASSED.toInt()) {
                checkpointSummary.numPassed++;

                float transferRate = 0;
                if (responseTime > 0) {
                    transferRate = (float) (transferSize * 1000.0 / responseTime);
                } else {
                    transferRate = 0;
                }

                checkpointSummary.minResponseTime = (int) Math.min(responseTime, checkpointSummary.minResponseTime);
                checkpointSummary.maxResponseTime = (int) Math.max(responseTime, checkpointSummary.maxResponseTime);
                // here we use the average response time as a total response time, not the average values.
                // The average value will be calculated right before flushing to the DB
                checkpointSummary.avgResponseTime += responseTime;

                checkpointSummary.minTransferRate = (float) Math.min(transferRate, checkpointSummary.minTransferRate);
                checkpointSummary.maxTransferRate = (float) Math.max(transferRate, checkpointSummary.maxTransferRate);
                // here we use the average transfer rate as a total transfer rate, not the average values.
                // The average value will be calculated right before flushing to the DB 
                checkpointSummary.avgTransferRate += transferRate;

            } else if (result == CheckpointResult.RUNNING.toInt()) {
                checkpointSummary.numRunning++;
            } else {
                throw new RuntimeException("Checkpoint result has invalid value '" + result + "'");
            }

        }

        private void flushCheckpointSummaries() throws DatabaseAccessException {

            try {
                for (Map<String, CheckpointSummary> summariesForLoadQueue : checkpointSummaries.values()) {
                    for (CheckpointSummary checkpointSummary : summariesForLoadQueue.values()) {

                        calculateAverageValues(checkpointSummary);
                        // actual flushing to the DB
                        doUpdateCheckpointSummary(checkpointSummary.checkpointSummaryId, checkpointSummary.numRunning,
                                                  checkpointSummary.numPassed, checkpointSummary.numFailed,
                                                  checkpointSummary.minResponseTime,
                                                  checkpointSummary.avgResponseTime,
                                                  checkpointSummary.maxResponseTime,
                                                  checkpointSummary.minTransferRate,
                                                  checkpointSummary.avgTransferRate,
                                                  checkpointSummary.maxTransferRate,
                                                  false);
                    }
                }
            } catch (Exception e) {
                throw new DbException("Could not flush checkpoint summaries to the logging DB", e);
            }

        }

        private void doUpdateCheckpointSummary( int checkpointSummaryId, int numRunning, int numPassed, int numFailed,
                                                int minResponseTime, double avgResponseTime, int maxResponseTime,
                                                double minTransferRate, double avgTransferRate, double maxTransferRate,
                                                boolean closeConnection ) throws DatabaseAccessException {

            CallableStatement statement = null;
            try {
                statement = connection.prepareCall(
                        "{ call sp_update_checkpoint_summary(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
                statement.setInt(1, checkpointSummaryId);
                statement.setInt(2, numPassed);
                statement.setInt(3, numFailed);
                statement.setInt(4, numRunning);
                statement.setInt(5, minResponseTime);
                statement.setInt(6, maxResponseTime);
                statement.setDouble(7, avgResponseTime);
                statement.setDouble(8, minTransferRate);
                statement.setDouble(9, maxTransferRate);
                statement.setDouble(10, avgTransferRate);
                statement.execute();
            } catch (Exception e) {
                throw new DatabaseAccessException("Could not update checkpoint summary " + checkpointSummaryId, e);
            } finally {
                if (closeConnection) {
                    DbUtils.close(connection, statement);
                } else {
                    DbUtils.closeStatement(statement);
                }
            }

        }

        private void calculateAverageValues( CheckpointSummary checkpointSummary ) {

            if (checkpointSummary.numPassed == 0) {
                // no need to update averages
                String toStr = checkpointSummary.limitedToString();
                if (toStr != null) {
                    log.warn("Checkpoint summary without passed actions. Summary info: " + toStr);
                }
            } else {
                if (checkpointSummary.avgTransferRate > 0) {
                    checkpointSummary.avgTransferRate /= checkpointSummary.numPassed;
                } else {
                    checkpointSummary.avgTransferRate = 0;
                }

                if (checkpointSummary.avgResponseTime > 0) {
                    checkpointSummary.avgResponseTime /= checkpointSummary.numPassed;
                } else {
                    checkpointSummary.avgResponseTime = 0;
                }
            }
        }

        private void flushCheckpoints() {

            if (checkpointsInsertData.isEmpty()) {
                return;
            }

            if (isMonitorEventsQueue) {
                batchStartTime = System.currentTimeMillis();
            }

            SQLServerBulkCopy bulkCopy = null;
            ByteArrayInputStream sis = null;
            try {
                SQLServerBulkCopyOptions copyOptions = new SQLServerBulkCopyOptions();
                copyOptions.setKeepIdentity(false);
                //copyOptions.isKeepNulls();
                // Depending on the size of the data being uploaded, and the amount of RAM, an optimum can be found here. Play around with this to improve performance.
                copyOptions.setBatchSize(this.cacheSize);

                // This is crucial to get good performance
                copyOptions.setTableLock(true);

                Connection destinationConnection = this.connection;

                bulkCopy = new SQLServerBulkCopy(destinationConnection);
                bulkCopy.setBulkCopyOptions(copyOptions);

                bulkCopy.setDestinationTableName("tCheckpoints");

                for (StringBuilder sb : checkpointsInsertData.values()) {

                    SQLServerBulkCSVFileRecord fileRecord = null;

                    sis = new ByteArrayInputStream(sb.toString().getBytes());

                    fileRecord = new SQLServerBulkCSVFileRecord(sis, null, ",", false);
                    fileRecord.addColumnMetadata(1, null, java.sql.Types.BIGINT, 0, 0); // checkpointId
                    fileRecord.addColumnMetadata(2, null, java.sql.Types.INTEGER, 0, 0); // checkpointSummaryId
                    fileRecord.addColumnMetadata(3, null, java.sql.Types.VARCHAR, 0, 0); // name
                    fileRecord.addColumnMetadata(4, null, java.sql.Types.INTEGER, 0, 0); // responseTime
                    fileRecord.addColumnMetadata(5, null, java.sql.Types.FLOAT, 0, 0); // transferRate
                    fileRecord.addColumnMetadata(6, null, java.sql.Types.VARCHAR, 0, 0); // transferRateUnit
                    fileRecord.addColumnMetadata(7, null, java.sql.Types.TINYINT, 0, 0); // result
                    fileRecord.addColumnMetadata(8, null, java.sql.Types.TIMESTAMP, 0, 0); // endTime // OR OTHER TIME

                    bulkCopy.writeToServer(fileRecord);

                }

            } catch (Exception e) {
                throw new DbException("Could not flush checkpoints", e);
            } finally {
                if (bulkCopy != null) {
                    bulkCopy.close();
                }
                if (sis != null) {
                    try {
                        sis.close();
                    } catch (IOException e) {
                    }
                }
            }

        }

    }

}
