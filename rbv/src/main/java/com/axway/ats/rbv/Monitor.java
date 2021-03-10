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
package com.axway.ats.rbv;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.rbv.executors.BasicExecutor;
import com.axway.ats.rbv.executors.Executor;
import com.axway.ats.rbv.executors.MetaExecutor;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.rules.Rule;
import com.axway.ats.rbv.storage.Matchable;

public class Monitor {

    private static final Logger log              = LogManager.getLogger(Monitor.class);

    private String              name;
    private Matchable           matchable;
    private final Executor      executor;
    private PollingParameters   pollingParameters;
    private int                 pollAttemptsLeft;
    private int                 pollAttemptsDone = 0;
    private MonitorListener     monitorListener;
    private boolean             expectedResult;
    private boolean             actualResult;

    private final boolean       endOnFirstMatch;
    private final boolean       endOnFirstFailure;

    private Timer               timer;
    private boolean             isActive;

    private String              lastRuleName;
    private String              lastError;

    private List<MetaData>      matchedMetaData;

    public Monitor( String name,
                    Matchable matchable,
                    Rule rootRule,
                    PollingParameters pollingParameters,
                    boolean expectedResult,
                    boolean endOnFirstMatch,
                    boolean endOnFirstFailure ) {

        this.name = name;
        this.matchable = matchable;
        this.executor = new MetaExecutor();
        this.executor.setRootRule(rootRule);
        this.pollingParameters = pollingParameters;
        this.expectedResult = expectedResult;
        this.endOnFirstMatch = endOnFirstMatch;
        this.endOnFirstFailure = endOnFirstFailure;
        this.isActive = false;

    }

    public Monitor( String name,
                    Matchable matchable,
                    Executor exec,
                    PollingParameters pollingParameters,
                    boolean expectedResult,
                    boolean endOnFirstMatch,
                    boolean endOnFirstFailure ) {

        this.name = name;
        this.matchable = matchable;
        this.executor = exec;
        this.pollingParameters = pollingParameters;
        this.expectedResult = expectedResult;
        this.endOnFirstMatch = endOnFirstMatch;
        this.endOnFirstFailure = endOnFirstFailure;
        this.isActive = false;

    }

    public synchronized void start(
                                    MonitorListener monitorListener ) throws RbvException {

        if (isActive) {
            throw new RbvException("Monitor '" + name + "' is already running");
        }

        logExpectedBehaviour();

        pollAttemptsLeft = pollingParameters.getPollAttempts();
        actualResult = !expectedResult;
        matchedMetaData = new ArrayList<MetaData>();

        this.monitorListener = monitorListener;

        matchable.open();

        timer = new Timer();
        timer.schedule(new MonitorTimerTask(),
                       pollingParameters.getInitialDelay(),
                       pollingParameters.getPollInterval());

        isActive = true;
    }

    private synchronized void end(
                                   boolean didErrorOccur, String errorMessage ) {

        this.lastError = errorMessage;

        if (executor instanceof BasicExecutor) {
            lastRuleName = ((BasicExecutor) executor).getLastRuleName();
        }

        if (isActive) {
            try {
                timer.cancel();
                matchable.close();

                //if we have at least one match then we succeeded
                actualResult = matchedMetaData.size() > 0;

            } catch (Exception e) {
                log.error("Exception while ending monitor '" + name + "'", e);
                actualResult = !expectedResult;

            } finally {
                isActive = false;

                if (!didErrorOccur) {
                    monitorListener.setFinished(name, (actualResult == expectedResult));
                } else {
                    monitorListener.setFinished(name, false);
                }
            }
        }
    }

    public synchronized void cancelExecution() throws RbvException {

        if (isActive) {

            timer.cancel();
            matchable.close();

            log.debug(name + " execution has been cancelled");

            isActive = false;
        }
    }

    public MetaData getFirstMatchedMetaData() {

        if (matchedMetaData.size() > 0) {
            return matchedMetaData.get(0);
        } else {
            //we either did not run the monitor or no matches
            return null;
        }
    }

    public List<MetaData> getAllMatchedMetaData() {

        return matchedMetaData;
    }

    public Matchable getMatchable() {

        return matchable;
    }

    public String getName() {

        return name;
    }

    public String getLastRuleName() {

        return lastRuleName;
    }

    public String getLastError() {

        return this.lastError;
    }

    private void logExpectedBehaviour() {

        StringBuilder msg = new StringBuilder();
        msg.append(name);
        msg.append(" expects to");
        if (!expectedResult) {
            msg.append(" not");
        }
        msg.append(" match ");
        msg.append(matchable.getDescription());
        msg.append("; Will");
        if (!endOnFirstMatch) {
            msg.append(" not");
        }
        msg.append(" end on first match; Will");
        if (!endOnFirstFailure) {
            msg.append(" not");
        }
        msg.append(" end on first failure;");
        log.info(msg);
    }

    private class MonitorTimerTask extends TimerTask {

        @Override
        public void run() {

            synchronized (Monitor.this) {
                try {
                    //first check if the monitor is active - it might have already been canceled
                    //in this case we don't want to execute anything else or we'll get exceptions
                    //because the matchable has already been closed
                    if (!isActive) {
                        return;
                    }

                    ++pollAttemptsDone;
                    log.info(name + " polling for " + matchable.getDescription() + ", attempts left: "
                             + pollAttemptsLeft);

                    List<MetaData> metaDataReceived;
                    if (endOnFirstMatch && endOnFirstFailure) {
                        metaDataReceived = matchable.getNewMetaData();
                    } else {
                        metaDataReceived = matchable.getAllMetaData();
                    }
                    log.info(name + " " + matchable.getMetaDataCounts());

                    String status = null;
                    if (expectedResult == true) {
                        // expecting to match data

                        // evaluate using the proper matchable executor
                        List<MetaData> meta = executor.evaluate(metaDataReceived);
                        if (meta != null && !meta.isEmpty()) {
                            matchedMetaData = meta;
                            if (endOnFirstMatch) {
                                end(false, "");
                                return;
                            }
                        } else if (endOnFirstFailure) {
                            status = "Expected to find " + matchable.getDescription()
                                     + " on all attempts, but did not find it on attempt number " + pollAttemptsDone;
                            end(true, status);
                        } else {
                            status = "Expected to find " + matchable.getDescription()
                                     + ", but did not find it";
                        }
                    } else {
                        // expecting to not match data

                        matchedMetaData = executor.evaluate(metaDataReceived);
                        if (matchedMetaData == null || matchedMetaData.isEmpty()) {
                            //nothing was matched
                            matchedMetaData = new ArrayList<MetaData>();

                            //if the expected monitor result is false
                            //this means that we don't expect any meta data to match
                            //so if endOnFirstMatch is true, we should end immediately
                            if (endOnFirstMatch) {
                                end(false, "");
                                return;
                            }
                        } else if (endOnFirstFailure) {
                            status = "Expected to not find " + matchable.getDescription()
                                     + " on all attempts, but found it on attempt number " + pollAttemptsDone;
                            end(true, status);
                        } else {
                            status = "Expected to not find " + matchable.getDescription()
                                     + ", but found it";
                        }
                    }

                    //decrement the counter
                    pollAttemptsLeft--;

                    //check if we should end
                    if (pollAttemptsLeft == 0) {
                        log.info(name + " no more attempts left - done");
                        end(false, status);
                    }
                } catch (Exception e) {
                    log.error("Exception during monitor execution", e);
                    end(true, "Exception during monitor execution: " + e.getMessage());
                }
            }
        }
    }
}
