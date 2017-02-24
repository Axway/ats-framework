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

public class Test_AllAtOncePattern extends BaseTest {

    @Test
    public void twoArgumentsConstructor() {

        AllAtOncePattern pattern = new AllAtOncePattern( 100, true );

        assertEquals( 100, pattern.getThreadCount() );
        assertEquals( true, pattern.isBlockUntilCompletion() );
        assertEquals( 1, pattern.getIterationCount() );
        assertEquals( 0L, pattern.getIntervalBetweenIterations() );
        assertEquals( -1L, pattern.getMinIntervalBetweenIterations() );
        assertEquals( -1L, pattern.getMaxIntervalBetweenIterations() );
    }

    @Test
    public void fourArgumentsConstructor() {

        AllAtOncePattern pattern = new AllAtOncePattern( 100, true, 20, 500 );

        assertEquals( 100, pattern.getThreadCount() );
        assertEquals( true, pattern.isBlockUntilCompletion() );
        assertEquals( 20, pattern.getIterationCount() );
        assertEquals( 500L, pattern.getIntervalBetweenIterations() );
        assertEquals( -1L, pattern.getMinIntervalBetweenIterations() );
        assertEquals( -1L, pattern.getMaxIntervalBetweenIterations() );
    }

    @Test
    public void fiveArgumentsConstructor() {

        AllAtOncePattern pattern = new AllAtOncePattern( 100, true, 20, 0, 5000 );

        assertEquals( 100, pattern.getThreadCount() );
        assertEquals( true, pattern.isBlockUntilCompletion() );
        assertEquals( 20, pattern.getIterationCount() );
        assertEquals( 0L, pattern.getIntervalBetweenIterations() );
        assertEquals( 0L, pattern.getMinIntervalBetweenIterations() );
        assertEquals( 5000L, pattern.getMaxIntervalBetweenIterations() );
    }

    @Test
    public void setBlockUntilCompletion() {

        AllAtOncePattern pattern = new AllAtOncePattern( 100, false, 20, 500 );

        assertFalse( pattern.isBlockUntilCompletion() );

        pattern.setBlockUntilCompletion( true );
        assertTrue( pattern.isBlockUntilCompletion() );
    }

    @Test(expected = IllegalArgumentException.class)
    public void setIntervalBetweenIterations_negative() {

        new AllAtOncePattern( 100, true, 10, -1 );
    }

    @Test(expected = IllegalArgumentException.class)
    public void setMinIntervalBetweenIterations_negative() {

        new AllAtOncePattern( 100, true, 10, -5, 5 );
    }

    @Test(expected = IllegalArgumentException.class)
    public void setMaxIntervalBetweenIterations_negative() {

        new AllAtOncePattern( 100, true, 10, 5, -5 );
    }

    @Test
    public void setMinIntervalBetweenIterationsBiggerThanMaxInterval() {

        AllAtOncePattern pattern = new AllAtOncePattern( 100, true, 10, 4000, 1000 );

        assertEquals( 1000L, pattern.getMinIntervalBetweenIterations() );
        assertEquals( 4000L, pattern.getMaxIntervalBetweenIterations() );
    }

    @Test
    public void distributeSeveralHosts() {

        AllAtOncePattern pattern = new AllAtOncePattern( 99, true, 20, 500 );
        List<ThreadingPattern> distributedPatterns = pattern.distribute( 3 );

        assertEquals( 3, distributedPatterns.size() );
        for( int i = 0; i < 3; i++ ) {
            AllAtOncePattern currentPattern = ( AllAtOncePattern ) distributedPatterns.get( i );

            assertEquals( 33, currentPattern.getThreadCount() );
            assertEquals( 20, currentPattern.getIterationCount() );
            assertEquals( 500L, currentPattern.getIntervalBetweenIterations() );
            assertEquals( true, currentPattern.isBlockUntilCompletion() );
        }
    }

    @Test
    public void distributeSeveralHostsUneven() {

        AllAtOncePattern pattern = new AllAtOncePattern( 100, true, 20, 500 );
        List<ThreadingPattern> distributedPatterns = pattern.distribute( 3 );

        assertEquals( 3, distributedPatterns.size() );
        for( int i = 0; i < 3; i++ ) {
            AllAtOncePattern currentPattern = ( AllAtOncePattern ) distributedPatterns.get( i );

            if( i != 1 ) {
                assertEquals( 33, currentPattern.getThreadCount() );
                assertEquals( 20, currentPattern.getIterationCount() );
                assertEquals( 500L, currentPattern.getIntervalBetweenIterations() );
                assertEquals( true, currentPattern.isBlockUntilCompletion() );
            } else{
                assertEquals( 34, currentPattern.getThreadCount() );
                assertEquals( 20, currentPattern.getIterationCount() );
                assertEquals( 500L, currentPattern.getIntervalBetweenIterations() );
                assertEquals( true, currentPattern.isBlockUntilCompletion() );
            } 
        }
    }

    @Test
    public void distributeOneHost() {

        AllAtOncePattern pattern = new AllAtOncePattern( 2, true, 20, 500 );
        List<ThreadingPattern> distributedPatterns = pattern.distribute( 3 );

        assertEquals( 1, distributedPatterns.size() );

        AllAtOncePattern currentPattern = ( AllAtOncePattern ) distributedPatterns.get( 0 );
        assertEquals( 2, currentPattern.getThreadCount() );
        assertEquals( 20, currentPattern.getIterationCount() );
        assertEquals( 500L, currentPattern.getIntervalBetweenIterations() );
        assertEquals( true, currentPattern.isBlockUntilCompletion() );
    }

    @Test
    public void toStringPositive() {

        AllAtOncePattern pattern1 = new AllAtOncePattern( 2, true, 20, 500 );
        assertEquals( "All at once - <number_threads> threads, 20 iterations with 500 ms interval",
                      pattern1.getPatternDescription() );

        AllAtOncePattern pattern2 = new AllAtOncePattern( 2, true, 20, 0 );
        assertEquals( "All at once - <number_threads> threads, 20 continuous iterations",
                      pattern2.getPatternDescription() );

        AllAtOncePattern pattern3 = new AllAtOncePattern( 2, true, 0, 500 );
        assertEquals( "All at once - <number_threads> threads", pattern3.getPatternDescription() );

    }
}
