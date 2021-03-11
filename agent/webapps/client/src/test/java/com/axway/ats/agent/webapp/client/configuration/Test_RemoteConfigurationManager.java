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
package com.axway.ats.agent.webapp.client.configuration;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.axway.ats.agent.core.configuration.RemoteLoggingConfigurator;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.webapp.client.AgentException_Exception;
import com.axway.ats.agent.webapp.client.AgentService;
import com.axway.ats.agent.webapp.client.AgentServicePool;
import com.axway.ats.core.AtsVersion;
import com.axway.ats.junit.BaseTestWebapps;

@RunWith( PowerMockRunner.class)
@PrepareForTest( { AgentServicePool.class })
public class Test_RemoteConfigurationManager extends BaseTestWebapps {

    private AgentServicePool mockAgentServicePool;
    private AgentService     mockAgentService;

    @Before
    public void setUp() {

        // create the mocks
        mockStatic(AgentServicePool.class);
        mockAgentServicePool = createMock(AgentServicePool.class);
        mockAgentService = createMock(AgentService.class);
    }

    @Test
    public void pushConfigurationPositive() throws AgentException, AgentException_Exception {

        expect(AgentServicePool.getInstance()).andReturn(mockAgentServicePool);
        expect(mockAgentServicePool.getClient("10.0.0.2")).andReturn(mockAgentService);
        expect(mockAgentService.pushConfiguration(isA(byte[].class))).andReturn(AtsVersion.getAtsVersion());

        replayAll();

        new RemoteConfigurationManager().pushConfiguration("10.0.0.2",
                                                           new RemoteLoggingConfigurator(null, -1));

        // verify results
        verifyAll();

        //        assertTrue( RemoteConfigurationManager.getInstance().isConfigured( "10.0.0.2",
        //                                                                           new RemoteLoggingConfigurator() ) );
    }

    @Test( expected = AgentException.class)
    public void pushConfigurationNegativeException() throws AgentException, AgentException_Exception {

        expect(AgentServicePool.getInstance()).andReturn(mockAgentServicePool);
        expect(mockAgentServicePool.getClient("10.0.0.3")).andReturn(mockAgentService);
        mockAgentService.pushConfiguration(isA(byte[].class));
        expectLastCall().andThrow(new AgentException_Exception("test",
                                                               new com.axway.ats.agent.webapp.client.AgentException()));

        replayAll();

        new RemoteConfigurationManager().pushConfiguration("10.0.0.3",
                                                           new RemoteLoggingConfigurator(null, -1));

        // verify results
        verifyAll();
    }

    @Test( expected = AgentException.class)
    public void pushConfigurationNegativeRuntimeException() throws AgentException, AgentException_Exception {

        expect(AgentServicePool.getInstance()).andReturn(mockAgentServicePool);
        expect(mockAgentServicePool.getClient("10.0.0.3")).andReturn(mockAgentService);
        mockAgentService.pushConfiguration(isA(byte[].class));
        expectLastCall().andThrow(new RuntimeException());

        replayAll();

        new RemoteConfigurationManager().pushConfiguration("10.0.0.3",
                                                           new RemoteLoggingConfigurator(null, -1));

        // verify results
        verifyAll();
    }
}
