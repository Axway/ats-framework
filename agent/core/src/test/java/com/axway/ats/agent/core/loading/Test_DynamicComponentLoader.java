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
package com.axway.ats.agent.core.loading;

import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.Test;

import com.axway.ats.agent.core.BaseTest;
import com.axway.ats.agent.core.ComponentActionMap;
import com.axway.ats.agent.core.ComponentRepository;
import com.axway.ats.agent.core.exceptions.AgentException;

public class Test_DynamicComponentLoader extends BaseTest {


    @Test
    public void testLoadComponentsPositive() throws AgentException {

        // Temporal fix for workaround of rare cleanup issues from other tests
        ComponentRepository.getInstance().clear();
        
        DynamicComponentLoader dynamicLoader = new DynamicComponentLoader( new File( RELATIVE_PATH_TO_TEST_RESOURCES ),
                                                                           new Object() );
        dynamicLoader.loadAvailableComponents( ComponentRepository.getInstance() );
        ComponentActionMap actionMap = ComponentRepository.getInstance()
                                                          .getComponentActionMap( TEST_COMPONENT_NAME );

        assertNotNull( actionMap );
    }
}
