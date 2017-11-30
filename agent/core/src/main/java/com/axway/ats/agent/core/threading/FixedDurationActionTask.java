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
 * Action task that will invoke the given set of actions for a specific duration
 */
public class FixedDurationActionTask extends AbstractActionTask {

    //Duration of the all iterations(in seconds)
    private long totalDuration;
    private long endTimestamp;

    /**
     * @param caller 
     *            the remote caller
     * @param threadsManager
     *            the thread iterations manager
     * @param totalDuration
     *            the duration of the whole execution (in seconds)
     * @param intervalBetweenIterations
     *            the delay between iterations in milliseconds
     * @param totalExecutionsPerTimeFrame
     *            the number of iterations to execute in a given time frame
     * @param timeFrameLength
     *            the time frame in seconds for executing a number of iterations
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
    public FixedDurationActionTask( String caller, String queueName, ThreadsManager threadsManager,
                                    IterationTimeoutManager itManager, int totalDuration,
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

        this.totalDuration = totalDuration;

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
            endTimestamp = timeFrameStartTimestamp + totalDuration * 1000;
        }

        ActionTaskResult executionResult = null;

        try {
            //invoke all the iterations of this queue
            while (true) {

                //check if we have been interrupted
                if (Thread.interrupted()) {
                    log.debug("Actions queue '" + queueName + "' has been cancelled - exiting");
                    executionResult = ActionTaskResult.CANCELED;
                    break;
                }

                try {
                    //invoke all the actions of 1 queue iteration
                    invokeActions();

                    //Sleep if needed before starting the next iteration.
                    //We will not sleep if it goes beyond the endTimestamp as we know that there will be 
                    //no more time for another iteration
                    if (!sleepBetweenIterations(endTimestamp)) {
                        break;
                    }

                    // check for speed restriction
                    if (totalExecutionsPerTimeFrame > 0 && evaluateSpeedProgress(endTimestamp)) {
                        // waiting for end of time frame goes beyond the end time, so no more iterations will be performed
                        break;
                    }
                } catch (InterruptedException ie) {
                    log.warn("Actions queue '" + queueName + "' has been cancelled - exiting");
                    executionResult = ActionTaskResult.CANCELED;
                    break;
                }

                if (isUseSynchronizedIterations) {
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
