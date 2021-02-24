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
package com.axway.ats.agent.core.configuration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.testng.Assert.assertNull;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;

import com.axway.ats.core.utils.ExceptionUtils;
import com.axway.ats.log.Log4j2Utils;
import com.axway.ats.log.appenders.ActiveDbAppender;

public class Test_RemoteLoggingConfigurator {

    private static String loggerName = "com.axway.ats";

    @After
    public void tearDown() {

        Log4j2Utils.removeAllAppendersFromLogger(loggerName);
        Log4j2Utils.removeAllAppendersFromLogger(Log4j2Utils.getRootLogger().getName());
    }

    @AfterClass
    public static void tearDownTest_RemoteLoggingConfigurator() {

        //BasicConfigurator.configure();
    }

    @Test
    public void testNeedsApplyNoAppender() {

        RemoteLoggingConfigurator remoteLoggingConfig = new RemoteLoggingConfigurator(null, -1);

        assertFalse(remoteLoggingConfig.needsApplying());
    }

    @Test
    public void testNeedsApplyWithAppenderExpectTrue() {

        ActiveDbAppender appender = ActiveDbAppender.newBuilder()
                                                    .setName("active-db-appender")
                                                    .setHost("test")
                                                    .setDatabase("test")
                                                    .setUser("test")
                                                    .setPassword("test")
                                                    .build();

        Logger log = LogManager.getRootLogger();
        Log4j2Utils.addAppenderToLogger(appender, log.getName());

        //construct the configurator - an appender is present
        RemoteLoggingConfigurator remoteLoggingConfig = new RemoteLoggingConfigurator(null, -1);

        //remove the appender, so the configurator will need to apply it
        //log.removeAppender(appender);
        Log4j2Utils.removeAppenderFromLogger(log.getName(), appender.getName());

        assertTrue(remoteLoggingConfig.needsApplying());
    }

    /**
     * Testing {@link RemoteLoggingConfigurator#needsApplying()} functionality ( must return true, after appender is removed form log4j )
     * and 
     * */
    @Test
    public void testApplyPositiveRootLogger() {

        ActiveDbAppender appender = ActiveDbAppender.newBuilder()
                                                    .setName("active-db-appender")
                                                    .setHost("test")
                                                    .setDatabase("test")
                                                    .setUser("test")
                                                    .setPassword("test")
                                                    .build();

        Logger log = LogManager.getRootLogger();
        Log4j2Utils.addAppenderToLogger(appender, log.getName());

        //construct the configurator - an appender is present
        RemoteLoggingConfigurator remoteLoggingConfig = new RemoteLoggingConfigurator(null, -1);

        //remove the appender, so the configurator will need to apply it
        Log4j2Utils.removeAppenderFromLogger(log.getName(), appender.getName());

        // check if needs to be applied - this sets the internal flags
        // so the next "apply" method will work as expected
        assertTrue(remoteLoggingConfig.needsApplying());

        boolean hasDbCheckError = false;

        try {
            //apply the appender
            //this statement will fail, due to missing PostgreSQL or MSSQL server at localhost
            remoteLoggingConfig.apply();
        } catch (Exception e) {
            if (!ExceptionUtils.containsMessage("Neither MSSQL, nor PostgreSQL server at 'test:1433' has database with name 'test'.",
                                                e, true)) {
                // an exception was caught, but its cause is not the expected one
                // re-throw the exception
                throw e;
            } else {
                // expected exception was caught
                hasDbCheckError = true;
            }
        }

        assertTrue(hasDbCheckError);
    }

    @Test
    public void testRevertPositiveRootLogger() {

        String appenderName = "active-db-appender";

        ActiveDbAppender appender = ActiveDbAppender.newBuilder()
                                                    .setName(appenderName)
                                                    .setHost("test")
                                                    .setDatabase("test")
                                                    .setUser("test")
                                                    .setPassword("test")
                                                    .build();

        Logger log = LogManager.getRootLogger();
        Log4j2Utils.addAppenderToLogger(appender, log.getName());

        //construct the configurator - an appender is present
        RemoteLoggingConfigurator remoteLoggingConfig = new RemoteLoggingConfigurator(null, -1);

        //remove the appender, so the configurator will need to apply it
        Log4j2Utils.removeAppenderFromLogger(log.getName(), appender.getName());

        assertNull(Log4j2Utils.getAppenderFromLogger(loggerName, appenderName));

        //apply the appender
        remoteLoggingConfig.apply();
        remoteLoggingConfig.revert();

        assertNull(Log4j2Utils.getAppenderFromLogger(loggerName, appenderName));

    }

    @Test
    public void testApplyPositive() {

        ActiveDbAppender appender = ActiveDbAppender.newBuilder()
                                                    .setName("active-db-appender")
                                                    .setHost("test")
                                                    .setDatabase("test")
                                                    .setUser("test")
                                                    .setPassword("test")
                                                    .build();

        Logger log = LogManager.getLogger(loggerName);
        Log4j2Utils.addAppenderToLogger(appender, log.getName());

        //construct the configurator - an appender is present
        RemoteLoggingConfigurator remoteLoggingConfig = new RemoteLoggingConfigurator(null, -1);

        //remove the appender, so the configurator will need to apply it
        Log4j2Utils.removeAppenderFromLogger(log.getName(), appender.getName());

        // check if needs to be applied - this sets the internal flags
        // so the next "apply" method will work as expected
        assertTrue(remoteLoggingConfig.needsApplying());

        boolean hasDbCheckError = false;

        try {
            //apply the appender
            //this statement will fail, due to missing PostgreSQL or MSSQL server at localhost
            remoteLoggingConfig.apply();
        } catch (Exception e) {
            if (!e.getCause()
                  .getMessage()
                  .contains("Neither MSSQL, nor PostgreSQL server at 'test:1433' has database with name 'test'.")) {
                // an exception was caught, but its cause is not the expected one
                // re-throw the exception
                throw e;
            } else {
                // expected exception was caught
                hasDbCheckError = true;
            }
        }

        assertTrue(hasDbCheckError);
    }

    @Test
    public void testRevertPositive() {

        String appenderName = "active-db-appender";

        ActiveDbAppender appender = ActiveDbAppender.newBuilder()
                                                    .setName(appenderName)
                                                    .setHost("test")
                                                    .setDatabase("test")
                                                    .setUser("test")
                                                    .setPassword("test")
                                                    .build();

        Logger log = LogManager.getLogger(loggerName);
        Log4j2Utils.addAppenderToLogger(appender, log.getName());

        //construct the configurator - an appender is present
        RemoteLoggingConfigurator remoteLoggingConfig = new RemoteLoggingConfigurator(null, -1);

        //remove the appender, so the configurator will need to apply it
        Log4j2Utils.removeAppenderFromLogger(log.getName(), appender.getName());

        //apply the appender
        remoteLoggingConfig.apply();
        remoteLoggingConfig.revert();

        assertNull(Log4j2Utils.getAppenderFromLogger(loggerName, appenderName));
    }

    @Test
    public void testNeedsApplyUserLoggerLevels() {

        // When creating the following instance, it will remember all loggers with set levels.
        // Then we will check if there is change in the level of known loggers

        // add a new logger, but no level is specified, we will disregard it
        //LogManager.getLogger("fake.logger");
        Log4j2Utils.getLogger("fake.logger");
        RemoteLoggingConfigurator remoteLoggingConfig = new RemoteLoggingConfigurator(null, -1);
        assertFalse(remoteLoggingConfig.needsApplying());

        // add logger and set its level
        Log4j2Utils.setLoggerLevel("fake.logger", Level.INFO);
        // read the log4j configuration and remember that custom logger
        remoteLoggingConfig = new RemoteLoggingConfigurator(null, -1);
        // set same level on same logger, not change is made
        Log4j2Utils.setLoggerLevel("fake.logger", Level.INFO);
        assertFalse(remoteLoggingConfig.needsApplying());

        // change logger level, this is a significant change
        Log4j2Utils.setLoggerLevel("fake.logger", Level.DEBUG);
        assertTrue(remoteLoggingConfig.needsApplying());
    }

    @Test
    public void tesApplyUserLoggerLevels() {

        // set the level
        Log4j2Utils.setLoggerLevel("fake.logger", Level.INFO);

        // read the configuration and remember the level
        RemoteLoggingConfigurator remoteLoggingConfig = new RemoteLoggingConfigurator(null, -1);

        // change the level in log4j
        Log4j2Utils.setLoggerLevel("fake.logger", Level.DEBUG);
        assertTrue(Log4j2Utils.getLogger("fake.logger").getLevel().equals(Level.DEBUG));
        assertTrue(remoteLoggingConfig.needsApplying());

        // apply the remembered level
        remoteLoggingConfig.apply();
        assertTrue(Log4j2Utils.getLogger("fake.logger").getLevel().equals(Level.INFO));
    }

    @Test
    public void testTheGenericAgentConfiguratorAlwaysNeedsApplying() {

        GenericAgentConfigurator genericConfigurator = new GenericAgentConfigurator();
        assertTrue(genericConfigurator.needsApplying());
    }
}
