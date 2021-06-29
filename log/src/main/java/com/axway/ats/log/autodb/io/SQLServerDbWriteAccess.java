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
package com.axway.ats.log.autodb.io;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.AtsVersion;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.DbUtils;
import com.axway.ats.core.utils.ExceptionUtils;
import com.axway.ats.log.autodb.CheckpointInfo;
import com.axway.ats.log.autodb.entities.Testcase;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.log.autodb.model.IDbWriteAccess;
import com.axway.ats.log.model.CheckpointLogLevel;
import com.axway.ats.log.model.CheckpointResult;
import com.axway.ats.log.model.LoadQueueResult;

public class SQLServerDbWriteAccess extends AbstractDbAccess implements IDbWriteAccess {

    // the checkpoint log level
    protected static CheckpointLogLevel    checkpointLogLevel = CheckpointLogLevel.SHORT;

    // when we start we do a quick sanity check
    boolean                                sanityRun          = false;

    // the events cache
    protected DbEventsCache                dbEventsCache;

    // the DB statements provider
    protected InsertEventStatementsFactory insertFactory;

    // if we are using batch mode
    protected boolean                      isBatchMode;

    /**
     * When true - we dump info about the usage of the events queue. It is
     * targeted as a debug tool when cannot sent the events to the DB fast
     * enough.
     */
    protected boolean                      isMonitorEventsQueue;

    /**
     * When copying runs/suites/etc, the timestamps are already in UTC.
     * Due to that any further conversion to UTC, will just strip additional hours from the already processed timestamp,
     * leading to incorrect time stamp
     * */
    protected boolean                      skipUTCConversion  = false;

    public SQLServerDbWriteAccess( DbConnection dbConnection,
                                   boolean isBatchMode ) throws DatabaseAccessException {

        super(dbConnection);
        this.isBatchMode = isBatchMode;
        this.insertFactory = new InsertEventStatementsFactory(isBatchMode);

        if (isBatchMode) {
            // some events are sent to the DB in batch mode, we cache them here
            dbEventsCache = new DbEventsCache(this);

            isMonitorEventsQueue = AtsSystemProperties.getPropertyAsBoolean(AtsSystemProperties.LOG__MONITOR_EVENTS_QUEUE,
                                                                            false);

            setMaxNumberOfCachedEvents(this.chunkSize);
        }
    }

    @Override
    public void setMaxNumberOfCachedEvents( int maxNumberOfCachedEvents ) {

        if (dbEventsCache != null) {
            dbEventsCache.setMaxNumberOfCachedEvents(maxNumberOfCachedEvents);
        }

    }

    /**
     * Insert a new run in the database
     *
     * @param runName
     *            name of the run
     * @param osName
     *            name of the OS
     * @param productName
     *            name of the product
     * @param versionName
     *            version of the product
     * @param buildName
     *            build version
     * @param timestamp
     * @param hostName
     *            name/IP of the machine , from which the run was started
     * @return
     */
    public int startRun(
                         String runName,
                         String osName,
                         String productName,
                         String versionName,
                         String buildName,
                         long timestamp,
                         String hostName,
                         boolean closeConnection ) throws DatabaseAccessException {

        timestamp = inUTC(timestamp);

        // then start the run
        final int indexRowsInserted = 8;
        final int indexRunId = 9;

        String errMsg = "Unable to insert run with name " + runName;

        CallableStatement callableStatement = null;
        try {
            refreshInternalConnection();

            callableStatement = connection.prepareCall("{ call sp_start_run(?, ?, ?, ?, ?, ?, ?, ? ,?) }");
            callableStatement.setString(1, productName);
            callableStatement.setString(2, versionName);
            callableStatement.setString(3, buildName);
            callableStatement.setString(4, runName);
            callableStatement.setString(5, osName);
            callableStatement.setTimestamp(6, new Timestamp(timestamp));
            callableStatement.setString(7, hostName);
            callableStatement.registerOutParameter(indexRowsInserted, Types.INTEGER);
            callableStatement.registerOutParameter(indexRunId, Types.INTEGER);

            callableStatement.execute();
            if (callableStatement.getInt(indexRowsInserted) == 1) {

                // check if the run ID is correct
                if (callableStatement.getInt(indexRunId) == 0) {
                    throw new DatabaseAccessException(errMsg
                                                      + " - run ID returned was 0");
                }
            } else {
                throw new DatabaseAccessException(errMsg);
            }

            // get the result
            return callableStatement.getInt(indexRunId);

        } catch (Exception e) {
            String procedureName = "sp_start_run";
            List<Object> argValues = new ArrayList<Object>();
            argValues.add(procedureName);
            argValues.add(versionName);
            argValues.add(buildName);
            argValues.add(runName);
            argValues.add(osName);
            argValues.add(timestamp);
            argValues.add(hostName);

            errMsg += " using the following statement: "
                      + constructStoredProcedureArgumentsMap(procedureName, argValues);
            throw new DatabaseAccessException(errMsg, e);
        } finally {
            if (closeConnection) {
                DbUtils.close(connection, callableStatement);
            } else {
                DbUtils.closeStatement(callableStatement);
            }
        }
    }

    /**
     * End a run in the database
     *
     * @param timestamp
     * @param runId
     */
    public void endRun(
                        long timestamp,
                        int runId,
                        boolean closeConnection ) throws DatabaseAccessException {

        if (isBatchMode) {
            flushCache();
        }

        final String errMsg = "Unable to end run with id " + runId;

        final int indexRowsInserted = 3;

        timestamp = inUTC(timestamp);

        CallableStatement callableStatement = null;
        try {
            refreshInternalConnection();

            callableStatement = connection.prepareCall("{ call sp_end_run(?, ?, ?) }");
            callableStatement.setInt(1, runId);
            callableStatement.setTimestamp(2, new Timestamp(timestamp));
            callableStatement.registerOutParameter(indexRowsInserted, Types.INTEGER);

            callableStatement.execute();
            if (callableStatement.getInt(indexRowsInserted) != 1) {
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
    public void updateRun(
                           int runId,
                           String runName,
                           String osName,
                           String productName,
                           String versionName,
                           String buildName,
                           String userNote,
                           String hostName,
                           boolean closeConnection ) throws DatabaseAccessException {

        final String errMsg = "Unable to update run with name '" + runName + "' and id " + runId;

        // then start the run
        final int indexRowsUpdate = 9;

        CallableStatement callableStatement = null;
        try {
            refreshInternalConnection();

            callableStatement = connection.prepareCall("{ call sp_update_run(?, ?, ?, ?, ?, ?, ?, ?, ?) }");
            callableStatement.setInt(1, runId);
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

    /**
     * Update meta info about an existing run
     *
     * @param runId
     * @param metaKey
     * @param metaValue
     * @param closeConnection
     * @throws DatabaseAccessException
     */
    public void addRunMetainfo(
                                int runId,
                                String metaKey,
                                String metaValue,
                                boolean closeConnection ) throws DatabaseAccessException {

        final String errMsg = "Unable to add run meta info '" + metaKey + "=" + metaValue
                              + "' to run with id " + runId;

        final int indexRowsInserted = 4;

        CallableStatement callableStatement = null;
        try {
            refreshInternalConnection();

            callableStatement = connection.prepareCall("{ call sp_add_run_metainfo(?, ?, ?, ?) }");
            callableStatement.setInt(1, runId);
            callableStatement.setString(2, metaKey);
            callableStatement.setString(3, metaValue);
            callableStatement.registerOutParameter(indexRowsInserted, Types.INTEGER);

            callableStatement.execute();
            if (callableStatement.getInt(indexRowsInserted) != 1) {
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

    public int startSuite(
                           String packageName,
                           String suiteName,
                           long timestamp,
                           int runId,
                           boolean closeConnection ) throws DatabaseAccessException {

        final String errMsg = "Unable to start suite with name " + suiteName;
        // create a new suite

        timestamp = inUTC(timestamp);

        CallableStatement callableStatement = null;
        try {
            refreshInternalConnection();

            // TODO : remove me after 3.6.0
            String dbVersionString = getDatabaseVersion();
            int dbVersion = Integer.parseInt(dbVersionString.replace(".", ""));

            if (dbVersion >= 350) {
                callableStatement = connection.prepareCall("{ call sp_start_suite(?, ?, ?, ?, ?, ?) }");

                if (packageName == null) {
                    packageName = "";
                }
                callableStatement.setString("@package", packageName);
            } else {
                callableStatement = connection.prepareCall("{ call sp_start_suite(?, ?, ?, ?, ?) }");
            }
            callableStatement.setString("@suiteName", suiteName);
            callableStatement.setInt("@runId", runId);
            callableStatement.setTimestamp("@dateStart", new Timestamp(timestamp));
            callableStatement.registerOutParameter("@RowsInserted", Types.INTEGER);
            callableStatement.registerOutParameter("@suiteId", Types.INTEGER);

            callableStatement.execute();

            if (callableStatement.getInt("@RowsInserted") != 1) {
                throw new DatabaseAccessException(errMsg);
            } else {
                if (callableStatement.getInt("@suiteId") == 0) {
                    throw new DatabaseAccessException(errMsg + " - suite ID returned was 0");
                }
            }
            // get the result
            return callableStatement.getInt("@suiteId");

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

    public void endSuite(
                          long timestamp,
                          int suiteId,
                          boolean closeConnection ) throws DatabaseAccessException {

        final String errMsg = "Unable to end suite with id " + suiteId;

        timestamp = inUTC(timestamp);

        final int indexRowsInserted = 3;

        CallableStatement callableStatement = null;
        try {
            refreshInternalConnection();

            callableStatement = connection.prepareCall("{ call sp_end_suite(?, ?, ?) }");
            callableStatement.setInt(1, suiteId);
            callableStatement.setTimestamp(2, new Timestamp(timestamp));
            callableStatement.registerOutParameter(indexRowsInserted, Types.INTEGER);

            callableStatement.execute();
            if (callableStatement.getInt(indexRowsInserted) != 1) {
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

    /**
     * Update the static information about an existing suite
     *
     * @param suiteId
     * @param suiteName
     * @param userNote
     * @param closeConnection
     * @throws DatabaseAccessException
     */
    @Override
    public void updateSuite(
                             int suiteId,
                             String suiteName,
                             String userNote,
                             boolean closeConnection ) throws DatabaseAccessException {

        final String errMsg = "Unable to update suite with name '" + suiteName + "' and id " + suiteId;

        final int indexRowsUpdate = 4;

        CallableStatement callableStatement = null;
        try {
            refreshInternalConnection();

            callableStatement = connection.prepareCall("{ call sp_update_suite(?, ?, ?, ?) }");
            callableStatement.setInt(1, suiteId);
            callableStatement.setString(2, suiteName);
            callableStatement.setString(3, userNote);
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

    /**
     * Clear all meta info about an existing test scenario. Intended to be
     * called prior to adding meta data about a scenario.
     *
     * @param scenarioId
     * @param closeConnection
     * @throws DatabaseAccessException
     */
    public void clearScenarioMetainfo(
                                       int scenarioId,
                                       boolean closeConnection ) throws DatabaseAccessException {

        final String errMsg = "Unable to clear scenario meta info for scenario with id " + scenarioId;

        final int indexRowsDeleted = 2;

        CallableStatement callableStatement = null;
        try {
            refreshInternalConnection();

            callableStatement = connection.prepareCall("{ call sp_clear_scenario_metainfo(?, ?) }");
            callableStatement.setInt(1, scenarioId);
            callableStatement.registerOutParameter(indexRowsDeleted, Types.INTEGER);

            callableStatement.execute();
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

    /**
     * Update meta info about an existing test scenario. This data is expected
     * to come from java method annotations
     *
     * @param testcaseId
     * @param metaKey
     * @param metaValue
     * @param closeConnection
     * @throws DatabaseAccessException
     */
    public void addScenarioMetainfo(
                                     int testcaseId,
                                     String metaKey,
                                     String metaValue,
                                     boolean closeConnection ) throws DatabaseAccessException {

        final String errMsg = "Unable to add scenario meta info '" + metaKey + "=" + metaValue
                              + "' to scenario for testcase with id " + testcaseId;

        final int indexRowsInserted = 4;

        CallableStatement callableStatement = null;
        try {
            refreshInternalConnection();

            callableStatement = connection.prepareCall("{ call sp_add_scenario_metainfo(?, ?, ?, ?) }");
            callableStatement.setInt(1, testcaseId);
            callableStatement.setString(2, metaKey);
            callableStatement.setString(3, metaValue);
            callableStatement.registerOutParameter(indexRowsInserted, Types.INTEGER);

            callableStatement.execute();
            if (callableStatement.getInt(indexRowsInserted) != 1) {
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

    public int startTestCase(
                              String suiteName,
                              String scenarioName,
                              String scenarioDescription,
                              String testcaseName,
                              long timestamp,
                              int suiteId,
                              boolean closeConnection ) throws DatabaseAccessException {

        final String errMsg = "Unable to start testcase with name " + testcaseName;

        timestamp = inUTC(timestamp);

        // start a new test case
        final int indexRowsInserted = 7;
        final int indexTestcaseId = 8;

        CallableStatement callableStatement = null;
        try {
            refreshInternalConnection();

            callableStatement = connection.prepareCall("{ call sp_start_testcase(?, ?, ?, ?, ?, ?, ?, ?) }");
            callableStatement.setInt(1, suiteId);
            callableStatement.setString(2, suiteName);
            callableStatement.setString(3, scenarioName);
            callableStatement.setString(4, scenarioDescription);
            callableStatement.setString(5, testcaseName);
            callableStatement.setTimestamp(6, new Timestamp(timestamp));
            callableStatement.registerOutParameter(indexRowsInserted, Types.INTEGER);
            callableStatement.registerOutParameter(indexTestcaseId, Types.INTEGER);

            callableStatement.execute();

            if (callableStatement.getInt(indexRowsInserted) != 1) {
                throw new DatabaseAccessException(errMsg);
            } else {
                if (callableStatement.getInt(indexTestcaseId) == 0) {
                    throw new DatabaseAccessException(errMsg + " - testcase id returned was 0");
                }
            }

            // get the result
            return callableStatement.getInt(indexTestcaseId);

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

    public void endTestCase(
                             int testcaseResult,
                             long timestamp,
                             int testcaseId,
                             boolean closeConnection ) throws DatabaseAccessException {

        final String errMsg = "Unable to end testcase with id " + testcaseId;

        timestamp = inUTC(timestamp);

        final int indexRowsInserted = 4;
        CallableStatement callableStatement = null;
        try {
            refreshInternalConnection();

            callableStatement = connection.prepareCall("{ call sp_end_testcase(?, ?, ?, ?) }");
            callableStatement.setInt(1, testcaseId);
            callableStatement.setInt(2, testcaseResult);
            callableStatement.setTimestamp(3, new Timestamp(timestamp));
            callableStatement.registerOutParameter(indexRowsInserted, Types.INTEGER);

            callableStatement.execute();
            if (callableStatement.getInt(indexRowsInserted) != 1) {
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

    public void updateTestcase(
                                String suiteFullName,
                                String scenarioName,
                                String scenarioDescription,
                                String testcaseName,
                                String userNote,
                                int testcaseResult,
                                int testcaseId,
                                long timestamp,
                                boolean closeConnection ) throws DatabaseAccessException {

        final String errMsg = "Unable to update testcase with name '" + testcaseName + "' and id " + testcaseId;

        timestamp = inUTC(timestamp);

        final int indexRowsUpdate = 9;

        CallableStatement callableStatement = null;
        try {
            refreshInternalConnection();

            callableStatement = connection.prepareCall("{ call sp_update_testcase(?, ?, ?, ?, ?, ?, ?, ?, ?) }");
            callableStatement.setInt(1, testcaseId);
            callableStatement.setString(2, suiteFullName);
            callableStatement.setString(3, scenarioName);
            callableStatement.setString(4, scenarioDescription);
            callableStatement.setString(5, testcaseName);
            callableStatement.setString(6, userNote);
            callableStatement.setInt(7, testcaseResult);
            callableStatement.setTimestamp(8, new Timestamp(timestamp));
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

    public void deleteTestcase(
                                List<Object> objectsToDelete ) throws DatabaseAccessException {

        StringBuilder testcaseIds = new StringBuilder();
        for (Object obj : objectsToDelete) {
            testcaseIds.append( ((Testcase) obj).testcaseId);
            testcaseIds.append(",");
        }
        testcaseIds.delete(testcaseIds.length() - 1, testcaseIds.length());

        final String errMsg = "Unable to delete testcase(s) with id " + testcaseIds;

        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        try {
            callableStatement = connection.prepareCall("{ call sp_delete_testcase(?) }");
            callableStatement.setString(1, testcaseIds.toString());
            callableStatement.execute();
        } catch (SQLException e) {
            throw new DatabaseAccessException(errMsg, e);
        } finally {
            DbUtils.close(connection, callableStatement);
        }
    }

    /**
     * Update meta info about an existing test case.
     *
     * @param testcaseId
     * @param metaKey
     * @param metaValue
     * @param closeConnection
     * @throws DatabaseAccessException
     */
    public void addTestcaseMetainfo(
                                     int testcaseId,
                                     String metaKey,
                                     String metaValue,
                                     boolean closeConnection ) throws DatabaseAccessException {

        final String errMsg = "Unable to add testcase meta info '" + metaKey + "=" + metaValue
                              + "' to testcase for with id " + testcaseId;

        final int indexRowsInserted = 4;

        CallableStatement callableStatement = null;
        try {
            refreshInternalConnection();

            callableStatement = connection.prepareCall("{ call sp_add_testcase_metainfo(?, ?, ?, ?) }");
            callableStatement.setInt(1, testcaseId);
            callableStatement.setString(2, metaKey);
            callableStatement.setString(3, metaValue);
            callableStatement.registerOutParameter(indexRowsInserted, Types.INTEGER);

            callableStatement.execute();
            if (callableStatement.getInt(indexRowsInserted) != 1) {
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

    public int startLoadQueue(
                               String name,
                               int sequence,
                               String hostsList,
                               String threadingPattern,
                               int numberThreads,
                               String machine,
                               long timestamp,
                               int testcaseId,
                               boolean closeConnection ) throws DatabaseAccessException {

        if (testcaseId < 1) {
            log.getLog4jLogger()
               .warn("Load queue '" + name
                     + "' will not be registered because there is no database connection!");
            return -1;
        }

        timestamp = inUTC(timestamp);

        final String errMsg = "Unable to start load queue with name " + name;

        // create a new load queue
        final int indexRowsInserted = 9;
        final int indexLoadQueueId = 10;

        CallableStatement callableStatement = null;
        try {
            refreshInternalConnection();

            callableStatement = connection.prepareCall("{ call sp_start_loadqueue(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }");
            callableStatement.setInt(1, testcaseId);
            callableStatement.setString(2, name);
            callableStatement.setInt(3, sequence);
            callableStatement.setString(4, hostsList);
            callableStatement.setString(5, threadingPattern);
            callableStatement.setInt(6, numberThreads);
            callableStatement.setString(7, machine);
            callableStatement.setTimestamp(8, new Timestamp(timestamp));
            callableStatement.registerOutParameter(indexRowsInserted, Types.INTEGER);
            callableStatement.registerOutParameter(indexLoadQueueId, Types.INTEGER);

            callableStatement.execute();

            if (callableStatement.getInt(indexRowsInserted) != 1) {
                throw new DatabaseAccessException(errMsg);
            } else {
                if (callableStatement.getInt(indexLoadQueueId) == 0) {
                    throw new DatabaseAccessException(errMsg + " - load queue id returned was 0");
                }
            }

            // get the result
            return callableStatement.getInt(indexLoadQueueId);

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

    public void endLoadQueue(
                              int result,
                              long timestamp,
                              int loadQueueId,
                              boolean closeConnection ) throws DatabaseAccessException {

        if (isBatchMode) {
            flushCache();
        }

        final String errMsg = "Unable to end load queue with id " + loadQueueId;

        timestamp = inUTC(timestamp);

        final int indexRowsInserted = 4;

        CallableStatement callableStatement = null;
        try {
            refreshInternalConnection();

            callableStatement = connection.prepareCall("{ call sp_end_loadqueue(?, ?, ?, ?) }");
            callableStatement.setInt(1, loadQueueId);
            callableStatement.setInt(2, result);
            callableStatement.setTimestamp(3, new Timestamp(timestamp));
            callableStatement.registerOutParameter(indexRowsInserted, Types.INTEGER);

            callableStatement.execute();
            if (callableStatement.getInt(indexRowsInserted) != 1) {
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

    public boolean insertMessage(
                                  String message,
                                  int level,
                                  boolean escapeHtml,
                                  String machineName,
                                  String threadName,
                                  long timestamp,
                                  int testCaseId,
                                  boolean closeConnection ) throws DatabaseAccessException {

        timestamp = inUTC(timestamp);

        Connection currentConnection;
        if (!isBatchMode) {
            currentConnection = refreshInternalConnection();
        } else {
            currentConnection = dbEventsCache.connection;
        }

        CallableStatement insertMessageStatement = insertFactory.getInsertTestcaseMessageStatement(currentConnection,
                                                                                                   message,
                                                                                                   level,
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

                if (insertMessageStatement.getUpdateCount() < 1) {
                    throw new DatabaseAccessException(errMsg);
                }
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

    public boolean insertRunMessage(
                                     String message,
                                     int level,
                                     boolean escapeHtml,
                                     String machineName,
                                     String threadName,
                                     long timestamp,
                                     int runId,
                                     boolean closeConnection ) throws DatabaseAccessException {

        String dbVersionString = getDatabaseVersion();
        int dbVersion = Integer.parseInt(dbVersionString.replace(".", "")); // TODO: fix version conversion like in 2 digit number cases

        if (dbVersion < 350) {

            return false;
        } else {

            timestamp = inUTC(timestamp);

            Connection currentConnection;
            if (!isBatchMode) {
                currentConnection = refreshInternalConnection();
            } else {
                currentConnection = dbEventsCache.connection;
            }

            CallableStatement insertMessageStatement = insertFactory.getInsertRunMessageStatement(currentConnection,
                                                                                                  message,
                                                                                                  level,
                                                                                                  escapeHtml,
                                                                                                  machineName,
                                                                                                  threadName,
                                                                                                  timestamp,
                                                                                                  runId);

            if (isBatchMode) {
                // schedule this event for batch execution
                return dbEventsCache.addInsertRunMessageEventToBatch(insertMessageStatement);
            } else {
                // execute this event now
                String errMsg = "Unable to insert run message '" + message + "'";

                try {
                    insertMessageStatement.execute();
                    if (insertMessageStatement.getUpdateCount() < 1) {
                        throw new DatabaseAccessException(errMsg);
                    }
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

    public boolean insertSuiteMessage(
                                       String message,
                                       int level,
                                       boolean escapeHtml,
                                       String machineName,
                                       String threadName,
                                       long timestamp,
                                       int suiteId,
                                       boolean closeConnection ) throws DatabaseAccessException {

        String dbVersionString = getDatabaseVersion();
        int dbVersion = Integer.parseInt(dbVersionString.replace(".", ""));

        if (dbVersion < 350) {

            return false;
        } else {

            timestamp = inUTC(timestamp);

            Connection currentConnection;
            if (!isBatchMode) {
                currentConnection = refreshInternalConnection();
            } else {
                currentConnection = dbEventsCache.connection;
            }

            CallableStatement insertMessageStatement = insertFactory.getInsertSuiteMessageStatement(currentConnection,
                                                                                                    message,
                                                                                                    level,
                                                                                                    escapeHtml,
                                                                                                    machineName,
                                                                                                    threadName,
                                                                                                    timestamp,
                                                                                                    suiteId);

            if (isBatchMode) {
                // schedule this event for batch execution
                return dbEventsCache.addInsertSuiteMessageEventToBatch(insertMessageStatement);
            } else {
                // execute this event now
                String errMsg = "Unable to insert suite message '" + message + "'";

                try {
                    insertMessageStatement.execute();
                    if (insertMessageStatement.getUpdateCount() < 1) {
                        throw new DatabaseAccessException(errMsg);
                    }
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

    public boolean insertCheckpoint(
                                     String name,
                                     long startTimestamp,
                                     long responseTime,
                                     long transferSize,
                                     String transferUnit,
                                     int result,
                                     int loadQueueId,
                                     boolean closeConnection ) throws DatabaseAccessException {

        startTimestamp = inUTC(startTimestamp);

        Connection currentConnection;
        if (!isBatchMode) {
            currentConnection = refreshInternalConnection();
        } else {
            currentConnection = dbEventsCache.connection;
        }

        CallableStatement insertCheckpointStatement = insertFactory.getInsertCheckpointStatement(currentConnection,
                                                                                                 name,
                                                                                                 responseTime,
                                                                                                 startTimestamp + responseTime,
                                                                                                 transferSize,
                                                                                                 transferUnit,
                                                                                                 result,
                                                                                                 checkpointLogLevel,
                                                                                                 loadQueueId);

        if (isBatchMode) {
            // schedule this event for batch execution
            return dbEventsCache.addInsertCheckpointEventToBatch(insertCheckpointStatement);
        } else {
            // execute this event now
            String errMsg = "Unable to insert checkpoint '" + name + "'";
            // final int indexRowsInserted = 8;

            try {
                insertCheckpointStatement.execute();
                // if( insertCheckpointStatement.getInt( indexRowsInserted ) < 1
                // ) {
                // throw new DatabaseAccessException( errMsg );
                // }
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

    /**
     * Get the current {@link CheckpointLogLevel} log level
     */
    public static CheckpointLogLevel getCheckpointLogLevel() {

        return checkpointLogLevel;
    }

    /**
     * Set the checkpoint log level. Default is {@link CheckpointLogLevel#SHORT}
     *
     *
     * @param newCheckpointLogLevel Options are {@link CheckpointLogLevel#FULL} - logging every single action into the
     *                               DB. <em>Note</em> that this might rapidly grow your DB. <br />
     *                               For {@link CheckpointLogLevel#SHORT} only total summary (aggregated status) is
     *                               updated.
     */
    public static void setCheckpointLogLevel(
                                              CheckpointLogLevel newCheckpointLogLevel ) {

        checkpointLogLevel = newCheckpointLogLevel;
    }

    public CheckpointInfo startCheckpoint(
                                           String name,
                                           String threadName,
                                           long startTimestamp,
                                           String transferUnit,
                                           int loadQueueId,
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
            callableStatement.setString(4, transferUnit);
            callableStatement.registerOutParameter(indexCheckpointSummaryId, Types.INTEGER);
            callableStatement.registerOutParameter(indexCheckpointId, Types.BIGINT);

            callableStatement.execute();

            // we always update the checkpoint summary table
            if (callableStatement.getInt(indexCheckpointSummaryId) == 0) {
                throw new DatabaseAccessException(errMsg + " - checkpoint summary ID returned was 0");
            }

            // we update the checkpoint table only in FULL mode
            if (checkpointLogLevel == CheckpointLogLevel.FULL
                && callableStatement.getLong(indexCheckpointId) == 0) {
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

    public void endCheckpoint(
                               CheckpointInfo runningCheckpointInfo,
                               long endTimestamp,
                               long transferSize,
                               int result,
                               boolean closeConnection ) throws DatabaseAccessException {

        final String errMsg = "Unable to end checkpoint with name '" + runningCheckpointInfo.getName()
                              + "', checkpoint summary id " + runningCheckpointInfo.getCheckpointSummaryId()
                              + ", id " + runningCheckpointInfo.getCheckpointId();

        endTimestamp = inUTC(endTimestamp);

        final int indexRowsInserted = 8;
        int responseTime = (int) (endTimestamp - runningCheckpointInfo.getStartTimestamp());

        CallableStatement callableStatement = null;
        try {
            refreshInternalConnection();

            callableStatement = connection.prepareCall("{ call sp_end_checkpoint(?, ?, ?, ?, ?, ?, ?, ?) }");
            callableStatement.setInt(1, runningCheckpointInfo.getCheckpointSummaryId());
            callableStatement.setLong(2, runningCheckpointInfo.getCheckpointId());
            callableStatement.setInt(3, responseTime >= 0
                                                          ? responseTime
                                                          : 0);
            callableStatement.setLong(4, transferSize);
            callableStatement.setInt(5, result);
            callableStatement.setInt(6, checkpointLogLevel.toInt());
            callableStatement.setTimestamp(7, new Timestamp(endTimestamp));
            callableStatement.registerOutParameter(indexRowsInserted, Types.INTEGER);

            callableStatement.execute();
            if (callableStatement.getInt(indexRowsInserted) != 1) {
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

    public void insertCheckpointSummary(
                                         String name,

                                         int numRunning,
                                         int numPassed,
                                         int numFailed,

                                         int minResponseTime,
                                         double avgResponseTime,
                                         int maxResponseTime,

                                         double minTransferRate,
                                         double avgTransferRate,
                                         double maxTransferRate,
                                         String transferRateUnit,
                                         int loadQueueId,
                                         boolean closeConnection ) throws DatabaseAccessException {

        final String errMsg = "Unable to insert checkpoint summary '" + name + "' for load queue "
                              + loadQueueId;

        PreparedStatement preparedStatement = null;
        try {
            refreshInternalConnection();

            preparedStatement = connection.prepareStatement("INSERT INTO tCheckpointsSummary"
                                                            + " (name,numRunning,numPassed,numFailed,minResponseTime,avgResponseTime,maxResponseTime,minTransferRate,avgTransferRate,maxTransferRate,transferRateUnit,loadQueueId) "
                                                            + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
            preparedStatement.setString(1, name);

            preparedStatement.setInt(2, numRunning);
            preparedStatement.setInt(3, numPassed);
            preparedStatement.setInt(4, numFailed);

            preparedStatement.setInt(5, minResponseTime);
            preparedStatement.setDouble(6, avgResponseTime);
            preparedStatement.setInt(7, maxResponseTime);

            preparedStatement.setDouble(8, minTransferRate);
            preparedStatement.setDouble(9, avgTransferRate);
            preparedStatement.setDouble(10, maxTransferRate);

            preparedStatement.setString(11, transferRateUnit);

            preparedStatement.setInt(12, loadQueueId);

            int updatedRecords = preparedStatement.executeUpdate();
            if (updatedRecords != 1) {
                throw new DatabaseAccessException(errMsg);
            }
        } catch (SQLException e) {
            throw new DatabaseAccessException(errMsg, e);
        } finally {
            if (closeConnection) {
                DbUtils.close(connection, preparedStatement);
            } else {
                DbUtils.closeStatement(preparedStatement);
            }
        }
    }

    public void insertSystemStatistics(
                                        int testCaseId,
                                        String machine,
                                        String statisticIds,
                                        String statisticValues,
                                        long timestamp,
                                        boolean closeConnection ) throws DatabaseAccessException {

        timestamp = inUTC(timestamp);

        CallableStatement callableStatement = null;
        try {
            refreshInternalConnection();

            callableStatement = connection.prepareCall("{ call sp_insert_system_statistic_by_ids(?, ?, ?, ?, ?) }");
            callableStatement.setString(3, statisticIds);
            callableStatement.setInt(1, testCaseId);
            callableStatement.setString(2, machine);
            callableStatement.setString(4, statisticValues);
            callableStatement.setTimestamp(5, new Timestamp(timestamp));

            callableStatement.execute();

        } catch (Exception e) {
            String errMsg = "Unable to insert system statistics, statistic IDs '" + statisticIds
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

    public void insertUserActivityStatistics(
                                              int testCaseId,
                                              String machine,
                                              String statisticIds,
                                              String statisticValues,
                                              long timestamp,
                                              boolean closeConnection ) throws DatabaseAccessException {

        timestamp = inUTC(timestamp);

        CallableStatement callableStatement = null;
        try {
            refreshInternalConnection();

            callableStatement = connection.prepareCall("{ call sp_insert_user_activity_statistic_by_ids(?, ?, ?, ?, ?) }");
            callableStatement.setString(3, statisticIds);
            callableStatement.setInt(1, testCaseId);
            callableStatement.setString(2, machine);
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

    public int populateCheckpointSummary( int loadQueueId, String name, String transferRateUnit,
                                          boolean closeConnection ) throws DatabaseAccessException {

        final String errMsg = "Unable to populate checkpoint summary '" + name + "' in load queue " + loadQueueId;

        final int indexCheckpointSummaryId = 4;

        CallableStatement callableStatement = null;
        try {
            refreshInternalConnection();

            callableStatement = connection.prepareCall("{ call sp_populate_checkpoint_summary(?, ?, ?, ?) }");
            callableStatement.setInt(1, loadQueueId);
            callableStatement.setString(2, name);
            callableStatement.setString(3, transferRateUnit);
            callableStatement.registerOutParameter(indexCheckpointSummaryId, Types.INTEGER);

            callableStatement.execute();

            if (callableStatement.getInt(indexCheckpointSummaryId) == 0) {
                throw new DatabaseAccessException(errMsg + " - checkpoint summary ID returned was 0");
            }

            return callableStatement.getInt(indexCheckpointSummaryId);

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

    public int populateSystemStatisticDefinition(
                                                  String name,
                                                  String parentName,
                                                  String internalName,
                                                  String unit,
                                                  String params ) throws DatabaseAccessException {

        if (parentName == null) {
            parentName = "";
        }
        if (internalName == null) {
            internalName = "";
        }

        CallableStatement callableStatement = null;
        Connection con = null;
        boolean useLocalConnection = false;
        try {
            if (connection == null || connection.isClosed()) {
                // connection not set externally so use new connection only for
                // this method invocation
                useLocalConnection = true;
                con = getConnection();
            } else {
                useLocalConnection = false;
                con = connection;
            }
            final int statisticId = 6;
            callableStatement = con.prepareCall("{ call sp_populate_system_statistic_definition(?, ?, ?, ?, ?, ?) }");
            callableStatement.setString(1, parentName);
            callableStatement.setString(2, internalName);
            callableStatement.setString(3, name);
            callableStatement.setString(4, unit);
            callableStatement.setString(5, params);
            callableStatement.registerOutParameter(statisticId, Types.INTEGER);

            callableStatement.execute();

            return callableStatement.getInt(statisticId);

        } catch (Exception e) {
            String errMsg = "Unable to populate statistic '" + name + "' with unit '" + unit
                            + "' and params '" + params + "'";
            throw new DatabaseAccessException(errMsg, e);
        } finally {
            DbUtils.closeStatement(callableStatement);
            if (useLocalConnection) {
                DbUtils.closeConnection(con);
            }
        }
    }

    public void updateMachineInfo(
                                   String machineName,
                                   String machineInfo,
                                   boolean closeConnection ) throws DatabaseAccessException {

        final String errMsg = "Unable to update the info about machine with name " + machineName;

        // then start the run
        final int indexRowsInserted = 3;

        CallableStatement callableStatement = null;
        try {
            refreshInternalConnection();

            callableStatement = connection.prepareCall("{ call sp_update_machine_info(?, ?, ?) }");
            callableStatement.setString(1, machineName);
            callableStatement.setString(2, machineInfo);
            callableStatement.registerOutParameter(indexRowsInserted, Types.INTEGER);

            callableStatement.execute();
            if (callableStatement.getInt(indexRowsInserted) != 1) {
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

    public boolean isRunPresent(
                                 int runId ) throws DatabaseAccessException {

        Connection connection = getConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.prepareStatement("SELECT COUNT(*) FROM tRuns WHERE runId = " + runId);
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

    public boolean isSuitePresent(
                                   int suiteId ) throws DatabaseAccessException {

        Connection connection = getConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.prepareStatement("SELECT COUNT(*) FROM tSuites WHERE suiteId = "
                                                    + suiteId);
            rs = statement.executeQuery();
            if (rs.next()) {
                return 1 == rs.getInt(1);
            }
        } catch (Exception e) {
            throw new DatabaseAccessException("Error checking whether suite with id " + suiteId + " exists",
                                              e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, statement);
        }

        return false;
    }

    public boolean isTestcasePresent(
                                      int testcaseId ) throws DatabaseAccessException {

        Connection connection = getConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.prepareStatement("SELECT COUNT(*) FROM tTestcases WHERE testcaseId = "
                                                    + testcaseId);
            rs = statement.executeQuery();
            if (rs.next()) {
                return 1 == rs.getInt(1);
            }
        } catch (Exception e) {
            throw new DatabaseAccessException("Error checking whether testcase with id " + testcaseId
                                              + " exists", e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, statement);
        }

        return false;
    }

    /**
     * Expected to be called only in batch mode. Flush any pending events
     *
     * @throws DatabaseAccessException
     */
    public void flushCache() throws DatabaseAccessException {

        dbEventsCache.flushCache();
    }

    /**
     * Expected to be called only in batch mode. Flush any pending events in
     * case the cache is full or it is too old
     *
     * @throws DatabaseAccessException
     */
    public void flushCacheIfNeeded() throws DatabaseAccessException {

        dbEventsCache.flushCacheIfNeeded();
    }

    public void runDbSanityCheck() throws DatabaseAccessException {

        DatabaseAccessException dbae = null;

        final String SANITY_PRODUCT = "SanityCheck(TestProduct)";
        final String SANITY_VERSION = "SanityCheck(TestVersion)";
        final String SANITY_BUILD = "SanityCheck(TestBuild)";
        final String SANITY_RUN = "SanityCheck(TestRun)";
        final String SANITY_OS = "SanityCheck(TestOS)";
        final String SANITY_SUITE = "SanityCheck(TestSuite)";
        final String SANITY_SCENARIO = "SanityCheck(TestScenario)";
        final String SANITY_TESTCASE = "SanityCheck(Testcase)";
        final String SANITY_LOADQUEUE = "SanityCheck(TestLoadqueue)";
        final String SANITY_CHECKPOINT = "SanityCheck(TestCheckpoint)";
        final String SANITY_MESSAGE = "SanityCheck(TestMessage)";
        final String SANITY_DESCRIPTION = "sanity description";
        final String SANITY_HOSTNAME = "SanityCheck(TestHostName)";

        boolean originalAutoCommitState = false;
        try {
            this.connection = getConnection();
            // in sanity mode, the connection will be reused
            sanityRun = true;

            String javaFrameworkVersion = AtsVersion.getAtsVersion();
            log.info("ATS framework version is '"
                     + javaFrameworkVersion + "'");

            log.info("Checking for ATS log database connection with the following parameters: "
                     + connection.toString());

            String databaseVersion = getDatabaseVersion();
            log.info("ATS Log database version is '"
                     + databaseVersion + "'");

            if (!javaFrameworkVersion.equalsIgnoreCase(databaseVersion)) {
                log.warn("You are using ATS version " + javaFrameworkVersion
                         + " with Log database version " + databaseVersion
                         + ". This might cause incompatibility problems!");
            }

            originalAutoCommitState = connection.getAutoCommit();
            // disable auto commit
            connection.setAutoCommit(false);

            long timestamp = Calendar.getInstance().getTimeInMillis();

            // start everything
            int runId = startRun(SANITY_RUN,
                                 SANITY_OS,
                                 SANITY_PRODUCT,
                                 SANITY_VERSION,
                                 SANITY_BUILD,
                                 timestamp,
                                 SANITY_HOSTNAME,
                                 false);

            // insert a run message
            insertRunMessage(SANITY_MESSAGE,
                             5,
                             false,
                             "machine0",
                             "group1-thread2",
                             timestamp,
                             runId,
                             false);

            int suiteId = startSuite("SANITY_PACKAGE", SANITY_SUITE, timestamp, runId, false);

            // insert a run message
            insertSuiteMessage(SANITY_MESSAGE,
                               5,
                               false,
                               "machine0",
                               "group1-thread2",
                               timestamp,
                               suiteId,
                               false);

            int testcaseId = startTestCase(SANITY_SUITE,
                                           SANITY_SCENARIO,
                                           SANITY_DESCRIPTION,
                                           SANITY_TESTCASE,
                                           timestamp,
                                           suiteId,
                                           false);

            // insert a test message
            insertMessage(SANITY_MESSAGE,
                          5,
                          false,
                          "machine0",
                          "group1-thread2",
                          timestamp,
                          testcaseId,
                          false);

            // insert a checkpoint
            int loadQueueId = startLoadQueue(SANITY_LOADQUEUE,
                                             0,
                                             "127.0.0.1:8080",
                                             "AllAtOnce",
                                             10,
                                             "localhost",
                                             timestamp,
                                             testcaseId,
                                             false);

            populateCheckpointSummary(loadQueueId, SANITY_CHECKPOINT, "KB", false);

            CheckpointInfo startedCheckpointInfo = startCheckpoint(SANITY_CHECKPOINT,
                                                                   "thread1",
                                                                   1000,
                                                                   "KB",
                                                                   loadQueueId,
                                                                   false);
            endCheckpoint(startedCheckpointInfo, 2000, 100, CheckpointResult.PASSED.toInt(), false);

            int statisticId1 = populateSystemStatisticDefinition("running users",
                                                                 "",
                                                                 "",
                                                                 "count",
                                                                 "param1_1");
            int statisticId2 = populateSystemStatisticDefinition("standby users",
                                                                 "",
                                                                 "",
                                                                 "count",
                                                                 "param2_1");
            insertSystemStatistics(testcaseId,
                                   "localhost",
                                   statisticId1 + "_" + statisticId2,
                                   "30_1",
                                   System.currentTimeMillis(),
                                   false);

            endLoadQueue(LoadQueueResult.PASSED.toInt(), timestamp, loadQueueId, false);

            // end everything
            endTestCase(1, timestamp, testcaseId, false);
            endSuite(timestamp, suiteId, false);
            endRun(timestamp, runId, false);

        } catch (SQLException sqle) {
            String errorMessage = "Unable to insert sanity check sample data";
            log.error(DbUtils.getFullSqlException(errorMessage, sqle));
            dbae = new DatabaseAccessException(errorMessage, sqle);
        } finally {

            if (dbEventsCache != null) {
                // it is in batch mode, we want to cleanup the events cached
                // while running the sanity check
                dbEventsCache.resetCache();
            }

            sanityRun = false;
            try {
                // rollback the connection
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException sqle) {
                String errorMessage = "Unable to revert sanity check data";
                log.error(DbUtils.getFullSqlException(errorMessage, sqle));
                if (dbae == null) {
                    dbae = new DatabaseAccessException(errorMessage, sqle);
                } else {
                    log.error("The transaction could not be rolled back, possible cause '"
                              + dbae.getMessage() + "'");
                }
            } finally {
                try {
                    if (connection != null) {
                        connection.setAutoCommit(originalAutoCommitState);
                    }
                } catch (SQLException e) { // do not hide the possible exception
                                           // in the rollback() catch block
                    log.error(DbUtils.getFullSqlException("Could not restore connection's autocommit state",
                                                          e));
                } finally {
                    DbUtils.closeConnection(connection);
                }
            }
        }
        // we check if there is thrown exception, the first thrown exception is
        // with priority
        if (dbae != null) {
            throw dbae;
        }
    }

    public void setSkipUTCConversion( boolean skip ) {

        this.skipUTCConversion = skip;
    }

    /**
     * @return the current connection object
     * @throws DatabaseAccessException
     */
    protected Connection refreshInternalConnection() throws DatabaseAccessException {

        if (!sanityRun) {
            this.connection = getConnection();
        }

        return this.connection;
    }

    /**
     * Set connection for use in DbAccess methods only
     * 
     * @param connection
     */
    protected void setInternalConnection(
                                          Connection connection ) throws IllegalStateException,
                                                                  IllegalArgumentException {

        if (connection == null) {
            throw new IllegalArgumentException("Connection to set could not be null. For clearing connection use clearInternalConnection() method");
        }
        if (this.connection != null) {
            throw new IllegalStateException("Trying to set new connection when previous is not cleared. Check if connection is cleared immediately after db access work is completed.");
        }
        this.connection = connection;
    }

    protected long inUTC( long timestamp ) {

        if (!skipUTCConversion) {

            return timestamp - TimeZone.getDefault().getOffset(timestamp);
        }

        return timestamp;
    }

    protected String constructStoredProcedureArgumentsMap( String procedureName, List<Object> arguments ) {

        StringBuilder sb = new StringBuilder();

        sb.append(procedureName + "(");

        if (arguments != null && arguments.size() > 0) {
            for (Object arg : arguments) {
                if (arg instanceof String || arg instanceof CharSequence || arg instanceof Character) {
                    sb.append("'" + arg + "'").append(", ");
                } else {
                    sb.append(arg);
                }
            }

            sb.setLength(sb.length() - 1); // remove trailing comma
        }

        sb.append(")");

        return sb.toString();
    }

    /**
     * The events cache processor
     */
    protected class DbEventsCache {

        private long maxCacheWaitTime = TimeUnit.SECONDS.toMillis(
                AtsSystemProperties.getPropertyAsNumber(AtsSystemProperties.LOG__MAX_CACHE_EVENTS_FLUSH_TIMEOUT, 10));

        private long cacheBirthTime;
        private int  maxNumberOfCachedEvents = AbstractDbAccess.DEFAULT_CHUNK_SIZE;

        protected Connection connection;

        private CallableStatement insertRunMessageStatement = null;
        private int               numberCachedRunMessages;

        private CallableStatement      insertSuiteMessageStatement = null;
        private int                    numberCachedSuiteMessages;

        private CallableStatement      insertTestcaseMessageStatement = null;
        private int                    numberCachedTestcaseMessages;

        private CallableStatement      insertCheckpointStatement      = null;
        private int                    numberCachedCheckpoints;

        private SQLServerDbWriteAccess parent;

        // temporary variables used for telling the user how long it takes to
        // commit the cached events
        private long                   batchStartTime;
        private int                    batchCheckpoints;
        private int                    batchMessages;

        public DbEventsCache( SQLServerDbWriteAccess parent ) throws DatabaseAccessException {

            this.parent = parent;
            this.connection = this.parent.getConnection();
            try {
                this.connection.setAutoCommit(false);
            } catch (SQLException e) {
                throw new DatabaseAccessException("Unable to set batch mode on DB connection", e);
            }

            numberCachedRunMessages = 0;
            numberCachedSuiteMessages = 0;
            numberCachedTestcaseMessages = 0;
            numberCachedCheckpoints = 0;
        }

        public Connection getConnection() {

            return this.connection;
        }

        /**
         * Specify max number of events to be collected for batch mode.
         * Note that if invoked this should be done early enough before any DB insert operation
         * Default value is {@link AbstractDbAccess#DEFAULT_CHUNK_SIZE}
         * @param maxNumberOfCachedEvents
         */
        public void setMaxNumberOfCachedEvents( int maxNumberOfCachedEvents ) {

            this.maxNumberOfCachedEvents = maxNumberOfCachedEvents;
        }

        public boolean addInsertRunMessageEventToBatch(
                                                        CallableStatement insertMessageStatement ) throws DatabaseAccessException {

            if (this.insertRunMessageStatement == null) {
                this.insertRunMessageStatement = insertMessageStatement;
            }
            try {
                this.insertRunMessageStatement.addBatch();
                ++numberCachedRunMessages;
            } catch (Exception e) {
                throw new DatabaseAccessException("Unable to schedule run message for batch execution", e);
            }

            updateCacheBirthtime();

            return flushCacheIfNeeded();
        }

        public boolean addInsertSuiteMessageEventToBatch(
                                                          CallableStatement insertMessageStatement ) throws DatabaseAccessException {

            if (this.insertSuiteMessageStatement == null) {
                this.insertSuiteMessageStatement = insertMessageStatement;
            }
            try {
                this.insertSuiteMessageStatement.addBatch();
                ++numberCachedSuiteMessages;
            } catch (Exception e) {
                throw new DatabaseAccessException("Unable to schedule suite message for batch execution",
                                                  e);
            }

            updateCacheBirthtime();

            return flushCacheIfNeeded();
        }

        public boolean addInsertTestcaseMessageEventToBatch(
                                                             CallableStatement insertMessageStatement ) throws DatabaseAccessException {

            if (this.insertTestcaseMessageStatement == null) {
                this.insertTestcaseMessageStatement = insertMessageStatement;
            }
            try {
                this.insertTestcaseMessageStatement.addBatch();
                ++numberCachedTestcaseMessages;
            } catch (Exception e) {
                throw new DatabaseAccessException("Unable to schedule testcase message for batch execution",
                                                  e);
            }

            updateCacheBirthtime();

            return flushCacheIfNeeded();
        }

        protected boolean addInsertCheckpointEventToBatch(
                                                           CallableStatement insertCheckpointStatement ) throws DatabaseAccessException {

            if (this.insertCheckpointStatement == null) {
                this.insertCheckpointStatement = insertCheckpointStatement;
            }
            try {
                this.insertCheckpointStatement.addBatch();
                ++numberCachedCheckpoints;
            } catch (Exception e) {
                throw new DatabaseAccessException("Unable to schedule a checkpoint for batch execution", e);
            }

            updateCacheBirthtime();

            return flushCacheIfNeeded();
        }

        private void updateCacheBirthtime() {

            // if this is the first event, we have to remember the cache birth
            // time
            if (numberCachedRunMessages + numberCachedSuiteMessages + numberCachedTestcaseMessages
                + numberCachedCheckpoints == 1) {
                cacheBirthTime = System.currentTimeMillis();
            }
        }

        public void flushCache() throws DatabaseAccessException {

            if (numberCachedRunMessages + numberCachedSuiteMessages + numberCachedTestcaseMessages
                + numberCachedCheckpoints == 0) {
                // no events in the cache
                return;
            }

            if (isMonitorEventsQueue) {
                batchStartTime = System.currentTimeMillis();
                batchCheckpoints = numberCachedCheckpoints;
                batchMessages = numberCachedRunMessages + numberCachedSuiteMessages
                                + numberCachedTestcaseMessages;
            }

            flushInsertRunMessageEvents();
            flushInsertSuiteMessageEvents();
            flushInsertTestcaseMessageEvents();
            flushInsertCheckpointEvents();

            cacheBirthTime = 0;

            if (isMonitorEventsQueue) {
                log.getLog4jLogger()
                   .info("Flushed "
                         + batchCheckpoints + " checkpoints and " + batchMessages + " messages in "
                         + (System.currentTimeMillis() - batchStartTime) + " ms");
            }
        }

        private boolean flushCacheIfNeeded() throws DatabaseAccessException {

            boolean isTimeToFlush = false;

            int numberEvents = numberCachedRunMessages + numberCachedSuiteMessages
                               + numberCachedTestcaseMessages + numberCachedCheckpoints;
            if (numberEvents > 0) {
                if (numberEvents >= maxNumberOfCachedEvents) {
                    isTimeToFlush = true;
                } else if (System.currentTimeMillis() - cacheBirthTime >= maxCacheWaitTime) {
                    isTimeToFlush = true;
                }
            }

            if (isTimeToFlush) {
                flushCache();
            }

            return isTimeToFlush;
        }

        private void flushInsertRunMessageEvents() throws DatabaseAccessException {

            if (numberCachedRunMessages == 0) {
                return;
            }

            boolean gotError = false;
            try {
                insertRunMessageStatement.executeBatch();

                // data sent to the DB, commit the transaction
                connection.commit();
            } catch (Exception e) {

                /*
                 * The next code is not used for now if( e instanceof
                 * BatchUpdateException ) { processUpdateCounts( (
                 * BatchUpdateException ) e ); }
                 */

                // rollback the entire transaction
                try {
                    connection.rollback();

                    log.getLog4jLogger()
                       .error(ExceptionUtils.getExceptionMsg(e,
                                                             "Commit failed while inserting "
                                                                + numberCachedRunMessages
                                                                + " run messages in one transaction"));
                } catch (Exception rollbackException) {
                    gotError = true;

                    log.getLog4jLogger()
                       .error(ExceptionUtils.getExceptionMsg(rollbackException,
                                                             "Commit and rollback both failed while inserting "
                                                                                + numberCachedRunMessages
                                                                                + " run messages in one transaction."
                                                                                + " Following is the rollback exception ..."));
                    rollbackException.printStackTrace();
                }
            } finally {
                resetRunMessagesCache();
            }

            if (gotError) {
                connection = parent.refreshInternalConnection();
            }
        }

        private void flushInsertSuiteMessageEvents() throws DatabaseAccessException {

            if (numberCachedSuiteMessages == 0) {
                return;
            }

            boolean gotError = false;
            try {
                insertSuiteMessageStatement.executeBatch();

                // data sent to the DB, commit the transaction
                connection.commit();
            } catch (Exception e) {

                /*
                 * The next code is not used for now if( e instanceof
                 * BatchUpdateException ) { processUpdateCounts( (
                 * BatchUpdateException ) e ); }
                 */

                // rollback the entire transaction
                try {
                    connection.rollback();

                    log.getLog4jLogger()
                       .error(ExceptionUtils.getExceptionMsg(e,
                                                             "Commit failed while inserting "
                                                                + numberCachedSuiteMessages
                                                                + " suite messages in one transaction"));
                } catch (Exception rollbackException) {
                    gotError = true;

                    log.getLog4jLogger()
                       .error(ExceptionUtils.getExceptionMsg(rollbackException,
                                                             "Commit and rollback both failed while inserting "
                                                                                + numberCachedSuiteMessages
                                                                                + " suite messages in one transaction."
                                                                                + " Following is the rollback exception ..."));
                    rollbackException.printStackTrace();
                }
            } finally {
                resetSuiteMessagesCache();
            }

            if (gotError) {
                connection = parent.refreshInternalConnection();
            }
        }

        private void flushInsertTestcaseMessageEvents() throws DatabaseAccessException {

            if (numberCachedTestcaseMessages == 0) {
                return;
            }

            boolean gotError = false;
            try {
                insertTestcaseMessageStatement.executeBatch();

                // data sent to the DB, commit the transaction
                connection.commit();
            } catch (Exception e) {

                /*
                 * The next code is not used for now if( e instanceof
                 * BatchUpdateException ) { processUpdateCounts( (
                 * BatchUpdateException ) e ); }
                 */

                // rollback the entire transaction
                try {
                    connection.rollback();

                    log.getLog4jLogger()
                       .error(ExceptionUtils.getExceptionMsg(e,
                                                             "Commit failed while inserting "
                                                                + numberCachedTestcaseMessages
                                                                + " testcase messages in one transaction"));
                } catch (Exception rollbackException) {
                    gotError = true;

                    log.getLog4jLogger()
                       .error(ExceptionUtils.getExceptionMsg(rollbackException,
                                                             "Commit and rollback both failed while inserting "
                                                                                + numberCachedTestcaseMessages
                                                                                + " testcase messages in one transaction."
                                                                                + " Following is the rollback exception ..."));
                    rollbackException.printStackTrace();
                }
            } finally {
                resetTestcaseMessagesCache();
            }

            if (gotError) {
                connection = parent.refreshInternalConnection();
            }
        }

        private void flushInsertCheckpointEvents() throws DatabaseAccessException {

            if (numberCachedCheckpoints == 0) {
                return;
            }

            boolean gotError = false;
            try {
                insertCheckpointStatement.executeBatch(); // TODO - check possible result including possible error statuses

                // data sent to the DB, commit the transaction
                connection.commit();
            } catch (Exception e) {

                /*
                 * The next code is not used for now if( e instanceof
                 * BatchUpdateException ) { processUpdateCounts( (
                 * BatchUpdateException ) e ); }
                 */

                // rollback the entire transaction
                try {
                    connection.rollback();

                    log.getLog4jLogger()
                       .error(ExceptionUtils.getExceptionMsg(e,
                                                             "Commit failed while inserting "
                                                                + numberCachedCheckpoints
                                                                + " checkpoints in one transaction"));
                } catch (Exception rollbackException) {
                    gotError = true;

                    log.getLog4jLogger()
                       .error(ExceptionUtils.getExceptionMsg(rollbackException,
                                                             "Commit and rollback both failed while inserting "
                                                                                + numberCachedCheckpoints
                                                                                + " checkpoints in one transaction."
                                                                                + " Following is the rollback exception ..."));
                    rollbackException.printStackTrace();
                }
            } finally {
                resetCheckpointsCache();
            }

            if (gotError) {
                connection = parent.refreshInternalConnection();
            }
        }

        protected void resetCache() {

            resetRunMessagesCache();
            resetSuiteMessagesCache();
            resetTestcaseMessagesCache();
            resetCheckpointsCache();
        }

        private void resetRunMessagesCache() {

            if (numberCachedRunMessages > 0) {
                numberCachedRunMessages = 0;
                try {
                    insertRunMessageStatement.clearBatch();
                } catch (SQLException e) {}
                insertRunMessageStatement = null;
            }
        }

        private void resetSuiteMessagesCache() {

            if (numberCachedSuiteMessages > 0) {
                numberCachedSuiteMessages = 0;
                try {
                    insertSuiteMessageStatement.clearBatch();
                } catch (SQLException e) {}
                insertSuiteMessageStatement = null;
            }
        }

        private void resetTestcaseMessagesCache() {

            if (numberCachedTestcaseMessages > 0) {
                numberCachedTestcaseMessages = 0;
                try {
                    insertTestcaseMessageStatement.clearBatch();
                } catch (SQLException e) {}
                insertTestcaseMessageStatement = null;
            }
        }

        private void resetCheckpointsCache() {

            if (numberCachedCheckpoints > 0) {
                numberCachedCheckpoints = 0;
                try {
                    insertCheckpointStatement.clearBatch();
                } catch (SQLException e) {}
                insertCheckpointStatement = null;
            }
        }

        /*
         * NOT SURE IF WE NEED TO DO SUCH CHECKS private static void
         * processUpdateCounts( BatchUpdateException bue ) {
         * 
         * int[] updateCounts = bue.getUpdateCounts();
         * 
         * // Some databases will continue to execute after one fails. // If so,
         * updateCounts.length will equal the number of batched statements. //
         * If not, updateCounts.length will equal the number of successfully
         * executed statements
         * 
         * for( int i = 0; i < updateCounts.length; i++ ) { if( updateCounts[i]
         * >= 0 ) { // Successfully executed; the number represents number of
         * affected rows } else if( updateCounts[i] == Statement.SUCCESS_NO_INFO
         * ) { // Successfully executed; number of affected rows not available }
         * else if( updateCounts[i] == Statement.EXECUTE_FAILED ) { // Failed to
         * execute } } }
         */
    }

    /**
     * Provides the event statements
     */
    protected class InsertEventStatementsFactory {

        private boolean             isBatchMode;

        private CallableStatement   insertRunMessageStatement;
        private CallableStatement   insertSuiteMessageStatement;
        private CallableStatement   insertTestcaseMessageStatement;
        private CallableStatement   insertCheckpointStatement;

        private static final String SP_INSERT_RUN_MESSAGE      = "{ call sp_insert_run_message(?, ?, ?, ?, ?, ?, ?) }";
        private static final String SP_INSERT_SUITE_MESSAGE    = "{ call sp_insert_suite_message(?, ?, ?, ?, ?, ?, ?) }";
        private static final String SP_INSERT_TESTCASE_MESSAGE = "{ call sp_insert_message(?, ?, ?, ?, ?, ?, ?) }";
        private static final String SP_INSERT_CHECKPOINT       = "{ call sp_insert_checkpoint(?, ?, ?, ?, ?, ?, ?, ?) }";

        public InsertEventStatementsFactory( boolean isBatchMode ) {

            this.isBatchMode = isBatchMode;
        }

        public CallableStatement getInsertTestcaseMessageStatement(
                                                                    Connection connection,
                                                                    String message,
                                                                    int level,
                                                                    boolean escapeHtml,
                                                                    String machineName,
                                                                    String threadName,
                                                                    long timestamp,
                                                                    int testCaseId ) throws DatabaseAccessException {

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
                throw new DatabaseAccessException("Unable to create SQL statement for inserting a message",
                                                  e);
            }

            try {
                // apply statement parameters
                theStatement.setInt(1, testCaseId);
                theStatement.setInt(2, level);
                theStatement.setString(3, message);
                theStatement.setBoolean(4, escapeHtml);
                theStatement.setString(5, machineName);
                theStatement.setString(6, threadName);
                theStatement.setTimestamp(7, new Timestamp(timestamp));

                return theStatement;
            } catch (Exception e) {
                throw new DatabaseAccessException("Unable to set parameters for inserting a message '"
                                                  + message + "'", e);
            }
        }

        public CallableStatement getInsertRunMessageStatement(
                                                               Connection connection,
                                                               String message,
                                                               int level,
                                                               boolean escapeHtml,
                                                               String machineName,
                                                               String threadName,
                                                               long timestamp,
                                                               int runId ) throws DatabaseAccessException {

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
                throw new DatabaseAccessException("Unable to create SQL statement for inserting a run message",
                                                  e);
            }

            try {
                // apply statement parameters
                theStatement.setInt(1, runId);
                theStatement.setInt(2, level);
                theStatement.setString(3, message);
                theStatement.setBoolean(4, escapeHtml);
                theStatement.setString(5, machineName);
                theStatement.setString(6, threadName);
                theStatement.setTimestamp(7, new Timestamp(timestamp));

                return theStatement;
            } catch (Exception e) {
                throw new DatabaseAccessException("Unable to set parameters for inserting a run message '"
                                                  + message + "'", e);
            }
        }

        public CallableStatement getInsertSuiteMessageStatement(
                                                                 Connection connection,
                                                                 String message,
                                                                 int level,
                                                                 boolean escapeHtml,
                                                                 String machineName,
                                                                 String threadName,
                                                                 long timestamp,
                                                                 int suiteId ) throws DatabaseAccessException {

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
                throw new DatabaseAccessException("Unable to create SQL statement for inserting a suite message",
                                                  e);
            }

            try {
                // apply statement parameters
                theStatement.setInt(1, suiteId);
                theStatement.setInt(2, level);
                theStatement.setString(3, message);
                theStatement.setBoolean(4, escapeHtml);
                theStatement.setString(5, machineName);
                theStatement.setString(6, threadName);
                theStatement.setTimestamp(7, new Timestamp(timestamp));

                return theStatement;
            } catch (Exception e) {
                throw new DatabaseAccessException("Unable to set parameters for inserting a suite message '"
                                                  + message + "'", e);
            }
        }

        CallableStatement getInsertCheckpointStatement(
                                                        Connection connection,
                                                        String name,
                                                        long responseTime,
                                                        long endTimestamp,
                                                        long transferSize,
                                                        String transferUnit,
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
                throw new DatabaseAccessException("Unable to create SQL statement for inserting checkpoints",
                                                  e);
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
                throw new DatabaseAccessException("Unable to set parameters for inserting a checkpoint '"
                                                  + name + "'", e);
            }
        }
    }
}
