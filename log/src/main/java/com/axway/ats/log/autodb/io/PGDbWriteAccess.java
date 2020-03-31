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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.DbUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.autodb.CheckpointInfo;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.log.model.CheckpointLogLevel;

public class PGDbWriteAccess extends SQLServerDbWriteAccess {

    // the DB statements provider
    // it shadows the DbWriteAccess's private variable with the same name
    protected PGInsertEventStatementsFactory insertFactory;

    public PGDbWriteAccess( DbConnection dbConnection, boolean isBatchMode ) throws DatabaseAccessException {

        super(dbConnection, isBatchMode);
        this.insertFactory = new PGInsertEventStatementsFactory(isBatchMode);
    }

    /**
     * Update the static information about an existing run
     *
     * @param runId
     * @param runName
     * @param osName
     * @param productName
     * @param versionName
     * @param buildName
     * @param userNote
     * @param hostName
     * @param closeConnection
     * @throws DatabaseAccessException
     */
    public void updateRun( int runId, String runName, String osName, String productName, String versionName,
                           String buildName, String userNote, String hostName, boolean closeConnection )
                                                                                                         throws DatabaseAccessException {

        final String errMsg = "Unable to update run with name '" + runName + "' and id " + runId;

        // then start the run
        final int indexRowsUpdate = 9;

        CallableStatement callableStatement = null;
        try {
            refreshInternalConnection();

            callableStatement = connection.prepareCall("{ call sp_update_run(?, ?, ?, ?, ?, ?, ?, ?, ?) }");
            callableStatement.setString(1, runId + "");
            callableStatement.setString(2, productName);
            callableStatement.setString(3, versionName);
            callableStatement.setString(4, buildName);
            callableStatement.setString(5, runName);
            callableStatement.setString(6, osName);
            callableStatement.setString(7, userNote);
            callableStatement.setString(8, hostName);
            callableStatement.registerOutParameter(indexRowsUpdate, Types.INTEGER);

            callableStatement.execute();
            if (callableStatement.getInt(indexRowsUpdate) != 1) {
                throw new DatabaseAccessException(errMsg);
            }
        } catch (Exception e) {
            throw new DatabaseAccessException(errMsg, e);
        } finally {
            if (closeConnection) {
                DbUtils.close(connection, callableStatement);
            } else {
                DbUtils.closeStatement(callableStatement);
            }
        }
    }

    @Override
    public int startSuite( String packageName, String suiteName, long timestamp, int runId, boolean closeConnection )
                                                                                                                      throws DatabaseAccessException {

        final String errMsg = "Unable to start suite with name " + suiteName;
        // create a new suite

        timestamp = inUTC(timestamp);

        final int indexRowsInserted = 5;
        final int indexSuiteId = 6;

        CallableStatement callableStatement = null;
        try {
            refreshInternalConnection();

            callableStatement = connection.prepareCall("{ call sp_start_suite(?, ?, ?, ?, ?, ?) }");

            if (packageName == null) {
                packageName = "";
            }
            callableStatement.setString(3, packageName);

            callableStatement.setString(1, suiteName);
            callableStatement.setInt(2, runId);
            callableStatement.setTimestamp(4, new Timestamp(timestamp));
            callableStatement.registerOutParameter(5, Types.INTEGER);
            callableStatement.registerOutParameter(6, Types.INTEGER);

            callableStatement.execute();

            if (callableStatement.getInt(indexRowsInserted) != 1) {
                throw new DatabaseAccessException(errMsg);
            } else {
                if (callableStatement.getInt(indexSuiteId) == 0) {
                    throw new DatabaseAccessException(errMsg + " - suite ID returned was 0");
                }
            }
            // get the result
            return callableStatement.getInt(indexSuiteId);

        } catch (Exception e) {
            throw new DatabaseAccessException(errMsg, e);
        } finally {
            if (closeConnection) {
                DbUtils.close(connection, callableStatement);
            } else {
                DbUtils.closeStatement(callableStatement);
            }
        }
    }

    @Override
    public boolean insertMessage( String message, int level, boolean escapeHtml, String machineName, String threadName,
                                  long timestamp, int testCaseId,
                                  boolean closeConnection ) throws DatabaseAccessException {

        // escape characters with ASCII code < 32
        message = StringUtils.escapeNonPrintableAsciiCharacters(message);

        timestamp = inUTC(timestamp);

        Connection currentConnection;
        if (!isBatchMode) {
            currentConnection = refreshInternalConnection();
        } else {
            currentConnection = dbEventsCache.connection;
        }

        CallableStatement insertMessageStatement = insertFactory.getInsertTestcaseMessageStatement(currentConnection,
                                                                                                   message, level,
                                                                                                   escapeHtml,
                                                                                                   machineName,
                                                                                                   threadName,
                                                                                                   timestamp,
                                                                                                   testCaseId);

        if (isBatchMode) {
            // schedule this event for batch execution
            return dbEventsCache.addInsertTestcaseMessageEventToBatch(insertMessageStatement);
        } else {
            // execute this event now
            String errMsg = "Unable to insert testcase message '" + message + "'";

            try {
                insertMessageStatement.execute();
            } catch (SQLException e) {
                String procedureName = "sp_insert_message";
                List<Object> argValues = new ArrayList<Object>();
                argValues.add(testCaseId);
                argValues.add(level);
                argValues.add(message);
                argValues.add(escapeHtml);
                argValues.add(machineName);
                argValues.add(threadName);
                argValues.add(timestamp);

                errMsg += " using the following statement: "
                          + constructStoredProcedureArgumentsMap(procedureName, argValues);
                throw new DatabaseAccessException(errMsg, e);
            } finally {
                if (closeConnection) {
                    DbUtils.close(connection, insertMessageStatement);
                } else {
                    DbUtils.closeStatement(insertMessageStatement);
                }
            }

            return false;
        }
    }

    @Override
    public boolean insertRunMessage( String message, int level, boolean escapeHtml, String machineName,
                                     String threadName, long timestamp, int runId,
                                     boolean closeConnection ) throws DatabaseAccessException {

        String dbVersionString = getDatabaseVersion();
        int dbVersion = Integer.parseInt(dbVersionString.replace(".", ""));

        if (dbVersion < 350) {

            return false;
        } else {

            // escape characters with ASCII code < 32
            message = StringUtils.escapeNonPrintableAsciiCharacters(message);

            timestamp = inUTC(timestamp);

            Connection currentConnection;
            if (!isBatchMode) {
                currentConnection = refreshInternalConnection();
            } else {
                currentConnection = dbEventsCache.connection;
            }

            CallableStatement insertMessageStatement = insertFactory.getInsertRunMessageStatement(currentConnection,
                                                                                                  message, level,
                                                                                                  escapeHtml,
                                                                                                  machineName,
                                                                                                  threadName, timestamp,
                                                                                                  runId);

            if (isBatchMode) {
                // schedule this event for batch execution
                return dbEventsCache.addInsertRunMessageEventToBatch(insertMessageStatement);
            } else {
                // execute this event now
                String errMsg = "Unable to insert run message '" + message + "'";

                try {
                    insertMessageStatement.execute();
                } catch (SQLException e) {
                    String procedureName = "sp_insert_run_message";
                    List<Object> argValues = new ArrayList<Object>();
                    argValues.add(runId);
                    argValues.add(level);
                    argValues.add(message);
                    argValues.add(escapeHtml);
                    argValues.add(machineName);
                    argValues.add(threadName);
                    argValues.add(timestamp);

                    errMsg += " using the following statement: "
                              + constructStoredProcedureArgumentsMap(procedureName, argValues);
                    throw new DatabaseAccessException(errMsg, e);
                } finally {
                    if (closeConnection) {
                        DbUtils.close(connection, insertMessageStatement);
                    } else {
                        DbUtils.closeStatement(insertMessageStatement);
                    }
                }

                return false;
            }
        }
    }

    @Override
    public boolean insertSuiteMessage( String message, int level, boolean escapeHtml, String machineName,
                                       String threadName, long timestamp, int suiteId,
                                       boolean closeConnection ) throws DatabaseAccessException {

        String dbVersionString = getDatabaseVersion();
        int dbVersion = Integer.parseInt(dbVersionString.replace(".", ""));

        if (dbVersion < 350) {

            return false;
        } else {

            // escape characters with ASCII code < 32
            message = StringUtils.escapeNonPrintableAsciiCharacters(message);

            timestamp = inUTC(timestamp);

            Connection currentConnection;
            if (!isBatchMode) {
                currentConnection = refreshInternalConnection();
            } else {
                currentConnection = dbEventsCache.connection;
            }

            CallableStatement insertMessageStatement = insertFactory.getInsertSuiteMessageStatement(currentConnection,
                                                                                                    message, level,
                                                                                                    escapeHtml,
                                                                                                    machineName,
                                                                                                    threadName,
                                                                                                    timestamp, suiteId);

            if (isBatchMode) {
                // schedule this event for batch execution
                return dbEventsCache.addInsertSuiteMessageEventToBatch(insertMessageStatement);
            } else {
                // execute this event now
                String errMsg = "Unable to insert suite message '" + message + "'";

                try {
                    insertMessageStatement.execute();
                } catch (SQLException e) {
                    String procedureName = "sp_insert_suite_message";
                    List<Object> argValues = new ArrayList<Object>();
                    argValues.add(suiteId);
                    argValues.add(level);
                    argValues.add(message);
                    argValues.add(escapeHtml);
                    argValues.add(machineName);
                    argValues.add(threadName);
                    argValues.add(timestamp);

                    errMsg += " using the following statement: "
                              + constructStoredProcedureArgumentsMap(procedureName, argValues);
                    throw new DatabaseAccessException(errMsg, e);
                } finally {
                    if (closeConnection) {
                        DbUtils.close(connection, insertMessageStatement);
                    } else {
                        DbUtils.closeStatement(insertMessageStatement);
                    }
                }

                return false;
            }
        }
    }

    public CheckpointInfo startCheckpoint( String name, long startTimestamp, String transferRateUnit, int loadQueueId,
                                           boolean closeConnection ) throws DatabaseAccessException {

        final String errMsg = "Unable to start checkpoint '" + name + "' in load queue " + loadQueueId;

        startTimestamp = inUTC(startTimestamp);

        final int indexCheckpointSummaryId = 5;
        final int indexCheckpointId = 6;

        CallableStatement callableStatement = null;
        try {
            refreshInternalConnection();

            callableStatement = connection.prepareCall("{ call sp_start_checkpoint(?, ?, ?, ?, ?, ?) }");
            callableStatement.setInt(1, loadQueueId);
            callableStatement.setString(2, name);
            callableStatement.setInt(3, checkpointLogLevel.toInt());
            callableStatement.setString(4, transferRateUnit);
            callableStatement.registerOutParameter(indexCheckpointSummaryId, Types.INTEGER);
            callableStatement.registerOutParameter(indexCheckpointId, Types.BIGINT);

            callableStatement.execute();

            // we always update the checkpoint summary table
            if (callableStatement.getInt(indexCheckpointSummaryId) == 0) {
                throw new DatabaseAccessException(errMsg + " - checkpoint summary ID returned was 0");
            }

            // we update the checkpoint table only in FULL mode
            if (checkpointLogLevel == CheckpointLogLevel.FULL && callableStatement.getInt(indexCheckpointId) == 0) {
                throw new DatabaseAccessException(errMsg + " - checkpoint ID returned was 0");
            }

            int checkpointSummaryId = callableStatement.getInt(indexCheckpointSummaryId);
            long checkpointId = callableStatement.getLong(indexCheckpointId);

            return new CheckpointInfo(name, checkpointSummaryId, checkpointId, startTimestamp);

        } catch (Exception e) {
            throw new DatabaseAccessException(errMsg, e);
        } finally {
            if (closeConnection) {
                DbUtils.close(connection, callableStatement);
            } else {
                DbUtils.closeStatement(callableStatement);
            }
        }
    }

    public boolean insertCheckpoint( String name, long startTimestamp, long responseTime, long transferSize,
                                     String transferUnit, int result, int loadQueueId,
                                     boolean closeConnection ) throws DatabaseAccessException {

        startTimestamp = inUTC(startTimestamp);

        Connection currentConnection;
        if (!isBatchMode) {
            currentConnection = refreshInternalConnection();
        } else {
            currentConnection = dbEventsCache.connection;
        }

        CallableStatement insertCheckpointStatement = insertFactory.getInsertCheckpointStatement(currentConnection,
                                                                                                 name, responseTime,
                                                                                                 startTimestamp + responseTime,
                                                                                                 transferSize,
                                                                                                 transferUnit, result,
                                                                                                 checkpointLogLevel,
                                                                                                 loadQueueId);

        if (isBatchMode) {
            // schedule this event for batch execution
            return dbEventsCache.addInsertCheckpointEventToBatch(insertCheckpointStatement);
        } else {
            // execute this event now
            String errMsg = "Unable to insert checkpoint '" + name + "'";
            try {
                insertCheckpointStatement.execute();
            } catch (SQLException e) {
                String procedureName = "sp_insert_checkpoint";
                List<Object> argValues = new ArrayList<Object>();
                argValues.add(loadQueueId);
                argValues.add(name);
                argValues.add(responseTime);
                argValues.add(startTimestamp + responseTime);
                argValues.add(transferSize);
                argValues.add(transferUnit);
                argValues.add(result);
                argValues.add(checkpointLogLevel.toInt());

                errMsg += " using the following statement: "
                          + constructStoredProcedureArgumentsMap(procedureName, argValues);
                throw new DatabaseAccessException(errMsg, e);
            } finally {
                if (closeConnection) {
                    DbUtils.close(connection, insertCheckpointStatement);
                } else {
                    DbUtils.closeStatement(insertCheckpointStatement);
                }
            }

            return false;
        }

    }

    @Override
    public void insertUserActivityStatistics( int testCaseId, String machine, String statisticIds,
                                              String statisticValues, long timestamp,
                                              boolean closeConnection ) throws DatabaseAccessException {

        timestamp = inUTC(timestamp);

        CallableStatement callableStatement = null;
        try {
            refreshInternalConnection();

            callableStatement = connection
                                          .prepareCall("{ call sp_insert_user_activity_statistic_by_ids(?, ?, ?, ?, ?) }");
            callableStatement.setInt(1, testCaseId);
            callableStatement.setString(2, machine);
            callableStatement.setString(3, statisticIds);
            callableStatement.setString(4, statisticValues);
            callableStatement.setTimestamp(5, new Timestamp(timestamp));

            callableStatement.execute();

        } catch (Exception e) {
            String errMsg = "Unable to insert user activity statistics, statistic IDs '" + statisticIds
                            + "', statistic values '" + statisticValues + "', timestamp " + timestamp;
            throw new DatabaseAccessException(errMsg, e);
        } finally {
            if (closeConnection) {
                DbUtils.close(connection, callableStatement);
            } else {
                DbUtils.closeStatement(callableStatement);
            }
        }
    }

    public boolean isRunPresent( int runId ) throws DatabaseAccessException {

        Connection connection = getConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.prepareStatement("SELECT COUNT(*) FROM \"tRuns\" WHERE runId = " + runId);
            rs = statement.executeQuery();
            if (rs.next()) {
                return 1 == rs.getInt(1);
            }
        } catch (Exception e) {
            throw new DatabaseAccessException("Error checking whether run with id " + runId + " exists", e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, statement);
        }

        return false;
    }

    public boolean isSuitePresent( int suiteId ) throws DatabaseAccessException {

        Connection connection = getConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.prepareStatement("SELECT COUNT(*) FROM \"tSuites\" WHERE suiteId = " + suiteId);
            rs = statement.executeQuery();
            if (rs.next()) {
                return 1 == rs.getInt(1);
            }
        } catch (Exception e) {
            throw new DatabaseAccessException("Error checking whether suite with id " + suiteId + " exists", e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, statement);
        }

        return false;
    }

    public boolean isTestcasePresent( int testcaseId ) throws DatabaseAccessException {

        Connection connection = getConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = connection
                                  .prepareStatement("SELECT COUNT(*) FROM \"tTestcases\" WHERE testcaseId = "
                                                    + testcaseId);
            rs = statement.executeQuery();
            if (rs.next()) {
                return 1 == rs.getInt(1);
            }
        } catch (Exception e) {
            throw new DatabaseAccessException("Error checking whether testcase with id " + testcaseId + " exists", e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, statement);
        }

        return false;
    }

    /**
     * Provides the event statements
     */
    protected class PGInsertEventStatementsFactory {

        private boolean             isBatchMode;

        private CallableStatement   insertRunMessageStatement;
        private CallableStatement   insertSuiteMessageStatement;
        private CallableStatement   insertTestcaseMessageStatement;
        private CallableStatement   insertCheckpointStatement;

        private static final String SP_INSERT_RUN_MESSAGE      = "{ call sp_insert_run_message(?, ?, ?, ?, ?, ?, ?) }";
        private static final String SP_INSERT_SUITE_MESSAGE    = "{ call sp_insert_suite_message(?, ?, ?, ?, ?, ?, ?) }";
        private static final String SP_INSERT_TESTCASE_MESSAGE = "{ call sp_insert_message(?, ?, ?, ?, ?, ?, ?) }";
        private static final String SP_INSERT_CHECKPOINT       = "{ call sp_insert_checkpoint(?, ?, ?, ?, ?, ?, ?, ?) }";

        public PGInsertEventStatementsFactory( boolean isBatchMode ) {

            this.isBatchMode = isBatchMode;
        }

        CallableStatement getInsertCheckpointStatement( Connection connection, String name, long responseTime,
                                                        long endTimestamp, long transferSize, String transferUnit,
                                                        int result,
                                                        CheckpointLogLevel checkpointLogLevel,
                                                        int loadQueueId ) throws DatabaseAccessException {

            // get the statement
            CallableStatement theStatement;
            try {
                if (isBatchMode) {
                    if (insertCheckpointStatement == null) {
                        insertCheckpointStatement = connection.prepareCall(SP_INSERT_CHECKPOINT);
                    }
                    theStatement = insertCheckpointStatement;
                } else {
                    theStatement = connection.prepareCall(SP_INSERT_CHECKPOINT);
                }
            } catch (SQLException e) {
                throw new DatabaseAccessException("Unable to create SQL statement for inserting checkpoints", e);
            }

            // apply statement parameters
            try {
                theStatement.setInt(1, loadQueueId);
                theStatement.setString(2, name);
                theStatement.setLong(3, responseTime);
                theStatement.setTimestamp(4, new Timestamp(endTimestamp));
                theStatement.setLong(5, transferSize);
                theStatement.setString(6, transferUnit);
                theStatement.setInt(7, result);
                theStatement.setInt(8, checkpointLogLevel.toInt());

                return theStatement;
            } catch (Exception e) {
                throw new DatabaseAccessException("Unable to set parameters for inserting a checkpoint '" + name + "'",
                                                  e);
            }
        }

        public CallableStatement getInsertTestcaseMessageStatement( Connection connection, String message, int level,
                                                                    boolean escapeHtml, String machineName,
                                                                    String threadName, long timestamp, int testCaseId )
                                                                                                                        throws DatabaseAccessException {

            // get the statement
            CallableStatement theStatement;
            try {
                if (isBatchMode) {
                    if (insertTestcaseMessageStatement == null) {
                        insertTestcaseMessageStatement = connection.prepareCall(SP_INSERT_TESTCASE_MESSAGE);
                    }
                    theStatement = insertTestcaseMessageStatement;
                } else {
                    theStatement = connection.prepareCall(SP_INSERT_TESTCASE_MESSAGE);
                }
            } catch (SQLException e) {
                throw new DatabaseAccessException("Unable to create SQL statement for inserting a message", e);
            }

            try {
                theStatement.setInt(1, testCaseId);
                theStatement.setInt(2, level);
                theStatement.setString(3, message);
                theStatement.setBoolean(4, escapeHtml);
                theStatement.setString(5, machineName);
                theStatement.setString(6, threadName);
                theStatement.setTimestamp(7, new Timestamp(timestamp));

                return theStatement;
            } catch (Exception e) {
                throw new DatabaseAccessException("Unable to set parameters for inserting a message '" + message + "'",
                                                  e);
            }
        }

        public CallableStatement getInsertRunMessageStatement( Connection connection, String message, int level,
                                                               boolean escapeHtml, String machineName,
                                                               String threadName, long timestamp, int runId )
                                                                                                              throws DatabaseAccessException {

            // get the statement
            CallableStatement theStatement;
            try {
                if (isBatchMode) {
                    if (insertRunMessageStatement == null) {
                        insertRunMessageStatement = connection.prepareCall(SP_INSERT_RUN_MESSAGE);
                    }
                    theStatement = insertRunMessageStatement;
                } else {
                    theStatement = connection.prepareCall(SP_INSERT_RUN_MESSAGE);
                }
            } catch (SQLException e) {
                throw new DatabaseAccessException("Unable to create SQL statement for inserting a run message", e);
            }

            // apply statement parameters

            try {
                theStatement.setInt(1, runId);
                theStatement.setInt(2, level);
                theStatement.setString(3, message);
                theStatement.setBoolean(4, escapeHtml);
                theStatement.setString(5, machineName);
                theStatement.setString(6, threadName);
                theStatement.setTimestamp(7, new Timestamp(timestamp));

                return theStatement;
            } catch (Exception e) {
                throw new DatabaseAccessException(
                                                  "Unable to set parameters for inserting a run message '" + message
                                                  + "'", e);
            }
        }

        public CallableStatement getInsertSuiteMessageStatement( Connection connection, String message, int level,
                                                                 boolean escapeHtml, String machineName,
                                                                 String threadName, long timestamp, int suiteId )
                                                                                                                  throws DatabaseAccessException {

            // get the statement
            CallableStatement theStatement;
            try {
                if (isBatchMode) {
                    if (insertSuiteMessageStatement == null) {
                        insertSuiteMessageStatement = connection.prepareCall(SP_INSERT_SUITE_MESSAGE);
                    }
                    theStatement = insertSuiteMessageStatement;
                } else {
                    theStatement = connection.prepareCall(SP_INSERT_SUITE_MESSAGE);
                }
            } catch (SQLException e) {
                throw new DatabaseAccessException("Unable to create SQL statement for inserting a suite message", e);
            }

            try {
                theStatement.setInt(1, suiteId);
                theStatement.setInt(2, level);
                theStatement.setString(3, message);
                theStatement.setBoolean(4, escapeHtml);
                theStatement.setString(5, machineName);
                theStatement.setString(6, threadName);
                theStatement.setTimestamp(7, new Timestamp(timestamp));

                return theStatement;
            } catch (Exception e) {
                throw new DatabaseAccessException(
                                                  "Unable to set parameters for inserting a suite message '" + message
                                                  + "'", e);
            }
        }

    }

}
