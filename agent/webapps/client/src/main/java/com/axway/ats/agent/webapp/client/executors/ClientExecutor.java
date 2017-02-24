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
package com.axway.ats.agent.webapp.client.executors;

import java.util.List;

import com.axway.ats.agent.core.action.ActionRequest;
import com.axway.ats.agent.core.exceptions.AgentException;

/**
 * Client execution of actions
 */
public interface ClientExecutor {

    /**
     * Execute a single action and return the result
     *
     * @param actionRequest the request bean
     * @return result of the execution
     * @throws AgentException
     */
    Object executeAction(
                          ActionRequest actionRequest ) throws AgentException;

    /**
     * Execute a set of actions
     *
     * @param actionRequests list of request beans
     * @throws AgentException
     */
    void executeActions(
                         List<ActionRequest> actionRequests ) throws AgentException;

    /**
     * Wait until the execution of all actions in multiple threads finishes
     *
     * @throws Agentxception
     */
    void waitUntilQueueFinish() throws AgentException;

    /**
     * Clean the environment for the given component with a specific environment configuration
     *
     * @param componentName name of the component to clean
     * @param environmentName name of the environment configuration
     * @param folderPath the path to the restore folder
     * @throws AgentException
     */
    void restore(
                  String componentName,
                  String environmentName,
                  String folderPath ) throws AgentException;

    /**
     * Clean the environment for all registered components with a specific environment configuration
     *
     * @param environmentName name of the environment configuration
     * @throws AgentException
     */
    void restoreAll(
                     String environmentName ) throws AgentException;

    /**
     * Backup the environment for the given components with a specific environment configuration
     *
     * @param componentName name of the component to create backup for
     * @param environmentName name of the environment configuration
     * @param folderPath the path to the backup folder
     * @throws AgentException
     */
    void backup(
                 String componentName,
                 String environmentName,
                 String folderPath ) throws AgentException;

    /**
     * Backup the environment for all registered components with a specific environment configuration
     *
     * @param environmentName name of the environment configuration
     * @throws AgentException
     */
    void backupAll(
                    String environmentName ) throws AgentException;

}
