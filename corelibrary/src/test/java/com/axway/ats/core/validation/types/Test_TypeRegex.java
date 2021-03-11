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
public class Test_TypeRegex extends BaseTest {

    private final Validator      validator                          = new Validator();

    /** Valid test data */
    private static final String  VALID_DATE                         = "28/02/2007";
    private static final String  VALID_IP                           = "192.168.1.1";
    private static final String  VALID_HOSTNAME                     = "www.myhost.co.uk";
    private static final String  VALID_HOSTNAME2                    = "s1";
    private static final String  VALID_HOSTNAME3                    = "ho_st.na123-me";
    private static final String  VALID_HOSTNAME4                    = "host.na123-m.test321";
    private static final String  VALID_HOSTNAME5                    = "1hostname4";
    private static final String  VALID_HOSTNAME6                    = "www.3456.com";
    private static final String  VALID_HOSTNAME7                    = "2600.com";
    private static final String  VALID_HOSTNAME8                    = "123455678";
    private static final String  VALID_DOMAIN_NAME_WITH_SUBDOMAIN   = ".google.com";
    private static final String  VALID_DOMAIN_NAME_WITH_SUBDOMAIN2  = ".google.2om";
    private static final String  VALID_DOMAIN_NAME_WITH_SUBDOMAIN3   = "localhost.localdomain";
    
    
    /** Invalid test data */
    private static final String  INVALID_DATE_LEAP                  = "29/02/2007";
    private static final String  INVALID_DATE_WRONG_MONTH           = "28/13/2007";
    private static final String  INVALID_DATE_WRONG_DAY             = "32/03/2007";
    private static final String  INVALID_DATE_WRONG_YEAR            = "32/03/-2";

    private static final Integer NON_STRING_VALUE                   = new Integer(1);
    private static final String  NON_EMPTY_STRING                   = "text-with-no-whitespaces-at-all";
    private static final String  DOUBLEQUOTED_STRING                = " \" \" ";
    private static final String  WHITESPACE_STRING                  = " text with white spaces";

    private static final String  IP_INVALID                         = "192.168.1.1:1111:88";
    private static final String  IP_WITH_NUMBERS_OUT_OF_RANGE_OVER  = "260.168.1.1";
    private static final String  IP_WITH_NUMBERS_OUT_OF_RANGE_BELOW = "192.168.-1.1";
    private static final String  IP_WITH_ALPHA_NUMBER               = "abc.168.1.1";
    private static final String  IP_WITH_FEWER_NUMBERS              = "192.168.1";

    private static final String  HOSTNAME_INVALID                   = "www.myhost.co.uk:1111";
    private static final String  HOSTNAME_INVALID3                  = "hostname.";
    private static final String  HOSTNAME_INVALID_CHARACTERS        = "www.myhost.co.uk\\";
    private static final String  HOSTNAME_INVALID_DOMAIN            = "www.myhost.co.abcdefghijkl";

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
    public void testDateValidation() throws Exception {

        assertTrue(this.validator.validate(ValidationType.STRING_DATE, VALID_DATE));

        assertFalse(this.validator.validate(ValidationType.STRING_DATE, NON_STRING_VALUE));
        assertFalse(this.validator.validate(ValidationType.STRING_DATE, INVALID_DATE_LEAP));
        assertFalse(this.validator.validate(ValidationType.STRING_DATE, INVALID_DATE_WRONG_DAY));
        assertFalse(this.validator.validate(ValidationType.STRING_DATE, INVALID_DATE_WRONG_MONTH));
        assertFalse(this.validator.validate(ValidationType.STRING_DATE, INVALID_DATE_WRONG_YEAR));
    }

    /**
     * Test this certain type's validation
     *
     * @throws Exception if any unexpected error occurs
     */
    @Test
    public void testDomainValidation() throws Exception {

        assertTrue(this.validator.validate(ValidationType.STRING_DOMAIN_NAME, VALID_HOSTNAME ));
        assertTrue(this.validator.validate(ValidationType.STRING_DOMAIN_NAME, VALID_HOSTNAME7 ));

        assertFalse(this.validator.validate(ValidationType.STRING_DOMAIN_NAME, HOSTNAME_INVALID));
        assertFalse(this.validator.validate(ValidationType.STRING_DOMAIN_NAME, HOSTNAME_INVALID_CHARACTERS));
        assertFalse(this.validator.validate(ValidationType.STRING_DOMAIN_NAME, HOSTNAME_INVALID_DOMAIN));
    }

    /**
     * Test this certain type's validation
     *
     * @throws Exception if any unexpected error occurs
     */
    @Test
    public void testDomainOrSubdomainValidation() throws Exception {

        assertTrue( this.validator.validate( ValidationType.STRING_DOMAIN_OR_SUBDOMAIN,
                                             VALID_DOMAIN_NAME_WITH_SUBDOMAIN3 ) );
        assertTrue( this.validator.validate( ValidationType.STRING_HOST_NAME, VALID_HOSTNAME8 ) );

        assertTrue( this.validator.validate( ValidationType.STRING_DOMAIN_OR_SUBDOMAIN,
                                             VALID_DOMAIN_NAME_WITH_SUBDOMAIN ) );
        assertTrue( this.validator.validate( ValidationType.STRING_DOMAIN_OR_SUBDOMAIN,
                                             VALID_DOMAIN_NAME_WITH_SUBDOMAIN2 ) );

        assertFalse( this.validator.validate( ValidationType.STRING_DOMAIN_OR_SUBDOMAIN, HOSTNAME_INVALID ) );
        assertFalse( this.validator.validate( ValidationType.STRING_DOMAIN_OR_SUBDOMAIN,
                                              HOSTNAME_INVALID_CHARACTERS ) );
        assertFalse( this.validator.validate( ValidationType.STRING_DOMAIN_OR_SUBDOMAIN,
                                              HOSTNAME_INVALID_DOMAIN ) );
    }

    /**
     * Test this certain type's validation
     *
     * @throws Exception if any unexpected error occurs
     */
    @Test
    public void testHostnameValidation() throws Exception {

        assertTrue(this.validator.validate(ValidationType.STRING_HOST_NAME, VALID_HOSTNAME));
        assertTrue(this.validator.validate(ValidationType.STRING_HOST_NAME, VALID_HOSTNAME2));
        assertTrue(this.validator.validate(ValidationType.STRING_HOST_NAME, VALID_HOSTNAME3));
        assertTrue(this.validator.validate(ValidationType.STRING_HOST_NAME, VALID_HOSTNAME4));
        assertTrue(this.validator.validate(ValidationType.STRING_HOST_NAME, VALID_HOSTNAME5));
        assertTrue(this.validator.validate(ValidationType.STRING_HOST_NAME, VALID_HOSTNAME6));
        assertTrue(this.validator.validate(ValidationType.STRING_HOST_NAME, VALID_DOMAIN_NAME_WITH_SUBDOMAIN3));

        assertFalse(this.validator.validate(ValidationType.STRING_HOST_NAME, HOSTNAME_INVALID));
        assertFalse(this.validator.validate(ValidationType.STRING_HOST_NAME, HOSTNAME_INVALID3));
        assertFalse(this.validator.validate(ValidationType.STRING_HOST_NAME, HOSTNAME_INVALID_CHARACTERS));
    }
    
    /**
     * Test this certain type's validation
     *
     * @throws Exception if any unexpected error occurs
     */
    @Test
    public void testIpValidation() throws Exception {

        assertTrue(this.validator.validate(ValidationType.STRING_IP, VALID_IP));

        assertFalse(this.validator.validate(ValidationType.STRING_IP, IP_INVALID));
        assertFalse(this.validator.validate(ValidationType.STRING_IP, IP_WITH_FEWER_NUMBERS));
        assertFalse(this.validator.validate(ValidationType.STRING_IP, IP_WITH_NUMBERS_OUT_OF_RANGE_BELOW));
        assertFalse(this.validator.validate(ValidationType.STRING_IP, IP_WITH_NUMBERS_OUT_OF_RANGE_OVER));
        assertFalse(this.validator.validate(ValidationType.STRING_IP, IP_WITH_ALPHA_NUMBER));
    }

    /**
     * Test this certain type's validation
     *
     * @throws Exception if any unexpected error occurs
     */
    @Test
    public void testWhitespaceValidation() throws Exception {

        assertTrue(this.validator.validate(ValidationType.STRING_NO_WHITESPACES, NON_EMPTY_STRING));

        assertFalse(this.validator.validate(ValidationType.STRING_NO_WHITESPACES, WHITESPACE_STRING));
        assertFalse(this.validator.validate(ValidationType.STRING_NO_WHITESPACES, DOUBLEQUOTED_STRING));
    }

    /**
     * Test this certain type's validation
     *
     * @throws Exception if any unexpected error occurs
     */
    @Test
    public void testDQWhitespaceValidation() throws Exception {

        assertTrue(this.validator.validate(ValidationType.STRING_NO_DOUBLEQUOTED_WHITESPACE,
                                           NON_EMPTY_STRING));

        assertFalse(this.validator.validate(ValidationType.STRING_NO_DOUBLEQUOTED_WHITESPACE,
                                            WHITESPACE_STRING));
        assertFalse(this.validator.validate(ValidationType.STRING_NO_DOUBLEQUOTED_WHITESPACE,
                                            DOUBLEQUOTED_STRING));
    }

}
