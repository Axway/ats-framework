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
import com.axway.ats.agent.webapp.client.AbstractAgentClient;
import com.axway.ats.agent.webapp.client.ActionQueue;
import com.axway.ats.agent.webapp.client.executors.LocalExecutor;
import com.axway.ats.agent.webapp.client.executors.RemoteExecutor;
import com.axway.ats.core.monitoring.SystemMonitorDefinitions;
import com.axway.ats.core.utils.HostUtils;

/**
 * This class should be inherited by all Java clients not servicing
 * performance tests.
 * 
 * It handles the communication with the web service
 */
public abstract class ActionClient extends AbstractAgentClient {

    private String           initializeRequestUrl;

    protected RemoteExecutor remoteExecutor;

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
     * @throws AgentException 
     */
    public ActionClient( String atsAgent,
                         String component ) {

        super(atsAgent, component);

    }

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
     * @param initializeRequestUrl 
     *            the URL that will be used to tell the ATS Agent to initialize actions that will be executed via this class
     * @throws AgentException 
     */
    public ActionClient( String atsAgent,
                         String component,
                         String initializeRequestUrl ) throws AgentException {

        super(atsAgent, component);

        this.initializeRequestUrl = initializeRequestUrl;
        remoteExecutor = new RemoteExecutor(HostUtils.getAtsAgentIpAndPort(atsAgent), this.initializeRequestUrl);

    }

    /**
     * Execute the given action. The action will not be registered in the database
     *
     * @param actionName name of the action
     * @param arguments arguments for the action
     * @return result of the action execution
     * @throws AgentException if exception occurs during action execution
     */
    protected Object executeActionWithoutRegister( String actionName,
                                                   Object[] arguments ) throws AgentException {

        return getActionResult(actionName, arguments, new String[]{}, new String[]{}, false, null, null, null, null);
    }

    /**
     * Execute the given action. The action will not be registered in the database
     *
     * @param actionName name of the action
     * @param arguments arguments for the action
     * @param metaKeys some meta info key
     * @param metaValues some meta info value
     * @return result of the action execution
     * @throws AgentException if exception occurs during action execution
     */
    protected Object executeActionWithoutRegister( String actionName,
                                                   Object[] arguments,
                                                   String[] metaKeys,
                                                   String[] metaValues ) throws AgentException {

        return getActionResult(actionName, arguments, metaKeys, metaValues, false, null, null, null, null);
    }
    
    /**
     * Execute the given action. The action will not be registered in the database
     *
     * @param actionName name of the action
     * @param arguments arguments for the action
     * @param metaKeys some meta info key
     * @param metaValues some meta info value
     * @param requestUrl some meta info key
     * @param httpMethod some meta info value
     * @param argumentsNames some meta info value
     * @param returnType some meta info value
     * @return result of the action execution
     * @throws AgentException if exception occurs during action execution
     */
    protected Object executeActionWithoutRegister( String actionName,
                                                   Object[] arguments,
                                                   String[] metaKeys,
                                                   String[] metaValues,
                                                   String requestUrl,
                                                   String httpMethod,
                                                   String[] argumentsNames,
                                                   Class<?> returnType ) throws AgentException {

        return getActionResult(actionName, arguments, metaKeys, metaValues, false, requestUrl, httpMethod, argumentsNames, returnType);
    }
    
    /**
     * Execute the given action. The action will not be registered in the database
     *
     * @param actionName name of the action
     * @param arguments arguments for the action
     * @param requestUrl some meta info key
     * @param httpMethod some meta info value
     * @param argumentsNames some meta info value
     * @param returnType some meta info value
     * @return result of the action execution
     * @throws AgentException if exception occurs during action execution
     */
    protected Object executeActionWithoutRegister( String actionName,
                                                   Object[] arguments,
                                                   String requestUrl,
                                                   String httpMethod,
                                                   String[] argumentsNames,
                                                   Class<?> returnType ) throws AgentException {

        return getActionResult(actionName, arguments, null, null, false, requestUrl, httpMethod, argumentsNames, returnType);
    }

    /**
     * Execute the given action. The action will be registered in the database
     *
     * @param actionName name of the action
     * @param arguments arguments for the action
     * @return result of the action execution
     * @throws AgentException if exception occurs during action execution
     */
    protected Object executeAction(
                                    String actionName,
                                    Object[] arguments ) throws AgentException {

        return getActionResult(actionName, arguments, new String[]{}, new String[]{}, true, null, null, null, null);
    }

    /**
     * Execute the given action. The action will be registered in the database
     *
     * @param actionName name of the action
     * @param arguments arguments for the action
     * @param metaKeys some meta info key
     * @param metaValues some meta info value
     * @return result of the action execution
     * @throws AgentException if exception occurs during action execution
     */
    protected Object executeAction(
                                    String actionName,
                                    Object[] arguments,
                                    String[] metaKeys,
                                    String[] metaValues ) throws AgentException {

        return getActionResult(actionName, arguments, metaKeys, metaValues, true, null, null, null, null);
    }

    /**
     * Execute the given action. The action will be registered in the database
     *
     * @param actionName name of the action
     * @param arguments arguments for the action
     * @param requestUrl some meta info key
     * @param httpMethod some meta info value
     * @param argumentsNames some meta info value
     * @param returnType some meta info value
     * @return result of the action execution
     * @throws AgentException if exception occurs during action execution
     */
    protected Object executeAction(
                                    String actionName,
                                    Object[] arguments,
                                    String requestUrl,
                                    String httpMethod,
                                    String[] argumentsNames,
                                    Class<?> returnType ) throws AgentException {

        return getActionResult(actionName, arguments, null, null, true, requestUrl, httpMethod, argumentsNames,
                               returnType);
    }

    /**
     * Execute the given action. The action will be registered in the database
     * 
     * @param componentName name of the component
     * @param actionName name of the action
     * @param args arguments
     * @param metaKeys some meta info key
     * @param metaValues some meta info value
     * @param requestUrl the URL that will be used to communicate with the agent
     * @param requestMethod the HTTP method that will be used to communicate with the agent
     * @param requestBody the request body that will be send to the Agent
     * @param argumentsNames the names of each argument
     *        These names, along with the arguments values will construct the HTTP request JSON body
     * @param returnType the class object of this request's response result
     */
    protected Object executeAction(
                                    String actionName,
                                    Object[] arguments,
                                    String[] metaKeys,
                                    String[] metaValues,
                                    String requestUrl,
                                    String httpMethod,
                                    String[] argumentsNames,
                                    Class<?> returnType ) throws AgentException {

        return getActionResult(actionName, arguments, metaKeys, metaValues, true, requestUrl, httpMethod,
                               argumentsNames, returnType);
    }

    /**
     * Execute the given action. The action will be registered in the database
     * 
     * @param componentName name of the component
     * @param actionName name of the action
     * @param args arguments
     * @param metaKeys some meta info key
     * @param metaValues some meta info value
     * @param requestUrl the URL that will be used to communicate with the agent
     * @param requestMethod the HTTP method that will be used to communicate with the agent
     * @param requestBody the request body that will be send to the Agent
     * @param argumentsNames the names of each argument
     *        These names, along with the arguments values will construct the HTTP request JSON body
     * @param returnType the class object of this request's response result
     * @param registerAction whether to register/log the action in the database
     */
    protected Object executeAction(
                                    String actionName,
                                    Object[] arguments,
                                    String[] metaKeys,
                                    String[] metaValues,
                                    String requestUrl,
                                    String httpMethod,
                                    String[] argumentsNames,
                                    Class<?> returnType,
                                    boolean registerAction ) throws AgentException {

        return getActionResult(actionName, arguments, metaKeys, metaValues, registerAction, requestUrl, httpMethod,
                               argumentsNames, returnType);
    }

    private Object getActionResult(
                                    String actionName,
                                    Object[] arguments,
                                    String[] metaKeys,
                                    String[] metaValues,
                                    boolean registerAction,
                                    String requestUrl,
                                    String httpMethod,
                                    String[] argumentsNames,
                                    Class<?> returnType ) throws AgentException {

        // construct an action request
        ActionRequest actionRequest = new ActionRequest(component, actionName, arguments, requestUrl, httpMethod,
                                                        argumentsNames, returnType);
        actionRequest.setRegisterActionExecution(registerAction);
        if (metaKeys != null && metaKeys.length > 0) {
            applyMetaData(actionRequest, metaKeys, metaValues);
        }

        // the returned result
        Object result = null;

        // Check if we are queuing - in this case all actions will be routed to the queue
        // The exception is when we are sending command to the Monitoring Service
        ActionQueue actionQueue = ActionQueue.getCurrentInstance();
        if (actionQueue == null || !actionQueue.isInQueueMode()
            || component.equals(SystemMonitorDefinitions.ATS_SYSTEM_MONITORING_COMPONENT_NAME)) {
            if (atsAgent.equals(LOCAL_JVM)) {
                LocalExecutor localExecutor = new LocalExecutor();
                result = localExecutor.executeAction(actionRequest);
            } else {
                result = remoteExecutor.executeAction(actionRequest);
            }
        } else {
            actionQueue.addActionRequest(actionRequest);
        }

        return result;
    }

    /**
     * Applies data which affects the way an action is executed.
     * It is assumed that meta data arrays are never null and are always same size
     * 
     * @param actionRequest the action request to apply on
     * @param metaKeys the meta key names
     * @param metaValues the meta values
     */
    private void applyMetaData( ActionRequest actionRequest, String[] metaKeys, String[] metaValues ) {

        for (int i = 0; i < metaKeys.length; i++) {
            switch (metaKeys[i]) {
                case "transferUnit":
                    actionRequest.setTransferUnit(metaValues[i] + "/sec");
                    break;
                default:
                    log.warn("Action '" + actionRequest.getActionName() + "' from component '"
                             + actionRequest.getComponentName() + "' is called with unknown meta key '"
                             + metaKeys[i] + "' and value '" + metaValues[i]
                             + "'. This meta key will not be applied on this action.");
            }
        }
    }
}
