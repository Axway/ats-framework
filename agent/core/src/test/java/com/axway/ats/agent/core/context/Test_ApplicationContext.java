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
package com.axway.ats.agent.core.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.axway.ats.agent.core.BaseTest;

public class Test_ApplicationContext extends BaseTest {

    @Test
    public void getAttribute() {

        ApplicationContext context = ApplicationContext.getInstance();
        assertNull( context.getAttribute( "asdf" ) );

        context.setAttribute( "test", "valueToMatch" );
        assertEquals( "valueToMatch", context.getAttribute( "test" ) );
    }

    @Test
    public void setAttribute() {

        ApplicationContext context = ApplicationContext.getInstance();
        context.setAttribute( "o'chukchi", "valueToMatch" );
        assertNotNull( context.getAttribute( "o'chukchi" ) );
    }

    @Test
    public void removeAttribute() {

        ApplicationContext context = ApplicationContext.getInstance();
        context.setAttribute( "o'chukchi", "valueToMatch" );
        assertNotNull( context.getAttribute( "o'chukchi" ) );

        context.removeAttribute( "o'chukchi" );
        assertNull( context.getAttribute( "o'chukchi" ) );
    }
}
