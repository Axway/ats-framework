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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.axway.ats.agent.core.exceptions.ActionExecutionException;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.threading.data.config.LoaderDataConfig;
import com.axway.ats.agent.core.threading.data.config.ParameterDataConfig;
import com.axway.ats.agent.core.threading.data.config.UsernameDataConfig;
import com.axway.ats.agent.core.threading.patterns.ThreadingPattern;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.utils.HostUtils;

/**
 * A client which encapsulates the most used performance
 * loading steps used in a test case.<br />
 */
@PublicAtsApi
public class LoadClient {

    /**
     * List of server:port listen addresses for ATS agents(loader)
     */
    protected Set<String>    loaderAddresses;

    protected String         queueName;
    private ThreadingPattern threadingPattern;
    private ActionQueue      actionQueue;
    private LoaderDataConfig loaderDataConfig;

    /**
     * Generic constructor.
     * ATS agents(loaders) must be specified later before executing the test
     * steps. Use {@link #addLoaderHost(String)} for this.
     */
    @PublicAtsApi
    public LoadClient() {

        this( ( String[] ) null );
    }

    /**
     * Constructor providing one ATS agent (used as loader) to run the test steps from
     * @param atsAgent the ATS agent to use
     * loaders
     */
    @PublicAtsApi
    public LoadClient( String atsAgent ) {

        this( ( atsAgent != null )
                                   ? new String[]{ atsAgent }
                                   : null );
    }

    /**
     * Constructor providing the ATS agents (used as loaders) to run the test steps from
     * @param atsAgents array of used ATS agents
     * loaders
     */
    @PublicAtsApi
    public LoadClient( String[] atsAgents ) {

        if( atsAgents == null ) {
            atsAgents = new String[0];
        }

        // add default port in case none is not provided by the user
        atsAgents = HostUtils.getAtsAgentsIpAndPort( atsAgents );
        this.loaderAddresses = new HashSet<String>( Arrays.asList( atsAgents ) );

        this.loaderDataConfig = new LoaderDataConfig();
    }

    /**
     * Called right before sending the queued actions
     * @throws AgentException
     */
    protected void configureAgentLoaders() throws AgentException {

        if( loaderAddresses.size() == 0 ) {
            throw new ActionExecutionException( "No Agent loaders are defined to run actions on" );
        }

        if( actionQueue == null ) {
            throw new ActionExecutionException( "The loader action queue is empty. You first need to start the queuing process and then execute the queued actions" );
        }
    }

    /**
     * Add Agent loader to the list of hosts where the test steps will be executed
     * @param agentLoader address of the remote ATS agent
     */
    @PublicAtsApi
    public void addLoaderHost( String agentLoader ) {

        // add default port in case none is not provided by the user
        agentLoader = HostUtils.getAtsAgentIpAndPort( agentLoader );

        loaderAddresses.add( agentLoader );
    }

    /**
     * Set the threading pattern
     * @param threadingPattern
     */
    @PublicAtsApi
    public void setThreadingPattern( ThreadingPattern threadingPattern ) {

        this.threadingPattern = threadingPattern;
    }

    /**
     * Add parameter configurator
     *
     * @param parameterDataConfig
     * @throws AgentException
     */
    @PublicAtsApi
    public void addParameterDataConfigurator( ParameterDataConfig parameterDataConfig ) throws AgentException {

        parameterDataConfig.verifyDataConfig();
        this.loaderDataConfig.addParameterConfig( parameterDataConfig );
    }

    /**
     * All actions after this point will be queued for later execution
     * @param queueName
     */
    @PublicAtsApi
    public void startQueueing( String queueName ) {

        this.queueName = queueName;
        actionQueue = ActionQueue.getNewInstance( this.loaderAddresses, queueName );
        actionQueue.startQueueing();
    }

    /**
     * Execute all queued actions
     *
     * @throws AgentException
     */
    @PublicAtsApi
    public void executeQueuedActions() throws AgentException {

        configureAgentLoaders();

        boolean isThereUsernameConfigurator = false;
        boolean isThereUsernameParam = false;

        for( ParameterDataConfig dataConfig : loaderDataConfig.getParameterConfigurations() ) {

            // check if UsernameDataConfigurator is used more than once
            if( isThereUsernameConfigurator ) {
                throw new ActionExecutionException( "You have used Username Data Configurator more than once. This is not allowed." );
            }

            if( dataConfig instanceof UsernameDataConfig ) {
                isThereUsernameConfigurator = true;

                // user names are specified
                ( ( UsernameDataConfig ) dataConfig ).verifyUsernamesAreWEnough( threadingPattern.getThreadCount() );
            } else if( "username".equals( dataConfig.getParameterName() ) ) {
                isThereUsernameParam = true;
            }
        }

        if( isThereUsernameConfigurator && isThereUsernameParam ) {
            throw new ActionExecutionException( "The parameter \"username\" can not be used for another data configurator "
                                                + "when Username data configurator is used." );
        }

        actionQueue.executeQueuedActions( new ArrayList<String>( this.loaderAddresses ), threadingPattern,
                                          loaderDataConfig );
    }
}
