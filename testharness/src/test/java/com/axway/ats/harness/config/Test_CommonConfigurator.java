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
package com.axway.ats.harness.config;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.config.ConfigurationRepository;
import com.axway.ats.config.exceptions.ConfigSourceDoesNotExistException;
import com.axway.ats.config.exceptions.ConfigurationException;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.harness.BaseTest;

import junit.framework.AssertionFailedError;

public class Test_CommonConfigurator extends BaseTest {

    private static final String configFile1Name               = "resource1.properties";
    private static final String configFile2Name               = "resource2.xml";
    private static final String nullPropertyConfigFileName    = "resourceNullProperty.properties";
    private static final String unknownPropertyConfigFileName = "resourceUnknownProperty.properties";

    private static File         configFile1;
    private static File         configFile2;
    private static File         nullPropertyConfigFile;
    private static File         unknownPropertyConfigFile;

    private static final String TEXTBOX1_HOST                 = "perf-8.perf.domain.com";
    private static final String TEXTBOX1_HOST2                = "perf-9.perf.domain.com";
    private static final int    TEXTBOX1_DBPORT               = 12345;
    private static final String TEXTBOX1_ADMINPASS            = "PASSWORD123";
    private static final String TEXTBOX1_DBNAME               = "dbname";
    private static final String TEXTBOX1_DBUSER               = "axway_user";
    private static final String TEXTBOX1_DBPASS               = "axway";
    private static final String TEXTBOX1_DBTYPE               = "Oracle";

    private static final String TEXTBOX2_HOST                 = "10.11.12.13";
    private static final String TEXTBOX2_ADMINUSER            = "admin";

    private static final String MAILSERVER_HOST               = "exch2003.perf.domain.com";
    private static final String MAILSERVER_DEFAULTPASS        = "PASSWORD123";

    private static final String MESSAGEBOX_HOST               = "domain.com";
    private static final String MESSAGEBOX_DBNAME             = "messages";
    private static final String MESSAGEBOX_DBUSER             = "root";
    private static final String MESSAGEBOX_DBPASS             = "axway";
    private static final String MESSAGEBOX_DBTABLE            = "messages_axway";

    @BeforeClass
    public static void setUpTest_TestDataConfigurator() throws URISyntaxException {

        URL configFile1URL = Test_CommonConfigurator.class.getResource(configFile1Name);
        configFile1 = new File(configFile1URL.toURI());

        URL configFile2URL = Test_CommonConfigurator.class.getResource(configFile2Name);
        configFile2 = new File(configFile2URL.toURI());

        URL nullPropertyConfigFileURL = Test_CommonConfigurator.class.getResource(nullPropertyConfigFileName);
        nullPropertyConfigFile = new File(nullPropertyConfigFileURL.toURI());

        URL unknownPropertyConfigFileURL = Test_CommonConfigurator.class.getResource(unknownPropertyConfigFileName);
        unknownPropertyConfigFile = new File(unknownPropertyConfigFileURL.toURI());
    }

    @Before
    public void setUp() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException,
                        SecurityException, NoSuchMethodException {

        //re-initialize the repository and configurator
        Method initMethod = ConfigurationRepository.class.getDeclaredMethod("initialize", new Class<?>[]{});
        initMethod.setAccessible(true);
        initMethod.invoke(ConfigurationRepository.getInstance(), new Object[]{});

        CommonConfigurator.clearInstance();
    }

    @Test
    public void registerConfigFile() throws ConfigurationException {

        CommonConfigurator testDataConfigurator = CommonConfigurator.getInstance();
        testDataConfigurator.registerConfigFile(configFile1.getAbsolutePath());
    }

    @Test
    public void registerConfigFileFromClasspath() throws ConfigurationException {

        CommonConfigurator testDataConfigurator = CommonConfigurator.getInstance();
        testDataConfigurator.registerConfigFileFromClasspath("/com/axway/ats/harness/config/resource2.xml");
    }

    @Test( expected = ConfigSourceDoesNotExistException.class)
    public void registerConfigFileNegative() throws ConfigurationException {

        CommonConfigurator testDataConfigurator = CommonConfigurator.getInstance();
        testDataConfigurator.registerConfigFile("dasdasd");
    }

    @Test( expected = ConfigSourceDoesNotExistException.class)
    public void registerConfigFileFromClasspathNegative() throws ConfigurationException {

        CommonConfigurator testDataConfigurator = CommonConfigurator.getInstance();
        testDataConfigurator.registerConfigFileFromClasspath("/asdfasdfas");
    }

    @Test
    public void testBoxProperties() throws ConfigurationException {

        CommonConfigurator testDataConfigurator = CommonConfigurator.getInstance();
        testDataConfigurator.registerConfigFile(configFile1.getAbsolutePath());

        TestBox textbox1 = testDataConfigurator.getTestBox("testbox1");
        TestBox textbox2 = testDataConfigurator.getTestBox("testbox2");

        assertEquals(TEXTBOX1_HOST, textbox1.getHost());
        assertEquals(TEXTBOX1_DBPORT, textbox1.getDbPort());
        assertEquals(TEXTBOX1_ADMINPASS, textbox1.getAdminPass());
        assertEquals(TEXTBOX1_DBNAME, textbox1.getDbName());
        assertEquals(TEXTBOX1_DBUSER, textbox1.getDbUser());
        assertEquals(TEXTBOX1_DBPASS, textbox1.getDbPass());
        assertEquals(TEXTBOX1_DBTYPE.toLowerCase(), textbox1.getDbType().toString().toLowerCase());

        assertEquals(TEXTBOX2_HOST, textbox2.getHost());
        assertEquals(TEXTBOX2_ADMINUSER, textbox2.getAdminUser());
    }

    @Test
    public void testBoxProperties2() throws ConfigurationException {

        CommonConfigurator testDataConfigurator = CommonConfigurator.getInstance();

        testDataConfigurator.registerConfigFile(configFile1.getAbsolutePath());
        TestBox textbox1 = testDataConfigurator.getTestBox("testbox1");
        assertEquals(TEXTBOX1_HOST, textbox1.getHost());

        testDataConfigurator.registerConfigFile(configFile2.getAbsolutePath());
        textbox1 = testDataConfigurator.getTestBox("testbox1");
        assertEquals(TEXTBOX1_HOST2, textbox1.getHost());

        testDataConfigurator.setTempProperty("common.testboxes.testbox1.host", "host1");
        textbox1 = testDataConfigurator.getTestBox("testbox1");
        assertEquals("host1", textbox1.getHost());

        testDataConfigurator.clearTempProperties();
        textbox1 = testDataConfigurator.getTestBox("testbox1");
        assertEquals(TEXTBOX1_HOST2, textbox1.getHost());
    }

    @Test
    public void testGetTestBoxes() {

        CommonConfigurator testDataConfigurator = CommonConfigurator.getInstance();
        testDataConfigurator.registerConfigFile(configFile1.getAbsolutePath());

        List<TestBox> testBoxes = testDataConfigurator.getTestBoxes();
        if (testBoxes == null || testBoxes.size() != 2) {
            throw new AssertionFailedError();
        }
    }

    @Test( expected = ConfigurationException.class)
    public void testBoxNegative() throws ConfigurationException {

        CommonConfigurator testDataConfigurator = CommonConfigurator.getInstance();
        testDataConfigurator.registerConfigFile(configFile1.getAbsolutePath());

        testDataConfigurator.getTestBox("INVALID_TEST_BOX");
    }

    @Test( expected = ConfigurationException.class)
    public void nullProperty() throws ConfigurationException {

        CommonConfigurator testDataConfigurator = CommonConfigurator.getInstance();
        testDataConfigurator.registerConfigFile(nullPropertyConfigFile.getAbsolutePath());
    }

    @Test
    public void unknownProperty() throws ConfigurationException {

        CommonConfigurator testDataConfigurator = CommonConfigurator.getInstance();
        testDataConfigurator.registerConfigFile(unknownPropertyConfigFile.getAbsolutePath());

        ConfigurationRepository.getInstance();
        TestBox testboxUnknownProperty = testDataConfigurator.getTestBox("testboxUnknownProperty");
        assertEquals("value1", testboxUnknownProperty.getProperty("someUnknownProperty"));
    }

    @Test
    public void messageBoxProperties() throws ConfigurationException {

        CommonConfigurator testDataConfigurator = CommonConfigurator.getInstance();
        testDataConfigurator.registerConfigFile(configFile1.getAbsolutePath());

        MessagesBox msgBox = testDataConfigurator.getMessagesBox("axway");
        assertEquals(MESSAGEBOX_HOST, msgBox.getHost());
        assertEquals(MESSAGEBOX_DBNAME, msgBox.getDbName());
        assertEquals(MESSAGEBOX_DBUSER, msgBox.getDbUser());
        assertEquals(MESSAGEBOX_DBPASS, msgBox.getDbPass());
        assertEquals(MESSAGEBOX_DBTABLE, msgBox.getDbTable());
    }

    @Test( expected = ConfigurationException.class)
    public void messageBoxNegative() throws ConfigurationException {

        CommonConfigurator testDataConfigurator = CommonConfigurator.getInstance();
        testDataConfigurator.registerConfigFile(configFile1.getAbsolutePath());

        testDataConfigurator.getMessagesBox("INVALID_MESSAGE_BOX");
    }

    @Test
    public void mailServerProperties() throws ConfigurationException {

        CommonConfigurator testDataConfigurator = CommonConfigurator.getInstance();
        testDataConfigurator.registerConfigFile(configFile1.getAbsolutePath());

        MailServer mailServer = testDataConfigurator.getMailServer("exchange");
        assertEquals(MAILSERVER_HOST, mailServer.getHost());
        assertEquals(MAILSERVER_DEFAULTPASS, mailServer.getDefaultPass());
    }

    @Test( expected = ConfigurationException.class)
    public void mailServerNegative() throws ConfigurationException {

        CommonConfigurator testDataConfigurator = CommonConfigurator.getInstance();
        testDataConfigurator.registerConfigFile(configFile1.getAbsolutePath());

        testDataConfigurator.getMailServer("INVALID_MAIL_SERVER");
    }

    @Test
    public void testLocalHost_HostLocality() {

        String host = "localhost";
        CommonConfigurator.getInstance().setHostLocality(host, false);
        boolean isLocal = HostUtils.isLocalHost(host);
        HostUtils.setHostLocality(host, true); // revert before assert so fail should not leave modified locality globally
        Assert.assertFalse(isLocal);

        host = "127.0.0.1";
        CommonConfigurator.getInstance().setHostLocality(host, false);
        isLocal = HostUtils.isLocalHost(host);
        HostUtils.setHostLocality(host, true); // revert
        Assert.assertFalse(isLocal);

        host = "my.hostname.localD123";
        CommonConfigurator.getInstance().setHostLocality(host, true);
        isLocal = HostUtils.isLocalHost(host);
        HostUtils.setHostLocality(host, false); // revert
        Assert.assertTrue(isLocal);

    }
}
