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
package com.axway.ats.agent.webapp.agentservice;

import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.axway.ats.agent.core.MainComponentLoader;

@RunWith( PowerMockRunner.class)
@PrepareForTest( { MainComponentLoader.class })
public class Test_AgentWsRequestListener extends BaseTestWebapps {

    private MainComponentLoader mockMainLoader;

    @Before
    public void setUp() {

        // create the mocks
        mockStatic(MainComponentLoader.class);
        mockMainLoader = createMock(MainComponentLoader.class);
    }

    @Test
    public void requestDestroyedPositive() {

        AgentWsRequestListener requestListener = new AgentWsRequestListener();
        requestListener.requestDestroyed(null);
    }

    @Test
    public void requestInitializedPositive() {

        expect(MainComponentLoader.getInstance()).andReturn(mockMainLoader);
        mockMainLoader.blockIfLoading();

        replayAll();

        AgentWsRequestListener requestListener = new AgentWsRequestListener();
        requestListener.requestInitialized(null);

        verifyAll();
    }
}
