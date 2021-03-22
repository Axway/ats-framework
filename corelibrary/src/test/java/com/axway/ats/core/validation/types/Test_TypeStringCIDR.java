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
public class Test_TypeStringCIDR extends BaseTest {

    private final Validator             validator                   = new Validator();

    /** Valid test data */
    private static final String         VALID_CIDR_ADDRESS          = "10.10.44.32/27";

    /** Invalid test data */
    private static final String         CIDR_ADDRESS_OUT_OF_RANGE   = "10.10.44.32/34";
    private static final String         CIDR_ADDRESS_ALPHA          = "10.10.44.32/abc";
    private static final String         CIDR_ADDRESS_MISSCALCULATED = "10.10.44.90/27";
    private static final String         CIDR_ADDRESS_MALFORMED      = "192.168.1.1-8";
    private static final String         CIDR_ADDRESS_IP_MALFORMED   = "10.1044.32/27";

    private static final ValidationType VALIDATION_TYPE             = ValidationType.STRING_CIDR;

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
    public void testValidation() throws Exception {

        assertTrue(this.validator.validate(VALIDATION_TYPE, VALID_CIDR_ADDRESS));

        assertFalse(this.validator.validate(VALIDATION_TYPE, CIDR_ADDRESS_OUT_OF_RANGE));
        assertFalse(this.validator.validate(VALIDATION_TYPE, CIDR_ADDRESS_ALPHA));
        assertFalse(this.validator.validate(VALIDATION_TYPE, CIDR_ADDRESS_MISSCALCULATED));
        assertFalse(this.validator.validate(VALIDATION_TYPE, CIDR_ADDRESS_MALFORMED));
        assertFalse(this.validator.validate(VALIDATION_TYPE, CIDR_ADDRESS_IP_MALFORMED));
    }
}
