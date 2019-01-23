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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.axway.ats.agent.core.BaseTest;

public class Test_ListDataConfig extends BaseTest {

    @Test
    public void gettersPositive() {

        List<String> args = new ArrayList<String>();
        args.add("test1");
        args.add("test2");

        ListDataConfig listDataConfig = new ListDataConfig("user", args);

        assertEquals(args, listDataConfig.getValues());
        assertEquals("user", listDataConfig.getParameterName());
        assertEquals(ParameterProviderLevel.PER_THREAD_STATIC, listDataConfig.getParameterProviderLevel());

        listDataConfig = new ListDataConfig("user", args, ParameterProviderLevel.PER_INVOCATION);

        assertEquals(args, listDataConfig.getValues());
        assertEquals("user", listDataConfig.getParameterName());
        assertEquals(ParameterProviderLevel.PER_INVOCATION, listDataConfig.getParameterProviderLevel());

        listDataConfig = new ListDataConfig("user", new String[]{ "test1", "test2" });

        assertEquals(args, listDataConfig.getValues());
        assertEquals("user", listDataConfig.getParameterName());
        assertEquals(ParameterProviderLevel.PER_THREAD_STATIC, listDataConfig.getParameterProviderLevel());

        listDataConfig = new ListDataConfig("user",
                                            new String[]{ "test1", "test2" },
                                            ParameterProviderLevel.PER_INVOCATION);

        assertEquals(args, listDataConfig.getValues());
        assertEquals("user", listDataConfig.getParameterName());
        assertEquals(ParameterProviderLevel.PER_INVOCATION, listDataConfig.getParameterProviderLevel());
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {

        List<String> args = new ArrayList<String>();
        args.add("test1");
        args.add("test2");

        ListDataConfig listDataConfig = new ListDataConfig("param1", args);

        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutStream = new ObjectOutputStream(byteOutStream);
        objectOutStream.writeObject(listDataConfig);

        ObjectInputStream objectInStream = new ObjectInputStream(new ByteArrayInputStream(byteOutStream.toByteArray()));
        ListDataConfig deserializedListDataConfig = (ListDataConfig) objectInStream.readObject();

        assertEquals("param1", deserializedListDataConfig.getParameterName());
        assertEquals(ParameterProviderLevel.PER_THREAD_STATIC,
                     deserializedListDataConfig.getParameterProviderLevel());
        assertEquals(args, deserializedListDataConfig.getValues());
    }

    @Test
    public void distributeOneHost() throws Exception {

        List<String> args = generateValues(100);

        ListDataConfig listDataConfig = new ListDataConfig("param1", args);

        List<ParameterDataConfig> dustributedDataConfigs = listDataConfig.distribute(1);
        assertEquals(1, dustributedDataConfigs.size());

        ListDataConfig currentListDataConfig = (ListDataConfig) dustributedDataConfigs.get(0);

        assertEquals("param1", currentListDataConfig.getParameterName());
        assertEquals(listDataConfig.getParameterProviderLevel(),
                     currentListDataConfig.getParameterProviderLevel());

        List<?> currentValues = currentListDataConfig.getValues();
        assertEquals(args.size(), currentValues.size());
        for(int i = 0 ; i < args.size() ; i++) {
            assertEquals(args.get(i), currentValues.get(i));
        }
        
    }

    @Test
    public void test_2_loaders_5_values() {

        final List<String> args = generateValues(5);

        final String paramName = "param1";

        final int loadersCount = 2;

        ListDataConfig listDataConfig = new ListDataConfig(paramName, args);

        List<ParameterDataConfig> dustributedDataConfigs = listDataConfig.distribute(loadersCount);
        assertEquals(loadersCount, dustributedDataConfigs.size());

        ListDataConfig currentListDataConfig = (ListDataConfig) dustributedDataConfigs.get(0);
        List<?> currentValues = currentListDataConfig.getValues();
        assertEquals(2, currentValues.size());
        assertEquals(args.get(0), currentValues.get(0));
        assertEquals(args.get(1), currentValues.get(1));
        
        currentListDataConfig = (ListDataConfig) dustributedDataConfigs.get(1);
        currentValues = currentListDataConfig.getValues();
        assertEquals(3, currentValues.size());
        assertEquals(args.get(2), currentValues.get(0));
        assertEquals(args.get(3), currentValues.get(1));
        assertEquals(args.get(4), currentValues.get(2));
    }
    
    @Test
    public void test_3_loaders_6_values() {

        final List<String> args = generateValues(6);

        final String paramName = "param1";

        final int loadersCount = 3;

        ListDataConfig listDataConfig = new ListDataConfig(paramName, args);

        List<ParameterDataConfig> dustributedDataConfigs = listDataConfig.distribute(loadersCount);
        assertEquals(loadersCount, dustributedDataConfigs.size());

        ListDataConfig currentListDataConfig = (ListDataConfig) dustributedDataConfigs.get(0);
        List<?> currentValues = currentListDataConfig.getValues();
        assertEquals(2, currentValues.size());
        assertEquals(args.get(0), currentValues.get(0));
        assertEquals(args.get(1), currentValues.get(1));
        
        currentListDataConfig = (ListDataConfig) dustributedDataConfigs.get(1);
        currentValues = currentListDataConfig.getValues();
        assertEquals(2, currentValues.size());
        assertEquals(args.get(2), currentValues.get(0));
        assertEquals(args.get(3), currentValues.get(1));
        
        currentListDataConfig = (ListDataConfig) dustributedDataConfigs.get(2);
        currentValues = currentListDataConfig.getValues();
        assertEquals(2, currentValues.size());
        assertEquals(args.get(4), currentValues.get(0));
        assertEquals(args.get(5), currentValues.get(1));
    }
    
    private List<String> generateValues( int count ) {

        List<String> args = new ArrayList<String>();
        for(int i = 0 ; i < count ; i++) {
            args.add(i+"");
        }
        return args;
    }
}
