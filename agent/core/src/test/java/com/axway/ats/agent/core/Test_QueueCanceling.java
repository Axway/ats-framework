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

import java.util.ArrayList;
import java.util.Arrays;
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
import com.axway.ats.agent.core.threading.data.config.ListDataConfig;
import com.axway.ats.agent.core.threading.data.config.ParameterProviderLevel;
import com.axway.ats.agent.core.threading.patterns.AllAtOncePattern;
import com.axway.ats.agent.core.threading.patterns.RampUpPattern;
import com.axway.ats.core.threads.ThreadsPerCaller;

public class Test_QueueCanceling extends BaseTest {

    private static final String ACTION_SLEEP        = "sleep action";
    private static final String ACTION_RUNNING      = "running action";
    private static final String QUEUE_NAME          = "Queue that will be canceled";
    private static final String HOST                = "IP";

    private static List<ActionRequest>        actionRequests = new ArrayList<ActionRequest>();
    private static MultiThreadedActionHandler actionHandler  = MultiThreadedActionHandler.getInstance(ThreadsPerCaller.getCaller());

    private int expectedNumExecutions;

    @BeforeClass
    public static void setUpTest_FutureTaskCanceling() throws AgentException {

        Component component = new Component( TEST_COMPONENT_NAME );
        ComponentActionMap actionMap = new ComponentActionMap( TEST_COMPONENT_NAME );
        actionMap.registerActionClass( LoadTestActionClass.class );
        component.setActionMap( actionMap );

        ComponentRepository componentRepository = ComponentRepository.getInstance();
        componentRepository.clear();
        componentRepository.putComponent( component );
    }

    @Before
    public void setUp() throws Exception {

        actionRequests.clear();

        expectedNumExecutions = -1;
        LoadTestActionClass.numExecutions = 0;

        assertEquals( "number of threads before the test is started",
                      0,
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
            assertEquals( "Expected number of action executions",
                          expectedNumExecutions,
                          LoadTestActionClass.numExecutions );
        }
    }

    /**
     * Both threads are canceled while running its 1st iteration
     */
    @Test
    public void allAtOnce_BothThreadsRunning() throws Exception {

        expectedNumExecutions = 2;

        actionRequests.add( new ActionRequest( TEST_COMPONENT_NAME, ACTION_SLEEP, new Object[]{ "1000" } ) );

        AllAtOncePattern pattern = new AllAtOncePattern( 2, false, 2, 0 );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        actionHandler.scheduleActions( HOST,
                                       QUEUE_NAME,
                                       -1,
                                       actionRequests,
                                       pattern,
                                       new LoaderDataConfig(),
                                       false );
        actionHandler.startQueue( QUEUE_NAME );

        Thread.sleep( 500 );
    }

    /**
     * Both threads are canceled while waiting between 1st and 2nd iterations
     */
    @Test
    public void allAtOnce_BothThreadsPaused() throws Exception {

        expectedNumExecutions = 2;

        actionRequests.add( new ActionRequest( TEST_COMPONENT_NAME, ACTION_SLEEP, new Object[]{ "500" } ) );

        AllAtOncePattern pattern = new AllAtOncePattern( 2, false, 2, 1000 );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        actionHandler.scheduleActions( HOST,
                                       QUEUE_NAME,
                                       -1,
                                       actionRequests,
                                       pattern,
                                       new LoaderDataConfig(),
                                       false );
        actionHandler.startQueue( QUEUE_NAME );

        Thread.sleep( 1000 );
    }

    /**
     * We cancel 1 thread while running its 1st iteration and 2nd thread while waiting between its 1st and 2nd iterations
     */
    @Test
    public void allAtOnce_OneThreadRunning_OneThreadPaused() throws Exception {

        expectedNumExecutions = 2;

        ListDataConfig parameterData = new ListDataConfig( "sleepTime",
                                                           Arrays.asList( new String[]{ "500", "2000" } ),
                                                           ParameterProviderLevel.PER_THREAD_STATIC );

        LoaderDataConfig loaderDataConfig = new LoaderDataConfig();
        loaderDataConfig.addParameterConfig( parameterData );

        actionRequests.add( new ActionRequest( TEST_COMPONENT_NAME,
                                               ACTION_SLEEP,
                                               new Object[]{ "sleepTime" } ) );

        AllAtOncePattern pattern = new AllAtOncePattern( 2, false, 2, 1000 );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        actionHandler.scheduleActions( HOST,
                                       QUEUE_NAME,
                                       -1,
                                       actionRequests,
                                       pattern,
                                       loaderDataConfig,
                                       false );
        actionHandler.startQueue( QUEUE_NAME );

        Thread.sleep( 1000 );
    }

    /**
     * All(or most of) the tests in this class let the actions do SLEEP (the internal thread state is TIMED_WAITING) while mean running.
     * In this test, the threads are really running(the internal thread state is RUNNABLE).
     * When we cancel the queue, one of the is RUNNING its first iteration, the other thread is sleeping between its first and second iterations.
     */
    @Test
    public void allAtOnce_OneThreadReallyRunning_OneThreadPaused() throws Exception {

        expectedNumExecutions = 2;

        ListDataConfig parameterData = new ListDataConfig( "runTime",
                                                           Arrays.asList( new String[]{ "500", "2000" } ),
                                                           ParameterProviderLevel.PER_THREAD_STATIC );

        LoaderDataConfig loaderDataConfig = new LoaderDataConfig();
        loaderDataConfig.addParameterConfig( parameterData );

        actionRequests.add( new ActionRequest( TEST_COMPONENT_NAME,
                                               ACTION_RUNNING,
                                               new Object[]{ "runTime" } ) );

        AllAtOncePattern pattern = new AllAtOncePattern( 2, false, 2, 1000 );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        actionHandler.scheduleActions( HOST,
                                       QUEUE_NAME,
                                       -1,
                                       actionRequests,
                                       pattern,
                                       loaderDataConfig,
                                       false );
        actionHandler.startQueue( QUEUE_NAME );

        Thread.sleep( 1000 );
    }

    /**
     * 2 threads are run synchronized, 1 thread will do its iteration and will be put in sleep by
     * the Thread Manager waiting until the 2 (slower) finish its iteration.
     * This is the moment we will cancel the queue. 
     */
    @Test
    public void allAtOnce_SynchronizedActions() throws Exception {

        expectedNumExecutions = 2;

        final String paramName = "sleepTime";
        ListDataConfig parameterData = new ListDataConfig( paramName,
                                                           Arrays.asList( new String[]{ "500", "1500" } ),
                                                           ParameterProviderLevel.PER_THREAD_STATIC );

        LoaderDataConfig loaderDataConfig = new LoaderDataConfig();
        loaderDataConfig.addParameterConfig( parameterData );

        actionRequests.add( new ActionRequest( TEST_COMPONENT_NAME,
                                               ACTION_SLEEP,
                                               new Object[]{ paramName } ) );

        AllAtOncePattern pattern = new AllAtOncePattern( 2, false, 2, 0 );
        pattern.setUseSynchronizedIterations( true );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        actionHandler.scheduleActions( HOST,
                                       QUEUE_NAME,
                                       -1,
                                       actionRequests,
                                       pattern,
                                       loaderDataConfig,
                                       true );
        actionHandler.startQueue( QUEUE_NAME );

        Thread.sleep( 1000 );
    }

    /**
     * Both threads will do 1 iteration. 
     * There won`t be any 3rd iteration, the remaining 3rd iteration cannot be added to no thread, so both threads will wait for 30 seconds.
     * 
     * At this moment we will cancel all threads
     */
    @Test
    public void allAtOnce_SetMaxSpeed_OneThreadRunning_OneThreadPaused() throws Exception {

        expectedNumExecutions = 3;

        actionRequests.add( new ActionRequest( TEST_COMPONENT_NAME, ACTION_SLEEP, new Object[]{ "500" } ) );

        AllAtOncePattern pattern = new AllAtOncePattern( 2, false, 5, 500 );
        pattern.setExecutionSpeed( 30, 3 );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        actionHandler.scheduleActions( HOST,
                                       QUEUE_NAME,
                                       -1,
                                       actionRequests,
                                       pattern,
                                       new LoaderDataConfig(),
                                       false );
        actionHandler.startQueue( QUEUE_NAME );

        Thread.sleep( 1500 );
    }
    
    /**
     * Both threads will do 1 iteration and will be paused for 30 seconds due to the max speed.
     * At this moment we will cancel both threads.
     */
    @Test
    public void allAtOnce_SetMaxSpeed_BothThreadsPaused() throws Exception {

        expectedNumExecutions = 2;

        actionRequests.add( new ActionRequest( TEST_COMPONENT_NAME, ACTION_SLEEP, new Object[]{ "500" } ) );

        AllAtOncePattern pattern = new AllAtOncePattern( 2, false, 5, 500 );
        pattern.setExecutionSpeed( 30, 2 );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        actionHandler.scheduleActions( HOST,
                                       QUEUE_NAME,
                                       -1,
                                       actionRequests,
                                       pattern,
                                       new LoaderDataConfig(),
                                       false );
        actionHandler.startQueue( QUEUE_NAME );
        Thread.sleep( 1500 );
    }

    @Test
    public void allAtOnce_SetMaxSpeed() throws Exception {

        expectedNumExecutions = 9;

        actionRequests.add( new ActionRequest( TEST_COMPONENT_NAME, ACTION_SLEEP, new Object[]{ "500" } ) );

        AllAtOncePattern pattern = new AllAtOncePattern( 9, false );
        pattern.setExecutionSpeed( 30, 15 );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        actionHandler.scheduleActions( HOST,
                                       QUEUE_NAME,
                                       -1,
                                       actionRequests,
                                       pattern,
                                       new LoaderDataConfig(),
                                       false );
        actionHandler.startQueue( QUEUE_NAME );
        Thread.sleep( 1500 );
    }

    /**
     * Cancel a ramp-up pattern while:
     *  - 1th thread is running its first iteration
     *  - 2nd thread is waiting to start its first iteration
     */
    @Test
    public void ramUp_OneThreadRunning_OneThreadNotStartedYet() throws Exception {

        expectedNumExecutions = 1;

        actionRequests.add( new ActionRequest( TEST_COMPONENT_NAME, ACTION_SLEEP, new Object[]{ "1000" } ) );

        RampUpPattern pattern = new RampUpPattern( 2, false, 2, 0, 2000, 1 );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        actionHandler.scheduleActions( HOST,
                                       QUEUE_NAME,
                                       -1,
                                       actionRequests,
                                       pattern,
                                       new LoaderDataConfig(),
                                       false );
        actionHandler.startQueue( QUEUE_NAME );

        Thread.sleep( 500 );
    }

    /**
     * Cancel a ramp-up pattern while:
     * - 1th thread has executed its first iteration and is sleeping before start the second iteration
     * - the 2nd thread is waiting to start its first iteration
     */
    @Test
    public void ramUp_OneThreadPaused_OneThreadNotStartedYet() throws Exception {

        expectedNumExecutions = 1;

        actionRequests.add( new ActionRequest( TEST_COMPONENT_NAME, ACTION_SLEEP, new Object[]{ "500" } ) );

        RampUpPattern pattern = new RampUpPattern( 2, false, 2, 1000, 2000, 1 );

        QueueExecutionStatistics.getInstance().initActionExecutionResults( QUEUE_NAME );
        actionHandler.scheduleActions( HOST,
                                       QUEUE_NAME,
                                       -1,
                                       actionRequests,
                                       pattern,
                                       new LoaderDataConfig(),
                                       false );
        actionHandler.startQueue( QUEUE_NAME );

        Thread.sleep( 1000 );
    }
}
