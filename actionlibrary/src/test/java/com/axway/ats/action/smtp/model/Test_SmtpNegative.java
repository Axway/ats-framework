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

import static org.junit.Assert.assertEquals;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.axway.ats.action.BaseTest;
import com.axway.ats.action.smtp.SmtpManager;

public class Test_SmtpNegative extends BaseTest {

    private SmtpManager smtp;
    private String      host = "unit4.ne.localdomain";
    private int         port = 25;

    @Before
    public void setUp() {

        smtp = new SmtpManager();
        LogManager.getRootLogger()
              .info("--------------------------START TESTCASE----------------------------------------");

    }

    @After
    public void tearDown() {

        LogManager.getRootLogger()
              .info("--------------------------END TESTCASE----------------------------------------");

        smtp.closeConnection(-1);

    }

    @Ignore
    @Test( expected = RuntimeException.class)
    public void openConnection_nullHost() {

        smtp.openConnections(null, port, 1);
    }

    @Ignore
    @Test( expected = RuntimeException.class)
    public void openConnection_wrongHost() {

        smtp.openConnections("unit.ne.localdomain", port, 1);
    }

    @Ignore
    @Test( expected = RuntimeException.class)
    public void openConnection_wrongPort() {

        smtp.openConnections(host, 2, 1);
    }

    @Ignore
    @Test( expected = RuntimeException.class)
    public void execCommand_nullCommand() {

        assertEquals(1, smtp.openConnections(host, port, 1));
        smtp.execCommand(0, null);
    }

    @Ignore
    @Test( expected = RuntimeException.class)
    public void execCommand() {

        assertEquals(1, smtp.openConnections(host, port, 1));
        smtp.execCommand(-1, "EHLO localhost");
    }

    @Ignore
    @Test( expected = RuntimeException.class)
    public void verifyConnectionArray() {

        smtp.verifyConnection(null);
    }
}
