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

import java.util.Calendar;
import java.util.List;

import com.axway.ats.agent.core.action.ActionRequest;
import com.axway.ats.agent.core.exceptions.ActionExecutionException;
import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.agent.core.threading.data.ParameterDataProvider;
import com.axway.ats.agent.core.threading.listeners.ActionTaskListener;

/**
 * Action task that will invoke the action queue multiple times
 */
public class MultipleInvocationsActionTask extends AbstractActionTask {

    //max number of iterations
    private final int totalIterations;
    //the number of currently executed iterations
    private int       currentIterations;

    /**
     * @param caller 
     *            the remote caller
     * @param actionInvokers
     *            list of invokers to call
     * @param threadsManager
     *            the thread iterations manager
     * @param endGate
     *            the end gate
     * @param totalIterations
     *            number of iterations
     * @param intervalBetweenIterations
     *            the delay between iterations in milliseconds
     * @param totalExecutionsPerTimeFrame
     *            the number of iterations to execute in a given time frame
     * @param timeFrameLength
     *            the time frame in seconds for executing a number of ubterations
     * @param actionRequests
     *            the action requests
     * @param dataProviders
     *            the data providers list
     * @param listeners
     *            the action task listeners
     * 
     * @throws ActionExecutionException
     *             if an action cannot be accessed
     * @throws NoCompatibleMethodFoundException
     * @throws NoSuchActionException
     * @throws NoSuchComponentException
     */
    public MultipleInvocationsActionTask( String caller, String queueName, ThreadsManager threadsManager,
                                          IterationTimeoutManager itManager, int totalIterations,
                                          long intervalBetweenIterations, long minIntervalBetweenIterations,
                                          long maxIntervalBetweenIterations, int totalExecutionsPerTimeFrame,
                                          long timeFrameLength, List<ActionRequest> actionRequests,
                                          List<ParameterDataProvider> dataProviders,
                                          List<ActionTaskListener> listeners,
                                          boolean isUseSynchronizedIterations ) throws ActionExecutionException,
                                                                                NoSuchComponentException,
                                                                                NoSuchActionException,
                                                                                NoCompatibleMethodFoundException {

        super(caller, queueName, threadsManager, itManager, actionRequests, dataProviders,
              intervalBetweenIterations, minIntervalBetweenIterations, maxIntervalBetweenIterations,
              listeners);

        this.totalIterations = totalIterations;
        this.currentIterations = 0;

        this.timeFrameLength = timeFrameLength * 1000; //convert to milliseconds
        this.timeFrameStartTimestamp = 0;
        this.totalExecutionsPerTimeFrame = totalExecutionsPerTimeFrame;
        this.currentIterationsInThisTimeFrame = 0;

        this.isUseSynchronizedIterations = isUseSynchronizedIterations;
    }

    @Override
    public ActionTaskResult execute() {

        if (timeFrameStartTimestamp == 0) {
            // we enter this method for first time
            timeFrameStartTimestamp = Calendar.getInstance().getTimeInMillis();
        }

        ActionTaskResult executionResult = null;
        try {
            //invoke all the iterations of this queue
            while (currentIterations < totalIterations) {

                // check if we have been interrupted
                if (Thread.interrupted()) {
                    log.debug("Actions queue '" + queueName + "' has been cancelled - exiting");
                    executionResult = ActionTaskResult.CANCELED;
                    break;
                }

                try {
                    //invoke all the actions of 1 queue iteration
                    invokeActions();

                    //sleep if needed before starting the next iteration
                    sleepBetweenIterations();

                    // check for speed restriction only if it's not the final iteration
                    if (totalExecutionsPerTimeFrame > 0 && currentIterations < totalIterations - 1) {
                        evaluateSpeedProgress(0);
                    }

                } catch (InterruptedException ie) {
                    log.warn("Actions queue '" + queueName + "' has been cancelled - exiting");
                    executionResult = ActionTaskResult.CANCELED;
                    break;
                }

                currentIterations++;

                if (isUseSynchronizedIterations && currentIterations < totalIterations) {
                    //we are synchronizing the iterations and we have more iterations to execute, 
                    //we have to pause now. We will be awaken when all threads have paused
                    executionResult = ActionTaskResult.PAUSED;
                    break;
                }
            }
        } finally {
            if (itManager != null && executionResult != ActionTaskResult.PAUSED) {
                // this queue is going down
                itManager.shutdown();
            }
        }

        if (executionResult == null) {
            executionResult = ActionTaskResult.FINISHED;
        }
        return executionResult;
    }
}
