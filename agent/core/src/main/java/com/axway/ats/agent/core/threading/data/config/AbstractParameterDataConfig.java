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
package com.axway.ats.agent.core.threading.data.config;

import java.util.List;

import com.axway.ats.agent.core.exceptions.AgentException;

/**
 * Abstract class to be extended by all data provider configurations
 */
@SuppressWarnings("serial")
public abstract class AbstractParameterDataConfig implements ParameterDataConfig {

    protected String                 parameterName;
    protected ParameterProviderLevel parameterProviderLevel;

    public AbstractParameterDataConfig( String parameterName,
                                        ParameterProviderLevel parameterProviderLevel ) {

        this.parameterName = parameterName;
        this.parameterProviderLevel = parameterProviderLevel;
    }

    @Override
    public String getParameterName() {

        return parameterName;
    }

    @Override
    public ParameterProviderLevel getParameterProviderLevel() {

        return parameterProviderLevel;
    }

    public void setParameterProviderLevel( ParameterProviderLevel parameterProviderLevel ) {

        this.parameterProviderLevel = parameterProviderLevel;
    }

    /**
     * Distributes the parameter data for a number of agents
     *
     * This method is not in the ParameterDataConfig interface as we want to not expose it to the users
     * as a public method
     *
     * @param agents the number of agents to distribute for
     * @return
     * @throws IllegalArgumentException if parameters could not be distributed across
     *          all agents like when parameter values are less then the number of agents
     */
    abstract List<ParameterDataConfig> distribute( int agents ) throws IllegalArgumentException;

    /**
     * Verify data config
     * @throws AgentException
     */
    public void verifyDataConfig() throws AgentException {

        // for some of the data configurators, data verification is not needed
    }
}
