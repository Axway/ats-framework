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
package com.axway.ats.agent.webapp.client.executors;

import static org.junit.Assert.assertEquals;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.notNull;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.axway.ats.agent.core.action.ActionRequest;
import com.axway.ats.agent.webapp.client.AgentService;
import com.axway.ats.agent.webapp.client.AgentServicePool;
import com.axway.ats.agent.webapp.client.ArgumentWrapper;
import com.axway.ats.core.AtsVersion;
import com.axway.ats.junit.BaseTestWebapps;

@RunWith( PowerMockRunner.class)
@PrepareForTest( { AgentServicePool.class })
public class Test_RemoteExecutor extends BaseTestWebapps {

    private AgentServicePool    mockAgentServicePool;
    private AgentService        mockAgentService;

    private static final String TEST_COMPONENT_NAME = "agenttest";

    @Before
    public void setUp() {

        // create the mocks
        mockStatic(AgentServicePool.class);
        mockAgentServicePool = createMock(AgentServicePool.class);

        mockStatic(AgentService.class);
        mockAgentService = createMock(AgentService.class);

    }

    @SuppressWarnings( "unchecked")
    @Test
    public void executeActionPositive() throws Exception {

        Object resultToReturn = new Integer( 4 );

        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutStream = new ObjectOutputStream( byteOutStream );
        objectOutStream.writeObject( resultToReturn );

        expect( AgentServicePool.getInstance() ).andReturn( mockAgentServicePool ).times( 3 );
        expect( mockAgentServicePool.getClientForHost( "10.1.1.3" ) ).andReturn( mockAgentService );
        expect( mockAgentServicePool.getClientForHost( "HOST_ID:10.1.1.3;THREAD_ID:main" ) ).andReturn( mockAgentService );
        expect( mockAgentServicePool.getClientForHostAndTestcase( "10.1.1.3", "HOST_ID:10.1.1.3;THREAD_ID:main" ) ).andReturn( mockAgentService );
        expect( mockAgentService.pushConfiguration( isA( byte[].class ) ) ).andReturn( AtsVersion.getAtsVersion() );
        mockAgentService.onTestStart( null );
        expect( mockAgentService.executeAction( eq( TEST_COMPONENT_NAME ), eq( "action 1" ),
                                                ( List<ArgumentWrapper> ) notNull() ) ).andReturn( byteOutStream.toByteArray() );
        replayAll();

        RemoteExecutor remoteExecutor = new RemoteExecutor( "10.1.1.3" );
        Object actualResult = remoteExecutor.executeAction( new ActionRequest( TEST_COMPONENT_NAME,
                                                                               "action 1",
                                                                               new Object[]{ 1 } ) );

        verifyAll();

        assertEquals( resultToReturn, actualResult );
    }

    @Test
    public void cleanPositive() throws Exception {

        expect(AgentServicePool.getInstance()).andReturn(mockAgentServicePool).times( 3 );
        expect(mockAgentServicePool.getClientForHost("10.1.1.4")).andReturn(mockAgentService).times( 2 );
        expect( mockAgentServicePool.getClientForHostAndTestcase( "10.1.1.4", "HOST_ID:10.1.1.4;THREAD_ID:main" ) ).andReturn( mockAgentService );
        expect( mockAgentService.pushConfiguration( isA( byte[].class ) ) ).andReturn( AtsVersion.getAtsVersion() );
        mockAgentService.onTestStart( null );
        
        mockAgentService.restoreEnvironment(TEST_COMPONENT_NAME, null, null);

        replayAll();

        RemoteExecutor remoteExecutor = new RemoteExecutor("10.1.1.4");
        remoteExecutor.restore(TEST_COMPONENT_NAME, null, null);

        verifyAll();
    }

    @Test
    public void cleanAllPositive() throws Exception {

        expect(AgentServicePool.getInstance()).andReturn(mockAgentServicePool);
        expect(mockAgentServicePool.getClientForHost("10.1.1.4")).andReturn(mockAgentService);
        mockAgentService.restoreEnvironment(null, null, null);

        replayAll();

        RemoteExecutor remoteExecutor = new RemoteExecutor("10.1.1.4");
        remoteExecutor.restoreAll(null);

        verifyAll();
    }

    @Test
    public void backupPositive() throws Exception {

        expect(AgentServicePool.getInstance()).andReturn(mockAgentServicePool);
        expect(mockAgentServicePool.getClientForHost("10.1.1.4")).andReturn(mockAgentService);
        mockAgentService.backupEnvironment(TEST_COMPONENT_NAME, null, null);

        replayAll();

        RemoteExecutor remoteExecutor = new RemoteExecutor("10.1.1.4");
        remoteExecutor.backup(TEST_COMPONENT_NAME, null, null);

        verifyAll();
    }

    @Test
    public void backupAllPositive() throws Exception {

        expect(AgentServicePool.getInstance()).andReturn(mockAgentServicePool);
        expect(mockAgentServicePool.getClientForHost("10.1.1.4")).andReturn(mockAgentService);
        mockAgentService.backupEnvironment(null, null, null);

        replayAll();

        RemoteExecutor remoteExecutor = new RemoteExecutor("10.1.1.4");
        remoteExecutor.backupAll(null);

        verifyAll();
    }

    @Test
    public void waitUntilAllQueuesFinishPositive() throws Exception {

        expect( AgentServicePool.getInstance() ).andReturn( mockAgentServicePool );
        expect( mockAgentServicePool.getClientForHost( "HOST_ID:10.1.1.4;THREAD_ID:main" ) ).andReturn( mockAgentService );
        mockAgentService.waitUntilAllQueuesFinish();

        replayAll();

        RemoteExecutor remoteExecutor = new RemoteExecutor("10.1.1.4");
        remoteExecutor.waitUntilQueueFinish();

        verifyAll();
    }
}
