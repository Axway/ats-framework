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
package com.axway.ats.common.system;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class Test_OperatingSystemType {

    @Test
    public void testLinux() {
        assertEquals(OperatingSystemType.getOsType("Linux"), OperatingSystemType.LINUX);
    }

    @Test
    public void testMacOs() {
        assertEquals(OperatingSystemType.getOsType("Mac OS X"), OperatingSystemType.MAC_OS);
    }

    @Test
    public void testWindows_2012_Server() {
        assertEquals(OperatingSystemType.getOsType("Windows Server 2012 R2"), OperatingSystemType.WINDOWS);
    }

    @Test
    public void testWindows_10() {
        assertEquals(OperatingSystemType.getOsType("Windows 10"), OperatingSystemType.WINDOWS);
    }

    @Test
    public void testUnknown() {
        assertEquals(OperatingSystemType.getOsType("Linux_123"), OperatingSystemType.UNKNOWN);
    }
}