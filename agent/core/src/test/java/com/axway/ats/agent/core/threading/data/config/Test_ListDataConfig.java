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
import static org.testng.Assert.assertTrue;

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
    public void distributeSeveralHostsUneven() throws Exception {

        List<String> args = new ArrayList<String>();
        args.add("test1");
        args.add("test2");
        args.add("test3");
        args.add("test4");
        args.add("test5");
        args.add("test6");

        ListDataConfig listDataConfig = new ListDataConfig("param1", args);

        List<ParameterDataConfig> dustributedDataConfigs = listDataConfig.distribute(3);
        assertEquals(3, dustributedDataConfigs.size());

        for (int i = 0; i < 3; i++) {
            ListDataConfig currentListDataConfig = (ListDataConfig) dustributedDataConfigs.get(i);

            assertEquals("param1", currentListDataConfig.getParameterName());
            assertEquals(listDataConfig.getParameterProviderLevel(),
                         currentListDataConfig.getParameterProviderLevel());

            List<?> currentValues = currentListDataConfig.getValues();
            assertEquals(2, currentValues.size());
            assertEquals(args.get(i * 2), currentValues.get(0));
            assertEquals(args.get(i * 2 + 1), currentValues.get(1));
        }
    }

    @Test
    public void distributeOneHost() throws Exception {

        List<String> args = new ArrayList<String>();
        args.add("test1");
        args.add("test2");
        args.add("test3");

        ListDataConfig listDataConfig = new ListDataConfig("param1", args);

        List<ParameterDataConfig> dustributedDataConfigs = listDataConfig.distribute(1);
        assertEquals(1, dustributedDataConfigs.size());

        ListDataConfig currentListDataConfig = (ListDataConfig) dustributedDataConfigs.get(0);

        assertEquals("param1", currentListDataConfig.getParameterName());
        assertEquals(listDataConfig.getParameterProviderLevel(),
                     currentListDataConfig.getParameterProviderLevel());

        List<?> currentValues = currentListDataConfig.getValues();
        assertEquals(3, currentValues.size());
        assertEquals(args.get(0), currentValues.get(0));
        assertEquals(args.get(1), currentValues.get(1));
        assertEquals(args.get(2), currentValues.get(2));
    }

    @Test
    public void distributeN() {

        int minHosts = 1;
        int maxHosts = 100;

        int minValues = 1;
        int maxValues = 300;

        for (int host = minHosts; host < maxHosts; host++) {
            for (int value = minValues; value < maxValues; value++) {
                doDistributeN(host, generateValues(value));
            }
        }

    }

    private void doDistributeN( int hosts, List<String> values ) {

        ListDataConfig listDataConfig = new ListDataConfig("param1", values);
        List<ParameterDataConfig> dustributedDataConfigs = listDataConfig.distribute(hosts);
        assertEquals(hosts, dustributedDataConfigs.size());

        int valuesFound = 0;

        int maxValue = (int) Math.ceil( ((float) values.size()) / ((float) hosts));
        int minValue = (int) Math.floor(values.size() / hosts);

        //System.out.println("Min values per host/agent is: " + minValue);
        //System.out.println("Max values per host/agent is: " + maxValue);

        for (int i = 0; i < hosts; i++) {
            ListDataConfig currentListDataConfig = (ListDataConfig) dustributedDataConfigs.get(i);
            int currentValuesSize = currentListDataConfig.getValues().size();
            valuesFound += currentValuesSize;
            assertTrue(maxValue >= currentValuesSize, "Value > Max value");
            assertTrue(minValue <= currentValuesSize, "Value < Min value");

        }
        assertEquals(values.size(), valuesFound);

    }

    private List<String> generateValues( int count ) {

        List<String> values = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            values.add("var " + i);
        }

        return values;
    }
}
