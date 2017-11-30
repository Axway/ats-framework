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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.axway.ats.agent.core.action.ActionRequest;
import com.axway.ats.agent.core.exceptions.ActionExecutionException;
import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.agent.core.model.EvenLoadDistributingUtils;
import com.axway.ats.agent.core.threading.data.ParameterDataProvider;
import com.axway.ats.agent.core.threading.exceptions.ActionTaskLoaderException;
import com.axway.ats.agent.core.threading.exceptions.ThreadingPatternNotSupportedException;
import com.axway.ats.agent.core.threading.listeners.ActionTaskListener;
import com.axway.ats.agent.core.threading.listeners.QueueLoaderListener;
import com.axway.ats.agent.core.threading.patterns.model.ExecutionPattern;
import com.axway.ats.agent.core.threading.patterns.model.RampUpStartPattern;

public class RampUpQueueLoader extends AbstractQueueLoader {

    private int                       numThreads;
    private int                       numThreadsPerStep;
    private long                      rampUpInterval;

    //these are the loader variables
    private int                       numThreadGroups;
    private int                       numThreadsInLastGroup;

    //for each group of threads create threads manager which will release
    //the thread execution according to the ramp up parameters
    private ArrayList<ThreadsManager> threadsManagers;

    //single end gate to monitor when all
    //the threads have finished
    private CountDownLatch            endGate;

    //the futures for all running tasks
    private List<Future<Object>>      taskFutures;

    //the default task listeners
    private List<ActionTaskListener>  defaultTaskListeners;

    private IterationTimeoutManager   itManager;

    /**
     * @param queueName
     * @param actionRequests
     * @param rampUpPattern
     * @param parameterDataProviders
     * @param listeners
     * @throws NoSuchComponentException
     * @throws NoSuchActionException
     * @throws NoCompatibleMethodFoundException
     */
    RampUpQueueLoader( String queueName, List<ActionRequest> actionRequests,
                       RampUpStartPattern rampUpStartPattern, ExecutionPattern executionPattern,
                       List<ParameterDataProvider> parameterDataProviders,
                       List<QueueLoaderListener> listeners ) throws NoSuchComponentException,
                                                             NoSuchActionException,
                                                             NoCompatibleMethodFoundException {

        super(queueName, actionRequests, rampUpStartPattern, executionPattern, null, parameterDataProviders,
              listeners);

        this.numThreads = rampUpStartPattern.getThreadCount();
        this.numThreadsPerStep = rampUpStartPattern.getThreadCountPerStep();
        this.rampUpInterval = rampUpStartPattern.getRampUpInterval();

        //calculate the number of groups necessary
        if (rampUpInterval == 0) {
            this.numThreadGroups = 1;
            this.numThreadsInLastGroup = numThreads;
        } else {
            this.numThreadGroups = numThreads / numThreadsPerStep;
            this.numThreadsInLastGroup = numThreads % numThreadsPerStep;

            //as this is a division of integers if the remainder was > 0,
            //then we have one additional group
            if (this.numThreadsInLastGroup > 0) {
                this.numThreadGroups++;
            } else {
                this.numThreadsInLastGroup = numThreadsPerStep;
            }
        }

        this.taskFutures = new ArrayList<Future<Object>>();

        //init the default listeners
        this.defaultTaskListeners = new ArrayList<ActionTaskListener>();
        this.defaultTaskListeners.add(new SimpleActionTaskListener());
    }

    @Override
    public synchronized void
            scheduleThreads( String caller,
                             boolean isUseSynchronizedIterations ) throws ActionExecutionException,
                                                                   ActionTaskLoaderException,
                                                                   NoSuchComponentException,
                                                                   NoSuchActionException,
                                                                   NoCompatibleMethodFoundException,
                                                                   ThreadingPatternNotSupportedException {

        //check the state first
        if (state != ActionTaskLoaderState.NOT_STARTED) {
            throw new ActionTaskLoaderException("Cannot schedule load queue " + queueName
                                                + " - it has already been scheduled");
        }

        //create the executor - terminate threads when finished
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        executor.setKeepAliveTime(0, TimeUnit.SECONDS);

        ExecutorCompletionService<Object> executionService = new ExecutorCompletionService<Object>(executor);

        //synchronization aids
        threadsManagers = new ArrayList<ThreadsManager>();

        // create the thread for managing max iteration length
        int iterationTimeout = startPattern.getIterationTimeout();
        if (iterationTimeout > 0) {
            itManager = new IterationTimeoutManager(iterationTimeout);
        }

        taskFutures = new ArrayList<Future<Object>>();

        for (int i = 0; i < numThreadGroups; i++) {

            //create the thread iterations manager for this group
            ThreadsManager threadsManager = new ThreadsManager();

            int numThreadsInGroup = (i != numThreadGroups - 1)
                                                               ? numThreadsPerStep
                                                               : numThreadsInLastGroup;
            //distribute executions per timeFrame according to the number of threads
            int executionsPerTimeFrame = executionPattern.getExecutionsPerTimeFrame();
            int[] executionsPerTimeFramePerThread = new EvenLoadDistributingUtils().getEvenLoad(executionsPerTimeFrame,
                                                                                                numThreads);
            if (numThreads > executionsPerTimeFrame && executionsPerTimeFrame > 0) {
                throw new ActionTaskLoaderException("We cannot evenly distribute " + executionsPerTimeFrame
                                                    + " iterations per time frame to " + numThreads
                                                    + " threads. Iterations per time frame must be at least as many as threads!");
            }

            for (int j = 0; j < numThreadsInGroup; j++) {
                Future<Object> taskFuture = executionService.submit(ActionTaskFactory.createTask(caller,
                                                                                                 queueName,
                                                                                                 executionPattern,
                                                                                                 executionsPerTimeFramePerThread[j],
                                                                                                 threadsManager,
                                                                                                 itManager,
                                                                                                 actionRequests,
                                                                                                 parameterDataProviders,
                                                                                                 defaultTaskListeners,
                                                                                                 isUseSynchronizedIterations),
                                                                    null);
                taskFutures.add(taskFuture);
            }

            threadsManagers.add(threadsManager);
        }

        state = ActionTaskLoaderState.SCHEDULED;
    }

    @Override
    public synchronized void start() throws ActionExecutionException, ActionTaskLoaderException {

        //check the state first
        if (state != ActionTaskLoaderState.SCHEDULED) {
            throw new ActionTaskLoaderException("Cannot start load queue " + queueName
                                                + " - it has not been scheduled yet");
        }

        state = ActionTaskLoaderState.RUNNING;

        if (blockUntilCompletion || rampUpInterval == 0) {
            // we can stay here until all tasks are started
            startAllTasks();
        } else {
            // we know it will take time until all tasks are started
            // but we are not allowed to wait - so we start all the
            // tasks from another thread
            Thread helpThread = new Thread(new Runnable() {

                @Override
                public void run() {

                    startAllTasks();
                }
            });
            helpThread.start();
        }

        //block until completed if necessary
        if (blockUntilCompletion) {
            waitUntilFinished();
        }
    }

    @Override
    public synchronized void resume() throws ActionExecutionException, ActionTaskLoaderException {

        //check the state first
        if (state != ActionTaskLoaderState.PAUSED) {
            throw new ActionTaskLoaderException("Cannot resume load queue " + queueName
                                                + " - it has not been paused yet");
        }

        state = ActionTaskLoaderState.RUNNING;

        startAllTasks();
    }

    private void startAllTasks() {

        // start iterations timeout manager before starting the threads
        if (itManager != null && !itManager.isAlive()) {
            itManager.start();
        }

        this.endGate = new CountDownLatch(numThreads);

        //start all thread groups one by one
        for (int i = 0; i < threadsManagers.size(); i++) {

            threadsManagers.get(i).start();
            try {
                Thread.sleep(rampUpInterval);
            } catch (InterruptedException ie) {
                log.error("Interrupted exception caught at before starting thread group " + (i + 1), ie);
            }
        }
    }

    @Override
    public void cancel() {

        //cancel only if still running
        log.debug("Cancelling all tasks");

        for (Future<Object> taskFuture : taskFutures) {
            taskFuture.cancel(true);

            log.debug("Cancelled all tasks with state " + state);
        }

        //notify the listeners
        callOnFinish();
    }

    @Override
    public synchronized void waitUntilFinished() {

        //wait only if the load queue has already been started or resumed
        //if it was only scheduled, then we don't need to wait
        while (state == ActionTaskLoaderState.RUNNING || state == ActionTaskLoaderState.PAUSED) {
            //block until all tasks exit
            try {
                wait();
            } catch (InterruptedException ie) {
                log.error("Interrupted exception caught", ie);
            }
        }
    }

    @Override
    public synchronized boolean waitUntilPaused() {

        if (state == ActionTaskLoaderState.PAUSED) {
            return true;
        }

        //wait only if the load queue has already been running
        if (state == ActionTaskLoaderState.RUNNING) {
            //block until all tasks exit
            try {
                wait();
            } catch (InterruptedException ie) {
                log.error("Interrupted exception caught", ie);
            }
        }

        return state == ActionTaskLoaderState.PAUSED;
    }

    private class SimpleActionTaskListener implements ActionTaskListener {

        @Override
        public void onStart() {

            log.registerThreadWithLoadQueue(queueName);
        }

        @Override
        public synchronized void onPause() {

            //this task has paused, so decrement the end gate counter
            endGate.countDown();

            //if all tasks have paused we need to update the load queue state
            if (endGate.getCount() == 0) {

                //notify the listeners that execution has paused
                callOnPause();
            }
        }

        @Override
        public synchronized void onFinish( Throwable throwable ) {

            if (throwable != null) {

                //log the error
                log.error("Exception caught while executing a task", throwable);
            }

            //this task has finished, so decrement the end gate counter
            endGate.countDown();

            //if all tasks have finished we need to end
            if (endGate.getCount() == 0) {

                //notify the listeners that execution has finished
                callOnFinish();
            }
        }
    }
}
