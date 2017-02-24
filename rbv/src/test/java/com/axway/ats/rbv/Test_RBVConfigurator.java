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
package com.axway.ats.rbv;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.config.exceptions.ConfigurationException;
import com.axway.ats.rbv.RbvConfigurator;

public class Test_RBVConfigurator extends BaseTest {

    private static RbvConfigurator rbvConfigurator;

    @BeforeClass
    public static void setUpTest_RBVConfigurator() throws ConfigurationException {

        rbvConfigurator = RbvConfigurator.getInstance();
    }

    @Before
    public void setUp() throws ConfigurationException {

        rbvConfigurator.clearTempProperties();
    }

    @Test
    public void verifyInitialValues() throws ConfigurationException {

        assertEquals( 0L, rbvConfigurator.getPollingInitialDelay() );
        assertEquals( 10, rbvConfigurator.getPollingAttempts() );
        assertEquals( 1000L, rbvConfigurator.getPollingInterval() );
        assertEquals( 30000L, rbvConfigurator.getPollingTimeout() );
    }

    @Test
    public void verifyValuesCanBeSetAtRuntimeAndThenCleared() throws ConfigurationException {

        rbvConfigurator.setPollingInitialDelay( 350 );
        rbvConfigurator.setPollingAttempts( 15 );
        rbvConfigurator.setPollingInterval( 35600 );
        rbvConfigurator.setPollingTimeout( 555000 );

        assertEquals( 350L, rbvConfigurator.getPollingInitialDelay() );
        assertEquals( 15, rbvConfigurator.getPollingAttempts() );
        assertEquals( 35600L, rbvConfigurator.getPollingInterval() );
        assertEquals( 555000L, rbvConfigurator.getPollingTimeout() );

        //now revert back to initial values
        rbvConfigurator.clearTempProperties();

        assertEquals( 0L, rbvConfigurator.getPollingInitialDelay() );
        assertEquals( 10, rbvConfigurator.getPollingAttempts() );
        assertEquals( 1000L, rbvConfigurator.getPollingInterval() );
        assertEquals( 30000L, rbvConfigurator.getPollingTimeout() );
    }
}
