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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.agent.core.configuration.Configurator;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.webapp.client.AgentException_Exception;
import com.axway.ats.agent.webapp.client.AgentService;
import com.axway.ats.agent.webapp.client.AgentServicePool;

/**
 * This class is used to send configuration settings to ATS Agent
 */
public class RemoteConfigurationManager {

    private static Logger log = LogManager.getLogger(RemoteConfigurationManager.class);

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

        // get the client instance
        AgentService agentServicePort = AgentServicePool.getInstance().getClient(atsAgent);

        List<Configurator> configurators = new ArrayList<Configurator>();
        configurators.add(configurator);

        String checkServerLogsStr = ". Check server logs for more details.";
        try {

            log.info("Pushing DB log configuration to ATS Agent " + atsAgent + "");
            
            // serialize the configurators
            ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutStream = new ObjectOutputStream(byteOutStream);
            objectOutStream.writeObject(configurators);

            // get Agent Version
            String agentVersion = agentServicePort.pushConfiguration(byteOutStream.toByteArray());

            log.info("Successfully set the " + configurator.getDescription() + " on ATS Agent at '"
                     + atsAgent + "'");
        } catch (IOException ioe) {
            // log hint for further serialization issue investigation
            String msg = "Could not serialize configurators" + checkServerLogsStr;
            log.error(msg, ioe);
            throw new AgentException(msg, ioe);
        } catch (AgentException_Exception ae) {
            String msg = ae.getMessage() + checkServerLogsStr;
            log.error(msg, ae);
            throw new AgentException(msg, ae.getCause());
        } catch (Exception e) {
            String msg = e.getMessage() + checkServerLogsStr;
            log.error(msg, e);
            throw new AgentException(msg, e);
        }
    }
}
