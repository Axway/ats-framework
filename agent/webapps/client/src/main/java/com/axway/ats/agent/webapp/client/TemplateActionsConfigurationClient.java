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

import java.util.Properties;

import com.axway.ats.agent.core.configuration.Configurator;
import com.axway.ats.agent.core.configuration.TemplateActionsConfigurator;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.webapp.client.configuration.RemoteConfigurationManager;
import com.axway.ats.common.PublicAtsApi;

/**
 * Used for changing some settings in the way template actions
 * are processed
 */
@PublicAtsApi
public class TemplateActionsConfigurationClient extends AbstractAgentClient {

    /**
     * @param atsAgent
     *            the ATS Agent to work with - if you
     *            pass LOCAL_JVM, the action execution will be performed in the
     *            current JVM without routing through the web service
     */
    @PublicAtsApi
    public TemplateActionsConfigurationClient( String atsAgent ) {

        super( atsAgent, "some fake component" );
    }

    @PublicAtsApi
    public void setMatchingFilesBySize( boolean matchFilesBySize ) throws AgentException {

        Properties taProperties = new Properties();
        taProperties.put( TemplateActionsConfigurator.MATCH_FILES_BY_SIZE,
                          String.valueOf( matchFilesBySize ) );

        TemplateActionsConfigurator templateActionsConfigurator = new TemplateActionsConfigurator( taProperties );
        pushConfiguration( templateActionsConfigurator );
    }

    @PublicAtsApi
    public void setMatchingFilesByContent( boolean matchFilesByContent ) throws AgentException {

        Properties taProperties = new Properties();
        taProperties.put( TemplateActionsConfigurator.MATCH_FILES_BY_CONTENT,
                          String.valueOf( matchFilesByContent ) );

        TemplateActionsConfigurator templateActionsConfigurator = new TemplateActionsConfigurator( taProperties );
        pushConfiguration( templateActionsConfigurator );
    }

    @PublicAtsApi
    public void setTemplateActionsFolder( String templateActionsFolder ) throws AgentException {

        Properties taProperties = new Properties();
        taProperties.put( TemplateActionsConfigurator.AGENT__TEMPLATE_ACTIONS_FOLDER_PROPERTY,
                          templateActionsFolder );

        TemplateActionsConfigurator templateActionsConfigurator = new TemplateActionsConfigurator( taProperties );
        pushConfiguration( templateActionsConfigurator );
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
