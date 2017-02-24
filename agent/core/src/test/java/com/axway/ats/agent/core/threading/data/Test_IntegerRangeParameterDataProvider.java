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
package com.axway.ats.agent.core.threading.data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;

import com.axway.ats.agent.core.BaseTest;
import com.axway.ats.agent.core.action.ArgumentValue;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.threading.data.config.ParameterProviderLevel;

public class Test_IntegerRangeParameterDataProvider extends BaseTest {

    @Test
    public void perThreadGeneration() {

        IntegerRangeParameterDataProvider dataProvider = new IntegerRangeParameterDataProvider( "param1",
                                                                                                10,
                                                                                                20,
                                                                                                ParameterProviderLevel.PER_THREAD_STATIC );

        ArgumentValue generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertEquals( 10, generatedValue.getValue() );

        //make sure only one instance per thread is returned
        generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertEquals( 10, generatedValue.getValue() );
    }

    @Test
    public void perInvocationGeneration() {

        IntegerRangeParameterDataProvider dataProvider = new IntegerRangeParameterDataProvider( "param1",
                                                                                                10,
                                                                                                20,
                                                                                                ParameterProviderLevel.PER_INVOCATION );

        ArgumentValue generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertEquals( 10, generatedValue.getValue() );

        //make sure only one instance per thread is returned
        generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertEquals( 11, generatedValue.getValue() );
    }

    @Test
    public void whenRangeEndIsReachedProviderGoesBackToRangeStart() {

        IntegerRangeParameterDataProvider dataProvider = new IntegerRangeParameterDataProvider( "param1",
                                                                                                10,
                                                                                                14,
                                                                                                ParameterProviderLevel.PER_INVOCATION );

        ArgumentValue generatedValue;
        for( int i = 10; i < 15; i++ ) {
            generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
            assertEquals( "param1", generatedValue.getName() );
            assertEquals( i, generatedValue.getValue() );
        }

        //make sure only one instance per thread is returned
        generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertEquals( 10, generatedValue.getValue() );
    }

    @Test
    public void initialize() throws AgentException {

        IntegerRangeParameterDataProvider dataProvider = new IntegerRangeParameterDataProvider( "param1",
                                                                                                10,
                                                                                                20,
                                                                                                ParameterProviderLevel.PER_INVOCATION );

        ArgumentValue generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertEquals( 10, generatedValue.getValue() );

        dataProvider.initialize();

        //make sure only one instance per thread is returned
        generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertEquals( 10, generatedValue.getValue() );
    }
}
