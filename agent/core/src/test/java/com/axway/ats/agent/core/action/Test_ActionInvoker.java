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

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.agent.core.BaseTest;
import com.axway.ats.agent.core.Component;
import com.axway.ats.agent.core.ComponentActionMap;
import com.axway.ats.agent.core.ComponentRepository;
import com.axway.ats.agent.core.exceptions.AgentException;

public class Test_ActionInvoker extends BaseTest {

    @BeforeClass
    public static void setUpTest_ActionInvoker() throws AgentException {

        Component component = new Component( TEST_COMPONENT_NAME );
        ComponentActionMap actionMap = new ComponentActionMap( TEST_COMPONENT_NAME );
        actionMap.registerActionClass( ReallyComplexActionClass.class );
        component.setActionMap( actionMap );

        ComponentRepository componentRepository = ComponentRepository.getInstance();
        componentRepository.clear();
        componentRepository.putComponent( component );
    }

    @Test
    public void getActionMethodPositive() throws Exception {

        Method expectedMethod = ReallyComplexActionClass.class.getMethod( "action1",
                                                                          new Class<?>[]{ Integer.TYPE,
                                                                                  Byte.class,
                                                                                  String.class } );

        ActionRequest actionRequest = new ActionRequest( TEST_COMPONENT_NAME, "action 1", new Object[]{ 0,
                ( byte ) 2,
                "test" } );

        ActionInvoker actionInvoker = new ActionInvoker( actionRequest );

        assertEquals( expectedMethod, actionInvoker.getActionMethod().getMethod() );
    }

    @Test
    public void getActionClassPositive() throws Exception {

        ActionRequest actionRequest = new ActionRequest( TEST_COMPONENT_NAME, "action 1", new Object[]{ 0,
                ( byte ) 2,
                "test" } );

        ActionInvoker actionInvoker = new ActionInvoker( actionRequest );

        assertEquals( ReallyComplexActionClass.class, actionInvoker.getActionClass() );
    }

    @Test
    public void getActionNamePositive() throws Exception {

        ActionRequest actionRequest = new ActionRequest( TEST_COMPONENT_NAME, "action 1", new Object[]{ 0,
                ( byte ) 2,
                "test" } );

        ActionInvoker actionInvoker = new ActionInvoker( actionRequest );

        assertEquals( "action 1", actionInvoker.getActionName() );
    }

    @Test
    public void invokePositive() throws Exception {

        ActionRequest actionRequest = new ActionRequest( TEST_COMPONENT_NAME, "action 1", new Object[]{ 0,
                ( byte ) 2,
                "test" } );

        ActionInvoker actionInvoker = new ActionInvoker( actionRequest );

        assertEquals( 1, actionInvoker.invoke( new ReallyComplexActionClass() ) );
    }

    @Test
    public void invokePositiveNullArgument() throws Exception {

        ActionRequest actionRequest = new ActionRequest( TEST_COMPONENT_NAME, "action 1", new Object[]{ 0,
                null,
                "test" } );

        ActionInvoker actionInvoker = new ActionInvoker( actionRequest );

        assertEquals( 1, actionInvoker.invoke( new ReallyComplexActionClass() ) );
    }
}
