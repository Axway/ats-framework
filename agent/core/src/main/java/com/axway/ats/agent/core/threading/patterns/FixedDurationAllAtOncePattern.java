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
import com.axway.ats.common.PublicAtsApi;

@PublicAtsApi
public final class FixedDurationAllAtOncePattern extends ThreadingPattern
        implements FixedDurationExecutionPattern {

    private int               duration;

    private static final long serialVersionUID = 1L;

    /**
     * Pattern used to start execution in several threads simultaneously - each thread will execute
     * the actions until the duration specified has passed
     * 
     * @param threadCount number of threads
     * @param blockUntilCompletion block the execution on the client until all threads finish
     * @param duration the duration time in seconds
     * @param intervalBetweenIterations fixed interval between the iterations in milliseconds
     */
    @PublicAtsApi
    public FixedDurationAllAtOncePattern( int threadCount, boolean blockUntilCompletion, int duration,
                                          long intervalBetweenIterations ) {

        super( threadCount, intervalBetweenIterations, -1, -1, blockUntilCompletion );

        this.duration = duration;
    }

    /**
     * Pattern used to start execution in several threads simultaneously - each thread will execute
     * the actions until the duration specified has passed
     * 
     * @param threadCount number of threads
     * @param blockUntilCompletion block the execution on the client until all threads finish
     * @param duration the duration time in seconds
     * @param minIntervalBetweenIterations minimum interval(in milliseconds) between the iterations
     * @param maxIntervalBetweenIterations maximum interval(in milliseconds) between the iterations
     */
    @PublicAtsApi
    public FixedDurationAllAtOncePattern( int threadCount, boolean blockUntilCompletion, int duration,
                                          long minIntervalBetweenIterations,
                                          long maxIntervalBetweenIterations ) {

        super( threadCount, 0, minIntervalBetweenIterations, maxIntervalBetweenIterations,
               blockUntilCompletion );

        this.duration = duration;
    }

    /**
     * Pattern used to start execution in several threads simultaneously - each thread will execute
     * the actions until the duration specified has passed - there will be no delay between iterations
     * 
     * @param threadCount number of threads
     * @param blockUntilCompletion block the execution on the client until all threads finish
     * @param duration the duration time in seconds
     */
    @PublicAtsApi
    public FixedDurationAllAtOncePattern( int threadCount, boolean blockUntilCompletion, int duration ) {

        super( threadCount, 0, -1, -1, blockUntilCompletion );

        this.duration = duration;
    }

    /**
     * @return the duration for the whole execution in seconds
     */
    @Override
    public int getDuration() {

        return duration;
    }

    /**
     * Whether the executed iterations between all threads will start at the same moment
     * @param useSynchronizedIterations
     */
    @PublicAtsApi
    public void setUseSynchronizedIterations( boolean useSynchronizedIterations ) {

        this.useSynchronizedIterations = useSynchronizedIterations;
    }

    @Override
    public String getPatternDescription() {

        String description = "Fixed duration all at once - " + NUMBER_THREADS_TOKEN + " threads in "
                             + duration + " seconds, ";
        if( intervalBetweenIterations > 0 ) {
            description += intervalBetweenIterations + " ms interval between iterations";
        } else if( minIntervalBetweenIterations >= 0 ) {
            description += minIntervalBetweenIterations + " to " + maxIntervalBetweenIterations
                           + " ms varying interval";
        } else {
            description += "no interval between iterations";
        }
        if( iterationTimeout > 0 ) {
            description += ", " + iterationTimeout + " secs iteration timeout";
        }
        if( useSynchronizedIterations ) {
            description += ", running synchronized iterations";
        }
        if( timeFrame > 0 ) {
            description += ", max " + executionsPerTimeFrame + " total iterations per " + timeFrame + " secs";
        }
        if( queuePassRateInPercents > 0 ) {
            description += ", pass if " + queuePassRateInPercents + "% of the iterations pass";
        }
        return description;
    }

    private FixedDurationAllAtOncePattern newInstance( int calculatedThreadCount,
                                                       int calculatedExecutionsPerTimeFrame ) {

        FixedDurationAllAtOncePattern pattern = new FixedDurationAllAtOncePattern( calculatedThreadCount,
                                                                                   this.blockUntilCompletion,
                                                                                   this.duration,
                                                                                   this.intervalBetweenIterations );
        pattern.setMinIntervalBetweenIterations( this.minIntervalBetweenIterations );
        pattern.setMaxIntervalBetweenIterations( this.maxIntervalBetweenIterations );
        pattern.timeFrame = this.timeFrame;
        pattern.executionsPerTimeFrame = calculatedExecutionsPerTimeFrame;
        pattern.iterationTimeout = this.iterationTimeout;
        return pattern;
    }

    @Override
    public List<ThreadingPattern> distribute( int numHosts ) {

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
            int[] threadCountValues = new EvenLoadDistributingUtils().getEvenLoad( threadCount, numHosts );
            // for each host - distribute the number of executions per time frame
            int[] executionsPerTimeFrameDistValues = new EvenLoadDistributingUtils().getEvenLoad( executionsPerTimeFrame,
                                                                                                  numHosts );
            for( int i = 0; i < numHosts; i++ ) {
                FixedDurationAllAtOncePattern newThreadingPattern;
                newThreadingPattern = newInstance( threadCountValues[i],
                                                   executionsPerTimeFrameDistValues[i] );
                newThreadingPattern.setUseSynchronizedIterations( this.isUseSynchronizedIterations() );
                distributedPatterns.add( newThreadingPattern );
            }
        }

        return distributedPatterns;
    }
}
