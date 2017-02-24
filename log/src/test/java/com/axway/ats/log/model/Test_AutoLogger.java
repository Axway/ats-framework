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
package com.axway.ats.log.model;

import static org.junit.Assert.assertNotNull;

import org.apache.log4j.BasicConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.log.model.AutoLogger;
import com.axway.ats.log.model.TestCaseResult;

public class Test_AutoLogger {

    @BeforeClass
    public static void setUpTest_AutoLogger() {

        BasicConfigurator.configure();
    }

    @Test
    public void constructAutoLogger() {

        AutoLogger autoLogger = AutoLogger.getLogger( "test.123" );
        assertNotNull( autoLogger.getInternalLogger() );
    }

    @Test
    public void debugMessage() {

        AutoLogger autoLogger = AutoLogger.getLogger( "test.123" );
        autoLogger.debug( "debug" );
    }

    @Test
    public void debugMessageWithException() {

        AutoLogger autoLogger = AutoLogger.getLogger( "test.123" );
        autoLogger.debug( "debug with exception", new Exception() );
    }

    @Test
    public void errorMessage() {

        AutoLogger autoLogger = AutoLogger.getLogger( "test.123" );
        autoLogger.error( "error" );
    }

    @Test
    public void errorMessageWithException() {

        AutoLogger autoLogger = AutoLogger.getLogger( "test.123" );
        autoLogger.error( "error with exception", new Exception() );
    }

    @Test
    public void fatalMessage() {

        AutoLogger autoLogger = AutoLogger.getLogger( "test.123" );
        autoLogger.fatal( "fatal" );
    }

    @Test
    public void fatalMessageWithException() {

        AutoLogger autoLogger = AutoLogger.getLogger( "test.123" );
        autoLogger.fatal( "fatal with exception", new Exception() );
    }

    @Test
    public void infoMessage() {

        AutoLogger autoLogger = AutoLogger.getLogger( "test.123" );
        autoLogger.info( "info" );
    }

    @Test
    public void infoMessageWithException() {

        AutoLogger autoLogger = AutoLogger.getLogger( "test.123" );
        autoLogger.info( "info with exception", new Exception() );
    }

    @Test
    public void traceMessage() {

        AutoLogger autoLogger = AutoLogger.getLogger( "test.123" );
        autoLogger.trace( "trace" );
    }

    @Test
    public void traceMessageWithException() {

        AutoLogger autoLogger = AutoLogger.getLogger( "test.123" );
        autoLogger.trace( "trace with exception", new Exception() );
    }

    @Test
    public void warnMessage() {

        AutoLogger autoLogger = AutoLogger.getLogger( "test.123" );
        autoLogger.warn( "warn" );
    }

    @Test
    public void warnMessageWithException() {

        AutoLogger autoLogger = AutoLogger.getLogger( "test.123" );
        autoLogger.warn( "warn with exception", new Exception() );
    }

    @Test
    public void insertRun() {

        AutoLogger autoLogger = AutoLogger.getLogger( "test.123" );
        autoLogger.startRun( "runName", "osName", "productName", "versionName", "buildName", "hostName" );
    }

    @Test
    public void insertGroup() {

        AutoLogger autoLogger = AutoLogger.getLogger( "test.123" );
        autoLogger.startSuite( "package", "groupName" );
        autoLogger.startSuite( "package", "groupName" );
    }

    @Test
    public void insertTestcase() {

        AutoLogger autoLogger = AutoLogger.getLogger( "test.123" );
        autoLogger.startTestcase( "groupName", "groupName", "name", "", "description" );
        autoLogger.startTestcase( "groupName", "groupName", "name", "", "description" );
    }

    @Test
    public void endRun() {

        AutoLogger autoLogger = AutoLogger.getLogger( "test.123" );
        autoLogger.endRun();
    }

    @Test
    public void endGroup() {

        AutoLogger autoLogger = AutoLogger.getLogger( "test.123" );
        autoLogger.endSuite();
    }

    @Test
    public void endTestcase() {

        AutoLogger autoLogger = AutoLogger.getLogger( "test.123" );
        autoLogger.endTestcase( TestCaseResult.PASSED );
    }
}
