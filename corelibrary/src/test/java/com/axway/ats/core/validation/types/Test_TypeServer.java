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
package com.axway.ats.core.validation.types;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.axway.ats.core.BaseTest;
import com.axway.ats.core.validation.ValidationType;
import com.axway.ats.core.validation.Validator;

/**
 * This Unit test is used to verify the proper work of the
 * {@link TypeObject} class
 */
public class Test_TypeServer extends BaseTest {

    private final Validator             validator                       = new Validator();

    /** Valid test data */
    private static final String         VALID_IPv4                      = "192.168.1.1";
    private static final String         VALID_IPv4_WITH_PORT            = "192.168.1.1:80";
    private static final String         VALID_IPv6_LOCALHOST_COMPRESSED = "::1";
    private static final String         VALID_IPv6_LOCALHOST            = "0:0:0:0:0:0:0:1";
    private static final String         VALID_IPv6_1                    = "2001:db8::1";
    private static final String         VALID_IPv6_2                    = "::ffff:192.0.2.47";
    private static final String         VALID_IPv6_3                    = "::129.144.52.38";
    private static final String         VALID_IPv6_4                    = "::FFFF:d";
    private static final String         VALID_IPv6_5                    = "::129.144.52.38";
    private static final String         VALID_IPv6_6                    = "fe80::20c:29ff:fe38:53b2%1";          // may fail if interface with such ID is not found. with %eth0 it fails on Win.
    private static final String         VALID_IPv6_WITH_PORT_1          = "[::FFFF:d]:5678";
    private static final String         VALID_IPv6_WITH_PORT_2          = "[::FFFF:129.144.52.38]:123";
    private static final String         VALID_IPv6_WITH_PORT_3          = "[fe80::20c:30ff:fe38:53b2%1]:80";
    private static final String         VALID_HOSTNAME                  = "www.myhost.co.uk";
    private static final String         VALID_HOSTNAME_WITH_PORT        = "www.myhost.co.uk:80";
    private static final String         VALID_CIDR_ADDRESS              = "10.10.44.32/27";

    /** Invalid test data */
    private static final String         CIDR_ADDRESS_OUT_OF_RANGE       = "10.10.44.32/34";
    private static final String         MALFORMED_SERVER_ADDRESS        = "192.168.1.1:1111:88";
    private static final String         INVALID_IPv4                    = "260.168.1.1";
    private static final String         INVALID_IPv6_1                  = "::FFFFF:d";
    private static final String         INVALID_IPv6_2                  = ":::FFFF:d";
    private static final String         INVALID_PORT                    = "192.168.1.1:11111111";
    private static final String         INVALID_HOSTNAME                = "wwwmyhostcouk.dsf\\";

    private static final ValidationType TYPE_SERVER                     = ValidationType.STRING_SERVER_ADDRESS;
    private static final ValidationType TYPE_SERVER_WITH_PORT           = ValidationType.STRING_SERVER_WITH_PORT;
    private static final ValidationType TYPE_SERVER_OR_CIDR             = ValidationType.STRING_SERVER_OR_CIDR;

    /**
     * Set up the test cases
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {

        // empty
    }

    /**
     * Test this certain type's validation
     *
     * @throws Exception if any unexpected error occurs
     */
    @Test
    public void testIPValidation() throws Exception {

        assertTrue(this.validator.validate(TYPE_SERVER, VALID_IPv4));
        assertTrue(this.validator.validate(TYPE_SERVER, VALID_IPv6_LOCALHOST_COMPRESSED));
        assertTrue(this.validator.validate(TYPE_SERVER, VALID_IPv6_LOCALHOST));
        assertTrue(this.validator.validate(TYPE_SERVER, VALID_IPv6_1));
        assertTrue(this.validator.validate(TYPE_SERVER, VALID_IPv6_2));
        assertTrue(this.validator.validate(TYPE_SERVER, VALID_IPv6_3));
        assertTrue(this.validator.validate(TYPE_SERVER, VALID_IPv6_4));
        assertTrue(this.validator.validate(TYPE_SERVER, VALID_IPv6_5));
        assertTrue(this.validator.validate(TYPE_SERVER, VALID_IPv6_6));
        assertTrue(this.validator.validate(TYPE_SERVER, VALID_IPv6_WITH_PORT_1));
        assertTrue(this.validator.validate(TYPE_SERVER, VALID_IPv6_WITH_PORT_2));
        assertTrue(this.validator.validate(TYPE_SERVER, VALID_IPv6_WITH_PORT_3));

        assertFalse(this.validator.validate(TYPE_SERVER, INVALID_IPv6_1));
        assertFalse(this.validator.validate(TYPE_SERVER, INVALID_IPv6_2));

        assertTrue(this.validator.validate(TYPE_SERVER, VALID_HOSTNAME));
        assertTrue(this.validator.validate(TYPE_SERVER, VALID_IPv4_WITH_PORT));
        assertTrue(this.validator.validate(TYPE_SERVER, VALID_HOSTNAME_WITH_PORT));

        assertFalse(this.validator.validate(TYPE_SERVER, new Object()));
        assertFalse(this.validator.validate(TYPE_SERVER, VALID_CIDR_ADDRESS));
        assertFalse(this.validator.validate(TYPE_SERVER, CIDR_ADDRESS_OUT_OF_RANGE));
        assertFalse(this.validator.validate(TYPE_SERVER, MALFORMED_SERVER_ADDRESS));
//        assertFalse(this.validator.validate(TYPE_SERVER, INVALID_IPv4));
        assertFalse(this.validator.validate(TYPE_SERVER, INVALID_HOSTNAME));
        assertFalse(this.validator.validate(TYPE_SERVER, INVALID_PORT));
    }

    /**
     * Test this certain type's validation
     *
     * @throws Exception if any unexpected error occurs
     */
    @Test
    public void testServerWithPortValidation() throws Exception {

        assertFalse(this.validator.validate(TYPE_SERVER_WITH_PORT, VALID_IPv4));
        assertFalse(this.validator.validate(TYPE_SERVER_WITH_PORT, VALID_HOSTNAME));
        assertFalse(this.validator.validate(TYPE_SERVER_WITH_PORT, VALID_IPv6_LOCALHOST_COMPRESSED));
        assertFalse(this.validator.validate(TYPE_SERVER_WITH_PORT, VALID_IPv6_LOCALHOST));
        assertFalse(this.validator.validate(TYPE_SERVER_WITH_PORT, VALID_IPv6_1));
        assertFalse(this.validator.validate(TYPE_SERVER_WITH_PORT, VALID_IPv6_2));
        assertFalse(this.validator.validate(TYPE_SERVER_WITH_PORT, VALID_IPv6_3));
        assertFalse(this.validator.validate(TYPE_SERVER_WITH_PORT, VALID_IPv6_4));
        assertFalse(this.validator.validate(TYPE_SERVER_WITH_PORT, VALID_IPv6_5));
        assertFalse(this.validator.validate(TYPE_SERVER_WITH_PORT, VALID_IPv6_6));
        assertFalse(this.validator.validate(TYPE_SERVER_WITH_PORT, INVALID_IPv6_1));
        assertFalse(this.validator.validate(TYPE_SERVER_WITH_PORT, INVALID_IPv6_2));

        assertTrue(this.validator.validate(TYPE_SERVER_WITH_PORT, VALID_IPv6_WITH_PORT_1));
        assertTrue(this.validator.validate(TYPE_SERVER_WITH_PORT, VALID_IPv6_WITH_PORT_2));
        assertTrue(this.validator.validate(TYPE_SERVER_WITH_PORT, VALID_IPv6_WITH_PORT_3));
        assertTrue(this.validator.validate(TYPE_SERVER_WITH_PORT, VALID_IPv4_WITH_PORT));
        assertTrue(this.validator.validate(TYPE_SERVER_WITH_PORT, VALID_HOSTNAME_WITH_PORT));

        assertFalse(this.validator.validate(TYPE_SERVER_WITH_PORT, new Object()));
        assertFalse(this.validator.validate(TYPE_SERVER_WITH_PORT, VALID_CIDR_ADDRESS));
        assertFalse(this.validator.validate(TYPE_SERVER_WITH_PORT, CIDR_ADDRESS_OUT_OF_RANGE));
        assertFalse(this.validator.validate(TYPE_SERVER_WITH_PORT, MALFORMED_SERVER_ADDRESS));
        assertFalse(this.validator.validate(TYPE_SERVER_WITH_PORT, INVALID_IPv4));
        assertFalse(this.validator.validate(TYPE_SERVER_WITH_PORT, INVALID_HOSTNAME));
        assertFalse(this.validator.validate(TYPE_SERVER_WITH_PORT, INVALID_PORT));
    }

    /**
     * Test this certain type's validation
     *
     * @throws Exception if any unexpected error occurs
     */
    @Test
    public void testServerOrCIDRValidation() throws Exception {

        assertTrue(this.validator.validate(TYPE_SERVER_OR_CIDR, VALID_CIDR_ADDRESS));
        assertTrue(this.validator.validate(TYPE_SERVER_OR_CIDR, VALID_IPv4));
        assertTrue(this.validator.validate(TYPE_SERVER_OR_CIDR, VALID_HOSTNAME));
        assertTrue(this.validator.validate(TYPE_SERVER_OR_CIDR, VALID_IPv4_WITH_PORT));
        assertTrue(this.validator.validate(TYPE_SERVER_OR_CIDR, VALID_HOSTNAME_WITH_PORT));

        assertFalse(this.validator.validate(TYPE_SERVER_OR_CIDR, new Object()));
        assertFalse(this.validator.validate(TYPE_SERVER_OR_CIDR, CIDR_ADDRESS_OUT_OF_RANGE));
        assertFalse(this.validator.validate(TYPE_SERVER_OR_CIDR, MALFORMED_SERVER_ADDRESS));
//        assertFalse(this.validator.validate(TYPE_SERVER_OR_CIDR, INVALID_IPv4));
        assertFalse(this.validator.validate(TYPE_SERVER_OR_CIDR, INVALID_HOSTNAME));
        assertFalse(this.validator.validate(TYPE_SERVER_OR_CIDR, INVALID_PORT));
    }
}
