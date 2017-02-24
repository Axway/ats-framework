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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.axway.ats.agent.core.exceptions.ComponentAlreadyDefinedException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.environment.EnvironmentUnit;

public class Test_ComponentRepository extends BaseTest {

    private static Component           component           = new Component( TEST_COMPONENT_NAME );
    private static ComponentRepository componentRepository = ComponentRepository.getInstance();

    @Before
    public void setUp() {

        ComponentActionMap actionMap = new ComponentActionMap( TEST_COMPONENT_NAME );
        actionMap.registerActionClass( ActionClassOne.class );
        actionMap.registerActionClass( ActionClassTwo.class );
        actionMap.setInitializationHandler( MockInitHandler.class );
        actionMap.setFinalizationHandler( MockFinalizationHandler.class );
        component.setActionMap( actionMap );

        List<EnvironmentUnit> environmentUnits = new ArrayList<EnvironmentUnit>();
        environmentUnits.add( new MockEnvironmentUnit() );

        ComponentEnvironment environment = new ComponentEnvironment( TEST_COMPONENT_NAME, null,
                                                                     environmentUnits, "" );
        component.setEnvironments( Arrays.asList( environment ) );

        //init the mock environment unit
        MockEnvironmentUnit.backupCalled = false;
        MockEnvironmentUnit.restoreCalled = false;

        componentRepository.clear();
    }

    @Test
    public void putComponentPositive() throws ComponentAlreadyDefinedException {

        componentRepository.putComponent( component );
    }

    @Test(expected = ComponentAlreadyDefinedException.class)
    public void putComponentNegative() throws ComponentAlreadyDefinedException {

        componentRepository.putComponent( component );
        componentRepository.putComponent( component );
    }

    @Test
    public void getComponentPositive() throws ComponentAlreadyDefinedException, NoSuchComponentException {

        componentRepository.putComponent( component );
        assertNotNull( componentRepository.getComponent( TEST_COMPONENT_NAME ) );
    }

    @Test(expected = NoSuchComponentException.class)
    public void getComponentNegative() throws ComponentAlreadyDefinedException, NoSuchComponentException {

        componentRepository.getComponent( "fasdfa" );
    }

    @Test
    public void getComponentActionMapPositive() throws ComponentAlreadyDefinedException,
                                                NoSuchComponentException {

        componentRepository.putComponent( component );
        assertNotNull( componentRepository.getComponentActionMap( TEST_COMPONENT_NAME ) );
    }

    @Test(expected = NoSuchComponentException.class)
    public void getComponentActionMapNegative() throws ComponentAlreadyDefinedException,
                                                NoSuchComponentException {

        componentRepository.getComponentActionMap( "fasdfa" );
    }

    @Test
    public void getComponentEnvironmentPositive() throws ComponentAlreadyDefinedException,
                                                  NoSuchComponentException {

        componentRepository.putComponent( component );
        assertNotNull( componentRepository.getComponentEnvironment( TEST_COMPONENT_NAME ) );
    }

    @Test(expected = NoSuchComponentException.class)
    public void getComponentEnvironmentNegative() throws ComponentAlreadyDefinedException,
                                                  NoSuchComponentException {

        componentRepository.getComponentEnvironment( "fasdfa" );
    }

    @Test
    public void getAllComponentsPositive() throws ComponentAlreadyDefinedException, NoSuchComponentException {

        assertEquals( 0, componentRepository.getAllComponents().size() );

        componentRepository.putComponent( component );
        assertEquals( 1, componentRepository.getAllComponents().size() );
    }

    @Test
    public void initializeAllComponentsPositive() throws ComponentAlreadyDefinedException,
                                                  NoSuchComponentException {

        componentRepository.putComponent( component );
        componentRepository.initializeAllComponents();

        //verify backup was made
        assertTrue( MockEnvironmentUnit.backupCalled );
    }

    @Test
    public void initializeAllComponentsPositiveNoInitHandler() throws ComponentAlreadyDefinedException,
                                                               NoSuchComponentException {

        component.getActionMap().setInitializationHandler( null );

        componentRepository.putComponent( component );
        componentRepository.initializeAllComponents();

        //verify backup was made
        assertTrue( MockEnvironmentUnit.backupCalled );
    }

    @Test
    public void finalizeAllComponentsPositive() throws ComponentAlreadyDefinedException,
                                                NoSuchComponentException {

        componentRepository.putComponent( component );
        componentRepository.finalizeAllComponents();
    }

    @Test
    public void finalizeAllComponentsPositiveNoFinalHandler() throws ComponentAlreadyDefinedException,
                                                              NoSuchComponentException {

        component.getActionMap().setFinalizationHandler( null );

        componentRepository.putComponent( component );
        componentRepository.finalizeAllComponents();
    }

    @Test
    public void clearPositive() throws ComponentAlreadyDefinedException, NoSuchComponentException {

        componentRepository.putComponent( component );
        assertEquals( 1, componentRepository.getAllComponents().size() );

        componentRepository.clear();
        assertEquals( 0, componentRepository.getAllComponents().size() );

    }
}
