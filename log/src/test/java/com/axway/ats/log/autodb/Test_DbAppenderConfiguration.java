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
package com.axway.ats.log.autodb;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.axway.ats.core.log.AtsLog4jLogger;
import com.axway.ats.log.autodb.exceptions.InvalidAppenderConfigurationException;

public class Test_DbAppenderConfiguration {

    static {
        AtsLog4jLogger.setLog4JConsoleLoggingOnly();
    }
    
    @Test
    public void gettersAndSettersPositive() {

        DbAppenderConfiguration appenderConfig = new DbAppenderConfiguration();

        appenderConfig.setHost("host1");
        assertEquals("host1", appenderConfig.getHost());

        appenderConfig.setDatabase("db1");
        assertEquals("db1", appenderConfig.getDatabase());

        appenderConfig.setUser("user1");
        assertEquals("user1", appenderConfig.getUser());

        appenderConfig.setPassword("pass1");
        assertEquals("pass1", appenderConfig.getPassword());

        appenderConfig.setEnableCheckpoints(true);
        assertEquals(true, appenderConfig.getEnableCheckpoints());
    }

    @Test
    public void validatePositive() throws InvalidAppenderConfigurationException {

        DbAppenderConfiguration appenderConfig = new DbAppenderConfiguration();

        appenderConfig.setHost("host1");
        appenderConfig.setDatabase("db1");
        appenderConfig.setUser("user1");
        appenderConfig.setPassword("pass1");
        appenderConfig.setEnableCheckpoints(true);

        appenderConfig.validate();
    }

    @Test( expected = InvalidAppenderConfigurationException.class)
    public void validateNegativeNoHost() throws InvalidAppenderConfigurationException {

        DbAppenderConfiguration appenderConfig = new DbAppenderConfiguration();

        appenderConfig.validate();
    }

    @Test( expected = InvalidAppenderConfigurationException.class)
    public void validateNegativeNoDatabase() throws InvalidAppenderConfigurationException {

        DbAppenderConfiguration appenderConfig = new DbAppenderConfiguration();

        appenderConfig.setHost("host1");

        appenderConfig.validate();
    }

    @Test( expected = InvalidAppenderConfigurationException.class)
    public void validateNegativeNoUser() throws InvalidAppenderConfigurationException {

        DbAppenderConfiguration appenderConfig = new DbAppenderConfiguration();

        appenderConfig.setHost("host1");
        appenderConfig.setDatabase("db1");

        appenderConfig.validate();
    }

    @Test( expected = InvalidAppenderConfigurationException.class)
    public void validateNegativeNoPassword() throws InvalidAppenderConfigurationException {

        DbAppenderConfiguration appenderConfig = new DbAppenderConfiguration();

        appenderConfig.setHost("host1");
        appenderConfig.setDatabase("db1");
        appenderConfig.setUser("user1");

        appenderConfig.validate();
    }
}
