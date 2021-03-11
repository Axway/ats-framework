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
package com.axway.ats.action;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.config.exceptions.ConfigurationException;

public class Test_ActionLibraryConfigurator extends BaseTest {

    private static ActionLibraryConfigurator actionLibraryConfigurator;

    @BeforeClass
    public static void setUpTest_ActionLibraryConfigurator() throws ConfigurationException {

        actionLibraryConfigurator = ActionLibraryConfigurator.getInstance();
    }

    @Before
    public void setUp() throws ConfigurationException {

        actionLibraryConfigurator.clearTempProperties();
    }

    @Test
    public void verifyInitialValues() throws ConfigurationException {

        assertEquals("messagesbox", actionLibraryConfigurator.getDefaultMessagesBox());
    }

    @Test
    public void verifyValuesCanBeSetAtRuntimeAndThenCleared() throws ConfigurationException {

        actionLibraryConfigurator.setDefaultMessagesBox("box1");

        assertEquals("box1", actionLibraryConfigurator.getDefaultMessagesBox());

        //now revert back to initial values
        actionLibraryConfigurator.clearTempProperties();

        assertEquals("messagesbox", actionLibraryConfigurator.getDefaultMessagesBox());
    }

}
