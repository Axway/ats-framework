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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.agent.core.BaseTest;
import com.axway.ats.agent.core.Component;
import com.axway.ats.agent.core.ComponentActionMap;
import com.axway.ats.agent.core.ComponentRepository;
import com.axway.ats.agent.core.action.ActionRequest;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.monitoring.queue.QueueExecutionStatistics;
import com.axway.ats.agent.core.threading.data.ParameterDataProvider;
import com.axway.ats.agent.core.threading.patterns.FixedDurationAllAtOncePattern;
import com.axway.ats.agent.core.threading.patterns.FixedDurationRampUpPattern;

public class DisabledTest_RampUpQueueLoaderFixedDuration extends BaseTest {

    static Logger                      log     = LogManager.getLogger( DisabledTest_RampUpQueueLoaderFixedDuration.class );
    private static final String        QUEUE_1 = "Performance queue";
    private QueueLoader                loader;

    private static List<ActionRequest> actionRequests;

    @BeforeClass
    public static void setUpTest_ActionInvoker() throws AgentException {

        Component component = new Component( TEST_COMPONENT_NAME );
        ComponentActionMap actionMap = new ComponentActionMap( TEST_COMPONENT_NAME );
        actionMap.registerActionClass( LoadTestActionClass.class );
        component.setActionMap( actionMap );

        ComponentRepository componentRepository = ComponentRepository.getInstance();
        componentRepository.clear();
        componentRepository.putComponent( component );

        ActionRequest actionRequest = new ActionRequest( TEST_COMPONENT_NAME, "action 1", new Object[]{ 3 } );

        actionRequests = new ArrayList<ActionRequest>();
        actionRequests.add( actionRequest );
    }

    @Before
    public void setUp() throws AgentException, InterruptedException {

        synchronized( LoadTestActionClass.class ) {
            LoadTestActionClass.numExecutions = 0;
        }
        log.warn( "cleaning numInvocations =" + LoadTestActionClass.numExecutions );
    }

    @Test
    public void allAtOncePatternBlockingPositive() throws Exception {

        FixedDurationAllAtOncePattern pattern = new FixedDurationAllAtOncePattern( 2, true, 2, 900 );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_1, actionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );
        assertEquals( 0, LoadTestActionClass.numExecutions );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_1 );
        //then start the loader
        loader.start();
        assertEquals( 6, LoadTestActionClass.numExecutions );
    }

    @Test
    public void allAtOncePatternNonBlockingPositive() throws Exception {

        FixedDurationAllAtOncePattern pattern = new FixedDurationAllAtOncePattern( 2, false, 2, 500 );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_1, actionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );

        //then start the loader
        long startTime = Calendar.getInstance().getTimeInMillis();
        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_1 );
        loader.start();
        long endTime = Calendar.getInstance().getTimeInMillis();

        assertTrue( endTime - startTime < 1000 );

        loader.waitUntilFinished();
    }

    @Test
    public void allAtOnceExceptionThrownInAction() throws Exception {

        FixedDurationAllAtOncePattern pattern = new FixedDurationAllAtOncePattern( 50, true, 5 );

        ActionRequest actionRequest = new ActionRequest( TEST_COMPONENT_NAME, "exception action",
                                                         new Object[]{} );

        ArrayList<ActionRequest> newActionRequests = new ArrayList<ActionRequest>();
        newActionRequests.add( actionRequest );

        loader = LoadQueueFactory.createLoadQueue( QUEUE_1, newActionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );

        //then start the loader
        long startTime = Calendar.getInstance().getTimeInMillis();
        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_1 );
        loader.start();
        long endTime = Calendar.getInstance().getTimeInMillis();

        //verify that execution will not stop
        assertTrue( endTime - startTime > 5 );
    }

    @Test
    public void rampUpPatternBlockingPositive() throws Exception {

        FixedDurationRampUpPattern pattern = new FixedDurationRampUpPattern( 2, true, 2, 900 );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_1, actionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );
        assertEquals( 0, LoadTestActionClass.numExecutions );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_1 );
        //then start the loader
        loader.start();
        assertEquals( 6, LoadTestActionClass.numExecutions );
    }

    @Test
    public void rampUpPatternBlockingWithRampUpPositive() throws Exception {

        FixedDurationRampUpPattern pattern = new FixedDurationRampUpPattern( 2, true, 3, 900, 5000, 1 );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_1, actionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );
        assertEquals( 0, LoadTestActionClass.numExecutions );

        //then start the loade
        long startTime = Calendar.getInstance().getTimeInMillis();
        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_1 );
        loader.start();
        long executionTime = Calendar.getInstance().getTimeInMillis() - startTime;

        assertTrue( LoadTestActionClass.numExecutions > 6 );
        assertTrue( executionTime > 5000 );
        assertTrue( executionTime < 11000 );
    }

    @Test
    public void cancelTasksPositive() throws Exception {

        FixedDurationRampUpPattern pattern = new FixedDurationRampUpPattern( 3, false, 10, 3000 );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_1, actionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );
        assertEquals( 0, LoadTestActionClass.numExecutions );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_1 );
        //then start the loader
        loader.start();

        //sleep until the first and second iterations are executed
        Thread.sleep( 3500 );

        assertEquals( 6, LoadTestActionClass.numExecutions );

        loader.cancel();
    }

    @Test
    public void cancelTasksBeforeStartPositive() throws Exception {

        FixedDurationRampUpPattern pattern = new FixedDurationRampUpPattern( 3, true, 10, 3000 );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_1, actionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_1 );
        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );
        assertEquals( 0, LoadTestActionClass.numExecutions );

        loader.cancel();
        assertEquals( 0, LoadTestActionClass.numExecutions );
    }

    @Test
    public void cancelFinishedTasksPositive() throws Exception {

        FixedDurationRampUpPattern pattern = new FixedDurationRampUpPattern( 3, false, 1, 100 );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_1, actionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );
        assertEquals( 0, LoadTestActionClass.numExecutions );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_1 );
        //then start the loader
        loader.start();

        //sleep until the first and second iterations are executed
        Thread.sleep( 1500 );

        assertTrue( LoadTestActionClass.numExecutions > 20 );

        loader.cancel();
    }

    @Test
    public void cancelTasksDuringExecutionPositive() throws Exception {

        ActionRequest actionRequest = new ActionRequest( TEST_COMPONENT_NAME, "sleep action",
                                                         new Object[]{ "10000" } );

        ArrayList<ActionRequest> newActionRequests = new ArrayList<ActionRequest>();
        newActionRequests.add( actionRequest );

        FixedDurationRampUpPattern pattern = new FixedDurationRampUpPattern( 3, false, 10, 0 );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_1, newActionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );
        assertEquals( 0, LoadTestActionClass.numExecutions );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_1 );
        //then start the loader
        loader.start();
        Thread.sleep( 1000 );
        loader.cancel();

        //assert that only the first execution took place
        assertEquals( 3, LoadTestActionClass.numExecutions );
    }
}
