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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.axway.ats.agent.core.BaseTest;
import com.axway.ats.core.reflect.AmbiguousMethodException;
import com.axway.ats.core.reflect.MethodFinder;
import com.axway.ats.core.reflect.TypeComparisonRule;

public class Test_StringToEnumRule extends BaseTest {

    @Test
    public void isCompatiblePositive() throws NoSuchMethodException, AmbiguousMethodException {

        List<TypeComparisonRule> typeComparisonRules = new ArrayList<TypeComparisonRule>();
        typeComparisonRules.add( new StringToEnumRule() );

        MethodFinder methodFinder = new MethodFinder( "methods",
                                                      Arrays.asList( ActionClassEnumArguments.class.getDeclaredMethods() ),
                                                      typeComparisonRules );

        //String argument
        assertNotNull( methodFinder.findMethod( new Class<?>[]{ String.class } ) );

        //array of Strings argument
        assertNotNull( methodFinder.findMethod( new Class<?>[]{ ( new String[]{} ).getClass(), Integer.TYPE } ) );
    }

    @Test(expected = NoSuchMethodException.class)
    public void isCompatibleNegativeWrongArgType() throws NoSuchMethodException, AmbiguousMethodException {

        List<TypeComparisonRule> typeComparisonRules = new ArrayList<TypeComparisonRule>();
        typeComparisonRules.add( new StringToEnumRule() );

        MethodFinder methodFinder = new MethodFinder( "methods",
                                                      Arrays.asList( ActionClassEnumArguments.class.getDeclaredMethods() ),
                                                      typeComparisonRules );

        //String argument
        assertNotNull( methodFinder.findMethod( new Class<?>[]{ Integer.class } ) );
    }

    @Test(expected = NoSuchMethodException.class)
    public void isCompatibleNegativeArgisArrayParamIsNot() throws NoSuchMethodException,
                                                          AmbiguousMethodException {

        List<TypeComparisonRule> typeComparisonRules = new ArrayList<TypeComparisonRule>();
        typeComparisonRules.add( new StringToEnumRule() );

        MethodFinder methodFinder = new MethodFinder( "methods",
                                                      Arrays.asList( ActionClassEnumArguments.class.getDeclaredMethods() ),
                                                      typeComparisonRules );

        //String argument
        assertNotNull( methodFinder.findMethod( new Class<?>[]{ ( new int[]{} ).getClass() } ) );
    }
}
