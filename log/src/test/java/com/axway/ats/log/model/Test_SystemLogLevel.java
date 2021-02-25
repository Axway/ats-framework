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

import static org.junit.Assert.assertTrue;

import org.apache.logging.log4j.Level;
import org.junit.Test;

public class Test_SystemLogLevel {

    @Test
    public void testSystemLevel() {

        // now it is ALL > TRACE > DEBUG > INFO > WARN > ERROR > FATAL > SYSTEM
        // in log4j1.2.xyz it was the othre way around
        assertTrue(SystemLogLevel.SYSTEM_INT < Level.FATAL.intLevel());
    }

    @Test
    public void toLevelString() {

        assertTrue(SystemLogLevel.toLevel("SYSTEM") == SystemLogLevel.SYSTEM);
        assertTrue(SystemLogLevel.toLevel("SYSTEM", Level.DEBUG) == SystemLogLevel.SYSTEM);
    }

    @Test
    public void toLevelInt() {

        assertTrue(SystemLogLevel.toLevel(SystemLogLevel.SYSTEM_INT) == SystemLogLevel.SYSTEM);
        assertTrue(SystemLogLevel.toLevel(SystemLogLevel.SYSTEM_INT, Level.DEBUG) == SystemLogLevel.SYSTEM);
    }

    @Test
    public void toLevelDefaultLevel() {

        assertTrue(SystemLogLevel.toLevel("dasdasd", SystemLogLevel.SYSTEM) == SystemLogLevel.SYSTEM);
        assertTrue(SystemLogLevel.toLevel(null, SystemLogLevel.SYSTEM) == SystemLogLevel.SYSTEM);
        assertTrue(SystemLogLevel.toLevel(111111, SystemLogLevel.SYSTEM) == SystemLogLevel.SYSTEM);
    }
}
