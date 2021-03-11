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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.config.exceptions.ConfigSourceDoesNotExistException;
import com.axway.ats.config.exceptions.ConfigurationException;
import com.axway.ats.config.exceptions.NoSuchPropertyException;

public class Test_AbstractConfigurator extends BaseTest {

    private static final String                  configFile1Name  = "resource1.properties";
    private static final String                  configFile2Name  = "resource2.xml";

    private static File                          configFile1      = null;
    private static File                          configFile2      = null;

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
    public void verifyUserResourceTakesPrecedenceOverOtherResource() throws ConfigurationException {

        TestConfigurator testConfigurator = new TestConfigurator();
        testConfigurator.addConfigFile(configFile2.getAbsolutePath());
        testConfigurator.addConfigFile(configFile1.getAbsolutePath());

        assertEquals("perf-8.perf.domain.com",
                     testConfigurator.getProperty("common.testboxes.testbox1.host"));
        assertEquals(12, testConfigurator.getProperties("common.testboxes").keySet().size());
    }

    @Test
    public void verifyPropertyNotInOneResourceWillBeTakenFromOthers() throws ConfigurationException {

        TestConfigurator testConfigurator = new TestConfigurator();

        testConfigurator.addConfigFile(configFile1.getAbsolutePath());
        testConfigurator.addConfigFile(configFile2.getAbsolutePath());

        assertEquals("exch2003.perf.domain.com",
                     testConfigurator.getProperty("common.mailservers.second-mail-server.host"));
        assertEquals(2,
                     testConfigurator.getProperties("common.mailservers.second-mail-server")
                                     .keySet()
                                     .size());
    }

    @Test
    public void verifyUserResourceIsAddedAsTopPriority() throws ConfigurationException {

        TestConfigurator testConfigurator = new TestConfigurator();

        testConfigurator.addConfigFile(configFile1.getAbsolutePath());
        testConfigurator.addConfigFile(configFile2.getAbsolutePath());

        //the value should be taken from the second resource, as it is added to the top of the list
        assertEquals("perf-9.perf.domain.com",
                     testConfigurator.getProperty("common.testboxes.testbox1.host"));
        assertEquals(12, testConfigurator.getProperties("common.testboxes").keySet().size());
    }

    @Test
    public void verifyInMemoryConfigWillOverwriteAllResources() throws ConfigurationException {

        TestConfigurator testConfigurator = new TestConfigurator();

        testConfigurator.addConfigFile(configFile1.getAbsolutePath());
        testConfigurator.addConfigFile(configFile2.getAbsolutePath());

        testConfigurator.setTempProperty("common.testboxes.testbox1.host", "test");
        assertEquals("test", testConfigurator.getProperty("common.testboxes.testbox1.host"));
        assertEquals(1, testConfigurator.getProperties("common.testboxes").keySet().size());
    }

    @Test
    public void verifyInMemoryConfigCanBeCleared() throws ConfigurationException {

        TestConfigurator testConfigurator = new TestConfigurator();

        testConfigurator.addConfigFile(configFile2.getAbsolutePath());
        testConfigurator.addConfigFile(configFile1.getAbsolutePath());

        testConfigurator.setTempProperty("common.testboxes.testbox1.host", "test");
        assertEquals("test", testConfigurator.getProperty("common.testboxes.testbox1.host"));
        assertEquals(1, testConfigurator.getProperties("common.testboxes").keySet().size());

        testConfigurator.clearTempProperties();
        assertEquals("perf-8.perf.domain.com",
                     testConfigurator.getProperty("common.testboxes.testbox1.host"));
        assertEquals(12, testConfigurator.getProperties("common.testboxes").keySet().size());
    }

    @Test
    public void verifyInMemoryConfigIsFirstAfterClear() throws ConfigurationException {

        TestConfigurator testConfigurator = new TestConfigurator();

        testConfigurator.addConfigFile(configFile1.getAbsolutePath());
        testConfigurator.addConfigFile(configFile2.getAbsolutePath());

        testConfigurator.clearTempProperties();

        testConfigurator.setTempProperty("common.testboxes.testbox1.host", "test");
        assertEquals("test", testConfigurator.getProperty("common.testboxes.testbox1.host"));
        assertEquals(1, testConfigurator.getProperties("common.testboxes").keySet().size());
    }

    @Test( expected = NoSuchPropertyException.class)
    public void verifyDefaultSettingsAreTakenLast() throws ConfigurationException {

        TestConfigurator testConfigurator = new TestConfigurator();
        testConfigurator.addConfigFile(configFile1.getAbsolutePath());

        //the value should be taken from the first resource, as the second one is added to the bottom of the list
        assertEquals("perf-8.perf.domain.com",
                     testConfigurator.getProperty("common.testboxes.testbox1.host"));
        assertEquals(12, testConfigurator.getProperties("common.testboxes").keySet().size());

        //the value should be taken from the first resource, as the second one is added to the bottom of the list
        assertEquals("value1", testConfigurator.getProperty("default.settings.setting1"));
        assertEquals(1, testConfigurator.getProperties("default.settings").keySet().size());
    }

    @Test( expected = NoSuchPropertyException.class)
    public void getPropertyWhichDoesNotExist() throws ConfigurationException {

        TestConfigurator testConfigurator = new TestConfigurator();

        testConfigurator.addConfigFile(configFile1.getAbsolutePath());
        testConfigurator.addConfigFile(configFile2.getAbsolutePath());

        testConfigurator.getProperty("asdfsadf");
    }

    @Test
    public void getPropertiesWhichDoNotExist() throws ConfigurationException {

        TestConfigurator testConfigurator = new TestConfigurator();

        testConfigurator.addConfigFile(configFile1.getAbsolutePath());
        testConfigurator.addConfigFile(configFile2.getAbsolutePath());

        assertEquals(0, testConfigurator.getProperties("sdasdasd").keySet().size());
    }

    @Test( expected = ConfigSourceDoesNotExistException.class)
    public void verifyExceptionIsThrownIfUserResourceDoesNotExist() throws ConfigurationException {

        TestConfigurator testConfigurator = new TestConfigurator();
        testConfigurator.addConfigFileFromClassPath("nonExistingFile.properties", false, true);
    }

    @Test
    public void getIntegerPropertyPositive() throws ConfigurationException {

        TestConfigurator testConfigurator = new TestConfigurator();
        testConfigurator.addConfigFile(configFile1.getAbsolutePath());

        assertEquals(3, testConfigurator.getIntegerProperty("test.integer"));
    }

    @Test( expected = ConfigurationException.class)
    public void getIntegerPropertyNegativeIntOverflow() throws ConfigurationException {

        TestConfigurator testConfigurator = new TestConfigurator();
        testConfigurator.addConfigFile(configFile1.getAbsolutePath());

        testConfigurator.getIntegerProperty("test.long");
    }

    @Test( expected = ConfigurationException.class)
    public void getIntegerPropertyNegativeNotANumber() throws ConfigurationException {

        TestConfigurator testConfigurator = new TestConfigurator();
        testConfigurator.addConfigFile(configFile1.getAbsolutePath());

        testConfigurator.getIntegerProperty("common.testboxes.testbox2.db");
    }

    @Test
    public void getLongPropertyPositive() throws ConfigurationException {

        TestConfigurator testConfigurator = new TestConfigurator();
        testConfigurator.addConfigFile(configFile1.getAbsolutePath());

        assertEquals(3L, testConfigurator.getLongProperty("test.integer"));
        assertEquals(50000000000000L, testConfigurator.getLongProperty("test.long"));
    }

    @Test( expected = ConfigurationException.class)
    public void getLongPropertyNegativeNotANumber() throws ConfigurationException {

        TestConfigurator testConfigurator = new TestConfigurator();
        testConfigurator.addConfigFile(configFile1.getAbsolutePath());

        testConfigurator.getLongProperty("common.testboxes.testbox2.db");
    }

    private static class TestConfigurator extends AbstractConfigurator {

        @Override
        protected void reloadData() throws ConfigurationException {

        }
    }
}
