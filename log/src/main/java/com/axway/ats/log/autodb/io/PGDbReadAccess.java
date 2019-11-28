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
import java.util.ArrayList;
import java.util.List;

import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.DbUtils;
import com.axway.ats.log.autodb.SqlRequestFormatter;
import com.axway.ats.log.autodb.entities.Checkpoint;
import com.axway.ats.log.autodb.entities.Machine;
import com.axway.ats.log.autodb.entities.RunMetaInfo;
import com.axway.ats.log.autodb.entities.ScenarioMetaInfo;
import com.axway.ats.log.autodb.entities.TestcaseMetainfo;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;

public class PGDbReadAccess extends SQLServerDbReadAccess {

    public PGDbReadAccess( DbConnection dbConnection ) {
        super(dbConnection);
    }

    @Override
    public List<Machine> getMachines() throws DatabaseAccessException {

        List<Machine> machines = new ArrayList<Machine>();

        Connection connection = getConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.prepareStatement("SELECT * FROM \"tMachines\" ORDER BY machineName");
            rs = statement.executeQuery();
            while (rs.next()) {
                Machine machine = new Machine();
                machine.machineId = rs.getInt("machineId");
                machine.name = rs.getString("machineName");
                machine.alias = rs.getString("machineAlias");
                machines.add(machine);
            }
        } catch (Exception e) {
            throw new DatabaseAccessException("Error retrieving machines", e);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(connection, statement);
        }

        return machines;
    }

    public List<Checkpoint> getCheckpoints( String testcaseId, String checkpointName, int utcTimeOffset,
                                            boolean dayLightSavingOn ) throws DatabaseAccessException {

        List<Checkpoint> checkpoints = new ArrayList<Checkpoint>();

        String sqlLog = new SqlRequestFormatter().add("testcase id", testcaseId)
                                                 .add("checkpoint name", checkpointName)
                                                 .format();
        Connection connection = getConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {

            statement = connection.prepareStatement("SELECT ch.checkpointId, ch.responseTime, ch.transferRate, ch.transferRateUnit, ch.result,"
                                                    + " CAST(EXTRACT(EPOCH FROM ch.endTime - CAST( '1970-01-01 00:00:00' AS TIMESTAMP)) AS INTEGER) as endTime, "
                                                    + " ch.endtime AS copyEndTime "
                                                    + "FROM \"tCheckpoints\" ch"
                                                    + " INNER JOIN \"tCheckpointsSummary\" chs on (chs.checkpointSummaryId = ch.checkpointSummaryId)"
                                                    + " INNER JOIN \"tLoadQueues\" c on (c.loadQueueId = chs.loadQueueId)"
                                                    + " INNER JOIN \"tTestcases\" tt on (tt.testcaseId = c.testcaseId) "
                                                    + "WHERE tt.testcaseId = CAST(? AS INTEGER) AND ch.name = ?");
            statement.setInt(1, Integer.parseInt(testcaseId));
            statement.setString(2, checkpointName);

            rs = statement.executeQuery();
            while (rs.next()) {

                Checkpoint checkpoint = new Checkpoint();
                checkpoint.checkpointId = rs.getInt("checkpointId");
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

    @Override
    public List<RunMetaInfo> getRunMetaInfo( int runId ) throws DatabaseAccessException {

        List<RunMetaInfo> runMetaInfoList = new ArrayList<>();
        Connection connection = getConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.prepareStatement("SELECT * FROM \"tRunMetainfo\" WHERE runId = "
                                                    + runId);
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
            throw new DatabaseAccessException("Error retrieving run metainfo for run with id '" + runId
                                              + "'", e);
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
            statement = connection.prepareStatement("SELECT * FROM \"tScenarioMetainfo\" WHERE scenarioId = "
                                                    + scenarioId);
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
            throw new DatabaseAccessException("Error retrieving scenario metainfo for scenario with id '"
                                              + scenarioId + "'", e);
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
            statement = connection.prepareStatement("SELECT * FROM \"tTestcaseMetainfo\" WHERE testcaseId = " + testcaseId);
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
