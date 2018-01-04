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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import com.axway.ats.core.dbaccess.ConnectionPool;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.DbUtils;
import com.axway.ats.core.dbaccess.mssql.DbConnSQLServer;
import com.axway.ats.core.dbaccess.postgresql.DbConnPostgreSQL;
import com.axway.ats.core.log.AtsConsoleLogger;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.appenders.ActiveDbAppender;
import com.axway.ats.log.autodb.entities.Run;
import com.axway.ats.log.autodb.entities.Testcase;
import com.axway.ats.log.autodb.events.AddRunMetainfoEvent;
import com.axway.ats.log.autodb.events.AddScenarioMetainfoEvent;
import com.axway.ats.log.autodb.events.CleanupLoadQueueStateEvent;
import com.axway.ats.log.autodb.events.EndCheckpointEvent;
import com.axway.ats.log.autodb.events.EndLoadQueueEvent;
import com.axway.ats.log.autodb.events.EndTestCaseEvent;
import com.axway.ats.log.autodb.events.InsertCheckpointEvent;
import com.axway.ats.log.autodb.events.InsertMessageEvent;
import com.axway.ats.log.autodb.events.InsertSystemStatisticEvent;
import com.axway.ats.log.autodb.events.InsertUserActivityStatisticEvent;
import com.axway.ats.log.autodb.events.JoinTestCaseEvent;
import com.axway.ats.log.autodb.events.RegisterThreadWithLoadQueueEvent;
import com.axway.ats.log.autodb.events.RememberLoadQueueStateEvent;
import com.axway.ats.log.autodb.events.StartCheckpointEvent;
import com.axway.ats.log.autodb.events.StartRunEvent;
import com.axway.ats.log.autodb.events.StartSuiteEvent;
import com.axway.ats.log.autodb.events.StartTestCaseEvent;
import com.axway.ats.log.autodb.events.UpdateRunEvent;
import com.axway.ats.log.autodb.events.UpdateSuiteEvent;
import com.axway.ats.log.autodb.events.UpdateTestcaseEvent;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.log.autodb.exceptions.IncorrectProcessorStateException;
import com.axway.ats.log.autodb.exceptions.LoadQueueAlreadyStartedException;
import com.axway.ats.log.autodb.exceptions.LoggingException;
import com.axway.ats.log.autodb.exceptions.NoSuchLoadQueueException;
import com.axway.ats.log.autodb.exceptions.ThreadAlreadyRegisteredWithLoadQueueException;
import com.axway.ats.log.autodb.exceptions.ThreadNotRegisteredWithLoadQueue;
import com.axway.ats.log.autodb.model.AbstractLoggingEvent;
import com.axway.ats.log.autodb.model.CacheableEvent;
import com.axway.ats.log.autodb.model.EventRequestProcessor;
import com.axway.ats.log.autodb.model.EventRequestProcessorListener;
import com.axway.ats.log.autodb.model.IDbWriteAccess;
import com.axway.ats.log.model.SystemLogLevel;

public class DbEventRequestProcessor implements EventRequestProcessor {

    private AtsConsoleLogger              atsConsoleLogger           = new AtsConsoleLogger(DbEventRequestProcessor.class);

    /**
     * The configuration for this appender
     */
    private DbAppenderConfiguration       appenderConfig;

    /**
     * The connection information
     */
    private DbConnection                  dbConnection;

    /**
     * The DB access instance
     */
    private IDbWriteAccess                dbAccess;

    /**
     * The layout according to which to format events
     */
    private Layout                        layout;

    /**
     * The current state of the event processor
     */
    private EventProcessorState           eventProcessorState;

    /**
     * Listener to be notified when events are processed
     */
    private EventRequestProcessorListener listener;

    /**
     * The name of the machine running this event processor
     */
    private String                        machineName;

    /**
     * We cache the suites opened by this run, so do not have to query the DB
     *
     * key = <suite name>; value = <suite ID>
     */
    private static Map<String, Integer>   suiteIdsCache              = new HashMap<String, Integer>();

    private boolean                       isBatchMode;

    /*
     * When rerunning a testcase, we have to delete the faulty one.
     * The main thread passes here the id of the test to be deleted.
     */
    private int                           testcaseToDelete           = -1;
    /*
     * This is a list with all deleted tests.
     * We use it in order to skip going to the DB as we know the operation will fail.
     */
    private List<Integer>                 deletedTestcases           = new ArrayList<Integer>();

    /*
     * If the current state of the DbEventProcessor could not process UpdateSuiteEvent,
     * preserve this event and fire it right after StartSuiteEvent is received
     * */
    private UpdateSuiteEvent              pendingUpdateSuiteEvent    = null;

    /*
     * The UpdateRunEvent, fired from the user.
     * */
    private UpdateRunEvent                userProvidedUpdateRunEvent = null;

    /*
     * The actual UpdateRunEvent that will be processed and executed
     * */
    private UpdateRunEvent                actualUpdateRunEvent       = null;

    /*
     * While these flag is true, all messages are logged as run messages
     * */
    private boolean                       afterSuiteMode             = false;

    /*
     * While these flag is true, all messages are logged as suite messages
     * */
    private boolean                       afterClassMode             = false;

    /*
     * While these flag is true, all messages, statistics and checkpoints
     * are logged as they have been logged from the testcase, that was ended most recently
     * */
    private boolean                       afterMethodMode            = false;

    /*
     * Keeps the ID of the last ended testcase, regardless of the testcase result (e.g. PASSED,FAILED,SKIPPED) 
     * */
    private int                           lastEndedTestcaseId        = -1;

    /*
     * Keeps the ID of the last ended suite
     * */
    private int                           lastEndedSuiteId           = -1;

    public DbEventRequestProcessor( DbAppenderConfiguration appenderConfig, Layout layout,
                                    boolean isBatchMode ) throws DatabaseAccessException {

        this(appenderConfig, layout, null, isBatchMode);
    }

    public DbEventRequestProcessor( DbAppenderConfiguration appenderConfig, Layout layout,
                                    EventRequestProcessorListener listener,
                                    boolean isBatchMode ) throws DatabaseAccessException {

        this.appenderConfig = appenderConfig;
        this.isBatchMode = isBatchMode;

        if (DbUtils.isMSSQLDatabaseAvailable(appenderConfig.getHost(),
                                             appenderConfig.getDatabase(),
                                             appenderConfig.getUser(),
                                             appenderConfig.getPassword())) {

            this.dbConnection = new DbConnSQLServer(appenderConfig.getHost(), appenderConfig.getDatabase(),
                                                    appenderConfig.getUser(), appenderConfig.getPassword());

            //create the db access layer
            this.dbAccess = new SQLServerDbWriteAccess((DbConnSQLServer) dbConnection, isBatchMode);

        } else if (DbUtils.isPostgreSQLDatabaseAvailable(appenderConfig.getHost(),
                                                         appenderConfig.getDatabase(),
                                                         appenderConfig.getUser(),
                                                         appenderConfig.getPassword())) {

            this.dbConnection = new DbConnPostgreSQL(appenderConfig.getHost(), appenderConfig.getDatabase(),
                                                     appenderConfig.getUser(), appenderConfig.getPassword());

            //create the db access layer
            this.dbAccess = new PGDbWriteAccess((DbConnPostgreSQL) dbConnection, isBatchMode);

        } else {
            String errMsg = "Neither MSSQL, nor PostgreSQL server at '" + appenderConfig.getHost()
                            + "' contains ATS log database with name '" + appenderConfig.getDatabase() + "'.";
            throw new DatabaseAccessException(errMsg);
        }

        this.eventProcessorState = new EventProcessorState();
        this.layout = layout;
        this.listener = listener;

        //get the hostname of the machine
        try {
            InetAddress addr = InetAddress.getLocalHost();
            this.machineName = addr.getHostName();
        } catch (UnknownHostException uhe) {
            this.machineName = "unknown host";
        }
    }

    /**
     * Set the layout for logging messages
     *
     * @param layout the layout
     */
    public void setLayout( Layout layout ) {

        this.layout = layout;
    }

    public int getRunId() {

        return eventProcessorState.getRunId();
    }

    public int getSuiteId() {

        return eventProcessorState.getSuiteId();
    }

    public String getRunName() {

        return eventProcessorState.getRunName();
    }

    public String getRunUserNote() {

        return eventProcessorState.getRunUserNote();
    }

    public int getTestCaseId() {

        return eventProcessorState.getTestCaseId();
    }

    public void processEventRequest( LogEventRequest eventRequest ) throws LoggingException {

        if (testcaseToDelete > 0) {
            /* Pause for a moment processing the current event.
             * This is (delete testcase)event is not coming from the FIFO queue, as we want to process it as
             * soon as possible, so any events related to this testcase are directly skipped.
             */
            deleteRequestedTestcase();

            // now resume the processing of the current event that came from the queue
        }

        if (isBatchMode && eventRequest == null) {
            // timeout waiting for next event - flush the current cache
            dbAccess.flushCache();
            return;
        }

        LoggingEvent event = eventRequest.getEvent();
        if (event instanceof AbstractLoggingEvent) {
            AbstractLoggingEvent dbAppenderEvent = (AbstractLoggingEvent) event;

            if (dbAppenderEvent instanceof UpdateSuiteEvent) {
                try {
                    dbAppenderEvent.checkIfCanBeProcessed(eventProcessorState);
                } catch (IncorrectProcessorStateException e) {
                    /* Suite not started yet, 
                     * so save the current event as pending 
                     * and fired it right after StartSuiteEvent is received
                    */
                    pendingUpdateSuiteEvent = (UpdateSuiteEvent) dbAppenderEvent;
                    return;
                }
            } else if (dbAppenderEvent instanceof UpdateRunEvent) {
                try {
                    /* Run not started yet.
                     * We will fire the event after StartRunEvent is received
                    */
                    userProvidedUpdateRunEvent = (UpdateRunEvent) dbAppenderEvent;
                    dbAppenderEvent.checkIfCanBeProcessed(eventProcessorState);
                } catch (IncorrectProcessorStateException e) {
                    /*
                     * If we get an exception, do no process the event any further.
                     * We will fire it after StartRunEvent is received.
                     * */
                    return;
                }
            } else {
                //first check if we can process the event at all
                dbAppenderEvent.checkIfCanBeProcessed(eventProcessorState);
            }

            if (isBatchMode && ! (event instanceof CacheableEvent)
                && eventProcessorState.getLifeCycleState() == LifeCycleState.TEST_CASE_STARTED) {
                // this event can not be cached - flush the current cache
                dbAccess.flushCache();
            }

            switch (dbAppenderEvent.getEventType()) {
                case START_RUN:
                    startRun((StartRunEvent) event, eventRequest.getTimestamp());
                    if (userProvidedUpdateRunEvent != null) {
                        constructUpdateRunEvent();
                        updateRun(actualUpdateRunEvent);
                    }
                    break;
                case END_RUN:
                    endRun(eventRequest.getTimestamp());
                    break;
                case UPDATE_RUN:
                    /*
                     * By using data from the latest UpdateRunEvent and the current run info,
                     * construct a pending UpdateRunEvent
                     * */
                    constructUpdateRunEvent();
                    updateRun(actualUpdateRunEvent);
                    break;
                case START_AFTER_SUITE:
                    afterSuiteMode = true;
                    break;
                case END_AFTER_SUITE:
                    afterSuiteMode = false;
                    break;
                case ADD_RUN_METAINFO:
                    addRunMetainfo((AddRunMetainfoEvent) event);
                    break;
                case START_SUITE:
                    startSuite((StartSuiteEvent) event, eventRequest.getTimestamp());
                    if (pendingUpdateSuiteEvent != null) {
                        updateSuite(pendingUpdateSuiteEvent);
                        pendingUpdateSuiteEvent = null;
                    }
                    break;
                case END_SUITE:
                    endSuite(eventRequest.getTimestamp());
                    break;
                case UPDATE_SUITE:
                    updateSuite((UpdateSuiteEvent) event);
                    break;
                case START_AFTER_CLASS:
                    afterClassMode = true;
                    break;
                case END_AFTER_CLASS:
                    afterClassMode = false;
                    break;
                case CLEAR_SCENARIO_METAINFO:
                    clearScenarioMetainfo();
                    break;
                case ADD_SCENARIO_METAINFO:
                    addScenarioMetainfo((AddScenarioMetainfoEvent) event);
                    break;
                case UPDATE_TEST_CASE:
                    updateTestcase((UpdateTestcaseEvent) event, eventRequest.getTimestamp());
                    break;
                case END_TEST_CASE:
                    endTestCase((EndTestCaseEvent) event, eventRequest.getTimestamp());
                    break;
                case START_TEST_CASE:
                    startTestCase((StartTestCaseEvent) event, eventRequest.getTimestamp());
                    break;
                case JOIN_TEST_CASE:
                    joinTestCase((JoinTestCaseEvent) event);
                    break;
                case LEAVE_TEST_CASE:
                    leaveTestCase();
                    break;
                case START_AFTER_METHOD:
                    afterMethodMode = true;
                    break;
                case END_AFTER_METHOD:
                    afterMethodMode = false;
                    break;
                case REMEMBER_LOADQUEUE_STATE:
                    rememberLoadQueueState((RememberLoadQueueStateEvent) event);
                    break;
                case CLEANUP_LOADQUEUE_STATE:
                    cleanupLoadQueueState((CleanupLoadQueueStateEvent) event);
                    break;
                case END_LOADQUEUE:
                    endLoadQueue((EndLoadQueueEvent) event, eventRequest.getTimestamp());
                    break;
                case REGISTER_THREAD_WITH_LOADQUEUE:
                    registerThreadWithLoadQueue((RegisterThreadWithLoadQueueEvent) event);
                    break;
                case START_CHECKPOINT:
                    startCheckpoint((StartCheckpointEvent) event);
                    break;
                case END_CHECKPOINT:
                    endCheckpoint((EndCheckpointEvent) event);
                    break;
                case INSERT_CHECKPOINT:
                    insertCheckpoint((InsertCheckpointEvent) event);
                    break;
                case INSERT_SYSTEM_STAT:
                    insertSystemStatistics((InsertSystemStatisticEvent) event);
                    break;
                case INSERT_USER_ACTIVITY_STAT:
                    insertUserActivityStatistics((InsertUserActivityStatisticEvent) event);
                    break;
                case INSERT_MESSAGE:
                    InsertMessageEvent insertMessageEvent = (InsertMessageEvent) event;
                    insertMessage(eventRequest, insertMessageEvent.isEscapeHtml(),
                                  insertMessageEvent.isRunMessage());
                    break;
                default:
                    throw new LoggingException("Unsupported logging event of type: "
                                               + dbAppenderEvent.getEventType());
            }
        } else {
            insertMessage(eventRequest, false, false);
        }
    }

    private void startRun( StartRunEvent startRunEvent, long timeStamp ) throws DatabaseAccessException {

        // this temporary map must be cleared prior to each run
        suiteIdsCache.clear();

        int previousRunId = eventProcessorState.getPreviousRunId();

        int newRunId;
        if (previousRunId == 0) {
            // run sanity check first
            dbAccess.runDbSanityCheck();

            // now create a new run for first time in this JVM execution
            newRunId = dbAccess.startRun(startRunEvent.getRunName(), startRunEvent.getOsName(),
                                         startRunEvent.getProductName(), startRunEvent.getVersionName(),
                                         startRunEvent.getBuildName(), timeStamp,
                                         startRunEvent.getHostName(), true);

            //output the run id in the console, so it can be used for results
            atsConsoleLogger.info(
                                  "Started a new RUN in Test Explorer's database with id: "
                                  + newRunId);
        } else {
            // we already had a run, now we will join to the previous run
            // we will update the name of the run only
            dbAccess.updateRun(previousRunId, startRunEvent.getRunName(), null, null, null, null, null, null,
                               true);
            newRunId = previousRunId;

            //output the run id in the console, so it can be used for results
            atsConsoleLogger.info("Joined an existing RUN in Test Explorer's database with id: "
                                  + newRunId);
        }

        eventProcessorState.setRunId(newRunId);
        eventProcessorState.setRunName(startRunEvent.getRunName());
        eventProcessorState.setRunUserNote(null);
        eventProcessorState.setLifeCycleState(LifeCycleState.RUN_STARTED);

        //notify the listener that the run started successfully
        if (listener != null) {
            listener.onRunStarted();
        }
    }

    private void endRun( long timeStamp ) throws DatabaseAccessException {

        int currentRunId = eventProcessorState.getRunId();

        dbAccess.endRun(timeStamp, currentRunId, true);

        //set the current appender state
        eventProcessorState.setPreviousRunId(currentRunId);
        eventProcessorState.setRunId(0);
        eventProcessorState.setLifeCycleState(LifeCycleState.INITIALIZED);

        if (listener != null) {
            listener.onRunFinished();
        }
    }

    private void updateRun( UpdateRunEvent updateRunEvent ) throws DatabaseAccessException {

        dbAccess.updateRun(eventProcessorState.getRunId(), updateRunEvent.getRunName(),
                           updateRunEvent.getOsName(), updateRunEvent.getProductName(),
                           updateRunEvent.getVersionName(), updateRunEvent.getBuildName(),
                           updateRunEvent.getUserNote(), updateRunEvent.getHostName(), true);
        if (updateRunEvent.getRunName() != null) {
            eventProcessorState.setRunName(updateRunEvent.getRunName());
        }
        if (updateRunEvent.getUserNote() != null) {
            eventProcessorState.setRunUserNote(updateRunEvent.getUserNote());
        }
    }

    private void addRunMetainfo( AddRunMetainfoEvent addRunMetainfoEvent ) throws DatabaseAccessException {

        dbAccess.addRunMetainfo(eventProcessorState.getRunId(), addRunMetainfoEvent.getMetaKey(),
                                addRunMetainfoEvent.getMetaValue(), true);
    }

    private void startSuite( StartSuiteEvent startSuiteEvent,
                             long timeStamp ) throws DatabaseAccessException {

        String suiteName = startSuiteEvent.getSuiteName();
        String packageName = startSuiteEvent.getPackage();

        int runId = eventProcessorState.getRunId();

        // If there is already a suite with same name in the same run don't open a new one, but
        // use the existing suite.
        if (suiteIdsCache.containsKey(eventProcessorState.getRunId() + suiteName)) {
            eventProcessorState.setSuiteId(suiteIdsCache.get(eventProcessorState.getRunId() + suiteName));
        } else {
            int suiteId = dbAccess.startSuite(packageName, suiteName, timeStamp, runId, true);

            //set the current suite id
            eventProcessorState.setSuiteId(suiteId);

            //put the id in the cache
            suiteIdsCache.put(runId + suiteName, suiteId);
        }

        //set the current appender state
        eventProcessorState.setLifeCycleState(LifeCycleState.SUITE_STARTED);
    }

    private void endSuite( long timeStamp ) throws DatabaseAccessException {

        try {
            dbAccess.endSuite(timeStamp, eventProcessorState.getSuiteId(), true);

            lastEndedSuiteId = eventProcessorState.getSuiteId();
        } finally {
            // even when this DB entity could not finish due to error,
            // we want to clear the internal state,
            // so next sub-entities do not go into this one
            eventProcessorState.setLifeCycleState(LifeCycleState.RUN_STARTED);

            eventProcessorState.setSuiteId(0);
        }
    }

    private void updateSuite( UpdateSuiteEvent event ) throws DatabaseAccessException {

        try {
            dbAccess.updateSuite(eventProcessorState.getSuiteId(), event.getSuiteName(), event.getUserNote(),
                                 true);
        } finally {
            // Due to change in the suite name, update suiteIdCache, 
            // only if suite name is not null and is not an empty string
            if (!StringUtils.isNullOrEmpty(event.getSuiteName())) {
                suiteIdsCache.put(eventProcessorState.getRunId() + event.getSuiteName(),
                                  eventProcessorState.getSuiteId());
            }
        }

    }

    private void clearScenarioMetainfo() throws DatabaseAccessException {

        dbAccess.clearScenarioMetainfo(eventProcessorState.getTestCaseId(), true);
    }

    private void
            addScenarioMetainfo( AddScenarioMetainfoEvent addScenarioMetainfoEvent ) throws DatabaseAccessException {

        dbAccess.addScenarioMetainfo(eventProcessorState.getTestCaseId(),
                                     addScenarioMetainfoEvent.getMetaKey(),
                                     addScenarioMetainfoEvent.getMetaValue(), true);
    }

    private void startTestCase( StartTestCaseEvent startTestCaseEvent,
                                long timeStamp ) throws LoggingException {

        int currentSuiteId = eventProcessorState.getSuiteId();

        String newSuiteName = startTestCaseEvent.getSuiteSimpleName();
        if (!StringUtils.isNullOrEmpty(newSuiteName)) {
            // Due to implementation specifics, it is not possible that a suite with
            // the needed name is not in the list of remembered suites (else clause).
            // This is guaranteed when we start the suite(which is before we start a testcase)

            // we want to make sure we are starting this testcase in a suite with this name
            String newSuiteIdentifier = eventProcessorState.getRunId() + newSuiteName;

            currentSuiteId = suiteIdsCache.get(newSuiteIdentifier);
        }

        /*
         * This event happens on the Test Executor side.
         */

        int testCaseId = dbAccess.startTestCase(startTestCaseEvent.getSuiteFullName(),
                                                startTestCaseEvent.getScenarioName(),
                                                startTestCaseEvent.getScenarioDescription(),
                                                startTestCaseEvent.getTestcaseName(), timeStamp,
                                                currentSuiteId, true);

        //set the current appender state
        TestCaseState testCaseState = new TestCaseState();
        testCaseState.setTestcaseId(testCaseId);
        eventProcessorState.setTestCaseState(testCaseState);
        eventProcessorState.setLifeCycleState(LifeCycleState.TEST_CASE_STARTED);

        //unblock the main thread which is waiting for the completion of this event
        if (listener != null) {
            listener.onTestcaseStarted();
        }
    }

    private void endTestCase( EndTestCaseEvent endTestCaseEvent, long timeStamp ) throws LoggingException {

        try {
            if (!this.deletedTestcases.contains(eventProcessorState.getTestCaseId())) {
                dbAccess.endTestCase(endTestCaseEvent.getTestCaseResult().toInt(), timeStamp,
                                     eventProcessorState.getTestCaseId(), true);
                lastEndedTestcaseId = eventProcessorState.getTestCaseId();
            }
        } finally {
            // even when this DB entity could not finish due to error,
            // we want to clear the internal state,
            // so next sub-entities do not go into this one
            eventProcessorState.setLifeCycleState(LifeCycleState.SUITE_STARTED);

            eventProcessorState.getTestCaseState().clearTestcaseId();
            eventProcessorState.getLoadQueuesState().clearAll();
        }
    }

    private void updateTestcase( UpdateTestcaseEvent updateTestcaseEvent, long timestamp ) throws LoggingException {

        String suiteFullName = updateTestcaseEvent.getSuiteFullName();
        String scenarioName = updateTestcaseEvent.getScenarioName();
        String scenarioDescription = updateTestcaseEvent.getScenarioDescription();
        String testcaseName = updateTestcaseEvent.getTestcaseName();
        String userNote = updateTestcaseEvent.getUserNote();
        int testcaseResult = updateTestcaseEvent.getTestcaseResult();
        int testcaseId = (getTestCaseId() == -1)
                                                 ? lastEndedTestcaseId
                                                 : getTestCaseId();

        dbAccess.updateTestcase(suiteFullName, scenarioName, scenarioDescription,
                                testcaseName, userNote, testcaseResult, testcaseId, timestamp, true);

    }

    /**
     * Remember a testcase that is to be deleted
     *
     * @param testcaseId
     */
    public void requestTestcaseDeletion( int testcaseId ) {

        /*
         * This code runs on the Test Executor side
         */

        this.testcaseToDelete = testcaseId;
    }

    private void deleteRequestedTestcase() throws DatabaseAccessException {

        /*
         * This code runs on the Test Executor side
         */

        // delete this testcase
        List<Object> testcasesToDelete = new ArrayList<Object>();
        Testcase tc = new Testcase();
        tc.testcaseId = String.valueOf(testcaseToDelete);
        testcasesToDelete.add(tc);

        dbAccess.deleteTestcase(testcasesToDelete);

        /*
         * Remember this testcase was deleted.
         * From now on all events related to this testcase will be simply skipped without going to the DB
         */
        deletedTestcases.add(testcaseToDelete);

        testcaseToDelete = -1;
    }

    private void joinTestCase( JoinTestCaseEvent joinTestCaseEvent ) throws LoggingException {

        /*
         * This event happens on the Agent side.
         */

        //set test case id
        eventProcessorState.setTestCaseState(joinTestCaseEvent.getTestCaseState());

        //set the current appender state
        eventProcessorState.setLifeCycleState(LifeCycleState.TEST_CASE_STARTED);

        /*
         * Now the Agent can log into the DB
         */
    }

    private void leaveTestCase() {

        /*
         * This event happens on the Agent side.
         */

        eventProcessorState.getTestCaseState().clearTestcaseId();

        //set the current appender state
        eventProcessorState.setLifeCycleState(LifeCycleState.INITIALIZED);

        /*
         * The Agent can no longer log into the DB
         */
    }

    private void
            rememberLoadQueueState( RememberLoadQueueStateEvent startLoadQueueEvent ) throws LoadQueueAlreadyStartedException {

        //first check if this load queue has already been started, just in case
        LoadQueuesState loadQueuesState = eventProcessorState.getLoadQueuesState();

        String loadQueueName = startLoadQueueEvent.getName();
        if (loadQueuesState.isLoadQueueRunning(loadQueueName)) {
            throw new LoadQueueAlreadyStartedException(loadQueueName);
        }

        //cache the load queue id
        loadQueuesState.addLoadQueue(loadQueueName, startLoadQueueEvent.getLoadQueueId());
    }

    private void
            cleanupLoadQueueState( CleanupLoadQueueStateEvent loadQueueStateEvent ) throws NoSuchLoadQueueException {

        //first check if this load queue is started at all
        LoadQueuesState loadQueuesState = eventProcessorState.getLoadQueuesState();
        String loadQueueName = loadQueueStateEvent.getName();
        int loadQueueId = loadQueuesState.getLoadQueueId(loadQueueName);
        loadQueuesState.removeLoadQueue(loadQueueName, loadQueueId);
    }

    private void endLoadQueue( EndLoadQueueEvent endLoadQueueEvent,
                               long timestamp ) throws NoSuchLoadQueueException, DatabaseAccessException {

        //first check if this load queue is started at all
        LoadQueuesState loadQueuesState = eventProcessorState.getLoadQueuesState();
        String loadQueueName = endLoadQueueEvent.getName();

        int loadQueueId = loadQueuesState.getLoadQueueId(loadQueueName);

        if (loadQueueId > 0) {
            try {

                dbAccess.endLoadQueue(endLoadQueueEvent.getResult().toInt(), timestamp, loadQueueId, true);
            } finally {
                // even when this DB entity could not finish due to error,
                // we want to clear the internal state,
                // so next sub-entities do not go into this one
                loadQueuesState.removeLoadQueue(loadQueueName, loadQueueId);
            }
        }
    }

    private void
            registerThreadWithLoadQueue( RegisterThreadWithLoadQueueEvent registerThreadWithLoadQueueEvent ) throws NoSuchLoadQueueException,
                                                                                                             ThreadAlreadyRegisteredWithLoadQueueException,
                                                                                                             ThreadNotRegisteredWithLoadQueue {

        LoadQueuesState loadQueuesState = eventProcessorState.getLoadQueuesState();
        String loadQueueName = registerThreadWithLoadQueueEvent.getLoadQueueName();

        //register the thread with the load queue - this way when a checkpoint comes in, we will
        //now which load queue it belongs to
        loadQueuesState.registerThreadWithLoadQueue(registerThreadWithLoadQueueEvent.getThreadName(),
                                                    loadQueuesState.getLoadQueueId(loadQueueName));
    }

    private void startCheckpoint( StartCheckpointEvent startCheckpointEvent ) throws LoggingException {

        //check if checkpoints are enabled at all
        if (appenderConfig.getEnableCheckpoints()) {

            LoadQueuesState loadQueuesState = eventProcessorState.getLoadQueuesState();
            int loadQueueId = loadQueuesState.getLoadQueueIdForThread(startCheckpointEvent.getThread());

            if (loadQueueId > 0) {
                final int testcaseId = eventProcessorState.getTestCaseId();
                if (!deletedTestcases.contains(testcaseId)) {
                    try {
                        CheckpointInfo startedCheckpointInfo = dbAccess.startCheckpoint(startCheckpointEvent.getName(),
                                                                                        startCheckpointEvent.getThread(),
                                                                                        startCheckpointEvent.getStartTimestamp(),
                                                                                        startCheckpointEvent.getTransferUnit(),
                                                                                        loadQueueId, true);
                        loadQueuesState.startCheckpoint(startedCheckpointInfo,
                                                        startCheckpointEvent.getThread());

                    } catch (LoggingException e) {
                        handleDeletedTestcase(e, testcaseId);
                    }
                }
            }
        }
    }

    private void endCheckpoint( EndCheckpointEvent endCheckpointEvent ) throws LoggingException {

        //check if checkpoints are enabled at all
        if (appenderConfig.getEnableCheckpoints()) {

            LoadQueuesState loadQueuesState = eventProcessorState.getLoadQueuesState();
            CheckpointInfo runningCheckpointInfo = loadQueuesState.endCheckpoint(endCheckpointEvent.getThread(),
                                                                                 endCheckpointEvent.getName(),
                                                                                 endCheckpointEvent.getEndTimestamp());

            final int testcaseId = eventProcessorState.getTestCaseId();
            if (!deletedTestcases.contains(testcaseId)) {
                try {
                    dbAccess.endCheckpoint(runningCheckpointInfo, endCheckpointEvent.getEndTimestamp(),
                                           endCheckpointEvent.getTransferSize(),
                                           endCheckpointEvent.getResult().toInt(), true);
                } catch (LoggingException e) {
                    handleDeletedTestcase(e, testcaseId);
                }
            }
        }
    }

    private void insertCheckpoint( InsertCheckpointEvent insertCheckpointEvent ) throws LoggingException {

        //check if checkpoints are enabled at all
        if (appenderConfig.getEnableCheckpoints()) {

            LoadQueuesState loadQueuesState = eventProcessorState.getLoadQueuesState();
            int loadQueueId = loadQueuesState.getLoadQueueIdForThread(insertCheckpointEvent.getThread());

            final int testcaseId = eventProcessorState.getTestCaseId();
            if (!deletedTestcases.contains(testcaseId)) {
                try {
                    dbAccess.insertCheckpoint(insertCheckpointEvent.getName(),
                                              insertCheckpointEvent.getStartTimestamp(),
                                              insertCheckpointEvent.getResponseTime(),
                                              insertCheckpointEvent.getTransferSize(),
                                              insertCheckpointEvent.getTransferUnit(),
                                              insertCheckpointEvent.getResult().toInt(), loadQueueId, true);
                } catch (LoggingException e) {
                    handleDeletedTestcase(e, testcaseId);
                }
            }
        }
    }

    private void
            insertSystemStatistics( InsertSystemStatisticEvent insertSystemStatEvent ) throws LoggingException {

        final int testcaseId = eventProcessorState.getTestCaseId();
        if (!deletedTestcases.contains(testcaseId)) {
            try {
                dbAccess.insertSystemStatistics(eventProcessorState.getTestCaseId(),
                                                insertSystemStatEvent.getMonitoredMachine(),
                                                insertSystemStatEvent.getStatisticIds(),
                                                insertSystemStatEvent.getStatisticValues(),
                                                insertSystemStatEvent.getTimestamp(), true);
            } catch (LoggingException e) {
                handleDeletedTestcase(e, testcaseId);
            }
        }
    }

    private void
            insertUserActivityStatistics( InsertUserActivityStatisticEvent insertUserActivityStatEvent ) throws LoggingException {

        final int testcaseId = eventProcessorState.getTestCaseId();
        if (!deletedTestcases.contains(testcaseId)) {
            try {
                dbAccess.insertUserActivityStatistics(eventProcessorState.getTestCaseId(),
                                                      insertUserActivityStatEvent.getMonitoredMachine(),
                                                      insertUserActivityStatEvent.getStatisticIds(),
                                                      insertUserActivityStatEvent.getStatisticValues(),
                                                      insertUserActivityStatEvent.getTimestamp(), true);
            } catch (LoggingException e) {
                handleDeletedTestcase(e, testcaseId);
            }
        }
    }

    private void insertMessage( LogEventRequest eventRequest, boolean escapeHtml,
                                boolean isRunMessage ) throws LoggingException {

        LoggingEvent event = eventRequest.getEvent();

        // If test case is not open, just return - this is necessary because components which are not aware of this
        // appender may try to log before the client has a chance of opening a test case.
        if ( (eventProcessorState.getLifeCycleState() == LifeCycleState.RUN_STARTED && !afterClassMode) ||
             isRunMessage || afterSuiteMode) {
            if (!ActiveDbAppender.isBeforeAndAfterMessagesLoggingSupported) {
                return;
            }
            int runId = eventProcessorState.getRunId();
            if (isRunMessage && eventProcessorState.getTestCaseState().getRunId() != 0) {
                runId = eventProcessorState.getTestCaseState().getRunId();
            }
            Level level = event.getLevel();
            try {
                dbAccess.insertRunMessage(getLoggingMesage(event), convertMsgLevel(level), escapeHtml,
                                          machineName, eventRequest.getThreadName(),
                                          eventRequest.getTimestamp(), runId, true);
            } catch (LoggingException e) {
                handleDeletedRun(e, runId);
            }
        } else if (eventProcessorState.getLifeCycleState() == LifeCycleState.TEST_CASE_STARTED || afterMethodMode) {

            final int testcaseId = (afterMethodMode)
                                                     ? lastEndedTestcaseId
                                                     : eventProcessorState.getTestCaseId();
            if (!deletedTestcases.contains(testcaseId)) {
                Level level = event.getLevel();
                try {
                    dbAccess.insertMessage(getLoggingMesage(event), convertMsgLevel(level), escapeHtml,
                                           machineName, eventRequest.getThreadName(),
                                           eventRequest.getTimestamp(), testcaseId, true);
                } catch (LoggingException e) {
                    handleDeletedTestcase(e, testcaseId);
                }
            }
        } else if (eventProcessorState.getLifeCycleState() == LifeCycleState.SUITE_STARTED || afterClassMode) {
            if (!ActiveDbAppender.isBeforeAndAfterMessagesLoggingSupported) {
                return;
            }
            final int suiteId = (afterClassMode)
                                                 ? lastEndedSuiteId
                                                 : eventProcessorState.getSuiteId();
            Level level = event.getLevel();
            try {
                dbAccess.insertSuiteMessage(getLoggingMesage(event), convertMsgLevel(level), escapeHtml,
                                            machineName, eventRequest.getThreadName(),
                                            eventRequest.getTimestamp(), suiteId, true);
            } catch (LoggingException e) {
                handleDeletedSuite(e, suiteId);
            }

        }
    }

    private String getLoggingMesage( LoggingEvent event ) {

        Throwable throwable = null;
        ThrowableInformation throwableInfo = event.getThrowableInformation();
        if (throwableInfo != null && throwableInfo.getThrowable() != null) {
            // logging through methods like error(new Exception);
            throwable = throwableInfo.getThrowable();
        } else if (event.getMessage() instanceof Throwable) {
            // logging through methods like error("some message", new Exception);
            throwable = (Throwable) event.getMessage();
        }

        // first format the message using the layout
        String message = layout.format(event);
        // then append the exception stack trace
        if (throwable != null) {
            message = getExceptionMsg(throwable, message);
        }

        return message;

    }

    private void handleDeletedRun( LoggingException e, int runId ) throws LoggingException {

        Throwable cause = e.getCause();
        if (cause != null && cause instanceof SQLException
            && cause.getMessage()
                    .contains("The INSERT statement conflicted with the FOREIGN KEY constraint")) {

            // Check if this error is caused due to deleted run
            if (!dbAccess.isRunPresent(runId)) {
                // This run seems to be deleted
            }
        } else {
            throw e;
        }
    }

    private void handleDeletedSuite( LoggingException e, int suiteId ) throws LoggingException {

        Throwable cause = e.getCause();
        if (cause != null && cause instanceof SQLException
            && cause.getMessage()
                    .contains("The INSERT statement conflicted with the FOREIGN KEY constraint")) {

            // Check if this error is caused due to deleted suite
            if (!dbAccess.isSuitePresent(suiteId)) {
                // This suite seems to be deleted
            }
        } else {
            throw e;
        }
    }

    private void handleDeletedTestcase( LoggingException e, int testcaseId ) throws LoggingException {

        Throwable cause = e.getCause();
        if (cause != null && cause instanceof SQLException
            && cause.getMessage()
                    .contains("The INSERT statement conflicted with the FOREIGN KEY constraint")) {

            // Check if this error is caused due to deleted testcase
            if (!dbAccess.isTestcasePresent(testcaseId)) {
                // This testcase seems to be deleted, mark it as deleted.
                // On the Agent side, this is the normal way to understand a testcase was deleted.
                deletedTestcases.add(testcaseId);
            }
        } else {
            throw e;
        }
    }

    private int convertMsgLevel( org.apache.log4j.Level level ) {

        switch (level.toInt()) {
            case Level.FATAL_INT:
                return 1;
            case Level.ERROR_INT:
                return 2;
            case Level.WARN_INT:
                return 3;
            case Level.INFO_INT:
                return 4;
            case Level.DEBUG_INT:
                return 5;
            case Level.TRACE_INT:
                return 6;
            case SystemLogLevel.SYSTEM_INT:
                return 7;
            default:
                return 4;
        }
    }

    private String getExceptionMsg( Throwable throwable, String usrMsg ) {

        StringBuffer msg = new StringBuffer();
        if (throwable != null) {
            msg.append("EXCEPTION\n");

            if (usrMsg != null && usrMsg.length() > 0) {
                msg.append("USER message:\n\t");
                msg.append(usrMsg);
                msg.append("\n");
            }

            msg.append("\nCALL STACK:");
            msg.append(getStackTraceInfo(throwable));
        }

        return msg.toString();
    }

    private String getStackTraceInfo( Throwable throwable ) {

        final Writer writer = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(writer);
        throwable.printStackTrace(printWriter);

        return writer.toString();
    }

    public void setEventProcessorState( EventProcessorState eventProcessorState ) {

        this.eventProcessorState = eventProcessorState;
    }

    private void constructUpdateRunEvent() {

        try {

            if (userProvidedUpdateRunEvent != null) {

                // get current run info from database
                Run run = getLatestRun();

                // replace the missing fields, received by the latest UpdateRunEvent with the one from the DB
                String runName = (userProvidedUpdateRunEvent.getRunName() != null)
                                                                                   ? userProvidedUpdateRunEvent.getRunName()
                                                                                   : run.runName;
                String osName = (userProvidedUpdateRunEvent.getOsName() != null)
                                                                                 ? userProvidedUpdateRunEvent.getOsName()
                                                                                 : run.os;
                String productName = (userProvidedUpdateRunEvent.getProductName() != null)
                                                                                           ? userProvidedUpdateRunEvent.getProductName()
                                                                                           : run.productName;
                String versionName = (userProvidedUpdateRunEvent.getVersionName() != null)
                                                                                           ? userProvidedUpdateRunEvent.getVersionName()
                                                                                           : run.versionName;
                String buildName = (userProvidedUpdateRunEvent.getBuildName() != null)
                                                                                       ? userProvidedUpdateRunEvent.getBuildName()
                                                                                       : run.buildName;
                String userNote = (userProvidedUpdateRunEvent.getUserNote() != null)
                                                                                     ? userProvidedUpdateRunEvent.getUserNote()
                                                                                     : run.userNote;
                String hostName = (userProvidedUpdateRunEvent.getHostName() != null)
                                                                                     ? userProvidedUpdateRunEvent.getHostName()
                                                                                     : run.hostName;

                // construct the new pending UpdateRunEvent
                actualUpdateRunEvent = new UpdateRunEvent(userProvidedUpdateRunEvent.getFQNOfLoggerClass(),
                                                          (Logger) userProvidedUpdateRunEvent.getLogger(),
                                                          runName, osName, productName, versionName,
                                                          buildName, userNote, hostName);

            }

        } catch (SQLException e) {
            /*  
             * Could not obtain run info from database.
             * No exception will be logged, because the event processor is busy handling the UpdateRunEvent
             * and will not be able to handle the log message event as well
            */
        }

    }

    private Run getLatestRun() throws SQLException {

        PreparedStatement stmt = null;

        Run run = new Run();

        try {

            Connection tmpConn = ConnectionPool.getConnection(dbConnection);

            stmt = tmpConn.prepareStatement("SELECT * FROM tRuns WHERE runId="
                                            + eventProcessorState.getRunId());
            ResultSet rs = stmt.executeQuery();
            rs.next();
            run.runId = rs.getString("runId");
            run.productName = rs.getString("productName");
            run.versionName = rs.getString("versionName");
            run.buildName = rs.getString("buildName");
            run.runName = rs.getString("runName");
            run.os = rs.getString("OS");
            run.hostName = rs.getString("hostName");
            if (run.hostName == null) {
                run.hostName = "";
            }
            run.userNote = rs.getString("userNote");
            if (run.userNote == null) {
                run.userNote = "";
            }

        } finally {
            DbUtils.closeStatement(stmt);
        }

        return run;
    }
}
