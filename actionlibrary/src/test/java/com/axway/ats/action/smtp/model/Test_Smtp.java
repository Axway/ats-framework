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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.axway.ats.action.BaseTest;
import com.axway.ats.action.smtp.SmtpConnection;
import com.axway.ats.action.smtp.SmtpManager;

public class Test_Smtp extends BaseTest {

    private SmtpManager smtpMgr;
    private String      host = "unit4.da.localdomain";
    private int         port = 25;

    @Before
    public void setUp() {

        smtpMgr = new SmtpManager();
        LogManager.getRootLogger()
              .info("--------------------------START TESTCASE----------------------------------------");

    }

    @After
    public void tearDown() {

        LogManager.getRootLogger()
              .info("--------------------------END TESTCASE----------------------------------------");

        smtpMgr.closeConnection(-1);

    }

    @Ignore
    @Test
    public void closeConnection() {

        assertEquals(2, smtpMgr.openConnections(host, port, 2));
        smtpMgr.closeConnection(0);
        smtpMgr.verifyConnection(0);
        smtpMgr.closeConnection(1);
        smtpMgr.verifyConnection(0, 1);
        assertEquals(4, smtpMgr.openConnections(host, port, 2));
        smtpMgr.closeConnection(-1);
        smtpMgr.verifyConnection(0, 1, 2, 3);

    }

    @Ignore
    @Test
    public void execCommand() {

        assertEquals(2, smtpMgr.openConnections(host, port, 2));

        assertEquals("5.5.2 Error: command not recognized", smtpMgr.execCommand(0, "EHLO!!!!"));
        assertEquals("5.5.2 Error: command not recognized", smtpMgr.execCommand(1, "EHLO!!!!"));
        assertFalse("5.5.2 Error: command not recognized".equals(smtpMgr.execCommand(0, "EHLO localhost")));
        assertFalse("5.5.2 Error: command not recognized".equals(smtpMgr.execCommand(1, "EHLO localhost")));

    }

    @Ignore
    @Test
    public void getConnectionObject() {

        assertEquals(1, smtpMgr.openConnections(host, port, 1));
        assertNotNull(smtpMgr.getConnection(0));
        assertNull(smtpMgr.getConnection(1));
        assertEquals(2, smtpMgr.openConnections(host, port, 1));
        assertNotNull(smtpMgr.getConnection(1));
        assertNull(smtpMgr.getConnection(2));
        assertNull(smtpMgr.getConnection(-1));

    }

    @Ignore
    @Test
    public void openConnection() {

        assertEquals(0, smtpMgr.openConnections(host, port, 0));
        assertEquals(1, smtpMgr.openConnections(host, port, 1));
        assertEquals(3, smtpMgr.openConnections(host, port, 2));
        assertEquals(3, smtpMgr.openConnections(host, port, -1));
        //The default port(25) is used
        assertEquals(4, smtpMgr.openConnections(host, -1, 1));
        assertEquals(4, smtpMgr.openConnections(host, port, -1));

    }

    @Ignore
    @Test
    public void verifyConnection() {

        assertEquals(10, smtpMgr.openConnections(host, port, 10));
        smtpMgr.verifyConnection(0, 0);
        smtpMgr.verifyConnection(1, 1);
        smtpMgr.verifyConnection(9, 9);
        smtpMgr.verifyConnection(0, 9);
        smtpMgr.verifyConnection(2, 4);
        smtpMgr.verifyConnection(9, 2);

        smtpMgr.verifyConnection(20, 2);
        smtpMgr.verifyConnection(0, 10);
        smtpMgr.verifyConnection(100, 100);
        smtpMgr.verifyConnection(-99, 100);
        smtpMgr.verifyConnection(-1, -1);
        smtpMgr.verifyConnection(-100, -1);

        smtpMgr.closeConnection(4);
        smtpMgr.verifyConnection(0, 9);
        smtpMgr.verifyConnection(0, 3);
        smtpMgr.verifyConnection(5, 9);

    }

    @Ignore
    @Test
    public void verifyConnectionArray() {

        assertEquals(10, smtpMgr.openConnections(host, port, 10));
        smtpMgr.verifyConnection(1, 2, 3);
        smtpMgr.verifyConnection(0);
        smtpMgr.verifyConnection(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        smtpMgr.verifyConnection(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

        smtpMgr.verifyConnection(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        smtpMgr.verifyConnection(-1);
        smtpMgr.verifyConnection(-20, -32, 23, -32, 4);
        smtpMgr.verifyConnection(10, 10, 10, 10, 10);

        smtpMgr.closeConnection(2);
        smtpMgr.verifyConnection(0, 2, 3);
        smtpMgr.verifyConnection(0, 1, 3);
        smtpMgr.verifyConnection(new int[]{ 1, 3 });
        smtpMgr.verifyConnection(new int[]{});

    }

    @Ignore
    @Test
    public void verifyConnectionCanBeOpened() {

        smtpMgr.verifyConnectionCanBeOpened(host, port);
        smtpMgr.verifyConnectionCanBeOpened(host, -1);
        smtpMgr.verifyConnectionCanBeOpened(host + ".NOSUCHHOST", port);
        smtpMgr.verifyConnectionCanBeOpened(host, 1);
        smtpMgr.verifyConnectionCanBeOpened(null, port);
    }

    @Ignore
    @Test
    public void verifyConnectionCannotBeOpened() {

        smtpMgr.verifyConnectionCannotBeOpened(host, 1);
        smtpMgr.verifyConnectionCannotBeOpened(host + ".NOSUCHHOST", port);

        smtpMgr.verifyConnectionCannotBeOpened(host, port);
        smtpMgr.verifyConnectionCannotBeOpened(null, port);

    }

    @Ignore
    @Test
    public void sendData() throws IOException {

        smtpMgr.openConnections(host, port, 1);
        SmtpConnection smtpConnection = smtpMgr.getConnection(0);

        smtpConnection.helo("t.t.com");
        smtpConnection.mailFrom("t@t.com", null);
        smtpConnection.rcptTo("test197@automator.da.localdomain", null);
        smtpConnection.data("Subject: Test 123\nTo:t@t.com");
        smtpConnection.quit();
    }

}
