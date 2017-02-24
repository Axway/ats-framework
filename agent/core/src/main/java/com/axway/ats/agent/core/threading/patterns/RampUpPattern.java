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
import com.axway.ats.agent.core.threading.patterns.model.FixedInvocationsExecutionPattern;
import com.axway.ats.agent.core.threading.patterns.model.RampUpStartPattern;
import com.axway.ats.common.PublicAtsApi;

@PublicAtsApi
public final class RampUpPattern extends ThreadingPattern
        implements FixedInvocationsExecutionPattern, RampUpStartPattern {

    private int               iterationCount;
    private long              rampUpInterval;
    private int               threadCountPerStep;

    private static final long serialVersionUID = 1L;

    /**
     * Pattern used for executing several thread in a ramp-up fashion
     * 
     * @param threadCount the total number of threads to start
     * @param blockUntilCompletion block the main thread until all threads finish
     * @param iterationCount number of iterations that each thread will do
     * @param intervalBetweenIterations fixed interval between each iteration in milliseconds
     * @param rampUpInterval ramp up interval in milliseconds
     * @param threadCountPerStep the thread step for the ramp up (must be greater than 0)
     */
    @PublicAtsApi
    public RampUpPattern( int threadCount, boolean blockUntilCompletion, int iterationCount,
                          long intervalBetweenIterations, long rampUpInterval, int threadCountPerStep ) {

        super( threadCount, intervalBetweenIterations, -1, -1, blockUntilCompletion );

        this.iterationCount = iterationCount;
        this.rampUpInterval = rampUpInterval;

        //check the argument now
        if( threadCountPerStep <= 0 ) {
            throw new IllegalArgumentException( "The thread count per step must be a positive integer" );
        }
        if( threadCountPerStep >= threadCount ) {
            throw new IllegalArgumentException( "The thread count per step must be smaller than the total thread count" );
        }

        this.threadCountPerStep = threadCountPerStep;
    }

    /**
     * Pattern used for executing several thread in a ramp-up fashion
     * 
     * @param threadCount the total number of threads to start
     * @param blockUntilCompletion block the main thread until all threads finish
     * @param iterationCount number of iterations that each thread will do
     * @param minIntervalBetweenIterations minimum interval(in milliseconds) between the iterations  
     * @param maxIntervalBetweenIterations maximum interval(in milliseconds) between the iterations 
     * @param rampUpInterval ramp up interval in milliseconds
     * @param threadCountPerStep the thread step for the ramp up (must be greater than 0)
     */
    @PublicAtsApi
    public RampUpPattern( int threadCount, boolean blockUntilCompletion, int iterationCount,
                          long minIntervalBetweenIterations, long maxIntervalBetweenIterations,
                          long rampUpInterval, int threadCountPerStep ) {

        this( threadCount, blockUntilCompletion, iterationCount, 0, rampUpInterval, threadCountPerStep );

        setMinIntervalBetweenIterations( minIntervalBetweenIterations );
        setMaxIntervalBetweenIterations( maxIntervalBetweenIterations );
    }

    /**
     * Pattern used for executing several thread in a ramp-up fashion
     * 
     * @param threadCount the total number of threads to start
     * @param blockUntilCompletion block the main thread until all threads finish
     */
    @PublicAtsApi
    public RampUpPattern( int threadCount, boolean blockUntilCompletion ) {

        this( threadCount, blockUntilCompletion, 1, 0 );
    }

    /**
     * Pattern used for executing several thread in a ramp-up fashion
     * 
     * @param threadCount the total number of threads to start
     * @param blockUntilCompletion block the main thread until all threads finish
     * @param iterationCount number of iterations that each thread will do
     * @param intervalBetweenIterations fixed interval between each iteration in milliseconds
     */
    @PublicAtsApi
    public RampUpPattern( int threadCount, boolean blockUntilCompletion, int iterationCount,
                          long intervalBetweenIterations ) {

        super( threadCount, intervalBetweenIterations, -1, -1, blockUntilCompletion );

        this.iterationCount = iterationCount;
        this.rampUpInterval = 0;
        this.threadCountPerStep = 1;
    }

    @Override
    public int getIterationCount() {

        return iterationCount;
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

        String description = "Ramp up - " + NUMBER_THREADS_TOKEN + " total threads, " + threadCountPerStep
                             + " threads every " + rampUpInterval + " ms";
        if( iterationCount > 0 ) {
            description += ", " + iterationCount;
            if( intervalBetweenIterations > 0 ) {
                description += " continuous iterations";
            } else if( minIntervalBetweenIterations >= 0 ) {
                description += " iterations with " + minIntervalBetweenIterations + " to "
                               + maxIntervalBetweenIterations + " ms varying interval";
            } else {
                description += " iterations with " + intervalBetweenIterations + " ms interval";
            }
        }
        if( iterationTimeout > 0 ) {
            description += ", " + iterationTimeout + " secs iteration timeout";
        }
        if( queuePassRateInPercents > 0 ) {
            description += ", pass if " + queuePassRateInPercents + "% of the iterations pass";
        }
        return description;
    }

    private RampUpPattern newInstance( int calculatedThreadCount, int calculatedThreadCountPerStep ) {

        RampUpPattern pattern = new RampUpPattern( calculatedThreadCount, this.blockUntilCompletion,
                                                   this.iterationCount, this.intervalBetweenIterations,
                                                   this.rampUpInterval, calculatedThreadCountPerStep );
        pattern.setMinIntervalBetweenIterations( this.minIntervalBetweenIterations );
        pattern.setMaxIntervalBetweenIterations( this.maxIntervalBetweenIterations );
        pattern.timeFrame = this.timeFrame;
        pattern.executionsPerTimeFrame = this.executionsPerTimeFrame;
        pattern.iterationTimeout = this.iterationTimeout;
        return pattern;
    }

    @Override
    public List<ThreadingPattern> distribute( int numHosts ) throws IllegalArgumentException {

        List<ThreadingPattern> distributedPatterns = new ArrayList<ThreadingPattern>();
        if( threadCount < numHosts ) {
            log.warn( "We cannot distribute just " + threadCount + " threads on " + numHosts
                      + " hosts. So all work will be done by one host" );
            distributedPatterns.add( this );
        } else if( this.timeFrame > 0 && executionsPerTimeFrame < numHosts ) {
            log.warn( "We cannot distribute just " + executionsPerTimeFrame + " iterations per time frame on "
                      + numHosts + " hosts. So all work will be done by one host" );
            distributedPatterns.add( this );
        } else {
            // for each host - distribute the total number of threads
            int[] distributionValues = new EvenLoadDistributingUtils().getEvenLoad( threadCount, numHosts );
            // for each host - distribute the number of threads per step
            if( threadCountPerStep < numHosts ) {
                throw new IllegalArgumentException( "The thread count per step [" + threadCountPerStep
                                                    + "] must be at least as much as the number of agents ["
                                                    + numHosts + "]" );
            }
            int[] stepThreadCountDistributionValues = new EvenLoadDistributingUtils().getEvenLoad( threadCountPerStep,
                                                                                                   numHosts );
            for( int i = 0; i < numHosts; i++ ) {
                ThreadingPattern newThreadingPattern;
                newThreadingPattern = newInstance( distributionValues[i],
                                                   stepThreadCountDistributionValues[i] );
                distributedPatterns.add( newThreadingPattern );
            }
        }

        return distributedPatterns;
    }
}
