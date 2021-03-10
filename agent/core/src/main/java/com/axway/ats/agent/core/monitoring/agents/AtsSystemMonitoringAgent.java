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
package com.axway.ats.agent.core.monitoring.agents;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.agent.core.monitoring.jvmmonitor.AtsJvmMonitor;
import com.axway.ats.agent.core.monitoring.systemmonitor.AtsSystemMonitor;
import com.axway.ats.agent.core.monitoring.systemmonitor.ReadingsRepository;
import com.axway.ats.common.performance.monitor.PerformanceMonitor;
import com.axway.ats.common.performance.monitor.beans.MonitorResults;
import com.axway.ats.common.performance.monitor.beans.ReadingBean;
import com.axway.ats.core.monitoring.MonitoringException;
import com.axway.ats.core.threads.ThreadsPerCaller;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.utils.TimeUtils;
import com.axway.ats.log.AtsDbLogger;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;

/**
 * A monitoring agent, it works on the MONITORED machine.
 */
public class AtsSystemMonitoringAgent extends AbstractMonitoringAgent {

    private static Logger            log                         = LogManager.getLogger(AtsSystemMonitoringAgent.class);

    /*
     * Skip checking in db appender is attached, because we are on the agent and not the executor.
     * */
    private static AtsDbLogger       dblog                       = AtsDbLogger.getLogger(AtsSystemMonitoringAgent.class.getName(), true);

    private static final int         MAX_LENGTH_STATISTIC_IDS    = 950;
    private static final int         MAX_LENGTH_STATISTIC_VALUES = 7950;

    private static final int         MAX_NUMBER_LOGGED_ERRORS    = 10;
    private static final String      CUSTOM_READING_PREFIX       = "[custom] ";

    private List<PerformanceMonitor> monitors;
    private MonitoringThread         monitoringThread;
    private Map<String, Integer>     pollErrors;

    public AtsSystemMonitoringAgent( int pollInterval,
                                     long executorTimeOffset ) {

        // we must have a completely clean instance when this method return
        this.monitors = new ArrayList<PerformanceMonitor>();
        this.pollErrors = new HashMap<String, Integer>();

        // set the new start polling interval
        setPollInterval(pollInterval * 1000); // make it in seconds
        setExecutorTimeOffset(executorTimeOffset);

        // if the test has ended without stopping the monitoring process
        // we now need to stop the monitoring process
        if (monitors.size() > 0) {
            log.warn("There are some monitors running from before. We will try to deinitialize them now");
            stopMonitoring();
        }
    }

    @Override
    public void startMonitoring() {

        monitoringThread = new MonitoringThread(pollInterval, executorTimeOffset);
        monitoringThread.start();
    }

    @Override
    public void stopMonitoring() {

        log.info("Stopping the monitor process");
        if (monitoringThread == null) {
            log.warn("Cannot stop the monitoring thread as it is currently not "
                     + "running. The monitoring was either not started at all, "
                     + "or it was already stopped.");
            return;
        }
        monitoringThread.stopRunning();

        // wait for up to 2 times the polling interval until the thread exit out
        boolean threadStopped = false;
        for (int i = 0; i < 10 * this.pollInterval / 1000; i++) {
            threadStopped = monitoringThread.isStopped();
            if (threadStopped) {
                log.info("Successfully stoped the monitor process");
                break;
            } else {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {}
            }
        }

        if (!threadStopped) {
            log.error("Could not stop the monitor process in the regular way. We will try to abort the thread");
            monitoringThread.interrupt();
        }

        monitoringThread = null;

        resetTheMonitoringAgent();
    }

    public void addMonitor(
                            PerformanceMonitor monitor ) {

        log.info("Attaching monitor: " + monitor.getDescription());
        if (isMonitorAlreadyPresent(monitor)) {
            log.error("Monitor already attached: " + monitor.getDescription());
        } else {
            monitors.add(monitor);
        }
    }

    public void resetTheMonitoringAgent() {

        for (PerformanceMonitor monitor : monitors) {
            log.info("Deinitializing monitor: " + monitor.getDescription());
            try {
                monitor.deinit();
            } catch (Exception e) {
                log.error("Error deinitializing monitor: " + monitor.getDescription(), e);
            }
        }

        this.monitors = new ArrayList<PerformanceMonitor>();
        this.pollErrors = new HashMap<String, Integer>();

    }

    private boolean isMonitorAlreadyPresent(
                                             PerformanceMonitor thisMonitor ) {

        for (PerformanceMonitor monitor : monitors) {
            if (monitor.getClass().getName().equals(thisMonitor.getClass().getName())) {
                return true;
            }
        }

        return false;
    }

    enum MONITORING_THREAD_STATE {
        RUNNING, STOPPING, STOPPED
    }

    class MonitoringThread extends Thread {

        private Logger                  log = LogManager.getLogger(MonitoringThread.class);

        private final int               pollInterval;
        private long                    executorTimeOffset;

        private MONITORING_THREAD_STATE monitoringThreadState;

        private String                  callerId;

        MonitoringThread( int pollInterval,
                          long executorTimeOffset ) {

            this.monitoringThreadState = MONITORING_THREAD_STATE.RUNNING;

            this.pollInterval = pollInterval;
            this.executorTimeOffset = executorTimeOffset;
            this.callerId = ThreadsPerCaller.getCaller();

            setName("Monitoring_system-" + this.callerId);

            log.debug("Monitoring thread started at timestamp " + new Date());
        }

        @Override
        public void run() {

            ThreadsPerCaller.registerThread(this.callerId);

            log.info("Started monitoring in intervals of " + pollInterval + " milliseconds");
            try {
                boolean hasFailureInPreviousPoll = false;
                int lastPollDuration = 0;
                while (monitoringThreadState == MONITORING_THREAD_STATE.RUNNING) {

                    int sleepTimeBeforeNextPoll = pollInterval - lastPollDuration;
                    if (sleepTimeBeforeNextPoll < 0) {
                        // we get here when the last poll took longer than the
                        // user provided poll interval
                        sleepTimeBeforeNextPoll = 0;
                        log.warn("Last poll time took longer than the poll interval."
                                 + " Details: last poll duration " + lastPollDuration
                                 + " ms, poll interval is " + pollInterval
                                 + " ms. You should probably consider increasing the poll interval");
                    } else {
                        if (sleepTimeBeforeNextPoll > pollInterval) {
                            sleepTimeBeforeNextPoll = 0;
                            log.warn("Polling duration is calculated as negative number (" + lastPollDuration
                                     + " ms). Possible reason is that system "
                                     + "time had been changed back. Poll will be issued now.");
                        }
                    }

                    Thread.sleep(sleepTimeBeforeNextPoll);

                    long startPollingTime = System.currentTimeMillis();
                    long currentTimestamp = System.currentTimeMillis() + this.executorTimeOffset;

                    // poll for new data
                    List<MonitorResults> newResults = new ArrayList<MonitorResults>();
                    for (PerformanceMonitor monitor : monitors) {
                        String monitorDescription = monitor.getDescription();
                        if (log.isDebugEnabled()) {
                            log.debug("Poll data for monitor: " + monitorDescription);
                        }
                        try {
                            if (!monitor.isInitialized()) {

                                MonitorResults results = new MonitorResults(currentTimestamp,
                                                                            monitor.pollNewDataForFirstTime());
                                if (! (monitor instanceof AtsSystemMonitor)
                                    && ! (monitor instanceof AtsJvmMonitor)) {
                                    // this is a custom monitor, so will add the
                                    // '[custom]' prefix
                                    for (ReadingBean reading : results.getReadings()) {
                                        if (reading instanceof ReadingBean) {
                                            ReadingBean newReading = (ReadingBean) reading;
                                            newReading.setName(CUSTOM_READING_PREFIX
                                                               + newReading.getName());
                                        }
                                    }
                                }
                                newResults.add(results);

                                // The monitor passed the 'first time poll', so
                                // we got the list of FullReadingBean.
                                // If we do not get here, an error has happened
                                // and we will call same method again the next
                                // time.
                                monitor.setInitialized();
                            } else {
                                newResults.add(new MonitorResults(currentTimestamp,
                                                                  monitor.pollNewData()));

                            }
                            if (hasFailureInPreviousPoll) {
                                // reset the polling errors counter because the
                                // monitor is now OK
                                pollErrors.remove(monitorDescription);
                            }
                        } catch (Throwable th) {
                            handlePollError(currentTimestamp, monitorDescription, th);
                        }
                    }

                    if (newResults.size() > 0) {
                        if (log.isDebugEnabled()) {
                            log.debug("new data: " + newResults.toString());
                        }

                        int resultsAddeed = 0;

                        if (newResults.size() > 0) {

                            // update the DB definitions if needed
                            for (MonitorResults monitorResult : newResults) {
                                updateDatabaseRepository(HostUtils.getLocalHostIP(), monitorResult.getReadings());
                            }

                            // log the results to the database
                            resultsAddeed = logResults(newResults);
                            log.debug("Successfully sent " + resultsAddeed
                                      + " system monitoring results to the logging database");
                        } else {
                            log.warn("No new system monitoring results to log");
                        }
                    }

                    lastPollDuration = (int) (System.currentTimeMillis() - startPollingTime);

                    // check if there is a failure during the poll
                    hasFailureInPreviousPoll = newResults.size() == 0;
                }
            } catch (InterruptedException e) {
                // we have been stopped by interrupting the monitoring thread
                log.error("Monitoring thread was interrupted", e);
            } catch (Throwable th) {
                log.error("Monitoring is aborted due to unexpected error", th);
            } finally {
                this.monitoringThreadState = MONITORING_THREAD_STATE.STOPPED;
                ThreadsPerCaller.unregisterThread();
            }
        }

        /**
         * Place an order to stop the monitoring after the current poll
         */
        public void stopRunning() {

            this.monitoringThreadState = MONITORING_THREAD_STATE.STOPPING;
        }

        /**
         * @return if the monitoring is stopped
         */
        public boolean isStopped() {

            return this.monitoringThreadState == MONITORING_THREAD_STATE.STOPPED;
        }

        private void handlePollError(
                                      long currentTimestamp,
                                      String monitorDescription,
                                      Throwable th ) {

            Integer monitorErrors = 0;
            if (pollErrors.containsKey(monitorDescription)) {
                monitorErrors = pollErrors.get(monitorDescription);
            }
            monitorErrors++;
            pollErrors.put(monitorDescription, monitorErrors);

            if (monitorErrors < MAX_NUMBER_LOGGED_ERRORS) {
                log.error("Error polling monitor '" + monitorDescription
                          + "'. All polled values from all monitors will be skipped for "
                          + TimeUtils.getFormattedDateTillMilliseconds(new Date(currentTimestamp))
                          + " timestamp", th);
            } else if (monitorErrors == MAX_NUMBER_LOGGED_ERRORS) {
                log.error("This is the " + MAX_NUMBER_LOGGED_ERRORS
                          + "th and last time we are logging polling error for monitor: "
                          + monitorDescription, th);
            }
        }

        private int logResults(
                                List<MonitorResults> monitorResults ) {

            // counter to hold the number of results which have logged
            int resultsAddeed = 0;

            for (MonitorResults newResultsLine : monitorResults) {
                resultsAddeed += logResultsForOneTimestamp(newResultsLine);
            }

            return resultsAddeed;
        }

        private int logResultsForOneTimestamp(
                                               MonitorResults resultsLine ) {

            int resultsAddeed = 0;

            // transform all reading info into 2 Strings: one for DB IDs and one
            // for their values
            StringBuilder statisticDbIds = new StringBuilder();
            StringBuilder statisticValues = new StringBuilder();
            for (ReadingBean reading : resultsLine.getReadings()) {
                String readingDbId = String.valueOf(reading.getDbId());
                if (readingDbId == null) {
                    /*log.error( "We do not have information in the database about this reading ["
                               + reading.toString() + "]. We will not insert this reading in the database." );*/
                    log.error("This reading [" + reading.toString()
                              + "] does not have set a reading ID which indicates an error in some of the attached monitors. We will not insert this reading in the database.");
                    continue;
                }
                String readingValue = reading.getValue();
                if (readingValue == null) {
                    log.error("Null value is passed for this reading [" + reading.toString()
                              + "]. We will not insert this reading in the database.");
                    continue;
                }
                statisticDbIds.append(readingDbId);
                statisticDbIds.append("_");

                statisticValues.append(readingValue);
                statisticValues.append("_");

                resultsAddeed++;

                if (statisticDbIds.length() > MAX_LENGTH_STATISTIC_IDS
                    || statisticValues.length() > MAX_LENGTH_STATISTIC_VALUES) {
                    // we have to send a chunk
                    statisticDbIds.setLength(statisticDbIds.length() - 1);
                    statisticValues.setLength(statisticValues.length() - 1);
                    dblog.insertSystemStatistcs(HostUtils.getLocalHostIP(),
                                                statisticDbIds.toString(),
                                                statisticValues.toString(),
                                                resultsLine.getTimestamp());

                    statisticDbIds.setLength(0);
                    statisticValues.setLength(0);
                }
            }

            if (statisticDbIds.length() > 0) {
                // send the last(or only) chunk
                statisticDbIds.setLength(statisticDbIds.length() - 1);
                statisticValues.setLength(statisticValues.length() - 1);

                dblog.insertSystemStatistcs(HostUtils.getLocalHostIP(),
                                            statisticDbIds.toString(),
                                            statisticValues.toString(),
                                            resultsLine.getTimestamp());
            }

            return resultsAddeed;
        }

        private void updateDatabaseRepository(
                                               String monitoredHost,
                                               List<ReadingBean> readings ) throws MonitoringException {

            try {
                ReadingsRepository.getInstance().updateDatabaseRepository(monitoredHost, readings);
            } catch (DatabaseAccessException e) {
                throw new MonitoringException("Could not update the logging database with new statistic definitions for "
                                              + monitoredHost, e);
            }
        }

    }
}
