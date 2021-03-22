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
package com.axway.ats.log.model;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;

import com.axway.ats.core.log.AtsLog4jLogger;
import com.axway.ats.log.AtsDbLogger;

// prevents linkage error
@PowerMockIgnore("javax.management.*")
public class Test_AtsDbLogger {

    @BeforeClass
    public static void setUpTest_AutoLogger() {

        // or not ?!?
        AtsLog4jLogger.setLog4JConsoleLoggingOnly();
    }

    @Test
    public void constructAutoLogger() {

        AtsDbLogger autoLogger = AtsDbLogger.getLogger("test.123", true);
        assertNotNull(autoLogger.getInternalLogger());
    }

    @Test
    public void debugMessage() {

        AtsDbLogger autoLogger = AtsDbLogger.getLogger("test.123", true);
        autoLogger.debug("debug");
    }

    @Test
    public void debugMessageWithException() {

        AtsDbLogger autoLogger = AtsDbLogger.getLogger("test.123", true);
        autoLogger.debug("debug with exception", new Exception());
    }

    @Test
    public void errorMessage() {

        AtsDbLogger autoLogger = AtsDbLogger.getLogger("test.123", true);
        autoLogger.error("error");
    }

    @Test
    public void errorMessageWithException() {

        AtsDbLogger autoLogger = AtsDbLogger.getLogger("test.123", true);
        autoLogger.error("error with exception", new Exception());
    }

    @Test
    public void fatalMessage() {

        AtsDbLogger autoLogger = AtsDbLogger.getLogger("test.123", true);
        autoLogger.fatal("fatal");
    }

    @Test
    public void fatalMessageWithException() {

        AtsDbLogger autoLogger = AtsDbLogger.getLogger("test.123", true);
        autoLogger.fatal("fatal with exception", new Exception());
    }

    @Test
    public void infoMessage() {

        AtsDbLogger autoLogger = AtsDbLogger.getLogger("test.123", true);
        autoLogger.info("info");
    }

    @Test
    public void infoMessageWithException() {

        AtsDbLogger autoLogger = AtsDbLogger.getLogger("test.123", true);
        autoLogger.info("info with exception", new Exception());
    }

    @Test
    public void traceMessage() {

        AtsDbLogger autoLogger = AtsDbLogger.getLogger("test.123", true);
        autoLogger.trace("trace");
    }

    @Test
    public void traceMessageWithException() {

        AtsDbLogger autoLogger = AtsDbLogger.getLogger("test.123", true);
        autoLogger.trace("trace with exception", new Exception());
    }

    @Test
    public void warnMessage() {

        AtsDbLogger autoLogger = AtsDbLogger.getLogger("test.123", true);
        autoLogger.warn("warn");
    }

    @Test
    public void warnMessageWithException() {

        AtsDbLogger autoLogger = AtsDbLogger.getLogger("test.123", true);
        autoLogger.warn("warn with exception", new Exception());
    }

    @Test
    public void insertRun() {

        AtsDbLogger autoLogger = AtsDbLogger.getLogger("test.123", true);
        autoLogger.startRun("runName", "osName", "productName", "versionName", "buildName", "hostName");
    }

    @Test
    public void insertGroup() {

        AtsDbLogger autoLogger = AtsDbLogger.getLogger("test.123", true);
        autoLogger.startSuite("package", "groupName");
        autoLogger.startSuite("package", "groupName");
    }

    @Test
    public void insertTestcase() {

        AtsDbLogger autoLogger = AtsDbLogger.getLogger("test.123", true);
        autoLogger.startTestcase("groupName", "groupName", "name", "", "description");
        autoLogger.startTestcase("groupName", "groupName", "name", "", "description");
    }

    @Test
    public void endRun() {

        AtsDbLogger autoLogger = AtsDbLogger.getLogger("test.123", true);
        autoLogger.endRun();
    }

    @Test
    public void endGroup() {

        AtsDbLogger autoLogger = AtsDbLogger.getLogger("test.123", true);
        autoLogger.endSuite();
    }

    @Test
    public void endTestcase() {

        AtsDbLogger autoLogger = AtsDbLogger.getLogger("test.123", true);
        autoLogger.endTestcase(TestCaseResult.PASSED);
    }
}
