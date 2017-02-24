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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
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
import com.axway.ats.log.autodb.entities.Checkpoint;
import com.axway.ats.log.autodb.entities.CheckpointSummary;
import com.axway.ats.log.autodb.entities.LoadQueue;
import com.axway.ats.log.autodb.entities.Machine;
import com.axway.ats.log.autodb.entities.Message;
import com.axway.ats.log.autodb.entities.Run;
import com.axway.ats.log.autodb.entities.Scenario;
import com.axway.ats.log.autodb.entities.Statistic;
import com.axway.ats.log.autodb.entities.StatisticDescription;
import com.axway.ats.log.autodb.entities.Suite;
import com.axway.ats.log.autodb.entities.Testcase;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.log.autodb.model.IDbReadAccess;
import com.axway.ats.log.autodb.model.StatisticAggregatedType;

public class DbReadAccess extends AbstractDbAccess implements IDbReadAccess {

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
    private static final int   START_FAKE_ID_VALUE_FOR_CHECKPOINTS                  = Integer.MAX_VALUE;
    private static final int   START_FAKE_ID_VALUE_FOR_AGGREGATED_CHECKPOINTS       = START_FAKE_ID_VALUE_FOR_CHECKPOINTS
                                                                                      / 2;
    private static final int   START_FAKE_ID_VALUE_FOR_AGGREGATED_SYSTEM_STATISTICS = START_FAKE_ID_VALUE_FOR_AGGREGATED_CHECKPOINTS
                                                                                      / 2;

    public static final String MACHINE_NAME_FOR_ATS_AGENTS                          = "ATS Agents";

    public DbReadAccess( DbConnection dbConnection ) {

        super( dbConnection );
    }

    @BackwardCompatibility
    public List<Run> getRuns( int startRecord, int recordsCount, String whereClause, String sortColumn,
                              boolean ascending ) throws DatabaseAccessException {

        List<Run> runs = new ArrayList<Run>();

        Connection connection = getConnection();

        String sqlLog = new SqlRequestFormatter().add( "start record", startRecord )
                                                 .add( "records", recordsCount )
                                                 .add( "where", whereClause )
                                                 .add( "sort by", sortColumn )
                                                 .add( "asc", ascending )
                                                 .format();

        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_get_runs(?, ?, ?, ?, ?) }" );
            callableStatement.setString( 1, String.valueOf( startRecord ) );
            callableStatement.setString( 2, String.valueOf( recordsCount ) );
            callableStatement.setString( 3, whereClause );
            callableStatement.setString( 4, sortColumn );
            callableStatement.setString( 5, ( ascending
                                                        ? "ASC"
                                                        : "DESC" ) );

            int numberRecords = 0;
            rs = callableStatement.executeQuery();
            while( rs.next() ) {
                Run run = new Run();
                run.runId = rs.getString( "runId" );
                run.productName = rs.getString( "productName" );
                run.versionName = rs.getString( "versionName" );
                run.buildName = rs.getString( "buildName" );
                run.runName = rs.getString( "runName" );
                run.os = rs.getString( "OS" );

                run.hostName = "";
                try {
                    @BackwardCompatibility
                    int dbInternalVersion = getDatabaseInternalVersion(); // run.hostName introduced in 3.10.0 (internalVersion = 1)

                    if( dbInternalVersion >= 1 ) {
                        run.hostName = rs.getString( "hostName" );
                    }

                } catch( NumberFormatException nfe ) {
                    run.hostName = "";
                    log.warn( "Error parsing dbInternalVersion. ", nfe );
                }

                Timestamp dateStartTimestamp = rs.getTimestamp( "dateStart" );
                run.dateStart = formatDateNoYear( dateStartTimestamp );
                run.dateStartLong = formatDate( dateStartTimestamp );

                Timestamp dateEndTimestamp = rs.getTimestamp( "dateEnd" );
                run.dateEnd = formatDateNoYear( dateEndTimestamp );
                run.dateEndLong = formatDate( dateEndTimestamp );

                int duration = rs.getInt( "duration" );
                if( duration < 0 ) {
                    // this may happen when the run is not ended and the time of the log server
                    // is behind with the time of the test executor host
                    duration = 0;
                }
                run.durationSeconds = duration;
                run.duration = formatTimeDiffereceFromSecondsToString( duration );

                run.scenariosTotal = rs.getInt( "scenariosTotal" );
                run.scenariosFailed = rs.getInt( "scenariosFailed" );
                run.scenariosSkipped = rs.getInt( "scenariosSkipped" );

                run.testcasesTotal = rs.getInt( "testcasesTotal" );
                run.testcasesFailed = rs.getInt( "testcasesFailed" );
                run.testcasesPassedPercent = String.valueOf( rs.getInt( "testcasesPassedPercent" ) ) + "%";
                run.testcaseIsRunning = rs.getBoolean( "testcaseIsRunning" );

                run.total = run.scenariosTotal + "/" + run.testcasesTotal;
                run.failed = run.scenariosFailed + "/" + run.testcasesFailed;

                run.userNote = rs.getString( "userNote" );
                if( run.userNote == null ) {
                    run.userNote = "";
                }
                runs.add( run );

                numberRecords++;
            }

            logQuerySuccess( sqlLog, "runs", numberRecords );
        } catch( Exception e ) {
            throw new DatabaseAccessException( "Error when " + sqlLog, e );
        } finally {
            DbUtils.closeResultSet( rs );
            DbUtils.close( connection, callableStatement );
        }

        return runs;
    }

    public int getRunsCount( String whereClause ) throws DatabaseAccessException {

        Connection connection = getConnection();

        String sqlLog = new SqlRequestFormatter().add( "where", whereClause ).format();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_get_runs_count(?) }" );
            callableStatement.setString( 1, whereClause );

            rs = callableStatement.executeQuery();
            int runsCount = 0;
            while( rs.next() ) {
                runsCount = rs.getInt( "runsCount" );
                logQuerySuccess( sqlLog, "runs", runsCount );
                break;
            }

            return runsCount;
        } catch( Exception e ) {
            throw new DatabaseAccessException( "Error when " + sqlLog, e );
        } finally {
            DbUtils.closeResultSet( rs );
            DbUtils.close( connection, callableStatement );
        }
    }

    @BackwardCompatibility
    public List<Suite> getSuites( int startRecord, int recordsCount, String whereClause, String sortColumn,
                                  boolean ascending,
                                  boolean dateFormatNoYear ) throws DatabaseAccessException {

        List<Suite> suites = new ArrayList<Suite>();

        Connection connection = getConnection();

        String sqlLog = new SqlRequestFormatter().add( "start record", startRecord )
                                                 .add( "records", recordsCount )
                                                 .add( "where", whereClause )
                                                 .add( "sort by", sortColumn )
                                                 .add( "asc", ascending )
                                                 .format();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_get_suites(?, ?, ?, ?, ?) }" );
            callableStatement.setString( 1, String.valueOf( startRecord ) );
            callableStatement.setString( 2, String.valueOf( recordsCount ) );
            callableStatement.setString( 3, whereClause );
            callableStatement.setString( 4, sortColumn );
            callableStatement.setString( 5, ( ascending
                                                        ? "ASC"
                                                        : "DESC" ) );
            int numberRecords = 0;
            rs = callableStatement.executeQuery();
            while( rs.next() ) {
                Suite suite = new Suite();
                suite.suiteId = rs.getString( "suiteId" );
                try {
                    @BackwardCompatibility
                    // suite.runId introduced 3.11.0 (internalVersion=3)
                    int dbInternalVersion = getDatabaseInternalVersion();

                    if( dbInternalVersion >= 3 ) {
                        suite.runId = rs.getString( "runId" );
                    }

                } catch( NumberFormatException nfe ) {
                    suite.runId = "";
                    log.warn( "Error parsing dbInternalVersion. ", nfe );
                }
                suite.name = rs.getString( "name" );
                if( dateFormatNoYear ) {
                    suite.dateStart = formatDateNoYear( rs.getTimestamp( "dateStart" ) );
                    suite.dateEnd = formatDateNoYear( rs.getTimestamp( "dateEnd" ) );
                } else {
                    suite.dateStart = formatDate( rs.getTimestamp( "dateStart" ) );
                    suite.dateEnd = formatDate( rs.getTimestamp( "dateEnd" ) );
                }

                int duration = rs.getInt( "duration" );
                if( duration < 0 ) {
                    // this may happen when the suite is not ended and the time of the log server
                    // is behind with the time of the test executor host
                    duration = 0;
                }
                suite.duration = formatTimeDiffereceFromSecondsToString( duration );

                suite.scenariosTotal = rs.getInt( "scenariosTotal" );
                suite.scenariosFailed = rs.getInt( "scenariosFailed" );
                suite.scenariosSkipped = rs.getInt( "scenariosSkipped" );

                suite.testcasesTotal = rs.getInt( "testcasesTotal" );
                suite.testcasesFailed = rs.getInt( "testcasesFailed" );
                suite.testcasesPassedPercent = String.valueOf( rs.getInt( "testcasesPassedPercent" ) ) + "%";
                suite.testcaseIsRunning = rs.getBoolean( "testcaseIsRunning" );

                suite.total = suite.scenariosTotal + "/" + suite.testcasesTotal;
                suite.failed = suite.scenariosFailed + "/" + suite.testcasesFailed;

                suite.userNote = rs.getString( "userNote" );

                suite.packageName = "";
                try {
                    @BackwardCompatibility
                    // suite.packageName introduced 3.5.0 and internalVersion=1 (in 3.10.0)
                    int dbInternalVersion = getDatabaseInternalVersion();

                    if( dbInternalVersion >= 1 ) {
                        suite.packageName = rs.getString( "package" );
                    }

                } catch( NumberFormatException nfe ) {
                    suite.packageName = "";
                    log.warn( "Error parsing dbInternalVersion. ", nfe );
                }

                suites.add( suite );
                numberRecords++;
            }

            logQuerySuccess( sqlLog, "suites", numberRecords );
        } catch( Exception e ) {
            throw new DatabaseAccessException( "Error when " + sqlLog, e );
        } finally {
            DbUtils.closeResultSet( rs );
            DbUtils.close( connection, callableStatement );
        }
        return suites;
    }

    public int getSuitesCount( String whereClause ) throws DatabaseAccessException {

        Connection connection = getConnection();

        String sqlLog = new SqlRequestFormatter().add( "where", whereClause ).format();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_get_suites_count(?) }" );
            callableStatement.setString( 1, whereClause );

            rs = callableStatement.executeQuery();
            int suitesCount = 0;
            while( rs.next() ) {
                suitesCount = rs.getInt( "suitesCount" );
                logQuerySuccess( sqlLog, "suites", suitesCount );
                break;
            }

            return suitesCount;
        } catch( Exception e ) {
            throw new DatabaseAccessException( "Error when " + sqlLog, e );
        } finally {
            DbUtils.closeResultSet( rs );
            DbUtils.close( connection, callableStatement );
        }
    }

    public List<Scenario> getScenarios( int startRecord, int recordsCount, String whereClause,
                                        String sortColumn, boolean ascending,
                                        boolean dateFormatNoYear ) throws DatabaseAccessException {

        List<Scenario> scenarios = new ArrayList<Scenario>();

        String sqlLog = new SqlRequestFormatter().add( "start record", startRecord )
                                                 .add( "records", recordsCount )
                                                 .add( "where", whereClause )
                                                 .add( "sort by", sortColumn )
                                                 .add( "asc", ascending )
                                                 .format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_get_scenarios(?, ?, ?, ?, ?) }" );
            callableStatement.setString( 1, String.valueOf( startRecord ) );
            callableStatement.setString( 2, String.valueOf( recordsCount ) );
            callableStatement.setString( 3, whereClause );
            callableStatement.setString( 4, sortColumn );
            callableStatement.setString( 5, ( ascending
                                                        ? "ASC"
                                                        : "DESC" ) );

            int numberRecords = 0;
            rs = callableStatement.executeQuery();
            while( rs.next() ) {
                Scenario scenario = new Scenario();
                scenario.scenarioId = rs.getString( "scenarioId" );
                scenario.suiteId = rs.getString( "suiteId" );
                scenario.name = rs.getString( "name" );
                scenario.description = rs.getString( "description" );

                scenario.testcasesTotal = rs.getInt( "testcasesTotal" );
                scenario.testcasesFailed = rs.getInt( "testcasesFailed" );
                scenario.testcasesPassedPercent = String.valueOf( rs.getInt( "testcasesPassedPercent" ) )
                                                  + "%";
                scenario.testcaseIsRunning = rs.getBoolean( "testcaseIsRunning" );

                if( dateFormatNoYear ) {
                    scenario.dateStart = formatDateNoYear( rs.getTimestamp( "dateStart" ) );
                    scenario.dateEnd = formatDateNoYear( rs.getTimestamp( "dateEnd" ) );
                } else {
                    scenario.dateStart = formatDate( rs.getTimestamp( "dateStart" ) );
                    scenario.dateEnd = formatDate( rs.getTimestamp( "dateEnd" ) );
                }

                int duration = rs.getInt( "duration" );
                if( duration < 0 ) {
                    // this may happen when the scenario is not ended and the time of the log server
                    // is behind with the time of the test executor host
                    duration = 0;
                }
                scenario.duration = formatTimeDiffereceFromSecondsToString( duration );

                scenario.result = rs.getInt( "result" );
                /*
                 *   -- 0 FAILED
                 *   -- 1 PASSED
                 *   -- 2 SKIPPED
                 *   -- 4 RUNNING
                 */
                switch( scenario.result ){
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

                scenario.userNote = rs.getString( "userNote" );
                scenarios.add( scenario );

                numberRecords++;
            }

            logQuerySuccess( sqlLog, "scenarios", numberRecords );
        } catch( Exception e ) {
            throw new DatabaseAccessException( "Error when " + sqlLog, e );
        } finally {
            DbUtils.closeResultSet( rs );
            DbUtils.close( connection, callableStatement );
        }

        return scenarios;
    }

    public int getScenariosCount( String whereClause ) throws DatabaseAccessException {

        String sqlLog = new SqlRequestFormatter().add( "where", whereClause ).format();

        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_get_scenarios_count(?) }" );
            callableStatement.setString( 1, whereClause );

            rs = callableStatement.executeQuery();
            int scenariosCount = 0;
            while( rs.next() ) {
                scenariosCount = rs.getInt( "scenariosCount" );
                logQuerySuccess( sqlLog, "scenarios", scenariosCount );
                break;
            }

            return scenariosCount;
        } catch( Exception e ) {
            throw new DatabaseAccessException( "Error when " + sqlLog, e );
        } finally {
            DbUtils.closeResultSet( rs );
            DbUtils.close( connection, callableStatement );
        }
    }

    public List<Testcase> getTestcases( int startRecord, int recordsCount, String whereClause,
                                        String sortColumn, boolean ascending,
                                        boolean dateFormatNoYear ) throws DatabaseAccessException {

        List<Testcase> testcases = new ArrayList<Testcase>();

        String sqlLog = new SqlRequestFormatter().add( "start record", startRecord )
                                                 .add( "records", recordsCount )
                                                 .add( "where", whereClause )
                                                 .add( "sort by", sortColumn )
                                                 .add( "asc", ascending )
                                                 .format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_get_testcases(?, ?, ?, ?, ?) }" );
            callableStatement.setString( 1, String.valueOf( startRecord ) );
            callableStatement.setString( 2, String.valueOf( recordsCount ) );
            callableStatement.setString( 3, whereClause );
            callableStatement.setString( 4, sortColumn );
            callableStatement.setString( 5, ( ascending
                                                        ? "ASC"
                                                        : "DESC" ) );

            int numberRecords = 0;
            rs = callableStatement.executeQuery();
            while( rs.next() ) {
                Testcase testcase = new Testcase();
                testcase.testcaseId = rs.getString( "testcaseId" );
                testcase.scenarioId = rs.getString( "scenarioId" );
                testcase.suiteId = rs.getString( "suiteId" );

                testcase.name = rs.getString( "name" );

                if( dateFormatNoYear ) {
                    testcase.dateStart = formatDateNoYear( rs.getTimestamp( "dateStart" ) );
                    testcase.dateEnd = formatDateNoYear( rs.getTimestamp( "dateEnd" ) );
                } else {
                    testcase.dateStart = formatDate( rs.getTimestamp( "dateStart" ) );
                    testcase.dateEnd = formatDate( rs.getTimestamp( "dateEnd" ) );
                }

                int duration = rs.getInt( "duration" );
                if( duration < 0 ) {
                    // this may happen when the test case is not ended and the time of the log server
                    // is behind with the time of the test executor host
                    duration = 0;
                }
                testcase.duration = formatTimeDiffereceFromSecondsToString( duration );

                testcase.result = rs.getInt( "result" );
                /*
                 *   -- 0 FAILED
                 *   -- 1 PASSED
                 *   -- 2 SKIPPED
                 *   -- 4 RUNNING
                 */
                switch( testcase.result ){
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

                testcase.userNote = rs.getString( "userNote" );
                testcases.add( testcase );

                numberRecords++;
            }

            logQuerySuccess( sqlLog, "test cases", numberRecords );
        } catch( Exception e ) {
            throw new DatabaseAccessException( "Error when " + sqlLog, e );
        } finally {
            DbUtils.closeResultSet( rs );
            DbUtils.close( connection, callableStatement );
        }

        return testcases;
    }

    public int getTestcasesCount( String whereClause ) throws DatabaseAccessException {

        String sqlLog = new SqlRequestFormatter().add( "where", whereClause ).format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_get_testcases_count(?) }" );
            callableStatement.setString( 1, whereClause );

            rs = callableStatement.executeQuery();
            int testcasesCount = 0;
            while( rs.next() ) {
                testcasesCount = rs.getInt( "testcasesCount" );
                logQuerySuccess( sqlLog, "test cases", testcasesCount );
                break;
            }

            return testcasesCount;
        } catch( Exception e ) {
            throw new DatabaseAccessException( "Error when " + sqlLog, e );
        } finally {
            DbUtils.closeResultSet( rs );
            DbUtils.close( connection, callableStatement );
        }
    }

    public List<Machine> getMachines() throws DatabaseAccessException {

        List<Machine> machines = new ArrayList<Machine>();

        Connection connection = getConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.prepareStatement( "SELECT * FROM tMachines ORDER BY machineName" );
            rs = statement.executeQuery();
            while( rs.next() ) {
                Machine machine = new Machine();
                machine.machineId = rs.getInt( "machineId" );
                machine.name = rs.getString( "machineName" );
                machine.alias = rs.getString( "machineAlias" );
                machines.add( machine );
            }
        } catch( Exception e ) {
            throw new DatabaseAccessException( "Error retrieving machines", e );
        } finally {
            DbUtils.closeResultSet( rs );
            DbUtils.close( connection, statement );
        }

        return machines;
    }

    public List<Message> getMessages( int startRecord, int recordsCount, String whereClause,
                                      String sortColumn, boolean ascending ) throws DatabaseAccessException {

        List<Message> messages = new ArrayList<Message>();

        SimpleDateFormat dateFormat = new SimpleDateFormat( "MMM dd" );
        SimpleDateFormat timeFormat = new SimpleDateFormat( "HH:mm:ss:S" );

        String sqlLog = new SqlRequestFormatter().add( "start record", startRecord )
                                                 .add( "records", recordsCount )
                                                 .add( "where", whereClause )
                                                 .add( "sort by", sortColumn )
                                                 .add( "asc", ascending )
                                                 .format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_get_messages(?, ?, ?, ?, ?) }" );
            callableStatement.setString( 1, String.valueOf( startRecord ) );
            callableStatement.setString( 2, String.valueOf( recordsCount ) );
            callableStatement.setString( 3, whereClause );
            callableStatement.setString( 4, sortColumn );
            callableStatement.setString( 5, ( ascending
                                                        ? "ASC"
                                                        : "DESC" ) );

            int numberRecords = 0;
            rs = callableStatement.executeQuery();
            Map<Integer, Message> splitMessages = new HashMap<Integer, Message>(); // <parentMessageId, Message>
            while( rs.next() ) {
                Message message = new Message();
                message.messageId = rs.getInt( "messageId" );
                message.messageContent = rs.getString( "message" );
                message.messageType = rs.getString( "typeName" );

                Timestamp timestamp = rs.getTimestamp( "timestamp" );
                message.date = dateFormat.format( timestamp );
                message.time = timeFormat.format( timestamp );

                message.machineName = rs.getString( "machineName" );
                message.threadName = rs.getString( "threadName" );
                message.parentMessageId = rs.getInt( "parentMessageId" );

                if( message.parentMessageId != 0 ) {
                    // split message
                    if( splitMessages.containsKey( message.parentMessageId ) ) {
                        // append to the message - result set is ordered by message ID
                        Message splitMessage = splitMessages.get( message.parentMessageId );
                        if( splitMessage.messageId < message.messageId ) {
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
                        splitMessages.put( message.parentMessageId, message );
                        messages.add( message );
                    }
                } else {
                    // single message
                    messages.add( message );
                }
                numberRecords++;
            }

            logQuerySuccess( sqlLog, "messages", numberRecords );
        } catch( Exception e ) {
            throw new DatabaseAccessException( "Error when " + sqlLog, e );
        } finally {
            DbUtils.closeResultSet( rs );
            DbUtils.close( connection, callableStatement );
        }
        return messages;
    }

    public List<Message> getRunMessages( int startRecord, int recordsCount, String whereClause,
                                         String sortColumn,
                                         boolean ascending ) throws DatabaseAccessException {

        List<Message> runMessages = new ArrayList<Message>();

        SimpleDateFormat dateFormat = new SimpleDateFormat( "MMM dd" );
        SimpleDateFormat timeFormat = new SimpleDateFormat( "HH:mm:ss:S" );

        String sqlLog = new SqlRequestFormatter().add( "start record", startRecord )
                                                 .add( "records", recordsCount )
                                                 .add( "where", whereClause )
                                                 .add( "sort by", sortColumn )
                                                 .add( "asc", ascending )
                                                 .format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_get_run_messages(?, ?, ?, ?, ?) }" );
            callableStatement.setString( 1, String.valueOf( startRecord ) );
            callableStatement.setString( 2, String.valueOf( recordsCount ) );
            callableStatement.setString( 3, whereClause );
            callableStatement.setString( 4, sortColumn );
            callableStatement.setString( 5, ( ascending
                                                        ? "ASC"
                                                        : "DESC" ) );

            int numberRecords = 0;
            rs = callableStatement.executeQuery();
            while( rs.next() ) {
                Message runMessage = new Message();
                runMessage.messageId = rs.getInt( "runMessageId" );
                runMessage.messageContent = rs.getString( "message" );
                runMessage.messageType = rs.getString( "typeName" );

                Timestamp timestamp = rs.getTimestamp( "timestamp" );
                runMessage.date = dateFormat.format( timestamp );
                runMessage.time = timeFormat.format( timestamp );

                runMessage.machineName = rs.getString( "machineName" );
                runMessage.threadName = rs.getString( "threadName" );
                runMessages.add( runMessage );

                numberRecords++;
            }

            logQuerySuccess( sqlLog, "run messages", numberRecords );
        } catch( Exception e ) {
            throw new DatabaseAccessException( "Error when " + sqlLog, e );
        } finally {
            DbUtils.closeResultSet( rs );
            DbUtils.close( connection, callableStatement );
        }

        return runMessages;
    }

    public List<Message> getSuiteMessages( int startRecord, int recordsCount, String whereClause,
                                           String sortColumn,
                                           boolean ascending ) throws DatabaseAccessException {

        List<Message> suiteMessages = new ArrayList<Message>();

        SimpleDateFormat dateFormat = new SimpleDateFormat( "MMM dd" );
        SimpleDateFormat timeFormat = new SimpleDateFormat( "HH:mm:ss:S" );

        String sqlLog = new SqlRequestFormatter().add( "start record", startRecord )
                                                 .add( "records", recordsCount )
                                                 .add( "where", whereClause )
                                                 .add( "sort by", sortColumn )
                                                 .add( "asc", ascending )
                                                 .format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_get_suite_messages(?, ?, ?, ?, ?) }" );
            callableStatement.setString( 1, String.valueOf( startRecord ) );
            callableStatement.setString( 2, String.valueOf( recordsCount ) );
            callableStatement.setString( 3, whereClause );
            callableStatement.setString( 4, sortColumn );
            callableStatement.setString( 5, ( ascending
                                                        ? "ASC"
                                                        : "DESC" ) );

            int numberRecords = 0;
            rs = callableStatement.executeQuery();
            while( rs.next() ) {
                Message suiteMessage = new Message();
                suiteMessage.messageId = rs.getInt( "suiteMessageId" );
                suiteMessage.messageContent = rs.getString( "message" );
                suiteMessage.messageType = rs.getString( "typeName" );

                Timestamp timestamp = rs.getTimestamp( "timestamp" );
                suiteMessage.date = dateFormat.format( timestamp );
                suiteMessage.time = timeFormat.format( timestamp );

                suiteMessage.machineName = rs.getString( "machineName" );
                suiteMessage.threadName = rs.getString( "threadName" );
                suiteMessages.add( suiteMessage );

                numberRecords++;
            }

            logQuerySuccess( sqlLog, "suite messages", numberRecords );
        } catch( Exception e ) {
            throw new DatabaseAccessException( "Error when " + sqlLog, e );
        } finally {
            DbUtils.closeResultSet( rs );
            DbUtils.close( connection, callableStatement );
        }

        return suiteMessages;
    }

    public int getMessagesCount( String whereClause ) throws DatabaseAccessException {

        String sqlLog = new SqlRequestFormatter().add( "where", whereClause ).format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_get_messages_count(?) }" );
            callableStatement.setString( 1, whereClause );

            rs = callableStatement.executeQuery();
            int messagesCount = 0;
            if( rs.next() ) {
                messagesCount = rs.getInt( "messagesCount" );
            }
            logQuerySuccess( sqlLog, "messages", messagesCount );

            return messagesCount;
        } catch( Exception e ) {
            throw new DatabaseAccessException( "Error when " + sqlLog, e );
        } finally {
            DbUtils.closeResultSet( rs );
            DbUtils.close( connection, callableStatement );
        }
    }

    public int getRunMessagesCount( String whereClause ) throws DatabaseAccessException {

        String sqlLog = new SqlRequestFormatter().add( "where", whereClause ).format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_get_run_messages_count(?) }" );
            callableStatement.setString( 1, whereClause );

            rs = callableStatement.executeQuery();
            int messagesCount = 0;
            if( rs.next() ) {
                messagesCount = rs.getInt( "messagesCount" );
            }
            logQuerySuccess( sqlLog, "run messages count", messagesCount );

            return messagesCount;
        } catch( Exception e ) {
            throw new DatabaseAccessException( "Error when " + sqlLog, e );
        } finally {
            DbUtils.closeResultSet( rs );
            DbUtils.close( connection, callableStatement );
        }
    }

    public int getSuiteMessagesCount( String whereClause ) throws DatabaseAccessException {

        String sqlLog = new SqlRequestFormatter().add( "where", whereClause ).format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_get_suite_messages_count(?) }" );
            callableStatement.setString( 1, whereClause );

            rs = callableStatement.executeQuery();
            int messagesCount = 0;
            if( rs.next() ) {
                messagesCount = rs.getInt( "messagesCount" );
            }
            logQuerySuccess( sqlLog, "suite messages count", messagesCount );

            return messagesCount;
        } catch( Exception e ) {
            throw new DatabaseAccessException( "Error when " + sqlLog, e );
        } finally {
            DbUtils.closeResultSet( rs );
            DbUtils.close( connection, callableStatement );
        }
    }

    public List<StatisticDescription> getSystemStatisticDescriptions( float timeOffset, String testcaseIds,
                                                                      Map<String, String> testcaseAliases ) throws DatabaseAccessException {

        List<StatisticDescription> statisticDescriptions = new ArrayList<StatisticDescription>();

        String sqlLog = new SqlRequestFormatter().add( "fdate", formatDateFromEpoch( timeOffset ) )
                                                 .add( "testcase ids", testcaseIds )
                                                 .format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_get_system_statistic_descriptions(?, ?) }" );
            callableStatement.setString( 1, formatDateFromEpoch( timeOffset ) );
            callableStatement.setString( 2, testcaseIds );

            rs = callableStatement.executeQuery();
            int numberRecords = 0;
            while( rs.next() ) {
                StatisticDescription statisticDescription = new StatisticDescription();

                statisticDescription.testcaseId = rs.getInt( "testcaseId" );

                // if user has provided testcase alias - use it instead the original testcase name
                if( testcaseAliases != null ) {
                    statisticDescription.testcaseName = testcaseAliases.get( String.valueOf( statisticDescription.testcaseId ) );
                }
                if( statisticDescription.testcaseName == null ) {
                    statisticDescription.testcaseName = rs.getString( "testcaseName" );
                }
                statisticDescription.testcaseStarttime = rs.getInt( "testcaseStarttime" );

                statisticDescription.machineId = rs.getInt( "machineId" );
                statisticDescription.machineName = rs.getString( "machineName" );

                statisticDescription.statisticTypeId = rs.getInt( "statsTypeId" );
                statisticDescription.statisticName = rs.getString( "name" );

                statisticDescription.unit = rs.getString( "units" );
                statisticDescription.params = rs.getString( "params" );
                statisticDescription.parent = rs.getString( "parentName" );
                statisticDescription.internalName = rs.getString( "internalName" );

                statisticDescription.numberMeasurements = rs.getInt( "statsNumberMeasurements" );
                statisticDescription.minValue = rs.getFloat( "statsMinValue" );
                statisticDescription.maxValue = rs.getFloat( "statsMaxValue" );
                statisticDescription.avgValue = rs.getFloat( "statsAvgValue" );

                statisticDescriptions.add( statisticDescription );

                numberRecords++;
            }

            logQuerySuccess( sqlLog, "system statistic descriptions", numberRecords );
        } catch( Exception e ) {
            throw new DatabaseAccessException( "Error when " + sqlLog, e );
        } finally {
            DbUtils.closeResultSet( rs );
            DbUtils.close( connection, callableStatement );
        }

        return statisticDescriptions;
    }

    public List<StatisticDescription> getCheckpointStatisticDescriptions( float timeOffset,
                                                                          String testcaseIds,
                                                                          Map<String, String> testcaseAliases ) throws DatabaseAccessException {

        List<StatisticDescription> statisticDescriptions = new ArrayList<StatisticDescription>();

        String sqlLog = new SqlRequestFormatter().add( "fdate", formatDateFromEpoch( timeOffset ) )
                                                 .add( "testcase ids", testcaseIds )
                                                 .format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_get_checkpoint_statistic_descriptions(?, ?) }" );
            callableStatement.setString( 1, formatDateFromEpoch( timeOffset ) );
            callableStatement.setString( 2, testcaseIds );

            rs = callableStatement.executeQuery();
            int numberRecords = 0;
            while( rs.next() ) {
                StatisticDescription statisticDescription = new StatisticDescription();

                statisticDescription.testcaseId = rs.getInt( "testcaseId" );

                // if user has provided testcase alias - use it instead the original testcase name
                if( testcaseAliases != null ) {
                    statisticDescription.testcaseName = testcaseAliases.get( String.valueOf( statisticDescription.testcaseId ) );
                }
                if( statisticDescription.testcaseName == null ) {
                    statisticDescription.testcaseName = rs.getString( "testcaseName" );
                }
                statisticDescription.testcaseStarttime = rs.getInt( "testcaseStarttime" );

                statisticDescription.machineId = 0; // Checkpoints will be collected and displayed for testcase
                statisticDescription.machineName = MACHINE_NAME_FOR_ATS_AGENTS;

                statisticDescription.queueName = rs.getString( "queueName" );

                statisticDescription.numberMeasurements = rs.getInt( "statsNumberMeasurements" );

                statisticDescription.statisticName = rs.getString( "name" );
                statisticDescription.unit = "ms"; // "statsUnit" field is null for checkpoint statistics, because the action response times are always measured in "ms"

                statisticDescriptions.add( statisticDescription );

                numberRecords++;
            }

            logQuerySuccess( sqlLog, "system statistic descriptions", numberRecords );
        } catch( Exception e ) {
            throw new DatabaseAccessException( "Error when " + sqlLog, e );
        } finally {
            DbUtils.closeResultSet( rs );
            DbUtils.close( connection, callableStatement );
        }

        return statisticDescriptions;
    }

    public List<Statistic> getSystemStatistics( float timeOffset, String testcaseIds, String machineIds,
                                                String statsTypeIds, Set<String> expectedStatisticUIDs,
                                                Set<Integer> expectedSingleStatisticIDs,
                                                Set<Integer> expectedCombinedStatisticIDs ) throws DatabaseAccessException {

        List<Statistic> allStatistics = new ArrayList<Statistic>();

        String sqlLog = new SqlRequestFormatter().add( "fdate", formatDateFromEpoch( timeOffset ) )
                                                 .add( "testcase ids", testcaseIds )
                                                 .add( "machine ids", machineIds )
                                                 .add( "stats type ids", statsTypeIds )
                                                 .format();

        /*
         * Combined statistics do not have real statistic IDs, but Test Explorer needs some.
         * We must provide unique IDs
         */
        Map<String, Integer> statisticFakeIds = new HashMap<String, Integer>();

        /*
         * The DB does not contain combined statistics, so we must create them.
         * All values of statistic with same statistic ID and timestamp are summed into 1 statistic
         */
        Map<String, Statistic> combinedStatistics = new HashMap<String, Statistic>();

        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_get_system_statistics(?, ?, ?, ?) }" );

            callableStatement.setString( 1, formatDateFromEpoch( timeOffset ) );
            callableStatement.setString( 2, testcaseIds );
            callableStatement.setString( 3, machineIds );
            callableStatement.setString( 4, statsTypeIds );

            rs = callableStatement.executeQuery();
            int numberRecords = 0;
            while( rs.next() ) {
                Statistic statistic = new Statistic();
                statistic.statisticTypeId = rs.getInt( "statsTypeId" );
                statistic.name = rs.getString( "statsName" );
                statistic.parentName = rs.getString( "statsParent" );
                statistic.unit = rs.getString( "statsUnit" );
                statistic.value = rs.getFloat( "value" );
                statistic.date = rs.getString( "statsAxis" );
                statistic.timestamp = rs.getInt( "statsAxisTimestamp" );

                statistic.machineId = rs.getInt( "machineId" );
                statistic.testcaseId = rs.getInt( "testcaseId" );

                boolean theUidIsExpected = false;
                if( expectedStatisticUIDs == null ) {
                    // used when copying a testcase
                    theUidIsExpected = true;
                } else {
                    String statisticUidToMatch = statistic.getUid().replace( "[", "" ).replace( "]", "" );
                    for( String expectedStatisticUID : expectedStatisticUIDs ) {
                        // we use matchers for combining statistics into virtual containers
                        if( statisticUidToMatch.matches( expectedStatisticUID.replace( "[", "" )
                                                                             .replace( "]", "" ) ) ) {
                            theUidIsExpected = true;
                            break;
                        }
                    }
                }

                if( theUidIsExpected ) {

                    // add to single statistics
                    if( expectedSingleStatisticIDs == null ) {
                        // used when copying a testcase
                        allStatistics.add( statistic );
                    } else if( expectedSingleStatisticIDs.contains( statistic.statisticTypeId ) ) {
                        allStatistics.add( statistic );
                    }

                    // add to combined statistics
                    if( expectedCombinedStatisticIDs != null
                        && expectedCombinedStatisticIDs.contains( statistic.statisticTypeId ) ) {

                        String statisticTempKey = statistic.statisticTypeId + "->" + statistic.timestamp;
                        Statistic combinedStatistic = combinedStatistics.get( statisticTempKey );
                        if( combinedStatistic == null ) {
                            // create a new combined statistic
                            combinedStatistic = new Statistic();
                            combinedStatistic.name = statistic.name;
                            combinedStatistic.parentName = Statistic.COMBINED_STATISTICS_CONTAINER;
                            combinedStatistic.unit = statistic.unit;
                            combinedStatistic.timestamp = statistic.timestamp;
                            combinedStatistic.machineId = statistic.machineId;
                            combinedStatistic.testcaseId = statistic.testcaseId;
                            combinedStatistic.statisticTypeId = getStatisticFakeId( DbReadAccess.START_FAKE_ID_VALUE_FOR_AGGREGATED_SYSTEM_STATISTICS,
                                                                                    statisticFakeIds,
                                                                                    combinedStatistic );

                            combinedStatistics.put( statisticTempKey, combinedStatistic );
                        }

                        // calculate the combined value
                        combinedStatistic.value = combinedStatistic.value + statistic.value;
                    }

                    numberRecords++;
                }
            }

            if( combinedStatistics.size() > 0 ) {
                // sort the combined statistics by their timestamps
                List<Statistic> sortedStatistics = new ArrayList<Statistic>( combinedStatistics.values() );
                Collections.sort( sortedStatistics, new Comparator<Statistic>() {

                    @Override
                    public int compare( Statistic stat1, Statistic stat2 ) {

                        return ( int ) ( stat1.timestamp - stat2.timestamp );
                    }
                } );

                // add the combined statistics to the others
                allStatistics.addAll( sortedStatistics );
            }

            logQuerySuccess( sqlLog, "system statistics", numberRecords );
        } catch( Exception e ) {
            throw new DatabaseAccessException( "Error when " + sqlLog, e );
        } finally {
            DbUtils.closeResultSet( rs );
            DbUtils.close( connection, callableStatement );
        }

        return allStatistics;
    }

    public List<Statistic> getSystemAggregatedStatistics( float timeOffset, String testcaseIds,
                                                          String machineIds, String statsTypeIds,
                                                          int interval,
                                                          int mode ) throws DatabaseAccessException {

        List<Statistic> statistics = new ArrayList<Statistic>();

        String sqlLog = new SqlRequestFormatter().add( "testcase ids", testcaseIds )
                                                 .add( "fdate", formatDateFromEpoch( timeOffset ) )
                                                 .add( "machine ids", machineIds )
                                                 .add( "stats type ids", statsTypeIds )
                                                 .add( "inverval (seconds)", interval )
                                                 .add( "mode (AVG-0001,SUM-0010,TOTALS-0100,COUNT-1000)",
                                                       mode )
                                                 .format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_get_system_aggregated_statistics(?, ?, ?, ?, ?, ?) }" );

            callableStatement.setString( 1, formatDateFromEpoch( timeOffset ) );
            callableStatement.setString( 2, testcaseIds );
            callableStatement.setString( 3, machineIds );
            callableStatement.setString( 4, statsTypeIds );
            callableStatement.setInt( 5, interval );
            callableStatement.setInt( 6, mode );

            rs = callableStatement.executeQuery();
            Map<Integer, Float> totalSumValues = new HashMap<Integer, Float>();
            int numberRecords = 0;
            while( rs.next() ) {
                Statistic statistic = new Statistic();
                statistic.statisticTypeId = rs.getInt( "statsTypeId" );
                statistic.name = rs.getString( "statsName" );
                statistic.unit = rs.getString( "statsUnit" );
                statistic.avgValue = rs.getFloat( "avgValue" );
                statistic.sumValue = rs.getFloat( "sumValue" );
                statistic.countValue = rs.getFloat( "countValue" );
                if( StatisticAggregatedType.isTotals( mode ) ) { // total sum value

                    float totalSumValue = statistic.sumValue;
                    if( totalSumValues.containsKey( statistic.statisticTypeId ) ) {
                        totalSumValue += totalSumValues.get( statistic.statisticTypeId );
                    }
                    totalSumValues.put( statistic.statisticTypeId, totalSumValue );
                    statistic.totalValue = totalSumValue;
                }
                statistic.timestamp = rs.getInt( "timestamp" );

                statistic.machineId = rs.getInt( "machineId" );
                statistic.testcaseId = rs.getInt( "testcaseId" );

                statistics.add( statistic );

                numberRecords++;
            }

            logQuerySuccess( sqlLog, "system aggregated statistics", numberRecords );
        } catch( Exception e ) {
            throw new DatabaseAccessException( "Error when " + sqlLog, e );
        } finally {
            DbUtils.closeResultSet( rs );
            DbUtils.close( connection, callableStatement );
        }

        return statistics;
    }

    public List<Statistic> getCheckpointStatistics( float timeOffset, String testcaseIds, String actionNames,
                                                    Set<String> expectedSingleActionUIDs,
                                                    Set<String> expectedCombinedActionUIDs ) throws DatabaseAccessException {

        List<Statistic> allStatistics = new ArrayList<Statistic>();

        String sqlLog = new SqlRequestFormatter().add( "fdate", formatDateFromEpoch( timeOffset ) )
                                                 .add( "testcase ids", testcaseIds )
                                                 .add( "checkpoint names", actionNames )
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

            callableStatement = connection.prepareCall( "{ call sp_get_checkpoint_statistics(?, ?, ?) }" );

            callableStatement.setString( 1, formatDateFromEpoch( timeOffset ) );
            callableStatement.setString( 2, testcaseIds );
            callableStatement.setString( 3, actionNames );

            int numberRecords = 0;
            rs = callableStatement.executeQuery();
            while( rs.next() ) {

                // add new statistic
                Statistic statistic = new Statistic();
                statistic.name = rs.getString( "statsName" );
                statistic.parentName = rs.getString( "queueName" );
                statistic.unit = "ms";
                statistic.value = rs.getFloat( "value" );
                statistic.timestamp = rs.getLong( "statsAxisTimestamp" );
                statistic.machineId = 0; // Checkpoints will be collected and displayed for testcase
                statistic.testcaseId = rs.getInt( "testcaseId" );

                statistic.statisticTypeId = getStatisticFakeId( START_FAKE_ID_VALUE_FOR_CHECKPOINTS,
                                                                fakeStatisticIds, statistic );

                // add to single statistics
                if( expectedSingleActionUIDs.contains( statistic.getUid() ) ) {
                    allStatistics.add( statistic );
                }

                // add to combined statistics
                if( expectedCombinedActionUIDs.contains( statistic.getCombinedStatisticUid() ) ) {

                    String statisticKey = statistic.timestamp + "->" + statistic.name;
                    Integer timesHaveThisStatisticAtThisTimestamp = combinedStatisticHitsAtSameTimestamp.get( statisticKey );

                    Statistic combinedStatistic;
                    if( timesHaveThisStatisticAtThisTimestamp == null ) {
                        // create a new combined statistic
                        combinedStatistic = new Statistic();
                        combinedStatistic.name = statistic.name;
                        combinedStatistic.parentName = Statistic.COMBINED_STATISTICS_CONTAINER;
                        combinedStatistic.unit = statistic.unit;
                        combinedStatistic.timestamp = statistic.timestamp;
                        combinedStatistic.machineId = statistic.machineId;
                        combinedStatistic.testcaseId = statistic.testcaseId;

                        // this is the first such statistic at this timestamp
                        timesHaveThisStatisticAtThisTimestamp = 1;
                    } else {
                        // create another copy of this statistic
                        combinedStatistic = combinedStatistics.get( statisticKey + "->"
                                                                    + timesHaveThisStatisticAtThisTimestamp )
                                                              .newInstance();

                        // we already had such statistic at this timestamp
                        timesHaveThisStatisticAtThisTimestamp++;
                    }

                    combinedStatistic.value = statistic.value;
                    combinedStatistic.statisticTypeId = getStatisticFakeId( START_FAKE_ID_VALUE_FOR_CHECKPOINTS,
                                                                            fakeStatisticIds,
                                                                            combinedStatistic );

                    // remember how many times we got same statistic at same timestamp
                    combinedStatisticHitsAtSameTimestamp.put( statisticKey,
                                                              timesHaveThisStatisticAtThisTimestamp );
                    // Remember this statistic in the list
                    // The way we create the map key assures the proper time ordering
                    combinedStatistics.put( statisticKey + "->" + timesHaveThisStatisticAtThisTimestamp,
                                            combinedStatistic );
                }

                numberRecords++;
            }

            if( combinedStatistics.size() > 0 ) {
                // sort the combined statistics by their timestamps
                List<Statistic> sortedStatistics = new ArrayList<Statistic>( combinedStatistics.values() );
                Collections.sort( sortedStatistics, new Comparator<Statistic>() {

                    @Override
                    public int compare( Statistic stat1, Statistic stat2 ) {

                        return ( int ) ( stat1.timestamp - stat2.timestamp );
                    }
                } );

                // add the combined statistics to the others
                allStatistics.addAll( sortedStatistics );
            }

            logQuerySuccess( sqlLog, "action response statistics", numberRecords );
        } catch( Exception e ) {
            throw new DatabaseAccessException( "Error when " + sqlLog, e );
        } finally {
            DbUtils.closeResultSet( rs );
            DbUtils.close( connection, callableStatement );
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

        Integer statisticId = statisticFakeIds.get( statisticUID );
        if( statisticId == null ) {
            statisticId = startValue - statisticFakeIds.size();
            statisticFakeIds.put( statisticUID, statisticId );
        }

        return statisticId;
    }

    public List<Statistic> getCheckpointAggregatedStatistics( float timeOffset, String testcaseIds,
                                                              String actionNames,
                                                              Set<String> expectedSingleActionUIDs,
                                                              Set<String> expectedCombinedActionUIDs,
                                                              int interval,
                                                              int mode ) throws DatabaseAccessException {

        List<Statistic> allStatistics = new ArrayList<Statistic>();

        String sqlLog = new SqlRequestFormatter().add( "testcase ids", testcaseIds )
                                                 .add( "fdate", formatDateFromEpoch( timeOffset ) )
                                                 .add( "checkpoint names", actionNames )
                                                 .add( "inverval (seconds)", interval )
                                                 .add( "mode (AVG-0001,SUM-0010,TOTALS-0100,COUNT-1000)",
                                                       mode )
                                                 .format();

        Map<String, Integer> fakeStatisticIds = new HashMap<String, Integer>();

        /*
         * The DB does not contain combined statistics, so we must create them.
         * All values of statistic with same name and timestamp are summed into 1 statistic
         */
        Map<String, Statistic> combinedStatistics = new HashMap<String, Statistic>();

        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_get_checkpoint_aggregated_statistics(?, ?, ?, ?, ?) }" );

            callableStatement.setString( 1, formatDateFromEpoch( timeOffset ) );
            callableStatement.setString( 2, testcaseIds );
            callableStatement.setString( 3, actionNames );
            callableStatement.setInt( 4, interval );
            callableStatement.setInt( 5, mode );

            int numberRecords = 0;
            Map<String, Float> totalSumValues = new HashMap<String, Float>();
            rs = callableStatement.executeQuery();
            while( rs.next() ) {

                Statistic statistic = new Statistic();
                statistic.name = rs.getString( "statsName" );
                statistic.parentName = rs.getString( "queueName" );
                statistic.unit = "ms";// "statsUnit" field is null for checkpoint statistics, because the action response times are always measured in "ms"
                statistic.avgValue = rs.getFloat( "avgValue" );
                statistic.sumValue = rs.getFloat( "sumValue" );
                statistic.countValue = rs.getFloat( "countValue" );
                if( StatisticAggregatedType.isTotals( mode ) ) { // total sum value

                    float totalSumValue = statistic.sumValue;
                    if( totalSumValues.containsKey( statistic.name ) ) {
                        totalSumValue += totalSumValues.get( statistic.name );
                    }
                    totalSumValues.put( statistic.name, totalSumValue );
                    statistic.totalValue = totalSumValue;
                }
                statistic.timestamp = rs.getLong( "timestamp" );
                statistic.machineId = 0; // Checkpoints will be collected and displayed for testcase
                statistic.testcaseId = rs.getInt( "testcaseId" );
                statistic.statisticTypeId = getStatisticFakeId( START_FAKE_ID_VALUE_FOR_AGGREGATED_CHECKPOINTS,
                                                                fakeStatisticIds, statistic );

                // add to single statistics
                if( expectedSingleActionUIDs.contains( statistic.getUid() ) ) {
                    allStatistics.add( statistic );
                }

                // add to combined statistics
                if( expectedCombinedActionUIDs.contains( statistic.getCombinedStatisticUid() ) ) {

                    String statisticTempKey = statistic.timestamp + "->" + statistic.name;
                    Statistic combinedStatistic = combinedStatistics.get( statisticTempKey );
                    if( combinedStatistic == null ) {
                        // create a new combined statistic
                        combinedStatistic = new Statistic();
                        combinedStatistic.name = statistic.name;
                        combinedStatistic.parentName = Statistic.COMBINED_STATISTICS_CONTAINER;
                        combinedStatistic.unit = statistic.unit;
                        combinedStatistic.timestamp = statistic.timestamp;
                        combinedStatistic.machineId = statistic.machineId;
                        combinedStatistic.testcaseId = statistic.testcaseId;
                        combinedStatistic.statisticTypeId = getStatisticFakeId( START_FAKE_ID_VALUE_FOR_AGGREGATED_CHECKPOINTS,
                                                                                fakeStatisticIds,
                                                                                combinedStatistic );

                        combinedStatistics.put( statisticTempKey, combinedStatistic );
                    }

                    // calculate the combined value
                    combinedStatistic.avgValue = combinedStatistic.avgValue + statistic.avgValue;
                    combinedStatistic.sumValue = combinedStatistic.sumValue + statistic.sumValue;
                    combinedStatistic.countValue = combinedStatistic.countValue + statistic.countValue;
                    combinedStatistic.totalValue = combinedStatistic.totalValue + statistic.totalValue;
                }

                numberRecords++;
            }

            if( combinedStatistics.size() > 0 ) {
                // sort the combined statistics by their timestamps
                List<Statistic> sortedStatistics = new ArrayList<Statistic>( combinedStatistics.values() );
                Collections.sort( sortedStatistics, new Comparator<Statistic>() {

                    @Override
                    public int compare( Statistic stat1, Statistic stat2 ) {

                        return ( int ) ( stat1.timestamp - stat2.timestamp );
                    }
                } );

                // add the combined statistics to the others
                allStatistics.addAll( sortedStatistics );
            }

            logQuerySuccess( sqlLog, "checkpoint aggregated statistics", numberRecords );
        } catch( Exception e ) {
            throw new DatabaseAccessException( "Error when " + sqlLog, e );
        } finally {
            DbUtils.closeResultSet( rs );
            DbUtils.close( connection, callableStatement );
        }

        return allStatistics;
    }

    public List<LoadQueue> getLoadQueues( String whereClause, String sortColumn, boolean ascending,
                                          boolean dateFormatNoYear ) throws DatabaseAccessException {

        List<LoadQueue> loadQueues = new ArrayList<LoadQueue>();

        String sqlLog = new SqlRequestFormatter().add( "where", whereClause )
                                                 .add( "sort by", sortColumn )
                                                 .add( "asc", ascending )
                                                 .format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_get_loadqueues(?, ?, ?) }" );
            callableStatement.setString( 1, "where " + whereClause );
            callableStatement.setString( 2, sortColumn );
            callableStatement.setString( 3, ( ascending
                                                        ? "ASC"
                                                        : "DESC" ) );

            rs = callableStatement.executeQuery();
            int numberRecords = 0;
            while( rs.next() ) {
                LoadQueue loadQueue = new LoadQueue();
                loadQueue.loadQueueId = rs.getInt( "loadQueueId" );
                loadQueue.name = rs.getString( "name" );
                loadQueue.sequence = rs.getInt( "sequence" );
                loadQueue.hostsList = rs.getString( "hostsList" );
                loadQueue.threadingPattern = rs.getString( "threadingPattern" );
                loadQueue.numberThreads = rs.getInt( "numberThreads" );
                if( loadQueue.threadingPattern != null ) {
                    loadQueue.threadingPattern = loadQueue.threadingPattern.replace( "<number_threads>",
                                                                                     String.valueOf( loadQueue.numberThreads ) );
                }

                if( dateFormatNoYear ) {
                    loadQueue.dateStart = formatDateNoYear( rs.getTimestamp( "dateStart" ) );
                    loadQueue.dateEnd = formatDateNoYear( rs.getTimestamp( "dateEnd" ) );
                } else {
                    loadQueue.dateStart = formatDate( rs.getTimestamp( "dateStart" ) );
                    loadQueue.dateEnd = formatDate( rs.getTimestamp( "dateEnd" ) );
                }

                int duration = rs.getInt( "duration" );
                if( duration < 0 ) {
                    // this may happen when the load queue is not ended and the time of the log server
                    // is behind with the time of the test executor host
                    duration = 0;
                }
                loadQueue.duration = formatTimeDiffereceFromSecondsToString( duration );
                loadQueue.result = rs.getInt( "result" );
                /*
                 *   -- 0 FAILED
                 *   -- 1 PASSED
                 *   -- 2 SKIPPED
                 *   -- 4 RUNNING
                 */
                switch( loadQueue.result ){
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

                loadQueues.add( loadQueue );
                numberRecords++;
            }

            logQuerySuccess( sqlLog, "loadqueues", numberRecords );
        } catch( Exception e ) {
            throw new DatabaseAccessException( "Error when " + sqlLog, e );
        } finally {
            DbUtils.closeResultSet( rs );
            DbUtils.close( connection, callableStatement );
        }

        return loadQueues;
    }

    public List<CheckpointSummary> getCheckpointsSummary( String whereClause, String sortColumn,
                                                          boolean ascending ) throws DatabaseAccessException {

        List<CheckpointSummary> checkpoints = new ArrayList<CheckpointSummary>();

        String sqlLog = new SqlRequestFormatter().add( "where", whereClause )
                                                 .add( "sort by", sortColumn )
                                                 .add( "asc", ascending )
                                                 .format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        ResultSet rs = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_get_checkpoints_summary(?, ?, ?) }" );
            callableStatement.setString( 1, "where " + whereClause );
            callableStatement.setString( 2, sortColumn );
            callableStatement.setString( 3, ( ascending
                                                        ? "ASC"
                                                        : "DESC" ) );

            rs = callableStatement.executeQuery();
            int numberRecords = 0;
            while( rs.next() ) {
                CheckpointSummary checkpointSummary = new CheckpointSummary();
                checkpointSummary.checkpointSummaryId = rs.getInt( "checkpointSummaryId" );
                checkpointSummary.name = rs.getString( "name" );

                checkpointSummary.numRunning = rs.getInt( "numRunning" );
                checkpointSummary.numPassed = rs.getInt( "numPassed" );
                checkpointSummary.numFailed = rs.getInt( "numFailed" );
                checkpointSummary.numTotal = checkpointSummary.numRunning + checkpointSummary.numPassed
                                             + checkpointSummary.numFailed;

                checkpointSummary.minResponseTime = rs.getInt( "minResponseTime" );
                if( checkpointSummary.minResponseTime == Integer.MAX_VALUE ) {
                    checkpointSummary.minResponseTime = 0;
                }
                checkpointSummary.avgResponseTime = rs.getFloat( "avgResponseTime" );
                checkpointSummary.maxResponseTime = rs.getInt( "maxResponseTime" );

                checkpointSummary.minTransferRate = rs.getFloat( "minTransferRate" );
                if( checkpointSummary.minTransferRate == Integer.MAX_VALUE ) {
                    checkpointSummary.minTransferRate = 0.0F;
                }
                checkpointSummary.avgTransferRate = rs.getFloat( "avgTransferRate" );
                checkpointSummary.maxTransferRate = rs.getFloat( "maxTransferRate" );
                checkpointSummary.transferRateUnit = rs.getString( "transferRateUnit" );

                checkpoints.add( checkpointSummary );
                numberRecords++;
            }

            logQuerySuccess( sqlLog, "checkpoints summary", numberRecords );
        } catch( Exception e ) {
            throw new DatabaseAccessException( "Error when " + sqlLog, e );
        } finally {
            DbUtils.closeResultSet( rs );
            DbUtils.close( connection, callableStatement );
        }

        return checkpoints;
    }

    public List<Checkpoint> getCheckpoints( String testcaseId,
                                            String checkpointName ) throws DatabaseAccessException {

        List<Checkpoint> checkpoints = new ArrayList<Checkpoint>();

        String sqlLog = new SqlRequestFormatter().add( "testcase id", testcaseId )
                                                 .add( "checkpoint name", checkpointName )
                                                 .format();
        Connection connection = getConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {

            statement = connection.prepareStatement( "SELECT ch.checkpointId, ch.responseTime, ch.transferRate, ch.transferRateUnit, ch.result,"
                                                     + " DATEDIFF(second, CONVERT( datetime, '1970-01-01 00:00:00', 20), ch.endTime) as endTime "
                                                     + "FROM tCheckpoints ch"
                                                     + " INNER JOIN tCheckpointsSummary chs on (chs.checkpointSummaryId = ch.checkpointSummaryId)"
                                                     + " INNER JOIN tLoadQueues c on (c.loadQueueId = chs.loadQueueId)"
                                                     + " INNER JOIN tTestcases tt on (tt.testcaseId = c.testcaseId) "
                                                     + "WHERE tt.testcaseId = ? AND ch.name = ?" );

            statement.setString( 1, testcaseId );
            statement.setString( 2, checkpointName );

            rs = statement.executeQuery();
            while( rs.next() ) {

                Checkpoint checkpoint = new Checkpoint();
                checkpoint.checkpointId = rs.getInt( "checkpointId" );
                checkpoint.name = checkpointName;
                checkpoint.responseTime = rs.getInt( "responseTime" );
                checkpoint.transferRate = rs.getFloat( "transferRate" );
                checkpoint.transferRateUnit = rs.getString( "transferRateUnit" );
                checkpoint.result = rs.getInt( "result" );
                checkpoint.endTime = rs.getLong( "endTime" );

                checkpoints.add( checkpoint );
            }

            logQuerySuccess( sqlLog, "checkpoints", checkpoints.size() );
        } catch( Exception e ) {
            throw new DatabaseAccessException( "Error when " + sqlLog, e );
        } finally {
            DbUtils.closeResultSet( rs );
            DbUtils.close( connection, statement );
        }

        return checkpoints;
    }

    protected Connection getConnection() throws DatabaseAccessException {

        Connection connection = super.getConnection();
        try {
            connection.setTransactionIsolation( Connection.TRANSACTION_READ_UNCOMMITTED );
        } catch( SQLException e ) {
            // Not a big deal, we will not read the entities which are in the process
            // of being inserted, but the transaction is still not completed
        }
        return connection;
    }

}
