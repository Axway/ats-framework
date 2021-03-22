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
package com.axway.ats.harness.config;

import org.junit.Test;

import com.axway.ats.config.exceptions.NullOrEmptyConfigurationPropertyException;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.harness.BaseTest;

public class Test_TestBox extends BaseTest {

    private static final String ONLY_SPACE_STR               = " ";
    private static final String NEGATIVE_NUMBER_FOR_PORT_STR = "-2";
    private static final String TOO_HIGH_PORT_NUMBER_STR     = "" + (HostUtils.HIGHEST_PORT_NUMBER + 1);
    private static final String NOT_A_NUMBER_PORT_STR        = "1ab";

    @Test( expected = NullOrEmptyConfigurationPropertyException.class)
    public void testHostNull() throws NullOrEmptyConfigurationPropertyException {

        TestBox testBox = new TestBox();
        testBox.setHost(null);
    }

    @Test( expected = NullOrEmptyConfigurationPropertyException.class)
    public void testHostEmpty() throws NullOrEmptyConfigurationPropertyException {

        TestBox testBox = new TestBox();
        testBox.setHost(ONLY_SPACE_STR); // test trim too
    }

    @Test( expected = NullOrEmptyConfigurationPropertyException.class)
    public void testDbPortNull() throws NullOrEmptyConfigurationPropertyException {

        TestBox testBox = new TestBox();
        testBox.setDbPort(null);
    }

    @Test( expected = NullOrEmptyConfigurationPropertyException.class)
    public void testDbPortEmpty() throws NullOrEmptyConfigurationPropertyException {

        TestBox testBox = new TestBox();
        testBox.setDbPort(ONLY_SPACE_STR);
    }

    @Test( expected = IllegalArgumentException.class)
    public void testDbPortNegativeInteger() throws IllegalArgumentException {

        TestBox testBox = new TestBox();
        testBox.setDbPort(NEGATIVE_NUMBER_FOR_PORT_STR);
    }

    @Test( expected = IllegalArgumentException.class)
    public void testDbPortTooHigh() throws IllegalArgumentException {

        TestBox testBox = new TestBox();
        testBox.setDbPort(TOO_HIGH_PORT_NUMBER_STR);
    }

    @Test( expected = IllegalArgumentException.class)
    public void testDbPortNotANumber() throws IllegalArgumentException {

        TestBox testBox = new TestBox();
        testBox.setDbPort(NOT_A_NUMBER_PORT_STR);
    }

    @Test( expected = NullOrEmptyConfigurationPropertyException.class)
    public void testDbNameNull() throws NullOrEmptyConfigurationPropertyException {

        TestBox testBox = new TestBox();
        testBox.setDbName(null);
    }

    @Test( expected = NullOrEmptyConfigurationPropertyException.class)
    public void testDbNameEmpty() throws NullOrEmptyConfigurationPropertyException {

        TestBox testBox = new TestBox();
        testBox.setDbName(ONLY_SPACE_STR);
    }

    @Test( expected = NullOrEmptyConfigurationPropertyException.class)
    public void testDbTypeNull() throws NullOrEmptyConfigurationPropertyException {

        TestBox testBox = new TestBox();
        testBox.setDbType(null);
    }

    @Test( expected = NullOrEmptyConfigurationPropertyException.class)
    public void testDbTypeEmpty() throws NullOrEmptyConfigurationPropertyException {

        TestBox testBox = new TestBox();
        testBox.setDbType(ONLY_SPACE_STR);
    }

    @Test( expected = NullOrEmptyConfigurationPropertyException.class)
    public void testAdminUserNull() throws NullOrEmptyConfigurationPropertyException {

        TestBox testBox = new TestBox();
        testBox.setAdminUser(null);
    }

    @Test( expected = NullOrEmptyConfigurationPropertyException.class)
    public void testAdminUserEmpty() throws NullOrEmptyConfigurationPropertyException {

        TestBox testBox = new TestBox();
        testBox.setAdminUser(ONLY_SPACE_STR);
    }

    @Test( expected = NullOrEmptyConfigurationPropertyException.class)
    public void testDbUserNull() throws NullOrEmptyConfigurationPropertyException {

        TestBox testBox = new TestBox();
        testBox.setDbUser(null);
    }

    @Test( expected = NullOrEmptyConfigurationPropertyException.class)
    public void testDbUserEmpty() throws NullOrEmptyConfigurationPropertyException {

        TestBox testBox = new TestBox();
        testBox.setDbUser(ONLY_SPACE_STR);
    }

    // Code expects not null. Possible misbehavior - why pass should not be null
    @Test( expected = NullOrEmptyConfigurationPropertyException.class)
    public void testAdminPassEmpty() throws NullOrEmptyConfigurationPropertyException {

        TestBox testBox = new TestBox();
        testBox.setAdminPass(ONLY_SPACE_STR);
    }

    // Code expects not null. Possible misbehavior - why pass should not be null
    @Test( expected = NullOrEmptyConfigurationPropertyException.class)
    public void testDbPassEmpty() throws NullOrEmptyConfigurationPropertyException {

        TestBox testBox = new TestBox();
        testBox.setDbPass(ONLY_SPACE_STR);
    }

}
