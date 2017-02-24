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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

import org.junit.Test;

import com.axway.ats.agent.core.BaseTest;
import com.axway.ats.agent.core.action.ArgumentValue;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.threading.data.config.FileNamesDataConfig;
import com.axway.ats.agent.core.threading.data.config.ListDataConfig;
import com.axway.ats.agent.core.threading.data.config.ParameterDataConfig;
import com.axway.ats.agent.core.threading.data.config.ParameterProviderLevel;
import com.axway.ats.agent.core.threading.data.config.RangeDataConfig;
import com.axway.ats.agent.core.threading.exceptions.ParameterDataProviderNotSupportedException;

public class Test_ParameterDataProviderFactory extends BaseTest {

    @Test
    public void createIntegerRangeProvider() throws AgentException {

        RangeDataConfig rangeConfig = new RangeDataConfig( "param1", 10, 20 );

        ParameterDataProvider dataProvider = ParameterDataProviderFactory.createDataProvider( rangeConfig );

        assertEquals( IntegerRangeParameterDataProvider.class, dataProvider.getClass() );

        ArgumentValue generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertEquals( 10, generatedValue.getValue() );
    }

    @Test
    public void createStringRangeProvider() throws AgentException {

        RangeDataConfig rangeConfig = new RangeDataConfig( "param1", "user{0}@test.com", 10, 20 );

        ParameterDataProvider dataProvider = ParameterDataProviderFactory.createDataProvider( rangeConfig );

        assertEquals( StringRangeParameterDataProvider.class, dataProvider.getClass() );

        ArgumentValue generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertEquals( "user10@test.com", generatedValue.getValue() );
    }

    @Test
    public void createFileNamesProvider() throws AgentException, URISyntaxException {

        URL testFileURL = Test_ParameterDataProviderFactory.class.getResource( "/testfolder/classloader.html" );
        File testFile = new File( testFileURL.toURI() );

        FileNamesDataConfig fileNamesConfig = new FileNamesDataConfig( "param1",
                                                                       testFile.getParent(),
                                                                       true,
                                                                       ParameterProviderLevel.PER_INVOCATION );

        ParameterDataProvider dataProvider = ParameterDataProviderFactory.createDataProvider( fileNamesConfig );

        assertEquals( FileNamesParameterDataProvider.class, dataProvider.getClass() );

        ArgumentValue generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertTrue( ( ( String ) generatedValue.getValue() ).endsWith( ".html" ) );
    }

    @Test
    public void createListProvider() throws AgentException, URISyntaxException {

        ListDataConfig listDataConfig = new ListDataConfig( "param1",
                                                            new String[]{ "test1", "test2" },
                                                            ParameterProviderLevel.PER_INVOCATION );

        ParameterDataProvider dataProvider = ParameterDataProviderFactory.createDataProvider( listDataConfig );

        assertEquals( ListParameterDataProvider.class, dataProvider.getClass() );

        ArgumentValue generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertEquals( "test1", generatedValue.getValue() );
    }

    @Test(expected = ParameterDataProviderNotSupportedException.class)
    public void createStringRangeProviderNegativeUnsupportedProvider() throws AgentException {

        ParameterDataProviderFactory.createDataProvider( new UnsupportedProviderConfig() );
    }

    /**
     * Class used just for testing
     */
    @SuppressWarnings("serial")
    private static class UnsupportedProviderConfig implements ParameterDataConfig {

        public String getParameterName() {

            return null;
        }

        public ParameterProviderLevel getParameterProviderLevel() {

            return null;
        }

        @Override
        public void verifyDataConfig() throws AgentException {

        }
    }
}
