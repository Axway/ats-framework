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

import com.axway.ats.agent.core.configuration.ConfigurationException;
import com.axway.ats.agent.core.configuration.ConfigurationSettings;
import com.axway.ats.agent.core.configuration.Configurator;
import com.axway.ats.agent.core.loading.ComponentLoaderType;

/**
 * This is the default local configurator for ATS Agent.
 * It is passed to the bootstrap loader the first time ATS Agent is initialized
 */
@SuppressWarnings("serial")
public class DefaultLocalConfigurator implements Configurator {

    @Override
    public void apply() throws ConfigurationException {

        //default loader for local JVM is classpath
        ConfigurationSettings.getInstance().setComponentLoaderType( ComponentLoaderType.CLASSPATH );
    }

    @Override
    public void revert() throws ConfigurationException {

        //nothing to do here, as this is the default configuration
    }

    @Override
    public boolean needsApplying() throws ConfigurationException {

        //always apply
        return true;
    }

    @Override
    public String getDescription() {

        return "default local configuration";
    }
}
