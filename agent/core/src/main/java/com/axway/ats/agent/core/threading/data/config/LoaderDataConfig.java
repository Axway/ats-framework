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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class encapsulates all configuration for the parameter data generation
 * per loader. This class will be serialized and transferred between the Agent
 * client and the core Agent subsystem
 */
@SuppressWarnings("serial")
public final class LoaderDataConfig implements Serializable {

    private List<ParameterDataConfig> parameterProviders;

    /**
     * Constructor
     */
    public LoaderDataConfig() {

        parameterProviders = new ArrayList<ParameterDataConfig>();
    }

    /**
     * Get a list of all parameter configurations for data
     * generation
     * 
     * @return list of the parameter configurations
     */
    public List<ParameterDataConfig> getParameterConfigurations() {

        return parameterProviders;
    }

    /**
     * Add a parameter configuration for data generation
     * 
     * @param parameterData the config class
     */
    public void addParameterConfig( ParameterDataConfig parameterData ) {

        parameterProviders.add( parameterData );
    }

    /**
     * Distributes all the parameter data to all agents
     * 
     * @param agents
     * @return
     */
    public List<LoaderDataConfig> distribute( int agents ) {

        // one instance of the configurator per loader
        List<LoaderDataConfig> distributedLoaderDataConfigs = new ArrayList<LoaderDataConfig>();
        for( int iConfigurator = 0; iConfigurator < agents; iConfigurator++ ) {
            distributedLoaderDataConfigs.add( new LoaderDataConfig() );
        }

        for( int iProvider = 0; iProvider < parameterProviders.size(); iProvider++ ) {
            // split the provider - one instance of the configurator per loader
            AbstractParameterDataConfig parameterProvider = ( AbstractParameterDataConfig ) parameterProviders.get( iProvider );
            List<ParameterDataConfig> distributedParameterProviders = parameterProvider.distribute( agents );

            // we know here the number of configurators is the same as the number of providers
            // assign one provider to each configurator
            for( int iConfigurator = 0; iConfigurator < agents; iConfigurator++ ) {
                distributedLoaderDataConfigs.get( iConfigurator )
                                            .addParameterConfig( distributedParameterProviders.get( iConfigurator ) );
            }
        }

        return distributedLoaderDataConfigs;
    }
}
