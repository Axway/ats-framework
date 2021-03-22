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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Instances of this class are used to manage the start of one or many threads, once or many times.
 * 
 * The main thread creates an instance of this class and passes it to all threads (workers) it will manage.
 * The main thread calls START which starts 1 iteration for all threads.
 * Each worker thread calls the WAIT FOR START and it will wait to be awaken(if no START for this iteration
 * is yet fired) or it will start right away(if a START was already fired for this iteration). Once started, the worker 
 * will run just once and then will call again WAIT FOR START.
 * 
 * The main thread is responsible to call START after all threads have completed their previous iteration.
 * 
 * As some threads run quicker then others, it happens that the quick threads request start signal again
 * while others are still running. That is why we remember the threads we have granted START for each iteration, 
 * so we do not allow them to start again in same iteration, but instead they are put to WAIT for the next iteration.
 *
 */
public class ThreadsManager {

    private static Logger      log                       = LogManager.getLogger(ThreadsManager.class);

    private int                iterationCounter;

    // lockers for each iteration
    private List<RunningState> iterationLockers          = new ArrayList<RunningState>();
    // the processed thread for each iteration
    private List<Set<Long>>    iterationProcessedThreads = new ArrayList<Set<Long>>();

    public ThreadsManager() {

        iterationCounter = -1;

        // we are always prepared for one iteration ahead for the case a thread is very fast and quickly request to executed again
        iterationLockers.add(new RunningState(false));
        iterationProcessedThreads.add(Collections.synchronizedSet(new HashSet<Long>()));
    }

    /**
     * Called by the main thread when it is time to start a new iteration
     */
    public void start() {

        // first allocate the needed structures that will be needed by the iteration after the next one(always one ahead)
        iterationLockers.add(new RunningState(false));
        iterationProcessedThreads.add(Collections.synchronizedSet(new HashSet<Long>()));

        // move to the next iteration
        iterationCounter++;

        // We do not need anymore the info about the previous iteration.
        // We do not remove the internal object from the list, we just release the not needed memory
        if (iterationCounter - 1 >= 0) {
            iterationProcessedThreads.get(iterationCounter - 1).clear();
        }

        // get the lock for this iteration
        RunningState thisIterationLocker = iterationLockers.get(iterationCounter);
        synchronized (thisIterationLocker) {
            // Start the current iteration

            // First we set the running state to TRUE and then wake up the already waiting thread.
            // If thread was too slow to request WAIT FOR START, it will not be blocked, but we will
            // let it run as the current iteration state is RUNNING
            thisIterationLocker.setRunning(true);
            thisIterationLocker.notifyAll();
        }
    }

    /**
     * Called by the work threads in order to start their next iteration
     */
    public void waitForStart() {

        // remember the current iteration counter(in case the main thread moves to new iteration meanwhile)
        int thisIterationCounter = iterationCounter;
        if (thisIterationCounter < 0) {
            // this is the case when a worker request START signal for first time, 
            // but the main thread could not fire the event quickly enough
            thisIterationCounter = 0;
        }

        // we distinguish the workers by their IDs, the id cannot be reused in the same Agent action queue
        Long threadId = Thread.currentThread().getId();

        // check if the worker already run during this iteration
        Set<Long> thisIterationProcessedThreads = iterationProcessedThreads.get(thisIterationCounter);
        if (!thisIterationProcessedThreads.contains(threadId)) {
            // not processed in this iteration
            thisIterationProcessedThreads.add(threadId);

            // get the locker for this iteration
            RunningState thisIterationLocker = iterationLockers.get(thisIterationCounter);
            synchronized (thisIterationLocker) {
                if (thisIterationLocker.isRunning()) {
                    // This iteration is already running, let the worker go
                } else {
                    try {
                        // This iteration is not running yet, worker must wait
                        thisIterationLocker.wait();
                    } catch (InterruptedException e) {
                        log.warn("Thread " + Thread.currentThread().getName()
                                 + " was interrupted while waiting to be awaken by the main thread for iteration "
                                 + thisIterationCounter
                                 + ". This will probably lead to have the thread running earlier than expected");
                        throw new RuntimeException(e); //throw exception, so the current future task could be stopped
                    }
                }
            }
        } else {
            // already processed in this iteration, block it for next iteration
            int nextIterationCounter = thisIterationCounter + 1;
            Set<Long> nextIterationProcessedThreads = iterationProcessedThreads.get(nextIterationCounter);
            nextIterationProcessedThreads.add(threadId);

            RunningState nextIterationLocker = iterationLockers.get(nextIterationCounter);
            synchronized (nextIterationLocker) {
                if (nextIterationLocker.isRunning()) {
                    /* 
                     * The next iteration is already running, let the worker go
                     * 
                     * This is a rare case, but it happens:
                     * The worker enters this method, remembers the current iteration number,
                     * checks it was run in the current iteration, so it must be queued for the next iteration,
                     * and just at this moment the main thread fires START, the iteration counter is increased and
                     * the iteration state is set to RUNNING. Right now if the worker decide to WAIT, it will never
                     * be awaken as the START event is already gone.
                     * 
                     * That is why we have to check the RUNNING state
                     */
                } else {
                    try {
                        // The next iteration is not running yet, worker must wait
                        nextIterationLocker.wait();
                    } catch (InterruptedException e) {
                        log.warn("Thread " + Thread.currentThread().getName()
                                 + " was interrupted while waiting to be awaken by the main thread for iteration "
                                 + thisIterationCounter
                                 + ". This will probably lead to have the thread running earlier than expected");
                        throw new RuntimeException(e); //throw exception, so the current future task could be stopped
                    }
                }
            }
        }
    }

    class RunningState {
        private boolean isRunning;

        RunningState( boolean isRunning ) {

            this.isRunning = isRunning;
        }

        boolean isRunning() {

            return isRunning == true;
        }

        void setRunning( boolean isRunning ) {

            this.isRunning = isRunning;
        }
    }
}
