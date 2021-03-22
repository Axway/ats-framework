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
package com.axway.ats.agent.core.threading.patterns;

import java.io.Serializable;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.agent.core.threading.patterns.model.StartPattern;
import com.axway.ats.common.PublicAtsApi;

public abstract class ThreadingPattern implements StartPattern, Serializable {

    private static final long     serialVersionUID             = 1L;
    protected int                 threadCount;
    protected boolean             blockUntilCompletion;
    protected int                 executionsPerTimeFrame;
    protected long                timeFrame;

    protected int                 iterationTimeout;

    protected boolean             useSynchronizedIterations;

    protected long                intervalBetweenIterations    = 0;
    protected long                minIntervalBetweenIterations = -1;
    protected long                maxIntervalBetweenIterations = -1;

    protected float               queuePassRateInPercents      = 0.0F;

    protected transient Logger    log;

    /*
     * When start actions into more than 1 agent at the same time,
     * we enter in the DB the threading pattern which is the same for all agents, 
     * but the number of threads we have here is the number for 1 agent only.
     * So we put this token in the DB and replace it with the actual number of threads
     * for all agents before showing the info to the user
     */
    protected final static String NUMBER_THREADS_TOKEN         = "<number_threads>";

    public ThreadingPattern( int threadCount, long intervalBetweenIterations,
                             long minIntervalBetweenIterations, long maxIntervalBetweenIterations,
                             boolean blockUntilCompletion ) {

        if (threadCount <= 0) {
            throw new IllegalArgumentException(threadCount
                                               + " is non-positive integer and is not accepted as thread count for threading pattern");
        } else {
            this.threadCount = threadCount;
        }
        this.blockUntilCompletion = blockUntilCompletion;

        if (intervalBetweenIterations < 0) {
            throw new IllegalArgumentException(intervalBetweenIterations
                                               + " ms is not a valid interval between queue iterations");
        }
        if (minIntervalBetweenIterations < 0 && minIntervalBetweenIterations != -1) {
            throw new IllegalArgumentException(minIntervalBetweenIterations
                                               + " ms is not a valid min interval between queue iterations");
        }
        if (maxIntervalBetweenIterations < 0 && maxIntervalBetweenIterations != -1) {
            throw new IllegalArgumentException(maxIntervalBetweenIterations
                                               + " ms is not a valid max interval between queue iterations");
        }

        this.intervalBetweenIterations = intervalBetweenIterations;

        this.log = LogManager.getLogger(this.getClass());

        if (minIntervalBetweenIterations != -1) {
            // user specified a varying interval
            if (minIntervalBetweenIterations > maxIntervalBetweenIterations) {
                this.log.warn("We will switch the provided minimum '" + minIntervalBetweenIterations
                              + " and maximum '" + maxIntervalBetweenIterations
                              + "' intervals between the iterations");
                this.minIntervalBetweenIterations = maxIntervalBetweenIterations;
                this.maxIntervalBetweenIterations = minIntervalBetweenIterations;
            } else if (minIntervalBetweenIterations == maxIntervalBetweenIterations) {
                // we will work as if user specified a fixed interval
                this.log.warn("You have provided the same minimum and maximum interval between iterations: "
                              + maxIntervalBetweenIterations);

                this.intervalBetweenIterations = minIntervalBetweenIterations;
                this.minIntervalBetweenIterations = -1;
                this.maxIntervalBetweenIterations = -1;
            } else {
                this.minIntervalBetweenIterations = minIntervalBetweenIterations;
                this.maxIntervalBetweenIterations = maxIntervalBetweenIterations;
            }
        }
    }

    @Override
    public int getThreadCount() {

        return threadCount;
    }

    @Override
    public boolean isBlockUntilCompletion() {

        return blockUntilCompletion;
    }

    @Override
    public void setBlockUntilCompletion( boolean blockUntilCompletion ) {

        this.blockUntilCompletion = blockUntilCompletion;
    }

    /**
     *
     * @return the number of iterations per given time frame
     */
    public int getExecutionsPerTimeFrame() {

        return executionsPerTimeFrame;
    }

    /**
     *
     * @return the time frame in seconds for executing a number of iterations
     */
    public long getTimeFrame() {

        return timeFrame;
    }

    /**
     * @return the iteration timeout in seconds
     */
    @Override
    public int getIterationTimeout() {

        return iterationTimeout;
    }

    /**
     * Set iteration timeout
     * 
     * @param iterationTimeout the iteration timeout in seconds
     */
    public void setIterationTimeout( int iterationTimeout ) {

        this.iterationTimeout = iterationTimeout;
    }

    public long getIntervalBetweenIterations() {

        return intervalBetweenIterations;
    }

    public long getMinIntervalBetweenIterations() {

        return minIntervalBetweenIterations;
    }

    public void setMinIntervalBetweenIterations( long minIntervalBetweenIterations ) {

        this.minIntervalBetweenIterations = minIntervalBetweenIterations;
    }

    public long getMaxIntervalBetweenIterations() {

        return maxIntervalBetweenIterations;
    }

    public void setMaxIntervalBetweenIterations( long maxIntervalBetweenIterations ) {

        this.maxIntervalBetweenIterations = maxIntervalBetweenIterations;
    }

    public float getQueuePassRate() {

        return queuePassRateInPercents;
    }

    /**
     * Set queue pass rate in percents. Queue passes if at least specific percentage of actions pass relative to all invoked actions.
     * @param queuePassRateInPercents float number between 0.0F (pass always, default value) and 100.0F (pass only if all actions pass)
     */
    public void setQueuePassRate( float queuePassRateInPercents ) {

        if (queuePassRateInPercents < 0.0 || queuePassRateInPercents > 100.0) {
            throw new IllegalArgumentException("The provided queue pass rate " + queuePassRateInPercents
                                               + " is invalid. It must be a number between 0 and 100");
        }

        this.queuePassRateInPercents = queuePassRateInPercents;
    }

    /**
    * In some cases you do not want to load the tested application on the maximum.<br><br>
    * 
    * ATS cannot make your actions execute slower, but using this method it is easy
    * to specify how many iterations will be executed for a period of time.
    * 
    * 
    * @param timeFrame the time in seconds for executing a number of iterations
    * @param executionsPerTimeFrame the number of iterations all threads together 
    * will execute during the timeFrame period. After the number of executions is
    * achieved, the threads will wait for the next time frame.
    */
    @PublicAtsApi
    public void setExecutionSpeed( long timeFrame, int executionsPerTimeFrame ) {

        if (executionsPerTimeFrame < 1) {
            throw new IllegalArgumentException("Can't distribute " + executionsPerTimeFrame
                                               + " iterations per thread.");
        }

        if (timeFrame < 1) {
            throw new IllegalArgumentException(timeFrame + " seconds is not a valid time frame.");
        }

        this.executionsPerTimeFrame = executionsPerTimeFrame;
        this.timeFrame = timeFrame;
    }

    /**
     * Distribute this threading pattern across a number of hosts
     *
     * @param maxNumHosts the max number of hosts to calculate for - usually this number will be
     * honored, excluding cases where the number of threads is one
     * @return list of threading patterns for each host
     *
     * @throws IllegalArgumentException
     */
    public abstract List<ThreadingPattern> distribute( int maxNumHosts ) throws IllegalArgumentException;

    public abstract String getPatternDescription();

    public boolean isUseSynchronizedIterations() {

        return useSynchronizedIterations;
    }
}
