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
package com.axway.ats.agent.webapp.client.configuration;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.axway.ats.agent.core.configuration.Configurator;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.webapp.client.RestHelper;
import com.axway.ats.core.AtsVersion;
import com.axway.ats.core.utils.ExecutorUtils;
import com.axway.ats.core.utils.HostUtils;

/**
 * This class is used to send configuration settings to ATS Agent
 */
public class RemoteConfigurationManager {

    private static Logger log = Logger.getLogger(RemoteConfigurationManager.class);

    /**
     * Push the provided configuration to the remote Agent
     *
     * @param atsAgent host to push the configuration to
     * @param configurator the configurator
     * @throws AgentException
     */
    public void pushConfiguration(
                                   String atsAgent,
                                   Configurator configurator ) throws AgentException {

<<<<<<< 28962be579ed52a328534ed957a99e04fb42b367
        AgentConfigurationLandscape.getInstance( atsAgent ).cacheConfigurator( configurator );
        
        // get the client instance
        AgentService agentServicePort = AgentServicePool.getInstance().getClientForHost(atsAgent);
||||||| merged common ancestors
        // get the client instance
        AgentService agentServicePort = AgentServicePool.getInstance().getClientForHost(atsAgent);
=======
        Map<String, Configurator> configurators = new HashMap<>();
        configurators.put(configurator.getClass().getName(), configurator);
>>>>>>> [Restify Agent] All methods have been implemented and documented

        RestHelper restHelper = new RestHelper();

        // create sessionId
        String sessionId = ExecutorUtils.createExecutorId(atsAgent, ExecutorUtils.getUserRandomToken(),
                                                          Thread.currentThread().getName());

        // create request body
        StringBuilder requestBody = new StringBuilder();
        requestBody.append("{")
                   .append("\"sessionId\":\"")
                   .append(sessionId)
                   .append("\"")
                   .append(",")
                   .append("\"")
                   .append("configurators")
                   .append("\"")
                   .append(":")
                   .append(restHelper.serializeJavaObject(configurators))
                   .append("}");

        // put the configurator to the agent and get the agent's ATS version
        String agentVersion = (String) restHelper.executeRequest(atsAgent, "agent/configurations", "PUT",
                                                                 requestBody.toString(),
                                                                 "ats_version", String.class);
        // get the ats version of the test executor
        String atsVersion = AtsVersion.getAtsVersion();

        if (!atsVersion.equals(agentVersion)) {
            log.warn("*** ATS WARNING *** You are using ATS version " + atsVersion
                     + " with ATS agent version " + agentVersion + " located at '"
                     + HostUtils.getAtsAgentIpAndPort(atsAgent)
                     + "'. This might cause incompatibility problems!");
        }

        log.info("Successfully set the " + configurator.getDescription() + " on ATS Agent at '"
                 + atsAgent + "'");
    }
}
