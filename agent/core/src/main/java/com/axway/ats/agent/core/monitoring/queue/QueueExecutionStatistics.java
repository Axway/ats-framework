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
package com.axway.ats.agent.core.monitoring.queue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.axway.ats.agent.core.exceptions.AgentException;

/**
 * Singleton keeping info about the action execution results for each queue 
 * running on some agent
 */
public class QueueExecutionStatistics {

    private Map<String, Map<String, ActionExecutionStatistic>> actionsPerQueue = new HashMap<String, Map<String, ActionExecutionStatistic>>();

    private static final QueueExecutionStatistics              instance;

    static {
        instance = new QueueExecutionStatistics();
    }

    public static QueueExecutionStatistics getInstance() {

        return instance;
    }

    /**
     * Called by the WS API when a queue is starting
     * 
     * @param queueName
     */
    public void initActionExecutionResults(
                                            String queueName ) throws AgentException {

        Map<String, ActionExecutionStatistic> thisQueueStatics = actionsPerQueue.get( queueName );
        if( thisQueueStatics != null ) {
            // there is already information about queue with same name, maybe this is another run of same test
            // cleanup this info
            thisQueueStatics.clear();
        } else {
            // unknown queue
            actionsPerQueue.put( queueName, new HashMap<String, ActionExecutionStatistic>() );
        }
    }

    /**
     * Called by the WS API when queue is already finished
     * 
     * @param queueName
     * @return
     */
    public List<ActionExecutionStatistic> getActionExecutionResults(
                                                                     String queueName ) throws AgentException {

        Collection<ActionExecutionStatistic> thisQueueStatics = actionsPerQueue.get( queueName ).values();

        return new ArrayList<ActionExecutionStatistic>( thisQueueStatics );
    }

    /**
     * Called by any thread right after finishing the execution of an action
     * 
     * @param queueName
     * @param actionName
     * @param passed
     */
    synchronized public void registerActionExecutionResult(
                                                            String queueName,
                                                            String actionName,
                                                            boolean passed ) {

        // find the queue, we know it is available as the initialize method was already called
        Map<String, ActionExecutionStatistic> thisQueueStatics = actionsPerQueue.get( queueName );

        // find the action
        ActionExecutionStatistic thisActionStatistic = thisQueueStatics.get( actionName );
        if( thisActionStatistic == null ) {
            // new action
            thisActionStatistic = new ActionExecutionStatistic( actionName );
            thisQueueStatics.put( actionName, thisActionStatistic );
        }

        // register the action execution result
        thisActionStatistic.registerExecutionResult( passed );
    }
}
