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
package com.axway.ats.agent.core;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.agent.core.configuration.ConfigurationSettings;
import com.axway.ats.agent.core.configuration.Configurator;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.loading.ComponentLoaderType;

public class Test_MainComponentLoader extends BaseTest {

    @BeforeClass
    public static void initTest_MainModuleLoader() {

        ConfigurationSettings.getInstance().setComponentLoaderType( ComponentLoaderType.CLASSPATH );
    }

    @Before
    public void setUp() {

        ComponentRepository.getInstance().clear();
    }

    @Test
    public void intializePositive() throws AgentException {

        MainComponentLoader.getInstance().initialize( new ArrayList<Configurator>() );
        ComponentActionMap actionMap = ComponentRepository.getInstance()
                                                          .getComponentActionMap( TEST_COMPONENT_NAME );

        assertNotNull( actionMap );
    }

    @Test
    public void destroyPositive() throws AgentException {

        MainComponentLoader.getInstance().initialize( new ArrayList<Configurator>() );
        MainComponentLoader.getInstance().destroy();
    }

    @Test
    public void blockIfLoading() throws AgentException {

        MainComponentLoader.getInstance().initialize( new ArrayList<Configurator>() );
        MainComponentLoader.getInstance().blockIfLoading();
    }
}
