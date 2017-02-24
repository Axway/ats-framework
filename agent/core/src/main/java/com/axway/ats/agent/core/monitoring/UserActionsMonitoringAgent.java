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
package com.axway.ats.agent.core.monitoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.axway.ats.common.performance.monitor.beans.BasicReadingBean;
import com.axway.ats.common.performance.monitor.beans.FullReadingBean;
import com.axway.ats.common.performance.monitor.beans.MonitorResults;

/**
 * A monitoring agent which collects and provides information about the Agent users activity
 */
public class UserActionsMonitoringAgent {

    private static Logger                                  log                           = Logger.getLogger( UserActionsMonitoringAgent.class );

    // instance for each remote caller
    private static Map<String, UserActionsMonitoringAgent> instances                     = new HashMap<String, UserActionsMonitoringAgent>();

    // a map which remembers how many users are currently running a particular action
    private Map<String, Integer>                           runningActionsMap;

    // the information ready to be sent out
    private List<MonitorResults>                           collectedResults;
    private Map<String, FullReadingBean>                   knownReadingBeans;

    //the id for the total number of running users must be the same among all Agents
    private static final String                            TOTAL_USERS_READING_UNIQUE_ID = "1000000";

    // a simple clock which gets the info from the actions map and fills the collected data buffer
    // this is done on regular time intervals
    private MonitoringThread                               monitoringThread;

    private UserActionsMonitoringAgent() {

        this.runningActionsMap = new HashMap<String, Integer>();
        this.collectedResults = new ArrayList<MonitorResults>();
        this.knownReadingBeans = new HashMap<String, FullReadingBean>();
    }

    public static synchronized UserActionsMonitoringAgent getInstance( String caller ) {

        UserActionsMonitoringAgent instance = instances.get( caller );
        if( instance == null ) {
            instance = new UserActionsMonitoringAgent();
            instances.put( caller, instance );
        }
        return instance;
    }

    /**
     * This method is called by the external monitoring system.
     *
     * Start the monitoring process
     *
     * @param startTimestamp the initial time stamp
     * @param pollInterval the polling interval
     */
    public void startMonitoring( long startTimestamp, int pollInterval ) {

        if( monitoringThread != null && monitoringThread.isAlive() ) {
            log.warn( "The user activity monitor is running from a previous run. We will stop it now" );

            monitoringThread.interrupt();
        }

        log.info( "Starting user activity monitor at intervals of " + pollInterval + " seconds" );

        resetTheMonitoringAgent();

        monitoringThread = new MonitoringThread( startTimestamp, pollInterval );
        monitoringThread.start();
    }

    /**
     * This method is called by the external monitoring system.
     *
     * Stop the monitoring process
     */
    public void stopMonitoring() {

        if( monitoringThread == null ) {
            log.warn( "Stopping the user activity monitor is skipped as it was not running. Possible cause - not started yet or already stopped." );
            resetTheMonitoringAgent();
            return;
        }

        monitoringThread.interrupt();
        monitoringThread = null;

        resetTheMonitoringAgent();

        log.info( "Stopped user activity monitor" );
    }

    /**
     * This method is called by the external monitoring system.
     *
     * The collected data is retrieved from the buffer, the buffer is cleaned for new data
     * @return
     */
    public synchronized List<MonitorResults> getMonitoringResults() {

        // create a copy of the collected results and send them to the Test Executor
        List<MonitorResults> collectedResultsToPass = new ArrayList<MonitorResults>( collectedResults );

        // purge the collected data
        collectedResults.clear();
        return collectedResultsToPass;
    }

    /**
     * This method is called by the Agent action invoker
     *
     * Indicates an Agent action is started, so we increment
     * the counter for this action
     *
     * @param actionName the name of the action
     */
    public synchronized void actionStarted( String actionName ) {

        if( monitoringThread != null ) {
            Integer nRunning = runningActionsMap.get( actionName );
            if( nRunning == null ) {
                runningActionsMap.put( actionName, 1 );
            } else {
                runningActionsMap.put( actionName, nRunning + 1 );
            }
        }
    }

    /**
     * This method is called by the Agent action invoker
     *
     * Indicates an Agent action is ended, so we decrement
     * the counter for this action
     *
     * @param actionName the name of the action
     */
    public synchronized void actionEnded( String actionName ) {

        if( monitoringThread != null ) {
            Integer nRunning = runningActionsMap.get( actionName );
            if( nRunning == null ) {
                /* We can not END this actions as it is not been STARTED
                 *
                 * This can be a normal situation if the test is aborted, the actions
                 * are still running and we start a new test which start again the
                 * monitoring process which cleans up the "runningActionsMap". Right
                 * at this moment 1 of the still running from the last test actions
                 * is ending, so we come right here
                 */
            } else {
                runningActionsMap.put( actionName, nRunning - 1 );
            }
        }
    }

    /**
     * @return a new instance of the actions map
     */
    private synchronized Map<String, Integer> getRunningActionsMapCopy() {

        return new HashMap<String, Integer>( this.runningActionsMap );
    }

    /**
     * Appends a new line with monitoring activity per timestamp
     * @param newData the new data
     */
    private synchronized void appendCollectedData( MonitorResults newMonitoringResults ) {

        collectedResults.add( newMonitoringResults );
    }

    /**
     * Resets the data buffers
     */
    private synchronized void resetTheMonitoringAgent() {

        runningActionsMap.clear();
        collectedResults.clear();
        knownReadingBeans.clear();
    }

    /**
     * The monitoring thread which works on specified time interval
     */
    class MonitoringThread extends Thread {

        private Logger    log = Logger.getLogger( MonitoringThread.class );

        private long      currentTimestamp;
        private final int pollInterval;

        // we keep the collected data for up to 1 hour,
        // if not collected yet - we discard this data
        private final int collectedDataBufferLimit;

        MonitoringThread( long currentTimestamp, int pollInterval ) {

            this.currentTimestamp = currentTimestamp;
            this.pollInterval = 1000 * pollInterval;
            this.collectedDataBufferLimit = 3600 / pollInterval;
        }

        @Override
        public void run() {

            log.info( "Started monitoring user activity in intervals of " + pollInterval + " milliseconds" );
            try {
                int lastPollTime = 0;
                while( true ) {

                    // we don't measure, but we calculate the timestamp, so it is synchronized
                    // between all monitored machines
                    this.currentTimestamp += pollInterval;

                    int sleepTimeBeforeNextPoll = pollInterval - lastPollTime;
                    if( sleepTimeBeforeNextPoll < 0 ) {
                        // we get here when the last poll took longer than the user provided poll interval
                        sleepTimeBeforeNextPoll = 0;
                        log.warn( "Last poll time took longer than the poll interval."
                                  + " Details: last poll time " + lastPollTime + " ms, poll interval is "
                                  + pollInterval + " ms" );
                    }
                    Thread.sleep( sleepTimeBeforeNextPoll );

                    long startPollingTime = System.currentTimeMillis();

                    List<BasicReadingBean> newReadingBeans = new ArrayList<BasicReadingBean>();
                    // poll for new data

                    int totalUserActions = 0;

                    // register how many users run each action
                    Map<String, Integer> lastRunningActionsMapChunks = getRunningActionsMapCopy();
                    for( Entry<String, Integer> actionMapChunk : lastRunningActionsMapChunks.entrySet() ) {

                        int usersRunningThisAction = actionMapChunk.getValue();

                        FullReadingBean knownReadingBean = knownReadingBeans.get( actionMapChunk.getKey() );
                        if( knownReadingBean != null ) {
                            newReadingBeans.add( new BasicReadingBean( knownReadingBean.getId(),
                                                                       String.valueOf( usersRunningThisAction ) ) );
                        } else {
                            FullReadingBean newReadingBean = new FullReadingBean( null,
                                                                                  "[users] " + actionMapChunk.getKey(),
                                                                                  "Count" );
                            newReadingBean.setId( actionMapChunk.getKey() );
                            newReadingBean.setValue( String.valueOf( usersRunningThisAction ) );
                            newReadingBeans.add( newReadingBean );

                            knownReadingBeans.put( actionMapChunk.getKey(), newReadingBean );
                        }

                        totalUserActions += usersRunningThisAction;
                    }
                    // register the total number of users, we always have this statistic
                    String totalUsers = "Total";
                    FullReadingBean knownTotalUsersReadingBean = knownReadingBeans.get( totalUsers );
                    if( knownTotalUsersReadingBean != null ) {
                        newReadingBeans.add( new BasicReadingBean( knownTotalUsersReadingBean.getId(),
                                                                   String.valueOf( totalUserActions ) ) );
                    } else {
                        FullReadingBean newTotalUsersReadingBean = new FullReadingBean( null,
                                                                                        "[users] "
                                                                                              + totalUsers,
                                                                                        "Count" );
                        newTotalUsersReadingBean.setId( TOTAL_USERS_READING_UNIQUE_ID );
                        newTotalUsersReadingBean.setValue( String.valueOf( totalUserActions ) );
                        newReadingBeans.add( newTotalUsersReadingBean );

                        knownReadingBeans.put( totalUsers, newTotalUsersReadingBean );
                    }

                    appendCollectedData( new MonitorResults( currentTimestamp, newReadingBeans ) );

                    lastPollTime = ( int ) ( System.currentTimeMillis() - startPollingTime );

                    if( collectedResults.size() > collectedDataBufferLimit ) {
                        log.warn( "The collected data was not requested for the last 1 hour, so we will discard it now" );
                        collectedResults.clear();
                    }
                }
            } catch( InterruptedException e ) {
                // we have been stopped, this is expected
                log.info( "Stopped monitoring the user activity" );
            } catch( Exception e ) {
                log.error( "User activity monitoring is aborted due to unexpected error", e );
            }
        }
    }
}
