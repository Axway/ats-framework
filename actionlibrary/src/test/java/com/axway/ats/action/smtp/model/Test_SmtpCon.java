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
package com.axway.ats.action.smtp.model;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.axway.ats.action.BaseTest;
import com.axway.ats.action.smtp.SmtpConnection;
import com.axway.ats.action.smtp.SmtpManager;

public class Test_SmtpCon extends BaseTest {

    //-------------------------------------------------------------------------
    private static final String host = "unit4.da.localdomain";
    private static final int    port = 25;
    private SmtpManager                smtp;
    private SmtpConnection             connection;

    @Before
    public void setUp() {

        smtp = new SmtpManager();
        smtp.openConnections( host, port, 1 );
        connection = smtp.getConnection( 0 );
    }

    @Ignore
    @Test
    public void authenticate() throws Exception {

        String username = "test123@automation.com";
        String password = "123";
        String mechanism = "PLAIN";
        connection.ehlo( "localhost" );
        assertTrue( connection.authenticate( mechanism, username, password ) );
    }

    @Ignore
    @Test
    public void data() {

    }
}
