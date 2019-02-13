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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.axway.ats.core.dbaccess.ConnectionPool;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.DbUtils;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.dbaccess.mssql.DbConnSQLServer;
import com.axway.ats.core.dbaccess.postgresql.DbConnPostgreSQL;
import com.axway.ats.core.log.AtsConsoleLogger;
import com.axway.ats.log.model.CheckpointLogLevel;

public class CheckpointsDbCache {

    private DbConnection         dbConnection;

    private List<Checkpoint>     checkpoints                    = new ArrayList<>();
    private Map<String, Integer> summaryIdsMap                  = new HashMap<>();

    private final static String  CHECKPOINTS_MAP_KEY_DELIMITER  = "^__^";

    private int                  checkpointLogLevel;
    public static final int      DEFAULT_MAX_CACHE_CHECKPOINTS  = 2000;
    private int                  maxCacheCheckpoints;

    private long                 firstInsertCheckpointStartTime = -1;

    public CheckpointsDbCache( DbConnection dbConnection, CheckpointLogLevel checkpointLogLevel,
                               int maxCacheCheckpoints ) {

        this.checkpointLogLevel = checkpointLogLevel.toInt();

        this.dbConnection = dbConnection;

        this.maxCacheCheckpoints = maxCacheCheckpoints;

    }

    public void addRunMessage() {

    }

    public void addSuiteMessage() {

    }

    public void addTestcaseMessage() {

    }

    public void addMessage( String message,
                            int level,
                            boolean escapeHtml,
                            String machineName,
                            String threadName,
                            long timestamp,
                            int testCaseId,
                            boolean closeConnection ) {

    }

    public void addCheckpoint( String name,
                               long startTimestamp,
                               long responseTime,
                               long transferSize,
                               String transferUnit,
                               int result,
                               int loadQueueId ) {

        Checkpoint checkpoint = new Checkpoint(name, startTimestamp, responseTime, transferSize, transferUnit,
                                               result, loadQueueId);
        checkpoints.add(checkpoint);
        summaryIdsMap.put(checkpoint.name + CHECKPOINTS_MAP_KEY_DELIMITER + checkpoint.loadQueueId,
                          checkpoint.checkpointSummaryId);

        if (firstInsertCheckpointStartTime == -1) {
            firstInsertCheckpointStartTime = System.currentTimeMillis();
        }

    }

    public boolean flush( boolean forced ) {

        boolean checkpointChunkSizeReached = checkpoints.size() >= maxCacheCheckpoints;
        boolean checkpointsCacheTooOld = (System.currentTimeMillis()
                                          - firstInsertCheckpointStartTime) >= (AbstractDbAccess.CACHE_MAX_DURATION_BEFORE_FLUSH);

        if (!forced) {
            // We are not forced to flush
            if (!checkpointChunkSizeReached && !checkpointsCacheTooOld) {
                // There is still room for more checkpoints and none of them is too old
                return false;
            }
        }

        if (checkpoints.size() <= 0) {
            // Nothing to flush, event if we are forced to
            return false;
        }

        Connection conn = null;
        Statement statement = null;
        try {
            conn = ConnectionPool.getConnection(dbConnection);
            statement = conn.prepareStatement(generateInsertCheckpointsSqlQuery());
            ((PreparedStatement) statement).executeUpdate();
            checkpoints.clear();
            firstInsertCheckpointStartTime = -1;
            return true;
        } catch (Exception e) {
            throw new DbException("Unable to insert checkpoints in database", e);
        } finally {
            DbUtils.close(conn, statement);
        }

    }

    private String generateInsertCheckpointsSqlQuery() {

        StringBuilder query = new StringBuilder();
        for (Checkpoint checkpoint : checkpoints) {
            if (checkpoint.checkpointSummaryId > -1) {
                query.append(checkpoint.generateUpdateCheckpointSummarySqlQuery())
                     .append("\n");

            }
            if (checkpointLogLevel != CheckpointLogLevel.SHORT.toInt()) {
                query.append(checkpoint.generateInsertCheckpointSqlQuery())
                     .append("\n");
            }

        }
        return query.toString();
    }

    class Checkpoint {

        AtsConsoleLogger acl = new AtsConsoleLogger(Checkpoint.class);

        String           name;
        long             startTimestamp;
        long             responseTime;
        long             transferSize;
        String           transferUnit;
        int              result;
        int              loadQueueId;
        int              checkpointSummaryId;
        double           transferRate;
        long             endTime;

        public Checkpoint( String name,
                           long startTimestamp,
                           long responseTime,
                           long transferSize,
                           String transferUnit,
                           int result,
                           int loadQueueId ) {

            this.name = name;
            this.startTimestamp = startTimestamp;
            this.responseTime = responseTime;
            this.transferSize = transferSize;
            this.transferRate = calculateTransferRate();
            this.transferUnit = transferUnit;
            this.result = result;
            this.loadQueueId = loadQueueId;
            this.checkpointSummaryId = getCheckpointSummaryId();
            this.endTime = this.startTimestamp + this.responseTime;

        }

        public Object generateInsertCheckpointSqlQuery() {

            StringBuilder sb = new StringBuilder();
            if (dbConnection instanceof DbConnPostgreSQL) {
                sb.append("INSERT INTO \"tCheckpoints\" ");
            } else if (dbConnection instanceof DbConnSQLServer) {
                sb.append("INSERT INTO tCheckpoints ");
            } else {
                throw new UnsupportedOperationException("DB connection '" + dbConnection.getClass().getName()
                                                        + "' is not supported");
            }

            sb.append("(checkpointSummaryId, name, responseTime, transferRate, transferRateUnit, result, endTime)")
              .append("VALUES (")
              .append(this.checkpointSummaryId + ", '" + this.name + "', " + this.responseTime + ", "
                      + this.transferRate + ", '" + this.transferUnit + "', " + this.result + ","
                      + new Timestamp(this.endTime) + ");");
            return sb.toString();
        }

        public Object generateUpdateCheckpointSummarySqlQuery() {

            StringBuilder sb = new StringBuilder();
            if (dbConnection instanceof DbConnPostgreSQL) {
                sb.append("UPDATE \"tCheckpointsSummary\" ");
            } else if (dbConnection instanceof DbConnSQLServer) {
                sb.append("UPDATE tCheckpointsSummary ");
            } else {
                throw new UnsupportedOperationException("DB connection '" + dbConnection.getClass().getName()
                                                        + "' is not supported");
            }

            if (this.result == 0) {// checkpoint failed
                sb.append("SET numFailed = numFailed + 1 ")
                  .append("WHERE checkpointSummaryId = " + this.checkpointSummaryId);
            } else {
                sb.append("SET numPassed = numPassed + 1, ")
                  .append("minResponseTime = CASE WHEN " + this.responseTime + " < minResponseTime THEN "
                          + this.responseTime + " ELSE minResponseTime END, ")
                  .append("maxResponseTime = CASE WHEN " + this.responseTime + " > maxResponseTime THEN "
                          + this.responseTime + " ELSE maxResponseTime END, ")
                  .append("avgResponseTime = (avgResponseTime * numPassed + " + this.responseTime
                          + ")/(numPassed + 1), ")
                  .append("minTransferRate = CASE WHEN " + this.transferRate + " < minTransferRate THEN "
                          + this.transferRate
                          + " ELSE minTransferRate END, ")
                  .append("maxTransferRate = CASE WHEN " + this.transferRate + " > maxTransferRate THEN "
                          + this.transferRate
                          + " ELSE maxTransferRate END, ")
                  .append("avgTransferRate = (avgTransferRate * numPassed +" + this.transferRate
                          + ")/(numPassed+ 1) ")
                  .append("WHERE checkpointSummaryId = " + this.checkpointSummaryId + ";");
            }

            return sb.toString();
        }

        private double calculateTransferRate() {

            if (CheckpointsDbCache.this.checkpointLogLevel == CheckpointLogLevel.SHORT.toInt()) {
                responseTime = 0;
                transferSize = 0;
            }

            if (responseTime > 0) {
                transferRate = transferSize * 1000.0 / responseTime;
            } else {
                transferRate = 0;
            }

            return transferRate;
        }

        private int getCheckpointSummaryId() {

            String mapKey = name + CHECKPOINTS_MAP_KEY_DELIMITER + loadQueueId;
            Integer summaryId = summaryIdsMap.get(mapKey);
            if (summaryId != null) {
                return summaryId.intValue();
            }

            Connection conn = null;
            CallableStatement statement = null;
            ResultSet rs = null;
            try {
                conn = ConnectionPool.getConnection(dbConnection);
                statement = conn.prepareCall("{ call sp_get_checkpoints_summary(?, ?, ?) }");
                statement.setString(1, "WHERE name= '" + name + "' AND loadQueueId = " + loadQueueId + "");
                statement.setString(2, "checkpointSummaryId");
                statement.setString(3, "ASC");
                rs = ((PreparedStatement) statement).executeQuery();
                if (rs.next()) {
                    int checkpointSummaryId = rs.getInt("checkpointSummaryId");
                    if (checkpointSummaryId == -1) {
                        acl.warn("summary ID for checkpoint '" + name + "' from queue '" + loadQueueId + "'");
                        return -1;
                    }
                    return checkpointSummaryId;
                } else {
                    throw new DbException("Result set is empty");
                }
            } catch (Exception e) {
                acl.error("Cannot get checkpoint summary ID for checkpoint '" + name + "' from queue '" + loadQueueId
                          + "'", e);
                return -1;
            } finally {
                DbUtils.closeResultSet(rs);
                DbUtils.close(conn, statement);
            }
        }
    }

    public void setMaxCacheCheckpoints( int maxNumberOfCachedEvents ) {

        this.maxCacheCheckpoints = maxNumberOfCachedEvents;

    }

    public void resetCache() {

       checkpoints.clear();
        
    }

}
