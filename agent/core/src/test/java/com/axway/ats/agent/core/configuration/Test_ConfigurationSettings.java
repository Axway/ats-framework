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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class Test_ConfigurationSettings {

    @Test
    public void setGetMonitorPollIntervalPositive() {

        ConfigurationSettings configSettings = ConfigurationSettings.getInstance();

        configSettings.setMonitorPollInterval( 4 );
        assertEquals( 4, configSettings.getMonitorPollInterval() );
    }

    @Test
    public void setGetComponentsFolder() {

        ConfigurationSettings configSettings = ConfigurationSettings.getInstance();

        configSettings.setComponentsFolder( "test/folder" );
        assertEquals( "test/folder", configSettings.getComponentsFolder() );
    }
}
