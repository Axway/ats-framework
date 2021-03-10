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
import com.axway.ats.agent.core.threading.exceptions.ActionTaskLoaderException;
import com.axway.ats.agent.core.threading.exceptions.ThreadingPatternNotSupportedException;
import com.axway.ats.agent.core.threading.listeners.QueueLoaderListener;
import com.axway.ats.agent.core.threading.patterns.model.EndPattern;
import com.axway.ats.agent.core.threading.patterns.model.ExecutionPattern;
import com.axway.ats.agent.core.threading.patterns.model.StartPattern;
import com.axway.ats.log.AtsDbLogger;

public abstract class AbstractQueueLoader implements QueueLoader {

    protected final AtsDbLogger           log;

    protected String                      queueName;
    protected StartPattern                startPattern;
    protected ExecutionPattern            executionPattern;
    protected EndPattern                  endPattern;
    protected List<ActionRequest>         actionRequests;
    protected boolean                     blockUntilCompletion;
    protected ActionTaskLoaderState       state;
    protected List<ParameterDataProvider> parameterDataProviders;
    protected List<QueueLoaderListener>   listeners;

    AbstractQueueLoader( String queueName, List<ActionRequest> actionRequests, StartPattern startPattern,
                         ExecutionPattern executionPattern, EndPattern endPattern,
                         List<ParameterDataProvider> parameterDataProviders,
                         List<QueueLoaderListener> listeners ) throws NoSuchActionException,
                                                               NoCompatibleMethodFoundException {

        /** Skip check whether DB appender is attached **/
        this.log = AtsDbLogger.getLogger(this.getClass().getName(), true);

        this.queueName = queueName;
        this.startPattern = startPattern;
        this.executionPattern = executionPattern;
        this.endPattern = endPattern;
        this.blockUntilCompletion = startPattern.isBlockUntilCompletion();
        this.actionRequests = actionRequests;
        this.state = ActionTaskLoaderState.NOT_STARTED;
        this.parameterDataProviders = parameterDataProviders;
        this.listeners = listeners;
    }

    @Override
    public abstract void scheduleThreads( String caller,
                                          boolean isUseSynchronizedIterations ) throws ActionExecutionException,
                                                                                ActionTaskLoaderException,
                                                                                NoSuchComponentException,
                                                                                NoSuchActionException,
                                                                                NoCompatibleMethodFoundException,
                                                                                ThreadingPatternNotSupportedException;

    @Override
    public abstract void start() throws ActionExecutionException, ActionTaskLoaderException;

    @Override
    public abstract void cancel();

    @Override
    public abstract void waitUntilFinished();

    @Override
    public synchronized final ActionTaskLoaderState getState() {

        return state;
    }

    /**
     * Call "on pause" for all listeners
     */
    protected final synchronized void callOnPause() {

        //set the state
        state = ActionTaskLoaderState.PAUSED;

        //notify the main thread that we are paused
        this.notifyAll();
    }

    /**
     * Call "on finish" for all listeners
     */
    protected final synchronized void callOnFinish() {

        //set the state
        state = ActionTaskLoaderState.FINISHED;

        //notify all listeners
        for (QueueLoaderListener listener : listeners) {
            listener.onFinish(queueName);
        }

        //notify the main thread that we are done
        this.notifyAll();
    }

    /**
     *
     * @return queue name
     */
    public String getName() {

        return queueName;
    }

}
