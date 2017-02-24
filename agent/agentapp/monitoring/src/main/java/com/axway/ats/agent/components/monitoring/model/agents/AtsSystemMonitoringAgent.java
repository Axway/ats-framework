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
package com.axway.ats.agent.components.monitoring.model.agents;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.axway.ats.agent.components.monitoring.model.jvmmonitor.AtsJvmMonitor;
import com.axway.ats.agent.components.monitoring.model.systemmonitor.AtsSystemMonitor;
import com.axway.ats.common.performance.monitor.PerformanceMonitor;
import com.axway.ats.common.performance.monitor.beans.BasicReadingBean;
import com.axway.ats.common.performance.monitor.beans.FullReadingBean;
import com.axway.ats.common.performance.monitor.beans.MonitorResults;
import com.axway.ats.core.utils.TimeUtils;

/**
 * A monitoring agent, it works on the MONITORED machine.
 */
public class AtsSystemMonitoringAgent extends AbstractMonitoringAgent {

    private static Logger            log                      = Logger.getLogger( AtsSystemMonitoringAgent.class );

    private static final int         MAX_NUMBER_LOGGED_ERRORS = 10;
    private static final String      CUSTOM_READING_PREFIX    = "[custom] ";

    private List<PerformanceMonitor> monitors;
    private MonitoringThread         monitoringThread;

    private List<MonitorResults>     collectedResults;
    private Map<String, Integer>     pollErrors;

    public AtsSystemMonitoringAgent( long startTimestamp, int pollInterval ) {

        // we must have a completely clean instance when this method return
        this.monitors = new ArrayList<PerformanceMonitor>();
        this.collectedResults = new ArrayList<MonitorResults>();
        this.pollErrors = new HashMap<String, Integer>();

        // set the new start timestamp and polling interval
        setStartTimestamp( startTimestamp );
        setPollInterval( pollInterval * 1000 ); // make it in seconds

        // if the test has ended without stopping the monitoring process
        // we now need to stop the monitoring process
        if( monitors.size() > 0 ) {
            log.warn( "There are some monitors running from before. We will try to deinitialize them now" );
            stopMonitoring();
        }
    }

    @Override
    public void startMonitoring() {

        monitoringThread = new MonitoringThread( getStartTimestamp(), pollInterval );
        monitoringThread.start();
    }

    @Override
    public void stopMonitoring() {

        log.info( "Stopping the monitor process" );
        if( monitoringThread == null ) {
            log.warn( "Cannot stop the monitoring thread as it is currently not "
                      + "running. The monitoring was either not started at all, "
                      + "or it was already stopped." );
            return;
        }
        monitoringThread.stopRunning();

        // wait for up to 2 times the polling interval until the thread exit out
        boolean threadStopped = false;
        for( int i = 0; i < 10 * this.pollInterval / 1000; i++ ) {
            threadStopped = monitoringThread.isStopped();
            if( threadStopped ) {
                log.info( "Successfully stoped the monitor process" );
                break;
            } else {
                try {
                    Thread.sleep( 200 );
                } catch( InterruptedException e ) {}
            }
        }

        if( !threadStopped ) {
            log.error( "Could not stop the monitor process in the regular way. We will try to abort the thread" );
            monitoringThread.interrupt();
        }

        monitoringThread = null;

        resetTheMonitoringAgent();
    }

    @Override
    public List<MonitorResults> getMonitoringResults() {

        List<MonitorResults> collectedResultsToPass;
        // create a copy of the collected results and send them to the Test Executor
        // do not allow the growth of this list right now
        synchronized( collectedResults ) {

            collectedResultsToPass = new ArrayList<MonitorResults>( collectedResults );

            // purge the collected data
            collectedResults.clear();
        }
        return collectedResultsToPass;
    }

    public void addMonitor( PerformanceMonitor monitor ) {

        log.info( "Attaching monitor: " + monitor.getDescription() );
        if( isMonitorAlreadyPresent( monitor ) ) {
            log.error( "Monitor already attached: " + monitor.getDescription() );
        } else {
            monitors.add( monitor );
        }
    }

    public void resetTheMonitoringAgent() {

        for( PerformanceMonitor monitor : monitors ) {
            log.info( "Deinitializing monitor: " + monitor.getDescription() );
            try {
                monitor.deinit();
            } catch( Exception e ) {
                log.error( "Error deinitializing monitor: " + monitor.getDescription(), e );
            }
        }

        this.monitors = new ArrayList<PerformanceMonitor>();
        this.pollErrors = new HashMap<String, Integer>();

        synchronized( collectedResults ) {
            this.collectedResults.clear();
        }
    }

    private boolean isMonitorAlreadyPresent( PerformanceMonitor thisMonitor ) {

        for( PerformanceMonitor monitor : monitors ) {
            if( monitor.getClass().getName().equals( thisMonitor.getClass().getName() ) ) {
                return true;
            }
        }

        return false;
    }

    enum MONITORING_THREAD_STATE {
        RUNNING, STOPPING, STOPPED
    }

    class MonitoringThread extends Thread {

        private Logger                  log = Logger.getLogger( MonitoringThread.class );

        private long                    currentTimestamp;
        private final int               pollInterval;

        // we keep the collected data for up to 1 hour,
        // if not collected yet - we discard this data
        private final int               collectedDataBufferLimit;

        private MONITORING_THREAD_STATE monitoringThreadState;

        MonitoringThread( long currentTimestamp, int pollInterval ) {

            this.monitoringThreadState = MONITORING_THREAD_STATE.RUNNING;

            this.currentTimestamp = currentTimestamp;
            this.pollInterval = pollInterval;
            this.collectedDataBufferLimit = 3600 / ( pollInterval / 1000 );

            log.debug( "Monitoring thread started at timestamp " + currentTimestamp );
        }

        @Override
        public void run() {

            log.info( "Started monitoring in intervals of " + pollInterval + " milliseconds" );
            try {
                boolean hasFailureInPreviousPoll = false;
                int lastPollDuration = 0;
                while( monitoringThreadState == MONITORING_THREAD_STATE.RUNNING ) {

                    // we don't measure, but we calculate the timestamp, so it is synchronized
                    // between all monitored machines
                    this.currentTimestamp += pollInterval;

                    int sleepTimeBeforeNextPoll = pollInterval - lastPollDuration;
                    if( sleepTimeBeforeNextPoll < 0 ) {
                        // we get here when the last poll took longer than the user provided poll interval
                        sleepTimeBeforeNextPoll = 0;
                        log.warn( "Last poll time took longer than the poll interval."
                                  + " Details: last poll duration " + lastPollDuration
                                  + " ms, poll interval is " + pollInterval
                                  + " ms. You should probably consider increasing the poll interval" );
                    } else {
                        if( sleepTimeBeforeNextPoll > pollInterval ) {
                            sleepTimeBeforeNextPoll = 0;
                            log.warn( "Polling duration is calculated as negative number (" + lastPollDuration
                                      + " ms). Possible reason is that system "
                                      + "time had been changed back. Poll will be issued now." );
                        }
                    }

                    Thread.sleep( sleepTimeBeforeNextPoll );

                    long startPollingTime = System.currentTimeMillis();

                    // poll for new data
                    List<MonitorResults> newResults = new ArrayList<MonitorResults>();
                    for( PerformanceMonitor monitor : monitors ) {
                        String monitorDescription = monitor.getDescription();
                        if( log.isDebugEnabled() ) {
                            log.debug( "Poll data for monitor: " + monitorDescription );
                        }
                        try {
                            if( !monitor.isInitialized() ) {

                                MonitorResults results = new MonitorResults( currentTimestamp,
                                                                             monitor.pollNewDataForFirstTime() );
                                if( ! ( monitor instanceof AtsSystemMonitor )
                                    && ! ( monitor instanceof AtsJvmMonitor ) ) {
                                    // this is a custom monitor, so will add the '[custom]' prefix
                                    for( BasicReadingBean reading : results.getReadings() ) {
                                        if( reading instanceof FullReadingBean ) {
                                            FullReadingBean newReading = ( FullReadingBean ) reading;
                                            newReading.setName( CUSTOM_READING_PREFIX
                                                                + newReading.getName() );
                                        }
                                    }
                                }
                                newResults.add( results );

                                // The monitor passed the 'first time poll', so we got the list of FullReadingBean.
                                // If we do not get here, an error has happened and we will call same method again the next time.
                                monitor.setInitialized();
                            } else {
                                newResults.add( new MonitorResults( currentTimestamp,
                                                                    monitor.pollNewData() ) );

                            }
                            if( hasFailureInPreviousPoll ) {
                                // reset the polling errors counter because the monitor is now OK
                                pollErrors.remove( monitorDescription );
                            }
                        } catch( Throwable th ) {
                            handlePollError( currentTimestamp, monitorDescription, th );
                        }
                    }

                    if( newResults.size() > 0 ) {
                        if( log.isDebugEnabled() ) {
                            log.debug( "new data: " + newResults.toString() );
                        }

                        synchronized( collectedResults ) {
                            collectedResults.addAll( newResults );
                        }
                    }

                    lastPollDuration = ( int ) ( System.currentTimeMillis() - startPollingTime );

                    if( collectedResults.size() > collectedDataBufferLimit ) {
                        log.warn( "The collected data was not requested for the last 1 hour, so we will discard it now" );

                        synchronized( collectedResults ) {
                            collectedResults.clear();
                        }
                    }

                    // check if there is a failure during the poll
                    hasFailureInPreviousPoll = newResults.size() == 0;
                }
            } catch( InterruptedException e ) {
                // we have been stopped by interrupting the monitoring thread
                log.error( "Monitoring thread was interrupted", e );
            } catch( Throwable th ) {
                log.error( "Monitoring is aborted due to unexpected error", th );
            } finally {
                this.monitoringThreadState = MONITORING_THREAD_STATE.STOPPED;
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

        private void handlePollError( long currentTimestamp, String monitorDescription, Throwable th ) {

            Integer monitorErrors = 0;
            if( pollErrors.containsKey( monitorDescription ) ) {
                monitorErrors = pollErrors.get( monitorDescription );
            }
            monitorErrors++;
            pollErrors.put( monitorDescription, monitorErrors );

            if( monitorErrors < MAX_NUMBER_LOGGED_ERRORS ) {
                log.error( "Error polling monitor '" + monitorDescription
                           + "'. All polled values from all monitors will be skipped for "
                           + TimeUtils.getFormattedDateTillMilliseconds( new Date( currentTimestamp ) )
                           + " timestamp", th );
            } else if( monitorErrors == MAX_NUMBER_LOGGED_ERRORS ) {
                log.error( "This is the " + MAX_NUMBER_LOGGED_ERRORS
                           + "th and last time we are logging polling error for monitor: "
                           + monitorDescription, th );
            }
        }
    }
}
