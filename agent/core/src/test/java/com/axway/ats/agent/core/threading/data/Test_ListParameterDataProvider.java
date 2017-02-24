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
import java.util.List;

import org.junit.Test;

import com.axway.ats.agent.core.BaseTest;
import com.axway.ats.agent.core.action.ArgumentValue;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.threading.data.config.ParameterProviderLevel;

public class Test_ListParameterDataProvider extends BaseTest {

    @Test
    public void perThreadStaticGeneration() {

        List<String> values = new ArrayList<String>();
        for( int i = 10; i < 20; i++ ) {
            values.add( "value" + i );
        }

        ListParameterDataProvider dataProvider = new ListParameterDataProvider( "param1",
                                                                                values,
                                                                                ParameterProviderLevel.PER_THREAD_STATIC );

        ArgumentValue generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertEquals( "value10", generatedValue.getValue() );

        //make sure only one instance per thread is returned
        generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertEquals( "value10", generatedValue.getValue() );
    }

    @Test
    public void perInvocationGeneration() {

        List<String> values = new ArrayList<String>();
        for( int i = 10; i < 20; i++ ) {
            values.add( "value" + i );
        }

        ListParameterDataProvider dataProvider = new ListParameterDataProvider( "param1",
                                                                                values,
                                                                                ParameterProviderLevel.PER_INVOCATION );

        ArgumentValue generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertEquals( "value10", generatedValue.getValue() );

        //make sure only one instance per thread is returned
        generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertEquals( "value11", generatedValue.getValue() );
    }

    @Test
    public void whenRangeEndIsReachedProviderGoesBackToRangeStart() {

        List<String> values = new ArrayList<String>();
        for( int i = 10; i < 15; i++ ) {
            values.add( "value" + i );
        }

        ListParameterDataProvider dataProvider = new ListParameterDataProvider( "param1",
                                                                                values,
                                                                                ParameterProviderLevel.PER_INVOCATION );

        ArgumentValue generatedValue;
        for( int i = 10; i < 15; i++ ) {
            generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
            assertEquals( "param1", generatedValue.getName() );
            assertEquals( "value" + i, generatedValue.getValue() );
        }

        //make sure only one instance per thread is returned
        generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertEquals( "value10", generatedValue.getValue() );
    }

    @Test
    public void initialize() throws AgentException {

        List<String> values = new ArrayList<String>();
        for( int i = 10; i < 14; i++ ) {
            values.add( "value" + i );
        }

        ListParameterDataProvider dataProvider = new ListParameterDataProvider( "param1",
                                                                                values,
                                                                                ParameterProviderLevel.PER_THREAD );

        ArgumentValue generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertEquals( "value10", generatedValue.getValue() );

        dataProvider.initialize();

        //make sure only one instance per thread is returned
        generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertEquals( "value10", generatedValue.getValue() );
    }
}
