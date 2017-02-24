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
package com.axway.ats.agent.core.action;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.agent.core.ActionHandler;
import com.axway.ats.agent.core.BaseTest;
import com.axway.ats.agent.core.Component;
import com.axway.ats.agent.core.ComponentActionMap;
import com.axway.ats.agent.core.ComponentRepository;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.core.validation.exceptions.InvalidInputArgumentsException;

public class Test_ActionMethodValidation extends BaseTest {

    private static final String TEST_CALLER_IP      = "caller ip";

    @BeforeClass
    public static void setUpTest_ActionHandler() throws AgentException {

        Component component = new Component( TEST_COMPONENT_NAME );
        ComponentActionMap actionMap = new ComponentActionMap( TEST_COMPONENT_NAME );
        actionMap.registerActionClass( ActionClassForValidation.class );
        component.setActionMap( actionMap );

        ComponentRepository componentRepository = ComponentRepository.getInstance();
        componentRepository.clear();
        componentRepository.putComponent( component );
    }

    @Test
    public void validateByteArrayPositive() throws AgentException {

        byte[] returnValue = ( byte[] ) ActionHandler
                                                     .executeAction( TEST_CALLER_IP,
                                                                     TEST_COMPONENT_NAME,
                                                                     "action 1",
                                                                     new Object[]{ new byte[]{ 1, 1, 3 },
                                                                             "test" } );

        assertTrue( Arrays.equals( new byte[]{ 1, 1, 3 }, returnValue ) );
    }

    @Test(expected = InvalidInputArgumentsException.class)
    public void validateByteArrayNegative() throws AgentException {

        ActionHandler.executeAction( TEST_CALLER_IP,
                                                   TEST_COMPONENT_NAME,
                                                   "action 1",
                                                   new Object[]{ new byte[]{ 15, 1, 3 }, "test" } );
    }

    @Test(expected = InvalidInputArgumentsException.class)
    public void validateStringNegative() throws AgentException {

        ActionHandler.executeAction( TEST_CALLER_IP,
                                                   TEST_COMPONENT_NAME,
                                                   "action 1",
                                                   new Object[]{ new byte[]{ 1, 1, 3 }, null } );
    }
}
