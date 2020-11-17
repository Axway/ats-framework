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

import static org.junit.Assert.assertTrue;

import org.apache.logging.log4j.Level;
import org.junit.Test;

import com.axway.ats.log.model.AutoLevel;

public class Test_AutoLevel {

    @Test
    public void testSystemLevel() {

        assertTrue(AutoLevel.SYSTEM_INT > Level.FATAL_INT);
    }

    @Test
    public void toLevelString() {

        assertTrue(AutoLevel.toLevel("SYSTEM") == AutoLevel.SYSTEM);
        assertTrue(AutoLevel.toLevel("SYSTEM", Level.DEBUG) == AutoLevel.SYSTEM);
    }

    @Test
    public void toLevelInt() {

        assertTrue(AutoLevel.toLevel(AutoLevel.SYSTEM_INT) == AutoLevel.SYSTEM);
        assertTrue(AutoLevel.toLevel(AutoLevel.SYSTEM_INT, Level.DEBUG) == AutoLevel.SYSTEM);
    }

    @Test
    public void toLevelDefaultLevel() {

        assertTrue(AutoLevel.toLevel("dasdasd", AutoLevel.SYSTEM) == AutoLevel.SYSTEM);
        assertTrue(AutoLevel.toLevel(null, AutoLevel.SYSTEM) == AutoLevel.SYSTEM);
        assertTrue(AutoLevel.toLevel(111111, AutoLevel.SYSTEM) == AutoLevel.SYSTEM);
    }
}
