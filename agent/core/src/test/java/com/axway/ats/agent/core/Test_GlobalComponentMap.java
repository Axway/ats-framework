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

import org.junit.Test;

import com.axway.ats.agent.core.exceptions.ComponentAlreadyDefinedException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;

public class Test_GlobalComponentMap extends BaseTest {

    @Test
    public void putComponentActionMapPositive() throws ComponentAlreadyDefinedException {

        Component actionMap = new Component( "putComponentPositive" );

        ComponentRepository.getInstance().putComponent( actionMap );
    }

    @Test(expected = ComponentAlreadyDefinedException.class)
    public void putComponentNegative() throws ComponentAlreadyDefinedException {

        Component actionMap = new Component( "putComponentNegative" );

        ComponentRepository.getInstance().putComponent( actionMap );
        ComponentRepository.getInstance().putComponent( actionMap );
    }

    @Test
    public void getComponentPositive() throws Exception {

        Component actionMap = new Component( "getComponentPositive" );

        ComponentRepository.getInstance().putComponent( actionMap );
        assertEquals( actionMap, ComponentRepository.getInstance().getComponent( "getComponentPositive" ) );
    }

    @Test(expected = NoSuchComponentException.class)
    public void getComponentNegative() throws Exception {

        ComponentRepository.getInstance().getComponent( "getComponentNegative" );
    }
}
