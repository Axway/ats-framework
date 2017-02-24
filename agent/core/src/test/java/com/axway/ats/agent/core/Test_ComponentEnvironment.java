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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.environment.AdditionalAction;
import com.axway.ats.environment.EnvironmentUnit;

public class Test_ComponentEnvironment extends BaseTest {

    private static ComponentEnvironment goodEnvironment;
    private static ComponentEnvironment badEnvironment;

    @BeforeClass
    public static void setUpTest_ComponentEnvironment() {

        List<EnvironmentUnit> goodEnvironmentUnits = new ArrayList<EnvironmentUnit>();
        goodEnvironmentUnits.add( new MockEnvironmentUnit() );

        goodEnvironment = new ComponentEnvironment( TEST_COMPONENT_NAME, null, goodEnvironmentUnits, "" );

        List<EnvironmentUnit> badEnvironmentUnits = new ArrayList<EnvironmentUnit>();
        badEnvironmentUnits.add( new MockEnvironmentUnitExceptions() );

        badEnvironment = new ComponentEnvironment( TEST_COMPONENT_NAME, null, badEnvironmentUnits, "" );
    }

    @Before
    public void setUp() {

        //initialize the mock environment unit
        MockEnvironmentUnit.backupCalled = false;
        MockEnvironmentUnit.restoreCalled = false;
    }

    @Test
    public void backupPositive() throws AgentException {

        goodEnvironment.backup( null );

        assertTrue( MockEnvironmentUnit.backupCalled );
    }

    @Test(expected = AgentException.class)
    public void backupNegative() throws AgentException {

        badEnvironment.backup( null );
    }

    @Test
    public void backupOnlyIfNotAlreadyDonePositive() throws AgentException {

        goodEnvironment.backupOnlyIfNotAlreadyDone();

        assertTrue( MockEnvironmentUnit.backupCalled );
    }

    @Test(expected = AgentException.class)
    public void backupOnlyIfNotAlreadyDoneNegative() throws AgentException {

        badEnvironment.backupOnlyIfNotAlreadyDone();
    }

    @Test
    public void restorePositive() throws AgentException {

        goodEnvironment.restore( null );

        assertTrue( MockEnvironmentUnit.restoreCalled );
    }

    @Test(expected = AgentException.class)
    public void restoreNegative() throws AgentException {

        badEnvironment.restore( null );
    }

    @Test
    public void restoreWithAdditionalActions() throws AgentException {

        List<AdditionalAction> aActions = new ArrayList<AdditionalAction>();
        aActions.add( new MockSystemProcessAction( "proc1", 0 ) );
        aActions.add( new MockSystemProcessAction( "proc2", 0 ) ); // this will not get executed as it will be overriden by the next
        aActions.add( new MockSystemProcessAction( "proc2", 1 ) ); // this will override the previous as it has the same command

        MockEnvironmentUnit mockEnvironmentUnit = new MockEnvironmentUnit();
        mockEnvironmentUnit.addAdditionalActions( aActions );

        List<EnvironmentUnit> environmentUnits = new ArrayList<EnvironmentUnit>();
        environmentUnits.add( mockEnvironmentUnit );

        // when we call restore on the environment, it will run restore on all environment units
        // and then will execute all the additional actions
        ComponentEnvironment environment = new ComponentEnvironment( TEST_COMPONENT_NAME, null,
                                                                     environmentUnits, "" );
        environment.restore( null );

        List<AdditionalAction> aActionsAfterRestore = environment.getEnvironmentUnits()
                                                                 .get( 0 )
                                                                 .getAdditionalActions();

        MockSystemProcessAction aAction1 = ( MockSystemProcessAction ) aActionsAfterRestore.get( 0 );
        assertTrue( aAction1.isExecuted() );
        assertEquals( 0, aAction1.getSleepInterval() );
        assertEquals( " shell command 'proc1'", aAction1.getDescription() );

        MockSystemProcessAction aAction2 = ( MockSystemProcessAction ) aActionsAfterRestore.get( 1 );
        assertFalse( aAction2.isExecuted() );
        assertEquals( 0, aAction2.getSleepInterval() );

        MockSystemProcessAction aAction3 = ( MockSystemProcessAction ) aActionsAfterRestore.get( 2 );
        assertTrue( aAction3.isExecuted() );
        assertEquals( 1, aAction3.getSleepInterval() );
    }
}
