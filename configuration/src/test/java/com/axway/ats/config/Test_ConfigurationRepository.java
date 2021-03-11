/*
 * Copyright 2017-2021 Axway Software
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
package com.axway.ats.config;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.config.exceptions.ConfigurationException;
import com.axway.ats.config.exceptions.NoSuchPropertyException;

public class Test_ConfigurationRepository extends BaseTest {

    private static final String                  configFile1Name  = "resource1.properties";
    private static final String                  configFile2Name  = "resource2.xml";

    private static File                          configFile1;
    private static File                          configFile2;

    private static final ConfigurationRepository configRepository = ConfigurationRepository.getInstance();

    @BeforeClass
    public static void setUpTest_ConfigurationRepository() throws URISyntaxException {

        URL configFile1URL = Test_ConfigurationRepository.class.getResource(configFile1Name);
        configFile1 = new File(configFile1URL.toURI());

        URL configFile2URL = Test_ConfigurationRepository.class.getResource(configFile2Name);
        configFile2 = new File(configFile2URL.toURI());
    }

    @Before
    public void setUp() {

        configRepository.initialize();
    }

    @Test
    public void registerUserConfigResourcePositive() throws ConfigurationException {

        configRepository.registerConfigFile(configFile1.getAbsolutePath());
        assertEquals("perf-8.perf.domain.com",
                     configRepository.getProperty("common.testboxes.testbox1.host"));
        assertEquals(12, configRepository.getProperties("common.testboxes").keySet().size());
    }

    @Test
    public void verifyResourceTakenLastOverrideOtherResource() throws ConfigurationException {

        configRepository.registerConfigFile(configFile2.getAbsolutePath());
        configRepository.registerConfigFile(configFile1.getAbsolutePath());
        assertEquals("perf-8.perf.domain.com",
                     configRepository.getProperty("common.testboxes.testbox1.host"));
        assertEquals(12, configRepository.getProperties("common.testboxes").keySet().size());
    }

    @Test
    public void verifyPropertyNotInOneResourceWillBeTakenFromOthers() throws ConfigurationException {

        configRepository.registerConfigFile(configFile1.getAbsolutePath());
        configRepository.registerConfigFile(configFile2.getAbsolutePath());
        assertEquals("exch2003.perf.domain.com",
                     configRepository.getProperty("common.mailservers.second-mail-server.host"));
        assertEquals(2,
                     configRepository.getProperties("common.mailservers.second-mail-server")
                                     .keySet()
                                     .size());
    }

    @Test
    public void verifyInMemoryConfigWillOverwriteAllResources() throws ConfigurationException {

        configRepository.registerConfigFile(configFile1.getAbsolutePath());
        configRepository.registerConfigFile(configFile2.getAbsolutePath());

        configRepository.setTempProperty("common.testboxes.testbox1.host", "test");
        assertEquals("test", configRepository.getProperty("common.testboxes.testbox1.host"));
        assertEquals(1, configRepository.getProperties("common.testboxes").keySet().size());
    }

    @Test( expected = NoSuchPropertyException.class)
    public void verifyDefaultSettingsAreTakenLast() throws ConfigurationException {

        configRepository.registerConfigFile(configFile1.getAbsolutePath());

        //the value should be taken from the first resource, as the second one is added to the bottom of the list
        assertEquals("perf-8.perf.domain.com",
                     configRepository.getProperty("common.testboxes.testbox1.host"));
        assertEquals(12, configRepository.getProperties("common.testboxes").keySet().size());

        //the value should be taken from the first resource, as the second one is added to the bottom of the list
        assertEquals("value1", configRepository.getProperty("default.settings.setting1"));
        assertEquals(1, configRepository.getProperties("default.settings").keySet().size());
    }

    @Test( expected = NoSuchPropertyException.class)
    public void getPropertyWhichDoesNotExist() throws ConfigurationException {

        configRepository.registerConfigFile(configFile1.getAbsolutePath());
        configRepository.registerConfigFile(configFile2.getAbsolutePath());

        configRepository.getProperty("asdfsadf");
    }

    @Test
    public void getPropertiesWhichDoNotExist() throws ConfigurationException {

        configRepository.registerConfigFile(configFile1.getAbsolutePath());
        configRepository.registerConfigFile(configFile2.getAbsolutePath());

        assertEquals(0, configRepository.getProperties("sdasdasd").keySet().size());
    }

    @Test
    public void checkPropertyIsTrimmed() throws ConfigurationException {

        configRepository.registerConfigFile(configFile1.getAbsolutePath());

        // the original value is 'abc ' but we expect 'abc'
        assertEquals("abc", configRepository.getProperty("test.has.white.space.at.end"));
    }

    @Test
    public void checkPropertyIsCleared() throws ConfigurationException {

        configRepository.registerConfigFile(configFile1.getAbsolutePath());
        assertEquals("perf-8.perf.domain.com",
                     configRepository.getProperty("common.testboxes.testbox1.host"));

        configRepository.registerConfigFile(configFile2.getAbsolutePath());
        assertEquals("perf-9.perf.domain.com",
                     configRepository.getProperty("common.testboxes.testbox1.host"));

        configRepository.setTempProperty("common.testboxes.testbox1.host", "host1");
        assertEquals("host1", configRepository.getProperty("common.testboxes.testbox1.host"));

        configRepository.clearTempResources();
        assertEquals("perf-9.perf.domain.com",
                     configRepository.getProperty("common.testboxes.testbox1.host"));
    }

    @Test
    public void verifyOperationsWithgetProperties() throws ConfigurationException {

        configRepository.registerConfigFile(configFile1.getAbsolutePath());
        Map<String, String> properties = configRepository.getProperties("common.testboxes.testbox1");
        assertEquals("perf-8.perf.domain.com", properties.get("common.testboxes.testbox1.host"));

        configRepository.registerConfigFile(configFile2.getAbsolutePath());
        properties = configRepository.getProperties("common.testboxes.testbox1");
        assertEquals("perf-9.perf.domain.com", properties.get("common.testboxes.testbox1.host"));

        configRepository.setTempProperty("common.testboxes.testbox1.host", "host1");
        properties = configRepository.getProperties("common.testboxes.testbox1");
        assertEquals("host1", properties.get("common.testboxes.testbox1.host"));
    }
}
