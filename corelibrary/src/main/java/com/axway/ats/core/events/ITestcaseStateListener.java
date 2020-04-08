/*
 * Copyright 2017-2020 Axway Software
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
package com.axway.ats.core.events;

import java.util.List;

public interface ITestcaseStateListener {

    /**
     * When a test is starting
     */
    public abstract void onTestStart();

    /**
     * When a test is ending
     */
    public abstract void onTestEnd();

    /**
     * Prior to interacting with ATS Agent, we must make sure it is properly configured
     *
     * @param atsAgents list of agents to configure
     * @throws Exception Only AgentException can be thrown here, but as the Core library cannot resolve this
     * exception, we have to use the parent of AgentException which is the regular java Exception
     */
    public abstract void onConfigureAtsAgents(
                                               List<String> atsAgents ) throws Exception;

    /**
     * Explicitly mark ATS agents as not configured. If agent was not previously configured, no error will be thrown<br>
     * Note that this method does not perform any operation to the actual agent, neither checks if the agent is still running in the provided host:port
     * @param atsAgents - list of ATS agents in the format <HOST>:<PORT> (if port is omitted, the default 8089 will be used instead) to be marked as not configured
     * */
    public abstract void invalidateConfiguredAtsAgents( List<String> atsAgents );

    /**
     * Called to release some resources on the agent side.
     * This is called when the client side counterparts are garbage collected.
     *
     * @param atsAgent the remote agent
     * @param internalProcessId ID of the object instance on the agent side
     */
    public abstract void cleanupInternalObjectResources(
                                                         String atsAgent,
                                                         String internalObjectResourceId );

}
