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
import com.axway.ats.agent.core.threading.exceptions.ActionTaskLoaderException;
import com.axway.ats.agent.core.threading.patterns.AllAtOncePattern;
import com.axway.ats.agent.core.threading.patterns.RampUpPattern;

public class DisabledTest_RampUpLoaderMultipleInvocations extends BaseTest {

    static Logger                      log        = LogManager.getLogger( DisabledTest_RampUpLoaderMultipleInvocations.class );
    private static final String        QUEUE_NAME = "test 1";

    private static List<ActionRequest> actionRequests;
    private QueueLoader                loader;

    @BeforeClass
    public static void setUpTest_ActionInvoker() throws AgentException {

        Component component = new Component( TEST_COMPONENT_NAME );
        ComponentActionMap actionMap = new ComponentActionMap( TEST_COMPONENT_NAME );
        actionMap.registerActionClass( LoadTestActionClass.class );
        component.setActionMap( actionMap );

        ComponentRepository componentRepository = ComponentRepository.getInstance();
        componentRepository.clear();
        componentRepository.putComponent( component );
    }

    @Before
    public void setUp() throws AgentException, InterruptedException {

        ActionRequest actionRequest = new ActionRequest( TEST_COMPONENT_NAME, "sleep action",
                                                         new Object[]{ "3" } );
        actionRequests = new ArrayList<ActionRequest>();
        actionRequests.add( actionRequest );

        synchronized( LoadTestActionClass.class ) {
            LoadTestActionClass.numExecutions = 0;
            log.warn( "clean numInvocations=" + LoadTestActionClass.numExecutions );
        }
    }

    @Test
    public void allAtOncePatternBlockingPositive() throws Exception {

        AllAtOncePattern pattern = new AllAtOncePattern( 5, true );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_NAME, actionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );
        assertEquals( 0, LoadTestActionClass.numExecutions );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        //then start the loader
        loader.start();
        assertEquals( 5, LoadTestActionClass.numExecutions );
    }

    @Test
    public void allAtOncePatternBlockingVerifyNoDelay() throws Exception {

        AllAtOncePattern pattern = new AllAtOncePattern( 5, true );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_NAME, actionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );

        //then start the loader
        long startTime = Calendar.getInstance().getTimeInMillis();
        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        loader.start();
        long endTime = Calendar.getInstance().getTimeInMillis();

        assertTrue( endTime - startTime < 2000 );
    }

    @Test
    public void allAtOncePatternNonBlockingPositive() throws Exception {

        AllAtOncePattern pattern = new AllAtOncePattern( 50, false );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_NAME, actionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );

        //then start the loader
        long startTime = Calendar.getInstance().getTimeInMillis();
        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        loader.start();
        long endTime = Calendar.getInstance().getTimeInMillis();

        assertTrue( endTime - startTime < 2000 );

        loader.waitUntilFinished();
    }

    @Test
    public void allAtOnceExceptionThrownInAction() throws Exception {

        AllAtOncePattern pattern = new AllAtOncePattern( 50, true );

        ActionRequest actionRequest = new ActionRequest( TEST_COMPONENT_NAME, "exception action",
                                                         new Object[]{} );

        ArrayList<ActionRequest> newActionRequests = new ArrayList<ActionRequest>();
        newActionRequests.add( actionRequest );

        loader = LoadQueueFactory.createLoadQueue( QUEUE_NAME, newActionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        //then start the loader
        loader.start();

        assertEquals( 0, LoadTestActionClass.numExecutions );
    }

    @Test
    public void rampUpPatternBlockingPositive() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 10, true );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_NAME, actionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );
        assertEquals( 0, LoadTestActionClass.numExecutions );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        //then start the loader
        loader.start();
        assertEquals( 10, LoadTestActionClass.numExecutions );
    }

    @Test
    public void rampUpPatternBlockingMultipleInvocationsNoDelayPositive() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 2, true, 10, 0 );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_NAME, actionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );
        assertEquals( 0, LoadTestActionClass.numExecutions );

        //then start the loader
        long startTime = Calendar.getInstance().getTimeInMillis();
        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        loader.start();
        long endTime = Calendar.getInstance().getTimeInMillis();

        assertEquals( 20, LoadTestActionClass.numExecutions );
        assertTrue( endTime - startTime < 2000 );
    }

    @Test
    public void rampUpPatternBlockingMultipleInvocationsWithDelayPositive() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 2, true, 4, 1000 );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_NAME, actionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );
        assertEquals( 0, LoadTestActionClass.numExecutions );

        //then start the loader
        long startTime = Calendar.getInstance().getTimeInMillis();
        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        loader.start();
        long endTime = Calendar.getInstance().getTimeInMillis();

        assertEquals( 8, LoadTestActionClass.numExecutions );
        assertTrue( endTime - startTime > 3000 );
    }

    @Test
    public void rampUpPatternBlockingVerifyNoDelayIfNoRampUp() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 5, true );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_NAME, actionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );

        //then start the loader
        long startTime = Calendar.getInstance().getTimeInMillis();
        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        loader.start();
        long endTime = Calendar.getInstance().getTimeInMillis();

        assertTrue( endTime - startTime < 2000 );
    }

    @Test
    public void rampUpPatternBlockingWithRampUpPositive() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 10, true, 1, 0, 1000, 2 );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_NAME, actionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );
        assertEquals( 0, LoadTestActionClass.numExecutions );

        //then start the loader
        long startTime = Calendar.getInstance().getTimeInMillis();
        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        loader.start();
        long executionTime = Calendar.getInstance().getTimeInMillis() - startTime;

        assertEquals( 10, LoadTestActionClass.numExecutions );
        assertTrue( executionTime > 4000 );
        assertTrue( executionTime < 6000 );
    }

    @Test
    public void rampUpPatternBlockingWithRampUpIrregularPatternPositive() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 10, true, 1, 0, 1000, 3 );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_NAME, actionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );
        assertEquals( 0, LoadTestActionClass.numExecutions );

        //then start the loader
        long startTime = Calendar.getInstance().getTimeInMillis();
        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        loader.start();
        long executionTime = Calendar.getInstance().getTimeInMillis() - startTime;

        assertEquals( 10, LoadTestActionClass.numExecutions );
        assertTrue( executionTime > 3000 );
        assertTrue( executionTime < 5000 );
    }

    @Test
    public void rampUpPatternNoRampUpNonBlockingPositive() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 3, false, 10, 10000 );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_NAME, actionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );
        assertEquals( 0, LoadTestActionClass.numExecutions );

        //then start the loader
        long startTime = Calendar.getInstance().getTimeInMillis();
        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        loader.start();
        long executionTime = Calendar.getInstance().getTimeInMillis() - startTime;

        assertTrue( executionTime < 1000 );

        loader.cancel();
    }

    @Test
    public void rampUpPatternNonBlockingPositive() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 3, false, 10, 10000, 2000, 1 );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_NAME, actionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );
        assertEquals( 0, LoadTestActionClass.numExecutions );

        //then start the loader
        long startTime = Calendar.getInstance().getTimeInMillis();
        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        loader.start();
        long executionTime = Calendar.getInstance().getTimeInMillis() - startTime;

        // as the pattern is not blocking, we will get here right away
        assertTrue( executionTime < 1000 );

        Thread.sleep( 2000 );

        loader.cancel();
    }

    @Test
    public void cancelTasksPositive() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 3, false, 10, 3000 );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_NAME, actionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );
        assertEquals( 0, LoadTestActionClass.numExecutions );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        //then start the loader
        loader.start();

        //sleep until the first and second iterations are executed
        Thread.sleep( 3500 );

        assertEquals( 6, LoadTestActionClass.numExecutions );

        loader.cancel();
    }

    @Test
    public void cancelTasksBeforeStartPositive() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 3, true, 10, 3000 );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_NAME, actionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );
        assertEquals( 0, LoadTestActionClass.numExecutions );

        loader.cancel();
        assertEquals( 0, LoadTestActionClass.numExecutions );
    }

    @Test
    public void cancelFinishedTasksPositive() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 3, false, 3, 0 );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_NAME, actionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );
        assertEquals( 0, LoadTestActionClass.numExecutions );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        //then start the loader
        loader.start();

        //sleep until the first and second iterations are executed
        Thread.sleep( 1000 );

        assertEquals( 9, LoadTestActionClass.numExecutions );

        loader.cancel();
    }

    @Test
    public void cancelTasksDuringExecutionPositive() throws Exception {

        ActionRequest actionRequest = new ActionRequest( TEST_COMPONENT_NAME, "sleep action",
                                                         new Object[]{ "10000" } );

        ArrayList<ActionRequest> newActionRequests = new ArrayList<ActionRequest>();
        newActionRequests.add( actionRequest );

        RampUpPattern pattern = new RampUpPattern( 3, false, 10, 0 );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_NAME, newActionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );
        assertEquals( 0, LoadTestActionClass.numExecutions );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        //then start the loader
        loader.start();
        Thread.sleep( 1000 );
        loader.cancel();

        //assert that only the first execution took place
        assertEquals( 3, LoadTestActionClass.numExecutions );
    }

    @Test
    public void getStateRunningBlockingPositive() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 3, true );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_NAME, actionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );
        assertEquals( ActionTaskLoaderState.SCHEDULED, loader.getState() );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        //then start the loader
        loader.start();
        assertEquals( ActionTaskLoaderState.FINISHED, loader.getState() );
    }

    @Test
    public void getStateNonBlockingPositive() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 3, false, 10, 3000 );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_NAME, actionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );
        assertEquals( ActionTaskLoaderState.SCHEDULED, loader.getState() );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        //then start the loader
        loader.start();
        assertEquals( ActionTaskLoaderState.RUNNING, loader.getState() );

        //cancel
        loader.cancel();
        assertEquals( ActionTaskLoaderState.FINISHED, loader.getState() );
    }

    @Test(expected = ActionTaskLoaderException.class)
    public void scheduleNegativeAlreadyScheduled() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 3, false, 10, 3000 );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_NAME, actionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        //first schedule the threads
        loader.scheduleThreads( "IP", false );
        Thread.sleep( 1 );

        try {
            loader.scheduleThreads( "IP", false );
        } finally {
            loader.cancel();
        }
    }

    @Test(expected = ActionTaskLoaderException.class)
    public void startNegativeNotScheduled() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 3, false, 10, 3000 );
        loader = LoadQueueFactory.createLoadQueue( QUEUE_NAME, actionRequests, pattern,
                                                   new ArrayList<ParameterDataProvider>(), null );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        //first schedule the threads
        loader.start();
    }
}
