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

import com.axway.ats.log.autodb.CheckpointInfo;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;

public interface IDbWriteAccess {

    /**
     * Specify max number of events to be collected for batch mode.
     * <p>Should be invoked before any insert event is invoked.</p>
     * @param maxNumberOfCachedEvents
     */
    public void setMaxNumberOfCachedEvents( int maxNumberOfCachedEvents );

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
    public int startRun( String runName, String osName, String productName, String versionName,
                         String buildName, long timestamp, String hostName,
                         boolean closeConnection ) throws DatabaseAccessException;

    /**
     * End a run in the database
     *
     * @param timestamp
     * @param runId
     */
    public void endRun( long timestamp, int runId, boolean closeConnection ) throws DatabaseAccessException;

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
     * @throws DatabaseAccessException
     */
    public void updateRun( int runId, String runName, String osName, String productName, String versionName,
                           String buildName, String userNote, String hostName,
                           boolean closeConnection ) throws DatabaseAccessException;

    public void deleteTestcase(
                                List<Object> objectsToDelete ) throws DatabaseAccessException;

    public void addRunMetainfo( int runId, String metaKey, String metaValue,
                                boolean closeConnection ) throws DatabaseAccessException;

    public int startSuite( String packageName, String suiteName, long timestamp, int runId,
                           boolean closeConnection ) throws DatabaseAccessException;

    public void endSuite( long timestamp, int suiteId,
                          boolean closeConnection ) throws DatabaseAccessException;

    /**
     * Update the static information about an existing suite
     *
     * @param suiteId
     * @param suiteName
     * @param userNote
     * @param closeConnection
     * @throws DatabaseAccessException
     */
    public void updateSuite( int suiteId, String suiteName, String userNote,
                             boolean closeConnection ) throws DatabaseAccessException;

    public void addScenarioMetainfo( int testcaseId, String metaKey, String metaValue,
                                     boolean closeConnection ) throws DatabaseAccessException;

    public void addTestcaseMetainfo( int testcaseId, String metaKey, String metaValue,
                                     boolean closeConnection ) throws DatabaseAccessException;

    public void clearScenarioMetainfo(
                                       int scenarioId,
                                       boolean closeConnection ) throws DatabaseAccessException;

    public int startTestCase( String suiteName, String scenarioName, String scenarioDescription,
                              String testCaseName, long timestamp, int suiteId,
                              boolean closeConnection ) throws DatabaseAccessException;

    public void endTestCase( int testcaseResult, long timestamp, int testcaseId,
                             boolean closeConnection ) throws DatabaseAccessException;

    public void updateTestcase(
                                String suiteFullName, String scenarioName, String scenarioDescription,
                                String testcaseName, String userNote, int testcaseResult,
                                int testcaseId, long timestamp,
                                boolean closeConnection ) throws DatabaseAccessException;

    public int startLoadQueue( String name, int sequence, String hostsList, String threadingPattern,
                               int numberThreads, String machine, long timestamp, int testcaseId,
                               boolean closeConnection ) throws DatabaseAccessException;

    public void endLoadQueue(
                              int result,
                              long timestamp,
                              int loadQueueId,
                              boolean closeConnection ) throws DatabaseAccessException;

    public boolean insertMessage( String message, int level, boolean escapeHtml, String machineName,
                                  String threadName, long timestamp, int testCaseId,
                                  boolean closeConnection ) throws DatabaseAccessException;

    public boolean insertRunMessage(
                                     String message,
                                     int level,
                                     boolean escapeHtml,
                                     String machineName,
                                     String threadName,
                                     long timestamp,
                                     int runId,
                                     boolean closeConnection ) throws DatabaseAccessException;

    public boolean insertSuiteMessage(
                                       String message,
                                       int level,
                                       boolean escapeHtml,
                                       String machineName,
                                       String threadName,
                                       long timestamp,
                                       int suiteId,
                                       boolean closeConnection ) throws DatabaseAccessException;

    public boolean insertCheckpoint( String name, long startTimestamp, long responseTime,
                                     long transferSize, String transferUnit, int result, int loadQueueId,
                                     boolean closeConnection ) throws DatabaseAccessException;

    public CheckpointInfo startCheckpoint( String name, String threadName, long startTimestamp,
                                           String transferUnit, int loadQueueId,
                                           boolean closeConnection ) throws DatabaseAccessException;

    public void endCheckpoint( CheckpointInfo runningCheckpointInfo, long endTimestamp, long transferSize,
                               int result, boolean closeConnection ) throws DatabaseAccessException;

    public void insertCheckpointSummary( String name,

                                         int numRunning, int numPassed, int numFailed,

                                         int minResponseTime, double avgResponseTime, int maxResponseTime,

                                         double minTransferRate, double avgTransferRate, double maxTransferRate,
                                         String transferRateUnit, int loadQueueId,
                                         boolean closeConnection ) throws DatabaseAccessException;

    public void insertSystemStatistics( int testCaseId, String machine, String statisticIds,
                                        String statisticValues, long timestamp,
                                        boolean closeConnection ) throws DatabaseAccessException;

    public void insertUserActivityStatistics( int testCaseId, String machine, String statisticIds,
                                              String statisticValues, long timestamp,
                                              boolean closeConnection ) throws DatabaseAccessException;

    public int populateSystemStatisticDefinition( String name, String parentName, String internalName,
                                                  String unit, String params ) throws DatabaseAccessException;

    public int populateCheckpointSummary( int loadQueueId, String name, String transferRateUnit,
                                          boolean closeConnection ) throws DatabaseAccessException;

    public void updateMachineInfo( String machineName, String machineInfo,
                                   boolean closeConnection ) throws DatabaseAccessException;

    public boolean isRunPresent(
                                 int runId ) throws DatabaseAccessException;

    public boolean isSuitePresent(
                                   int suiteId ) throws DatabaseAccessException;

    public boolean isTestcasePresent(
                                      int testcaseId ) throws DatabaseAccessException;

    /**
     * Expected to be called only in batch mode.
     * Flush any pending events
     *
     * @throws DatabaseAccessException
     */
    public void flushCache() throws DatabaseAccessException;

    /**
     * Expected to be called only in batch mode.
     * Flush any pending events in case the
     * cache is full or it is too old
     *
     * @throws DatabaseAccessException
     */
    public void flushCacheIfNeeded() throws DatabaseAccessException;

    public void runDbSanityCheck() throws DatabaseAccessException;

}
