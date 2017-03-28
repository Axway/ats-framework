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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.axway.ats.agent.core.configuration.ConfigurationException;
import com.axway.ats.junit.BaseTestWebapps;

public class Test_DefaultLocalConfigurator extends BaseTestWebapps {

    @Test
    public void needsApplying() throws ConfigurationException {

        DefaultLocalConfigurator localConfigurator = new DefaultLocalConfigurator();
        assertTrue( localConfigurator.needsApplying() );
    }

    @Test
    public void revert() throws ConfigurationException {

        DefaultLocalConfigurator localConfigurator = new DefaultLocalConfigurator();
        localConfigurator.revert();
    }

    @Test
    public void apply() throws ConfigurationException {

        DefaultLocalConfigurator localConfigurator = new DefaultLocalConfigurator();
        localConfigurator.apply();
    }
}
