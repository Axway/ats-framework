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
package com.axway.ats.agent.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.agent.core.action.ActionRequest;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.monitoring.queue.QueueExecutionStatistics;
import com.axway.ats.agent.core.threading.LoadTestActionClass;
import com.axway.ats.agent.core.threading.data.config.LoaderDataConfig;
import com.axway.ats.agent.core.threading.exceptions.LoadQueueAlreadyExistsException;
import com.axway.ats.agent.core.threading.exceptions.NoSuchLoadQueueException;
import com.axway.ats.agent.core.threading.patterns.AllAtOncePattern;
import com.axway.ats.agent.core.threading.patterns.RampUpPattern;
import com.axway.ats.core.threads.ThreadsPerCaller;

public class DisabledTest_MultiThreadedActionHandler extends BaseTest {

    private static final String               QUEUE_1        = "First performance queue";
    private static final String               QUEUE_2        = "Second performance queue";

    private static List<ActionRequest>        actionRequests = new ArrayList<ActionRequest>();
    private static MultiThreadedActionHandler actionHandler  = MultiThreadedActionHandler.getInstance(ThreadsPerCaller.getCaller());

    private int                               expectedNumExecutions;

    @BeforeClass
    public static void setUpTest_MultiThreadedActionHandler() throws AgentException {

        Component component = new Component( TEST_COMPONENT_NAME );
        ComponentActionMap actionMap = new ComponentActionMap( TEST_COMPONENT_NAME );
        actionMap.registerActionClass( LoadTestActionClass.class );
        component.setActionMap( actionMap );

        ComponentRepository componentRepository = ComponentRepository.getInstance();
        componentRepository.clear();
        componentRepository.putComponent( component );

        ActionRequest actionRequest = new ActionRequest( TEST_COMPONENT_NAME, "action 1", new Object[]{ 3 } );
        actionRequests.add( actionRequest );
    }

    @Before
    public void setUp() throws AgentException {

        expectedNumExecutions = -1;
        LoadTestActionClass.numExecutions = 0;

        assertEquals( "number of threads before the test is started", 0,
                      new LoadTestActionClass().getAllThreads().size() );
    }

    @After
    public void tearDown() throws Exception {

        // cancel all queues
        actionHandler.cancelAllQueues();
        // verify no remaining threads are present
        Thread.sleep( 200 );
        int numThreadsLeft = new LoadTestActionClass().getAllThreads().size();
        if( numThreadsLeft > 0 ) {
            // we will give some more time for the threads to terminate
            Thread.sleep( 3000 );
            numThreadsLeft = new LoadTestActionClass().getAllThreads().size();
            if( numThreadsLeft > 0 ) {
                // Some threads are still alive. Fail the test
                assertEquals( "number of threads after test is over", 0, numThreadsLeft );
            }
        }

        // verify the number of action executions
        if( expectedNumExecutions != -1 ) {
            assertEquals( "Expected number of action executions", expectedNumExecutions,
                          LoadTestActionClass.numExecutions );
        }
    }

    @Test
    public void executeActionsAllAtOncePatternBlockingPositive() throws Exception {

        AllAtOncePattern pattern = new AllAtOncePattern( 5, true );
        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_1 );
        actionHandler.executeActions( "IP", QUEUE_1, -1, actionRequests, pattern, new LoaderDataConfig() );

        expectedNumExecutions = 5;
    }

    @Test
    public void executeActionsAllAtOncePatternBlockingVerifyNoDelay() throws Exception {

        AllAtOncePattern pattern = new AllAtOncePattern( 5, true );

        long startTime = Calendar.getInstance().getTimeInMillis();
        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_1 );
        actionHandler.executeActions( "IP", QUEUE_1, -1, actionRequests, pattern, new LoaderDataConfig() );
        long endTime = Calendar.getInstance().getTimeInMillis();

        assertTrue( endTime - startTime < 1000 );
    }

    @Test
    public void executeActionsRampUpPatternBlockingPositive() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 10, true );
        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_1 );
        actionHandler.executeActions( "IP", QUEUE_1, -1, actionRequests, pattern, new LoaderDataConfig() );

        //check that all were executed
        expectedNumExecutions = 10;
    }

    @Test
    public void executeActionsRampUpPatternBlockingWithRampUpPositive() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 10, true, 1, 0, 1000, 2 );

        //then start the queue
        long startTime = Calendar.getInstance().getTimeInMillis();
        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_1 );
        actionHandler.executeActions( "IP", QUEUE_1, -1, actionRequests, pattern, new LoaderDataConfig() );
        long executionTime = Calendar.getInstance().getTimeInMillis() - startTime;

        assertTrue( executionTime >= 4000 );
        assertTrue( executionTime < 6000 );

        expectedNumExecutions = 10;
    }

    @Test
    public void executeActionsRampUpPatternBlockingWithRampUpPositiveMultipleInvocations() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 10, true, 2, 1000, 2000, 5 );

        //then start the queue
        long startTime = Calendar.getInstance().getTimeInMillis();
        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_1 );
        actionHandler.executeActions( "IP", QUEUE_1, -1, actionRequests, pattern, new LoaderDataConfig() );
        long executionTime = Calendar.getInstance().getTimeInMillis() - startTime;

        assertTrue( executionTime >= 4000 );
        assertTrue( executionTime < 6000 );

        expectedNumExecutions = 20;
    }

    @Test
    public void executeActionsRampUpPatternNonBlockingPositive() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 3, false, 10, 10000, 2000, 1 );

        //then start the queue
        long startTime = Calendar.getInstance().getTimeInMillis();
        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_1 );
        actionHandler.executeActions( "IP", QUEUE_1, -1, actionRequests, pattern, new LoaderDataConfig() );
        long executionTime = Calendar.getInstance().getTimeInMillis() - startTime;

        // as the pattern is not blocking, we will get here right away
        assertTrue( executionTime < 1000 );
    }

    @Test
    public void scheduleActionsRampUpPatternBlockingWithRampUpPositive() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 10, true, 1, 0, 5000, 2 );

        //then start the queue
        long startTime = Calendar.getInstance().getTimeInMillis();
        actionHandler.scheduleActions( "IP", QUEUE_1, -1, actionRequests, pattern, new LoaderDataConfig(),
                                       false );
        long executionTime = Calendar.getInstance().getTimeInMillis() - startTime;

        assertTrue( executionTime < 1000 );
    }

    @Test(expected = LoadQueueAlreadyExistsException.class)
    public void scheduleActionsNegativeQueueAlreadyExists() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 10, true, 1, 0, 5000, 2 );
        actionHandler.scheduleActions( "IP", QUEUE_1, -1, actionRequests, pattern, new LoaderDataConfig(),
                                       false );
        actionHandler.scheduleActions( "IP", QUEUE_1, -1, actionRequests, pattern, new LoaderDataConfig(),
                                       false );
    }

    @Test
    public void startRampUpPatternBlockingWithRampUpPositive() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 10, true, 1, 0, 1000, 2 );

        actionHandler.scheduleActions( "IP", QUEUE_1, -1, actionRequests, pattern, new LoaderDataConfig(),
                                       false );

        //then start the queue
        long startTime = Calendar.getInstance().getTimeInMillis();
        actionHandler.startQueue( QUEUE_1 );
        long executionTime = Calendar.getInstance().getTimeInMillis() - startTime;

        assertTrue( executionTime >= 4000 );
        assertTrue( executionTime < 7000 );

        expectedNumExecutions = 10;
    }

    @Test
    public void startRampUpPatternNonBlockingPositive() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 3, false, 10, 10000, 2000, 1 );
        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_1 );

        actionHandler.scheduleActions( "IP", QUEUE_1, -1, actionRequests, pattern, new LoaderDataConfig(),
                                       false );

        //then start the queue
        long startTime = Calendar.getInstance().getTimeInMillis();
        actionHandler.startQueue( QUEUE_1 );
        long executionTime = Calendar.getInstance().getTimeInMillis() - startTime;

        // as the pattern is not blocking, we will get here right away
        assertTrue( executionTime < 1000 );
    }

    @Test(expected = NoSuchLoadQueueException.class)
    public void startRampUpPatternNegativeQueueDoesNotExist() throws Exception {

        actionHandler.startQueue( QUEUE_1 );
    }

    @Test
    public void cancelAllQueuesPositive() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 3, false, 10, 3000 );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_1 );

        //then start the queue
        actionHandler.executeActions( "IP", QUEUE_1, -1, actionRequests, pattern, new LoaderDataConfig() );

        //sleep until the first and second iterations are executed
        Thread.sleep( 3500 );

        assertEquals( 6, LoadTestActionClass.numExecutions );

        actionHandler.cancelAllQueues();

        //assert nothing else was executed
        Thread.sleep( 3500 );
        assertEquals( 6, LoadTestActionClass.numExecutions );
    }

    @Test
    public void cancelAllQueuesBeforeStartPositive() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 3, true, 10, 3000 );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_1 );
        //first schedule the threads
        actionHandler.scheduleActions( "IP", QUEUE_1, -1, actionRequests, pattern, new LoaderDataConfig(),
                                       false );

        actionHandler.cancelAllQueues();
        expectedNumExecutions = 0;
    }

    @Test
    public void cancelAllQueuesFinishedTasksPositive() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 3, false, 3, 0 );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_1 );
        //then start the queue
        actionHandler.executeActions( "IP", QUEUE_1, -1, actionRequests, pattern, new LoaderDataConfig() );

        //sleep until the first and second iterations are executed
        Thread.sleep( 1000 );

        expectedNumExecutions = 9;
    }

    @Test
    public void cancelAllQueuesDuringExecutionPositive() throws Exception {

        ActionRequest actionRequest = new ActionRequest( TEST_COMPONENT_NAME, "sleep action",
                                                         new Object[]{ "10000" } );

        ArrayList<ActionRequest> newActionRequests = new ArrayList<ActionRequest>();
        newActionRequests.add( actionRequest );

        RampUpPattern pattern = new RampUpPattern( 3, false, 10, 0 );

        assertEquals( 0, LoadTestActionClass.numExecutions );
        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_1 );

        //then start the queue
        actionHandler.executeActions( "IP", QUEUE_1, -1, newActionRequests, pattern, new LoaderDataConfig() );
        Thread.sleep( 1000 );
        actionHandler.cancelAllQueues();

        //assert that only the first execution took place
        expectedNumExecutions = 3;
    }

    @Test
    public void waitUntilAcitonQueueFinishesPositive() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 3, true, 3, 1000, 200, 2 );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_1 );

        //then start the queue
        actionHandler.executeActions( "IP", QUEUE_1, -1, actionRequests, pattern, new LoaderDataConfig() );

        //wait until the queue finishes
        actionHandler.waitUntilQueueFinish( QUEUE_1 );

        expectedNumExecutions = 9;
    }

    @Test
    public void waitUntilAllQueuesFinishPositive() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 3, false, 3, 1000, 200, 2 );

        //then start the queue
        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_1 );
        actionHandler.executeActions( "IP", QUEUE_1, -1, actionRequests, pattern, new LoaderDataConfig() );
        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_2 );
        actionHandler.executeActions( "IP", QUEUE_2, -1, actionRequests, pattern, new LoaderDataConfig() );

        //wait until the queue finishes
        actionHandler.waitUntilAllQueuesFinish();

        expectedNumExecutions = 18;
    }

    @Test
    public void waitUntilAllQueuesFinishPositiveStateDifferentThanRunning() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 3, false, 3, 1000, 200, 2 );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_1 );
        //first queue finished
        actionHandler.executeActions( "IP", QUEUE_1, -1, actionRequests, new AllAtOncePattern( 1, true ),
                                      new LoaderDataConfig() );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_2 );
        //second queue running
        actionHandler.executeActions( "IP", QUEUE_2, -1, actionRequests, pattern, new LoaderDataConfig() );

        //third queue scheduled
        actionHandler.scheduleActions( "IP", "test 3", -1, actionRequests, pattern, new LoaderDataConfig(),
                                       false );

        //wait until the queue finishes
        actionHandler.waitUntilAllQueuesFinish();

        expectedNumExecutions = 10;
    }

    @Test
    public void getRunningQueuesCountPositive() throws Exception {

        RampUpPattern pattern = new RampUpPattern( 3, false, 3, 1000, 200, 2 );

        assertEquals( 0, actionHandler.getRunningQueuesCount() );
        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_1 );

        //first queue finished
        actionHandler.executeActions( "IP", QUEUE_1, -1, actionRequests, pattern, new LoaderDataConfig() );

        assertEquals( 1, actionHandler.getRunningQueuesCount() );
        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_2 );

        //second queue running
        actionHandler.executeActions( "IP", QUEUE_2, -1, actionRequests, pattern, new LoaderDataConfig() );

        assertEquals( 2, actionHandler.getRunningQueuesCount() );

        //third queue scheduled
        actionHandler.scheduleActions( "IP", "test 3", -1, actionRequests, pattern, new LoaderDataConfig(),
                                       false );

        assertEquals( 2, actionHandler.getRunningQueuesCount() );

        //wait until the queue finishes
        actionHandler.waitUntilAllQueuesFinish();

        assertEquals( 0, actionHandler.getRunningQueuesCount() );
    }

    @Test
    public void finishedQueuesAreCleanedUpOnSchedule() throws Exception {

        AllAtOncePattern pattern = new AllAtOncePattern( 1, true );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_1 );
        //first queue finished
        actionHandler.executeActions( "IP", QUEUE_1, -1, actionRequests, pattern, new LoaderDataConfig() );

        //second queue running
        actionHandler.executeActions( "IP", QUEUE_1, -1, actionRequests, pattern, new LoaderDataConfig() );

        //wait until the queue finishes
        actionHandler.waitUntilAllQueuesFinish();
    }
}
