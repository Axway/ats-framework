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
import com.axway.ats.agent.webapp.restservice.api.agent.AgentPropertiesRestEntryPoint;
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

        AgentConfigurationLandscape.getInstance(atsAgent).cacheConfigurator(configurator);

        Map<String, Configurator> configurators = new HashMap<>();
        configurators.put(configurator.getClass().getName(), configurator);

        RestHelper restHelper = new RestHelper();

        // create callerId
        String callerId = ExecutorUtils.createCallerId();

        // get the ATS Agent version
        String agentVersion = (String) restHelper.executeRequest(atsAgent, "agent/properties", "POST",
                                                                 "{\"callerId\":\"" + callerId + "\",\"operation\":\""
                                                                                                       + AgentPropertiesRestEntryPoint.GET_ATS_VERSION_OPERATION
                                                                                                       + "\"}",
                                                                 "ats_version",
                                                                 String.class);

        // get the ats version of the test executor
        String atsVersion = AtsVersion.getAtsVersion();

        // compare both versions and log warning if they do are not the same
        if (!atsVersion.equals(agentVersion)) {
            log.warn("*** ATS WARNING *** You are using ATS version " + atsVersion
                     + " with ATS agent version " + agentVersion + " located at '"
                     + HostUtils.getAtsAgentIpAndPort(atsAgent)
                     + "'. This might cause incompatibility problems!");
        }

        // create request body
        StringBuilder requestBody = new StringBuilder();
        requestBody.append("{")
                   .append("\"callerId\":\"")
                   .append(callerId)
                   .append("\"")
                   .append(",")
                   .append("\"")
                   .append("configurators")
                   .append("\"")
                   .append(":")
                   .append(restHelper.serializeJavaObject(configurators))
                   .append("}");

        log.info("Pushing DB log configuration to ATS Agent " + atsAgent + "");

        // configure the agent
        restHelper.executeRequest(atsAgent, "agent/configurations", "PUT",
                                  requestBody.toString(),
                                  null, null);

        log.info("Successfully set the " + configurator.getDescription() + " on ATS Agent at '"
                 + atsAgent + "'");
    }
}
