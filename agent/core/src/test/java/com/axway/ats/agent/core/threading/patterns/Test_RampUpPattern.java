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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.axway.ats.agent.core.BaseTest;

public class Test_RampUpPattern extends BaseTest {

    @Test
    public void twoArgumentsConstructor() {

        RampUpPattern pattern = new RampUpPattern( 100, true );

        assertEquals( 100, pattern.getThreadCount() );
        assertEquals( true, pattern.isBlockUntilCompletion() );
        assertEquals( 1, pattern.getIterationCount() );
        assertEquals( 0L, pattern.getRampUpInterval() );
        assertEquals( 1, pattern.getThreadCountPerStep() );
        assertEquals( 0L, pattern.getIntervalBetweenIterations() );
        assertEquals( -1L, pattern.getMinIntervalBetweenIterations() );
        assertEquals( -1L, pattern.getMaxIntervalBetweenIterations() );
    }

    @Test
    public void fourArgumentsConstructor() {

        RampUpPattern pattern = new RampUpPattern( 100, true, 20, 500 );

        assertEquals( 100, pattern.getThreadCount() );
        assertEquals( true, pattern.isBlockUntilCompletion() );
        assertEquals( 20, pattern.getIterationCount() );
        assertEquals( 0L, pattern.getRampUpInterval() );
        assertEquals( 1, pattern.getThreadCountPerStep() );
        assertEquals( 500L, pattern.getIntervalBetweenIterations() );
        assertEquals( -1L, pattern.getMinIntervalBetweenIterations() );
        assertEquals( -1L, pattern.getMaxIntervalBetweenIterations() );
    }

    @Test
    public void sixArgumentsConstructor() {

        RampUpPattern pattern = new RampUpPattern( 100, true, 20, 500, 1000, 4 );

        assertEquals( 100, pattern.getThreadCount() );
        assertEquals( true, pattern.isBlockUntilCompletion() );
        assertEquals( 20, pattern.getIterationCount() );
        assertEquals( 1000L, pattern.getRampUpInterval() );
        assertEquals( 4, pattern.getThreadCountPerStep() );
        assertEquals( 500L, pattern.getIntervalBetweenIterations() );
        assertEquals( -1L, pattern.getMinIntervalBetweenIterations() );
        assertEquals( -1L, pattern.getMaxIntervalBetweenIterations() );
    }

    @Test
    public void sevenArgumentsConstructor() {

        RampUpPattern pattern = new RampUpPattern( 100, true, 20, 1000, 5000, 1000, 10 );

        assertEquals( 100, pattern.getThreadCount() );
        assertEquals( true, pattern.isBlockUntilCompletion() );
        assertEquals( 20, pattern.getIterationCount() );
        assertEquals( 1000L, pattern.getRampUpInterval() );
        assertEquals( 10, pattern.getThreadCountPerStep() );
        assertEquals( 0L, pattern.getIntervalBetweenIterations() );
        assertEquals( 1000L, pattern.getMinIntervalBetweenIterations() );
        assertEquals( 5000L, pattern.getMaxIntervalBetweenIterations() );
    }

    @Test(expected = IllegalArgumentException.class)
    public void sixArgumentsConstructorNegativeZeroThreadsPerStep() {

        new RampUpPattern( 100, true, 20, 500, 1000, 0 );
    }

    @Test(expected = IllegalArgumentException.class)
    public void sixArgumentsConstructorNegativeThreadsPerStepEqualToTotalThreads() {

        new RampUpPattern( 100, true, 20, 500, 1000, 100 );
    }

    @Test
    public void setBlockUntilCompletion() {

        RampUpPattern pattern = new RampUpPattern( 100, false, 20, 500, 1000, 5 );

        assertFalse( pattern.isBlockUntilCompletion() );

        pattern.setBlockUntilCompletion( true );
        assertTrue( pattern.isBlockUntilCompletion() );
    }

    @Test
    public void distributeSeveralHosts() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 99, true, 20, 500, 1000, 9 );
        List<ThreadingPattern> distributedPatterns = pattern.distribute( 3 );

        assertEquals( 3, distributedPatterns.size() );
        for( int i = 0; i < 3; i++ ) {
            RampUpPattern currentPattern = ( RampUpPattern ) distributedPatterns.get( i );

            assertEquals( 33, currentPattern.getThreadCount() );
            assertEquals( 20, currentPattern.getIterationCount() );
            assertEquals( 500L, currentPattern.getIntervalBetweenIterations() );
            assertEquals( true, currentPattern.isBlockUntilCompletion() );
            assertEquals( 1000L, currentPattern.getRampUpInterval() );
            assertEquals( 3, currentPattern.getThreadCountPerStep() );
        }
    }

    @Test
    public void distributeSeveralHostsUneven() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 100, true, 20, 500, 1000, 5 );
        List<ThreadingPattern> distributedPatterns = pattern.distribute( 3 );

        assertEquals( 3, distributedPatterns.size() );
        for( int i = 0; i < 3; i++ ) {
            RampUpPattern currentPattern = ( RampUpPattern ) distributedPatterns.get( i );

            if( i == 0 ) {
                assertEquals( 33, currentPattern.getThreadCount() );
                assertEquals( 20, currentPattern.getIterationCount() );
                assertEquals( 500L, currentPattern.getIntervalBetweenIterations() );
                assertEquals( true, currentPattern.isBlockUntilCompletion() );
                assertEquals( 1000L, currentPattern.getRampUpInterval() );
                assertEquals( 1, currentPattern.getThreadCountPerStep() );
            } else if( i == 1 ) {
                assertEquals( 34, currentPattern.getThreadCount() );
                assertEquals( 20, currentPattern.getIterationCount() );
                assertEquals( 500L, currentPattern.getIntervalBetweenIterations() );
                assertEquals( true, currentPattern.isBlockUntilCompletion() );
                assertEquals( 1000L, currentPattern.getRampUpInterval() );
                assertEquals( 2, currentPattern.getThreadCountPerStep() );
            } else {
                assertEquals( 33, currentPattern.getThreadCount() );
                assertEquals( 20, currentPattern.getIterationCount() );
                assertEquals( 500L, currentPattern.getIntervalBetweenIterations() );
                assertEquals( true, currentPattern.isBlockUntilCompletion() );
                assertEquals( 1000L, currentPattern.getRampUpInterval() );
                assertEquals( 2, currentPattern.getThreadCountPerStep() );
            }
        }
    }

    @Test
    public void distributeOneHost() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 2, true, 20, 500, 1000, 1 );
        List<ThreadingPattern> distributedPatterns = pattern.distribute( 3 );

        assertEquals( 1, distributedPatterns.size() );

        RampUpPattern currentPattern = ( RampUpPattern ) distributedPatterns.get( 0 );
        assertEquals( 2, currentPattern.getThreadCount() );
        assertEquals( 20, currentPattern.getIterationCount() );
        assertEquals( 500L, currentPattern.getIntervalBetweenIterations() );
        assertEquals( true, currentPattern.isBlockUntilCompletion() );
        assertEquals( 1000L, currentPattern.getRampUpInterval() );
        assertEquals( 1, currentPattern.getThreadCountPerStep() );
    }

    @Test(expected = IllegalArgumentException.class)
    public void numberOfThreadsPerStepLessThanTheNumberOfHosts() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 99, true, 20, 500, 1000, 2 );
        pattern.distribute( 3 );
    }

    @Test
    public void toStringPositive() {

        RampUpPattern pattern1 = new RampUpPattern( 2, true, 20, 500, 1000, 1 );
        assertEquals( "Ramp up - <number_threads> total threads, 1 threads every 1000 ms, 20 continuous iterations",
                      pattern1.getPatternDescription() );

        RampUpPattern pattern2 = new RampUpPattern( 2, true, 20, 0, 1000, 1 );
        assertEquals( "Ramp up - <number_threads> total threads, 1 threads every 1000 ms, 20 iterations with 0 ms interval",
                      pattern2.getPatternDescription() );

        RampUpPattern pattern3 = new RampUpPattern( 2, true, 0, 500, 1000, 1 );
        assertEquals( "Ramp up - <number_threads> total threads, 1 threads every 1000 ms",
                      pattern3.getPatternDescription() );
    }
}
