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

import java.util.ArrayList;
import java.util.List;

import com.axway.ats.agent.core.model.EvenLoadDistributingUtils;
import com.axway.ats.agent.core.threading.patterns.model.FixedDurationExecutionPattern;
import com.axway.ats.agent.core.threading.patterns.model.RampUpStartPattern;
import com.axway.ats.common.PublicAtsApi;

@PublicAtsApi
public final class FixedDurationRampUpPattern extends ThreadingPattern
        implements RampUpStartPattern, FixedDurationExecutionPattern {

    private int               duration;
    private long              rampUpInterval;
    private int               threadCountPerStep;

    private static final long serialVersionUID = 1L;

    /**
     * Pattern used for executing several thread in a ramp-up fashion
     * 
     * @param threadCount the total number of threads to start
     * @param blockUntilCompletion block the main thread until all threads finish
     * @param duration the duration time in seconds
     * @param intervalBetweenIterations fixed interval between each iteration in milliseconds
     * @param rampUpInterval ramp up interval in milliseconds
     * @param threadCountPerStep the thread step for the ramp up (must be greater than 0)
     */
    @PublicAtsApi
    public FixedDurationRampUpPattern( int threadCount, boolean blockUntilCompletion, int duration,
                                       long intervalBetweenIterations, long rampUpInterval,
                                       int threadCountPerStep ) {

        super(threadCount, intervalBetweenIterations, -1, -1, blockUntilCompletion);

        this.duration = duration;

        this.rampUpInterval = rampUpInterval;

        //check the argument now
        if (threadCountPerStep <= 0) {
            throw new IllegalArgumentException("The thread count per step must be a positive integer");
        }
        if (threadCountPerStep >= threadCount) {
            throw new IllegalArgumentException("The thread count per step must be smaller than the total thread count");
        }

        this.threadCountPerStep = threadCountPerStep;
    }

    /**
     * Pattern used for executing several thread in a ramp-up fashion
     * 
     * @param threadCount the total number of threads to start
     * @param blockUntilCompletion block the main thread until all threads finish
     * @param duration the duration time in seconds
     * @param intervalBetweenIterations fixed interval between each iteration in milliseconds
     * @param rampUpInterval ramp up interval in milliseconds
     * @param threadCountPerStep the thread step for the ramp up (must be greater than 0)
     */
    @PublicAtsApi
    public FixedDurationRampUpPattern( int threadCount, boolean blockUntilCompletion, int duration,
                                       long minIntervalBetweenIterations, long maxIntervalBetweenIterations,
                                       long rampUpInterval, int threadCountPerStep ) {

        this(threadCount, blockUntilCompletion, duration, 0, rampUpInterval, threadCountPerStep);

        setMinIntervalBetweenIterations(minIntervalBetweenIterations);
        setMaxIntervalBetweenIterations(maxIntervalBetweenIterations);
    }

    /**
     * Pattern used for executing several thread in a ramp-up fashion
     * 
     * @param threadCount the total number of threads to start
     * @param blockUntilCompletion block the main thread until all threads finish
     * @param duration the duration time in seconds
     * @param intervalBetweenIterations fixed interval between each iteration in milliseconds
     */
    @PublicAtsApi
    public FixedDurationRampUpPattern( int threadCount, boolean blockUntilCompletion, int duration,
                                       long intervalBetweenIterations ) {

        super(threadCount, intervalBetweenIterations, -1, -1, blockUntilCompletion);

        this.duration = duration;
        this.rampUpInterval = 0;
        this.threadCountPerStep = 1;
    }

    /**
     * Pattern used for executing several thread in a ramp-up fashion
     * 
     * @param threadCount the total number of threads to start
     * @param blockUntilCompletion block the main thread until all threads finish
     * @param duration the duration time in seconds
     */
    @PublicAtsApi
    public FixedDurationRampUpPattern( int threadCount, boolean blockUntilCompletion, int duration ) {

        this(threadCount, blockUntilCompletion, duration, 0);
    }

    @Override
    public int getDuration() {

        return duration;
    }

    @Override
    public long getRampUpInterval() {

        return rampUpInterval;
    }

    @Override
    public int getThreadCountPerStep() {

        return threadCountPerStep;
    }

    @Override
    public String getPatternDescription() {

        String description = "Fixed duration ramp up - " + NUMBER_THREADS_TOKEN + " total threads in "
                             + duration + " seconds, " + threadCountPerStep + " threads every "
                             + rampUpInterval + " ms, ";
        if (intervalBetweenIterations > 0) {
            description += intervalBetweenIterations + " ms interval between iterations";
        } else if (minIntervalBetweenIterations >= 0) {
            description += minIntervalBetweenIterations + " to " + maxIntervalBetweenIterations
                           + " ms varying interval";
        } else {
            description += "no interval between iterations";
        }
        if (iterationTimeout > 0) {
            description += ", " + iterationTimeout + " secs iteration timeout";
        }
        if (queuePassRateInPercents > 0) {
            description += ", pass if " + queuePassRateInPercents + "% of the iterations pass";
        }
        return description;
    }

    private FixedDurationRampUpPattern newInstance( int calculatedThreadCount,
                                                    int calculatedThreadCountPerStep ) {

        FixedDurationRampUpPattern pattern = new FixedDurationRampUpPattern(calculatedThreadCount,
                                                                            this.blockUntilCompletion,
                                                                            this.duration,
                                                                            this.intervalBetweenIterations,
                                                                            this.rampUpInterval,
                                                                            calculatedThreadCountPerStep);
        pattern.setMinIntervalBetweenIterations(this.minIntervalBetweenIterations);
        pattern.setMaxIntervalBetweenIterations(this.maxIntervalBetweenIterations);
        pattern.timeFrame = this.timeFrame;
        pattern.executionsPerTimeFrame = this.executionsPerTimeFrame;
        pattern.iterationTimeout = this.iterationTimeout;
        return pattern;
    }

    @Override
    public List<ThreadingPattern> distribute( int numHosts ) {

        List<ThreadingPattern> distributedPatterns = new ArrayList<ThreadingPattern>();
        if (threadCount < numHosts) {
            log.warn("We cannot distribute just " + threadCount + " threads on " + numHosts
                     + " hosts. So all work will be done by one host");
            distributedPatterns.add(this);
        } else if (this.timeFrame > 0 && executionsPerTimeFrame < numHosts) {
            log.warn("We cannot distribute just " + executionsPerTimeFrame + " iterations per time frame on "
                     + numHosts + " hosts. So all work will be done by one host");
            distributedPatterns.add(this);
        } else {
            // for each host - distribute the total number of threads
            int[] threadCountDistributionValues = new EvenLoadDistributingUtils().getEvenLoad(threadCount,
                                                                                              numHosts);
            // for each host - distribute the number of threads per step
            if (threadCountPerStep < numHosts) {
                throw new IllegalArgumentException("The thread count per step [" + threadCountPerStep
                                                   + "] must be at least as much as the number of agents ["
                                                   + numHosts + "]");
            }
            int[] stepThreadCountDistributionValues = new EvenLoadDistributingUtils().getEvenLoad(threadCountPerStep,
                                                                                                  numHosts);
            for (int i = 0; i < numHosts; i++) {
                distributedPatterns.add(newInstance(threadCountDistributionValues[i],
                                                    stepThreadCountDistributionValues[i]));
            }
        }

        return distributedPatterns;
    }
}
