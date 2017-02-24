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

public class Test_FixedDurationRampUpPattern extends BaseTest {

    @Test
    public void threeArgumentsConstructor() {

        FixedDurationRampUpPattern pattern = new FixedDurationRampUpPattern( 100, true, 63 );

        assertEquals( 100, pattern.getThreadCount() );
        assertEquals( true, pattern.isBlockUntilCompletion() );
        assertEquals( 63, pattern.getDuration() );
        assertEquals( 0L, pattern.getRampUpInterval() );
        assertEquals( 1, pattern.getThreadCountPerStep() );
        assertEquals( 0L, pattern.getIntervalBetweenIterations() );
        assertEquals( -1L, pattern.getMinIntervalBetweenIterations() );
        assertEquals( -1L, pattern.getMaxIntervalBetweenIterations() );
    }

    @Test
    public void fourArgumentsConstructor() {

        FixedDurationRampUpPattern pattern = new FixedDurationRampUpPattern( 100, true, 20, 500 );

        assertEquals( 100, pattern.getThreadCount() );
        assertEquals( true, pattern.isBlockUntilCompletion() );
        assertEquals( 20, pattern.getDuration() );
        assertEquals( 0L, pattern.getRampUpInterval() );
        assertEquals( 1, pattern.getThreadCountPerStep() );
        assertEquals( 500L, pattern.getIntervalBetweenIterations() );
        assertEquals( -1L, pattern.getMinIntervalBetweenIterations() );
        assertEquals( -1L, pattern.getMaxIntervalBetweenIterations() );
    }

    @Test
    public void sevenArgumentsConstructor() {

        FixedDurationRampUpPattern pattern = new FixedDurationRampUpPattern( 100,
                                                                             true,
                                                                             20,
                                                                             1000,
                                                                             5000,
                                                                             1000,
                                                                             10 );

        assertEquals( 100, pattern.getThreadCount() );
        assertEquals( true, pattern.isBlockUntilCompletion() );
        assertEquals( 20, pattern.getDuration() );
        assertEquals( 1000L, pattern.getRampUpInterval() );
        assertEquals( 10, pattern.getThreadCountPerStep() );
        assertEquals( 0L, pattern.getIntervalBetweenIterations() );
        assertEquals( 1000L, pattern.getMinIntervalBetweenIterations() );
        assertEquals( 5000L, pattern.getMaxIntervalBetweenIterations() );
    }

    @Test
    public void sixArgumentsConstructor() {

        FixedDurationRampUpPattern pattern = new FixedDurationRampUpPattern( 100, true, 20, 500, 1000, 4 );

        assertEquals( 100, pattern.getThreadCount() );
        assertEquals( true, pattern.isBlockUntilCompletion() );
        assertEquals( 20, pattern.getDuration() );
        assertEquals( 500L, pattern.getIntervalBetweenIterations() );
        assertEquals( 1000L, pattern.getRampUpInterval() );
        assertEquals( 4, pattern.getThreadCountPerStep() );
    }

    @Test(expected = IllegalArgumentException.class)
    public void sixArgumentsConstructorNegativeZeroThreadsPerStep() {

        new FixedDurationRampUpPattern( 100, true, 20, 500, 1000, 0 );
    }

    @Test(expected = IllegalArgumentException.class)
    public void sixArgumentsConstructorNegativeThreadsPerStepEqualToTotalThreads() {

        new FixedDurationRampUpPattern( 100, true, 20, 500, 1000, 100 );
    }

    @Test
    public void setBlockUntilCompletion() {

        FixedDurationRampUpPattern pattern = new FixedDurationRampUpPattern( 100, false, 20, 500, 1000, 5 );

        assertFalse( pattern.isBlockUntilCompletion() );

        pattern.setBlockUntilCompletion( true );
        assertTrue( pattern.isBlockUntilCompletion() );
    }

    @Test
    public void distributeSeveralHosts() {

        FixedDurationRampUpPattern pattern = new FixedDurationRampUpPattern( 99, true, 20, 500, 1000, 9 );
        List<ThreadingPattern> distributedPatterns = pattern.distribute( 3 );

        assertEquals( 3, distributedPatterns.size() );
        for( int i = 0; i < 3; i++ ) {
            FixedDurationRampUpPattern currentPattern = ( FixedDurationRampUpPattern ) distributedPatterns.get( i );

            assertEquals( 33, currentPattern.getThreadCount() );
            assertEquals( 20, currentPattern.getDuration() );
            assertEquals( 500L, currentPattern.getIntervalBetweenIterations() );
            assertEquals( true, currentPattern.isBlockUntilCompletion() );
            assertEquals( 1000L, currentPattern.getRampUpInterval() );
            assertEquals( 3, currentPattern.getThreadCountPerStep() );
        }
    }

    @Test
    public void distributeSeveralHostsUneven() {

        FixedDurationRampUpPattern pattern = new FixedDurationRampUpPattern( 100, true, 20, 500, 1000, 5 );
        List<ThreadingPattern> distributedPatterns = pattern.distribute( 3 );

        assertEquals( 3, distributedPatterns.size() );
        for( int i = 0; i < 3; i++ ) {
            FixedDurationRampUpPattern currentPattern = ( FixedDurationRampUpPattern ) distributedPatterns.get( i );

            if( i == 0 ) {
                assertEquals( 33, currentPattern.getThreadCount() );
                assertEquals( 20, currentPattern.getDuration() );
                assertEquals( 500L, currentPattern.getIntervalBetweenIterations() );
                assertEquals( true, currentPattern.isBlockUntilCompletion() );
                assertEquals( 1000L, currentPattern.getRampUpInterval() );
                assertEquals( 1, currentPattern.getThreadCountPerStep() );
            } else if( i == 1 ) {
                assertEquals( 34, currentPattern.getThreadCount() );
                assertEquals( 20, currentPattern.getDuration() );
                assertEquals( 500L, currentPattern.getIntervalBetweenIterations() );
                assertEquals( true, currentPattern.isBlockUntilCompletion() );
                assertEquals( 1000L, currentPattern.getRampUpInterval() );
                assertEquals( 2, currentPattern.getThreadCountPerStep() );
            } else{
                assertEquals( 33, currentPattern.getThreadCount() );
                assertEquals( 20, currentPattern.getDuration() );
                assertEquals( 500L, currentPattern.getIntervalBetweenIterations() );
                assertEquals( true, currentPattern.isBlockUntilCompletion() );
                assertEquals( 1000L, currentPattern.getRampUpInterval() );
                assertEquals( 2, currentPattern.getThreadCountPerStep() );
            }
        }
    }

    @Test
    public void distributeOneHost() {

        FixedDurationRampUpPattern pattern = new FixedDurationRampUpPattern( 2, true, 20, 500, 1000, 1 );
        List<ThreadingPattern> distributedPatterns = pattern.distribute( 3 );

        assertEquals( 1, distributedPatterns.size() );

        FixedDurationRampUpPattern currentPattern = ( FixedDurationRampUpPattern ) distributedPatterns.get( 0 );
        assertEquals( 2, currentPattern.getThreadCount() );
        assertEquals( 20, currentPattern.getDuration() );
        assertEquals( 500L, currentPattern.getIntervalBetweenIterations() );
        assertEquals( true, currentPattern.isBlockUntilCompletion() );
        assertEquals( 1000L, currentPattern.getRampUpInterval() );
        assertEquals( 1, currentPattern.getThreadCountPerStep() );
    }

    @Test(expected = IllegalArgumentException.class)
    public void numberOfThreadsPerStepLessThanTheNumberOfHosts() throws Exception {

        FixedDurationRampUpPattern pattern = new FixedDurationRampUpPattern( 99, true, 20, 500, 1000, 2 );
        pattern.distribute( 3 );
    }

    @Test
    public void toStringPositive() {

        FixedDurationRampUpPattern pattern1 = new FixedDurationRampUpPattern( 2, true, 20, 500, 1000, 1 );
        assertEquals( "Fixed duration ramp up - <number_threads> total threads in 20 seconds, 1 threads every 1000 ms, 500 ms interval between iterations",
                      pattern1.getPatternDescription() );

        FixedDurationRampUpPattern pattern2 = new FixedDurationRampUpPattern( 2, true, 20, 0, 1000, 1 );
        assertEquals( "Fixed duration ramp up - <number_threads> total threads in 20 seconds, 1 threads every 1000 ms, no interval between iterations",
                      pattern2.getPatternDescription() );
    }
}
