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

import com.axway.ats.agent.core.action.ActionRequest;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.webapp.client.executors.LocalExecutor;
import com.axway.ats.agent.webapp.client.executors.RemoteExecutor;
import com.axway.ats.core.monitoring.SystemMonitorDefinitions;

/**
 * This class should be inherited by all Java clients not servicing
 * performance tests.
 * 
 * It handles the communication with the web service
 */
public abstract class ActionClient extends AbstractAgentClient {

    /**
     * Constructor for this class - sets the host on which the action to be
     * performed, as well as the component name
     *
     * @param atsAgent
     *            the ATS agent to work with - if you pass
     *            LOCAL_JVM, the action execution will be performed in the
     *            current JVM without routing through the web service
     * @param component
     *            the name of the component for which to execute an action
     */
    public ActionClient( String atsAgent,
                         String component ) {

        super( atsAgent, component );
    }

    /**
     * Execute the given action. The action will not be populated in the database
     *
     * @param actionName
     *            name of the action
     * @param arguments
     *            arguments for the action
     * @return result of the action execution
     * @throws AgentException
     *             if exception occurs during action execution
     */
    protected Object executeActionWithoutRegister( String actionName,
                                                   Object[] arguments ) throws AgentException {

        return getActionResult( actionName, arguments, false );
    }

    /**
     * Execute the given action. The action will be populated in the database
     *
     * @param actionName
     *            name of the action
     * @param arguments
     *            arguments for the action
     * @param registerAction
     *            register action in the database
     * @return result of the action execution
     * @throws AgentException
     *             if exception occurs during action execution
     */
    protected Object executeAction(
                                    String actionName,
                                    Object[] arguments ) throws AgentException {

        return getActionResult( actionName, arguments, true );
    }
    
    private Object getActionResult( 
                                    String actionName, 
                                    Object[] arguments,
                                    boolean registerAction ) throws AgentException {

        // construct an action request
        ActionRequest actionRequest = new ActionRequest( component, actionName, arguments );
        actionRequest.setRegisterActionExecution( registerAction );

        // the returned result
        Object result = null;

        // Check if we are queuing - in this case all actions will be routed to the queue
        // The exception is when we are sending command to the Monitoring Service
        ActionQueue actionQueue = ActionQueue.getCurrentInstance();
        if( !actionQueue.isInQueueMode()
            || component.equals( SystemMonitorDefinitions.ATS_SYSTEM_MONITORING_COMPONENT_NAME ) ) {
            if( atsAgent.equals( LOCAL_JVM ) ) {
                LocalExecutor localExecutor = new LocalExecutor();
                result = localExecutor.executeAction( actionRequest );
            } else {
                RemoteExecutor remoteExecutor = new RemoteExecutor( atsAgent );
                result = remoteExecutor.executeAction( actionRequest );
            }
        } else {
            actionQueue.addActionRequest( actionRequest );
        }

        return result;
    }
}
