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
package com.axway.ats.agent.core.configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used for applying or reverting a set of configurators
 */
public class ConfigurationManager {

    private static ConfigurationManager instance;

    private List<Configurator>          currentConfigurators;

    private ConfigurationManager() {

        currentConfigurators = new ArrayList<Configurator>();
    }

    public static synchronized ConfigurationManager getInstance() {

        if (instance == null) {
            instance = new ConfigurationManager();
        }

        return instance;
    }

    /**
     * Apply the given configurators
     * 
     * @param configurators list of configurators to apply
     * @throws ConfigurationException on error
     */
    public void apply(
                       List<Configurator> configurators ) throws ConfigurationException {

        //first revert all previous configurations
        for (Configurator configurator : configurators) {
            if (configurator.needsApplying()) {
                //first revert any previous configuration
                configurator.revert();

                //and then apply the new one
                configurator.apply();
            }
        }

        currentConfigurators = configurators;
    }

    /**
     * Revert all configurations made
     * 
     * @throws ConfigurationException on error
     */
    public void revert() throws ConfigurationException {

        for (Configurator configurator : currentConfigurators) {
            configurator.revert();
        }
    }
}
