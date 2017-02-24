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

import com.axway.ats.agent.core.BaseTest;
import com.axway.ats.agent.core.action.ActionRequest;
import com.axway.ats.agent.core.exceptions.ActionExecutionException;
import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.agent.core.threading.data.ParameterDataProvider;
import com.axway.ats.agent.core.threading.exceptions.ThreadingPatternNotSupportedException;
import com.axway.ats.agent.core.threading.patterns.AllAtOncePattern;
import com.axway.ats.agent.core.threading.patterns.FixedDurationAllAtOncePattern;
import com.axway.ats.agent.core.threading.patterns.ThreadingPattern;
import com.axway.ats.agent.core.threading.patterns.model.ExecutionPattern;

public class Test_ActionTaskFactory extends BaseTest {

    @Test
    public void createActionTaskMultipleInvocations() throws ActionExecutionException,
                                                     NoSuchComponentException, NoSuchActionException,
                                                     NoCompatibleMethodFoundException,
                                                     ThreadingPatternNotSupportedException {

        AllAtOncePattern pattern = new AllAtOncePattern( 10, true, 20, 1400 );
        Runnable actionTask = ActionTaskFactory.createTask( "IP",
                                                            "Action_Queue",
                                                            pattern,
                                                            pattern.getExecutionsPerTimeFrame(),
                                                            new ThreadsManager(),
                                                            null,
                                                            new ArrayList<ActionRequest>(),
                                                            new ArrayList<ParameterDataProvider>(),
                                                            null,
                                                            false );

        assertEquals( MultipleInvocationsActionTask.class, actionTask.getClass() );
    }

    @Test
    public void createActionTaskFixedDuration() throws ActionExecutionException, NoSuchComponentException,
                                               NoSuchActionException, NoCompatibleMethodFoundException,
                                               ThreadingPatternNotSupportedException {

        FixedDurationAllAtOncePattern pattern = new FixedDurationAllAtOncePattern( 10, true, 20, 1400 );
        Runnable actionTask = ActionTaskFactory.createTask( "IP",
                                                            "Action_Queue",
                                                            pattern,
                                                            pattern.getExecutionsPerTimeFrame(),
                                                            new ThreadsManager(),
                                                            null,
                                                            new ArrayList<ActionRequest>(),
                                                            new ArrayList<ParameterDataProvider>(),
                                                            null,
                                                            false );

        assertEquals( FixedDurationActionTask.class, actionTask.getClass() );
    }

    @Test(expected = ThreadingPatternNotSupportedException.class)
    public void createActionTaskNegativePatternNotSupported() throws ActionExecutionException,
                                                             NoSuchComponentException, NoSuchActionException,
                                                             NoCompatibleMethodFoundException,
                                                             ThreadingPatternNotSupportedException {

        UnsupportedPattern pattern = new UnsupportedPattern( 10, false );
        Runnable actionTask = ActionTaskFactory.createTask( "IP",
                                                            "Action_Queue",
                                                            pattern,
                                                            pattern.getExecutionsPerTimeFrame(),
                                                            new ThreadsManager(),
                                                            null,
                                                            new ArrayList<ActionRequest>(),
                                                            new ArrayList<ParameterDataProvider>(),
                                                            null,
                                                            false );

        assertEquals( FixedDurationActionTask.class, actionTask.getClass() );
    }

    @SuppressWarnings("serial")
    private static class UnsupportedPattern extends ThreadingPattern implements ExecutionPattern {

        public UnsupportedPattern( int threadCount,
                                   boolean blockUntilCompletion ) {

            super( threadCount, 0, -1, -1, blockUntilCompletion );
        }

        @Override
        public List<ThreadingPattern> distribute(
                                                  int maxNumHosts ) {

            return null;
        }

        public long getIntervalBetweenIterations() {

            return 0;
        }

        @Override
        public String getPatternDescription() {

            return "Some unsupported pattern";
        }

        @Override
        public int getExecutionsPerTimeFrame() {

            return 0;
        }

        @Override
        public long getTimeFrame() {

            return 0;
        }

        @Override
        public void setExecutionSpeed(
                                       long timeFrame,
                                       int executionsPerTimeFrame ) {

            
        }
    }
}
