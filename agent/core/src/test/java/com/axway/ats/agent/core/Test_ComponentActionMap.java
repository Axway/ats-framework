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

import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.loading.ClasspathComponentLoader;

public class Test_ComponentActionMap extends BaseTest {

    private static ComponentActionMap actionMap;

    @BeforeClass
    public static void setUpTest_ComponentActionMap() throws AgentException {

        ClasspathComponentLoader classPathLoader = new ClasspathComponentLoader( new Object() );
        classPathLoader.loadAvailableComponents( ComponentRepository.getInstance() );
        actionMap = ComponentRepository.getInstance().getComponentActionMap( TEST_COMPONENT_NAME );
    }

    @Test
    public void getActionMethodPositive() throws NoSuchActionException, NoCompatibleMethodFoundException {

        assertNotNull( actionMap.getActionMethod( "action 1", new Class<?>[]{ Integer.class } ) );
    }

    @Test(expected = NoSuchActionException.class)
    public void getActionMethodNegative() throws NoSuchActionException, NoCompatibleMethodFoundException {

        actionMap.getActionMethod( "action 1123", new Class<?>[]{} );
    }

    @Test
    public void getInitializationHandler() throws NoSuchActionException {

        assertNotNull( actionMap.getInitializationHandler() );
    }

    @Test
    public void getFinalizationHandler() throws NoSuchActionException {

        assertNotNull( actionMap.getFinalizationHandler() );
    }

    @Test
    public void getCleanupHandler() throws NoSuchActionException {

        assertNotNull( actionMap.getCleanupHandler() );
    }
}
