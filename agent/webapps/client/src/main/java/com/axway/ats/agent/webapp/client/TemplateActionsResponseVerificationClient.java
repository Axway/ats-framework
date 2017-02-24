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

import com.axway.ats.agent.core.configuration.Configurator;
import com.axway.ats.agent.core.configuration.TemplateActionsResponseVerificationConfigurator;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.webapp.client.configuration.RemoteConfigurationManager;

public class TemplateActionsResponseVerificationClient extends AbstractAgentClient {

    private TemplateActionsResponseVerificationConfigurator verificationConfigurator;

    /**
     * @param atsAgent
     *            the ATS Agent to work with - if you
     *            pass LOCAL_JVM, the action execution will be performed in the
     *            current JVM without routing through the web service
     */
    public TemplateActionsResponseVerificationClient( String atsAgent,
                                                      TemplateActionsResponseVerificationConfigurator verificationConfigurator ) {

        super( atsAgent, "some fake component" );
        this.verificationConfigurator = verificationConfigurator;
    }

    public void send() throws AgentException {

        pushConfiguration( verificationConfigurator );
    }

    private void pushConfiguration( Configurator configurator ) throws AgentException {

        if( atsAgent.equals( LOCAL_JVM ) ) {

            configurator.apply();
        } else {
            // send the Agent configuration
            new RemoteConfigurationManager().pushConfiguration( atsAgent, configurator );
        }
    }
}
