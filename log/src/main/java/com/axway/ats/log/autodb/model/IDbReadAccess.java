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
package com.axway.ats.log.autodb.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

public interface IDbReadAccess {

    public List<Run> getRuns(
                              int startRecord,
                              int recordsCount,
                              String whereClause,
                              String sortColumn,
                              boolean ascending,
                              int utcTimeOffset ) throws DatabaseAccessException;

    public int getRunsCount(
                             String whereClause ) throws DatabaseAccessException;

    public List<Suite> getSuites(
                                  int startRecord,
                                  int recordsCount,
                                  String whereClause,
                                  String sortColumn,
                                  boolean ascending,
                                  int utcTimeOffset ) throws DatabaseAccessException;

    public int getSuitesCount(
                               String whereClause ) throws DatabaseAccessException;

    public List<Scenario> getScenarios(
                                        int startRecord,
                                        int recordsCount,
                                        String whereClause,
                                        String sortColumn,
                                        boolean ascending,
                                        int utcTimeOffset ) throws DatabaseAccessException;

    public int getScenariosCount(
                                  String whereClause ) throws DatabaseAccessException;

    public List<Testcase> getTestcases(
                                        int startRecord,
                                        int recordsCount,
                                        String whereClause,
                                        String sortColumn,
                                        boolean ascending,
                                        int utcTimeOffset ) throws DatabaseAccessException;

    public int getTestcasesCount(
                                  String whereClause ) throws DatabaseAccessException;

    public List<Machine> getMachines() throws DatabaseAccessException;

    public List<Machine> getMachines( String whereClause ) throws DatabaseAccessException;

    public List<Message> getMessages(
                                      int startRecord,
                                      int recordsCount,
                                      String whereClause,
                                      String sortColumn,
                                      boolean ascending,
                                      int utcTimeOffset ) throws DatabaseAccessException;

    public List<Message> getRunMessages(
                                         int startRecord,
                                         int recordsCount,
                                         String whereClause,
                                         String sortColumn,
                                         boolean ascending,
                                         int utcTimeOffset ) throws DatabaseAccessException;

    public List<Message> getSuiteMessages(
                                           int startRecord,
                                           int recordsCount,
                                           String whereClause,
                                           String sortColumn,
                                           boolean ascending,
                                           int utcTimeOffset ) throws DatabaseAccessException;

    public int getMessagesCount(
                                 String whereClause ) throws DatabaseAccessException;

    public int getRunMessagesCount(
                                    String whereClause ) throws DatabaseAccessException;

    public int getSuiteMessagesCount(
                                      String whereClause ) throws DatabaseAccessException;

    public List<StatisticDescription> getSystemStatisticDescriptions(
                                                                      float timeOffset,
                                                                      String whereClause,
                                                                      Map<String, String> testcaseAliases,
                                                                      int utcTimeOffset,
                                                                      boolean dayLightSavingOn ) throws DatabaseAccessException;

    public List<StatisticDescription> getCheckpointStatisticDescriptions(
                                                                          float timeOffset,
                                                                          String whereClause,
                                                                          Set<String> expectedSingleActionUIDs,
                                                                          int utcTimeOffset,
                                                                          boolean dayLightSavingOn ) throws DatabaseAccessException;

    public Map<String, Integer>
            getNumberOfCheckpointsPerQueue( String testcaseIds ) throws DatabaseAccessException;

    public List<Statistic> getSystemStatistics(
                                                float timeOffset,
                                                String testcaseIds,
                                                String machineIds,
                                                String statsTypeIds,
                                                int utcTimeOffset,
                                                boolean dayLightSavingOn ) throws DatabaseAccessException;

    public List<Statistic> getSystemStatistics(
                                                float timeOffset,
                                                String testcaseIds,
                                                String machineIds,
                                                String statsTypeIds,
                                                String whereClause,
                                                int utcTimeOffset,
                                                boolean dayLightSavingOn ) throws DatabaseAccessException;

    public List<Statistic> getSystemStatistics( String testcaseIds,
                                                String machineIds,
                                                String statsTypeIds,
                                                String whereClause ) throws DatabaseAccessException;

    public List<Checkpoint> getCheckpoints( String testcaseId,
                                            int loadQueueId,
                                            String checkpointName,
                                            int utcTimeOffset,
                                            boolean dayLightSavingOn ) throws DatabaseAccessException;

    public List<Checkpoint> getCheckpoints( String testcaseId,
                                            int loadQueueId,
                                            String checkpointName,
                                            String whereClause,
                                            int utcTimeOffset,
                                            boolean dayLightSavingOn ) throws DatabaseAccessException;

    public List<Statistic> getCheckpointStatistics(
                                                    float timeOffset,
                                                    String testcaseIds,
                                                    String actionNames,
                                                    String actionParents,
                                                    Set<String> expectedSingleActionUIDs,
                                                    Set<String> expectedCombinedActionUIDs,
                                                    int utcTimeOffset,
                                                    boolean dayLightSavingOn ) throws DatabaseAccessException;

    public List<LoadQueue> getLoadQueues(
                                          String whereClause,
                                          String sortColumn,
                                          boolean ascending,
                                          int utcTimeOffset ) throws DatabaseAccessException;

    public List<CheckpointSummary> getCheckpointsSummary(
                                                          String whereClause,
                                                          String sortColumn,
                                                          boolean ascending ) throws DatabaseAccessException;

    public List<RunMetaInfo> getRunMetaInfo( int runId ) throws DatabaseAccessException;

    public List<ScenarioMetaInfo> getScenarioMetaInfo( int scenarioId ) throws DatabaseAccessException;

    public List<TestcaseMetainfo> getTestcaseMetainfo( int testcaseId ) throws DatabaseAccessException;

}
