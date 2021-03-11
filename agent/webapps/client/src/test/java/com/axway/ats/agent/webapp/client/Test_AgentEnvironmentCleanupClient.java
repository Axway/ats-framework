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
package com.axway.ats.agent.webapp.client;

import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectNew;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.axway.ats.agent.webapp.client.executors.LocalExecutor;
import com.axway.ats.agent.webapp.client.executors.RemoteExecutor;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.junit.BaseTestWebapps;

@RunWith( PowerMockRunner.class)
@PrepareForTest( { Test_AgentEnvironmentCleanupClient.TestEnvironmentClient.class })
public class Test_AgentEnvironmentCleanupClient extends BaseTestWebapps {
    private static final String TEST_COMPONENT_NAME         = "agenttest";
    private static final int    AGENT_DEFAULT_PORT_FOR_TEST = 8089;

    private static int          portNumberBeforeTest;
    private LocalExecutor       mockLocalExecutor;
    private RemoteExecutor      mockRemoteExecutor;

    @BeforeClass
    public static void setUpClass() {

        portNumberBeforeTest = AtsSystemProperties.getAgentDefaultPort();
    }

    @Before
    public void setUp() {

        // create the mocks
        mockLocalExecutor = createMock(LocalExecutor.class);
        mockRemoteExecutor = createMock(RemoteExecutor.class);

        AtsSystemProperties.setAgentDefaultPort(AGENT_DEFAULT_PORT_FOR_TEST);
    }

    @After
    public void cleanUp() {

        AtsSystemProperties.setAgentDefaultPort(portNumberBeforeTest); // restore
    }

    @Test
    public void cleanLocalPositive() throws Exception {

        expectNew(LocalExecutor.class).andReturn(mockLocalExecutor);
        mockLocalExecutor.restore(TEST_COMPONENT_NAME, null, null);

        replayAll();

        TestEnvironmentClient envClient = new TestEnvironmentClient("local");
        envClient.restore(TEST_COMPONENT_NAME);

        verifyAll();
    }

    @Test
    public void cleanRemotePositive() throws Exception {

        expectNew(RemoteExecutor.class, "10.1.1.1:8089").andReturn(mockRemoteExecutor);
        mockRemoteExecutor.restore(TEST_COMPONENT_NAME, null, null);

        replayAll();

        TestEnvironmentClient envClient = new TestEnvironmentClient("10.1.1.1");
        envClient.restore(TEST_COMPONENT_NAME);

        verifyAll();
    }

    @Test
    public void cleanAllLocalPositive() throws Exception {

        expectNew(LocalExecutor.class).andReturn(mockLocalExecutor);
        mockLocalExecutor.restoreAll(null);

        replayAll();

        TestEnvironmentClient envClient = new TestEnvironmentClient("local");
        envClient.restoreAllComponents();

        verifyAll();
    }

    @Test
    public void cleanAllRemotePositive() throws Exception {

        expectNew(RemoteExecutor.class, "10.1.1.1:8089").andReturn(mockRemoteExecutor);
        mockRemoteExecutor.restoreAll(null);

        replayAll();

        TestEnvironmentClient envClient = new TestEnvironmentClient("10.1.1.1");
        envClient.restoreAllComponents();

        verifyAll();
    }

    @Test
    public void backupLocalPositive() throws Exception {

        expectNew(LocalExecutor.class).andReturn(mockLocalExecutor);
        mockLocalExecutor.backup(TEST_COMPONENT_NAME, null, null);

        replayAll();

        TestEnvironmentClient envClient = new TestEnvironmentClient("local");
        envClient.backup(TEST_COMPONENT_NAME);

        verifyAll();
    }

    @Test
    public void backupRemotePositive() throws Exception {

        expectNew(RemoteExecutor.class, "10.1.1.1:8089").andReturn(mockRemoteExecutor);
        mockRemoteExecutor.backup(TEST_COMPONENT_NAME, null, null);

        replayAll();

        TestEnvironmentClient envClient = new TestEnvironmentClient("10.1.1.1");
        envClient.backup(TEST_COMPONENT_NAME);

        verifyAll();
    }

    @Test
    public void backupAllLocalPositive() throws Exception {

        expectNew(LocalExecutor.class).andReturn(mockLocalExecutor);
        mockLocalExecutor.backupAll(null);

        replayAll();

        TestEnvironmentClient envClient = new TestEnvironmentClient("local");
        envClient.backupAllComponents();

        verifyAll();
    }

    @Test
    public void backupAllRemotePositive() throws Exception {

        expectNew(RemoteExecutor.class, "10.1.1.1:8089").andReturn(mockRemoteExecutor);
        mockRemoteExecutor.backupAll(null);

        replayAll();

        TestEnvironmentClient envClient = new TestEnvironmentClient("10.1.1.1");
        envClient.backupAllComponents();

        verifyAll();
    }

    /**
      * Test class
      */
    public static class TestEnvironmentClient extends EnvironmentCleanupClient {

        public TestEnvironmentClient( String host ) {

            super(host);
        }
    }
}
