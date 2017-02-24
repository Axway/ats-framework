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
package com.axway.ats.agent.core.threading.data.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.junit.Test;

import com.axway.ats.agent.core.BaseTest;

public class Test_RangeDataConfig extends BaseTest {

    @Test
    public void replaceStringArguments() {

        //with range start and end
        RangeDataConfig rangeConfig = new RangeDataConfig( "param1", "test{0}@test.com", 10, 20 );

        assertEquals( "param1", rangeConfig.getParameterName() );
        assertEquals( ParameterProviderLevel.PER_THREAD_STATIC, rangeConfig.getParameterProviderLevel() );
        assertEquals( "test{0}@test.com", rangeConfig.getStaticValue() );
        assertEquals( 10, rangeConfig.getRangeStart() );
        assertEquals( 20, rangeConfig.getRangeEnd() );

        //only with range end
        rangeConfig = new RangeDataConfig( "param1", "test{0}@test.com", 5 );

        assertEquals( "param1", rangeConfig.getParameterName() );
        assertEquals( ParameterProviderLevel.PER_THREAD_STATIC, rangeConfig.getParameterProviderLevel() );
        assertEquals( "test{0}@test.com", rangeConfig.getStaticValue() );
        assertEquals( 5, rangeConfig.getRangeStart() );
        assertEquals( Integer.MAX_VALUE, rangeConfig.getRangeEnd() );

        //with range start and end, per invocation
        rangeConfig = new RangeDataConfig( "param1",
                                           "test{0}@test.com",
                                           10,
                                           20,
                                           ParameterProviderLevel.PER_INVOCATION );

        assertEquals( "param1", rangeConfig.getParameterName() );
        assertEquals( ParameterProviderLevel.PER_INVOCATION, rangeConfig.getParameterProviderLevel() );
        assertEquals( "test{0}@test.com", rangeConfig.getStaticValue() );
        assertEquals( 10, rangeConfig.getRangeStart() );
        assertEquals( 20, rangeConfig.getRangeEnd() );
    }

    @Test
    public void replaceIntegerArguments() {

        //with range start and end
        RangeDataConfig rangeConfig = new RangeDataConfig( "param1", 10, 20 );

        assertEquals( "param1", rangeConfig.getParameterName() );
        assertEquals( ParameterProviderLevel.PER_THREAD_STATIC, rangeConfig.getParameterProviderLevel() );
        assertNull( rangeConfig.getStaticValue() );
        assertEquals( 10, rangeConfig.getRangeStart() );
        assertEquals( 20, rangeConfig.getRangeEnd() );

        //only with range end
        rangeConfig = new RangeDataConfig( "param1", 5 );

        assertEquals( "param1", rangeConfig.getParameterName() );
        assertEquals( ParameterProviderLevel.PER_THREAD_STATIC, rangeConfig.getParameterProviderLevel() );
        assertNull( rangeConfig.getStaticValue() );
        assertEquals( 5, rangeConfig.getRangeStart() );
        assertEquals( Integer.MAX_VALUE, rangeConfig.getRangeEnd() );

        //with range start and end - per invocation
        rangeConfig = new RangeDataConfig( "param1", 10, 20, ParameterProviderLevel.PER_INVOCATION );

        assertEquals( "param1", rangeConfig.getParameterName() );
        assertEquals( ParameterProviderLevel.PER_INVOCATION, rangeConfig.getParameterProviderLevel() );
        assertNull( rangeConfig.getStaticValue() );
        assertEquals( 10, rangeConfig.getRangeStart() );
        assertEquals( 20, rangeConfig.getRangeEnd() );
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {

        RangeDataConfig rangeConfig = new RangeDataConfig( "param1", "test{0}@test.com", 10, 20 );

        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutStream = new ObjectOutputStream( byteOutStream );
        objectOutStream.writeObject( rangeConfig );

        ObjectInputStream objectInStream = new ObjectInputStream( new ByteArrayInputStream( byteOutStream.toByteArray() ) );
        RangeDataConfig deserializedRangeConfig = ( RangeDataConfig ) objectInStream.readObject();

        assertEquals( "param1", deserializedRangeConfig.getParameterName() );
        assertEquals( ParameterProviderLevel.PER_THREAD_STATIC,
                      deserializedRangeConfig.getParameterProviderLevel() );
        assertEquals( "test{0}@test.com", deserializedRangeConfig.getStaticValue() );
        assertEquals( 10, deserializedRangeConfig.getRangeStart() );
        assertEquals( 20, deserializedRangeConfig.getRangeEnd() );
    }

    @Test
    public void distributeSeveralHostsUneven() throws Exception {

        RangeDataConfig rangeConfig = new RangeDataConfig( "param1", "test{0}@test.com", 10, 20 );

        List<ParameterDataConfig> dustributedDataConfigs = rangeConfig.distribute( 3 );
        assertEquals( 3, dustributedDataConfigs.size() );

        for( int i = 0; i < 3; i++ ) {
            RangeDataConfig currentRangeDataConfig = ( RangeDataConfig ) dustributedDataConfigs.get( i );

            assertEquals( "param1", currentRangeDataConfig.getParameterName() );
            assertEquals( rangeConfig.getParameterProviderLevel(),
                          currentRangeDataConfig.getParameterProviderLevel() );
        }

        RangeDataConfig firstRangeDataConfig = ( RangeDataConfig ) dustributedDataConfigs.get( 0 );
        assertEquals( 10, firstRangeDataConfig.getRangeStart() );
        assertEquals( 12, firstRangeDataConfig.getRangeEnd() );
        
        RangeDataConfig secondRangeDataConfig = ( RangeDataConfig ) dustributedDataConfigs.get( 1 );
        assertEquals( 14, secondRangeDataConfig.getRangeStart() );
        assertEquals( 17, secondRangeDataConfig.getRangeEnd() );
        
        RangeDataConfig thirdRangeDataConfig = ( RangeDataConfig ) dustributedDataConfigs.get( 2 );
        assertEquals( 18, thirdRangeDataConfig.getRangeStart() );
        assertEquals( 21, thirdRangeDataConfig.getRangeEnd() );
    
    }

    @Test
    public void distributeSeveralHostsEeven() throws Exception {

        RangeDataConfig rangeConfig = new RangeDataConfig( "param1", "test{0}@test.com", 10, 42 );

        List<ParameterDataConfig> dustributedDataConfigs = rangeConfig.distribute( 3 );
        assertEquals( 3, dustributedDataConfigs.size() );

        for( int i = 0; i < 3; i++ ) {
            RangeDataConfig currentRangeDataConfig = ( RangeDataConfig ) dustributedDataConfigs.get( i );

            assertEquals( "param1", currentRangeDataConfig.getParameterName() );
            assertEquals( rangeConfig.getParameterProviderLevel(),
                          currentRangeDataConfig.getParameterProviderLevel() );
        }

        RangeDataConfig firstRangeDataConfig = ( RangeDataConfig ) dustributedDataConfigs.get( 0 );
        assertEquals( 10, firstRangeDataConfig.getRangeStart() );
        assertEquals( 20, firstRangeDataConfig.getRangeEnd() );

        RangeDataConfig secondRangeDataConfig = ( RangeDataConfig ) dustributedDataConfigs.get( 1 );
        assertEquals( 21, secondRangeDataConfig.getRangeStart() );
        assertEquals( 31, secondRangeDataConfig.getRangeEnd() );

        RangeDataConfig thirdRangeDataConfig = ( RangeDataConfig ) dustributedDataConfigs.get( 2 );
        assertEquals( 32, thirdRangeDataConfig.getRangeStart() );
        assertEquals( 42, thirdRangeDataConfig.getRangeEnd() );
    }

    @Test
    public void distributeOneHost() throws Exception {

        RangeDataConfig rangeConfig = new RangeDataConfig( "param1", "test{0}@test.com", 10, 20 );

        List<ParameterDataConfig> dustributedDataConfigs = rangeConfig.distribute( 1 );
        assertEquals( 1, dustributedDataConfigs.size() );

        RangeDataConfig currentRangeDataConfig = ( RangeDataConfig ) dustributedDataConfigs.get( 0 );

        assertEquals( "param1", currentRangeDataConfig.getParameterName() );
        assertEquals( rangeConfig.getParameterProviderLevel(),
                      currentRangeDataConfig.getParameterProviderLevel() );

        assertEquals( 10, currentRangeDataConfig.getRangeStart() );
        assertEquals( 20, currentRangeDataConfig.getRangeEnd() );
    }
}
