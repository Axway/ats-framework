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
package com.axway.ats.agent.webapp.client.executors;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.axway.ats.agent.core.Component;
import com.axway.ats.agent.core.ComponentActionMap;
import com.axway.ats.agent.core.ComponentRepository;
import com.axway.ats.agent.core.EnvironmentHandler;
import com.axway.ats.agent.core.MainComponentLoader;
import com.axway.ats.agent.core.MultiThreadedActionHandler;
import com.axway.ats.agent.core.action.ActionRequest;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.webapp.ActionClassOne;
import com.axway.ats.core.threads.ThreadsPerCaller;
import com.axway.ats.junit.BaseTestWebapps;

@RunWith( PowerMockRunner.class)
@PrepareForTest( { MainComponentLoader.class, EnvironmentHandler.class, MultiThreadedActionHandler.class })
public class Test_LocalExecutor extends BaseTestWebapps {

    private MainComponentLoader        mockMainLoader;
    private EnvironmentHandler         mockEnvironmentHandler;
    private MultiThreadedActionHandler mockMultiThreadedActionHandler;

    private static final String        TEST_COMPONENT_NAME = "agenttest";

    @BeforeClass
    public static void setUpTest_LocalExecutor() throws AgentException {

        Component component = new Component(TEST_COMPONENT_NAME);
        ComponentActionMap actionMap = new ComponentActionMap(TEST_COMPONENT_NAME);
        actionMap.registerActionClass(ActionClassOne.class);
        component.setActionMap(actionMap);

        ComponentRepository componentRepository = ComponentRepository.getInstance();
        componentRepository.clear();
        componentRepository.putComponent(component);
    }

    @Before
    public void setUp() {

        // create the mocks
        mockStatic(MainComponentLoader.class);
        mockMainLoader = createMock(MainComponentLoader.class);

        mockStatic(EnvironmentHandler.class);
        mockEnvironmentHandler = createMock(EnvironmentHandler.class);

        mockStatic(MultiThreadedActionHandler.class);
        mockMultiThreadedActionHandler = createMock(MultiThreadedActionHandler.class);

        //init the test values
        ActionClassOne.ACTION_VALUE = 0;

        //prevent loading of agent components from the classpath
        Whitebox.setInternalState(LocalExecutor.class, "isLocalMachineConfigured", true);
    }

    @SuppressWarnings( "unchecked")
    @Test
    public void constructor() throws Exception {

        Whitebox.setInternalState(LocalExecutor.class, "isLocalMachineConfigured", false);

        expect(MainComponentLoader.getInstance()).andReturn(mockMainLoader);
        mockMainLoader.initialize(isA(ArrayList.class));

        replayAll();

        new LocalExecutor();

        verifyAll();

        assertTrue((Boolean) Whitebox.getInternalState(LocalExecutor.class,
                                                       "isLocalMachineConfigured"));
    }

    @Test
    public void secondConstructorCallWillNotReconfigure() throws Exception {

        replayAll();

        new LocalExecutor();

        verifyAll();

        assertTrue((Boolean) Whitebox.getInternalState(LocalExecutor.class,
                                                       "isLocalMachineConfigured"));
    }

    @Test
    public void executeActionPositive() throws Exception {

        LocalExecutor localExecutor = new LocalExecutor();
        localExecutor.executeAction(new ActionRequest(TEST_COMPONENT_NAME, "action 1",
                                                      new Object[]{ 1 }));

        assertEquals(1, ActionClassOne.ACTION_VALUE);
    }

    @Test
    public void cleanPositive() throws Exception {

        expect(EnvironmentHandler.getInstance()).andReturn(mockEnvironmentHandler);
        mockEnvironmentHandler.restore(TEST_COMPONENT_NAME, null, null);

        replayAll();

        LocalExecutor localExecutor = new LocalExecutor();
        localExecutor.restore(TEST_COMPONENT_NAME, null, null);
    }

    @Test
    public void cleanAllPositive() throws Exception {

        expect(EnvironmentHandler.getInstance()).andReturn(mockEnvironmentHandler);
        mockEnvironmentHandler.restoreAll(null);

        replayAll();

        LocalExecutor localExecutor = new LocalExecutor();
        localExecutor.restoreAll(null);
    }

    @Test
    public void backupPositive() throws Exception {

        expect(EnvironmentHandler.getInstance()).andReturn(mockEnvironmentHandler);
        mockEnvironmentHandler.backup(TEST_COMPONENT_NAME, null, null);

        replayAll();

        LocalExecutor localExecutor = new LocalExecutor();
        localExecutor.backup(TEST_COMPONENT_NAME, null, null);
    }

    @Test
    public void backupAllPositive() throws Exception {

        expect(EnvironmentHandler.getInstance()).andReturn(mockEnvironmentHandler);
        mockEnvironmentHandler.backupAll(null);

        replayAll();

        LocalExecutor localExecutor = new LocalExecutor();
        localExecutor.backupAll(null);
    }

    @Test
    public void waitUntilAllQueuesFinishPositive() throws Exception {

        expect(MultiThreadedActionHandler.getInstance(ThreadsPerCaller.getCaller())).andReturn(mockMultiThreadedActionHandler);
        mockMultiThreadedActionHandler.waitUntilAllQueuesFinish();

        replayAll();

        LocalExecutor localExecutor = new LocalExecutor();
        localExecutor.waitUntilQueueFinish();
    }

}
