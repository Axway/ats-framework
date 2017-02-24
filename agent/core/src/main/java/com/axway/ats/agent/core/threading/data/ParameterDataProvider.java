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
package com.axway.ats.agent.core.threading.data;

import java.util.List;

import com.axway.ats.agent.core.action.ArgumentValue;
import com.axway.ats.agent.core.threading.exceptions.ParameterDataProviderInitalizationException;

/**
 * A parameter data provider. Implementations of this interface are used
 * for providing data to multiple threads when executing action with specific
 * parameter names
 */
public interface ParameterDataProvider {

    /**
     * Initialize the parameter data provider
     */
    public void initialize() throws ParameterDataProviderInitalizationException;

    /**
     * Get a new value for this parameter data provider
     *
     * @return the new value which should be passed as argument
     */
    public ArgumentValue getValue(
                                   List<ArgumentValue> alreadyResolvedValues );

    /**
     * Get the parameter name
     * @return the parameter name
     */
    public String getParameterName();

    /**
     * Get the data provider instance
     * @return the data provider instance
     */
    public Class<?> getDataConfiguratorClass();

}
