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

import static org.junit.Assert.assertFalse;

import java.util.Random;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.axway.ats.agent.core.BaseTest;

public class Test_ThreadsManager extends BaseTest {

    // logging is disabled by default
    static final boolean   LOGGING_ALLOWED                  = false;
    private static Logger  log                              = LogManager.getLogger( Test_ThreadsManager.class );

    // number of threads has been tried as high as 5000 for 30 iterations
    static final int       N_THREADS                        = 100;
    static final int       N_ITERATIONS                     = 3;

    // the class we are unit-testing
    private ThreadsManager threadsManager;

    // the new iteration starts when all workers have finished the current iteration
    static int             nWorkersCompletedInThisIteration = 0;

    // synchronization object
    static Object          waitForIterationCompletionObject = new Object();

    // we use this token in order to distinguish test threads created by this test among all running threads
    static final String    WORKER_THREAD_ID                 = "TestWorker thread";

    @Test
    public void positiveTest() {

        // create the threads manager
        this.threadsManager = new ThreadsManager();

        // create all the threads
        ThreadPoolExecutor executor = ( ThreadPoolExecutor ) Executors.newCachedThreadPool();
        executor.setKeepAliveTime( 0, TimeUnit.SECONDS );
        ExecutorCompletionService<Object> executionService = new ExecutorCompletionService<Object>( executor );

        IterationListener listener = new IterationListener();

        msg( log, "Ask Java to create all threads" );
        for( int j = 0; j < N_THREADS; j++ ) {
            executionService.submit( new TestWorker( N_ITERATIONS, threadsManager, listener ), null );
        }

        // run all iterations
        for( int i = 0; i < N_ITERATIONS; i++ ) {
            msg( log, "ITERATION " + i + "\n\n" );
            runThisIteration();

            waitForIterationCompletion();
        }

        // it may take a little while all threads are gone
        try {
            Thread.sleep( 1000 );
        } catch( InterruptedException e ) {}

        // check if there are any remaining threads started by this test
        checkAllRemainingThreads();
    }

    void runThisIteration() {

        synchronized( Test_ThreadsManager.waitForIterationCompletionObject ) {
            nWorkersCompletedInThisIteration = 0;
        }

        msg( log, "Run all threads" );
        this.threadsManager.start();
        msg( log, "All threads are running now" );
    }

    private void waitForIterationCompletion() {

        msg( log, "Wait for completion of this iteration - START" );
        synchronized( waitForIterationCompletionObject ) {
            try {
                waitForIterationCompletionObject.wait();
            } catch( InterruptedException e ) {
                e.printStackTrace();
            }
        }
        msg( log, "Wait for completion of this iteration - END" );
    }

    private void checkAllRemainingThreads() {

        // Find the root thread group
        ThreadGroup root = Thread.currentThread().getThreadGroup().getParent();
        while( root.getParent() != null ) {
            root = root.getParent();
        }

        // Check each thread group
        getAllRemainingThreadsForThisGroup( root, 0 );
    }

    private void getAllRemainingThreadsForThisGroup(
                                                     ThreadGroup group,
                                                     int level ) {

        // Get threads in `group'
        int numThreads = group.activeCount();
        Thread[] threads = new Thread[numThreads * 2];
        numThreads = group.enumerate( threads, false );

        // Enumerate each thread in `group'
        for( int i = 0; i < numThreads; i++ ) {
            Thread thread = threads[i];
            // we have changed the toString method for our test threads
            assertFalse( "This thread " + thread.getName() + " should not exist at this point of the test",
                         thread.toString().equals( Test_ThreadsManager.WORKER_THREAD_ID ) );

        }

        // Get thread subgroups of `group'
        int numGroups = group.activeGroupCount();
        ThreadGroup[] groups = new ThreadGroup[numGroups * 2];
        numGroups = group.enumerate( groups, false );

        // Recursively visit each subgroup
        for( int i = 0; i < numGroups; i++ ) {
            getAllRemainingThreadsForThisGroup( groups[i], level + 1 );
        }
    }

    public static void msg(
                            Logger log,
                            String msg ) {

        if( LOGGING_ALLOWED ) {
            log.info( msg );
        }
    }
}

class IterationListener {
    private static Logger log = LogManager.getLogger( IterationListener.class );

    public void onFinish() {

        synchronized( Test_ThreadsManager.waitForIterationCompletionObject ) {

            Thread thread = Thread.currentThread();
            Test_ThreadsManager.msg( log, thread.getName() + " is over" );

            ++Test_ThreadsManager.nWorkersCompletedInThisIteration;
            Test_ThreadsManager.msg( log, "Number completed threads: "
                                          + Test_ThreadsManager.nWorkersCompletedInThisIteration );
            if( Test_ThreadsManager.nWorkersCompletedInThisIteration == Test_ThreadsManager.N_THREADS ) {
                Test_ThreadsManager.nWorkersCompletedInThisIteration = 0;
                Test_ThreadsManager.msg( log,
                                         "All threads of this iteration are completed, wake up the main thread" );
                Test_ThreadsManager.waitForIterationCompletionObject.notifyAll();
            }
        }
    }
}

class TestWorker implements Runnable {

    private static Logger     log = LogManager.getLogger( TestWorker.class );

    private ThreadsManager    threadsManager;
    private IterationListener listener;

    private int               iterations;

    public TestWorker( int iterations,
                       ThreadsManager resetableLocker,
                       IterationListener listener ) {

        this.iterations = iterations;
        this.threadsManager = resetableLocker;
        this.listener = listener;

    }

    @Override
    public void run() {

        String threadName = Thread.currentThread().getName();
        for( int i = 0; i < iterations; i++ ) {
            Test_ThreadsManager.msg( log, threadName + " iteration " + i + ". Wait to be started" );

            threadsManager.waitForStart();

            long sleepTime = new Random().nextInt( 3 ) + 1;
            Test_ThreadsManager.msg( log, threadName + " will run for " + sleepTime + " sec" );
            try {
                Thread.sleep( sleepTime * 1000 );
            } catch( InterruptedException e ) {}
            Test_ThreadsManager.msg( log, threadName + " has finished" );

            listener.onFinish();
        }

        Test_ThreadsManager.msg( log, threadName + " will exit now" );
    }

    @Override
    public String toString() {

        return Test_ThreadsManager.WORKER_THREAD_ID;
    }
}
