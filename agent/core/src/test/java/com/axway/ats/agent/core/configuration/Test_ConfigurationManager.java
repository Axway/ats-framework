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

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class Test_ConfigurationManager {

    private static int simpleSetting = 0;

    @Before
    public void setUp() {

        simpleSetting = 0;
    }

    @Test
    public void applyPositive() throws ConfigurationException {

        MockConfigurator mockConfigurator = new MockConfigurator();
        List<Configurator> configurators = new ArrayList<Configurator>();
        configurators.add( mockConfigurator );

        ConfigurationManager.getInstance().apply( configurators );

        assertEquals( 21, simpleSetting );
    }

    @Test
    public void revertPositive() throws ConfigurationException {

        MockConfigurator mockConfigurator = new MockConfigurator();
        List<Configurator> configurators = new ArrayList<Configurator>();
        configurators.add( mockConfigurator );

        ConfigurationManager.getInstance().apply( configurators );
        ConfigurationManager.getInstance().revert();

        assertEquals( 10, simpleSetting );
    }

    @Test(expected = ConfigurationException.class)
    public void applyNegative() throws ConfigurationException {

        MockConfiguratorThrowingExceptions mockConfigurator = new MockConfiguratorThrowingExceptions();
        List<Configurator> configurators = new ArrayList<Configurator>();
        configurators.add( mockConfigurator );

        ConfigurationManager.getInstance().apply( configurators );
    }

    @Test(expected = ConfigurationException.class)
    public void revertNegative() throws ConfigurationException {

        MockConfiguratorThrowingExceptions mockConfigurator = new MockConfiguratorThrowingExceptions();
        List<Configurator> configurators = new ArrayList<Configurator>();
        configurators.add( mockConfigurator );

        ConfigurationManager.getInstance().apply( configurators );
        ConfigurationManager.getInstance().revert();
    }

    @SuppressWarnings("serial")
    private static class MockConfigurator implements Configurator {

        public void apply() throws ConfigurationException {

            simpleSetting = 21;
        }

        public boolean needsApplying() throws ConfigurationException {

            return true;
        }

        public void revert() throws ConfigurationException {

            simpleSetting = 10;

        }

        @Override
        public String getDescription() {

            return "mock configurator";
        }

    }

    @SuppressWarnings("serial")
    private static class MockConfiguratorThrowingExceptions implements Configurator {

        public void apply() throws ConfigurationException {

            throw new ConfigurationException( "dasd" );
        }

        public boolean needsApplying() throws ConfigurationException {

            throw new ConfigurationException( "dasd" );
        }

        public void revert() throws ConfigurationException {

            throw new ConfigurationException( "dasd" );
        }

        @Override
        public String getDescription() {

            return "mock configurator throwing exception";
        }
    }
}
