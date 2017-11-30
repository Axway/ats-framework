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

import com.axway.ats.agent.core.action.ActionRequest;
import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.agent.core.threading.data.ParameterDataProvider;
import com.axway.ats.agent.core.threading.exceptions.ThreadingPatternNotSupportedException;
import com.axway.ats.agent.core.threading.listeners.QueueLoaderListener;
import com.axway.ats.agent.core.threading.patterns.AllAtOncePattern;
import com.axway.ats.agent.core.threading.patterns.FixedDurationAllAtOncePattern;
import com.axway.ats.agent.core.threading.patterns.FixedDurationRampUpPattern;
import com.axway.ats.agent.core.threading.patterns.RampUpPattern;
import com.axway.ats.agent.core.threading.patterns.ThreadingPattern;
import com.axway.ats.agent.core.threading.patterns.model.ExecutionPattern;
import com.axway.ats.agent.core.threading.patterns.model.RampUpStartPattern;

/**
 * This is the factory for creating action task load queues
 */
public class LoadQueueFactory {

    //prevent instantiation
    private LoadQueueFactory() {

    }

    /**
     * Create an action task load queue based on the threading pattern selected
     * 
     * @param queueName name of the queue
     * @param actionRequests the action requests to execute with this queue
     * @param threadingPattern the threading pattern
     * @return
     * @throws NoSuchComponentException there is no registered component for one of the requested actions
     * @throws NoSuchActionException one of the actions requested does not exist
     * @throws NoCompatibleMethodFoundException if no action method is found, which corresponds to this request
     * @throws ThreadingPatternNotSupportedException if the supplied threading pattern is not supported
     */
    public static final QueueLoader createLoadQueue( String queueName,
                                                     List<ActionRequest> actionRequests,
                                                     ThreadingPattern threadingPattern,
                                                     List<ParameterDataProvider> parameterDataProviders,
                                                     List<QueueLoaderListener> listeners ) throws NoSuchComponentException,
                                                                                           NoSuchActionException,
                                                                                           NoCompatibleMethodFoundException,
                                                                                           ThreadingPatternNotSupportedException {

        if (listeners == null) {
            listeners = new ArrayList<QueueLoaderListener>();
        }

        if (threadingPattern.getClass() == AllAtOncePattern.class) {

            //convert the "all at once" to "ramp up pattern"
            AllAtOncePattern allAtOncePattern = (AllAtOncePattern) threadingPattern;
            RampUpPattern rampUpPattern = new RampUpPattern(allAtOncePattern.getThreadCount(),
                                                            allAtOncePattern.isBlockUntilCompletion(),
                                                            allAtOncePattern.getIterationCount(),
                                                            allAtOncePattern.getIntervalBetweenIterations());
            rampUpPattern.setMinIntervalBetweenIterations(allAtOncePattern.getMinIntervalBetweenIterations());
            rampUpPattern.setMaxIntervalBetweenIterations(allAtOncePattern.getMaxIntervalBetweenIterations());
            rampUpPattern.setIterationTimeout(allAtOncePattern.getIterationTimeout());

            return new RampUpQueueLoader(queueName, actionRequests, rampUpPattern, allAtOncePattern,
                                         parameterDataProviders, listeners);

        } else if (threadingPattern.getClass() == FixedDurationAllAtOncePattern.class) {

            //convert the "fixed duration all at once" to "fixed duration ramp up pattern"
            FixedDurationAllAtOncePattern allAtOncePattern = (FixedDurationAllAtOncePattern) threadingPattern;
            FixedDurationRampUpPattern rampUpPattern = new FixedDurationRampUpPattern(allAtOncePattern.getThreadCount(),
                                                                                      allAtOncePattern.isBlockUntilCompletion(),
                                                                                      allAtOncePattern.getDuration(),
                                                                                      allAtOncePattern.getIntervalBetweenIterations());
            rampUpPattern.setMinIntervalBetweenIterations(allAtOncePattern.getMinIntervalBetweenIterations());
            rampUpPattern.setMaxIntervalBetweenIterations(allAtOncePattern.getMaxIntervalBetweenIterations());
            rampUpPattern.setIterationTimeout(threadingPattern.getIterationTimeout());

            return new RampUpQueueLoader(queueName, actionRequests, rampUpPattern, allAtOncePattern,
                                         parameterDataProviders, listeners);

        } else if (threadingPattern.getClass() == RampUpPattern.class
                   || threadingPattern.getClass() == FixedDurationRampUpPattern.class) {

            //this is a real ramp-up pattern
            return new RampUpQueueLoader(queueName, actionRequests,
                                         (RampUpStartPattern) threadingPattern,
                                         (ExecutionPattern) threadingPattern, parameterDataProviders,
                                         listeners);

        } else {
            throw new ThreadingPatternNotSupportedException(threadingPattern.getClass().getSimpleName());
        }
    }
}
