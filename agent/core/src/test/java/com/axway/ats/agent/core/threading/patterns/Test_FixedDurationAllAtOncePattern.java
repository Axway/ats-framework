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

public class Test_FixedDurationAllAtOncePattern extends BaseTest {

    @Test
    public void threeArgumentsConstructor() {

        FixedDurationAllAtOncePattern pattern = new FixedDurationAllAtOncePattern( 100, true, 60 );

        assertEquals( 100, pattern.getThreadCount() );
        assertEquals( true, pattern.isBlockUntilCompletion() );
        assertEquals( 60, pattern.getDuration() );
        assertEquals( 0L, pattern.getIntervalBetweenIterations() );
        assertEquals( -1L, pattern.getMinIntervalBetweenIterations() );
        assertEquals( -1L, pattern.getMaxIntervalBetweenIterations() );
    }

    @Test
    public void fourArgumentsConstructor() {

        FixedDurationAllAtOncePattern pattern = new FixedDurationAllAtOncePattern( 100, true, 20, 500 );

        assertEquals( 100, pattern.getThreadCount() );
        assertEquals( true, pattern.isBlockUntilCompletion() );
        assertEquals( 20, pattern.getDuration() );
        assertEquals( 500L, pattern.getIntervalBetweenIterations() );
        assertEquals( -1L, pattern.getMinIntervalBetweenIterations() );
        assertEquals( -1L, pattern.getMaxIntervalBetweenIterations() );
    }

    @Test
    public void fiveArgumentsConstructor() {

        FixedDurationAllAtOncePattern pattern = new FixedDurationAllAtOncePattern( 100, true, 20, 1000, 5000 );

        assertEquals( 100, pattern.getThreadCount() );
        assertEquals( true, pattern.isBlockUntilCompletion() );
        assertEquals( 20, pattern.getDuration() );
        assertEquals( 0L, pattern.getIntervalBetweenIterations() );
        assertEquals( 1000L, pattern.getMinIntervalBetweenIterations() );
        assertEquals( 5000L, pattern.getMaxIntervalBetweenIterations() );
    }

    @Test
    public void setBlockUntilCompletion() {

        FixedDurationAllAtOncePattern pattern = new FixedDurationAllAtOncePattern( 100, false, 20, 500 );

        assertFalse( pattern.isBlockUntilCompletion() );

        pattern.setBlockUntilCompletion( true );
        assertTrue( pattern.isBlockUntilCompletion() );
    }

    @Test
    public void distributeSeveralHosts() {

        FixedDurationAllAtOncePattern pattern = new FixedDurationAllAtOncePattern( 99, true, 20, 500 );
        List<ThreadingPattern> distributedPatterns = pattern.distribute( 3 );

        assertEquals( 3, distributedPatterns.size() );
        for( int i = 0; i < 3; i++ ) {
            FixedDurationAllAtOncePattern currentPattern = ( FixedDurationAllAtOncePattern ) distributedPatterns.get( i );

            assertEquals( 33, currentPattern.getThreadCount() );
            assertEquals( 20, currentPattern.getDuration() );
            assertEquals( 500L, currentPattern.getIntervalBetweenIterations() );
            assertEquals( true, currentPattern.isBlockUntilCompletion() );
        }
    }

    @Test
    public void distributeSeveralHostsUneven() {

        FixedDurationAllAtOncePattern pattern = new FixedDurationAllAtOncePattern( 100, true, 20, 500 );
        List<ThreadingPattern> distributedPatterns = pattern.distribute( 3 );

        assertEquals( 3, distributedPatterns.size() );
        for( int i = 0; i < 3; i++ ) {
            FixedDurationAllAtOncePattern currentPattern = ( FixedDurationAllAtOncePattern ) distributedPatterns.get( i );

            if( i != 1 ) {
                assertEquals( 33, currentPattern.getThreadCount() );
                assertEquals( 20, currentPattern.getDuration() );
                assertEquals( 500L, currentPattern.getIntervalBetweenIterations() );
                assertEquals( true, currentPattern.isBlockUntilCompletion() );
            } else {
                assertEquals( 34, currentPattern.getThreadCount() );
                assertEquals( 20, currentPattern.getDuration() );
                assertEquals( 500L, currentPattern.getIntervalBetweenIterations() );
                assertEquals( true, currentPattern.isBlockUntilCompletion() );
            }
        }
    }

    @Test
    public void distributeOneHost() {

        FixedDurationAllAtOncePattern pattern = new FixedDurationAllAtOncePattern( 2, true, 20, 500 );
        List<ThreadingPattern> distributedPatterns = pattern.distribute( 3 );

        assertEquals( 1, distributedPatterns.size() );

        FixedDurationAllAtOncePattern currentPattern = ( FixedDurationAllAtOncePattern ) distributedPatterns.get( 0 );
        assertEquals( 2, currentPattern.getThreadCount() );
        assertEquals( 20, currentPattern.getDuration() );
        assertEquals( 500L, currentPattern.getIntervalBetweenIterations() );
        assertEquals( true, currentPattern.isBlockUntilCompletion() );
    }

    @Test
    public void toStringPositive() {

        FixedDurationAllAtOncePattern pattern1 = new FixedDurationAllAtOncePattern( 100, true, 20, 500 );
        assertEquals( "Fixed duration all at once - <number_threads> threads in 20 seconds, 500 ms interval between iterations",
                      pattern1.getPatternDescription() );

        FixedDurationAllAtOncePattern pattern2 = new FixedDurationAllAtOncePattern( 100, true, 20, 0 );
        assertEquals( "Fixed duration all at once - <number_threads> threads in 20 seconds, no interval between iterations",
                      pattern2.getPatternDescription() );
    }
}
