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

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.exceptions.InternalComponentException;
import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;

public class Test_ActionHandler extends BaseTest {

    private static final String TEST_CALLER_IP      = "caller ip";

    @BeforeClass
    public static void setUpTest_ActionHandler() throws AgentException {

        Component component = new Component( TEST_COMPONENT_NAME );
        ComponentActionMap actionMap = new ComponentActionMap( TEST_COMPONENT_NAME );
        actionMap.registerActionClass( ActionClassOne.class );
        actionMap.registerActionClass( ActionClassTwo.class );
        component.setActionMap( actionMap );

        ComponentRepository componentRepository = ComponentRepository.getInstance();
        componentRepository.clear();
        componentRepository.putComponent( component );
    }

    @Before
    public void setUp() {

        //init the test values
        ActionClassOne.ACTION_VALUE = 0;
        ActionClassTwo.ACTION_VALUE = 0;
    }

    @Test
    public void executeActionPositive() throws Exception {

        ActionHandler.executeAction( TEST_CALLER_IP,
                                                   TEST_COMPONENT_NAME,
                                                   "action 1",
                                                   new Object[]{ 1 } );

        assertEquals( 1, ActionClassOne.ACTION_VALUE );
    }

    @Test(expected = NoSuchActionException.class)
    public void executeActionNegativeNoSuchAction() throws Exception {

        ActionHandler.executeAction( TEST_CALLER_IP,
                                                   TEST_COMPONENT_NAME,
                                                   "action 1234",
                                                   new Object[]{ 1 } );
    }

    @Test(expected = NoSuchComponentException.class)
    public void executeActionNegativeNoSuchComponent() throws Exception {

        ActionHandler.executeAction( TEST_CALLER_IP,
                                                   "agenttest123",
                                                   "action 1",
                                                   new Object[]{ 1 } );
    }

    @Test(expected = NoCompatibleMethodFoundException.class)
    public void executeActionNegativeWrongArgNum() throws Exception {

        ActionHandler.executeAction( TEST_CALLER_IP,
                                                   TEST_COMPONENT_NAME,
                                                   "action 1",
                                                   new Object[]{ 1, 4 } );

        assertEquals( 1, ActionClassOne.ACTION_VALUE );
    }

    @Test(expected = InternalComponentException.class)
    public void executeActionCheckedException() throws Exception {

        ActionHandler.executeAction( TEST_CALLER_IP,
                                                   TEST_COMPONENT_NAME,
                                                   "action checked exception",
                                                   new Object[]{} );
    }

    @Test(expected = InternalComponentException.class)
    public void executeActionUncheckedException() throws Exception {

        ActionHandler.executeAction( TEST_CALLER_IP,
                                                   TEST_COMPONENT_NAME,
                                                   "action unchecked exception",
                                                   new Object[]{} );
    }

    @Test(expected = NoCompatibleMethodFoundException.class)
    public void executeActionIllegalArguments() throws Exception {

        ActionHandler.executeAction( TEST_CALLER_IP,
                                                   TEST_COMPONENT_NAME,
                                                   "action double",
                                                   new Object[]{ "3" } );
    }
}
