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

import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.agent.core.exceptions.AgentException;

public class Test_ActionHandlerOverloading extends BaseTest {

    private static final String TEST_CALLER_IP      = "caller ip";

    @BeforeClass
    public static void setUpTest_ActionHandler() throws AgentException {

        Component component = new Component( TEST_COMPONENT_NAME );
        ComponentActionMap actionMap = new ComponentActionMap( TEST_COMPONENT_NAME );
        actionMap.registerActionClass( ActionClassOverloadedMethods.class );
        component.setActionMap( actionMap );

        ComponentRepository componentRepository = ComponentRepository.getInstance();
        componentRepository.clear();
        componentRepository.putComponent( component );
    }

    @Test
    public void findOverloadedMethodStringArgPositive() throws AgentException {

        Integer returnValue = ( Integer ) ActionHandler.executeAction( TEST_CALLER_IP,
                                                                                     TEST_COMPONENT_NAME,
                                                                                     "action 1",
                                                                                     new Object[]{ "Test" } );

        assertEquals( 1, ( int ) returnValue );
    }

    @Test
    public void findOverloadedMethodByteArgPositive() throws AgentException {

        Integer returnValue = ( Integer ) ActionHandler
                                                       .executeAction( TEST_CALLER_IP,
                                                                       TEST_COMPONENT_NAME,
                                                                       "action 1",
                                                                       new Object[]{ new Byte( "3" ) } );

        assertEquals( 2, ( int ) returnValue );
    }

    @Test
    public void findOverloadedMethodPrimitiveByteArgPositive() throws AgentException {

        byte argByte = 3;
        Integer returnValue = ( Integer ) ActionHandler.executeAction( TEST_CALLER_IP,
                                                                                     TEST_COMPONENT_NAME,
                                                                                     "action 1",
                                                                                     new Object[]{ argByte } );

        assertEquals( 2, ( int ) returnValue );
    }

    @Test
    public void findOverloadedMethodPrimitiveIntParameterPositive() throws AgentException {

        Integer returnValue = ( Integer ) ActionHandler.executeAction( TEST_CALLER_IP,
                                                                                     TEST_COMPONENT_NAME,
                                                                                     "action 1",
                                                                                     new Object[]{ 3 } );

        assertEquals( 3, ( int ) returnValue );
    }
}
