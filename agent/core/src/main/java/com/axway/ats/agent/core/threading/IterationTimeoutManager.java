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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * When the pattern has an iteration timeout set, this class starts a thread which
 * monitors the queue execution and interrupts it when the timeout is hit. 
 * 
 * Interrupts while:
 *  - an action i running
 *
 * Does not interrupt while:
 *  - a sleep between iterations is running
 */
public class IterationTimeoutManager extends Thread {

    // the name of the thread managing the iterations
    public static final String            THREAD_NAME        = "ATS_ITERATION_TIMEOUT_MANAGER_";

    // list of threads to manage
    // <thread name, thread description>
    private Map<String, ThreadDescriptor> threads;

    // the max iteration timeout as provided by the user
    private int                           timeoutMillis;

    // the moment the soonest iteration will start
    private long                          nextIterationStartTime;
    private static final long             NOT_SET_START_TIME = Long.MAX_VALUE;

    // whether the performance queue is over
    private boolean                       isQueueOver;

    public IterationTimeoutManager( int timeoutSeconds ) {

        this.timeoutMillis = timeoutSeconds * 1000;
        this.nextIterationStartTime = NOT_SET_START_TIME;
        this.isQueueOver = false;

        this.threads = new HashMap<>();
    }

    /**
     * Called by each thread to inform a new iteration is starting
     * 
     * @param actionTask
     * @param thread
     * @param iterationStartTime
     */
    public void setIterationStartTime( AbstractActionTask actionTask, long iterationStartTime ) {

        Thread thread = Thread.currentThread();

        synchronized( this.threads ) {
            // find the soonest iteration start time
            if( this.nextIterationStartTime > iterationStartTime ) {
                this.nextIterationStartTime = iterationStartTime;
            }

            // set the start time of the next iteration for this thread
            String threadName = thread.getName();
            ThreadDescriptor threadDesc = this.threads.get( threadName );
            if( threadDesc == null ) { // add the thread if not known
                threadDesc = new ThreadDescriptor();
                threadDesc.actionTask = actionTask;
                threadDesc.thread = thread;
                this.threads.put( threadName, threadDesc );
            }
            threadDesc.iterationStartTime = iterationStartTime;
        }
    }

    /**
     * Called by each thread to inform an iteration is over
     */
    public void clearIterationStartTime() {

        String threadName = Thread.currentThread().getName();

        synchronized( this.threads ) {
            ThreadDescriptor threadDesc = this.threads.get( threadName );
            if( threadDesc != null ) {
                long thisThreadIterationStartTime = threadDesc.iterationStartTime;
                threadDesc.iterationStartTime = NOT_SET_START_TIME;

                if( thisThreadIterationStartTime == this.nextIterationStartTime ) {
                    // we have to recalculate the start time of the next iteration
                    this.nextIterationStartTime = NOT_SET_START_TIME;
                    for( ThreadDescriptor _threadDesc : threads.values() ) {
                        if( this.nextIterationStartTime > _threadDesc.iterationStartTime ) {
                            this.nextIterationStartTime = _threadDesc.iterationStartTime;
                        }
                    }
                }
            }
        }
    }

    /**
     * Started just before starting the threads it monitors
     */
    @Override
    public void run() {

        Thread.currentThread().setName( THREAD_NAME + Thread.currentThread().getName() );

        // waiting for the first iteration to start
        waitForNextIterationStart();

        // manage the threads until they all die
        boolean thereAreAliveThreads;
        do {
            waitForNextIterationStart();

            thereAreAliveThreads = false;
            long now = System.currentTimeMillis();

            sleepUntilNextIterationStartTime();

            // clear the next iteration start time, we will calculate it while cycling over the threads
            this.nextIterationStartTime = NOT_SET_START_TIME;

            synchronized( this.threads ) {
                // check if some iterations are late
                Iterator<ThreadDescriptor> it = this.threads.values().iterator();
                while( it.hasNext() ) {
                    ThreadDescriptor threadDesc = it.next();

                    if( !threadDesc.thread.isAlive() || threadDesc.actionTask.isExternallyInterrupted() ) {
                        // 1. thread is not alive
                        // 2. thread is externally interrupted - for example user canceled the queue
                        // in both cases, we do not want to manage this thread anymore
                        it.remove();

                        this.isQueueOver = this.threads.size() == 0; // check if there are more threads present
                    } else {
                        thereAreAliveThreads = true; // will keep cycling

                        // check if the thread is timed-out, it is still alive, probably sleeping before next iteration
                        if( !threadDesc.actionTask.isTimedOut() // do not deal with timed out threads, this flag will be cleared before next iteration
                            && threadDesc.iterationStartTime != NOT_SET_START_TIME // do not deal with threads with unknown iteration start time
                        ) {
                            if( threadDesc.iterationStartTime + timeoutMillis <= now ) {
                                // thread has hit the timeout, it is time to interrupt it

                                // 1. mark the thread as timed out.
                                // This way when it receive an InterruptedException, it will know
                                // it was interrupted due to timeout
                                threadDesc.actionTask.setTimedOut( ( int ) ( ( now
                                                                               - threadDesc.iterationStartTime )
                                                                             / 1000 ) );

                                // 2. clear the start time.
                                // If this thread sleeps between iterations, 
                                // we might meanwhile come here again because it is time to check some other thread for timeout.  
                                // Without clearing the start time, we will do timeout calculation using the old iteration start time,
                                // so this thread will appear to have timed out again, so we will interrupt it when this is not right.
                                threadDesc.iterationStartTime = NOT_SET_START_TIME;

                                // 3. interrupt this thread
                                threadDesc.thread.interrupt();
                            } else {
                                // the timeout is not hit, but we need to find the next iteration start time
                                if( this.nextIterationStartTime > threadDesc.iterationStartTime ) {
                                    this.nextIterationStartTime = threadDesc.iterationStartTime;
                                }
                            }
                        }
                    }
                }
            }
        } while( thereAreAliveThreads && !this.isQueueOver );
    }

    /**
     * Called by each thread to inform it is going down
     */
    public void shutdown() {

        String threadName = Thread.currentThread().getName();

        synchronized( this.threads ) {
            ThreadDescriptor threadDesc = this.threads.get( threadName );
            if( threadDesc != null ) {
                this.threads.remove( threadName );
                this.isQueueOver = this.threads.size() == 0;
            }
        }
    }

    /**
     * Wait for the soonest iteration to start
     */
    private void waitForNextIterationStart() {

        do {
            if( this.isQueueOver ) {
                return;
            }

            try {
                Thread.sleep( 1000 );
            } catch( InterruptedException e ) {}
        } while( this.nextIterationStartTime == NOT_SET_START_TIME );
    }

    /**
     * Sleep until the next iteration start time
     */
    private void sleepUntilNextIterationStartTime() {

        do {
            long timeToSleep = this.nextIterationStartTime + this.timeoutMillis - System.currentTimeMillis();;
            if( timeToSleep <= 0 // there was some delay, no time to sleep

                // do not have a know iteration start time, maybe the threads are currently sleeping between iterations
                || this.nextIterationStartTime == NOT_SET_START_TIME

                || this.isQueueOver // the queue is over
            ) {
                return;
            }

            // It is easier to just sleep for <timeToSleep>, but there is a corner case:
            // If user set a very long sleep, longer than the total queue execution time,
            // then we would sleep for that long period of time even after the queue is over.
            // That is why we sleep shortly and keep checking the <isQueueOver> flag
            try {
                Thread.sleep( 1000 );
            } catch( InterruptedException e ) {}
        } while( true );
    }

    class ThreadDescriptor {
        private AbstractActionTask actionTask;
        private Thread             thread;
        private long               iterationStartTime;
    }
}
