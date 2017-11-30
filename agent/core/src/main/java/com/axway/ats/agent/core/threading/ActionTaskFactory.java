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

import java.util.List;

import com.axway.ats.agent.core.action.ActionRequest;
import com.axway.ats.agent.core.exceptions.ActionExecutionException;
import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.agent.core.threading.data.ParameterDataProvider;
import com.axway.ats.agent.core.threading.exceptions.ThreadingPatternNotSupportedException;
import com.axway.ats.agent.core.threading.listeners.ActionTaskListener;
import com.axway.ats.agent.core.threading.patterns.model.ExecutionPattern;
import com.axway.ats.agent.core.threading.patterns.model.FixedDurationExecutionPattern;
import com.axway.ats.agent.core.threading.patterns.model.FixedInvocationsExecutionPattern;

public class ActionTaskFactory {

    //prevent instantiation
    private ActionTaskFactory() {

    }

    /**
     * Create an action task based on the provided input data
     * 
     * @param caller the remote caller
     * @param threadingPattern the threading pattern
     * @param threadsManager the thread iterations manager for the task
     * @param actionRequests the action requests
     * @param parameterDataProviders the data providers for the parameters
     * @param listeners the task event listeners
     * @return the created action task
     * @throws NoCompatibleMethodFoundException 
     * @throws NoSuchActionException 
     * @throws NoSuchComponentException 
     * @throws ActionExecutionException 
     * @throws ThreadingPatternNotSupportedException 
     */
    public static final Runnable createTask(
                                             String caller,
                                             String queueName,
                                             ExecutionPattern executionPattern,
                                             int executionsPerTimeFrame,
                                             ThreadsManager threadsManager,
                                             IterationTimeoutManager itManager,
                                             List<ActionRequest> actionRequests,
                                             List<ParameterDataProvider> parameterDataProviders,
                                             List<ActionTaskListener> listeners,
                                             boolean isUseSynchronizedIterations )
                                                                                   throws ActionExecutionException,
                                                                                   NoSuchComponentException,
                                                                                   NoSuchActionException,
                                                                                   NoCompatibleMethodFoundException,
                                                                                   ThreadingPatternNotSupportedException {

        if (executionPattern instanceof FixedInvocationsExecutionPattern) {
            FixedInvocationsExecutionPattern fixedInvocationsExecutionPattern = (FixedInvocationsExecutionPattern) executionPattern;

            int iterationCount = fixedInvocationsExecutionPattern.getIterationCount();
            long intervalBetweenIterations = fixedInvocationsExecutionPattern.getIntervalBetweenIterations();
            long minIntervalBetweenIterations = fixedInvocationsExecutionPattern.getMinIntervalBetweenIterations();
            long maxIntervalBetweenIterations = fixedInvocationsExecutionPattern.getMaxIntervalBetweenIterations();
            long timeFrame = fixedInvocationsExecutionPattern.getTimeFrame();

            return new MultipleInvocationsActionTask(caller,
                                                     queueName,
                                                     threadsManager,
                                                     itManager,
                                                     iterationCount,
                                                     intervalBetweenIterations,
                                                     minIntervalBetweenIterations,
                                                     maxIntervalBetweenIterations,
                                                     executionsPerTimeFrame,
                                                     timeFrame,
                                                     actionRequests,
                                                     parameterDataProviders,
                                                     listeners,
                                                     isUseSynchronizedIterations);

        } else if (executionPattern instanceof FixedDurationExecutionPattern) {

            FixedDurationExecutionPattern fixedDurationExecutionPattern = (FixedDurationExecutionPattern) executionPattern;

            int duration = fixedDurationExecutionPattern.getDuration();
            long intervalBetweenIterations = fixedDurationExecutionPattern.getIntervalBetweenIterations();
            long minIntervalpBetweenIterations = fixedDurationExecutionPattern.getMinIntervalBetweenIterations();
            long maxIntervalBetweenIterations = fixedDurationExecutionPattern.getMaxIntervalBetweenIterations();
            long timeFrame = fixedDurationExecutionPattern.getTimeFrame();

            return new FixedDurationActionTask(caller, queueName,
                                               threadsManager,
                                               itManager,
                                               duration,
                                               intervalBetweenIterations,
                                               minIntervalpBetweenIterations,
                                               maxIntervalBetweenIterations,
                                               executionsPerTimeFrame,
                                               timeFrame,
                                               actionRequests,
                                               parameterDataProviders,
                                               listeners,
                                               isUseSynchronizedIterations);
        } else {
            throw new ThreadingPatternNotSupportedException(executionPattern.getClass().getSimpleName());
        }
    }
}
