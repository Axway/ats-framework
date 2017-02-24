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
package com.axway.ats.agent.core.threading;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.axway.ats.agent.core.action.ActionRequest;
import com.axway.ats.agent.core.threading.data.ParameterDataProvider;
import com.axway.ats.agent.core.threading.exceptions.ThreadingPatternNotSupportedException;
import com.axway.ats.agent.core.threading.patterns.AllAtOncePattern;
import com.axway.ats.agent.core.threading.patterns.FixedDurationAllAtOncePattern;
import com.axway.ats.agent.core.threading.patterns.FixedDurationRampUpPattern;
import com.axway.ats.agent.core.threading.patterns.RampUpPattern;
import com.axway.ats.agent.core.threading.patterns.ThreadingPattern;

public class Test_LoadQueueFactory {

    @Test
    public void createQueueAllAtOncePatternPositive() throws Exception {

        AllAtOncePattern pattern = new AllAtOncePattern( 100, false );

        QueueLoader loader = LoadQueueFactory.createLoadQueue( "test", new ArrayList<ActionRequest>(),
                                                               pattern,
                                                               new ArrayList<ParameterDataProvider>(), null );

        assertEquals( RampUpQueueLoader.class, loader.getClass() );
    }

    @Test
    public void createQueueRampUpPatternPositive() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 100, false );

        QueueLoader loader = LoadQueueFactory.createLoadQueue( "test", new ArrayList<ActionRequest>(),
                                                               pattern,
                                                               new ArrayList<ParameterDataProvider>(), null );

        assertEquals( RampUpQueueLoader.class, loader.getClass() );
    }

    @Test
    public void createQueueFixedDurationAllAtOncePatternPositive() throws Exception {

        FixedDurationAllAtOncePattern pattern = new FixedDurationAllAtOncePattern( 100, false, 30 );

        QueueLoader loader = LoadQueueFactory.createLoadQueue( "test", new ArrayList<ActionRequest>(),
                                                               pattern,
                                                               new ArrayList<ParameterDataProvider>(), null );

        assertEquals( RampUpQueueLoader.class, loader.getClass() );
    }

    @Test
    public void createQueueFixedDurationRampUpPatternPositive() throws Exception {

        FixedDurationRampUpPattern pattern = new FixedDurationRampUpPattern( 100, false, 3600 );

        QueueLoader loader = LoadQueueFactory.createLoadQueue( "test", new ArrayList<ActionRequest>(),
                                                               pattern,
                                                               new ArrayList<ParameterDataProvider>(), null );

        assertEquals( RampUpQueueLoader.class, loader.getClass() );
    }

    @Test(expected = ThreadingPatternNotSupportedException.class)
    public void createQueueNegativePatternNotSupported() throws Exception {

        QueueLoader loader = LoadQueueFactory.createLoadQueue( "test", new ArrayList<ActionRequest>(),
                                                               new UnsupportedPattern( 10, true ),
                                                               new ArrayList<ParameterDataProvider>(), null );

        loader.getClass();
    }

    @SuppressWarnings("serial")
    private static class UnsupportedPattern extends ThreadingPattern {

        public UnsupportedPattern( int threadCount, boolean blockUntilCompletion ) {

            super( threadCount, 0, -1, -1, blockUntilCompletion );
        }

        @Override
        public List<ThreadingPattern> distribute( int maxNumHosts ) {

            return null;
        }

        @Override
        public String getPatternDescription() {

            return "Some unsupported pattern";
        }

        @Override
        public void setExecutionSpeed( long timeFrame, int executionsPerTimeFrame ) {

        }
    }
}
