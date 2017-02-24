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

import com.axway.ats.agent.core.exceptions.AgentException;

/**
 * This interface must be implemented by all configuration classes
 * for parameter data providers
 */
public interface ParameterDataConfig extends Serializable {

    /**
     * Get the name of the parameters this config is for
     *
     * @return the name of the action parameter
     */
    public String getParameterName();

    /**
     * Get the level of the data provider
     *
     * @return the level of the data provider
     */
    public ParameterProviderLevel getParameterProviderLevel();

    /**
     * Verify data config
     * @throws AgentException
     */
    public abstract void verifyDataConfig() throws AgentException;
}
