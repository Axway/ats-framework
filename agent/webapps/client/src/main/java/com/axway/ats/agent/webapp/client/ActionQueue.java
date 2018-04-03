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
package com.axway.ats.agent.webapp.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.axway.ats.agent.core.action.ActionRequest;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.threading.data.config.LoaderDataConfig;
import com.axway.ats.agent.core.threading.patterns.ThreadingPattern;
import com.axway.ats.agent.webapp.client.executors.DistributedLoadExecutor;
import com.axway.ats.agent.webapp.client.executors.LocalLoadExecutor;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.utils.HostUtils;

/**
 * This is the client class which will queue actions instead of executing them immediately. 
 * All queued actions are executed in multiple threads according to the pattern provided.
 * 
 * We use too often static methods here, but changes will cause compilation errors in existing tests.
 */
@PublicAtsApi
public class ActionQueue {

    private static final Logger      log            = Logger.getLogger(ActionQueue.class);

    // queue instances
    // < executor thread, < queues > >
    private static Map<String, List<ActionQueue>> queueInstancesMap = new HashMap<String, List<ActionQueue>>();

    // the agents a queue runs on
    private Set<String>              atsAgents;

    // the queued actions
    private List<ActionRequest>      queuedActions;
    private boolean                  inQueueMode;

    // a queue is identified by its name and sequence
    private String                   name;
    /* 
     * Sometimes QAs create more than one queue with same name. This is done for example
     * to combine the real user activity in morning and afternoon.
     * In such case we distinguish these queues using the sequence number, so the queues are distinct,
     * but Test Explorer shows them combined in some way. 
     * 
     * In other cases, when the same queue is executed more than once, we do not use the sequence number. 
     * So we do not have distinct queues in the DB. 
     * In these cases we suppose the user does not do any significant change but simply reruns the queue. If for example
     * the threading pattern is changed Test Explorer will display the pattern used during the first execution only.
     */
    private int                      sequence;

    // we use this flag to decide if we have to wait for queue completion
    private boolean                  isQueueFinished;

    private ActionQueue( Set<String> atsAgents, String name, int sequence ) {

        queuedActions = new ArrayList<ActionRequest>();
        inQueueMode = false;

        if (atsAgents == null) {
            atsAgents = new HashSet<String>();
        }

        // add default port in case none is not provided by the user
        String[] atsAgentsArray = HostUtils.getAtsAgentsIpAndPort(new ArrayList<String>(atsAgents).toArray(new String[atsAgents.size()]));
        this.atsAgents = new HashSet<String>(Arrays.asList(atsAgentsArray));

        this.name = name;
        this.sequence = sequence;
    }

    /**
     * Get a new queue instance 
     * 
     * @param atsAgents the ATS agents this queue will work on
     * @param name the queue name
     * @return
     */
    @PublicAtsApi
    public static synchronized ActionQueue getNewInstance( Set<String> atsAgents, String name ) {

        List<ActionQueue> queueInstances = queueInstancesMap.get( Thread.currentThread().getName() );

        if( queueInstances == null ) {
            queueInstances = new ArrayList<ActionQueue>();
            queueInstancesMap.put( Thread.currentThread().getName(), queueInstances );
        }

        // find the max queue sequence for queues with this name
        int lastQueueSequence = -1;
        for( ActionQueue queue : queueInstances ) {
            if( queue.name.equals( name ) && lastQueueSequence < queue.sequence ) {
                lastQueueSequence = queue.sequence;
            }
        }

        // create a new queue instance for this name and sequence
        ActionQueue newQueue = new ActionQueue( atsAgents, name, lastQueueSequence + 1 );
        queueInstances.add( newQueue );

        return newQueue;
    }

    /**
     * @return the latest
     */
    @PublicAtsApi
    public static synchronized ActionQueue getCurrentInstance() {

        List<ActionQueue> queueInstances = queueInstancesMap.get( Thread.currentThread().getName() );

        if( queueInstances != null ) {
            return queueInstances.get( queueInstances.size() - 1 );
        } else {
            return null;
        }
    }
    
    /**
     * Start queuing the actions
     */
    public void startQueueing() {

        inQueueMode = true;
    }

    /**
     * Add an action request to the queue
     *
     * @param actionRequest the action request to add
     * @throws AgentException on error
     */
    public void addActionRequest( ActionRequest actionRequest ) throws AgentException {

        queuedActions.add(actionRequest);
    }

    /**
     * Execute all queued actions in multiple threads in the local JVM
     *
     * @param threadingPattern the multithreading pattern
     * @throws AgentException on error
     */
    public void executeQueuedActions( ThreadingPattern threadingPattern ) throws AgentException {

        executeQueuedActions(null, threadingPattern);
    }

    /**
     * Execute all queued actions in multiple threads in the local JVM
     *
     * @param threadingPattern the multithreading pattern
     * @throws AgentException on error
     */
    public void executeQueuedActions( ThreadingPattern threadingPattern,
                                      LoaderDataConfig loaderDataConfig ) throws AgentException {

        executeQueuedActions(null, threadingPattern, loaderDataConfig);
    }

    /**
     * Execute all queued actions in multiple threads
     *
     * @param atsAgents the hosts to execute to
     * @param threadingPattern the multithreading pattern
     * @throws AgentException on error
     */
    public void executeQueuedActions( List<String> atsAgents,
                                      ThreadingPattern threadingPattern ) throws AgentException {

        executeQueuedActions(atsAgents, threadingPattern, null);
    }

    /**
    * Execute all queued actions in multiple threads
    *
    * @param atsAgents the hosts to execute at
    * @param threadingPattern the multithreading pattern
    * @param loaderDataConfig the loader variable data configuration
    * @throws AgentException on error
    */
    public void executeQueuedActions( List<String> atsAgents, ThreadingPattern threadingPattern,
                                      LoaderDataConfig loaderDataConfig ) throws AgentException {

        if (threadingPattern == null) {
            throw new AgentException("Threading pattern is not specified. You must specify one");
        }

        // we are queuing actions anymore, it is time to execute the already queued actions
        inQueueMode = false;
        
        // the queue is starting right now
        isQueueFinished = false;

        //create an empty instance of the config
        if (loaderDataConfig == null) {
            loaderDataConfig = new LoaderDataConfig();
        }

        if (atsAgents == null || atsAgents.size() == 0) {
            //local execution
            LocalLoadExecutor localLoadExecutor = new LocalLoadExecutor(name, sequence, threadingPattern,
                                                                        loaderDataConfig);
            localLoadExecutor.executeActions(queuedActions);
        } else {
            if (threadingPattern.getThreadCount() < atsAgents.size()) {
                String firstHost = atsAgents.get(0);
                log.warn("The total number of threads [" + threadingPattern.getThreadCount()
                         + "] is less than the number of remote execution hosts [" + atsAgents.size()
                         + "], so all threads will be run on just one host [" + firstHost + "]");

                atsAgents = new ArrayList<String>();
                atsAgents.add(firstHost);
            }

            //distributed remote execution
            DistributedLoadExecutor distributedLoadExecutor = new DistributedLoadExecutor(name, sequence,
                                                                                          atsAgents,
                                                                                          threadingPattern,
                                                                                          loaderDataConfig);
            distributedLoadExecutor.executeActions(queuedActions);
        }

        if (threadingPattern.isBlockUntilCompletion()) {
            isQueueFinished = true;
        }
    }

    /**
     * Wait until all queues finish
     * 
     * @throws AgentException
     */
    @PublicAtsApi
    public static void waitUntilAllQueuesFinish() throws AgentException {

        List<ActionQueue> queueInstances = queueInstancesMap.get( Thread.currentThread().getName() );

        if( queueInstances != null ) {
            for( ActionQueue queue : queueInstances ) {

                if( !queue.isQueueFinished ) {
                    DistributedLoadExecutor distributedLoadExecutor = new DistributedLoadExecutor( queue.name,
                                                                                                   queue.sequence,
                                                                                                   new ArrayList<String>( queue.atsAgents ),
                                                                                                   null,
                                                                                                   null );
                    distributedLoadExecutor.waitUntilQueueFinish();
                    queue.isQueueFinished = true;
                }
            }
        }
    }

    /**
     * Wait until the specified queue finish
     * 
     * @param queueName
     * @throws AgentException
     */
    @PublicAtsApi
    public static void waitUntilQueueFinish( String queueName ) throws AgentException {

        ActionQueue queue = getRunningQueue( queueName, true );

        DistributedLoadExecutor distributedLoadExecutor = new DistributedLoadExecutor( queue.name,
                                                                                       queue.sequence,
                                                                                       new ArrayList<String>( queue.atsAgents ),
                                                                                       null, null );
        distributedLoadExecutor.waitUntilQueueFinish();
        queue.isQueueFinished = true;
    }

    /**
     * Cancel a running queue
     * 
     * @param queueName the name of the queue
     * @throws AgentException if no such queue is found
     */
    @PublicAtsApi
    public static void cancelQueue( String queueName ) throws AgentException {

        ActionQueue queue = getRunningQueue( queueName, true );
        
        DistributedLoadExecutor distributedLoadExecutor = new DistributedLoadExecutor( queue.name,
                                                                                       queue.sequence,
                                                                                       new ArrayList<String>( queue.atsAgents ),
                                                                                       null, null );
        distributedLoadExecutor.cancelAllActions();
        queue.isQueueFinished = true;

    }

    /**
     * Checks whether a queue is running
     * 
     * @param queueName the name of the queue
     * @return true - if running; false - if not running or does not exist 
     * @throws AgentException
     */
    @PublicAtsApi
    public static boolean isQueueRunning( String queueName ) throws AgentException {

        ActionQueue queue = getRunningQueue( queueName, false );

        if( queue != null ) {
            DistributedLoadExecutor distributedLoadExecutor = new DistributedLoadExecutor( queue.name,
                                                                                           queue.sequence,
                                                                                           new ArrayList<String>( queue.atsAgents ),
                                                                                           null, null );
            return distributedLoadExecutor.isQueueRunning( queueName );
        }

        return false;
    }
    
    private static ActionQueue getRunningQueue( String queueName, boolean throwErrorWhenNotFound ) throws AgentException {

        List<ActionQueue> queueInstances = queueInstancesMap.get( Thread.currentThread().getName() );

        if( queueInstances != null ) {
            for( ActionQueue queue : queueInstances ) {
                if( queue.name.equals( queueName ) && !queue.isQueueFinished ) {
                    return queue;
                }
            }
        }

        if( throwErrorWhenNotFound ) {
            throw new AgentException( "Queue with name '" + queueName + "' not found" );
        } else {
            return null;
        }
    }

    /**
     * @return true if we are in queuing mode
     */
    boolean isInQueueMode() {

        return inQueueMode;
    }
}
