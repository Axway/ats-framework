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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.DbUtils;
import com.axway.ats.core.utils.BackwardCompatibility;
import com.axway.ats.log.AtsDbReader;
import com.axway.ats.log.autodb.SqlRequestFormatter;
import com.axway.ats.log.autodb.entities.Checkpoint;
import com.axway.ats.log.autodb.entities.CheckpointSummary;
import com.axway.ats.log.autodb.entities.LoadQueue;
import com.axway.ats.log.autodb.entities.Machine;
import com.axway.ats.log.autodb.entities.Message;
import com.axway.ats.log.autodb.entities.Run;
import com.axway.ats.log.autodb.entities.RunMetaInfo;
import com.axway.ats.log.autodb.entities.Scenario;
import com.axway.ats.log.autodb.entities.ScenarioMetaInfo;
import com.axway.ats.log.autodb.entities.Statistic;
import com.axway.ats.log.autodb.entities.StatisticDescription;
import com.axway.ats.log.autodb.entities.Suite;
import com.axway.ats.log.autodb.entities.Testcase;
import com.axway.ats.log.autodb.entities.TestcaseMetainfo;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.log.autodb.model.IDbReadAccess;

public class SQLServerDbReadAccess extends AbstractDbAccess implements IDbReadAccess {

    /*  Some methods has an argument 'dayLightSavingOn'.
     *  This argument is used to align the time stamp for Time zones, that have Day-light saving 
     *  Another common arguments is timeOffset.
     *  This argument is the current time zone offset from UTC.
     *  Since all time stamps are send to the Database in UTC format, via this argument,
     *  all received time stamps will be with proper time localization.
     * */

    /*
     *  Test Explorer needs some statistic id in order to quickly distinguish between different statistics.
     *  Some statistics do not have a type ID from the DB, like:
     *      - action responses - they are found by only name and queue name
     *      - combined statistics - they combine the info from real DB statistics
     *
     *  We must guarantee these IDs are unique - they must not be used by the other types of statistics.
     *  Regular statistics have DB IDs starting from 0. For the rest we calculate the IDs
     *  starting from MAX_INTEGER and going down
     */
    private static final int   START_FAKE_ID_VALUE_FOR_CHECKPOINTS = Integer.MAX_VALUE;

    public static final String MACHINE_NAME_FOR_ATS_AGENTS         = "ATS Agents";

    public SQLServerDbReadAccess( DbConnection dbConnection ) {

        super(dbConnection);
    }

    @BackwardCompatibility
    public List<Run> getRuns( int startRecord, int recordsCount, String whereClause, String sortColumn,
                              boolean ascending, int utcTimeOffset ) throws DatabaseAccessException {

        List<Run> runs = new ArrayList<Run>();

        Connection connection = getConnection();

        String sqlLog = new SqlRequestFormatter().add("start record", startRecord)
                                                 .add("records", recordsCount)
                                                 .add("where", whereClause)
                                                 .add("sort by", sortColumn)
                                                 .add("asc", ascending)
                                                 .format();

        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall("{ call sp_get_runs(?, ?, ?, ?, ?) }");
            callableStatement.setString(1, String.valueOf(startRecord));
            callableStatement.setString(2, String.valueOf(recordsCount));
            callableStatement.setString(3, whereClause);
            callableStatement.setString(4, sortColumn);
            callableStatement.setString(5, (ascending
                                                      ? "ASC"
                                                      : "DESC"));

            int numberRecords = 0;
            rs = callableStatement.executeQuery();
            while (rs.next()) {
                Run run = new Run();
                run.runId = rs.getString("runId");
                run.productName = rs.getString("productName");
                run.versionName = rs.getString("versionName");
                run.buildName = rs.getString("buildName");
                run.runName = rs.getString("runName");
                run.os = rs.getString("OS");

                run.hostName = "";
                try {
                    @BackwardCompatibility
                    int dbInternalVersion = getDatabaseInternalVersion(); // run.hostName introduced in 3.10.0 (internalVersion = 1)

                    if (dbInternalVersion >= 1) {
                        run.hostName = rs.getString("hostName");
                    }

                } catch (NumberFormatException nfe) {
                    run.hostName = "";
                    log.getLog4jLogger().warn("Error parsing dbInternalVersion. ", nfe);
                }

                if (rs.getTimestamp("dateStart") != null) {
                    run.setStartTimestamp(rs.getTimestamp("dateStart").getTime());
                }
                if (rs.getTimestamp("dateEnd") != null) {
                    run.setEndTimestamp(rs.getTimestamp("dateEnd").getTime());
                }
                run.setTimeOffset(utcTimeOffset);

                run.scenariosTotal = rs.getInt("scenariosTotal");
                run.scenariosFailed = rs.getInt("scenariosFailed");
                run.scenariosSkipped = rs.getInt("scenariosSkipped");

                run.testcasesTotal = rs.getInt("testcasesTotal");
                run.testcasesFailed = rs.getInt("testcasesFailed");
                run.testcasesPassedPercent = String.valueOf(rs.getInt("testcasesPassedPercent")) + "%";
                run.testcaseIsRunning = rs.getBoolean("testcaseIsRunning");

                run.total = run.scenariosTotal + "/" + run.testcasesTotal;
                run.failed = run.scenariosFailed + "/" + run.testcasesFailed;

                run.userNote = rs.getString("userNote");
                if (run.userNote == null) {
                    run.userNote = "";
                }
                runs.add(run);

                numberRecords++;
            }

            logQuerySuccess(sqlLog, "runs", numberRecords);
        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, callableStatement);
        }

        return runs;
    }

    public int getRunsCount( String whereClause ) throws DatabaseAccessException {

        Connection connection = getConnection();

        String sqlLog = new SqlRequestFormatter().add("where", whereClause).format();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall("{ call sp_get_runs_count(?) }");
            callableStatement.setString(1, whereClause);

            rs = callableStatement.executeQuery();
            int runsCount = 0;
            while (rs.next()) {
                runsCount = rs.getInt("runsCount");
                logQuerySuccess(sqlLog, "runs", runsCount);
                break;
            }

            return runsCount;
        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, callableStatement);
        }
    }

    @BackwardCompatibility
    public List<Suite> getSuites( int startRecord, int recordsCount, String whereClause, String sortColumn,
                                  boolean ascending,
                                  int utcTimeOffset ) throws DatabaseAccessException {

        List<Suite> suites = new ArrayList<Suite>();

        Connection connection = getConnection();

        String sqlLog = new SqlRequestFormatter().add("start record", startRecord)
                                                 .add("records", recordsCount)
                                                 .add("where", whereClause)
                                                 .add("sort by", sortColumn)
                                                 .add("asc", ascending)
                                                 .format();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall("{ call sp_get_suites(?, ?, ?, ?, ?) }");
            callableStatement.setString(1, String.valueOf(startRecord));
            callableStatement.setString(2, String.valueOf(recordsCount));
            callableStatement.setString(3, whereClause);
            callableStatement.setString(4, sortColumn);
            callableStatement.setString(5, (ascending
                                                      ? "ASC"
                                                      : "DESC"));
            int numberRecords = 0;
            rs = callableStatement.executeQuery();
            while (rs.next()) {
                Suite suite = new Suite();
                suite.suiteId = rs.getString("suiteId");
                try {
                    @BackwardCompatibility
                    // suite.runId introduced 3.11.0 (internalVersion=3)
                    int dbInternalVersion = getDatabaseInternalVersion();

                    if (dbInternalVersion >= 3) {
                        suite.runId = rs.getString("runId");
                    }

                } catch (NumberFormatException nfe) {
                    suite.runId = "";
                    log.getLog4jLogger().warn("Error parsing dbInternalVersion. ", nfe);
                }
                suite.name = rs.getString("name");

                if (rs.getTimestamp("dateStart") != null) {
                    suite.setStartTimestamp(rs.getTimestamp("dateStart").getTime());
                }
                if (rs.getTimestamp("dateEnd") != null) {
                    suite.setEndTimestamp(rs.getTimestamp("dateEnd").getTime());
                }
                suite.setTimeOffset(utcTimeOffset);

                suite.scenariosTotal = rs.getInt("scenariosTotal");
                suite.scenariosFailed = rs.getInt("scenariosFailed");
                suite.scenariosSkipped = rs.getInt("scenariosSkipped");

                suite.testcasesTotal = rs.getInt("testcasesTotal");
                suite.testcasesFailed = rs.getInt("testcasesFailed");
                suite.testcasesPassedPercent = String.valueOf(rs.getInt("testcasesPassedPercent")) + "%";
                suite.testcaseIsRunning = rs.getBoolean("testcaseIsRunning");

                suite.total = suite.scenariosTotal + "/" + suite.testcasesTotal;
                suite.failed = suite.scenariosFailed + "/" + suite.testcasesFailed;

                suite.userNote = rs.getString("userNote");

                suite.packageName = "";
                try {
                    @BackwardCompatibility
                    // suite.packageName introduced 3.5.0 and internalVersion=1 (in 3.10.0)
                    int dbInternalVersion = getDatabaseInternalVersion();

                    if (dbInternalVersion >= 1) {
                        suite.packageName = rs.getString("package");
                    }

                } catch (NumberFormatException nfe) {
                    suite.packageName = "";
                    log.getLog4jLogger().warn("Error parsing dbInternalVersion. ", nfe);
                }

                suites.add(suite);
                numberRecords++;
            }

            logQuerySuccess(sqlLog, "suites", numberRecords);
        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, callableStatement);
        }
        return suites;
    }

    public int getSuitesCount( String whereClause ) throws DatabaseAccessException {

        Connection connection = getConnection();

        String sqlLog = new SqlRequestFormatter().add("where", whereClause).format();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall("{ call sp_get_suites_count(?) }");
            callableStatement.setString(1, whereClause);

            rs = callableStatement.executeQuery();
            int suitesCount = 0;
            while (rs.next()) {
                suitesCount = rs.getInt("suitesCount");
                logQuerySuccess(sqlLog, "suites", suitesCount);
                break;
            }

            return suitesCount;
        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, callableStatement);
        }
    }

    public List<Scenario> getScenarios( int startRecord, int recordsCount, String whereClause,
                                        String sortColumn, boolean ascending,
                                        int utcTimeOffset ) throws DatabaseAccessException {

        List<Scenario> scenarios = new ArrayList<Scenario>();

        String sqlLog = new SqlRequestFormatter().add("start record", startRecord)
                                                 .add("records", recordsCount)
                                                 .add("where", whereClause)
                                                 .add("sort by", sortColumn)
                                                 .add("asc", ascending)
                                                 .format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall("{ call sp_get_scenarios(?, ?, ?, ?, ?) }");
            callableStatement.setString(1, String.valueOf(startRecord));
            callableStatement.setString(2, String.valueOf(recordsCount));
            callableStatement.setString(3, whereClause);
            callableStatement.setString(4, sortColumn);
            callableStatement.setString(5, (ascending
                                                      ? "ASC"
                                                      : "DESC"));

            int numberRecords = 0;
            rs = callableStatement.executeQuery();
            while (rs.next()) {
                Scenario scenario = new Scenario();
                scenario.scenarioId = rs.getString("scenarioId");
                scenario.suiteId = rs.getString("suiteId");
                scenario.name = rs.getString("name");
                scenario.description = rs.getString("description");

                scenario.testcasesTotal = rs.getInt("testcasesTotal");
                scenario.testcasesFailed = rs.getInt("testcasesFailed");
                scenario.testcasesPassedPercent = String.valueOf(rs.getInt("testcasesPassedPercent"))
                                                  + "%";
                scenario.testcaseIsRunning = rs.getBoolean("testcaseIsRunning");

                if (rs.getTimestamp("dateStart") != null) {
                    scenario.setStartTimestamp(rs.getTimestamp("dateStart").getTime());
                }
                if (rs.getTimestamp("dateEnd") != null) {
                    scenario.setEndTimestamp(rs.getTimestamp("dateEnd").getTime());
                }

                scenario.setTimeOffset(utcTimeOffset);

                scenario.result = rs.getInt("result");
                /*
                 *   -- 0 FAILED
                 *   -- 1 PASSED
                 *   -- 2 SKIPPED
                 *   -- 4 RUNNING
                 */
                switch (scenario.result) {
                    case 0:
                        scenario.state = "FAILED";
                        break;
                    case 1:
                        scenario.state = "PASSED";
                        break;
                    case 2:
                        scenario.state = "SKIPPED";
                        break;
                    case 4:
                        scenario.state = "RUNNING";
                        break;
                    default:
                        //TODO: add warning
                        scenario.state = "unknown";
                }

                scenario.userNote = rs.getString("userNote");
                scenarios.add(scenario);

                numberRecords++;
            }

            logQuerySuccess(sqlLog, "scenarios", numberRecords);
        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, callableStatement);
        }

        return scenarios;
    }

    public int getScenariosCount( String whereClause ) throws DatabaseAccessException {

        String sqlLog = new SqlRequestFormatter().add("where", whereClause).format();

        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall("{ call sp_get_scenarios_count(?) }");
            callableStatement.setString(1, whereClause);

            rs = callableStatement.executeQuery();
            int scenariosCount = 0;
            while (rs.next()) {
                scenariosCount = rs.getInt("scenariosCount");
                logQuerySuccess(sqlLog, "scenarios", scenariosCount);
                break;
            }

            return scenariosCount;
        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, callableStatement);
        }
    }

    public List<Testcase> getTestcases( int startRecord, int recordsCount, String whereClause,
                                        String sortColumn, boolean ascending,
                                        int utcTimeOffset ) throws DatabaseAccessException {

        List<Testcase> testcases = new ArrayList<Testcase>();

        String sqlLog = new SqlRequestFormatter().add("start record", startRecord)
                                                 .add("records", recordsCount)
                                                 .add("where", whereClause)
                                                 .add("sort by", sortColumn)
                                                 .add("asc", ascending)
                                                 .format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall("{ call sp_get_testcases(?, ?, ?, ?, ?) }");
            callableStatement.setString(1, String.valueOf(startRecord));
            callableStatement.setString(2, String.valueOf(recordsCount));
            callableStatement.setString(3, whereClause);
            callableStatement.setString(4, sortColumn);
            callableStatement.setString(5, (ascending
                                                      ? "ASC"
                                                      : "DESC"));

            int numberRecords = 0;
            rs = callableStatement.executeQuery();
            while (rs.next()) {
                Testcase testcase = new Testcase();
                testcase.testcaseId = rs.getString("testcaseId");
                testcase.scenarioId = rs.getString("scenarioId");
                testcase.suiteId = rs.getString("suiteId");

                testcase.name = rs.getString("name");

                if (rs.getTimestamp("dateStart") != null) {
                    testcase.setStartTimestamp(rs.getTimestamp("dateStart").getTime());
                }
                if (rs.getTimestamp("dateEnd") != null) {
                    testcase.setEndTimestamp(rs.getTimestamp("dateEnd").getTime());
                }
                testcase.setTimeOffset(utcTimeOffset);

                testcase.result = rs.getInt("result");
                /*
                 *   -- 0 FAILED
                 *   -- 1 PASSED
                 *   -- 2 SKIPPED
                 *   -- 4 RUNNING
                 */
                switch (testcase.result) {
                    case 0:
                        testcase.state = "FAILED";
                        break;
                    case 1:
                        testcase.state = "PASSED";
                        break;
                    case 2:
                        testcase.state = "SKIPPED";
                        break;
                    case 4:
                        testcase.state = "RUNNING";
                        break;
                    default:
                        //TODO: add warning
                        testcase.state = "unknown";
                }

                testcase.userNote = rs.getString("userNote");
                testcases.add(testcase);

                numberRecords++;
            }

            logQuerySuccess(sqlLog, "test cases", numberRecords);
        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, callableStatement);
        }

        return testcases;
    }

    public int getTestcasesCount( String whereClause ) throws DatabaseAccessException {

        String sqlLog = new SqlRequestFormatter().add("where", whereClause).format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall("{ call sp_get_testcases_count(?) }");
            callableStatement.setString(1, whereClause);

            rs = callableStatement.executeQuery();
            int testcasesCount = 0;
            while (rs.next()) {
                testcasesCount = rs.getInt("testcasesCount");
                logQuerySuccess(sqlLog, "test cases", testcasesCount);
                break;
            }

            return testcasesCount;
        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, callableStatement);
        }
    }

    public List<Machine> getMachines( String whereClause ) throws DatabaseAccessException {

        String sqlLog = new SqlRequestFormatter().add("where", whereClause).format();

        List<Machine> machines = new ArrayList<Machine>();

        Connection connection = getConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.prepareStatement("SELECT * FROM tMachines " + whereClause + " ORDER BY machineName");
            rs = statement.executeQuery();
            while (rs.next()) {
                Machine machine = new Machine();
                machine.machineId = rs.getInt("machineId");
                machine.name = rs.getString("machineName");
                machine.alias = rs.getString("machineAlias");
                machines.add(machine);
            }
        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, statement);
        }

        return machines;

    }

    public List<Machine> getMachines() throws DatabaseAccessException {

        return getMachines("WHERE 1=1");

    }

    public List<Message> getMessages( int startRecord, int recordsCount, String whereClause,
                                      String sortColumn, boolean ascending,
                                      int utcTimeOffset ) throws DatabaseAccessException {

        List<Message> messages = new ArrayList<Message>();

        String sqlLog = new SqlRequestFormatter().add("start record", startRecord)
                                                 .add("records", recordsCount)
                                                 .add("where", whereClause)
                                                 .add("sort by", sortColumn)
                                                 .add("asc", ascending)
                                                 .format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall("{ call sp_get_messages(?, ?, ?, ?, ?) }");
            callableStatement.setString(1, String.valueOf(startRecord));
            callableStatement.setString(2, String.valueOf(recordsCount));
            callableStatement.setString(3, whereClause);
            callableStatement.setString(4, sortColumn);
            callableStatement.setString(5, (ascending
                                                      ? "ASC"
                                                      : "DESC"));

            int numberRecords = 0;
            rs = callableStatement.executeQuery();
            Map<Integer, Message> splitMessages = new HashMap<Integer, Message>(); // <parentMessageId, Message>
            while (rs.next()) {
                Message message = new Message();
                message.messageId = rs.getInt("messageId");
                message.messageContent = rs.getString("message");
                message.messageType = rs.getString("typeName");

                if (rs.getTimestamp("timestamp") != null) {
                    message.setStartTimestamp(rs.getTimestamp("timestamp").getTime());
                }

                message.setTimeOffset(utcTimeOffset);

                message.machineName = rs.getString("machineName");
                message.threadName = rs.getString("threadName");
                message.parentMessageId = rs.getInt("parentMessageId");

                if (message.parentMessageId != 0) {
                    // split message
                    if (splitMessages.containsKey(message.parentMessageId)) {
                        // append to the message - result set is ordered by message ID
                        Message splitMessage = splitMessages.get(message.parentMessageId);
                        if (splitMessage.messageId < message.messageId) {
                            // append at the end
                            splitMessage.messageContent = splitMessage.messageContent
                                                          + message.messageContent;
                        } else {
                            // append at the beginning
                            splitMessage.messageContent = message.messageContent
                                                          + splitMessage.messageContent;
                        }
                    } else {
                        // first part of the split message
                        splitMessages.put(message.parentMessageId, message);
                        messages.add(message);
                    }
                } else {
                    // single message
                    messages.add(message);
                }
                numberRecords++;
            }

            logQuerySuccess(sqlLog, "messages", numberRecords);
        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, callableStatement);
        }
        return messages;
    }

    public List<Message> getRunMessages( int startRecord, int recordsCount, String whereClause,
                                         String sortColumn,
                                         boolean ascending,
                                         int utcTimeOffset ) throws DatabaseAccessException {

        List<Message> runMessages = new ArrayList<Message>();

        String sqlLog = new SqlRequestFormatter().add("start record", startRecord)
                                                 .add("records", recordsCount)
                                                 .add("where", whereClause)
                                                 .add("sort by", sortColumn)
                                                 .add("asc", ascending)
                                                 .format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall("{ call sp_get_run_messages(?, ?, ?, ?, ?) }");
            callableStatement.setString(1, String.valueOf(startRecord));
            callableStatement.setString(2, String.valueOf(recordsCount));
            callableStatement.setString(3, whereClause);
            callableStatement.setString(4, sortColumn);
            callableStatement.setString(5, (ascending
                                                      ? "ASC"
                                                      : "DESC"));

            int numberRecords = 0;
            rs = callableStatement.executeQuery();
            while (rs.next()) {
                Message runMessage = new Message();
                runMessage.messageId = rs.getInt("runMessageId");
                runMessage.messageContent = rs.getString("message");
                runMessage.messageType = rs.getString("typeName");

                if (rs.getTimestamp("timestamp") != null) {
                    runMessage.setStartTimestamp(rs.getTimestamp("timestamp").getTime());
                }
                runMessage.setTimeOffset(utcTimeOffset);

                runMessage.machineName = rs.getString("machineName");
                runMessage.threadName = rs.getString("threadName");
                runMessages.add(runMessage);

                numberRecords++;
            }

            logQuerySuccess(sqlLog, "run messages", numberRecords);
        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, callableStatement);
        }

        return runMessages;
    }

    public List<Message> getSuiteMessages( int startRecord, int recordsCount, String whereClause,
                                           String sortColumn,
                                           boolean ascending,
                                           int utcTimeOffset ) throws DatabaseAccessException {

        List<Message> suiteMessages = new ArrayList<Message>();

        String sqlLog = new SqlRequestFormatter().add("start record", startRecord)
                                                 .add("records", recordsCount)
                                                 .add("where", whereClause)
                                                 .add("sort by", sortColumn)
                                                 .add("asc", ascending)
                                                 .format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall("{ call sp_get_suite_messages(?, ?, ?, ?, ?) }");
            callableStatement.setString(1, String.valueOf(startRecord));
            callableStatement.setString(2, String.valueOf(recordsCount));
            callableStatement.setString(3, whereClause);
            callableStatement.setString(4, sortColumn);
            callableStatement.setString(5, (ascending
                                                      ? "ASC"
                                                      : "DESC"));

            int numberRecords = 0;
            rs = callableStatement.executeQuery();
            while (rs.next()) {
                Message suiteMessage = new Message();
                suiteMessage.messageId = rs.getInt("suiteMessageId");
                suiteMessage.messageContent = rs.getString("message");
                suiteMessage.messageType = rs.getString("typeName");

                if (rs.getTimestamp("timestamp") != null) {
                    suiteMessage.setStartTimestamp(rs.getTimestamp("timestamp").getTime());
                }

                suiteMessage.setTimeOffset(utcTimeOffset);

                suiteMessage.machineName = rs.getString("machineName");
                suiteMessage.threadName = rs.getString("threadName");
                suiteMessages.add(suiteMessage);

                numberRecords++;
            }

            logQuerySuccess(sqlLog, "suite messages", numberRecords);
        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, callableStatement);
        }

        return suiteMessages;
    }

    public int getMessagesCount( String whereClause ) throws DatabaseAccessException {

        String sqlLog = new SqlRequestFormatter().add("where", whereClause).format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall("{ call sp_get_messages_count(?) }");
            callableStatement.setString(1, whereClause);

            rs = callableStatement.executeQuery();
            int messagesCount = 0;
            if (rs.next()) {
                messagesCount = rs.getInt("messagesCount");
            }
            logQuerySuccess(sqlLog, "messages", messagesCount);

            return messagesCount;
        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, callableStatement);
        }
    }

    public int getRunMessagesCount( String whereClause ) throws DatabaseAccessException {

        String sqlLog = new SqlRequestFormatter().add("where", whereClause).format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall("{ call sp_get_run_messages_count(?) }");
            callableStatement.setString(1, whereClause);

            rs = callableStatement.executeQuery();
            int messagesCount = 0;
            if (rs.next()) {
                messagesCount = rs.getInt("messagesCount");
            }
            logQuerySuccess(sqlLog, "run messages count", messagesCount);

            return messagesCount;
        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, callableStatement);
        }
    }

    public int getSuiteMessagesCount( String whereClause ) throws DatabaseAccessException {

        String sqlLog = new SqlRequestFormatter().add("where", whereClause).format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall("{ call sp_get_suite_messages_count(?) }");
            callableStatement.setString(1, whereClause);

            rs = callableStatement.executeQuery();
            int messagesCount = 0;
            if (rs.next()) {
                messagesCount = rs.getInt("messagesCount");
            }
            logQuerySuccess(sqlLog, "suite messages count", messagesCount);

            return messagesCount;
        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, callableStatement);
        }
    }

    public List<StatisticDescription> getSystemStatisticDescriptions(
                                                                      float timeOffset,
                                                                      String whereClause,
                                                                      Map<String, String> testcaseAliases,
                                                                      int utcTimeOffset,
                                                                      boolean dayLightSavingOn ) throws DatabaseAccessException {

        List<StatisticDescription> statisticDescriptions = new ArrayList<StatisticDescription>();

        String sqlLog = new SqlRequestFormatter().add("fdate", formatDateFromEpoch(timeOffset))
                                                 .add("whereClause", whereClause)
                                                 .format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall("{ call sp_get_system_statistic_descriptions(?, ?) }");
            callableStatement.setString(1, formatDateFromEpoch(timeOffset));
            callableStatement.setString(2, whereClause);

            rs = callableStatement.executeQuery();
            int numberRecords = 0;
            while (rs.next()) {
                StatisticDescription statisticDescription = new StatisticDescription();

                statisticDescription.testcaseId = rs.getInt("testcaseId");

                // if user has provided testcase alias - use it instead the original testcase name
                if (testcaseAliases != null) {
                    statisticDescription.testcaseName = testcaseAliases.get(String.valueOf(statisticDescription.testcaseId));
                }
                if (statisticDescription.testcaseName == null) {
                    statisticDescription.testcaseName = rs.getString("testcaseName");
                }

                long startTimestamp = rs.getInt("testcaseStarttime");
                if (dayLightSavingOn) {
                    startTimestamp += 3600; // add 1h to time stamp
                }
                statisticDescription.setStartTimestamp(startTimestamp);
                statisticDescription.setTimeOffset(utcTimeOffset);

                statisticDescription.machineId = rs.getInt("machineId");
                statisticDescription.machineName = rs.getString("machineName");

                statisticDescription.statisticTypeId = rs.getInt("statsTypeId");
                statisticDescription.statisticName = rs.getString("name");

                statisticDescription.unit = rs.getString("units");
                statisticDescription.params = rs.getString("params");
                statisticDescription.parent = rs.getString("parentName");
                statisticDescription.internalName = rs.getString("internalName");

                statisticDescription.numberMeasurements = rs.getInt("statsNumberMeasurements");
                statisticDescription.minValue = rs.getFloat("statsMinValue");
                statisticDescription.maxValue = rs.getFloat("statsMaxValue");
                statisticDescription.avgValue = rs.getFloat("statsAvgValue");

                statisticDescriptions.add(statisticDescription);

                numberRecords++;
            }

            logQuerySuccess(sqlLog, "system statistic descriptions", numberRecords);
        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, callableStatement);
        }

        return statisticDescriptions;
    }

    public List<StatisticDescription> getCheckpointStatisticDescriptions( float timeOffset,
                                                                          String whereClause,
                                                                          Set<String> expectedSingleActionUIDs,
                                                                          int utcTimeOffset,
                                                                          boolean dayLightSavingOn ) throws DatabaseAccessException {

        List<StatisticDescription> statisticDescriptions = new ArrayList<StatisticDescription>();

        String sqlLog = new SqlRequestFormatter().add("fdate", formatDateFromEpoch(timeOffset))
                                                 .add("where clause", whereClause)
                                                 .format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall("{ call sp_get_checkpoint_statistic_descriptions(?, ?) }");
            callableStatement.setString(1, formatDateFromEpoch(timeOffset));
            callableStatement.setString(2, whereClause);

            rs = callableStatement.executeQuery();
            int numberRecords = 0;
            while (rs.next()) {
                StatisticDescription statisticDescription = new StatisticDescription();

                statisticDescription.testcaseId = rs.getInt("testcaseId");

                if (statisticDescription.testcaseName == null) {
                    statisticDescription.testcaseName = rs.getString("testcaseName");
                }

                long startTimestamp = rs.getInt("testcaseStarttime");
                if (dayLightSavingOn) {
                    startTimestamp += 3600; // add 1h to time stamp
                }
                statisticDescription.setStartTimestamp(startTimestamp);
                statisticDescription.setTimeOffset(utcTimeOffset);

                statisticDescription.machineId = 0; // Checkpoints will be collected and displayed for testcase, for all machines/agents(loaders)
                statisticDescription.machineName = MACHINE_NAME_FOR_ATS_AGENTS;

                statisticDescription.queueName = rs.getString("queueName");

                statisticDescription.numberMeasurements = rs.getInt("statsNumberMeasurements");

                statisticDescription.minValue = rs.getInt("statsMinValue");
                statisticDescription.avgValue = rs.getInt("statsAvgValue");
                statisticDescription.maxValue = rs.getInt("statsMaxValue");

                statisticDescription.statisticName = rs.getString("name");
                statisticDescription.unit = "ms"; // "statsUnit" field is null for checkpoint statistics, because the action response times are always measured in "ms"

                String actionUid = statisticDescription.testcaseId + "->" + statisticDescription.machineId
                                   + "->" + statisticDescription.queueName + "->"
                                   + statisticDescription.statisticName;
                // add to single statistics
                if (expectedSingleActionUIDs.isEmpty() || expectedSingleActionUIDs.contains(actionUid)) {
                    statisticDescriptions.add(statisticDescription);
                    numberRecords++;
                }
            }

            logQuerySuccess(sqlLog, "system statistic descriptions", numberRecords);
        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, callableStatement);
        }

        return statisticDescriptions;
    }

    public Map<String, Integer>
            getNumberOfCheckpointsPerQueue( String testcaseIds ) throws DatabaseAccessException {

        Map<String, Integer> allStatistics = new HashMap<String, Integer>();

        String sqlLog = new SqlRequestFormatter().add("testcase ids", testcaseIds).format();

        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall("{ call sp_get_number_of_checkpoints_per_queue(?) }");
            callableStatement.setString(1, testcaseIds);

            rs = callableStatement.executeQuery();
            int numberRecords = 0;
            while (rs.next()) {
                String name = rs.getString("name");
                int queueNumbers = rs.getInt("numberOfQueue");
                allStatistics.put(name, queueNumbers);
            }

            logQuerySuccess(sqlLog, "system statistics", numberRecords);
        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, callableStatement);
        }

        return allStatistics;
    }

    public List<Statistic> getSystemStatistics(
                                                float timeOffset,
                                                String testcaseIds,
                                                String machineIds,
                                                String statsTypeIds,
                                                String whereClause,
                                                int utcTimeOffset,
                                                boolean dayLightSavingOn ) throws DatabaseAccessException {

        List<Statistic> allStatistics = new ArrayList<Statistic>();

        String sqlLog = new SqlRequestFormatter().add("fdate", formatDateFromEpoch(timeOffset))
                                                 .add("testcase ids", testcaseIds)
                                                 .add("machine ids", machineIds)
                                                 .add("stats type ids", statsTypeIds)
                                                 .add("where", whereClause)
                                                 .format();

        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall("{ call sp_get_system_statistics(?, ?, ?, ?, ?) }");

            callableStatement.setString(1, formatDateFromEpoch(timeOffset));
            callableStatement.setString(2, testcaseIds);
            callableStatement.setString(3, machineIds);
            callableStatement.setString(4, statsTypeIds);
            callableStatement.setString(5, whereClause);

            rs = callableStatement.executeQuery();
            int numberRecords = 0;
            while (rs.next()) {
                Statistic statistic = new Statistic();
                statistic.statisticTypeId = rs.getInt("statsTypeId");
                statistic.name = rs.getString("statsName");
                statistic.parentName = rs.getString("statsParent");
                statistic.unit = rs.getString("statsUnit");
                statistic.value = rs.getFloat("value");
                statistic.setDate(rs.getString("statsAxis"));

                long startTimestamp = rs.getInt("statsAxisTimestamp");
                if (dayLightSavingOn) {
                    startTimestamp += 3600; // add 1h to time stamp
                }
                statistic.setStartTimestamp(startTimestamp);
                statistic.setTimeOffset(utcTimeOffset);

                statistic.machineId = rs.getInt("machineId");
                statistic.testcaseId = rs.getInt("testcaseId");

                numberRecords++;
                // add the combined statistics to the others
                allStatistics.add(statistic);
            }

            logQuerySuccess(sqlLog, "system statistics", numberRecords);
        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, callableStatement);
        }

        return allStatistics;

    }

    /**
     * Currently used by {@link AtsDbReader#getStatisticsDescriptionForDateRange(int, int, int, long, long)}
     * */
    public List<Statistic> getSystemStatistics( String testcaseIds,
                                                String machineIds,
                                                String statsTypeIds,
                                                String whereClause ) throws DatabaseAccessException {

        List<Statistic> allStatistics = new ArrayList<Statistic>();

        String sqlLog = new SqlRequestFormatter().add("testcase ids", testcaseIds)
                                                 .add("machine ids", machineIds)
                                                 .add("stats type ids", statsTypeIds)
                                                 .add("where", whereClause)
                                                 .format();

        Connection connection = getConnection();
        PreparedStatement prepareStatement = null;
        ResultSet rs = null;

        try {
            int numberRecords = 0;
            StringBuilder query = new StringBuilder();
            query.append("SELECT")
                 .append(" st.name as statsName, st.units as statsUnit, st.params, st.parentName as statsParent, st.internalName,")
                 .append(" ss.systemStatsId, ss.testcaseId, ss.machineId as machineId, ss.statsTypeId as statsTypeId, ss.timestamp as timestamp, ss.value as value,")
                 .append(" t.testcaseId as testcaseId")
                 .append(" FROM tSystemStats ss")
                 .append(" INNER JOIN tStatsTypes st ON ss.statsTypeId = st.statsTypeId")
                 .append(" INNER JOIN tTestcases  t ON ss.testcaseId = t.testcaseId")
                 .append(" WHERE")
                 .append(" t.testcaseId IN (" + testcaseIds + ")")
                 .append(" AND ss.statsTypeId IN (" + statsTypeIds + ")")
                 .append(" AND ss.machineId IN (" + machineIds + ")")
                 .append(" AND " + whereClause);

            prepareStatement = connection.prepareStatement(query.toString());
            rs = prepareStatement.executeQuery();
            while (rs.next()) {
                Statistic statistic = new Statistic();
                statistic.statisticTypeId = rs.getInt("statsTypeId");
                statistic.name = rs.getString("statsName");
                statistic.parentName = rs.getString("statsParent");
                statistic.unit = rs.getString("statsUnit");
                statistic.value = rs.getFloat("value");
                statistic.machineId = rs.getInt("machineId");
                statistic.testcaseId = rs.getInt("testcaseId");

                long startTimestamp = rs.getTimestamp("timestamp").getTime();
                statistic.setStartTimestamp(startTimestamp);
                statistic.setEndTimestamp(startTimestamp);

                numberRecords++;
                // add the combined statistics to the others
                allStatistics.add(statistic);
            }

            logQuerySuccess(sqlLog, "system statistics", numberRecords);
        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, prepareStatement);
        }

        return allStatistics;
    }

    public List<Statistic> getSystemStatistics( float timeOffset,
                                                String testcaseIds,
                                                String machineIds,
                                                String statsTypeIds,
                                                int utcTimeOffset,
                                                boolean dayLightSavingOn )
                                                                           throws DatabaseAccessException {

        return getSystemStatistics(timeOffset, testcaseIds, machineIds, statsTypeIds, "1=1", utcTimeOffset,
                                   dayLightSavingOn);
    }

    public List<Statistic> getCheckpointStatistics( float timeOffset, String testcaseIds, String actionNames,
                                                    String actionParents,
                                                    Set<String> expectedSingleActionUIDs,
                                                    Set<String> expectedCombinedActionUIDs,
                                                    int utcTimeOffset,
                                                    boolean dayLightSavingOn ) throws DatabaseAccessException {

        List<Statistic> allStatistics = new ArrayList<Statistic>();

        String sqlLog = new SqlRequestFormatter().add("fdate", formatDateFromEpoch(timeOffset))
                                                 .add("testcase ids", testcaseIds)
                                                 .add("checkpoint names", actionNames)
                                                 .add("checkpoint parents", actionParents)
                                                 .format();

        Map<String, Integer> fakeStatisticIds = new HashMap<String, Integer>();

        /*
         * The DB does not contain combined statistics, so we must create them.
         *
         * All statistics with same name are combined in one statistic(no matter how many queues are).
         * In cases when there are more than one hits at same timestamp, we do not sum the values, but we
         * pass the same number of statistics for this timestamp - users see balloon marker on Test Explorer
         */
        Map<String, Statistic> combinedStatistics = new HashMap<String, Statistic>();
        Map<String, Integer> combinedStatisticHitsAtSameTimestamp = new HashMap<String, Integer>();

        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall("{ call sp_get_checkpoint_statistics(?, ?, ?, ?) }");

            callableStatement.setString(1, formatDateFromEpoch(timeOffset));
            callableStatement.setString(2, testcaseIds);
            callableStatement.setString(3, actionNames);
            callableStatement.setString(4, actionParents);

            int numberRecords = 0;
            rs = callableStatement.executeQuery();
            while (rs.next()) {

                // add new statistic
                Statistic statistic = new Statistic();
                statistic.name = rs.getString("statsName");
                statistic.parentName = rs.getString("queueName");
                statistic.unit = "ms";
                statistic.value = rs.getFloat("value");
                if (dayLightSavingOn) {
                    statistic.setStartTimestamp(rs.getLong("statsAxisTimestamp") + 3600); // add 1h to time stamp
                } else {
                    statistic.setStartTimestamp(rs.getLong("statsAxisTimestamp"));
                }

                statistic.setTimeOffset(utcTimeOffset);

                statistic.machineId = 0; // Checkpoints will be collected and displayed for testcase
                statistic.testcaseId = rs.getInt("testcaseId");

                statistic.statisticTypeId = getStatisticFakeId(START_FAKE_ID_VALUE_FOR_CHECKPOINTS,
                                                               fakeStatisticIds, statistic);

                // add to single statistics
                if (expectedSingleActionUIDs.contains(statistic.getUid())) {
                    allStatistics.add(statistic);
                }

                // add to combined statistics
                if (expectedCombinedActionUIDs.contains(statistic.getCombinedStatisticUid())) {

                    String statisticKey = statistic.getStartTimestamp() + "->" + statistic.name;
                    Integer timesHaveThisStatisticAtThisTimestamp = combinedStatisticHitsAtSameTimestamp.get(statisticKey);

                    Statistic combinedStatistic;
                    if (timesHaveThisStatisticAtThisTimestamp == null) {
                        // create a new combined statistic
                        combinedStatistic = new Statistic();
                        combinedStatistic.name = statistic.name;
                        combinedStatistic.parentName = Statistic.COMBINED_STATISTICS_CONTAINER;
                        combinedStatistic.unit = statistic.unit;

                        combinedStatistic.setStartTimestamp(statistic.getStartTimestamp());
                        combinedStatistic.setTimeOffset(statistic.getTimeOffset());

                        combinedStatistic.machineId = statistic.machineId;
                        combinedStatistic.testcaseId = statistic.testcaseId;

                        // this is the first such statistic at this timestamp
                        timesHaveThisStatisticAtThisTimestamp = 1;
                    } else {
                        // create another copy of this statistic
                        combinedStatistic = combinedStatistics.get(statisticKey + "->"
                                                                   + timesHaveThisStatisticAtThisTimestamp)
                                                              .newInstance();

                        // we already had such statistic at this timestamp
                        timesHaveThisStatisticAtThisTimestamp++;
                    }

                    combinedStatistic.value = statistic.value;
                    combinedStatistic.statisticTypeId = getStatisticFakeId(START_FAKE_ID_VALUE_FOR_CHECKPOINTS,
                                                                           fakeStatisticIds,
                                                                           combinedStatistic);

                    // remember how many times we got same statistic at same timestamp
                    combinedStatisticHitsAtSameTimestamp.put(statisticKey,
                                                             timesHaveThisStatisticAtThisTimestamp);
                    // Remember this statistic in the list
                    // The way we create the map key assures the proper time ordering
                    combinedStatistics.put(statisticKey + "->" + timesHaveThisStatisticAtThisTimestamp,
                                           combinedStatistic);
                }

                numberRecords++;
            }

            if (combinedStatistics.size() > 0) {
                // sort the combined statistics by their timestamps
                List<Statistic> sortedStatistics = new ArrayList<Statistic>(combinedStatistics.values());
                Collections.sort(sortedStatistics, new Comparator<Statistic>() {

                    @Override
                    public int compare( Statistic stat1, Statistic stat2 ) {

                        return (int) (stat1.getStartTimestamp() - stat2.getStartTimestamp());
                    }
                });

                // add the combined statistics to the others
                allStatistics.addAll(sortedStatistics);
            }

            logQuerySuccess(sqlLog, "action response statistics", numberRecords);
        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, callableStatement);
        }

        return allStatistics;
    }

    /**
     * Some statistics do not have a statistic type ID from the DB, but we need one
     * @param startValue
     * @param statisticFakeIds
     * @param statistic
     * @return
     */
    private Integer getStatisticFakeId( int startValue, Map<String, Integer> statisticFakeIds,
                                        Statistic statistic ) {

        final String statisticUID = statistic.parentName + "->" + statistic.name;

        Integer statisticId = statisticFakeIds.get(statisticUID);
        if (statisticId == null) {
            statisticId = startValue - statisticFakeIds.size();
            statisticFakeIds.put(statisticUID, statisticId);
        }

        return statisticId;
    }

    public List<LoadQueue> getLoadQueues( String whereClause, String sortColumn, boolean ascending,
                                          int utcTimeOffset ) throws DatabaseAccessException {

        List<LoadQueue> loadQueues = new ArrayList<LoadQueue>();

        String sqlLog = new SqlRequestFormatter().add("where", whereClause)
                                                 .add("sort by", sortColumn)
                                                 .add("asc", ascending)
                                                 .format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall("{ call sp_get_loadqueues(?, ?, ?) }");
            callableStatement.setString(1, "where " + whereClause);
            callableStatement.setString(2, sortColumn);
            callableStatement.setString(3, (ascending
                                                      ? "ASC"
                                                      : "DESC"));

            rs = callableStatement.executeQuery();
            int numberRecords = 0;
            while (rs.next()) {
                LoadQueue loadQueue = new LoadQueue();
                loadQueue.loadQueueId = rs.getInt("loadQueueId");
                loadQueue.name = rs.getString("name");
                loadQueue.sequence = rs.getInt("sequence");
                loadQueue.hostsList = rs.getString("hostsList");
                loadQueue.threadingPattern = rs.getString("threadingPattern");
                loadQueue.numberThreads = rs.getInt("numberThreads");
                if (loadQueue.threadingPattern != null) {
                    loadQueue.threadingPattern = loadQueue.threadingPattern.replace("<number_threads>",
                                                                                    String.valueOf(loadQueue.numberThreads));
                }

                if (rs.getTimestamp("dateStart") != null) {
                    loadQueue.setStartTimestamp(rs.getTimestamp("dateStart").getTime());
                }
                if (rs.getTimestamp("dateEnd") != null) {
                    loadQueue.setEndTimestamp(rs.getTimestamp("dateEnd").getTime());
                }
                loadQueue.setTimeOffset(utcTimeOffset);

                loadQueue.result = rs.getInt("result");
                /*
                 *   -- 0 FAILED
                 *   -- 1 PASSED
                 *   -- 2 SKIPPED
                 *   -- 4 RUNNING
                 */
                switch (loadQueue.result) {
                    case 0:
                        loadQueue.state = "FAILED";
                        break;
                    case 1:
                        loadQueue.state = "PASSED";
                        break;
                    case 2:
                        loadQueue.state = "SKIPPED";
                        break;
                    case 4:
                        loadQueue.state = "RUNNING";
                        break;
                    default:
                        //TODO: add warning
                        loadQueue.state = "unknown";
                }

                loadQueues.add(loadQueue);
                numberRecords++;
            }

            logQuerySuccess(sqlLog, "loadqueues", numberRecords);
        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, callableStatement);
        }

        return loadQueues;
    }

    public List<CheckpointSummary> getCheckpointsSummary( String whereClause, String sortColumn,
                                                          boolean ascending ) throws DatabaseAccessException {

        List<CheckpointSummary> checkpoints = new ArrayList<CheckpointSummary>();

        String sqlLog = new SqlRequestFormatter().add("where", whereClause)
                                                 .add("sort by", sortColumn)
                                                 .add("asc", ascending)
                                                 .format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall("{ call sp_get_checkpoints_summary(?, ?, ?) }");
            callableStatement.setString(1, "where " + whereClause);
            callableStatement.setString(2, sortColumn);
            callableStatement.setString(3, (ascending
                                                      ? "ASC"
                                                      : "DESC"));

            rs = callableStatement.executeQuery();
            int numberRecords = 0;
            while (rs.next()) {
                CheckpointSummary checkpointSummary = new CheckpointSummary();
                checkpointSummary.checkpointSummaryId = rs.getInt("checkpointSummaryId");
                checkpointSummary.name = rs.getString("name");

                checkpointSummary.numRunning = rs.getInt("numRunning");
                checkpointSummary.numPassed = rs.getInt("numPassed");
                checkpointSummary.numFailed = rs.getInt("numFailed");
                checkpointSummary.numTotal = checkpointSummary.numRunning + checkpointSummary.numPassed
                                             + checkpointSummary.numFailed;

                checkpointSummary.minResponseTime = rs.getInt("minResponseTime");
                if (checkpointSummary.minResponseTime == Integer.MAX_VALUE) {
                    checkpointSummary.minResponseTime = 0;
                }
                checkpointSummary.avgResponseTime = rs.getDouble("avgResponseTime");
                checkpointSummary.maxResponseTime = rs.getInt("maxResponseTime");

                checkpointSummary.minTransferRate = rs.getDouble("minTransferRate");
                if (checkpointSummary.minTransferRate == Integer.MAX_VALUE) {
                    checkpointSummary.minTransferRate = 0.0F;
                }
                checkpointSummary.avgTransferRate = rs.getDouble("avgTransferRate");
                checkpointSummary.maxTransferRate = rs.getDouble("maxTransferRate");
                checkpointSummary.transferRateUnit = rs.getString("transferRateUnit");

                checkpoints.add(checkpointSummary);
                numberRecords++;
            }

            logQuerySuccess(sqlLog, "checkpoints summary", numberRecords);
        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, callableStatement);
        }

        return checkpoints;
    }

    /**
     * Get checkpoints, using additional where clause<br>Note that the whereClause does not need to include WHERE keyword in the beginning*/
    public List<Checkpoint> getCheckpoints( String testcaseId,
                                            int loadQueueId,
                                            String checkpointName,
                                            int utcTimeOffset,
                                            boolean dayLightSavingOn ) throws DatabaseAccessException {

        return getCheckpoints(testcaseId, loadQueueId, checkpointName, "1=1", utcTimeOffset, dayLightSavingOn);
    }

    public List<Checkpoint> getCheckpoints( String testcaseId,
                                            int loadQueueId,
                                            String checkpointName,
                                            String whereClause,
                                            int utcTimeOffset,
                                            boolean dayLightSavingOn ) throws DatabaseAccessException {

        List<Checkpoint> checkpoints = new ArrayList<Checkpoint>();

        String sqlLog = new SqlRequestFormatter().add("testcase id", testcaseId)
                                                 .add("loadQueue id", loadQueueId)
                                                 .add("checkpoint name", checkpointName)
                                                 .add("where", whereClause)
                                                 .format();
        Connection connection = getConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {

            statement = connection.prepareStatement("SELECT ch.checkpointId, ch.responseTime, ch.transferRate, ch.transferRateUnit, ch.result,"
                                                    + " DATEDIFF(second, CONVERT( datetime, '1970-01-01 00:00:00', 20), ch.endTime) as endTime,"
                                                    + " ch.endtime AS copyEndTime"
                                                    + " FROM tCheckpoints ch"
                                                    + " INNER JOIN tCheckpointsSummary chs on (chs.checkpointSummaryId = ch.checkpointSummaryId)"
                                                    + " INNER JOIN tLoadQueues c on (c.loadQueueId = chs.loadQueueId)"
                                                    + " INNER JOIN tTestcases tt on (tt.testcaseId = c.testcaseId) "
                                                    + "WHERE tt.testcaseId = ? AND c.loadQueueId = ? AND ch.name = ? AND "
                                                    + whereClause);

            statement.setString(1, testcaseId);
            statement.setInt(2, loadQueueId);
            statement.setString(3, checkpointName);

            rs = statement.executeQuery();
            while (rs.next()) {

                Checkpoint checkpoint = new Checkpoint();
                checkpoint.checkpointId = rs.getLong("checkpointId");
                checkpoint.name = checkpointName;
                checkpoint.responseTime = rs.getInt("responseTime");
                checkpoint.transferRate = rs.getFloat("transferRate");
                checkpoint.transferRateUnit = rs.getString("transferRateUnit");
                checkpoint.result = rs.getInt("result");

                if (dayLightSavingOn) {
                    checkpoint.setEndTimestamp(rs.getLong("endTime") + 3600); // add 1h
                } else {
                    checkpoint.setEndTimestamp(rs.getLong("endTime"));
                }
                checkpoint.setTimeOffset(utcTimeOffset);
                checkpoint.copyEndTimestamp = rs.getTimestamp("copyEndTime").getTime();

                checkpoints.add(checkpoint);
            }

            logQuerySuccess(sqlLog, "checkpoints", checkpoints.size());
        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, statement);
        }

        return checkpoints;
    }

    protected Connection getConnection() throws DatabaseAccessException {

        Connection connection = super.getConnection();
        try {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        } catch (SQLException e) {
            // Not a big deal, we will not read the entities which are in the process
            // of being inserted, but the transaction is still not completed
        }
        return connection;
    }

    @Override
    public List<RunMetaInfo> getRunMetaInfo( int runId ) throws DatabaseAccessException {

        List<RunMetaInfo> runMetaInfoList = new ArrayList<>();
        Connection connection = getConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.prepareStatement("SELECT * FROM tRunMetainfo WHERE runId = " + runId);
            rs = statement.executeQuery();
            while (rs.next()) {
                RunMetaInfo runMetainfo = new RunMetaInfo();
                runMetainfo.metaInfoId = rs.getInt("metaInfoId");
                runMetainfo.runId = rs.getInt("runId");
                runMetainfo.name = rs.getString("name");
                runMetainfo.value = rs.getString("value");
                runMetaInfoList.add(runMetainfo);
            }
        } catch (Exception e) {
            throw new DatabaseAccessException("Error retrieving run metainfo for run with id '" + runId + "'", e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, statement);
        }

        return runMetaInfoList;
    }

    @Override
    public List<ScenarioMetaInfo> getScenarioMetaInfo( int scenarioId ) throws DatabaseAccessException {

        List<ScenarioMetaInfo> scenarioMetaInfoList = new ArrayList<>();
        Connection connection = getConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.prepareStatement("SELECT * FROM tScenarioMetainfo WHERE scenarioId = " + scenarioId);
            rs = statement.executeQuery();
            while (rs.next()) {
                ScenarioMetaInfo scenarioMetainfo = new ScenarioMetaInfo();
                scenarioMetainfo.metaInfoId = rs.getInt("metaInfoId");
                scenarioMetainfo.scenarioId = rs.getInt("scenarioId");
                scenarioMetainfo.name = rs.getString("name");
                scenarioMetainfo.value = rs.getString("value");
                scenarioMetaInfoList.add(scenarioMetainfo);
            }
        } catch (Exception e) {
            throw new DatabaseAccessException("Error retrieving scenario metainfo for scenario with id '" + scenarioId
                                              + "'", e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, statement);
        }

        return scenarioMetaInfoList;
    }

    @Override
    public List<TestcaseMetainfo> getTestcaseMetainfo( int testcaseId ) throws DatabaseAccessException {

        List<TestcaseMetainfo> list = new ArrayList<>();
        Connection connection = getConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.prepareStatement("SELECT * FROM tTestcaseMetainfo WHERE testcaseId = " + testcaseId);
            rs = statement.executeQuery();
            while (rs.next()) {
                TestcaseMetainfo testcaseMetainfo = new TestcaseMetainfo();
                testcaseMetainfo.metaInfoId = rs.getInt("metaInfoId");
                testcaseMetainfo.testcaseId = rs.getInt("testcaseId");
                testcaseMetainfo.name = rs.getString("name");
                testcaseMetainfo.value = rs.getString("value");
                list.add(testcaseMetainfo);
            }
        } catch (Exception e) {
            throw new DatabaseAccessException("Error retrieving testcase metainfo for testcase with id '" + testcaseId
                                              + "'", e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, statement);
        }

        return list;
    }

}
