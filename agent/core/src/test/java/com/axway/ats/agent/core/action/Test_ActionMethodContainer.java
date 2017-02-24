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

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;

import org.junit.Test;

import com.axway.ats.agent.core.BaseTest;
import com.axway.ats.agent.core.exceptions.ActionAlreadyDefinedException;
import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;

public class Test_ActionMethodContainer extends BaseTest {

    @Test
    public void addPositive() throws SecurityException, NoSuchMethodException, ActionAlreadyDefinedException {

        //implementing method
        Method readMethod = FileInputStream.class.getDeclaredMethod( "read",
                                                                     new Class<?>[]{ ( new byte[]{} ).getClass() } );

        //create a new container
        ActionMethodContainer methodContainer = new ActionMethodContainer( "Component", "action" );

        //create a new action
        ActionMethod actionMethod = new ActionMethod( "Component", "action", readMethod, null );

        methodContainer.add( actionMethod );
    }

    @Test(expected = ActionAlreadyDefinedException.class)
    public void addNegativeAmbigousMethod() throws SecurityException, NoSuchMethodException,
                                           ActionAlreadyDefinedException {

        //implementing method
        Method readMethod = FileInputStream.class.getDeclaredMethod( "read",
                                                                     new Class<?>[]{ ( new byte[]{} ).getClass() } );

        //create a new container
        ActionMethodContainer methodContainer = new ActionMethodContainer( "Component", "action" );

        //create a new action
        ActionMethod actionMethod = new ActionMethod( "Component", "action", readMethod, null );

        methodContainer.add( actionMethod );
        methodContainer.add( actionMethod );
    }

    @Test
    public void getPositive() throws SecurityException, NoSuchMethodException, ActionAlreadyDefinedException,
                             NoCompatibleMethodFoundException {

        //implementing method
        Method readMethod = FileInputStream.class.getDeclaredMethod( "read",
                                                                     new Class<?>[]{ ( new byte[]{} ).getClass() } );

        //create a new container
        ActionMethodContainer methodContainer = new ActionMethodContainer( "Component", "action" );

        //create a new action
        ActionMethod actionMethod = new ActionMethod( "Component", "action", readMethod, null );

        methodContainer.add( actionMethod );
        assertNotNull( methodContainer.get( new Class<?>[]{ ( new byte[]{} ).getClass() } ) );
    }

    @Test(expected = NoCompatibleMethodFoundException.class)
    public void getNegativeNoMethod() throws SecurityException, NoSuchMethodException,
                                     ActionAlreadyDefinedException, NoCompatibleMethodFoundException {

        //implementing method
        Method readMethod = FileInputStream.class.getDeclaredMethod( "read",
                                                                     new Class<?>[]{ ( new byte[]{} ).getClass() } );

        //create a new container
        ActionMethodContainer methodContainer = new ActionMethodContainer( "Component", "action" );

        //create a new action
        ActionMethod actionMethod = new ActionMethod( "Component", "action", readMethod, null );

        methodContainer.add( actionMethod );
        assertNotNull( methodContainer.get( new Class<?>[]{} ) );
    }

    @Test(expected = NoCompatibleMethodFoundException.class)
    public void getNegativeAmbigous() throws SecurityException, NoSuchMethodException,
                                     ActionAlreadyDefinedException, NoCompatibleMethodFoundException {

        //implementing methods
        Method method1 = ActionMethodContainerTester.class.getDeclaredMethod( "ambiguousMethod",
                                                                              new Class<?>[]{ File.class } );
        Method method2 = ActionMethodContainerTester.class.getDeclaredMethod( "ambiguousMethod",
                                                                              new Class<?>[]{ FileInputStream.class } );

        //create a new container
        ActionMethodContainer methodContainer = new ActionMethodContainer( "Component", "action" );

        //create a new action
        ActionMethod actionMethod1 = new ActionMethod( "Component", "action", method1, null );
        ActionMethod actionMethod2 = new ActionMethod( "Component", "action", method2, null );

        methodContainer.add( actionMethod1 );
        methodContainer.add( actionMethod2 );

        methodContainer.get( new Class<?>[]{ null } );
    }
}
