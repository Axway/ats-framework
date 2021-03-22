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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.performance.monitor.beans.MonitorResults;
import com.axway.ats.common.performance.monitor.beans.ReadingBean;
import com.axway.ats.core.threads.ThreadsPerCaller;

/**
 * A monitoring agent which collects and provides information about the Agent users activity
 */
public class UserActionsMonitoringAgent {

    private static Logger                                  log       = LogManager.getLogger(UserActionsMonitoringAgent.class);

    // instance for each remote caller
    private static Map<String, UserActionsMonitoringAgent> instances = new HashMap<String, UserActionsMonitoringAgent>();

    // a map which remembers how many users are currently running a particular action
    private Map<String, Integer>                           runningActionsMap;

    // a simple clock which gets the info from the actions map and fills the collected data buffer
    // this is done on regular time intervals
    private MonitoringThread                               monitoringThread;

    // this is the agent ip:port on which the current thread is started
    private String                                         agentAddress;

    private UserActionsMonitoringAgent() {

        this.runningActionsMap = new HashMap<String, Integer>();

    }

    public static synchronized UserActionsMonitoringAgent getInstance(
                                                                       String caller ) {

        UserActionsMonitoringAgent instance = instances.get(caller);
        if (instance == null) {
            instance = new UserActionsMonitoringAgent();
            instances.put(caller, instance);
        }
        return instance;
    }

    public void setAgentAddress(
                                 String agentAddress ) {

        this.agentAddress = agentAddress;
    }

    /**
     * This method is called by the external monitoring system.
     *
     * Start the monitoring process
     *
     * @param startTimestamp the initial time stamp
     * @param pollInterval the polling interval
     */
    public void startMonitoring(
                                 long startTimestamp,
                                 int pollInterval ) {

        if (monitoringThread != null && monitoringThread.isAlive()) {
            log.warn("The user activity monitor is running from a previous run. We will stop it now");

            monitoringThread.interrupt();
        }

        log.info("Starting user activity monitor at intervals of " + pollInterval + " seconds");

        resetTheMonitoringAgent();

        monitoringThread = new MonitoringThread(startTimestamp, pollInterval);
        monitoringThread.start();
    }

    /**
     * This method is called by the external monitoring system.
     *
     * Stop the monitoring process
     */
    public void stopMonitoring() {

        if (monitoringThread == null) {
            log.warn("Stopping the user activity monitor is skipped as it was not running. Possible cause - not started yet or already stopped.");
            resetTheMonitoringAgent();
            return;
        }

        monitoringThread.interrupt();
        monitoringThread = null;

        resetTheMonitoringAgent();

        log.info("Stopped user activity monitor");
    }

    /**
     * This method is called by the Agent action invoker
     *
     * Indicates an Agent action is started, so we increment
     * the counter for this action
     *
     * @param actionName the name of the action
     */
    public synchronized void actionStarted(
                                            String actionName ) {

        if (monitoringThread != null) {
            Integer nRunning = runningActionsMap.get(actionName);
            if (nRunning == null) {
                runningActionsMap.put(actionName, 1);
            } else {
                runningActionsMap.put(actionName, nRunning + 1);
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
    public synchronized void actionEnded(
                                          String actionName ) {

        if (monitoringThread != null) {
            Integer nRunning = runningActionsMap.get(actionName);
            if (nRunning == null) {
                /* We can not END this actions as it is not been STARTED
                 *
                 * This can be a normal situation if the test is aborted, the actions
                 * are still running and we start a new test which start again the
                 * monitoring process which cleans up the "runningActionsMap". Right
                 * at this moment 1 of the still running from the last test actions
                 * is ending, so we come right here
                 */
            } else {
                runningActionsMap.put(actionName, nRunning - 1);
            }
        }
    }

    /**
     * @return a new instance of the actions map
     */
    private synchronized Map<String, Integer> getRunningActionsMapCopy() {

        return new HashMap<String, Integer>(this.runningActionsMap);
    }

    /**
     * Resets the data buffers
     */
    private synchronized void resetTheMonitoringAgent() {

        runningActionsMap.clear();
    }

    /**
     * The monitoring thread which works on specified time interval
     */
    class MonitoringThread extends Thread {

        private Logger    log = LogManager.getLogger(MonitoringThread.class);

        private long      currentTimestamp;
        private final int pollInterval;

        private String    callerId;

        MonitoringThread( long currentTimestamp,
                          int pollInterval ) {

            this.currentTimestamp = currentTimestamp;
            this.pollInterval = 1000 * pollInterval;

            this.callerId = ThreadsPerCaller.getCaller();

            setName("Monitoring_users-" + this.callerId);
        }

        @Override
        public void run() {

            ThreadsPerCaller.registerThread(this.callerId);

            log.info("Started monitoring user activity in intervals of " + pollInterval + " milliseconds");
            try {
                int lastPollTime = 0;
                while (true) {

                    // we don't measure, but we calculate the timestamp, so it is synchronized
                    // between all monitored machines
                    this.currentTimestamp += pollInterval;

                    int sleepTimeBeforeNextPoll = pollInterval - lastPollTime;
                    if (sleepTimeBeforeNextPoll < 0) {
                        // we get here when the last poll took longer than the user provided poll interval
                        sleepTimeBeforeNextPoll = 0;
                        log.warn("Last poll time took longer than the poll interval."
                                 + " Details: last poll time " + lastPollTime + " ms, poll interval is "
                                 + pollInterval + " ms");
                    }
                    Thread.sleep(sleepTimeBeforeNextPoll);

                    long startPollingTime = System.currentTimeMillis();

                    List<ReadingBean> newReadingBeans = new ArrayList<ReadingBean>();
                    // poll for new data

                    int totalUserActions = 0;

                    // register how many users run each action
                    Map<String, Integer> lastRunningActionsMapChunks = getRunningActionsMapCopy();
                    for (Entry<String, Integer> actionMapChunk : lastRunningActionsMapChunks.entrySet()) {

                        int usersRunningThisAction = actionMapChunk.getValue();

                        ReadingBean newReadingBean = new ReadingBean(null,
                                                                     "[users] " + actionMapChunk.getKey(),
                                                                     "Count");
                        newReadingBean.setValue(String.valueOf(usersRunningThisAction));
                        newReadingBeans.add(newReadingBean);

                        totalUserActions += usersRunningThisAction;

                    }

                    // register the total number of users, we always have this statistic
                    String totalUsers = "Total";
                    ReadingBean newTotalUsersReadingBean = new ReadingBean(null,
                                                                           "[users] "
                                                                                 + totalUsers,
                                                                           "Count");

                    newTotalUsersReadingBean.setValue(String.valueOf(totalUserActions));
                    newReadingBeans.add(newTotalUsersReadingBean);

                    MonitorResults newMonitorResults = new MonitorResults(currentTimestamp,
                                                                          newReadingBeans);

                    lastPollTime = (int) (System.currentTimeMillis() - startPollingTime);

                    // log the results in the DB
                    UserActivityLoggingUtils.logCollectedResults(agentAddress, newMonitorResults);

                }
            } catch (InterruptedException e) {
                // we have been stopped, this is expected
                log.info("Stopped monitoring the user activity");
            } catch (Exception e) {
                log.error("User activity monitoring is aborted due to unexpected error", e);
            } finally {
                ThreadsPerCaller.unregisterThread();
            }
        }
    }

}
