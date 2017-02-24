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

import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.agent.core.BaseTest;
import com.axway.ats.agent.core.exceptions.ContextException;

public class Test_SessionContext extends BaseTest {

    public static SessionContext context;

    @BeforeClass
    public static void setUPTest_SessionContext() throws ContextException {

        SessionContext.setCurrentContext( "sessionid12345" );
        context = SessionContext.getCurrentContext();
    }

    @Test
    public void getAttribute() {

        assertNull( context.getAttribute( "asdf" ) );

        context.setAttribute( "test", "valueToMatch" );
        assertEquals( "valueToMatch", context.getAttribute( "test" ) );
    }

    @Test
    public void setAttribute() {

        context.setAttribute( "o'chukchi", "valueToMatch" );
        assertNotNull( context.getAttribute( "o'chukchi" ) );
    }

    @Test
    public void removeAttribute() {

        context.setAttribute( "o'chukchi", "valueToMatch" );
        assertNotNull( context.getAttribute( "o'chukchi" ) );

        context.removeAttribute( "o'chukchi" );
        assertNull( context.getAttribute( "o'chukchi" ) );
    }

    @Test
    public void getCurrentContextPositive() throws ContextException {

        assertNotNull( SessionContext.getCurrentContext() );
    }

    @Test(expected = ContextException.class)
    public void getCurrentContextNegative() throws ContextException {

        SessionContext.exitCurrentContext();
        SessionContext.getCurrentContext();
    }
}
