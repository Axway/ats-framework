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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.axway.ats.agent.core.exceptions.ActionExecutionException;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.exceptions.ComponentAlreadyDefinedException;
import com.axway.ats.agent.core.exceptions.InternalComponentException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.environment.EnvironmentUnit;

public class Test_EnvironmentHandler extends BaseTest {

    private static Component          component          = new Component( TEST_COMPONENT_NAME );
    private static EnvironmentHandler environmentHandler = EnvironmentHandler.getInstance();

    @Before
    public void setUp() throws ComponentAlreadyDefinedException {

        ComponentActionMap actionMap = new ComponentActionMap( TEST_COMPONENT_NAME );
        actionMap.registerActionClass( ActionClassOne.class );
        actionMap.registerActionClass( ActionClassTwo.class );
        actionMap.setInitializationHandler( MockInitHandler.class );
        actionMap.setFinalizationHandler( MockFinalizationHandler.class );
        actionMap.setCleanupHandler( MockEnvironementCleanupHandler.class );
        component.setActionMap( actionMap );

        List<EnvironmentUnit> environmentUnits = new ArrayList<EnvironmentUnit>();
        environmentUnits.add( new MockEnvironmentUnit() );

        ComponentEnvironment environment = new ComponentEnvironment( TEST_COMPONENT_NAME, null,
                                                                     environmentUnits, "" );
        component.setEnvironments( new ArrayList<ComponentEnvironment>( Arrays.asList( environment ) ) );

        ComponentRepository.getInstance().clear();
        ComponentRepository.getInstance().putComponent( component );

        //init the mock environment unit
        MockEnvironmentUnit.backupCalled = false;
        MockEnvironmentUnit.restoreCalled = false;
        MockEnvironmentUnit2.backupCalled = false;
        MockEnvironmentUnit2.restoreCalled = false;
        MockEnvironementCleanupHandler.cleanCalled = false;
    }

    @After
    public void afterMethod() {

        component = new Component( TEST_COMPONENT_NAME );
    }

    @Test
    public void cleanupOneComponentPositive() throws Exception {

        environmentHandler.restore( TEST_COMPONENT_NAME, null, null );

        assertTrue( MockEnvironmentUnit.restoreCalled );
        assertTrue( MockEnvironementCleanupHandler.cleanCalled );
    }

    @Test
    public void cleanupOneComponentNoCleanupHandler() throws Exception {

        component.getActionMap().setCleanupHandler( null );
        environmentHandler.restore( TEST_COMPONENT_NAME, null, null );

        assertTrue( MockEnvironmentUnit.restoreCalled );
        assertFalse( MockEnvironementCleanupHandler.cleanCalled );
    }

    @Test(expected = NoSuchComponentException.class)
    public void cleanupOneComponentNegativeNoSuchComponent() throws Exception {

        EnvironmentHandler.getInstance().restore( "asdas", null, null );
    }

    @Test
    public void cleanupAllComponentsPositive() throws Exception {

        component.getEnvironments().get( 0 ).getEnvironmentUnits().add( new MockEnvironmentUnit2() );

        EnvironmentHandler.getInstance().restoreAll( null );

        assertTrue( MockEnvironmentUnit.restoreCalled );
        assertTrue( MockEnvironmentUnit2.restoreCalled );
        assertTrue( MockEnvironementCleanupHandler.cleanCalled );
    }

    @Test
    public void cleanupAllComponentsNoCleanupHandlerPositive() throws Exception {

        component.getActionMap().setCleanupHandler( null );
        EnvironmentHandler.getInstance().restoreAll( null );

        assertTrue( MockEnvironmentUnit.restoreCalled );
        assertFalse( MockEnvironementCleanupHandler.cleanCalled );
    }

    @Test
    public void backupOneComponentPositive() throws Exception {

        environmentHandler.backup( TEST_COMPONENT_NAME, null, null );

        assertTrue( MockEnvironmentUnit.backupCalled );
    }

    @Test(expected = NoSuchComponentException.class)
    public void backupOneComponentNegativeNoSuchComponent() throws Exception {

        EnvironmentHandler.getInstance().backup( "asdas", null, null );
    }

    @Test
    public void backupAllComponentsPositive() throws Exception {

        ComponentEnvironment env2 = new ComponentEnvironment( TEST_COMPONENT_NAME, "env name 2",
                                                              Arrays.asList( new EnvironmentUnit[]{ new MockEnvironmentUnit2() } ),
                                                              "" );
        component.getEnvironments().add( env2 );

        EnvironmentHandler.getInstance().backupAll( null );

        assertTrue( MockEnvironmentUnit.backupCalled );
        assertFalse( MockEnvironmentUnit2.backupCalled );
    }

    @Test
    public void multipleEnvironments_backupSecond() throws ActionExecutionException, NoSuchComponentException,
                                                    InternalComponentException, AgentException {

        ComponentEnvironment env1 = new ComponentEnvironment( TEST_COMPONENT_NAME, "env name 1",
                                                              Arrays.asList( new EnvironmentUnit[]{ new MockEnvironmentUnit() } ),
                                                              "" );
        ComponentEnvironment env2 = new ComponentEnvironment( TEST_COMPONENT_NAME, "env name 2",
                                                              Arrays.asList( new EnvironmentUnit[]{ new MockEnvironmentUnit2() } ),
                                                              "" );

        component.setEnvironments( Arrays.asList( env1, env2 ) );

        EnvironmentHandler.getInstance().backup( TEST_COMPONENT_NAME, "env name 2", null );

        assertFalse( MockEnvironmentUnit.backupCalled );
        assertTrue( MockEnvironmentUnit2.backupCalled );
    }

    @Test
    public void multipleEnvironments_restoreSecond() throws ActionExecutionException,
                                                     NoSuchComponentException, InternalComponentException,
                                                     AgentException {

        ComponentEnvironment env1 = new ComponentEnvironment( TEST_COMPONENT_NAME, "env name 1",
                                                              Arrays.asList( new EnvironmentUnit[]{ new MockEnvironmentUnit() } ),
                                                              "" );
        ComponentEnvironment env2 = new ComponentEnvironment( TEST_COMPONENT_NAME, "env name 2",
                                                              Arrays.asList( new EnvironmentUnit[]{ new MockEnvironmentUnit2() } ),
                                                              "" );

        component.setEnvironments( Arrays.asList( env1, env2 ) );

        EnvironmentHandler.getInstance().restore( TEST_COMPONENT_NAME, "env name 2", null );

        assertFalse( MockEnvironmentUnit.restoreCalled );
        assertTrue( MockEnvironmentUnit2.restoreCalled );
    }

    @Test
    public void multipleEnvironments_backupAll_specificEnv() throws ActionExecutionException,
                                                             NoSuchComponentException,
                                                             InternalComponentException, AgentException {

        ComponentEnvironment env1 = new ComponentEnvironment( TEST_COMPONENT_NAME, "env name 1",
                                                              Arrays.asList( new EnvironmentUnit[]{ new MockEnvironmentUnit() } ),
                                                              "" );
        ComponentEnvironment env2 = new ComponentEnvironment( TEST_COMPONENT_NAME, "env name 2",
                                                              Arrays.asList( new EnvironmentUnit[]{ new MockEnvironmentUnit2() } ),
                                                              "" );

        component.setEnvironments( Arrays.asList( env1, env2 ) );

        EnvironmentHandler.getInstance().backupAll( "env name 2" );

        assertFalse( MockEnvironmentUnit.backupCalled );
        assertTrue( MockEnvironmentUnit2.backupCalled );
    }

    @Test
    public void multipleEnvironments_restoreAll_specificEnv() throws ActionExecutionException,
                                                              NoSuchComponentException,
                                                              InternalComponentException, AgentException {

        ComponentEnvironment env1 = new ComponentEnvironment( TEST_COMPONENT_NAME, "env name 1",
                                                              Arrays.asList( new EnvironmentUnit[]{ new MockEnvironmentUnit() } ),
                                                              "" );
        ComponentEnvironment env2 = new ComponentEnvironment( TEST_COMPONENT_NAME, "env name 2",
                                                              Arrays.asList( new EnvironmentUnit[]{ new MockEnvironmentUnit2() } ),
                                                              "" );

        component.setEnvironments( Arrays.asList( env1, env2 ) );

        EnvironmentHandler.getInstance().restoreAll( "env name 2" );

        assertFalse( MockEnvironmentUnit.restoreCalled );
        assertTrue( MockEnvironmentUnit2.restoreCalled );
    }

}
