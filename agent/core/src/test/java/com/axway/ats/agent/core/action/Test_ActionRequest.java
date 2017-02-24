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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.axway.ats.agent.core.BaseTest;

public class Test_ActionRequest extends BaseTest {

    @Test
    public void getComponentNamePositive() {

        String componentName = "component name &^%*&^%";
        ActionRequest actionRequest = new ActionRequest( componentName, "action name", new Object[]{} );

        assertEquals( componentName, actionRequest.getComponentName() );
    }

    @Test
    public void getActionNamePositive() {

        String actionName = "action name &^%*&^%";
        ActionRequest actionRequest = new ActionRequest( "component", actionName, new Object[]{} );

        assertEquals( actionName, actionRequest.getActionName() );
    }

    @Test
    public void getArgumentsPositive() {

        Object[] args = new Object[]{ 3, ( byte ) 5, null, "hello" };
        ActionRequest actionRequest = new ActionRequest( "component", "action", args );

        assertArrayEquals( args, actionRequest.getArguments() );
    }
}
