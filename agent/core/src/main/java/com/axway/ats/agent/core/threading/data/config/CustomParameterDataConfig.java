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
/**
 * 
 */
package com.axway.ats.agent.core.threading.data.config;

import java.util.List;
import java.util.Map;

import com.axway.ats.agent.core.threading.data.CustomParameterDataProvider;

/**
 * Data configuration which is supposed to be extended by a particular custom implementation.
 * It can be used to implement a custom way of providing data for the executed actions.
 */
@SuppressWarnings( "serial")
public abstract class CustomParameterDataConfig extends AbstractParameterDataConfig {

    /**
     * Some data that has to be passed from the test in order to control the custom provider behavior. 
     * This is implementation specific.
     */
    protected Map<String, String> controlTokens;

    /**
     * Constructs a custom data configuration which is used to instantiate a custom data provider.
     * 
     * <br><b>NOTE:</b> The implementation classes are expected to be direct children of this class.
     * 
     * @param parameterName the name of the parameter to generate data for
     * @param controlTokens control tokens if any
     * <br><b>NOTE:</b> This must not be a null value as we will not be able to call the constructor with java reflection.
     * It must be a Map with or without elements.
     * @param parameterProviderLevel the level at which to generate values
     */
    public CustomParameterDataConfig( String parameterName,
                                      Map<String, String> controlTokens,
                                      ParameterProviderLevel parameterProviderLevel ) {
        super(parameterName, parameterProviderLevel);

        this.controlTokens = controlTokens;
    }

    /**
     * We make it public here, so custom classes(from external java packages) can override it
     */
    public abstract List<ParameterDataConfig> distribute(
                                                          int agents ) throws IllegalArgumentException;

    /**
     * @return the custom data provider class, we need this when so can create an instance of the proper custom data configurator
     */
    public abstract Class<? extends CustomParameterDataProvider> getDataProviderClass();

    public Map<String, String> getControlTokens() {

        return controlTokens;
    }
}
