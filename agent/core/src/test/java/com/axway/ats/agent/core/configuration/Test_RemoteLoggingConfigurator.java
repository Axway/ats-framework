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
package com.axway.ats.agent.core.configuration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;

import com.axway.ats.core.dbaccess.mssql.DbConnSQLServer;
import com.axway.ats.log.appenders.ActiveDbAppender;

public class Test_RemoteLoggingConfigurator {

    private static String loggerName = "com.axway.ats";

    @After
    public void tearDown() {

        try {
            Logger.getLogger(loggerName).removeAllAppenders();
            Logger.getRootLogger().removeAllAppenders();
        } catch (Exception e) {
            // when a PassiveDbAppender is attached and we try to remove it
            // a channel is created, since no one still exists
            // this leads to the creating of DbEventRequestProcessor that tries to connect to a non-existing database server
            // that connection try fails, so we check the stack trace for it
            assertTrue(containsMessage(e,
                                       "Neither MSSQL, nor PostgreSQL server at 'test:1433' contains ATS log database with name 'test'."));
        }

    }

    @AfterClass
    public static void tearDownTest_RemoteLoggingConfigurator() {

        BasicConfigurator.configure();
    }

    @Test
    public void testNeedsApplyNoAppender() {

        RemoteLoggingConfigurator remoteLoggingConfig = new RemoteLoggingConfigurator(null, -1);

        assertFalse(remoteLoggingConfig.needsApplying());
    }

    @Test
    public void testNeedsApplyWithAppenderExpectTrue() {

        ActiveDbAppender appender = new ActiveDbAppender();
        appender.setHost("test");
        appender.setDatabase("test");
        appender.setUser("test");
        appender.setPassword("test");

        Logger log = Logger.getRootLogger();
        log.addAppender(appender);

        //construct the configurator - an appender is present
        RemoteLoggingConfigurator remoteLoggingConfig = new RemoteLoggingConfigurator(null, -1);

        //remove the appender, so the configurator will need to apply it
        log.removeAppender(appender);

        assertTrue(remoteLoggingConfig.needsApplying());
    }

    /**
     * Testing {@link RemoteLoggingConfigurator#needsApplying()} functionality ( must return true, after appender is removed form log4j )
     * and 
     * */
    @Test
    public void testApplyPositiveRootLogger() {

        ActiveDbAppender appender = new ActiveDbAppender();
        appender.setHost("test");
        appender.setPort(DbConnSQLServer.DEFAULT_PORT + "");
        appender.setDatabase("test");
        appender.setUser("test");
        appender.setPassword("test");

        Logger log = Logger.getRootLogger();
        log.addAppender(appender);

        //construct the configurator - an appender is present
        RemoteLoggingConfigurator remoteLoggingConfig = new RemoteLoggingConfigurator(null, -1);

        //remove the appender, so the configurator will need to apply it
        log.removeAppender(appender);

        // check if needs to be applied - this sets the internal flags
        // so the next "apply" method will work as expected
        assertTrue(remoteLoggingConfig.needsApplying());

        //apply the appender
        //this statement will fail, due to missing PostgreSQL or MSSQL server at localhost
        remoteLoggingConfig.apply();
    }

    @Test
    public void testRevertPositiveRootLogger() {

        ActiveDbAppender appender = new ActiveDbAppender();
        appender.setHost("test");
        appender.setPort(DbConnSQLServer.DEFAULT_PORT + "");
        appender.setDatabase("test");
        appender.setUser("test");
        appender.setPassword("test");

        Logger log = Logger.getRootLogger();
        log.addAppender(appender);

        //construct the configurator - an appender is present
        RemoteLoggingConfigurator remoteLoggingConfig = new RemoteLoggingConfigurator(null, -1);

        //remove the appender, so the configurator will need to apply it
        log.removeAppender(appender);

        //apply the appender
        remoteLoggingConfig.apply();
        remoteLoggingConfig.revert();

        assertFalse(log.getAllAppenders().hasMoreElements());
    }

    @Test
    public void testApplyPositive() {

        ActiveDbAppender appender = new ActiveDbAppender();
        appender.setHost("test");
        appender.setPort(DbConnSQLServer.DEFAULT_PORT + "");
        appender.setDatabase("test");
        appender.setUser("test");
        appender.setPassword("test");

        Logger log = Logger.getLogger(loggerName);
        log.addAppender(appender);

        //construct the configurator - an appender is present
        RemoteLoggingConfigurator remoteLoggingConfig = new RemoteLoggingConfigurator(null, -1);

        //remove the appender, so the configurator will need to apply it
        log.removeAppender(appender);

        // check if needs to be applied - this sets the internal flags
        // so the next "apply" method will work as expected
        assertTrue(remoteLoggingConfig.needsApplying());

        //apply the appender
        //this statement will fail, due to missing PostgreSQL or MSSQL server at localhost
        remoteLoggingConfig.apply();
    }

    @Test
    public void testRevertPositive() {

        ActiveDbAppender appender = new ActiveDbAppender();
        appender.setHost("test");
        appender.setDatabase("test");
        appender.setUser("test");
        appender.setPassword("test");

        Logger log = Logger.getLogger(loggerName);
        log.addAppender(appender);

        //construct the configurator - an appender is present
        RemoteLoggingConfigurator remoteLoggingConfig = new RemoteLoggingConfigurator(null, -1);

        //remove the appender, so the configurator will need to apply it
        log.removeAppender(appender);

        //apply the appender
        remoteLoggingConfig.apply();
        remoteLoggingConfig.revert();

        assertFalse(log.getAllAppenders().hasMoreElements());
    }

    @Test
    public void testNeedsApplyUserLoggerLevels() {

        // When creating the following instance, it will remember all loggers with set levels.
        // Then we will check if there is change in the level of known loggers

        // add a new logger, but no level is specified, we will disregard it
        Logger.getLogger("fake.logger");
        RemoteLoggingConfigurator remoteLoggingConfig = new RemoteLoggingConfigurator(null, -1);
        assertFalse(remoteLoggingConfig.needsApplying());

        // add logger and set its level
        Logger.getLogger("fake.logger").setLevel(Level.INFO);
        // read the log4j configuration and remember that custom logger
        remoteLoggingConfig = new RemoteLoggingConfigurator(null, -1);
        // set same level on same logger, not change is made
        Logger.getLogger("fake.logger").setLevel(Level.INFO);
        assertFalse(remoteLoggingConfig.needsApplying());

        // change logger level, this is a significant change
        Logger.getLogger("fake.logger").setLevel(Level.DEBUG);
        assertTrue(remoteLoggingConfig.needsApplying());
    }

    @Test
    public void tesApplyUserLoggerLevels() {

        // set the level
        Logger.getLogger("fake.logger").setLevel(Level.INFO);

        // read the configuration and remember the level
        RemoteLoggingConfigurator remoteLoggingConfig = new RemoteLoggingConfigurator(null, -1);

        // change the level in log4j
        Logger.getLogger("fake.logger").setLevel(Level.DEBUG);
        assertTrue(Logger.getLogger("fake.logger").getLevel().equals(Level.DEBUG));
        assertTrue(remoteLoggingConfig.needsApplying());

        // apply the remembered level
        remoteLoggingConfig.apply();
        assertTrue(Logger.getLogger("fake.logger").getLevel().equals(Level.INFO));
    }

    @Test
    public void testTheGenericAgentConfiguratorAlwaysNeedsApplying() {

        GenericAgentConfigurator genericConfigurator = new GenericAgentConfigurator();
        assertTrue(genericConfigurator.needsApplying());
    }

    /**
     * Check whether the exception message is contained in some of the stack trace elements
     * @param ex the initial exception (head of the stack trace)
     * @param msg the exception message
     * */
    private boolean containsMessage( Exception ex, String msg ) {

        Throwable t = ex;
        while (t != null) {
            String message = t.getMessage();
            if (message.contains(msg)) {
                return true;
            }
            t = t.getCause();
        }

        return false;
    }
}
