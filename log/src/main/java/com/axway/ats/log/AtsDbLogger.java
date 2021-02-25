/*
 * Copyright 2017-2021 Axway Software
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

import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.threads.ThreadsPerCaller;
import com.axway.ats.log.appenders.ActiveDbAppender;
import com.axway.ats.log.appenders.PassiveDbAppender;
import com.axway.ats.log.autodb.TestCaseState;
import com.axway.ats.log.autodb.entities.TestcaseMetainfo;
import com.axway.ats.log.autodb.events.AddRunMetainfoEvent;
import com.axway.ats.log.autodb.events.AddScenarioMetainfoEvent;
import com.axway.ats.log.autodb.events.AddTestcaseMetainfoEvent;
import com.axway.ats.log.autodb.events.CleanupLoadQueueStateEvent;
import com.axway.ats.log.autodb.events.ClearScenarioMetainfoEvent;
import com.axway.ats.log.autodb.events.DeleteTestCaseEvent;
import com.axway.ats.log.autodb.events.EndAfterClassEvent;
import com.axway.ats.log.autodb.events.EndAfterMethodEvent;
import com.axway.ats.log.autodb.events.EndAfterSuiteEvent;
import com.axway.ats.log.autodb.events.EndCheckpointEvent;
import com.axway.ats.log.autodb.events.EndLoadQueueEvent;
import com.axway.ats.log.autodb.events.EndRunEvent;
import com.axway.ats.log.autodb.events.EndSuiteEvent;
import com.axway.ats.log.autodb.events.EndTestCaseEvent;
import com.axway.ats.log.autodb.events.GetCurrentTestCaseEvent;
import com.axway.ats.log.autodb.events.InsertCheckpointEvent;
import com.axway.ats.log.autodb.events.InsertMessageEvent;
import com.axway.ats.log.autodb.events.InsertSystemStatisticEvent;
import com.axway.ats.log.autodb.events.InsertUserActivityStatisticEvent;
import com.axway.ats.log.autodb.events.JoinTestCaseEvent;
import com.axway.ats.log.autodb.events.LeaveTestCaseEvent;
import com.axway.ats.log.autodb.events.RegisterThreadWithLoadQueueEvent;
import com.axway.ats.log.autodb.events.RememberLoadQueueStateEvent;
import com.axway.ats.log.autodb.events.StartAfterClassEvent;
import com.axway.ats.log.autodb.events.StartAfterMethodEvent;
import com.axway.ats.log.autodb.events.StartAfterSuiteEvent;
import com.axway.ats.log.autodb.events.StartCheckpointEvent;
import com.axway.ats.log.autodb.events.StartRunEvent;
import com.axway.ats.log.autodb.events.StartSuiteEvent;
import com.axway.ats.log.autodb.events.StartTestCaseEvent;
import com.axway.ats.log.autodb.events.UpdateRunEvent;
import com.axway.ats.log.autodb.events.UpdateSuiteEvent;
import com.axway.ats.log.autodb.events.UpdateTestcaseEvent;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.log.autodb.model.IDbReadAccess;
import com.axway.ats.log.model.CheckpointResult;
import com.axway.ats.log.model.LoadQueueResult;
import com.axway.ats.log.model.TestCaseResult;

/**
 * Utility class for working with the ATS logging system
 * 
 */
@PublicAtsApi
public class AtsDbLogger {

    private final static String ATS_DB_LOGGER_CLASS_NAME = AtsDbLogger.class.getName();

    /**
     * Flag that is used to log WARN for not attached ATS DB logger only once
     */
    private static boolean      isWarningMessageLogged   = false;

    protected Logger            logger;

    private AtsDbLogger( Logger logger, boolean skipAppenderCheck ) {

        this.logger = logger;
        // check if the ActiveDbAppender is specified in log4j2.xml
        if (!skipAppenderCheck) {
            if (!ActiveDbAppender.isAttached) {
                if (!isWarningMessageLogged) {
                    this.logger.warn(
                                     "ATS Database appender is not attached in root logger element in log4j2.xml file. "
                                     + "No test data will be sent to ATS Log database and some methods from '"
                                     + AtsDbLogger.class.getName() + "' class will not work as expected");
                    isWarningMessageLogged = true;
                }
            }
        }
    }

    @PublicAtsApi
    public static synchronized AtsDbLogger getLogger( String name ) {

        return new AtsDbLogger(LogManager.getLogger(name), false);
    }

    @PublicAtsApi
    public static synchronized AtsDbLogger getLogger( Logger logger ) {

        return new AtsDbLogger(logger, false);
    }

    /**
     * This method is intended for internal (by ATS devs) usage only.
     * @param name the name of the logger
     * @param skipAppenderCheck enable/disable check for availability of db appender
     * 
     *
     * */
    public static synchronized AtsDbLogger getLogger(
                                                      String name, boolean skipAppenderCheck ) {

        return new AtsDbLogger(LogManager.getLogger(name), skipAppenderCheck);
    }

    /**
     * This method is intended for internal (by ATS devs) usage only.
     * @param logger the Apache log4j2 logger
     * @param skipAppenderCheck enable/disable check for availability of db appender
     */
    public static synchronized AtsDbLogger getLogger(
                                                      Logger logger, boolean skipAppenderCheck ) {

        return new AtsDbLogger(logger, skipAppenderCheck);
    }

    public Logger getInternalLogger() {

        return this.logger;
    }

    /**
     * Insert a debug message
     *
     * @param message the message
     * @param t exception
     */
    public void debug(
                       Object message,
                       Throwable t ) {

        logger.debug(message, t);
    }

    /**
     * Insert a debug message
     *
     * @param message the message
     */
    public void debug(
                       Object message ) {

        logger.debug(message);
    }

    /**
     * Insert an error message
     *
     * @param message the message
     * @param t exception
     */
    public void error(
                       Object message,
                       Throwable t ) {

        logger.error(message, t);
    }

    /**
     * Insert an error message
     *
     * @param message the message
     */
    public void error(
                       Object message ) {

        logger.error(message);
    }

    /**
     * Insert a fatal message
     *
     * @param message the message
     * @param t exception
     */
    public void fatal(
                       Object message,
                       Throwable t ) {

        logger.fatal(message, t);
    }

    /**
     * Insert a fatal message
     *
     * @param message the message
     */
    public void fatal(
                       Object message ) {

        logger.fatal(message);
    }

    /**
     * Insert an info message
     *
     * @param message the message
     * @param t exception
     */
    public void info(
                      Object message,
                      Throwable t ) {

        logger.info(message, t);
    }

    public void info(
                      Object message,
                      boolean sendRunMessage ) {

        if (sendRunMessage) {
            sendEvent(new InsertMessageEvent(ATS_DB_LOGGER_CLASS_NAME,
                                             logger,
                                             Level.INFO,
                                             getNonNullToString(message),
                                             null,
                                             false,
                                             sendRunMessage));
        } else {
            logger.info(message);
        }

    }

    /**
     * Insert an info message
     *
     * @param message the message
     */
    public void info(
                      Object message ) {

        logger.info(message);
    }

    /**
     * Insert a trace message
     *
     * @param message the message
     * @param t exception
     */
    public void trace(
                       Object message,
                       Throwable t ) {

        logger.trace(message, t);
    }

    /**
     * Insert a trace message
     *
     * @param message the message
     */
    public void trace(
                       Object message ) {

        logger.trace(message);
    }

    /**
     * Insert a warning message
     *
     * @param message the message
     * @param t exception
     */
    public void warn(
                      Object message,
                      Throwable t ) {

        logger.warn(message, t);
    }

    /**
     * Insert a warning message
     *
     * @param message the message
     */
    public void warn(
                      Object message ) {

        logger.warn(message);
    }

    /**
     * Start a new run
     *
     * @param runName name of the run
     * @param osName name of the OS
     * @param productName name of the product
     * @param versionName version of the product
     * @param buildName build of the product
     * @param hostName name/IP of the machine , from which the run was started
     */
    public void startRun(
                          String runName,
                          String osName,
                          String productName,
                          String versionName,
                          String buildName,
                          String hostName ) {

        sendEvent(new StartRunEvent(ATS_DB_LOGGER_CLASS_NAME,
                                    logger,
                                    runName,
                                    osName,
                                    productName,
                                    versionName,
                                    buildName,
                                    hostName));
    }

    /**
     * End a run
     */
    public void endRun() {

        sendEvent(new EndRunEvent(ATS_DB_LOGGER_CLASS_NAME, logger));
    }

    /**
     * Add some meta info about a run.
     * Must be called while there is an existing run
     * 
     * @param metaKey key
     * @param metaValue value
     */
    public void addRunMetainfo(
                                String metaKey,
                                String metaValue ) {

        // TODO Check on lower level that there is no existing metaKey with same value. Otherwise UPDATE should be performed
        sendEvent(new AddRunMetainfoEvent(ATS_DB_LOGGER_CLASS_NAME, logger, metaKey, metaValue));
    }

    /**
     * Update the static information about the current run.
     * <br><b>NOTE</b>: This method can be called at anytime after a run is started.
     *
     * <br><br><b>NOTE</b>: Pass 'null' value to any parameter which must not be modified.
     *
     * @param runName name of the run
     * @param osName name of the OS
     * @param productName name of the product
     * @param versionName version of the product
     * @param buildName build of the product
     * @param userNote some user note about this run
     * @param hostName name/IP of the machine , from which the run was started
     */
    public void updateRun(
                           String runName,
                           String osName,
                           String productName,
                           String versionName,
                           String buildName,
                           String userNote,
                           String hostName ) {

        sendEvent(new UpdateRunEvent(ATS_DB_LOGGER_CLASS_NAME,
                                     logger,
                                     runName,
                                     osName,
                                     productName,
                                     versionName,
                                     buildName,
                                     userNote,
                                     hostName));
    }

    /**
     * Start a new suite
     *
     * @param packageName name of the package
     * @param suiteName name of the suite
     */
    public void startSuite(
                            String packageName,
                            String suiteName ) {

        sendEvent(new StartSuiteEvent(ATS_DB_LOGGER_CLASS_NAME, logger, suiteName, packageName));
    }

    /**
     * Update the static information about the current suite.
     * <br><b>NOTE</b>: This method can be called at any time after a suite is started.
     *
     * <br><br><b>NOTE</b>: Pass 'null' value to any parameter which must not be modified.
     *
     * @param newSuiteName the new value to set for the name of the current suite
     * @param newUserNote  the new value to set for the user note of the current suite
     */
    public void updateSuite( String newSuiteName,
                             String newUserNote ) {

        sendEvent(new UpdateSuiteEvent(ATS_DB_LOGGER_CLASS_NAME, logger, newSuiteName, newUserNote));
    }

    /**
     * End the current suite
     */
    public void endSuite() {

        sendEvent(new EndSuiteEvent(ATS_DB_LOGGER_CLASS_NAME, logger));
    }

    /**
     * Clear all meta info about a scenario.
     */
    public void clearScenarioMetainfo() {

        sendEvent(new ClearScenarioMetainfoEvent(ATS_DB_LOGGER_CLASS_NAME, logger));
    }

    /**
     * Add some meta info about a scenario.
     * Must be called while there is an existing scenario
     * 
     * @param metaKey key
     * @param metaValue value
     */
    public void addScenarioMetainfo(
                                     String metaKey,
                                     String metaValue ) {

        // TODO Check on lower level that there is no existing metaKey with same value. Otherwise UPDATE should be performed
        sendEvent(new AddScenarioMetainfoEvent(ATS_DB_LOGGER_CLASS_NAME, logger, metaKey, metaValue));
    }

    /**
     * Start a new test case
     *
     * @param name the name of the test case
     * @param inputArguments the input arguments of the test case
     */
    public void startTestcase(
                               String suiteFullName,
                               String suiteSimpleName,
                               String name,
                               String inputArguments,
                               String testDescription ) {

        sendEvent(new StartTestCaseEvent(ATS_DB_LOGGER_CLASS_NAME,
                                         logger,
                                         suiteFullName,
                                         suiteSimpleName,
                                         name,
                                         inputArguments,
                                         testDescription));
    }

    /**
     * Update a testcase. If value is null for any of the parameters, no update will be performed for that parameter.
     * 
     * @param suiteFullName full name ( package.java_class_name ) of the suite.
     * <div> Note that the fullName must contains at least two tokens, divided by dot character ( e.g. com.foobar ) </div>
     * @param testcaseId the testcase ID
     * @param suiteSimpleName the name of the Java class, containing the tests
     * @param scenarioName the scenario name
     * @param inputArguments the test method parameters
     * @param scenarioDescription the scenario description
     * @param testcaseResult the result of the testcase (PASSED,FAILED,SKIPPED)
     * 
     * */
    public void updateTestcase( int testcaseId,
                                String suiteFullName,
                                String suiteSimpleName,
                                String scenarioName,
                                String inputArguments,
                                String scenarioDescription,
                                int testcaseResult ) {

        sendEvent(new UpdateTestcaseEvent(ATS_DB_LOGGER_CLASS_NAME,
                                          logger,
                                          testcaseId,
                                          suiteFullName,
                                          suiteSimpleName,
                                          scenarioName,
                                          inputArguments,
                                          scenarioDescription,
                                          testcaseResult,
                                          System.currentTimeMillis()));

    }

    /**
     * End the current test case
     *
     * @param testCaseResult the result of the test case execution
     */
    public void endTestcase(
                             TestCaseResult testCaseResult ) {

        sendEvent(new EndTestCaseEvent(ATS_DB_LOGGER_CLASS_NAME, logger, testCaseResult));
    }

    /**
     * Delete the testcase with the give ID
     * 
     * @param testCaseId
     */
    public void deleteTestcase(
                                int testCaseId ) {

        sendEvent(new DeleteTestCaseEvent(ATS_DB_LOGGER_CLASS_NAME, logger, testCaseId));
    }

    /**
     * Add some meta info about a testcase.
     * Must be called inside @Test/@BeforeMethod/@AfterMethod
     * 
     * @param metaKey key
     * @param metaValue value
     */
    @PublicAtsApi
    public void addTestcaseMetainfo(
                                     String metaKey,
                                     String metaValue ) {

        // TODO Check on lower level that there is no existing metaKey with same value. Otherwise UPDATE should be performed
        sendEvent(new AddTestcaseMetainfoEvent(ATS_DB_LOGGER_CLASS_NAME, logger, metaKey, metaValue));
    }

    /**
     * Add some meta info about a testcase.
     * Must be called while there is an existing testcase
     * 
     * @param metaKey key
     * @param metaValue value
     */
    public void addTestcaseMetainfo(
                                     int testcaseId,
                                     String metaKey,
                                     String metaValue ) {

        // TODO Check on lower level that there is no existing metaKey with same value. Otherwise UPDATE should be performed
        sendEvent(new AddTestcaseMetainfoEvent(ATS_DB_LOGGER_CLASS_NAME, logger, testcaseId, metaKey, metaValue));
    }

    /**
     * Register a thread with the given load queue
     *
     * @param loadQueueName name of the load queue to register this thread with
     */
    public void registerThreadWithLoadQueue(
                                             String loadQueueName ) {

        sendEvent(new RegisterThreadWithLoadQueueEvent(ATS_DB_LOGGER_CLASS_NAME,
                                                       logger,
                                                       Thread.currentThread().getName(),
                                                       loadQueueName));
    }

    /**
     * Remember the load queue state
     *
     * @param name name of the load queue
     * @param loadQueueId database load queue id
     * @param threadingPattern description of the threading pattern
     * @param numberThreads the number of threads this load queue starts
     */
    public void rememberLoadQueueState(
                                        String name,
                                        int loadQueueId,
                                        String threadingPattern,
                                        int numberThreads ) {

        sendEvent(new RememberLoadQueueStateEvent(ATS_DB_LOGGER_CLASS_NAME,
                                                  logger,
                                                  name,
                                                  loadQueueId,
                                                  threadingPattern,
                                                  numberThreads));
    }

    /**
     * Cleanup the load queue state
     *
     * @param name name of the load queue
     */
    public void cleanupLoadQueueState(
                                       String name ) {

        sendEvent(new CleanupLoadQueueStateEvent(ATS_DB_LOGGER_CLASS_NAME, logger, name));
    }

    /**
     * End the load queue with the given name
     *
     * @param name name of the load queue
     * @param result the result of the load queue execution
     */
    public void endLoadQueue(
                              String name,
                              LoadQueueResult result ) {

        sendEvent(new EndLoadQueueEvent(ATS_DB_LOGGER_CLASS_NAME, logger, name, result));
    }

    /**
     * Start a checkpoint
     *
     * @param name the name of the checkpoint
     */
    public void startCheckpoint(
                                 String name ) {

        sendEvent(new StartCheckpointEvent(ATS_DB_LOGGER_CLASS_NAME,
                                           logger,
                                           name,
                                           "",
                                           Thread.currentThread().getName()));
    }

    /**
     * Start a checkpoint which is doing some data transfer
     *
     * @param name the name of the checkpoint
     * @param transferUnit the data transfer unit
     */
    public void startCheckpoint(
                                 String name,
                                 String transferUnit ) {

        sendEvent(new StartCheckpointEvent(ATS_DB_LOGGER_CLASS_NAME,
                                           logger,
                                           name,
                                           transferUnit,
                                           Thread.currentThread().getName()));
    }

    /**
     * Start a checkpoint which is doing some data transfer
     *
     * @param name the name of the checkpoint
     * @param transferUnit the data transfer unit
     * @param startTimestamp the event time
     */
    public void startCheckpoint(
                                 String name,
                                 String transferUnit,
                                 long startTimestamp ) {

        sendEvent(new StartCheckpointEvent(ATS_DB_LOGGER_CLASS_NAME,
                                           logger,
                                           name,
                                           transferUnit,
                                           Thread.currentThread().getName(),
                                           startTimestamp));
    }

    /**
     * End a checkpoint and calculate the execution time and transfer rate
     *
     * @param name name of the checkpoint
     * @param transferSize the size of the transfer
     * @param result the result of the checkpoint execution
     */
    public void endCheckpoint(
                               String name,
                               long transferSize,
                               CheckpointResult result ) {

        sendEvent(new EndCheckpointEvent(ATS_DB_LOGGER_CLASS_NAME,
                                         logger,
                                         name,
                                         Thread.currentThread().getName(),
                                         transferSize,
                                         result));
    }

    /**
     * End a checkpoint and calculate the execution time and transfer rate
     *
     * @param name name of the checkpoint
     * @param transferSize the size of the transfer
     * @param result the result of the checkpoint execution
     * @param endTimestamp the event time
     */
    public void endCheckpoint(
                               String name,
                               long transferSize,
                               CheckpointResult result,
                               long endTimestamp ) {

        sendEvent(new EndCheckpointEvent(ATS_DB_LOGGER_CLASS_NAME,
                                         logger,
                                         name,
                                         Thread.currentThread().getName(),
                                         transferSize,
                                         result,
                                         endTimestamp));
    }

    /**
     * Directly insert a checkpoint. The user provides all the needed info.
     *
     * @param name the name of the checkpoint
     */
    public void insertCheckpoint(
                                  String name,
                                  long responseTime,
                                  CheckpointResult result ) {

        sendEvent(new InsertCheckpointEvent(ATS_DB_LOGGER_CLASS_NAME,
                                            logger,
                                            name,
                                            -1,
                                            responseTime,
                                            0,
                                            "",
                                            Thread.currentThread().getName(),
                                            result));
    }

    public void insertCheckpoint(
                                  String name,
                                  long startTimestamp,
                                  long responseTime,
                                  long transferSize,
                                  String transferUnit,
                                  CheckpointResult result ) {

        sendEvent(new InsertCheckpointEvent(ATS_DB_LOGGER_CLASS_NAME,
                                            logger,
                                            name,
                                            startTimestamp,
                                            responseTime,
                                            transferSize,
                                            transferUnit,
                                            Thread.currentThread().getName(),
                                            result));
    }

    /**
     * Insert system statistics identified by their DB IDs
     *
     * @param monitoredMachine the monitored machine
     * @param statisticIds the statistics' DB IDs
     * @param statisticValues the statistics' values
     * @param timestamp the timestamp
     */
    public void insertSystemStatistcs(
                                       String monitoredMachine,
                                       String statisticIds,
                                       String statisticValues,
                                       long timestamp ) {

        sendEvent(new InsertSystemStatisticEvent(ATS_DB_LOGGER_CLASS_NAME,
                                                 logger,
                                                 monitoredMachine,
                                                 statisticIds,
                                                 statisticValues,
                                                 timestamp));
    }

    /**
     * Insert user activity statistics identified by their DB IDs
     *
     * @param monitoredMachine the monitored machine
     * @param statisticIds the statistics' DB IDs
     * @param statisticValues the statistics' values
     * @param timestamp the timestamp
     */
    public void insertUserActivityStatistcs(
                                             String monitoredMachine,
                                             String statisticIds,
                                             String statisticValues,
                                             long timestamp ) {

        sendEvent(new InsertUserActivityStatisticEvent(ATS_DB_LOGGER_CLASS_NAME,
                                                       logger,
                                                       monitoredMachine,
                                                       statisticIds,
                                                       statisticValues,
                                                       timestamp));
    }

    /**
     * Join to an existing test case
     *
     * @param testCaseState the state of the test case to join to
     */
    public void joinTestCase(
                              TestCaseState testCaseState ) {

        sendEvent(new JoinTestCaseEvent(ATS_DB_LOGGER_CLASS_NAME, logger, testCaseState));
    }

    /**
     * Leave the test case to which we have joined
     */
    public void leaveTestCase() {

        sendEvent(new LeaveTestCaseEvent(ATS_DB_LOGGER_CLASS_NAME, logger));
    }

    /**
     * After this call, all messages are treated as TESTCASE messages
     * */
    public void startAfterMethod() {

        sendEvent(new StartAfterMethodEvent(ATS_DB_LOGGER_CLASS_NAME, logger));
    }

    /**
     * Clears effect of the StartAfterMethod invocation
     * */
    public void endAfterMethod() {

        sendEvent(new EndAfterMethodEvent(ATS_DB_LOGGER_CLASS_NAME, logger));
    }

    /**
     * After this call, all messages are treated as SUITE messages
     * */
    public void startAfterClass() {

        sendEvent(new StartAfterClassEvent(ATS_DB_LOGGER_CLASS_NAME, logger));
    }

    /**
     * Clears effect of the StartAfterClass invocation
     * */
    public void endAfterClass() {

        sendEvent(new EndAfterClassEvent(ATS_DB_LOGGER_CLASS_NAME, logger));
    }

    /**
     * After this call, all messages are treated as RUN messages
     * */
    public void startAfterSuite() {

        sendEvent(new StartAfterSuiteEvent(ATS_DB_LOGGER_CLASS_NAME, logger));
    }

    /**
     * Clears effect of the StartAfterSuite invocation
     * */
    public void endAfterSuite() {

        sendEvent(new EndAfterSuiteEvent(ATS_DB_LOGGER_CLASS_NAME, logger));
    }

    /**
     * This event can not go through the regular way of sending log4j2 events in the case with Passive DB appenders. 
     * The reason is that we have to evaluate the result after the work of each passive appender and stop
     * calling these appenders when the first one(the only one serving this caller) has processed the event. 
     */
    public TestCaseState getCurrentTestCaseState() {

        GetCurrentTestCaseEvent event = new GetCurrentTestCaseEvent(ATS_DB_LOGGER_CLASS_NAME, logger);

        PassiveDbAppender passiveDbAppender = PassiveDbAppender.getCurrentInstance(ThreadsPerCaller.getCaller());

        if (passiveDbAppender != null) {

            // assume we are on the ATS Agent side
            GetCurrentTestCaseEvent resultEvent = passiveDbAppender.getCurrentTestCaseState(event);
            if (resultEvent != null) {
                // we found the right Passive appender
                return resultEvent.getTestCaseState();
            }
        } else {

            ActiveDbAppender activeAppender = ActiveDbAppender.getCurrentInstance();

            if (activeAppender != null) {
                return activeAppender.getCurrentTestCaseState(event).getTestCaseState();
            }
        }

        return null;
    }

    public boolean isDebugEnabled() {

        return logger.isDebugEnabled();
    }

    /*
     * Reading methods
     * */

    /**
     * Retrieve {@link TestcaseMetainfo} for current testcase. Works on the Test Executor only.
     * Note that this method always reads from DB in order to make sure that such information is already in database. 
     * For cases where metainfo should be read often it is recommended that values are cached.
     * @return list of {@link TestcaseMetainfo}
     * @throws DatabaseAccessException in case of a DB error
     */
    @PublicAtsApi
    public List<TestcaseMetainfo> getTestcaseMetainfo() throws DatabaseAccessException {

        int testcaseId = ActiveDbAppender.getCurrentInstance().getTestCaseId();
        return getTestcaseMetainfo(testcaseId);
    }

    /**
     * Retrieve {@link TestcaseMetainfo} for a particular testcase. Works on the Test Executor only.
     * @param testcaseId - the testcase ID. For current testcase ID, you may use 
     * <pre>{@code ActiveDbAppender.getCurrentInstance().getTestCaseId()}</pre>
     * Note that this method always reads from DB in order to make sure that such information is already in database. 
     * For cases where metainfo should be read often it is recommended that values are cached.
     * @return list of {@link TestcaseMetainfo}
     * @throws DatabaseAccessException in case of a DB error
     */
    @PublicAtsApi
    public List<TestcaseMetainfo> getTestcaseMetainfo( int testcaseId ) throws DatabaseAccessException {

        IDbReadAccess dbReadAccess = ActiveDbAppender.getCurrentInstance().obtainDbReadAccessObject();
        List<TestcaseMetainfo> metainfo = dbReadAccess.getTestcaseMetainfo(testcaseId);
        return metainfo;
    }

    /**
     * Send an event to the logging system
     *
     * @param event the event to send
     */
    private void sendEvent(
                            LogEvent event ) {

        // check if this level is allowed for the repository at all
        if (Log4j2Utils.getRootLogger().getLevel().isMoreSpecificThan(event.getLevel())) {
            return;
        }

        // check if the event level is allowed for this logger
        if (event.getLevel().isMoreSpecificThan(logger.getLevel())) {
            Log4j2Utils.getLoggerConfig(logger.getName()).log(event);
        }
    }

    private String getNonNullToString( Object obj ) {

        if (obj == null) {
            return "";
        } else {
            return obj.toString(); // possibly this could also return null but seems not an issue
        }

    }
}
