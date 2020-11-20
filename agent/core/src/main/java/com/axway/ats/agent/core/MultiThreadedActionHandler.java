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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.axway.ats.agent.core.action.ActionRequest;
import com.axway.ats.agent.core.exceptions.ActionExecutionException;
import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.agent.core.threading.AbstractQueueLoader;
import com.axway.ats.agent.core.threading.ActionTaskLoaderState;
import com.axway.ats.agent.core.threading.LoadQueueFactory;
import com.axway.ats.agent.core.threading.QueueLoader;
import com.axway.ats.agent.core.threading.data.ParameterDataProvider;
import com.axway.ats.agent.core.threading.data.ParameterDataProviderFactory;
import com.axway.ats.agent.core.threading.data.config.LoaderDataConfig;
import com.axway.ats.agent.core.threading.data.config.ParameterDataConfig;
import com.axway.ats.agent.core.threading.exceptions.ActionTaskLoaderException;
import com.axway.ats.agent.core.threading.exceptions.LoadQueueAlreadyExistsException;
import com.axway.ats.agent.core.threading.exceptions.NoSuchLoadQueueException;
import com.axway.ats.agent.core.threading.exceptions.ParameterDataProviderInitalizationException;
import com.axway.ats.agent.core.threading.exceptions.ParameterDataProviderNotSupportedException;
import com.axway.ats.agent.core.threading.exceptions.ThreadingPatternNotSupportedException;
import com.axway.ats.agent.core.threading.listeners.QueueLoaderListener;
import com.axway.ats.agent.core.threading.patterns.ThreadingPattern;
import com.axway.ats.log.AtsDbLogger;

/**
 * Responsible for handling execution of actions in multiple threads
 */
public class MultiThreadedActionHandler {

    /*
     * Skip checking in db appender is attached, because we are on the agent and not the executor.
     * Also we want for actions to be executed on the agent even if data will not be sent to ATS Log database
     * */
    private static final AtsDbLogger                       log                           =
            AtsDbLogger.getLogger(MultiThreadedActionHandler.class.getName(), true);

    // Map, holding all the MultiThreadedActionHandler instances for each Caller
    // < caller id, MultiThreadedActionHandler >
    private static Map<String, MultiThreadedActionHandler> multiThreadedActionHandlerMap = new HashMap<>();

    // List holding all the load queues
    // < queue name, queue loader>
    private Map<String, QueueLoader>                       queueLoadersMap;

    private List<QueueLoaderListener>                      listeners;

    private MultiThreadedActionHandler() {

        this.queueLoadersMap = new HashMap<String, QueueLoader>();

        this.listeners = new ArrayList<QueueLoaderListener>();

        //add the default listeners
        this.listeners.add(new SimpleLoadQueueListener());
    }

    /**
     * Get the singleton instance without any additional listeners
     *
     * @return the instance
     */
    public static synchronized MultiThreadedActionHandler getInstance( String caller ) {

        MultiThreadedActionHandler instance = multiThreadedActionHandlerMap.get(caller);
        if (instance == null) {
            instance = new MultiThreadedActionHandler();
            multiThreadedActionHandlerMap.put(caller, instance);
        }

        return instance;
    }

    public static synchronized void cancellAllQueuesFromAgent( String caller ) {

        Iterator<String> it = multiThreadedActionHandlerMap.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            if (key.equals(caller)) {
                multiThreadedActionHandlerMap.get(key).cancelAllQueues();
            } else {
                log.warn("Remaining queues from Caller [" + caller + "]: ");
                log.warn(multiThreadedActionHandlerMap.get(key).queueLoadersMap.keySet().toString());
            }
        }
    }

    /**
     * Execute the given set of actions in multiple threads
     *
     * @param caller the remote caller
     * @param queueName name of the queue
     * @param actionRequests the actions
     * @param threadingPattern the pattern according to which to start the threads
     * @throws NoSuchComponentException if one of the actions references a component which is not registered
     * @throws NoSuchActionException if one of the actions does not exist
     * @throws NoCompatibleMethodFoundException if a method for executing the given action is not available
     * @throws ActionExecutionException if the action cannot be executed
     * @throws ThreadingPatternNotSupportedException if the threading pattern is not supported
     * @throws NoSuchLoadQueueException
     * @throws ActionTaskLoaderException
     * @throws LoadQueueAlreadyExistsException
     * @throws ParameterDataProviderNotSupportedException
     * @throws ParameterDataProviderInitalizationException
     */
    public void executeActions( String caller, String queueName, int queueId,
                                List<ActionRequest> actionRequests, ThreadingPattern threadingPattern,
                                LoaderDataConfig loaderDataConfig ) throws NoSuchComponentException,
                                                                    NoSuchActionException,
                                                                    NoCompatibleMethodFoundException,
                                                                    ActionExecutionException,
                                                                    ThreadingPatternNotSupportedException,
                                                                    NoSuchLoadQueueException,
                                                                    ActionTaskLoaderException,
                                                                    LoadQueueAlreadyExistsException,
                                                                    ParameterDataProviderNotSupportedException,
                                                                    ParameterDataProviderInitalizationException {

        scheduleActions(caller, queueName, queueId, actionRequests, threadingPattern, loaderDataConfig,
                        false);
        startQueue(queueName);
    }

    /**
     * 
     * @param caller
     * @param queueName
     * @param queueId
     * @param actionRequests
     * @param threadingPattern
     * @param loaderDataConfig
     * @param isUseSynchronizedIterations
     * @throws NoSuchComponentException
     * @throws NoSuchActionException
     * @throws NoCompatibleMethodFoundException
     * @throws ThreadingPatternNotSupportedException
     * @throws ActionExecutionException
     * @throws ActionTaskLoaderException
     * @throws LoadQueueAlreadyExistsException
     * @throws ParameterDataProviderNotSupportedException
     * @throws ParameterDataProviderInitalizationException
     */
    public void scheduleActions( String caller, String queueName, int queueId,
                                 List<ActionRequest> actionRequests, ThreadingPattern threadingPattern,
                                 LoaderDataConfig loaderDataConfig,
                                 boolean isUseSynchronizedIterations ) throws NoSuchComponentException,
                                                                       NoSuchActionException,
                                                                       NoCompatibleMethodFoundException,
                                                                       ThreadingPatternNotSupportedException,
                                                                       ActionExecutionException,
                                                                       ActionTaskLoaderException,
                                                                       LoadQueueAlreadyExistsException,
                                                                       ParameterDataProviderNotSupportedException,
                                                                       ParameterDataProviderInitalizationException {

        //first cleanup the queues
        cleanupFinishedQueues();

        //check if we already have this queue and it has not finished yet
        //if the queue has finished, we can simply discard it
        QueueLoader queueLoader = queueLoadersMap.get(queueName);
        if (queueLoader != null) {

            throw new LoadQueueAlreadyExistsException(queueName, queueLoader.getState());
        }

        //create the data providers
        List<ParameterDataProvider> parameterDataProviders = new ArrayList<ParameterDataProvider>();
        for (ParameterDataConfig paramDataConfigs : loaderDataConfig.getParameterConfigurations()) {
            parameterDataProviders.add(ParameterDataProviderFactory.createDataProvider(paramDataConfigs));
        }

        //create the loader
        queueLoader = LoadQueueFactory.createLoadQueue(queueName, actionRequests, threadingPattern,
                                                       parameterDataProviders, listeners);

        log.rememberLoadQueueState(queueName, queueId, threadingPattern.getPatternDescription(),
                                   threadingPattern.getThreadCount());

        //start the queue
        queueLoader.scheduleThreads(caller, isUseSynchronizedIterations);
        queueLoadersMap.put(queueName, queueLoader);

        log.info("Scheduled queue '" + queueName + "'");
    }

    /**
     * @param queueName queue name
     * @throws NoSuchLoadQueueException
     * @throws ActionExecutionException
     * @throws ActionTaskLoaderException
     */
    public void startQueue( String queueName ) throws NoSuchLoadQueueException, ActionExecutionException,
                                               ActionTaskLoaderException {

        //first cleanup the queues
        cleanupFinishedQueues();

        QueueLoader queueLoader = queueLoadersMap.get(queueName);
        if (queueLoader == null) {
            throw new NoSuchLoadQueueException(queueName);
        }

        log.info("Starting queue '" + queueName + "'");

        //start the queue
        queueLoader.start();
    }

    public void resumeQueue( String actionQueueName ) throws NoSuchLoadQueueException,
                                                      ActionExecutionException, ActionTaskLoaderException {

        //first cleanup the queues
        cleanupFinishedQueues();

        QueueLoader queueLoader = queueLoadersMap.get(actionQueueName);
        if (queueLoader == null) {
            throw new NoSuchLoadQueueException(actionQueueName);
        }

        //resume the queue
        queueLoader.resume();
    }

    /**
     * Cancel all scheduled and started queues
     */
    public void cancelAllQueues() {

        //cancel all queues
        for (QueueLoader queueLoader : queueLoadersMap.values()) {
            queueLoader.cancel();
            if (queueLoader instanceof AbstractQueueLoader) {

                log.info("Cancelled execution of queue '" + ((AbstractQueueLoader) queueLoader).getName()
                         + "'");
            }
        }

        // cleanup the FINISHED queues
        cleanupFinishedQueues();
    }

    /**
     * Cancel queue
     */
    public void cancelQueue( String queueName ) {

        //cancel queue
        for (QueueLoader queueLoader : queueLoadersMap.values()) {
            if ( (queueLoader instanceof AbstractQueueLoader)
                 && ((AbstractQueueLoader) queueLoader).getName().equals(queueName)) {

                queueLoader.cancel();
            }
        }

        log.info("Cancelled execution of queue '" + queueName + "'");

        // cleanup the FINISHED queues
        cleanupFinishedQueues();
    }

    /**
     * @param queueName the queue name
     * @return if the queue is still running
     */
    public boolean isQueueRunning( String queueName ) {

        // search for the queue
        for (QueueLoader queueLoader : queueLoadersMap.values()) {
            if ( (queueLoader instanceof AbstractQueueLoader)
                 && ((AbstractQueueLoader) queueLoader).getName().equals(queueName)) {

                return true;
            }
        }

        return false;
    }

    /**
     * Wait until a given queue finishes
     *
     * @param queueName name of the queue
     * @throws NoSuchLoadQueueException if such queue has not been started
     */
    public void waitUntilQueueFinish( String queueName ) throws NoSuchLoadQueueException {

        QueueLoader queueLoader = queueLoadersMap.get(queueName);
        if (queueLoader == null) {
            log.warn("We will not wait for queue with name '" + queueName
                     + "' to finish as such queue is not present");
        } else {
            //wait until the queue finishes
            queueLoader.waitUntilFinished();

            // cleanup the FINISHED queues
            cleanupFinishedQueues();
        }
    }

    /**
     * Wait until a all started queues finish
     */
    public void waitUntilAllQueuesFinish() {

        Set<QueueLoader> queueLoadersClone = new HashSet<QueueLoader>(queueLoadersMap.values());
        for (QueueLoader queueLoader : queueLoadersClone) {
            //wait until the queue finishes
            queueLoader.waitUntilFinished();
        }

        // cleanup the FINISHED queues
        cleanupFinishedQueues();
    }

    /**
     * Wait for a queue to be paused
     * @param queueName the name of the queue
     * @return if it was PAUSED. For example it could be FINISHED or interrupted while RUNNING
     * @throws NoSuchLoadQueueException
     */
    public boolean waitUntilQueueIsPaused( String queueName ) throws NoSuchLoadQueueException {

        QueueLoader queueLoader = queueLoadersMap.get(queueName);
        if (queueLoader == null) {
            throw new NoSuchLoadQueueException(queueName);
        }

        //wait until the queue is paused
        return queueLoader.waitUntilPaused();
    }

    /**
     * Get the count of all running queues
     */
    public int getRunningQueuesCount() {

        int count = 0;
        for (QueueLoader queueLoader : queueLoadersMap.values()) {
            if (queueLoader.getState() == ActionTaskLoaderState.RUNNING) {
                count++;
            }
        }

        return count;
    }

    /**
     * Cleanup any finished queues
     */
    private synchronized void cleanupFinishedQueues() {

        for (Iterator<QueueLoader> iterator = queueLoadersMap.values().iterator(); iterator.hasNext();) {
            QueueLoader currentQueueLoader = iterator.next();
            if (currentQueueLoader.getState() == ActionTaskLoaderState.FINISHED) {
                iterator.remove();
            }
        }
    }

    /**
     * This listener is responsible for removing the agent from the list
     * of agents once it is done
     */
    private class SimpleLoadQueueListener implements QueueLoaderListener {

        public void onFinish( String actionQueueName ) {

            log.cleanupLoadQueueState(actionQueueName);

            log.info("Finished executing queue '" + actionQueueName + "'");
        }
    }
}
